/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.subject.episode.tv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItemsWithLifecycle
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import me.him188.ani.app.data.models.subject.nameCn
import me.him188.ani.app.navigation.LocalNavigator
import me.him188.ani.app.navigation.SubjectDetailPlaceholder
import me.him188.ani.app.tools.formatDateTime
import me.him188.ani.app.domain.comment.CommentContext
import me.him188.ani.app.ui.comment.UIRichText
import me.him188.ani.app.ui.danmaku.DanmakuEditorState
import me.him188.ani.app.ui.foundation.AsyncImage
import me.him188.ani.app.ui.foundation.avatar.AvatarImage
import me.him188.ani.app.ui.foundation.focus.resolveFocusRepeatedly
import me.him188.ani.app.ui.lang.Lang
import me.him188.ani.app.ui.lang.episode_send_danmaku
import me.him188.ani.app.ui.lang.subject_episode_danmaku_list_empty
import me.him188.ani.app.ui.richtext.UIRichElement
import me.him188.ani.app.ui.subject.details.SubjectDetailsUIState
import me.him188.ani.app.ui.subject.episode.EpisodePageState
import me.him188.ani.app.ui.subject.episode.EpisodeViewModel
import me.him188.ani.app.ui.subject.person.PeoplePreviewTarget
import me.him188.ani.app.ui.subject.person.rememberPeopleClickHandler
import me.him188.ani.app.ui.subject.episode.details.DanmakuSourceChips
import me.him188.ani.app.ui.subject.episode.details.DanmakuTimeShiftDialog
import me.him188.ani.app.ui.subject.episode.details.components.renderDanmakuServiceId
import me.him188.ani.danmaku.api.DanmakuContent
import me.him188.ani.danmaku.api.DanmakuLocation
import me.him188.ani.danmaku.api.DanmakuServiceId
import me.him188.ani.utils.analytics.Analytics
import me.him188.ani.utils.analytics.AnalyticsEvent.Companion.SubjectEnter
import me.him188.ani.utils.analytics.AnalyticsEvent.Companion.SubjectRecommendationClick
import me.him188.ani.utils.analytics.recordEvent
import org.jetbrains.compose.resources.stringResource

// ---- 面板调参 ----

/** 面板宽度 (Prime 实测约屏宽 1/4): 弹幕列表/评论等文字面板. */
private val TV_PANEL_WIDTH = 420.dp

/** 卡片类面板宽度 (相关推荐/角色/制作人员: 头像 + 单行文字, 收窄; 放不下的文字聚焦跑马灯). */
private val TV_PANEL_CARD_WIDTH = 240.dp

/** 面板最大高度 (向上最多长到这里, 条目少时按内容收缩). */
private val TV_PANEL_MAX_HEIGHT = 300.dp

/** 面板条目未聚焦底色 (半透明玻璃, 视频上可读). */
private val TV_PANEL_ITEM_COLOR = Color.Black.copy(alpha = 0.55f)

/** 面板条目聚焦底色. */
private val TV_PANEL_ITEM_FOCUSED_COLOR = Color.Black.copy(alpha = 0.8f)

/**
 * L2 浮出面板宿主: 弹幕列表 / 相关推荐 / 本集评论.
 *
 * 统一形态: `LazyColumn(reverseLayout = true)` —— index 0 在底部, 向上导航 = 索引递增,
 * 聚焦项吸附到底缘 (禁默认 bring-into-view + animateScrollToItem), 最底项按下键
 * 显式回到打开面板的胶囊. 面板每次浮出都复位到底部 (进入焦点恒落最下第一项).
 */
@Composable
internal fun TvPlayerPanelHost(
    overlay: TvPlayerOverlayState,
    vm: EpisodeViewModel,
    page: EpisodePageState,
    setShowEditCommentSheet: (Boolean) -> Unit,
    pauseOnPlaying: () -> Unit,
    /** 各胶囊按钮的焦点请求器: 面板最底项按下键显式回到打开它的那个胶囊. */
    pillFocusRequesters: Map<TvPlayerPanel, FocusRequester>,
    modifier: Modifier = Modifier,
) {
    val danmakuListState = rememberLazyListState()
    val recommendationsListState = rememberLazyListState()
    val commentsListState = rememberLazyListState()
    val charactersListState = rememberLazyListState()
    val staffListState = rememberLazyListState()

    // 当前聚焦条目下标 (吸底滚动用; 同一时刻只有一个面板在场, 共享一个)
    val focusedIndex = remember { mutableIntStateOf(-1) }
    // 每面板独立的入口请求器: AnimatedContent 淡切期间新旧两个面板并存,
    // 共享一个请求器会双挂载 (解析可能聚到即将卸载的旧面板上, 焦点随之丢失)
    val entryFocusRequesters = remember { TvPlayerPanel.entries.associateWith { FocusRequester() } }
    // 弹幕面板底部 chips 行当前是否存在 (加载早期/全部源失败时不组合), 由面板上报;
    // 左右键跳相邻胶囊的豁免判断依据 —— chips 缺席时 index 0 是普通弹幕行, 不豁免
    val danmakuChipsPresent = remember { mutableStateOf(false) }

    // 面板每次浮出都复位到底部 (index 0): 进入焦点 (点击胶囊/上键) 永远落在最下面
    // 第一项 —— 保留上次滚动位置会让进入焦点落在"不知道第几项"上, 反直觉
    LaunchedEffect(overlay.activePanel) {
        val listState = when (overlay.activePanel) {
            TvPlayerPanel.DANMAKU_LIST -> danmakuListState
            TvPlayerPanel.RECOMMENDATIONS -> recommendationsListState
            TvPlayerPanel.COMMENTS -> commentsListState
            TvPlayerPanel.CHARACTERS -> charactersListState
            TvPlayerPanel.STAFF -> staffListState
            null -> return@LaunchedEffect
        }
        focusedIndex.intValue = -1
        runCatching { listState.scrollToItem(0) }
    }

    // 点击胶囊把焦点送进面板: 等面板组合完成, 到位确认 (focusRegion) + 重试.
    // 消化统一 pendingFocus 的 PANEL 目标 (入口请求器在本子树, 屏幕级解析器够不着);
    // collectLatest: 其它目标的新请求会替换 PANEL 请求并取消本解析, 无互抢
    LaunchedEffect(Unit) {
        snapshotFlow { overlay.pendingFocus }.collectLatest { (target, _) ->
            if (target != TvPlayerFocusTarget.PANEL) return@collectLatest
            resolveFocusRepeatedly(
                arrived = {
                    // 面板已关掉也视为到位 (放弃解析)
                    overlay.activePanel == null || overlay.focusRegion == TvPlayerFocusRegion.PANEL
                },
            ) {
                overlay.activePanel?.let {
                    runCatching { entryFocusRequesters.getValue(it).requestFocus() }
                }
            }
        }
    }

    AnimatedContent(
        targetState = overlay.activePanel,
        modifier = modifier,
        transitionSpec = {
            when {
                initialState == null ->
                    (slideInVertically(tween(200)) { it / 4 } + fadeIn(tween(200))) togetherWith
                            fadeOut(tween(120))

                targetState == null ->
                    fadeIn(tween(120)) togetherWith
                            (slideOutVertically(tween(200)) { it / 4 } + fadeOut(tween(200)))

                else -> fadeIn(tween(150)) togetherWith fadeOut(tween(100))
            }
        },
        contentAlignment = Alignment.BottomStart,
        label = "tvPlayerPanel",
    ) { panel ->
        val panelWidth = when (panel) {
            TvPlayerPanel.RECOMMENDATIONS, TvPlayerPanel.CHARACTERS, TvPlayerPanel.STAFF ->
                TV_PANEL_CARD_WIDTH

            else -> TV_PANEL_WIDTH
        }
        val panelModifier = Modifier
            .width(panelWidth)
            .heightIn(max = TV_PANEL_MAX_HEIGHT)
            .padding(bottom = 14.dp)
            // 最底项 (index 0) 按下键: 显式回到打开本面板的胶囊 —— 交给空间搜索会落到
            // 面板正下方的任意按钮, 落错后该按钮的聚焦回调又把面板切成自己的 (面板跳变)
            .onPreviewKeyEvent { event ->
                when {
                    panel == null -> false

                    event.key == Key.DirectionDown && focusedIndex.intValue == 0 -> {
                        if (event.type == KeyEventType.KeyDown) {
                            runCatching { pillFocusRequesters.getValue(panel).requestFocus() }
                        }
                        true
                    }

                    // 条目上按左/右: 焦点跳到打开本面板的胶囊的左/右相邻胶囊 (面板随之
                    // 切换). 面板条目单列, 左右键没有面板内目标, 交给空间搜索会斜跳到
                    // 下方按钮行的任意按钮; 例外: 弹幕列表最底行 (index 0) 是横排 chips
                    // (且 chips 确实在场, 缺席时 index 0 是普通弹幕行), 左右键留给行内导航
                    (event.key == Key.DirectionLeft || event.key == Key.DirectionRight) &&
                            !(panel == TvPlayerPanel.DANMAKU_LIST && focusedIndex.intValue == 0 &&
                                    danmakuChipsPresent.value) -> {
                        if (event.type == KeyEventType.KeyDown) {
                            val neighborIndex = TV_PILL_VISUAL_ORDER.indexOf(panel) +
                                    (if (event.key == Key.DirectionLeft) -1 else 1)
                            TV_PILL_VISUAL_ORDER.getOrNull(neighborIndex)?.let {
                                runCatching { pillFocusRequesters.getValue(it).requestFocus() }
                            }
                        }
                        // 到头 (最左/最右胶囊的面板) 也消费, 防斜跳
                        true
                    }

                    else -> false
                }
            }
        when (panel) {
            null -> Box(Modifier.height(0.dp))

            TvPlayerPanel.STAFF -> TvStaffPanel(
                vm, overlay, staffListState, focusedIndex,
                entryFocusRequesters.getValue(panel), panelModifier,
            )

            TvPlayerPanel.CHARACTERS -> TvCharactersPanel(
                vm, overlay, charactersListState, focusedIndex,
                entryFocusRequesters.getValue(panel), panelModifier,
            )

            TvPlayerPanel.RECOMMENDATIONS -> TvRecommendationsPanel(
                vm, overlay, recommendationsListState, focusedIndex,
                entryFocusRequesters.getValue(panel), panelModifier,
            )

            TvPlayerPanel.COMMENTS -> TvCommentsPanel(
                vm, page, overlay, commentsListState, focusedIndex,
                entryFocusRequesters.getValue(panel),
                setShowEditCommentSheet, pauseOnPlaying, panelModifier,
            )

            TvPlayerPanel.DANMAKU_LIST -> TvDanmakuListPanel(
                vm, page, overlay, danmakuListState, focusedIndex,
                entryFocusRequesters.getValue(panel),
                onChipsPresentChanged = { danmakuChipsPresent.value = it },
                modifier = panelModifier,
            )
        }
    }
}

// ============================ 共用脚手架 ============================

/**
 * 底锚可导航列表: reverseLayout (index 0 在底), 聚焦项吸底, 关默认 bring-into-view.
 *
 * 面板入口请求器不挂在本列表上 (对焦点组 requestFocus 的进组落点不确定, 实测会落到
 * 视觉最上面的条目), 由各面板挂到自己的 index 0 (最底, 紧邻胶囊按钮) 条目上.
 */
@Composable
private fun TvPanelList(
    listState: LazyListState,
    overlay: TvPlayerOverlayState,
    focusedIndex: MutableIntState,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 8.dp,
    content: LazyListScope.() -> Unit,
) {
    val noBringIntoView = remember {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
        }
    }
    // 聚焦项吸底: reverseLayout 下 scrollToItem 把目标项对齐到列表起点 = 底缘
    LaunchedEffect(listState) {
        snapshotFlow { focusedIndex.intValue }.collectLatest { idx ->
            if (idx >= 0) runCatching { listState.animateScrollToItem(idx) }
        }
    }
    CompositionLocalProvider(LocalBringIntoViewSpec provides noBringIntoView) {
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = modifier
                .focusGroup()
                .onFocusChanged { if (it.hasFocus) overlay.focusRegion = TvPlayerFocusRegion.PANEL },
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            content()
        }
    }
}

/** 面板条目: 半透明玻璃底, 聚焦白色圆角描边 (参考 Prime 实测). [content] 收到实时聚焦态 (跑马灯用). */
@Composable
private fun TvPanelItem(
    index: Int,
    focusedIndex: MutableIntState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (focused: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) focusedIndex.intValue = index },
        shape = RoundedCornerShape(12.dp),
        color = if (focused) TV_PANEL_ITEM_FOCUSED_COLOR else TV_PANEL_ITEM_COLOR,
        contentColor = Color.White,
        border = if (focused) BorderStroke(2.dp, Color.White) else null,
        interactionSource = interactionSource,
    ) {
        content(focused)
    }
}

// ============================ 弹幕列表面板 ============================

/**
 * 弹幕列表: 底部 (index 0) 是弹幕源开关/重新匹配/调整延迟 chips (即导航入口第一站),
 * 上方为全集弹幕 (时间升序, 越往上越晚).
 */
@Composable
private fun TvDanmakuListPanel(
    vm: EpisodeViewModel,
    page: EpisodePageState,
    overlay: TvPlayerOverlayState,
    listState: LazyListState,
    focusedIndex: MutableIntState,
    entryFocusRequester: FocusRequester,
    /** 底部 chips 行是否在场 (宿主的左右键豁免判断依据), 变化时上报. */
    onChipsPresentChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.danmakuListState.collectAsStateWithLifecycle()
    var editingShiftServiceId by remember { mutableStateOf<DanmakuServiceId?>(null) }
    SideEffect { onChipsPresentChanged(state.sourceItems.isNotEmpty()) }

    // 弹幕条目多且单行, 间距收到最紧. 条目是可点击 Surface, M3 会给它套 48dp
    // 最小交互尺寸 —— 单行文字实际不到 30dp, 多出的全变成上下空隙, 这里关掉
    // 弹幕条目的导航 index 基底: chips 项存在时它占 0, 条目从 1 起; chips 未组合
    // (加载早期/全部源失败) 时条目自己从 0 起 —— 否则没有任何项是 index 0,
    // 面板"最底项按下键回胶囊"的守卫 (focusedIndex == 0) 永不命中
    val itemIndexBase = if (state.sourceItems.isNotEmpty()) 1 else 0
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
    TvPanelList(listState, overlay, focusedIndex, modifier, itemSpacing = 2.dp) {
        // 底部: 弹幕源 chips (原侧边栏弹幕列表里的源开关/延迟按钮, 功能不变)
        if (state.sourceItems.isNotEmpty()) {
            item("danmaku_sources") {
                Surface(
                    Modifier
                        .fillMaxWidth()
                        // 面板入口落点 (index 0, 进焦点落到第一个 chip)
                        .focusRequester(entryFocusRequester)
                        .onFocusChanged { if (it.hasFocus) focusedIndex.intValue = 0 },
                    shape = RoundedCornerShape(12.dp),
                    color = TV_PANEL_ITEM_COLOR,
                    contentColor = Color.White,
                ) {
                    DanmakuSourceChips(
                        sourceItems = state.sourceItems,
                        onToggleSource = { serviceId, enabled -> vm.setDanmakuSourceEnabled(serviceId, enabled) },
                        onManualMatch = { serviceId ->
                            page.danmakuStatistics.fetchResults.find { it.serviceId == serviceId }?.let {
                                vm.startMatchingDanmaku(it.providerId)
                            }
                        },
                        onAdjustShift = { serviceId -> editingShiftServiceId = serviceId },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
        if (state.isLoading) {
            item("danmaku_loading") {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            }
        } else if (state.danmakuItems.isEmpty()) {
            item("danmaku_empty") {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(Lang.subject_episode_danmaku_list_empty),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        items(
            count = state.danmakuItems.size,
            key = { i -> "${state.danmakuItems[i].id}-$i" },
        ) { i ->
            val danmaku = state.danmakuItems[i]
            TvPanelItem(
                index = i + itemIndexBase,
                focusedIndex = focusedIndex,
                onClick = {},
                // chips 未组合时首条弹幕即 index 0, 兼任面板入口落点
                modifier = if (i + itemIndexBase == 0) Modifier.focusRequester(entryFocusRequester) else Modifier,
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        danmaku.content,
                        Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (danmaku.isSelf) MaterialTheme.colorScheme.primary else Color.White,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        renderTvPlayerTimeShort(danmaku.timeMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
    }

    // 弹幕源延迟调整对话框 (复用手机版, 已带 TV slider 按键适配)
    val editingShiftSource = editingShiftServiceId?.let { serviceId ->
        page.danmakuStatistics.fetchResults.firstOrNull { it.serviceId == serviceId }
    }
    if (editingShiftSource != null) {
        DanmakuTimeShiftDialog(
            serviceName = renderDanmakuServiceId(editingShiftSource.serviceId),
            currentShiftMillis = editingShiftSource.config.shiftMillis,
            onDismissRequest = { editingShiftServiceId = null },
            onConfirm = { newShift ->
                vm.setDanmakuSourceShiftMillis(editingShiftSource.serviceId, newShift)
                editingShiftServiceId = null
            },
        )
    }
}

private fun renderTvPlayerTimeShort(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

// ============================ 相关推荐面板 ============================

/** 相关推荐: 卡片形态 (封面 + 标题), 点击离开播放器进入对应条目详情页 (与手机版行为一致). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TvRecommendationsPanel(
    vm: EpisodeViewModel,
    overlay: TvPlayerOverlayState,
    listState: LazyListState,
    focusedIndex: MutableIntState,
    entryFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current
    val recommendations by vm.episodeDetailsState.recommendations
    TvPanelList(listState, overlay, focusedIndex, modifier) {
        items(
            count = recommendations.size,
            key = { i -> recommendations[i].uniqueId },
        ) { i ->
            val recommendation = recommendations[i]
            TvPanelItem(
                index = i,
                focusedIndex = focusedIndex,
                modifier = if (i == 0) Modifier.focusRequester(entryFocusRequester) else Modifier,
                onClick = {
                    val targetSubjectId = recommendation.subjectId?.toInt() ?: return@TvPanelItem
                    Analytics.recordEvent(SubjectRecommendationClick) {
                        put("subject_id", targetSubjectId)
                    }
                    Analytics.recordEvent(SubjectEnter) {
                        put("source", "episode_recommendation")
                        put("subject_id", targetSubjectId)
                    }
                    navigator.navigateSubjectDetails(
                        targetSubjectId,
                        SubjectDetailPlaceholder(
                            id = targetSubjectId,
                            name = recommendation.name,
                            nameCN = recommendation.nameCn ?: "",
                            coverUrl = recommendation.imageUrl,
                        ),
                    )
                },
            ) { focused ->
                Row(
                    Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        recommendation.imageUrl,
                        contentDescription = null,
                        Modifier
                            .size(width = 52.dp, height = 72.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        // 单行, 放不下时聚焦跑马灯 (与选集卡片同规矩)
                        Text(
                            recommendation.nameCn ?: recommendation.name.orEmpty(),
                            if (focused) Modifier.basicMarquee(iterations = Int.MAX_VALUE) else Modifier,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = if (focused) TextOverflow.Clip else TextOverflow.Ellipsis,
                        )
                        recommendation.name?.takeIf { it != recommendation.nameCn }?.let {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                it,
                                if (focused) Modifier.basicMarquee(iterations = Int.MAX_VALUE) else Modifier,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = if (focused) TextOverflow.Clip else TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================ 本集评论面板 ============================

/**
 * 本集评论: 紧凑纯文本条目 (保持单条目单焦点, 富文本/大图/回复等完整功能在详情页评论区);
 * 点击条目 = 回复该评论 (与手机版评论列表一致).
 */
@Composable
private fun TvCommentsPanel(
    vm: EpisodeViewModel,
    page: EpisodePageState,
    overlay: TvPlayerOverlayState,
    listState: LazyListState,
    focusedIndex: MutableIntState,
    entryFocusRequester: FocusRequester,
    setShowEditCommentSheet: (Boolean) -> Unit,
    pauseOnPlaying: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val comments = vm.episodeCommentState.list.collectAsLazyPagingItemsWithLifecycle()
    TvPanelList(listState, overlay, focusedIndex, modifier) {
        items(
            count = comments.itemCount,
            key = comments.itemKey { it.stableId },
        ) { i ->
            val comment = comments[i] ?: return@items
            TvPanelItem(
                index = i,
                focusedIndex = focusedIndex,
                modifier = if (i == 0) Modifier.focusRequester(entryFocusRequester) else Modifier,
                onClick = {
                    vm.commentEditorState.startEdit(
                        CommentContext.EpisodeReply(
                            vm.subjectId,
                            page.episodePresentation.episodeId.toLong(),
                            comment.sourceCommentId,
                        ),
                    )
                    setShowEditCommentSheet(true)
                    pauseOnPlaying()
                },
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            comment.author?.nickname ?: comment.author?.id?.toString().orEmpty(),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            formatDateTime(comment.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        remember(comment.stableId) { comment.content.toPlainText() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** 富文本压成纯文本 (面板紧凑形态; 完整富文本在详情页评论区). */
private fun UIRichText.toPlainText(): String =
    elements.joinToString(separator = "") { element ->
        when (element) {
            is UIRichElement.AnnotatedText -> element.slice.joinToString(separator = "") { slice ->
                when (slice) {
                    is UIRichElement.Annotated.Text -> slice.content
                    is UIRichElement.Annotated.Sticker -> "[表情]"
                }
            }

            is UIRichElement.Quote -> ""
            is UIRichElement.Image -> "[图片]"
        }
    }

// ============================ 角色 / 制作人员面板 ============================

/**
 * 角色面板: 卡片形态 (头像 + 名字 + 角色/CV), 点击弹居中人物预览
 * (需调用方在面板宿主外包 [me.him188.ani.app.ui.subject.person.PeoplePreviewHost]).
 * 数据与详情层/选集条共用 subjectDetailsStateLoader (进屏已预载);
 * 用完整名单 pager (原详情页"查看全部"的数据源) —— 详情页的角色/制作人员区块已移除,
 * 本面板是 TV 上唯一入口, 向上翻页自动加载更多.
 */
@Composable
private fun TvCharactersPanel(
    vm: EpisodeViewModel,
    overlay: TvPlayerOverlayState,
    listState: LazyListState,
    focusedIndex: MutableIntState,
    entryFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val detailsState = vm.episodeDetailsState
    val uiState by detailsState.subjectDetailsStateLoader.state
        .collectAsStateWithLifecycle(SubjectDetailsUIState.Placeholder(detailsState.subjectId))
    val details = (uiState as? SubjectDetailsUIState.Ok)?.value ?: return
    val characters = details.charactersPager.collectAsLazyPagingItemsWithLifecycle()
    val onClickCharacter = rememberPeopleClickHandler()
    TvPanelList(listState, overlay, focusedIndex, modifier) {
        items(
            count = characters.itemCount,
            key = characters.itemKey { it.character.id },
        ) { i ->
            val item = characters[i] ?: return@items
            TvPanelItem(
                index = i,
                focusedIndex = focusedIndex,
                modifier = if (i == 0) Modifier.focusRequester(entryFocusRequester) else Modifier,
                onClick = { onClickCharacter(PeoplePreviewTarget.Character(item.character.id)) },
            ) {
                val cv = item.character.actors.firstOrNull()?.displayName
                TvPersonPanelItemContent(
                    avatarUrl = item.character.imageMedium,
                    name = item.character.displayName,
                    subtitle = if (cv.isNullOrBlank()) item.role.nameCn else item.role.nameCn + " · " + cv,
                )
            }
        }
    }
}

/** 制作人员面板: 卡片形态 (头像 + 名字 + 职位), 点击弹居中人物预览; 完整名单 (同角色面板). */
@Composable
private fun TvStaffPanel(
    vm: EpisodeViewModel,
    overlay: TvPlayerOverlayState,
    listState: LazyListState,
    focusedIndex: MutableIntState,
    entryFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val detailsState = vm.episodeDetailsState
    val uiState by detailsState.subjectDetailsStateLoader.state
        .collectAsStateWithLifecycle(SubjectDetailsUIState.Placeholder(detailsState.subjectId))
    val details = (uiState as? SubjectDetailsUIState.Ok)?.value ?: return
    val staff = details.staffPager.collectAsLazyPagingItemsWithLifecycle()
    val onClickPerson = rememberPeopleClickHandler()
    TvPanelList(listState, overlay, focusedIndex, modifier) {
        items(
            count = staff.itemCount,
            key = staff.itemKey { it.personInfo.id },
        ) { i ->
            val person = staff[i] ?: return@items
            TvPanelItem(
                index = i,
                focusedIndex = focusedIndex,
                modifier = if (i == 0) Modifier.focusRequester(entryFocusRequester) else Modifier,
                onClick = { onClickPerson(PeoplePreviewTarget.Person(person.personInfo.id)) },
            ) {
                TvPersonPanelItemContent(
                    avatarUrl = person.personInfo.imageMedium,
                    name = person.personInfo.displayName,
                    subtitle = person.position.nameCn ?: "",
                )
            }
        }
    }
}

/** 面板人物条目内容: 头像 + 名字 + 副标题 (容器用面板玻璃条目). */
@Composable
private fun TvPersonPanelItemContent(
    avatarUrl: String?,
    name: String,
    subtitle: String,
) {
    Row(
        Modifier.padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 头像方圆角, crop 顶部对齐 (人物图多为立绘, 顶部对齐保证露脸)
        Box(Modifier.size(TV_PERSON_PANEL_AVATAR_SIZE).clip(RoundedCornerShape(8.dp))) {
            AvatarImage(
                avatarUrl,
                Modifier.size(TV_PERSON_PANEL_AVATAR_SIZE),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 人物面板条目头像尺寸. */
private val TV_PERSON_PANEL_AVATAR_SIZE = 48.dp

// ============================ 弹幕发送入口 ============================

/**
 * 弹幕发送入口: 胶囊行末尾的圆钮, 点击向右展开成输入框 (自动聚焦弹系统键盘,
 * IME 确认发送后收起, 返回键收起 —— 与搜索页输入框同套路).
 */
@Composable
internal fun TvDanmakuSendEntry(
    overlay: TvPlayerOverlayState,
    danmakuEditorState: DanmakuEditorState,
    vm: EpisodeViewModel,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val expanded = overlay.danmakuInputExpanded
    val fieldFocusRequester = remember { FocusRequester() }
    val buttonFocusRequester = remember { FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var fieldFocused by remember { mutableStateOf(false) }
    var everExpanded by remember { mutableStateOf(false) }
    val isSending by danmakuEditorState.isSending.collectAsStateWithLifecycle()

    // 展开: 轮询聚焦输入框 + 弹键盘; 收起: 焦点还给圆钮
    LaunchedEffect(expanded) {
        if (expanded) {
            everExpanded = true
            if (resolveFocusRepeatedly(attempts = 20, arrived = { fieldFocused }) {
                    runCatching { fieldFocusRequester.requestFocus() }
                }
            ) {
                keyboard?.show()
            }
        } else if (everExpanded) {
            keyboard?.hide()
            // 圆钮是常驻焦点目标, 请求器一附着 (不抛异常) 即视为到位;
            // 控制层已隐藏则放弃 (焦点归属由根路由处理)
            var requested = false
            resolveFocusRepeatedly(
                attempts = 20,
                arrived = { requested || overlay.layer != TvPlayerLayer.CONTROLS },
            ) {
                if (overlay.layer == TvPlayerLayer.CONTROLS) {
                    runCatching { buttonFocusRequester.requestFocus() }.onSuccess { requested = true }
                }
            }
        }
    }

    val send: () -> Unit = send@{
        val text = danmakuEditorState.text.trim()
        if (text.isEmpty() || isSending) return@send
        scope.launch {
            danmakuEditorState.post(
                DanmakuContent(
                    vm.player.getCurrentPositionMillis(),
                    text = text,
                    color = Color.White.toArgb(),
                    location = DanmakuLocation.NORMAL,
                ),
            )
        }
        overlay.danmakuInputExpanded = false
    }

    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Surface(
        onClick = { if (!expanded) overlay.danmakuInputExpanded = true },
        modifier = modifier
            .focusRequester(buttonFocusRequester)
            // 本按钮与面板胶囊同在 PILLS 区域, 但它不是面板触发器: 聚焦到它时收起浮出的
            // 面板 (面板只在焦点区域变成进度条/图标行时才自动清, 从"评论"胶囊右移过来
            // 区域不变, 不收就一直挂着)
            .onFocusChanged { if (it.hasFocus) overlay.activePanel = null },
        shape = CircleShape,
        color = if (focused && !expanded) Color.White else Color.White.copy(alpha = 0.14f),
        contentColor = if (focused && !expanded) Color.Black else Color.White,
        interactionSource = interactionSource,
    ) {
        Row(
            Modifier.padding(horizontal = TV_PILL_PADDING_H, vertical = TV_PILL_PADDING_V),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Edit, null, Modifier.size(TV_PILL_ICON_SIZE))
            if (!expanded) {
                Text(
                    stringResource(Lang.episode_send_danmaku),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            } else {
                BasicTextField(
                    value = danmakuEditorState.text,
                    onValueChange = { danmakuEditorState.text = it },
                    modifier = Modifier
                        .width(240.dp)
                        .focusRequester(fieldFocusRequester)
                        .onFocusChanged {
                            fieldFocused = it.isFocused
                            if (it.isFocused) overlay.focusRegion = TvPlayerFocusRegion.PILLS
                        },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    ),
                    cursorBrush = SolidColor(Color.White),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                )
            }
        }
    }
}
