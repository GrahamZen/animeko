/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.network

import androidx.datastore.core.DataStore
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import me.him188.ani.app.domain.foundation.HttpClientProvider
import me.him188.ani.app.domain.foundation.get
import me.him188.ani.app.platform.currentAniBuildConfig
import me.him188.ani.utils.coroutines.IO_
import me.him188.ani.utils.logging.info
import me.him188.ani.utils.logging.logger
import me.him188.ani.utils.logging.warn
import kotlin.coroutines.CoroutineContext

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
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 获取条目横版背景图 URL (w1280). [originalName] 为日文原名 (SubjectInfo.name).
     * 找不到或未配置 token 时返回 null.
     */
    suspend fun getBackdropUrl(subjectId: Int, originalName: String): String? = withContext(ioDispatcher) {
        if (currentAniBuildConfig.tmdbApiToken.isBlank() || originalName.isBlank()) return@withContext null

        readCache().backdropUrls[subjectId]?.let { cached ->
            return@withContext cached.ifEmpty { null }
        }

        val path = try {
            searchLayered(subjectId, originalName) { query -> searchBackdropPath(query) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(e) { "Failed to search TMDB backdrop for subject $subjectId, will retry next time" }
            return@withContext null // 网络错误不写缓存, 下次进页面重试
        }

        val url = path?.let { "$IMAGE_BASE_URL$it" }
        logger.info { "TMDB backdrop for subject $subjectId: ${url ?: "not found"}" }
        dataStore.updateData {
            it.copy(backdropUrls = it.backdropUrls + (subjectId to (url ?: "")))
        }
        url
    }

    /**
     * 获取条目所有分集缩略图索引.
     *
     * 主键是播出日期而非集号: TMDB 与 Bangumi 的季/集划分对不齐
     * (分割放送合并为一季、Bangumi 跨季连续编号), 播出日期是唯一可靠的对应关系.
     * 仅当 TMDB 上该剧只有一季正片时才另存按集号的索引 (此时两边集号一一对应),
     * 供 Bangumi 无分集播出日期的老番兜底 (如 1997 剑风传奇, Bangumi 全部分集无日期).
     *
     * 元数据按季一次性拉取 (一季一个请求) 并按 subjectId 持久缓存;
     * 图片本体由 UI 层 (LazyRow + coil) 惰性加载, 此处只返回 URL.
     */
    suspend fun getEpisodeStills(subjectId: Int, originalName: String): TmdbEpisodeStills =
        withContext(ioDispatcher) {
            if (currentAniBuildConfig.tmdbApiToken.isBlank() || originalName.isBlank()) {
                return@withContext TmdbEpisodeStills()
            }

            readCache().episodeStills[subjectId]?.let { return@withContext it }

            val stills = try {
                fetchEpisodeStills(subjectId, originalName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn(e) { "Failed to fetch TMDB episode stills for subject $subjectId, will retry next time" }
                return@withContext TmdbEpisodeStills() // 网络错误不写缓存, 下次进页面重试
            }

            logger.info {
                "TMDB episode stills for subject $subjectId: " +
                    "${stills.byAirDate.size} by air date, ${stills.byEpisodeNumber.size} by episode number"
            }
            dataStore.updateData {
                it.copy(episodeStills = it.episodeStills + (subjectId to stills))
            }
            stills
        }

    private suspend fun fetchEpisodeStills(subjectId: Int, originalName: String): TmdbEpisodeStills = client.use {
        val token = currentAniBuildConfig.tmdbApiToken
        val tvId = searchLayered(subjectId, originalName) { candidate ->
            searchAnime(candidate, "tv").let { it.primary.firstOrNull() ?: it.fallback.firstOrNull() }?.id
        } ?: return@use TmdbEpisodeStills()

        val detailBody = get("$API_BASE_URL/tv/$tvId") { bearerAuth(token) }.bodyAsText()
        val seasons = json.decodeFromString(TmdbTvDetail.serializer(), detailBody).seasons
        val singleSeason = seasons.count { it.seasonNumber > 0 } == 1

        val byAirDate = mutableMapOf<String, String>()
        val byEpisodeNumber = mutableMapOf<Int, String>()
        val runtimeByAirDate = mutableMapOf<String, Int>()
        val runtimeByEpisodeNumber = mutableMapOf<Int, Int>()
        for (season in seasons) {
            val seasonBody = get("$API_BASE_URL/tv/$tvId/season/${season.seasonNumber}") {
                bearerAuth(token)
            }.bodyAsText()
            for (ep in json.decodeFromString(TmdbSeasonDetail.serializer(), seasonBody).episodes) {
                val useEpisodeNumberKey = singleSeason && season.seasonNumber == 1
                // 时长与图分开记录: 没图的集也可能有时长
                ep.runtime?.takeIf { it > 0 }?.let { runtime ->
                    ep.airDate?.let { runtimeByAirDate[it] = runtime }
                    if (useEpisodeNumberKey) ep.episodeNumber?.let { runtimeByEpisodeNumber[it] = runtime }
                }
                val still = ep.stillPath ?: continue
                val url = "$STILL_IMAGE_BASE_URL$still"
                ep.airDate?.let { byAirDate[it] = url }
                if (useEpisodeNumberKey) {
                    ep.episodeNumber?.let { byEpisodeNumber[it] = url }
                }
            }
        }
        TmdbEpisodeStills(byAirDate, byEpisodeNumber, runtimeByAirDate, runtimeByEpisodeNumber)
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
        subjectId: Int,
        originalName: String,
        search: suspend (query: String) -> R?,
    ): R? {
        val tried = mutableSetOf<String>()
        suspend fun trySearch(query: String): R? = if (tried.add(query)) search(query) else null

        val nameCandidates = searchQueryCandidates(originalName)
        nameCandidates.firstOrNull()?.let { trySearch(it) }?.let { return it }
        resolveRootNameOrNull(subjectId, originalName)?.let { rootName ->
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
     * 沿 Bangumi 关联条目回溯到"根条目"名: 每跳优先「主线故事」(从番外/短篇跳回本篇),
     * 其次「前传」(沿季链上溯), 走到没有出边为止 —— 通常是第一季, 名字最干净, 正对应
     * TMDB "一个剧条目含全部季"的组织方式. 带环路保护与跳数上限.
     *
     * 直接调 Bangumi v0 公开 API 而非 Ani API: 后者服务端会过滤掉「主线故事」关系
     * (实测 getRelatedSubjects 对 Re:ゼロ休憩時間只返回续集). 失败返回 null, 不影响兜底.
     */
    private suspend fun resolveRootNameOrNull(subjectId: Int, originalName: String): String? = try {
        var currentId = subjectId
        var rootName: String? = null
        val seen = mutableSetOf(subjectId)
        var hops = 0
        while (hops < MAX_RELATION_HOPS) {
            val body = client.use {
                get("$BANGUMI_API_BASE_URL/v0/subjects/$currentId/subjects").bodyAsText()
            }
            val relations = json.decodeFromString(ListSerializer(BgmRelatedSubject.serializer()), body)
                .filter { it.type == BGM_SUBJECT_TYPE_ANIME }
            val next = relations.firstOrNull { it.relation == "主线故事" }
                ?: relations.firstOrNull { it.relation == "前传" }
                ?: break
            if (!seen.add(next.id)) break
            currentId = next.id
            if (next.name.isNotBlank()) rootName = next.name
            hops++
        }
        rootName?.takeIf { it != originalName }?.also {
            logger.info { "Resolved root subject for $subjectId via Bangumi relations: $it ($hops hops)" }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.warn(e) { "Failed to resolve root subject via Bangumi relations for subject $subjectId" }
        null
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
        val body = get("$API_BASE_URL/search/$type") {
            parameter("query", query)
            parameter("include_adult", "true")
            bearerAuth(currentAniBuildConfig.tmdbApiToken)
        }.bodyAsText()
        val tokens = tokenizeForMatch(query)
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
        TmdbAnimeSearchResults(
            primary = matched,
            // 兜底档只做主标题校验, 不值得为弱信号再发别名请求
            fallback = results.filter { it.genreIds.isEmpty() && it.originalLanguage == "ja" }
                .filter { it.matchesTokens(tokens) },
        )
    }

    private suspend fun fetchAlternativeTitles(id: Int, type: String): List<String> = client.use {
        val body = get("$API_BASE_URL/$type/$id/alternative_titles") {
            bearerAuth(currentAniBuildConfig.tmdbApiToken)
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
        private const val API_BASE_URL = "https://api.themoviedb.org/3"
        private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w1280"
        private const val GENRE_ANIMATION = 16
        private const val BANGUMI_API_BASE_URL = "https://api.bgm.tv"
        private const val BGM_SUBJECT_TYPE_ANIME = 2

        /** 关联回溯跳数上限 (实测常见链 1-2 跳, 上限只是环路/脏数据保险). */
        private const val MAX_RELATION_HOPS = 8

        /** OVA/OAD/特别篇类关键字: 触发母番名还原 (这些内容在 TMDB 里是母番的 season 0 特别篇). */
        private val OVA_KEYWORD_REGEX =
            Regex("""(?i)\b(?:OVA|OAD)S?\b|特別[編篇]|特别篇|スペシャル""")

        /** 分集 still 只有 w92/w185/w300/original 几档, w300 太糊, 用原图 (单张几十 KB, coil 有磁盘缓存). */
        private const val STILL_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/original"
    }
}

/** 日文假名/汉字 (含中文): 候选名末尾回退时视为名字本体, 到此为止不再往前剥. */
private fun Char.isCjkOrKana(): Boolean =
    this in '぀'..'ヿ' || // 平假名 + 片假名 (含长音符 ー)
        this in '一'..'鿿' || // CJK 统一汉字
        this == '々' // 々 (叠字符)

@Serializable
data class TmdbImageCache(
    /** subjectId -> backdrop URL; 空串表示已确认 TMDB 无此条目图 (负缓存). */
    val backdropUrls: Map<Int, String> = emptyMap(),
    /** subjectId -> 分集缩略图 (按播出日期索引); 存在但为空 = 已确认无图 (负缓存). */
    val episodeStills: Map<Int, TmdbEpisodeStills> = emptyMap(),
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
         */
        const val CURRENT_VERSION = 11
    }
}

@Serializable
data class TmdbEpisodeStills(
    /** 播出日期 `YYYY-MM-DD` -> still 图 URL. */
    val byAirDate: Map<String, String> = emptyMap(),
    /**
     * 集号 -> still 图 URL; 仅当 TMDB 上该剧只有一季正片时非空
     * (多季时 Bangumi 连续编号与 TMDB 分季编号对不齐, 按集号匹配不可靠).
     * 供 Bangumi 分集无播出日期的老番兜底.
     */
    val byEpisodeNumber: Map<Int, String> = emptyMap(),
    /** 播出日期 -> 分集时长 (分钟). Bangumi 侧无此数据, 只能靠 TMDB. */
    val runtimeByAirDate: Map<String, Int> = emptyMap(),
    /** 集号 -> 分集时长 (分钟); 键规则同 [byEpisodeNumber]. */
    val runtimeByEpisodeNumber: Map<Int, Int> = emptyMap(),
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

/** 查询词分词: 按非字母/数字切开, 小写. 用于 [TmdbSearchResult.matchesTokens]. */
private fun tokenizeForMatch(query: String): List<String> =
    query.lowercase().split(Regex("""[^\p{L}\p{N}]+""")).filter { it.isNotBlank() }

/** 标题归一化: 只保留字母/数字 (假名/汉字也是字母), 小写 —— 忽略标点/空白/全半角差异. */
private fun normalizeForMatch(s: String): String =
    s.lowercase().filter { it.isLetterOrDigit() }

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
)
