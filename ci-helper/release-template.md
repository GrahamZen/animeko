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

[github-android-legacy-arm64-v8a]: https://github.com/GrahamZen/animeko/releases/download/$GIT_TAG/ani-$TAG_VERSION-legacy-arm64-v8a.apk

[github-android-legacy-armeabi-v7a]: https://github.com/GrahamZen/animeko/releases/download/$GIT_TAG/ani-$TAG_VERSION-legacy-armeabi-v7a.apk

[github-android-legacy]: https://github.com/GrahamZen/animeko/releases/download/$GIT_TAG/ani-$TAG_VERSION-legacy-universal.apk

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

下方有 QQ 群二维码，也可以[点这里进群](https://qm.qq.com/q/JaXFdpv3mC)/搜索群号1045984894。入群问题的答案：$REPO_OWNER

## 下载

[//]: # (@formatter:off  因为"版本"前面不能换行)

优先下载与自己设备架构对应的安装包，体积更小、更省存储；不确定或装不上时再用 `universal`（包含全部架构，体积最大）。

[//]: # (@formatter:on)

| 处理器架构                | 适用于               | 下载                                                                                                      |
|---------------------|-------------------|---------------------------------------------------------------------------------------------------------|
| arm64-v8a | 64 位电视与电视盒子       | [GitHub][github-android-arm64-v8a]       |
| armeabi-v7a   | 32 位电视与电视盒子             | [GitHub][github-android-armeabi-v7a] |
| x86_64              | x86 电视盒子及模拟器      | [GitHub][github-android-x86_64]                |
| universal           | 所有设备（不确定架构时选这个）   | [GitHub][github-android]                |
| legacy arm64-v8a    | Android 7.1 ~ 8.0 的 64 位盒子 | [GitHub][github-android-legacy-arm64-v8a] |
| legacy armeabi-v7a  | Android 7.1 ~ 8.0 的 32 位盒子 | [GitHub][github-android-legacy-armeabi-v7a] |
| legacy universal    | Android 7.1 ~ 8.0，不确定架构时选这个 | [GitHub][github-android-legacy] |

[github-android-qr]: https://github.com/GrahamZen/animeko/releases/download/$GIT_TAG/ani-$TAG_VERSION-universal.apk.github.qrcode.png

### Android 7.1 兼容包

表格前四行要求 Android 8.1 及以上。装不上并提示 `INSTALL_FAILED_OLDER_SDK` 的话，改用带 `legacy` 的后三行，它们支持到 Android 7.1。

兼容包和正式包功能一致，但只在少量设备上验证过；能装正式包就别用它。另外 Android 7.1 ~ 8.0 上 BT 引擎跑在应用进程内，退到后台被系统回收后下载不会保活。

## 本次更新
- 数据源新增「直链 API」类型：用一份 JSON 配置对接直接返回播放地址的接口，不必为每个站点单独适配；添加时会给出一份结构完整的示例配置，按目标站点改写即可。目前只能通过「Animeko控制台」添加
- 数据源配置新增「直连取流」：在线源可配置直接向站点接口取播放地址，不再需要系统 WebView 解析页面；系统 WebView 过旧的电视（表现为搜得到但一播就失败）也能播放这类源。目前只能通过导入数据源 JSON 配置
- 电视播放器的按钮可以自己排：设置 - 播放器 - 「自定义播放器按钮」进入一个不播放的播放器，按确认键把按钮拿起来挪位置，长按确认键把用不上的藏起来；可以存几套排法换着用
- 电视播放器的进度条会显示已经缓冲好的部分；BT 资源还会逐片标出正在下载和暂时拿不到的片段
- 新番时间表的版式可以自己选：设置 - 界面 - 「新番时间表版式」，改版前的竖版卡片网格和上游的经典列表都拿了回来，默认仍是左侧详情 + 单列时间线
- RSS 数据源支持直接提供视频地址的订阅源，不再只能用于 BT 种子；这类资源按在线源处理，可直接播放
- 电视端卡片之间导航更跟手：背景图和简介的切换不再排队等动画，冷启动后第一张背景图也出得更快；选「流畅」时换卡即时完成
- 手机的锁屏和控制中心可以控制电视播放：接入后不用解锁进网页，在锁屏上就能暂停、快进快退、拖进度条和切集，封面与进度也会显示。默认手动接入，需要时在「Animeko控制台」的播放页点一下（接入会占用手机的音频焦点，正在放的音乐会被打断）
- 「Animeko控制台」的播放卡补齐常用操作：倍速条（范围与档位同电视上那条）、跳过片头的 85 秒快进，以及「重新搜索（含新数据源）」——搜索用的数据源列表是进播放页那一刻定下的，改完数据源或更新订阅后按一下，新源才会参与
- 「Animeko控制台」的弹幕时间偏移不用再连点加减：按住数字左右拖动就能微调（0.1 秒一档），点一下可以直接输入具体秒数
- 「Animeko控制台」的播放卡音量可以换样式：设置 - 本机偏好 - 「播放卡的音量控件」，新增加减按钮（按一下走一档、按住数字左右拖微调、点数字直接输），避免手机上碰一下滑动条就把音量拉满；也可以保留原来的滑动条，或者不显示
- 「Animeko控制台」的数据源页新增「测试」与「全部测试」：逐个或一次性检查各数据源能否连上，结果显示在每个源下面（测的是站点连通性，不代表一定搜得到资源）
- 「Animeko控制台」改搜索名时，次要搜索名可以点一下与主搜索名对调，不用再手动剪切粘贴

----

- 「直链 API」数据源改用本机浏览器的 User-Agent 发起请求，不再使用所有设备都相同的固定值
- 向数据源与其他第三方站点发起的请求不再携带标识本应用的请求头，避免被针对性屏蔽
- 播放器里的弹窗底色统一成半透明
- 修复作品名里季号位置不同（如「第三季」在名字中间或末尾）时，在线源的结果被整体判为「不是这部作品」而全部排除的问题
- 修复更新提示里文案较长时把「立即更新」按钮顶出屏幕，只能取消
- 修复从详情页返回电视搜索页时，焦点先在顶部的搜索词上闪一下再回到结果
- 修复播放中从弹窗里点人物圆头像会跳去全屏页面，播放被暂停
- 修复升级后已下载完的 BT 缓存全部变回「未完成」：数据库迁移会把旧的完成记录按集搬过去，不再丢失
- 修复合集资源（如「01-12 合集」）里播放某一集时可能拿到另一集的文件，表现为缓存显示已完成却播不了，然后自动切到在线源
- 修复缓存标记为「已完成」但文件实际不完整时仍被当成本地文件打开；现在会校对文件大小，对不上就重新下载
- 修复删掉合集里的某一集再重新缓存时，界面立刻显示「下载完成」却一直卡在正在缓冲（代价是删掉其中几集后空间要等整个种子都不再使用时才回收）
- 修复反复拖动进度条时偶尔报「加载失败」并自动切换数据源（在线源上尤其容易碰到）
- 修复手机锁屏和控制中心的播放控件在电视切换番剧时被系统收走：屏幕上还留着一个进度停在 0、按了没反应的空壳（网页处于后台时必现）
- 修复「Animeko控制台」的播放卡偶尔整个停更新，要按一下播放键或切个标签再回来才恢复
- 修复电视休眠后重新进入时，启动的二维码弹窗会出现在详情页等页面上并挡住遥控器操作；现在只在首页弹

### 已知问题

* **画质增强（设置 - 播放 - 默认画质增强）在电视上不可用，建议保持「关」**：NVIDIA Shield 上开启后只有声音没有画面；部分机型能播但严重掉帧（实测 24fps 的片只出 6fps 左右）并很快卡住，之后连不带增强的视频也可能起不来，需要强制停止应用恢复。

> Android TV 遥控器使用说明、系统版本要求与已知问题，请见仓库 [README 的「📺 Android TV 版说明」](https://github.com/GrahamZen/animeko#-android-tv-版说明)。

## 交流群

使用中遇到问题、想提建议或反馈 bug，[欢迎进群](https://qm.qq.com/q/JaXFdpv3mC)，或用手机 QQ 扫下面的二维码/搜索群号1045984894。
入群问题的答案：$REPO_OWNER。

![加入 QQ 反馈群](https://quickchart.io/qr?text=https%3A%2F%2Fqm.qq.com%2Fq%2FJaXFdpv3mC&size=200&margin=2&ecLevel=M&dark=000000&light=ffffff)
