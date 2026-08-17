/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.cache

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.combinedClickable
import me.him188.ani.app.ui.foundation.aniCombinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.him188.ani.app.domain.media.cache.engine.MediaCacheEngineKey
import me.him188.ani.app.domain.media.cache.engine.MediaStats
import me.him188.ani.app.tools.getOrZero
import me.him188.ani.app.ui.adaptive.AniListDetailPaneScaffold
import me.him188.ani.app.ui.foundation.widgets.dismissDialogButton
import me.him188.ani.app.ui.adaptive.AniTopAppBar
import me.him188.ani.app.ui.adaptive.AniTopAppBarDefaults
import me.him188.ani.app.ui.adaptive.ListDetailLayoutParameters
import me.him188.ani.app.ui.cache.components.CacheEpisodeState
import me.him188.ani.app.ui.cache.components.CacheFilterAndSortBar
import me.him188.ani.app.ui.cache.components.CacheFilterAndSortState
import me.him188.ani.app.ui.cache.components.CacheGroupState
import me.him188.ani.app.ui.cache.components.CacheManagementOverallStats
import me.him188.ani.app.ui.cache.components.CacheSelectionFloatingToolbar
import me.him188.ani.app.ui.cache.components.CacheSelectionState
import me.him188.ani.app.ui.cache.components.DownloadStateIcon
import me.him188.ani.app.ui.cache.components.TestCacheGroupSates
import me.him188.ani.app.ui.cache.components.createTestMediaStats
import me.him188.ani.app.ui.cache.components.rememberCacheFilterAndSortState
import me.him188.ani.app.ui.foundation.LocalAniUiBehavior
import me.him188.ani.app.ui.cache.components.rememberCacheSelectionState
import me.him188.ani.app.ui.foundation.ProvideCompositionLocalsForPreview
import me.him188.ani.app.ui.foundation.animation.AniAnimatedVisibility
import me.him188.ani.app.ui.foundation.layout.AniWindowInsets
import me.him188.ani.app.ui.foundation.layout.currentWindowAdaptiveInfo1
import me.him188.ani.app.ui.foundation.layout.paneHorizontalPadding
import me.him188.ani.app.ui.foundation.layout.plus
import me.him188.ani.app.ui.foundation.navigation.BackHandler
import me.him188.ani.app.ui.foundation.rememberAsyncHandler
import me.him188.ani.app.ui.foundation.rememberCurrentTopAppBarContainerColor
import me.him188.ani.app.ui.foundation.session.SelfAvatar
import me.him188.ani.app.ui.foundation.theme.AniThemeDefaults
import me.him188.ani.app.ui.foundation.theme.appChromeHazeSource
import me.him188.ani.app.ui.foundation.widgets.BackNavigationIconButton
import me.him188.ani.app.ui.foundation.widgets.LocalToaster
import me.him188.ani.app.ui.lang.Lang
import me.him188.ani.app.ui.lang.cache_episode_pause_download
import me.him188.ani.app.ui.lang.cache_episode_resume_download
import me.him188.ani.app.ui.lang.cache_management_delete_cache_confirmation
import me.him188.ani.app.ui.lang.cache_management_delete_cache_title
import me.him188.ani.app.ui.lang.cache_management_delete_summary
import me.him188.ani.app.ui.lang.cache_management_delete_summary_item
import me.him188.ani.app.ui.lang.cache_management_downloading_count
import me.him188.ani.app.ui.lang.cache_management_enter_selection_mode
import me.him188.ani.app.ui.lang.cache_management_episode_label
import me.him188.ani.app.ui.lang.cache_management_exit_selection
import me.him188.ani.app.ui.lang.cache_management_finished_count
import me.him188.ani.app.ui.lang.cache_management_invalid_cache_info
import me.him188.ani.app.ui.lang.cache_management_more_actions
import me.him188.ani.app.ui.lang.cache_management_more_info
import me.him188.ani.app.ui.lang.cache_management_play
import me.him188.ani.app.ui.lang.cache_management_select_all
import me.him188.ani.app.ui.lang.cache_management_select_item_for_details
import me.him188.ani.app.ui.lang.cache_management_selected_count
import me.him188.ani.app.ui.lang.cache_management_selection_summary
import me.him188.ani.app.ui.lang.cache_management_streaming_not_supported
import me.him188.ani.app.ui.lang.cache_subject_cancel
import me.him188.ani.app.ui.lang.cache_subject_delete
import me.him188.ani.app.ui.lang.cache_unknown
import me.him188.ani.app.ui.lang.main_screen_page_cache_management
import me.him188.ani.app.ui.settings.rendering.P2p
import me.him188.ani.app.ui.user.SelfInfoUiState
import me.him188.ani.datasources.api.topic.FileSize.Companion.bytes
import me.him188.ani.utils.platform.annotations.TestOnly
import org.jetbrains.compose.resources.stringResource

/**
 * 全局缓存管理页面状态
 */
@Immutable
data class CacheManagementState(
    val overallStats: MediaStats,
    val groups: List<CacheGroupState>,
) {
    internal val entries = groups.flatMap { it.entries }

    companion object {
        val Placeholder = CacheManagementState(
            MediaStats.Unspecified,
            emptyList(),
        )
    }
}


/**
 * 全局缓存管理页面
 */
@Composable
fun CacheManagementScreen(
    vm: CacheManagementViewModel,
    selfInfo: SelfInfoUiState?,
    onPlay: (CacheEpisodeState) -> Unit,
    onClickLogin: () -> Unit,
    onNavigateCacheDetail: (cacheId: String) -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    windowInsets: WindowInsets = AniWindowInsets.forPageContent(),
) {
    val state by vm.stateFlow.collectAsStateWithLifecycle()
    ForcedDarkTheme {
        CacheManagementScreen(
            state,
            selfInfo,
            onPlay,
            onResume = { vm.resumeCache(it) },
            onPause = { vm.pauseCache(it) },
            onDelete = { vm.deleteCache(it) },
            onViewDetail = { onNavigateCacheDetail(it.cacheId) },
            onClickLogin = onClickLogin,
            modifier = modifier,
            navigationIcon = navigationIcon,
            windowInsets = windowInsets,
        )
    }
}


@Composable
fun CacheManagementScreen(
    state: CacheManagementState,
    selfInfo: SelfInfoUiState?,
    onPlay: (CacheEpisodeState) -> Unit,
    onResume: (CacheEpisodeState) -> Unit,
    onPause: (CacheEpisodeState) -> Unit,
    onViewDetail: (CacheEpisodeState) -> Unit,
    onDelete: (CacheEpisodeState) -> Unit,
    onClickLogin: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    windowInsets: WindowInsets = AniWindowInsets.forPageContent(),
) {
    // appBarColors 派生出顶栏 / 总体统计块 / 筛选 stickyHeader 三处底色, 默认的
    // surfaceContainerLowest 在浅色下是压在外壳背景上的白色矩形. 沉浸式外壳下统一改成外壳
    // 背景色 (视觉上无边界); 不用透明 —— stickyHeader 需要不透明底盖住滚到其下的列表项.
    val appBarColors = if (LocalAniUiBehavior.current.immersiveShell) {
        val shellBackground = AniThemeDefaults.shellBackgroundColor
        AniThemeDefaults.topAppBarColors().copy(
            containerColor = shellBackground,
            scrolledContainerColor = shellBackground,
        )
    } else {
        AniThemeDefaults.topAppBarColors()
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val listState = rememberLazyListState()
    val detailListState = rememberLazyListState()
    val cacheFilterState = rememberCacheFilterAndSortState()
    val selectionState = rememberCacheSelectionState()

    // TV: 删除某条目时, 聚焦的是该条目行, 行被移出列表 -> 焦点丢失. 删除后把焦点落到 top bar 右上角按钮
    // (始终存在). 用 pending 标志 + 等 entries 真正更新后再请求, 避免在列表更新前请求(那时行还在).
    val focusDriven = LocalAniUiBehavior.current.focusDrivenNavigation
    val topBarActionFocusRequester = remember { FocusRequester() }
    var pendingTopBarFocus by remember { mutableStateOf(false) }
    LaunchedEffect(state.entries) {
        if (pendingTopBarFocus) {
            pendingTopBarFocus = false
            if (focusDriven) {
                withFrameNanos { }
                runCatching { topBarActionFocusRequester.requestFocus() }
            }
        }
    }

    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val listDetailLayoutParameters = ListDetailLayoutParameters.calculate(navigator.scaffoldDirective)

    // 已过滤的 entries
    val selectionEntries = if (listDetailLayoutParameters.preferSinglePane)
        cacheFilterState.applyFilterAndSort(state.entries) else state.entries

    // region selection
    var deleteSelectedCacheDialog by rememberSaveable { mutableStateOf(false) }

    // 当前选中的 entries
    val selectedEntries = remember(selectionEntries, selectionState.selectedIds) {
        selectionEntries.filter { it.cacheId in selectionState.selectedIds }
    }
    // 当前选中的 entries 数量
    val selectionCount = selectionState.selectedIds.size

    // 当 list detail pane 的类型改变并且在编辑模式时, 需要确保 selectedIds 只能是当前可见的 entries
    LaunchedEffect(state.entries, selectionState.inSelection) {
        if (selectionState.inSelection) {
            val validIds = state.entries.map { it.cacheId }.toSet()
            selectionState.overrideSelected(selectionState.selectedIds.filter { id -> id in validIds }.toSet())
        }
    }

    // 选择模式下导航返回应该退出选择模式
    BackHandler(selectionState.inSelection) { selectionState.clear() }

    // 当前正在浏览的 cache group
    var currentViewingGroupKey by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.groups) {
        if (state.groups.isEmpty()) {
            currentViewingGroupKey = null
        } else if (state.groups.none { it.key == currentViewingGroupKey }) {
            currentViewingGroupKey = state.groups.first().key
        }
    }
    val currentViewingGroup = remember(state.groups, currentViewingGroupKey) {
        state.groups.firstOrNull { it.key == currentViewingGroupKey }
    }

    /**
     * 「全选」作用的范围.
     *
     * **两栏形态下只作用于右边正在看的那一部作品**, 而不是整页所有条目: 左边是作品列表、右边是
     * 那一部的剧集, 顶栏那颗按钮长在右边这一栏的上方, 读起来就是"选中这一部的全部剧集".
     * 原先它选的是 [selectionEntries] (两栏下 = 全部作品的全部剧集) —— 2026-08-17 真机上
     * 用户在第二季那一组按全选, 连着另外两部作品共 10 条缓存一起删掉了, 而且删除确认框
     * 不报条数也不报是谁, 弹窗长得和只删两条时一模一样.
     *
     * 单栏形态没有这个歧义 (列表本来就是全部剧集), 维持原样.
     */
    val selectAllEntries = if (listDetailLayoutParameters.preferSinglePane) {
        selectionEntries
    } else {
        currentViewingGroup?.entries ?: selectionEntries
    }
    // 「全选」这颗按钮此刻是不是"取消全选": 只看它自己的范围
    val allSelected = remember(selectAllEntries, selectionState.selectedIds) {
        selectAllEntries.isNotEmpty() && selectAllEntries.all { it.cacheId in selectionState.selectedIds }
    }

    // 确认删除的对话框
    if (deleteSelectedCacheDialog) {
        DeleteActionDialog(
            onDismiss = { deleteSelectedCacheDialog = false },
            // 删除不可撤销, 而选中集合可能跨作品 (左边列表也能整组选), 所以必须把"到底要删什么"
            // 摊开写: 几条, 哪几部, 各几条
            summary = rememberCacheDeleteSummary(selectedEntries),
            onConfirm = {
                selectionEntries.filter { it.cacheId in selectionState.selectedIds }
                    .forEach { onDelete(it) }
                selectionState.clear()
                deleteSelectedCacheDialog = false
            },
        )
    }

    CacheManagementLayout(
        state = state,
        cacheFilterState = cacheFilterState,
        selectionState = selectionState,
        navigator = navigator,
        cacheEntries = state.entries,
        filteredEntries = selectionEntries,
        groupedEntries = state.groups,
        topBar = {
            CacheManagementTopBar(
                selectionMode = selectionState.inSelection,
                selectionCount = selectionCount,
                allSelected = allSelected,
                hasEntries = selectAllEntries.isNotEmpty(),
                onEnterSelection = { selectionState.enterSelectionWith(emptySet()) },
                onExitSelection = { selectionState.clear() },
                onToggleSelectAll = {
                    // 加减自己范围内的 id, 而不是整体覆盖: 两栏下用户可能已经在左边列表里整组选过
                    // 别的作品, 那份选择不该被这颗按钮悄悄抹掉 (反过来取消全选也只取消自己这部)
                    val ids = selectAllEntries.map { it.cacheId }
                    selectionState.enterSelectionWith(
                        if (allSelected) selectionState.selectedIds - ids.toSet()
                        else selectionState.selectedIds + ids,
                    )
                },
                selfInfo = selfInfo,
                onClickLogin = onClickLogin,
                navigationIcon = navigationIcon,
                appBarColors = appBarColors,
                windowInsets = AniWindowInsets.forTopAppBarWithoutDesktopTitle(),
                scrollBehavior = scrollBehavior,
                actionFocusRequester = topBarActionFocusRequester,
            )
        },
        bottomBar = {
            AniAnimatedVisibility(selectionState.inSelection) {
                CacheSelectionFloatingToolbar(
                    resumeEnabled = selectedEntries.any { !it.isFinished && it.isPaused },
                    pauseEnabled = selectedEntries.any { !it.isFinished && !it.isPaused && !it.isFailed },
                    deleteEnabled = selectedEntries.isNotEmpty(),
                    onResumeSelected = {
                        selectedEntries.filter { !it.isFinished && it.isPaused }.forEach(onResume)
                    },
                    onPauseSelected = {
                        selectedEntries.filter { !it.isFinished && !it.isPaused && !it.isFailed }.forEach(onPause)
                    },
                    onDeleteSelected = { deleteSelectedCacheDialog = true },
                    windowInsets = windowInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                )
            }
        },
        selectedEntries = selectedEntries,
        selectedGroup = currentViewingGroup,
        appBarColors = appBarColors,
        scrollBehavior = scrollBehavior,
        listState = listState,
        detailListState = detailListState,
        onSelectGroup = { currentViewingGroupKey = it?.key },
        onToggleSelected = { entry -> selectionState.toggleSelection(entry.cacheId) },
        onEnterSelection = { entry ->
            selectionState.enterSelectionWith(selectionState.selectedIds + entry.cacheId)
        },
        onToggleGroupSelection = { group ->
            selectionState.toggleSelection(*group.entries.map { it.cacheId }.toTypedArray())
        },
        onEnterGroupSelection = { group ->
            selectionState.enterSelectionWith(selectionState.selectedIds + group.entries.map { it.cacheId })
        },
        onPlay = onPlay,
        onResume = onResume,
        onPause = onPause,
        // TV: 单条删除后该行被移除会丢焦点, 标记一下, 待 entries 更新后把焦点落到 top bar.
        onDelete = { entry ->
            onDelete(entry)
            pendingTopBarFocus = true
        },
        onViewDetail = onViewDetail,
        windowInsets = windowInsets,
        listDetailLayoutParameters = listDetailLayoutParameters,
        modifier = modifier,
    )
}


@Composable
private fun CacheManagementLayout(
    state: CacheManagementState,
    cacheFilterState: CacheFilterAndSortState,
    selectionState: CacheSelectionState,
    navigator: ThreePaneScaffoldNavigator<String>,
    cacheEntries: List<CacheEpisodeState>,
    filteredEntries: List<CacheEpisodeState>,
    groupedEntries: List<CacheGroupState>,
    selectedGroup: CacheGroupState?,
    onSelectGroup: (CacheGroupState?) -> Unit,
    onToggleSelected: (CacheEpisodeState) -> Unit,
    onEnterSelection: (CacheEpisodeState) -> Unit,
    onToggleGroupSelection: (CacheGroupState) -> Unit,
    onEnterGroupSelection: (CacheGroupState) -> Unit,
    onPlay: (CacheEpisodeState) -> Unit,
    onResume: (CacheEpisodeState) -> Unit,
    onPause: (CacheEpisodeState) -> Unit,
    onDelete: (CacheEpisodeState) -> Unit,
    onViewDetail: (CacheEpisodeState) -> Unit,
    appBarColors: TopAppBarColors,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    selectedEntries: List<CacheEpisodeState>,
    scrollBehavior: TopAppBarScrollBehavior,
    listState: LazyListState,
    detailListState: LazyListState,
    windowInsets: WindowInsets,
    listDetailLayoutParameters: ListDetailLayoutParameters,
    modifier: Modifier = Modifier
) {
    val tasker = rememberAsyncHandler()

    // 沉浸式外壳: 透明, 透出外壳统一的全屏背景 (见 MainScreen); 其它平台维持原页面背景色
    val transparentBackground = LocalAniUiBehavior.current.immersiveShell
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        containerColor = if (transparentBackground) Color.Transparent else AniThemeDefaults.pageContentBackgroundColor,
        contentWindowInsets = windowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        // bottom padding 作为列表的 contentPadding, 让内容可以滚动到毛玻璃导航栏下方.
        val listBottomPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding())
        val paneExtraPadding = currentWindowAdaptiveInfo1().windowSizeClass.paneHorizontalPadding
        AniListDetailPaneScaffold(
            // 毛玻璃 app chrome 的模糊来源.
            modifier = Modifier
                .appChromeHazeSource(backgroundColor = AniThemeDefaults.pageContentBackgroundColor)
                .padding(
                    start = paddingValues.calculateStartPadding(layoutDirection),
                    top = paddingValues.calculateTopPadding(),
                    end = paddingValues.calculateEndPadding(layoutDirection),
                ),
            navigator = navigator,
            listPaneTopAppBar = null,
            listPaneContent = {
                val listSpacedBy = if (isSinglePane) 0.dp else 24.dp
                if (isSinglePane) {
                    val topAppBarContainerColor by rememberCurrentTopAppBarContainerColor(appBarColors, scrollBehavior)
                    LazyColumn(
                        modifier = Modifier
                            .paneWindowInsetsPadding()
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .fillMaxWidth()
                            .wrapContentWidth()
                            .widthIn(max = 1300.dp),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = listBottomPadding + PaddingValues(bottom = paneExtraPadding),
                    ) {
                        item("overall_stats") {
                            Surface(
                                color = appBarColors.containerColor,
                                contentColor = contentColorFor(appBarColors.containerColor),
                            ) {
                                if (selectionState.inSelection) {
                                    CacheSelectionSummary(
                                        selectedEntries,
                                        Modifier
                                            .paneContentPadding()
                                            .padding(horizontal = listSpacedBy)
                                            .fillMaxWidth(),
                                    )
                                } else {
                                    CacheManagementOverallStats(
                                        { state.overallStats },
                                        Modifier
                                            .paneContentPadding()
                                            .padding(horizontal = listSpacedBy)
                                            .fillMaxWidth(),
                                    )
                                }
                            }
                        }
                        stickyHeader("filter_row") {
                            CacheFilterAndSortBar(
                                state = cacheFilterState,
                                modifier = Modifier.paneContentPadding().fillMaxWidth(),
                                containerColor = topAppBarContainerColor,
                                mediaCacheEngineOptions = remember(cacheEntries) {
                                    cacheEntries.mapNotNull { it.engineKey }.distinct()
                                },
                            )
                        }
                        items(filteredEntries, key = { it.listItemKey }) { entry ->
                            CacheListItem(
                                entry = entry,
                                selectionMode = selectionState.inSelection,
                                selected = entry.cacheId in selectionState.selectedIds,
                                onToggleSelected = { onToggleSelected(entry) },
                                onEnterSelection = { onEnterSelection(entry) },
                                onPlay = { onPlay(entry) },
                                onResume = { onResume(entry) },
                                onPause = { onPause(entry) },
                                onViewDetail = { onViewDetail(entry) },
                                onDelete = { onDelete(entry) },
                                modifier = Modifier.paneContentPadding().padding(horizontal = listSpacedBy),
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .paneContentPadding()
                            .paneWindowInsetsPadding()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = listBottomPadding + PaddingValues(bottom = paneExtraPadding),
                    ) {
                        item("overall_stats") {
                            Surface(
                                color = appBarColors.containerColor,
                                contentColor = contentColorFor(appBarColors.containerColor),
                            ) {
                                if (selectionState.inSelection) {
                                    CacheSelectionSummary(
                                        selectedEntries,
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                    )
                                } else {
                                    CacheManagementOverallStats(
                                        { state.overallStats },
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                        items(groupedEntries, key = { it.key }) { group ->
                            CacheSubjectListItem(
                                group = group,
                                selected = group.key == selectedGroup?.key,
                                selectionMode = selectionState.inSelection,
                                selectedCacheIds = selectionState.selectedIds,
                                onToggleGroupSelection = onToggleGroupSelection,
                                onLongClick = { onEnterGroupSelection(group) },
                                onClick = {
                                    onSelectGroup(group)
                                    tasker.launch {
                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            },
            detailPane = {
                if (isSinglePane) {
                    Box(
                        Modifier
                            .paneContentPadding()
                            .paneWindowInsetsPadding(),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .paneContentPadding(extraStart = -paneExtraPadding, extraEnd = -paneExtraPadding)
                            .paneWindowInsetsPadding()
                            .fillMaxHeight(),
                        state = detailListState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = listBottomPadding + PaddingValues(vertical = paneExtraPadding),
                    ) {
                        val entries = selectedGroup?.entries.orEmpty()
                        if (entries.isEmpty()) {
                            item("empty_detail") {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(stringResource(Lang.cache_management_select_item_for_details))
                                }
                            }
                        } else {
                            items(entries, key = { it.listItemKey }) { entry ->
                                CacheListItem(
                                    entry = entry,
                                    selectionMode = selectionState.inSelection,
                                    selected = entry.cacheId in selectionState.selectedIds,
                                    onToggleSelected = { onToggleSelected(entry) },
                                    onEnterSelection = { onEnterSelection(entry) },
                                    onPlay = { onPlay(entry) },
                                    onResume = { onResume(entry) },
                                    onPause = { onPause(entry) },
                                    onViewDetail = { onViewDetail(entry) },
                                    onDelete = { onDelete(entry) },
                                    contentPadding = PaddingValues(paneExtraPadding),
                                    transparentBackgroundIfUnselected = true,
                                )
                            }
                        }
                    }
                }
            },
            // Bottom 通过 listBottomPadding 应用, 这里不再包含, 避免重复.
            contentWindowInsets = windowInsets.only(WindowInsetsSides.Horizontal),
            useSharedTransition = false,
        )
    }
}

@Composable
private fun CacheManagementTopBar(
    selectionMode: Boolean,
    selectionCount: Int,
    allSelected: Boolean,
    hasEntries: Boolean,
    onEnterSelection: () -> Unit,
    onExitSelection: () -> Unit,
    onToggleSelectAll: () -> Unit,
    selfInfo: SelfInfoUiState?,
    onClickLogin: () -> Unit,
    navigationIcon: @Composable () -> Unit,
    appBarColors: TopAppBarColors,
    windowInsets: WindowInsets,
    scrollBehavior: TopAppBarScrollBehavior?,
    /** TV: 绑到右上角 action 按钮的 FocusRequester. 由上层 hoist, 既用于切换选择模式后夺回焦点, 也用于
     *  删除条目后把焦点落到这里(top bar 始终存在). */
    actionFocusRequester: FocusRequester = remember { FocusRequester() },
) {
    // TV: 切换选择模式会把整个 TopAppBar 换成另一套按钮(if/else 两个不同的 AniTopAppBar), 原来聚焦的
    // 右上角按钮被销毁 -> 焦点丢失. FocusRequester hoist 在上层(不随 if/else 重建), 绑到两个分支右上角的
    // action 按钮上; 模式切换后请求焦点回该按钮, 焦点就不会丢. 跳过首次组合, 避免进页面就抢焦点.
    val focusDriven = LocalAniUiBehavior.current.focusDrivenNavigation
    var selectionModeSeen by remember { mutableStateOf(false) }
    LaunchedEffect(selectionMode) {
        if (!focusDriven) return@LaunchedEffect
        if (!selectionModeSeen) {
            selectionModeSeen = true
            return@LaunchedEffect
        }
        withFrameNanos { } // 等新分支布局完成再请求
        runCatching { actionFocusRequester.requestFocus() }
    }
    val actionFocusModifier = if (focusDriven) Modifier.focusRequester(actionFocusRequester) else Modifier

    if (selectionMode) {
        val selectedCountText = stringResource(Lang.cache_management_selected_count, selectionCount)
        val exitSelectionText = stringResource(Lang.cache_management_exit_selection)
        val selectAllText = stringResource(Lang.cache_management_select_all)
        AniTopAppBar(
            title = { AniTopAppBarDefaults.Title(selectedCountText) },
            navigationIcon = {
                IconButton(onClick = onExitSelection) { Icon(Icons.Rounded.Close, exitSelectionText) }
            },
            actions = {
                IconButton(
                    onClick = onToggleSelectAll,
                    enabled = hasEntries,
                    modifier = actionFocusModifier,
                ) {
                    Icon(
                        if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                        selectAllText,
                    )
                }
            },
            avatar = { },
            colors = appBarColors,
            windowInsets = windowInsets,
            scrollBehavior = scrollBehavior,
        )
    } else {
        AniTopAppBar(
            title = { AniTopAppBarDefaults.Title(stringResource(Lang.main_screen_page_cache_management)) },
            navigationIcon = navigationIcon,
            actions = {
                val enterSelectionModeText = stringResource(Lang.cache_management_enter_selection_mode)
                IconButton(
                    onClick = onEnterSelection,
                    enabled = hasEntries,
                    modifier = actionFocusModifier,
                ) {
                    Icon(Icons.Default.Checklist, enterSelectionModeText)
                }
            },
            // TV: 头像由主页左侧边栏统一承载, 顶栏不再重复
            avatar = if (focusDriven) {
                { }
            } else {
                selfInfo?.let {
                    { recommendedSize ->
                        SelfAvatar(
                            state = it,
                            onClick = onClickLogin,
                            size = recommendedSize,
                        )
                    }
                } ?: { }
            },
            colors = appBarColors,
            windowInsets = windowInsets,
            scrollBehavior = scrollBehavior,
        )
    }
}


/**
 * 多选模式下代替总体统计的选择摘要: "已选 n 项 · 共 x GB · n 个下载中".
 */
@Composable
private fun CacheSelectionSummary(
    selectedEntries: List<CacheEpisodeState>,
    modifier: Modifier = Modifier,
) {
    val totalSize = remember(selectedEntries) {
        selectedEntries.fold(0L) { acc, entry -> acc + entry.totalSize.inBytes }.bytes
    }
    val downloadingCount = remember(selectedEntries) {
        selectedEntries.count { !it.isFinished && !it.isPaused && !it.isFailed }
    }
    val summaryText = stringResource(Lang.cache_management_selection_summary, selectedEntries.size, "$totalSize")
    val downloadingText = stringResource(Lang.cache_management_downloading_count, downloadingCount)
    Text(
        if (downloadingCount > 0) "$summaryText · $downloadingText" else summaryText,
        modifier.padding(vertical = 12.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

object CacheManagementTestTags {
    const val DELETE_CONFIRM_BUTTON = "cache_management_delete_confirm_button"
}

/**
 * @param summary 「到底要删什么」的明细 (几条 / 哪几部 / 各几条); `null` = 单条删除, 用户点的就是
 *   那一行, 不需要复述. 见 [rememberCacheDeleteSummary].
 */
@Composable
internal fun DeleteActionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    summary: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(Lang.cache_management_delete_cache_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Lang.cache_management_delete_cache_confirmation))
                if (summary != null) {
                    Text(summary, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(CacheManagementTestTags.DELETE_CONFIRM_BUTTON),
            ) { Text(stringResource(Lang.cache_subject_delete), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = dismissDialogButton(stringResource(Lang.cache_subject_cancel), onDismiss),
    )
}

/**
 * 批量删除前那句明细: `将删除 10 项缓存` + 按作品分行的 `作品名（7 项）`.
 *
 * 按作品分行而不是一行列完: 电视上一行放不下几个中文剧名, 而"跨了几部作品"恰恰是这句话要传达的
 * 关键信息 —— 用户以为只在删眼前这一部时, 多出来的那几行就是刹车. 超过 [MAX_TITLES] 部只列前几部
 * 加省略号 (真到那个量级, "跨了很多部"这个事实已经传达到了).
 */
@Composable
private fun rememberCacheDeleteSummary(entries: List<CacheEpisodeState>): String? {
    if (entries.isEmpty()) return null
    val header = stringResource(Lang.cache_management_delete_summary, entries.size)
    val byTitle = entries.groupBy { it.subjectName }
    // 逐条取资源 (stringResource 只能在组合里调), 所以先在组合里把每一行拼好
    val lines = byTitle.entries.take(MAX_DELETE_SUMMARY_TITLES).map { (title, list) ->
        stringResource(Lang.cache_management_delete_summary_item, title, list.size)
    }
    val more = if (byTitle.size > MAX_DELETE_SUMMARY_TITLES) "\n…" else ""
    return header + "\n" + lines.joinToString("\n") + more
}

/** 删除明细里最多列几部作品, 见 [rememberCacheDeleteSummary]. */
private const val MAX_DELETE_SUMMARY_TITLES = 4

@Composable
private fun CacheSubjectListItem(
    group: CacheGroupState,
    selected: Boolean,
    selectionMode: Boolean,
    selectedCacheIds: Set<String>,
    onToggleGroupSelection: (CacheGroupState) -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .aniCombinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = MaterialTheme.shapes.large,
        tonalElevation = if (selected) 6.dp else 1.dp,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectionMode) {
                val allGroupSelected = group.entries.all { it.cacheId in selectedCacheIds }
                Checkbox(
                    checked = allGroupSelected,
                    onCheckedChange = { onToggleGroupSelection(group) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }

            Column(
                Modifier.weight(1f).animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val finishedCountText = stringResource(
                    Lang.cache_management_finished_count,
                    group.finishedCount,
                    group.entries.size,
                )
                Text(
                    group.subjectName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        finishedCountText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (group.downloadingCount > 0) {
                        val downloadingCountText = stringResource(
                            Lang.cache_management_downloading_count,
                            group.downloadingCount,
                        )
                        Text(
                            downloadingCountText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Row {
                    LinearProgressIndicator(
                        progress = { group.averageProgress.coerceIn(0f, 1f) },
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

        }
    }
}

@Composable
private fun CacheListItem(
    entry: CacheEpisodeState,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelected: () -> Unit,
    onEnterSelection: () -> Unit,
    onPlay: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit,
    onViewDetail: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    transparentBackgroundIfUnselected: Boolean = false,
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }

    if (showConfirm) {
        DeleteActionDialog(
            onDismiss = { showConfirm = false },
            onConfirm = {
                onDelete()
                showConfirm = false
            },
        )
    }

    Surface(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .aniCombinedClickable(
                onClick = {
                    if (selectionMode) {
                        onToggleSelected()
                    } else {
                        showMenu = true
                    }
                },
                onLongClick = {
                    onEnterSelection()
                },
            ),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        // 多选选中态用较轻的 surfaceContainer (≈ primary 8% 状态层), 强指示交给 Checkbox.
        color = if (selected) MaterialTheme.colorScheme.surfaceContainer else
            (if (transparentBackgroundIfUnselected) Color.Transparent else MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(contentPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 设计稿: 多选模式下复选框在行首, 行尾单项操作隐藏.
                if (selectionMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onToggleSelected() },
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        entry.engineKey?.let { key ->
                            val icon = renderEngineIcon(key)
                            val desc = when (key) {
                                MediaCacheEngineKey.Anitorrent -> "BT"
                                MediaCacheEngineKey.WebM3u -> "Web"
                                else -> stringResource(Lang.cache_unknown)
                            }
                            Icon(icon, desc, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Text(
                            entry.subjectName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        stringResource(Lang.cache_management_episode_label, entry.sort, entry.displayName),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (!selectionMode) Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DownloadStateIcon(entry.state)
                    val moreActionsText = stringResource(Lang.cache_management_more_actions)
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Rounded.MoreVert, moreActionsText)
                    }

                    CacheActionDropdown(
                        show = showMenu,
                        onDismiss = { showMenu = false },
                        episode = entry,
                        onPlay = {
                            onPlay()
                            showMenu = false
                        },
                        onResume = {
                            onResume()
                            showMenu = false
                        },
                        onPause = {
                            onPause()
                            showMenu = false
                        },
                        onViewDetail = {
                            onViewDetail()
                            showMenu = false
                        },
                        onDelete = {
                            showConfirm = true
                        },
                    )
                }
            }

            AniAnimatedVisibility(
                !entry.isFinished,
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val progress by animateFloatAsState(entry.progress.getOrZero())
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.weight(1f),
                        strokeCap = StrokeCap.Round,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        entry.speedText?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                        entry.progressText?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
        }
    }
}

private fun renderEngineIcon(key: MediaCacheEngineKey) = when (key) {
    MediaCacheEngineKey.Anitorrent -> Icons.Filled.P2p
    MediaCacheEngineKey.WebM3u -> Icons.Filled.Language
    else -> Icons.AutoMirrored.Rounded.HelpOutline
}

@Composable
internal fun CacheActionDropdown(
    show: Boolean,
    onDismiss: () -> Unit,
    episode: CacheEpisodeState,
    onPlay: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit,
    onViewDetail: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
) {
    val toaster = LocalToaster.current
    val resumeDownloadText = stringResource(Lang.cache_episode_resume_download)
    val pauseDownloadText = stringResource(Lang.cache_episode_pause_download)
    val playText = stringResource(Lang.cache_management_play)
    val invalidCacheInfoText = stringResource(Lang.cache_management_invalid_cache_info)
    val streamingNotSupportedText = stringResource(Lang.cache_management_streaming_not_supported)
    val moreInfoText = stringResource(Lang.cache_management_more_info)
    DropdownMenu(
        expanded = show,
        onDismissRequest = onDismiss,
        modifier = modifier,
        offset = offset,
    ) {
        if (!episode.isFinished) {
            if (episode.isPaused) {
                DropdownMenuItem(
                    text = { Text(resumeDownloadText) },
                    leadingIcon = { Icon(Icons.Rounded.Restore, null) },
                    onClick = {
                        onResume()
                        onDismiss()
                    },
                )
            } else if (!episode.isFailed) {
                DropdownMenuItem(
                    text = { Text(pauseDownloadText) },
                    leadingIcon = { Icon(Icons.Rounded.Pause, null) },
                    onClick = {
                        onPause()
                        onDismiss()
                    },
                )
            }
        }
        if (!episode.isFailed) {
            DropdownMenuItem(
                text = { Text(playText) },
                leadingIcon = { Icon(Icons.Rounded.PlayArrow, null) },
                onClick = {
                    when (episode.playability) {
                        CacheEpisodeState.Playability.PLAYABLE -> {
                            onPlay()
                            onDismiss()
                        }

                        CacheEpisodeState.Playability.INVALID_SUBJECT_EPISODE_ID -> {
                            toaster.toast(invalidCacheInfoText)
                        }

                        CacheEpisodeState.Playability.STREAMING_NOT_SUPPORTED -> {
                            toaster.toast(streamingNotSupportedText)
                        }
                    }
                },
            )
        }
        onViewDetail?.let {
            DropdownMenuItem(
                text = { Text(moreInfoText) },
                leadingIcon = { Icon(Icons.Rounded.Info, null) },
                onClick = {
                    it()
                    onDismiss()
                },
            )
        }

        DropdownMenuItem(
            text = { Text(stringResource(Lang.cache_subject_delete), color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
            onClick = {
                onDelete()
                onDismiss()
            },
        )
    }
}

@OptIn(TestOnly::class)
@Preview
@Composable
private fun PreviewCacheManagementScreen() {
    ProvideCompositionLocalsForPreview {
        CacheManagementScreen(
            state = remember {
                CacheManagementState(
                    createTestMediaStats(),
                    TestCacheGroupSates,
                )
            },
            selfInfo = null,
            onPlay = { },
            onResume = {},
            onPause = {},
            onDelete = {},
            onClickLogin = { },
            onViewDetail = { },
            navigationIcon = { BackNavigationIconButton({ }) },
        )
    }
}
