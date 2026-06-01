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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import me.him188.ani.app.data.models.preference.DarkMode
import me.him188.ani.app.domain.player.VideoLoadingState
import me.him188.ani.app.ui.danmaku.DanmakuEditorState
import me.him188.ani.app.ui.danmaku.PlayerDanmakuHost
import me.him188.ani.app.ui.foundation.LocalImageViewerHandler
import me.him188.ani.app.ui.foundation.animation.AniAnimatedVisibility
import me.him188.ani.app.ui.foundation.theme.AniTheme
import me.him188.ani.app.ui.foundation.tv.tvResolveFocus
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

/** 详情层淡入时长 (毫秒). */
private const val TV_DETAILS_FADE_IN_MS = 300

/** 详情层淡出时长 (毫秒): 放慢一档, 瞬时/快速移除观感像闪切. */
private const val TV_DETAILS_FADE_OUT_MS = 500

/**
 * TV 播放器界面 (Prime Video 风格):
 *
 * - 纯视频态 (HIDDEN): 只有画面和弹幕. 确认/暂停键切换播放并唤出控制层, 上下键仅唤出,
 *   左右键快进退并唤出.
 * - 控制层 (CONTROLS): 顶部标题/时钟, 底部 [胶囊按钮行 + 进度条 + 图标行];
 *   聚焦胶囊按钮时其上方浮出对应面板 (弹幕列表/相关推荐/本集评论), 面板条目吸附底部,
 *   从下往上导航.
 * - 详情页 (DETAILS): 图标行按下键唤出, 隐藏全部播放器组件, 视频画面作为详情页背景.
 *
 * 所有按键语义集中在本文件的唯一路由 (根 onPreviewKeyEvent), 层级切换集中在
 * [TvPlayerOverlayState]; 各行容器只通过 onFocusChanged 上报焦点区域, 不各自处理按键.
 */
@Composable
internal fun TvEpisodeScreenContent(
    vm: EpisodeViewModel,
    page: EpisodePageState,
    danmakuHostState: DanmakuHostState,
    danmakuEditorState: DanmakuEditorState,
    setShowEditCommentSheet: (Boolean) -> Unit,
    pauseOnPlaying: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val overlay = remember { TvPlayerOverlayState() }
    val sheetsController = rememberVideoSideSheetsController<EpisodeVideoSideSheetPage>()
    val anySheetVisible by sheetsController.hasPageAsState()
    val imageViewer = LocalImageViewerHandler.current

    SideEffect { vm.onUIReady() }

    // 预载条目详情 (分集列表/TMDB 缩略图等): 控制层选集条与详情层共用同一 loader
    // (有"已加载"守卫, 不会重复请求). 进屏即载, 图标行首次下键时选集条通常已就绪.
    LaunchedEffect(Unit) {
        val detailsState = vm.episodeDetailsState
        detailsState.subjectDetailsStateLoader.load(detailsState.subjectId, detailsState.subjectInfo.value)
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

    // ---- 唯一按键路由: 所有 Back 语义与层级切换都在这里, 状态读取只发生在事件回调内 ----
    val onRootKeyEvent: (KeyEvent) -> Boolean = router@{ event ->
        val key = event.key
        val isKeyDown = event.type == KeyEventType.KeyDown
        val isKeyUp = event.type == KeyEventType.KeyUp
        val isBack = key == Key.Back || key == Key.Escape

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

                    Key.DirectionLeft -> {
                        vm.player.skip(-TV_PLAYER_SEEK_STEP_MILLIS)
                        overlay.showControls()
                        true
                    }

                    Key.DirectionRight -> {
                        vm.player.skip(TV_PLAYER_SEEK_STEP_MILLIS)
                        overlay.showControls()
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
                    // 无分集 (未开播/数据未到) 直通详情页. 选集条内再往下: 详情页 (第三层)
                    Key.DirectionDown -> when (overlay.focusRegion) {
                        TvPlayerFocusRegion.BOTTOM_ROW -> {
                            if (overlay.episodeStripAvailable) {
                                overlay.expandEpisodeStrip()
                            } else {
                                overlay.openDetails()
                            }
                            true
                        }

                        TvPlayerFocusRegion.EPISODES -> {
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
            tvResolveFocus(
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

            // 缓冲/加载指示 (居中悬浮)
            TvPlayerLoadingLayer(vm, Modifier.align(Alignment.Center))

            // 播放/暂停切换反馈: 画面中央浮现对应图标并渐隐 (监听播放器状态流,
            // 无论切换来自确认键/控制按钮/面板操作都有反馈)
            TvPauseFlash(vm.player, Modifier.align(Alignment.Center))

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
        // key(flashKey): 快速连按时重启动画 (从满透明度重新开始)
        key(flashKey) {
            val alpha = remember { Animatable(1f) }
            LaunchedEffect(Unit) {
                alpha.animateTo(
                    0f,
                    tween(TV_PAUSE_FLASH_DURATION_MS, delayMillis = TV_PAUSE_FLASH_HOLD_MS),
                )
                visible = false
            }
            Box(modifier.graphicsLayer { this.alpha = alpha.value }) {
                // 无底衬: 亮画面上靠黑色偏移投影保证可见
                TvPauseBars(
                    Color.Black.copy(alpha = TV_PAUSE_FLASH_SHADOW_ALPHA),
                    Modifier.offset(x = 1.dp, y = 1.5.dp),
                )
                TvPauseBars(Color.White)
            }
        }
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

/** 暂停反馈的渐隐时长与起始停留 (毫秒). */
private const val TV_PAUSE_FLASH_DURATION_MS = 500
private const val TV_PAUSE_FLASH_HOLD_MS = 120

/** 暂停反馈投影不透明度. */
private const val TV_PAUSE_FLASH_SHADOW_ALPHA = 0.55f
