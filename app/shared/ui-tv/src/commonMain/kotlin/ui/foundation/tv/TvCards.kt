/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation.tv

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.him188.ani.app.ui.external.placeholder.placeholder
import me.him188.ani.app.ui.foundation.AsyncImage
import kotlin.math.pow

/**
 * TV 竖版封面卡片 (探索页 / 追番页共用): 聚焦时主题主色外圈 (外圈与封面之间留一圈空隙,
 * 不需要动态取色). [imageUrl] 为 null 时显示加载占位. 长按 (遥控器确定键长按 / 触屏长按)
 * 弹出 [menu] (用于承载与详情页收藏按钮一致的收藏状态下拉).
 *
 * 焦点请求也可通过 [modifier] 挂 [FocusRequester]: 请求会委托给子树里第一个焦点目标
 * (卡片内容本体), 因此外部可以叠加多枚请求器 (如"首卡"与"恢复焦点"各一枚).
 */
@Composable
fun TvPortraitCard(
    imageUrl: String?,
    contentDescription: String?,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocusChangedExtra: ((Boolean) -> Unit)? = null,
    menu: (@Composable (expanded: Boolean, onDismiss: () -> Unit) -> Unit)? = null,
    /** 集数观看进度 (0..1): 贴卡片底缘画一条细进度条; null 不显示. */
    progress: Float? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    var menuExpanded by remember { mutableStateOf(false) }
    // 遥控器确认键长按检测 (同详情页播放按钮): 按下连发计数到阈值即"按住途中"立即弹菜单 (不等松开).
    // 残余确认键 (后续连发 / 松开) 由弹出的菜单自身吞掉 (调用方负责, 见探索页 collectionMenuFor).
    var confirmDownCount by remember { mutableStateOf(0) }
    var longPressFired by remember { mutableStateOf(false) }
    Box(
        modifier
            .aspectRatio(TV_PORTRAIT_CARD_COVER_RATIO)
            .then(
                if (focused) {
                    Modifier.border(
                        TV_CARD_FOCUS_RING_WIDTH,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(TV_PORTRAIT_CARD_CORNER + TV_CARD_FOCUS_GAP),
                    )
                } else Modifier,
            ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(TV_CARD_FOCUS_GAP),
            shape = RoundedCornerShape(TV_PORTRAIT_CARD_CORNER),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                    .onFocusChanged {
                        if (it.isFocused) onFocused()
                        onFocusChangedExtra?.invoke(it.isFocused)
                        if (!it.isFocused) {
                            confirmDownCount = 0
                            longPressFired = false
                        }
                    }
                    .then(
                        if (menu == null) {
                            Modifier
                        } else {
                            Modifier.onPreviewKeyEvent { event ->
                                val isConfirm = event.key == Key.DirectionCenter ||
                                        event.key == Key.Enter || event.key == Key.NumPadEnter
                                if (!isConfirm) return@onPreviewKeyEvent false
                                when (event.type) {
                                    KeyEventType.KeyDown -> {
                                        confirmDownCount++
                                        if (!longPressFired &&
                                            confirmDownCount > TV_LONG_PRESS_CONFIRM_KEY_REPEATS
                                        ) {
                                            longPressFired = true
                                            menuExpanded = true // 按住途中到阈值立即弹菜单
                                        }
                                    }

                                    KeyEventType.KeyUp -> {
                                        val fired = longPressFired
                                        confirmDownCount = 0
                                        longPressFired = false
                                        if (!fired) onClick() // 未达长按 = 短按, 触发点击
                                    }
                                }
                                true // 吞掉本卡确认键: 短按由 KeyUp 点击; 长按残余键改由菜单吞掉
                            }
                        },
                    )
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick,
                        onLongClick = menu?.let { { menuExpanded = true } },
                    ),
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        imageUrl,
                        contentDescription = contentDescription,
                        Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        Modifier.fillMaxSize()
                            .placeholder(true, shape = RoundedCornerShape(TV_PORTRAIT_CARD_CORNER)),
                    )
                }
                // 集数观看进度条: 与详情页选集卡 (FocusEpisodeProgressBar) 同款悬浮胶囊条 —
                // 左右内缩避开圆角, 离底边一点空隙, 圆头, 白 30% 轨道 + 主题色填充
                if (progress != null && progress > 0f) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = TV_CARD_PROGRESS_BAR_INSET)
                            .padding(bottom = TV_CARD_PROGRESS_BAR_BOTTOM_GAP)
                            .fillMaxWidth()
                            .height(TV_CARD_PROGRESS_BAR_HEIGHT)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = TV_CARD_PROGRESS_TRACK_ALPHA)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .height(TV_CARD_PROGRESS_BAR_HEIGHT)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
        // 菜单以卡片右下角为锚点弹出 (DropdownMenu 默认从锚点向右/上下就近展开): 放一个对齐到
        // 卡片右下角的零尺寸锚点, 菜单即从右下角向右上方向弹出.
        if (menu != null) {
            Box(Modifier.align(Alignment.BottomEnd)) {
                menu(menuExpanded) { menuExpanded = false }
            }
        }
    }
}

/**
 * TV Hero 操作按钮 (立即观看 / 更多详细内容 / 继续观看等). 两枚都是深/浅灰实心
 * (参考 Prime: 主按钮略亮, 次按钮接近底色), 按白天/黑夜主题分别取色. 聚焦时整颗按钮
 * 高亮为主题主色、文字/图标反色 (onPrimary), 与侧边栏选中一致.
 * [filled] = true 为主按钮 (略亮一档). 图标 + 文字单行.
 */
@Composable
fun TvHeroButton(
    text: String,
    icon: ImageVector,
    filled: Boolean,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocusChangedExtra: ((Boolean) -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    // 按当前主题明暗取底色 (由 surface 亮度判定, 兼容手动日夜切换):
    // 黑夜: 主按钮 rgb(49,54,61), 次按钮接近黑; 白天: 对应的浅灰两档.
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val baseContainer = when {
        dark && filled -> Color(0xFF31363D)
        dark -> Color(0xFF17191C)
        filled -> Color(0xFFDBE0E6)
        else -> Color(0xFFF1F3F6)
    }
    val container = if (focused) MaterialTheme.colorScheme.primary else baseContainer
    val content = if (focused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    // 按 TV_HERO_BUTTON_SCALE 整体缩放内边距/图标/字号
    val scale = TV_HERO_BUTTON_SCALE
    val textStyle = MaterialTheme.typography.titleSmall.let {
        it.copy(fontSize = it.fontSize * scale, lineHeight = it.lineHeight * scale)
    }
    Surface(
        onClick = onClick,
        modifier = modifier
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .onFocusChanged {
                if (it.isFocused) onFocused()
                onFocusChangedExtra?.invoke(it.isFocused)
            },
        shape = RoundedCornerShape(TV_HERO_BUTTON_CORNER),
        color = container,
        interactionSource = interactionSource,
    ) {
        Row(
            // 内边距对齐 Prime 实测 (单行按钮 30.5dp 高, 水平留白 ~13dp, 垂直墨迹留白 ~9dp)
            Modifier.padding(horizontal = 14.dp * scale, vertical = 8.dp * scale),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp * scale),
        ) {
            Icon(icon, contentDescription = null, Modifier.size(20.dp * scale), tint = content)
            Text(text, color = content, style = textStyle, maxLines = 1)
        }
    }
}

/**
 * TV hero 区标题/正文文字色 (对齐 Prime 实测): 黑夜 #F1F1F1 —— 亮中性白, 无色相、无投影
 * (实测字形边缘无暗晕, 可读性靠文字够亮 + backdrop 渐隐压暗). M3 的 onSurface/onSurfaceVariant
 * 偏暗且带紫色相, 在深色 backdrop 上显得发糊. 白天用等效中性深灰.
 */
@Composable
fun tvHeroContentColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xFFF1F1F1) else Color(0xFF1A1C1E)

/** TV hero 区次要信息文字色 (连载信息/日期等, 对齐 Prime 实测): 黑夜 #B4B5B7 中性灰; 白天等效. */
@Composable
fun tvHeroSecondaryContentColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xFFB4B5B7) else Color(0xFF5B5D60)

/**
 * TV backdrop 下缘渐隐的 DstOut 渐变停点: 擦除 alpha 在 [start]..[end] (绘制坐标 0..1)
 * 内从 0 平滑升到 1. 曲线 = quintic smootherstep (两端一、二阶导都为 0) 经 1-(1-s)^power
 * 反变换: 起点以零斜率极缓进入 (看不到"渐变开始"的分界线), 中段较快压暗, 尾段以指数级
 * 放缓渐近全黑、一直渐变到 [end] (通常传图的底边 1.0) —— 终点同样无分界线.
 * [power] 越大前段越快、尾巴越长. 采样多段生成停点, 避免手写折点产生马赫带.
 */
fun tvBackdropFadeToBlackStops(
    start: Float,
    end: Float,
    power: Float = 2.5f,
    samples: Int = 20,
): Array<Pair<Float, Color>> = Array(samples + 1) { i ->
    val f = i / samples.toFloat()
    val s5 = f * f * f * (f * (f * 6f - 15f) + 10f)
    (start + (end - start) * f) to Color.Black.copy(alpha = 1f - (1f - s5).pow(power))
}

/**
 * TV backdrop 边缘渐隐的 DstOut 渐变停点: 擦除 alpha 在 [start]..[end] 内从 [maxAlpha]
 * 平滑降到 0 ([start] 之前按 [maxAlpha] 擦除, [end] 之后图完全清晰). smoothstep 采样,
 * 理由同上. [maxAlpha] < 1 时是"压暗"而非完全擦除 (如顶缘给悬浮文字提高可读性).
 */
fun tvBackdropFadeFromBlackStops(
    start: Float,
    end: Float,
    maxAlpha: Float = 1f,
    samples: Int = 14,
): Array<Pair<Float, Color>> = Array(samples + 1) { i ->
    val f = i / samples.toFloat()
    val s = f * f * (3f - 2f * f)
    (start + (end - start) * f) to Color.Black.copy(alpha = maxAlpha * (1f - s))
}

/**
 * TV 页面背景 backdrop 层 (追番页 / 搜索页共用): 16:9 贴右上角, 高度为屏高固定比例
 * ([TV_BACKDROP_HEIGHT_FRACTION]), 顶缘轻度压暗 + 左缘/下缘 DstOut 渐隐入页面背景
 * (恒用探索页"卡片态"渐变). 调用方通常 `Modifier.align(Alignment.TopEnd)`.
 *
 * [backdropUrl] 用 lambda 而非值传入: URL 由"聚焦条目"状态推导, 状态读取发生在本组件
 * 内部 —— 遥控器每移一格只重组这一小块, 不连带调用方整个页面作用域重组.
 */
@Composable
fun TvPageBackdropLayer(
    backdropUrl: () -> String?,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        backdropUrl(),
        modifier,
        animationSpec = tween(TV_BACKDROP_CROSSFADE_MILLIS),
    ) { url ->
        if (url != null) {
            Box(
                Modifier
                    .fillMaxHeight(TV_BACKDROP_HEIGHT_FRACTION)
                    .aspectRatio(TV_BACKDROP_ASPECT_RATIO, matchHeightConstraintsFirst = true)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        // 停点由平滑曲线采样生成 (无折点, 避免暗色端可见的马赫带分界线);
                        // 顶缘轻度压暗 (非全擦): 给悬浮在 backdrop 上的顶部文字一层可读性 scrim
                        drawRect(
                            brush = Brush.verticalGradient(
                                *tvBackdropFadeFromBlackStops(
                                    start = 0f, end = TV_BACKDROP_TOP_SCRIM_END,
                                    maxAlpha = TV_BACKDROP_TOP_SCRIM_ALPHA,
                                ),
                            ),
                            blendMode = BlendMode.DstOut,
                        )
                        drawRect(
                            brush = Brush.horizontalGradient(
                                *tvBackdropFadeFromBlackStops(
                                    start = TV_BACKDROP_LEFT_FADE_START,
                                    end = TV_BACKDROP_LEFT_FADE_END,
                                ),
                            ),
                            blendMode = BlendMode.DstOut,
                        )
                        // 下缘渐隐: 零斜率极缓起步 + 指数级长尾渐近全黑, 一直渐变到图底
                        drawRect(
                            brush = Brush.verticalGradient(
                                *tvBackdropFadeToBlackStops(
                                    start = TV_BACKDROP_BOTTOM_FADE_START,
                                    end = 1f,
                                ),
                            ),
                            blendMode = BlendMode.DstOut,
                        )
                    },
            ) {
                AsyncImage(
                    url,
                    contentDescription = null,
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

// ============ TV 沉浸式页面 (探索/追番/搜索) 共享调参 ============
// 探索页轮播 (hero) 态的参数不在此列, 单独放在 TvExplorationPage 里.

// ---- backdrop ----

/** backdrop 宽高比. */
const val TV_BACKDROP_ASPECT_RATIO = 16f / 9f

/** backdrop 高度占屏高比例 (追番/搜索; 探索页因轮播布局单独一档). */
const val TV_BACKDROP_HEIGHT_FRACTION = 0.70f

/** backdrop 换图的淡入淡出时长 (毫秒). */
const val TV_BACKDROP_CROSSFADE_MILLIS = 600

/** backdrop 顶缘压暗带终点 (图片高度坐标 0..1; 顶部悬浮文字的可读性 scrim). */
const val TV_BACKDROP_TOP_SCRIM_END = 0.16f

/** backdrop 顶缘压暗强度 (1 = 完全擦除). */
const val TV_BACKDROP_TOP_SCRIM_ALPHA = 0.7f

/**
 * backdrop 下缘渐隐起点 (图片高度坐标 0..1, 此处开始向下渐暗, 一直渐变到图底).
 * 卡片聚焦态共用; 探索页轮播态另有自己的一档.
 */
const val TV_BACKDROP_BOTTOM_FADE_START = 0.78f

/**
 * TV hero backdrop 左缘渐隐窗口起点 (图片宽度坐标 0..1, 此前全擦除).
 * 探索 (卡片态) / 追番 / 搜索三页共用, 改这里三页一起变.
 */
const val TV_BACKDROP_LEFT_FADE_START = 0.02f

/** TV hero backdrop 左缘渐隐窗口终点 (此处起图完全清晰). 三页共用. */
const val TV_BACKDROP_LEFT_FADE_END = 0.3f

// ---- hero 文字 ----

/** TV hero 标题占屏宽比例 (右侧留给 backdrop 清晰区). */
const val TV_HERO_TITLE_WIDTH_FRACTION = 0.5f

/** TV hero 简介/状态行文字占内容列宽比例 (右边界之外留给 backdrop 清晰区). 三页共用. */
const val TV_HERO_SUMMARY_WIDTH_FRACTION = 0.4f

/** TV hero 信息块换条目时文字的渐隐渐现时长 (毫秒). */
const val TV_HERO_TEXT_FADE_MILLIS = 500

/** TV hero 媒体 (backdrop/简介等) 请求防抖: 焦点在卡片间快速划过时不发请求. */
const val TV_HERO_MEDIA_DEBOUNCE_MILLIS = 300L

// ---- 卡片网格 ----

/** 竖版海报卡片宽度 (Adaptive 网格按此为最小宽度自动决定列数). */
val TV_PAGE_CARD_WIDTH: Dp = 112.dp

/** 卡片间距. */
val TV_PAGE_CARD_SPACING = 10.dp

// ---- 底部遮罩 / 右下角提示 / 页面留白 ----

/** 底缘渐变遮罩高度 (覆盖被视口截断的下一行卡片露出的整段). */
val TV_PAGE_BOTTOM_SCRIM_HEIGHT = 90.dp

/** 底缘遮罩在最底边的不透明度 (1 = 底边完全融入页面背景). */
const val TV_PAGE_BOTTOM_SCRIM_MAX_ALPHA = 0.95f

/** 右下角遥控键提示的底部留白. */
val TV_PAGE_HINT_BOTTOM_PAD = 12.dp

/** 右下角遥控键提示的图标尺寸. */
val TV_PAGE_HINT_ICON_SIZE = 14.dp

/** 内容右侧留白. */
val TV_PAGE_END_PAD = 48.dp

/** 竖版封面宽高比 (与详情页封面一致; 网格行高估算也用它). */
const val TV_PORTRAIT_CARD_COVER_RATIO = 0.72f

/** 卡片圆角. */
private val TV_PORTRAIT_CARD_CORNER = 8.dp

/** 聚焦外圈描边宽度 (主题主色). */
private val TV_CARD_FOCUS_RING_WIDTH = 2.5.dp

/** 聚焦外圈与封面之间的空隙 (卡片内容常驻内缩此值, 聚焦时空隙处露出底色形成"色圈+留白"). */
private val TV_CARD_FOCUS_GAP = 3.dp

/** 卡片确认键长按阈值 (KeyDown 连发次数超过此值即视为长按, 弹出收藏菜单). */
private const val TV_LONG_PRESS_CONFIRM_KEY_REPEATS = 2

/** 继续观看卡片底部集数进度条 (样式对齐详情页 FocusEpisodeProgressBar): 条厚. */
private val TV_CARD_PROGRESS_BAR_HEIGHT = 2.5.dp

/** 进度条左右内缩 (避开卡片圆角). */
private val TV_CARD_PROGRESS_BAR_INSET = 10.dp

/** 进度条与卡片底边的空隙. */
private val TV_CARD_PROGRESS_BAR_BOTTOM_GAP = 4.dp

/** 进度条轨道 (未看部分) 的白色不透明度. */
private const val TV_CARD_PROGRESS_TRACK_ALPHA = 0.3f

/** Hero 操作按钮圆角. */
private val TV_HERO_BUTTON_CORNER = 8.dp

/** 操作按钮整体缩放比例 (内边距/图标/字号统一乘此值). 调小让按钮更紧凑. */
private const val TV_HERO_BUTTON_SCALE = 0.9f
