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

电视端的卡片导航整体改成「聚焦框不动、卡片滑动」，长按方向键也不再越按越快。「跳过 OP/ED」从左下角的卡片搬到了进度条上方那排按钮里，还能选择只给按钮、不自动跳。电视上可以在播放器里发表本集评论并插入表情。

### 播放

* 电视上「跳过 OP/ED」改成进度条上方那排按钮最右边的一颗：不挡画面，跟着进度条一起收放。它一出现焦点就落在上面，按确定即可取消跳过（或跳过）。

### 界面

* 电视上的卡片导航改成「聚焦框不动、卡片滑动」。
* 电视上长按方向键不会越按越快了。
* 新番时间表在日期上左右导航时，下面的卡片不再一屏一屏地闪。
* 电视上按手机版式做的那几排横滑卡片（详情页的角色，人物页的出演角色、参演作品，以及关联条目）也换成了同一套：焦点从上方进来或在行内移动时，卡片不再横着跳一小段。这些卡片的聚焦效果同时从压在封面上的半透明高亮改成主题色描边，一眼看得出焦点在哪张。
* 电视上「一起看」的入口从进度条上方那排移到了下方的圆钮里（弹幕开关、弹幕设置之后），与其它功能按钮同一形态。
* 详情页背景图往下保留得更多：选集卡片及其下方不再是一整片近黑，与上方有图的部分也没有明显分界了。

### 设置

* 「跳过 OP 和 ED」由开关改成三选一：自动跳过（跳之前给几秒反悔）／显示跳过按钮（不自动跳，OP、ED 期间给一颗按钮由你决定）／不跳过。原来关掉自动跳过的人升级后仍是「不跳过」。

### 修复

* 修复电视上设置页点「分享日志文件」直接闪退。电视上这项改成了「导出当日日志文件」，会弹出系统文件选择器让你挑保存位置——插着 U 盘就能直接存进 U 盘。
* 探索页「继续观看」栏改了收藏（标记看过、抛弃等）之后卡片一直不更新，只能重启应用才恢复。顺带这一栏刷新的条数与显示的条数对齐了，后面那些条目也能拿到新播出的剧集（该功能需长期使用才可验证，如遇到bug可以提issue来反馈）。
* 下一集是特别篇（SP、OVA 等）时「下一集」按钮不再消失，也会正常自动连播：这类剧集在 bangumi 上常常没有播出日期，之前被一律当成「还没开播」挡掉了。
* 详情页长按选集卡片标记看过后，那一排卡片不再先窜到下一集又滑回来。
* 倍速播放时遇到 OP、ED 常常不跳过（提示自己消失，看着像被误取消了）。
* OP、ED 的提示现在每次都会给：拖回片头重看时也会重新提示，不再只有头一次遇到才有。
* 电视上长按左右键不再先快进／快退一次再进入拖动预览，按住就直接开始拖。

> Android TV 遥控器使用说明、系统版本要求与已知问题，请见仓库 [README 的「📺 Android TV 版说明」](https://github.com/GrahamZen/animeko#-android-tv-版说明)。
