/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.subject.details.sections

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import me.him188.ani.app.ui.foundation.AsyncImage
import me.him188.ani.app.ui.foundation.LocalPlatform
import me.him188.ani.app.ui.foundation.isTv
import me.him188.ani.app.ui.lang.Lang
import me.him188.ani.app.ui.lang.subject_details_episodes
import me.him188.ani.app.ui.lang.subject_details_next_page
import me.him188.ani.app.ui.lang.subject_details_prev_page
import me.him188.ani.app.ui.lang.subject_episode_cache
import me.him188.ani.app.ui.lang.subject_episode_duration_minutes
import me.him188.ani.app.ui.lang.subject_episode_mark_watched
import me.him188.ani.app.ui.lang.subject_episode_unwatch
import me.him188.ani.app.ui.subject.details.components.EpisodePaging
import me.him188.ani.app.ui.subject.episode.list.EpisodeListItem
import me.him188.ani.datasources.api.PackedDate
import me.him188.ani.datasources.api.topic.UnifiedCollectionType
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * 单个剧集网格单元 (对应 Figma `EpisodeGridItem`).
 *
 * 着色规则见 `docs/subject-details-rewrite/01-decision-algorithms.md` §3:
 * - 容器: 播放中→primaryContainer, 已看(DONE/DROPPED)→surfaceContainerLow, 未看→surfaceContainerHigh
 * - 集号: 播放中→primary, 已看→onSurfaceVariant@60%, 未看→onSurface(LocalContentColor)
 * - 集名: 已看→onSurfaceVariant@60%, 未看→onSurfaceVariant
 */
@Composable
fun EpisodeGridCell(
    item: EpisodeListItem,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 72.dp,
) {
    val isWatched = item.isDoneOrDropped
    val containerColor = when {
        isPlaying -> MaterialTheme.colorScheme.primaryContainer
        isWatched -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val dimmed = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val sortColor = when {
        isPlaying -> MaterialTheme.colorScheme.primary
        isWatched -> dimmed
        else -> LocalContentColor.current
    }
    val nameColor = if (isWatched) dimmed else MaterialTheme.colorScheme.onSurfaceVariant

    // 聚焦 (遥控器/键盘) 时集名跑马灯滚动展示全文, 未聚焦时保持省略号
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Surface(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        interactionSource = interactionSource,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (isPlaying) {
                    Icon(
                        rememberVectorPainter(Icons.Rounded.GraphicEq),
                        contentDescription = null,
                        Modifier.height(16.dp).width(16.dp),
                        tint = sortColor,
                    )
                }
                Text(
                    item.sort.toString(),
                    color = sortColor,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                item.nameCn.ifBlank { item.name },
                if (focused) Modifier.basicMarquee(iterations = Int.MAX_VALUE) else Modifier,
                color = nameColor,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = if (focused) TextOverflow.Clip else TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 桌面 (双栏/三栏) 选集网格: 每页最多 2 行, 每行列数随列宽动态计算.
 * 初始页为含 [currentEpisodeId] 的页.
 *
 * [header] 的 `pager` 参数为分页控件: 超过一页时非 null (定稿: 分页控件替代 header 集数文案),
 * 不足一页时为 null, 调用方应回退显示集数文案.
 */
@Composable
fun PagedEpisodesGrid(
    episodes: List<EpisodeListItem>,
    currentEpisodeId: Int?,
    onEpisodeClick: (EpisodeListItem) -> Unit,
    modifier: Modifier = Modifier,
    cellMinWidth: Dp = 96.dp,
    cellSpacing: Dp = 12.dp,
    rowsPerPage: Int = 2,
    header: @Composable (pager: (@Composable () -> Unit)?) -> Unit = { it?.invoke() },
) {
    BoxWithConstraints(modifier) {
        val columns = remember(maxWidth, cellMinWidth, cellSpacing) {
            (((maxWidth + cellSpacing) / (cellMinWidth + cellSpacing)).toInt()).coerceAtLeast(1)
        }
        val capacity = (columns * rowsPerPage).coerceAtLeast(1)
        val paging = remember(episodes.size, capacity) { EpisodePaging(episodes.size, capacity) }
        val currentIndex = remember(episodes, currentEpisodeId) {
            if (currentEpisodeId == null) -1 else episodes.indexOfFirst { it.episodeId == currentEpisodeId }
        }
        var page by remember(paging, currentIndex) { mutableStateOf(paging.initialPage(currentIndex)) }
        val range = paging.itemRange(page)

        // TV: 翻页会把整页格子销毁重建, 持有焦点的格子消失后焦点会被重置到页面左上角 (翻到边界页时
        // 翻页按钮变 disabled 也会丢焦点). 这里实现遥控器的自然翻页:
        //  - 网格最后一行按"下" -> 下一页并聚焦新页第一格; 第一行按"上" -> 上一页并聚焦末格;
        //  - 用翻页按钮翻到边界页 (按钮即将 disabled) 时, 把焦点移到格子上, 避免焦点悬空.
        val isTv = LocalPlatform.current.isTv()
        val firstCellFocus = remember { FocusRequester() }
        val lastCellFocus = remember { FocusRequester() }
        // 0 = 不移动焦点; 1 = 翻页后聚焦第一格; 2 = 翻页后聚焦最后一格
        var pendingFocus by remember { mutableStateOf(0) }
        LaunchedEffect(page, pendingFocus) {
            if (pendingFocus == 0) return@LaunchedEffect
            withFrameNanos { } // 等新页格子布局完成
            runCatching {
                (if (pendingFocus == 1) firstCellFocus else lastCellFocus).requestFocus()
            }
            pendingFocus = 0
        }

        Column(verticalArrangement = Arrangement.spacedBy(cellSpacing)) {
            header(
                if (paging.isPaged) {
                    {
                        EpisodePager(
                            page = page,
                            pageCount = paging.pageCount,
                            range = range.first + 1..range.last + 1,
                            total = episodes.size,
                            onPrev = {
                                if (page > 0) {
                                    // 翻到第一页后按钮将变 disabled, 焦点会悬空, 移到格子上
                                    if (isTv && page - 1 == 0) pendingFocus = 1
                                    page--
                                }
                            },
                            onNext = {
                                if (page < paging.pageCount - 1) {
                                    if (isTv && page + 1 == paging.pageCount - 1) pendingFocus = 1
                                    page++
                                }
                            },
                        )
                    }
                } else {
                    null
                },
            )
            val pageItems = episodes.subList(range.first.coerceIn(0, episodes.size), (range.last + 1).coerceIn(0, episodes.size))
            // 竖向按行排布, 每行 columns 个
            val rows = pageItems.chunked(columns)
            for ((rowIndex, row) in rows.withIndex()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(cellSpacing),
                ) {
                    for ((colIndex, item) in row.withIndex()) {
                        val isFirstCell = rowIndex == 0 && colIndex == 0
                        val isLastCell = rowIndex == rows.lastIndex && colIndex == row.lastIndex
                        EpisodeGridCell(
                            item,
                            isPlaying = item.episodeId == currentEpisodeId,
                            onClick = { onEpisodeClick(item) },
                            modifier = Modifier.weight(1f)
                                .then(if (isFirstCell) Modifier.focusRequester(firstCellFocus) else Modifier)
                                .then(if (isLastCell) Modifier.focusRequester(lastCellFocus) else Modifier)
                                .then(
                                    if (!isTv || !paging.isPaged) Modifier
                                    else Modifier.onPreviewKeyEvent { event ->
                                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                        when {
                                            event.key == Key.DirectionDown &&
                                                    rowIndex == rows.lastIndex && page < paging.pageCount - 1 -> {
                                                pendingFocus = 1
                                                page++
                                                true
                                            }

                                            event.key == Key.DirectionUp &&
                                                    rowIndex == 0 && page > 0 -> {
                                                pendingFocus = 2
                                                page--
                                                true
                                            }

                                            else -> false
                                        }
                                    },
                                ),
                        )
                    }
                    // 补齐末行空位, 保持等宽
                    repeat(columns - row.size) {
                        Box(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodePager(
    page: Int,
    pageCount: Int,
    range: IntRange,
    total: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onPrev, enabled = page > 0) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = stringResource(Lang.subject_details_prev_page),
            )
        }
        Text(
            "${range.first} – ${range.last} / $total",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onNext, enabled = page < pageCount - 1) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(Lang.subject_details_next_page),
            )
        }
    }
}

/**
 * 手机 (compact) 选集: LazyRow 横滑不分页, 自动滚至当前集.
 *
 * 单元尺寸对齐 Figma `EpisodeCard` (1594:1024): 高 64, 约 16:10.
 */
@Composable
fun EpisodesRow(
    episodes: List<EpisodeListItem>,
    currentEpisodeId: Int?,
    onEpisodeClick: (EpisodeListItem) -> Unit,
    modifier: Modifier = Modifier,
    cellWidth: Dp = 104.dp,
    cellHeight: Dp = 64.dp,
    cellSpacing: Dp = 10.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    val listState = rememberLazyListState()
    val currentIndex = remember(episodes, currentEpisodeId) {
        if (currentEpisodeId == null) -1 else episodes.indexOfFirst { it.episodeId == currentEpisodeId }
    }
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) listState.scrollToItem(currentIndex)
    }
    LazyRow(
        modifier,
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(cellSpacing),
        contentPadding = contentPadding,
    ) {
        items(episodes, key = { it.episodeId }) { item ->
            EpisodeGridCell(
                item,
                isPlaying = item.episodeId == currentEpisodeId,
                onClick = { onEpisodeClick(item) },
                modifier = Modifier.width(cellWidth),
                height = cellHeight,
            )
        }
    }
}

/**
 * TV 选集: 单行固定锚点轮播 (Prime Video 式) —— 聚焦卡片始终吸附在行首并带外圈高亮,
 * 遥控器左右导航时焦点视觉位置不动, 卡片列表整体平滑滑过. 行上方展示当前聚焦集的
 * "集号. 集名 + 简介". 自动滚至当前集.
 *
 * 未聚焦任何格子时展示 [currentEpisodeId] (当前/下一集) 的信息.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvEpisodeCarousel(
    episodes: List<EpisodeListItem>,
    currentEpisodeId: Int?,
    onEpisodeClick: (EpisodeListItem) -> Unit,
    modifier: Modifier = Modifier,
    /** episodeId -> TMDB 分集缩略图 URL. 无图的集回退纯文字卡, 卡片尺寸不变. */
    episodeStills: Map<Int, String> = emptyMap(),
    /** episodeId -> 播放进度 (0..1), 有记录的集在卡片底部画进度条 (Prime Video 式). */
    playProgress: Map<Int, Float> = emptyMap(),
    /** episodeId -> 分集时长 (分钟, TMDB), 显示在聚焦集信息行右侧; 缺失则不显示. */
    episodeRuntimes: Map<Int, Int> = emptyMap(),
    /** 非 null 时卡片支持长按确认键打开单集操作菜单 (标记看过/取消看过). */
    onSetEpisodeCollectionType: ((EpisodeListItem, UnifiedCollectionType) -> Unit)? = null,
    cellWidth: Dp = 240.dp,
    cellHeight: Dp = 135.dp,
    cellSpacing: Dp = 12.dp,
    header: @Composable () -> Unit = {},
    /**
     * 非 null 时卡片按上键固定聚焦到该目标 (如区块标题行右侧的网格入口按钮).
     * 不指定时空间焦点搜索只考虑与聚焦卡片同列 ("beam" 内) 的候选 —— 聚焦卡片固定在行首最左,
     * 而标题行按钮在最右, 不同列, 于是向上会跳过按钮直接落到更上方 Hero 区的按钮.
     */
    upFocus: FocusRequester? = null,
) {
    var focusedEpisodeId by remember { mutableStateOf<Int?>(null) }

    // 长按卡片打开的单集操作菜单; 关闭后把焦点还给长按的那张卡片
    var actionTarget by remember { mutableStateOf<EpisodeListItem?>(null) }
    var actionRestoreEpisodeId by remember { mutableStateOf<Int?>(null) }
    val actionRestoreFocus = remember { FocusRequester() }
    LaunchedEffect(actionRestoreEpisodeId) {
        if (actionRestoreEpisodeId != null) {
            withFrameNanos { }
            runCatching { actionRestoreFocus.requestFocus() }
            actionRestoreEpisodeId = null
        }
    }
    actionTarget?.let { target ->
        TvEpisodeActionDialog(
            item = target,
            onSetCollectionType = { type ->
                onSetEpisodeCollectionType?.invoke(target, type)
                actionTarget = null
                actionRestoreEpisodeId = target.episodeId
            },
            onDismissRequest = {
                actionTarget = null
                actionRestoreEpisodeId = target.episodeId
            },
        )
    }
    val displayed = episodes.firstOrNull { it.episodeId == (focusedEpisodeId ?: currentEpisodeId) }
        ?: episodes.firstOrNull()

    val listState = rememberLazyListState()
    val currentIndex = remember(episodes, currentEpisodeId) {
        if (currentEpisodeId == null) -1 else episodes.indexOfFirst { it.episodeId == currentEpisodeId }
    }
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) listState.scrollToItem(currentIndex)
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        header()
        if (displayed != null) {
            // 固定高度, 避免聚焦切换 (简介有无/长短不同) 时下方的行上下跳动
            Row(
                Modifier.fillMaxWidth().height(64.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "${displayed.sort}. ${displayed.nameCn.ifBlank { displayed.name }}",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (displayed.desc.isNotBlank()) {
                        Text(
                            displayed.desc,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                // 撑满信息行高度: 时长与标题同线, 日期贴底, 视觉上与左侧标题+简介等高
                TvEpisodeMetaColumn(
                    runtimeMinutes = episodeRuntimes[displayed.episodeId],
                    airDate = displayed.airDate,
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                )
            }
        }
        // 焦点从行外进入时恢复到上次聚焦的卡片 (首次进入落到当前集), 而不是交给空间焦点搜索 ——
        // 否则从右上角的按钮向下导航会命中右缘刚组合出来的卡片, 触发横向 BringIntoView 把整行拉向左
        val restoreFocus = remember { FocusRequester() }
        val fallbackIndex = if (currentIndex >= 0) currentIndex else 0

        // Prime Video 式固定锚点轮播: 聚焦卡片始终吸附在行首, 按左右键时焦点的视觉位置不动,
        // 卡片列表整体平滑滑过 (最后一集也一样 —— 行尾留出整行空白让末卡也能吸附到行首).
        // 为此禁用默认 BringIntoView 的"最小滚动"(它只保证可见, 焦点卡片会在行内游走),
        // 横向滚动改由当前聚焦下标显式驱动.
        var activeFocusEpisodeId by remember { mutableStateOf<Int?>(null) }
        val activeFocusIndex = remember(episodes, activeFocusEpisodeId) {
            activeFocusEpisodeId?.let { id -> episodes.indexOfFirst { it.episodeId == id } } ?: -1
        }
        LaunchedEffect(activeFocusIndex) {
            if (activeFocusIndex >= 0) listState.animateScrollToItem(activeFocusIndex)
        }
        val noBringIntoView = remember {
            object : BringIntoViewSpec {
                override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
            }
        }
        CompositionLocalProvider(LocalBringIntoViewSpec provides noBringIntoView) {
            BoxWithConstraints {
                LazyRow(
                    Modifier.focusRestorer(restoreFocus),
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(cellSpacing),
                    contentPadding = PaddingValues(end = (this.maxWidth - cellWidth).coerceAtLeast(0.dp)),
                ) {
                    itemsIndexed(episodes, key = { _, item -> item.episodeId }) { index, item ->
                        // 外圈高亮 (Prime Video 式): 聚焦时沿卡片圆角紧贴描边, 用主题动态色
                        // (详情页启用封面取色主题, primary 即动态色)
                        val ringFocused = item.episodeId == activeFocusEpisodeId
                        TvEpisodeCard(
                            item,
                            stillUrl = episodeStills[item.episodeId],
                            isPlaying = item.episodeId == currentEpisodeId,
                            onClick = { onEpisodeClick(item) },
                            modifier = Modifier.width(cellWidth)
                                .then(
                                    if (ringFocused) {
                                        Modifier.border(
                                            TV_FOCUS_RING_WIDTH,
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(12.dp),
                                        )
                                    } else Modifier,
                                )
                                .then(if (index == fallbackIndex) Modifier.focusRequester(restoreFocus) else Modifier)
                                .then(
                                    if (item.episodeId == actionRestoreEpisodeId) {
                                        Modifier.focusRequester(actionRestoreFocus)
                                    } else Modifier,
                                )
                                .then(
                                    if (upFocus != null) {
                                        Modifier.focusProperties { up = upFocus }
                                    } else Modifier,
                                )
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        focusedEpisodeId = item.episodeId
                                        activeFocusEpisodeId = item.episodeId
                                    } else if (activeFocusEpisodeId == item.episodeId) {
                                        // 焦点离开整行时熄灭外圈; 行内移动会先聚焦新卡再走到这里, 不受影响
                                        activeFocusEpisodeId = null
                                    }
                                },
                            height = cellHeight,
                            progress = playProgress[item.episodeId],
                            onLongClick = if (onSetEpisodeCollectionType == null) null else {
                                { actionTarget = item }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** TV 选集卡片聚焦时的外圈描边宽度 (紧贴卡片圆角, 画在卡片边缘内侧, 不会被 LazyRow 裁掉). */
private val TV_FOCUS_RING_WIDTH = 3.dp

/**
 * TV 选集卡片 (Prime Video 式大卡): 有 TMDB 分集缩略图时图占满卡片, 集号/集名压在
 * 底部 scrim 上; 无图时回退 [EpisodeGridCell] 的纯文字样式, 尺寸一致. 聚焦时集名跑马灯.
 */
@Composable
fun TvEpisodeCard(
    item: EpisodeListItem,
    stillUrl: String?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 135.dp,
    /** 播放进度 (0..1); 非 null 时卡片底部画进度条 (Prime Video 式). */
    progress: Float? = null,
    /** 非 null 时支持长按确认键 (按住 OK) 触发, 用于打开单集操作菜单. */
    onLongClick: (() -> Unit)? = null,
) {
    val isWatched = item.isDoneOrDropped
    val containerColor = when {
        isPlaying -> MaterialTheme.colorScheme.primaryContainer
        isWatched -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val dimmed = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val sortColor = when {
        isPlaying -> MaterialTheme.colorScheme.primary
        isWatched -> dimmed
        else -> LocalContentColor.current
    }
    val nameColor = if (isWatched) dimmed else MaterialTheme.colorScheme.onSurfaceVariant
    // Prime Video 式: 已看的集固定满条 ("看过"由进度条表达, 图不再压暗); 未看完的显示续播点
    val effectiveProgress = if (isWatched) 1f else progress

    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val name = item.nameCn.ifBlank { item.name }

    // 长按确认键检测: 遥控器按住 OK 时系统连发 KeyDown (repeat), 计数超过阈值视为长按.
    // 完全接管确认键 (down/up 都消费), 由本层在 KeyUp 时分发 click/longClick,
    // 避免与 clickable 的按键处理竞争 (clickable 在 KeyUp 触发 onClick 会与长按撞车).
    var confirmKeyDownCount by remember { mutableStateOf(0) }
    val longPressModifier = if (onLongClick == null) Modifier else Modifier.onPreviewKeyEvent { event ->
        val isConfirmKey = event.key == Key.DirectionCenter ||
            event.key == Key.Enter || event.key == Key.NumPadEnter
        if (!isConfirmKey) return@onPreviewKeyEvent false
        when (event.type) {
            KeyEventType.KeyDown -> {
                confirmKeyDownCount++
                true
            }

            KeyEventType.KeyUp -> {
                val longPressed = confirmKeyDownCount >= LONG_PRESS_CONFIRM_KEY_REPEATS
                confirmKeyDownCount = 0
                if (longPressed) onLongClick() else onClick()
                true
            }

            else -> false
        }
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(height).then(longPressModifier),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        interactionSource = interactionSource,
    ) {
        if (stillUrl != null) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    stillUrl,
                    contentDescription = null,
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                // 底部 scrim 保证集号/集名可读
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0.5f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.85f),
                        ),
                    ),
                )
                Row(
                    Modifier.align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // 聚焦时文字前显示播放三角 (Prime Video 式); 未聚焦的播放中卡片仍显示声浪图标
                    if (focused) {
                        Icon(
                            rememberVectorPainter(Icons.Rounded.PlayArrow),
                            contentDescription = null,
                            Modifier.height(16.dp).width(16.dp),
                            tint = Color.White,
                        )
                    } else if (isPlaying) {
                        Icon(
                            rememberVectorPainter(Icons.Rounded.GraphicEq),
                            contentDescription = null,
                            Modifier.height(14.dp).width(14.dp),
                            tint = Color.White,
                        )
                    }
                    Text(
                        item.sort.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                    )
                    Text(
                        name,
                        if (focused) Modifier.basicMarquee(iterations = Int.MAX_VALUE) else Modifier,
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = if (focused) TextOverflow.Clip else TextOverflow.Ellipsis,
                    )
                }
                TvEpisodeProgressBar(
                    effectiveProgress,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    Modifier.align(Alignment.BottomStart),
                )
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    // 聚焦时文字前显示播放三角 (Prime Video 式); 未聚焦的播放中卡片仍显示声浪图标
                    if (focused) {
                        Icon(
                            rememberVectorPainter(Icons.Rounded.PlayArrow),
                            contentDescription = null,
                            Modifier.height(16.dp).width(16.dp),
                            tint = sortColor,
                        )
                    } else if (isPlaying) {
                        Icon(
                            rememberVectorPainter(Icons.Rounded.GraphicEq),
                            contentDescription = null,
                            Modifier.height(16.dp).width(16.dp),
                            tint = sortColor,
                        )
                    }
                    Text(
                        item.sort.toString(),
                        color = sortColor,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    name,
                    if (focused) Modifier.basicMarquee(iterations = Int.MAX_VALUE) else Modifier,
                    color = nameColor,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = if (focused) TextOverflow.Clip else TextOverflow.Ellipsis,
                )
                }
                TvEpisodeProgressBar(
                    effectiveProgress,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    Modifier.align(Alignment.BottomStart),
                )
            }
        }
    }
}

/**
 * 卡片底部的播放进度条 (Prime Video 式细条). [progress] 为 null 或 0 时不绘制;
 * 已看完的集也会有满条 (进度记录保留到结尾).
 */
@Composable
private fun TvEpisodeProgressBar(
    progress: Float?,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    if (progress == null || progress <= 0f) return
    Box(
        modifier.fillMaxWidth().height(4.dp).background(trackColor),
    ) {
        Box(
            Modifier.fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/**
 * 聚焦集信息行右侧的元数据列: 上行时长 (分钟, 来自 TMDB), 下行播出日期 (Bangumi).
 * 两者都缺失时不占位. 字号与左侧标题块对齐 (时长同标题, 日期略小).
 */
@Composable
private fun TvEpisodeMetaColumn(
    runtimeMinutes: Int?,
    airDate: PackedDate,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(4.dp),
) {
    val dateText = formatAirDate(airDate)
    if (runtimeMinutes == null && dateText == null) return
    Column(
        modifier,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = Alignment.End,
    ) {
        if (runtimeMinutes != null) {
            Text(
                stringResource(Lang.subject_episode_duration_minutes, runtimeMinutes),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
        }
        if (dateText != null) {
            Text(
                dateText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
        }
    }
}

private fun formatAirDate(date: PackedDate): String? {
    if (date == PackedDate.Invalid) return null
    val month = date.month.toString().padStart(2, '0')
    val day = date.day.toString().padStart(2, '0')
    return "${date.year}-$month-$day"
}

/**
 * 长按选集卡片弹出的单集操作菜单: 标记看过 / 取消看过.
 *
 * Bangumi API 本身支持单集想看/看过/抛弃, 但本应用的单集状态同步链路
 * (Ani API `toAniEpisodeCollectionTypeUpdate`) 只区分看过与未看,
 * 与手机版长按数字方块的 toggle 行为一致, 故只提供这两项.
 * 当前状态打勾; 初始焦点落在会改变状态的那一项.
 *
 * 按钮上方展示这一集的完整简介 (轮播信息行只有 2 行省略, 这里是全文入口);
 * 简介极长时限高滚动兜底 (遥控器无法滚动, 但至少不把按钮挤出屏幕).
 */
@Composable
private fun TvEpisodeActionDialog(
    item: EpisodeListItem,
    onSetCollectionType: (UnifiedCollectionType) -> Unit,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val initialFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            withFrameNanos { }
            runCatching { initialFocus.requestFocus() }
        }
        val watched = item.isDoneOrDropped
        BoxWithConstraints {
            // 高度由内容决定, 宽度追到与屏幕同宽高比: 先按基准宽度测内容高, 再用
            // 高 × 屏幕比 反推宽度, 迭代两轮吸收文本重排引起的高度变化.
            // 简介很短/没有时反推宽度会小于基准宽度, 用基准宽度兜底.
            val aspect = constraints.maxWidth.toFloat() / constraints.maxHeight.toFloat()
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Layout(
                    content = {
                        Column(Modifier.padding(vertical = 16.dp)) {
                            Text(
                                "${item.sort}. ${item.nameCn.ifBlank { item.name }}",
                                Modifier.padding(horizontal = 24.dp),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (item.desc.isNotBlank()) {
                                Text(
                                    item.desc,
                                    Modifier.padding(horizontal = 24.dp)
                                        .padding(top = 8.dp)
                                        .heightIn(max = 240.dp)
                                        .verticalScroll(rememberScrollState()),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            Box(Modifier.height(8.dp))
                            TvEpisodeActionRow(
                                label = stringResource(Lang.subject_episode_mark_watched),
                                showCheck = watched,
                                onClick = { onSetCollectionType(UnifiedCollectionType.DONE) },
                                modifier = if (!watched) Modifier.focusRequester(initialFocus) else Modifier,
                            )
                            TvEpisodeActionRow(
                                label = stringResource(Lang.subject_episode_unwatch),
                                showCheck = false,
                                onClick = { onSetCollectionType(UnifiedCollectionType.NOT_COLLECTED) },
                                modifier = if (watched) Modifier.focusRequester(initialFocus) else Modifier,
                            )
                        }
                    },
                ) { measurables, cs ->
                    val measurable = measurables.first()
                    val baseWidth = 400.dp.roundToPx()
                    var width = baseWidth
                    repeat(2) {
                        val h = measurable.minIntrinsicHeight(width)
                        width = (h * aspect).roundToInt()
                            .coerceIn(baseWidth, (cs.maxWidth * 0.85f).roundToInt())
                    }
                    val placeable = measurable.measure(Constraints(minWidth = width, maxWidth = width))
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
            }
        }
    }
}

@Composable
private fun TvEpisodeActionRow(
    label: String,
    showCheck: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
        Row(
            Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                rememberVectorPainter(Icons.Rounded.Check),
                contentDescription = null,
                Modifier.width(20.dp).height(20.dp),
                tint = if (showCheck) LocalContentColor.current else Color.Transparent,
            )
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** 长按判定: 确认键连发 KeyDown 达到该次数视为长按 (首次 repeat 约在按住 500ms 后). */
private const val LONG_PRESS_CONFIRM_KEY_REPEATS = 2

/**
 * TV 选集快速跳转子菜单: 数字方块一排排的网格 (沿用旧版选集对话框
 * [me.him188.ani.app.ui.subject.episode.list.EpisodeListDialog] 的形态).
 * 轮播是主体, 这里是辅助入口 —— 上千集 (如 ONE PIECE) 时逐格横向导航不现实,
 * 网格配合 D-pad 纵向移动一排跳十几集. 上千集必须懒加载, 用 LazyVerticalGrid
 * 而非旧版的 FlowRow. 打开时自动滚动到当前集并聚焦.
 */
@Composable
fun TvEpisodeGridDialog(
    episodes: List<EpisodeListItem>,
    currentEpisodeId: Int?,
    onEpisodeClick: (EpisodeListItem) -> Unit,
    onDismissRequest: () -> Unit,
    /** episodeId -> 分集时长 (分钟, TMDB), 显示在聚焦集标题右侧; 缺失则不显示. */
    episodeRuntimes: Map<Int, Int> = emptyMap(),
    /** 非 null 时标题行右侧显示缓存入口 (跳转本条目缓存页), 对应旧版选集对话框右上角的下载按钮. */
    onCacheClick: (() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val gridState = rememberLazyGridState()
        val currentFocus = remember { FocusRequester() }
        val currentIndex = remember(episodes, currentEpisodeId) {
            if (currentEpisodeId == null) -1 else episodes.indexOfFirst { it.episodeId == currentEpisodeId }
        }
        // 方块里只有集数, 聚焦集的标题展示在网格上方这一行
        var focusedEpisodeId by remember { mutableStateOf<Int?>(null) }
        val displayed = episodes.firstOrNull { it.episodeId == (focusedEpisodeId ?: currentEpisodeId) }
            ?: episodes.firstOrNull()
        LaunchedEffect(currentIndex) {
            if (currentIndex >= 0) {
                gridState.scrollToItem(currentIndex)
                withFrameNanos { }
                runCatching { currentFocus.requestFocus() }
            }
        }
        Surface(
            Modifier.fillMaxWidth(0.7f).heightIn(max = 480.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(Lang.subject_details_episodes),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (onCacheClick != null) {
                        IconButton(onCacheClick) {
                            Icon(
                                rememberVectorPainter(Icons.Rounded.Download),
                                contentDescription = stringResource(Lang.subject_episode_cache),
                            )
                        }
                    }
                }
                if (displayed != null) {
                    // 固定单行高度, 避免焦点在方块间移动时下方网格上下跳动;
                    // 标题太长时跑马灯滚动 (basicMarquee 不溢出则静止), 右侧时长/播出日期
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${displayed.sort}. ${displayed.nameCn.ifBlank { displayed.name }}",
                            Modifier.weight(1f).basicMarquee(iterations = Int.MAX_VALUE),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                        )
                        TvEpisodeMetaColumn(
                            runtimeMinutes = episodeRuntimes[displayed.episodeId],
                            airDate = displayed.airDate,
                        )
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(56.dp),
                    modifier = Modifier.weight(1f, fill = false),
                    state = gridState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(episodes, key = { _, item -> item.episodeId }) { index, item ->
                        TvEpisodeSortCell(
                            item,
                            isPlaying = item.episodeId == currentEpisodeId,
                            onClick = { onEpisodeClick(item) },
                            modifier = (if (index == currentIndex) Modifier.focusRequester(currentFocus) else Modifier)
                                .onFocusChanged { if (it.isFocused) focusedEpisodeId = item.episodeId },
                        )
                    }
                }
            }
        }
    }
}

/** [TvEpisodeGridDialog] 里的数字方块. 着色沿用 [EpisodeGridCell] 规则, 未开播的集置灰. */
@Composable
private fun TvEpisodeSortCell(
    item: EpisodeListItem,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isWatched = item.isDoneOrDropped
    val containerColor = when {
        isPlaying -> MaterialTheme.colorScheme.primaryContainer
        isWatched || !item.isBroadcast -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val sortColor = when {
        isPlaying -> MaterialTheme.colorScheme.primary
        !item.isBroadcast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isWatched -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                item.sort.toString(),
                color = sortColor,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
        }
    }
}
