/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.network

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import me.him188.ani.app.data.models.recommend.RecommendedItemInfo
import me.him188.ani.app.data.models.recommend.RecommendedSubjectInfo
import me.him188.ani.app.data.models.subject.CanonicalTagKind
import me.him188.ani.app.data.network.mapper.orBangumiPlaceholder
import me.him188.ani.app.data.persistent.database.dao.RecommendationFeedDao
import me.him188.ani.app.data.persistent.database.dao.RecommendationFeedEntity
import me.him188.ani.app.data.persistent.database.dao.SubjectCollectionDao
import me.him188.ani.app.data.recommendation.InterestProfile
import me.him188.ani.app.data.recommendation.RecommendationGroup
import me.him188.ani.app.data.recommendation.RecommendationGroupKind
import me.him188.ani.app.data.recommendation.computeInterestProfile
import me.him188.ani.app.data.repository.Repository
import me.him188.ani.app.domain.search.SearchSort
import me.him188.ani.datasources.api.PackedDate
import me.him188.ani.datasources.bangumi.next.apis.SubjectBangumiNextApi
import me.him188.ani.datasources.bangumi.next.models.BangumiNextSubjectType
import me.him188.ani.utils.coroutines.IO_
import me.him188.ani.utils.ktor.ApiInvoker
import me.him188.ani.utils.logging.info
import me.him188.ani.utils.logging.warn
import me.him188.ani.utils.platform.currentTimeMillis
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 探索页推荐.
 *
 * **结果缓存在 Room 里, 进页只读缓存, 一个请求都不发** ([recommendationGroups]).
 * 改之前是"翻页时现算": 每翻一页拿几个种子、每个种子一个请求, 一次会话实测 946 个请求, 而且全都
 * 发生在用户正盯着这一页的时候. 现在一次重算封顶十来个请求, 且发生在首帧之后 (见 [requestRefresh]).
 *
 * 推荐分成几组, 各组来源不同 (见 [RecommendationGroupKind]):
 * 兴趣画像由**本地收藏**算出 (零请求, 见 [computeInterestProfile]), 各组据此去 bangumi 召回.
 * 只有"因为你喜欢《X》"这一组依赖种子的 `/p1/subjects/{id}/recs`; 其余几组走搜索与热门,
 * **都不需要登录**, 所以未登录用户的探索页照样是满的.
 */
class RecommendationRepository(
    private val bangumiSubjectApi: ApiInvoker<SubjectBangumiNextApi>,
    private val subjectCollectionDao: SubjectCollectionDao,
    private val feedDao: RecommendationFeedDao,
    private val trendsRepository: TrendsRepository,
    private val searchService: AniSubjectSearchService,
    /**
     * 重算跑在这个 scope 上, **不能挂调用方协程**: 页面那边是 `collectLatest` 收的, 换一次焦点
     * 就会把在途的重算连同落库一起掐掉 —— 详情页"连选集卡骨架都没有"就是这么来的.
     */
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineContext = Dispatchers.IO_,
) : Repository() {
    /** 分好组的推荐, 只读缓存表, 零请求. */
    fun recommendationGroups(): Flow<List<RecommendationGroup>> =
        feedDao.allFlow().map { rows ->
            // 表里已经是全局有序的, 相邻同组的连在一起, 顺序扫一遍就分完
            rows.groupBy { it.groupKey }
                .mapNotNull { (key, groupRows) ->
                    val kind = RecommendationGroupKind.ofKeyOrNull(key) ?: return@mapNotNull null
                    kind to groupRows
                }
                .sortedBy { (_, groupRows) -> groupRows.minOf { it.orderIndex } }
                .map { (kind, groupRows) ->
                    RecommendationGroup(
                        kind = kind,
                        titleArg = groupRows.firstNotNullOfOrNull { it.titleArg },
                        items = groupRows.sortedBy { it.orderIndex }.map { it.toInfo() },
                    )
                }
        }

    /** 拍平成一条流的推荐, 给还没按组画的界面用 (手机端竖排网格). */
    fun recommendedSubjectsPager(): Flow<PagingData<RecommendedItemInfo>> =
        Pager(
            // 关掉占位: 原来那个网络 PagingSource 不报总数, 界面一直是"没有 null 项"的形状;
            // Room 的 PagingSource 会报, 开着占位就会凭空多出 null 项来
            PagingConfig(pageSize = 30, enablePlaceholders = false),
        ) {
            feedDao.pagingSource()
        }.flow.map { data -> data.map { it.toInfo() } }

    private fun RecommendationFeedEntity.toInfo() = RecommendedSubjectInfo(
        bangumiId = subjectId,
        nameCn = nameCn,
        imageLarge = imageLarge,
    )

    private val refreshMutex = Mutex()

    /**
     * 请求重算一次. 缓存还新鲜就什么都不做; 同时只会有一次在跑.
     *
     * @param force 用户主动要"换一批"时传 true: 跳过新鲜度判断与首帧宽限, 立刻重算.
     */
    fun requestRefresh(force: Boolean = false) {
        // 已经有一次在跑就直接算数: 不必排队, 那次跑完的结果是一样的
        if (!refreshMutex.tryLock()) return
        scope.launch {
            try {
                refreshOnce(force)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 推荐算不出来不该牵连别的东西: 缓存里的旧结果照旧显示
                logger.warn(e) { "bgm-direct: recommendations 重算失败, 保留旧缓存" }
            } finally {
                refreshMutex.unlock()
            }
        }
    }

    private suspend fun refreshOnce(force: Boolean) {
        if (!force) {
            // 先判新鲜度再等 —— 反过来的话, 绝大多数进页都要白白挂几秒才发现无事可做
            val computedAt = feedDao.computedAt()
            val staleAlgo = (feedDao.algoVersion() ?: 0) != RecommendationFeedEntity.CURRENT_ALGO_VERSION
            if (!staleAlgo && computedAt != null && currentTimeMillis() - computedAt < TTL_MILLIS) {
                logger.info { "bgm-direct: recommendations 缓存仍新鲜, 跳过重算" }
                return
            }
            // 首帧宽限: 进页那一两秒在布局、拉封面与 hero 背景, 重算这时候插进去只会让首屏更慢,
            // 而它的结果本来就要等**下次**进页才用得上, 一点都不急.
            // **缓存空的时候不能这么等**: 那是装完/升级完第一次进页, 推荐区正空着, 等几秒
            // 就是干瞪眼几秒 —— 那一次结果是要当场用的.
            delay(if (computedAt == null) COLD_START_GRACE else FIRST_FRAME_GRACE)
        }

        val startMillis = currentTimeMillis()
        val (groups, requests) = withContext(ioDispatcher) { compute() }
        if (groups.isEmpty()) {
            // 一组都没算出来时**不要清空**: 宁可让用户接着看上一批
            logger.warn { "bgm-direct: recommendations 算出 0 组, 保留旧缓存" }
            return
        }

        val computedAt = currentTimeMillis()
        var orderIndex = 0
        val rows = groups.flatMap { group ->
            group.items.map { item ->
                RecommendationFeedEntity(
                    groupKey = group.kind.key,
                    orderIndex = orderIndex++,
                    subjectId = item.bangumiId,
                    nameCn = item.nameCn,
                    imageLarge = item.imageLarge,
                    computedAt = computedAt,
                    titleArg = group.titleArg,
                    algoVersion = RecommendationFeedEntity.CURRENT_ALGO_VERSION,
                )
            }
        }
        feedDao.replaceAll(rows)
        logger.info {
            "bgm-direct: recommendations 重算完成 ${rows.size} 条 / ${groups.size} 组 " +
                    "(${groups.joinToString { "${it.kind.key}=${it.items.size}" }}), " +
                    "$requests 个请求, ${computedAt - startMillis}ms"
        }
    }

    /** @return 算出来的分组, 与这次发了多少个请求 */
    private suspend fun compute(): Pair<List<RecommendationGroup>, Int> {
        val collections = subjectCollectionDao
            .mostRecentUpdated(limit = PROFILE_LOOKBACK, offset = 0)
            .first()
        val collected = collections.mapTo(HashSet()) { it.subjectId }
        val profile = computeInterestProfile(collections, currentTimeMillis())
        logger.info { "bgm-direct: recommendations 画像 收藏=${collections.size} $profile" }

        // 跨组去重: 同一部作品只在最靠前的那一组里出现. 命中多个兴趣不该变成重复出现
        val seen = HashSet<Int>()
        val groups = mutableListOf<RecommendationGroup>()
        var requests = 0

        fun takeGroup(
            kind: RecommendationGroupKind,
            candidates: List<RecommendedSubjectInfo>,
            titleArg: String? = null,
        ) {
            val items = candidates.asSequence()
                .filter { it.bangumiId !in collected && seen.add(it.bangumiId) }
                .take(GROUP_SIZE)
                .toList()
            if (items.size < MIN_GROUP_SIZE) {
                // 凑不满一行的组不如不要: 半行卡片在电视上很难看, 也说明这个方向没料
                logger.info { "bgm-direct: recommendations 组 ${kind.key} 只有 ${items.size} 条, 丢掉" }
                items.forEach { seen.remove(it.bangumiId) }
                return
            }
            groups += RecommendationGroup(kind, titleArg, items)
        }

        // ---- 1. 因为你喜欢《X》 ----
        // 种子按权重从高到低试, 直到有一部真的有推荐数据. **新番条目在 bangumi 上根本没有
        // `/recs`** (真机上高 id 种子一条不出), 所以必须允许换一部再试, 不能只认第一个.
        for (seed in profile.seeds) {
            if (requests >= MAX_SEED_REQUESTS) break
            val recs = fetchRecs(seed.subjectId)
            requests++
            val before = groups.size
            takeGroup(RecommendationGroupKind.BECAUSE_YOU_LIKED, recs, titleArg = seed.name)
            if (groups.size > before) break
        }

        // ---- 2. 符合你口味的高分动画 / 高分经典 ----
        // 用户的高权重标签各搜一次再合并成一组, **不是每个标签单独一行** —— "校园""恋爱""日常"
        // 各来一行的话, 三行内容高度重复, 看着像同一行抄了三遍.
        val tagCandidates = mutableListOf<RecommendedSubjectInfo>()
        for (tag in profile.tags.take(MAX_TAG_QUERIES)) {
            tagCandidates += searchByTag(tag.name)?.also { requests++ } ?: continue
        }
        if (tagCandidates.isNotEmpty()) {
            takeGroup(RecommendationGroupKind.FOR_YOU_HIGH_RATED, tagCandidates.shuffled())
        } else {
            // 没有画像 (未登录 / 新装) 就退成纯排行榜. 起点随机, 免得所有人都是同一批殿堂老番
            searchTopRated()?.let { requests++; takeGroup(RecommendationGroupKind.TOP_RATED, it) }
        }

        // ---- 3. 本季你可能会喜欢 ----
        // 不看画像也成立, 而且能防止首页永远都是老作品
        searchThisSeason()?.let { requests++; takeGroup(RecommendationGroupKind.THIS_SEASON, it) }

        // ---- 4. 换换口味 ----
        // 与最强兴趣**同一类**但用户没碰过的方向: 喜欢"治愈"就试"温情/纯爱", 有连接点又不重复
        neighborTagOf(profile)?.let { neighbor ->
            searchByTag(neighbor)?.let { requests++; takeGroup(RecommendationGroupKind.CHANGE_TASTE, it) }
        }

        // ---- 5. 大家最近在看 ----
        runCatching { trendsRepository.getTrendsInfo() }
            .onSuccess { trends ->
                requests++
                takeGroup(
                    RecommendationGroupKind.TRENDING,
                    trends.subjects.map {
                        RecommendedSubjectInfo(it.bangumiId, it.nameCn, it.imageLarge)
                    },
                )
            }
            .onFailure { logger.warn(it) { "bgm-direct: recommendations 热门失败" } }

        return groups to requests
    }

    private suspend fun fetchRecs(subjectId: Int): List<RecommendedSubjectInfo> = runCatching {
        bangumiSubjectApi {
            coroutineScope { getSubjectRecs(subjectId, limit = RECS_PER_SEED).body().data }
        }
    }.getOrElse {
        logger.warn(it) { "bgm-direct: recommendations 取 $subjectId 的 recs 失败" }
        emptyList()
    }.mapNotNull { rec ->
        val subject = rec.subject
        // 「看过这部的人也看过」不分条目类型, 漫画/游戏/三次元都会混进来 (实测 276792 的
        // 10 条推荐里有 3 条不是动画). 探索页只放动画.
        if (subject.type != BangumiNextSubjectType.Anime) return@mapNotNull null
        RecommendedSubjectInfo(
            bangumiId = subject.id,
            nameCn = subject.nameCN.ifEmpty { subject.name },
            imageLarge = subject.images?.large.orBangumiPlaceholder(),
        )
    }

    private suspend fun searchByTag(tag: String): List<RecommendedSubjectInfo>? =
        search(SubjectSearchFilters(tags = listOf(tag), ranks = listOf(">=1"), nsfw = false), SearchSort.RANK, "tag=$tag")

    private suspend fun searchTopRated(): List<RecommendedSubjectInfo>? =
        search(
            SubjectSearchFilters(ranks = listOf(">=1"), nsfw = false),
            SearchSort.RANK,
            "topRated",
            offset = Random.nextInt(TOP_RATED_OFFSET_RANGE),
        )

    private suspend fun searchThisSeason(): List<RecommendedSubjectInfo>? =
        search(
            SubjectSearchFilters(airDates = listOf(">=${currentSeasonStart()}"), nsfw = false),
            // 本季不能按排名排: 新番大多还没有排名, ranks>=1 会把整季筛空. 按热度来
            SearchSort.COLLECTION,
            "thisSeason",
        )

    private suspend fun search(
        filters: SubjectSearchFilters,
        sort: SearchSort,
        what: String,
        offset: Int = 0,
    ): List<RecommendedSubjectInfo>? = runCatching {
        searchService.searchSubjects(
            keyword = "",
            offset = offset,
            limit = SEARCH_PAGE_SIZE,
            sort = sort,
            filters = filters,
        )
    }.onFailure {
        logger.warn(it) { "bgm-direct: recommendations 搜索 $what 失败" }
    }.getOrNull()?.map { details ->
        val info = details.subjectInfo
        RecommendedSubjectInfo(
            bangumiId = info.subjectId,
            nameCn = info.nameCn.ifEmpty { info.name },
            imageLarge = info.imageLarge,
        )
    }

    /**
     * 与最强兴趣同类、但用户没碰过的一个标签.
     *
     * 同类是关键: 从"情绪"里挑另一个情绪标签, 与已有兴趣有一两个连接点, 而不是随便扔一个
     * 完全不相干的方向过去.
     */
    private fun neighborTagOf(profile: InterestProfile): String? {
        val top = profile.tags.firstOrNull() ?: return null
        val kind = CanonicalTagKind.matchOrNull(top.name) ?: return null
        val own = profile.tags.mapTo(HashSet()) { it.name }
        return kind.values.filter { it !in own }.randomOrNull()
    }

    /** 本季开始那天, `YYYY-MM-01`. */
    private fun currentSeasonStart(): String {
        val today = PackedDate.now()
        val month = when (today.month) {
            in 1..3 -> 1
            in 4..6 -> 4
            in 7..9 -> 7
            else -> 10
        }
        return "${today.year}-${month.toString().padStart(2, '0')}-01"
    }

    private companion object {
        /** 缓存多久算过期. 推荐不是时效性内容, 不必勤快. */
        val TTL_MILLIS = 12.hours.inWholeMilliseconds

        /** 缓存里已有结果时, 进页到开始重算之间等多久 —— 让首屏先画完. */
        val FIRST_FRAME_GRACE = 3.seconds

        /** 缓存是空的 (装完第一次进页) 时等多久: 只让首帧过去, 别让推荐区空着干等. */
        val COLD_START_GRACE = 500.milliseconds

        /** 每组几条. 与电视上一行的卡片数一致, 一组正好一行. */
        const val GROUP_SIZE = 12

        /** 少于这个数的组直接丢掉, 不画半行. */
        const val MIN_GROUP_SIZE = 6

        /** "因为你喜欢"最多试几个种子 (每个一个请求). */
        const val MAX_SEED_REQUESTS = 3

        /** 高分组最多用几个兴趣标签 (每个一个请求). */
        const val MAX_TAG_QUERIES = 3

        /** 每次搜索要几条. bangumi 的 limit 上限是 50. */
        const val SEARCH_PAGE_SIZE = 50

        /** 纯排行榜那一组的起点在 [0, 这个数) 里随机. 再往后就不算"高分"了. */
        const val TOP_RATED_OFFSET_RANGE = 300

        /** `/recs` 的 limit 上限是 10. */
        const val RECS_PER_SEED = 10

        /** 算画像时往回看多少条收藏. 全表拉出来只为算个画像不值当. */
        const val PROFILE_LOOKBACK = 500
    }
}
