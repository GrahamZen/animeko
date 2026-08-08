/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

import org.gradle.api.Project
import java.io.File
import java.util.Properties

fun Project.getProperty(name: String) =
    getPropertyOrNull(name) ?: error("Property $name not found")

fun Project.getPropertyOrNull(name: String) =
    getLocalProperty(name)
        ?: System.getProperty(name)
        ?: System.getenv(name)
        ?: findProperty(name)?.toString()
        ?: properties[name]?.toString()
        ?: extensions.extraProperties.runCatching { get(name).toString() }.getOrNull()


val Project.localPropertiesFile: File get() = project.rootProject.file("local.properties")

fun Project.getLocalProperty(key: String): String? {
    return if (localPropertiesFile.exists()) {
        val properties = Properties()
        localPropertiesFile.inputStream().buffered().use { input ->
            properties.load(input)
        }
        properties.getProperty(key)
    } else {
        localPropertiesFile.createNewFile()
        null
    }
}


fun Project.getIntProperty(name: String) = getProperty(name).toInt()

/**
 * 兼容包 (Android 7.1 / API 25) 的总开关: `-Pani.android.legacy=true`.
 *
 * 默认关闭, 关闭时正式包的构建配置与本开关引入前**完全一致** —— 这是刻意的:
 * core library desugaring 是模块级设置, 无法只对老设备生效, 一旦对正式包开启,
 * 所有用户的 java.time / java.nio.file 都会换成回填实现 (实测踩过: 用错变体会让 27+ 的 BT 播放静默卡死).
 * 因此兼容性下调只在单独构建兼容包时打开, 正式包一行字节码都不受影响.
 *
 * @see androidMinSdk
 */
val Project.buildLegacyAndroidApp
    get() = getPropertyOrNull("ani.android.legacy")?.toBooleanStrict() ?: false

/**
 * 兼容包的 minSdk. 25 = Android 7.1.1, 也是 ISRG Root X1 进入系统信任库的第一个版本 ——
 * 再往下 Let's Encrypt 签发的证书会直接握手失败, 得自带根证书才行.
 */
const val LEGACY_ANDROID_MIN_SDK = 25

/**
 * 所有 Android 模块的 minSdk 都必须读这里而不是直接读 `android.min.sdk` 属性:
 * 开兼容包时要整体下调, 否则库模块仍是 27, manifest 合并会直接失败.
 */
val Project.androidMinSdk: Int
    get() = if (buildLegacyAndroidApp) LEGACY_ANDROID_MIN_SDK else getIntProperty("android.min.sdk")

val Project.enableAnitorrent
    get() = (getPropertyOrNull("ani.enable.anitorrent") ?: "false").toBooleanStrict()

val Project.enableIos
    get() = getPropertyOrNull("ani.enable.ios")?.toBooleanStrict() ?: false

val Project.buildIosFramework
    get() = getPropertyOrNull("ani.build.framework")?.toBooleanStrict() ?: false

val Project.enableFirebase
    get() = getPropertyOrNull("ani.enable.firebase")?.toBooleanStrict() ?: false
