/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.recommendation

import me.him188.ani.app.data.models.subject.CanonicalTagKind
import me.him188.ani.app.data.persistent.database.dao.SubjectCollectionEntity
import me.him188.ani.datasources.api.topic.UnifiedCollectionType
import kotlin.math.ln
import kotlin.math.pow

/**
 * 用户的兴趣画像: 几个最有代表性的兴趣方向, 外加几部"最能代表口味"的种子作品.
 *
 * **完全由本地数据算出, 一个请求都不发** —— `subject_collection` 表里该有的都有 (每部的标签及其
 * 票数、自己的评分、收藏状态、最近改动时间). 所以进探索页时算它是免费的.
 */
class InterestProfile(
    /** 归一化后的兴趣方向, 权重降序, 至多 [MAX_TAGS] 个. */
    val tags: List<WeightedTag>,
    /** 最能代表口味的作品, 权重降序. 给"因为你喜欢《X》"用. */
    val seeds: List<Seed>,
) {
    class WeightedTag(val name: String, val weight: Double) {
        override fun toString(): String = "$name=${(weight * 100).toInt() / 100.0}"
    }

    class Seed(val subjectId: Int, val name: String)

    val isEmpty: Boolean get() = tags.isEmpty() && seeds.isEmpty()

    override fun toString(): String = "InterestProfile(tags=$tags, seeds=${seeds.map { it.subjectId }})"

    companion object {
        val Empty = InterestProfile(emptyList(), emptyList())

        /** 最终留几个兴趣方向. 再多就没有代表性了, 而且每个方向要花一个请求. */
        const val MAX_TAGS = 6
    }
}

/**
 * 从收藏算兴趣画像. 纯函数, 没有副作用, 方便直接测.
 *
 * @param nowMillis 当前时间, 算时间衰减用.
 */
fun computeInterestProfile(
    collections: List<SubjectCollectionEntity>,
    nowMillis: Long,
): InterestProfile {
    if (collections.isEmpty()) return InterestProfile.Empty

    // 每部作品一个权重, 再累加到它的标签上. **不能按"出现次数"简单统计**: 看完并打了高分,
    // 与"想看"点了一下, 说明的东西完全不同.
    val weighted = collections.map { it to weightOf(it, nowMillis) }

    // tag -> 累计权重; tag -> 出现在几部作品里 (算 IDF 用)
    val scores = HashMap<String, Double>()
    val documentFrequency = HashMap<String, Int>()

    for ((entity, weight) in weighted) {
        if (weight == 0.0) continue
        val tags = entity.tags
        val maxCount = tags.maxOfOrNull { it.count } ?: continue
        if (maxCount <= 0) continue

        for (tag in tags) {
            val kind = CanonicalTagKind.matchOrNull(tag.name) ?: continue
            if (kind !in MEANINGFUL_KINDS) continue
            // 条目内可信度: 只有极少数用户打的标签不作数
            val confidence = tag.count.toDouble() / maxCount
            if (confidence < MIN_TAG_CONFIDENCE) continue

            scores[tag.name] = (scores[tag.name] ?: 0.0) + weight * confidence
            documentFrequency[tag.name] = (documentFrequency[tag.name] ?: 0) + 1
        }
    }

    // IDF: 在很多作品里都出现的标签区分度低 (收藏里一半都带"奇幻", 那它说明不了什么)
    val total = collections.size.toDouble()
    val ranked = scores.mapNotNull { (name, score) ->
        if (score <= 0.0) return@mapNotNull null
        val df = documentFrequency[name] ?: return@mapNotNull null
        InterestProfile.WeightedTag(name, score * ln(total / df + 1.0))
    }.sortedByDescending { it.weight }.take(InterestProfile.MAX_TAGS)

    val maxWeight = ranked.firstOrNull()?.weight ?: 0.0
    val normalized = if (maxWeight > 0.0) {
        ranked.map { InterestProfile.WeightedTag(it.name, it.weight / maxWeight) }
    } else {
        emptyList()
    }

    val seeds = weighted.asSequence()
        .filter { (_, weight) -> weight >= SEED_MIN_WEIGHT }
        .sortedByDescending { (_, weight) -> weight }
        .take(MAX_SEEDS)
        .map { (entity, _) ->
            InterestProfile.Seed(entity.subjectId, entity.nameCn.ifEmpty { entity.name })
        }
        .toList()

    return InterestProfile(normalized, seeds)
}

/**
 * 一部作品对口味的说明力.
 *
 * 越近期的行为权重越高 (半衰期 [HALF_LIFE_DAYS] 天): 三年前追的番不该和上周看完的一样重.
 * 负分是"明确不喜欢", 会把对应标签往下压.
 *
 * **没有把追番进度算进去**: 那要连 `episode_collection` 一起查, 而"在看"本身已经是够强的信号了.
 */
private fun weightOf(entity: SubjectCollectionEntity, nowMillis: Long): Double {
    val base = when (entity.collectionType) {
        UnifiedCollectionType.DONE -> when (entity.selfRatingInfo.score) {
            0 -> 1.5 // 看完了没评分: 中等正反馈
            in 8..10 -> 3.0
            in 6..7 -> 1.0
            else -> -2.0 // 看完了给低分 = 明确不喜欢
        }

        UnifiedCollectionType.DOING -> 2.0
        UnifiedCollectionType.WISH -> 0.6
        UnifiedCollectionType.ON_HOLD -> -0.5
        UnifiedCollectionType.DROPPED -> -2.0
        UnifiedCollectionType.NOT_COLLECTED -> return 0.0
    }
    val days = ((nowMillis - entity.lastUpdated).coerceAtLeast(0L)).toDouble() / MILLIS_PER_DAY
    return base * 0.5.pow(days / HALF_LIFE_DAYS)
}

/**
 * 哪几类标签算"兴趣方向".
 *
 * 丢掉的那几类要么过于宽泛 (`Region` 的"日本"、`Category` 的"TV"几乎每部都有, 搜出来的东西
 * 和没筛一样), 要么不是口味 (`Rating`/`Technology`). `Series` (高达/Fate) 不进画像 —— 它是
 * "同一个系列", 拿去搜只会推同系列续作, 那是"相关条目"不是"猜你喜欢".
 */
private val MEANINGFUL_KINDS = setOf(
    CanonicalTagKind.Genre,
    CanonicalTagKind.Emotion,
    CanonicalTagKind.Setting,
    CanonicalTagKind.Audience,
    CanonicalTagKind.Source,
)

/** 标签票数低于条目内最高票数的这个比例就不作数. */
private const val MIN_TAG_CONFIDENCE = 0.15

private const val HALF_LIFE_DAYS = 180.0
private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000.0

/** 能当种子的最低权重: 至少得是"看完给了 6 分以上"或"正在追"那一档. */
private const val SEED_MIN_WEIGHT = 0.8

private const val MAX_SEEDS = 3
