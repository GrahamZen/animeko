/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation.tv

import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay

/**
 * TV 焦点落点轮询解析器: 每帧执行一次 [attempt] (通常是 requestFocus, 可附带滚动等
 * 前置动作), 以 [arrived] 确认真正到位, 到位或试满 [attempts] 次即结束.
 *
 * 为什么必须轮询 + 到位确认 (全库统一经验, 勿用单次 requestFocus 或其返回值):
 * - 目标节点未组合/未附着时 requestFocus 被焦点系统**静默拒绝**, 不抛异常,
 *   `runCatching { requestFocus() }.isSuccess` 恒真, 是假成功;
 * - 页面切换/弹窗开合期间其它异步焦点分配可能后到抢焦点, 单次请求会被覆盖,
 *   需要多帧断言直到 [arrived] (由目标的 onFocusChanged 置位) 确认.
 *
 * @param attempts 重试次数上限. 默认 40 次 (~1.2s), 覆盖绝大多数组合/动画时序;
 *   数据加载等更慢的场景酌情调大.
 * @param delayMillis 每次尝试间的补充等待 (帧间隔之外); 0 = 只按帧重试
 *   (适合目标已组合、只等焦点系统就绪的场景).
 * @param arrived 到位判据, 通常读一个由目标 onFocusChanged 置位的标志.
 * @param attempt 每次尝试的动作; requestFocus 需调用方自行 runCatching
 *   (节点未附着时抛 IllegalStateException).
 * @return 是否在限次内到位.
 */
suspend fun tvResolveFocus(
    attempts: Int = 40,
    delayMillis: Long = 30,
    arrived: () -> Boolean,
    attempt: suspend () -> Unit,
): Boolean {
    repeat(attempts) {
        withFrameNanos { }
        // 先查后试: 已到位 (或已放弃) 时不再多发一次 requestFocus ——
        // 用户可能已把焦点移走, 多发的这次会把焦点抢回来一格
        if (arrived()) return true
        attempt()
        if (arrived()) return true
        if (delayMillis > 0) delay(delayMillis)
    }
    return false
}
