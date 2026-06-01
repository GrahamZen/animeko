/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.update

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.him188.ani.app.data.network.protocol.ReleaseClass
import me.him188.ani.app.platform.currentAniBuildConfig
import me.him188.ani.app.platform.getAniUserAgent
import me.him188.ani.utils.ktor.getPlatformKtorEngine
import me.him188.ani.utils.logging.error
import me.him188.ani.utils.logging.info
import me.him188.ani.utils.logging.logger

internal const val FORK_OWNER = "GrahamZen"
internal const val FORK_REPO = "animeko"

class UpdateChecker {
    /**
     * 检查是否有更新的版本. 返回最新版本的信息, 或者 `null` 表示没有新版本.
     */
    suspend fun checkLatestVersion(
        releaseClass: ReleaseClass,
        currentVersion: String = currentAniBuildConfig.versionName,
    ): NewVersion? {
        HttpClient(getPlatformKtorEngine()) {
            expectSuccess = true
        }.use { client ->
            return try {
                client.getVersionFromGitHub(currentVersion, releaseClass).also {
                    logger.info { "Got latest version from GitHub: ${it?.name}" }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                logger.error(e) { "Failed to get latest version from GitHub" }
                throw e
            }
        }
    }

    private suspend fun HttpClient.getVersionFromGitHub(
        currentVersion: String,
        releaseClass: ReleaseClass,
    ): NewVersion? {
        val releases = get("https://api.github.com/repos/$FORK_OWNER/$FORK_REPO/releases") {
            parameter("per_page", 20)
            header(HttpHeaders.UserAgent, getAniUserAgent())
        }.bodyAsText().let {
            json.decodeFromString<List<GitHubRelease>>(it)
        }

        val latest = releases
            .filter { !it.draft }
            .filter { release ->
                when (releaseClass) {
                    ReleaseClass.STABLE -> !release.prerelease
                    else -> true // BETA / ALPHA / RC: include prerelease
                }
            }
            .firstOrNull() ?: return null

        val versionName = latest.tagName.removePrefix("v")
        if (!isNewerThan(versionName, currentVersion)) return null

        return NewVersion(
            name = versionName,
            changelogs = listOf(
                Changelog(
                    version = versionName,
                    publishedAt = latest.publishedAt,
                    changes = latest.body,
                ),
            ),
            downloadUrlAlternatives = latest.assets
                .filter { it.name.endsWith(".apk") }
                .map { it.browserDownloadUrl },
            publishedAt = latest.publishedAt,
        )
    }

    /**
     * Returns true if [candidate] is a newer version than [current].
     *
     * Handles semver with optional pre-release suffix, e.g. "4.0.0-beta04".
     * Stable (no suffix) is considered newer than any pre-release with the same numbers.
     */
    private fun isNewerThan(candidate: String, current: String): Boolean {
        if (candidate == current) return false

        fun parse(v: String): Pair<List<Int>, String> {
            val dashIdx = v.indexOf('-')
            return if (dashIdx >= 0) {
                v.substring(0, dashIdx).split('.').map { it.toIntOrNull() ?: 0 } to
                        v.substring(dashIdx + 1)
            } else {
                v.split('.').map { it.toIntOrNull() ?: 0 } to ""
            }
        }

        val (cNums, cPre) = parse(candidate)
        val (vNums, vPre) = parse(current)

        for (i in 0 until maxOf(cNums.size, vNums.size)) {
            val c = cNums.getOrElse(i) { 0 }
            val v = vNums.getOrElse(i) { 0 }
            if (c != v) return c > v
        }

        // Same numeric version — stable beats pre-release
        if (cPre.isEmpty() && vPre.isNotEmpty()) return true  // stable > beta
        if (cPre.isNotEmpty() && vPre.isEmpty()) return false  // beta < stable
        return cPre > vPre // both pre-release: compare lexicographically
    }

    private companion object {
        private val logger = logger<UpdateChecker>()
        private val json = Json { ignoreUnknownKeys = true }
    }
}

@Serializable
private data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("body") val body: String = "",
    @SerialName("draft") val draft: Boolean = false,
    @SerialName("prerelease") val prerelease: Boolean = false,
    @SerialName("published_at") val publishedAt: String = "",
    @SerialName("assets") val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
private data class GitHubAsset(
    @SerialName("name") val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
)
