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
- 新增「Animeko控制台」：用浏览器遥控完成电视上遥控器不方便的操作，支持搜索、选集、播放、缓存、下载、设置和日志查看；可在手机或电脑上使用，要求电视和浏览器在同一局域网内。启用「后台保持连接」后，电视休眠或退出 Ani 后手机仍可连接；常驻服务延迟到冷启动完成后再启动，避免触发系统的前台服务时限。
- 电视详情页改为按页翻动，角色与制作人员使用圆头像横排，评价区重新排版；选集图片提前加载，第一次向下翻页更流畅，原来的「作品信息」移入简介的「显示更多」
- 新番时间表全面改版：左侧显示当前条目的大图与详情，右侧使用单列时间线，按左右键直接换天
- 平板等触屏设备安装 TV 包后可直接触摸操作：点击卡片和搜索框、滑动页面，播放器中单击呼出控件、双击暂停、长按倍速并拖动进度条
- 新增 Android 7.1 至 8.0 兼容安装包，自动更新会按系统版本选择可安装的包
- 电视与手机遥控的搜索筛选新增年份和季度，不用再靠标签间接组合
- 电视端重做卡片滚动和详情页缩放转场，「视觉效果」提供流畅、均衡和完整三档
- 电视播放器的按钮可以自己排：设置 - 播放器 - 「自定义播放器按钮」进入一个不播放的播放器，按确认键把按钮拿起来挪位置，长按确认键把用不上的藏起来；可以存几套排法换着用

----

- 数据源搜索中修改过的作品搜索名会按作品记住并可一键恢复 Bangumi 名称，后续剧集、下次进入播放页、缓存页与手机遥控的缓存选源都会沿用
- RSS 数据源支持直接提供视频地址的订阅源，不再只能用于 BT 种子；这类资源按在线源处理，可直接播放
- 数据源配置新增「直连取流」：在线源可配置直接向站点接口取播放地址，不再需要系统 WebView 解析页面；系统 WebView 过旧的电视（表现为搜得到但一播就失败）也能播放这类源。目前只能通过导入数据源 JSON 配置
- 数据源新增「直链 API」类型：用一份 JSON 配置对接直接返回播放地址的接口，不必为每个站点单独适配；添加时会给出一份结构完整的示例配置，按目标站点改写即可。目前只能通过「Animeko控制台」添加
- 设置中可关闭 TMDB 背景图，TMDB 无法连接时直接使用作品封面
- 电视默认主题改为深色和纯黑背景，并修复缓存管理页在浅色主题下出现黑色区域
- 自动更新在 GitHub 无法连接时会改用国内镜像检查和下载安装包
- 修复作品名里季号位置不同（如「第三季」在名字中间或末尾）时，在线源的结果被整体判为「不是这部作品」而全部排除的问题
- 修复低内存设备观看超长篇作品时，加载或切集闪退
- 修复缓存部分在线视频时应用反复闪退，以及 BT 服务重启后未重新绑定、缓存一直等待服务的问题
- 修复 Android 7.1 兼容包解析在线源时闪退、动漫花园等 BT 数据源搜不到资源的问题
- 修复部分电视因系统自带同名组件，在线源全部搜不到结果或打开播放器闪退的问题
- 修复部分在线源页面混入 `javascript:` 等无效链接时，整个数据源搜不到结果的问题
- 修复设置代理后数据源能搜到结果但在线源无法播放：播放和网页解析现在也会使用 Ani 的代理；局域网地址仍然直连，系统 WebView 遇到 SOCKS5 代理时网页解析仍会直连
- 修复设备没有可用浏览器时点击链接闪退和复制日志时闪退；打不开浏览器时改为显示二维码供手机扫码打开
- 修复电视搜索页 NSFW 条目在「模糊」模式下显示黑块、首次搜索偶尔一直转圈的问题

### 已知问题

* **画质增强（设置 - 播放 - 默认画质增强）在电视上不可用，建议保持「关」**：NVIDIA Shield 上开启后只有声音没有画面；部分机型能播但严重掉帧（实测 24fps 的片只出 6fps 左右）并很快卡住，之后连不带增强的视频也可能起不来，需要强制停止应用恢复。

> Android TV 遥控器使用说明、系统版本要求与已知问题，请见仓库 [README 的「📺 Android TV 版说明」](https://github.com/GrahamZen/animeko#-android-tv-版说明)。

## 交流群

使用中遇到问题、想提建议或反馈 bug，[欢迎进群](https://qm.qq.com/q/JaXFdpv3mC)，或用手机 QQ 扫下面的二维码/搜索群号1045984894。
入群问题的答案：$REPO_OWNER。

![加入 QQ 反馈群](https://quickchart.io/qr?text=https%3A%2F%2Fqm.qq.com%2Fq%2FJaXFdpv3mC&size=200&margin=2&ecLevel=M&dark=000000&light=ffffff)
