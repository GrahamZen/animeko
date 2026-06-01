/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation

import androidx.compose.ui.graphics.ImageBitmap

/**
 * TV 播放器暂停帧快照传递 (跨导航): 播放器在跳转缓存页前捕获当前画面写入,
 * 缓存页取走作为半透明遮罩背景 (导航离开播放器时播放自动暂停, 该帧即暂停画面).
 *
 * 一次性消费 ([take] 即清空): 只有从播放器进入的缓存页会拿到帧,
 * 其他入口 (如条目详情页) 不会误用陈旧画面.
 */
object PlayerFrameHolder {
    private var frame: ImageBitmap? = null
    private var subjectId: Int? = null

    /** 播放器侧: 跳转前写入捕获的帧 (捕获失败传 null 则缓存页回退普通背景). */
    fun put(frame: ImageBitmap?, subjectId: Int? = null) {
        this.frame = frame
        this.subjectId = subjectId
    }

    /**
     * 缓存页侧: **只读不清**.
     *
     * 原先是 take() (取走即清). 那样一来缓存页从更深的页面 (管理全部缓存) 返回时,
     * 页面重组会再取一次而拿到 null, 背景退回浅色白底 —— fork 当时是靠让 VM 跨导航存活绕开的,
     * 但上游 2026-08-23 的缓存重做刻意把 detail pane 的 VM 收紧到组合生命周期
     * (避免累积后台 collector), 两个目标直接冲突.
     *
     * 改成只读之后帧不再与任何组件的生命周期绑定. 防陈旧靠 [subjectId] 对号:
     * 只有正是从那一部的播放器跳过来时才认这张帧, 别处进缓存页看到的仍是普通背景.
     * (同详情页 backdrop 从"取走即清"改成 peek 三态的做法.)
     */
    fun peek(forSubjectId: Int? = null): ImageBitmap? =
        if (forSubjectId == null || forSubjectId == subjectId) frame else null

    /** 明确丢弃当前帧 (如播放器换了条目). */
    fun clear() {
        frame = null
        subjectId = null
    }
}
