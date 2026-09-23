/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.update

import kotlinx.coroutines.delay
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.seconds

/**
 * 落地版 (见 `AniBuildConfig.isMigrationLanding`) 等迁移彻底结束才去更新: 接管数据、搬缓存、卸载旧版的提示都处理完.
 * 更新到的最新版不带迁移代码, 中途装上去会把没搬完的东西丢在半路, 卸载提示也不会再出现.
 *
 * app/android 启动时接上 [migrationPending] (即 `SettingsMigration.isMigrationUiPending`), 其它平台恒为 `false`.
 */
object MigrationLandingGate {
    @Volatile
    var migrationPending: () -> Boolean = { false }

    suspend fun awaitMigrationDone() {
        while (migrationPending()) delay(POLL_INTERVAL)
    }

    private val POLL_INTERVAL = 2.seconds
}
