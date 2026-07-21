# Fork 维护指南 (Android TV)

本仓库是 [open-ani/animeko](https://github.com/open-ani/animeko) 的 Android TV fork。
本文档记录 fork 与上游的全部差异及其原因,供 rebase 上游时逐条核对。**每次 rebase 或新增改动后请更新本文档。**

## 代码组织约定

为了把 rebase 冲突面压到最小,fork 改动遵循以下优先级:

1. **纯新增文件**(首选): TV 页面/组件放独立文件,文件名带 `Tv` 前缀,与被替换的上游实现同 package
   (可直接访问 `internal` 成员,外部 import 路径不变)。上游文件里只留 `if (isTv)` 入口分支。
2. **散点适配**: 焦点/按键处理尽量收敛成 `TvInputModifiers.kt` / `TvFocusHelpers.kt` 里的扩展函数,
   调用点一行接入。**收敛已有内联逻辑时禁止"顺手统一"细微语义**(`onPreviewKeyEvent` vs `onKeyEvent`、
   KeyDown vs KeyUp 等差异都是调试出来的),改完必须真机验证对应页面。
3. **不可避免的行内修改**(下表逐个登记): 上游函数内部的语义性修改,rebase 冲突时需重新理解意图。
4. **平台无关的 bug 修复优先提上游 PR**,合并后 fork 的对应补丁在下次 rebase 时自然消失。

不拆独立 `ui-tv` Gradle module: TV 页面深度依赖各 `ui-*` module 的 internal 状态类,拆出去反而制造 API 摩擦面。

## Rebase 工作流

- `git rerere` 已启用 (`rerere.enabled` + `rerere.autoupdate`),重复冲突自动重放上次解法。
- fork 提交按功能归组(fixup/autosquash 进对应提交),单 commit 冲突面小;新改动用
  `git commit --fixup <hash>` + `GIT_SEQUENCE_EDITOR=: git rebase -i --autosquash <hash>^`。
- 上游整文件重构(如 captcha 重写、页面 redesign)时,不要硬合文本: 先接受上游版本,
  再按本文档记录的"改动原因"在新架构上重新实现。
- rebase 后验证: `./gradlew :app:android:compileDefaultDebugKotlin` 通过,再按下表分区在 TV 真机抽测。

## 一、fork 新增文件(rebase 零冲突)

| 文件 | 用途 |
|---|---|
| `app/android/src/main/kotlin/tv/AniDreamService.kt` | TV 屏保 (Daydream) |
| `app/android/src/main/kotlin/tv/TvHomeChannels.kt` | TV 桌面频道/继续观看 |
| `app-data .../network/TmdbImageService.kt` | TMDB 横版背景图/剧集缩略图 + DataStore 缓存 |
| `app-data .../network/BangumiSummaryService.kt` | bgm.tv 直连简介兜底 (Ani 服务器无数据时) |
| `app-data .../torrent/service/TorrentDiagnosticsServer.kt` | debug 构建 localhost 种子诊断端口 |
| `app-data .../mediasource/ChineseConverter.kt` (+各平台 actual) | 简繁转换 (中文条目匹配修复的一部分) |
| `app-platform .../ui/foundation/TVPlatform.kt` (+各平台 actual) | `Platform.isTv()` 检测 |
| `app-platform .../navigation/MainPageRequest.kt` | `requestMainPage`: 弹回主页时经 SavedStateHandle 切 tab |
| `app/shared .../ui/main/TvMainScreenLayout.kt` | TV 主页外壳 (侧边栏 + 内容区 focusGroup) |
| `ui-foundation .../TvConstants.kt` / `TvFocusHelpers.kt` / `TvInputModifiers.kt` | TV 共享常量、焦点工具、按键 Modifier |
| `ui-foundation .../session/TvNavigationSideRail.kt` | TV 可展开侧边导航栏 (主页+详情页共用) |
| `ui-foundation .../widgets/AniBottomSheetDefaults.kt` | TV bottom sheet 统一样式 |
| `ui-exploration .../TvExplorationPage.kt` | TV 沉浸式探索页 (hero 背景 + 轮播) |
| `ui-subject .../details/layout/SubjectDetailsTvPage.kt` | TV 条目详情页 (全屏背景 + 选集轮播) |
| `ui-subject .../details/sections/TvEpisodesSection.kt` | TV 选集轮播/网格卡片组件 (从 EpisodesSection 拆出) |
| `ui-settings .../tabs/media/source/TvSortMediaSourceList.kt` | TV 遥控器数据源排序列表 (从 MediaSourceGroup 拆出) |
| `.github/workflows/fork-release.yml` | fork 专属 Android release 流程 (与上游 release.yml 隔离) |
| `.github/workflows/notify-mirror.yml` | release 发布后通知镜像仓库 |

## 二、修改的上游文件及原因

### 主壳 / 导航 / 主题

| 文件 | 原因 |
|---|---|
| `ui/main/MainScreen.kt` | TV 分支入口 (调 `TvMainScreenLayout`);双击导航滚顶;TV 下各页顶栏隐藏参数 |
| `ui/main/AniAppContent.kt` | TV 全局焦点兜底循环;观察 `MAIN_REQUESTED_PAGE_KEY` 切 tab |
| `ui/main/AniApp.kt` | TV 拦截未消费 BACK 键 (框架会映射成 FocusDirection.Exit 抢焦点) |
| `navigation/AniNavigator.jvm.kt` / `.ios.kt` | `popBackOrNavigateToMain` 弹回前调 `requestMainPage` |
| `ui/foundation/theme/AppTheme.kt` | `tvPageBackgroundColor`;TV/桌面焦点指示 (LocalIndication) |
| `animation/AniMotionScheme.kt` / `NavigationMotionScheme.kt` | TV 动效调整 |
| `activity/MainActivity.kt`、`AndroidManifest.xml`、`app/android/build.gradle.kts` | TV launcher/banner/权限/签名配置 |
| `platform/AniBuildConfig.kt` | `tmdbApiToken` 配置项 |
| `platform/CommonKoinModule.kt` | 注册 TmdbImageService / BangumiSummaryService;数据库迁移注册 |
| `data/persistent/SettingsStore.kt` | tmdbImageCacheStore |
| `data/models/preference/ThemeSettings.kt`、`ui/settings/tabs/theme/ThemePreferences.kt` | TV 沉浸式探索页开关 |

### TV 焦点/按键散点适配 (改动小、模式统一)

`ExplorationScreen` `ScheduleScreen` `SearchFilter` `SearchPageResultColumn` `SuggestionSearchBarState`
`TrendingSubjectsCarousel` `CollectionPage` `SubjectCollectionsColumn` `EditCollectionTypeDropDown`
`EditableSubjectCollectionTypeButton` `SubjectCollectionTypeButton` `SubjectProgressButton`
`EpisodeListItem` `PeopleDetailsPage` `PeoplePreview` `RelatedSubjectsRow` `SubjectDetailsSections`
`SubjectPeopleSections` `EpisodesSection` `CommentColumn` `SliderItem` `SorterState` `AppSettingsTab`
`ProfilePopup` `ProfilePopupLayout` `SettingsScreen` `WelcomeScreen` `TopAppBarGoBackButton`
`MediaSelectorFilters` `MediaSelectorView` `MediaSourceResultsView` `CacheManagementScreen`
`CacheGroupDetailsPage` `EpisodeGrid` `PaginatedEpisodeList` `EpisodeListSection` `EpisodeDetails`
— 均为: 可聚焦化、D-pad 键处理、TV 下隐藏顶栏/调整形状背景、焦点恢复。逐个 hook 很小;冲突时按语义重加。

### 行内语义性修改 (rebase 冲突时需重点理解)

| 文件 | 原因 |
|---|---|
| `ui/subject/episode/EpisodeVideo.kt` `EpisodePage.kt` `EpisodeViewModel.kt` | TV 遥控器播放控制 (确认键播放/暂停、方向键快进等) 与上游手势/OP-ED 跳过共存 |
| `video-player .../PlayerControllerBar.kt` `VideoScaffold.kt` `VideoSideSheets.kt` `GestureLock.kt` `PlayerGestureHost.kt` `AudioSwitcher.kt` `SubtitleSwitcher.kt` | 播放器控件 TV 聚焦/键控;`keepLayoutWhenHidden` 保持布局 |
| `EpisodeVideoSideSheet` `EpisodeVideoSettings` `EpisodeSelectorSideSheet` `EpisodeVideoMediaSelectorSideSheet` | 播放器侧边抽屉 TV 导航 |
| `ui-cache .../subject/CacheListGroup.kt` | `EpisodeCacheActionIcon` 重写: 节点稳定 + 事件驱动焦点夺回 (LocalCacheFocusOwner) |
| `ui-settings .../tabs/media/source/MediaSourceGroup.kt` | item/三点按钮完全接管确认键 (KeyUp 派发, 长按进排序);TV 排序模式切换 |
| `ui-subject .../details/*` (SubjectDetailsPage/State/StateFactory/StateLoader/MultiColumnPage/LayoutParams) | TV 详情页数据流: TMDB/bgm 简介兜底链、背景图、`MultiColumnScaffold`/`SubjectRelatedBlock` 改 internal 供 TV 页复用 |
| `ui-settings .../update/UpdateChecker.kt` `UpdateNotifierHost.kt` | 更新源指向本 fork release;通知行为替换 |
| `tools/update/AndroidUpdateInstaller.kt` | FileProvider 安装 APK |
| `app-data .../captcha/CaptchaBrowser.kt` `InteractiveSolveDialog.kt` `WebViewCaptchaBrowser.kt` (+desktop/test 签名同步) | TV 虚拟光标过验证码: `View(onExitRequest, onConfirmRequest)` 默认参数扩展 |

### 平台无关修复 (上游 PR 候选 — 合并后从本表删除)

| 文件 | 修复 |
|---|---|
| `TorrentCacheInfoDao.kt` (+Dao 测试) `AniDatabase.kt` `TorrentMediaCacheEngine.kt` `AbstractMediaCacheEngine.kt` `TorrentMediaCacheStorage.kt` `TorrentMediaResolver.kt` (+SelectVideoFile 测试) | 种子按集存储文件选择,修多集合集选错文件/S00 特别篇撞集号 |
| `ui-cache` 删除竞态 / 启动时已完成缓存不显示 | 缓存列表 bug |
| `GetSubjectRecommendationFlowUseCase.kt` | 过滤服务器注入的广告条目 |
| `SubjectCollectionRepository.kt` `MediaListFilters.kt` `LabelFirstRawTitleParser.kt` (+测试) + ChineseConverter | 中文条目名/别名匹配、标题解析 |
| `BaseJellyfinMediaSource.kt` | Jellyfin 外挂字幕流 |
| `AniTorrentService.kt` `TorrentServiceConnectionManager.kt` | 种子服务修复/诊断 |

## 三、TV 真机验证分区 (rebase 后抽测)

1. 主页: 侧边栏展开/收起、切 tab、返回键回探索页、头像动作
2. 探索页: hero 轮播、卡片导航、按返回退出
3. 详情页: 选集轮播/网格弹层、简介兜底 (找一个 Ani 服务器无简介的条目)、侧边栏遮罩
4. 播放器: 遥控器播放暂停/快进、选集/弹幕/媒体源侧边抽屉、OP/ED 跳过
5. 缓存页: 列表操作按钮焦点不丢、多选工具栏、删除后不复活
6. 设置: 滑条上下键离开、数据源长按排序、验证码 WebView 虚拟光标
7. 更新: 检查更新指向 fork release、下载安装
