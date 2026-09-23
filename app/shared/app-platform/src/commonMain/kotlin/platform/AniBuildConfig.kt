/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.platform

import androidx.compose.runtime.Stable
import me.him188.ani.utils.platform.currentPlatform


@Stable
interface AniBuildConfig {
    /**
     * `3.0.0-rc04`
     */
    val versionName: String
    val isDebug: Boolean
    val dandanplayAppId: String
    val dandanplayAppSecret: String

    /** TMDB API Read Access Token (v4 Bearer), 用于获取横版背景图/剧集缩略图. 未配置时为空串. */
    val tmdbApiToken: String
        get() = ""
    val sentryDsn: String
    val overrideAniApiServer: String
        get() = ""

    val distroChannel: String

    /** 迁移跳板包要装的落地版安装包在 release 里的文件名前缀 (`<前缀>-<版本>-<架构>.apk`, 见 gradle.properties). */
    val updateAssetPrefix: String
        get() = "ani"

    /** 放落地版的 GitHub 仓库, `owner/repo` (见 gradle.properties); 发版前走真机更新时可指到测试仓库. */
    val updateRepository: String
        get() = "GrahamZen/izuko-tv"

    /**
     * 本包是不是**跳板包**: 仍用旧 applicationId, 唯一的用处是把老用户引导到新应用的落地版 ([migrationLandingVersion]) 上.
     * 它的"更新"固定是装落地版, 不找最新版 (见 `UpdateChecker`).
     */
    val isMigrationBridge: Boolean
        get() = false

    /** 本包是不是新应用的落地版. 旧版本线上恒为 `false`, 留着只为共用迁移那部分代码. */
    val isMigrationLanding: Boolean
        get() = false

    /** 跳板包要装的落地版版本号 (不带 `v`). */
    val migrationLandingVersion: String
        get() = ""

    val sentryEnabled: Boolean
        get() = true
    val analyticsEnabled: Boolean
        get() = true

    val isDefaultDistro: Boolean
        get() = distroChannel == DISTRO_PLATFORM_DEFAULT

    companion object {
        @Stable
        fun current(): AniBuildConfig = currentAniBuildConfig

        private const val DISTRO_PLATFORM_DEFAULT = "default"
    }
}

/**
 * E.g. `3000` for `3.0.0`, `3012` for `3.1.2`
 */
val AniBuildConfig.fourDigitVersionCode: String
    get() = buildString {
        val split = versionName.substringBefore("-").split(".")
        if (split.size == 3) {
            split[0].toIntOrNull()?.let {
                append(it.toString())
            }
            split[1].toIntOrNull()?.let {
                append(it.toString().padStart(2, '0'))
            }
            split[2].toIntOrNull()?.let {
                append(it.toString())
            }
        } else {
            for (section in split) {
                section.toIntOrNull()?.let {
                    append(it.toString())
                }
            }
        }
    }

@Stable
@PublishedApi
internal expect val currentAniBuildConfigImpl: AniBuildConfig

@Stable
inline val currentAniBuildConfig: AniBuildConfig get() = currentAniBuildConfigImpl

/**
 * 满足各个数据源建议格式的 User-Agent, 所有 HTTP 请求都应该带此 UA.
 */
fun getAniUserAgent(
    version: String = currentAniBuildConfig.versionName,
    platform: String = currentPlatform().nameAndArch,
): String = "open-ani/ani/$version ($platform) (https://github.com/open-ani/ani)"
