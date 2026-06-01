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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.him188.ani.app.data.network.tmdbStillCardSizeUrl
import me.him188.ani.app.ui.foundation.AsyncImage
import me.him188.ani.app.ui.foundation.tv.tvResolveFocus
import me.him188.ani.app.ui.lang.Lang
import me.him188.ani.app.ui.lang.subject_details_episodes
import me.him188.ani.app.ui.lang.subject_episode_cache
import me.him188.ani.app.ui.lang.subject_episode_duration_minutes
import me.him188.ani.app.ui.lang.subject_episode_mark_watched
import me.him188.ani.app.ui.lang.subject_episode_unwatch
import me.him188.ani.app.ui.subject.episode.list.EpisodeListItem
import me.him188.ani.datasources.api.PackedDate
import me.him188.ani.datasources.api.topic.UnifiedCollectionType
import org.jetbrains.compose.resources.stringResource

/**
 * TV 选集: 单行固定锚点轮播 —— 聚焦卡片始终吸附在行首并带外圈高亮,
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
    /** episodeId -> 播放进度 (0..1), 有记录的集在卡片底部画进度条. */
    playProgress: Map<Int, Float> = emptyMap(),
    /** episodeId -> 分集时长 (分钟, TMDB), 显示在聚焦集信息行右侧; 缺失则不显示. */
    episodeRuntimes: Map<Int, Int> = emptyMap(),
    /** episodeId -> TMDB 中文分集简介; 有则排在 Bangumi 简介 (多为日文) 之前展示. */
    episodeOverviews: Map<Int, String> = emptyMap(),
    /**
     * 聚焦集简介的自定义渲染 (如注入 TV 阅读模式组件); null 时用默认 2 行截断文本.
     * [onHorizontalNav] 供简介组件在聚焦时把左右键转为切换聚焦集 (±1, 到两端无效果),
     * 卡片行同步滑动, 焦点仍留在简介上.
     */
    descContent: (@Composable ColumnScope.(desc: String, onHorizontalNav: (delta: Int) -> Unit) -> Unit)? = null,
    /** 非 null 时卡片支持长按确认键打开单集操作菜单 (标记看过/取消看过). */
    onSetEpisodeCollectionType: ((EpisodeListItem, UnifiedCollectionType) -> Unit)? = null,
    /**
     * 长按菜单开合上报 (播放器选集条用于抑制控制层自动隐藏; 菜单是独立窗口,
     * 按键不经过播放器根路由, 不上报会在菜单开着时被 5 秒计时收掉).
     */
    onActionMenuExpandedChanged: ((Boolean) -> Unit)? = null,
    /**
     * false 时不渲染卡片行上方的聚焦集小标题行与简介行 (播放器选集条的集信息
     * 由调用方放在卡片行下方, 见 [onDisplayedChanged]). 切换不影响卡片行状态.
     */
    showEpisodeInfo: Boolean = true,
    /**
     * 展示中的集 (聚焦卡, 无聚焦时为当前集) 变化时回调 —— 调用方在轮播外自行渲染
     * 集信息 (如播放器选集条在卡片行下方放简介). null 时不回调.
     */
    onDisplayedChanged: ((EpisodeListItem?) -> Unit)? = null,
    /**
     * 聚焦卡片吸附到行首时额外露出的上一张卡片切边宽度 (0 = 不露, 原行为).
     * 首张卡不适用 (左边没有卡).
     */
    focusedCardPeek: Dp = 0.dp,
    cellWidth: Dp = 256.dp,
    cellHeight: Dp = 144.dp,
    cellSpacing: Dp = 16.dp,
    /**
     * 页面级水平留白. 标题行/信息行正常留边; 卡片行只把它作为滚动停靠的 contentPadding,
     * 内容可一直画到容器 (屏幕) 右边缘不被裁 (出血).
     * 为此本组件应以全宽放置, 不要包在带水平 padding 的容器里.
     */
    horizontalPadding: Dp = 0.dp,
    /**
     * 聚焦集小标题行/简介行的右侧留白 (距容器右缘). 选集整页的放大封面向下延伸到这片区域,
     * 传"封面宽 + 间距"可让这些文字与上方大标题/简介共用同一右边界, 不与封面重叠.
     * 默认与 [horizontalPadding] 相同 (左右对称). 卡片行不受影响 (仍全宽出血).
     */
    endPadding: Dp = horizontalPadding,
    /** 可选标题行 (如 "选集" + 连载进度); null 时不渲染 (TV 详情页该位置放聚焦集小标题). */
    header: (@Composable () -> Unit)? = null,
    /**
     * 非 null 时卡片按上键固定聚焦到该目标 (如区块标题行右侧的网格入口按钮).
     * 不指定时空间焦点搜索只考虑与聚焦卡片同列 ("beam" 内) 的候选 —— 聚焦卡片固定在行首最左,
     * 而标题行按钮在最右, 不同列, 于是向上会跳过按钮直接落到更上方 Hero 区的按钮.
     */
    upFocus: FocusRequester? = null,
    /**
     * 非 null 时卡片按下键固定聚焦到该目标 (如选集页之下的区块). 跨区块向下的空间焦点
     * 搜索隔着大段不可聚焦内容 (作品信息表) 时找不到目标, 需显式指路.
     */
    downFocus: FocusRequester? = null,
    /** 非 null 时滚动到该集卡片并聚焦 (如选集网格菜单关闭后跳到菜单里聚焦的集), 完成后回调 [onRevealConsumed]. */
    revealEpisodeId: Int? = null,
    onRevealConsumed: () -> Unit = {},
    /**
     * 非 null 时挂在卡片行 (LazyRow) 上: 调用方对它 requestFocus 可把焦点送进轮播,
     * focusRestorer 会恢复到上次聚焦的卡片 (首次为当前集). 详情页返回键分层用
     * ("选集之下的区域按返回回到选集卡片").
     */
    rowFocusRequester: FocusRequester? = null,
) {
    var focusedEpisodeId by remember { mutableStateOf<Int?>(null) }
    // 实时聚焦的卡片 (失焦即清空), 区别于 focusedEpisodeId (记录"最后聚焦", 不清空):
    // 焦点断言必须用实时值 —— 用最后聚焦值会在"目标恰好是上次聚焦的卡"时误判已完成
    var activeFocusEpisodeId by remember { mutableStateOf<Int?>(null) }

    // 长按卡片打开的单集操作菜单; 关闭后把焦点还给长按的那张卡片
    var actionTarget by remember { mutableStateOf<EpisodeListItem?>(null) }
    // 菜单开合上报 (开着期间挂起, 关闭/离开组合时自动回报 false)
    if (actionTarget != null && onActionMenuExpandedChanged != null) {
        DisposableEffect(Unit) {
            onActionMenuExpandedChanged(true)
            onDispose { onActionMenuExpandedChanged(false) }
        }
    }
    var actionRestoreEpisodeId by remember { mutableStateOf<Int?>(null) }
    val actionRestoreFocus = remember { FocusRequester() }
    LaunchedEffect(actionRestoreEpisodeId) {
        val target = actionRestoreEpisodeId ?: return@LaunchedEffect
        // 只请求一次不够: 对话框窗口关闭时系统会异步把焦点还给宿主窗口之前的元素,
        // LazyRow 的 focusRestorer 又会把它"恢复"到上一张聚焦卡 (或回退卡), 与这里的
        // 显式聚焦竞争, 谁后执行谁生效 (表现为随机跳错集). 另外 scrollToItem 后目标卡
        // 可能还没组合完成, 首次请求会落空.
        tvResolveFocus(attempts = 20, delayMillis = 0, arrived = { activeFocusEpisodeId == target }) {
            runCatching { actionRestoreFocus.requestFocus() }
        }
        actionRestoreEpisodeId = null
    }
    val displayed = episodes.firstOrNull { it.episodeId == (focusedEpisodeId ?: currentEpisodeId) }
        ?: episodes.firstOrNull()
    if (onDisplayedChanged != null) {
        LaunchedEffect(displayed) { onDisplayedChanged(displayed) }
    }

    val currentIndex = remember(episodes, currentEpisodeId) {
        if (currentEpisodeId == null) -1 else episodes.indexOfFirst { it.episodeId == currentEpisodeId }
    }
    // 初始滚动位置直接建在当前集 (而非从 0 起再靠效应滚动): 播放器选集条每次展开
    // 都是全新组合, 若初始在 0, 焦点解析会赶在滚动前落到第一张卡 —— 当前集卡尚未
    // 组合, focusRestorer 的兜底请求器没挂上, 落点就错了. 初始即在当前集,
    // 当前集卡首帧组合, 落焦必中.
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = currentIndex.coerceAtLeast(0),
    )
    // 吸附偏移: 负值让目标卡片停在行首偏右, 左侧露出上一张卡的切边; 首张卡不偏 (左边没有卡)
    val peekPx = with(LocalDensity.current) { focusedCardPeek.roundToPx() }
    val snapOffsetFor: (Int) -> Int = { index -> if (index > 0) -peekPx else 0 }
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) listState.scrollToItem(currentIndex, snapOffsetFor(currentIndex))
    }

    // 焦点停在简介块上时左右键切换聚焦集: 展示的集号/标题/简介随之更新, 卡片行同步
    // 滑动一格 (此时没有卡片真正聚焦, 滚动不能靠 activeFocusEpisodeId 驱动), 到两端无效果
    val scope = rememberCoroutineScope()
    val moveDisplayedBy: (Int) -> Unit = moveDisplayed@{ delta ->
        val displayedId = focusedEpisodeId ?: currentEpisodeId
        val index = episodes.indexOfFirst { it.episodeId == displayedId }.coerceAtLeast(0)
        val target = index + delta
        if (target !in episodes.indices) return@moveDisplayed
        focusedEpisodeId = episodes[target].episodeId
        scope.launch { listState.animateScrollToItem(target, snapOffsetFor(target)) }
    }

    // 跳到指定集 (选集网格菜单关闭后): 先滚动让目标卡片进入组合, 再复用 actionRestore 的下一帧聚焦机制
    // 网格菜单的长按跳转在"按住途中"就触发 (不等松开): 同一次按住残余的确认键事件
    // (后续连发 KeyDown / 最终 KeyUp) 可能落到跳转后聚焦的卡片上, 误触发卡片的长按/点击.
    // 跳转时置起本标志, 卡片吞掉全部确认键直到收到 KeyUp; 松开事件可能根本不会送到
    // 主窗口 (按键手势归属已关闭的菜单窗口), 超时兜底解除.
    var swallowHeldConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(swallowHeldConfirm) {
        if (swallowHeldConfirm) {
            delay(1500)
            swallowHeldConfirm = false
        }
    }

    LaunchedEffect(revealEpisodeId) {
        if (revealEpisodeId != null) {
            val index = episodes.indexOfFirst { it.episodeId == revealEpisodeId }
            if (index >= 0) {
                listState.scrollToItem(index, snapOffsetFor(index))
                actionRestoreEpisodeId = revealEpisodeId
                swallowHeldConfirm = true
            }
            onRevealConsumed()
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (header != null) {
            Box(Modifier.fillMaxWidth().padding(horizontal = horizontalPadding)) {
                header()
            }
        }
        if (displayed != null && showEpisodeInfo) {
            // 聚焦集小标题行 (原"选集"标题行的位置): 标题靠左, 时长/播出日期靠右对齐行尾,
            // 全部按文字基线对齐 (标题与元数据字号不同, 垂直居中会错位);
            // 行宽以 endPadding 收边, 与上方大标题/简介及下方集简介共用同一右边界 (不与封面重叠)
            Row(
                Modifier.fillMaxWidth()
                    .padding(start = horizontalPadding, end = endPadding),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "${displayed.sort}. ${displayed.nameCn.ifBlank { displayed.name }}",
                    Modifier.weight(1f).alignByBaseline(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val runtimeMinutes = episodeRuntimes[displayed.episodeId]
                if (runtimeMinutes != null) {
                    Text(
                        stringResource(Lang.subject_episode_duration_minutes, runtimeMinutes),
                        Modifier.alignByBaseline(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                }
                formatAirDate(displayed.airDate)?.let { dateText ->
                    Text(
                        dateText,
                        Modifier.alignByBaseline(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                }
            }
            // 信息行: 聚焦集简介, 右边界同上.
            // 固定高度避免聚焦切换 (简介有无/长短不同) 时下方跳动; 简介文字顶对齐,
            // 调小此值只会把下方卡片行整体上移, 不影响上方文字. 见 [EPISODE_FOCUSED_DESC_HEIGHT].
            Column(
                Modifier.fillMaxWidth()
                    .padding(start = horizontalPadding, end = endPadding)
                    .height(EPISODE_FOCUSED_DESC_HEIGHT),
            ) {
                // TMDB 中文简介优先, Bangumi (多为日文) 跟在后面; 截断态通常只见中文
                val desc = mergedEpisodeDesc(episodeOverviews[displayed.episodeId], displayed.desc)
                if (descContent != null) {
                    // 简介为空也保持组合: 左右键切到无简介的集时可聚焦占位仍在, 焦点不会丢
                    descContent(desc, moveDisplayedBy)
                } else if (desc.isNotBlank()) {
                    Text(
                        desc,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        // 焦点从行外进入时恢复到上次聚焦的卡片 (首次进入落到当前集), 而不是交给空间焦点搜索 ——
        // 否则从右上角的按钮向下导航会命中右缘刚组合出来的卡片, 触发横向 BringIntoView 把整行拉向左.
        // 兜底目标跟随当前展示的集 (而非固定当前集): 在简介上左右切换后上次聚焦卡可能已滚出组合,
        // 此时进入卡片行应落到展示中的集
        val restoreFocus = remember { FocusRequester() }
        val displayedIndex = displayed?.let { d -> episodes.indexOfFirst { it.episodeId == d.episodeId } } ?: -1
        val fallbackIndex = if (displayedIndex >= 0) displayedIndex else 0

        // 固定锚点轮播: 聚焦卡片始终吸附在行首, 按左右键时焦点的视觉位置不动,
        // 卡片列表整体平滑滑过 (最后一集也一样 —— 行尾留出整行空白让末卡也能吸附到行首).
        // 为此禁用默认 BringIntoView 的"最小滚动"(它只保证可见, 焦点卡片会在行内游走),
        // 横向滚动改由当前聚焦下标显式驱动 (activeFocusEpisodeId 声明在函数开头).
        val activeFocusIndex = remember(episodes, activeFocusEpisodeId) {
            activeFocusEpisodeId?.let { id -> episodes.indexOfFirst { it.episodeId == id } } ?: -1
        }
        LaunchedEffect(activeFocusIndex) {
            if (activeFocusIndex >= 0) {
                listState.animateScrollToItem(activeFocusIndex, snapOffsetFor(activeFocusIndex))
            }
        }
        val noBringIntoView = remember {
            object : BringIntoViewSpec {
                override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
            }
        }
        CompositionLocalProvider(LocalBringIntoViewSpec provides noBringIntoView) {
            BoxWithConstraints {
                LazyRow(
                    Modifier
                        .then(rowFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                        .focusRestorer(restoreFocus),
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(cellSpacing),
                    // start: 停靠时聚焦卡片与其他区块左对齐; end: 行尾留出整行空白让末卡也能
                    // 吸附到行首. 都只是滚动停靠位, 卡片仍可画到全宽容器右边缘 (出血)
                    contentPadding = PaddingValues(
                        start = horizontalPadding,
                        end = (this.maxWidth - horizontalPadding - cellWidth).coerceAtLeast(0.dp),
                    ),
                ) {
                    itemsIndexed(episodes, key = { _, item -> item.episodeId }) { index, item ->
                        // 外圈高亮: 聚焦时沿卡片圆角紧贴描边, 用主题动态色
                        // (详情页启用封面取色主题, primary 即动态色)
                        val ringFocused = item.episodeId == activeFocusEpisodeId
                        Box {
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
                                            // 主题动态色渐变 (左上 primary → 右下 secondary)
                                            Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary,
                                                ),
                                            ),
                                            RoundedCornerShape(EPISODE_CARD_CORNER),
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
                                    if (upFocus != null || downFocus != null) {
                                        Modifier.focusProperties {
                                            if (upFocus != null) up = upFocus
                                            if (downFocus != null) down = downFocus
                                        }
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
                            // 网格菜单长按跳转后, 吞掉同一次按住的残余确认键 (松开前不派发)
                            swallowConfirmKeys = swallowHeldConfirm,
                            onConfirmKeysReleased = { swallowHeldConfirm = false },
                        )
                        // 长按弹出的操作菜单: 锚定在卡片下方, 只有 标记看过/取消看过 两项
                        if (onSetEpisodeCollectionType != null) {
                            TvEpisodeActionDropdown(
                                expanded = actionTarget?.episodeId == item.episodeId,
                                watched = item.isDoneOrDropped,
                                onSetCollectionType = { type ->
                                    onSetEpisodeCollectionType.invoke(item, type)
                                    actionTarget = null
                                    actionRestoreEpisodeId = item.episodeId
                                },
                                onDismissRequest = {
                                    actionTarget = null
                                    actionRestoreEpisodeId = item.episodeId
                                },
                            )
                        }
                        }
                    }
                }
            }
        }
    }
}

/** TV 选集卡片聚焦时的外圈描边宽度 (紧贴卡片圆角, 画在卡片边缘内侧, 不会被 LazyRow 裁掉). */
private val TV_FOCUS_RING_WIDTH = 1.5.dp

/**
 * 详情页选集卡片的圆角半径 (网格卡 / TV 卡片本体 / TV 聚焦外圈描边共用, 三者必须一致).
 * 调小让卡片棱角更硬朗, 调大更圆润.
 */
internal val EPISODE_CARD_CORNER = 6.dp

/**
 * TV 轮播里聚焦集简介行的固定高度. 固定是为了聚焦切换 (简介有无/长短不同) 时下方卡片不跳动.
 * 简介文字顶对齐, 下半部分常留白 —— 调小此值只把卡片行整体上移 (远离下方区块), 不影响上方文字.
 */
private val EPISODE_FOCUSED_DESC_HEIGHT = 56.dp

/** TV 详情页弹出菜单容器的不透明度: 半透明, 隐约透出下层内容 (全部菜单统一用此值). */
const val TV_MENU_CONTAINER_ALPHA = 0.95f

/**
 * TV 选集卡片 (大卡): 有 TMDB 分集缩略图时图占满卡片, 集号/集名压在
 * 底部 scrim 上; 无图时回退 [EpisodeGridCell] 的纯文字样式, 尺寸一致. 聚焦时集名跑马灯.
 */
@Composable
fun TvEpisodeCard(
    item: EpisodeListItem,
    stillUrl: String?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 144.dp,
    /** 播放进度 (0..1); 非 null 时卡片底部画进度条. */
    progress: Float? = null,
    /** 非 null 时支持长按确认键 (按住 OK) 触发, 用于打开单集操作菜单. */
    onLongClick: (() -> Unit)? = null,
    /**
     * true 时吞掉全部确认键事件不派发 (网格菜单长按跳转后, 同一次按住的残余事件
     * 会落到本卡片上); 收到 KeyUp (按住结束) 时回调 [onConfirmKeysReleased].
     */
    swallowConfirmKeys: Boolean = false,
    onConfirmKeysReleased: () -> Unit = {},
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
    // 已看的集固定满条 ("看过"由进度条表达, 图不再压暗); 未看完的显示续播点
    val effectiveProgress = if (isWatched) 1f else progress

    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val name = item.nameCn.ifBlank { item.name }

    // 图标尺寸用 sp (跟随字体缩放), 并按矢量内部留白补偿, 使可见图形高度 ≈ 集号数字
    // (titleSmall 14sp) 的大写高度 ~10sp: PlayArrow 三角占 24 视口的 14 (58%) → 17sp;
    // GraphicEq 占 16/24 (67%) → 15sp
    val playIconSize = with(LocalDensity.current) { 17.sp.toDp() }
    val playingIconSize = with(LocalDensity.current) { 15.sp.toDp() }

    // 长按确认键检测: 遥控器按住 OK 时系统连发 KeyDown (repeat), 计数超过阈值视为长按.
    // 完全接管确认键 (down/up 都消费), 由本层在 KeyUp 时分发 click/longClick,
    // 避免与 clickable 的按键处理竞争 (clickable 在 KeyUp 触发 onClick 会与长按撞车).
    var confirmKeyDownCount by remember { mutableStateOf(0) }
    val longPressModifier = if (onLongClick == null) Modifier else Modifier.onPreviewKeyEvent { event ->
        val isConfirmKey = event.key == Key.DirectionCenter ||
            event.key == Key.Enter || event.key == Key.NumPadEnter
        if (!isConfirmKey) return@onPreviewKeyEvent false
        // 残余按住保护: 网格菜单长按跳转后同一次按住尚未松开, 吞掉全部事件不计数不派发
        if (swallowConfirmKeys) {
            if (event.type == KeyEventType.KeyUp) onConfirmKeysReleased()
            confirmKeyDownCount = 0
            return@onPreviewKeyEvent true
        }
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

    // 按住确认键的视觉反馈 (同网格方块): 按住期间轻微缩小, 达到长按阈值后弹回 —
    // "缩下去又弹回来" = 已按住成功, 松开即触发长按操作
    val pressing = onLongClick != null && confirmKeyDownCount in 1 until LONG_PRESS_CONFIRM_KEY_REPEATS
    val pressScale by animateFloatAsState(if (pressing) 0.94f else 1f)
    Surface(
        onClick = onClick,
        // scale 放链最外层: 调用方 modifier 里带聚焦外圈描边, 按住缩小时描边跟着一起缩
        modifier = Modifier.scale(pressScale).then(modifier).height(height).then(longPressModifier),
        shape = RoundedCornerShape(EPISODE_CARD_CORNER),
        color = containerColor,
        interactionSource = interactionSource,
    ) {
        if (stillUrl != null) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    // 卡片不用 original 档原图 (那是给全屏 hero 的), 降到 w780 省下载/解码
                    tmdbStillCardSizeUrl(stillUrl),
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
                        // 底部留出进度条区域 (空隙 4dp + 条 4dp), 避免文字与进度条重叠
                        .padding(horizontal = 10.dp)
                        .padding(top = 8.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // 聚焦时文字前显示播放三角; 未聚焦的播放中卡片仍显示声浪图标
                    if (focused) {
                        Icon(
                            rememberVectorPainter(Icons.Rounded.PlayArrow),
                            contentDescription = null,
                            Modifier.size(playIconSize),
                            tint = Color.White,
                        )
                    } else if (isPlaying) {
                        Icon(
                            rememberVectorPainter(Icons.Rounded.GraphicEq),
                            contentDescription = null,
                            Modifier.size(playingIconSize),
                            tint = Color.White,
                        )
                    }
                    // 集号与集名字号不同 (titleSmall/bodySmall), 用基线对齐而非盒子居中,
                    // 否则行高差会让小字看起来上下飘
                    Text(
                        item.sort.toString(),
                        Modifier.alignByBaseline(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                    )
                    Text(
                        name,
                        Modifier.alignByBaseline()
                            .then(if (focused) Modifier.basicMarquee(iterations = Int.MAX_VALUE) else Modifier),
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
                    // 聚焦时文字前显示播放三角; 未聚焦的播放中卡片仍显示声浪图标
                    if (focused) {
                        Icon(
                            rememberVectorPainter(Icons.Rounded.PlayArrow),
                            contentDescription = null,
                            Modifier.size(playIconSize),
                            tint = sortColor,
                        )
                    } else if (isPlaying) {
                        Icon(
                            rememberVectorPainter(Icons.Rounded.GraphicEq),
                            contentDescription = null,
                            Modifier.size(playingIconSize),
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

/** 展示用分集简介: TMDB 本地化简介 (跟随 APP 语言) 在前, Bangumi 简介 (多为日文) 跟在后面. */
fun mergedEpisodeDesc(tmdbOverview: String?, bangumiDesc: String): String =
    listOfNotNull(
        tmdbOverview?.takeIf { it.isNotBlank() },
        bangumiDesc.takeIf { it.isNotBlank() },
    ).joinToString("\n\n")

/**
 * 卡片底部的播放进度条 (细条). [progress] 为 null 或 0 时不绘制;
 * 已看完的集也会有满条 (进度记录保留到结尾).
 * 与卡片边缘留出间距: 贴死底边会被卡片圆角与聚焦描边裁掉一截.
 */
@Composable
private fun TvEpisodeProgressBar(
    progress: Float?,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    if (progress == null || progress <= 0f) return
    Box(
        modifier
            // 尺寸 (960px 卡 = 240dp): 条厚 8px=2dp,
            // 左右内缩 ~20px=5dp (避开圆角), 离底边一点点空隙
            .padding(horizontal = 10.dp)
            .padding(bottom = 4.dp)
            .fillMaxWidth()
            .height(2.5.dp)
            .clip(CircleShape)
            .background(trackColor),
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
fun TvEpisodeMetaColumn(
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
 * 长按选集卡片弹出的单集操作菜单: 锚定在卡片下方的 DropdownMenu, 只有 标记看过 / 取消看过 两项.
 *
 * Bangumi API 本身支持单集想看/看过/抛弃, 但本应用的单集状态同步链路
 * (Ani API `toAniEpisodeCollectionTypeUpdate`) 只区分看过与未看,
 * 与手机版长按数字方块的 toggle 行为一致, 故只提供这两项.
 * 当前状态打勾; 打开时初始焦点落在会改变状态的那一项.
 * (分集完整简介的入口是信息行简介的阅读模式, 不再放在本菜单里.)
 *
 * 需组合在锚点 (选集卡片) 所在的 Box 内.
 */
@Composable
private fun TvEpisodeActionDropdown(
    expanded: Boolean,
    watched: Boolean,
    onSetCollectionType: (UnifiedCollectionType) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val initialFocus = remember { FocusRequester() }
    // 到位确认: 弹出窗口的焦点分配与 focusRestorer 竞争, 单次请求可能被覆盖 (焦点死区)
    var initialFocused by remember { mutableStateOf(false) }
    val initialFocusModifier = Modifier
        .focusRequester(initialFocus)
        .onFocusChanged { if (it.hasFocus) initialFocused = true }
    LaunchedEffect(expanded) {
        if (expanded) {
            initialFocused = false
            tvResolveFocus(attempts = 20, delayMillis = 0, arrived = { initialFocused }) {
                runCatching { initialFocus.requestFocus() }
            }
        }
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = TV_MENU_CONTAINER_ALPHA),
    ) {
        // 不用 leadingIcon (占位图标会在文字左边留一片空白); 当前状态用尾部对勾表示
        DropdownMenuItem(
            text = { Text(stringResource(Lang.subject_episode_mark_watched)) },
            onClick = { onSetCollectionType(UnifiedCollectionType.DONE) },
            modifier = if (!watched) initialFocusModifier else Modifier,
            trailingIcon = if (!watched) null else {
                {
                    Icon(
                        rememberVectorPainter(Icons.Rounded.Check),
                        contentDescription = null,
                        Modifier.size(20.dp),
                    )
                }
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(Lang.subject_episode_unwatch)) },
            onClick = { onSetCollectionType(UnifiedCollectionType.NOT_COLLECTED) },
            modifier = if (watched) initialFocusModifier else Modifier,
        )
    }
}

/** 长按判定: 确认键连发 KeyDown 达到该次数视为长按 (首次 repeat 约在按住 500ms 后). */
internal const val LONG_PRESS_CONFIRM_KEY_REPEATS = 2

/**
 * TV 选集快速跳转子菜单: 从 Hero "选集"圆钮上方弹出 (信息带贴屏幕底部), 内容为数字方块网格
 * (沿用旧版选集对话框 [me.him188.ani.app.ui.subject.episode.list.EpisodeListDialog] 的形态).
 * 轮播是主体, 这里是辅助入口 —— 上千集 (如 ONE PIECE) 时逐格横向导航不现实,
 * 网格配合 D-pad 纵向移动一排跳十几集. 上千集必须懒加载, 用 LazyVerticalGrid
 * 而非旧版的 FlowRow. 打开时自动滚动到当前集并聚焦.
 *
 * 用裸 [Popup] 而非 material3 DropdownMenu: 后者的内容列带 width(IntrinsicSize.Max),
 * 内在尺寸测量会穿透到 LazyVerticalGrid (SubcomposeLayout 不支持内在测量, 直接崩溃).
 *
 * 需组合在锚点 (入口圆钮) 所在的 Box 内, 菜单弹出位置跟随锚点.
 */
@Composable
fun TvEpisodeGridDropdown(
    expanded: Boolean,
    episodes: List<EpisodeListItem>,
    currentEpisodeId: Int?,
    onEpisodeClick: (EpisodeListItem) -> Unit,
    /** 返回键/点击外部关闭菜单 (不跳转轮播, 调用方把焦点还给入口圆钮). */
    onDismissRequest: () -> Unit,
    /** episodeId -> 分集时长 (分钟, TMDB), 显示在聚焦集标题右侧; 缺失则不显示. */
    episodeRuntimes: Map<Int, Int> = emptyMap(),
    /** 非 null 时标题行右侧显示缓存入口 (跳转本条目缓存页), 对应旧版选集对话框右上角的下载按钮. */
    onCacheClick: (() -> Unit)? = null,
    /** 非 null 时方格支持长按确认键 (按住 OK): 由调用方关闭菜单并让轮播跳到该集. */
    onEpisodeLongClick: ((EpisodeListItem) -> Unit)? = null,
) {
    if (!expanded) return
    // 从入口圆钮上方弹出 (信息带贴屏幕底部, 向下没有空间; 观感对齐收藏按钮的菜单):
    // 菜单底缘在按钮顶缘上方 8dp, 左缘对齐按钮左缘, 越界时收回窗口内
    val density = LocalDensity.current
    val positionProvider = remember(density) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val gap = with(density) { 8.dp.roundToPx() }
                val x = anchorBounds.left
                    .coerceAtMost(windowSize.width - popupContentSize.width)
                    .coerceAtLeast(0)
                val y = (anchorBounds.top - gap - popupContentSize.height).coerceAtLeast(0)
                return IntOffset(x, y)
            }
        }
    }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        // 方块里只有集数, 聚焦集的标题展示在网格上方这一行 (整个内容随关闭离开组合, 状态自动重置)
        var focusedEpisodeId by remember { mutableStateOf<Int?>(null) }
        val gridState = rememberLazyGridState()
        val currentFocus = remember { FocusRequester() }
        val currentIndex = remember(episodes, currentEpisodeId) {
            if (currentEpisodeId == null) -1 else episodes.indexOfFirst { it.episodeId == currentEpisodeId }
        }
        val displayed = episodes.firstOrNull { it.episodeId == (focusedEpisodeId ?: currentEpisodeId) }
            ?: episodes.firstOrNull()
        LaunchedEffect(currentIndex) {
            if (currentIndex >= 0) {
                gridState.scrollToItem(currentIndex)
                // 到位确认 (聚焦格的 onFocusChanged 会置 focusedEpisodeId): 弹出窗口的
                // 异步焦点分配可能覆盖单次请求
                tvResolveFocus(
                    attempts = 20, delayMillis = 0,
                    arrived = { focusedEpisodeId == currentEpisodeId },
                ) {
                    runCatching { currentFocus.requestFocus() }
                }
            }
        }
        // 详情页页面级禁用了 BringIntoView (区块吸附需要), Popup 内容继承了该设置,
        // 会导致网格不跟随焦点滚动 (焦点走出视口后卡在最后一批已组合的格子上).
        // 这里恢复接口默认行为 (最小滚动保持聚焦格可见).
        val defaultBringIntoView = remember { object : BringIntoViewSpec {} }
        CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoView) {
        Surface(
            Modifier.width(560.dp).heightIn(max = 480.dp),
            shape = RoundedCornerShape(16.dp),
            // 半透明容器 (详情页所有弹出菜单统一), 隐约透出下层内容
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = TV_MENU_CONTAINER_ALPHA),
            shadowElevation = 8.dp,
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
                        onLongClick = onEpisodeLongClick?.let { longClick -> { longClick(item) } },
                    )
                }
            }
            }
        }
        }
    }
}

/**
 * [TvEpisodeGridDropdown] 里的数字方块. 着色沿用 [EpisodeGridCell] 规则, 未开播的集置灰.
 * [onLongClick] 非 null 时支持长按确认键 (按住 OK) 触发: 检测方式同 [TvEpisodeCard],
 * 但按住计数一到阈值就立即触发 (不等松开) —— 跳转类操作即时反馈更顺手.
 */
@Composable
private fun TvEpisodeSortCell(
    item: EpisodeListItem,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    var confirmKeyDownCount by remember { mutableStateOf(0) }
    // 长按已在 KeyDown 阶段触发过, 后续 KeyUp 不再派发 click
    var longPressFired by remember { mutableStateOf(false) }
    val longPressModifier = if (onLongClick == null) Modifier else Modifier.onPreviewKeyEvent { event ->
        val isConfirmKey = event.key == Key.DirectionCenter ||
            event.key == Key.Enter || event.key == Key.NumPadEnter
        if (!isConfirmKey) return@onPreviewKeyEvent false
        when (event.type) {
            KeyEventType.KeyDown -> {
                confirmKeyDownCount++
                if (!longPressFired && confirmKeyDownCount >= LONG_PRESS_CONFIRM_KEY_REPEATS) {
                    longPressFired = true
                    onLongClick()
                }
                true
            }

            KeyEventType.KeyUp -> {
                val fired = longPressFired
                longPressFired = false
                confirmKeyDownCount = 0
                if (!fired) onClick()
                true
            }

            else -> false
        }
    }
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
    // 按住确认键的视觉反馈: 按住期间方块轻微缩小; 计数达到长按阈值后恢复原状 —
    // "缩下去又弹回来" = 已按住成功, 松开即触发长按操作
    val pressing = onLongClick != null && confirmKeyDownCount in 1 until LONG_PRESS_CONFIRM_KEY_REPEATS
    val pressScale by animateFloatAsState(if (pressing) 0.88f else 1f)
    Surface(
        onClick = onClick,
        // scale 放链最外层: 调用方 modifier 里可能带描边/底色, 按住缩小时一起缩
        modifier = Modifier.scale(pressScale).then(modifier).height(48.dp).then(longPressModifier),
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
