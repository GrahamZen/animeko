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

重新设计播放器界面，下拉可弹出选集卡片，再下拉弹出简略详情页。提醒：经过这几次更新后，电视大屏幕上比较推荐使用深色模式。

新版本提示卡片在 TV 上焦点直接落到「自动更新」，下载完成后自动安装；无操作 20 秒自动消失（下载进度卡不受影响）。

### 一些隐藏操作

除了画面右下角的提示之外还有这些操作：

* **长按确认键**：播放器选集条的卡片上标记该集看过；选集网格的方格上跳到该集的选集卡片。
* **播放 / 暂停键**：播放器内任意层级（控制层 / 选集条 / 详情层）随时切换播放暂停。
* **快进 / 快退键**：播放器内直接切下一集 / 上一集。
* **浮出面板的条目上按左 / 右键**：直接横跳到相邻面板。

### 性能优化

降低低端设备性能压力：

* 详情页动态渐变背景静止时不再逐帧全屏重绘；详情页滚动动画期间不再整页重组。
* 图片加载：内存缓存从固定 10MB 改为按可用内存比例分配；选集卡片剧照按需降档下载（原图档仅用于全屏背景），流量与解码开销约降 4 倍。
* 图片磁盘缓存定容 300MB 并自动淘汰旧文件；设置 → 存储 新增图片缓存占用展示与手动清理入口。
* 修复封面加载时间过久的问题：部分 IP 直连不通时，快速换路重试；封面偶发 421 加载失败也会自动换连接重试。

> Android TV 遥控器使用说明、系统版本要求与已知问题，请见仓库 [README 的「📺 Android TV 版说明」](https://github.com/GrahamZen/animeko#-android-tv-版说明)。
