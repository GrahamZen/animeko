/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.subject.details.state

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.intl.Locale
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import me.him188.ani.app.data.models.subject.RelatedCharacterInfo
import me.him188.ani.app.data.models.subject.SelfRatingInfo
import me.him188.ani.app.data.models.subject.SubjectCollectionInfo
import me.him188.ani.app.data.models.subject.SubjectInfo
import me.him188.ani.app.data.models.subject.SubjectProgressInfo
import me.him188.ani.app.data.network.BangumiRelatedPeopleService
import me.him188.ani.app.data.network.TmdbImageService
import me.him188.ani.app.data.repository.episode.BangumiCommentRepository
import me.him188.ani.app.data.repository.episode.EpisodeCollectionRepository
import me.him188.ani.app.data.repository.episode.EpisodeProgressRepository
import me.him188.ani.app.data.repository.player.EpisodePlayHistoryRepository
import me.him188.ani.app.data.repository.subject.SetSubjectCollectionTypeOrDeleteUseCase
import me.him188.ani.app.data.repository.subject.SubjectCollectionRepository
import me.him188.ani.app.data.repository.subject.SubjectRelationsRepository
import me.him188.ani.app.data.repository.user.SettingsRepository
import me.him188.ani.app.ui.comment.CommentMapperContext.parseToUIComment
import me.him188.ani.app.ui.comment.CommentState
import me.him188.ani.app.ui.foundation.produceState
import me.him188.ani.app.ui.foundation.stateOf
import me.him188.ani.app.ui.rating.EditableRatingState
import me.him188.ani.app.ui.subject.AiringLabelState
import me.him188.ani.app.ui.subject.SubjectProgressState
import me.him188.ani.app.ui.subject.collection.components.EditableSubjectCollectionTypeState
import me.him188.ani.app.ui.subject.collection.progress.SubjectProgressStateFactory
import me.him188.ani.app.ui.subject.details.updateRating
import me.him188.ani.app.ui.subject.episode.list.EpisodeListUiState
import me.him188.ani.datasources.api.PackedDate
import me.him188.ani.datasources.api.topic.UnifiedCollectionType
import me.him188.ani.datasources.api.topic.isDoneOrDropped
import me.him188.ani.utils.platform.annotations.TestOnly
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Compose [Locale] -> TMDB 语言码 (`language-REGION`), 决定 TMDB 本地化字段 (分集简介等) 的语言.
 * 中文无地区时按简体处理 (TMDB 的中文翻译以 zh-CN 为主).
 */
private fun Locale.toTmdbLanguage(): String = when {
    region.isNotEmpty() -> "$language-$region"
    language == "zh" -> "zh-CN"
    else -> language
}

interface SubjectDetailsStateFactory {
    fun create(subjectInfoFlow: Flow<SubjectInfo>): Flow<SubjectDetailsState>
    fun create(subjectInfo: SubjectInfo): Flow<SubjectDetailsState>

    /**
     * @param placeholder 通常是仅仅包含少量信息的预加载的 subject 信息.
     *        例如从探索页导航到详情页时, subject 名字和封面图时已知的, 可以作为预加载信息以第一时间显示一些东西.
     */
    fun create(
        subjectId: Int,
        placeholder: SubjectInfo? = null
    ): Flow<SubjectDetailsState>

    fun create(subjectCollectionInfo: SubjectCollectionInfo, scope: CoroutineScope): SubjectDetailsState
}

class DefaultSubjectDetailsStateFactory : SubjectDetailsStateFactory, KoinComponent {
    private val settingsRepository: SettingsRepository by inject()
    private val subjectCollectionRepository: SubjectCollectionRepository by inject()
    private val episodeProgressRepository: EpisodeProgressRepository by inject()
    private val episodeCollectionRepository: EpisodeCollectionRepository by inject()
    private val bangumiRelatedPeopleService: BangumiRelatedPeopleService by inject()
    private val subjectRelationsRepository: SubjectRelationsRepository by inject()
    private val bangumiCommentRepository: BangumiCommentRepository by inject()
    private val setSubjectCollectionTypeOrDeleteUseCase: SetSubjectCollectionTypeOrDeleteUseCase by inject()
    private val tmdbImageService: TmdbImageService by inject()
    private val episodePlayHistoryRepository: EpisodePlayHistoryRepository by inject()

    override fun create(
        subjectInfoFlow: Flow<SubjectInfo>
    ): Flow<SubjectDetailsState> = flow {
        coroutineScope {
            val subjectProgressStateFactory = createSubjectProgressStateFactory()


            subjectInfoFlow.transformLatest { subjectInfo ->
                coroutineScope {
                    val subjectCollectionFlow = subjectCollectionRepository.subjectCollectionFlow(subjectInfo.subjectId)
                        .shareIn(this, started = SharingStarted.Eagerly, replay = 1)

                    emit(
                        createImpl(
                            subjectInfo,
                            subjectCollectionFlow,
                            subjectCollectionFlow.map { it.collectionType }.stateIn(this),
                            subjectProgressStateFactory,
                        ),
                    )
                    awaitCancellation()
                }
            }.collect()
            awaitCancellation()
        }
    }

    override fun create(
        subjectInfo: SubjectInfo,
    ): Flow<SubjectDetailsState> = flow {
        coroutineScope {
            val subjectProgressStateFactory = createSubjectProgressStateFactory()
            val subjectCollectionFlow = subjectCollectionRepository.subjectCollectionFlow(subjectInfo.subjectId)
                .shareIn(this, started = SharingStarted.Eagerly, replay = 1)

            emit(
                createImpl(
                    subjectInfo,
                    subjectCollectionFlow,
                    subjectCollectionFlow.map { it.collectionType }.stateIn(this),
                    subjectProgressStateFactory,
                ),
            )
            awaitCancellation()
        }
    }

    override fun create(subjectId: Int, placeholder: SubjectInfo?): Flow<SubjectDetailsState> = flow {
        coroutineScope {
            val subjectProgressStateFactory = createSubjectProgressStateFactory()
            val subjectCollectionInfoFlow = subjectCollectionRepository.subjectCollectionFlow(subjectId)
                .stateIn(this)

            emit(
                createImpl(
                    subjectCollectionInfoFlow.value.subjectInfo,
                    subjectCollectionInfoFlow,
                    subjectCollectionInfoFlow.map { it.collectionType }.stateIn(this),
                    subjectProgressStateFactory,
                ),
            )

            awaitCancellation()
        }
    }

    override fun create(subjectCollectionInfo: SubjectCollectionInfo, scope: CoroutineScope): SubjectDetailsState {
        val subjectProgressStateFactory = createSubjectProgressStateFactory()

        val subjectCollectionInfoFlow = MutableStateFlow(subjectCollectionInfo)
        return scope.createImpl(
            subjectCollectionInfoFlow.value.subjectInfo,
            subjectCollectionInfoFlow,
            MutableStateFlow(subjectCollectionInfo.collectionType),
            subjectProgressStateFactory,
        )
    }

    private fun createSubjectProgressStateFactory() = SubjectProgressStateFactory(
        episodeProgressRepository,
    )

    private fun CoroutineScope.createImpl(
        subjectInfo: SubjectInfo,
        subjectCollectionFlow: SharedFlow<SubjectCollectionInfo>,
        selfCollectionTypeStateFlow: StateFlow<UnifiedCollectionType>,
        subjectProgressStateFactory: SubjectProgressStateFactory
    ): SubjectDetailsState {
        val totalStaffCountState = mutableStateOf<Int?>(null)
        val totalCharactersCountState = mutableStateOf<Int?>(null)

        val subjectId = subjectInfo.subjectId
        val editableSubjectCollectionTypeState = EditableSubjectCollectionTypeState(
            selfCollectionTypeFlow = subjectCollectionFlow
                .map { it.collectionType },
            hasAnyUnwatched = hasAnyUnwatched@{
                val collections = episodeCollectionRepository.subjectEpisodeCollectionInfosFlow(subjectId)
                    .flowOn(Dispatchers.Default).firstOrNull() ?: return@hasAnyUnwatched true

                collections.any { !it.collectionType.isDoneOrDropped() }
            },
            onSetSelfCollectionType = {
                setSubjectCollectionTypeOrDeleteUseCase(subjectId, it)
            },
            onSetAllEpisodesWatched = {
                episodeCollectionRepository.setAllEpisodesWatched(subjectId)
            },
            this,
        )

        val editableRatingState = EditableRatingState(
            ratingInfo = stateOf(subjectInfo.ratingInfo),
            selfRatingInfo = subjectCollectionFlow.map { it.selfRatingInfo }
                .produceState(SelfRatingInfo.Empty, this),
            enableEdit = subjectCollectionFlow
                .map { it.collectionType != UnifiedCollectionType.NOT_COLLECTED }
                .produceState(false, this),
            isCollected = {
                val collection =
                    subjectCollectionFlow.replayCache.firstOrNull() ?: return@EditableRatingState false
                collection.collectionType != UnifiedCollectionType.NOT_COLLECTED
            },
            onRate = { request ->
                subjectCollectionRepository.updateRating(
                    subjectId,
                    request,
                )
            },
            this,
            subjectId,
        )


        val subjectProgressInfoState =
            subjectCollectionFlow.map { info ->
                SubjectProgressInfo.compute(
                    info.subjectInfo, info.episodes, PackedDate.now(),
                    recurrence = info.recurrence,
                )
            }.produceState(null, this)

        val subjectProgressState = subjectProgressStateFactory.run {
            SubjectProgressState(
                subjectProgressInfoState,
            )
        }

        val comments = bangumiCommentRepository.subjectCommentsPager(subjectId)
            .map { page ->
                page.map { it.parseToUIComment() }
            }
            .cachedIn(this)

        val subjectCommentState = CommentState(
            list = comments,
            countState = stateOf(null),
            onSubmitCommentReaction = { _, _, _ -> },
            backgroundScope = this,
        )

//        val relatedPersonsFlow = bangumiRelatedPeopleService.relatedPersonsFlow(subjectId)
//            .onEach {
//                withContext(Dispatchers.Main) { totalStaffCountState.value = it.size }
//            }
//            .stateIn(this, SharingStarted.Eagerly, null)
//
        val loadingState = LoadStates(
            refresh = LoadState.Loading,
            prepend = LoadState.NotLoading(false),
            append = LoadState.NotLoading(false),
        )

//        val relatedCharactersFlow = bangumiRelatedPeopleService.relatedCharactersFlow(subjectId)
//            .onEach {
//                withContext(Dispatchers.Main) { totalCharactersCountState.value = it.size }
//            }
//            .stateIn(this, SharingStarted.Eagerly, null)

        val relatedPersonsFlow = subjectRelationsRepository.subjectRelatedPersonsFlow(subjectId)
            .onEach {
                withContext(Dispatchers.Main) { totalStaffCountState.value = it.size }
            }
            .stateIn(this, SharingStarted.Eagerly, null)

        val relatedCharactersFlow = subjectRelationsRepository.subjectRelatedCharactersFlow(subjectId)
            .onEach {
                withContext(Dispatchers.Main) { totalCharactersCountState.value = it.size }
            }
            .stateIn(this, SharingStarted.Eagerly, null)

        val minuteTicker = flow {
            while (true) {
                emit(Unit)
                delay(1.minutes)
            }
        }

        // TMDB 分集数据 (缩略图 URL + 时长 + 本地化简介) 对齐到 episodeId: 播出日期优先
        // (0/+1/-1 天容差, 深夜档跨日两边常差一天, 实测 DanMachi III: Bangumi 10-02 vs TMDB 10-03),
        // 无日期的老番 (如 1997 剑风传奇) 按集号兜底 (byEpisodeNumber 仅 TMDB 单季剧非空).
        // 简介语言跟随 APP 语言设置, 用户切换语言后 (combine) 自动按新语言重取.
        // Lazily: 仅 TV 详情页收集, 其他平台不发起 TMDB 请求.
        val tmdbLanguageFlow = settingsRepository.uiSettings.flow
            .map { (it.appLanguage ?: Locale.current).toTmdbLanguage() }
            .distinctUntilChanged()
        val tmdbEpisodeMediaFlow = combine(subjectCollectionFlow, tmdbLanguageFlow) { collection, language ->
            val stills = tmdbImageService.getEpisodeStills(subjectId, subjectInfo.name, language)
            val stillUrls = mutableMapOf<Int, String>()
            val runtimes = mutableMapOf<Int, Int>()

            // 同日多集连播 (如 無職転生Ⅲ 第1+2话一小时首播) 时 TMDB 同日期是多集列表,
            // 匹配要按"这是当日第几集"对位; 先数出每集在其播出日内的序号.
            val sameDateOrdinals = mutableMapOf<Int, Int>()
            run {
                val counts = mutableMapOf<String, Int>()
                for (episode in collection.episodes) {
                    val date = episode.episodeInfo.airDate
                    if (date.isInvalid) continue
                    val key = runCatching { LocalDate(date.year, date.month, date.day) }
                        .getOrNull()?.toString() ?: continue
                    val ordinal = counts.getOrElse(key) { 0 }
                    sameDateOrdinals[episode.episodeId] = ordinal
                    counts[key] = ordinal + 1
                }
            }

            val overviews = mutableMapOf<Int, String>()
            for (episode in collection.episodes) {
                val date = episode.episodeInfo.airDate
                val local = if (date.isInvalid) null else runCatching {
                    LocalDate(date.year, date.month, date.day)
                }.getOrNull()
                val episodeNumber = episode.episodeInfo.sort.number
                    ?.takeIf { it == it.toInt().toFloat() }?.toInt()

                val sameDay = local?.let {
                    stills.byAirDate[it.toString()]
                        ?: stills.byAirDate[it.plus(1, DateTimeUnit.DAY).toString()]
                        ?: stills.byAirDate[it.minus(1, DateTimeUnit.DAY).toString()]
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
                    ?: episodeNumber?.takeIf { local == null }?.let { stills.byEpisodeNumber[it] }
                    ?: stills.findSpecialByName(episode.episodeInfo.name, episode.episodeInfo.nameCn)
                    ?: continue
                media.stillUrl?.let { stillUrls[episode.episodeId] = it }
                media.runtimeMinutes?.let { runtimes[episode.episodeId] = it }
                media.overview?.let { overviews[episode.episodeId] = it }
            }
            // 整部简介覆盖: 仅当 APP 语言为中文、Bangumi 简介整段无中文 (全日文原文/纯英文,
            // 自带翻译的混排简介不算) 且 TMDB 有中文简介时生效 —— TMDB 中文简介在前,
            // Bangumi 原文简介附在后面 (原文不丢, 供对照)
            val summaryOverride = stills.showOverview?.takeIf {
                language.startsWith("zh") && summaryHasNoChinese(subjectInfo.summary)
            }?.let { tmdbOverview ->
                val original = subjectInfo.summary.trim()
                if (original.isEmpty()) tmdbOverview else "$tmdbOverview\n\n$original"
            }
            TmdbSubjectMedia(stillUrls.toMap(), runtimes.toMap(), overviews.toMap(), summaryOverride)
        }.shareIn(this, SharingStarted.Lazily, replay = 1)

        val state = SubjectDetailsState(
            subjectId = subjectInfo.subjectId,
            info = subjectInfo,
            selfCollectionTypeState = selfCollectionTypeStateFlow
                .produceState(scope = this),
            airingLabelState = AiringLabelState(
                subjectCollectionFlow.map { it.airingInfo }.produceState(null, scope = this),
                subjectProgressInfoState,
            ),
            staffPager = relatedPersonsFlow
                .map {
                    PagingData.from(
                        it ?: emptyList(),
                        sourceLoadStates = loadingState,
                    )
                }
                .cachedIn(this),
            exposedStaffPager = relatedPersonsFlow
                .filterNotNull()
                .map { list ->
                    list.take(EXPOSED_STAFF_COUNT)
                }
                .map { PagingData.from(it) }
                .cachedIn(this),
            totalStaffCountState = totalStaffCountState,
            charactersPager = relatedCharactersFlow.map {
                PagingData.from(
                    it ?: emptyList(),
                    sourceLoadStates = loadingState,
                )
            }.cachedIn(this),
            totalCharactersCountState = totalCharactersCountState,
            relatedSubjectsPager = bangumiRelatedPeopleService.relatedSubjectsFlow(subjectId)
                .map {
                    PagingData.from(it)
                }
                .cachedIn(this),
            exposedCharactersPager = relatedCharactersFlow
                .filterNotNull()
                .map { it.computeExposed() }
                .map { PagingData.from(it) }
                .cachedIn(this),
            editableSubjectCollectionTypeState = editableSubjectCollectionTypeState,
            editableRatingState = editableRatingState,
            subjectProgressState = subjectProgressState,
            subjectCommentState = subjectCommentState,
            presentation = combine(minuteTicker, subjectCollectionFlow) { _, collection ->
                val now = Clock.System.now()
                SubjectDetailsPresentation(
                    subjectId = subjectId,
                    displayName = collection.subjectInfo.displayName,
                    EpisodeListUiState.from(collection, now),
                )
            }.stateIn(
                this, SharingStarted.WhileSubscribed(5000),
                SubjectDetailsPresentation.Placeholder.copy(subjectId = subjectId),
            ),
            // Lazily: 仅 TV 详情页 Hero 收集此 flow, 其他平台不发起 TMDB 请求.
            // 请求挂住时这个一次性 flow 永远不发射, Hero 会一直按"有图"排版等待 (背景空白),
            // 因此限时重试, 全部失败按无图处理 (回退竖版封面排版).
            tmdbBackdropUrlFlow = flow {
                emit(
                    retryWithTimeoutOrNull {
                        tmdbImageService.getBackdropUrl(subjectId, subjectInfo.name)
                    },
                )
            }.shareIn(this, SharingStarted.Lazily, replay = 1),
            // 分集缩略图/时长/中文简介: TMDB 按播出日期索引 (季/集号与 Bangumi 对不齐,
            // 播出日期是唯一可靠键), 这里对齐到 episodeId. Lazily 同上, 仅 TV 选集卡片收集.
            tmdbEpisodeStillsFlow = tmdbEpisodeMediaFlow.map { it.episodeStills },
            tmdbEpisodeRuntimesFlow = tmdbEpisodeMediaFlow.map { it.episodeRuntimes },
            tmdbEpisodeOverviewsFlow = tmdbEpisodeMediaFlow.map { it.episodeOverviews },
            // Bangumi 简介为全外文时的 TMDB 中文整部简介替换 (无则 null, UI 用原文)
            tmdbSummaryOverrideFlow = tmdbEpisodeMediaFlow.map { it.summaryOverride },
            // 各集播放进度比例, TV 选集卡片底部进度条用. Lazily 同上.
            playProgressFlow = episodePlayHistoryRepository.flow.map { histories ->
                buildMap {
                    for (history in histories) {
                        val duration = history.durationMillis ?: continue
                        if (duration <= 0) continue
                        put(history.episodeId, (history.positionMillis.toFloat() / duration).coerceIn(0f, 1f))
                    }
                }
            }.shareIn(this, SharingStarted.Lazily, replay = 1),
        )
        return state
    }
}

@TestOnly
class TestSubjectDetailsStateFactory : SubjectDetailsStateFactory {
    @TestOnly
    override fun create(subjectInfoFlow: Flow<SubjectInfo>): Flow<SubjectDetailsState> {
        return emptyFlow()
//        return flowOf(
//            SubjectDetailsState(
//                info = SubjectInfo.Empty,
//                selfCollectionTypeState = stateOf(UnifiedCollectionType.WISH),
//                airingLabelState = createTestAiringLabelState(),
//                staffPager = flowOf(PagingData.empty()),
//                totalStaffCountState = stateOf(null),
//                charactersPager = flowOf(PagingData.empty()),
//                totalCharactersCountState = stateOf(null),
//                relatedSubjectsPager = flowOf(PagingData.empty()),
//                episodeListState = EpisodeListState(
//                    subjectId = stateOf(0),
//                    theme = stateOf(EpisodeListProgressTheme.Default),
//                    episodeProgressInfoList = stateOf(emptyList()),
//                    onSetEpisodeWatched = {},
//                    backgroundScope = CoroutineScope(Dispatchers.Default),
//                ),
//                authState = AuthState(
//                    state = produceState(null, CoroutineScope(Dispatchers.Default)),
//                    launchAuthorize = {},
//                    retry = {},
//                    CoroutineScope(Dispatchers.Default),
//                ),
//                editableSubjectCollectionTypeState = EditableSubjectCollectionTypeState(
//                    selfCollectionType = produceState(
//                        UnifiedCollectionType.NOT_COLLECTED,
//                        CoroutineScope(Dispatchers.Default),
//                    ),
//                    hasAnyUnwatched = { true },
//                    onSetSelfCollectionType = {},
//                    onSetAllEpisodesWatched = {},
//                    CoroutineScope(Dispatchers.Default),
//                ),
//                editableRatingState = EditableRatingState(
//                    ratingInfo = stateOf(null),
//                    selfRatingInfo = produceState(SelfRatingInfo.Empty, CoroutineScope(Dispatchers.Default)),
//                    enableEdit = produceState(false, CoroutineScope(Dispatchers.Default)),
//                    isCollected = { false },
//                    onRate = {},
//                    CoroutineScope(Dispatchers.Default),
//                ),
//                subjectProgressState = SubjectProgressState(
//                    subjectProgressInfoState = stateOf(null),
//                    episodeProgressInfoList = produceState(emptyList(), CoroutineScope(Dispatchers.Default)),
//                ),
//                subjectCommentState = CommentState(
//                    sourceVersion = produceState(null, CoroutineScope(Dispatchers.Default)),
//                    list = produceState(emptyList(), CoroutineScope(Dispatchers.Default)),
//                    hasMore = produceState(true, CoroutineScope(Dispatchers.Default)),
//                    onReload = {},
//                    onLoadMore = {},
//                    onSubmitCommentReaction = { _, _, _ -> },
//                    CoroutineScope(Dispatchers.Default),
//                ),
//            ),
//        )
    }

    override fun create(subjectInfo: SubjectInfo): Flow<SubjectDetailsState> {
        return emptyFlow()
    }

    override fun create(subjectId: Int, placeholder: SubjectInfo?): Flow<SubjectDetailsState> {
        return emptyFlow()
    }

    override fun create(subjectCollectionInfo: SubjectCollectionInfo, scope: CoroutineScope): SubjectDetailsState {
        throw UnsupportedOperationException()
    }

}

/**
 * 执行 [block], 单次最长等待 [timeout]; 超时或失败则重试, 共尝试 [attempts] 次, 全部失败返回 null.
 * 超时的尝试立即重发, 立刻抛错的尝试等 1 秒再试 (避免密集重试). 外层取消正常传播.
 */
private suspend fun <T> retryWithTimeoutOrNull(
    attempts: Int = 5,
    timeout: Duration = 5.seconds,
    block: suspend () -> T,
): T? {
    repeat(attempts) {
        try {
            return withTimeout(timeout) { block() }
        } catch (e: TimeoutCancellationException) {
            // 超时: 立即重试
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            delay(1.seconds)
        }
    }
    return null
}

/** 角色区块露出数 (Figma 定稿 1515:336: 8 个). */
private const val EXPOSED_CHARACTERS_COUNT = 8

/** 制作人员露出数 (Figma 定稿三栏右栏卡: 10 个职位; 双栏/手机由 UI 层截取前 6). */
private const val EXPOSED_STAFF_COUNT = 10

private fun List<RelatedCharacterInfo>.computeExposed(): List<RelatedCharacterInfo> {
    // 主角优先; 主角不足 4 个时按原始顺序补足 (含配角).
    val mains = filter { it.isMainCharacter() }
    return if (mains.size >= 4) {
        mains.take(EXPOSED_CHARACTERS_COUNT)
    } else {
        take(EXPOSED_CHARACTERS_COUNT)
    }
}

/** TMDB 整合数据 (分集图/时长/简介 + 整部简介替换), 一次抓取共享给多个 flow. */
private class TmdbSubjectMedia(
    val episodeStills: Map<Int, String>,
    val episodeRuntimes: Map<Int, Int>,
    val episodeOverviews: Map<Int, String>,
    /** Bangumi 简介整段无中文时: TMDB 中文整部简介 + 原文附后; 不需要覆盖或 TMDB 无翻译时为 null. */
    val summaryOverride: String?,
)

/**
 * Bangumi 简介是否"整段没有中文内容" (全日文原文 / 纯英文等) —— 此时才值得用 TMDB 中文简介整段替换.
 *
 * 逐行判定: 一行含 CJK 汉字且假名占比很低 (< 20%, 容忍引用日文原名的少量假名) 即视为中文行,
 * 只要有任何中文行 (如 Bangumi 简介自带翻译的中日混排) 就返回 false 不替换.
 * 纯汉字的日文行会被误判为中文行, 但后果只是保守不替换, 可接受.
 */
private fun summaryHasNoChinese(summary: String): Boolean {
    var sawContent = false
    for (line in summary.lineSequence()) {
        val text = line.trim()
        if (text.isEmpty()) continue
        sawContent = true
        var han = 0
        var kana = 0
        for (c in text) {
            when (c) {
                in '一'..'鿿' -> han++
                in '぀'..'ヿ' -> kana++
            }
        }
        if (han > 0 && kana < han * 0.2f) return false
    }
    return sawContent
}
