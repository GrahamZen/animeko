/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.subject.episode.tv

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * TV 播放器覆盖层的层级. 整个播放器界面只有这一个状态机, 所有层级切换都经由
 * [TvPlayerOverlayState] 的方法, 所有按键语义都收敛在 TvEpisodeScreen 根部的唯一路由里,
 * 不在各组件内散落 BackHandler/onKeyEvent 补丁.
 */
enum class TvPlayerLayer {
    /** 纯视频态: 无任何组件, 焦点在根节点上收按键. */
    HIDDEN,

    /** 控制层: 顶部标题/时钟 + 胶囊按钮行 (聚焦时上方浮出面板) + 进度条 + 图标行. */
    CONTROLS,

    /** 详情页覆盖层: 隐藏全部播放器组件, 正在播放的视频画面作为详情页背景. */
    DETAILS,
}

/** 胶囊按钮对应的浮出面板 (第二层). */
enum class TvPlayerPanel {
    /** 弹幕列表 (底部含弹幕源开关/延迟调整). */
    DANMAKU_LIST,

    /** 相关推荐 (卡片形态). */
    RECOMMENDATIONS,

    /** 本集评论. */
    COMMENTS,

    /** 角色 (卡片形态: 头像 + 名字 + 角色/CV, 点击弹人物预览). */
    CHARACTERS,

    /** 制作人员 (卡片形态: 头像 + 名字 + 职位, 点击弹人物预览). */
    STAFF,
}

/**
 * 焦点当前所在的区域. 由各行容器的 onFocusChanged 上报 —— 只在获得焦点时更新,
 * 失焦不清除 (焦点交接的瞬间两边都是无焦点, 清除会产生瞬时 NONE 抖动).
 * 根按键路由据此决定边界行为 (如图标行按下进详情页, 面板内按返回回进度条).
 */
enum class TvPlayerFocusRegion {
    NONE,

    /** 浮出面板内 (弹幕列表/推荐/评论的条目上). */
    PANEL,

    /** 进度条上方的胶囊按钮行. */
    PILLS,

    /** 进度条行. */
    PROGRESS,

    /** 进度条下方的图标行. */
    BOTTOM_ROW,

    /** 图标行下方的选集条 (Prime 形态: 图标行按下键展开, 焦点在轮播卡片上). */
    EPISODES,
}

/**
 * 焦点落点解析的目标 (见 [TvPlayerOverlayState.pendingFocus]).
 * ROOT/PROGRESS/EPISODE_STRIP/BOTTOM_ROW 由 TvEpisodeScreen 的统一解析器消化,
 * PANEL 由面板宿主 (TvPlayerPanelHost) 消化 —— 入口请求器在它那棵子树里.
 */
enum class TvPlayerFocusTarget {
    /** 根节点 (回纯视频态收回焦点). */
    ROOT,

    /** 进度条行. */
    PROGRESS,

    /** 选集条轮播卡片. */
    EPISODE_STRIP,

    /** 进度条下方图标行. */
    BOTTOM_ROW,

    /** 浮出面板入口. */
    PANEL,
}

/**
 * TV 播放器覆盖层状态机.
 *
 * 性能约定: 这里的每个字段都是独立的 State, 消费方须在尽可能小的作用域读取
 * (布局层用 AnimatedVisibility 的 lambda / 子组件内读取), 避免按键一次整层重组.
 */
@Stable
class TvPlayerOverlayState {
    var layer: TvPlayerLayer by mutableStateOf(TvPlayerLayer.HIDDEN)
        private set

    /** 当前浮出的面板; null = 无面板. 由胶囊按钮聚焦时设置, 焦点移到进度条/图标行时清除. */
    var activePanel: TvPlayerPanel? by mutableStateOf(null)

    var focusRegion: TvPlayerFocusRegion by mutableStateOf(TvPlayerFocusRegion.NONE)

    /** 弹幕输入框展开中 (IME 态): 除 Back 收起外, 按键全部交给输入框. */
    var danmakuInputExpanded: Boolean by mutableStateOf(false)

    /** 打开中的下拉弹层数量 (倍速/画面比例等, 经 onExpandedChanged 上报): >0 时不自动隐藏. */
    var openPopupCount: Int by mutableIntStateOf(0)

    /** 播放器统计悬浮层开关 (三个点菜单切换). */
    var showPlayerStats: Boolean by mutableStateOf(false)

    /**
     * 选集条展开中 (Prime 形态): 胶囊/进度条/图标行隐藏, 选集条完整展开在底部.
     * 收起态只在图标行下露出 "剧集" 标题 + 卡片顶部一条 (peek).
     */
    var episodeStripExpanded: Boolean by mutableStateOf(false)
        private set

    /** 选集条有内容可聚焦 (分集列表已加载且非空), 由选集条组件上报; 无内容时图标行下键直通详情页. */
    var episodeStripAvailable: Boolean by mutableStateOf(false)

    /** 自动隐藏计时锚: 每次按键交互自增, 计时协程以它为 key 重启. */
    var interactionTick: Int by mutableIntStateOf(0)
        private set

    /**
     * 待解析的焦点落点 (目标 + 序号; 序号使同目标连续请求也能重新触发解析).
     * 单一 pending: 新请求**替换**旧请求 —— 过去每个目标各挂一个独立轮询循环,
     * 快速交替 (如选集条 展开→收起→展开) 时新旧循环并发运行, 一方到位后另一方
     * 仍会继续 requestFocus 一秒多, 把焦点抢回去. 现在解析器用 collectLatest
     * 收本字段, 新请求一到旧解析立即取消, 不存在互抢窗口.
     *
     * 初始即请求 ROOT (进入页面根节点收焦, 纯视频态直接收按键).
     */
    var pendingFocus: Pair<TvPlayerFocusTarget, Int> by mutableStateOf(TvPlayerFocusTarget.ROOT to 0)
        private set

    private fun requestFocus(target: TvPlayerFocusTarget) {
        pendingFocus = target to (pendingFocus.second + 1)
    }

    fun markInteraction() {
        interactionTick++
    }

    /** 下拉弹层开合上报 (倍速/比例/字幕/三个点): 引用计数 + 重置自动隐藏计时. */
    fun onPopupExpandedChanged(expanded: Boolean) {
        openPopupCount = (openPopupCount + if (expanded) 1 else -1).coerceAtLeast(0)
        markInteraction()
    }

    /** 把焦点送进当前浮出面板 (点击胶囊按钮时). */
    fun requestPanelFocus() {
        requestFocus(TvPlayerFocusTarget.PANEL)
    }

    /** 唤出控制层; [focusProgress] = 进入后把焦点放到进度条行 (默认). */
    fun showControls(focusProgress: Boolean = true) {
        layer = TvPlayerLayer.CONTROLS
        // 上次隐藏时遗留的视觉状态在这里复位, 而不是在 hideAll/openDetails 里:
        // 覆盖层淡出途中复位会让被隐藏的控制行/图标行反向播放"回来"的入场动画
        // (半程可见后整层才消失), 观感是返回卡了一下
        episodeStripExpanded = false
        activePanel = null
        focusRegion = TvPlayerFocusRegion.NONE
        markInteraction()
        if (focusProgress) requestFocus(TvPlayerFocusTarget.PROGRESS)
    }

    /** 从详情层回到选集条 (详情页顶部按上键): 控制层出现时选集条直接是展开态并聚焦. */
    fun returnToEpisodeStrip() {
        // 无分集 (未开播条目) 时选集条不渲染: 展开态会把控制行也藏起来, 焦点请求器
        // 永远解析不到, 整层没有任何可聚焦目标 —— 退回普通控制层 (焦点落进度条)
        if (!episodeStripAvailable) {
            showControls()
            return
        }
        layer = TvPlayerLayer.CONTROLS
        episodeStripExpanded = true
        activePanel = null
        focusRegion = TvPlayerFocusRegion.NONE
        markInteraction()
        requestFocus(TvPlayerFocusTarget.EPISODE_STRIP)
    }

    /** 展开选集条 (图标行按下键): 控制行隐藏, 焦点送到轮播卡片 (当前集). */
    fun expandEpisodeStrip() {
        episodeStripExpanded = true
        markInteraction()
        requestFocus(TvPlayerFocusTarget.EPISODE_STRIP)
    }

    /** 收起选集条 (卡片上按上键): 控制行回来, 焦点还给图标行. */
    fun collapseEpisodeStrip() {
        episodeStripExpanded = false
        markInteraction()
        requestFocus(TvPlayerFocusTarget.BOTTOM_ROW)
    }

    /** 隐藏一切组件回纯视频态, 焦点收回根节点. 视觉状态不复位 (见 [showControls]). */
    fun hideAll() {
        layer = TvPlayerLayer.HIDDEN
        danmakuInputExpanded = false
        requestFocus(TvPlayerFocusTarget.ROOT)
    }

    /** 打开详情页覆盖层 (隐藏全部播放器组件). 视觉状态不复位 (见 [showControls]). */
    fun openDetails() {
        layer = TvPlayerLayer.DETAILS
        danmakuInputExpanded = false
    }

    /** 把焦点送回进度条行 (面板内按返回等). */
    fun focusProgress() {
        requestFocus(TvPlayerFocusTarget.PROGRESS)
    }
}
