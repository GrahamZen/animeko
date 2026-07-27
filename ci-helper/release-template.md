$RELEASE_NOTES

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

|                  | 下载                                               | 常见问题                                        |
|------------------|--------------------------------------------------|---------------------------------------------|
| 安卓 电视      | [主线](https://github.com/GrahamZen/animeko/releases/download/$GIT_TAG/ani-$TAG_VERSION-universal.apk)       |                                             |

[github-android-qr]: https://github.com/GrahamZen/animeko/releases/download/$GIT_TAG/ani-$TAG_VERSION-universal.apk.github.qrcode.png

<details>
<summary> Android 细分架构下载 </summary>

[//]: # (@formatter:off  因为"版本"前面不能换行)

如果不知道自己是什么架构，建议下载 `universal` 版本。

[//]: # (@formatter:on)

| 处理器架构              | 适用于             | 下载                                                                                                      |
|--------------------|-----------------|---------------------------------------------------------------------------------------------------------|
| universal (推荐)     | 所有设备            | [GitHub][github-android]                                     |
| arm64-v8a (64 位)   | 几乎所有手机和平板      | [GitHub][github-android-arm64-v8a]       |
| armeabi-v7a (32 位) | 旧手机             | [GitHub][github-android-armeabi-v7a] |
| x86_64             | Chromebook 及模拟器 | [GitHub][github-android-x86_64]                |

</details>

## 本次更新

接 6.0.0-alpha05 的增量更新，集中在选集流程与遥控器焦点。

### 播放器

* 图标行按下键时分集列表还没加载完，不再直接跳进内嵌详情页 —— 现在会记住这次按键，选集条一就绪就自动弹出；只有确认没有分集（未开播 / 加载失败）才去详情页。
* 选集条弹得更快：可用性改为读播放器自己的分集列表，不再等整套详情数据组装完。实测从最长 3.7 秒缩短到进屏 1 秒内就绪，弹出时首帧就能选。
* 选集卡片的剧照在进播放器后就预取，并与详情页共用同一份缓存 —— 选集条一弹出就有图；反过来，先进播放器再退回详情页往下翻，剧集图同样是热的。顺带修掉预取还没下完时卡片又发一遍同一张图的请求（同一份带宽下反而比不预取更慢）。
* 按住下键不再一路穿到详情层（一次按下最多换一层）。
* 选集条卡片上按上键的落点修正。

### 详情页

* 作品简介与集简介按可用高度截断，右下角「显示更多」打开全文弹窗：上下键按行滚动，返回关闭。正文本身不再可聚焦，遥控器少一个停留点。
* 长按选集卡片从原来的下拉菜单改成「本集详情」弹窗：该集剧照作背景 + 完整简介滚动 + 一个按钮（按当前状态给出「标记看过」或「取消看过」，不再摆一个必然是空操作的）。
* 集简介右侧的时长 / 播出日期改为上下贴边，与正文首行末行对齐；作品简介与集简介两块正文宽度统一。
* 修复焦点在选集卡片上按返回，偶发先回到第一页、再按一次又回到卡片页（长按出过弹窗后可稳定触发）。

### 追番页

* 修复从卡片进详情页再返回后看不到焦点圈、要先按一下方向键才落回卡片。
* 修复按住右键时焦点在分类标签间无限循环（最后一个跳回第一个）。
* 修复上述操作还会把选中的分类改成第一个、整个网格被换掉。
* 修复进页恢复焦点期间用遥控器移动焦点会被拉回去。

### 其他

* 新版本提示卡片：焦点在卡片里时按返回 = 关掉卡片，不会再穿到页面把当前页退掉。
* 搜索页顶部一行（搜索框 / 筛选）聚焦时看得见了。

### 内部改动

TV 适配已拆成独立的构建变体与独立界面模块，共享代码里不再有任何设备判断，手机包不会再编进 TV 界面。对已安装的用户是原地升级，不需要卸载重装。

> Android TV 遥控器使用说明、系统版本要求与已知问题，请见仓库 [README 的「📺 Android TV 版说明」](https://github.com/GrahamZen/animeko#-android-tv-版说明)。
