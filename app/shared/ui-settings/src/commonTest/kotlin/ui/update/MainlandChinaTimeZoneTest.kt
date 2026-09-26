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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainlandChinaTimeZoneTest {
    @Test
    fun `mainland china zones count`() {
        assertTrue(isMainlandChinaTimeZone("Asia/Shanghai"))
        assertTrue(isMainlandChinaTimeZone("Asia/Urumqi"))
        assertTrue(isMainlandChinaTimeZone("PRC"))
    }

    @Test
    fun `other zones do not count`() {
        assertFalse(isMainlandChinaTimeZone("Asia/Hong_Kong"))
        assertFalse(isMainlandChinaTimeZone("Asia/Taipei"))
        assertFalse(isMainlandChinaTimeZone("Asia/Tokyo"))
        assertFalse(isMainlandChinaTimeZone("UTC"))
    }
}
