/*
 * Copyright (C) 2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.remote

import kotlin.test.Test
import kotlin.test.assertContains

class RemoteControlPageMediaSessionTest {
    private val page = renderRemoteControlPage(
        initialTab = "player",
        searchFormHtml = "",
        requestSectionHtml = "",
    )

    @Test
    fun `system media controls are explicitly enabled with silent local audio`() {
        assertContains(page, "id=\"pb-system\"")
        assertContains(page, "function createSilentAudio()")
        assertContains(page, "new Blob([buffer], { type: 'audio/wav' })")
        assertContains(page, "mediaAudio.play()")
        assertContains(page, "window.mediaSessionActive = true")
    }

    @Test
    fun `media session publishes metadata state and progress`() {
        assertContains(page, "navigator.mediaSession.metadata = new MediaMetadata")
        assertContains(page, "title: mediaState.title")
        assertContains(page, "artist: mediaState.episode")
        assertContains(page, "navigator.mediaSession.playbackState")
        assertContains(page, "navigator.mediaSession.setPositionState")
    }

    @Test
    fun `native controls map to existing remote player endpoints`() {
        assertContains(page, "setMediaHandler('play'")
        assertContains(page, "setMediaHandler('pause'")
        assertContains(page, "setMediaHandler('seekbackward'")
        assertContains(page, "setMediaHandler('seekforward'")
        assertContains(page, "setMediaHandler('seekto'")
        assertContains(page, "setMediaHandler('previoustrack'")
        assertContains(page, "setMediaHandler('nexttrack'")
        assertContains(page, "post('api/player/control'")
        assertContains(page, "post('api/player/episode'")
    }

    @Test
    fun `enabled session keeps polling while page is hidden`() {
        assertContains(page, "!window.mediaSessionActive && (document.hidden || cur !== 'player' || window.sheets.any())")
        assertContains(page, "hooks.unavailable.push(function () { mediaState = null; disableMediaSession(true); })")
    }
}
