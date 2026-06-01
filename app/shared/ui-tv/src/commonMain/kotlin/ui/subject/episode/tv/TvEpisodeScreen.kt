/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.subject.episode.tv

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward5
import androidx.compose.material.icons.rounded.Replay5
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import me.him188.ani.app.ui.foundation.LocalImageLoader
import me.him188.ani.app.ui.subject.details.sections.episodeStillImageRequest
import me.him188.ani.app.ui.subject.details.SubjectDetailsUIState
import me.him188.ani.app.data.models.preference.DarkMode
import me.him188.ani.app.domain.player.VideoLoadingState
import me.him188.ani.app.ui.danmaku.DanmakuEditorState
import me.him188.ani.app.ui.danmaku.PlayerDanmakuHost
import me.him188.ani.app.ui.foundation.LocalImageViewerHandler
import me.him188.ani.app.ui.foundation.animation.AniAnimatedVisibility
import me.him188.ani.app.ui.foundation.theme.AniTheme
import me.him188.ani.app.ui.foundation.focus.resolveFocusRepeatedly
import me.him188.ani.app.ui.subject.episode.EpisodePageState
import me.him188.ani.app.ui.subject.episode.EpisodeViewModel
import me.him188.ani.app.ui.subject.episode.video.components.EpisodeVideoSideSheetPage
import me.him188.ani.app.ui.subject.episode.video.loading.EpisodeVideoLoadingIndicator
import me.him188.ani.app.videoplayer.ui.PlayerStatsOverlay
import me.him188.ani.app.videoplayer.ui.VideoPlayer
import me.him188.ani.app.videoplayer.ui.hasPageAsState
import me.him188.ani.app.videoplayer.ui.progress.PlayerControllerDefaults
import me.him188.ani.app.videoplayer.ui.progress.rememberMediaProgressSliderState
import me.him188.ani.app.videoplayer.ui.rememberPlayerStatsState
import me.him188.ani.app.videoplayer.ui.rememberVideoSideSheetsController
import me.him188.ani.danmaku.ui.DanmakuHostState
import org.openani.mediamp.MediampPlayer
import org.openani.mediamp.PlaybackState
import org.openani.mediamp.isPlaying
import org.openani.mediamp.togglePause

/** 遥控器左右键单次快进/快退步长. */
internal const val TV_PLAYER_SEEK_STEP_MILLIS = 5_000L

/** 控制层自动隐藏延时 (播放中且无面板/弹层/输入时, Prime 行为). */
internal const val TV_PLAYER_AUTO_HIDE_MILLIS = 5_000L

/** 剧照预取的起始延迟: 让首帧起播先用完带宽. */
private const val TV_STILL_PREFETCH_DELAY_MILLIS = 2_000L

/** 等待 TMDB 剧照索引到达的上限: 无图条目 (未匹配到 TMDB) 永远等不到, 到点放弃. */
private const val TV_STILL_PREFETCH_WAIT_MILLIS = 30_000L

/** 预取当前集之后的集数 (往后是主要浏览方向; 选集条一屏 4 张, 多备几张够翻一屏). */
private const val TV_STILL_PREFETCH_AHEAD = 6

/** 详情层淡入时长 (毫秒). */
private const val TV_DETAILS_FADE_IN_MS = 300

/** 详情层淡出时长 (毫秒): 放慢一档, 瞬时/快速移除观感像闪切. */
private const val TV_DETAILS_FADE_OUT_MS = 500

/**
 * TV 播放器界面 (Prime Video 风格):
 *
 * - 纯视频态 (HIDDEN): 只有画面和弹幕. 确认/暂停键切换播放并唤出控制层, 上下键仅唤出,
 *   左右键快进退但**不唤出** (中央浮现快进退图标作反馈).
 * - 控制层 (CONTROLS): 顶部标题/时钟, 底部 [胶囊按钮行 + 进度条 + 图标行];
 *   聚焦胶囊按钮时其上方浮出对应面板 (弹幕列表/相关推荐/本集评论), 面板条目吸附底部,
 *   从下往上导航.
 * - 详情页 (DETAILS): 图标行按下键唤出, 隐藏全部播放器组件, 视频画面作为详情页背景.
 *
 * 所有按键语义集中在本文件的唯一路由 (根 onPreviewKeyEvent), 层级切换集中在
 * [TvPlayerOverlayState]; 各行容器只通过 onFocusChanged 上报焦点区域, 不各自处理按键.
 */
@Composable
fun TvEpisodeScreenContent(
    vm: EpisodeViewModel,
    page: EpisodePageState,
    danmakuHostState: DanmakuHostState,
    danmakuEditorState: DanmakuEditorState,
    setShowEditCommentSheet: (Boolean) -> Unit,
    pauseOnPlaying: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val overlay = remember { TvPlayerOverlayState() }
    val seekFlash = remember { TvSeekFlashState() }
    val sheetsController = rememberVideoSideSheetsController<EpisodeVideoSideSheetPage>()
    val anySheetVisible by sheetsController.hasPageAsState()
    val imageViewer = LocalImageViewerHandler.current

    SideEffect { vm.onUIReady() }

    // 预载条目详情 (TMDB 剧照/时长/简介, 详情层内容): 详情层与选集条的增量信息共用同一 loader
    // (有"已加载"守卫, 不会重复请求). 分集列表本身不等它, 见 EpisodeViewModel.episodeListUiStateFlow.
    LaunchedEffect(Unit) {
        val detailsState = vm.episodeDetailsState
        detailsState.subjectDetailsStateLoader.load(detailsState.subjectId, detailsState.subjectInfo.value)
    }

    // 预热 presentation: 它是 WhileSubscribed(5000) 的惰性流, 而读它的详情层只在被唤出时才组合 ——
    // 在那之前没有任何收集者, 上游根本没启动. 实测控制层隔 6.4 秒才唤出的那次, loader 早在
    // +0.8s 就 Ok 了, presentation 却一直等到 +6.49s 才脱离占位, 白等 5.7 秒.
    // 挂一个空收集者让它跟起播一起预热: 数据仍由各处 UI 自己读, 这里只负责把上游拉起来.
    LaunchedEffect(Unit) {
        vm.episodeDetailsState.subjectDetailsStateLoader.state
            .filterIsInstance<SubjectDetailsUIState.Ok>().first()
            .value.presentation.collect { }
    }

    // 预取选集条卡片的剧照.
    //
    // 卡片行只在展开态组合 (见 TvPlayerEpisodeStrip), 不预取的话按下键那一刻才发请求, 首屏是
    // 一排空卡; 反向也一样 —— 从播放器退回详情页往下翻, 图同样是冷的. 两处的请求由
    // episodeStillImageRequest 钉成同一条缓存, 所以这里预取的位图两边都能直接命中.
    //
    // 放在进屏 (而不是控制层出现时): 用户可能一直看片、从没展开过选集条就退出去了, 那时按
    // 控制层触发就还是冷的. 延迟一下避开首帧起播的带宽争抢 —— 一屏几张 w780 约几十 KB 一张,
    // 对能流视频的连接微不足道, 但没必要跟起播抢.
    val imageLoader = LocalImageLoader.current
    val platformContext = LocalPlatformContext.current
    LaunchedEffect(Unit) {
        delay(TV_STILL_PREFETCH_DELAY_MILLIS)
        val detailsState = vm.episodeDetailsState
        val ok = detailsState.subjectDetailsStateLoader.state
            .filterIsInstance<SubjectDetailsUIState.Ok>().first().value
        // 无图条目 (TMDB 未匹配到) 会一直等不到非空, 由外层超时收场
        val stills = withTimeoutOrNull(TV_STILL_PREFETCH_WAIT_MILLIS) {
            ok.tmdbEpisodeStillsFlow.first { it.isNotEmpty() }
        } ?: return@LaunchedEffect
        // 分集列表与选集条同源 (播放器自己那条): 详情状态的 presentation 此刻可能还是占位值,
        // 读它会拿到空列表, 于是一张都不预取
        val episodes = vm.episodeListUiStateFlow.filterNotNull().first().mainEpisodes
        val current = episodes.indexOfFirst { it.episodeId == vm.episodeSelectorState.current?.episodeId }
            .coerceAtLeast(0)
        // 以当前集为中心的一小段: 选集条初始滚到当前集, 往后是主要浏览方向, 往前留一张
        val urls = ((current - 1).coerceAtLeast(0)..(current + TV_STILL_PREFETCH_AHEAD))
            .mapNotNull { i -> stills[episodes.getOrNull(i)?.episodeId ?: return@mapNotNull null] }
        for (url in urls) {
            // 选集条一露面就停手: 卡片自己会请求同一批图, 而 Coil **不合并**并发的同 key 请求 ——
            // 不停手就是同一张图同时下两遍, 抢的还是同一份带宽 (实测 12 个并发 fetch 时单张
            // 40KB 的图要 700~900ms, 比不预取还慢). 已下完的留在缓存里, 卡片直接命中.
            if (overlay.episodeStripExpanded) break
            // 串行而非并发 enqueue: 预取是背景工作, 一次占一个连接就够, 不跟正在播的视频抢
            imageLoader.execute(episodeStillImageRequest(platformContext, url))
        }
    }

    val progressSliderState = rememberMediaProgressSliderState(
        vm.player,
        vm.progressChaptersFlow,
        onPreview = {},
        onPreviewFinished = { vm.player.seekTo(it) },
    )

    val rootFocusRequester = remember { FocusRequester() }
    val progressRowFocusRequester = remember { FocusRequester() }
    val bottomRowFocusRequester = remember { FocusRequester() }
    val episodeStripFocusRequester = remember { FocusRequester() }
    var rootFocused by remember { mutableStateOf(false) }
    // 同一次物理按下已经换过一层: 按住下键时遥控器连发 KeyDown (约 50ms 一次), 而下键在控制层里
    // 每一档都换一层 (图标行 -> 选集条 -> 详情层), 连发会一路跳到底 —— 观感是选集条刚滑出来就
    // 闪进了详情页. 松手 (KeyUp) 才解锁. 只锁"换层"的那几档, 面板内按住下键滚列表不受影响
    var downKeyLatched by remember { mutableStateOf(false) }

    // ---- 唯一按键路由: 所有 Back 语义与层级切换都在这里, 状态读取只发生在事件回调内 ----
    val onRootKeyEvent: (KeyEvent) -> Boolean = router@{ event ->
        val key = event.key
        val isKeyDown = event.type == KeyEventType.KeyDown
        val isKeyUp = event.type == KeyEventType.KeyUp
        val isBack = key == Key.Back || key == Key.Escape

        if (key == Key.DirectionDown) {
            if (isKeyUp) {
                downKeyLatched = false
            } else if (isKeyDown && downKeyLatched) {
                return@router true
            }
        }

        // 图片查看器 (详情页评论区打开的大图) 优先: 返回关闭
        if (imageViewer.viewing.value) {
            if (isBack) {
                if (isKeyUp) imageViewer.clear()
                return@router true
            }
            return@router false
        }
        // 弹幕输入态: 只拦返回收起, 其余全部交给输入框/IME
        if (overlay.danmakuInputExpanded) {
            if (isBack) {
                if (isKeyUp) overlay.danmakuInputExpanded = false
                return@router true
            }
            overlay.markInteraction()
            return@router false
        }
        // 侧边 sheet (数据源/选集/弹幕设置) 打开: 返回关闭, 其余交给 sheet 内部导航
        if (anySheetVisible) {
            overlay.markInteraction()
            if (isBack) {
                if (isKeyUp) sheetsController.close()
                return@router true
            }
            return@router false
        }

        when (overlay.layer) {
            TvPlayerLayer.HIDDEN -> {
                if (!isKeyDown) return@router false
                when (key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter,
                    Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause,
                        -> {
                        // 暂停态下恢复播放不唤出控制层 (画面动起来即反馈); 播放态下暂停仍唤出
                        val resuming = vm.player.playbackState.value == PlaybackState.PAUSED
                        vm.player.togglePause()
                        if (!resuming) overlay.showControls()
                        true
                    }

                    Key.DirectionUp, Key.DirectionDown -> {
                        overlay.showControls()
                        true
                    }

                    // 快进退不唤出控制层: 调时间轴不该把画面下半压掉一半再等 5 秒自动隐藏.
                    // 反馈改走中央快进退图标 (与暂停反馈同款), 见 [TvSeekFlash]
                    Key.DirectionLeft -> {
                        vm.player.skip(-TV_PLAYER_SEEK_STEP_MILLIS)
                        seekFlash.flash(forward = false)
                        true
                    }

                    Key.DirectionRight -> {
                        vm.player.skip(TV_PLAYER_SEEK_STEP_MILLIS)
                        seekFlash.flash(forward = true)
                        true
                    }

                    Key.MediaFastForward -> {
                        if (vm.episodeSelectorState.hasNextEpisode) vm.episodeSelectorState.selectNext()
                        true
                    }

                    Key.MediaRewind -> {
                        if (vm.episodeSelectorState.hasPrevEpisode) vm.episodeSelectorState.selectPrev()
                        true
                    }

                    // Back 不消费: 交给系统返回, 退出播放器
                    else -> false
                }
            }

            TvPlayerLayer.CONTROLS -> {
                overlay.markInteraction()
                if (isBack) {
                    if (isKeyUp) {
                        // 面板条目上: 返回回进度条 (面板随焦点区域变化收起); 其余: 全部隐藏
                        if (overlay.focusRegion == TvPlayerFocusRegion.PANEL) {
                            overlay.focusProgress()
                        } else {
                            overlay.hideAll()
                        }
                    }
                    return@router true
                }
                if (!isKeyDown) return@router false
                when (key) {
                    // 图标行再往下: 展开选集条 (Prime 形态, 焦点落当前集卡片);
                    // 确认无分集 (未开播/加载失败) 才直通详情页, 数据未到则等就绪后自动展开.
                    // 选集条内再往下: 详情页 (第三层)
                    Key.DirectionDown -> when (overlay.focusRegion) {
                        TvPlayerFocusRegion.BOTTOM_ROW -> {
                            downKeyLatched = true
                            when (overlay.episodeStrip) {
                                TvEpisodeStripState.AVAILABLE -> overlay.expandEpisodeStrip()
                                // 还在加载: 记下意图等就绪 (跳详情页是"确认无分集"才该做的)
                                TvEpisodeStripState.LOADING -> overlay.expandEpisodeStripWhenReady()
                                TvEpisodeStripState.EMPTY -> overlay.openDetails()
                            }
                            true
                        }

                        TvPlayerFocusRegion.EPISODES -> {
                            downKeyLatched = true
                            overlay.openDetails()
                            true
                        }

                        else -> false // 其余交给空间焦点导航
                    }

                    // 选集条内按上键: 收起选集条, 控制行回来, 焦点还给图标行
                    Key.DirectionUp ->
                        if (overlay.focusRegion == TvPlayerFocusRegion.EPISODES) {
                            overlay.collapseEpisodeStrip()
                            true
                        } else {
                            false
                        }

                    // 进度条行: 左右快进退, 确认切换播放
                    Key.DirectionLeft ->
                        if (overlay.focusRegion == TvPlayerFocusRegion.PROGRESS) {
                            vm.player.skip(-TV_PLAYER_SEEK_STEP_MILLIS)
                            true
                        } else {
                            false
                        }

                    Key.DirectionRight ->
                        if (overlay.focusRegion == TvPlayerFocusRegion.PROGRESS) {
                            vm.player.skip(TV_PLAYER_SEEK_STEP_MILLIS)
                            true
                        } else {
                            false
                        }

                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter ->
                        if (overlay.focusRegion == TvPlayerFocusRegion.PROGRESS) {
                            vm.player.togglePause()
                            true
                        } else {
                            false
                        }

                    Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                        vm.player.togglePause()
                        true
                    }

                    Key.MediaFastForward -> {
                        if (vm.episodeSelectorState.hasNextEpisode) vm.episodeSelectorState.selectNext()
                        true
                    }

                    Key.MediaRewind -> {
                        if (vm.episodeSelectorState.hasPrevEpisode) vm.episodeSelectorState.selectPrev()
                        true
                    }

                    else -> false
                }
            }

            TvPlayerLayer.DETAILS -> when {
                // 详情页内按返回: 隐藏整个覆盖层回纯视频 (方案约定, 不走详情页内部返回分层)
                isBack -> {
                    if (isKeyUp) overlay.hideAll()
                    true
                }

                key == Key.MediaPlayPause || key == Key.MediaPlay || key == Key.MediaPause -> {
                    if (isKeyDown) vm.player.togglePause()
                    true
                }

                else -> false
            }
        }
    }

    // ---- 焦点落点解析 (到位确认 + 重试, 不裸 requestFocus) ----
    // 单一解析器消化 overlay.pendingFocus (PANEL 除外, 由面板宿主消化): collectLatest
    // 保证新请求一到旧解析立即取消 —— 过去四个目标各挂一个循环, 快速交替 (选集条
    // 展开→收起→展开) 时新旧循环并发 requestFocus 互抢焦点
    LaunchedEffect(Unit) {
        snapshotFlow { overlay.pendingFocus }.collectLatest { (target, _) ->
            val (expectedLayer, requester) = when (target) {
                TvPlayerFocusTarget.ROOT -> TvPlayerLayer.HIDDEN to rootFocusRequester
                TvPlayerFocusTarget.PROGRESS -> TvPlayerLayer.CONTROLS to progressRowFocusRequester
                TvPlayerFocusTarget.EPISODE_STRIP -> TvPlayerLayer.CONTROLS to episodeStripFocusRequester
                TvPlayerFocusTarget.BOTTOM_ROW -> TvPlayerLayer.CONTROLS to bottomRowFocusRequester
                TvPlayerFocusTarget.PANEL -> return@collectLatest
            }
            resolveFocusRepeatedly(
                arrived = {
                    // 层已切走 = 放弃解析 (新层的落点由后续请求负责)
                    overlay.layer != expectedLayer || when (target) {
                        TvPlayerFocusTarget.ROOT -> rootFocused
                        TvPlayerFocusTarget.PROGRESS -> overlay.focusRegion == TvPlayerFocusRegion.PROGRESS
                        TvPlayerFocusTarget.EPISODE_STRIP -> overlay.focusRegion == TvPlayerFocusRegion.EPISODES
                        TvPlayerFocusTarget.BOTTOM_ROW -> overlay.focusRegion == TvPlayerFocusRegion.BOTTOM_ROW
                        TvPlayerFocusTarget.PANEL -> true
                    }
                },
            ) {
                if (overlay.layer == expectedLayer) {
                    runCatching { requester.requestFocus() }
                }
            }
        }
    }
    // 焦点移到进度条/图标行时收起浮出面板 (聚焦交接的瞬时 NONE 不清除)
    LaunchedEffect(Unit) {
        snapshotFlow { overlay.focusRegion }.collectLatest { region ->
            if (region == TvPlayerFocusRegion.PROGRESS || region == TvPlayerFocusRegion.BOTTOM_ROW) {
                overlay.activePanel = null
            }
        }
    }
    // 自动隐藏 (Prime 行为): 播放中且无面板/侧 sheet/下拉/输入态, 5 秒无按键收起.
    // 用 snapshotFlow 而非 LaunchedEffect key, 避免每次按键使本组合作用域失效
    LaunchedEffect(Unit) {
        snapshotFlow {
            listOf(
                overlay.layer, overlay.interactionTick, overlay.activePanel,
                overlay.openPopupCount, overlay.danmakuInputExpanded, anySheetVisible,
            )
        }.collectLatest {
            if (overlay.layer != TvPlayerLayer.CONTROLS) return@collectLatest
            if (overlay.activePanel != null || overlay.openPopupCount > 0 ||
                overlay.danmakuInputExpanded || anySheetVisible
            ) {
                return@collectLatest
            }
            delay(TV_PLAYER_AUTO_HIDE_MILLIS)
            // 暂停时不自动隐藏 (Prime 行为); 到点时再查一次播放状态
            if (vm.player.playbackState.value.isPlaying) {
                overlay.hideAll()
            }
        }
    }

    AniTheme(darkModeOverride = DarkMode.DARK) {
        Box(
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .onPreviewKeyEvent(onRootKeyEvent)
                .focusRequester(rootFocusRequester)
                .onFocusChanged { rootFocused = it.isFocused }
                .focusable(), // 根节点可聚焦: 纯视频态持焦收按键
        ) {
            // 视频面: 独立稳定槽位, 覆盖层任何变化不触碰
            VideoPlayer(
                vm.player,
                Modifier.matchParentSize(),
            )

            // 弹幕层
            AniAnimatedVisibility(page.danmakuEnabled, Modifier.matchParentSize()) {
                Box(Modifier.matchParentSize()) {
                    PlayerDanmakuHost(vm.player, danmakuHostState, vm.uiDanmakuEventFlow)
                }
            }

            // 缓冲/加载指示 (居中悬浮).
            //
            // 快进退反馈也在画面正中, 而快进必然引发一次重新缓冲 —— 不让路的话每次按左右键
            // 圆弧箭头都和"正在缓冲"叠在一起. 快进反馈优先: 它在场期间把缓冲指示按成隐形,
            // 走完 (约 0.6 秒) 之后若还在缓冲自然露出来.
            //
            // 隐形而不是从组合里摘掉: TvPlayerLoadingLayer 的 collectAsStateWithLifecycle
            // 初值是 VideoLoadingState.Initial, 重新挂载会有一帧非 Succeed 状态, 闪一下
            // "正在自动选择"; 里面"缓冲太久"的 15 秒计时也会跟着重置.
            //
            // 状态读在 graphicsLayer 的 lambda 里: 直接读会让整个播放器界面随反馈的出现/消失重组
            TvPlayerLoadingLayer(
                vm,
                Modifier
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = if (seekFlash.visible) 0f else 1f },
            )

            // 播放/暂停切换反馈: 画面中央浮现对应图标并渐隐 (监听播放器状态流,
            // 无论切换来自确认键/控制按钮/面板操作都有反馈)
            TvPauseFlash(vm.player, Modifier.align(Alignment.Center))

            // 快进退反馈: 纯视频态左右键不唤出控制层, 这是唯一的反馈
            TvSeekFlash(seekFlash, Modifier.align(Alignment.Center))

            // 跳过 OP/ED 提示 (左下角, 独立于控制层常显)
            Box(Modifier.align(Alignment.BottomStart).padding(start = 48.dp, bottom = 140.dp)) {
                AniAnimatedVisibility(visible = vm.playerSkipOpEdState.showSkipTips) {
                    PlayerControllerDefaults.LeftBottomTips(
                        onClick = { vm.playerSkipOpEdState.cancelSkipOpEd() },
                    )
                }
            }

            // 控制层 (L1 + 浮出面板 L2)
            AniAnimatedVisibility(
                visible = overlay.layer == TvPlayerLayer.CONTROLS,
                modifier = Modifier.matchParentSize(),
            ) {
                TvPlayerControlsOverlay(
                    overlay = overlay,
                    vm = vm,
                    page = page,
                    danmakuEditorState = danmakuEditorState,
                    progressSliderState = progressSliderState,
                    progressRowFocusRequester = progressRowFocusRequester,
                    bottomRowFocusRequester = bottomRowFocusRequester,
                    episodeStripFocusRequester = episodeStripFocusRequester,
                    sheetsController = sheetsController,
                    setShowEditCommentSheet = setShowEditCommentSheet,
                    pauseOnPlaying = pauseOnPlaying,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // 播放器统计悬浮层 (三个点菜单开关)
            if (overlay.showPlayerStats) {
                val playerStats by rememberPlayerStatsState(vm.player)
                PlayerStatsOverlay(playerStats)
            }

            // 详情页覆盖层 (L3): 视频画面作背景. 淡入淡出 —— 尤其顶部按上键回选集条时,
            // 瞬时移除会闪一下; 淡出放慢一档 (默认过渡太快, 观感仍像闪切)
            AniAnimatedVisibility(
                visible = overlay.layer == TvPlayerLayer.DETAILS,
                modifier = Modifier.matchParentSize(),
                enter = fadeIn(tween(TV_DETAILS_FADE_IN_MS)),
                exit = fadeOut(tween(TV_DETAILS_FADE_OUT_MS)),
            ) {
                TvPlayerDetailsOverlay(
                    vm = vm,
                    page = page,
                    onClose = { overlay.hideAll() },
                    onExitUpToStrip = { overlay.returnToEpisodeStrip() },
                    modifier = Modifier.matchParentSize(),
                )
            }

            // 右侧侧边 sheets (数据源/选集/弹幕设置), 复用现有实现
            Box(Modifier.matchParentSize()) {
                TvPlayerSideSheets(vm, sheetsController)
            }
        }
    }
}

/** 缓冲/加载指示: 状态收集限制在本组合内, 不牵连整屏. */
@Composable
private fun TvPlayerLoadingLayer(
    vm: EpisodeViewModel,
    modifier: Modifier = Modifier,
) {
    val videoLoadingStateFlow = remember(vm) { vm.videoStatisticsFlow.map { it.videoLoadingState } }
    val videoLoadingState by videoLoadingStateFlow.collectAsStateWithLifecycle(VideoLoadingState.Initial)
    Box(modifier) {
        EpisodeVideoLoadingIndicator(
            vm.player,
            videoLoadingState,
            optimizeForFullscreen = true,
        )
    }
}

/**
 * 中央反馈的浮现渐隐容器 (暂停/快进退共用): [content] 画两遍 —— 先黑色偏移一档作投影,
 * 再白色本体; 无底衬, 亮画面上靠投影保证可见.
 *
 * [flashKey] 每次自增都重启动画: 快速连按时从满透明度重新开始, 而不是接着上一次淡下去.
 */
@Composable
private fun TvCenterFlash(
    flashKey: Int,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (color: Color, modifier: Modifier) -> Unit,
) {
    key(flashKey) {
        val alpha = remember { Animatable(1f) }
        LaunchedEffect(Unit) {
            alpha.animateTo(
                0f,
                tween(TV_PAUSE_FLASH_DURATION_MS, delayMillis = TV_PAUSE_FLASH_HOLD_MS),
            )
            onFinished()
        }
        Box(modifier.graphicsLayer { this.alpha = alpha.value }) {
            content(Color.Black.copy(alpha = TV_PAUSE_FLASH_SHADOW_ALPHA), Modifier.offset(x = 1.dp, y = 1.5.dp))
            content(Color.White, Modifier)
        }
    }
}

/**
 * 暂停反馈: 每次 播放->暂停 切换, 中央浮现暂停图标后渐隐. 恢复播放无反馈
 * (画面动起来本身即反馈).
 *
 * 直接监听播放器状态流而非按键: 确认键/图标行按钮/自动暂停等任何触发方式都有反馈.
 * 缓冲等中间态不算切换 (播放中卡缓冲再恢复不闪); 进屏的首个状态不闪.
 */
@Composable
private fun TvPauseFlash(
    player: MediampPlayer,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    var flashKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(player) {
        var prev: Boolean? = null
        player.playbackState.collect { state ->
            val playing = when (state) {
                PlaybackState.PLAYING -> true
                PlaybackState.PAUSED -> false
                else -> return@collect
            }
            if (prev == true && !playing) {
                visible = true
                flashKey++
            }
            prev = playing
        }
    }
    if (visible) {
        TvCenterFlash(flashKey, onFinished = { visible = false }, modifier) { color, mod ->
            TvPauseBars(color, mod)
        }
    }
}

/**
 * 遥控器左右键快进退的中央反馈状态.
 *
 * 纯视频态下左右键**不唤出控制层** (快进退不该把画面压掉一半), 于是反馈只剩这一个居中图标,
 * 必须由按键路由显式触发 —— 快进退不改变播放器状态流, 没法像 [TvPauseFlash] 那样自己监听.
 */
@Stable
private class TvSeekFlashState {
    /** 每次按键自增, 用作 [TvCenterFlash] 的重启键. */
    var tick: Int by mutableIntStateOf(0)
        private set

    var forward: Boolean by mutableStateOf(true)
        private set

    var visible: Boolean by mutableStateOf(false)
        private set

    fun flash(forward: Boolean) {
        this.forward = forward
        visible = true
        tick++
    }

    fun onFinished() {
        visible = false
    }
}

/**
 * 快进退反馈: 中央浮现"圆弧箭头 + 秒数"后渐隐, 与 [TvPauseFlash] 同款 (同投影/同时长).
 *
 * 图形与图标行的"跳过 OP/ED" (AniIcons.Forward85 等) 同族, 只是秒数不同 —— 图标里的 5
 * 对应 [TV_PLAYER_SEEK_STEP_MILLIS], 改步长时记得一起换 (Material 现成的只有 5/10/30 三档).
 */
@Composable
private fun TvSeekFlash(
    state: TvSeekFlashState,
    modifier: Modifier = Modifier,
) {
    if (!state.visible) return
    TvCenterFlash(state.tick, onFinished = { state.onFinished() }, modifier) { color, mod ->
        Icon(
            if (state.forward) Icons.Rounded.Forward5 else Icons.Rounded.Replay5,
            null,
            mod.size(TV_SEEK_FLASH_ICON_SIZE),
            tint = color,
        )
    }
}

/** 双竖杠暂停图形: Material Pause 图标太矮胖, 对照 Prime 实测 (4K/640dpi 下 18x112px, 胶囊端头) 自绘. */
@Composable
private fun TvPauseBars(color: Color, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(TV_PAUSE_FLASH_BAR_GAP)) {
        repeat(2) {
            Box(
                Modifier
                    .size(TV_PAUSE_FLASH_BAR_WIDTH, TV_PAUSE_FLASH_BAR_HEIGHT)
                    .background(color, CircleShape),
            )
        }
    }
}

/** 暂停反馈双竖杠尺寸 (Prime 实测换算). */
private val TV_PAUSE_FLASH_BAR_WIDTH = 4.5.dp
private val TV_PAUSE_FLASH_BAR_HEIGHT = 28.dp
private val TV_PAUSE_FLASH_BAR_GAP = 7.5.dp

/**
 * 快进退图标尺寸: 与暂停竖杠的**外接圆**对齐, 两个提示在画面中央占一样大的一团.
 *
 * 竖杠组包围盒 16.5 x 28dp (两杠分列两角), 外接圆直径即对角线 32.5dp; 圆弧箭头是圆形图形,
 * 占满 Material 24dp 网格里 20dp 的活动区, 外接圆直径 = 尺寸 x 20/24. 于是 40dp 给出
 * 33.3dp, 与 32.5dp 差 3% —— 不能按"图形高度"凑 (那样得 34dp, 外接圆就小了两成).
 */
private val TV_SEEK_FLASH_ICON_SIZE = 40.dp

/** 暂停反馈的渐隐时长与起始停留 (毫秒). */
private const val TV_PAUSE_FLASH_DURATION_MS = 500
private const val TV_PAUSE_FLASH_HOLD_MS = 120

/** 暂停反馈投影不透明度. */
private const val TV_PAUSE_FLASH_SHADOW_ALPHA = 0.55f
