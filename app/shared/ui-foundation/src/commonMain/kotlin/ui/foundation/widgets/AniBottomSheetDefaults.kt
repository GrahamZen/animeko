/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation.widgets

import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import me.him188.ani.app.ui.foundation.LocalPlatform
import me.him188.ani.app.ui.foundation.isTv

object AniBottomSheetDefaults {
    /**
     * [ModalBottomSheet][androidx.compose.material3.ModalBottomSheet] 的最大宽度.
     *
     * M3 默认写死 640.dp, 在 TV 横屏大屏上 (960dp 宽) 只占 2/3 宽而高度近全屏, 显得窄长;
     * TV 上改为窗口宽度的 90%, 让自适应内容 (如详情页) 按宽布局渲染. 其余平台维持 M3 默认.
     */
    @Composable
    fun sheetMaxWidth(): Dp {
        return if (LocalPlatform.current.isTv()) {
            val containerWidthPx = LocalWindowInfo.current.containerSize.width
            with(LocalDensity.current) { (containerWidthPx * 0.9f).toDp() }
        } else {
            BottomSheetDefaults.SheetMaxWidth
        }
    }
}
