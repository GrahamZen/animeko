/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation.effects

import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import me.him188.ani.utils.logging.logger
import me.him188.ani.utils.logging.warn

private val logger = logger("NoticeSound")

/**
 * 用哪一颗系统按键音. **换音色只改这一行.**
 *
 * 可选值与 ROM 里的音频文件一一对应 (2026-08-11 真机 `/product/media/audio/ui/`):
 * - [AudioManager.FX_KEYPRESS_RETURN] → `KeypressReturn.ogg`, 回车/确认音, 语义最贴"好了"
 * - [AudioManager.FX_KEYPRESS_STANDARD] → `KeypressStandard.ogg`, 普通按键
 * - [AudioManager.FX_KEYPRESS_INVALID] → `KeypressInvalid.ogg`, 最长最显眼, 但语义是"无效操作"
 * - [AudioManager.FX_KEYPRESS_DELETE] / [AudioManager.FX_KEYPRESS_SPACEBAR] → 对应的键音
 * - [AudioManager.FX_KEY_CLICK] → `Effect_Tick.ogg`. **别用**: 电视系统 UI 的遥控器导航音就是它,
 *   用户在桌面上一直在听, 拿它当提示会被当成背景噪音.
 */
private const val NOTICE_SOUND_EFFECT = AudioManager.FX_KEYPRESS_RETURN

/**
 * 音量给满. 默认那一档 (传 -1 是"音乐音量 -3dB") 在电视喇叭上偏小, 而这声提示的前提就是用户
 * 没在看屏幕, 听不见等于没有.
 */
private const val NOTICE_SOUND_VOLUME = 1f

/**
 * 响一声系统自带的按键音.
 *
 * 用 [AudioManager.playSoundEffect] 走系统 UI 音效那一套, 而**不是**通知音, 原因:
 * - **电视上通常压根没配通知音**. 2026-08-11 真机: `settings get system notification_sound` → `null`,
 *   整个 ROM 的 `/product/media/audio` 下只有 6 个按键音 + 2 个闹钟音, 一个通知音都没有 ——
 *   所以 `RingtoneManager` 那条路 (含枚举可用通知音) 在电视上必然是一声不响.
 *   **坑**: `RingtoneManager.getDefaultUri()` 返回的是 `content://settings/system/notification_sound`
 *   这个**符号 URI**, 系统有没有真的配过它都不为 null, 于是"拿不到就退回别的音"这种写法是死代码;
 *   真要判存在只能用 `getActualDefaultRingtoneUri(context, type)` (未设置时才返回 null).
 * - [android.media.ToneGenerator] 合成音试过, 音色难听.
 * - Animeko 自己一处按键音都不用, 所以这声按键音在应用内不会被误当成操作反馈.
 *
 * 这条路会自动尊重系统的"界面音效"开关 (`Settings.System.SOUND_EFFECTS_ENABLED`), 用户关了就不响 ——
 * 这是刻意的, 不再另找退路硬响.
 */
@Composable
actual fun rememberNoticeSoundPlayer(): () -> Unit {
    val context = LocalContext.current
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    DisposableEffect(audioManager) {
        // 预载, 否则第一声经常被吞: 系统的 SoundPool 是懒加载的, 第一次 playSoundEffect 常常只
        // 触发加载而放不出声 —— 而这声提示往往一次会话只响一回, 吞掉就等于没有.
        //
        // 不配对调用 unloadSoundEffects: 那是**全局**状态 (整机共用一个 SoundPool), 卸掉会连带
        // 影响系统 UI 自己的音效; 官方文档也只把它定位成"想省内存时才调".
        runCatching { audioManager?.loadSoundEffects() }
            .onFailure { logger.warn(it) { "Failed to preload system sound effects" } }
        onDispose { }
    }

    return remember(audioManager) {
        {
            runCatching { audioManager?.playSoundEffect(NOTICE_SOUND_EFFECT, NOTICE_SOUND_VOLUME) }
                .onFailure { logger.warn(it) { "Failed to play notice sound effect" } }
        }
    }
}
