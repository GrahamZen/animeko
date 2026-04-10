/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.domain.player.extension

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import me.him188.ani.app.data.repository.player.EpisodePlayHistoryRepository
import me.him188.ani.app.domain.episode.EpisodeFetchSelectPlayState
import me.him188.ani.app.domain.episode.EpisodeSession
import me.him188.ani.app.domain.episode.UnsafeEpisodeSessionApi
import me.him188.ani.utils.logging.info
import me.him188.ani.utils.logging.logger
import org.koin.core.Koin
import org.openani.mediamp.PlaybackState

/**
 * 记忆播放进度.
 *
 * 在以下情况时保存播放进度:
 * - 切换数据源
 * - 暂停
 * - 播放完成
 */
class RememberPlayProgressExtension(
    private val context: PlayerExtensionContext,
    koin: Koin,
) : PlayerExtension(name = "SaveProgressExtension") {
    private val playProgressRepository: EpisodePlayHistoryRepository by koin.inject()

    override fun onStart(episodeSession: EpisodeSession, backgroundTaskScope: ExtensionBackgroundTaskScope) {
        val mediaLoaded = CompletableDeferred<Unit>()
        backgroundTaskScope.launch("MediaLoadedListener") {
            context.subscribeEvents<EpisodeFetchSelectPlayState.MediaLoadedEvent>().collectLatest { event ->
                if (event.episodeId == episodeSession.episodeId && mediaLoaded.isActive) {
                    mediaLoaded.complete(Unit)
                }
            }
        }

        backgroundTaskScope.launch("MediaSelectorListener") {
            mediaLoaded.await() // 播放器开始播放了再跑这个 extension
            episodeSession.fetchSelectFlow.collectLatest inner@{ fetchSelect ->
                if (fetchSelect == null) return@inner

                fetchSelect.mediaSelector.events.onBeforeSelect.collect {
                    // 切换 数据源 前保存播放进度
                    savePlayProgressOrRemove(episodeSession.episodeId)
                }
            }
        }

        backgroundTaskScope.launch("PlaybackStateListener") {
            val player = context.player
            // 每次加载新 media 后恢复一次播放进度。
            // mediamp 0.1.6 的 ExoPlayer 会在每次 seek rebuffer 完成后都发出 READY，
            // 而旧版只在 onMediaItemTransition 时发出一次 READY。
            // 如果不限制只恢复一次，会导致循环 seek（READY → seekTo → rebuffer → READY → seekTo ...）。
            // 使用 collectLatest 监听 mediaData 变化，每次新 media 加载后重置恢复状态。
            player.mediaData.collectLatest { mediaData ->
                if (mediaData == null) return@collectLatest
                var positionRestored = false
                player.playbackState.collect { playbackState ->
                    when (playbackState) {
                        // 加载播放进度（仅在新 media 加载后的首次 READY 时执行）
                        PlaybackState.READY -> {
                            if (positionRestored) return@collect
                            positionRestored = true

                            val positionMillis =
                                playProgressRepository.getPositionMillisByEpisodeId(episodeSession.episodeId)
                            if (positionMillis == null) {
                                logger.info { "Did not find saved position" }
                            } else {
                                logger.info { "Loaded saved position: $positionMillis, waiting for video properties" }
                                player.mediaProperties.filter { it != null && it.durationMillis > 0L }.firstOrNull()
                                logger.info { "Loaded saved position: $positionMillis, video properties ready, seeking" }
                                withContext(Dispatchers.Main + NonCancellable) { // android must call in main thread
                                    player.seekTo(positionMillis)
                                }
                            }
                        }

                        PlaybackState.PAUSED -> {
                            mediaLoaded.await() // 播放器开始播放了一次之后再保存状态
                            savePlayProgressOrRemove(episodeSession.episodeId)
                        }

                        PlaybackState.FINISHED -> {
                            mediaLoaded.await() // 播放器开始播放了一次之后再保存状态
                            savePlayProgressOrRemove(episodeSession.episodeId)
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    @OptIn(UnsafeEpisodeSessionApi::class)
    override suspend fun onBeforeSwitchEpisode(newEpisodeId: Int) {
        savePlayProgressOrRemove(context.getCurrentEpisodeId())
    }

    @OptIn(UnsafeEpisodeSessionApi::class)
    override suspend fun onClose() {
        savePlayProgressOrRemove(context.getCurrentEpisodeId())
    }

    private suspend fun savePlayProgressOrRemove(
        episodeId: Int
    ) {
        val player = context.player
        val playbackState = player.playbackState.value
        val videoDurationMillis = player.mediaProperties.value?.durationMillis

        if (videoDurationMillis == null || videoDurationMillis <= 0L) {
            return
        }

        when (playbackState) {
            PlaybackState.DESTROYED,
            PlaybackState.CREATED,
            PlaybackState.READY,
            PlaybackState.ERROR -> return

            PlaybackState.FINISHED,
            PlaybackState.PAUSED,
            PlaybackState.PLAYING,
            PlaybackState.PAUSED_BUFFERING -> {
                val currentPositionMillis = withContext(Dispatchers.Main.immediate) {
                    try {
                        player.getCurrentPositionMillis()
                    } catch (e: Error) {
                        // Caused by: java.lang.Error: Invalid memory access
                        // https://github.com/open-ani/animeko/issues/1787
                        0L
                    }
                }

                if (currentPositionMillis <= 0L) {
                    return
                }

                if (videoDurationMillis - currentPositionMillis < 5000 || currentPositionMillis > videoDurationMillis) {
                    playProgressRepository.remove(episodeId)
                } else {
                    playProgressRepository.saveOrUpdate(episodeId, currentPositionMillis)
                }
                return
            }
        }
    }

    companion object : EpisodePlayerExtensionFactory<RememberPlayProgressExtension> {
        override fun create(context: PlayerExtensionContext, koin: Koin): RememberPlayProgressExtension =
            RememberPlayProgressExtension(context, koin)

        private val logger = logger<RememberPlayProgressExtension>()
    }
}
