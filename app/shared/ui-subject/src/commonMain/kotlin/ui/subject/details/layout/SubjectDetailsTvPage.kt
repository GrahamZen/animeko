/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.subject.details.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.paging.compose.collectAsLazyPagingItemsWithLifecycle
import coil3.compose.AsyncImagePainter
import com.kmpalette.color
import com.kmpalette.palette.graphics.Palette
import kotlinx.collections.immutable.toImmutableList
import me.him188.ani.app.data.models.subject.RelatedPersonInfo
import me.him188.ani.app.data.models.subject.SubjectInfo
import me.him188.ani.app.data.models.subject.Tag
import me.him188.ani.app.domain.episode.SetEpisodeCollectionTypeRequest
import me.him188.ani.app.tools.ColorUtils
import me.him188.ani.app.ui.foundation.AsyncImage
import me.him188.ani.app.ui.foundation.text.ProvideContentColor
import me.him188.ani.app.ui.foundation.theme.AniThemeDefaults
import me.him188.ani.app.ui.lang.Lang
import me.him188.ani.app.ui.lang.subject_details_episodes
import me.him188.ani.app.ui.lang.subject_details_info
import me.him188.ani.app.ui.lang.subject_details_login_to_collect
import me.him188.ani.app.ui.lang.subject_details_show_more
import me.him188.ani.app.ui.subject.AiringLabel
import me.him188.ani.app.ui.subject.collection.components.EditableSubjectCollectionTypeButton
import me.him188.ani.app.ui.subject.collection.progress.SubjectProgressButton
import me.him188.ani.app.ui.subject.details.components.AnimatedGradientBackground
import me.him188.ani.app.ui.subject.details.components.COVER_WIDTH_TO_HEIGHT_RATIO
import me.him188.ani.app.ui.subject.details.sections.CharactersSection
import me.him188.ani.app.ui.subject.details.sections.ReviewsPreviewSection
import me.him188.ani.app.ui.subject.details.sections.SectionHeader
import me.him188.ani.app.ui.subject.details.sections.StaffSection
import me.him188.ani.app.ui.subject.details.sections.SubjectCollectionStatsRow
import me.him188.ani.app.ui.subject.details.sections.SubjectInfoTable
import me.him188.ani.app.ui.subject.details.sections.SubjectRatingSummary
import me.him188.ani.app.ui.subject.details.sections.SubjectTagsSection
import me.him188.ani.app.ui.subject.details.sections.TvEpisodeCarousel
import me.him188.ani.app.ui.subject.details.sections.TvEpisodeGridDialog
import me.him188.ani.app.ui.subject.details.state.SubjectDetailsState
import me.him188.ani.app.ui.subject.renderSubjectSeason
import me.him188.ani.app.ui.user.SelfInfoUiState
import org.jetbrains.compose.resources.stringResource

/**
 * TV (10-foot UI) 条目详情页: 单列信息流, 参考主流 TV 流媒体应用 (Prime Video 等) 的结构 —
 * Hero 首屏 (背景封面 + 标题/元数据/简介/主操作) + 各内容区块顺序下排.
 *
 * 与 [SubjectDetailsMultiColumnPage] 内容一致, 仅重排:
 * 原侧栏的作品信息表 / 收藏统计 / 标签下沉到"关联作品"之后的"作品信息"块.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SubjectDetailsTvPage(
    state: SubjectDetailsState,
    selfInfo: SelfInfoUiState,
    layoutParams: SubjectDetailsLayoutParams,
    onPlay: (episodeId: Int) -> Unit,
    onClickTag: (Tag) -> Unit,
    onClickLogin: () -> Unit,
    onShowComments: () -> Unit,
    modifier: Modifier = Modifier,
    onEpisodeCollectionUpdate: (SetEpisodeCollectionTypeRequest) -> Unit = {},
    showTopBar: Boolean = true,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    backgroundPalette: Palette? = null,
    navigationIcon: @Composable () -> Unit = {},
    onClickOpenExternal: () -> Unit = {},
    onCoverImageSuccess: (AsyncImagePainter.State.Success) -> Unit = {},
    onClickCache: (() -> Unit)? = null,
) {
    // info 加载中: 显示 TV 布局自己的加载占位, 不 return 空白 —— 调用方在 TV 上
    // 不等 info 就进入本页 (避免先闪单栏旧布局), 加载通常一瞬.
    val info = state.info
    if (info == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val presentation by state.presentation.collectAsStateWithLifecycle()
    val episodes = presentation.episodeListUiState.mainEpisodes
    val currentEpisodeId = remember(episodes) { episodes.firstOrNull { !it.isDoneOrDropped }?.episodeId }

    val exposedCharacters = state.exposedCharactersPager.collectAsLazyPagingItemsWithLifecycle()
    val allCharacters = state.charactersPager.collectAsLazyPagingItemsWithLifecycle()
    val totalCharactersCount by state.totalCharactersCountState
    val exposedStaff = state.exposedStaffPager.collectAsLazyPagingItemsWithLifecycle()
    val allStaff = state.staffPager.collectAsLazyPagingItemsWithLifecycle()
    val totalStaffCount by state.totalStaffCountState
    val related = state.relatedSubjectsPager.collectAsLazyPagingItemsWithLifecycle()
    val comments = state.subjectCommentState.list.collectAsLazyPagingItemsWithLifecycle()
    val commentCount = state.subjectCommentState.count

    // 水平留白由本页面各区块自理 (Hero 背景图需贴屏幕边缘出血), 不在滚动容器上统一加
    val pad = layoutParams.contentHorizontalPadding
    // TMDB 横版背景图, 三态: 结果未出时 Hero 不放任何图并按"有图"样式排版 (等待,
    // 常见情形图直接淡入零跳变); 确认无图才切换到竖版封面回退. 若直接用 null 当加载中,
    // 每次进页都会先闪一下"无图回退"布局再切到有图, 视觉上像页面跳变.
    var backdropResolved by remember(state) { mutableStateOf(false) }
    var tmdbBackdropUrl by remember(state) { mutableStateOf<String?>(null) }
    LaunchedEffect(state) {
        state.tmdbBackdropUrlFlow.collect {
            tmdbBackdropUrl = it
            backdropResolved = true
        }
    }
    // TMDB 分集缩略图 (episodeId -> URL); 无图的集回退纯文字卡
    val tmdbEpisodeStills by state.tmdbEpisodeStillsFlow.collectAsStateWithLifecycle(emptyMap())
    // 各集播放进度 (episodeId -> 0..1), 选集卡片底部进度条
    val playProgress by state.playProgressFlow.collectAsStateWithLifecycle(emptyMap())
    // TMDB 分集时长 (episodeId -> 分钟), 聚焦集信息行右侧
    val episodeRuntimes by state.tmdbEpisodeRuntimesFlow.collectAsStateWithLifecycle(emptyMap())
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // "显示更多"子页面: 完整简介 + 制作人员 + 作品信息 (主页面只保留 3 行简介)
    var showInfoPage by rememberSaveable { mutableStateOf(false) }
    val summaryFocus = remember { FocusRequester() }
    var restoreSummaryFocus by remember { mutableStateOf(false) }
    if (showInfoPage) {
        TvSubjectInfoDialog(
            info = info,
            backdropUrl = tmdbBackdropUrl,
            mainEpisodeCount = episodes.size.takeIf { it > 0 },
            exposedStaff = exposedStaff,
            allStaff = allStaff,
            totalStaffCount = totalStaffCount,
            staffGridColumns = layoutParams.staffGridColumns,
            onClickTag = onClickTag,
            onDismissRequest = {
                showInfoPage = false
                restoreSummaryFocus = true
            },
        )
    }
    LaunchedEffect(restoreSummaryFocus) {
        if (restoreSummaryFocus) {
            withFrameNanos { }
            runCatching { summaryFocus.requestFocus() }
            restoreSummaryFocus = false
        }
    }

    // 选集快速跳转网格 (辅助入口, 轮播仍是主体): 上千集时逐格横向导航不现实
    var showEpisodeGrid by rememberSaveable { mutableStateOf(false) }
    val episodeGridEntryFocus = remember { FocusRequester() }
    var restoreEpisodeGridEntryFocus by remember { mutableStateOf(false) }
    if (showEpisodeGrid) {
        TvEpisodeGridDialog(
            episodes = episodes,
            currentEpisodeId = currentEpisodeId,
            episodeRuntimes = episodeRuntimes,
            onEpisodeClick = {
                showEpisodeGrid = false
                onPlay(it.episodeId)
            },
            onDismissRequest = {
                showEpisodeGrid = false
                restoreEpisodeGridEntryFocus = true
            },
            onCacheClick = onClickCache,
        )
    }
    LaunchedEffect(restoreEpisodeGridEntryFocus) {
        if (restoreEpisodeGridEntryFocus) {
            withFrameNanos { }
            runCatching { episodeGridEntryFocus.requestFocus() }
            restoreEpisodeGridEntryFocus = false
        }
    }

    BoxWithConstraints(modifier) {
        // Hero 区块占满首屏: 标题在顶, 信息带锚定在画面最底部 (Prime Video 式).
        // 扣除顶栏 (~64dp) 与内容顶部留白, 信息带底缘正好贴屏幕下边界, 下一区块完全在折叠线以下.
        val heroHeight = maxHeight - 64.dp - layoutParams.contentTopPadding - 16.dp
        // 区块吸附位置的基准: 区块列 (pad Column) 在滚动内容中的起始 y
        val sectionsBaseOffsetPx = with(LocalDensity.current) {
            (layoutParams.contentTopPadding + heroHeight + layoutParams.sectionSpacing).toPx()
        }
        // 画面纵向运动全部由"分区吸附"显式驱动 (Prime Video 式): 焦点在 Hero 区 (顶栏/信息带)
        // 内移动画面固定在顶部; 焦点进入某个区块则滚动到该区块顶部. 为此禁用纵向滚动容器的
        // 默认 BringIntoView (否则它与吸附动画互相打架, 造成跳动); 区块列内部重新提供默认
        // spec, 保证选集行等横向 LazyRow 的横向滚动不受影响.
        val defaultBringIntoViewSpec = LocalBringIntoViewSpec.current
        val noBringIntoView = remember {
            object : BringIntoViewSpec {
                override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
            }
        }
        CompositionLocalProvider(LocalBringIntoViewSpec provides noBringIntoView) {
        MultiColumnScaffold(
        layoutParams.copy(contentHorizontalPadding = 0.dp),
        Modifier,
        showTopBar,
        windowInsets,
        navigationIcon = {
            // 焦点落到顶栏按钮时同样滚回页面顶部: 向上导航时焦点可能跳过信息带直接到达顶栏
            // (信息带在视口外时空间导航选最近候选), 若不处理, 画面会停在滚动位置看不到标题
            Box(
                Modifier.onFocusChanged {
                    if (it.hasFocus) scope.launch { scrollState.animateScrollTo(0) }
                },
            ) {
                TvTopBarButtonScrim { navigationIcon() }
            }
        },
        onClickOpenExternal = onClickOpenExternal,
        actionsOverride = {
            TvTopBarButtonScrim {
                IconButton(onClickOpenExternal) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                }
            }
        },
        topBarTitle = info.displayName,
        scrollState = scrollState,
        // TV 无固定标题栏惯例 (Prime/Netflix 均无); 返回等按钮以透明形式常驻浮于内容上
        stickyTopBarEnabled = false,
        backgroundOverlay = {
            val surfaceColor = MaterialTheme.colorScheme.surface
            val colors = remember(backgroundPalette) {
                backgroundPalette?.swatches
                    ?.map { ColorUtils.blendColor(it.color, surfaceColor, 0.85f) }
                    ?.toImmutableList()
            }
            if (colors != null) {
                AnimatedGradientBackground(
                    colors,
                    speed = 0.05,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // 仅在有 TMDB 横版图时铺全屏背景; 无图时 Hero 右侧放竖版封面 (见 TvHeroBlock)
            tmdbBackdropUrl?.let { url ->
                TvHeroBackdrop(
                    imageUrl = url,
                    scrollState = scrollState,
                    onSuccess = onCoverImageSuccess,
                )
            }
        },
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(layoutParams.sectionSpacing),
        ) {
            TvHeroBlock(
                state = state,
                info = info,
                selfInfo = selfInfo,
                onPlay = onPlay,
                onClickLogin = onClickLogin,
                horizontalPadding = pad,
                onShowInfoPage = { showInfoPage = true },
                summaryFocusRequester = summaryFocus,
                // 加载中按"有图"排版 (不显示右侧封面): 大多数条目有 backdrop, 图到了直接淡入;
                // 确认无图才切一次到封面回退, 避免"回退→有图"的两段跳变
                hasBackdrop = tmdbBackdropUrl != null || !backdropResolved,
                onCoverImageSuccess = onCoverImageSuccess,
                // 占满首屏, 信息带贴底
                modifier = Modifier.height(heroHeight)
                    // 焦点回到 Hero 信息带时滚回页面顶部, 否则标题永远滚不回来
                    // (滚动仅由焦点元素的 BringIntoView 驱动, 而标题不可聚焦)
                    .onFocusChanged {
                        if (it.hasFocus) scope.launch { scrollState.animateScrollTo(0) }
                    },
            )
            CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
            Column(
                Modifier.padding(horizontal = pad),
                verticalArrangement = Arrangement.spacedBy(layoutParams.sectionSpacing),
            ) {
            SnapOnFocusSection(scrollState, sectionsBaseOffsetPx) {
            TvEpisodeCarousel(
                episodes = episodes,
                currentEpisodeId = currentEpisodeId,
                onEpisodeClick = { onPlay(it.episodeId) },
                episodeStills = tmdbEpisodeStills,
                playProgress = playProgress,
                episodeRuntimes = episodeRuntimes,
                // 长按卡片: 标记看过/取消看过
                onSetEpisodeCollectionType = { item, type ->
                    onEpisodeCollectionUpdate(
                        SetEpisodeCollectionTypeRequest(state.subjectId, item.episodeId, type),
                    )
                },
                // 卡片按上键固定到标题行右侧的网格入口 (卡片在最左、按钮在最右, 空间搜索够不到)
                upFocus = episodeGridEntryFocus,
                header = {
                    SectionHeader(stringResource(Lang.subject_details_episodes)) {
                        ProvideContentColor(MaterialTheme.colorScheme.onSurfaceVariant) {
                            AiringLabel(
                                state.airingLabelState,
                                style = MaterialTheme.typography.bodyMedium,
                                progressColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        // 选集快速跳转网格入口 (对应旧版选集对话框)
                        IconButton(
                            { showEpisodeGrid = true },
                            Modifier.focusRequester(episodeGridEntryFocus),
                        ) {
                            Icon(
                                Icons.Rounded.GridView,
                                contentDescription = stringResource(Lang.subject_details_episodes),
                            )
                        }
                    }
                },
            )
            }
            SnapOnFocusSection(scrollState, sectionsBaseOffsetPx) {
                CharactersSection(exposedCharacters, allCharacters, totalCharactersCount)
            }
            // 制作人员与作品信息移入"显示更多"子页面, 见 TvSubjectInfoDialog
            if (related.itemCount > 0) {
                SnapOnFocusSection(scrollState, sectionsBaseOffsetPx) {
                    SubjectRelatedBlock(related)
                }
            }
            SnapOnFocusSection(scrollState, sectionsBaseOffsetPx) {
                ReviewsPreviewSection(comments, commentCount, onShowAll = onShowComments)
            }
            }
            }
        }
        }
        }
    }
}

/** 顶栏按钮的暗色胶囊底 + 白色图标: 保证按钮浮在任意亮度的背景图上都清晰可辨. */
@Composable
private fun TvTopBarButtonScrim(content: @Composable () -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 4.dp)
            .background(Color.Black.copy(alpha = 0.4f), CircleShape),
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Row { content() }
        }
    }
}

/**
 * Hero 信息带中间的简介块: 固定 3 行截断, 整块可聚焦/点击, 进入完整信息子页面.
 * 无简介 (连载中/刚公布的条目常见) 时仍保留入口, 仅显示"作品信息"标签.
 */
@Composable
private fun TvSummaryBlock(
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (summary.isNotBlank()) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(Lang.subject_details_show_more),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    stringResource(Lang.subject_details_info),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * "显示更多"全屏子页面: 完整简介 + 制作人员 + 作品信息 (收藏统计/信息表/标签).
 * 遥控器返回键关闭 (Dialog onDismissRequest); 初始焦点在简介文本上, 向下导航
 * 依次经过制作人员/标签, 由 BringIntoView 驱动子页面自身的滚动.
 */
@Composable
private fun TvSubjectInfoDialog(
    info: SubjectInfo,
    backdropUrl: String?,
    mainEpisodeCount: Int?,
    exposedStaff: LazyPagingItems<RelatedPersonInfo>,
    allStaff: LazyPagingItems<RelatedPersonInfo>,
    totalStaffCount: Int?,
    staffGridColumns: Int,
    onClickTag: (Tag) -> Unit,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val summaryFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            withFrameNanos { }
            runCatching { summaryFocus.requestFocus() }
        }
        Surface(Modifier.fillMaxSize(), color = AniThemeDefaults.pageContentBackgroundColor) {
            // 有 backdrop 时铺一层半透明背景图 (固定不随内容滚动), 与主页面 Hero 呼应
            backdropUrl?.let { url ->
                AsyncImage(
                    url,
                    contentDescription = null,
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = TV_INFO_DIALOG_BACKDROP_ALPHA },
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 48.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // 顶部: 左侧标题 + 完整简介, 右侧竖版封面.
                // 简介不截断, 比封面高时自然延伸到封面底部之下 (封面顶对齐, 不拉伸).
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(info.displayName, style = MaterialTheme.typography.headlineSmall)
                        if (info.summary.isNotBlank()) {
                            Text(
                                info.summary,
                                Modifier
                                    .fillMaxWidth()
                                    .focusRequester(summaryFocus)
                                    .focusable(),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    if (info.imageLarge.isNotBlank()) {
                        AsyncImage(
                            info.imageLarge,
                            contentDescription = null,
                            Modifier
                                .width(240.dp)
                                .aspectRatio(COVER_WIDTH_TO_HEIGHT_RATIO)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                StaffSection(
                    exposedStaff,
                    allStaff,
                    totalStaffCount,
                    gridColumns = staffGridColumns,
                )
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(Lang.subject_details_info), style = MaterialTheme.typography.titleMedium)
                    SubjectCollectionStatsRow(info.collectionStats)
                    SubjectInfoTable(info, mainEpisodeCount = mainEpisodeCount)
                    SubjectTagsSection(info.tags, onClickTag)
                }
            }
        }
    }
}

/**
 * 区块级吸附: 焦点进入本区块时, 页面滚动到"区块顶部对齐屏幕上方". 焦点在区块内部
 * 移动时画面完全静止 (本页已禁用纵向 BringIntoView, 见 [SubjectDetailsTvPage]).
 */
@Composable
private fun SnapOnFocusSection(
    scrollState: ScrollState,
    sectionsBaseOffsetPx: Float,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var positionInColumn by remember { mutableFloatStateOf(0f) }
    val snapMarginPx = with(LocalDensity.current) { SECTION_SNAP_TOP_MARGIN.toPx() }
    Box(
        Modifier
            .onGloballyPositioned { positionInColumn = it.positionInParent().y }
            .onFocusChanged {
                if (it.hasFocus) {
                    val target = (sectionsBaseOffsetPx + positionInColumn - snapMarginPx)
                        .roundToInt().coerceAtLeast(0)
                    scope.launch { scrollState.animateScrollTo(target) }
                }
            },
    ) { content() }
}

/**
 * Hero 全屏背景图 (页面背景层, 不随内容滚动): 贴顶/贴右出血, 左缘与底缘渐变入页面背景色,
 * 随滚动淡出以免与滚上来的内容争夺可读性. 对齐 Prime Video 等 TV 应用的沉浸式详情页背景.
 */
@Composable
private fun TvHeroBackdrop(
    imageUrl: String,
    scrollState: ScrollState,
    onSuccess: (AsyncImagePainter.State.Success) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // 向下滚动逐渐淡出, 但保留半透明而非完全消失
                    val progress = (scrollState.value / HERO_BACKDROP_FADE_DISTANCE.toPx()).coerceIn(0f, 1f)
                    alpha = 1f - progress * (1f - HERO_BACKDROP_MIN_ALPHA)
                    // 底部渐隐用 DstOut 擦除本层 alpha, 需要离屏合成
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    // 底部渐隐: 擦除图片自身的透明度, 露出下层的动态渐变背景,
                    // 而不是画一层纯背景色盖住它 (否则浅色主题下是一片突兀的纯白)
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.62f to Color.Transparent,
                            0.98f to Color.Black,
                        ),
                        blendMode = BlendMode.DstOut,
                    )
                },
        ) {
            AsyncImage(
                imageUrl,
                contentDescription = null,
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onSuccess = onSuccess,
            )
            // 左侧暗色 scrim: 保证浮在图上的标题可读 (对齐 Prime Video 的处理)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.6f),
                        0.55f to Color.Transparent,
                    ),
                ),
            )
        }
    }
}

/** 滚动多远后背景图淡到 [HERO_BACKDROP_MIN_ALPHA]. */
private val HERO_BACKDROP_FADE_DISTANCE = 300.dp

/** 向下滚动后背景图保留的透明度 (不完全消失). */
private const val HERO_BACKDROP_MIN_ALPHA = 0.3f

/** 区块吸附后距屏幕顶部的边距. */
private val SECTION_SNAP_TOP_MARGIN = 56.dp

/** "显示更多"子页面背景 backdrop 的透明度: 只作氛围衬底, 不能压过正文可读性. */
private const val TV_INFO_DIALOG_BACKDROP_ALPHA = 0.2f

/** Hero 主操作按钮的圆角: 比 M3 默认胶囊更尖 (Prime Video 式). */
private val TV_BUTTON_SHAPE = RoundedCornerShape(8.dp)

/**
 * Hero 首屏内容 (滚动列内): 标题浮于背景图上 (白色, 图左有暗色 scrim 保证对比);
 * 其余 (元数据 / 评分 / 简介 / 主操作) 下沉到图的底部渐变区自成一段. 背景图见 [TvHeroBackdrop].
 */
@Composable
private fun TvHeroBlock(
    state: SubjectDetailsState,
    info: SubjectInfo,
    selfInfo: SelfInfoUiState,
    onPlay: (episodeId: Int) -> Unit,
    onClickLogin: () -> Unit,
    horizontalPadding: Dp,
    onShowInfoPage: () -> Unit,
    summaryFocusRequester: FocusRequester,
    /** 是否有全屏横版背景图. 无图时标题用主题色, 且在右侧展示竖版封面. */
    hasBackdrop: Boolean,
    onCoverImageSuccess: (AsyncImagePainter.State.Success) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(start = horizontalPadding)) {
        // 上半区: 左 = 标题 (有背景图时白色浮于图上); 右 = 无横版图时的竖版封面,
        // 高度正好撑满 "顶栏按钮之下、信息带之上", 随内容滚出屏幕
        Row(Modifier.weight(1f).fillMaxWidth().padding(end = horizontalPadding)) {
            Column(
                Modifier.weight(1f).padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    info.displayName,
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (hasBackdrop) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (info.name.isNotBlank() && info.name != info.displayName) {
                    Text(
                        info.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasBackdrop) Color.White.copy(alpha = 0.78f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!hasBackdrop && info.summary.isNotBlank()) {
                    // 无横版图时标题下方较空: 简介填进来, 放不下省略;
                    // 完整简介看"作品信息"子页面 (此时信息带入口只显示标签, 不重复文字)
                    Text(
                        info.summary,
                        Modifier.weight(1f, fill = false).padding(top = 8.dp, bottom = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!hasBackdrop) {
                AsyncImage(
                    info.imageLarge,
                    contentDescription = null,
                    Modifier
                        .fillMaxHeight()
                        .padding(bottom = 16.dp)
                        .aspectRatio(COVER_WIDTH_TO_HEIGHT_RATIO)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                    onSuccess = onCoverImageSuccess,
                )
            }
        }

        // 信息带 (参考 Prime Video): 左 = 元数据 + 评分竖排; 中 = 简介; 右 = 主操作按钮竖排
        Row(
            Modifier.fillMaxWidth().padding(end = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column(
                Modifier.width(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 元数据: 季度 · 播出状态
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    Text(
                        renderSubjectSeason(info.airDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "·",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AiringLabel(
                        state.airingLabelState,
                        style = MaterialTheme.typography.bodyMedium,
                        progressColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SubjectRatingSummary(
                    info.ratingInfo,
                    onClick = { state.editableRatingState.requestEdit() },
                )
            }
            Column(Modifier.weight(1f)) {
                // 3 行截断; 点击进入子页面看完整简介/制作人员/作品信息, 不原地展开.
                // 无简介时也保留入口 (子页面还有制作人员/作品信息), 仅显示"作品信息"标签;
                // 无横版图时简介已展示在标题下方, 这里同样只显示入口标签, 不重复文字
                TvSummaryBlock(
                    summary = if (hasBackdrop) info.summary else "",
                    onClick = onShowInfoPage,
                    modifier = Modifier.focusRequester(summaryFocusRequester),
                )
            }
            Column(
                Modifier.width(220.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 更尖的圆角 (Prime Video 式), 非 M3 默认全圆胶囊
                SubjectProgressButton(
                    state.subjectProgressState,
                    onPlay = { state.subjectProgressState.episodeIdToPlay?.let(onPlay) },
                    Modifier.fillMaxWidth(),
                    shape = TV_BUTTON_SHAPE,
                )
                if (selfInfo.isSessionValid == false) {
                    OutlinedButton(onClickLogin, Modifier.fillMaxWidth(), shape = TV_BUTTON_SHAPE) {
                        Text(stringResource(Lang.subject_details_login_to_collect))
                    }
                } else {
                    EditableSubjectCollectionTypeButton(
                        state.editableSubjectCollectionTypeState,
                        Modifier.fillMaxWidth(),
                        shape = TV_BUTTON_SHAPE,
                    )
                }
            }
        }
    }
}
