/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.exploration

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItemsWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import me.him188.ani.app.data.models.recommend.RecommendedSubjectInfo
import me.him188.ani.app.data.models.subject.ContinueWatchingStatus
import me.him188.ani.app.data.models.subject.SubjectCollectionInfo
import me.him188.ani.app.data.models.subject.subjectInfo
import me.him188.ani.app.data.network.BangumiSummaryService
import me.him188.ani.app.data.network.TmdbImageService
import me.him188.ani.app.data.network.matchToEpisodes
import me.him188.ani.app.data.network.newestAiredDateStringOrNull
import me.him188.ani.app.data.network.toTmdbLanguage
import me.him188.ani.app.data.repository.player.EpisodePlayHistoryRepository
import me.him188.ani.app.data.repository.subject.SetSubjectCollectionTypeOrDeleteUseCase
import me.him188.ani.app.data.repository.subject.SubjectCollectionRepository
import me.him188.ani.app.data.repository.user.SettingsRepository
import me.him188.ani.app.domain.foundation.LoadError
import me.him188.ani.app.domain.usecase.GlobalKoin
import me.him188.ani.app.navigation.LocalNavigator
import me.him188.ani.app.navigation.SubjectDetailPlaceholder
import me.him188.ani.app.ui.foundation.navigation.BackHandler
import me.him188.ani.app.ui.foundation.AsyncImage
import me.him188.ani.app.ui.foundation.ifThen
import me.him188.ani.app.ui.foundation.stateOf
import me.him188.ani.app.ui.foundation.tv.TvHeroButton
import me.him188.ani.app.ui.foundation.tv.TvPortraitCard
import me.him188.ani.app.ui.foundation.tv.TV_BACKDROP_LEFT_FADE_END
import me.him188.ani.app.ui.foundation.tv.TV_BACKDROP_ASPECT_RATIO
import me.him188.ani.app.ui.foundation.tv.TV_BACKDROP_BOTTOM_FADE_START
import me.him188.ani.app.ui.foundation.tv.TV_BACKDROP_CROSSFADE_MILLIS
import me.him188.ani.app.ui.foundation.tv.TV_HERO_MEDIA_DEBOUNCE_MILLIS
import me.him188.ani.app.ui.foundation.tv.TV_HERO_TEXT_FADE_MILLIS
import me.him188.ani.app.ui.foundation.tv.TV_HERO_TITLE_WIDTH_FRACTION
import me.him188.ani.app.ui.foundation.tv.TV_PAGE_BOTTOM_SCRIM_HEIGHT
import me.him188.ani.app.ui.foundation.tv.TV_PAGE_BOTTOM_SCRIM_MAX_ALPHA
import me.him188.ani.app.ui.foundation.tv.TV_PAGE_CARD_SPACING
import me.him188.ani.app.ui.foundation.tv.TV_PAGE_CARD_WIDTH
import me.him188.ani.app.ui.foundation.tv.TV_PAGE_END_PAD
import me.him188.ani.app.ui.foundation.tv.TV_PAGE_HINT_BOTTOM_PAD
import me.him188.ani.app.ui.foundation.tv.TV_PAGE_HINT_ICON_SIZE
import me.him188.ani.app.ui.foundation.tv.TV_BACKDROP_LEFT_FADE_START
import me.him188.ani.app.ui.foundation.tv.TV_HERO_SUMMARY_WIDTH_FRACTION
import me.him188.ani.app.ui.foundation.tv.tvBackdropFadeFromBlackStops
import me.him188.ani.app.ui.foundation.tv.tvBackdropFadeToBlackStops
import me.him188.ani.app.ui.foundation.tv.tvHeroContentColor
import me.him188.ani.app.ui.foundation.tv.tvHeroSecondaryContentColor
import me.him188.ani.app.ui.foundation.widgets.LocalToaster
import me.him188.ani.app.ui.foundation.widgets.showLoadError
import me.him188.ani.app.ui.lang.Lang
import me.him188.ani.app.ui.lang.exploration_continue_watching
import me.him188.ani.app.ui.lang.exploration_recommendations
import me.him188.ani.app.ui.lang.exploration_tv_air_date
import me.him188.ani.app.ui.lang.exploration_tv_all_caught_up
import me.him188.ani.app.ui.lang.exploration_tv_minutes_left
import me.him188.ani.app.ui.lang.exploration_tv_watched_latest
import me.him188.ani.app.ui.lang.subject_progress_updates_on
import me.him188.ani.app.ui.lang.tv_card_remote_hint
import me.him188.ani.app.tools.WeekFormatter
import me.him188.ani.datasources.api.toLocalDateOrNull
import me.him188.ani.app.ui.lang.exploration_tv_more_details
import me.him188.ani.app.ui.lang.exploration_tv_next_episode
import me.him188.ani.app.ui.lang.exploration_tv_watch_now
import me.him188.ani.app.ui.lang.playback_history_episode_label
import me.him188.ani.app.ui.subject.AiringLabel
import me.him188.ani.app.ui.subject.AiringLabelState
import me.him188.ani.app.ui.subject.collection.components.EditCollectionTypeDropDown
import me.him188.ani.datasources.api.topic.UnifiedCollectionType
import me.him188.ani.utils.analytics.Analytics
import me.him188.ani.utils.analytics.AnalyticsEvent.Companion.SubjectEnter
import me.him188.ani.utils.analytics.recordEvent
import org.jetbrains.compose.resources.stringResource

/**
 * TV 沉浸式探索页: 全屏背景为聚焦条目的 TMDB backdrop (左/下渐隐入背景色),
 * 上半区展示聚焦条目的标题 / Bangumi 评分数字 + 连载信息 / 简介; 下半区为可滚动的卡片区 ——
 * 最高热点与继续观看横向延伸, 推荐纵向无限行. 卡片全部为竖版封面, 聚焦时主题色外圈.
 *
 * 数据加载全异步不阻塞 UI: 聚焦换卡先立即换标题, Bangumi 文字信息 (一次请求, 本地有缓存) 先到先显,
 * TMDB backdrop 慢到慢显 (crossfade). 每个条目的结果都缓存, 回焦即时显示.
 */
@Composable
fun TvExplorationPage(
    state: ExplorationPageState,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current
    val collectionRepo = remember { GlobalKoin.get<SubjectCollectionRepository>() }
    val tmdb = remember { GlobalKoin.get<TmdbImageService>() }
    val bangumiSummaryService = remember { GlobalKoin.get<BangumiSummaryService>() }
    val setCollectionTypeUseCase = remember { GlobalKoin.get<SetSubjectCollectionTypeOrDeleteUseCase>() }
    val settingsRepository = remember { GlobalKoin.get<SettingsRepository>() }
    val playHistoryRepository = remember { GlobalKoin.get<EpisodePlayHistoryRepository>() }
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current

    // 聚焦条目 (卡片 onFocusChanged 上报); 标题/封面来自卡片自身数据, 立即可显示
    var heroTarget by remember { mutableStateOf<TvHeroTarget?>(null) }
    // subjectId -> Bangumi 完整条目信息 (评分/连载/简介). 在看列表的条目自带, 聚焦时直接种入.
    val infoCache = remember { mutableStateMapOf<Int, SubjectCollectionInfo>() }
    // subjectId -> TMDB backdrop URL (null = 已查过但没有, 不再重查; 请求失败不缓存)
    val backdropCache = remember { mutableStateMapOf<Int, String?>() }
    // 继续观看行: subjectId -> 下一集 TMDB 数据 (剧照 + 单集简介; 字段为 null = 查过没有).
    // 记录 episodeId 是为了看完一集后 (下一集变化) 自动换图, 服务层有持久缓存, 重查很便宜.
    val episodeStillCache = remember { mutableStateMapOf<Int, TvNextEpisodeMedia>() }
    // 播放历史 (响应式): 退出播放器回到本页时进度条 / 剩余分钟自动更新.
    // 继续观看卡的进度条与 hero 剩余分钟都从这里取"下一集"的播放位置.
    val playHistories by playHistoryRepository.flow.collectAsStateWithLifecycle(emptyList())
    // subjectId -> bgm.tv 简介兜底 (Ani 服务器部分条目 summary 为空, 直连 bgm.tv 补; "" = 也没有)
    val summaryFallbackCache = remember { mutableStateMapOf<Int, String>() }

    // 异步加载聚焦条目的 Hero 数据: 焦点换卡时 collectLatest 取消在途请求, 不会卡 UI
    LaunchedEffect(Unit) {
        snapshotFlow { heroTarget }.filterNotNull().collectLatest { target ->
            coroutineScope {
            var info = infoCache[target.subjectId]
            if (info == null) {
                // 防抖: 快速划过卡片时不发请求
                delay(TV_HERO_MEDIA_DEBOUNCE_MILLIS)
                info = withTimeoutOrNull(HERO_FETCH_TIMEOUT_MILLIS) {
                    runCatching { collectionRepo.subjectCollectionFlow(target.subjectId).first() }.getOrNull()
                } ?: return@coroutineScope
                infoCache[target.subjectId] = info
            }
            // 过期缓存自刷新: repository 的 flow 先 emit 本地缓存 (可能过期, 如收藏时"未开播"、
            // 现已完结), 过期时会拉服务器并再次 emit. 上面 .first() 拿到旧值就取消会把刷新请求
            // 一并取消, 过期状态永远留在页面 —— 这里持续收集, 后续 emission 覆盖 infoCache
            // (聚焦换卡时 collectLatest 取消; 延迟一拍避免快速划卡时空转)
            launch {
                delay(TV_HERO_MEDIA_DEBOUNCE_MILLIS)
                runCatching {
                    collectionRepo.subjectCollectionFlow(target.subjectId).collect { fresh ->
                        infoCache[target.subjectId] = fresh
                    }
                }
            }
            // 继续观看: hero 背景优先用"下一集"的单集剧照, 直观提示播放进度节点.
            // 连载番的永久缓存可能不含新播集, 传已播出最新集日期触发陈旧重取 (服务层闸门限频).
            val nextEpisodeId = if (target.fromFollowed) info.progressInfo.nextEpisodeIdToPlay else null
            if (nextEpisodeId != null && episodeStillCache[target.subjectId]?.episodeId != nextEpisodeId) {
                runCatching {
                    val language = (settingsRepository.uiSettings.flow.first().appLanguage ?: Locale.current)
                        .toTmdbLanguage()
                    val stills = tmdb.getEpisodeStills(
                        target.subjectId, info.subjectInfo.name, language,
                        newestWantedAirDate = info.episodes.newestAiredDateStringOrNull(),
                    )
                    stills.matchToEpisodes(info.episodes)[nextEpisodeId]
                }.onSuccess { media ->
                    episodeStillCache[target.subjectId] =
                        TvNextEpisodeMedia(nextEpisodeId, media?.stillUrl, media?.overview)
                }
            }
            // 整部 backdrop: 单集剧照缺失时的兜底 (以及非继续观看行的主图), 拿到剧照就不再拉
            val hasEpisodeStill = target.fromFollowed && episodeStillCache[target.subjectId]?.stillUrl != null
            if (!hasEpisodeStill && target.subjectId !in backdropCache) {
                runCatching {
                    // 官方主背景图 (与详情页 hero 同源, 进详情零跳变); 屏保轮播才用全量列表
                    tmdb.getBackdropUrl(target.subjectId, info.subjectInfo.name)
                }.onSuccess { url ->
                    backdropCache[target.subjectId] = url
                }
            }
            // Ani 服务器简介为空时直连 bgm.tv 补 (服务端部分条目 summary 缺失, 仅替代不合并).
            // 放在 backdrop 请求之后: 兜底请求不拖慢背景图显示.
            // 网络错误不写缓存 (getSummary 抛出): 下次聚焦该条目重试, 不把瞬时断网当"确认没有".
            if (info.subjectInfo.summary.isBlank() && target.subjectId !in summaryFallbackCache) {
                runCatching { bangumiSummaryService.getSummary(target.subjectId) }
                    .onSuccess { summaryFallbackCache[target.subjectId] = it.orEmpty() }
            }
            }
        }
    }

    // 继续观看优先展示下一集剧照, 缺失时回退整部 backdrop
    val backdropUrl = heroTarget?.let { target ->
        (if (target.fromFollowed) episodeStillCache[target.subjectId]?.stillUrl else null)
            ?: backdropCache[target.subjectId]
    }

    val onFocusItem: (subjectId: Int, title: String, seed: SubjectCollectionInfo?, fromFollowed: Boolean) -> Unit =
        { subjectId, title, seed, fromFollowed ->
            // 继续观看行的 seed 来自 paging flow (始终最新), 无条件覆盖 —— 看完一集回到本页时
            // 进度/下一集要跟着变 (只在缺失时写入会把 info 冻结在页面首次聚焦时的状态)
            if (seed != null && (fromFollowed || subjectId !in infoCache)) infoCache[subjectId] = seed
            heroTarget = TvHeroTarget(subjectId, title, fromFollowed)
        }
    val navigateToSubject: (subjectId: Int, name: String, cover: String, source: String) -> Unit =
        { subjectId, name, cover, source ->
            Analytics.recordEvent(SubjectEnter) {
                put("source", source)
                put("subject_id", subjectId)
            }
            navigator.navigateSubjectDetails(
                subjectId = subjectId,
                placeholder = SubjectDetailPlaceholder(id = subjectId, name = name, coverUrl = cover),
            )
        }
    // 立即观看: 直接进播放页 —— 有观看进度接着播下一集, 没有则从第一集开始;
    // 分集信息尚未加载到 (聚焦后异步拉取中) 时退化为进详情页, 保证点击总有响应
    val navigateToPlay: (subjectId: Int, name: String, cover: String, source: String) -> Unit =
        { subjectId, name, cover, source ->
            val info = infoCache[subjectId]
            val episodeId = info?.progressInfo?.nextEpisodeIdToPlay
                ?: info?.episodes?.firstOrNull()?.episodeId
            if (episodeId != null) {
                Analytics.recordEvent(SubjectEnter) {
                    put("source", source)
                    put("subject_id", subjectId)
                }
                navigator.navigateEpisodeDetails(subjectId, episodeId)
            } else {
                navigateToSubject(subjectId, name, cover, source)
            }
        }
    // 卡片长按弹出的收藏下拉 (复用详情页收藏按钮的 EditCollectionTypeDropDown). 当前收藏状态取自
    // infoCache (聚焦时异步拉取的 SubjectCollectionInfo), 未就绪则按未收藏处理.
    val collectionMenuFor: (Int) -> @Composable (expanded: Boolean, onDismiss: () -> Unit) -> Unit =
        { subjectId ->
            { expanded, onDismiss ->
                // 长按"按住途中"弹菜单时, 同一次按住残余的确认键 (后续连发 KeyDown / 最终 KeyUp) 会落到
                // 刚弹出的菜单上误触第一项. 菜单打开后短暂吞掉确认键, 收到 KeyUp (松开) 即停; 松开事件
                // 可能不送达 (归属其它窗口), 用超时兜底.
                var swallowConfirm by remember { mutableStateOf(false) }
                LaunchedEffect(expanded) {
                    if (expanded) {
                        swallowConfirm = true
                        delay(1500)
                        swallowConfirm = false
                    } else {
                        swallowConfirm = false
                    }
                }
                EditCollectionTypeDropDown(
                    currentType = infoCache[subjectId]?.collectionType ?: UnifiedCollectionType.NOT_COLLECTED,
                    expanded = expanded,
                    onDismissRequest = onDismiss,
                    onClick = { action ->
                        scope.launch {
                            runCatching { setCollectionTypeUseCase(subjectId, action.type) }
                                .onFailure { toaster.showLoadError(LoadError.fromException(it)) }
                        }
                    },
                    modifier = Modifier.onPreviewKeyEvent { event ->
                        if (!swallowConfirm) return@onPreviewKeyEvent false
                        val isConfirm = event.key == Key.DirectionCenter ||
                                event.key == Key.Enter || event.key == Key.NumPadEnter
                        if (!isConfirm) return@onPreviewKeyEvent false
                        if (event.type == KeyEventType.KeyUp) swallowConfirm = false
                        true
                    },
                )
            }
        }

    // 最高热度改为 Hero 轮播 (无卡片行): 两枚操作按钮 (立即观看 / 更多详细内容) + 右侧不可聚焦
    // 的轮播指示器. 焦点在按钮上 = TRENDING, hero 由 carouselIndex 驱动. 焦点在按钮上时左右键手动切换轮播
    // (在第一个条目按左键则不消费, 交给焦点系统去聚焦侧边栏探索按钮); 用户静止一段时间后自动轮播下一个.
    val trending = state.trendingSubjectInfoPager
    val carouselSize = minOf(trending.itemCount, TV_CAROUSEL_MAX_DOTS)
    // 这几个 UI 状态用 rememberSaveable 跨导航保存 (进详情页返回后区块/滚动/轮播位置不变)
    var carouselIndex by rememberSaveable { mutableIntStateOf(0) }
    // 每次用户手动切换 +1, 用作自动轮播 LaunchedEffect 的 key: 手动操作即重置计时 ("否则就不动")
    var carouselInteraction by remember { mutableIntStateOf(0) }
    var focusedSection by rememberSaveable { mutableStateOf(TvExplorationSection.TRENDING) }
    var recFocusedRow by rememberSaveable { mutableIntStateOf(0) }
    // 从卡片区按上键回到 hero: 置 TRENDING 让按钮重新组合, 再请求聚焦立即观看按钮
    var pendingHeroFocus by remember { mutableStateOf(false) }
    // 返回键分层规则用: 卡片区是否持有焦点 / 当前聚焦卡在行内的真实下标
    var cardAreaHasFocus by remember { mutableStateOf(false) }
    var focusedCardIndexInRow by remember { mutableIntStateOf(0) }
    // 推荐区非首行的行首卡按返回: 先回推荐首行的首卡 (滚动到首行后经信号触发行内聚焦机制)
    var pendingRecFirstRowFocus by remember { mutableStateOf(false) }
    // 每行的"聚焦首卡"信号 (值增加即触发; key: -1 = 继续观看行, 其余 = 推荐行号)
    val rowFirstCardSignals = remember { mutableStateMapOf<Int, Int>() }
    // 从详情页返回时恢复焦点到离开前聚焦的卡片行 (focusedSection/recFocusedRow 已跨导航保存):
    // 快照进入时的目标区块/行, 把恢复请求器挂到该行内此前聚焦的那张卡上; 卡片行内焦点下标也已保存.
    val cardRestoreRequester = remember { FocusRequester() }
    var cardRestored by remember { mutableStateOf(false) }
    val restoreSection = remember { focusedSection }
    val restoreRow = remember { recFocusedRow }
    val carouselItem = if (carouselSize > 0) trending[carouselIndex.coerceIn(0, carouselSize - 1)] else null

    // 手动切换轮播 (delta = ±1, 循环); 同时重置自动轮播计时
    val switchCarousel: (Int) -> Unit = { delta ->
        if (carouselSize > 0) {
            carouselIndex = ((carouselIndex + delta) % carouselSize + carouselSize) % carouselSize
            carouselInteraction++
        }
    }
    // 从卡片区顶行按上键: 回到 hero (展开按钮并聚焦立即观看)
    val navigateUpToHero: () -> Boolean = {
        focusedSection = TvExplorationSection.TRENDING
        pendingHeroFocus = true
        true
    }
    // 返回键分层规则 (统一在页面级决策, 页面已有完整的焦点簿记):
    //   不在行首卡 -> 回本行首卡; 推荐区非首行的行首卡 -> 回推荐首行的首卡; 行首卡 -> 立即观看.
    BackHandler(
        enabled = cardAreaHasFocus && focusedSection != TvExplorationSection.TRENDING,
    ) {
        when {
            focusedCardIndexInRow > 0 -> {
                val key = if (focusedSection == TvExplorationSection.FOLLOWED) -1 else recFocusedRow
                rowFirstCardSignals[key] = (rowFirstCardSignals[key] ?: 0) + 1
            }

            focusedSection == TvExplorationSection.RECOMMENDATIONS && recFocusedRow > 0 ->
                pendingRecFirstRowFocus = true

            else -> navigateUpToHero()
        }
    }

    // TRENDING 时轮播条目驱动 hero (标题即时, 评分/连载/简介/backdrop 异步跟上)
    LaunchedEffect(carouselIndex, focusedSection, carouselSize) {
        if (focusedSection == TvExplorationSection.TRENDING && carouselSize > 0) {
            trending[carouselIndex.coerceIn(0, carouselSize - 1)]?.let {
                onFocusItem(it.bangumiId, it.nameCn, null, false)
            }
        }
    }
    // 自动轮播: 仅在 TRENDING 时推进; carouselInteraction 变化 (手动切换) 会重启本效果, 重置计时
    LaunchedEffect(carouselSize, focusedSection, carouselInteraction) {
        if (focusedSection != TvExplorationSection.TRENDING || carouselSize <= 1) {
            return@LaunchedEffect
        }
        while (true) {
            delay(TV_CAROUSEL_AUTO_ADVANCE_MILLIS)
            carouselIndex = (carouselIndex + 1) % carouselSize
        }
    }
    // 回到 hero 的挂起聚焦: 等按钮重新组合后请求焦点 (requestFocus 未附着时静默失败, 用聚焦标志判成功)
    LaunchedEffect(pendingHeroFocus) {
        if (!pendingHeroFocus) return@LaunchedEffect
        repeat(20) {
            withFrameNanos { }
            runCatching { state.trendingFirstItemFocusRequester.requestFocus() }
            if (state.trendingFirstItemFocused.value) {
                pendingHeroFocus = false
                return@LaunchedEffect
            }
            delay(16)
        }
        pendingHeroFocus = false
    }
    // 初始/返回焦点 (统一在此处, ExplorationScreen 不再对沉浸式布局单独抢焦点):
    // 首次进入或曾在 hero -> 立即观看按钮; 曾在某卡片行 -> 恢复到该行此前聚焦的卡片 (卡片状态不变).
    LaunchedEffect(Unit) {
        repeat(80) {
            withFrameNanos { }
            if (restoreSection == TvExplorationSection.TRENDING) {
                if (state.trendingFirstItemFocused.value) return@LaunchedEffect
                runCatching { state.trendingFirstItemFocusRequester.requestFocus() }
            } else {
                if (cardRestored) return@LaunchedEffect
                runCatching { cardRestoreRequester.requestFocus() }
            }
            delay(60)
        }
    }

    Box(modifier.fillMaxSize()) {
        // 背景 backdrop 层: 按原比例 (16:9) 缩放, 贴右上角, 高度为屏高的固定比例 (对齐 Prime 实测:
        // 图占屏顶约 76%). 左缘/下缘 DstOut 渐隐入页面背景, 保证叠在渐隐区上的文字可读.
        // 两态渐变 (Prime 两张截图实测): 焦点在 hero (轮播) 时收得晚 (下缘 58%→76% 屏高渐隐,
        // 左缘只擦一小段); 焦点落到卡片区时下缘提前收、左缘大幅加深 (44%→72% 屏高, 清晰区只剩
        // 右上角), 卡片行压在 <25% 可见度的长尾上. 两组停点间用动画插值平滑过渡.
        val backdropCardness by animateFloatAsState(
            if (focusedSection == TvExplorationSection.TRENDING) 0f else 1f,
            animationSpec = tween(TV_BACKDROP_STATE_ANIM_MILLIS),
            label = "backdropCardness",
        )
        Crossfade(
            backdropUrl,
            Modifier.align(Alignment.TopEnd),
            animationSpec = tween(TV_BACKDROP_CROSSFADE_MILLIS),
        ) { url ->
            if (url != null) {
                Box(
                    Modifier
                        .fillMaxHeight(TV_EXPLORATION_BACKDROP_HEIGHT_FRACTION)
                        .aspectRatio(TV_BACKDROP_ASPECT_RATIO, matchHeightConstraintsFirst = true)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            // 渐变带端点在两态间插值; 停点由平滑曲线采样生成 (无折点,
                            // 避免暗色端可见的马赫带分界线), 曲线形状两态共用.
                            val t = backdropCardness
                            // 左缘渐隐 (擦除图片 alpha, 露出页面背景; 文字叠在这段上仍可读)
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    *tvBackdropFadeFromBlackStops(
                                        // 卡片态端点用三页共享值 (轮播 hero 态维持自己的一套)
                                        start = lerp(0f, TV_BACKDROP_LEFT_FADE_START, t),
                                        end = lerp(0.46f, TV_BACKDROP_LEFT_FADE_END, t),
                                    ),
                                ),
                                blendMode = BlendMode.DstOut,
                            )
                            // 下缘渐隐: 零斜率极缓起步 + 指数级长尾渐近全黑, 一直渐变到图底,
                            // 两端都看不到分界线 (图坐标; 50% 擦除点 hero 态 ≈ 屏高 65%, 卡片态 ≈ 55%)
                            drawRect(
                                brush = Brush.verticalGradient(
                                    *tvBackdropFadeToBlackStops(
                                        start = lerp(
                                            TV_EXPLORATION_BOTTOM_FADE_START_HERO,
                                            TV_BACKDROP_BOTTOM_FADE_START,
                                            t,
                                        ),
                                        end = 1f,
                                    ),
                                ),
                                blendMode = BlendMode.DstOut,
                            )
                        },
                ) {
                    AsyncImage(
                        url,
                        contentDescription = null,
                        Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }

        // 内容列: 左侧再留 TV_EXPLORATION_START_PAD (外层已让开侧边栏 48dp) ——
        // 总左缘 64dp, 使侧边栏按钮中心 (32dp) 恰好在屏幕左缘与内容左缘的正中间
        Column(Modifier.fillMaxSize().padding(start = TV_EXPLORATION_START_PAD)) {
            val heroExpanded = focusedSection == TvExplorationSection.TRENDING
            // Hero 区: 信息块 (固定高度, 保证不同条目切换时卡片区不跳) + 展开态才有的操作按钮 (在其下方,
            // 短间距). 右侧居中悬浮不可聚焦的轮播指示器 (仅展开态). 焦点移到下方卡片时按钮/指示器消失,
            // 卡片区 (weight) 顺势上移贴住信息块 —— 无大片空白.
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, end = TV_PAGE_END_PAD),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    // 顶部固定 (对齐 backdrop 顶部); 高度即"介绍块上下边界距离": 展开态用标准高度,
                    // 卡片区聚焦时用 COLLAPSED 高度并让文字底对齐 —— 块变高时文字下边界随之下移并
                    // 紧贴下方卡片, 一个变量同时控制介绍下边界与卡片行位置.
                    // 换轮播/聚焦条目时整块文字渐隐渐现 (contentKey=条目), 消除瞬时替换的闪动;
                    // 过渡期间退场内容读退场条目自己的缓存数据. 块内始终顶对齐: 标题固定在块顶,
                    // 有 info 时简介 weight(1f) 撑满至块底; 无 info (等 API) 时标题仍停在顶部,
                    // info 到达不引起位置跳动 —— 加载前后文字位置一致.
                    AnimatedContent(
                        targetState = heroTarget,
                        modifier = Modifier.fillMaxWidth()
                            .height(if (heroExpanded) TV_HERO_INFO_HEIGHT else TV_HERO_INFO_HEIGHT_COLLAPSED),
                        transitionSpec = {
                            fadeIn(tween(TV_HERO_TEXT_FADE_MILLIS)) togetherWith
                                    fadeOut(tween(TV_HERO_TEXT_FADE_MILLIS))
                        },
                        contentKey = { it?.subjectId },
                        label = "heroInfoText",
                    ) { target ->
                        val info = target?.let { infoCache[it.subjectId] }
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (target != null) {
                                Text(
                                    target.title,
                                    Modifier.fillMaxWidth(TV_HERO_TITLE_WIDTH_FRACTION),
                                    color = tvHeroContentColor(),
                                    style = MaterialTheme.typography.headlineLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (info != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    // 评分: ★ 评分数字/10
                                    val score = info.subjectInfo.ratingInfo.score
                                    if ((score.toFloatOrNull() ?: 0f) > 0f) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                Icons.Rounded.Star,
                                                contentDescription = null,
                                                Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                            Text(
                                                "$score/10",
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                        }
                                    }
                                    // 连载信息, 后面跟一个空格 + 开播年月
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AiringLabel(
                                            remember(info) {
                                                AiringLabelState(
                                                    stateOf(info.airingInfo),
                                                    stateOf(info.progressInfo),
                                                )
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            progressColor = tvHeroSecondaryContentColor(),
                                        )
                                        val airDate = info.subjectInfo.airDate
                                        if (airDate.isValid) {
                                            Text(
                                                "    " + stringResource(
                                                    Lang.exploration_tv_air_date,
                                                    airDate.year, airDate.month,
                                                ),
                                                color = tvHeroSecondaryContentColor(),
                                                style = MaterialTheme.typography.labelLarge,
                                            )
                                        }
                                    }
                                }
                                // 继续观看: 独立一行显示下一集集号 + 集名 (Bangumi 本地数据, 即时显示)
                                val nextEp = if (target?.fromFollowed == true) {
                                    info.progressInfo.nextEpisodeIdToPlay?.let { nextId ->
                                        info.episodes.firstOrNull { it.episodeId == nextId }
                                    }
                                } else null
                                if (nextEp != null) {
                                    val epLabel = stringResource(
                                        Lang.playback_history_episode_label,
                                        nextEp.episodeInfo.sort.toString(),
                                    )
                                    val epName = nextEp.episodeInfo.nameCn.ifBlank { nextEp.episodeInfo.name }
                                    // 三态 (nextEp 语义见 SubjectProgressInfo.compute — 追平时指回已看完的最新一集):
                                    //  - 已看完最新一集/全部 (Watched/Done): "第 8 集 · 集名 · 已看完"
                                    //  - 看到一半 (有播放记录): "第 4 集 · 集名 · 剩余 23 分钟"
                                    //  - 看完上一集且有新集/还没开始: "下一集: 第 4 集 · 集名".
                                    // 集号与尾段为固定段永不截断; 集名居中段, 超长跑马灯滚动展示全文
                                    val caughtUp = info.progressInfo.continueWatchingStatus.let {
                                        it is ContinueWatchingStatus.Watched || it is ContinueWatchingStatus.Done
                                    }
                                    val remainingMinutes = if (caughtUp) null else playHistories
                                        .firstOrNull { it.episodeId == nextEp.episodeId }
                                        ?.let { history ->
                                            val duration = history.durationMillis
                                            if (duration != null && duration > 0 && history.positionMillis > 0) {
                                                // 向上取整: 剩 30 秒也显示 1 分钟
                                                (((duration - history.positionMillis).coerceAtLeast(0L) + 59_999) / 60_000)
                                                    .toInt().coerceAtLeast(1)
                                            } else null
                                        }
                                    Row(
                                        Modifier.fillMaxWidth(TV_HERO_SUMMARY_WIDTH_FRACTION)
                                            // 定高使"本行 + 10dp 列间距"恰为简介两行行距 (2×20dp):
                                            // 有无此行时简介的换行网格对齐, 最后一行结束位置一致,
                                            // 继续观看/推荐两态下文字到下方卡片的距离才相同
                                            .height(TV_HERO_STATUS_ROW_HEIGHT),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        val epInfoColor = tvHeroSecondaryContentColor()
                                        val epInfoStyle = MaterialTheme.typography.labelLarge
                                        Text(
                                            if (caughtUp || remainingMinutes != null) epLabel
                                            else stringResource(Lang.exploration_tv_next_episode, epLabel),
                                            color = epInfoColor,
                                            style = epInfoStyle,
                                            maxLines = 1,
                                        )
                                        if (epName.isNotBlank()) {
                                            Text(
                                                " · $epName",
                                                Modifier.weight(1f, fill = false)
                                                    .basicMarquee(iterations = Int.MAX_VALUE),
                                                color = epInfoColor,
                                                style = epInfoStyle,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Clip,
                                            )
                                        }
                                        if (caughtUp) {
                                            // 连载已追平: "已看完最新一集 · 周三更新" (更新时间同详情页观看按钮,
                                            // WeekFormatter); 完结看完: "已看完"
                                            val watchedStatus = info.progressInfo.continueWatchingStatus
                                                    as? ContinueWatchingStatus.Watched
                                            val updatesOn = watchedStatus?.nextEpisodeAirDate?.toLocalDateOrNull()
                                                ?.let { date ->
                                                    stringResource(
                                                        Lang.subject_progress_updates_on,
                                                        WeekFormatter.System.format(date),
                                                    )
                                                }
                                            Text(
                                                " · " + (
                                                    if (watchedStatus != null) {
                                                        stringResource(Lang.exploration_tv_watched_latest)
                                                    } else {
                                                        stringResource(Lang.exploration_tv_all_caught_up)
                                                    }
                                                    ) + (updatesOn?.let { " · $it" } ?: ""),
                                                color = epInfoColor,
                                                style = epInfoStyle,
                                                maxLines = 1,
                                            )
                                        } else if (remainingMinutes != null) {
                                            Text(
                                                " · " + stringResource(
                                                    Lang.exploration_tv_minutes_left, remainingMinutes,
                                                ),
                                                color = epInfoColor,
                                                style = epInfoStyle,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                                // 简介占满信息块剩余高度: 行数由 TV_HERO_INFO_HEIGHT 决定; 宽度用单独的
                                // HERO_SUMMARY_WIDTH_FRACTION, 调小让右边留给 backdrop 清晰区, 文字更易读.
                                // 继续观看: 优先展示下一集的 TMDB 单集简介 (回忆剧情起点), 缺失回退整部简介
                                val nextEpOverview = target?.takeIf { it.fromFollowed }
                                    ?.let { episodeStillCache[it.subjectId]?.overview }
                                    ?.takeIf { it.isNotBlank() }
                                Text(
                                    nextEpOverview
                                        ?: info.subjectInfo.summary.trim()
                                            .ifBlank { target?.let { summaryFallbackCache[it.subjectId] }.orEmpty() },
                                    Modifier.weight(1f).fillMaxWidth(TV_HERO_SUMMARY_WIDTH_FRACTION),
                                    color = tvHeroContentColor(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    // 操作按钮 (仅展开态; 作用于当前轮播条目). 左右键手动切换轮播: 首个条目按左不消费,
                    // 交给焦点系统去聚焦侧边栏探索按钮. 关闭 48dp 最小可交互尺寸约束, 否则缩小后的按钮被撑到
                    // 48dp 高、内容居中, 两枚之间会出现空白.
                    if (heroExpanded) {
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                            Column(
                                Modifier
                                    .padding(top = TV_HERO_INFO_TO_BUTTONS_GAP)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                        when (event.key) {
                                            Key.DirectionLeft -> {
                                                if (carouselIndex > 0) {
                                                    switchCarousel(-1)
                                                    true
                                                } else {
                                                    false // 第一个条目: 交给焦点系统 -> 侧边栏探索按钮
                                                }
                                            }

                                            Key.DirectionRight -> {
                                                switchCarousel(1)
                                                true
                                            }

                                            else -> false
                                        }
                                    },
                                verticalArrangement = Arrangement.spacedBy(TV_HERO_BUTTON_GAP),
                            ) {
                                TvHeroButton(
                                    text = stringResource(Lang.exploration_tv_watch_now),
                                    icon = Icons.Rounded.PlayArrow,
                                    filled = true,
                                    onClick = {
                                        carouselItem?.let {
                                            navigateToPlay(it.bangumiId, it.nameCn, it.imageLarge, "home_trending_play")
                                        }
                                    },
                                    onFocused = { focusedSection = TvExplorationSection.TRENDING },
                                    // 进入主页 / 从卡片区按上返回时的聚焦目标: 立即观看按钮
                                    focusRequester = state.trendingFirstItemFocusRequester,
                                    onFocusChangedExtra = { state.trendingFirstItemFocused.value = it },
                                )
                                TvHeroButton(
                                    text = stringResource(Lang.exploration_tv_more_details),
                                    icon = Icons.Rounded.Info,
                                    filled = false,
                                    onClick = {
                                        carouselItem?.let {
                                            navigateToSubject(it.bangumiId, it.nameCn, it.imageLarge, "home_trending_detail")
                                        }
                                    },
                                    onFocused = { focusedSection = TvExplorationSection.TRENDING },
                                )
                            }
                        }
                    }
                }
            }

            // 卡片区: 持久透明区块标题 (显示当前聚焦区块) + 其下裁剪滚动区. LazyColumn 默认裁剪到自身
            // 边界, 聚焦行吸到其顶部, 上方行 (含前面区块 / 推荐前几行) 被裁掉不外露 —— 满足"聚焦某区块
            // 时上方全部挡住、推荐第二行起前面卡片看不见". 标题独立在滚动区之上, 透明浮在 backdrop 上,
            // 不随滚动、不挡背景. 每个区块一行, 均为固定锚点轮播 (焦点靠左不动, 卡片滑动).
            val followedItems = state.followedSubjectsPager.collectAsLazyPagingItemsWithLifecycle()
            val recommendations = state.recommendationPager.collectAsLazyPagingItemsWithLifecycle()
            val hasFollowed = followedItems.itemCount > 0

            // 持久区块标题 (透明浮层, 显示当前聚焦区块名). TRENDING 时不需要标题 (hero 自带信息), 用极小
            // 间距代替那 40dp 空标题, 使继续观看紧贴按钮下方 (只留 TV_HERO_BUTTONS_TO_CONTENT_GAP).
            if (focusedSection == TvExplorationSection.TRENDING) {
                Spacer(Modifier.height(TV_HERO_BUTTONS_TO_CONTENT_GAP))
            } else {
                TvSectionHeader(
                    when (focusedSection) {
                        TvExplorationSection.TRENDING -> ""
                        TvExplorationSection.FOLLOWED -> stringResource(Lang.exploration_continue_watching)
                        TvExplorationSection.RECOMMENDATIONS -> stringResource(Lang.exploration_recommendations)
                    },
                )
                // 聚焦某区块时行内标题被吸顶滚出, 只剩这个持久标题紧贴卡片行 (行内标题原本的列间距消失,
                // 显得标题贴卡). 用此间距补回标题到卡片行的距离 (与未聚焦时同一参数, 保证两种情形一致).
                Spacer(Modifier.height(TV_SECTION_HEADER_TO_ROW_GAP))
            }

            val noBringIntoView = remember {
                object : BringIntoViewSpec {
                    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
                }
            }
            CompositionLocalProvider(LocalBringIntoViewSpec provides noBringIntoView) {
                val listState = rememberLazyListState()
                // 列表 item (标题与卡片行拆开, 已无热点行): 继续观看标题=0 行=1 (若有); 之后推荐标题 + 各行.
                // 吸顶滚动到"卡片行"(而非标题): 聚焦区块的行内标题被裁到视口上方, 由顶部持久标题代替 (避免重复).
                // TRENDING 时滚到 0, 让继续观看标题+首行在 hero 下方露出.
                val recRowBase = if (hasFollowed) 3 else 1
                LaunchedEffect(focusedSection, recFocusedRow, hasFollowed) {
                    val target = when (focusedSection) {
                        TvExplorationSection.TRENDING -> 0
                        TvExplorationSection.FOLLOWED -> if (hasFollowed) 1 else 0
                        TvExplorationSection.RECOMMENDATIONS -> recRowBase + recFocusedRow
                    }
                    listState.animateScrollToItem(target)
                }
                // 返回回推荐首行: 先把区块状态定到首行 (吸顶滚动与本目标一致, 不互相拉扯),
                // 滚动让首行组合出来, 再发信号让行内机制聚焦其首卡 (含行内横向归位)
                LaunchedEffect(pendingRecFirstRowFocus) {
                    if (!pendingRecFirstRowFocus) return@LaunchedEffect
                    recFocusedRow = 0
                    runCatching { listState.scrollToItem(recRowBase) }
                    withFrameNanos { }
                    rowFirstCardSignals[0] = (rowFirstCardSignals[0] ?: 0) + 1
                    pendingRecFirstRowFocus = false
                }

                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth().clipToBounds()
                        .onFocusChanged { cardAreaHasFocus = it.hasFocus },
                    state = listState,
                    contentPadding = PaddingValues(end = TV_PAGE_END_PAD, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(TV_SECTION_HEADER_TO_ROW_GAP),
                ) {
                    if (hasFollowed) {
                        item {
                            TvSectionHeader(
                                stringResource(Lang.exploration_continue_watching),
                                transparent = focusedSection == TvExplorationSection.FOLLOWED,
                            )
                        }
                        item {
                            // 继续观看是最顶行: 按上键回到 hero
                            TvAnchoredCardRow(
                                itemCount = followedItems.itemCount,
                                onNavigateUp = navigateUpToHero,
                                firstCardFocusSignal = rowFirstCardSignals[-1] ?: 0,
                                restoreRequester = if (restoreSection == TvExplorationSection.FOLLOWED) {
                                    cardRestoreRequester
                                } else null,
                                onRestoreFocusChanged = if (restoreSection == TvExplorationSection.FOLLOWED) {
                                    { if (it) cardRestored = true }
                                } else null,
                            ) { index, reportFocus, restoreFr, restoreOfc ->
                                val item = followedItems[index]
                                val subject = item?.subjectInfo
                                TvPortraitCard(
                                    imageUrl = subject?.imageLarge,
                                    contentDescription = subject?.displayName,
                                    onClick = {
                                        subject?.let {
                                            navigateToSubject(it.subjectId, it.displayName, it.imageLarge, "home_followed")
                                        }
                                    },
                                    onFocused = {
                                        item?.let {
                                            // 在看条目自带完整信息, 种入缓存立即显示评分/连载/简介
                                            onFocusItem(
                                                it.subjectInfo.subjectId,
                                                it.subjectInfo.displayName,
                                                it.subjectCollectionInfo,
                                                true,
                                            )
                                        }
                                        focusedSection = TvExplorationSection.FOLLOWED
                                        focusedCardIndexInRow = index
                                        reportFocus()
                                    },
                                    modifier = Modifier.width(TV_PAGE_CARD_WIDTH)
                                        // 播放/暂停键: 直接进播放器续播 (分集信息未加载时退化为进详情)
                                        .onPreviewKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyDown &&
                                                (event.key == Key.MediaPlayPause || event.key == Key.MediaPlay)
                                            ) {
                                                subject?.let {
                                                    navigateToPlay(
                                                        it.subjectId, it.displayName, it.imageLarge,
                                                        "home_followed_play",
                                                    )
                                                    true
                                                } ?: false
                                            } else {
                                                false
                                            }
                                        },
                                    focusRequester = restoreFr,
                                    onFocusChangedExtra = restoreOfc,
                                    menu = subject?.let { collectionMenuFor(it.subjectId) },
                                    // 下一集的播放进度 (语义同详情页选集卡): 看到一半按播放位置;
                                    // 已看完最新一集在等更新 (Watched) / 看完全部 (Done) 显示满条;
                                    // 看完上一集且有新集 / 还没开始看 (无播放记录) 不显示
                                    progress = item?.subjectCollectionInfo?.progressInfo?.let { progressInfo ->
                                        val caughtUp = progressInfo.continueWatchingStatus.let {
                                            it is ContinueWatchingStatus.Watched || it is ContinueWatchingStatus.Done
                                        }
                                        if (caughtUp) 1f
                                        else progressInfo.nextEpisodeIdToPlay
                                            ?.let { nextId -> playHistories.firstOrNull { it.episodeId == nextId } }
                                            ?.let { history ->
                                                val duration = history.durationMillis
                                                if (duration != null && duration > 0) {
                                                    (history.positionMillis.toFloat() / duration).coerceIn(0f, 1f)
                                                } else null
                                            }
                                    },
                                )
                            }
                        }
                    }

                    item {
                        TvSectionHeader(
                            stringResource(Lang.exploration_recommendations),
                            transparent = focusedSection == TvExplorationSection.RECOMMENDATIONS,
                        )
                    }
                    // 推荐: 每行也是固定锚点轮播, 按固定行容量分块, 行数随分页无限增长 (纵向无限行)
                    val recRowCount =
                        (recommendations.itemCount + TV_EXPLORATION_REC_ROW_SIZE - 1) / TV_EXPLORATION_REC_ROW_SIZE
                    items(recRowCount) { rowIndex ->
                        val rowStart = rowIndex * TV_EXPLORATION_REC_ROW_SIZE
                        val rowItemCount = minOf(TV_EXPLORATION_REC_ROW_SIZE, recommendations.itemCount - rowStart)
                        TvAnchoredCardRow(
                            itemCount = rowItemCount,
                            // 无继续观看时推荐首行是最顶行, 按上键回到 hero
                            onNavigateUp = if (!hasFollowed && rowIndex == 0) navigateUpToHero else null,
                            firstCardFocusSignal = rowFirstCardSignals[rowIndex] ?: 0,
                            // 推荐行横向循环: 末卡右侧即首卡
                            loop = true,
                            restoreRequester = if (
                                restoreSection == TvExplorationSection.RECOMMENDATIONS && rowIndex == restoreRow
                            ) cardRestoreRequester else null,
                            onRestoreFocusChanged = if (
                                restoreSection == TvExplorationSection.RECOMMENDATIONS && rowIndex == restoreRow
                            ) {
                                { if (it) cardRestored = true }
                            } else null,
                        ) { localIndex, reportFocus, restoreFr, restoreOfc ->
                            val item = recommendations[rowStart + localIndex] as? RecommendedSubjectInfo
                            TvPortraitCard(
                                imageUrl = item?.imageLarge,
                                contentDescription = item?.nameCn,
                                onClick = {
                                    item?.let {
                                        navigateToSubject(it.bangumiId, it.nameCn, it.imageLarge, "home_recommendation")
                                    }
                                },
                                onFocused = {
                                    item?.let { onFocusItem(it.bangumiId, it.nameCn, null, false) }
                                    focusedSection = TvExplorationSection.RECOMMENDATIONS
                                    recFocusedRow = rowIndex
                                    focusedCardIndexInRow = localIndex
                                    reportFocus()
                                },
                                modifier = Modifier.width(TV_PAGE_CARD_WIDTH)
                                    // 播放/暂停键: 直接进播放器 (无进度从第一集; 信息未加载退化为详情)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown &&
                                            (event.key == Key.MediaPlayPause || event.key == Key.MediaPlay)
                                        ) {
                                            item?.let {
                                                navigateToPlay(
                                                    it.bangumiId, it.nameCn, it.imageLarge,
                                                    "home_recommendation_play",
                                                )
                                                true
                                            } ?: false
                                        } else {
                                            false
                                        }
                                    },
                                focusRequester = restoreFr,
                                onFocusChangedExtra = restoreOfc,
                                menu = item?.let { collectionMenuFor(it.bangumiId) },
                            )
                        }
                    }
                }
            }
        }

        // 轮播指示器 (不可聚焦): 垂直位置钉在 hero backdrop 下边界 (底边压线, 可用
        // TV_CAROUSEL_INDICATOR_EDGE_RAISE 上抬微调), 内容区水平居中; 仅展开态显示.
        if (focusedSection == TvExplorationSection.TRENDING && carouselSize > 1) {
            Box(
                Modifier.fillMaxWidth()
                    .fillMaxHeight(TV_EXPLORATION_BACKDROP_HEIGHT_FRACTION),
                contentAlignment = Alignment.BottomCenter,
            ) {
                TvCarouselIndicator(
                    count = carouselSize,
                    selectedIndex = carouselIndex.coerceIn(0, carouselSize - 1),
                    modifier = Modifier.padding(bottom = TV_CAROUSEL_INDICATOR_EDGE_RAISE),
                )
            }
        }

        // 底缘弱渐变遮罩 (页面背景色, smoothstep 采样无折点): 轻压被视口截断的下一行卡片,
        // 保证右下角提示在滚动的海报上仍可读. 只绘制, 不参与点击/焦点.
        run {
            val bg = MaterialTheme.colorScheme.background
            Box(
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(TV_PAGE_BOTTOM_SCRIM_HEIGHT)
                    .background(
                        Brush.verticalGradient(
                            *Array(11) { i ->
                                val f = i / 10f
                                val ease = f * f * (3f - 2f * f)
                                f to bg.copy(alpha = ease * TV_PAGE_BOTTOM_SCRIM_MAX_ALPHA)
                            },
                        ),
                    ),
            )
        }

        // 右下角遥控键提示 (参考 Prime 的同位置提示): 次要色低调常显
        Row(
            Modifier.align(Alignment.BottomEnd)
                .padding(end = TV_PAGE_END_PAD, bottom = TV_PAGE_HINT_BOTTOM_PAD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = null,
                Modifier.size(TV_PAGE_HINT_ICON_SIZE),
                tint = tvHeroSecondaryContentColor(),
            )
            Text(
                stringResource(Lang.tv_card_remote_hint),
                color = tvHeroSecondaryContentColor(),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/** 探索页卡片区的三个区块 (纵向吸顶滚动按此定位). */
private enum class TvExplorationSection {
    TRENDING,
    FOLLOWED,
    RECOMMENDATIONS,
}

/**
 * 固定锚点横向卡片行 (同选集轮播): 聚焦卡片始终吸附在行首, 按左右键时
 * 焦点视觉位置不动, 卡片列表整体滑过. 需在禁用 BringIntoView 的环境内使用 (滚动由本组件
 * 按聚焦下标显式驱动); 行尾留出整行空白让末卡也能吸附到行首.
 */
@Composable
private fun TvAnchoredCardRow(
    itemCount: Int,
    modifier: Modifier = Modifier,
    onNavigateUp: (() -> Boolean)? = null,
    loop: Boolean = false,
    /** 外部触发"聚焦本行首卡" (值增加即触发; 行未组合时无效, 调用方需先滚动让本行组合). */
    firstCardFocusSignal: Int = 0,
    // 从详情页返回时, 若本行是恢复目标行, 把此请求器挂到此前聚焦的那张卡上 (focusedIndex 已保存)
    restoreRequester: FocusRequester? = null,
    onRestoreFocusChanged: ((Boolean) -> Unit)? = null,
    card: @Composable (
        index: Int,
        reportFocus: () -> Unit,
        restoreRequester: FocusRequester?,
        onRestoreFocusChanged: ((Boolean) -> Unit)?,
    ) -> Unit,
) {
    // focusedIndex 用 rememberSaveable 跨导航保存: 返回时据此恢复本行横向滚动与恢复焦点目标卡
    var focusedIndex by rememberSaveable { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()
    // "聚焦本行首卡"执行机制 (由外部 firstCardFocusSignal 触发, 返回键决策在页面级统一做):
    // 目标卡拿到焦点才算完成 (可能要先滚动让它组合出来), 轮询重试.
    // 已处理信号记水位线 (rememberSaveable 随行存续): 行滚出视口再滚回重组时,
    // LaunchedEffect 会带着旧信号值重跑, 不能把它当成新触发抢焦点.
    var pendingBackToFirst by remember { mutableStateOf(false) }
    var handledFirstCardSignal by rememberSaveable { mutableIntStateOf(0) }
    val firstCardBackRequester = remember { FocusRequester() }
    LaunchedEffect(firstCardFocusSignal) {
        if (firstCardFocusSignal > handledFirstCardSignal) {
            handledFirstCardSignal = firstCardFocusSignal
            pendingBackToFirst = true
        }
    }
    LaunchedEffect(pendingBackToFirst) {
        if (!pendingBackToFirst) return@LaunchedEffect
        repeat(30) {
            withFrameNanos { }
            runCatching { listState.scrollToItem(0) }
            runCatching { firstCardBackRequester.requestFocus() }
            if (focusedIndex == 0) {
                pendingBackToFirst = false
                return@LaunchedEffect
            }
            delay(30)
        }
        pendingBackToFirst = false
    }
    LaunchedEffect(focusedIndex) {
        if (focusedIndex >= 0) listState.animateScrollToItem(focusedIndex)
    }
    // 横向循环: 用虚拟"无限"列表, 卡片按 index % itemCount 取 —— 右移到末卡再右即回到首卡.
    // 起点在 index 0 (首卡在最左), 左移到首卡再左则离开本行 (交给焦点系统 -> 侧边栏).
    val loopEnabled = loop && itemCount > 1
    val virtualCount = if (loopEnabled) Int.MAX_VALUE else itemCount
    BoxWithConstraints(
        modifier.then(
            if (onNavigateUp != null) {
                Modifier.onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                        onNavigateUp()
                    } else {
                        false
                    }
                }
            } else Modifier,
        ),
    ) {
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(TV_PAGE_CARD_SPACING),
            contentPadding = PaddingValues(
                end = (this.maxWidth - TV_PAGE_CARD_WIDTH).coerceAtLeast(0.dp),
            ),
        ) {
            items(virtualCount) { index ->
                val isRestoreItem = restoreRequester != null && index == focusedIndex
                // 首卡挂"返回回首卡"请求器 (挂容器上, requestFocus 委托给内部第一个焦点目标)
                Box(Modifier.ifThen(index == 0) { focusRequester(firstCardBackRequester) }) {
                    card(
                        if (loopEnabled) index % itemCount else index,
                        { focusedIndex = index },
                        if (isRestoreItem) restoreRequester else null,
                        if (isRestoreItem) onRestoreFocusChanged else null,
                    )
                }
            }
        }
    }
}

/** 聚焦卡片 → Hero 展示目标 (标题从卡片数据即时取得, 其余异步). */
private data class TvHeroTarget(
    val subjectId: Int,
    val title: String,
    /** 是否来自"继续观看"行: hero 背景优先展示下一集的 TMDB 单集剧照 (而非整部 backdrop). */
    val fromFollowed: Boolean = false,
)

/** 继续观看 hero 的下一集 TMDB 数据; [episodeId] 用于看完一集后 (下一集变化) 失效重查. */
private data class TvNextEpisodeMedia(
    val episodeId: Int,
    val stillUrl: String?,
    val overview: String?,
)

/**
 * 区块标题行. 只占文字自身高度 (标题到卡片行的距离统一由外部 [TV_SECTION_HEADER_TO_ROW_GAP] 控制,
 * 不再由固定盒高引入额外空白).
 * [transparent] 时文字不可见但仍占位: 聚焦区块自己的行内标题设为透明, 由顶部持久标题代替显示,
 * 避免"内容撑不满一屏无法滚动裁掉"时行内标题与持久标题重复; 透明保留高度, 切焦点无跳动.
 */
@Composable
private fun TvSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    transparent: Boolean = false,
) {
    Box(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            title,
            color = if (transparent) Color.Transparent else Color.Unspecified,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
    }
}

/**
 * 区块标题到卡片行的间距. 同时控制两种情形:
 * - 聚焦该区块时: 持久标题下方到吸顶卡片行的间距 (Spacer);
 * - 未聚焦时: 行内标题与卡片行 (及卡片行之间) 的 LazyColumn 竖向间距.
 * 一个参数保证两种情形距离一致.
 */
private val TV_SECTION_HEADER_TO_ROW_GAP = 12.dp

/**
 * 轮播指示器 (横排小圆点, 不可聚焦, 纯展示). 当前项为拉长胶囊, 其余为小圆点. 由外部左右键驱动
 * ([selectedIndex]), 自身不处理焦点与按键. 少于 2 项时不显示. 在给定宽度内水平居中.
 */
@Composable
private fun TvCarouselIndicator(
    count: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
) {
    if (count <= 1) return
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val active = i == selectedIndex
            Box(
                Modifier
                    .height(6.dp)
                    .width(if (active) 20.dp else 6.dp)
                    .background(
                        if (active) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        },
                        RoundedCornerShape(50),
                    ),
            )
        }
    }
}

/**
 * Hero 信息块固定高度 (标题 + 评分/连载行 + 简介). 简介用 weight 填满其下剩余空间 —— 调大此值
 * = 简介显示更多行; 固定高度保证不同条目切换 (简介长短不同) 时下方卡片区不跳动. 展开态 (焦点在 hero)
 * 时其下再叠加操作按钮块; 焦点移到卡片时按钮块消失, 卡片区顺势上移贴住信息块 (无大片空白).
 */
private val TV_HERO_INFO_HEIGHT = 200.dp

/** Hero 信息块与操作按钮块之间的间距 (较短, 让按钮贴近简介). */
private val TV_HERO_INFO_TO_BUTTONS_GAP = 6.dp

/** 两枚操作按钮之间的间距 (很短). */
private val TV_HERO_BUTTON_GAP = 4.dp

/** 按钮块下方到继续观看栏的间距 (取代原 40dp 空标题, 让继续观看紧贴按钮). */
private val TV_HERO_BUTTONS_TO_CONTENT_GAP = 12.dp

/**
 * 卡片区聚焦时介绍块的高度 (= 介绍块上下边界距离; 顶部固定). 比展开态 [TV_HERO_INFO_HEIGHT] 高一些,
 * 文字底对齐使其下边界随之下移并紧贴卡片行, 从而露出更多 backdrop、卡片不再上移遮挡. 调大则更靠下.
 */
private val TV_HERO_INFO_HEIGHT_COLLAPSED = 240.dp

/**
 * Hero "下一集"状态行的固定高度. 加上列间距 10dp 后恰为简介两行行距 (bodyMedium
 * lineHeight 20dp × 2), 使有无此行时简介行网格对齐 (见使用处).
 */
private val TV_HERO_STATUS_ROW_HEIGHT = 30.dp

/** 轮播指示器最多显示的圆点数 (同时也是自动轮播覆盖的条目数). */
private const val TV_CAROUSEL_MAX_DOTS = 20

/** 自动轮播切换间隔. */
private const val TV_CAROUSEL_AUTO_ADVANCE_MILLIS = 6000L

/** 轮播指示器相对 hero backdrop 下边界的上抬量 (0 = 指示器底边正好压在下边界上). */
private val TV_CAROUSEL_INDICATOR_EDGE_RAISE = 18.dp

/**
 * backdrop 下缘渐隐起点的轮播 (hero) 态档位 (图片高度坐标 0..1); 卡片聚焦态用三页
 * 共享的 [TV_BACKDROP_BOTTOM_FADE_START], 焦点移动时在两者间插值.
 */
private const val TV_EXPLORATION_BOTTOM_FADE_START_HERO = 0.88f

/**
 * 内容左侧额外留白 (外层 MainScreen 已让开侧边栏收起宽度 48dp, 总左缘 = 48 + 此值).
 * 默认 16 使总左缘 64, 侧边栏按钮中心 (32) 恰在屏幕左缘与内容左缘的正中间.
 */
private val TV_EXPLORATION_START_PAD = 16.dp

/** backdrop 高度占屏高比例 (Prime 实测: 图占屏顶约 76%, 渐变尾正好压在卡片区上缘之下). */
private const val TV_EXPLORATION_BACKDROP_HEIGHT_FRACTION = 0.66f

/** backdrop 两态渐变 (hero 态 <-> 卡片态) 切换动画时长. */
private const val TV_BACKDROP_STATE_ANIM_MILLIS = 400

/** 推荐区每行的条目数 (每行是一条固定锚点轮播, 行数随分页无限增长). */
private const val TV_EXPLORATION_REC_ROW_SIZE = 12

/** Bangumi 条目信息请求超时. */
private const val HERO_FETCH_TIMEOUT_MILLIS = 10_000L

