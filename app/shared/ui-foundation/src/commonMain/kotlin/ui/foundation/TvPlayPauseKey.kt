/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * 播放/暂停键全集. 比 [TV_PLAY_KEYS] 多一个 [Key.MediaPause] —— 那边是"能长按的播放键"
 * (长按=强刷), 只暂停的键不该被长按改写; 这边是"按下就切换播放状态"的键, 三个一视同仁.
 */
val TV_PLAY_PAUSE_KEYS = setOf(Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause)

/**
 * 遥控器播放/暂停键的处理器: 播放页在自己整棵组合上提供 (值 = 切换播放/暂停), 别处为 null.
 *
 * **为什么要用 CompositionLocal 而不是直接写进播放页的按键路由**: 播放页的唯一按键路由挂在
 * 根节点的 `onPreviewKeyEvent` 上, 只覆盖得到同一个窗口内的东西. Dialog / Popup /
 * DropdownMenu 在 Android 上各是**独立窗口**, 它们的按键根本到不了播放页的路由, 于是画面还在
 * 后面放着, 播放/暂停键却按不动 (弹窗自己也不认识这个键, 事件就此消失). CompositionLocal 能
 * 跨窗口边界 —— Dialog 的内容是父组合的子组合, 照常继承 —— 正好用来把播放器的开关递进去.
 *
 * 用法: 独立窗口的内容根 (弹窗面板 / 菜单容器) 挂 [Modifier.tvPlayPauseKey]. 播放页之外
 * 本地为 null, 修饰符退化成空操作, 同一个组件在别的页面照常无副作用.
 */
val LocalTvPlayPauseHandler = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * 让盖在播放画面上的**独立窗口** (弹窗/下拉菜单/浮出层) 也能用遥控器播放/暂停键切换播放状态.
 *
 * 语义与播放页根路由里那几档完全一致: 按下即切换, 不碰焦点也不碰控制层的显隐 —— 弹窗各有各的
 * 焦点归属与关闭后的落点, 这个键不该把它们弄乱; 反馈由画面中央的暂停图标给
 * (它监听播放器状态流, 与触发来源无关).
 *
 * 挂在能收到该窗口按键的祖先上 (弹窗内容根 / 菜单容器), 不是逐个控件挂.
 *
 * 无论有没有处理器都消费掉整个手势 (KeyDown + KeyUp): 播放页里它已经有确定的语义,
 * 放行下去只会被弹窗内的控件当成不认识的键再丢一次.
 */
@Composable
fun Modifier.tvPlayPauseKey(): Modifier {
    val handler = LocalTvPlayPauseHandler.current ?: return this
    return onPreviewKeyEvent { event ->
        if (event.key !in TV_PLAY_PAUSE_KEYS) return@onPreviewKeyEvent false
        if (event.type == KeyEventType.KeyDown) handler()
        true
    }
}
