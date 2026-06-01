/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.models.episode

import me.him188.ani.app.data.models.UserInfo

enum class EpisodeCommentSource {
    ANI,
    BANGUMI,
}

data class EpisodeComment(
    val stableId: String,
    val source: EpisodeCommentSource,
    val sourceCommentId: String,
    val commentId: String,
    val episodeId: Long,

    /**
     * Timestamp, millis
     */
    val createdAt: Long,
    val content: String,
    val author: UserInfo?,
    val reactions: List<EpisodeCommentReaction> = emptyList(),
    val replies: List<EpisodeComment> = listOf(),
    val canReply: Boolean = false,

    /**
     * 被回复的那一条同层回复的 [sourceCommentId]. 直接回复主楼, 或数据源不提供时为 `null`.
     *
     * 电视上的完整评论弹窗用它显示"回复 @某人". 目前没有数据源填它: Bangumi 的 `relatedID` 原先
     * 由客户端直连 next.bgm.tv 时拿得到, 服务端合并后的 `listEpisodeComments` 不带这个关系
     * (见 `AniEpisodeComment`), 于是那一行不显示. 服务端补上之后在映射处填这里即可.
     */
    val replyToCommentId: String? = null,
)

data class EpisodeCommentReaction(
    val value: String,
    val count: Int,
    val selected: Boolean,
)
