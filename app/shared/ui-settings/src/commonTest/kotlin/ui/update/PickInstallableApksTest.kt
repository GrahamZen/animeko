/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.update

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 自动更新的选包逻辑. 这里出错的表现是"自动更新后安装提示不兼容":
 * downloadUrlAlternatives 被 FileDownloader 当成同一文件的备选源 (第一个成功即停),
 * 所以列表首项必须是本机装得上的包, 且列表里不能混入其它架构.
 */
class PickInstallableApksTest {
    // 与 release 实际产物同名 (CI 按目录字母序上传, GitHub 按 asset id 返回, 故 arm64 在最前)
    private val releaseAssets = listOf(
        asset("ani-6.0.1-arm64-v8a.apk"),
        asset("ani-6.0.1-armeabi-v7a.apk"),
        asset("ani-6.0.1-universal.apk"),
        asset("ani-6.0.1-x86_64.apk"),
    )

    private fun asset(name: String) = GitHubAsset(name, "https://example.com/$name")

    private fun List<GitHubAsset>.names() = map { it.name }

    @Test
    fun `arm64 device gets its own package first`() {
        assertEquals(
            listOf("ani-6.0.1-arm64-v8a.apk", "ani-6.0.1-universal.apk"),
            releaseAssets.pickInstallableApks("arm64-v8a").names(),
        )
    }

    @Test
    fun `32-bit device does not get the arm64 package`() {
        // 这正是报障的设备: 修复前它拿到的是首项 arm64-v8a, 装不上
        assertEquals(
            listOf("ani-6.0.1-armeabi-v7a.apk", "ani-6.0.1-universal.apk"),
            releaseAssets.pickInstallableApks("armeabi-v7a").names(),
        )
    }

    @Test
    fun `x86_64 device does not get the arm64 package`() {
        assertEquals(
            listOf("ani-6.0.1-x86_64.apk", "ani-6.0.1-universal.apk"),
            releaseAssets.pickInstallableApks("x86_64").names(),
        )
    }

    @Test
    fun `unknown abi keeps the original list`() {
        // 非 Android 平台, 或将来新增的 ABI: 不筛, 保持旧行为
        assertEquals(releaseAssets.names(), releaseAssets.pickInstallableApks(null).names())
    }

    @Test
    fun `universal only release stays installable`() {
        val universalOnly = listOf(asset("ani-6.0.1-universal.apk"))
        assertEquals(universalOnly.names(), universalOnly.pickInstallableApks("armeabi-v7a").names())
    }

    @Test
    fun `unrecognized naming falls back to the original list`() {
        // 命名规则变了 (没有架构后缀也没有 universal): 宁可退回旧行为, 也不要交出空列表
        val renamed = listOf(asset("ani-6.0.1.apk"))
        assertEquals(renamed.names(), renamed.pickInstallableApks("arm64-v8a").names())
    }
}
