/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.update

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.him188.ani.app.ui.foundation.LocalAniUiBehavior
import me.him188.ani.app.ui.foundation.focus.resolveFocusRepeatedly
import me.him188.ani.app.ui.foundation.ifThen
import me.him188.ani.app.ui.foundation.widgets.AniCenteredPanelDialog
import me.him188.ani.app.ui.lang.Lang
import me.him188.ani.app.ui.lang.settings_update_popup_close
import me.him188.ani.app.ui.lang.settings_update_popup_new_version
import me.him188.ani.app.ui.lang.settings_update_popup_see_details
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.rememberCoroutineScope

/**
 * 完整更新内容弹窗.
 *
 * 气泡上只放得下前几条 ([NewVersion.majorChanges]), 而"查看详情"以前直接跳浏览器 —— 电视上
 * 跳出去看网页几乎不可用 (没有指针, 也未必装着浏览器). 现在先在应用内看全文, 底部仍留一个
 * 跳浏览器的按钮.
 *
 * 遥控器形态用居中大面板 (与其余面板同一形态), 指针设备用普通对话框.
 */
@Composable
fun NewVersionDetailsDialog(
    version: String,
    changes: String,
    onOpenInBrowser: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val title = stringResource(Lang.settings_update_popup_new_version)
    if (LocalAniUiBehavior.current.panelsAsCenteredDialogs) {
        AniCenteredPanelDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("$title $version") },
            widthFraction = 0.6f,
            heightFraction = 0.8f,
        ) {
            Column(Modifier.fillMaxSize()) {
                ChangelogBody(
                    changes = changes,
                    // 遥控器上滚动区自己得是焦点目标, 否则一片纯文本没有任何可聚焦元素, 方向键
                    // 无从翻页 (焦点会直接落到底部按钮上, 长文永远看不到后面)
                    focusable = true,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                DialogActions(onOpenInBrowser = onOpenInBrowser, onDismissRequest = onDismissRequest)
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("$title $version") },
            text = {
                ChangelogBody(
                    changes = changes,
                    focusable = false,
                    modifier = Modifier.heightIn(max = 420.dp).fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = onOpenInBrowser) {
                    Icon(Icons.AutoMirrored.Outlined.Launch, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Lang.settings_update_popup_see_details))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(Lang.settings_update_popup_close))
                }
            },
        )
    }
}

@Composable
private fun DialogActions(
    onOpenInBrowser: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onOpenInBrowser) {
            Icon(Icons.AutoMirrored.Outlined.Launch, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Lang.settings_update_popup_see_details))
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onDismissRequest) {
            Text(stringResource(Lang.settings_update_popup_close))
        }
    }
}

/**
 * 更新内容正文: 按 release 说明里的小节标题 (`### 修复`) 分段, 条目前面画点.
 *
 * 没有走富文本渲染: 这份文本只有"标题 / 条目"两种行, 而 release 说明里偶尔出现的行内代码与
 * 链接语法照原样显示也读得懂, 不值得为它把 markdown 渲染搬进更新弹窗.
 */
@Composable
private fun ChangelogBody(
    changes: String,
    focusable: Boolean,
    modifier: Modifier = Modifier,
) {
    val lines = remember(changes) { parseChangelogLines(changes) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    // 打开就把焦点放在正文上: 用户点"查看详情"就是来看内容的, 让他直接能翻.
    // 弹窗是独立窗口, 组合刚建立时 requestFocus 会被静默拒绝, 所以要重试到位 (见 resolveFocusRepeatedly)
    LaunchedEffect(focusable) {
        if (focusable) {
            resolveFocusRepeatedly(arrived = { focused }) {
                runCatching { focusRequester.requestFocus() }
            }
        }
    }

    Box(
        modifier
            .ifThen(focusable) {
                onPreviewKeyEvent { event ->
                    // 焦点搜索只发生在 KeyDown, KeyUp 一律放行
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val page = (scrollState.viewportSize * SCROLL_PAGE_FRACTION).coerceAtLeast(1f)
                    when {
                        event.key == Key.DirectionDown && scrollState.canScrollForward -> {
                            scope.launch { scrollState.animateScrollBy(page) }
                            true
                        }

                        event.key == Key.DirectionUp && scrollState.canScrollBackward -> {
                            scope.launch { scrollState.animateScrollBy(-page) }
                            true
                        }
                        // 翻到底再按下键就放行, 焦点落到下方按钮上; 上键对称 (顶上没有目标, 焦点不动)
                        else -> false
                    }
                }
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .focusable()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = if (focused) 2.dp else 1.dp,
                        color = if (focused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(12.dp)
            },
    ) {
        Column(Modifier.verticalScroll(scrollState)) {
            lines.forEachIndexed { index, line ->
                when (line) {
                    is ChangelogLine.Section -> Text(
                        text = line.text,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = if (index == 0) 0.dp else 12.dp, bottom = 4.dp),
                    )

                    is ChangelogLine.Item -> Row(Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.Top) {
                        Text("•", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(8.dp))
                        Text(line.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private sealed interface ChangelogLine {
    /** 小节标题 (`### 修复`). */
    data class Section(val text: String) : ChangelogLine

    /** 一条更新内容. */
    data class Item(val text: String) : ChangelogLine
}

private fun parseChangelogLines(changes: String): List<ChangelogLine> =
    changes.lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { line ->
            if (line.startsWith("#")) {
                ChangelogLine.Section(line.trimStart('#').trim())
            } else {
                ChangelogLine.Item(line.removePrefix("- ").removePrefix("* "))
            }
        }
        .toList()

/** 上下键一次翻多少: 留一点重叠, 免得翻页后接不上前一屏的最后一行. */
private const val SCROLL_PAGE_FRACTION = 0.8f
