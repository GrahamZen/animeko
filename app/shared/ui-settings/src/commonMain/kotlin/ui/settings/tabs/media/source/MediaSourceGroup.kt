/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.settings.tabs.media.source

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.him188.ani.app.domain.mediasource.rss.RssMediaSource
import me.him188.ani.app.domain.mediasource.web.SelectorMediaSource
import me.him188.ani.app.navigation.LocalNavigator
import me.him188.ani.app.ui.foundation.LocalPlatform
import me.him188.ani.app.ui.foundation.animation.AniAnimatedVisibility
import me.him188.ani.app.ui.foundation.ifThen
import me.him188.ani.app.ui.foundation.isTv
import me.him188.ani.app.ui.foundation.interaction.onRightClickIfSupported
import me.him188.ani.app.ui.lang.Lang
import me.him188.ani.app.ui.lang.settings_media_source_add
import me.him188.ani.app.ui.lang.settings_media_source_cancel
import me.him188.ani.app.ui.lang.settings_media_source_cancel_sort
import me.him188.ani.app.ui.lang.settings_media_source_delete
import me.him188.ani.app.ui.lang.settings_media_source_delete_can_readd
import me.him188.ani.app.ui.lang.settings_media_source_delete_confirm
import me.him188.ani.app.ui.lang.settings_media_source_delete_no_config
import me.him188.ani.app.ui.lang.settings_media_source_delete_with_config
import me.him188.ani.app.ui.lang.settings_media_source_disable
import me.him188.ani.app.ui.lang.settings_media_source_disabled
import me.him188.ani.app.ui.lang.settings_media_source_edit
import me.him188.ani.app.ui.lang.settings_media_source_enable
import me.him188.ani.app.ui.lang.settings_media_source_from_subscription
import me.him188.ani.app.ui.lang.settings_media_source_list
import me.him188.ani.app.ui.lang.settings_media_source_list_description
import me.him188.ani.app.ui.lang.settings_media_source_save_sort
import me.him188.ani.app.ui.lang.settings_media_source_select_template
import me.him188.ani.app.ui.lang.settings_media_source_sort
import me.him188.ani.app.ui.lang.settings_media_source_start_test
import me.him188.ani.app.ui.lang.settings_media_source_stop_test
import me.him188.ani.app.ui.settings.framework.ConnectionTesterResultIndicator
import me.him188.ani.app.ui.settings.framework.components.SettingsScope
import me.him188.ani.app.ui.settings.framework.components.TextButtonItem
import me.him188.ani.app.ui.settings.framework.components.TextItem
import me.him188.ani.app.ui.settings.framework.rememberSorterState
import me.him188.ani.app.ui.settings.rendering.MediaSourceIcon
import me.him188.ani.app.ui.settings.rendering.MediaSourceIcons
import me.him188.ani.datasources.api.source.FactoryId
import me.him188.ani.datasources.api.source.MediaSourceInfo
import me.him188.ani.datasources.api.source.MediaSourceTier
import me.him188.ani.datasources.api.source.parameter.MediaSourceParameters
import me.him188.ani.datasources.api.source.parameter.isEmpty
import me.him188.ani.utils.platform.isMobile
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.reorderable
import org.jetbrains.compose.resources.stringResource

@Stable
internal val MediaSourcesUsingNewSettings = listOf(
    RssMediaSource.FactoryId,
    SelectorMediaSource.FactoryId,
)

@Composable
internal fun SettingsScope.MediaSourceGroup(
    state: MediaSourceGroupState,
    edit: EditMediaSourceState,
) {
    val navigator = LocalNavigator.current
    val uiScope = rememberCoroutineScope()
    var showSelectTemplate by remember { mutableStateOf(false) }
    if (showSelectTemplate) {
        // 选一个数据源来添加
        SelectMediaSourceTemplateDialog(
            templates = state.availableMediaSourceTemplates,
            onClick = { template ->
                showSelectTemplate = false

                // 一些数据源要用单独编辑页面
                when {
                    template.factoryId in MediaSourcesUsingNewSettings -> {
                        val editing = edit.startAdding(template)
                        val job = edit.confirmEdit(editing)
                        uiScope.launch {
                            job.join()
                            navigator.navigateEditMediaSource(template.factoryId, editing.editingMediaSourceId)
                        }
                        return@SelectMediaSourceTemplateDialog
                    }

                    // 旧的数据源类型, 仍然使用旧的对话框形式添加
                    template.parameters.list.isEmpty() -> {
                        // 没有参数, 直接添加
                        edit.confirmEdit(edit.startAdding(template))
                        return@SelectMediaSourceTemplateDialog
                    }

                    else -> edit.startAdding(template)
                }
            },
            onDismissRequest = { showSelectTemplate = false },
        )
    }

    edit.editMediaSourceState?.let {
        // 准备添加这个数据源, 需要配置
        // TODO: replace with a separate page
        EditMediaSourceDialog(it, onDismissRequest = { edit.cancelEdit() })
    }

    val sorter = rememberSorterState<MediaSourcePresentation>(
        onComplete = { list -> state.reorderMediaSources(newOrder = list.map { it.instanceId }) },
    )
    val isTv = LocalPlatform.current.isTv()
    // TV: 从三点按钮长按进入排序时, 排序列表把初始焦点接到发起项上 (原列表整体消失, 焦点会丢)
    var tvSortInitialFocusId by remember { mutableStateOf<String?>(null) }

    Group(
        title = { Text(stringResource(Lang.settings_media_source_list, state.mediaSources.size)) },
        description = { Text(stringResource(Lang.settings_media_source_list_description)) },
        actions = {
            AniAnimatedVisibility(
                visible = sorter.isSorting,
            ) {
                Row {
                    IconButton({ sorter.cancel() }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(Lang.settings_media_source_cancel_sort),
                        )
                    }
                }
            }
            AniAnimatedVisibility(
                visible = !sorter.isSorting,
            ) {
                Row {
                    IconButton(
                        {
                            edit.cancelEdit()
                            showSelectTemplate = true
                        },
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(Lang.settings_media_source_add))
                    }
                }
            }
            Crossfade(sorter.isSorting, Modifier.animateContentSize()) { isSorting ->
                if (isSorting) {
                    Button(
                        {
                            sorter.complete()
                        },
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = stringResource(Lang.settings_media_source_save_sort),
                        )
                    }
                } else {
                    IconButton(
                        {
                            edit.cancelEdit()
                            sorter.start(state.mediaSources)
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = stringResource(Lang.settings_media_source_sort),
                        )
                    }
                }
            }
        },
    ) {
        Box {
            // TV 排序时不组合普通列表 (alpha 0 的隐形项仍可聚焦, 会干扰遥控器导航);
            // 指针平台保留它给覆盖层提供尺寸 (matchParentSize)
            if (!(sorter.isSorting && isTv)) {
            Column(
                Modifier
                    .ifThen(sorter.isSorting) { alpha(0f) }
                    .wrapContentHeight(),
            ) {
                state.mediaSources.forEachIndexed { index, item ->
                    if (index != 0) {
                        HorizontalDividerItem()
                    }
                    val startEditing = {
                        if (item.factoryId in MediaSourcesUsingNewSettings) {
                            navigator.navigateEditMediaSource(item.factoryId, item.instanceId)
                        } else {
                            edit.startEditing(item)
                        }
                    }
                    val platform = LocalPlatform.current

                    var showMoreDropdown by remember { mutableStateOf(false) }
                    val moreButtonFocus = remember { FocusRequester() }
                    // item 长按到阈值时立即把焦点交给三点按钮 (不等松开); 同一次按住的残余
                    // 事件 (后续连发/最终松开) 落到按钮上时全部吞掉 —— 松开后重新按住才算数
                    var swallowMoreButtonResidualPress by remember { mutableStateOf(false) }
                    var showConfirmDeletionDialog by rememberSaveable { mutableStateOf(false) }
                    if (showConfirmDeletionDialog) {
                        AlertDialog(
                            onDismissRequest = { showConfirmDeletionDialog = false },
                            icon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            title = { Text(stringResource(Lang.settings_media_source_delete)) },
                            text = {
                                if (item.parameters.isEmpty()) {
                                    Text(stringResource(Lang.settings_media_source_delete_no_config))
                                } else {
                                    Text(stringResource(Lang.settings_media_source_delete_with_config))
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    {
                                        edit.deleteMediaSource(item);
                                        showConfirmDeletionDialog = false
                                    },
                                ) {
                                    Text(
                                        stringResource(Lang.settings_media_source_delete_confirm),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    {
                                        showConfirmDeletionDialog = false
                                    },
                                ) { Text(stringResource(Lang.settings_media_source_cancel)) }
                            },
                        )
                    }

                    // TV: 完全接管确认键, 短按/长按的动作都在松开 (KeyUp) 时才派发.
                    // 不能用 combinedClickable 的长按 (按住途中就触发): 按住途中切焦点会把
                    // 按键事件劈成两半 —— item 只收到 KeyDown 收不到 KeyUp, 按压态卡死
                    // (表现为残留阴影, 且后续长按行为错乱); KeyUp 落到三点按钮上被当成点击误开菜单.
                    var itemConfirmKeyDownCount by remember { mutableStateOf(0) }
                    // item 本体是否聚焦 (不含子元素): 焦点在行内的三点按钮上时, 本行的
                    // preview 拦截器必须放行 —— 否则按钮 (子节点) 永远收不到确认键,
                    // 短按会被这里判成"编辑"直接进页面
                    var itemSelfFocused by remember { mutableStateOf(false) }
                    val itemFocus = remember { FocusRequester() }
                    MediaSourceItem(
                        item,
                        Modifier
                            .focusRequester(itemFocus)
                            .onFocusChanged {
                                itemSelfFocused = it.isFocused
                                // 长按把焦点交给按钮后, 本行收不到那次按住的 KeyUp, 计数手动清零
                                if (!it.isFocused) itemConfirmKeyDownCount = 0
                            }
                            .ifThen(isTv) {
                                onPreviewKeyEvent { event ->
                                    if (!itemSelfFocused) return@onPreviewKeyEvent false
                                    val isConfirmKey = event.key == Key.DirectionCenter ||
                                        event.key == Key.Enter || event.key == Key.NumPadEnter
                                    if (!isConfirmKey) return@onPreviewKeyEvent false
                                    when (event.type) {
                                        KeyEventType.KeyDown -> {
                                            itemConfirmKeyDownCount++
                                            // 按住到阈值立即把焦点交给三点按钮 (不等松开);
                                            // 残余事件由按钮的 swallow 保护吞掉, 不会误触排序/菜单
                                            if (itemConfirmKeyDownCount == TV_SORT_LONG_PRESS_REPEATS) {
                                                swallowMoreButtonResidualPress = true
                                                runCatching { moreButtonFocus.requestFocus() }
                                            }
                                            true
                                        }

                                        KeyEventType.KeyUp -> {
                                            // 只有未达阈值的短按会走到这里 (达阈值时焦点已交出去)
                                            val longPressed =
                                                itemConfirmKeyDownCount >= TV_SORT_LONG_PRESS_REPEATS
                                            itemConfirmKeyDownCount = 0
                                            if (!longPressed) startEditing()
                                            true
                                        }

                                        else -> false
                                    }
                                }
                            }
                            .combinedClickable(
                                onClickLabel = "编辑",
                                onLongClick = {
                                    // TV 的确认键已被上面接管, 这里只剩手机的触摸长按
                                    if (!isTv && platform.isMobile()) {
                                        sorter.start(state.mediaSources)
                                    }
                                },
                                onLongClickLabel = "开始排序",
                                onClick = startEditing,
                            ).onRightClickIfSupported {
                                showMoreDropdown = true
                            },
                    ) {
                        IconButton({}, enabled = false) { // 放在 button 里保持 padding 一致
                            ConnectionTesterResultIndicator(
                                item.connectionTester,
                                showIdle = false,
                            )
                        }

                        Box {
                            // 不用 IconButton: TV 需要自定义确认键长按 (进入排序), 且动作必须在
                            // 松开时才派发 (同 item 的处理, 否则残余按键事件会误触发排序列表);
                            // 尺寸/圆形指示与 IconButton (40dp 容器 + 24dp 图标) 一致
                            var moreConfirmKeyDownCount by remember { mutableStateOf(0) }
                            Box(
                                Modifier
                                    .focusRequester(moreButtonFocus)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .ifThen(isTv) {
                                        onPreviewKeyEvent { event ->
                                            // 返回键: 焦点退回本行 item (而不是冒泡出去退出设置页)
                                            if (event.key == Key.Back || event.key == Key.Escape) {
                                                if (event.type == KeyEventType.KeyUp) {
                                                    runCatching { itemFocus.requestFocus() }
                                                }
                                                return@onPreviewKeyEvent true
                                            }
                                            val isConfirmKey = event.key == Key.DirectionCenter ||
                                                event.key == Key.Enter || event.key == Key.NumPadEnter
                                            if (!isConfirmKey) return@onPreviewKeyEvent false
                                            // 残余按住保护: item 长按把焦点交过来时按键还没松开,
                                            // 这次按住的剩余事件全部吞掉, 松开后重新按住才算数
                                            if (swallowMoreButtonResidualPress) {
                                                if (event.type == KeyEventType.KeyUp) {
                                                    swallowMoreButtonResidualPress = false
                                                }
                                                moreConfirmKeyDownCount = 0
                                                return@onPreviewKeyEvent true
                                            }
                                            when (event.type) {
                                                KeyEventType.KeyDown -> {
                                                    moreConfirmKeyDownCount++
                                                    true
                                                }

                                                KeyEventType.KeyUp -> {
                                                    val longPressed =
                                                        moreConfirmKeyDownCount >= TV_SORT_LONG_PRESS_REPEATS
                                                    moreConfirmKeyDownCount = 0
                                                    if (longPressed) {
                                                        // 长按三点按钮: 进入遥控器排序模式
                                                        tvSortInitialFocusId = item.instanceId
                                                        edit.cancelEdit()
                                                        sorter.start(state.mediaSources)
                                                    } else {
                                                        showMoreDropdown = true
                                                    }
                                                    true
                                                }

                                                else -> false
                                            }
                                        }
                                    }
                                    .clickable(onClick = { showMoreDropdown = true }),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.MoreVert,
                                    contentDescription = "更多",
                                )
                            }

                            MoreOptionsDropdown(
                                showMoreDropdown,
                                onDismissRequest = { showMoreDropdown = false },
                                onDeleteRequest = { showConfirmDeletionDialog = true },
                                item,
                                onEnabledChange = { edit.toggleMediaSourceEnabled(item, it) },
                                onEdit = startEditing,
                            )
                        }
                    }
                }
            }
            }
            if (sorter.isSorting && isTv) {
                // TV 遥控器排序: 可聚焦列表, 按住选中 -> 上下键移动 -> 确认放下
                TvSortMediaSourceList(
                    sorter,
                    initialFocusInstanceId = tvSortInitialFocusId,
                    Modifier.wrapContentHeight(),
                )
            } else if (sorter.isSorting) {
                // 往上面再盖一层, 因为 SettingsTab 已经有 scrollable 了, LazyColumn 如果不加高度限制会出错
                LazyColumn(
                    state = sorter.listState,
                    modifier = Modifier
                        .matchParentSize()
                        .reorderable(sorter.reorderableState)
                        .detectReorderAfterLongPress(sorter.reorderableState),
                ) {
                    itemsIndexed(
                        sorter.sortingData,
                        key = { _, item -> item.instanceId },
                    ) { index, item ->
                        if (index != 0) {
                            HorizontalDividerItem()
                        }
                        ReorderableItem(sorter.reorderableState, key = item.instanceId) { isDragging ->
                            val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)
                            MediaSourceItem(
                                item,
                                Modifier
                                    .shadow(elevation.value)
                                    .background(MaterialTheme.colorScheme.surface), // match card background
                            ) {
                                Icon(
                                    Icons.Rounded.Reorder,
                                    "拖拽排序",
                                    Modifier.detectReorder(sorter.reorderableState),
                                )
                            }
                        }
                    }
                }
            } else {
                // 清空 list 状态, 否则在删除一个项目后再切换到排序状态, 有的项目会消失
                LazyColumn(Modifier.height(0.dp), sorter.listState) { }
            }
        }

        HorizontalDividerItem()


        TextButtonItem(
            onClick = {
                state.mediaSourceTesters.toggleTest()
            },
            Modifier.ifThen(sorter.isSorting) { alpha(0f) },
            enabled = !sorter.isSorting,
            title = {
                if (state.mediaSourceTesters.anyTesting) {
                    Text(stringResource(Lang.settings_media_source_stop_test))
                } else {
                    Text(stringResource(Lang.settings_media_source_start_test))
                }
            },
        )
    }
}


private const val DISABLED_ALPHA = 0.38f

@Composable
internal fun SettingsScope.MediaSourceItem(
    item: MediaSourcePresentation,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = item.isEnabled,
    actions: @Composable RowScope.() -> Unit,
) {
//    ListItem(
//        headlineContent = title,
//        leadingContent = icon?.let { { it() } },
//        supportingContent = description,
//        trailingContent = action,
//    )
    TextItem(
        modifier = modifier,
        description = {
            SelectionContainer {
                val fromSubscriptionText = stringResource(Lang.settings_media_source_from_subscription)
                Text(
                    remember(item, fromSubscriptionText) {
                        buildString {
                            val desc = item.info.description.orEmpty()
                            val subUrl = item.ownerSubscriptionUrl
                            if (subUrl != null) {
                                if (desc.isNotBlank()) {
                                    appendLine(desc)
                                }
                                append(fromSubscriptionText)
                                append(subUrl)
                            } else {
                                append(desc)
                            }
                        }
                    },
                    Modifier.ifThen(!isEnabled) { alpha(DISABLED_ALPHA) },
                )
            }
        },
        icon = {
            Box(
                Modifier.ifThen(!isEnabled) { alpha(DISABLED_ALPHA) }.clip(MaterialTheme.shapes.extraSmall).size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                MediaSourceIcon(item.info, Modifier.size(48.dp))
            }
        },
        action = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                actions()
            }
        },
        title = {
            val disabledText = stringResource(Lang.settings_media_source_disabled)
            val name = if (!isEnabled) {
                item.info.displayName + disabledText
            } else {
                item.info.displayName
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item.instance.source.apply {
                    Icon(
                        imageVector = MediaSourceIcons.location(this.location, this.kind),
                        contentDescription = this.info.description,
                        modifier = Modifier.size(20.dp).ifThen(!isEnabled) { alpha(DISABLED_ALPHA) },
                    )
                }
                Text(
                    name,
                    Modifier.ifThen(!isEnabled) { alpha(DISABLED_ALPHA) }.basicMarquee(),
                    textAlign = TextAlign.Center,
                )
                item.info.tier?.let { tier ->
                    MediaSourceTierTag(
                        tier = tier,
                        modifier = Modifier.ifThen(!isEnabled) { alpha(DISABLED_ALPHA) },
                    )
                }
            }
        },
    )
}

@Composable
private fun MediaSourceTierTag(
    tier: MediaSourceTier,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = "T${tier.value}",
            modifier = Modifier.wrapContentSize().padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            softWrap = false,
        )
    }
}

@Composable
private fun MoreOptionsDropdown(
    showMore: Boolean,
    onDismissRequest: () -> Unit,
    onDeleteRequest: () -> Unit,
    item: MediaSourcePresentation,
    onEnabledChange: (enabled: Boolean) -> Unit,
    onEdit: () -> Unit,
) {
    DropdownMenu(
        expanded = showMore,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuItem(
            leadingIcon = {
                if (item.isEnabled) {
                    Icon(Icons.Rounded.VisibilityOff, null)
                } else {
                    Icon(Icons.Rounded.Visibility, null)
                }
            },
            text = {
                if (item.isEnabled) {
                    Text(stringResource(Lang.settings_media_source_disable))
                } else {
                    Text(stringResource(Lang.settings_media_source_enable))
                }
            },
            onClick = {
                onEnabledChange(!item.isEnabled)
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
            text = { Text(stringResource(Lang.settings_media_source_edit)) }, // 直接点击数据源一行也可以编辑, 但还是在这里放一个按钮以免有人不知道
            onClick = {
                onEdit()
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
            text = {
                Text(
                    stringResource(Lang.settings_media_source_delete_can_readd),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            onClick = {
                onDeleteRequest()
                onDismissRequest()
            },
        )
    }
}

@Composable
internal fun SelectMediaSourceTemplateDialog(
    templates: List<MediaSourceTemplate>,
    onClick: (MediaSourceTemplate) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(stringResource(Lang.settings_media_source_select_template))
        },
        confirmButton = {
            TextButton(onDismissRequest) {
                Text(stringResource(Lang.settings_media_source_cancel))
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column {
                if (scrollState.canScrollBackward) {
                    HorizontalDivider()
                }
                Column(
                    Modifier.verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    templates.forEach { item ->
                        MediaSourceCard(
                            onClick = { onClick(item) },
                            title = {
                                Text(
                                    item.info.displayName,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            },
                            Modifier,
                            icon = {
                                Box(Modifier.clip(MaterialTheme.shapes.extraSmall).size(48.dp)) {
                                    MediaSourceIcon(item.info, Modifier.size(48.dp))
                                }
                            },
                            content = {
                                item.info.description?.let {
                                    Text(it)
                                }
                            },
                        )
                    }
                }
                if (scrollState.canScrollForward) {
                    HorizontalDivider()
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun MediaSourceCard(
    onClick: () -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    ListItem(
        headlineContent = title,
        modifier.clickable(onClick = onClick),
        leadingContent = icon?.let {
            {
                Box(Modifier.wrapContentSize().size(24.dp), contentAlignment = Alignment.Center) {
                    it()
                }
            }
        },
        supportingContent = content,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Preview
@Composable
private fun PreviewSelectMediaSourceTemplateDialog() {
    SelectMediaSourceTemplateDialog(
        templates = listOf(
            MediaSourceTemplate(
                factoryId = FactoryId("1"),
                info = MediaSourceInfo("Test"),
                parameters = MediaSourceParameters.Empty,
            ),
            MediaSourceTemplate(
                factoryId = FactoryId("123"),
                info = MediaSourceInfo("Test2"),
                parameters = MediaSourceParameters.Empty,
            ),
        ),
        onClick = {},
        onDismissRequest = {},
    )
}
