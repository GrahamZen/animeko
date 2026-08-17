/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.network

import androidx.compose.runtime.mutableStateMapOf
import androidx.datastore.core.DataStore
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import me.him188.ani.app.domain.foundation.HttpClientProvider
import me.him188.ani.app.domain.foundation.ServerListFeature
import me.him188.ani.app.domain.foundation.ServerListFeatureConfig
import me.him188.ani.app.domain.foundation.get
import me.him188.ani.app.domain.foundation.withValue
import me.him188.ani.app.domain.settings.NetworkTroubleBeacon
import me.him188.ani.app.platform.currentAniBuildConfig
import me.him188.ani.utils.coroutines.IO_
import me.him188.ani.utils.logging.info
import me.him188.ani.utils.logging.logger
import me.him188.ani.utils.logging.warn
import me.him188.ani.utils.platform.currentTimeMillis
import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.TimeSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * 从 TMDB 获取条目的横版背景图 (backdrop), 用于 TV 详情页 Hero 背景等.
 *
 * Bangumi 只有竖版封面; TMDB 的 backdrop 是"剧"级别的, 用日文原名搜索命中即可,
 * 搜不到时沿 Bangumi 关联条目回溯到根条目再搜 (见 [searchLayered]).
 * 不涉及季/集映射 (TMDB 与 Bangumi 的季划分对不齐的问题只影响以后的分集缩略图,
 * 届时匹配键须用分集播出日期而非集号, 见 fork 内验证: 無職転生 两 cour 合并为 TMDB S1,
 * 進撃の巨人 Final Season 的 Bangumi 60 话对应 TMDB S4E1).
 *
 * 结果按 subjectId 持久缓存 (含"确认无图"的负缓存, 存空串); 网络错误不缓存.
 * 未配置 `ani.tmdb.api.token` 时直接返回 null, 功能自动关闭.
 */
class TmdbImageService(
    httpClientProvider: HttpClientProvider,
    private val dataStore: DataStore<TmdbImageCache>,
    private val ioDispatcher: CoroutineContext = Dispatchers.IO_,
) {
    private val client = httpClientProvider.get()

    /**
     * Ani 的条目关系索引, 用于解析系列主条目名 (见 [resolveLineageViaAni]).
     *
     * 单独借一个带 [ServerListFeature] 的客户端: Ani 的接口 baseurl 是占位符, 要靠这个
     * feature 在可用服务器之间选路, 上面那个裸 client 拿不到.
     */
    private val aniRelationsApi = AniApiProvider(
        httpClientProvider.get(setOf(ServerListFeature.withValue(ServerListFeatureConfig.Default))),
    ).subjectRelationsApi

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 短连接超时: api.themoviedb.org 解析出的部分 IP 直连不通, 全局默认 30s 连接超时
     * 会让单个请求挂满半分钟才轮到重试 (实测 30183ms, 表现为 backdrop 半分钟才出来).
     * 5s 连不上就报错, 交给全局 HttpRequestRetry 换连接重试.
     */
    private fun HttpRequestBuilder.shortConnectTimeout() {
        timeout {
            connectTimeoutMillis = 5_000
            requestTimeoutMillis = 20_000
        }
    }

    /**
     * 关联回溯兜底专用的超时, 比 [shortConnectTimeout] 更短.
     *
     * 那只是"直搜没命中时去 Bangumi 要个根条目名"的兜底, 却卡在 hero 出图的关键路径上;
     * 而 `api.bgm.tv` 在部分网络下和 TMDB 一样被 DNS 投毒, 5 秒连接超时意味着 IPv4 + IPv6
     * 各等满 = 10 秒白等 (issue #7 报告者日志实测单次 9.2 秒). 2 秒够通的网络跑完一次
     * 关联查询, 不通的也早点让路去走削字兜底.
     */
    private fun HttpRequestBuilder.lineageTimeout() {
        timeout {
            connectTimeoutMillis = 2_000
            requestTimeoutMillis = 6_000
        }
    }

    /**
     * 当前生效的 API 域名下标 ([API_BASE_URLS]). 回退成功后记住, 免得之后每个请求都先在
     * 不通的那个上白等一次超时. 只在进程内有效, 冷启动重新从主域名开始试.
     */
    private var activeApiBaseIndex = 0

    /**
     * 用 Ani 的关系索引解析系列主条目名 —— 墙内可直连的那条路.
     *
     * 接口一次就返回 `seriesMainSubjectNames`, 既不必像 Bangumi 那条路逐跳回溯, 也不用再
     * 查一次条目详情拿名字. 上游已把绝大多数请求从 bgm 迁到自家服务器, 这里跟上.
     *
     * 代价是拿不到「主线故事」出边, 判不了衍生作, 所以 [BgmLineage.isDerivative] 给 **null
     * (未知)** 而不是 false —— false 的语义是"确认正传", 会让分集索引把 S0 殿后, 衍生条目
     * 就此错拿正片数据. 未知则维持原顺序, 与拿不到关系数据时一致.
     * 拿不到系列主条目 (或主条目就是自己) 时返回 null, 由调用方回落到 Bangumi.
     */
    private suspend fun resolveLineageViaAni(subjectId: Int, originalName: String): BgmLineage? = try {
        val relations = aniRelationsApi { getSubjectRelations(subjectId.toLong()).body() }
        val rootName = relations.seriesMainSubjectNames
            .firstOrNull { it.isNotBlank() && it != originalName }
        if (rootName == null) {
            null
        } else {
            logger.info { "Resolved lineage for $subjectId via Ani: root=$rootName" }
            BgmLineage(rootName = rootName, isDerivative = null)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.warn(e) { "Failed to resolve lineage via Ani relations for subject $subjectId" }
        null
    }

    /**
     * 发 TMDB API 请求, 主域名不通时自动换备用域名重试一次. [path] 以 `/` 开头, 不含 base.
     *
     * 只对连不上/超时这类异常回退: 4xx 是 token 或参数的问题, 换域名结果一样, 直接抛出.
     */
    private suspend fun HttpClient.getApi(
        path: String,
        block: HttpRequestBuilder.() -> Unit,
    ): HttpResponse {
        val firstIndex = activeApiBaseIndex
        try {
            return get("${API_BASE_URLS[firstIndex]}$path", block)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            throw e
        } catch (e: Exception) {
            val fallbackIndex = (firstIndex + 1) % API_BASE_URLS.size
            if (fallbackIndex == firstIndex) throw e
            logger.warn(e) {
                "TMDB API ${API_BASE_URLS[firstIndex]} unreachable, retrying on ${API_BASE_URLS[fallbackIndex]}"
            }
            val response = get("${API_BASE_URLS[fallbackIndex]}$path", block)
            // 成功了才记住, 免得一次偶发失败就把后续请求长期赶到备用域名上
            activeApiBaseIndex = fallbackIndex
            logger.info { "TMDB API base switched to ${API_BASE_URLS[fallbackIndex]}" }
            return response
        }
    }

    /**
     * Bangumi 关联接口连续失败的次数, 到 [LINEAGE_FAILURE_LIMIT] 就本次进程不再尝试.
     *
     * `api.bgm.tv` 在部分网络下被 DNS 投毒 (解析到无关 IP), 每次请求要 IPv4 + IPv6 各等满
     * 5 秒连接超时 = 10 秒; 而它卡在 backdrop 解析的关键路径上 (直搜没命中 → 回溯根条目名 →
     * 拿新名字重搜), 于是**每个**直搜未命中的条目都要白等这 10 秒 (issue #7 报告者日志实测).
     *
     * 这种不通是持续状态而不是偶发, 一直重试没有意义. 成功一次就清零 —— 临时抖动不该永久
     * 关掉这条兜底路径; 冷启动也重新计数, 免得用户换了网络还被上次的判定卡着.
     *
     * **原子量而不是普通 `var`**: 不同条目的解析是并发的 (只有同条目才合流, 见
     * [backdropInFlight]), 两个并发失败各读到 0 各写回 1 就丢掉一次计数, 熔断要多等一轮失败
     * 才跳; 非原子读还没有跨线程可见性保证. 清零与递增之间仍是"后写的赢" —— 那正是
     * "连续失败"该有的语义 (成功一次就该清零), 这里要修的只是别丢递增.
     */
    @OptIn(ExperimentalAtomicApi::class)
    private val lineageFailureStreak = AtomicInt(0)

    /**
     * 在途的 backdrop 解析, **精确按 subjectId 合流** (single-flight).
     *
     * hero 的聚焦请求和网格邻居预取几乎同时进来时, 两边都会读到"缓存里还没有"然后各自打一遍
     * 网络 —— 实测同一条目的 search/alternative_titles 整组请求发了两轮 (issue #7 报告者日志),
     * 在 2 秒/请求的线路上等于白白翻倍.
     *
     * **不能用分桶锁代替** (原来是 `subjectId % 16` 的 16 把锁): 撞桶时前台聚焦请求会排在一条
     * **无关**条目的后台预取后面, 而这台机器上后台预取挂死 10 秒是常态, 最坏顶到外层 15 秒
     * 超时 —— 等于把上层 [TvHeroPrefetch] 排好的前台优先级又随机打乱一次. 概率也不低: 网格页
     * 一步发四个邻居预取, 前台撞上任一个约 1/4.
     *
     * 精确合流还多一层收益: 同条目的后来者直接 `await` 同一个结果, 而不是排队再跑一遍缓存检查.
     * 表不会无限增长 —— 任务完成即摘除, 里面只有此刻真在解析的那几条.
     */
    private val backdropInFlight = mutableMapOf<Int, Deferred<String?>>()

    private val backdropInFlightLock = Mutex()

    /**
     * 承载合流任务的作用域: 任务不能挂在**发起者**的协程上 —— 发起者 (某次聚焦) 被取消时,
     * 合流到它身上的其他调用方不该跟着一起失败, 而且这条链跑完写进缓存对谁都有用.
     */
    private val resolveScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    /**
     * 代理设置页的连通性探测 —— **接口那一半**.
     *
     * 接口与图片本体是两个域名 (`api.tmdb.org` / `image.tmdb.org`), 在墙内**各自独立被墙**,
     * 而且方向常常相反: `api.themoviedb.org` 对大陆默认不通, 图床走 CDN 却正常 (issue #7 定论).
     * 所以这两半**分成两项各自出结果** —— 合成一个红叉的话, 用户分不清"挂代理只需覆盖接口"
     * 和"整个 TMDB 都不通", 而电视上导不出日志, 设置页那一行是唯一的自助反馈途径.
     *
     * 未配置 `ani.tmdb.api.token` 时直接算失败: 那种情况下整个功能本来就是关的
     * ([getBackdropUrl] 直接返回 null), 报"通"只会让人以为图马上就要出来了.
     *
     * 逐个域名试, 通的那个记进 [activeApiBaseIndex], 之后取图直接走它.
     *
     * **一次探测把该说的都写进日志**: 每个域名各自的耗时与失败原因、最终用了哪个 —— 用户点一次
     * 测试, 能导日志的场合就不必再来第二轮 (见 [logApiOutcome]).
     */
    suspend fun testApiConnection(): Boolean = withContext(ioDispatcher) {
        val token = currentAniBuildConfig.tmdbApiToken
        if (token.isBlank()) {
            // 不打这条的话, "这个包没配 token" 和 "网络不通" 在日志里长得一模一样 (都是静默失败)
            logger.warn { "TMDB API test: FAILED — no API token in this build" }
            return@withContext false
        }
        val attempts = mutableListOf<String>()
        val reachableIndex = API_BASE_URLS.indices.firstOrNull { index ->
            val base = API_BASE_URLS[index]
            val mark = TimeSource.Monotonic.markNow()
            try {
                // /configuration 是最轻的鉴权端点, 顺带验证 token 有效 (token 不对是 401)
                val status = client.use {
                    get("$base/configuration") {
                        bearerAuth(token)
                        shortConnectTimeout()
                        expectSuccess = false
                    }.status
                }
                attempts += "$base ${if (status.isSuccess()) "ok" else "$status"} in ${mark.elapsedNow().inWholeMilliseconds}ms"
                status.isSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                attempts += "$base ${e::class.simpleName ?: "error"} in ${mark.elapsedNow().inWholeMilliseconds}ms"
                false
            }
        }
        if (reachableIndex == null) {
            logger.warn { "TMDB API test: FAILED — ${attempts.joinToString("; ")}" }
            return@withContext false
        }
        activeApiBaseIndex = reachableIndex
        // 用到备用域名时明确说出来: 主用的 api.tmdb.org 是 TMDB 不宣传的历史别名, 哪天下线或
        // 被墙, 这行日志是唯一能看出来的地方
        logger.info {
            "TMDB API test: ok via ${API_BASE_URLS[reachableIndex]}" +
                    "${if (reachableIndex > 0) " (fallback)" else ""} — ${attempts.joinToString("; ")}"
        }
        true
    }

    /**
     * 代理设置页的连通性探测 —— **图片 CDN 那一半**, 与 [testApiConnection] 各自独立出结果.
     *
     * 只看能否拿到 HTTP 响应, 不看状态码 (见 [IMAGE_PROBE_URL]); 被墙的表现是连不上或超时.
     * 不检查 token: 图床是公开 CDN, 没 token 也该照常通 —— 这样"没配 token"就只让接口那项变红,
     * 两项一对照就能看出是配置问题而不是网络问题.
     */
    suspend fun testImageConnection(): Boolean = withContext(ioDispatcher) {
        val mark = TimeSource.Monotonic.markNow()
        try {
            client.use {
                head(IMAGE_PROBE_URL) {
                    shortConnectTimeout()
                    expectSuccess = false
                }
            }
            logger.info { "TMDB image CDN test: ok in ${mark.elapsedNow().inWholeMilliseconds}ms" }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(e) { "TMDB image CDN test: FAILED in ${mark.elapsedNow().inWholeMilliseconds}ms" }
            false
        }
    }

    /**
     * 本进程内已解析出结果的 backdrop: subjectId -> URL, **值为 `null` = 已确认无图**,
     * 键不存在 = 还没解析过. 这是**全应用唯一的一层进程级 backdrop 热缓存**.
     *
     * 存在的意义有两个:
     *  - **同步可读** (见 [peekBackdropUrl]): [getBackdropUrl] 即使全部命中持久缓存,
     *    也要走一次 `withContext(ioDispatcher)` + DataStore 读盘, 耗时随磁盘/GC 抖动 ——
     *    详情页首帧等不到它, 只能先按"加载中"渲染, 之后再把图淡进来.
     *  - **快照可观察**: TV 各页的 hero 背景直接在组合里读它 (经 `tvHeroBackdropUrl`),
     *    预取写入后自动重组. 曾经页面层还各自养过薄映射 (最多时四份互不相认), 同一部作品
     *    从哪个页面进详情页首帧长什么样取决于哪张表恰好有货 —— 现在只有这一张.
     *
     * 有界: 超过 [RESOLVED_HOT_CACHE_MAX] 时**按写入顺序淘汰最老的一批**, 不整表清空
     * (清空会让长会话里早就解析过的条目成批退化回"先空着再淡入", 还会卡住探索页的跳转门控).
     * 淘汰只丢热缓存, 持久层还在, 代价是那些条目下次要多一次读盘.
     *
     * 写入可能来自多个 IO 线程, 淘汰簿记用 [resolvedLock] 保护; SnapshotStateMap 自身线程安全.
     */
    private val resolvedBackdropUrls = mutableStateMapOf<Int, String?>()
    private val resolvedLock = SynchronizedObject()

    /** 写入顺序簿记, 只在 [resolvedLock] 里碰; 与表一起构成"淘汰最老"而不是"整表清空". */
    private val resolvedInsertionOrder = ArrayDeque<Int>()

    /**
     * 同步读取本进程**已经解析过**的 backdrop URL, 不发请求也不读盘.
     *
     * 给"上一个页面早就查过同一条目"的场景做首帧初值用 (TV 探索/搜索/时间表页聚焦时会预取
     * 背景图, 点进详情页时结果就在这张表里). 首帧直接拿到 URL 意味着图还在 Coil 内存缓存里,
     * 详情页 Hero 一进场就是满的, 没有"先空着再淡入"那一下.
     *
     * 在组合里读是**快照订阅**: 结果落表时读方自动重组.
     *
     * @return URL; `null` = 没有图 —— 可能是已确认无图, 也可能是还没解析过,
     *   两者要区分时用 [peekBackdropResolved] (不再用空串混编在返回值里).
     */
    fun peekBackdropUrl(subjectId: Int): String? = resolvedBackdropUrls[subjectId]

    /** 该条目是否已解析过 (含"已确认无图"). 与 [peekBackdropUrl] 一起构成三态. */
    fun peekBackdropResolved(subjectId: Int): Boolean = resolvedBackdropUrls.containsKey(subjectId)

    private fun rememberResolvedBackdrop(subjectId: Int, url: String?) {
        synchronized(resolvedLock) {
            if (subjectId !in resolvedBackdropUrls) {
                resolvedInsertionOrder.addLast(subjectId)
                // 淘汰一批而不是一条: 均摊掉每次写入都要动表的开销
                if (resolvedInsertionOrder.size > RESOLVED_HOT_CACHE_MAX) {
                    repeat(RESOLVED_HOT_CACHE_EVICT_BATCH) {
                        resolvedInsertionOrder.removeFirstOrNull()?.let { resolvedBackdropUrls.remove(it) }
                    }
                }
            }
            resolvedBackdropUrls[subjectId] = url
        }
    }

    /**
     * 获取条目横版背景图 URL (w1280). [originalName] 为日文原名 (SubjectInfo.name).
     * 找不到或未配置 token 时返回 null.
     *
     * @param activeAsOfDate 该条目最新已播集的日期 (`YYYY-MM-DD`), 拿不到分集时可传开播日期.
     *   决定负缓存的有效期 (见 [negativeCacheTtl]); 不传则负缓存永久有效 (旧行为).
     */
    suspend fun getBackdropUrl(
        subjectId: Int,
        originalName: String,
        activeAsOfDate: String? = null,
    ): String? {
        // 本进程已解析出 URL 的直接给结果, 连 withContext 与读盘都省掉 (正缓存永久有效, 读盘只会
        // 拿到同一个 URL).
        //
        // 负缓存 (值为 null) 故意不在这里短路: 它该不该重取取决于 activeAsOfDate 与重取闸门,
        // 而这张表只记结果不记时间, 短路会把"传了更近播出日期本该重取一次"的条目钉死到进程结束.
        // `map[id]` 对"值为 null"与"没解析过"都返回 null, 正好只短路正缓存.
        resolvedBackdropUrls[subjectId]?.let { return it }
        return resolveBackdropUrl(subjectId, originalName, activeAsOfDate)
    }

    /**
     * 读盘 / 走网络的慢路径, 仅由 [getBackdropUrl] 在进程内热缓存未命中时调用.
     * 同一条目的并发调用合流到同一个任务上, 见 [backdropInFlight].
     */
    private suspend fun resolveBackdropUrl(
        subjectId: Int,
        originalName: String,
        activeAsOfDate: String?,
    ): String? {
        if (currentAniBuildConfig.tmdbApiToken.isBlank() || originalName.isBlank()) return null
        val task = backdropInFlightLock.withLock {
            backdropInFlight[subjectId] ?: resolveScope.async {
                try {
                    doResolveBackdropUrl(subjectId, originalName, activeAsOfDate)
                } finally {
                    backdropInFlightLock.withLock { backdropInFlight.remove(subjectId) }
                }
            }.also { backdropInFlight[subjectId] = it }
        }
        return task.await()
    }

    private suspend fun doResolveBackdropUrl(
        subjectId: Int,
        originalName: String,
        activeAsOfDate: String?,
    ): String? = withContext(ioDispatcher) {
        run {
            // 合流等待期间前一个任务可能已经把这条解析完了, 再看一眼热表, 命中就连读盘都省了
            resolvedBackdropUrls[subjectId]?.let { return@withContext it }

            val cache = readCache()
            cache.backdropUrls[subjectId]?.let { cached ->
                if (cached.isNotEmpty()) {
                    // 正缓存永久有效: URL 拿到就不会变
                    rememberResolvedBackdrop(subjectId, cached)
                    return@withContext cached
                }
                // 负缓存: 过期才重取, 且闸门保证进程内每条目只放行一次 ——
                // TMDB 侧确实没图时, 反复进出详情页不会反复空拉
                val stale = negativeCacheStale(cache.backdropMissAt[subjectId], activeAsOfDate)
                if (!backdropRefreshGate.shouldRefresh(subjectId) { stale }) {
                    rememberResolvedBackdrop(subjectId, null)
                    return@withContext null
                }
                logger.info { "Retrying TMDB backdrop for subject $subjectId (negative cache expired)" }
            }

            val path = try {
                searchLayered(originalName, { resolveLineageOrNull(subjectId, originalName)?.rootName }) { query ->
                    searchBackdropPath(query)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn(e) { "Failed to search TMDB backdrop for subject $subjectId, will retry next time" }
                // 亮一下信标: 用户此刻正在浏览而背景图没出来, 下次打开动作面板就自动测一轮连通性
                NetworkTroubleBeacon.report("tmdb backdrop search failed for subject $subjectId")
                return@withContext null // 网络错误不写缓存, 下次进页面重试
            }

            val url = path?.let { "$IMAGE_BASE_URL$it" }
            logger.info { "TMDB backdrop for subject $subjectId: ${url ?: "not found"}" }
            dataStore.updateData { it.withBackdropResult(subjectId, url) }
            rememberResolvedBackdrop(subjectId, url)
            url
        }
    }

    /**
     * 获取条目在 TMDB 上的全部横版剧照 (backdrop) URL (w1280), 用于 TV 屏保轮播.
     *
     * 条目匹配与 [getBackdropUrl] 同一套三层搜索; 命中后再拉 `/images` 一次取全量
     * (不带 language 参数, backdrop 基本都是无语言图, 过滤反而会漏).
     * 找不到条目或未配置 token 时返回空列表, 调用方跳过该动画.
     * 结果按 subjectId 持久缓存 (空列表 = 已确认无图的负缓存); 网络错误不缓存.
     */
    suspend fun getAllBackdropUrls(
        subjectId: Int,
        originalName: String,
        activeAsOfDate: String? = null,
    ): List<String> = withContext(ioDispatcher) {
        if (currentAniBuildConfig.tmdbApiToken.isBlank() || originalName.isBlank()) return@withContext emptyList()

        val cache = readCache()
        cache.allBackdrops[subjectId]?.let { cached ->
            if (cached.isNotEmpty()) return@withContext cached
            val stale = negativeCacheStale(cache.allBackdropsMissAt[subjectId], activeAsOfDate)
            if (!allBackdropsRefreshGate.shouldRefresh(subjectId) { stale }) return@withContext cached
            logger.info { "Retrying TMDB backdrops for subject $subjectId (negative cache expired)" }
        }

        val urls = try {
            searchLayered(originalName, { resolveLineageOrNull(subjectId, originalName)?.rootName }) { query ->
                searchAnimeRef(query)
            }?.let { fetchBackdropPaths(it) }
                .orEmpty()
                .take(MAX_BACKDROPS_PER_SUBJECT)
                .map { "$IMAGE_BASE_URL$it" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(e) { "Failed to fetch TMDB backdrops for subject $subjectId, will retry next time" }
            return@withContext emptyList() // 网络错误不写缓存, 下次重试
        }

        logger.info { "TMDB backdrops for subject $subjectId: ${urls.size}" }
        dataStore.updateData {
            it.copy(
                allBackdrops = it.allBackdrops + (subjectId to urls),
                allBackdropsMissAt = if (urls.isNotEmpty()) {
                    it.allBackdropsMissAt - subjectId
                } else {
                    it.allBackdropsMissAt + (subjectId to currentTimeMillis())
                },
            )
        }
        urls
    }

    /** 跨类型取匹配条目的引用 (type + id), 档次顺序与 [searchBackdropPath] 一致. */
    private suspend fun searchAnimeRef(query: String): TmdbMediaRef? {
        val tv = searchAnime(query, "tv")
        tv.primary.firstNotNullOfOrNull { it.id }?.let { return TmdbMediaRef("tv", it) }
        val movie = searchAnime(query, "movie")
        return movie.primary.firstNotNullOfOrNull { it.id }?.let { TmdbMediaRef("movie", it) }
            ?: tv.fallback.firstNotNullOfOrNull { it.id }?.let { TmdbMediaRef("tv", it) }
            ?: movie.fallback.firstNotNullOfOrNull { it.id }?.let { TmdbMediaRef("movie", it) }
    }

    /** `/{type}/{id}/images` 的全部 backdrop 路径 (TMDB 已按投票排序). */
    private suspend fun fetchBackdropPaths(ref: TmdbMediaRef): List<String> = client.use {
        val body = getApi("/${ref.type}/${ref.id}/images") {
            bearerAuth(currentAniBuildConfig.tmdbApiToken)
            shortConnectTimeout()
        }.bodyAsText()
        json.decodeFromString(TmdbImagesResponse.serializer(), body).backdrops.mapNotNull { it.filePath }
    }

    /**
     * 获取条目所有分集数据 (缩略图 / 时长 / [language] 语言的简介) 索引.
     *
     * 主键是播出日期而非集号: TMDB 与 Bangumi 的季/集划分对不齐
     * (分割放送合并为一季、Bangumi 跨季连续编号), 播出日期是唯一可靠的对应关系.
     * 仅当 TMDB 上该剧只有一季正片时才另存按集号的索引 (此时两边集号一一对应),
     * 供 Bangumi 无分集播出日期的老番兜底 (如 1997 剑风传奇, Bangumi 全部分集无日期).
     *
     * 元数据按季一次性拉取 (一季一个请求) 并按 subjectId 持久缓存; 缓存记录抓取语言,
     * 用户切换 APP 语言后按新语言重取 (简介是本地化字段).
     * 图片本体由 UI 层 (LazyRow + coil) 惰性加载, 此处只返回 URL.
     *
     * **返回 null = 这次没拿到** (网络失败且没有可用旧缓存), 与"成功拉取但 TMDB 上确实没有
     * 剧照"(返回空的 [TmdbEpisodeStills]) 是两回事: 调用方若把前者也当成"确认无图"记进自己的
     * 缓存, 一次瞬时抖动就会让该条目在整个进程生命周期里再不重试.
     *
     * @param language TMDB 语言码 (如 `zh-CN`), 决定简介语言.
     * @param newestWantedAirDate 调用方希望缓存覆盖到的最新播出日期 (`YYYY-MM-DD`).
     *   缓存是按条目永久保存的, 连载番早先拉取的缓存不含之后新播的集; 传入此参数后,
     *   若缓存最新日期落后于它 (超出 ±1 天匹配容差), 经 [stillsRefreshGate] 放行
     *   (进程内每条目最多一次, 防 TMDB 自身滞后时反复空拉) 重取一次.
     */
    suspend fun getEpisodeStills(
        subjectId: Int,
        originalName: String,
        language: String,
        newestWantedAirDate: String? = null,
    ): TmdbEpisodeStills? =
        withContext(ioDispatcher) {
            if (currentAniBuildConfig.tmdbApiToken.isBlank() || originalName.isBlank()) {
                return@withContext TmdbEpisodeStills()
            }

            val cached = readCache().episodeStills[subjectId]?.takeIf { it.language == language }
            if (cached != null) {
                val refresh = newestWantedAirDate != null &&
                    stillsRefreshGate.shouldRefresh(subjectId) { !cached.coversAirDate(newestWantedAirDate) }
                if (!refresh) return@withContext cached
            }

            val stills = try {
                fetchEpisodeStills(subjectId, originalName, language)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn(e) { "Failed to fetch TMDB episode stills for subject $subjectId, will retry next time" }
                NetworkTroubleBeacon.report("tmdb episode stills failed for subject $subjectId")
                // 网络错误不写缓存; 陈旧重取失败时继续用旧缓存, 首次拉取失败返回 null (见 KDoc)
                return@withContext cached
            }

            // 陈旧重取拿到空结果 (如 TMDB 瞬时搜索不中) 时保留旧缓存, 不用坏数据覆盖好数据
            if (cached != null && stills.isEmpty() && !cached.isEmpty()) {
                logger.info { "TMDB episode stills refresh for subject $subjectId returned empty, keeping cached" }
                return@withContext cached
            }

            logger.info {
                "TMDB episode stills for subject $subjectId (lang=$language): " +
                    "${stills.byAirDate.size} by air date, ${stills.byEpisodeNumber.size} by episode number"
            }
            dataStore.updateData {
                it.copy(episodeStills = it.episodeStills + (subjectId to stills))
            }
            stills
        }

    /** 分集缓存的陈旧重取闸门: 进程内每条目最多放行一次, 见 [getEpisodeStills]. */
    private val stillsRefreshGate = StaleRefreshGate<Int>()

    /** backdrop 负缓存的重取闸门 (与 [allBackdropsRefreshGate] 分开计次), 见 [negativeCacheStale]. */
    private val backdropRefreshGate = StaleRefreshGate<Int>()

    /** 全量剧照负缓存的重取闸门. */
    private val allBackdropsRefreshGate = StaleRefreshGate<Int>()

    /**
     * 负缓存 ("TMDB 上没有这张图") 还能不能相信.
     *
     * 图和标题都是 TMDB 社区在开播后陆续补的, 所以新番的"没有"往往只是"还没有" ——
     * 一次空结果被永久缓存的后果是: 之后 TMDB 补了图, 这个条目也永远不会再查一次
     * (表现为"别人有图我没有", 而代理测试里 TMDB 全绿, 因为压根没发请求).
     *
     * @param missAt 负缓存写入时刻; null = 旧缓存没记时间, 给一次重取机会
     *   (这样就不必像匹配算法变更那样 bump [TmdbImageCache.CURRENT_VERSION] 作废整个缓存,
     *   代价从"所有条目重新搜索"降到"只重取负缓存那几条")
     */
    private fun negativeCacheStale(missAt: Long?, activeAsOfDate: String?): Boolean {
        if (missAt == null) return true
        val ttl = negativeCacheTtl(activeAsOfDate) ?: return false
        // 相减而非比较绝对值: 时钟回拨得到负数, 自然判为未过期, 不会因为系统时间乱跳而反复重取
        return currentTimeMillis() - missAt >= ttl.inWholeMilliseconds
    }

    /**
     * 负缓存有效期; null = 永久.
     *
     * 判据是"这部番有多活"而非"开播多久": 两年前开播但仍在连载的长番, 按开播日期会被误判成
     * 老番而拿到永久负缓存. 因此 [activeAsOfDate] 取最新已播集的日期 (口径同
     * [getEpisodeStills] 的 `newestWantedAirDate`), 调用方拿不到分集时退化为开播日期.
     */
    private fun negativeCacheTtl(activeAsOfDate: String?): Duration? {
        val aired = activeAsOfDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return when (aired.daysUntil(today)) {
            // 还在播或刚完结: TMDB 正在陆续补图, 最坏等三天
            in Int.MIN_VALUE..NEGATIVE_CACHE_AIRING_DAYS -> NEGATIVE_CACHE_TTL_AIRING
            // 补图概率已低, 但不能说没有
            in (NEGATIVE_CACHE_AIRING_DAYS + 1)..NEGATIVE_CACHE_RECENT_DAYS -> NEGATIVE_CACHE_TTL_RECENT
            // 一年都没人补, 基本不会再有; 屏保轮播会扫全部收藏, 老番参与重试会明显放大请求
            else -> null
        }
    }

    private suspend fun fetchEpisodeStills(
        subjectId: Int,
        originalName: String,
        language: String,
    ): TmdbEpisodeStills = client.use {
        val token = currentAniBuildConfig.tmdbApiToken
        // 血统判定在搜索前主动做 (搜索层只在直搜落空时才需要根条目名):
        // 建索引时要用"是否衍生作"决定 season 0 的取舍, 见下方季循环.
        val lineage = resolveLineageOrNull(subjectId, originalName)

        // 逐层搜索 (层次同 searchLayered), 对确认正传的条目多一条规则: 首候选 (完整条目名)
        // 的非精确标题命中不可信 —— 超集标题常是拆分出的兄弟条目 (如 "進撃の巨人 The Final
        // Season" 命中单集条目 "…完結編(後編)"), 正传季应归属母条目; 暂存该命中, 根条目名
        // 与削字候选全落空时才回退采用. 衍生作不受影响 (正确条目常常正是超集标题, 如
        // デート・ア・バレット 前編, 必须直搜命中).
        val nameCandidates = searchQueryCandidates(originalName)
        val tried = mutableSetOf<String>()
        var acceptedId: Int? = null
        var matchedQuery: String? = null
        var tentativeId: Int? = null
        suspend fun trySearch(query: String): Boolean {
            if (!tried.add(query)) return false
            val result = searchAnime(query, "tv")
                .let { it.primary.firstOrNull() ?: it.fallback.firstOrNull() } ?: return false
            val id = result.id ?: return false
            if (lineage?.isDerivative == false && query == nameCandidates.firstOrNull() &&
                !result.hasExactTitle(normalizeForMatch(query))
            ) {
                tentativeId = id
                return false
            }
            acceptedId = id
            matchedQuery = query
            return true
        }
        run {
            nameCandidates.firstOrNull()?.let { if (trySearch(it)) return@run }
            lineage?.rootName?.let { root ->
                searchQueryCandidates(root).forEach { if (trySearch(it)) return@run }
            }
            nameCandidates.drop(1).forEach { if (trySearch(it)) return@run }
        }
        if (acceptedId == null && tentativeId != null) {
            acceptedId = tentativeId
            matchedQuery = nameCandidates.firstOrNull()
        }
        val tvId = acceptedId ?: return@use TmdbEpisodeStills()
        // 排查错配时可据此人工核对 tvId 指向的剧对不对
        logger.info { "TMDB tv match for subject $subjectId: https://www.themoviedb.org/tv/$tvId" }

        // language: 顺带取整部剧的本地化简介 (Bangumi 简介为日文原文时整段替换用);
        // TMDB 无该语言翻译时 overview 为空串, 存 null 由 Bangumi 简介兜底
        val detailBody = getApi("/tv/$tvId") {
            parameter("language", language)
            bearerAuth(token)
            shortConnectTimeout()
        }.bodyAsText()
        val detail = json.decodeFromString(TmdbTvDetail.serializer(), detailBody)
        val seasons = detail.seasons
        val singleSeason = seasons.count { it.seasonNumber > 0 } == 1

        // 确认正传的条目把 season 0 (特别篇) 排在正片之后入索引: TMDB 常把同期放送的
        // 衍生短篇挂在正传条目的特别篇下, 且与正片同日播出 (如 Re:ゼロ休憩時間 4th 与
        // 正传 4th season 喪失編 逐集同日), S0 先入索引会让正传分集错拿短篇的数据 ——
        // 殿后使同日对位优先取正片; 不整个跳过是因为正传的第0话这类特别篇只存在于 S0
        // (如 無職転生Ⅱ 第0集), 日期只在 S0 出现时仍要能命中. 判定失败
        // (关系数据缺失/请求失败) 时维持原顺序.
        val specialsLast = lineage?.isDerivative == false
        // 反向: 衍生条目靠根条目名才归并到本篇的 (直搜自己的名字落空), 说明它没有独立
        // TMDB 条目, 分集必然在本篇的 S0 里 —— 只索引 S0. 否则正片与短篇播出日差 ±1 天时
        // (如 休憩時間 3rd 比正传晚一天), 精确日期会先命中正片, 衍生分集错拿正片数据.
        // 直搜命中自己条目的衍生作 (如有独立条目的外传) 走正常全量索引.
        val matched = matchedQuery
        val rootName = lineage?.rootName
        val specialsOnly = lineage?.isDerivative == true && matched != null &&
            rootName != null && matched in searchQueryCandidates(rootName)
        val indexedSeasons = when {
            specialsOnly -> seasons.filter { it.seasonNumber == 0 }
            specialsLast -> seasons.sortedBy { if (it.seasonNumber == 0) 1 else 0 }
            else -> seasons
        }
        val byAirDate = mutableMapOf<String, MutableList<TmdbEpisodeMedia>>()
        val byEpisodeNumber = mutableMapOf<Int, TmdbEpisodeMedia>()
        val specialsByNumber = mutableMapOf<Int, TmdbEpisodeMedia>()
        for (season in indexedSeasons) {
            // language: 分集简介取该语言的翻译 (无翻译时 overview 为空, 由 Bangumi 简介兜底);
            // still/时长/日期与语言无关.
            val seasonBody = getApi("/tv/$tvId/season/${season.seasonNumber}") {
                parameter("language", language)
                bearerAuth(token)
                shortConnectTimeout()
            }.bodyAsText()
            for (ep in json.decodeFromString(TmdbSeasonDetail.serializer(), seasonBody).episodes) {
                val media = TmdbEpisodeMedia(
                    stillUrl = ep.stillPath?.let { "$STILL_IMAGE_BASE_URL$it" },
                    runtimeMinutes = ep.runtime?.takeIf { it > 0 },
                    overview = ep.overview?.trim()?.takeIf { it.isNotBlank() },
                )
                // 同一天可能有多集 (双集连播首播, 如 無職転生Ⅲ 第1+2话), 按集号顺序追加成列表,
                // 匹配侧按 Bangumi "当日第几集" 对位取用. 字段全空的集也要占位, 保持对位不错乱.
                ep.airDate?.let { byAirDate.getOrPut(it) { mutableListOf() }.add(media) }
                if (singleSeason && season.seasonNumber == 1) {
                    ep.episodeNumber?.let { byEpisodeNumber[it] = media }
                }
                if (season.seasonNumber == 0) {
                    ep.episodeNumber?.let { specialsByNumber[it] = media }
                }
            }
        }

        // S0 集名索引 (剧的原语言): 特别篇在 Bangumi 与 TMDB 的播出日期记录常有出入
        // (如 転スラ "救われるラミリス 後編" 两边差 8 天), 日期对不上时匹配侧用
        // "集名精确一致"兜底. 名字必须按原语言再取一次 S0 —— 上面按 APP 语言取的是
        // 译名, 与 Bangumi 中文名是不同来源的译文, 几乎必然对不上 (菈/拉之差);
        // Bangumi 的原名与 TMDB 原语言名才能逐字一致. 重名的集直接全部丢弃, 保精度.
        val byName = mutableMapOf<String, TmdbEpisodeMedia?>()
        if (specialsByNumber.isNotEmpty()) {
            val originalLanguage = detail.originalLanguage?.takeIf { it.isNotBlank() } ?: "ja"
            val s0Body = getApi("/tv/$tvId/season/0") {
                parameter("language", originalLanguage)
                bearerAuth(token)
                shortConnectTimeout()
            }.bodyAsText()
            for (ep in json.decodeFromString(TmdbSeasonDetail.serializer(), s0Body).episodes) {
                val media = ep.episodeNumber?.let { specialsByNumber[it] } ?: continue
                val key = ep.name?.let { normalizeForMatch(it) }?.takeIf { it.isNotEmpty() } ?: continue
                byName[key] = if (key in byName) null else media
            }
        }

        TmdbEpisodeStills(
            byAirDate,
            byEpisodeNumber,
            language,
            showOverview = detail.overview?.trim()?.takeIf { it.isNotBlank() },
            specialsByName = byName.mapNotNull { (k, v) -> v?.let { k to it } }.toMap(),
        )
    }

    /**
     * 三层搜索, 层内层间都短路 (命中即停, 已试过的词不重试):
     *
     * 1. 原名直搜 — 有独立 TMDB 条目的剧场版/衍生作 (如 デート・ア・バレット) 必须先命中
     *    自己的条目, 回溯放前面会把它们错误归并到母番;
     * 2. Bangumi 关联条目回溯到根条目再搜 — 数据驱动, 覆盖 "Re:ゼロから始める休憩時間"
     *    这类换名短篇 (任何削字规则都不可解); 根条目名也过一遍削字候选;
     * 3. 削字规则兜底 — Bangumi 关系数据缺失的条目仍靠它.
     */
    private suspend fun <R : Any> searchLayered(
        originalName: String,
        resolveRootName: suspend () -> String?,
        search: suspend (query: String) -> R?,
    ): R? {
        val tried = mutableSetOf<String>()
        suspend fun trySearch(query: String): R? = if (tried.add(query)) search(query) else null

        val nameCandidates = searchQueryCandidates(originalName)
        nameCandidates.firstOrNull()?.let { trySearch(it) }?.let { return it }
        resolveRootName()?.let { rootName ->
            searchQueryCandidates(rootName).forEach { candidate ->
                trySearch(candidate)?.let { return it }
            }
        }
        nameCandidates.drop(1).forEach { candidate ->
            trySearch(candidate)?.let { return it }
        }
        return null
    }

    /**
     * 沿 Bangumi 关联条目回溯"血统": 每跳优先「主线故事」(从番外/短篇跳回本篇),
     * 其次「前传」(沿季链上溯), 走到没有出边为止 —— 通常是第一季, 名字最干净, 正对应
     * TMDB "一个剧条目含全部季"的组织方式. 带环路保护与跳数上限.
     *
     * 顺带判定正传/衍生: 链上任何一跳出现「主线故事」出边即为衍生 (衍生/番外条目才有
     * 这种指回本篇的边, 正传季只有前传/续集); 整链只走前传则确认正传. 时长比对方案
     * (正片 ~24min vs 短篇 ~2min) 曾作备选, 但未播出的集两边时长都缺, 关系判定不依赖
     * 播出数据, 更稳.
     *
     * 直接调 Bangumi v0 公开 API 而非 Ani API: 后者服务端会过滤掉「主线故事」关系
     * (实测 getRelatedSubjects 对 Re:ゼロ休憩時間只返回续集). 失败返回 null, 不影响兜底.
     */
    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun resolveLineageOrNull(subjectId: Int, originalName: String): BgmLineage? {
        // 先走 Ani 的关系索引: 墙内可直连, 一次请求直接拿到名字 (见 [resolveLineageViaAni]).
        // 它给不出系列主条目时才回落到下面的 Bangumi 逐跳回溯.
        resolveLineageViaAni(subjectId, originalName)?.let { return it }
        if (lineageFailureStreak.load() >= LINEAGE_FAILURE_LIMIT) return null
        return try {
            var currentId = subjectId
            var rootName: String? = null
            var sawMainStoryEdge = false
            val seen = mutableSetOf(subjectId)
            var hops = 0
            while (hops < MAX_RELATION_HOPS) {
                val body = client.use {
                    get("$BANGUMI_API_BASE_URL/v0/subjects/$currentId/subjects") {
                        lineageTimeout()
                    }.bodyAsText()
                }
                val relations = json.decodeFromString(ListSerializer(BgmRelatedSubject.serializer()), body)
                    .filter { it.type == BGM_SUBJECT_TYPE_ANIME }
                val mainStory = relations.firstOrNull { it.relation == "主线故事" }
                if (mainStory != null) sawMainStoryEdge = true
                val next = mainStory
                    ?: relations.firstOrNull { it.relation == "前传" }
                    ?: break
                if (!seen.add(next.id)) break
                currentId = next.id
                if (next.name.isNotBlank()) rootName = next.name
                hops++
            }
            lineageFailureStreak.store(0)
            BgmLineage(
                rootName = rootName?.takeIf { it != originalName },
                isDerivative = sawMainStoryEdge,
            ).also {
                logger.info {
                    "Resolved lineage for $subjectId: root=${it.rootName ?: "(self)"}, " +
                        "derivative=${it.isDerivative} ($hops hops)"
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 递增的结果留住自己用: 再 load 一次可能已经被别的条目改过, 日志与判定就对不上了
            val streak = lineageFailureStreak.fetchAndAdd(1) + 1
            logger.warn(e) {
                "Failed to resolve lineage via Bangumi relations for subject $subjectId " +
                    "($streak/$LINEAGE_FAILURE_LIMIT consecutive failures)"
            }
            if (streak >= LINEAGE_FAILURE_LIMIT) {
                logger.warn { "Bangumi relation lookups disabled for this session (api.bgm.tv unreachable)" }
            }
            null
        }
    }

    /**
     * 跨类型按信号强弱取 backdrop: tv 动画 → movie 动画 → tv 兜底 → movie 兜底.
     * 兜底档 (genre 缺失 + 日语原声) 必须排在两个类型的动画档之后 —— 否则舞台剧/纪录片
     * 这类无 genre 条目会抢在真正的动画前面 (实测 "千と千尋の神隠し" 的 tv 搜索首位是
     * 舞台剧纪录片, 无 genre、日语、标题含全部查询词, 正确的 movie 条目反而排在了后面).
     * tv 动画档命中时不发 movie 请求 (最常见情形保持单请求).
     */
    private suspend fun searchBackdropPath(query: String): String? {
        val tv = searchAnime(query, "tv")
        tv.primary.firstNotNullOfOrNull { it.backdropPath }?.let { return it }
        val movie = searchAnime(query, "movie")
        return movie.primary.firstNotNullOfOrNull { it.backdropPath }
            ?: tv.fallback.firstNotNullOfOrNull { it.backdropPath }
            ?: movie.fallback.firstNotNullOfOrNull { it.backdropPath }
    }

    /**
     * TMDB 搜索, 结果限定为动画且标题须与查询词逐词匹配.
     *
     * 动画过滤: TMDB 会把同名真人版排在动画前面 (如 ONE PIECE 首位是 Netflix 真人剧),
     * 必须按 genre 16 (Animation) 过滤; 个别条目缺失 genre 数据, 用日语原声兜底.
     * 全都不是动画时宁可不出图也不出真人版.
     *
     * 标题校验: TMDB 的模糊搜索对短查询词会返回貌似相关的错误条目 (实测 "うらおん!"
     * 返回 "うらみちお兄さん", "君の名は。" 的 tv 搜索返回 "君の魔名はリナ・ウィッチ..."),
     * 要求查询词的每个分词都作为子串出现在结果标题里 —— 标题多出词允许 (如
     * "デート・ア・バレット 前編 デッド・オア・バレット" 命中不带 "前編" 的查询词),
     * 插字/换字则拒绝. 校验失败宁可无结果, 交给下一层候选 (关联回溯/削字).
     */
    private suspend fun searchAnime(query: String, type: String): TmdbAnimeSearchResults = client.use {
        val body = getApi("/search/$type") {
            parameter("query", query)
            parameter("include_adult", "true")
            bearerAuth(currentAniBuildConfig.tmdbApiToken)
            shortConnectTimeout()
        }.bodyAsText()
        val tokens = tokenizeForMatch(query)
        val queryNormalized = normalizeForMatch(query)
        val results = json.decodeFromString(TmdbSearchResponse.serializer(), body).results
        val anime = results.filter { GENRE_ANIMATION in it.genreIds }
        val matched = anime.filter { it.matchesTokens(tokens) }
            .ifEmpty {
                // 主标题没匹配上时查别名再校验一次: TMDB 模糊搜索能命中而主标题不含查询词,
                // 通常是别名在起作用 (如 JoJo 主条目别名含 "スティール・ボール・ラン ジョジョの奇妙な冒険").
                // 只查最靠前的 2 个结果, 且仅发生在失败路径, 结果又按条目持久缓存, 成本一次性.
                anime.take(2).filter { result ->
                    val id = result.id ?: return@filter false
                    val altTitles = runCatching { fetchAlternativeTitles(id, type) }.getOrElse { emptyList() }
                    altTitles.isNotEmpty() && result.matchesTokens(tokens, altTitles)
                }
            }
            // 标题与查询完全一致的排最前: "标题多出词允许"会让外传/衍生作也通过校验
            // (如搜 DanMachi 正传名, TMDB 把外传 "ソード・オラトリア ...だろうか外伝" 排在
            // 正传前面, 外传标题包含完整正传名), 完全一致的正传必须优先; 稳定排序,
            // 无完全一致时保持 TMDB 原序 (前編/後編这类只有超集标题的场景不受影响).
            .sortedByDescending { it.hasExactTitle(queryNormalized) }
        TmdbAnimeSearchResults(
            primary = matched,
            // 兜底档只做主标题校验, 不值得为弱信号再发别名请求
            fallback = results.filter { it.genreIds.isEmpty() && it.originalLanguage == "ja" }
                .filter { it.matchesTokens(tokens) },
        )
    }

    private suspend fun fetchAlternativeTitles(id: Int, type: String): List<String> = client.use {
        val body = getApi("/$type/$id/alternative_titles") {
            bearerAuth(currentAniBuildConfig.tmdbApiToken)
            shortConnectTimeout()
        }.bodyAsText()
        val parsed = json.decodeFromString(TmdbAlternativeTitles.serializer(), body)
        (parsed.results + parsed.titles).mapNotNull { it.title }
    }

    /**
     * 生成搜索候选名, 依次尝试: 原名 → 去掉 OVA/OAD 类关键字 → 从季标记处截断 →
     * 去掉罗马数字季号 → 去掉尾部裸数字季号 → 末尾非文字字符逐个回退 →
     * (仅 OVA 条目) 逐词去尾回退到母番名.
     *
     * 候选是懒惰短路搜索的 (firstNotNullOfOrNull): 前面的候选命中后, 后面的不发请求;
     * 结果按条目持久缓存, 只有全部规则落空的条目才会把候选走到底, 多出的查询成本一次性.
     *
     * TMDB 把分割放送/续季并进同一个剧条目, 用 Bangumi 本季条目名常搜不到
     * (如 "無職転生 ～...～ 第2クール" 0 结果, 去后缀即命中); 季标记后面可能还跟着
     * 篇章名 (如 "Re:ゼロ... 4th season 喪失編"), 所以从标记处截断到串尾;
     * 序数词式 ("4th season") 与 "Season 4" 式都要认.
     *
     * OVA/OAD 在 TMDB 中是母番的特别篇 (season 0), 已被分集索引覆盖且按播出日期
     * (发售日) 可精确匹配 (实测 進撃の巨人 OAD、DanMachi 各季 OVA 均逐日对上),
     * 所以只需把条目名还原成母番名: 去掉关键字直接搜 (含副标题也常能命中, 如
     * "進撃の巨人 悔いなき選択"), 搜不到再逐词去掉尾部副标题.
     */
    private fun searchQueryCandidates(name: String): List<String> = buildList {
        fun addCandidate(candidate: String) {
            val trimmed = candidate.replace(Regex("""\s+"""), " ").trim()
            if (trimmed.isNotBlank() && trimmed !in this) add(trimmed)
        }
        addCandidate(name)

        val ovaMode = OVA_KEYWORD_REGEX.containsMatchIn(name)
        val base = if (ovaMode) name.replace(OVA_KEYWORD_REGEX, " ") else name
        addCandidate(base)

        val suffixStripped = base
            .replace(Regex("""第\s*\d+\s*(クール|期|部|シーズン|季).*$"""), "")
            .replace(
                Regex("""\s(?:(?:Part|Season|Cour)\s*\d+|\d+(?:st|nd|rd|th)\s+Season)\b.*$""", RegexOption.IGNORE_CASE),
                "",
            )
        addCandidate(suffixStripped)
        val romanStripped = suffixStripped.replace(Regex("""[ⅡⅢⅣⅤⅥⅦⅧⅨⅩ]"""), "")
        addCandidate(romanStripped)
        // 裸数字季号: 续季常直接在名字尾部跟数字 (如 "有頂天家族2" — TMDB 只有 "有頂天家族" 一个剧条目).
        // 只认 1-2 位, 3 位以上视为名字本体 (如 "モブサイコ100"); 且作为末位候选,
        // 仅在前面候选全部落空时才轮到, 名字本体恰好以数字结尾的条目会先被原名命中.
        // (下面的逐字符回退不适用纯拉丁名, 这条规则保留给它们, 如 "STEINS;GATE 0".)
        addCandidate(romanStripped.replace(Regex("""\s*[0-9０-９]{1,2}$"""), ""))

        // 末尾非文字字符逐个回退: 尾部季号/副标题形态繁多 (ASCII 罗马数字 "灼眼のシャナII"、
        // "R2"、"III -Final-" 等), 枚举不完; 从末尾逐字符去掉非日文/中文的字符, 每一步都
        // 作为候选 (先长后短, 更具体的先试). 要求剩余部分仍含日文/中文字符, 避免把
        // "BLEACH" 这类纯拉丁名逐字拆碎; 限最多回退 12 字符, 防病态长尾.
        var walked = romanStripped
        var steps = 0
        while (steps < 12) {
            val trimmed = walked.trimEnd()
            val last = trimmed.lastOrNull() ?: break
            if (last.isCjkOrKana()) break
            walked = trimmed.dropLast(1)
            steps++
            if (walked.none { it.isCjkOrKana() }) break
            addCandidate(walked)
        }

        if (ovaMode) {
            // OVA 副标题搜不到时逐词回退 (如 "進撃の巨人 悔いなき選択" → "進撃の巨人"), 最多 3 层
            var truncated = romanStripped.replace(Regex("""\s+"""), " ").trim()
            var depth = 0
            while (depth < 3 && truncated.contains(' ')) {
                truncated = truncated.substringBeforeLast(' ').trim()
                addCandidate(truncated)
                depth++
            }
        }
    }

    /**
     * 读缓存; 版本不符时整体作废重建 —— 匹配算法变更后旧结果可能是错的
     * (如动画过滤加入前 ONE PIECE 缓存了真人剧的 backdrop).
     */
    private suspend fun readCache(): TmdbImageCache {
        val cache = dataStore.data.first()
        if (cache.version == TmdbImageCache.CURRENT_VERSION) return cache
        return dataStore.updateData { TmdbImageCache(version = TmdbImageCache.CURRENT_VERSION) }
    }

    private companion object {
        private val logger = logger<TmdbImageService>()
        /**
         * TMDB API 的两个域名, 按优先级排列, 都是官方的: `api.tmdb.org` 的证书主体是
         * `CN=*.tmdb.org` (Amazon ACM 签发), 与 `api.themoviedb.org` 同为 CloudFront 后端,
         * 同一个 token 通用, 响应逐字节一致.
         *
         * 主用别名的原因: `api.themoviedb.org` 在中国大陆基本连不上 (TCP 超时, 不是 DNS
         * 投毒 —— 报告者开了加密 DNS 也救不回来, 家宽和移动流量都一样), 而封锁按 SNI 域名
         * 粒度做, 换个域名就绕开了. 图床 `image.tmdb.org` 一直是通的, 所以只要 API 能通,
         * 图就能出来 (issue #7).
         *
         * 不删掉 `api.themoviedb.org`: 别名是官方不宣传的历史域名, 可能下线或以后也被墙,
         * 留作回退. 见 [getApi].
         */
        private val API_BASE_URLS = listOf(
            "https://api.tmdb.org/3",
            "https://api.themoviedb.org/3",
        )
        private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w1280"

        /**
         * 图床连通性探测用的真实图片 (w92 档, 约 9 KB).
         *
         * 不探裸目录 `t/p/w1280`: 那个路径边缘不缓存, 每次都回源, 实测要 2.5 秒才吐一个 404,
         * 还偶发 `Connection reset` —— 一次探测两个请求就占掉 5 秒, 探测本身成了设置页
         * 那一行慢的主因 (issue #7 报告者日志). 真实图片命中边缘缓存, 快且稳, 顺带验证了
         * 图片确实下得下来.
         *
         * 这张图哪天被换掉也不影响判定: 这里只看能否拿到 HTTP 响应, 不看状态码 —— 404
         * 同样说明域名是通的, 被墙才会连不上或超时.
         */
        private const val IMAGE_PROBE_URL = "https://image.tmdb.org/t/p/w92/rBOnrVlck7BIlGeWVlzYiZeg4l2.jpg"
        private const val GENRE_ANIMATION = 16
        private const val BANGUMI_API_BASE_URL = "https://api.bgm.tv"
        private const val BGM_SUBJECT_TYPE_ANIME = 2

        /** 关联回溯跳数上限 (实测常见链 1-2 跳, 上限只是环路/脏数据保险). */
        private const val MAX_RELATION_HOPS = 8


        /**
         * 关联回溯连续失败多少次后本次进程放弃 (见 [lineageFailureStreak]).
         *
         * 取 2 而不是 1: 单次失败可能只是抖动, 两次连续失败基本可以断定这条网络到
         * `api.bgm.tv` 不通. 再大就没意义了 —— 每多试一次就是白等 10 秒.
         */
        private const val LINEAGE_FAILURE_LIMIT = 2

        /**
         * 进程内 backdrop 热表的容量上限. 远高于一次浏览会掠过的条目数 (单条只是一个 URL 字符串);
         * 超限按写入顺序淘汰最老一批 —— 见 [resolvedBackdropUrls] 处不许整表清空的原因.
         */
        private const val RESOLVED_HOT_CACHE_MAX = 600

        /** 超限时一次淘汰的条数 (均摊淘汰开销, 不必每次写入都动表). */
        private const val RESOLVED_HOT_CACHE_EVICT_BATCH = 100

        /** 最新已播集在此天数内 = 还在播或刚完结, 负缓存按 [NEGATIVE_CACHE_TTL_AIRING] 失效. */
        private const val NEGATIVE_CACHE_AIRING_DAYS = 60

        /** 最新已播集在此天数内 = 近作, 负缓存按 [NEGATIVE_CACHE_TTL_RECENT] 失效; 更早则永久. */
        private const val NEGATIVE_CACHE_RECENT_DAYS = 365

        private val NEGATIVE_CACHE_TTL_AIRING = 3.days
        private val NEGATIVE_CACHE_TTL_RECENT = 30.days

        /** 单条目剧照上限 (屏保轮播用不到更多, 控制缓存体积). */
        private const val MAX_BACKDROPS_PER_SUBJECT = 20

        /** OVA/OAD/特别篇类关键字: 触发母番名还原 (这些内容在 TMDB 里是母番的 season 0 特别篇). */
        private val OVA_KEYWORD_REGEX =
            Regex("""(?i)\b(?:OVA|OAD)S?\b|特別[編篇]|特别篇|スペシャル""")

        /**
         * 分集 still 官方档位只有 w92/w185/w300/original, w300 太糊, 存原图档 URL;
         * 消费端按用途降档: 选集卡片 [tmdbStillCardSizeUrl] (w780), 全屏 hero 背景
         * [tmdbStillHeroSizeUrl] (w1280) —— 都不直接解码原图.
         */
        private const val STILL_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/original"
    }
}

/**
 * 把 original 档的 TMDB still URL 降到卡片档: 选集卡片目标尺寸远小于全屏, 原图档
 * (1920 级, ~180KB) 的下载/解码是纯浪费. w780 不在 still 官方档位表里, 但 CDN
 * 实测同样支持 (~40KB), 且远高于卡片所需分辨率. 非 TMDB original URL 原样返回.
 */
fun tmdbStillCardSizeUrl(url: String): String = url.replace("/t/p/original/", "/t/p/w780/")

/**
 * 把 original 档的 TMDB still URL 降到全屏 hero 背景档 (探索/追番页"下一集剧照"背景):
 * w1280 铺 4K backdrop 约 2 倍放大, 经渐隐/压暗后 10-foot 距离不可辨; 原图档偶有 4K 级
 * (解码位图 8-33MB), 低端盒子上每次聚焦换卡都是一记下载+解码重锤 (2026-07-31 性能整改).
 * 非 TMDB original URL 原样返回.
 *
 * [fullQuality] 为真时原样返回 (设置里开了完整视觉效果, 见
 * [ThemeSettings.tvFullVisualEffects][me.him188.ani.app.data.models.preference.ThemeSettings]).
 * 因此**存缓存时不要降档**, 存原图档 URL, 由显示端按当前设置现降 —— 否则改设置要清缓存才生效.
 */
fun tmdbStillHeroSizeUrl(url: String, fullQuality: Boolean = false): String =
    if (fullQuality) url else url.replace("/t/p/original/", "/t/p/w1280/")

/** 日文假名/汉字 (含中文): 候选名末尾回退时视为名字本体, 到此为止不再往前剥. */
private fun Char.isCjkOrKana(): Boolean =
    this in '぀'..'ヿ' || // 平假名 + 片假名 (含长音符 ー)
        this in '一'..'鿿' || // CJK 统一汉字
        this == '々' // 々 (叠字符)

/**
 * 写入一条 backdrop 解析结果, **并把持久表压在上限内**.
 *
 * ## 为什么必须有上限
 *
 * `dataStore.updateData { it.copy(backdropUrls = map + entry) }` 是**整表复制 + 整个缓存重新
 * 序列化落盘**, 所以每解析一个新条目的写盘成本是 `O(已缓存条目数)`. 这张表原先没有任何上限
 * (旁边 [TmdbImageCache.backdropMissAt] 那句"免得随收藏量无限增长"说的是时间戳表, 主表漏了),
 * 于是成本随使用**单调上升且持久化** —— 重装前不会自愈.
 *
 * 从前增长速度是"用户真正聚焦过的条目", 一天几十条还能忍; 加了邻居预取之后每移动一格要解析
 * 最多四个条目 (聚焦 + 三个邻居), 增速直接翻几倍, 这个上限就成了必需品.
 *
 * ## 为什么是写入顺序而不是真 LRU
 *
 * 真 LRU 要在**每次读命中**时把条目移到队尾, 而读命中是最热的路径 (每次聚焦都查) —— 那等于
 * 把"零成本的读"变成"整表重写". 正缓存一旦写下就永久有效、后续全部走进程内热表短路, 所以
 * 写入顺序≈首次解析顺序, 淘汰最早解析的那批与 LRU 的差别在这里可以忽略.
 *
 * 淘汰按批 ([PERSISTED_BACKDROP_EVICT_BATCH]) 而不是每次挤掉一条, 免得到达上限之后每一次
 * 写入都要重算淘汰集。被淘汰的条目下次聚焦时重新走一次 TMDB, 只是慢一点, 不会出错。
 */
internal fun TmdbImageCache.withBackdropResult(subjectId: Int, url: String?): TmdbImageCache {
    val urls = backdropUrls + (subjectId to (url ?: ""))
    // 拿到图就清掉时间戳, 免得这个 map 随收藏量无限增长
    val missAt = if (url != null) {
        backdropMissAt - subjectId
    } else {
        backdropMissAt + (subjectId to currentTimeMillis())
    }
    if (urls.size <= PERSISTED_BACKDROP_MAX) {
        return copy(backdropUrls = urls, backdropMissAt = missAt)
    }
    // Map.plus 返回 LinkedHashMap, 反序列化出来的也是 —— keys 的迭代顺序就是写入顺序
    val dropped = urls.keys.take(urls.size - PERSISTED_BACKDROP_MAX + PERSISTED_BACKDROP_EVICT_BATCH).toSet()
    return copy(backdropUrls = urls - dropped, backdropMissAt = missAt - dropped)
}

/**
 * 持久 backdrop 表的条目上限. 一条约 60 字节 (URL) —— 2000 条约 120KB, 是每次新解析都要
 * 重新序列化的量, 再大就该换存储结构而不是抬上限了.
 */
private const val PERSISTED_BACKDROP_MAX = 2000

/** 到达上限后一次淘汰多少条 (均摊重算淘汰集的开销). */
private const val PERSISTED_BACKDROP_EVICT_BATCH = 200

@Serializable
data class TmdbImageCache(
    /** subjectId -> backdrop URL; 空串表示已确认 TMDB 无此条目图 (负缓存). */
    val backdropUrls: Map<Int, String> = emptyMap(),
    /** subjectId -> 分集缩略图 (按播出日期索引); 存在但为空 = 已确认无图 (负缓存). */
    val episodeStills: Map<Int, TmdbEpisodeStills> = emptyMap(),
    /** subjectId -> 全部横版剧照 URL (屏保轮播用); 空列表 = 已确认无图 (负缓存). 新字段有默认值, 不影响旧缓存. */
    val allBackdrops: Map<Int, List<String>> = emptyMap(),
    /**
     * subjectId -> [backdropUrls] 负缓存的写入时刻 (epoch millis), 决定它何时失效.
     * 新番的"没有 backdrop"通常只是"还没有" (TMDB 的图由社区在开播后陆续补), 见 [negativeCacheTtl].
     * 缺失 (旧缓存写下的负缓存) 视为已过期, 下次访问重取一次. 新字段有默认值, 不影响旧缓存.
     */
    val backdropMissAt: Map<Int, Long> = emptyMap(),
    /**
     * 同 [backdropMissAt], 对应 [allBackdrops].
     * 必须与前者分开存: 共用一份时间戳会让"单图重取成功后清除时间戳"把全量剧照的负缓存
     * 变成永久有效, 屏保轮播从此不再重试.
     */
    val allBackdropsMissAt: Map<Int, Long> = emptyMap(),
    /** 匹配算法版本, 与 [CURRENT_VERSION] 不符时整个缓存作废 (旧算法结果可能有误). */
    val version: Int = 0,
) {
    companion object {
        val Empty = TmdbImageCache()

        /**
         * v1: 搜索加入动画过滤 + 季后缀降级, 之前缓存的结果可能命中真人版, 作废.
         * v2: 分集缩略图增加单季剧的按集号索引, 旧缓存缺该字段, 作废重取.
         * v3: 季标记改为截断式且支持 "4th season" 序数词, 此前搜不到的条目留有负缓存, 作废.
         * v4: OVA/OAD 条目还原母番名搜索, 此前这类条目全是负缓存, 作废.
         * v5: 分集缩略图索引增加时长 (runtime) 字段, 旧缓存缺该数据, 作废重取.
         * v6: 支持尾部裸数字季号 (如 "有頂天家族2"), 此前这类条目全是负缓存, 作废.
         * v7: 末尾非文字字符逐个回退 (如 "灼眼のシャナII"), 同上作废负缓存.
         * v8: 新增 Bangumi 关联条目回溯层 (主线故事/前传归根), 同上作废负缓存.
         * v9: 搜索结果加标题逐词校验, 此前模糊搜索可能缓存了错误条目的图 (如
         *     "うらおん!" 命中 "うらみちお兄さん"), 作废.
         * v10: 标题校验放宽为跨标题并集 + 别名 (alternative_titles) 兜底, 混写名
         *      (BanG Dream! ゆめ∞みた) 与仅别名命中 (スティール・ボール・ラン) 的
         *      条目此前是负缓存, 作废.
         * v11: "genre 缺失 + 日语"兜底档降到所有类型的动画档之后, 此前可能缓存了
         *      舞台剧/纪录片的图 (如 千と千尋の神隠し 的舞台剧纪录片), 作废.
         * v12: 标题完全一致的结果优先于"标题多出词"的结果, 此前正传名可能命中标题
         *      包含正传全名的外传 (如 DanMachi 命中 ソード・オラトリア 外伝, 分集
         *      日期全对不上导致选集卡片无图), 作废.
         * v13: 播出日期索引改为"日期 -> 当日多集列表" (双集连播首播时后一集不再覆盖
         *      前一集, 如 無職転生Ⅲ 第1话曾显示第2话的图), 结构变更, 作废.
         * v14: 每集整合为单条目 (图 + 时长 + 新增本地化简介, 语言跟随 APP 设置), 结构变更, 作废.
         * v15: 新增整部剧的本地化简介 (showOverview, Bangumi 日文简介整段替换用), 旧缓存缺该字段, 作废重取.
         * v16: 按 Bangumi 关系链判定正传/衍生后取舍 season 0 —— 正传跳过 S0 (同期衍生短篇
         *      与正片同日播出时占据同一日期键且排在正片前面, 如 Re:ゼロ 4th season 喪失編
         *      逐集错拿休憩時間 4th 的数据); 归并到本篇的衍生条目只索引 S0 (播出日差 ±1 天时
         *      精确日期会先命中正片, 如 休憩時間 3rd 错拿正传的数据), 作废重取.
         * v17: 正传不再整个跳过 S0, 改为殿后入索引 (同日对位仍正片优先) —— 只存在于 S0 的
         *      第0话特别篇找回图 (如 無職転生Ⅱ 第0集); 正传首候选的非精确标题命中降级为
         *      暂存 (進撃の巨人 The Final Season 曾命中拆分的 完結編(後編) 单集条目);
         *      新增 S0 原语言集名索引, 日期对不上时按集名精确一致兜底 (転スラ
         *      救われるラミリス 後編 两边日期差 8 天). 结构变更, 作废重取.
         * v18: 标题校验加 Unicode 兼容折叠 (康熙部首/全角/罗马数字), 此前 TMDB 上用这类字符
         *      录入原名的条目 (如 乙女ゲー世界はモブに厳しい世界です 的 ⼄⼥) 搜索命中却被校验
         *      判为不匹配, 留下负缓存, 作废.
         */
        const val CURRENT_VERSION = 18
    }
}

/** TMDB 单个分集的展示数据: 缩略图 / 时长 / 简介, 均可缺失. */
@Serializable
data class TmdbEpisodeMedia(
    val stillUrl: String? = null,
    val runtimeMinutes: Int? = null,
    /** 分集简介 (按抓取语言本地化, 见 [TmdbEpisodeStills.language]; TMDB 无该语言翻译时为 null). */
    val overview: String? = null,
)

@Serializable
data class TmdbEpisodeStills(
    /**
     * 播出日期 `YYYY-MM-DD` -> 当日全部分集 (按集号升序).
     * 通常一天一集; 双集连播首播 (如 無職転生Ⅲ 第1+2话) 时一天多集,
     * 匹配侧按 Bangumi "当日第几集" 的序号对位.
     */
    val byAirDate: Map<String, List<TmdbEpisodeMedia>> = emptyMap(),
    /**
     * 集号 -> 分集数据; 仅当 TMDB 上该剧只有一季正片时非空
     * (多季时 Bangumi 连续编号与 TMDB 分季编号对不齐, 按集号匹配不可靠).
     * 供 Bangumi 分集无播出日期的老番兜底.
     */
    val byEpisodeNumber: Map<Int, TmdbEpisodeMedia> = emptyMap(),
    /** 抓取时用的 TMDB 语言码 (决定 overview 语言); 与当前 APP 语言不符时缓存不命中, 按新语言重取. */
    val language: String = "",
    /** 整部剧的本地化简介 ([language] 语言); TMDB 无该语言翻译或未匹配到剧时为 null. */
    val showOverview: String? = null,
    /**
     * season 0 特别篇的 "归一化原语言集名 -> 分集数据" 索引 (重名集已剔除).
     * 特别篇两边日期记录常有出入 (±1 天都够不着), 日期匹配落空时按集名精确一致兜底,
     * 用 [findSpecialByName] 查询.
     */
    val specialsByName: Map<String, TmdbEpisodeMedia> = emptyMap(),
) {
    /** 按集名 (原名/中文名等, 依次尝试) 精确匹配 season 0 特别篇; 名字归一化后比较. */
    fun findSpecialByName(vararg names: String?): TmdbEpisodeMedia? =
        names.firstNotNullOfOrNull { name ->
            name?.let { normalizeForMatch(it) }?.takeIf { it.isNotEmpty() }?.let { specialsByName[it] }
        }

    /**
     * 缓存是否已覆盖到播出日期 [date] (`YYYY-MM-DD`) 的分集.
     * 留 1 天余量 (两边日期常差一天, 匹配侧容差 ±1): 缓存最新日期 >= date-1 即视为覆盖.
     * 无任何按日期数据 (未匹配到剧/纯老番) 视为未覆盖, 由调用方的闸门限制重取频率.
     */
    fun coversAirDate(date: String): Boolean {
        val newestCached = byAirDate.keys.maxOrNull() ?: return false
        val wantedMinusSlack = runCatching {
            LocalDate.parse(date).minus(1, DateTimeUnit.DAY).toString()
        }.getOrElse { date }
        return newestCached >= wantedMinusSlack
    }

    /** 是否完全没有任何分集/剧集数据 (匹配失败或空剧). */
    fun isEmpty(): Boolean =
        byAirDate.isEmpty() && byEpisodeNumber.isEmpty() && specialsByName.isEmpty() && showOverview == null
}

/** TMDB 条目引用: 搜索命中的类型 (tv/movie) + id, 供 `/images` 等后续请求用. */
private class TmdbMediaRef(val type: String, val id: Int)

/** TMDB `/{type}/{id}/images` 响应 (只取 backdrops). */
@Serializable
private data class TmdbImagesResponse(
    val backdrops: List<TmdbImageFile> = emptyList(),
)

@Serializable
private data class TmdbImageFile(
    @SerialName("file_path") val filePath: String? = null,
)

/** 条目所属系列的回溯结果, 见 `resolveLineageOrNull`. */
private class BgmLineage(
    /** 根条目名 (通常是第一季); 与原名相同或没走到别的条目时为 null. */
    val rootName: String?,
    /**
     * 是否衍生/番外条目 (回溯链上出现过「主线故事」出边).
     * true = 衍生, false = 确认正传 (整链只有前传边), 建分集索引时可放心跳过 TMDB season 0 特别篇.
     *
     * **null = 未知**, 必须与 false 区分开: 走 Ani 关系索引那条路时拿不到「主线故事」出边
     * (见 `resolveLineageViaAni`), 若把未知当成"确认正传", 衍生条目的分集就会因为 S0 被殿后
     * 而错拿正片数据 —— 正是各处判定注释里警告的那种错序. 三处判定都写成 `== false` /
     * `== true` 的显式比较, null 自然落到"两边都不成立", 即维持原顺序.
     */
    val isDerivative: Boolean?,
)

/** Bangumi v0 `/subjects/{id}/subjects` 关联条目; relation 是中文关系名 ("前传"/"主线故事"...). */
@Serializable
private data class BgmRelatedSubject(
    val id: Int = 0,
    val type: Int = 0,
    val name: String = "",
    val relation: String = "",
)

@Serializable
private data class TmdbSearchResponse(
    val results: List<TmdbSearchResult> = emptyList(),
)

@Serializable
private data class TmdbSearchResult(
    val id: Int? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("original_name") val originalName: String? = null, // tv
    @SerialName("original_title") val originalTitle: String? = null, // movie
    val name: String? = null, // tv 本地化标题
    val title: String? = null, // movie 本地化标题
)

/** 查询词分词: 兼容折叠后按非字母/数字切开, 小写. 用于 [TmdbSearchResult.matchesTokens]. */
private fun tokenizeForMatch(query: String): List<String> =
    foldCompatibility(query).lowercase().split(Regex("""[^\p{L}\p{N}]+""")).filter { it.isNotBlank() }

/** 标题归一化: 兼容折叠后只保留字母/数字 (假名/汉字也是字母), 小写 —— 忽略标点/空白差异. */
private fun normalizeForMatch(s: String): String =
    foldCompatibility(s).lowercase().filter { it.isLetterOrDigit() }

/**
 * Unicode 兼容折叠 (NFKC 的子集; KMP 无标准库实现, 只覆盖标题里实际见过的类别):
 * 康熙部首 → 汉字, 全角字母/数字 → 半角, 罗马数字字符 → 拉丁字母.
 *
 * TMDB 标题由社区录入, 偶有用"看着一样但码位不同"的字符写的 —— 实测
 * "乙女ゲー世界はモブに厳しい世界です" 的原名开头是康熙部首 ⼄(U+2F04)⼥(U+2F25) 而非
 * 汉字 乙(U+4E59)女(U+5973). 搜索本身能命中 (TMDB 内部做了归一), 但标题校验逐字比较,
 * 不折叠就会把命中的正确条目判为不匹配, 表现为详情页无 backdrop、选集卡片无缩略图,
 * 且结果被负缓存. 查询词与标题两侧都要折叠才能对上.
 */
private fun foldCompatibility(s: String): String {
    if (s.none { it.compatibilityFoldOrNull() != null }) return s // 绝大多数标题无需折叠, 免去分配
    return buildString(s.length) {
        for (ch in s) append(ch.compatibilityFoldOrNull() ?: ch)
    }
}

private fun Char.compatibilityFoldOrNull(): String? = when (code) {
    in 0x2F00..0x2FD5 -> KANGXI_RADICALS[code - 0x2F00].toString()
    // 全角字母/数字与半角相差固定偏移 (全角标点会被 normalizeForMatch 直接滤掉, 无需折叠)
    in 0xFF10..0xFF19, in 0xFF21..0xFF3A, in 0xFF41..0xFF5A -> (code - 0xFEE0).toChar().toString()
    in 0x2160..0x2169 -> ROMAN_NUMERALS[code - 0x2160] // Ⅰ..Ⅹ
    in 0x2170..0x2179 -> ROMAN_NUMERALS[code - 0x2170] // ⅰ..ⅹ
    else -> null
}

/** 康熙部首 (U+2F00..U+2FD5) 按码位顺序对应的 CJK 统一汉字, 214 个. */
private const val KANGXI_RADICALS =
    "一丨丶丿乙亅二亠人儿入八冂冖冫几凵刀力勹匕匚匸十卜卩厂厶又口囗土士夂夊夕大女子宀" +
        "寸小尢尸屮山巛工己巾干幺广廴廾弋弓彐彡彳心戈戶手支攴文斗斤方无日曰月木欠止歹殳毋" +
        "比毛氏气水火爪父爻爿片牙牛犬玄玉瓜瓦甘生用田疋疒癶白皮皿目矛矢石示禸禾穴立竹米糸" +
        "缶网羊羽老而耒耳聿肉臣自至臼舌舛舟艮色艸虍虫血行衣襾見角言谷豆豕豸貝赤走足身車辛" +
        "辰辵邑酉釆里金長門阜隶隹雨靑非面革韋韭音頁風飛食首香馬骨高髟鬥鬯鬲鬼魚鳥鹵鹿麥麻" +
        "黃黍黑黹黽鼎鼓鼠鼻齊齒龍龜龠"

/** 罗马数字字符 (Ⅰ..Ⅹ / ⅰ..ⅹ) 的拉丁写法: 季号常见这种写法 (如 "無職転生Ⅱ" vs TMDB 的 "II"). */
private val ROMAN_NUMERALS = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")

/**
 * 查询词的每个分词都出现在该条目的某个标题 (原名/本地化名/[extraTitles] 别名) 里才算匹配.
 *
 * 是"分词 → 标题集合"的并集校验, 不要求单一标题全含: 混写名只能这样匹配 —— 如
 * "BanG Dream! ゆめ∞みた", TMDB 原名是假名写法 (バンドリ！ ゆめ∞みた)、英文名是
 * 罗马字写法 (BanG Dream! YUME∞MITA), 每个标题各覆盖一半分词. 每个分词仍必须
 * 能在官方标题集里找到, 插字/换字的错误条目 (如 "君の魔名は...") 依然会被拒.
 */
private fun TmdbSearchResult.matchesTokens(tokens: List<String>, extraTitles: List<String> = emptyList()): Boolean {
    if (tokens.isEmpty()) return false
    val titles = (listOfNotNull(originalName, originalTitle, name, title) + extraTitles)
        .map(::normalizeForMatch)
    return tokens.all { token -> titles.any { it.contains(token) } }
}

/** 是否有标题与查询完全一致 (归一化后). 用于在多个通过校验的结果中把正主排到外传/衍生作之前. */
private fun TmdbSearchResult.hasExactTitle(queryNormalized: String): Boolean =
    listOfNotNull(originalName, originalTitle, name, title)
        .any { normalizeForMatch(it) == queryNormalized }

/**
 * 动画搜索结果分两档: [primary] 确认为动画 (genre 16, 标题/别名校验通过);
 * [fallback] genre 数据缺失但日语原声的弱信号兜底 —— 调用方须把它排在所有类型的
 * [primary] 之后 (见 `searchBackdropPath`), 否则舞台剧/纪录片会抢在真正的动画前面.
 */
private class TmdbAnimeSearchResults(
    val primary: List<TmdbSearchResult>,
    val fallback: List<TmdbSearchResult>,
)

/** TMDB `/{type}/{id}/alternative_titles` 响应: tv 用 `results` 字段, movie 用 `titles`. */
@Serializable
private data class TmdbAlternativeTitles(
    val results: List<TmdbAltTitle> = emptyList(),
    val titles: List<TmdbAltTitle> = emptyList(),
)

@Serializable
private data class TmdbAltTitle(val title: String? = null)

@Serializable
private data class TmdbTvDetail(
    val seasons: List<TmdbSeasonRef> = emptyList(),
    /** 整部剧的简介 (按请求的 language 本地化; 无该语言翻译时 TMDB 返回空串). */
    val overview: String? = null,
    /** 剧的原语言 (如 "ja"); S0 集名索引按原语言取名, 与 Bangumi 原名可逐字比较. */
    @SerialName("original_language") val originalLanguage: String? = null,
)

@Serializable
private data class TmdbSeasonRef(
    @SerialName("season_number") val seasonNumber: Int = 0,
)

@Serializable
private data class TmdbSeasonDetail(
    val episodes: List<TmdbEpisodeRef> = emptyList(),
)

@Serializable
private data class TmdbEpisodeRef(
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("episode_number") val episodeNumber: Int? = null,
    val runtime: Int? = null,
    val overview: String? = null,
    /** 集名 (按请求的 language 本地化); 仅 S0 原语言二次请求时用于建集名索引. */
    val name: String? = null,
)
