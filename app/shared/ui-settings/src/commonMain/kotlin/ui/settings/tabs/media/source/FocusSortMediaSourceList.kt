/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.settings.tabs.media.source

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import me.him188.ani.app.ui.foundation.ifThen
import me.him188.ani.app.ui.settings.framework.components.SettingsScope
import me.him188.ani.app.ui.settings.framework.SorterState
import org.burnoutcrew.reorderable.ReorderableItem

/** TV 排序: 确认键连发 KeyDown 达到该次数视为长按 (首次 repeat 约在按住 500ms 后). */
internal const val SORT_LONG_PRESS_REPEATS = 2

/**
 * TV 遥控器排序列表 (替代指针拖拽的 [ReorderableItem] 覆盖层, 后者的项在 TV 上不可聚焦):
 * - 每一项均可聚焦; 按住确认键选中该项 (短按也可选中), 选中项抬升阴影提示;
 * - 选中期间上下键把该项在列表中上/下移动, 焦点跟随该项 (key 定位), 不会移走;
 * - 再按确认键放下 (返回键也可放下);
 * - 保存/取消排序仍用组头部的 ✓/✕ 按钮.
 */
@Composable
internal fun SettingsScope.FocusSortMediaSourceList(
    sorter: SorterState<MediaSourcePresentation>,
    initialFocusInstanceId: String?,
    modifier: Modifier = Modifier,
) {
    var pickedId by remember { mutableStateOf<String?>(null) }
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (initialFocusInstanceId == null) return@LaunchedEffect
        // 进入排序时普通列表 (含发起排序的三点按钮) 整体消失, 焦点会丢;
        // 把焦点接到发起项上. 多帧重试: 首帧本列表可能尚未组合完成.
        repeat(10) {
            withFrameNanos { }
            runCatching { initialFocus.requestFocus() }
        }
    }
    Column(modifier.wrapContentHeight()) {
        sorter.sortingData.forEachIndexed { index, item ->
            if (index != 0) {
                HorizontalDividerItem()
            }
            // key 定位: 移动列表项时组合节点随之移动, 焦点跟着项走 (而不是留在原位置)
            key(item.instanceId) {
                val picked = pickedId == item.instanceId
                var confirmKeyDownCount by remember { mutableStateOf(0) }
                var longPressFired by remember { mutableStateOf(false) }
                val elevation by animateDpAsState(if (picked) 16.dp else 0.dp)
                MediaSourceItem(
                    item,
                    Modifier
                        .shadow(elevation)
                        .background(MaterialTheme.colorScheme.surface)
                        .ifThen(item.instanceId == initialFocusInstanceId) { focusRequester(initialFocus) }
                        .onPreviewKeyEvent { event ->
                            val isConfirmKey = event.key == Key.DirectionCenter ||
                                event.key == Key.Enter || event.key == Key.NumPadEnter
                            when {
                                isConfirmKey && event.type == KeyEventType.KeyDown -> {
                                    confirmKeyDownCount++
                                    // 按住到阈值立即选中 (不等松开)
                                    if (!longPressFired && confirmKeyDownCount >= SORT_LONG_PRESS_REPEATS) {
                                        longPressFired = true
                                        if (!picked) pickedId = item.instanceId
                                    }
                                    true
                                }

                                isConfirmKey && event.type == KeyEventType.KeyUp -> {
                                    val fired = longPressFired
                                    longPressFired = false
                                    confirmKeyDownCount = 0
                                    // 短按: 已选中 -> 放下; 未选中 -> 选中 (与按住等效, 更顺手)
                                    if (!fired) pickedId = if (picked) null else item.instanceId
                                    true
                                }

                                picked && (event.key == Key.DirectionUp || event.key == Key.DirectionDown) -> {
                                    if (event.type == KeyEventType.KeyDown) {
                                        sorter.move(index, if (event.key == Key.DirectionUp) -1 else 1)
                                    }
                                    // 边界处也吞掉: 选中期间焦点绝不离开选中项
                                    true
                                }

                                picked && (event.key == Key.Back || event.key == Key.Escape) -> {
                                    if (event.type == KeyEventType.KeyUp) pickedId = null
                                    true
                                }

                                else -> false
                            }
                        }
                        // 提供焦点能力与聚焦指示; 确认键已被上面的 onPreviewKeyEvent 全权接管
                        .clickable(onClickLabel = "选中以排序", onClick = {}),
                ) {
                    Icon(
                        Icons.Rounded.Reorder,
                        contentDescription = "排序",
                    )
                }
            }
        }
    }
}
