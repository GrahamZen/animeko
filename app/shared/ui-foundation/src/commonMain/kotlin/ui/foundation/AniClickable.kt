/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.delay
import me.him188.ani.utils.platform.currentTimeMillis

/**
 * Confirm-key aware clickable modifier that prevents long-press from triggering unwanted clicks.
 * 
 * Under focus-driven navigation, this modifier:
 * - Handles DPAD_CENTER (OK button) key events properly
 * - Distinguishes between short-press (click) and long-press
 * - Prevents long-press from triggering onClick
 * - Only triggers onClick on KeyUp for short presses
 * 
 * Otherwise falls back to standard clickable behavior.
 * 
 * @param enabled Controls the enabled state
 * @param onClickLabel Semantic label for the click action
 * @param onClick Callback invoked on short-press/click
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.aniClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    onClick: () -> Unit
): Modifier = composed {
    val focusDriven = LocalAniUiBehavior.current.focusDrivenNavigation
    val resolvedIndication = indication ?: LocalIndication.current

    if (focusDriven) {
        var keyDownTime by remember { mutableStateOf(0L) }
        var isLongPress by remember { mutableStateOf(false) }
        
        this
            .onKeyEvent { keyEvent ->
                if (!enabled) return@onKeyEvent false
                
                when {
                    keyEvent.key == Key.DirectionCenter && keyEvent.type == KeyEventType.KeyDown -> {
                        if (keyDownTime == 0L) {
                            // First key down
                            keyDownTime = currentTimeMillis()
                            isLongPress = false
                        } else {
                            // Key repeat (long press)
                            isLongPress = true
                        }
                        true // Consume the event
                    }
                    keyEvent.key == Key.DirectionCenter && keyEvent.type == KeyEventType.KeyUp -> {
                        val pressDuration = currentTimeMillis() - keyDownTime
                        keyDownTime = 0L
                        
                        // Only trigger click for short press (< 500ms)
                        if (!isLongPress && pressDuration < LONG_CLICK_DURATION_MILLIS) {
                            onClick()
                        }
                        isLongPress = false
                        true // Consume the event
                    }
                    else -> false
                }
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = resolvedIndication,
                enabled = enabled,
                onClickLabel = onClickLabel,
                onClick = onClick,
            )
    } else {
        // Non-TV: use standard clickable
        this.combinedClickable(
            interactionSource = interactionSource,
            indication = resolvedIndication,
            enabled = enabled,
            onClickLabel = onClickLabel,
            onClick = onClick,
        )
    }
}

/**
 * Confirm-key aware combined clickable modifier with support for both click and long-click.
 * 
 * Under focus-driven navigation, this modifier:
 * - Handles DPAD_CENTER (OK button) key events properly
 * - Distinguishes between short-press (onClick) and long-press (onLongClick)
 * - Prevents long-press from triggering onClick
 * - Triggers onLongClick after holding for threshold duration
 * 
 * Otherwise falls back to standard combinedClickable behavior.
 * 
 * @param enabled Controls the enabled state
 * @param onClickLabel Semantic label for the click action
 * @param onLongClickLabel Semantic label for the long-click action
 * @param onDoubleClick Callback invoked on double-click (primarily for touch input)
 * @param onClick Callback invoked on short-press/click
 * @param onLongClick Callback invoked on long-press, or null if not supported
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.aniCombinedClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    onLongClickLabel: String? = null,
    onDoubleClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    onClick: () -> Unit
): Modifier = composed {
    val focusDriven = LocalAniUiBehavior.current.focusDrivenNavigation
    val resolvedIndication = indication ?: LocalIndication.current

    if (focusDriven && onLongClick != null) {
        var keyDownTime by remember { mutableStateOf(0L) }
        var longClickTriggered by remember { mutableStateOf(false) }
        var isKeyDown by remember { mutableStateOf(false) }
        
        // Monitor for long press threshold
        LaunchedEffect(isKeyDown) {
            if (isKeyDown && keyDownTime > 0L) {
                delay(LONG_CLICK_DURATION_MILLIS) // Long press threshold
                if (isKeyDown && currentTimeMillis() - keyDownTime >= LONG_CLICK_DURATION_MILLIS) {
                    longClickTriggered = true
                    onLongClick()
                }
            }
        }
        
        this
            .onKeyEvent { keyEvent ->
                if (!enabled) return@onKeyEvent false
                
                when {
                    keyEvent.key == Key.DirectionCenter && keyEvent.type == KeyEventType.KeyDown -> {
                        if (keyDownTime == 0L) {
                            // First key down
                            keyDownTime = currentTimeMillis()
                            longClickTriggered = false
                            isKeyDown = true
                        }
                        true // Consume the event
                    }
                    keyEvent.key == Key.DirectionCenter && keyEvent.type == KeyEventType.KeyUp -> {
                        isKeyDown = false
                        val pressDuration = currentTimeMillis() - keyDownTime
                        keyDownTime = 0L
                        
                        // Only trigger click if long click wasn't triggered and it was a short press
                        if (!longClickTriggered && pressDuration < LONG_CLICK_DURATION_MILLIS) {
                            onClick()
                        }
                        longClickTriggered = false
                        true // Consume the event
                    }
                    else -> false
                }
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = resolvedIndication,
                enabled = enabled,
                onClickLabel = onClickLabel,
                onLongClickLabel = onLongClickLabel,
                onDoubleClick = onDoubleClick,
                onLongClick = onLongClick,
                onClick = onClick,
            )
    } else if (focusDriven) {
        // TV without long click - use aniClickable
        aniClickable(enabled, onClickLabel, interactionSource, indication, onClick)
    } else {
        // Non-TV: use standard combinedClickable
        this.combinedClickable(
            interactionSource = interactionSource,
            indication = resolvedIndication,
            enabled = enabled,
            onClickLabel = onClickLabel,
            onLongClickLabel = onLongClickLabel,
            onDoubleClick = onDoubleClick,
            onLongClick = onLongClick,
            onClick = onClick,
        )
    }
}

/**
 * 吞掉"把本界面开出来的那一次长按"的余波: 从挂载起, 直到看见确认键抬起为止, 所有确认键事件
 * 一律消费.
 *
 * 遥控器按住确认键期间系统会以约 50ms 一次连发 KeyDown. [aniCombinedClickable] 在 500ms 上
 * 触发 onLongClick 弹出下拉菜单时用户的手还没松 —— 菜单一拿到焦点, 紧接着的那几发 KeyDown
 * 与随后的 KeyUp 就落在菜单第一项上, 表现为"菜单刚弹出来就自己把第一项选了".
 *
 * 挂在弹层内容上 (如 `DropdownMenu(modifier = ...)`): `onPreviewKeyEvent` 自弹层根部向下传,
 * 会先于菜单项拿到事件.
 *
 * 同一条规则在 `FocusEpisodeCard` 里还有一份内联实现 (`swallowConfirmKeys`, 详情页长按播放
 * 按钮跳到选集卡片后不让残余按键误触那张卡). 那份与卡片自己的确认键计数器绑在一起, 暂未合并.
 *
 * @param enabled 仅当本界面**确实是被长按开出来的**时传 true. 短按开出来的弹层不能开 ——
 *   那会把用户接下来第一次正常按键吃掉.
 */
fun Modifier.consumeHeldConfirmKey(enabled: Boolean = true): Modifier = composed {
    if (!enabled) return@composed this

    var released by remember { mutableStateOf(false) }
    // 兜底: 抬起有可能在本弹层挂载之前就发生了 (长按到点与松手只差几十毫秒, 那一下 KeyUp 会
    // 落在原来的行上), 没有这个超时就会一直吞到天荒地老, 把用户下一次正常按键也吃掉
    LaunchedEffect(Unit) {
        delay(HELD_CONFIRM_KEY_GRACE_MILLIS)
        released = true
    }
    onPreviewKeyEvent { event ->
        if (released) return@onPreviewKeyEvent false
        val isConfirm = event.key == Key.DirectionCenter ||
                event.key == Key.Enter ||
                event.key == Key.NumPadEnter
        if (!isConfirm) return@onPreviewKeyEvent false
        if (event.type == KeyEventType.KeyUp) released = true
        true
    }
}

/** [consumeHeldConfirmKey] 等待抬起的上限 (与 FocusEpisodeCard 那份内联实现取同一档). */
private const val HELD_CONFIRM_KEY_GRACE_MILLIS = 1_500L
