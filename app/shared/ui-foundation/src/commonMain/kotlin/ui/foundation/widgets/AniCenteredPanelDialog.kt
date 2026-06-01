/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 半透明居中大面板弹窗: 按窗口比例定尺寸, 下层内容 (视频画面 / 页面) 经系统遮罩隐约透出.
 *
 * 用于大屏上替代贴边侧栏与底部抽屉 —— 贴边面板离视线中心远, 焦点跳到屏幕边缘的过程也难以
 * 看清. 是否改用这个形态由
 * [AniUiBehavior.panelsAsCenteredDialogs][me.him188.ani.app.ui.foundation.AniUiBehavior.panelsAsCenteredDialogs]
 * 决定.
 *
 * 返回键由 [Dialog] 自行消费 (独立窗口), 调用方无需再装 BackHandler.
 */
@Composable
fun AniCenteredPanelDialog(
    onDismissRequest: () -> Unit,
    title: (@Composable () -> Unit)? = null,
    widthFraction: Float = CENTERED_PANEL_WIDTH_FRACTION,
    heightFraction: Float = CENTERED_PANEL_HEIGHT_FRACTION,
    /**
     * 非 null 时高度由 [widthFraction] 推出的宽度按此宽高比算, 忽略 [heightFraction] ——
     * 背景是定比例的图 (如 16:9 剧照) 时用它, 面板与图同比例, 图铺满时不会被裁掉上下或左右.
     */
    aspectRatio: Float? = null,
    /**
     * 满幅铺在面板里的背景 (如剧照), 上方自动压一层遮罩保证正文可读.
     *
     * 非 null 时面板底色改为透明 (否则半透明玻璃色会盖住背景), 且内容色固定为白 ——
     * 遮罩之上永远是深底, 不能跟随主题的 onSurface (浅色主题下会变成深字压深底).
     */
    background: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            Modifier.fillMaxWidth(widthFraction)
                .then(
                    if (aspectRatio != null) {
                        Modifier.aspectRatio(aspectRatio)
                    } else {
                        Modifier.fillMaxHeight(heightFraction)
                    },
                ),
            shape = RoundedCornerShape(16.dp),
            color = if (background != null) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = CENTERED_PANEL_ALPHA)
            },
        ) {
            Box {
                if (background != null) {
                    background()
                    // 遮罩: 背景图的亮度/花色不可控, 压到足够暗才能保证任意图上正文都读得清
                    Box(
                        Modifier.matchParentSize()
                            .background(Color.Black.copy(alpha = CENTERED_PANEL_SCRIM_ALPHA)),
                    )
                }
                CompositionLocalProvider(
                    LocalContentColor provides if (background != null) {
                        Color.White
                    } else {
                        LocalContentColor.current
                    },
                ) {
                    Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                        title?.let {
                            ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                                Row(Modifier.fillMaxWidth().padding(bottom = 16.dp)) { it() }
                            }
                        }
                        content()
                    }
                }
            }
        }
    }
}

private const val CENTERED_PANEL_WIDTH_FRACTION = 0.72f
private const val CENTERED_PANEL_HEIGHT_FRACTION = 0.85f

/** 面板不透明度: 半透明玻璃感, 下层 (视频画面 / 页面) 隐约透出. */
private const val CENTERED_PANEL_ALPHA = 0.94f

/** 背景图上的遮罩不透明度: 够暗才能压住亮色剧照, 又留得住图的轮廓. */
private const val CENTERED_PANEL_SCRIM_ALPHA = 0.72f
