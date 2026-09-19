/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.domain.mediasource.directapi

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import me.him188.ani.app.domain.foundation.DeviceBrowserUserAgentHolder
import me.him188.ani.app.domain.foundation.RequestUserAgentAttribute
import me.him188.ani.datasources.api.source.MediaFetchRequest
import me.him188.ani.datasources.api.source.direct.DirectLink
import me.him188.ani.utils.ktor.ScopedHttpClient
import me.him188.ani.utils.logging.logger
import me.him188.ani.utils.logging.warn

/**
 * 按 [DirectApiConfig] 执行"搜条目 -> 找剧集 -> 取地址"三步.
 *
 * 不含任何站点特有的逻辑: 站点的差异全部由配置表达.
 */
internal class DirectApiEngine(
    private val config: DirectApiConfig,
    private val client: ScopedHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val userAgent: String?
        get() = config.userAgent.takeIf { it.isNotBlank() } ?: DeviceBrowserUserAgentHolder.current

    /** bangumi 条目 id -> 站内条目 id. 站内搜索要好几次请求, 值得缓存, 包括"找不到"这个结果. */
    private val subjectIdCacheLock = Mutex()
    private val subjectIdCache = LinkedHashMap<String, String?>()

    suspend fun checkConnection(): Boolean {
        val url = buildUrl(config.subject.request.url, baseVariables() + ("subjectName" to "test"))
        return runCatching { fetchBytes(url) != null }.getOrElse { false }
    }

    suspend fun queryLinks(request: MediaFetchRequest): List<DirectLink> {
        val bangumiSubjectId = request.subjectId.takeIf { it.isNotBlank() } ?: return emptyList()
        val subjectId = resolveSubjectId(bangumiSubjectId, request.subjectNames) ?: return emptyList()

        val variables = baseVariables(request) + ("subjectId" to subjectId)
        val episodeValue = resolveEpisodeValue(request, variables) ?: return emptyList()

        return fetchLines(variables + ("episodeId" to episodeValue))
    }

    // ============================ 三步 ============================

    private suspend fun resolveSubjectId(bangumiSubjectId: String, subjectNames: List<String>): String? {
        subjectIdCacheLock.withLock {
            if (subjectIdCache.containsKey(bangumiSubjectId)) return subjectIdCache[bangumiSubjectId]
        }
        val resolved = searchSubjectId(bangumiSubjectId, subjectNames)
        subjectIdCacheLock.withLock {
            if (subjectIdCache.size >= SUBJECT_ID_CACHE_SIZE) {
                subjectIdCache.keys.firstOrNull()?.let { subjectIdCache.remove(it) }
            }
            subjectIdCache[bangumiSubjectId] = resolved
        }
        return resolved
    }

    private suspend fun searchSubjectId(bangumiSubjectId: String, subjectNames: List<String>): String? {
        val subject = config.subject
        for (name in subjectNames.filter { it.isNotBlank() }.take(subject.maxNames.coerceAtLeast(1))) {
            val variables = baseVariables() + mapOf("subjectName" to name, "bangumiSubjectId" to bangumiSubjectId)
            val items = fetchItems(subject.request, variables)
            for (item in items.take(subject.maxCandidates.coerceAtLeast(1))) {
                val candidateId = item.stringByPath(subject.idPath) ?: continue
                val verify = subject.verify
                    // 不校验就只能信搜索结果的第一条
                    ?: return candidateId
                val verifyRoot = fetchRoot(
                    verify,
                    variables + mapOf("candidateId" to candidateId, "subjectId" to candidateId),
                ) ?: continue
                if (verifyRoot.stringByPath(subject.verifyPath) == bangumiSubjectId) return candidateId
            }
        }
        return null
    }

    private suspend fun resolveEpisodeValue(request: MediaFetchRequest, variables: Map<String, String>): String? {
        val episode = config.episode
        val items = fetchItems(episode.request, variables)
        if (items.isEmpty()) return null

        // 有 bangumi 分集 id 就精确命中
        if (episode.matchPath.isNotBlank() && request.episodeId.isNotBlank()) {
            items.firstOrNull { item ->
                item.selectByPath(episode.matchPath).any { it.asStringOrNull() == request.episodeId }
            }?.let { return it.stringByPath(episode.valuePath) }
        }

        // 否则按集号. 注意站点的集号可能写成 "1.0" 或 "01", 所以按数值比
        if (episode.sortPath.isNotBlank()) {
            val target = request.episodeSort.toString().toFloatOrNull()
            if (target != null) {
                items.firstOrNull { item ->
                    item.stringByPath(episode.sortPath)?.toFloatOrNull() == target
                }?.let { return it.stringByPath(episode.valuePath) }
            }
        }
        return null
    }

    private suspend fun fetchLines(variables: Map<String, String>): List<DirectLink> {
        val lines = config.lines
        return buildDirectLinks(fetchItems(lines.request, variables), lines)
    }

    // ============================ 请求与取值 ============================

    private suspend fun fetchItems(
        request: DirectApiConfig.RequestConfig,
        variables: Map<String, String>,
    ): List<DataNode> = fetchRoot(request, variables)?.selectByPath(request.itemsPath).orEmpty()

    private suspend fun fetchRoot(
        request: DirectApiConfig.RequestConfig,
        variables: Map<String, String>,
    ): DataNode? {
        if (request.url.isBlank()) return null
        val bytes = fetchBytes(buildUrl(request.url, variables)) ?: return null
        return parseResponse(bytes, request.format, json)
    }

    private suspend fun fetchBytes(url: String): ByteArray? = try {
        client.use {
            get(url) {
                // client 自带的 UA 是写死的常量, 每台设备一样; 有本机 UA 就用本机的.
                // 走属性而不是直接写 header: client 的 UA 是 append 上去的, 直接写会变成两个值
                userAgent?.let { ua -> attributes.put(RequestUserAgentAttribute, ua) }
            }.readRawBytes()
        }
    } catch (e: Exception) {
        logger.warn(e) { "Request failed: $url" }
        null
    }

    private fun baseVariables(request: MediaFetchRequest? = null): Map<String, String> = buildMap {
        put("baseUrl", config.baseUrl.trimEnd('/'))
        if (request != null) {
            put("bangumiSubjectId", request.subjectId)
            put("bangumiEpisodeId", request.episodeId)
            put("episodeSort", request.episodeSort.toString())
            put("episodeEp", request.episodeEp?.toString().orEmpty())
            put("subjectName", request.subjectNames.firstOrNull().orEmpty())
        }
    }

    private fun buildUrl(template: String, variables: Map<String, String>): String {
        var result = template
        for ((name, value) in variables) {
            if (!result.contains("{$name}")) continue
            // baseUrl 本身带 "://" 和 "/", 不能编码
            val encoded = if (name == "baseUrl") value else value.encodeURLParameter()
            result = result.replace("{$name}", encoded)
        }
        return result
    }

    private companion object {
        private const val SUBJECT_ID_CACHE_SIZE = 64
        private val logger = logger<DirectApiEngine>()
    }
}

/**
 * 把取到的条目按 [lines] 组装成 [DirectLink]. 与 HTTP 无关, 便于用录下来的响应做测试.
 */
internal fun buildDirectLinks(items: List<DataNode>, lines: DirectApiConfig.LinesConfig): List<DirectLink> =
    items.mapNotNull { item -> toDirectLink(item, lines) }
        .distinctBy { it.url }
        .limitPerChannel(lines.maxPerChannel)

private fun toDirectLink(item: DataNode, lines: DirectApiConfig.LinesConfig): DirectLink? {
    val rawUrl = item.stringByPath(lines.urlPath) ?: return null
    val url = applyTransforms(rawUrl, lines.urlTransforms) ?: return null
    val channel = lines.channelPath.takeIf { it.isNotBlank() }
        ?.let { item.stringByPath(it) }
        ?.let { applyTransforms(it, lines.channelTransforms) }
        ?.let { lines.channelNames[it] ?: it }
    val subjectName = lines.subjectNamePath.takeIf { it.isNotBlank() }?.let { item.stringByPath(it) }
    val title = lines.titlePath.takeIf { it.isNotBlank() }?.let { item.stringByPath(it) }
    return DirectLink(
        url = url,
        title = listOfNotNull(subjectName, title).joinToString(" ").ifBlank { url },
        channel = channel?.takeIf { it.isNotBlank() },
        subjectName = subjectName,
    )
}

/** 一集可能有几十条地址, 同一条线路只留前几个, 免得淹没数据源选择器. */
private fun List<DirectLink>.limitPerChannel(max: Int): List<DirectLink> {
    if (max <= 0) return this
    val counts = HashMap<String, Int>()
    return filter { link ->
        val key = link.channel.orEmpty()
        val count = counts.getOrElse(key) { 0 }
        if (count >= max) {
            false
        } else {
            counts[key] = count + 1
            true
        }
    }
}
