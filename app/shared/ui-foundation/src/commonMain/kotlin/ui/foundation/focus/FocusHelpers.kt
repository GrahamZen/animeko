/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation.focus

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import me.him188.ani.app.ui.foundation.LocalAniUiBehavior

/**
 * Provides the [FocusRequester] for the comment-tab button in the episode page tab row.
 * When the user presses Back while inside the comment list, focus returns to this tab.
 *
 * Provided by episode screen composables; defaults to null (no-op on non-TV).
 */
val LocalCommentTabFocusRequester = compositionLocalOf<FocusRequester?> { null }

/**
 * Provides the [FocusRequester] for the primary action button in a subject-details pane
 * (e.g. the "Select Episode" button). Focus is requested here when the user explicitly
 * selects a subject from the search results list.
 *
 * Provided by search/detail screen composables; defaults to null (no-op on non-TV).
 */
val LocalDetailsFocusRequester = compositionLocalOf<FocusRequester?> { null }

/**
 * Modifier that ensures a default focusable element exists on TV platforms.
 * 
 * This should be applied to the root composable of any screen/page to ensure
 * that when the page is displayed, there is always a focusable element available.
 * 
 * On TV platforms:
 * - Creates a FocusRequester and applies it to the element
 * - Automatically requests focus when the element is composed
 * - Makes the element focusable
 * 
 * On non-TV platforms:
 * - Does nothing (returns the modifier unchanged)
 * 
 * Usage:
 * ```
 * Box(
 *     modifier = Modifier
 *         .fillMaxSize()
 *         .defaultFocus()
 * ) {
 *     // Your content
 * }
 * ```
 */
fun Modifier.defaultFocus(): Modifier = composed {
    val focusDriven = LocalAniUiBehavior.current.focusDrivenNavigation
    
    if (focusDriven) {
        val focusRequester = remember { FocusRequester() }
        
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        
        this
            .focusRequester(focusRequester)
            .focusable()
    } else {
        this
    }
}

/**
 * Composable wrapper that ensures default focus on TV platforms.
 * 
 * This is a convenience wrapper around defaultFocus() modifier.
 * It wraps the content in a focusable container that automatically
 * receives focus on TV platforms.
 * 
 * Usage:
 * ```
 * DefaultFocusContainer {
 *     // Your screen content
 * }
 * ```
 */
@Composable
fun DefaultFocusContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.defaultFocus()
    ) {
        content()
    }
}
