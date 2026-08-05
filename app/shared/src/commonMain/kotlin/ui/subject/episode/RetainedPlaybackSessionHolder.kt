/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.subject.episode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import me.him188.ani.app.domain.player.VideoLoadingState
import me.him188.ani.app.ui.foundation.playback.LocalPlaybackSessionEntry
import me.him188.ani.app.ui.foundation.playback.PlaybackSessionEntry
import me.him188.ani.app.ui.foundation.playback.RetainedPlaybackSessionInfo
import me.him188.ani.app.ui.mediaselect.summary.MediaSelectorSummary
import org.openani.mediamp.PlaybackState
import org.openani.mediamp.isPlaying
import kotlin.time.Duration.Companion.seconds

/**
 * 后台会话里值得打断用户去提示一声的事 (见 [RetainedPlaybackSessionHolder.notices]).
 *
 * 只提示"用户不知道就会白等"的状态: 起播就绪 (可以回来看了), 以及各种再等也不会自己好的问题
 * (要用户回去换源/手选). 播放页在前台时一概不发 —— 那些状态界面上本来就写着.
 *
 * 文案见 `rememberRetainedPlaybackNoticeTexts`.
 */
@Immutable
sealed interface RetainedPlaybackNotice {
    /** 数据源解析完成且播放器已经开播 (缓冲出了画面), 回去就能看. */
    data object Ready : RetainedPlaybackNotice

    /** 解析数据源失败. [cause] 决定提示里的原因. */
    data class LoadFailed(val cause: VideoLoadingState.Failed) : RetainedPlaybackNotice

    /** 流解析出来了但播放器打不开 (常见于 web 源嗅探到的地址失效). */
    data object PlayerError : RetainedPlaybackNotice

    /** 所有数据源都查完了, 没有可播放的结果. */
    data object NoMediaFound : RetainedPlaybackNotice

    /** 查完了但没有自动选中 (偏好不是 WEB 时不自动选), 在等用户自己挑. */
    data object NeedsManualSelection : RetainedPlaybackNotice
}

/**
 * 保留播放会话的宿主 (见 `AniUiBehavior.retainPlaybackSession`).
 *
 * 播放页的 [EpisodeViewModel] 默认挂在播放页那个返回栈条目上, 按返回退出即被销毁 —— 播放器、
 * 已经搜出来的数据源、已经解析好的播放流全部作废, 再进去从头再来 (而"从头再来"在 web 源上意味着
 * 重搜一遍 + 重新嗅探视频地址, 是十几秒的量级). 本类把它挪到**自己的** [ViewModelStore] 里,
 * 而本类挂在应用根部, 于是:
 *
 * - 退出播放页只销毁界面, 会话照旧活着, 再进来 [androidx.lifecycle.viewmodel.compose.viewModel]
 *   按同一个 key 拿回同一个 [EpisodeViewModel] —— 状态自然接上, 没有任何"恢复"逻辑;
 * - 数据源还在搜的时候退出去, 搜索继续跑 (见 [guard] 里对 `pageState` 的订阅), 起播就绪或者
 *   卡住了都从 [notices] 发一声让外面提示用户 —— 这正是这套机制的用处;
 * - 全程只保留一个会话: [prepare] 见到不是同一集就先 [ViewModelStore.clear] 掉旧的再让调用方
 *   建新的. **先销后建**是刻意的 —— 两个 ExoPlayer 同时在场会在低端电视盒子上抢硬件解码器;
 * - 会话随应用界面销毁 ([onCleared]), 不跨进程存活.
 *
 * 用一个私有 [ViewModelStore] 而不是自己 new [EpisodeViewModel]: `ViewModel.onCleared` 是
 * protected 的, 只有 store 能正确触发它 (播放器的释放、进度落库全挂在那条链上), 而 store 里
 * 恒定只有一个 VM, 所以"销毁当前会话"就是 `clear()`.
 *
 * 侧边栏等入口只看 [PlaybackSessionEntry] 这一小片接口 (经 [LocalPlaybackSessionEntry] 拿到),
 * 不认识本类, 也就不可能自己造出第二个播放器.
 */
class RetainedPlaybackSessionHolder : ViewModel(), ViewModelStoreOwner, PlaybackSessionEntry {
    /**
     * 当前会话的 store, 恒定只装一个 [EpisodeViewModel].
     *
     * 一个会话一个 store (而不是共用一个 store 按 key 区分): [ViewModelStore.clear] 是清全部,
     * 要做到"销毁上一个会话而不动新的"就只能一个会话一个 store.
     */
    private var currentStore = ViewModelStore()

    override val viewModelStore: ViewModelStore get() = currentStore

    /**
     * 已被替换、但界面可能还在退场动画里的上一个会话.
     *
     * 不当场销毁它: 播放页的组合还活着的时候销毁 VM, 那个页面就会拿着一个已经关掉的播放器继续
     * 渲染 (退场动画期间它仍可能重组, 把 surface 重新交给已 release 的 ExoPlayer). 等它的组合
     * 真正销毁 ([onPageDisposed]) 再清. 新会话要先搜数据源 (秒级) 才会碰解码器, 这点重叠无害.
     */
    private var retiringStore: ViewModelStore? = null
    private var retiringVm: EpisodeViewModel? = null

    /** 当前有播放页组合在场的那个 VM (为 null 表示播放页不在组合里). */
    private var composed: EpisodeViewModel? = null

    override var session: RetainedPlaybackSessionInfo? by mutableStateOf(null)
        private set

    /**
     * 当前会话的 VM; 它本来就在 [currentStore] 里, 这里只是留个取得到的引用.
     *
     * 是 snapshot state: [ComposeRetainedContent] 要跟着它变.
     */
    private var current: EpisodeViewModel? by mutableStateOf(null)

    /** 播放页此刻是否在前台. 由导航状态驱动 ([setPlayerPageVisible]). */
    private val playerPageVisible = MutableStateFlow(true)

    private val _notices = MutableSharedFlow<RetainedPlaybackNotice>(extraBufferCapacity = 4)

    /**
     * 会话在**后台**发生了用户该知道的事时发一次 (就绪 / 各种要用户处理的问题).
     *
     * 只在不看着播放页的时候发: 在播放页上画面自己就动起来了、错误也直接写在画面上, 再弹提示是噪音.
     */
    val notices: SharedFlow<RetainedPlaybackNotice> get() = _notices

    /** 监视当前会话的协程 (见 [guard]); 换会话时重启. */
    private var guardJob: Job? = null

    /**
     * 进播放页时先调用: 要播的不是当前保留的那一集就销毁旧会话, 腾出解码器给马上要建的新会话.
     *
     * 同一集则什么都不做, 随后的 `viewModel(...)` 会拿回原来那个 VM (这正是"回得去"的实现).
     */
    fun prepare(subjectId: Int, episodeId: Int) {
        val next = RetainedPlaybackSessionInfo(subjectId, episodeId)
        if (session == next) return
        guardJob?.cancel()
        guardJob = null
        // 上一轮退场的会话若还没清 (正常在页面销毁时就清了), 到这里一律清掉, 不留第二份
        clearRetiring()
        val outgoing = current
        if (outgoing != null && composed === outgoing) {
            // 旧播放页还在组合里 (从播放器直接跳另一集时的退场动画), 等它销毁再清
            retiringVm = outgoing
            retiringStore = currentStore
        } else {
            currentStore.clear()
        }
        currentStore = ViewModelStore()
        current = null
        session = next
    }

    /** 播放页拿到 VM 后登记, 本类据此监视它 (见 [guard]). */
    fun attach(vm: EpisodeViewModel) {
        if (current === vm) return
        current = vm
        guardJob?.cancel()
        guardJob = viewModelScope.launch { guard(vm) }
    }

    /** 播放页的组合建立/销毁; 用来判断能不能安全销毁一个会话 (见 [retiringStore]). */
    fun onPageComposed(vm: EpisodeViewModel) {
        composed = vm
    }

    fun onPageDisposed(vm: EpisodeViewModel) {
        if (composed === vm) composed = null
        if (retiringVm === vm) clearRetiring()
    }

    private fun clearRetiring() {
        retiringStore?.clear()
        retiringStore = null
        retiringVm = null
    }

    /** 播放页是否在前台; 由导航状态驱动, 与组合的存活无关. */
    fun setPlayerPageVisible(visible: Boolean) {
        playerPageVisible.value = visible
    }

    override fun close() {
        guardJob?.cancel()
        guardJob = null
        clearRetiring()
        currentStore.clear()
        currentStore = ViewModelStore()
        current = null
        session = null
    }

    override fun onCleared() {
        super.onCleared()
        clearRetiring()
        currentStore.clear()
        current = null
        composed = null
    }

    /**
     * 会话在后台期间仍要挂在组合里的东西. 挂在应用根部 (见 `AniAppContent`).
     *
     * Android 的 WEB 数据源解析器要靠组合挂载才拿得到 WebView 的宿主 Context
     * (`AndroidWebMediaResolver.ComposeContent`), 而原先挂载点在播放页的组合里 —— 退出播放页后
     * 后台还在跑的解析会一路抛 `WebVideoSourceResolver not attached`, 自动换源逐个试完, 最后
     * 界面上是"加载失败: 未知错误". 这里替播放页挂着, 它的挂载是引用计数的, 两处同时挂无妨.
     *
     * 注意用的是**会话自己那个** resolver: Koin 里 `MediaResolver` 是 factory, 每次注入都是新
     * 实例, 在根部另外注入一个挂上去等于挂了个没人用的.
     *
     * [key] 不能省: `ComposeContent` 里那个 `DisposableEffect` 的 key 是常量 (它本来只被播放页
     * 挂载, 而每个播放页都是新的组合, 所以够用). 换会话时本函数的调用点不变、effect 的 key 也不变,
     * effect 就不会重建 —— 于是它还挂着**上一个** resolver, 新会话那个从来没被挂上, 后台解析
     * 继续 "not attached". 按 VM 分组才能让旧的销毁、新的挂载.
     */
    @Composable
    fun ComposeRetainedContent() {
        val vm = current ?: return
        key(vm) {
            vm.mediaResolver.ComposeContent()
        }
    }

    /** 只在不看着播放页时提示; 见 [notices]. */
    private fun notify(notice: RetainedPlaybackNotice) {
        if (!playerPageVisible.value) _notices.tryEmit(notice)
    }

    /**
     * 会话在后台期间的四件事. 全部只读 [vm] 暴露的流, 不碰它的内部状态.
     */
    private suspend fun guard(vm: EpisodeViewModel) = coroutineScope {
        // 1. 替界面当订阅者, 让流水线在没有界面的时候继续跑.
        //    pageState 是 WhileSubscribed(5s) 的, 而"保证数据源会一直查询"的那个 collector 就挂在
        //    它的 scope 里 (见 EpisodeViewModel.createPageStateFlow) —— 没人订阅时数据源搜索会在
        //    5 秒后停下, 那样"退出去等它加载"就不成立了.
        launch { vm.pageState.collect { } }

        // 2. 后台不出声. 只在离开那一刻暂停是不够的: 数据源解析完成后流水线自己会 resume
        //    (PlayerSession.loadMedia 末尾), 于是必须持续按住 —— 这也正是常见的"退出去之后
        //    忽然从后台传出声音".
        //
        //    唯一的例外是"一起看"跟随模式 (`playbackAutomationSuppressed`): 那时候播与不播由房主
        //    说了算, 本地任何自动暂停都是跟房间对着干 —— 房主在播, 房间的持续校正每秒发现本地是
        //    暂停就下发一次同步, 播放器 resume, 这里又按回去, 一秒一轮; 与此同时本地位置不动而
        //    房主在走, 偏差越拉越大, 于是每轮还多一次 seek + "已与房主同步"的提示, 状态翻转还都
        //    是"不连续", 每次都触发一次上报. 播放页在场时的自动暂停 (`AutoPauseEffect`) 本来就用
        //    同一个开关放过跟随模式, 这里跟着它, 前台后台一致.
        launch {
            combine(
                playerPageVisible,
                vm.player.playbackState,
                vm.playbackAutomationSuppressed,
            ) { visible, state, roomControlled -> Triple(visible, state, roomControlled) }
                .collect { (visible, state, roomControlled) ->
                    if (!visible && !roomControlled && state.isPlaying) {
                        // 记下是自动暂停的: 回到播放页由 AutoPauseEffect 自动恢复
                        vm.autoPausedOnLeave = true
                        vm.player.pause()
                    }
                }
        }

        // 3. 后台起播就绪 → 通知外面提示用户. 这是用户等的那一下.
        launch {
            vm.videoStatisticsFlow
                .map { it.videoLoadingState }
                .distinctUntilChanged()
                .filterIsInstance<VideoLoadingState.Succeed>()
                .collectLatest {
                    // Succeed 只是"播放地址交给播放器了"(见 PlayerSession.loadMedia), 之后还有取容器头、
                    // 建解码器、缓冲首帧 —— 实测 1~18 秒, 在线源越慢越久. 按 Succeed 提示的话用户回来
                    // 还得对着黑屏干等 (进度条右侧还是 0:00), 那这声提示就没起到作用. 等真的开播再说.
                    //
                    // 后台一到 PLAYING 就会被第 2 条按成 PAUSED, 但这里是常驻的收集者, 那一次 PLAYING
                    // 收得到; 万一被合并掉或者一直卡在缓冲, 靠超时兜底照样提示 (地址确实有了, 让用户
                    // 自己决定要不要回去等).
                    val state = withTimeoutOrNull(PLAYBACK_START_WAIT) {
                        vm.player.playbackState.first {
                            it == PlaybackState.PLAYING || it == PlaybackState.ERROR
                        }
                    }
                    // 播放器直接报错: 交给第 4 条报"打不开这个源", 别再说一句"已就绪"
                    if (state == PlaybackState.ERROR) return@collectLatest
                    notify(RetainedPlaybackNotice.Ready)
                }
        }

        // 4. 后台出了再等也不会自己好的问题 → 同样提示一声, 否则用户会一直等一个不会来的 Ready.
        launch {
            combine(
                vm.videoStatisticsFlow.map { it.videoLoadingState }.distinctUntilChanged(),
                vm.player.playbackState,
                vm.pageState.map { selectionProblemOf(it) }.distinctUntilChanged(),
            ) { loading, playback, selection -> problemOf(loading, playback, selection) }
                .distinctUntilChanged()
                // 等状态稳定下来再提示: 播放失败常常是一闪而过的中间态 —— SwitchMediaOnPlayerErrorExtension
                // 会在出错约 1 秒后自动换下一个源重试, 每换一次都必然路过 Failed, 逐个弹提示就成了刷屏.
                // 稳定后仍是问题才提示; 万一之后自动换源又成功了, 用户紧接着会收到 Ready, 不会被误导太久.
                .debounce(PROBLEM_SETTLE_DELAY)
                .collect { problem -> problem?.let { notify(it) } }
        }
    }

    private companion object {
        /** 问题状态要持续这么久才提示, 见 [guard] 第 4 条. */
        private val PROBLEM_SETTLE_DELAY = 6.seconds

        /** 解析成功后最多等这么久的"真的开播", 到点仍然提示就绪, 见 [guard] 第 3 条. */
        private val PLAYBACK_START_WAIT = 25.seconds
    }
}

/** 数据源搜索层面的"再等也没用". */
private enum class SelectionProblem {
    None,

    /** 有搜到结果, 但不会自动选 (偏好不是 WEB), 在等用户挑. */
    NeedsManualSelection,

    /** 全部源都查完了, 一个可播的结果都没有. */
    NoMedia,
}

private fun selectionProblemOf(state: EpisodePageState?): SelectionProblem {
    // 页面状态还没算出来 (刚进页面) 或还是占位数据: 什么都判断不了
    if (state == null || state.isPlaceholder) return SelectionProblem.None
    // 已经选中了就不是选择层面的问题 (解析/播放能不能成另说, 那是 problemOf 的前两条)
    if (state.mediaSelectorSummary is MediaSelectorSummary.Selected) return SelectionProblem.None
    val results = state.mediaSourceResultListPresentation
    // 源还没登记上来 (刚进页面) 或还有源在查 —— 等着就行, 这才是这套机制的正常用途
    if (results.list.isEmpty() || results.anyLoading) return SelectionProblem.None
    return if (results.list.any { it.totalCount > 0 }) SelectionProblem.NeedsManualSelection
    else SelectionProblem.NoMedia
}

private fun problemOf(
    loading: VideoLoadingState,
    playback: PlaybackState,
    selection: SelectionProblem,
): RetainedPlaybackNotice? = when {
    // Cancelled 不是问题: 它是"换源"的中间态 (loadMedia 被取消), 紧接着就会重新开始解析
    loading is VideoLoadingState.Failed && loading != VideoLoadingState.Cancelled ->
        RetainedPlaybackNotice.LoadFailed(loading)

    playback == PlaybackState.ERROR -> RetainedPlaybackNotice.PlayerError
    selection == SelectionProblem.NeedsManualSelection -> RetainedPlaybackNotice.NeedsManualSelection
    selection == SelectionProblem.NoMedia -> RetainedPlaybackNotice.NoMediaFound
    else -> null
}
