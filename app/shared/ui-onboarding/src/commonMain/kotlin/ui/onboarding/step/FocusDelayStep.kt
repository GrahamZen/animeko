/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.onboarding.step

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import me.him188.ani.app.data.models.preference.FocusSettings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.getValue
import me.him188.ani.app.ui.foundation.layout.currentWindowAdaptiveInfo1
import me.him188.ani.app.ui.onboarding.WizardLayoutParams
import me.him188.ani.app.ui.settings.SettingsTab
import me.him188.ani.app.ui.settings.framework.components.TextItem

@Composable
internal fun FocusDelayStep(
    uiState: FocusDelayUIState,
    onUpdateConfig: (FocusSettings) -> Unit,
    modifier: Modifier = Modifier,
    layoutParams: WizardLayoutParams = WizardLayoutParams.calculate(currentWindowAdaptiveInfo1().windowSizeClass)
) {
    SettingsTab(modifier = modifier) {
        Group(
            title = { Text("焦点延迟 (ms)") },
            useThinHeader = true,
            modifier = Modifier
                .padding(horizontal = layoutParams.horizontalPadding)
                .padding(top = layoutParams.verticalPadding),
        ) {
            var editingDelay by remember { mutableStateOf<Pair<String, Long>?>(null) }
            var onConfirmDelay by remember { mutableStateOf<((Long) -> Unit)?>(null) }

            if (editingDelay != null && onConfirmDelay != null) {
                EditDelayDialog(
                    title = editingDelay!!.first,
                    initialValue = editingDelay!!.second,
                    onDismiss = { editingDelay = null },
                    onConfirm = {
                        onConfirmDelay?.invoke(it)
                        editingDelay = null
                    }
                )
            }

            TextItem(
                title = { Text("全局焦点延迟") },
                description = { Text("通用 UI 交互的标准延迟 (例如弹出菜单打开)。") },
                onClick = {
                    editingDelay = "全局焦点延迟" to uiState.focusSettings.globalFocusDelay
                    onConfirmDelay = { newDelay ->
                        onUpdateConfig(uiState.focusSettings.copy(globalFocusDelay = newDelay))
                    }
                },
                action = { Text("${uiState.focusSettings.globalFocusDelay} ms") }
            )
            TextItem(
                title = { Text("动画焦点延迟") },
                description = { Text("等待动画 (例如侧边栏) 的延迟。") },
                onClick = {
                    editingDelay = "动画焦点延迟" to uiState.focusSettings.animatedFocusDelay
                    onConfirmDelay = { newDelay ->
                        onUpdateConfig(uiState.focusSettings.copy(animatedFocusDelay = newDelay))
                    }
                },
                action = { Text("${uiState.focusSettings.animatedFocusDelay} ms") }
            )
            TextItem(
                title = { Text("短焦点延迟") },
                description = { Text("快速更新 (例如搜索结果) 的短延迟。") },
                onClick = {
                    editingDelay = "短焦点延迟" to uiState.focusSettings.shortFocusDelay
                    onConfirmDelay = { newDelay ->
                        onUpdateConfig(uiState.focusSettings.copy(shortFocusDelay = newDelay))
                    }
                },
                action = { Text("${uiState.focusSettings.shortFocusDelay} ms") }
            )
        }
    }
}

@Composable
private fun EditDelayDialog(
    title: String,
    initialValue: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var text by remember { mutableStateOf(initialValue.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.all { char -> char.isDigit() }) text = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                text.toLongOrNull()?.let { onConfirm(it) }
                onDismiss()
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Stable
data class FocusDelayUIState(
    val focusSettings: FocusSettings = FocusSettings.Default,
) {
    companion object {
        @Stable
        val Placeholder = FocusDelayUIState()
    }
}
