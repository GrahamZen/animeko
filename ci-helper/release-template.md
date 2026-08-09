[//]: # (ANI-SERVER-MAGIC-SEPARATOR)

[//]: # (注意: api server 依赖这个特殊分隔符)

[//]: # (对于所有可用的变量列表, 参考 CI release.yml 的 step release-notes)

[github-win-x64]: https://github.com/GrahamZen/animeko/releases/download/$GIT_TAG/ani-$TAG_VERSION-windows-x86_64.zip

[github-mac-x64]: https://github.com/GrahamZen/animeko/releases/download/$GIT_TAG/ani-$TAG_VERSION-macos-x86_64.dmg

[github-mac-aarch64]: https://github.com/GrahamZen/animeko/releases/download/$GIT_TAG/ani-$TAG_VERSION-macos-aarch64.dmg

[github-android]: https://github.com/GrahamZen/animeko/releases/download/$GIT_TAG/ani-$TAG_VERSION-universal.apk

[github-android-arm64-v8a]: https://github.com/GrahamZen/animeko/releases/download/$GIT_TAG/ani-$TAG_VERSION-arm64-v8a.apk

[github-android-armeabi-v7a]: https://github.com/GrahamZen/animeko/releases/download/$GIT_TAG/ani-$TAG_VERSION-armeabi-v7a.apk

[github-android-x86_64]: https://github.com/GrahamZen/animeko/releases/download/$GIT_TAG/ani-$TAG_VERSION-x86_64.apk

[cf-win-x64]: https://d.myani.org/$GIT_TAG/ani-$TAG_VERSION-windows-x86_64.zip

[cf-linux-x64]: https://d.myani.org/$GIT_TAG/ani-$TAG_VERSION-linux-x86_64.appimage

[cf-mac-x64]: https://d.myani.org/$GIT_TAG/ani-$TAG_VERSION-macos-x86_64.zip

[cf-mac-aarch64]: https://d.myani.org/$GIT_TAG/ani-$TAG_VERSION-macos-aarch64.dmg

[cf-ios]: https://d.myani.org/$GIT_TAG/ani-$TAG_VERSION.ipa

[cf-android]: https://d.myani.org/$GIT_TAG/ani-$TAG_VERSION-universal.apk

[cf-android-arm64-v8a]: https://d.myani.org/$GIT_TAG/ani-$TAG_VERSION-arm64-v8a.apk

[cf-android-armeabi-v7a]: https://d.myani.org/$GIT_TAG/ani-$TAG_VERSION-armeabi-v7a.apk

[cf-android-x86_64]: https://d.myani.org/$GIT_TAG/ani-$TAG_VERSION-x86_64.apk

[ghproxy-win-x64]: https://ghfast.top/?q=https%3A%2F%2Fgithub.com%2FGrahamZen%2Fanimeko%2Freleases%2Fdownload%2F$GIT_TAG%2Fani-$TAG_VERSION-windows-x86_64.zip

[ghproxy-mac-x64]: https://ghfast.top/?q=https%3A%2F%2Fgithub.com%2FGrahamZen%2Fanimeko%2Freleases%2Fdownload%2F$GIT_TAG%2Fani-$TAG_VERSION-macos-x86_64.zip

[ghproxy-linux-x64]: https://ghfast.top/?q=https%3A%2F%2Fgithub.com%2FGrahamZen%2Fanimeko%2Freleases%2Fdownload%2F$GIT_TAG%2Fani-$TAG_VERSION-linux-x86_64.appimage

[ghproxy-mac-aarch64]: https://ghfast.top/?q=https%3A%2F%2Fgithub.com%2FGrahamZen%2Fanimeko%2Freleases%2Fdownload%2F$GIT_TAG%2Fani-$TAG_VERSION-macos-aarch64.dmg

[ghproxy-ios]: https://ghfast.top/?q=https%3A%2F%2Fgithub.com%2FGrahamZen%2Fanimeko%2Freleases%2Fdownload%2F$GIT_TAG%2Fani-$TAG_VERSION.ipa

[ghproxy-android]: https://ghfast.top/?q=https%3A%2F%2Fgithub.com%2FGrahamZen%2Fanimeko%2Freleases%2Fdownload%2F$GIT_TAG%2Fani-$TAG_VERSION-universal.apk

[ghproxy-android-arm64-v8a]: https://ghfast.top/?q=https%3A%2F%2Fgithub.com%2FGrahamZen%2Fanimeko%2Freleases%2Fdownload%2F$GIT_TAG%2Fani-$TAG_VERSION-arm64-v8a.apk

[ghproxy-android-armeabi-v7a]: https://ghfast.top/?q=https%3A%2F%2Fgithub.com%2FGrahamZen%2Fanimeko%2Freleases%2Fdownload%2F$GIT_TAG%2Fani-$TAG_VERSION-armeabi-v7a.apk

[ghproxy-android-x86_64]: https://ghfast.top/?q=https%3A%2F%2Fgithub.com%2FGrahamZen%2Fanimeko%2Freleases%2Fdownload%2F$GIT_TAG%2Fani-$TAG_VERSION-x86_64.apk

[macOS 无法打开解决方案]: https://myani.org/wiki/macos-unable-to-open

[Windows下字体与背景颜色异常解决方案]: https://myani.org/wiki/windows-font-bg-color-issue

[Linux 安装说明]: https://myani.org/wiki/linux-install

[macOS Intel芯片版本安装教程]: https://myani.org/wiki/macos-intel-install


[iOS 自签]: https://myani.org/wiki/ios-install

## 下载

[//]: # (@formatter:off  因为"版本"前面不能换行)

优先下载与自己设备架构对应的安装包，体积更小、更省存储；不确定或装不上时再用 `universal`（包含全部架构，体积最大）。

[//]: # (@formatter:on)

| 处理器架构                | 适用于               | 下载                                                                                                      |
|---------------------|-------------------|---------------------------------------------------------------------------------------------------------|
| arm64-v8a (64 位, 推荐) | 几乎所有电视与电视盒子       | [GitHub][github-android-arm64-v8a]       |
| armeabi-v7a (32 位)   | 旧电视盒子             | [GitHub][github-android-armeabi-v7a] |
| x86_64              | x86 电视盒子及模拟器      | [GitHub][github-android-x86_64]                |
| universal           | 所有设备（不确定架构时选这个）   | [GitHub][github-android]                |

[github-android-qr]: https://github.com/GrahamZen/animeko/releases/download/$GIT_TAG/ani-$TAG_VERSION-universal.apk.github.qrcode.png

## 本次更新

更新提示里的「查看详情」按钮改为在**应用内**弹出完整更新内容。评论终于能好好看了（表情、图片、完整评论与回复），退出播放页也不再丢掉正在加载的数据源。

### 评论

* 评论里的 bangumi 表情显示成图片，以前只看到 `(bgm38)` 这样的代码。别人给评论贴的表情也会画出来。
* 图床图片与部分番剧封面加载不出来的问题修好了，GIF 也会动了；失效的图片明说「已失效」。
* 评论面板上选中一条按确定看完整内容，左右键翻上一条／下一条。打开面板不会暂停播放。

### 播放

* 退出播放页后播放状态默认保留：数据源、已解析好的播放流、播放进度都还在，侧边栏多出「正在播放」入口，点它秒回原处。等源要十几秒时可以先退出去翻别的，后台加载好或者卡住了都会提示一声。
* 长按左右键挪进度改成越按越快，长片子里挪几分钟不用连按。
* 电视上「跳过 OP/ED」的提示不再是左下角那张浅色卡片，改成进度条上方那排按钮最右边的一颗：不挡画面，跟着进度条一起收放。它一出现焦点就落在上面，按确定即可取消跳过（或跳过），不想理它就把焦点移开，其余按键该干嘛干嘛。

### 界面

* 播放器上的各层压暗遮罩整体调亮：唤出进度条时屏幕下方不再是一片纯黑，完整评论弹窗周边、内嵌详情页底部也都能透出画面。
* 详情页背景图往下保留得更多：选集卡片及其下方不再是一整片近黑，与上方有图的部分也没有明显分界了。
* 更新提示里的「查看详情」改为在**应用内**弹出完整更新内容，弹窗底部仍留了跳转到发布页的按钮。

### 设置

* 新增「退出播放页后保留播放状态」（默认开），关掉即恢复原来的行为：退出即结束播放，每次进播放页重新搜源。
* 设置页做了TV端优化。
* 移除电视上的「倍速范围」设置：遥控器上根本调不动，还会顺带改掉常驻倍速与长按倍速。范围固定为 0.25x–4x。
* 「跳过 OP 和 ED」由开关改成三选一：自动跳过（跳之前给几秒反悔）／显示跳过按钮（不自动跳，OP、ED 期间给一颗按钮由你决定）／不跳过。原来关掉自动跳过的人升级后仍是「不跳过」。

### 修复

* 修复进播放器显示假倍速（写着 1.25x 实际是原速）；播放器里调的倍速现在也会记住。
* 修复电视上设置页点「分享日志文件」直接闪退。电视上这项改成了「导出当日日志文件」，会弹出系统文件选择器让你挑保存位置——插着 U 盘就能直接存进 U 盘。
* 点了跳过 OP（或自动跳过）之后，画面不再被拉回上次的播放进度。（可能还有bug，欢迎反馈）
* 「选择数据源后自动关闭弹窗」以前没接线，怎么设置都是选完即关。现在默认（关）会留在选择器里，开了才会自动关。
* 详情页长按选集卡片标记看过后，那一排卡片不再先窜到下一集又滑回来。
* 发送弹幕的输入框失焦后自动收起，不再挡着画面。
* 倍速播放时遇到 OP、ED 常常不跳过（提示自己消失，看着像被误取消了）。
* OP、ED 的提示现在每次都会给：拖回片头重看时也会重新提示，不再只有头一次遇到才有。
* 电视上长按左右键不再先快进／快退一次再进入拖动预览，按住就直接开始拖。
* 网页数据源上单集作品（剧场版等）不再因为集号解析失败而搜不到资源。

### 同步上游

* 剧集评论改由服务端合并后下发，连续翻页不会再出现重复的评论；Bangumi 来源的评论变为只读。
* Jellyfin／Emby 数据源支持用用户名和密码登录，不必再手填 API key。
* 修复部分设备上点了更新装不上（交给第三方安装器时没把安装包的读取权限一起给）。

> Android TV 遥控器使用说明、系统版本要求与已知问题，请见仓库 [README 的「📺 Android TV 版说明」](https://github.com/GrahamZen/animeko#-android-tv-版说明)。
