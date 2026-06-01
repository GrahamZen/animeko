/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.network

import androidx.compose.ui.text.intl.Locale
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import me.him188.ani.app.data.models.episode.EpisodeCollectionInfo
import kotlin.time.Clock

/**
 * Compose [Locale] -> TMDB 语言码 (`language-REGION`), 决定 TMDB 本地化字段 (分集简介等) 的语言.
 * 中文无地区时按简体处理 (TMDB 的中文翻译以 zh-CN 为主).
 */
fun Locale.toTmdbLanguage(): String = when {
    region.isNotEmpty() -> "$language-$region"
    language == "zh" -> "zh-CN"
    else -> language
}

/**
 * 把 TMDB 分集数据对齐到 Bangumi 分集, 返回 episodeId -> 分集数据.
 *
 * 播出日期优先 (0/+1/-1 天容差, 深夜档跨日两边常差一天, 实测 DanMachi III:
 * Bangumi 10-02 vs TMDB 10-03), 无日期的老番 (如 1997 剑风传奇) 按集号兜底
 * (byEpisodeNumber 仅 TMDB 单季剧非空), 特别篇按集名精确匹配兜底.
 *
 * 详情页选集缩略图与探索页继续观看 hero 共用此逻辑, 保证同一集两处拿到同一张图.
 */
fun TmdbEpisodeStills.matchToEpisodes(
    episodes: List<EpisodeCollectionInfo>,
): Map<Int, TmdbEpisodeMedia> {
    val result = mutableMapOf<Int, TmdbEpisodeMedia>()

    // 同日多集连播 (如 無職転生Ⅲ 第1+2话一小时首播) 时 TMDB 同日期是多集列表,
    // 匹配要按"这是当日第几集"对位; 先数出每集在其播出日内的序号.
    val sameDateOrdinals = mutableMapOf<Int, Int>()
    run {
        val counts = mutableMapOf<String, Int>()
        for (episode in episodes) {
            val date = episode.episodeInfo.airDate
            if (date.isInvalid) continue
            val key = runCatching { LocalDate(date.year, date.month, date.day) }
                .getOrNull()?.toString() ?: continue
            val ordinal = counts.getOrElse(key) { 0 }
            sameDateOrdinals[episode.episodeId] = ordinal
            counts[key] = ordinal + 1
        }
    }

    for (episode in episodes) {
        val date = episode.episodeInfo.airDate
        val local = if (date.isInvalid) null else runCatching {
            LocalDate(date.year, date.month, date.day)
        }.getOrNull()
        val episodeNumber = episode.episodeInfo.sort.number
            ?.takeIf { it == it.toInt().toFloat() }?.toInt()

        val sameDay = local?.let {
            byAirDate[it.toString()]
                ?: byAirDate[it.plus(1, DateTimeUnit.DAY).toString()]
                ?: byAirDate[it.minus(1, DateTimeUnit.DAY).toString()]
        }
        val byDate = sameDay?.let { list ->
            // 与当日列表按序对位; 两边同日集数不一致时取末位保底
            list.getOrNull(sameDateOrdinals[episode.episodeId] ?: 0) ?: list.lastOrNull()
        }
        // 集号兜底仅限 Bangumi 分集完全没有日期的老番: 有日期却对不上说明匹配到的
        // TMDB 条目本身可疑 (如正传名命中单季外传), 按集号硬凑只会拿到错图.
        // 集名精确一致的 S0 特别篇兜底不受此限 —— 特别篇两边日期记录常有出入
        // (如 救われるラミリス 後編 差 8 天), 而逐字同名是比日期更强的证据.
        val media = byDate
            ?: episodeNumber?.takeIf { local == null }?.let { byEpisodeNumber[it] }
            ?: findSpecialByName(episode.episodeInfo.name, episode.episodeInfo.nameCn)
            ?: continue
        result[episode.episodeId] = media
    }
    return result
}

/**
 * [EpisodeCollectionInfo.episodeInfo] 播出日期的 `YYYY-MM-DD` 形式;
 * 无效日期返回 null. 供 [TmdbImageService.getEpisodeStills] 的 `newestWantedAirDate` 参数用.
 */
fun EpisodeCollectionInfo.airDateStringOrNull(): String? {
    val date = episodeInfo.airDate
    if (date.isInvalid) return null
    return runCatching { LocalDate(date.year, date.month, date.day).toString() }.getOrNull()
}

/**
 * "应当已经播出"的最新一集的日期 (`YYYY-MM-DD`); 全部无日期或均未播出返回 null.
 * 供 [TmdbImageService.getEpisodeStills] 的 `newestWantedAirDate` 参数用:
 * 连载番最后几集的日期在未来, TMDB 不可能有数据, 以"今天"截断, 避免无意义的陈旧重取.
 */
fun List<EpisodeCollectionInfo>.newestAiredDateStringOrNull(): String? {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
    return mapNotNull { it.airDateStringOrNull() }.filter { it <= today }.maxOrNull()
}
