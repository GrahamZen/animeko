/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.richtext

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.him188.ani.app.tools.HtmlColor
import me.him188.ani.app.ui.comment.BangumiCommentSticker
import me.him188.ani.app.ui.comment.BangumiStickers
import me.him188.ani.utils.bbcode.BBCode
import me.him188.ani.utils.bbcode.RichElement
import me.him188.ani.utils.bbcode.RichText
import me.him188.ani.utils.logging.logger
import me.him188.ani.utils.logging.warn
import org.jetbrains.compose.resources.DrawableResource
import kotlin.coroutines.CoroutineContext

@Composable
fun rememberBBCodeRichTextState(
    initialText: String,
    defaultTextSize: TextUnit = LocalTextStyle.current.fontSize,
): BBCodeRichTextState {
    val scope = rememberCoroutineScope()
    return remember(scope) {
        BBCodeRichTextState(initialText, defaultTextSize, scope)
    }
}

@Stable
class BBCodeRichTextState(
    initialText: String,
    defaultTextSize: TextUnit,
    scope: CoroutineScope,
    parseContext: CoroutineContext = Dispatchers.Default
) {
    private val logger = logger<BBCodeRichTextState>()

    private val textFlow = MutableStateFlow(initialText)
    var elements: List<UIRichElement> by mutableStateOf(listOf())
        private set

    init {
        scope.launch {
            textFlow.collectLatest { code ->
                val richText = withContext(parseContext) {
                    try {
                        BBCode.parse(code)
                    } catch (ex: Exception) {
                        logger.warn(ex) { "failed to parse bbcode \"$code\"" }
                        null
                    }
                }
                if (richText != null) {
                    elements = richText.toUIRichElements(defaultTextSize.value)
                }

            }
        }
    }

    fun setText(text: String) {
        textFlow.value = text
    }
}

// TODO: move to BBCodeRichTextState
fun RichText.toUIRichElements(overrideTextSize: Float? = null): List<UIRichElement> = buildList {
    val annotated = mutableListOf<UIRichElement.Annotated>()

    elements.forEach { e ->
        when (e) {
            is RichElement.Text -> annotated.addAll(e.toUIAnnotated(overrideTextSize))

            is RichElement.BangumiSticker -> annotated.add(
                e.toUISticker(overrideTextSize ?: RichTextDefaults.FontSize),
            )

            is RichElement.Kanmoji -> annotated.add(
                e.toUISticker(overrideTextSize ?: RichTextDefaults.FontSize),
            )

            is RichElement.Quote -> {
                if (annotated.isNotEmpty()) {
                    add(UIRichElement.AnnotatedText(annotated.toList()))
                    annotated.clear()
                }
                add(UIRichElement.Quote(e.contents.toUIRichElements()))
            }

            is RichElement.Image -> {
                if (annotated.isNotEmpty()) {
                    add(UIRichElement.AnnotatedText(annotated.toList()))
                    annotated.clear()
                }
                // 贴的常常是图床的网页地址而不是图片直链, 见 normalizeImageUrl
                add(UIRichElement.Image(normalizeImageUrl(e.imageUrl), e.jumpUrl))
            }
        }
    }

    if (annotated.isNotEmpty()) {
        add(UIRichElement.AnnotatedText(annotated.toList()))
        annotated.clear()
    }
}

fun RichText.toUIBriefText(): UIRichElement.AnnotatedText {
    val plainText = StringBuilder()
    val annotated = mutableListOf<UIRichElement.Annotated>()

    fun flushText() {
        if (plainText.isNotEmpty()) {
            annotated.add(UIRichElement.Annotated.Text(plainText.toString(), RichTextDefaults.FontSize))
            plainText.clear()
        }
    }

    elements.forEach { e ->
        when (e) {
            is RichElement.Image -> plainText.append("[图片]")
            is RichElement.Quote -> plainText.append("[引用]")

            // 表情在缩略行里也出图 (与展开后的正文一致), 认不出的代码退化成原文本
            is RichElement.Text -> e.toUIAnnotated(RichTextDefaults.FontSize).forEach { piece ->
                when (piece) {
                    is UIRichElement.Annotated.Text -> plainText.append(piece.content.replace('\n', ' '))
                    is UIRichElement.Annotated.Sticker -> {
                        flushText()
                        annotated.add(piece)
                    }
                }
            }

            is RichElement.Kanmoji -> e.toUISticker(RichTextDefaults.FontSize).let {
                if (it is UIRichElement.Annotated.Sticker) {
                    flushText()
                    annotated.add(it)
                } else {
                    plainText.append(e.id)
                }
            }

            is RichElement.BangumiSticker -> e.toUISticker(RichTextDefaults.FontSize).let {
                if (it is UIRichElement.Annotated.Sticker) {
                    flushText()
                    annotated.add(it)
                } else {
                    plainText.append("(bgm${e.id})")
                }
            }
        }
    }

    flushText()

    return UIRichElement.AnnotatedText(annotated)
}

/**
 * 一段文本 -> 行内元素.
 *
 * 会把文本里的表情代码切出来单独成一枚表情: `(musume_06)` 这类**带下划线**的代码 BBCode 文法没有
 * 对应产生式 (`bgm_sticker` 只认 `(bgm` + 数字), 会被当普通文本吐出来, 所以在这一层补一道扫描.
 * 见 [BangumiStickers.findTokens].
 */
private fun RichElement.Text.toUIAnnotated(overrideTextSize: Float?): List<UIRichElement.Annotated> {
    // 与改动前一致: 纯空白的文本段不产生元素
    if (value.isBlank()) return emptyList()

    fun text(content: String) = UIRichElement.Annotated.Text(
        content = content,
        size = overrideTextSize ?: size.toFloat(),
        color = HtmlColor.parse(color),
        italic = italic,
        underline = underline,
        strikethrough = strikethrough,
        bold = bold,
        mask = mask,
        code = code,
        url = jumpUrl,
    )

    val tokens = BangumiStickers.findTokens(value)
    if (tokens.isEmpty()) return listOf(text(value))

    return buildList {
        var consumed = 0
        tokens.forEach { token ->
            if (token.range.first > consumed) add(text(value.substring(consumed, token.range.first)))
            add(stickerOf(token.value, resource = null, jumpUrl = jumpUrl))
            consumed = token.range.last + 1
        }
        if (consumed < value.length) add(text(value.substring(consumed)))
    }
}

/**
 * `(bgm38)` -> 行内表情. 最早那 125 张随包, 其余按地址现拉.
 * 代码不认识 (Bangumi 又上新了表情包而 [BangumiStickers] 没跟上) 时退化成原文本 `(bgm999)`,
 * 不然 [UIRichElement.Annotated.Sticker] 会渲染成一块看不见的空白, 等于整条表情凭空消失.
 */
private fun RichElement.BangumiSticker.toUISticker(textSize: Float): UIRichElement.Annotated =
    stickerOf("(bgm$id)", resource = BangumiCommentSticker[id], jumpUrl = jumpUrl, textSize = textSize)

/** 颜文字 (`(=A=)` 这类) -> 行内表情. 与 [RichElement.BangumiSticker.toUISticker] 同理. */
private fun RichElement.Kanmoji.toUISticker(textSize: Float): UIRichElement.Annotated =
    stickerOf(id, resource = null, jumpUrl = jumpUrl, textSize = textSize)

/** 表情代码 -> 行内表情; 随包没图、地址也拼不出来的, 退化成原文本. */
private fun stickerOf(
    token: String,
    resource: DrawableResource?,
    jumpUrl: String?,
    textSize: Float = RichTextDefaults.FontSize,
): UIRichElement.Annotated {
    val imageUrl = BangumiStickers.imageUrlOf(token)
    if (resource == null && imageUrl == null) {
        return UIRichElement.Annotated.Text(content = token, size = textSize, url = jumpUrl)
    }
    return UIRichElement.Annotated.Sticker(
        id = token,
        resource = resource,
        imageUrl = imageUrl,
        url = jumpUrl,
    )
}
