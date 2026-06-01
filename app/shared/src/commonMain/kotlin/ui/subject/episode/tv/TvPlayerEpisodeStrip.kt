/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.subject.episode.tv

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.him188.ani.app.domain.episode.SetEpisodeCollectionTypeRequest
import me.him188.ani.app.navigation.LocalNavigator
import me.him188.ani.app.ui.foundation.animation.AniAnimatedVisibility
import me.him188.ani.app.ui.foundation.widgets.LocalToaster
import me.him188.ani.app.ui.foundation.widgets.showLoadError
import me.him188.ani.app.ui.subject.details.SubjectDetailsUIState
import me.him188.ani.app.ui.subject.details.sections.TvEpisodeCarousel
import me.him188.ani.app.ui.subject.details.sections.TvEpisodeMetaColumn
import me.him188.ani.app.ui.subject.details.sections.mergedEpisodeDesc
import me.him188.ani.app.ui.subject.episode.EpisodeViewModel
import me.him188.ani.app.ui.subject.episode.list.EpisodeListItem

// ---- 调参 ----

/** 一行完整显示的卡片数 (卡宽由屏宽反推: 左右各留页面边距, 正好放下这么多张). */
private const val TV_STRIP_VISIBLE_CARDS = 4

/** 卡片间距. */
private val TV_STRIP_CARD_SPACING = 16.dp

/** 聚焦卡吸附行首时左侧露出的上一张卡切边宽度 (可视化"左边还有"). */
private val TV_STRIP_PREV_CARD_PEEK = 16.dp

/** 选集条展开/收起的滑动动画时长 (毫秒). */
private const val TV_STRIP_SLIDE_MS = 250

/** 聚焦集简介视口行数 (放不下的部分自动滚动展示). */
private const val TV_EPISODE_DESC_VISIBLE_LINES = 3

/** 简介自动滚动: 开始滚动前的等待 (切换聚焦集后重新计时). */
private const val TV_EPISODE_DESC_SCROLL_START_DELAY_MILLIS = 4_000L

/** 简介自动滚动: 滚到底后的停顿, 之后跳回顶部循环. */
private const val TV_EPISODE_DESC_SCROLL_END_PAUSE_MILLIS = 3_000L

/** 简介自动滚动速度 (dp/秒). */
private const val TV_EPISODE_DESC_SCROLL_SPEED_DP_PER_SEC = 14f

/**
 * 播放器控制层里的选集条 (Prime 形态): 复用详情页的选集轮播
 * ([TvEpisodeCarousel], 缩略图/播放进度/长按标记看过等一应俱全).
 *
 * 仅在展开态渲染 (由 [TvPlayerOverlayState.episodeStripExpanded] 驱动, 图标行按下键
 * 唤出, 平时完全不可见): 控制行隐藏 (调用方处理), 卡片行在上 (无标题行, 一行正好
 * [TV_STRIP_VISIBLE_CARDS] 张完整卡, 聚焦卡左侧露上一张卡切边), 下方是聚焦集简介
 * (自动滚动) + 右侧 时长/播出日期 两行. 点击卡片切换当前播放集并回纯画面;
 * 再按下键进详情层.
 *
 * 数据与详情层共用 subjectDetailsStateLoader (进屏已预载); 分集列表为空 (未开播/加载中)
 * 时整条不渲染, 并把 [TvPlayerOverlayState.episodeStripAvailable] 置 false ——
 * 根路由据此让图标行下键直通详情页.
 */
@Composable
internal fun TvPlayerEpisodeStrip(
    vm: EpisodeViewModel,
    overlay: TvPlayerOverlayState,
    stripFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val detailsState = vm.episodeDetailsState
    // 兜底触发加载 (正常已在进屏时预载, loader 有"已加载"守卫)
    LaunchedEffect(Unit) {
        detailsState.subjectDetailsStateLoader.load(detailsState.subjectId, detailsState.subjectInfo.value)
    }
    val uiState by detailsState.subjectDetailsStateLoader.state
        .collectAsStateWithLifecycle(SubjectDetailsUIState.Placeholder(detailsState.subjectId))
    val state = (uiState as? SubjectDetailsUIState.Ok)?.value
    if (state == null) {
        SideEffect { overlay.episodeStripAvailable = false }
        return
    }
    val presentation by state.presentation.collectAsStateWithLifecycle()
    val episodes = presentation.episodeListUiState.mainEpisodes
    SideEffect { overlay.episodeStripAvailable = episodes.isNotEmpty() }
    if (episodes.isEmpty()) return

    val tmdbEpisodeStills by state.tmdbEpisodeStillsFlow.collectAsStateWithLifecycle(emptyMap())
    val playProgress by state.playProgressFlow.collectAsStateWithLifecycle(emptyMap())
    val episodeRuntimes by state.tmdbEpisodeRuntimesFlow.collectAsStateWithLifecycle(emptyMap())
    val episodeOverviews by state.tmdbEpisodeOverviewsFlow.collectAsStateWithLifecycle(emptyMap())

    val navigator = LocalNavigator.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    // 展示中的集 (聚焦卡, 无聚焦时为当前集), 轮播回调上报; 下方简介/时长/日期跟随它
    var displayed by remember { mutableStateOf<EpisodeListItem?>(null) }

    // 仅展开态渲染: 平时完全不可见 (无 peek), 图标行按下键唤出.
    // 入场从底部整体上滑 (视觉上 = 卡片本来就在进度条下方, 聚焦时上移进画面,
    // 上方控制行同时淡出), 收起反向滑出
    AniAnimatedVisibility(
        visible = overlay.episodeStripExpanded,
        modifier = modifier,
        enter = slideInVertically(tween(TV_STRIP_SLIDE_MS)) { it } + fadeIn(tween(TV_STRIP_SLIDE_MS)),
        exit = slideOutVertically(tween(TV_STRIP_SLIDE_MS)) { it } + fadeOut(tween(TV_STRIP_SLIDE_MS)),
    ) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .onFocusChanged { if (it.hasFocus) overlay.focusRegion = TvPlayerFocusRegion.EPISODES }
            .focusGroup(),
    ) {
        // 卡宽由屏宽反推: 左右各留页面边距, 一行正好 TV_STRIP_VISIBLE_CARDS 张完整卡
        val cellWidth = (this.maxWidth - TV_PLAYER_HORIZONTAL_PAD * 2 -
            TV_STRIP_CARD_SPACING * (TV_STRIP_VISIBLE_CARDS - 1)) / TV_STRIP_VISIBLE_CARDS
        val cellHeight = cellWidth * 9f / 16f
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TvEpisodeCarousel(
                episodes = episodes,
                // "正在播放" 徽标 + 初始滚动位置 = 当前播放集 (详情页里是"下一集要看的")
                currentEpisodeId = vm.episodeSelectorState.current?.episodeId,
                onEpisodeClick = { item ->
                    // 与详情层选集一致: 就地切换当前播放集; 不在列表里 (特别篇等) 整页导航
                    if (!vm.episodeSelectorState.selectEpisodeId(item.episodeId)) {
                        navigator.navigateEpisodeDetails(vm.subjectId, item.episodeId)
                    }
                    overlay.hideAll()
                },
                episodeStills = tmdbEpisodeStills,
                playProgress = playProgress,
                episodeRuntimes = episodeRuntimes,
                episodeOverviews = episodeOverviews,
                horizontalPadding = TV_PLAYER_HORIZONTAL_PAD,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
                cellSpacing = TV_STRIP_CARD_SPACING,
                // 聚焦卡吸附行首时左侧露出上一张卡切边
                focusedCardPeek = TV_STRIP_PREV_CARD_PEEK,
                // 集信息不放卡片行上方: 简介/时长/日期由下方区域展示 (见 onDisplayedChanged)
                showEpisodeInfo = false,
                onDisplayedChanged = { displayed = it },
                // 长按卡片: 标记看过/取消看过 (菜单开合上报, 抑制控制层自动隐藏)
                onSetEpisodeCollectionType = { item, type ->
                    scope.launch {
                        vm.setEpisodeCollectionType.invokeSafe(
                            SetEpisodeCollectionTypeRequest(state.subjectId, item.episodeId, type),
                        )?.let { toaster.showLoadError(it) }
                    }
                },
                onActionMenuExpandedChanged = { overlay.onPopupExpandedChanged(it) },
                rowFocusRequester = stripFocusRequester,
            )
            // 卡片行下方: 聚焦集简介 (自动滚动) + 右侧 时长/播出日期 两行
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TV_PLAYER_HORIZONTAL_PAD),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                val desc = displayed?.let {
                    mergedEpisodeDesc(episodeOverviews[it.episodeId], it.desc)
                }.orEmpty()
                // 简介: 固定 3 行视口自动滚动 (不可聚焦, 无阅读模式)
                TvAutoScrollEpisodeDesc(desc, Modifier.weight(1f))
                displayed?.let {
                    TvEpisodeMetaColumn(
                        runtimeMinutes = episodeRuntimes[it.episodeId],
                        airDate = it.airDate,
                    )
                }
            }
        }
    }
    }
}

/**
 * 选集条聚焦集简介: 固定 [TV_EPISODE_DESC_VISIBLE_LINES] 行视口, 不可聚焦.
 * 文字放不下时等待 [TV_EPISODE_DESC_SCROLL_START_DELAY_MILLIS] 后匀速向下滚动,
 * 滚到底停顿后跳回顶部循环; 切换聚焦集 (文字变化) 时回到顶部重新计时.
 */
@Composable
private fun TvAutoScrollEpisodeDesc(desc: String, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.bodySmall
    val density = LocalDensity.current
    val lineHeight = if (style.lineHeight.isSpecified) style.lineHeight else 16.sp
    val viewportHeight = with(density) { lineHeight.toDp() } * TV_EPISODE_DESC_VISIBLE_LINES
    val scrollState = rememberScrollState()
    LaunchedEffect(desc) {
        scrollState.scrollTo(0)
        while (true) {
            delay(TV_EPISODE_DESC_SCROLL_START_DELAY_MILLIS)
            val max = scrollState.maxValue
            // 视口装得下整段文字: 不滚 (文字变化会重启本效应)
            if (max <= 0) return@LaunchedEffect
            val durationMillis = with(density) {
                (max / TV_EPISODE_DESC_SCROLL_SPEED_DP_PER_SEC.dp.toPx() * 1000).toInt()
            }.coerceAtLeast(1)
            scrollState.animateScrollTo(max, tween(durationMillis, easing = LinearEasing))
            delay(TV_EPISODE_DESC_SCROLL_END_PAUSE_MILLIS)
            scrollState.scrollTo(0)
        }
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(viewportHeight)
            .verticalScroll(scrollState, enabled = false),
    ) {
        Text(
            desc,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = style,
        )
    }
}
