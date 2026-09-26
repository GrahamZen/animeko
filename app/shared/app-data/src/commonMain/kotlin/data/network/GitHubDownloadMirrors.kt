/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.network

import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import me.him188.ani.utils.ktor.ScopedHttpClient
import me.him188.ani.utils.logging.info
import me.him188.ani.utils.logging.logger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

/**
 * GitHub release 下载 (跳板包下载新应用) 的加速镜像.
 *
 * 大陆常见 api.github.com 与 jsDelivr 能通、github.com 的下载不通或极慢. 公共加速站存活期短, 各地、各运营商通不通也不一样,
 * 所以清单放在新应用仓库根目录的 `github-download-mirrors.json` (新应用的应用内更新用的是同一份), 死一个换一个不用发版;
 * 下载时原地址与所有镜像一起试, 用最快的那个 (见 `DefaultFileDownloader`).
 *
 * 跳板包只下载一次新应用, 清单不存: 建出来就开始拉 (jsDelivr 在前, raw 兜底), 拉不到或等不及用内置的.
 *
 * 条目两种写法:
 * - 前缀 `https://域名[/前缀]`: 原地址整个接在后面, 如 `https://gh-proxy.com/https://github.com/…`;
 * - 含 [URL_PLACEHOLDER] 的模板: 原地址换进去, 给把地址放在参数里的站用.
 */
class GitHubDownloadMirrors(
    /** 清单所在的仓库, `owner/repo`. */
    private val repository: String,
    private val client: () -> ScopedHttpClient,
    scope: CoroutineScope,
) {
    private val logger = logger<GitHubDownloadMirrors>()

    /** 拉到的清单; 都拉不到为 `null`. */
    private val remote = scope.async { fetch() }

    /** GitHub 上的 [url] 的全部来源: 原地址在前, 然后按清单顺序各个镜像. */
    suspend fun sourcesOf(url: String): List<String> {
        val entries = withTimeoutOrNull(REMOTE_WAIT) { remote.await() } ?: BUNDLED
        return listOf(url) + entries.map { resolve(it, url) }
    }

    private suspend fun fetch(): List<String>? {
        for (source in sourceUrls()) {
            val text = try {
                client().use {
                    get(source) {
                        timeout {
                            connectTimeoutMillis = 5_000
                            requestTimeoutMillis = 10_000
                        }
                    }.bodyAsText()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.info { "$FILE_NAME: $source unreachable (${e::class.simpleName})" }
                continue
            }
            val entries = parse(text)?.mapNotNull(::normalize)?.distinct().orEmpty()
            if (entries.isEmpty()) {
                logger.info { "$FILE_NAME from $source has no usable entry, ignoring" }
                continue
            }
            logger.info { "$FILE_NAME from $source: $entries" }
            return entries
        }
        logger.info { "$FILE_NAME: all sources unreachable, using bundled $BUNDLED" }
        return null
    }

    /** jsDelivr 在前: 在中国大陆连得上, `raw.githubusercontent.com` 常常连不上, 放最后兜底. */
    private fun sourceUrls(): List<String> =
        listOf("testingcf.jsdelivr.net", "gcore.jsdelivr.net", "cdn.jsdelivr.net")
            .map { "https://$it/gh/$repository@main/$FILE_NAME" } +
            "https://raw.githubusercontent.com/$repository/main/$FILE_NAME"

    companion object {
        const val URL_PLACEHOLDER = "{url}"

        private const val FILE_NAME = "github-download-mirrors.json"

        /** 下载前最多等清单这么久, 等不到用内置的. */
        private val REMOTE_WAIT = 15.seconds

        /** 拉不到清单时用的, 与新应用内置的一致. */
        private val BUNDLED = listOf(
            "https://gh.xxooo.cf",
            "https://gh-proxy.com",
            "https://gh-proxy.org",
            "https://ghfast.top",
        )

        /**
         * 归一化清单条目, 规则与新应用相同; 认不出返回 `null`. 前缀归一成没有结尾斜杠的形状, 模板只把协议与域名转成小写.
         *
         * 接受不写协议 (按 https)、结尾斜杠、前后空白. 不接受账号密码与片段; 前缀也不能带查询串 (要放进参数就写成模板).
         */
        fun normalize(input: String): String? {
            val s = input.trim()
            if (s.isEmpty() || s.any { it.isWhitespace() }) return null
            val withScheme = if ("://" in s) s else "https://$s"
            val scheme = withScheme.substringBefore("://").lowercase()
            if (scheme != "https" && scheme != "http") return null
            val rest = withScheme.substringAfter("://")
            val authority = rest.takeWhile { it != '/' && it != '?' && it != '#' }.lowercase()
            if (!isValidAuthority(authority) || '#' in rest) return null
            val tail = rest.substring(authority.length)
            val placeholders = tail.windowed(URL_PLACEHOLDER.length).count { it == URL_PLACEHOLDER }
            return when {
                placeholders == 1 -> "$scheme://$authority$tail"
                placeholders > 1 || '?' in tail || '{' in tail || '}' in tail -> null
                else -> "$scheme://$authority${tail.trimEnd('/')}"
            }
        }

        private fun isValidAuthority(authority: String): Boolean {
            if ('@' in authority) return false
            val host = authority.substringBefore(':')
            if (host.isEmpty() || '.' !in host || host.startsWith('.') || host.endsWith('.')) return false
            if (!host.all { it.isLetterOrDigit() || it == '.' || it == '-' }) return false
            if (':' in authority) {
                val port = authority.substringAfter(':').toIntOrNull() ?: return false
                if (port !in 1..65535) return false
            }
            return true
        }

        /** 原地址 [url] 经镜像 [entry] (已归一化) 的下载地址. */
        fun resolve(entry: String, url: String): String =
            if (URL_PLACEHOLDER in entry) entry.replace(URL_PLACEHOLDER, url) else "$entry/$url"

        /** 取出 `mirrors` 里的字符串条目; 格式不对返回 `null`. */
        private fun parse(text: String): List<String>? {
            val root = try {
                Json.parseToJsonElement(text)
            } catch (_: Exception) {
                return null
            }
            val array = (root as? JsonObject)?.get("mirrors") as? JsonArray ?: return null
            return array.mapNotNull { (it as? JsonPrimitive)?.takeIf { primitive -> primitive.isString }?.content }
        }
    }
}
