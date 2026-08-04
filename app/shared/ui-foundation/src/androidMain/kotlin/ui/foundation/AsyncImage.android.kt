/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation

import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import coil3.EventListener
import coil3.Image
import coil3.ImageLoader
import coil3.asImage
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.imageDecoderEnabled
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.SuccessResult

actual fun ImageBitmap.asCoilImage(): Image {
    return this.asAndroidBitmap().asImage()
}

/**
 * Android 侧的解码器设置.
 *
 * **动图**: 加上 [AnimatedImageDecoder] 才会动 —— coil 的动图解码器在单独的 artifact (`coil-gif`) 里,
 * 不注册的话 GIF 只出第一帧 (Bangumi 的表情包有不少是动图, 见
 * [me.him188.ani.app.ui.comment.BangumiStickers]). 它注册在 coil 自带的解码器之前, 会先接走动图.
 *
 * **关掉 [imageDecoderEnabled]**: coil 在 API>=29 上默认把 `StaticImageDecoder`
 * (基于系统的 `android.graphics.ImageDecoder`) 排在 `BitmapFactoryDecoder` 之前, 而它在部分设备上会对
 * 完全正常的图报
 * `ImageDecoder$DecodeException: Failed to create image decoder with message 'unimplemented'`
 * —— 实测 Shield 上番剧封面 (`static.myani.org/.../large`) 与评论里贴的图都中招, 表现是占位撑开后
 * 又塌掉、图始终不出来. 解码器一旦被选中就不会再回退到下一个 (只有 `create` 返回 null 才会),
 * 所以只能不让它上场, 统一走久经考验的 `BitmapFactory`.
 *
 * 代价是失去 `ImageDecoder` 独有的格式 (API 31+ 的 AVIF); Bangumi/常见图床都不发这些,
 * 换不出图的 bug 是划得来的.
 */
internal actual fun ImageLoader.Builder.configurePlatformDecoders(): ImageLoader.Builder = apply {
    components {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            add(AnimatedImageDecoder.Factory())
        } else {
            add(GifDecoder.Factory())
        }
    }
    imageDecoderEnabled(false)
}

internal actual fun imageLoadIssueEventListenerFactory(): EventListener.Factory =
    EventListener.Factory { ImageLoadIssueEventListener() }

private class ImageLoadIssueEventListener : EventListener() {
    private val tracker = ImageLoadIssueTracker()

    override fun fetchStart(request: ImageRequest, fetcher: Fetcher, options: Options) {
        tracker.fetchStart()
    }

    override fun fetchEnd(
        request: ImageRequest,
        fetcher: Fetcher,
        options: Options,
        result: FetchResult?,
    ) {
        tracker.fetchEnd()
    }

    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
        tracker.success(request, result)
    }

    override fun onError(request: ImageRequest, result: ErrorResult) {
        tracker.error(request, result)
    }
}
