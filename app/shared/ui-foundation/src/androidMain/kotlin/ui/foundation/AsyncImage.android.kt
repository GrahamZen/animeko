/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import coil3.ComponentRegistry
import coil3.EventListener
import coil3.Image
import coil3.ImageLoader
import coil3.asImage
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.gif.GifDecoder
import coil3.imageDecoderEnabled
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.SuccessResult
import coil3.serviceLoaderEnabled

actual fun ImageBitmap.asCoilImage(): Image {
    return this.asAndroidBitmap().asImage()
}

/**
 * Android 侧的解码器开关.
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
 *
 * **关掉 [serviceLoaderEnabled]**: `coil-gif` 的 aar 里带着
 * `META-INF/services/coil3.util.DecoderServiceLoaderTarget`, 而 `RealImageLoader` 会自动把它加进
 * registry —— 它给的正是 API>=28 走 `AnimatedImageDecoder` 的工厂, 也就是上面刚关掉的那条
 * `ImageDecoder` 路 (见 [addPlatformDecoders]). 关掉之后所有组件都由我们自己显式注册
 * (`coil-svg` 与 `coil-network-ktor3` 同样带 services 文件, 它们在 `createDefaultImageLoader` 里已经
 * 手动 `add` 过了), 以后新加 coil artifact 时要记得跟着手动注册.
 */
internal actual fun ImageLoader.Builder.configurePlatformDecoders(): ImageLoader.Builder = apply {
    imageDecoderEnabled(false)
    serviceLoaderEnabled(false)
}

/**
 * 动图解码器: 不注册的话 GIF 只出第一帧 (Bangumi 的表情包有不少是动图, 见
 * [me.him188.ani.app.ui.comment.BangumiStickers]).
 *
 * 一律用 `Movie` 版的 [GifDecoder], 不用 API>=28 的 `AnimatedImageDecoder`: 后者与
 * [configurePlatformDecoders] 里刚关掉的 `StaticImageDecoder` 走的是同一个
 * `android.graphics.ImageDecoder` (两者都靠 `toImageDecoderSourceOrNull` 取源), 在会报 'unimplemented'
 * 的那类设备上 GIF 会**整张加载失败** —— 解码器选中就不回退, 连 `BitmapFactory` 的第一帧都出不来,
 * 比不加动图支持更糟.
 *
 * 代价是动图只支持 GIF: 动态 WebP/HEIF 落到 `BitmapFactory`, 显示为静态第一帧.
 */
internal actual fun ComponentRegistry.Builder.addPlatformDecoders() {
    add(GifDecoder.Factory())
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
