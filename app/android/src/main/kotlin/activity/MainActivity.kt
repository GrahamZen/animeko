/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.android.activity

import android.app.UiModeManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.view.WindowCompat
import me.him188.ani.android.BuildConfig
import me.him188.ani.android.tv.TvHomeChannels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.him188.ani.app.navigation.AniNavigator
import me.him188.ani.app.platform.rememberPlatformWindow
import me.him188.ani.app.ui.exprovider.ExternalContentProviderFactory
import me.him188.ani.app.ui.exprovider.LocalExternalContentProvider
import me.him188.ani.app.ui.foundation.LocalIsAndroidTV
import me.him188.ani.app.ui.foundation.layout.LocalPlatformWindow
import me.him188.ani.app.ui.foundation.theme.SystemBarColorEffect
import me.him188.ani.app.ui.foundation.widgets.LocalToaster
import me.him188.ani.app.ui.foundation.widgets.Toaster
import me.him188.ani.app.ui.main.AniApp
import me.him188.ani.app.ui.main.AniAppContent
import me.him188.ani.utils.logging.error
import me.him188.ani.utils.logging.logger
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject

class MainActivity : AniComponentActivity() {
    private val logger = logger<MainActivity>()
    private val aniNavigator = AniNavigator()

    private val externalContentProviderFactory: ExternalContentProviderFactory by inject()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        handleStartIntent(intent)
    }

    private fun handleStartIntent(intent: Intent) {
        val data = intent.data ?: return
        if (data.scheme != "ani") return
        if (data.host == "subjects") {
            val id = data.pathSegments.getOrNull(0)?.toIntOrNull() ?: return
            lifecycleScope.launch {
                try {
                    if (!aniNavigator.isNavControllerReady()) {
                        aniNavigator.awaitNavController()
                        delay(1000) // 等待初始化好, 否则跳转可能无效
                    }
                    aniNavigator.navigateSubjectDetails(id, placeholder = null)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to navigate to subject details" }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleStartIntent(intent)

        // TV 主屏预览频道 (热门动画 / 继续观看), 仅 TV 生效且每进程只跑一次;
        // 延迟到启动高峰之后, 需要 activity context 才能弹出添加频道的系统确认框
        lifecycleScope.launch {
            delay(10_000)
            TvHomeChannels.updateOnce(this@MainActivity, getKoin())
        }

        enableEdgeToEdge(
            // 透明状态栏
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            // 透明导航栏
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )

        // 允许画到 system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val toaster = object : Toaster {
            override fun toast(text: String) {
                Toast.makeText(this@MainActivity, text, Toast.LENGTH_LONG).show()
            }
        }

        val externalContentProvider = externalContentProviderFactory.create(this, lifecycleScope)

        setContent {
            // 提供在 AniApp 外层, 让 AniApp 根部的 TV 返回键拦截 (isTv) 能读到正确的值
            CompositionLocalProvider(LocalIsAndroidTV provides isUIModeTV()) {
                AniApp {
                    val externalComponentProviderUpdated by rememberUpdatedState(externalContentProvider)

                    SystemBarColorEffect()

                    CompositionLocalProvider(
                        LocalToaster provides toaster,
                        LocalPlatformWindow provides rememberPlatformWindow(this),
                        LocalExternalContentProvider provides externalComponentProviderUpdated,
                    ) {
                        // Expose Modifier.testTag as resource-id in accessibility/uiautomator dumps,
                        // so UI-automation agents can locate elements by stable ids (debug only).
                        @OptIn(ExperimentalComposeUiApi::class)
                        val rootModifier = if (BuildConfig.DEBUG) {
                            Modifier.semantics { testTagsAsResourceId = true }
                        } else {
                            Modifier
                        }
                        Box(rootModifier) {
                            AniAppContent(aniNavigator)
                        }
                    }
                }
            }
        }
    }

    private fun isUIModeTV(): Boolean {
        val uiModeManager = getSystemService(UiModeManager::class.java)
        return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
}
