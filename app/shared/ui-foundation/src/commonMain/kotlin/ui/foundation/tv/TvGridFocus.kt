/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation.tv

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.delay

/**
 * TV 竖版海报网格的统一焦点落点协调器 (追番页/搜索结果页共用).
 *
 * 所有"把焦点送到网格某张卡"的入口 (同列上下导航 / 返回回首卡 / 顶部行下键落视口首行 /
 * 进页恢复焦点 / 跨 tab 行对齐) 都通过 [request]/[requestRow] 发出统一落点请求, 由网格所在
 * 组合内的 [resolve] 循环消化: 等数据就绪 → 解析目标下标 → 请求聚焦 (目标已组合时不打断
 * 吸顶动画) → 卡片 [onCardFocused] 确认到位, 不到位滚动让目标组合出来再试.
 *
 * 不能直连 requestFocus 的原因: 目标卡未组合时 requestFocus 被焦点系统静默拒绝
 * (runCatching 照样报成功), 按键被吞且不重试, 表现为焦点卡死; 统一走带到位确认的
 * 解析循环, 避免多套解析器各自维护退出条件时相互踩坑.
 */
@Stable
class TvGridFocusController {
    /** 当前落点请求; null = 空闲. 调用方可据此判断"解析进行中" (如抑制 tab 的聚焦即选中). */
    var pending: TvGridFocusRequest? by mutableStateOf(null)
        private set

    /**
     * 解析出的目标卡下标 (该卡挂 [requester]); -1 = 无. 每次导航都会 设置->清除 变两次,
     * 使用处用 derivedStateOf 收窄成"是否目标卡"的布尔, 只让目标卡自己 (挂/摘请求器) 重组.
     */
    var resolvedIndex: Int by mutableIntStateOf(-1)
        private set

    /** 目标卡的焦点请求器: 卡片在 index == [resolvedIndex] 时挂载. */
    val requester: FocusRequester = FocusRequester()

    // 目标卡真实拿到焦点的确认标志 (由 [onCardFocused] 置位). 不能拿"最后聚焦下标 == 目标"
    // 当退出条件: 目标恰好等于此前聚焦过的卡时 (如 tab 行下键落回吸顶可视首行行首, 而那正是
    // 离开网格前聚焦的卡) 会在焦点尚未移动时"假成功"提前退出.
    private var arrived = false

    /** 请求聚焦绝对下标 [index] 的卡 (超出数据量时夹到最后一张). */
    fun request(index: Int) {
        pending = TvGridFocusRequest(index = index, seq = (pending?.seq ?: 0) + 1)
    }

    /** 行对齐落点: 聚焦第 [row] 行的最左/最右卡, 行数不足时夹到最后一行对应端 (跨 tab 导航用). */
    fun requestRow(row: Int, rowStart: Boolean) {
        pending = TvGridFocusRequest(row = row, rowStart = rowStart, seq = (pending?.seq ?: 0) + 1)
    }

    /** 卡片 onFocused 中调用: 确认落点到位. */
    fun onCardFocused(index: Int) {
        if (index == resolvedIndex) arrived = true
    }

    /**
     * 落点解析循环: 在网格所在组合内 `LaunchedEffect(controller.pending, ...)` 调用.
     * [onEmptyIdle] 非 null 时: 数据为空且不在首屏加载 → 执行收尾动作 (如聚焦 tab 标签) 并结束;
     * 为 null 时空数据只等待 (直到超时或数据到达).
     */
    suspend fun resolve(
        gridState: LazyGridState,
        columns: () -> Int,
        itemCount: () -> Int,
        isLoadingFirstPage: () -> Boolean = { false },
        onEmptyIdle: (() -> Unit)? = null,
        attempts: Int = 80,
    ) {
        val target = pending ?: return
        arrived = false
        repeat(attempts) {
            withFrameNanos { }
            val count = itemCount()
            val cols = columns().coerceAtLeast(1)
            if (count > 0) {
                val idx = target.index?.coerceAtMost(count - 1)
                    ?: if (target.rowStart) {
                        val i = target.row * cols
                        if (i < count) i else ((count - 1) / cols) * cols
                    } else {
                        minOf(target.row * cols + cols - 1, count - 1)
                    }
                resolvedIndex = idx
                withFrameNanos { } // 等目标卡上的请求器挂载
                runCatching { requester.requestFocus() }
                if (arrived) {
                    pending = null
                    resolvedIndex = -1
                    return
                }
                // 目标卡未组合 (聚焦失败): 滚过去让它组合出来再试
                runCatching { gridState.scrollToItem((idx / cols) * cols) }
            } else if (onEmptyIdle != null && !isLoadingFirstPage()) {
                onEmptyIdle()
                pending = null
                resolvedIndex = -1
                return
            }
            delay(30)
        }
        pending = null
        resolvedIndex = -1
    }
}

/**
 * 统一网格落点请求: [index] 非 null 时为绝对目标卡下标; 否则按 [row] + [rowStart] 行对齐 ——
 * 聚焦第 [row] 行的最左 ([rowStart]=true) / 最右卡. [seq] 使连续发出的同参请求也能重新触发解析.
 */
data class TvGridFocusRequest(
    val index: Int? = null,
    val row: Int = 0,
    val rowStart: Boolean = true,
    val seq: Int = 0,
)

/**
 * 竖版海报网格的方向键路由 (追番页/搜索结果页共用), 配合 [TvGridFocusController]:
 * - 上/下键显式同列导航, 不交给默认方向搜索 —— 吸顶后上一行在视口外未组合, 越界组合只补出
 *   前一个 item (上一行最后一张), 焦点必然斜跳; 吸顶滚动进行中方向搜索又按瞬时几何位置挑
 *   候选, 偶尔斜跳到别的列. 顶行上键交 [onTopRowUp]; 末行不满时同列下方没有卡则落到最后
 *   一张; 已是最后一行则消费掉防斜跳.
 * - 播放/暂停键交 [onPlayKey] (聚焦卡直达播放).
 * - 其余 KeyDown 交 [extraKeys] (如追番页跨 tab 左右导航), 返回 false 走默认焦点搜索.
 */
fun Modifier.tvGridKeyNavigation(
    controller: TvGridFocusController,
    focusedIndex: () -> Int,
    itemCount: () -> Int,
    columns: () -> Int,
    onTopRowUp: () -> Boolean,
    onPlayKey: (focusedIndex: Int) -> Boolean,
    enabled: () -> Boolean = { true },
    extraKeys: ((event: KeyEvent, focusedIndex: Int, columns: Int, itemCount: Int) -> Boolean)? = null,
): Modifier = onPreviewKeyEvent { event ->
    if (!enabled()) return@onPreviewKeyEvent false
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    val focused = focusedIndex()
    val count = itemCount()
    if (focused < 0 || count == 0) return@onPreviewKeyEvent false
    val cols = columns().coerceAtLeast(1)
    when (event.key) {
        Key.DirectionUp ->
            if (focused < cols) {
                onTopRowUp()
            } else {
                controller.request(focused - cols)
                true
            }

        Key.DirectionDown -> {
            val next = focused + cols
            when {
                next < count -> {
                    controller.request(next)
                    true
                }

                // 末行不满时同列下方没有卡: 落到最后一张
                focused / cols < (count - 1) / cols -> {
                    controller.request(count - 1)
                    true
                }

                // 已是最后一行: 消费掉, 防止焦点斜跳
                else -> true
            }
        }

        Key.MediaPlayPause, Key.MediaPlay -> onPlayKey(focused)

        else -> extraKeys?.invoke(event, focused, cols, count) ?: false
    }
}
