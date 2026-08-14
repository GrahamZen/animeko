/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation.tv

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.text.intl.Locale
import kotlinx.coroutines.flow.first
import me.him188.ani.app.data.models.episode.EpisodeCollectionInfo
import me.him188.ani.app.data.models.subject.SubjectInfo
import me.him188.ani.app.data.network.BangumiSummaryService
import me.him188.ani.app.data.network.TmdbImageService
import me.him188.ani.app.data.network.matchToEpisodes
import me.him188.ani.app.data.network.newestAiredDateStringOrNull
import me.him188.ani.app.data.network.tmdbStillHeroSizeUrl
import me.him188.ani.app.data.network.toTmdbLanguage
import me.him188.ani.app.data.repository.user.SettingsRepository

/**
 * "继续观看"下一集的 TMDB 媒体: 单集剧照 + 单集简介. 字段为 null = 查过确认没有.
 *
 * 存的是**原图档 URL**, 显示时才按设置降档 (见 [tvHeroBackdropUrl]) —— 存降档结果的话用户
 * 打开"完整视觉效果"得清缓存才生效.
 */
@Immutable
data class TvNextEpisodeMedia(
    val episodeId: Int,
    val stillUrl: String?,
    val overview: String?,
)

/**
 * **TV 各页 hero 附属数据的进程级缓存** (下一集剧照 + 简介兜底).
 *
 * **backdrop 不在这里**: 整部 backdrop 的进程级热缓存只有服务层那一张
 * ([TmdbImageService.peekBackdropUrl], 快照可观察), 页面层不再养第二份 —— 曾经两层并存时
 * "已确认无图"一边编码成 `""` 一边编码成 `null`, 容量策略也各不相同.
 *
 * ## 为什么是进程级而不是 `remember`
 *
 * 导航离开时页面退出组合, `remember` 的解析结果一并销毁; 返回本页要从头再解析一遍
 * (读 DataStore 缓存 -> 出 URL -> Coil 取图), 约 300ms. 而背景图外面套着一层 `Crossfade`
 * (换聚焦条目时用的), URL 迟到就意味着**这层 crossfade 在页面入场动画中途才起步** ——
 * 屏幕上同时跑着两条互不相干的透明度曲线, 相乘出来的观感就是"alpha 走着走着突然跳一档".
 *
 * 真机逐帧实测 (2026-08-12, 从详情页返回探索页, 60fps 录屏): 页面自身的入场淡入在第 219 帧
 * 就已跑完 (页面其余内容此后完全静止), 背景图那一层却在第 218 帧只有最终亮度的 **37%**、
 * 下一帧跳到 **81%**、再花 150ms 才爬到 100%; 中途整屏平均亮度一度掉到起始值的 43%.
 * 这就是那下"闪"与"折线感"的来源, 与文字/排版无关.
 *
 * 提到进程级之后返回时首帧就有 URL, `Crossfade` 的初值即终值 (初值不触发动画), 背景图与页面
 * 其余部分共用同一条入场淡入曲线.
 *
 * ## 为什么四个页面共用一张表
 *
 * 探索页、追番页、搜索页、时间表原本各自 `remember { mutableStateMapOf() }` 一份, 于是
 * **同一部作品换个页面进详情页, 首帧观感就不一样**: 从预取过的那页进有图, 从没预取过的那页
 * 进要等一次网络. 现在四页写同一张表, 谁先聚焦过, 其余三页与详情页都直接命中.
 *
 * ## 容量
 *
 * 超过 [MAX_ENTRIES] 时**按写入顺序淘汰最老的一批**, 不整表清空 —— 清空会让长会话里早就
 * 解析过的条目成批退化回"先空着再淡入". 写入都发生在页面协程 (主线程), 簿记不需要加锁.
 */
object TvHeroMediaCache {
    private const val MAX_ENTRIES = 300

    /** 一次淘汰的条数 (均摊开销, 不必每次写入都动表). */
    private const val EVICT_BATCH = 50

    /** subjectId -> "继续观看"下一集的 TMDB 剧照与简介. */
    val nextEpisodeMedia: SnapshotStateMap<Int, TvNextEpisodeMedia> = mutableStateMapOf()
    private val nextEpisodeOrder = ArrayDeque<Int>()

    /**
     * subjectId -> bgm.tv 简介兜底 (Ani 服务器部分条目 summary 为空, 直连 bgm.tv 补; "" = 也没有).
     */
    val summaryFallbacks: SnapshotStateMap<Int, String> = mutableStateMapOf()
    private val summaryOrder = ArrayDeque<Int>()

    internal fun putNextEpisodeMedia(subjectId: Int, media: TvNextEpisodeMedia) =
        putEvicting(nextEpisodeMedia, nextEpisodeOrder, subjectId, media)

    internal fun putSummaryFallback(subjectId: Int, summary: String) =
        putEvicting(summaryFallbacks, summaryOrder, subjectId, summary)

    private fun <V> putEvicting(
        map: SnapshotStateMap<Int, V>,
        order: ArrayDeque<Int>,
        key: Int,
        value: V,
    ) {
        if (key !in map) {
            order.addLast(key)
            if (order.size > MAX_ENTRIES) {
                repeat(EVICT_BATCH) { order.removeFirstOrNull()?.let(map::remove) }
            }
        }
        map[key] = value
    }
}

/**
 * **整部 backdrop 的预取**: 已解析过就直接返回, 否则解析一次 —— 结果自动落进服务层热表
 * ([TmdbImageService.peekBackdropUrl] 同步可读且快照可观察), 页面不必再自己记.
 *
 * 与详情页 Hero 同源, 所以这同时是**详情页的预取** —— 进详情页的门控条件就是问它有没有值
 * (见各页 `navigateToSubject`), 拿到剧照也不能跳过这一步.
 *
 * 正缓存永久有效, 每个条目全生命周期只会真的请求一次. **请求失败不写缓存**, 下次聚焦重试 ——
 * 不把瞬时断网当成"确认没有".
 *
 * @param originalName 必须传**原名 (日文)**: 中文译名在 TMDB 上命中率低, 而失败会写持久负缓存.
 * @param activeAsOfDate 该条目最新已播出集的日期. 新番刚播时 TMDB 往往还没有 backdrop,
 *   负缓存据此限期失效. 拿不到就传 null.
 */
suspend fun TmdbImageService.prefetchTvBackdrop(
    subjectId: Int,
    originalName: String,
    activeAsOfDate: String? = null,
) {
    if (peekBackdropResolved(subjectId)) return
    runCatching { getBackdropUrl(subjectId, originalName, activeAsOfDate = activeAsOfDate) }
}

/**
 * **"下一集"单集剧照的预取**: 观看中的条目用它当 hero 背景, 直观提示播放进度节点.
 *
 * 连载番的永久缓存可能不含新播集, 传已播出最新集日期触发陈旧重取 (服务层闸门限频).
 *
 * 已经存着同一集的结果就跳过 —— 但看完一集后 `nextEpisodeId` 会变, 那时要重取.
 */
suspend fun TmdbImageService.prefetchTvNextEpisodeMedia(
    subjectId: Int,
    subjectInfo: SubjectInfo,
    episodes: List<EpisodeCollectionInfo>,
    nextEpisodeId: Int,
    settingsRepository: SettingsRepository,
) {
    if (TvHeroMediaCache.nextEpisodeMedia[subjectId]?.episodeId == nextEpisodeId) return
    runCatching {
        val language = (settingsRepository.uiSettings.flow.first().appLanguage ?: Locale.current)
            .toTmdbLanguage()
        val stills = getEpisodeStills(
            subjectId, subjectInfo.name, language,
            newestWantedAirDate = episodes.newestAiredDateStringOrNull(),
        )
        stills.matchToEpisodes(episodes)[nextEpisodeId]
    }.onSuccess { media ->
        TvHeroMediaCache.putNextEpisodeMedia(
            subjectId,
            TvNextEpisodeMedia(nextEpisodeId, media?.stillUrl, media?.overview),
        )
    }
}

/**
 * **简介兜底**: Ani 服务器部分条目 summary 为空, 直连 bgm.tv 补 (仅替代不合并).
 *
 * 网络错误不写缓存 (getSummary 抛出): 下次聚焦该条目重试, 不把瞬时断网当"确认没有".
 */
suspend fun BangumiSummaryService.prefetchTvSummaryFallback(subjectId: Int) {
    if (subjectId in TvHeroMediaCache.summaryFallbacks) return
    runCatching { getSummary(subjectId) }
        .onSuccess { TvHeroMediaCache.putSummaryFallback(subjectId, it.orEmpty()) }
}

/**
 * **hero 背景图的统一取值**: 单集剧照优先 (只有"继续观看/在看"的条目才有), 缺失回退整部
 * backdrop —— 后者直接读服务层热表 ([TmdbImageService.peekBackdropUrl], 快照可观察,
 * 预取落表即重组).
 *
 * 剧照按设置降档: 默认 w1280. 原图偶有 4K 级, 解码 8-33MB 是低端盒子每次换卡的重锤,
 * 而铺满后经渐隐压暗在 10-foot 距离不可辨. backdrop 那路服务层已是 w1280 档.
 *
 * 两级都没有时再回退竖版封面 ([coverUrl]), 与详情页同一套 —— 见那边 `heroBackdropUrl` 的
 * 说明: `ContentScale.Crop` 默认居中, 取的是海报中间那条横带; 封面用 Bangumi 的 l 档
 * (实测 1400~2700 px 宽), 压着 scrim 与底缘渐隐, 4K 面板上放大看不出来.
 */
fun TmdbImageService.tvHeroBackdropUrl(
    subjectId: Int?,
    fullVisualEffects: Boolean,
    /** true 时优先用"下一集"剧照 (继续观看行 / 在看的条目). */
    preferNextEpisodeStill: Boolean = false,
    /**
     * 前两级都没有时的回退: 该条目的竖版封面. null/空 = 不回退 (背景留空).
     *
     * **只在已确认没有横版图之后才用**: 解析途中就顶上去的话, 会先闪一下封面再被 TMDB 图
     * 换掉 —— 详情页踩过这个, 那边同样要等 `backdropResolved`.
     */
    coverUrl: String? = null,
): String? {
    if (subjectId == null) return null
    val stillEntry = if (preferNextEpisodeStill) TvHeroMediaCache.nextEpisodeMedia[subjectId] else null
    stillEntry?.stillUrl?.let { return tmdbStillHeroSizeUrl(it, fullVisualEffects) }
    peekBackdropUrl(subjectId)?.let { return it }
    // 剧照那一路也得先有结论: 只查过 backdrop 就回退, 剧照随后到达还是会闪一下
    val stillResolved = !preferNextEpisodeStill || stillEntry != null
    return coverUrl?.takeIf { it.isNotBlank() && stillResolved && peekBackdropResolved(subjectId) }
}

/**
 * 已经能拿到 hero 背景图 (或已确认没有) —— 进详情页的门控条件.
 * "已确认无图"也算备齐: 那种条目等下去也不会有图.
 */
fun TmdbImageService.tvHeroBackdropReady(subjectId: Int): Boolean = peekBackdropResolved(subjectId)
