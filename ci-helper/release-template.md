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

修复国内用户直连TMDB失败因此加载不出背景的问题。为**长按返回**增加一个动作面板；另外修了一批播放、缓存和图片加载的问题。

### 遥控器：长按手势与动作面板

* **长按播放键**：任何页面弹出动作面板，焦点直接落在「正在播放」卡上——再按一下确定就回到那一集。
* **长按返回**：播放器画面上有任何组件遮挡时，长按返回可以直接回到画面；其他页面弹出**同一个**动作面板（给没有播放键的遥控器用，行为与长按播放键完全一样）。
* 两个长按**各自可配**（设置-界面）：打开动作面板 / 直接回到正在播放 / 不做任何事（后者只给返回键，因为播放键长按不认领就等于白扔一个手势）。想要旧手感的把播放键设成「直接回到正在播放」即可，和返回键开面板正好分工。
* **首页按返回的行为改成三选一**（设置-界面）：直接退出 / 弹出动作面板 / **连按两次退出**。默认换成了最后一种——它比直接退出安全，又比弹面板快，而面板本身随时可以长按唤出。之前显式关过「退出前先询问」的，仍然是直接退出。
* 面板从上到下是这样：
    * **「正在播放」卡**： 整块按下去就是回去接着看，右端 ✕ 是关掉它——**按 ✕ 面板不会跟着关**，卡片就地退成「接下来播放」（关闭区整块消失、进度线定住压暗），你能看见确实关掉了（面板里其余每一颗按下去都是离开，只有它和刷新是改面板自己显示的东西）。没有后台播放时这块**不是空的**：它变成**「接下来播放」**——上次没看完的那一集，那一集看完了就是下一集，按一下直接接着看（连这个都没有才是空卡）。面板高度三种情况都不变。
    * 卡片右端那一条现在是**后台播放的开关**：正在播时是 ✕（关掉它），没有在播时是**「在后台准备这一集」**——不进播放器，先让它在后台把数据源找好、缓冲上，你接着浏览，好了照常提示。慢的源要十几秒，这样就不用进去干等了。
    * 卡片上那张图优先用**你退出播放器时停住的那一帧**，取不到才依次退回这一集的剧照、整部作品的背景图。
    * 卡片顶上小字写明后台加载到哪一步了（正在查找数据源 / 正在缓冲 / 正在播放），卡住时直接写原因，并变成醒目的警示色。
    * **服务连通那一条**： 在底下最后一颗图标上按右可走到刷新按钮，长按跳到设置里的代理页。开机后第一次弹出面板会自动测一遍，之后用户可手动刷新；浏览时遇到加载失败或等待超时也会自动刷新。
* 「一起看」入口从侧边栏挪进这个面板，并在电视上**默认开启**；仍可在设置-应用里关掉。
* 修好了搜索结果和首页推荐行上**长按播放键会被当成短按**（直接开播）的问题，现在所有页面的长按行为一致。
* 侧边栏底部有后台播放时会多一颗**「正在播放」图标**：点它直接回去，图标本身在后台还在找源/缓冲时会跳动，就绪后静止，出问题变色。

### 其他

* 更新提示里的**「查看详情」现在按 markdown 显示**：加粗、链接、行内代码、图片都能正常渲染，不再是一堆星号和方括号（图片走的是评论区那条加载链路）。
* **设置、缓存管理、播放历史这类页面在返回栈里只留一份**：以前从动作面板反复跳设置会一层层叠上去，返回要按很多次才退得出来；条目详情、人物这些内容页不受影响（来回浏览的历史要留着）。
* 相邻卡片背景图会自动预加载。
* 退出播放页后短时间内再回去，进度条缩略图不再从头重建（以前每进一次都要另起一个播放器解析一遍，既卡切页动画又占内存；反复快进快出时甚至会把应用撑到被系统杀掉）。
* 后台加载好的默认提示音换成更轻的一声。

### 修复

* 播放器里的选集条只列正片、和详情页对不上；现在特别篇按序号插在正片之间。
* 缓存管理页的删除确认框现在写清楚要删几项、涉及哪几部。
* 电视首页偶尔一进来看不到轮播区的两个按钮。
* 刚进应用就弹出更新提示时，按返回会先弹「再按一次退出」却退不出去——现在更新气泡在场时返回键一律先关它。
* 设置里测连接不通时要转满一分钟才判失败。
* 设置-网络的 TMDB 连通性拆成「背景图接口」和「背景图 CDN」两项：这两个域名常常一个通一个不通，合成一项时看不出该不该挂代理。

> Android TV 遥控器使用说明、系统版本要求与已知问题，请见仓库 [README 的「📺 Android TV 版说明」](https://github.com/GrahamZen/animeko#-android-tv-版说明)。
