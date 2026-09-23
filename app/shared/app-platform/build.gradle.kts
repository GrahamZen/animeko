/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

import com.android.build.gradle.ProguardFiles.getDefaultProguardFile

plugins {
    id("ani.kmp-compose")
    alias(libs.plugins.kotlin.plugin.serialization)

    // alias(libs.plugins.kotlinx.atomicfu)
    idea
    id("ani.build-config")
}

val aniAuthServerUrlDebug =
    getPropertyOrNull("ani.auth.server.url.debug") ?: "https://auth.myani.org"
val aniAuthServerUrlRelease = getPropertyOrNull("ani.auth.server.url.release") ?: "https://auth.myani.org"
val dandanplayAppId = getPropertyOrNull("ani.dandanplay.app.id") ?: ""
val dandanplayAppSecret = getPropertyOrNull("ani.dandanplay.app.secret") ?: ""
val tmdbApiToken = getPropertyOrNull("ani.tmdb.api.token") ?: ""
val sentryDsn = getPropertyOrNull("ani.sentry.dsn") ?: ""
val analyticsKey = getPropertyOrNull("ani.analytics.key") ?: ""
val overrideAniApiServer = getPropertyOrNull("ani.api.server")?.takeIf { it.isNotBlank() }

val distroChannel = getPropertyOrNull("ani.distro.channel") ?: "default"
// 迁移跳板包 (这条分支只出它, 构建时带 -Pani.android.migrationBridge=true): 它的"更新"固定是装新应用的落地版,
// 见 gradle.properties 的 ani.migration.* 与 ani.update.asset.prefix.
val migrationBridge = (getPropertyOrNull("ani.android.migrationBridge") ?: "false").toBooleanStrict()
val migrationLandingVersion = getProperty("ani.migration.landing.version")
val updateAssetPrefix = getProperty("ani.update.asset.prefix")
// 放落地版的仓库. 发版前真机走一遍时用 -P 或 local.properties 的 ani.update.repository 指到测试仓库
val updateRepository = getPropertyOrNull("ani.update.repository") ?: getProperty("ani.migration.repository")

kotlin {
    android {
        namespace = "me.him188.ani.app.platform"
        // TODO AGP Migration: Test package optimization
        optimization {
            minify = false
            keepRules.apply {
                files(
                    getDefaultProguardFile("proguard-android-optimize.txt", layout.buildDirectory),
                    *sharedAndroidProguardRules(),
                )
            }
        }
    }

    sourceSets.commonMain.dependencies {
        api(projects.utils.platform)
        api(projects.app.shared.appLang)
        api(libs.kotlinx.coroutines.core)
        api(projects.danmaku.danmakuApi)
        api(libs.kotlinx.collections.immutable)

        api(libs.compose.lifecycle.viewmodel.compose)
        api(libs.compose.lifecycle.runtime.compose)
        api(libs.compose.navigation.compose)
        api(libs.compose.navigation.runtime)
        api(libs.compose.navigation3.runtime)
        api(libs.kotlinx.serialization.json)
        api(libs.compose.material3.adaptive.core)
        api(libs.compose.material3.adaptive.layout)
        api(libs.compose.material3.adaptive.navigation0)

        api(libs.koin.core)
        api(projects.utils.analytics)
    }
    sourceSets.commonTest.dependencies {
        implementation(projects.utils.uiTesting)
        implementation(libs.turbine)
    }
    sourceSets.androidMain.dependencies {
        api(projects.utils.buildConfig)
    }
    sourceSets.desktopMain.dependencies {
        api(libs.jna)
        api(libs.jna.platform)
    }
    sourceSets.iosMain.dependencies {
        // Workaround for CMP bug since 1.8.0. Removing this will cause IDE sync failure and may break ios build.
        api("androidx.performance:performance-annotation:1.0.0-alpha01")
    }
}

//if (bangumiClientDesktopAppId == null || bangumiClientDesktopSecret == null) {
//    logger.warn("bangumi.oauth.client.desktop.appId or bangumi.oauth.client.desktop.secret is not set. Bangumi authorization will not work. Get a token from https://bgm.tv/dev/app and set them in local.properties.")
//}

/// BUILD CONFIG

buildConfig {
    packageName.set("me.him188.ani.app.platform")
    className.set("AniBuildConfig")
    outputDir.set(layout.buildDirectory.dir("generated/buildconfig"))

    // Desktop platform configuration
    fun BuildConfigPlatform.firebaseFields() {
        fun getProp(name: String): String {
            return if (enableFirebase) {
                getProperty(name).also {
                    check(it.isNotBlank()) { "Local property '$name' is not set. You must either set it or disable `ani.enable.firebase`." }
                }
            } else {
                ""
            }
        }
        stringField("firebaseGAAppId", getProp("firebase.ga.app.id"), isOverride = false)
//        stringField("firebaseApiKey", getProp("firebase.api.key"), isOverride = false)
        //            stringField("firebaseStorageBucket", getProperty("firebase.storage.bucket"), isOverride = false)
        //            stringField("firebaseProjectId", getProperty("firebase.project.id"), isOverride = false)
        //            stringField("firebaseGATrackingId", getProperty("firebase.ga.tracking.id"), isOverride = false)
        //            
        //            stringField("firebaseGAMeasurementId", getProperty("firebase.ga.measurement.id"), isOverride = false)
        stringField("firebaseGAApiSecret", getProp("firebase.ga.api.secret"), isOverride = false)

        booleanField("analyticsEnabled", enableFirebase)
    }

    platform("desktop") {
        stringField("versionName", project.version.toString())
        expressionField(
            "isDebug",
            "System.getenv(\"ANI_DEBUG\") == \"true\" || System.getProperty(\"ani.debug\") == \"true\"",
        )
        stringField("dandanplayAppId", dandanplayAppId)
        stringField("dandanplayAppSecret", dandanplayAppSecret)
        stringField("tmdbApiToken", tmdbApiToken)
        stringField("sentryDsn", sentryDsn)
        stringField("overrideAniApiServer", overrideAniApiServer ?: "")
        stringField("distroChannel", distroChannel)
        stringField("updateAssetPrefix", updateAssetPrefix)
        stringField("updateRepository", updateRepository)
        booleanField("isMigrationBridge", migrationBridge)
        booleanField("isMigrationLanding", false)
        stringField("migrationLandingVersion", migrationLandingVersion)

        firebaseFields()
    }

    // Android platform configuration
    platform("android") {
        stringField("versionName", project.version.toString())
        expressionField("isDebug", "me.him188.ani.buildconfig.AndroidBuildConfig.DEBUG")
        stringField("dandanplayAppId", dandanplayAppId)
        stringField("dandanplayAppSecret", dandanplayAppSecret)
        stringField("tmdbApiToken", tmdbApiToken)
        stringField("sentryDsn", sentryDsn)
        stringField("overrideAniApiServer", overrideAniApiServer ?: "")
        stringField("distroChannel", distroChannel)
        stringField("updateAssetPrefix", updateAssetPrefix)
        stringField("updateRepository", updateRepository)
        booleanField("isMigrationBridge", migrationBridge)
        booleanField("isMigrationLanding", false)
        stringField("migrationLandingVersion", migrationLandingVersion)

        booleanField("analyticsEnabled", enableFirebase)
    }

    // iOS platform configuration (only if enabled)
    if (enableIos) {
        platform("ios") {
            stringField("versionName", project.version.toString())
            booleanField("isDebug", false)
            stringField("dandanplayAppId", dandanplayAppId)
            stringField("dandanplayAppSecret", dandanplayAppSecret)
            stringField("tmdbApiToken", tmdbApiToken)
            stringField("sentryDsn", sentryDsn)

            val sentryEnabled = (getPropertyOrNull("ani.sentry.ios") ?: "true").toBooleanStrict()
            booleanField("sentryEnabled", sentryEnabled)
            stringField("overrideAniApiServer", overrideAniApiServer ?: "")
            stringField("distroChannel", distroChannel)

            firebaseFields()
        }
    }
}
