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

非常建议更新。性能优化与滚动手感改进，重新设计新番时间表（提供了切换旧版时间表的选项）。

### 性能

* 大幅降低电视页面的渲染开销。低端设备遥控器操作更跟手。

### 界面

* 优化卡片滚动效果。
* 长按选集卡片弹出的详情里，左右键可直接翻到相邻集。
* 播放器左上角显示正在播放的数据源（图标和名称）。
* 播放器浮出面板（弹幕、评论、推荐等）高度改为按屏幕自适应，大屏一次能看到更多条目。
* 播放器上的各层压暗遮罩整体调亮：唤出进度条时屏幕下方不再压成一片纯黑，完整评论弹窗周边、内嵌详情页（尤其底部）也都能透出画面。
* 内嵌详情页底部的两条评论、长按选集卡片弹出的详情弹窗也一并调亮：评论卡不再是实心黑砖，弹窗周边与剧照上的压暗都放松了，画面和剧照透得出来。

* 退出播放页后播放状态默认保留：数据源搜索、已解析好的播放流、播放进度都还在，侧边栏多出一个「正在播放」入口，点它秒回原处（焦点落在该条目上时，下方浮出「关闭」按钮）。等数据源要十几秒时不必干等——退出去翻别的，后台加载好会提示一声；后台卡住了也会提示（加载失败、播放器打不开该源、所有源都没搜到、需要手动选源），不用盯着等一个不会来的就绪提示。同一时刻只保留一个，点开另一集即替换；离开播放页即暂停，不会在后台出声（在「一起看」里跟随房主时例外：播放与暂停归房主，退出播放页后仍与房主同步，和待在播放页时一样）。可在设置里关掉。

### 设置

* 新增「完整视觉效果」开关（合并原「完整过渡动画」）：分类切换的滑动过渡、加载占位闪烁动画、长标题持续滚动、背景剧照原图。默认关闭以保证低端设备流畅，设备性能充裕可开启。
* 新增「界面缩放」：部分电视上报的屏幕密度不准，导致界面过大或过小，可用此项校正。
* 新增「退出播放页后保留播放状态」（默认开）：关掉则恢复原来的行为，退出即结束播放，每次进播放页重新搜索数据源。

### 修复

* 设置页左右两栏互相记住焦点停留位置：从右侧按左键回到左栏上次停留的项（首次回到当前选中分类），从左栏按右键回到右侧上次停留的设置项；滑块上按返回键也可回到左栏。同时移除了电视上无法使用的分栏宽度拖动把手，右侧设置项不再套一层圆角卡片，直接铺在页面背景上。
* 播放器里调倍速现在会记住（与手机端一致）：之前电视上改完不写回设置，切集或重新起播就被弹回原值，倍速滑块用的也不是设置里的倍速范围。
* 后台「已加载好」的提示改为等真的开播了才发，回去就能直接看：以前拿到播放地址就提示，而播放器还要取容器头、建解码器、缓冲首帧（慢的源要十几秒），回去仍是黑屏、进度条右边还是 0:00。同时修掉一处抢资源：回到播放页时进度条缩略图的预热会立刻另起一路解码去读同一个远程文件（它假设播放位置附近的数据一定已缓冲好，而此时播放器才刚开始取文件头），现在改为等起播之后再预热。
* 退出播放页后台继续加载数据源时不再报「加载失败：未知错误」：网页数据源解析要靠界面挂载的 WebView，播放页一销毁就解析不了，会把所有源试一遍全部失败。
* 手动匹配的弹幕源不再因改设置而丢失，弹幕时间轴调整立即生效。
* Toast 配色跟随应用主题。
* 追番页封面不再反复变回灰色占位闪烁。
* 卡片封面/剧照加载失败后自动重试，不再一直空着。

> Android TV 遥控器使用说明、系统版本要求与已知问题，请见仓库 [README 的「📺 Android TV 版说明」](https://github.com/GrahamZen/animeko#-android-tv-版说明)。
