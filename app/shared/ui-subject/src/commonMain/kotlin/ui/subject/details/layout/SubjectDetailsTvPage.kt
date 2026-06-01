/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.subject.details.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import me.him188.ani.app.ui.foundation.navigation.BackHandler
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.paging.compose.collectAsLazyPagingItemsWithLifecycle
import coil3.compose.AsyncImagePainter
import com.kmpalette.color
import com.kmpalette.palette.graphics.Palette
import kotlinx.collections.immutable.toImmutableList
import me.him188.ani.app.data.models.subject.SubjectCollectionStats
import me.him188.ani.app.data.models.subject.SubjectInfo
import me.him188.ani.app.data.models.subject.Tag
import me.him188.ani.app.domain.episode.SetEpisodeCollectionTypeRequest
import me.him188.ani.app.navigation.LocalNavigator
import me.him188.ani.app.tools.ColorUtils
import me.him188.ani.app.ui.foundation.AsyncImage
import me.him188.ani.app.ui.foundation.session.TvNavigationSideRail
import me.him188.ani.app.ui.foundation.session.buildTvRailItems
import me.him188.ani.app.ui.lang.Lang
import me.him188.ani.app.ui.lang.subject_details_episodes
import me.him188.ani.app.ui.lang.subject_details_info
import me.him188.ani.app.ui.lang.subject_details_login_to_collect
import me.him188.ani.app.ui.lang.subject_details_no_summary
import me.him188.ani.app.ui.lang.subject_details_related_subjects
import me.him188.ani.app.ui.lang.subject_details_show_more
import me.him188.ani.app.ui.lang.subject_details_stat_collected
import me.him188.ani.app.ui.lang.subject_details_stat_watching
import me.him188.ani.app.ui.lang.subject_details_stat_wish
import me.him188.ani.app.ui.subject.AiringLabel
import me.him188.ani.app.ui.subject.SubjectProgressState
import me.him188.ani.app.ui.subject.rememberSubjectStatusStrings
import me.him188.ani.app.ui.subject.collection.components.EditableSubjectCollectionTypeDialogsHost
import me.him188.ani.app.ui.subject.collection.components.EditableSubjectCollectionTypeState
import me.him188.ani.app.ui.subject.collection.components.SubjectCollectionActions
import me.him188.ani.app.ui.subject.collection.components.EditCollectionTypeDropDown
import me.him188.ani.app.ui.subject.collection.components.SubjectCollectionActionsForCollect
import me.him188.ani.app.ui.subject.collection.components.renderCollectionTypeAsCurrent
import me.him188.ani.app.ui.subject.details.components.AnimatedGradientBackground
import me.him188.ani.app.ui.subject.details.components.COVER_WIDTH_TO_HEIGHT_RATIO
import me.him188.ani.app.ui.subject.details.components.RatingHistogram
import me.him188.ani.app.ui.subject.details.components.RelatedSubjectsLazyRow
import me.him188.ani.app.ui.subject.details.components.rememberNavigateToRelatedSubject
import me.him188.ani.app.ui.subject.details.sections.CharactersSection
import me.him188.ani.app.ui.subject.details.sections.ReviewsPreviewSection
import me.him188.ani.app.ui.subject.details.sections.SectionHeader
import me.him188.ani.app.ui.subject.details.sections.StaffSection
import me.him188.ani.app.ui.subject.details.sections.SubjectInfoTable
import me.him188.ani.app.ui.subject.details.sections.groupThousands
import me.him188.ani.app.ui.subject.details.sections.SubjectRatingSummary
import me.him188.ani.app.ui.subject.details.sections.LONG_PRESS_CONFIRM_KEY_REPEATS
import me.him188.ani.app.ui.subject.details.sections.TV_MENU_CONTAINER_ALPHA
import me.him188.ani.app.ui.subject.details.sections.TvEpisodeCarousel
import me.him188.ani.app.ui.subject.details.sections.TvEpisodeGridDropdown
import me.him188.ani.app.ui.subject.details.state.SubjectDetailsState
import me.him188.ani.app.ui.subject.renderSubjectSeason
import me.him188.ani.app.ui.user.SelfInfoUiState
import me.him188.ani.datasources.api.topic.UnifiedCollectionType
import org.jetbrains.compose.resources.stringResource

/**
 * TV (10-foot UI) 条目详情页: 单列信息流, 参考主流 TV 流媒体应用的结构 —
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
    onClickOpenExternal: () -> Unit = {},
    onCoverImageSuccess: (AsyncImagePainter.State.Success) -> Unit = {},
    onClickCache: (() -> Unit)? = null,
) {
    // 页面间过渡由 TV 专用导航转场承担 (NavigationMotionScheme.calculateTv, 同步 crossfade):
    // 滚动归零/焦点落位等状态恢复发生在入场淡入的头几帧, 无可见闪动. 页内不再叠加渐显
    // (两层透明度相乘会让入场页中途露出底色).

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
    // TMDB 本地化分集简介 (episodeId -> 简介), 排在 Bangumi 简介前展示
    val episodeOverviews by state.tmdbEpisodeOverviewsFlow.collectAsStateWithLifecycle(emptyMap())
    // Bangumi 简介整段无中文 (全日文/纯英文) 时用 TMDB 中文整部简介替换; null = 用原文
    val tmdbSummaryOverride by state.tmdbSummaryOverrideFlow.collectAsStateWithLifecycle(null)
    // Ani 服务器简介为空时的 bgm.tv 兜底 (null = 结果未出, "" = bgm 也没有, 非空 = bgm 简介)
    val bangumiSummaryFallback by state.bangumiSummaryFallbackFlow.collectAsStateWithLifecycle(null)
    // 简介优先级: Ani 服务器 > bgm.tv (仅替代不合并) > TMDB 中文.
    // Ani 有简介时维持原逻辑 (全外文则被 TMDB 中文替换); Ani 为空时等 bgm.tv 结果 (未出结果先按
    // 空显示, 避免先闪 TMDB 再换成 bgm), bgm.tv 也没有才用 TMDB 中文兜底.
    val displaySummary = if (info.summary.isNotBlank()) {
        tmdbSummaryOverride ?: info.summary
    } else when (bangumiSummaryFallback) {
        null -> ""
        "" -> tmdbSummaryOverride.orEmpty()
        else -> bangumiSummaryFallback.orEmpty()
    }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Hero 标签墙的状态: rememberSaveable 跨"点击标签→搜索→返回"保留 —
    // 返回本页时浏览模式不变, 焦点直接恢复到最后聚焦的那个标签上 (restorePending 标记)
    var tagsBrowseMode by rememberSaveable { mutableStateOf(false) }
    var focusedTagIndex by rememberSaveable { mutableStateOf(-1) }
    var tagsRestorePending by rememberSaveable { mutableStateOf(false) }

    // 进入页面时初始焦点给 Hero 区的播放按钮.
    // 过去初始焦点由左上角返回按钮提供, 该按钮在 TV 上已移除.
    // 例外: 从标签跳转的搜索页返回时, 焦点由标签墙自行恢复到标签上, 这里不抢.
    val heroPrimaryFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (tagsRestorePending && focusedTagIndex >= 0) return@LaunchedEffect
        // 返回本页 (关联条目跳转返回/播放页退出) 时滚动位置会恢复到离开时的区块,
        // 与"初始焦点在海报页"不一致 —— 先显示旧位置再跳回, 有一次可见的回跳运动.
        // 统一为"重新进入直接落在海报页": 无动画瞬时归零, 首帧即海报页.
        scrollState.scrollTo(0)
        withFrameNanos { }
        runCatching { heroPrimaryFocus.requestFocus() }
    }

    // 返回键分层, 三级: 选集页之下的区域 (角色/制作人员/关联条目...)
    // 按返回先回到选集卡片; 选集页内按返回回到最顶上的海报页 (焦点回播放按钮);
    // 海报页再按返回才真正退出详情页. 纵向滚动均由聚焦驱动 (SnapOnFocusSection 吸附).
    // 弹窗/阅读模式等自行消费返回键的场景优先级更高, 不会走到这里.
    var episodesRegionFocused by remember { mutableStateOf(false) }
    val episodesCarouselFocus = remember { FocusRequester() }
    val episodesSummaryFocus = remember { FocusRequester() }
    BackHandler(enabled = scrollState.value > 0) {
        if (episodesRegionFocused) {
            scope.launch { scrollState.animateScrollTo(0) }
            runCatching { heroPrimaryFocus.requestFocus() }
        } else {
            // 聚焦轮播行 (focusRestorer 恢复到上次聚焦的卡片), 选集页随焦点吸附滚入.
            // 无分集时 (未开播条目) 轮播没有可聚焦的卡片, 退而聚焦简介块
            // ("暂无信息"兜底保证它恒可聚焦), 返回键不至于无效
            runCatching {
                if (episodes.isEmpty()) episodesSummaryFocus.requestFocus()
                else episodesCarouselFocus.requestFocus()
            }
        }
    }

    // 选集快速跳转网格 (辅助入口, 轮播仍是主体): 上千集时逐格横向导航不现实
    var showEpisodeGrid by rememberSaveable { mutableStateOf(false) }
    val episodeGridEntryFocus = remember { FocusRequester() }
    var restoreEpisodeGridEntryFocus by remember { mutableStateOf(false) }
    // 网格菜单关闭后轮播要跳到的集 (菜单里最后聚焦的那格)
    var revealEpisodeId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(restoreEpisodeGridEntryFocus) {
        if (restoreEpisodeGridEntryFocus) {
            withFrameNanos { }
            runCatching { episodeGridEntryFocus.requestFocus() }
            restoreEpisodeGridEntryFocus = false
        }
    }

    BoxWithConstraints(modifier) {
        // TV 上不渲染顶栏 (原本只为返回/主页/外链按钮而设, 已全部移除), 滚动内容直达屏幕上边缘.
        // 顶部留白 ≈ 内容左侧留白 (区块统一的水平留白, 海报页与下方区块左边界对齐),
        // 名义值要再减去标题上方的附加空白 (标题列自带 top 8dp + headlineLarge 行高
        // 顶部内衬约 12dp): 左边距贴的是字形左缘, 顶部也要贴字形上缘才对等.
        val contentTopPad = (pad - TV_HERO_TITLE_TOP_TRIM).coerceAtLeast(0.dp)
        // Hero 区块占满首屏: 标题在顶, 信息带锚定在画面最底部.
        // 信息带底缘正好贴屏幕下边界, 下一区块完全在折叠线以下.
        val heroHeight = maxHeight - contentTopPad - 16.dp
        // 区块吸附位置不再解析推算: SnapOnFocusSection 内部实测区块在滚动内容中的位置
        // (屏幕位置 + 已滚距离), 脚手架顶部留白/insets 等全部自动包含, 无固定偏差.
        // 选集区自成完整一屏 (标题+简介+封面+轮播): 吸附后整页占满屏幕,
        // 上下各留 EPISODES_PAGE_VERTICAL_MARGIN 的空隙.
        val episodesPageHeight = maxHeight - EPISODES_PAGE_VERTICAL_MARGIN * 2
        // 画面纵向运动全部由"分区吸附"显式驱动: 焦点在 Hero 区 (顶栏/信息带)
        // 内移动画面固定在顶部; 焦点进入某个区块则滚动到该区块顶部. 为此禁用纵向滚动容器的
        // 默认 BringIntoView (否则它与吸附动画互相打架, 造成跳动); 区块列内部重新提供默认
        // spec, 保证选集行等横向 LazyRow 的横向滚动不受影响.
        val defaultBringIntoViewSpec = LocalBringIntoViewSpec.current
        val noBringIntoView = remember {
            object : BringIntoViewSpec {
                override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
            }
        }
        // 左缘 overlay 导航栏 (zIndex 置顶): 仅在横屏海报首屏 (未下滑) 显示.
        // 收起态是贴左缘的一列纯图标; 焦点从 Hero 左列按钮按左进入后展开为图标+文字,
        // 并从左侧压一层渐变遮罩盖住海报页. 用途: 详情页可经关联条目无限嵌套,
        // 这里提供一键回主页的逃生通道 (返回键只逐层退). 图标/文字尺寸对齐主页导航栏.
        if (scrollState.value == 0) {
            // 遮罩颜色用主题色 (surface 向 surfaceTint 偏移, 再稍向黑压深以在海报上保证可读),
            // 随主题/动态取色变化; 压深比例按日夜主题分档 (见常量注释, 可调).
            // 羽化渐变方式由共用侧边栏统一按探索页那套平滑多色标处理.
            val railScrimDarken = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
                TV_DETAILS_RAIL_SCRIM_DARKEN_DARK
            } else {
                TV_DETAILS_RAIL_SCRIM_DARKEN_LIGHT
            }
            val railScrimColor = lerp(
                lerp(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surfaceTint,
                    TV_DETAILS_RAIL_SCRIM_TINT,
                ),
                Color.Black,
                railScrimDarken,
            )
            TvDetailsSideRail(
                onExitToHero = { runCatching { heroPrimaryFocus.requestFocus() } },
                modifier = Modifier.align(Alignment.CenterStart).zIndex(1f),
                scrimColor = railScrimColor,
            )
        }
        CompositionLocalProvider(LocalBringIntoViewSpec provides noBringIntoView) {
        MultiColumnScaffold(
        layoutParams.copy(
            contentHorizontalPadding = 0.dp,
            contentTopPadding = contentTopPad,
        ),
        Modifier,
        // 顶栏按钮 (返回/主页/外链) 在 TV 上已全部移除, 顶栏本身也不再渲染,
        // 否则 Scaffold 会把滚动区压到顶栏之下, 往下翻时内容在 64dp 处被裁出一条边界
        showTopBar = false,
        windowInsets,
        scrollState = scrollState,
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
            // 起始留白只加在 Hero 块内 (startPadding), 不能加在整列上:
            // 列级 padding 会把选集轮播 LazyRow 的左边界一起右移, 向左滑过锚点的
            // 卡片在此处被硬裁出一条边 (卡片行必须保持全宽出血); 侧边栏也只在海报首屏显示
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(layoutParams.sectionSpacing),
        ) {
            TvHeroBlock(
                state = state,
                info = info,
                selfInfo = selfInfo,
                onPlay = onPlay,
                onClickLogin = onClickLogin,
                onClickOpenExternal = onClickOpenExternal,
                horizontalPadding = pad,
                primaryButtonFocusRequester = heroPrimaryFocus,
                // 中列: 收藏统计 + 标签墙 + 连载信息. 标签墙浏览模式需要 BringIntoView
                // 滚动露出隐藏标签, 恢复默认 spec (页面级已禁用)
                middleColumn = {
                    CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
                        TvHeroInfoColumn(
                            state = state,
                            info = info,
                            // 点击标签跳转搜索: 标记返回时要把焦点恢复到该标签
                            onClickTag = {
                                tagsRestorePending = true
                                onClickTag(it)
                            },
                            browseMode = tagsBrowseMode,
                            onBrowseModeChange = { tagsBrowseMode = it },
                            focusedTagIndex = focusedTagIndex,
                            onFocusedTagIndexChange = { focusedTagIndex = it },
                            restorePending = tagsRestorePending,
                            onRestoreConsumed = { tagsRestorePending = false },
                            modifier = Modifier.weight(1f).padding(start = 24.dp),
                        )
                    }
                },
                // 播放按钮底部进度条: 取"继续观看"目标集的进度
                playProgress = state.subjectProgressState.episodeIdToPlay?.let { playProgress[it] },
                // 播放按钮长按: 跳到当前集的选集卡片 (复用网格菜单的 reveal 机制 ——
                // 轮播滚到该集并聚焦, 页面随焦点吸附到选集页, 按住的残余确认键由卡片吞掉)
                onLongPressPlay = {
                    (state.subjectProgressState.episodeIdToPlay ?: currentEpisodeId)
                        ?.let { revealEpisodeId = it }
                },
                // 加载中按"有图"排版 (不显示右侧封面): 大多数条目有 backdrop, 图到了直接淡入;
                // 确认无图才切一次到封面回退, 避免"回退→有图"的两段跳变
                hasBackdrop = tmdbBackdropUrl != null || !backdropResolved,
                onCoverImageSuccess = onCoverImageSuccess,
                displaySummary = displaySummary,
                // 收藏钮右侧的"选集"圆钮 + 锚定其下的快速跳转网格菜单
                episodeGridCapsule = {
                    Box {
                        TvCapsuleButton(
                            onClick = { showEpisodeGrid = true },
                            icon = { Icon(Icons.Rounded.GridView, contentDescription = null) },
                            label = { Text(stringResource(Lang.subject_details_episodes), softWrap = false) },
                            modifier = Modifier.focusRequester(episodeGridEntryFocus),
                        )
                        TvEpisodeGridDropdown(
                            expanded = showEpisodeGrid,
                            episodes = episodes,
                            currentEpisodeId = currentEpisodeId,
                            episodeRuntimes = episodeRuntimes,
                            onEpisodeClick = {
                                showEpisodeGrid = false
                                onPlay(it.episodeId)
                            },
                            // 返回键正常关闭: 焦点还给入口圆钮, 不跳转
                            onDismissRequest = {
                                showEpisodeGrid = false
                                restoreEpisodeGridEntryFocus = true
                            },
                            // 长按 (按住 OK) 某集方格: 轮播跳到该集, 焦点落到卡片上并触发选集区吸附滚动
                            onEpisodeLongClick = { item ->
                                showEpisodeGrid = false
                                revealEpisodeId = item.episodeId
                            },
                            onCacheClick = onClickCache,
                        )
                    }
                },
                // 占满首屏, 信息带贴底
                modifier = Modifier.height(heroHeight)
                    // 焦点回到 Hero 信息带时滚回页面顶部, 否则标题永远滚不回来
                    // (滚动仅由焦点元素的 BringIntoView 驱动, 而标题不可聚焦)
                    .onFocusChanged {
                        if (it.hasFocus) scope.launch { scrollState.animateScrollTo(0) }
                    },
            )
            CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
            // 水平留白不加在区块列上, 由各区块自理: 选集轮播的卡片行要一直画到屏幕右边缘
            // (出血, 停靠留边由轮播内部 contentPadding 提供), 其余区块照常留边
            Column(
                verticalArrangement = Arrangement.spacedBy(layoutParams.sectionSpacing),
            ) {
            SnapOnFocusSection(scrollState, EPISODES_PAGE_VERTICAL_MARGIN) {
            // 选集整页: 上半 = 完整标题 + 简介 (截断, 占满剩余高度) + 右侧竖版封面,
            // 下半 = 选集轮播; 合起来正好一屏 (上下留 EPISODES_PAGE_VERTICAL_MARGIN).
            // 封面尺寸从整页高度推出 (而非上半区高度): 高 = 整页 x TV_EPISODES_COVER_HEIGHT_FRACTION,
            // 锚定右上, 超出上半区的部分向下延伸 (不占布局高度, 不推挤轮播); 上半区的
            // 标题/简介与下方轮播的小标题/集简介都以"封面宽 + 32dp 间距"收右边界 ——
            // 四者右缘对齐到同一条线, 全部不与封面重叠
            val episodesCoverHeight = episodesPageHeight * TV_EPISODES_COVER_HEIGHT_FRACTION
            val episodesTextEndReserve = if (info.imageLarge.isNotBlank()) {
                episodesCoverHeight * COVER_WIDTH_TO_HEIGHT_RATIO + 32.dp
            } else {
                0.dp
            }
            Column(
                // 整页高度收窄 EPISODES_PAGE_CONTENT_LIFT: 上半是 weight(1f) 的简介, 收窄只吃掉
                // 简介文字下方的留白 (文字顶对齐不动), 把轮播及其后区块整体上移. 封面仍按原
                // episodesPageHeight 计算 (TopEnd 无界锚定不占布局高度), 不受影响.
                Modifier.height(episodesPageHeight - EPISODES_PAGE_CONTENT_LIFT)
                    // 返回键分层要区分"焦点在选集页内/选集页之下" (见 BackHandler)
                    .onFocusChanged { episodesRegionFocused = it.hasFocus },
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = pad),
            ) {
                Column(
                    Modifier.fillMaxHeight().padding(end = episodesTextEndReserve),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        info.displayName,
                        style = MaterialTheme.typography.headlineLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // 固定占满标题下的剩余高度 (两种模式尺寸一致);
                    // 聚焦后按确认键进入阅读模式 (上下键滚动 + 右侧滚动条).
                    // 简介为空 (未开播条目常见, 可能连分集都没有) 时兜底显示"暂无信息",
                    // 保持本块始终可聚焦 —— 否则选集页可能没有任何焦点目标, 向下导航整页跳过
                    TvScrollableSummary(
                        displaySummary.ifBlank { stringResource(Lang.subject_details_no_summary) },
                        Modifier.weight(1f).fillMaxWidth()
                            // 无分集时返回键分层的焦点兜底目标 (见 BackHandler)
                            .focusRequester(episodesSummaryFocus),
                    )
                }
                if (info.imageLarge.isNotBlank()) {
                    AsyncImage(
                        info.imageLarge,
                        contentDescription = null,
                        Modifier
                            .align(Alignment.TopEnd)
                            .wrapContentHeight(align = Alignment.Top, unbounded = true)
                            .height(episodesCoverHeight)
                            .aspectRatio(COVER_WIDTH_TO_HEIGHT_RATIO)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            TvEpisodeCarousel(
                episodes = episodes,
                horizontalPadding = pad,
                // 小标题行/集简介行与上半区文字共用右边界 (给封面让位)
                endPadding = pad + episodesTextEndReserve,
                // 聚焦集简介用阅读模式组件: 平时按高度截断, 按确认键进入滚动阅读;
                // 视口只有两行高, 一次滚一行
                descContent = { desc, onHorizontalNav ->
                    TvScrollableSummary(
                        desc,
                        Modifier.weight(1f).fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        contentPadding = 4.dp,
                        scrollLines = 2,
                        // 焦点在简介上时左右键切换聚焦集 (卡片行同步滑动)
                        onHorizontalNav = onHorizontalNav,
                    )
                },
                currentEpisodeId = currentEpisodeId,
                onEpisodeClick = { onPlay(it.episodeId) },
                episodeStills = tmdbEpisodeStills,
                playProgress = playProgress,
                episodeRuntimes = episodeRuntimes,
                episodeOverviews = episodeOverviews,
                // 长按卡片: 标记看过/取消看过
                onSetEpisodeCollectionType = { item, type ->
                    onEpisodeCollectionUpdate(
                        SetEpisodeCollectionTypeRequest(state.subjectId, item.episodeId, type),
                    )
                },
                // 网格菜单关闭后跳到菜单里聚焦的那一集
                revealEpisodeId = revealEpisodeId,
                onRevealConsumed = { revealEpisodeId = null },
                // 返回键分层: 选集之下的区域按返回把焦点送回轮播卡片
                rowFocusRequester = episodesCarouselFocus,
                // 不再有"选集"标题行: 该位置改放聚焦集的小标题 (见 TvEpisodeCarousel),
                // "看过/全X话"连载进度与 Hero 重复已去掉; 卡片按上键交给空间焦点搜索
            )
            }
            }
            SnapOnFocusSection(scrollState, layoutParams.sectionSpacing) {
                CharactersSection(
                    exposedCharacters, allCharacters, totalCharactersCount,
                    modifier = Modifier.padding(horizontal = pad),
                )
            }
            // 制作人员 + 作品信息: 同一个导航区域 (作品信息无可聚焦元素, 归入制作人员区,
            // 焦点在制作人员网格内下移越界时页面最小滚动, 可逐步露出下方的信息表)
            SnapOnFocusSection(scrollState, layoutParams.sectionSpacing) {
                Column(verticalArrangement = Arrangement.spacedBy(layoutParams.sectionSpacing)) {
                    Box(Modifier.padding(horizontal = pad)) {
                        StaffSection(
                            exposedStaff,
                            allStaff,
                            totalStaffCount,
                            gridColumns = layoutParams.staffGridColumns,
                        )
                    }
                    Column(
                        Modifier.padding(horizontal = pad),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // 收藏统计与标签已上移到 Hero 信息带中列
                        SectionHeader(stringResource(Lang.subject_details_info))
                        SubjectInfoTable(info, mainEpisodeCount = episodes.size.takeIf { it > 0 })
                    }
                }
            }
            // 关联条目 + 评价: 同一个导航区域 (rail 与评价卡之间上下移动页面按需最小滚动)
            SnapOnFocusSection(scrollState, layoutParams.sectionSpacing) {
                Column(verticalArrangement = Arrangement.spacedBy(layoutParams.sectionSpacing)) {
                    if (related.itemCount > 0) {
                        // TV 上用横向单行 rail 而非多行网格 (行内横向滚动由默认 BringIntoView 驱动)
                        Column(
                            Modifier.padding(horizontal = pad),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            SectionHeader(stringResource(Lang.subject_details_related_subjects))
                            RelatedSubjectsLazyRow(
                                related,
                                onClick = rememberNavigateToRelatedSubject(),
                                itemWidth = 150.dp,
                                spacing = 20.dp,
                            )
                        }
                    }
                    ReviewsPreviewSection(
                        comments, commentCount, onShowAll = onShowComments,
                        modifier = Modifier.padding(horizontal = pad),
                    )
                }
            }
            }
            }
        }
        }
        }
    }
}

/**
 * 详情页左缘 overlay 导航栏, 仅横屏海报首屏显示: 与主页侧边栏完全同一实现 ([TvNavigationSideRail]).
 * 条目也与主页一致 (头像 → 用户信息页 / 搜索 / 探索 / 收藏 / 缓存 / 设置); 头像 selfInfo 就地取.
 * 详情页可经关联条目无限嵌套, 返回键只逐层退, 故本栏在条目上按返回键/右键把焦点还给 Hero 播放按钮
 * (由 [onExitToHero] 处理), 作为一键回主页的逃生通道.
 */
@Composable
private fun TvDetailsSideRail(
    onExitToHero: () -> Unit,
    modifier: Modifier = Modifier,
    scrimColor: Color? = null,
) {
    val navigator = LocalNavigator.current
    // 详情页不显示头像/用户名 (selfInfo = null), 但保留头像槽位使其余按钮位置不变
    TvNavigationSideRail(
        selfInfo = null,
        onAvatarClick = {},
        onExitFocus = onExitToHero,
        scrimColor = scrimColor,
        // 与主页同一份条目, 只差点击行为: 切 tab 前先弹掉整个嵌套栈回主页
        items = buildTvRailItems(
            onSearch = { navigator.navigateSubjectSearch() },
            onNavigateToPage = { navigator.popBackOrNavigateToMain(it) },
            onSettings = { navigator.navigateSettings() },
        ),
        modifier = modifier,
    )
}

/**
 * 图标按钮的字形 (glyph) 尺寸: 侧边栏与 Hero 圆钮共用. 配 32dp 容器,
 * 即 M3 extra-small icon button 规格 (20dp icon / 32dp container).
 */
private val TV_ICON_GLYPH_SIZE = 20.dp

/** 详情页侧边栏遮罩: surface 向 surfaceTint (封面取色动态主色) 的偏移比例, 调大主题色更浓. */
private const val TV_DETAILS_RAIL_SCRIM_TINT = 0.35f

/**
 * 详情页侧边栏遮罩向黑压深的比例 —— 浅色 (白天) 主题档.
 * 调小更浅 (0 = 不压深, 纯主题色面板); 白天面板浅、文字图标是深色 (onSurface), 越浅反而对比越高.
 */
private const val TV_DETAILS_RAIL_SCRIM_DARKEN_LIGHT = 0.06f

/** 详情页侧边栏遮罩向黑压深的比例 —— 深色 (黑夜) 主题档. 深色底配浅色文字, 压深无碍可读. */
private const val TV_DETAILS_RAIL_SCRIM_DARKEN_DARK = 0.35f

/**
 * Hero 标题顶部留白的视觉补偿: 标题列自带 top 8dp + headlineLarge 行高顶部内衬约 12dp,
 * 从名义顶部留白中减去, 使字形上缘到屏幕上边界的距离 ≈ 字形左缘到左边界的距离.
 */
private val TV_HERO_TITLE_TOP_TRIM = 20.dp

/**
 * 选集整页上的简介文字块: 平时按可用高度截断; 聚焦后按确认键进入"阅读模式" ——
 * 上下键滚动文字并在右侧显示滚动条, 方向/确认键锁定在块内不移动焦点,
 * 返回键退出 (隐藏滚动条, 文字回到顶部). 焦点意外离开时也自动退出.
 */
@Composable
private fun TvScrollableSummary(
    summary: String,
    modifier: Modifier = Modifier,
    /** 文字样式; null 用 bodyMedium. */
    style: TextStyle? = null,
    /** 块内边距 (紧凑场景如选集信息行可调小). */
    contentPadding: Dp = 8.dp,
    /** 每次按上下键滚动的行数 (视口很矮的场景如选集信息行应设为 1). */
    scrollLines: Int = 3,
    /**
     * 非 null 时截断态 (非阅读模式) 的左右键交给它处理 (-1/+1) 并消费, 不再走空间焦点搜索 ——
     * 选集页用来在焦点停在简介上时切换聚焦集. 阅读模式下左右键仍锁定在块内.
     */
    onHorizontalNav: ((delta: Int) -> Unit)? = null,
) {
    val textStyle = style ?: MaterialTheme.typography.bodyMedium
    var focused by remember { mutableStateOf(false) }
    var readingMode by remember { mutableStateOf(false) }
    val textScroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val lineHeightPx = with(density) {
        (if (textStyle.lineHeight.isSpecified) textStyle.lineHeight else 20.sp).toPx()
    }
    // 阅读态文字的完整排版结果: 视口高度与滚动都按其精确行界计算 (两种模式文字宽度
    // 完全一致 -> 换行一致, 该排版对两种模式都成立), 不用标称行高估算 (首行字体内衬会摊不平)
    var readingLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    // 视口能放下的整行数 (阅读态组合时计算, 按键滚动时引用)
    var readingFitLines by remember { mutableStateOf(1) }
    // 当前视口顶部的行号: 滚动按行号推进并 scrollTo 精确行顶, 不累积像素误差
    var topLine by remember { mutableStateOf(0) }
    val onSurface = MaterialTheme.colorScheme.onSurface
    // 阅读模式底色比聚焦态更淡: 大面积墨色会压住文字
    val backgroundColor by animateColorAsState(
        when {
            readingMode -> tvGlassColor(TV_GLASS_READING_ALPHA)
            focused -> tvGlassColor()
            else -> Color.Transparent
        },
    )

    fun exitReading() {
        readingMode = false
        topLine = 0
        scope.launch { textScroll.scrollTo(0) }
    }

    // 上下键按行滚动: 视口顶行号推进 scrollLines 行后滚到该行行顶 (精确行界, 永远整行显示)
    fun scrollByLines(delta: Int) {
        val layout = readingLayout ?: return
        val maxTop = (layout.lineCount - readingFitLines).coerceAtLeast(0)
        val target = (topLine + delta).coerceIn(0, maxTop)
        if (target == topLine) return
        topLine = target
        scope.launch {
            textScroll.animateScrollTo(
                layout.getLineTop(target).roundToInt(),
                animationSpec = tween(TV_READING_SCROLL_ANIM_MS),
            )
        }
    }

    BoxWithConstraints(
        modifier
            .clip(TV_BUTTON_SHAPE)
            .background(backgroundColor)
            .onFocusChanged {
                focused = it.isFocused
                if (!it.isFocused && readingMode) exitReading()
            }
            .onPreviewKeyEvent { event ->
                val isConfirm = event.key == Key.DirectionCenter ||
                    event.key == Key.Enter || event.key == Key.NumPadEnter
                if (!readingMode) {
                    when {
                        // 确认键进入阅读模式 (down/up 都消费, 避免泄漏给外层)
                        isConfirm -> {
                            if (event.type == KeyEventType.KeyUp) readingMode = true
                            true
                        }

                        onHorizontalNav != null &&
                            (event.key == Key.DirectionLeft || event.key == Key.DirectionRight) -> {
                            if (event.type == KeyEventType.KeyDown) {
                                onHorizontalNav(if (event.key == Key.DirectionLeft) -1 else 1)
                            }
                            true
                        }

                        else -> false
                    }
                } else when (event.key) {
                    Key.DirectionUp -> {
                        if (event.type == KeyEventType.KeyDown) scrollByLines(-scrollLines)
                        true
                    }

                    Key.DirectionDown -> {
                        if (event.type == KeyEventType.KeyDown) scrollByLines(scrollLines)
                        true
                    }

                    Key.Back, Key.Escape -> {
                        if (event.type == KeyEventType.KeyUp) exitReading()
                        true
                    }

                    // 阅读模式锁定焦点: 左右与确认键也全部吞掉
                    Key.DirectionLeft, Key.DirectionRight -> true
                    else -> isConfirm
                }
            }
            .focusable(),
    ) {
        if (readingMode) {
            // 阅读视口: 按阅读态排版的精确行界把可用高度向下取整到整行 ——
            // 能放下几整行就显示几整行 (可能比截断态多一行), 视口底边落在行界上,
            // 绝不露出半行也不遮住半行. 排版结果未就绪的首帧用标称行高兜底.
            val paddingPx = with(density) { contentPadding.toPx() }
            val availablePx = constraints.maxHeight - paddingPx * 2
            val layout = readingLayout
            val textViewportPx: Float
            if (layout != null && layout.lineCount > 0) {
                var n = 0
                while (n < layout.lineCount && layout.getLineBottom(n) <= availablePx) n++
                readingFitLines = n.coerceAtLeast(1)
                textViewportPx = layout.getLineBottom(readingFitLines - 1)
            } else {
                readingFitLines = ((availablePx / lineHeightPx).toInt()).coerceAtLeast(1)
                textViewportPx = readingFitLines * lineHeightPx
            }
            val viewportHeight = with(density) { (textViewportPx + paddingPx * 2).toDp() }
            Box(Modifier.fillMaxWidth().height(viewportHeight)) {
                Column(Modifier.fillMaxSize().verticalScroll(textScroll)) {
                    Text(
                        summary,
                        // 宽度与截断态完全一致 (滚动条空间两种模式都预留), 换行不变
                        Modifier.fillMaxWidth()
                            .padding(contentPadding)
                            .padding(end = TV_READING_SCROLLBAR_RESERVE),
                        style = textStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        onTextLayout = { readingLayout = it },
                    )
                }
                // 右侧滚动条: 轨道与视口同高, 滑块位置/长度按滚动进度与视口占比计算
                if (textScroll.maxValue > 0) {
                    BoxWithConstraints(
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(4.dp),
                    ) {
                        val total = (textScroll.maxValue + textScroll.viewportSize).coerceAtLeast(1)
                        Box(
                            Modifier.fillMaxSize().clip(CircleShape)
                                .background(onSurface.copy(alpha = 0.15f)),
                        )
                        Box(
                            Modifier
                                .offset(y = maxHeight * (textScroll.value.toFloat() / total))
                                .fillMaxWidth()
                                .height(maxHeight * (textScroll.viewportSize.toFloat() / total))
                                .clip(CircleShape)
                                .background(onSurface.copy(alpha = 0.6f)),
                        )
                    }
                }
            }
        } else {
            Text(
                summary,
                // 与阅读态同宽 (含滚动条预留): 两种模式换行一致, 每行字数不变
                Modifier.fillMaxWidth()
                    .padding(contentPadding)
                    .padding(end = TV_READING_SCROLLBAR_RESERVE),
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Hero 信息带中列: 左 = 统计数字 + 连载信息 (总高对齐左列按钮块); 右 = 标签墙 (三行截断).
 *
 * 标签平时即可聚焦/点击; 放不下时"显示更多"跟在最后一个可见标签右边 (FlowRow overflow).
 * 按下"显示更多"弹出标签菜单 (Popup, 同选集网格菜单形态): 尽可能显示全部标签,
 * 放不下时纵向导航自动滚动; Popup 独立于页面, 页面绝不会跟着滚. 返回键关闭菜单,
 * 焦点回到"显示更多".
 *
 * 点击标签跳转搜索后返回本页: [browseMode] (菜单开合)/[focusedTagIndex]/[restorePending]
 * 由调用方 rememberSaveable 保留, 重组时菜单原样恢复, 焦点直接回到最后聚焦的标签上.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TvHeroInfoColumn(
    state: SubjectDetailsState,
    info: SubjectInfo,
    onClickTag: (Tag) -> Unit,
    browseMode: Boolean,
    onBrowseModeChange: (Boolean) -> Unit,
    /** 最后聚焦的标签下标 (-1 无), 跨页面往返恢复焦点用. */
    focusedTagIndex: Int,
    onFocusedTagIndexChange: (Int) -> Unit,
    /** 为 true 时 (点击标签跳转后返回) 把焦点恢复到 [focusedTagIndex] 标签上. */
    restorePending: Boolean,
    onRestoreConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tags = info.tags
    val showMoreFocus = remember { FocusRequester() }
    val tagFocus = remember { FocusRequester() }
    var currentTagFocus by remember { mutableStateOf(-1) }
    // 截断态标签的跨页返回焦点恢复 (菜单打开状态下的恢复由 TvTagsMenu 自理)
    var pendingFocusIndex by remember {
        mutableStateOf(if (restorePending && !browseMode && focusedTagIndex >= 0) focusedTagIndex else -1)
    }
    var pendingShowMoreFocus by remember { mutableStateOf(false) }
    LaunchedEffect(pendingFocusIndex) {
        val target = pendingFocusIndex
        if (target < 0) return@LaunchedEffect
        // 多帧断言 (同选集轮播的处理): 页面切换的异步焦点恢复可能后到抢焦点
        repeat(20) {
            withFrameNanos { }
            if (currentTagFocus == target) {
                pendingFocusIndex = -1
                onRestoreConsumed()
                return@LaunchedEffect
            }
            runCatching { tagFocus.requestFocus() }
        }
        pendingFocusIndex = -1
        onRestoreConsumed()
    }
    LaunchedEffect(pendingShowMoreFocus) {
        if (pendingShowMoreFocus) {
            withFrameNanos { }
            runCatching { showMoreFocus.requestFocus() }
            pendingShowMoreFocus = false
        }
    }

    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        // 左: 连载信息在上, 垂直中心对齐左列 44dp 圆钮行的中心; 统计数字在下,
        // 垂直中心对齐 38dp 播放按钮的中心 (44 + SpaceBetween 自动 10 + 38 与左列几何同构;
        // 三列底对齐, 总高一致, 顶也是对齐的)
        Column(
            Modifier.height(TV_HERO_MIDDLE_HEIGHT),
            verticalArrangement = Arrangement.SpaceBetween,
            // 列宽 = 两块中较宽者, 窄的一块水平居中 —— 连载信息与统计数字的水平中心对齐
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.height(TV_CAPSULE_SIZE).offset(y = TV_AIRING_ALIGN_TRIM),
                contentAlignment = Alignment.CenterStart,
            ) {
                // 两行内容比锚定盒 (圆钮行高, 32dp) 高: 无界测量 + 居中对齐,
                // 超出部分对称溢出而不是被盒子底边裁掉 (圆钮从 44dp 缩小后两行放不下了)
                Column(
                    Modifier.wrapContentHeight(align = Alignment.CenterVertically, unbounded = true),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        renderSubjectSeason(info.airDate),
                        // 连载信息整体比统计数字小一号 (titleSmall / labelMedium)
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                    ) {
                        AiringLabel(
                            state.airingLabelState,
                            style = MaterialTheme.typography.labelMedium,
                            progressColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Box(
                Modifier.height(38.dp).offset(y = TV_STATS_ALIGN_TRIM),
                contentAlignment = Alignment.CenterStart,
            ) {
                // 同上: 内容高于锚定盒时无界测量, 对称溢出不裁剪
                Box(Modifier.wrapContentHeight(align = Alignment.CenterVertically, unbounded = true)) {
                    TvCompactStatsRow(info.collectionStats)
                }
            }
        }
        // 右: 标签墙, 占剩余宽度, 三行截断 + 行尾"显示更多" (弹出标签菜单);
        // 容器与左列按钮块同高, 内容顶对齐 —— 顶行顶缘 = 块顶 = 连载信息顶缘
        // (连载信息盒在中列顶部), 超出块高的部分向下溢出.
        // wrapContentHeight(unbounded): FlowRow 用无限高度测量 —— 若受容器约束,
        // FlowRow 的 overflow 逻辑会把放不下的一整行标签丢掉 (曾导致少一行)
        Box(
            Modifier.weight(1f).height(TV_HERO_MIDDLE_HEIGHT),
            contentAlignment = Alignment.TopStart,
        ) {
            FlowRow(
                Modifier.fillMaxWidth()
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .offset(y = TV_TAGS_ALIGN_TRIM),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                maxLines = 3,
                overflow = FlowRowOverflow.expandIndicator {
                    TvShowMoreTagsButton(
                        onClick = { onBrowseModeChange(true) },
                        modifier = Modifier.focusRequester(showMoreFocus),
                    )
                },
            ) {
                tags.forEachIndexed { i, tag ->
                    TvTagChip(
                        tag.name,
                        Modifier
                            .then(if (i == pendingFocusIndex) Modifier.focusRequester(tagFocus) else Modifier)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    currentTagFocus = i
                                    onFocusedTagIndexChange(i)
                                } else if (currentTagFocus == i) {
                                    currentTagFocus = -1
                                }
                            }
                            .clickable { onClickTag(tag) },
                    )
                }
            }
            TvTagsMenu(
                expanded = browseMode,
                tags = tags,
                onClickTag = onClickTag,
                initialFocusIndex = if (restorePending && focusedTagIndex >= 0) focusedTagIndex else 0,
                onTagFocused = onFocusedTagIndexChange,
                onRestoreConsumed = onRestoreConsumed,
                onDismissRequest = {
                    onBrowseModeChange(false)
                    pendingShowMoreFocus = true
                },
            )
        }
    }
}

/**
 * "显示更多"弹出的标签菜单: 从锚点上方弹出 (同选集网格菜单的形态与定位), 尽可能显示全部标签;
 * 放不下时纵向移动焦点自动滚动 (菜单内恢复默认 BringIntoView), 可导航到所有标签.
 * Popup 独立于页面滚动容器, 页面不会跟着动. 返回键/点击外部关闭.
 *
 * 需组合在锚点 (标签墙) 所在的 Box 内.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun TvTagsMenu(
    expanded: Boolean,
    tags: List<Tag>,
    onClickTag: (Tag) -> Unit,
    onDismissRequest: () -> Unit,
    /** 打开时聚焦的标签下标 (跨页返回时为上次聚焦的标签, 平时为 0). */
    initialFocusIndex: Int = 0,
    onTagFocused: (Int) -> Unit = {},
    onRestoreConsumed: () -> Unit = {},
) {
    if (!expanded) return
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
        // 页面级禁用了 BringIntoView; 菜单内恢复默认行为, 焦点纵向移动时自动滚动
        val defaultBringIntoView = remember { object : BringIntoViewSpec {} }
        CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoView) {
            val tagFocus = remember { FocusRequester() }
            var currentFocus by remember { mutableStateOf(-1) }
            var pendingFocus by remember { mutableStateOf(initialFocusIndex.coerceIn(0, tags.lastIndex)) }
            LaunchedEffect(Unit) {
                repeat(20) {
                    withFrameNanos { }
                    if (currentFocus == pendingFocus) {
                        pendingFocus = -1
                        onRestoreConsumed()
                        return@LaunchedEffect
                    }
                    runCatching { tagFocus.requestFocus() }
                }
                pendingFocus = -1
                onRestoreConsumed()
            }
            Surface(
                Modifier.width(560.dp).heightIn(max = 400.dp),
                shape = RoundedCornerShape(16.dp),
                // 半透明容器 (详情页所有弹出菜单统一), 隐约透出下层内容
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = TV_MENU_CONTAINER_ALPHA),
                shadowElevation = 8.dp,
            ) {
                FlowRow(
                    Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.forEachIndexed { i, tag ->
                        TvTagChip(
                            tag.name,
                            Modifier
                                .then(if (i == pendingFocus) Modifier.focusRequester(tagFocus) else Modifier)
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        currentFocus = i
                                        onTagFocused(i)
                                    } else if (currentFocus == i) {
                                        currentFocus = -1
                                    }
                                }
                                .clickable { onClickTag(tag) },
                        )
                    }
                }
            }
        }
    }
}


/** 收藏统计: 竖排单元 —— 数字在上 (细字重), 收藏/在看/想看小字在下. */
@Composable
private fun TvCompactStatsRow(
    stats: SubjectCollectionStats,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(
            stats.collect to stringResource(Lang.subject_details_stat_collected),
            stats.doing to stringResource(Lang.subject_details_stat_watching),
            stats.wish to stringResource(Lang.subject_details_stat_wish),
        ).forEach { (count, label) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    remember(count) { groupThousands(count) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * TV 标签 chip: 无描边, 低透明度主题色玻璃底 (对齐 M3 state-layer 观感), 紧凑内边距.
 * [modifier] 由调用方注入 focusRequester/onFocusChanged/clickable; clip 在最外层,
 * 点击/聚焦指示随圆角裁切.
 */
@Composable
private fun TvTagChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        Modifier
            .clip(TV_TAG_SHAPE)
            .then(modifier)
            .background(tvGlassColor(0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/** [TvTagChip] 的圆角. */
private val TV_TAG_SHAPE = RoundedCornerShape(6.dp)

/**
 * 标签墙的"显示更多"小按钮 (跟在最后一个可见标签右边): 聚焦时玻璃底提示.
 *
 * 结构与 [TvTagChip] 完全同构 (Box + 同字号 + 同内边距) -> 高度严格相同, 与标签
 * 最后一行对齐. 不能用可点击 Surface: M3 会给它套最小交互尺寸 (48dp), 占位变高、
 * 可见部分垂直居中, 看起来比标签矮半截且下沉.
 */
@Composable
private fun TvShowMoreTagsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val containerColor by animateColorAsState(
        if (focused) tvGlassColor() else Color.Transparent,
    )
    Box(
        Modifier
            .clip(TV_TAG_SHAPE)
            .then(modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            stringResource(Lang.subject_details_show_more),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
    }
}

/**
 * 区块级导航控制:
 * - 焦点从区块外进入 -> 区块顶吸附到屏幕上方 ([snapTopMargin] 处);
 * - 区块内自由导航: 焦点仍在可见范围内则页面完全不动; 焦点会越出可见范围时
 *   最小滚动露出焦点 (边缘留 [SECTION_ITEM_REVEAL_MARGIN] 余量);
 * - 区块内向上滚动的下限是吸附位 —— 永不越过区块顶, 不会露出上一个区块;
 * - 焦点落到区块最上排 (顶缘距区块顶 < [TV_SECTION_TOPMOST_THRESHOLD]) 时,
 *   区块整体回吸到屏幕顶部.
 *
 * 页面级纵向 BringIntoView 已禁用 (见 [SubjectDetailsTvPage]), 纵向滚动全部由本组件
 * 通过 bringIntoViewResponder 显式驱动: focusable 获得焦点时总会发起 bringIntoView
 * 请求, 请求自带焦点元素在本区块内的精确边界, 是唯一可靠的"焦点位置"来源.
 *
 * 区块在滚动内容中的位置是实测的 (屏幕位置 + 已滚距离, 该和在滚动中恒定),
 * 而非按布局参数解析推算 —— 脚手架的顶部留白/insets 等全部自动包含, 不会有固定偏差.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SnapOnFocusSection(
    scrollState: ScrollState,
    /**
     * 吸附后区块顶距屏幕顶的留白. 普通区块传区块间距 (layoutParams.sectionSpacing):
     * 吸附后区块上方只剩区块间的纯空隙, 上一个区块的底边正好压在屏幕顶上, 不露出.
     * 选集整页传 [EPISODES_PAGE_VERTICAL_MARGIN] (整页上下各留同样空隙).
     */
    snapTopMargin: Dp,
    content: @Composable () -> Unit,
) {
    // 区块顶在滚动内容坐标系中的 y: 实测屏幕位置 + 当前滚动量
    var sectionContentY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val snapMarginPx = with(density) { snapTopMargin.toPx() }
    val revealMarginPx = with(density) { SECTION_ITEM_REVEAL_MARGIN.toPx() }
    val topmostThresholdPx = with(density) { TV_SECTION_TOPMOST_THRESHOLD.toPx() }
    var sectionFocused by remember { mutableStateOf(false) }
    // 焦点刚从区块外进入, 下一个 bringIntoView 请求执行吸顶 (焦点回调在请求前同步触发)
    var pendingEntrySnap by remember { mutableStateOf(false) }
    val responder = remember(scrollState) {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect = localRect

            override suspend fun bringChildIntoView(localRect: () -> Rect?) {
                val rect = localRect() ?: return
                val sectionTop = sectionContentY
                val snapTarget = (sectionTop - snapMarginPx).coerceAtLeast(0f)
                val itemTop = sectionTop + rect.top
                val itemBottom = sectionTop + rect.bottom
                val viewport = scrollState.viewportSize
                val current = scrollState.value.toFloat()
                val entry = pendingEntrySnap
                pendingEntrySnap = false
                val target = when {
                    // 进入区块 / 焦点在区块最上排: 区块顶吸附到屏幕上方
                    entry || rect.top < topmostThresholdPx -> snapTarget
                    // 焦点上缘越出可见范围: 上滚露出, 但不越过区块顶 (不露出上一个区块)
                    itemTop < current + revealMarginPx ->
                        (itemTop - revealMarginPx).coerceAtLeast(snapTarget)
                    // 焦点下缘越出可见范围: 下滚露出
                    itemBottom > current + viewport - revealMarginPx ->
                        itemBottom - viewport + revealMarginPx
                    // 完全可见: 页面不动
                    else -> return
                }
                val rounded = target.roundToInt().coerceAtLeast(0)
                if (rounded != scrollState.value) scrollState.animateScrollTo(rounded)
            }
        }
    }
    Box(
        Modifier
            .onGloballyPositioned {
                sectionContentY = it.positionInRoot().y + scrollState.value
            }
            .onFocusChanged { state ->
                if (state.hasFocus && !sectionFocused) pendingEntrySnap = true
                sectionFocused = state.hasFocus
            }
            .bringIntoViewResponder(responder),
    ) { content() }
}

/**
 * Hero 全屏背景图 (页面背景层, 不随内容滚动): 贴顶/贴右出血, 左缘与底缘渐变入页面背景色,
 * 随滚动淡出以免与滚上来的内容争夺可读性.
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
            // 左侧暗色 scrim: 保证浮在图上的标题可读
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

/** 区块内导航时焦点边缘距屏幕上/下缘的最小可见余量: 焦点越出才滚动, 滚动后留出该余量. */
private val SECTION_ITEM_REVEAL_MARGIN = 24.dp

/**
 * 焦点元素顶缘距区块顶小于此值视为"区块最上排", 聚焦时区块整体回吸到屏幕顶部
 * (须大于区块标题行高度, 小于第二排可聚焦元素的顶缘位置).
 */
private val TV_SECTION_TOPMOST_THRESHOLD = 100.dp

/** 选集整页 (标题+简介+封面+轮播) 吸附后距屏幕上/下边缘的空隙. */
private val EPISODES_PAGE_VERTICAL_MARGIN = 24.dp

/**
 * 选集整页"上半简介 + 轮播"这层相对整页高度的收窄量: 收窄吃掉简介文字下方留白 (文字顶对齐
 * 不动), 把轮播及其后区块整体上移. 调大上移更多, 0 则恢复占满整页. 不影响封面 (按原整页高算).
 */
private val EPISODES_PAGE_CONTENT_LIFT = 16.dp

/**
 * 选集整页右侧竖版封面高度占整页高度的比例. 封面锚定右上, 超出"标题+简介"区的部分
 * 向下延伸到聚焦集小标题/简介右侧 (这些文字均以封面宽收右边界, 不会被盖住).
 */
private const val TV_EPISODES_COVER_HEIGHT_FRACTION = 0.6f

/** Hero 主操作按钮的圆角: 比 M3 默认胶囊更尖. */
private val TV_BUTTON_SHAPE = RoundedCornerShape(8.dp)

/**
 * 圆钮 (收藏 / 选集 / 在 Bangumi 打开) 的容器直径: M3 extra-small icon button 规格
 * (32dp 容器 / 20dp 字形, 见 [TV_ICON_GLYPH_SIZE]), 与左缘侧边栏图标按钮同尺寸.
 * 聚焦填充即容器本身.
 */
private val TV_CAPSULE_SIZE = 32.dp

/** Hero 中列左侧 (统计+连载) 的高度: 与左列 "圆钮行 (44dp) + 间距 (10dp) + 播放按钮 (38dp)" 一致. */
private val TV_HERO_MIDDLE_HEIGHT = TV_CAPSULE_SIZE + 10.dp + 38.dp

// ===== 信息带中列对齐手动微调 (在几何对齐基础上的修正量; 正值向下移, 负值向上移) =====

/** 连载信息 (两行整体) 相对左列三圆钮中心的垂直微调. */
private val TV_AIRING_ALIGN_TRIM = -10.dp

/** 统计数字 (两行整体) 相对播放按钮中心的垂直微调. */
private val TV_STATS_ALIGN_TRIM = -10.dp

/** 标签墙 (整体) 相对左列按钮块 (圆钮行+播放按钮) 整体中心的垂直微调. */
private val TV_TAGS_ALIGN_TRIM = -8.dp

/**
 * 信息带按钮玻璃底的墨色浓度.
 *
 * 信息带所在的背景图底部区域已渐隐、露出 surface 色的页面背景 (见 [TvHeroBackdrop]),
 * 因此按钮底色以 onSurface 为墨色加此透明度: 暗色主题为白色半透明,
 * 浅色主题自动变为深色半透明; 配合不透明 onSurface 内容色, 任意主题下均清晰.
 */
private const val TV_GLASS_ALPHA = 0.10f

/** 阅读模式等大面积底色再减淡一档, 避免大块墨色压住文字. */
private const val TV_GLASS_READING_ALPHA = 0.03f

/**
 * 简介块右侧为阅读模式滚动条预留的宽度. 截断态与阅读态都预留 —— 两种模式文字宽度
 * 完全一致, 换行/每行字数不变 (阅读态视口的整行量化也依赖两态排版一致).
 */
private val TV_READING_SCROLLBAR_RESERVE = 12.dp

/** 阅读模式每次按键滚动的动画时长 (ms): 越小滚得越快. */
private const val TV_READING_SCROLL_ANIM_MS = 120

/**
 * TV 详情页玻璃底色: 以 onSurface 为墨色、向主题动态色 (surfaceTint, 封面取色的 primary)
 * 偏移少许 —— 比纯灰更融入取色主题. 浓度对齐 M3 state-layer 惯例 (8%-12% 的淡层).
 */
@Composable
private fun tvGlassColor(alpha: Float = TV_GLASS_ALPHA): Color = lerp(
    MaterialTheme.colorScheme.onSurface,
    MaterialTheme.colorScheme.surfaceTint,
    0.35f,
).copy(alpha = alpha)

/** 按钮聚焦时主色填充的不透明度: 留一点透明让背景透出来, 不至于一块实心色块. */
private const val TV_FOCUSED_CONTAINER_ALPHA = 0.8f

/**
 * 收藏圆钮: 平时只显示当前收藏状态的图标, 聚焦时横向展开状态文字.
 * 点击弹出五态收藏菜单 (DropdownMenu); 设为"看过"时弹出"同时标记所有剧集"对话框.
 */
@Composable
private fun TvCollectionCapsule(
    state: EditableSubjectCollectionTypeState,
    modifier: Modifier = Modifier,
) {
    EditableSubjectCollectionTypeDialogsHost(state)
    val presentation by state.presentationFlow.collectAsStateWithLifecycle()
    val type = presentation.selfCollectionType
    val action = remember(type) { SubjectCollectionActionsForCollect.find { it.type == type } }
    Box(modifier) {
        TvCapsuleButton(
            // 更新进行中忽略点击但保持可聚焦: enabled=false 会让按钮失去焦点能力, 焦点会飞走
            onClick = { if (!presentation.isSetSelfCollectionTypeWorking) state.showDropdown = true },
            icon = { action?.icon?.invoke() },
            label = {
                if (type == UnifiedCollectionType.NOT_COLLECTED) {
                    action?.title?.invoke()
                } else {
                    Text(renderCollectionTypeAsCurrent(type), softWrap = false)
                }
            },
        )
        EditCollectionTypeDropDown(
            state,
            // 半透明容器 (详情页所有弹出菜单统一)
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = TV_MENU_CONTAINER_ALPHA),
        )
    }
}

/**
 * 图标圆钮: 固定 [TV_CAPSULE_SIZE] 正圆, 无底色无描边, 仅主题色图标;
 * 聚焦时填充主题主色 (动态主题下即封面取色), 并在圆钮上方浮现 [label] 纯文字标签
 * 标签不占布局空间, 不推挤周围.
 */
@Composable
private fun TvCapsuleButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val onSurface = MaterialTheme.colorScheme.onSurface
    val containerColor by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.primary.copy(alpha = TV_FOCUSED_CONTAINER_ALPHA)
        else Color.Transparent,
    )
    val contentColor by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.onPrimary else onSurface,
    )
    Box(modifier) {
        Box(
            Modifier
                .size(TV_CAPSULE_SIZE)
                .onFocusChanged { focused = it.isFocused }
                .clip(CircleShape)
                .background(containerColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                // 20dp 字形: 外层约束把默认 24dp 的 Icon 收到字形尺寸
                Box(Modifier.size(TV_ICON_GLYPH_SIZE), contentAlignment = Alignment.Center) {
                    icon()
                }
            }
        }
        // 聚焦时上方浮现的文字标签: layout(0,0) 不占任何布局空间;
        // 相对圆钮水平居中, 底缘在圆钮上缘之上 8dp
        Box(
            Modifier.layout { measurable, _ ->
                val placeable = measurable.measure(Constraints())
                val anchorWidth = TV_CAPSULE_SIZE.roundToPx()
                layout(0, 0) {
                    placeable.place(
                        x = (anchorWidth - placeable.width) / 2,
                        y = -(placeable.height + 8.dp.roundToPx()),
                    )
                }
            },
        ) {
            AnimatedVisibility(focused, enter = fadeIn(), exit = fadeOut()) {
                ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                    CompositionLocalProvider(LocalContentColor provides onSurface) {
                        Row { label() }
                    }
                }
            }
        }
    }
}

/**
 * Hero 主操作 (播放) 按钮: 玻璃底圆角矩形, ▶ 图标 + 文字居中,
 * 聚焦时填充主题主色 (动态主题下即封面取色). 当前要播的集有播放进度时,
 * 按钮正下方画一条与按钮同宽的细进度条 (在按钮外部, 不随聚焦反色).
 *
 * [onLongPress] 非 null 时支持长按确认键 (按住到阈值即触发, 不等松开): 详情页用来
 * 跳到当前集的选集卡片. 此时短按的点击改在 KeyUp 触发 (确认键全部在 preview 层消费).
 */
@Composable
private fun TvPlayButton(
    state: SubjectProgressState,
    onPlay: () -> Unit,
    playProgress: Float?,
    modifier: Modifier = Modifier,
    /** 作用于按钮本体 (如 focusRequester); [modifier] 作用于"按钮 + 进度条"整体. */
    buttonModifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val onSurface = MaterialTheme.colorScheme.onSurface
    val containerColor by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.primary.copy(alpha = TV_FOCUSED_CONTAINER_ALPHA)
        else tvGlassColor(),
    )
    val contentColor by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.onPrimary else onSurface,
    )
    // 长按检测 (同选集网格/排序格的确认键连发计数): 按住到阈值立即触发跳转 (不等松开),
    // 残余按键由目标卡片吞掉 (轮播 reveal 自带 swallowHeldConfirm); 焦点离开时复位.
    var confirmDownCount by remember { mutableStateOf(0) }
    var longPressFired by remember { mutableStateOf(false) }
    val strings = rememberSubjectStatusStrings()
    Column(modifier) {
        Surface(
            onClick = onPlay,
            modifier = buttonModifier
                .fillMaxWidth()
                .onFocusChanged {
                    focused = it.isFocused
                    if (!it.isFocused) {
                        confirmDownCount = 0
                        longPressFired = false
                    }
                }
                .then(
                    if (onLongPress == null) Modifier else Modifier.onPreviewKeyEvent { event ->
                        val isConfirm = event.key == Key.DirectionCenter ||
                            event.key == Key.Enter || event.key == Key.NumPadEnter
                        if (!isConfirm) return@onPreviewKeyEvent false
                        when (event.type) {
                            KeyEventType.KeyDown -> {
                                confirmDownCount++
                                if (!longPressFired && confirmDownCount >= LONG_PRESS_CONFIRM_KEY_REPEATS) {
                                    longPressFired = true
                                    onLongPress()
                                }
                            }

                            KeyEventType.KeyUp -> {
                                val fired = longPressFired
                                confirmDownCount = 0
                                longPressFired = false
                                if (!fired) onPlay()
                            }
                        }
                        true // 确认键全部消费, 不再走 Surface 自带的点击 (否则短按会双触发)
                    },
                ),
            shape = TV_BUTTON_SHAPE,
            color = containerColor,
            contentColor = contentColor,
        ) {
            Row(
                // 文字 titleMedium (行高 24sp) 配 38dp 高: 文字与上下边界各留 7dp,
                Modifier.height(38.dp).fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Text(
                    state.buttonText(strings),
                    style = MaterialTheme.typography.titleSmall,
                    softWrap = false,
                )
            }
        }
        val progress = playProgress?.coerceIn(0f, 1f)
        if (progress != null && progress > 0f && progress < 1f) {
            Box(
                Modifier
                    // 进度条嵌在按钮底边内侧 (负偏移整条叠上按钮, 条的下缘与按钮下缘重合);
                    // 不占布局高度 (layout 高度上报 0):
                    // 信息带三列底对齐, 左列底必须恒为播放按钮底 —— 若进度条占高度,
                    // 有观看进度的条目左列会多出高度, 中列所有中心对齐整体歪掉
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, 0) {
                            placeable.place(0, -placeable.height)
                        }
                    }
                    .fillMaxWidth()
                    // 两端缩进按钮圆角半径: 条长 = 按钮底边未被圆角削掉的直线段
                    .padding(horizontal = 8.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(onSurface.copy(alpha = 0.25f)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(onSurface),
                )
            }
        }
    }
}

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
    onClickOpenExternal: () -> Unit,
    horizontalPadding: Dp,
    /** 进入页面时的初始焦点: Hero 区主操作按钮 (播放). */
    primaryButtonFocusRequester: FocusRequester,
    /** 信息带中列内容 (收藏统计/标签墙/连载信息), 由调用方注入 (需要页面级状态). */
    middleColumn: @Composable RowScope.() -> Unit = {},
    /** 是否有全屏横版背景图. 无图时标题用主题色, 且在右侧展示竖版封面. */
    hasBackdrop: Boolean,
    onCoverImageSuccess: (AsyncImagePainter.State.Success) -> Unit,
    modifier: Modifier = Modifier,
    /** 当前要播的集的播放进度 (0..1), 无记录为 null; 播放按钮底部进度条用. */
    playProgress: Float? = null,
    /** 播放按钮长按 (按住确认键到阈值) 的动作; 详情页传"跳到当前集的选集卡片". */
    onLongPressPlay: (() -> Unit)? = null,
    /** 收藏钮右侧的"选集"圆钮 (含锚定其下的网格菜单), 由调用方注入. */
    episodeGridCapsule: @Composable () -> Unit = {},
    /** 展示用简介 (Bangumi 全外文时已替换为 TMDB 中文); 默认用原文. */
    displaySummary: String = info.summary,
) {
    Column(modifier.fillMaxWidth().padding(start = horizontalPadding)) {
        // 上半区: 左 = 标题 (有背景图时白色浮于图上); 右 = 无横版图时的竖版封面,
        // 高度正好撑满 "顶栏按钮之下、信息带之上", 随内容滚出屏幕
        Row(Modifier.weight(1f).fillMaxWidth().padding(end = horizontalPadding)) {
            Column(
                Modifier.weight(1f).padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // 白色标题浮于背景图上, 图亮部会看不清: 加柔和黑色阴影兜底
                val titleShadow = if (hasBackdrop) {
                    with(LocalDensity.current) {
                        Shadow(
                            color = Color.Black.copy(alpha = 0.6f),
                            offset = Offset(0f, 1.dp.toPx()),
                            blurRadius = 6.dp.toPx(),
                        )
                    }
                } else null
                Text(
                    info.displayName,
                    style = MaterialTheme.typography.headlineLarge.copy(shadow = titleShadow),
                    color = if (hasBackdrop) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (info.name.isNotBlank() && info.name != info.displayName) {
                    Text(
                        info.name,
                        style = MaterialTheme.typography.bodyMedium.copy(shadow = titleShadow),
                        color = if (hasBackdrop) Color.White.copy(alpha = 0.78f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!hasBackdrop && displaySummary.isNotBlank()) {
                    // 无横版图时标题下方较空: 简介填进来, 放不下省略;
                    // 完整简介看"作品信息"子页面 (此时信息带入口只显示标签, 不重复文字)
                    Text(
                        displaySummary,
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

        // 信息带: 左 = 圆钮行 / 播放按钮 上下两行; 中 = 统计+标签墙+连载信息;
        // 右 = 完整评分区. 三列底部对齐 (标签墙底缘与评分底缘齐平, 信息带整体贴底的延续).
        // 列间距显式控制 (不用 spacedBy): 左↔中 24; 中↔右 12 —— 标签墙右缘外扩一档,
        // FlowRow 换行的锯齿空白不至于叠上整份间距显得中右之间空一条
        Row(
            Modifier.fillMaxWidth().padding(end = horizontalPadding),
            verticalAlignment = Alignment.Bottom,
        ) {
            // 左列整体提层: 圆钮聚焦时上方浮现的文字标签要盖在上方内容之上.
            // IntrinsicSize.Max: 播放按钮 fillMaxWidth 后与上方"圆钮行 + 连载信息"等宽.
            Column(
                Modifier.zIndex(1f).width(IntrinsicSize.Max),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 上: 圆钮行 — 收藏 + 选集 + 在 Bangumi 打开 (原右上角按钮; TV 上仅详情页有该操作).
                // 间距 8dp: M3 图标按钮排布的标准相邻间距 (容器即聚焦填充, 不会互相贴上)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selfInfo.isSessionValid == false) {
                        // 未登录: 圆钮显示"收藏"图标, 聚焦浮现登录提示, 点击进登录页
                        TvCapsuleButton(
                            onClick = onClickLogin,
                            icon = { SubjectCollectionActions.Collect.icon() },
                            label = {
                                Text(stringResource(Lang.subject_details_login_to_collect), softWrap = false)
                            },
                        )
                    } else {
                        TvCollectionCapsule(state.editableSubjectCollectionTypeState)
                    }
                    // 选集快速跳转 (圆钮 + 下拉网格), 样式与相邻圆钮一致
                    episodeGridCapsule()
                    TvCapsuleButton(
                        onClick = onClickOpenExternal,
                        icon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) },
                        label = { Text("Bangumi", softWrap = false) },
                    )
                }
                // 下: 播放按钮 (下方带播放进度条), 宽度与列同宽
                // (IntrinsicSize.Max: 取"圆钮行 / 按钮文字固有宽"中较大者).
                // offset 微微上移 (纯视觉, 不占布局, 周围组件与三列底对齐的几何全部不动)
                TvPlayButton(
                    state.subjectProgressState,
                    onPlay = { state.subjectProgressState.episodeIdToPlay?.let(onPlay) },
                    playProgress = playProgress,
                    modifier = Modifier.fillMaxWidth().offset(y = (-4).dp),
                    buttonModifier = Modifier.focusRequester(primaryButtonFocusRequester),
                    onLongPress = onLongPressPlay,
                )
            }
            middleColumn()
            // 右: 评分区 — 分布直方图在上, 评分摘要在下. IntrinsicSize.Max 取两者固有宽度的
            // 较大值: 直方图不会小于自身最小宽度 (压窄会让 "10" 标签折行), 摘要更宽时直方图拉伸同宽
            // 居中: 列宽 = 两者中较宽者 (IntrinsicSize.Max), 窄的一个水平居中 ——
            // 直方图与评分摘要的水平中心对齐
            Column(
                Modifier.padding(start = 12.dp).width(IntrinsicSize.Max),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RatingHistogram(
                    info.ratingInfo,
                    Modifier.fillMaxWidth(),
                    barHeight = 36.dp,
                )
                // 直方图紧贴下方评分: 无额外间距 (直方图自身与刻度行间已有 6dp)
                SubjectRatingSummary(
                    info.ratingInfo,
                    onClick = { state.editableRatingState.requestEdit() },
                )
            }
        }
    }
}
