/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.subject.episode.tv

import androidx.compose.ui.graphics.ImageBitmap
import org.openani.mediamp.MediampPlayer

// 仅 Android TV 需要; 其他平台不进 TV 播放器
internal actual suspend fun captureTvPlayerFrame(player: MediampPlayer): ImageBitmap? = null
