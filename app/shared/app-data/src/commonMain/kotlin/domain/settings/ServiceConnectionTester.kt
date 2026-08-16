/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.domain.settings

import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import me.him188.ani.app.data.network.TmdbImageService
import me.him188.ani.app.domain.settings.ServiceConnectionTester.Service
import me.him188.ani.client.apis.TrendsAniApi
import me.him188.ani.datasources.api.source.ConnectionStatus
import me.him188.ani.datasources.bangumi.BangumiClient
import me.him188.ani.utils.coroutines.SingleTaskExecutor
import me.him188.ani.utils.ktor.ApiInvoker
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.measureTimedValue

/**
 * Orchestrates the concurrent testing of multiple [Service] instances.
 *
 * Each [Service] is tested asynchronously when [testAll] is called. The state of each service
 * transitions through [TestState] according to the outcome of its [Service.test] function.
 *
 * - If [testAll] is called again while a previous test run is still in progress,
 *   the existing tasks are canceled and set to [TestState.Idle], and new tasks begin.
 * - If the caller's coroutine (that invokes [testAll]) is canceled, all testing coroutines
 *   are also canceled, and their states revert to [TestState.Idle].
 * - [stopAll] can be invoked manually to cancel any ongoing tests and reset all states to [TestState.Idle].
 *
 * This class is **thread-safe** and can be called from multiple coroutines/threads concurrently.
 *
 * @param defaultDispatcher coroutine dispatcher to run the tests ([Service.test]) and results aggregation.
 *
 * @see ServiceConnectionTesters.createDefault
 */
class ServiceConnectionTester(
    services: List<Service>,
    private val defaultDispatcher: CoroutineContext = Dispatchers.Default,
) {
    private val services = services.map { ServiceImpl(it) }

    /**
     * A [Flow] of [Results], which contains the current [TestState] of all [Service]s being tested.
     */
    val results: Flow<Results> =
        combine(this.services.map { service -> service.state.map { service.service to it } }) { states ->
            Results(states.toMap(LinkedHashMap())) // retain order
        }.shareIn(
            CoroutineScope(defaultDispatcher), // note: we can't use backgroundScope here because backgroundScope may have a Job, which is not accepted by shareIn.
            started = SharingStarted.WhileSubscribed(), replay = 0,
        )

    private val singleTaskExecutor = SingleTaskExecutor(defaultDispatcher)

    /**
     * Start testing all services and suspend until all services are tested.
     *
     * Lifecycle of the testing task is bounded by this function.
     * That is, is this function is cancelled, all testing coroutines are also cancelled.
     * Calling this function the second time will cancel the previous call.
     */
    suspend fun testAll() {
        singleTaskExecutor.invoke {
            for (service in services) {
                launch {
                    service.test()
                }
            }
        }
    }

    /**
     * Stop all testing.
     *
     * This cancels all testing coroutines and results running services' states to [TestState.Idle],
     * but does not clear the completed states.
     */
    fun stopAll() {
        singleTaskExecutor.cancelCurrent()
    }

    class Service(
        /**
         * 给调用方识别的 ID. [ServiceConnectionTester] 不会使用此 ID.
         */
        val id: String,
        /**
         * Test if this service is available.
         *
         * This function is not allowed to throw exceptions, otherwise it will become [TestState.Error] and is considered a bug.
         */
        val test: suspend () -> Boolean,
    )

    sealed class TestState {
        // also initial state
        data object Idle : TestState()

        data object Testing : TestState()
        data class Success(
            val time: Duration,
        ) : TestState()

        /**
         * Indicates a normal failure, e.g., HTTP status code is not 200.
         */
        data object Failed : TestState()

        /**
         * Indicates an unexpected error, e.g., an exception is thrown.
         * This should be considered a bug.
         */
        data class Error(
            val e: Throwable,
        ) : TestState()
    }

    class Results internal constructor(
        internal val states: Map<Service, TestState>,
    ) {
        val idToStateMap: Map<String, TestState> by lazy { states.mapKeys { it.key.id } }

        fun findStateById(id: String): TestState? = states.keys.find { it.id == id }?.let { states[it] }

        fun anyFailed() = states.values.any { it is TestState.Failed }
        fun allCompleted() = states.values.all {
            when (it) {
                is TestState.Error -> true
                TestState.Failed -> true
                TestState.Idle -> false
                is TestState.Success -> true
                TestState.Testing -> false
            }
        }
    }

    private class ServiceImpl(
        val service: Service,
    ) {
        private val _state: MutableStateFlow<TestState> = MutableStateFlow(TestState.Idle)
        val state: StateFlow<TestState> = _state.asStateFlow()
        private val lock = Mutex()

        /**
         * Test the service.
         *
         * This function must be called by only one coroutine at a time, otherwise it throws.
         *
         * If the coroutine is cancelled, the state is re-set to [TestState.Idle] and the [CancellationException] is propagated.
         */
        suspend fun test() {
            // Note that we set `owner=this` (which is always the same), 
            // so that the lock basically ensures the function is always called by a single coroutine at a time.
            // This is a strong assertion to ensure the `testAll` algorithm works correctly.
            lock.withLock(owner = this) {
                _state.value = TestState.Testing
                try {
                    val (res, t) = measureTimedValue { service.test() }
                    _state.value = if (res) TestState.Success(t) else TestState.Failed
                } catch (e: CancellationException) {
                    _state.value = TestState.Idle
                    throw e
                } catch (e: Throwable) {
                    _state.value = TestState.Error(e)
                }
            }
        }

        fun resetToIdle() {
            _state.value = TestState.Idle
        }
    }
}


/**
 * 给单项探测封顶, 超时即判失败.
 *
 * 探测请求多数没覆盖超时, 吃的是全局 30 秒连接超时, 而 IPv4 + IPv6 各试一次就是 60 秒 ——
 * `api.bgm.tv` 在墙内被 DNS 投毒时, 设置页那一行要整整转一分钟才变红叉 (issue #7 报告者
 * 实测), 这期间用户只看到一个转圈, 分不清是在等还是已经卡死.
 *
 * **必须用 [withTimeoutOrNull] 而不是 `withTimeout`**: 后者抛的是 `CancellationException`
 * 的子类, 会被 [ServiceConnectionTester.Service] 的取消分支当成"用户主动取消"而置回
 * `TestState.Idle` —— 那样这一行会永远停在转圈上, 比现在还糟.
 */
private suspend fun withTestTimeout(
    timeoutMillis: Long = ServiceConnectionTesters.DEFAULT_TEST_TIMEOUT_MILLIS,
    block: suspend () -> Boolean,
): Boolean = withTimeoutOrNull(timeoutMillis) { block() } ?: false

object ServiceConnectionTesters {
    const val ID_BANGUMI = "BANGUMI"
    const val ID_BANGUMI_NEXT = "BANGUMI_NEXT"
    const val ID_ANI = "ANI"
    const val ID_TMDB = "TMDB"

    /**
     * 图片 CDN 单独一项: 与接口是两个域名, 在墙内各自独立被墙且方向常常相反
     * (接口不通、图床却通). 合成一项的话用户分不清该不该挂代理, 见 [TmdbImageService].
     */
    const val ID_TMDB_IMAGE = "TMDB_IMAGE"

    /** 单项探测的时间上限, 见 [withTestTimeout]. 能连通的服务握手远用不到 5 秒. */
    internal const val DEFAULT_TEST_TIMEOUT_MILLIS = 5_000L

    /** TMDB 那项内部串了两三个请求, 单独给更宽的上限, 见调用处. */
    internal const val TMDB_TEST_TIMEOUT_MILLIS = 15_000L

    val DefaultServiceIds = setOf(ID_BANGUMI, ID_BANGUMI_NEXT, ID_ANI, ID_TMDB, ID_TMDB_IMAGE)

    fun createDefault(
        bangumiClient: BangumiClient,
        aniClient: ApiInvoker<TrendsAniApi>,
        tmdbImageService: TmdbImageService,
        serviceIds: Set<String> = DefaultServiceIds,
        defaultDispatcher: CoroutineContext = Dispatchers.Default,
    ): ServiceConnectionTester {
        return ServiceConnectionTester(
            listOf(
                Service(ID_BANGUMI) {
                    withTestTimeout {
                        bangumiClient.testConnectionMaster() == ConnectionStatus.SUCCESS
                    }
                },
                Service(ID_BANGUMI_NEXT) {
                    withTestTimeout {
                        bangumiClient.testConnectionNext() == ConnectionStatus.SUCCESS
                    }
                },
                Service(ID_ANI) {
                    withTestTimeout {
                        runCatching {
                            // Note, we may have `expectSuccess = true` so on failure it will throw an exception.
                            aniClient.invoke {
                                getTrends().response.status.isSuccess()
                            }
                        }.getOrElse { false }
                    }
                },
                // 详情页背景图与选集卡片剧照的来源. 接口与图片 CDN 是两个域名, 分成两项各自
                // 出结果 —— 墙内两者常常一通一不通, 合成一项会让用户无从判断, 见 TmdbImageService
                Service(ID_TMDB) {
                    // 可能要依次试主备两个域名, 给的上限比别人宽, 否则通的网络也会被判超时
                    withTestTimeout(TMDB_TEST_TIMEOUT_MILLIS) {
                        tmdbImageService.testApiConnection()
                    }
                },
                Service(ID_TMDB_IMAGE) {
                    // 只有一个域名一次 HEAD, 用默认上限就够
                    withTestTimeout {
                        tmdbImageService.testImageConnection()
                    }
                },
            ).filter { it.id in serviceIds },
            defaultDispatcher,
        )

    }
}
