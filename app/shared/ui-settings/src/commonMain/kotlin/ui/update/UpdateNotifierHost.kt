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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.him188.ani.app.platform.LocalContext
import me.him188.ani.app.ui.foundation.LocalPlatform
import me.him188.ani.app.ui.foundation.animation.AniAnimatedVisibility
import me.him188.ani.app.ui.foundation.isTv
import me.him188.ani.app.ui.foundation.tv.tvResolveFocus
import kotlinx.coroutines.delay

/** 入口更新提示卡无操作自动消失时长 (毫秒). */
private const val UPDATE_CARD_AUTO_DISMISS_MILLIS = 20_000L

/**
 * 检测新版本并在右下角显示更新卡片 (上游同款带按钮样式): 详情 / 自动更新 / 关闭.
 * 点自动更新走与设置页完全相同的下载流程 (下载卡片可取消/重试), TV 上下载完自动安装.
 * TV: 卡片出现时初始焦点直接落在 "自动更新" 按钮上.
 */
@Composable
fun BoxScope.UpdateNotifier(
    viewModel: AppUpdateViewModel = viewModel { AppUpdateViewModel() },
) {
    SideEffect {
        viewModel.startAutomaticCheckLatestVersion()
    }

    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val presentation by viewModel.presentationFlow.collectAsStateWithLifecycle()
    val newVersion = presentation.newVersion
    val state = presentation.state

    // Per-version dismiss state
    var dismissed by rememberSaveable(newVersion?.name) { mutableStateOf(false) }

    // TV: 下载完成后自动安装 (与设置页一致), 遥控器用户不必再按一次安装
    val isTv = LocalPlatform.current.isTv()
    val downloaded = state is AppUpdateState.Downloaded
    LaunchedEffect(isTv, downloaded) {
        if (isTv && downloaded) {
            viewModel.install(context)
        }
    }

    // 安装失败对话框: 失败由 ViewModel 状态承载 (install 本身立即返回)
    presentation.installationFailure?.let { failure ->
        FailedToInstallDialog(
            message = failure.reason.toString(),
            onDismissRequest = { viewModel.dismissInstallationFailure() },
            state = state,
        )
    }

    val showCard = !dismissed && (state is AppUpdateState.HasUpdate || presentation.isDownloading)
    val hasUpdateCard = showCard && state is AppUpdateState.HasUpdate

    // TV: 气泡出现时把初始焦点送到"自动更新"按钮 (到位确认 + 重试, 见 tvResolveFocus)
    val autoUpdateFocus = remember { FocusRequester() }
    var autoUpdateFocused by remember { mutableStateOf(false) }
    LaunchedEffect(isTv, hasUpdateCard, newVersion?.name) {
        if (isTv && hasUpdateCard) {
            // 到位标志复位: 同一组合生命周期内出现第二个版本的卡片时,
            // 上一轮遗留的 true 会让解析立即"假到位", 新卡片拿不到初始焦点
            autoUpdateFocused = false
            tvResolveFocus(arrived = { autoUpdateFocused }) {
                runCatching { autoUpdateFocus.requestFocus() }
            }
        }
    }

    // 无操作自动消失: 提示卡出现一段时间后自行关闭, 不永久挡住右下角内容.
    // 开始下载后 hasUpdateCard 变 false, 本效应取消 —— 下载进度卡不受影响
    LaunchedEffect(hasUpdateCard, newVersion?.name) {
        if (hasUpdateCard) {
            delay(UPDATE_CARD_AUTO_DISMISS_MILLIS)
            dismissed = true
        }
    }

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
                    autoUpdateButtonModifier = Modifier
                        .focusRequester(autoUpdateFocus)
                        // 双向上报: 只报得不报失时, 按钮已聚焦下解析效应重跑 (复位标志后
                        // requestFocus 无事件) 会烧满轮询并抢回用户移开的焦点
                        .onFocusChanged { autoUpdateFocused = it.isFocused },
                )
            }

            presentation.isDownloading -> {
                DownloadingUpdatePopupCard(
                    version = newVersion ?: return@AniAnimatedVisibility,
                    fileDownloaderStats = presentation.fileDownloaderStats,
                    error = presentation.downloadError,
                    isInstalling = state is AppUpdateState.Installing,
                    onInstallClick = { viewModel.install(context) },
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

    // TV: 下载完成后自动安装, 避免遥控器用户在下载结束后还要再操作一次按钮.
    // 用状态变为 Downloaded (而非定时器) 触发, 以适配不同设备的下载耗时.
    val isTv = LocalPlatform.current.isTv()
    val downloaded = state is AppUpdateState.Downloaded
    LaunchedEffect(isTv, downloaded) {
        if (isTv && downloaded) {
            viewModel.install(context)
        }
    }

    // 安装失败对话框: 失败由 ViewModel 状态承载 (install 本身立即返回)
    presentation.installationFailure?.let { failure ->
        FailedToInstallDialog(
            message = failure.reason.toString(),
            onDismissRequest = { viewModel.dismissInstallationFailure() },
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
                    isInstalling = state is AppUpdateState.Installing,
                    onInstallClick = { viewModel.install(context) },
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
