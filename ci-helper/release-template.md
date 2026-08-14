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

尝试修复BT一直不下载的问题。优化进详情页的过渡；退出播放页后保留的会话，提示除了弹条消息还会响一声，音色可以选也可以关。

### 界面

* 点卡片进详情页落地即有背景图和标题，不再是一页转圈。
* 低端电视上连续左右移动卡片更跟手：连续导航时背景图与简介不再每格都跟着换，停下来才换一次（单次按键仍然立刻换）。想要每格都换可以在设置-主题里打开「完整视觉效果」。
* 电视上选集卡片的底部重排过（几何参照 Prime Video）：进度条更粗更长、离卡片边缘更远，不再和聚焦描边压在一起；没有观看进度的卡片文字不再空出一条的高度，纯文字卡片的文字左缘也与有剧照的对齐了。
* 首页、追番、搜索里的竖版卡片跟着改成同一套：进度条粗细一致、两端与卡片圆角对齐，位置也更贴近底边；聚焦描边细了一档、更贴近封面。

### 设置

* 后台会话的提示除了弹条消息还会响一声，音色可选，也可以关掉。
* 「跳过 OP 和 ED」多了一档「自动跳过，取消后保留跳过按钮」：按了取消之后这一段还没放完，仍然可以补跳（跳过按钮目前只有电视端有）。

### 修复

* 电视上打开完整评论、数据源选择、设置等盖在画面上的弹窗时，遥控器的播放/暂停键按不动；现在除了跳转到新页面（本来就会暂停），任何浮层开着都能直接暂停或继续播放。
* 电视上从详情页或播放器点开人物、关联条目进入新页面，返回时会退回页面顶部：现在回到离开前的位置（那张卡片、那个区块），人物预览弹窗也会跟着回来；播放器返回后不再是「组件全隐藏」，而是原来展开的那个面板。
* 刚装好或刚开机时，订阅更新会赶在网络就绪之前失败一次，然后一小时内不再重试，表现为「搜不到数据源，只能走磁力」；现在失败后会很快重试。
* 部分电视盒子自带的系统组件会顶掉应用内的网页解析库，导致所有在线（WEB）数据源一律搜索失败，只剩 BT 源。
* 手机和桌面上的人物卡、关联作品卡出现了电视端的焦点描边。
* 长按倍速松手后倍速有时残留：界面显示原速，实际还在快放。
* 换过几个 WEB 数据源后应用越用越卡。
* 进详情页时侧边栏按钮的文字闪一下就没。

### 同步上游

* 播放器换到新的状态模型（mediamp 0.3.0）、PikPak 支持通过 HTTPS 显式缓存 BT，以及 Kotlin 2.4.10 / Compose Multiplatform 1.11.1。

> Android TV 遥控器使用说明、系统版本要求与已知问题，请见仓库 [README 的「📺 Android TV 版说明」](https://github.com/GrahamZen/animeko#-android-tv-版说明)。
