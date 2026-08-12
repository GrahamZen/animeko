/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import me.him188.ani.app.ui.foundation.theme.EasingDurations
import kotlin.math.roundToInt

/**
 * @see AniMotionScheme
 */
@Stable
@Immutable
data class NavigationMotionScheme(
    val enterTransition: EnterTransition,
    val exitTransition: ExitTransition,
    val popEnterTransition: EnterTransition,
    val popExitTransition: ExitTransition,
) {
    companion object {
        inline val current
            @Composable get() = LocalNavigationMotionScheme.current

        // https://m3.material.io/styles/motion/easing-and-duration/applying-easing-and-duration#e5b958f0-435d-4e84-aed4-8d1ea395fa5c
        private const val enterDuration = EasingDurations.emphasizedDecelerate
        private const val exitDuration = EasingDurations.emphasizedAccelerate

        // https://m3.material.io/styles/motion/easing-and-duration/applying-easing-and-duration#26a169fb-caf3-445e-8267-4f1254e3e8bb
        // https://developer.android.com/develop/ui/compose/animation/shared-elements
        private val enterEasing = EmphasizedDecelerateEasing
        private val exitEasing = EmphasizedAccelerateEasing

        /**
         * 全屏页面间的同步 crossfade (dissolve, 无位移): 上一页变浅的同时下一页变深,
         * 同时长同步进行, 中途不经过空白底色 (fade-through 的"先全白再显示"在全屏
         * 海报页之间观感突兀).
         *
         * **线性缓动是刻意的**: 两条互补的线性曲线相加恒定, 全程总亮度不变; 换成
         * emphasized 那类曲线, 两端快中间慢, 中点会塌下去一块 —— 表现就是"闪一下".
         */
        fun calculateCrossfade(): NavigationMotionScheme {
            val exit = fadeOut(tween(CROSSFADE_DURATION, easing = LinearEasing))
            val enter = fadeIn(tween(CROSSFADE_DURATION, easing = LinearEasing))
            return NavigationMotionScheme(
                enterTransition = enter,
                exitTransition = exit,
                popEnterTransition = enter,
                popExitTransition = exit,
            )
        }

        /**
         * 取 M3 的 `emphasizedDecelerate` (400ms), 与非 crossfade 那条路的入场时长一致.
         *
         * 原值是 **1000ms**, 而 M3 最长的时长 token 也才 500ms: 全屏转场的建议区间是
         * 300ms (手机基准) ~ 390ms (大屏, 手机 +30%), 超过 400ms 就开始显得迟钝 ——
         * 用户反馈"点卡片进详情页的过渡不舒服"正是这一条.
         *
         * 它同时也是**卡顿**的来源: 全屏 crossfade 期间上下两个页面都在组合并绘制,
         * 4K UI 下每帧成本翻倍, 而这一整秒恰好压在详情页最重的首屏工作上 (backdrop
         * 取图/解码、剧照分集匹配). 缩到 400ms 把这个重叠窗口砍掉六成.
         *
         * 电视端不用官方那套 card → 详情的**共享元素** (container transform):
         * `SharedTransitionLayout` 的 scope provider 在 `AniAppContent` 里是注释掉的,
         * 全应用都没启用, 要接是另一件事 (见该处注释).
         */
        private const val CROSSFADE_DURATION = EasingDurations.emphasizedDecelerate

        fun calculate(useSlide: Boolean): NavigationMotionScheme {
            val slideInMargin = 1f / 16
            val slideOutMargin = 1f / 16

            val enterTransition: EnterTransition = run {
                if (useSlide) {
                    val delay = exitDuration
                    val slideIn = slideInHorizontally(
                        tween(enterDuration, delayMillis = delay, easing = enterEasing),
                        initialOffsetX = { (it * slideInMargin).roundToInt() },
                    )
                    val fadeIn = fadeIn(tween(enterDuration, delayMillis = exitDuration, easing = enterEasing))
                    slideIn.plus(fadeIn)
                } else {
                    fadeIn(tween(enterDuration, delayMillis = exitDuration, easing = enterEasing))
                }
            }

            val exitTransition: ExitTransition = kotlin.run {
                val fadeOut = fadeOut(tween(exitDuration, easing = exitEasing))
                if (useSlide) {
                    slideOutHorizontally(
                        tween(exitDuration, easing = exitEasing),
                        targetOffsetX = { -(it * slideOutMargin).roundToInt() },
                    ).plus(fadeOut)
                } else {
                    fadeOut
                }
            }

            val popEnterTransition = run {
                val fadeIn = fadeIn(tween(enterDuration, delayMillis = exitDuration, easing = enterEasing))
                if (useSlide) {
                    slideInHorizontally(
                        tween(enterDuration, delayMillis = exitDuration, easing = enterEasing),
                        initialOffsetX = { -(it * slideInMargin).roundToInt() },
                    ) + fadeIn
                } else {
                    fadeIn // clean fade
                }
            }

            // 从页面 A 回到上一个页面 B, 切走页面 A 的动画
            val popExitTransition: ExitTransition = run {
                val fadeOut = fadeOut(tween(exitDuration, easing = exitEasing))
                if (useSlide) {
                    val slide = slideOutHorizontally(
                        tween(exitDuration, easing = exitEasing),
                        targetOffsetX = { (it * slideOutMargin).roundToInt() },
                    )
                    slide.plus(fadeOut)
                } else {
                    fadeOut
                }
            }

            return NavigationMotionScheme(
                enterTransition = enterTransition,
                exitTransition = exitTransition,
                popEnterTransition = popEnterTransition,
                popExitTransition = popExitTransition,
            )
        }
    }
}

@Stable
val LocalNavigationMotionScheme = staticCompositionLocalOf<NavigationMotionScheme> {
    error("No LocalNavigationMotionScheme provided")
}
