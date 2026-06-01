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

/**
 * 捕获播放器当前画面 (降采样到约 720p, 供缓存页背景用; 见 TvPlayerFrameHolder).
 * 仅 Android (TV) 实现; 失败或其他平台返回 null.
 */
internal expect suspend fun captureTvPlayerFrame(player: MediampPlayer): ImageBitmap?
