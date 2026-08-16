/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package me.him188.ani.app.videoplayer.media

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.ColorInfo
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.container.NalUnitUtil
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.ForwardingExtractor
import androidx.media3.extractor.ForwardingExtractorOutput
import androidx.media3.extractor.ForwardingTrackOutput
import androidx.media3.extractor.TrackOutput
import androidx.media3.extractor.text.SubtitleParser
import me.him188.ani.utils.logging.info
import me.him188.ani.utils.logging.logger

/*
 * 给容器里没写色彩信息的 HEVC 轨补上 [ColorInfo].
 *
 * ## 为什么需要
 *
 * media3 的 `MatroskaExtractor` **只从容器的 Colour 元素**取色彩信息:
 *
 * ```java
 * // MatroskaExtractor.java:2535 (1.9.0)
 * @Nullable ColorInfo colorInfo = null;
 * if (hasColorInfo) { ... }
 * ```
 *
 * 而大量 mkvmerge 压制的番剧**根本没写那个元素** (实测手上的 WebRip 全都没有), 色彩信息只存在于
 * HEVC 的 SPS VUI 里。于是 `Format.colorInfo == null`, `MediaFormatUtil.maybeSetColorInfo` 什么
 * 都不设, ACodec 拿不到 color aspects 就**不会去调 `native_window_set_buffers_data_space`** ——
 * 视频层的 dataspace 于是保持着上一手留下的、没初始化的内存。
 *
 * 讽刺的是同一个库里 `HevcConfig.parse` 早就把值解出来了 (`HevcConfig.java:144`
 * `colorSpace = spsData.colorSpace`), 只是 `MatroskaExtractor` 没去用它。所以这里做的事就是把
 * 那条断掉的线接上, 用的还是 media3 自己的 SPS 解析器, 不是猜。
 *
 * ## 症状 (2026-08-15/16 Shield 上追了一整晚)
 *
 * 播 10bit HEVC 时, 视频层的 dataspace 是随机垃圾: `0x8` `0x2` `0x424` `0x4f` `0x3140786d`
 * `0x49dc7630` `0x4af74a78` …… 合法的 BT.709 limited 应该是 `0x10c10000`, **一次都没出现过**。
 * 大多数垃圾值的 transfer 位落在无意义的地方, 显示通路当没信息处理, 画面正常 —— 但只要撞上
 * `transfer = 7 (ST2084/PQ)`, 一段 SDR 就会被按 HDR 曲线还原: 暗部压死、高光爆掉、对比度炸开。
 * 概率不低, 一晚上撞了七八次。
 *
 * 排查中被逐个排除的: 取帧缩略图 (设置里关掉照样出)、跨进程解码 (从 mediaserver 挪进应用内照样
 * 出)、切集时的解码器 churn (全新进入播放器也出)、上游 mediamp/media3 升级 (好坏两次的驱动日志
 * 逐行相同, ColorPrimaries/TransferCharacteristics/MatrixCoefficients 全是 1)。
 *
 * ## 挂在哪
 *
 * mediamp 把 `RenderersFactory` 写死了 (`ExoPlayerMediampPlayer` 里直接 `WsolaRenderersFactory`),
 * 没有 `MediaCodecAdapter.Factory` 一类的钩子; 但 libass 那条 pipeline 的 `ExtractorsFactory`
 * 是我们自己的, 所以从 `TrackOutput.format` 这一层改 —— 在 `Format` 交给渲染器**之前**补好。
 *
 * ## 边界
 *
 * - `colorInfo != null` 就不碰: 容器写了的以容器为准。
 * - 只认 `video/hevc`: H.264 的 SPS 布局不同, 需要另一个解析器; 手上没有复现样本, 不顺手一起做。
 * - SPS 里也没有色彩信息时返回 `null`, 保持现状 —— 宁可维持现在这个 (有概率出错的) 行为, 也不
 *   凭分辨率猜一个 BT.709 出来盖掉真实意图。
 */

private val logger = logger("HevcColorInfoRepair")

/**
 * 包一层, 把视频轨里缺失的 [ColorInfo] 从 HEVC SPS 里补回来。见本文件顶部的说明。
 */
internal fun ExtractorsFactory.withHevcColorInfoRepair(): ExtractorsFactory =
    ColorInfoRepairingExtractorsFactory(this)

private class ColorInfoRepairingExtractorsFactory(
    private val delegate: ExtractorsFactory,
) : ExtractorsFactory {
    override fun createExtractors(): Array<Extractor> = delegate.createExtractors().repairing()

    override fun createExtractors(
        uri: Uri,
        responseHeaders: Map<String, List<String>>,
    ): Array<Extractor> = delegate.createExtractors(uri, responseHeaders).repairing()

    // 这两个默认方法必须转发: ProgressiveMediaSource.Factory 会往 ExtractorsFactory 上配字幕解析器,
    // 用接口默认实现的话 (它只 return this) 就把 libass 的那份悄悄丢了.
    override fun setSubtitleParserFactory(
        subtitleParserFactory: SubtitleParser.Factory,
    ): ExtractorsFactory {
        delegate.setSubtitleParserFactory(subtitleParserFactory)
        return this
    }

    override fun experimentalSetTextTrackTranscodingEnabled(enabled: Boolean): ExtractorsFactory {
        delegate.experimentalSetTextTrackTranscodingEnabled(enabled)
        return this
    }

    private fun Array<Extractor>.repairing(): Array<Extractor> =
        Array(size) { ColorInfoRepairingExtractor(this[it]) }
}

private class ColorInfoRepairingExtractor(delegate: Extractor) : ForwardingExtractor(delegate) {
    override fun init(output: ExtractorOutput) {
        super.init(ColorInfoRepairingExtractorOutput(output))
    }
}

private class ColorInfoRepairingExtractorOutput(
    delegate: ExtractorOutput,
) : ForwardingExtractorOutput(delegate) {
    override fun track(id: Int, type: Int): TrackOutput {
        val track = super.track(id, type)
        return if (type == C.TRACK_TYPE_VIDEO) ColorInfoRepairingTrackOutput(track) else track
    }
}

private class ColorInfoRepairingTrackOutput(delegate: TrackOutput) : ForwardingTrackOutput(delegate) {
    override fun format(format: Format) {
        super.format(format.withColorInfoFromHevcSps())
    }
}

private fun Format.withColorInfoFromHevcSps(): Format {
    if (colorInfo != null) return this // 容器写了就以容器为准
    if (sampleMimeType != MimeTypes.VIDEO_H265) return this
    val repaired = parseHevcColorInfo(initializationData) ?: return this
    logger.info {
        "Container had no colour info; recovered from HEVC SPS: " +
                "space=${repaired.colorSpace} range=${repaired.colorRange} " +
                "transfer=${repaired.colorTransfer}"
    }
    return buildUpon().setColorInfo(repaired).build()
}

/**
 * 从 codec-specific data (start code 分隔的 VPS/SPS/PPS) 里找到 SPS 并解出色彩信息.
 *
 * 用的是 media3 自己的 [NalUnitUtil.parseH265SpsNalUnit] —— 与 `HevcConfig.parse` 走的是同一个
 * 解析器, 只是那边解完没往 `Format` 上放.
 */
private fun parseHevcColorInfo(initializationData: List<ByteArray>): ColorInfo? {
    for (csd in initializationData) {
        forEachNalUnit(csd) { start, end ->
            // nal_unit_header: forbidden_zero(1) nal_unit_type(6) ...
            val nalUnitType = (csd[start].toInt() shr 1) and 0x3F
            if (nalUnitType != NalUnitUtil.H265_NAL_UNIT_TYPE_SPS) return@forEachNalUnit
            val sps = runCatching {
                NalUnitUtil.parseH265SpsNalUnit(csd, start, end, null)
            }.getOrElse { e ->
                logger.info { "Failed to parse HEVC SPS for colour info: $e" }
                return@forEachNalUnit
            }
            if (sps.colorSpace == Format.NO_VALUE &&
                sps.colorRange == Format.NO_VALUE &&
                sps.colorTransfer == Format.NO_VALUE
            ) {
                return@forEachNalUnit // SPS 里也没写, 别编
            }
            return ColorInfo.Builder()
                .setColorSpace(sps.colorSpace)
                .setColorRange(sps.colorRange)
                .setColorTransfer(sps.colorTransfer)
                .build()
        }
    }
    return null
}

/**
 * 遍历 start code (`00 00 01` 或 `00 00 00 01`) 分隔的 NAL, 回调收到的是**去掉 start code 之后**
 * 的 `[起, 止)` —— 与 `NalUnitUtil.parseH265SpsNalUnit` 期望的一致.
 */
private inline fun forEachNalUnit(data: ByteArray, block: (start: Int, end: Int) -> Unit) {
    val starts = ArrayList<Int>(4)
    var i = 0
    while (i + 2 < data.size) {
        if (data[i].toInt() == 0 && data[i + 1].toInt() == 0 && data[i + 2].toInt() == 1) {
            starts.add(i + 3)
            i += 3
        } else {
            i++
        }
    }
    for (index in starts.indices) {
        val start = starts[index]
        // 下一个 NAL 的 start code 最多占 4 字节 (00 00 00 01), 从它的负载位置往回退到 start code 之前
        val end = if (index + 1 < starts.size) {
            val nextStartCode = starts[index + 1] - 3
            if (nextStartCode > start && data[nextStartCode - 1].toInt() == 0) nextStartCode - 1 else nextStartCode
        } else {
            data.size
        }
        if (end > start) block(start, end)
    }
}
