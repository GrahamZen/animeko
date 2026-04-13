/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.update

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import me.him188.ani.app.platform.LocalContext
import me.him188.ani.app.ui.foundation.animation.AniAnimatedVisibility

/**
 * 检测新版本并在右下角显示一个 5 秒后自动消失的提示.
 * 没有下载按钮, 仅展示版本号文字.
 */
@Composable
fun BoxScope.UpdateNotifier(
    viewModel: AppUpdateViewModel = viewModel { AppUpdateViewModel() },
) {
    SideEffect {
        viewModel.startAutomaticCheckLatestVersion()
    }

    val presentation by viewModel.presentationFlow.collectAsStateWithLifecycle()
    val newVersion = presentation.newVersion

    // Reset visibility whenever a new version name appears
    var visible by rememberSaveable(newVersion?.name) { mutableStateOf(true) }

    // Auto-dismiss after 5 seconds
    LaunchedEffect(newVersion?.name) {
        if (newVersion != null) {
            delay(5_000)
            visible = false
        }
    }

    // Keep the last known name alive so the exit animation can still render text
    var lastName by remember { mutableStateOf<String?>(null) }
    if (newVersion != null) lastName = newVersion.name

    AniAnimatedVisibility(
        visible = newVersion != null && visible,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(24.dp),
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier.shadow(4.dp, MaterialTheme.shapes.extraLarge),
        ) {
            Text(
                text = "发现新版本 ${lastName ?: ""}，可前往设置更新",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

/**
 * 设置页中的更新提示卡片，带下载和安装按钮，永久显示直到手动关闭.
 */
@Composable
fun BoxScope.UpdateSettingsNotifier(
    viewModel: AppUpdateViewModel = viewModel { AppUpdateViewModel() },
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val presentation by viewModel.presentationFlow.collectAsStateWithLifecycle()
    val newVersion = presentation.newVersion
    val state = presentation.state

    // Per-version dismiss state
    var dismissed by rememberSaveable(newVersion?.name) { mutableStateOf(false) }

    // Install-failure dialog
    var installErrorMessage by remember { mutableStateOf<String?>(null) }

    installErrorMessage?.let { msg ->
        FailedToInstallDialog(
            message = msg,
            onDismissRequest = { installErrorMessage = null },
            state = state,
        )
    }

    val showCard = !dismissed && (state is AppUpdateState.HasUpdate || presentation.isDownloading)

    AniAnimatedVisibility(
        visible = showCard,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(24.dp),
    ) {
        when {
            state is AppUpdateState.HasUpdate -> {
                NewVersionPopupCard(
                    version = newVersion?.name ?: "",
                    changes = newVersion?.majorChanges ?: emptyList(),
                    onDetailsClick = {
                        newVersion?.let {
                            uriHandler.openUri(
                                "https://github.com/$FORK_OWNER/$FORK_REPO/releases/tag/v${it.name}",
                            )
                        }
                    },
                    onAutoUpdateClick = {
                        newVersion?.let { viewModel.startDownload(it, uriHandler) }
                    },
                    onDismissRequest = { dismissed = true },
                )
            }

            presentation.isDownloading -> {
                DownloadingUpdatePopupCard(
                    version = newVersion ?: return@AniAnimatedVisibility,
                    fileDownloaderStats = presentation.fileDownloaderStats,
                    error = presentation.downloadError,
                    onInstallClick = {
                        val result = viewModel.install(context)
                        if (result != null) {
                            installErrorMessage = result.message ?: "未知错误"
                        }
                    },
                    onCancelClick = {
                        viewModel.cancelDownload()
                        dismissed = true
                    },
                    onRetryClick = { viewModel.restartDownload(uriHandler) },
                )
            }
        }
    }
}
