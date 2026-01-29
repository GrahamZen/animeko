/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.models.preference

import kotlinx.serialization.Serializable

@Serializable
data class FocusSettings(
    val globalFocusDelay: Long = 300L,
    val animatedFocusDelay: Long = 300L,
    val shortFocusDelay: Long = 100L,
    val _placeHolder: Int = 0, // ensure non-empty for serialization if needed, similar to other settings
) {
    companion object {
        val Default = FocusSettings()
    }
}
