/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.android.tv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImagePainter
import com.kmpalette.palette.graphics.Palette
import me.him188.ani.app.data.models.subject.SubjectInfo
import me.him188.ani.app.data.models.subject.Tag
import me.him188.ani.app.domain.episode.SetEpisodeCollectionTypeRequest
import me.him188.ani.app.navigation.AniNavigator
import me.him188.ani.app.navigation.MainScreenPage
import me.him188.ani.app.navigation.NavRoutes
import me.him188.ani.app.platform.AppTerminator
import me.him188.ani.app.ui.foundation.LocalAniUiBehavior
import me.him188.ani.app.ui.foundation.LocalTvBackLongPressHost
import me.him188.ani.app.ui.foundation.LocalTvPageRefreshHost
import me.him188.ani.app.ui.foundation.LocalTvPlayLongPressHost
import me.him188.ani.app.ui.foundation.TV_PLAY_KEYS
import me.him188.ani.app.ui.foundation.TvBackLongPressHandler
import me.him188.ani.app.ui.foundation.TvBackLongPressHost
import me.him188.ani.app.ui.foundation.TvKeyLongPressHandler
import me.him188.ani.app.ui.foundation.TvKeyLongPressHost
import me.him188.ani.app.ui.foundation.TvPageRefreshHost
import me.him188.ani.app.ui.foundation.playback.PlaybackSessionEntry
import me.him188.ani.app.ui.foundation.theme.LocalThemeSettings
import me.him188.ani.app.ui.foundation.tvKeyLongPressInterceptor
import me.him188.ani.app.ui.foundation.watchtogether.WatchTogetherEntryState
import me.him188.ani.app.ui.main.TvQuickActionMenu
import me.him188.ani.app.ui.subject.episode.RetainedPlaybackSessionHolder
import me.him188.ani.app.ui.exploration.ExplorationPageVariant
import me.him188.ani.app.ui.exploration.LocalExplorationPageVariant
import me.him188.ani.app.ui.exploration.TvExplorationPage
import me.him188.ani.app.ui.exploration.schedule.LocalSchedulePageVariant
import me.him188.ani.app.ui.exploration.schedule.SchedulePageVariant
import me.him188.ani.app.ui.exploration.schedule.TvSchedulePage
import me.him188.ani.app.ui.exploration.search.LocalSearchPageVariant
import me.him188.ani.app.ui.exploration.search.SearchPageVariant
import me.him188.ani.app.ui.exploration.search.TvSearchPage
import me.him188.ani.app.ui.main.LocalMainScreenShellVariant
import me.him188.ani.app.ui.main.MainScreenShellVariant
import me.him188.ani.app.ui.main.TvMainScreenLayout
import me.him188.ani.app.ui.subject.collection.CollectionPageVariant
import me.him188.ani.app.ui.subject.collection.LocalCollectionPageVariant
import me.him188.ani.app.ui.subject.collection.TvCollectionPage
import me.him188.ani.app.ui.subject.details.LocalSubjectDetailsPageVariant
import me.him188.ani.app.ui.subject.details.SubjectDetailsPageVariant
import me.him188.ani.app.ui.subject.details.layout.SubjectDetailsLayoutParams
import me.him188.ani.app.ui.subject.details.layout.SubjectDetailsTvLoadingPlaceholder
import me.him188.ani.app.ui.subject.details.layout.SubjectDetailsTvPage
import me.him188.ani.app.ui.subject.details.state.SubjectDetailsState
import me.him188.ani.app.ui.subject.episode.EpisodeScreenVariant
import me.him188.ani.app.ui.subject.episode.LocalEpisodeScreenVariant
import me.him188.ani.app.ui.subject.episode.tv.TvEpisodeScreenContent
import me.him188.ani.app.ui.user.SelfInfoUiState
import org.koin.mp.KoinPlatform

/**
 * TV 页面变体装配: 把遥控器形态的页面实现注入各共享页面的变体插槽.
 *
 * 共享代码只认识插槽 (`Local*Variant`), 不认识 TV; 是否安装变体由应用入口决定
 * (见 MainActivity 的 UI mode 判断).
 */
/** [InstallTvPageVariants] 的条件版: 非 TV 直接组合 [content], 零影响. */
@Composable
fun MaybeInstallTvPageVariants(isTv: Boolean, aniNavigator: AniNavigator, content: @Composable () -> Unit) {
    if (isTv) InstallTvPageVariants(aniNavigator, content) else content()
}

@Composable
fun InstallTvPageVariants(aniNavigator: AniNavigator, content: @Composable () -> Unit) {
    // 遥控器全局长按手势 (机制与分层见 TvKeyLongPressHost 的 KDoc): 每个键集一份跟踪器,
    // 挂在下方根 Box 上; "长按之后干什么"由在场的界面注册 (播放器收叠层在栈顶, 这里只有兜底)
    val backLongPress = remember { TvBackLongPressHost() }
    val playLongPress = remember { TvKeyLongPressHost(TV_PLAY_KEYS) }
    // 各页把自己的强制刷新动作注册进来, 给快捷菜单的「刷新本页」用
    val pageRefresh = remember { TvPageRefreshHost() }
    CompositionLocalProvider(
        LocalTvBackLongPressHost provides backLongPress,
        // 下发播放键宿主只为让独立窗口的桥接够得着 (处理器仍只有下面那一个)
        LocalTvPlayLongPressHost provides playLongPress,
        LocalTvPageRefreshHost provides pageRefresh,
        LocalMainScreenShellVariant provides MainScreenShellVariant {
                page, selfInfo, navigator, onNavigateToPage, onNavigateToSettings,
                onNavigateToSearch, onLogout, modifier, pageContent,
            ->
            // 退出确认弹窗的「确定」= 真退出: AppTerminator 会先收掉 torrent 服务再退进程
            // (Android 上光 finish Activity 的话 :torrent_service 进程还挂着)
            val context = LocalContext.current
            val appTerminator = remember { KoinPlatform.getKoin().get<AppTerminator>() }
            TvMainScreenLayout(
                page, selfInfo, navigator, onNavigateToPage, onNavigateToSettings,
                onNavigateToSearch, onLogout,
                onExitApp = { appTerminator.exitApp(context, 0) },
                modifier = modifier, pageContent = pageContent,
            )
        },
        LocalEpisodeScreenVariant provides EpisodeScreenVariant {
                vm, page, danmakuHostState, danmakuEditorState,
                setShowEditCommentSheet, pauseOnPlaying, modifier,
            ->
            TvEpisodeScreenContent(
                vm, page, danmakuHostState, danmakuEditorState,
                setShowEditCommentSheet, pauseOnPlaying, modifier,
            )
        },
        LocalExplorationPageVariant provides ExplorationPageVariant { state, modifier ->
            TvExplorationPage(state, modifier)
        },
        LocalSchedulePageVariant provides SchedulePageVariant { presentation, onRetry, modifier ->
            TvSchedulePage(presentation, onRetry, modifier)
        },
        LocalSearchPageVariant provides SearchPageVariant { state, onIntent, suggestionsPager, modifier ->
            TvSearchPage(state, onIntent, suggestionsPager, modifier)
        },
        LocalCollectionPageVariant provides CollectionPageVariant { state, modifier ->
            TvCollectionPage(state, modifier)
        },
        // 这个变体有两个方法 (页面 + 首屏占位), 不能用 SAM lambda 写法
        LocalSubjectDetailsPageVariant provides TvSubjectDetailsPageVariant,
    ) {
        // 长按手势兜不兜、菜单开不开, 都要先看当前在哪个目的地:
        //  - 播放页: 长按返回归播放器自己 (收叠层, 注册在栈顶), 播放键本来就在播放器语义里;
        //  - 向导/登录/授权这类流程页: 中途跳走会把没做完的流程整个丢掉, 长按保持普通语义
        // Navigation 3: 当前目的地就是返回栈栈顶那个路由对象 (原先是 currentBackStackEntry.destination).
        // runCatching 仍要留着: 返回栈由 AniAppContent 组合时才 setBackStack, 本函数在它外面, 冷启动
        // 那几帧读它会抛 (见 AniNavigator.backStack)
        val currentDestinationClaimable = {
            val route = runCatching { aniNavigator.backStack.lastOrNull() }.getOrNull()
            route != null &&
                    route !is NavRoutes.EpisodeDetail &&
                    route !is NavRoutes.Welcome &&
                    route !is NavRoutes.Onboarding &&
                    route !is NavRoutes.OnboardingComplete &&
                    route !is NavRoutes.EmailLoginStart &&
                    route !is NavRoutes.EmailLoginVerify &&
                    route !is NavRoutes.BangumiAuthorize
        }
        // **两个手势开同一个动作面板**, 行为完全一致 (含默认焦点), 学一个等于学两个:
        //
        // - **长按播放键 = 主入口**. 播放键几乎不存在误触 (没人会按住播放键不放), 而"长按媒体键
        //   弹出媒体面板"本身也讲得通. 原先它是"直接跳回正在播放"的盲跳, 现在换成"先看一眼再
        //   确认": 面板一开焦点就落在正在播放卡上, 再按一下确定就回去了 —— 多一下按键, 换来
        //   看清楚停在哪儿、加载到哪一步, 而且「关闭会话」也在旁边一步 (以前想关反而得换手势).
        // - **长按返回 = 兜底**. 精简遥控器 (Chromecast 那类) 没有播放键, 对他们这是唯一入口.
        //   返回键常用而退出应用不可逆, 所以让默认焦点落在卡片上很要紧: 误触之后条件反射按确定
        //   = 回到正在播放 (完全可逆), 按返回 = 关掉面板, 默认落点方圆两步之内没有不可逆动作.
        //
        // 长按返回这条是栈底兜底 (最先注册), 播放器的"收叠层"处理器比它优先.
        var showQuickMenu by remember { mutableStateOf(false) }
        TvBackLongPressHandler {
            if (currentDestinationClaimable()) {
                showQuickMenu = true
                true
            } else {
                false
            }
        }
        // 保留的会话与 AniAppContent 里是同一个 Activity 级 ViewModel (viewModel 同 owner 同 key
        // 返回同一实例), 这里拿它只为把把手传给面板 (读 session/progress/status + close).
        val retainSession = LocalAniUiBehavior.current.retainPlaybackSession &&
                LocalThemeSettings.current.tvRetainPlaybackSession
        val sessionHolder = viewModel { RetainedPlaybackSessionHolder() }
        val playbackEntry: PlaybackSessionEntry =
            if (retainSession) sessionHolder else PlaybackSessionEntry.None
        // 「一起看」入口把手: 同样是 Activity 级 ViewModel, 与 AniAppContent 里 provide 给
        // LocalWatchTogetherEntry 的是同一个实例 —— 本处在那个 provider 的**外面**, 读
        // CompositionLocal 只会拿到默认空实例 (见 WatchTogetherEntryState)
        val watchTogetherEntry = viewModel { WatchTogetherEntryState() }
        // 没有会话时照样开面板 (不再 toast"没有正在播放"): 面板里那张压暗的占位卡把同一句话说了,
        // 而且顺带把"这个手势是干什么的"演示了一遍 —— 一个只在没会话时才出现的 toast, 恰好只有
        // 不知道这手势的人会看到, 却什么也没让他们看见
        TvKeyLongPressHandler(playLongPress) {
            if (!currentDestinationClaimable()) return@TvKeyLongPressHandler false
            showQuickMenu = true
            true
        }
        if (showQuickMenu) {
            val context = LocalContext.current
            val appTerminator = remember { KoinPlatform.getKoin().get<AppTerminator>() }
            TvQuickActionMenu(
                navigator = aniNavigator,
                playback = playbackEntry,
                refreshHost = pageRefresh,
                watchTogether = watchTogetherEntry,
                onGoHome = {
                    // 焦点交接走标志 (探索页消费, 见 TvBackLongPressHost.pendingHomeFocus);
                    // 不在 Main 上时先 pop 回去, 落在别的 tab 上由主壳看着标志补一步切换
                    backLongPress.pendingHomeFocus = true
                    val onMain = runCatching {
                        aniNavigator.backStack.lastOrNull() is NavRoutes.Main
                    }.getOrNull() == true
                    if (!onMain) aniNavigator.popBackOrNavigateToMain(MainScreenPage.Exploration)
                },
                onExitApp = { appTerminator.exitApp(context, 0) },
                onDismissRequest = { showQuickMenu = false },
            )
        }
        Box(
            Modifier
                .tvKeyLongPressInterceptor(backLongPress)
                .tvKeyLongPressInterceptor(playLongPress),
        ) {
            content()
        }
    }
}

/**
 * 条目详情页的 TV 变体. 与其他插槽不同, 它有两个方法 (页面本体 + 首屏占位),
 * 不能用 SAM lambda 写法.
 */
private object TvSubjectDetailsPageVariant : SubjectDetailsPageVariant {
    @Composable
    override fun Page(
        state: SubjectDetailsState,
        selfInfo: SelfInfoUiState,
        layoutParams: SubjectDetailsLayoutParams,
        onPlay: (episodeId: Int) -> Unit,
        onClickTag: (Tag) -> Unit,
        onClickLogin: () -> Unit,
        onShowComments: () -> Unit,
        modifier: Modifier,
        onEpisodeCollectionUpdate: (SetEpisodeCollectionTypeRequest) -> Unit,
        showTopBar: Boolean,
        windowInsets: WindowInsets,
        backgroundPalette: Palette?,
        onClickOpenExternal: () -> Unit,
        onCoverImageSuccess: (AsyncImagePainter.State.Success) -> Unit,
        onClickCache: (() -> Unit)?,
        videoBackground: Boolean,
        onVideoBackgroundExitUp: (() -> Unit)?,
    ) {
        SubjectDetailsTvPage(
            state = state,
            selfInfo = selfInfo,
            layoutParams = layoutParams,
            onPlay = onPlay,
            onClickTag = onClickTag,
            onClickLogin = onClickLogin,
            onShowComments = onShowComments,
            modifier = modifier,
            onEpisodeCollectionUpdate = onEpisodeCollectionUpdate,
            showTopBar = showTopBar,
            windowInsets = windowInsets,
            backgroundPalette = backgroundPalette,
            onClickOpenExternal = onClickOpenExternal,
            onCoverImageSuccess = onCoverImageSuccess,
            onClickCache = onClickCache,
            videoBackground = videoBackground,
            onVideoBackgroundExitUp = onVideoBackgroundExitUp,
        )
    }

    @Composable
    override fun LoadingPlaceholder(
        subjectInfo: SubjectInfo?,
        layoutParams: SubjectDetailsLayoutParams,
        modifier: Modifier,
        windowInsets: WindowInsets,
    ) {
        SubjectDetailsTvLoadingPlaceholder(subjectInfo, layoutParams, modifier, windowInsets)
    }
}
