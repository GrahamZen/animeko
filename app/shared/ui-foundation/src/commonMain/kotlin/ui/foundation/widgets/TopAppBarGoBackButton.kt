/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.him188.ani.app.ui.foundation.LocalPlatform
import me.him188.ani.app.ui.foundation.isTv

@Composable
fun BackNavigationIconButton(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // TV 上不显示屏幕返回按钮: 遥控器返回键已全局可用 (见 AniApp 根部拦截), 按钮纯属多余.
    // 该按钮曾兼任进入页面时的初始焦点锚点; 该职责已移交 AniAppContent 的导航级焦点兜底
    // 与各页面自己的焦点锚点 (如 TV 详情页的播放按钮).
    if (LocalPlatform.current.isTv()) return

    TopAppBarActionButton(onNavigateBack, modifier) {
        Icon(
            Icons.AutoMirrored.Outlined.ArrowBack,
            null,
        )
    }
}

@Composable
fun TopAppBarActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick,
        modifier,
//        Modifier.offset(x = (-8).dp, y = (-8).dp).width(36.dp + 16.dp).height(36.dp + 16.dp)
    ) { // 让可点击区域大一点, 更方便
        Box(Modifier.size(24.dp)) {
            content()
        }
    }
}
