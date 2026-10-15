# Repository Instructions

## 项目定位

- XThief 是**纯 Android 应用**（应用包名 `btm.m.xthief`），通过 X（Twitter）链接解析并下载视频。
- **禁止**把它做成 LSPosed / Xposed 模块：不要引入 `io.github.libxposed`、不要写 `assets/xposed_init`、不要加 `de.robv.android.xposed.category.MODULE_SETTINGS` 入口。
- 界面风格参考 HyperChanger（`D:\ReverseProject\HyperChanger`，只读参考，禁止修改）。

## UI 约束

- 界面只能使用 MIUIX（`top.yukonga.miuix.kmp.*`）、Backdrop（`com.kyant.backdrop.*`）与 `androidx.compose.foundation` / `androidx.compose.ui` 自研组件。
- **界面中禁止出现任何 Material 风格组件**：不要使用 `androidx.compose.material.*` / `material3` 的组件、图标或主题。
- `androidx.compose.material:material-icons-extended` 允许作为依赖保留（HyperChanger 同样使用），但不得用它渲染界面元素。
- `androidx.compose.material3:material3-window-size-class` 是 MIUIX 0.9.3（`miuix-ui-android`）自带的传递依赖，仅提供窗口尺寸分级计算，**不是界面组件**，允许保留。除此之外不要主动引入任何 Material / Material3 artifact（不要写进 `app/build.gradle.kts`）。
- 图标统一使用 MIUIX Icons 0.9.3 中确实存在的图标（`MiuixIcons.Regular.*`，实际定义在 `top.yukonga.miuix.kmp.icon.extended` 包）。该版本**没有** `History` / `Video` / `Library` 图标，历史用 `Recent`、媒体库用 `Photos`。新增图标前先确认 AAR 中存在同名 `XxxKt` 类。
- 应用主题继承 framework 的 `android:style/Theme.DeviceDefault.Light.NoActionBar`，不要引入 Material 主题。
- 应用图标使用单层 PNG（`res/drawable-nodpi/ic_xthief.png`），不要改造成自适应图标，以免系统叠加白底或裁切。关于页使用 `ic_xthief_sp.png` 与 `ic_xthief_text_sp.png`。
- 底部导航栏必须保持 HyperChanger 的四种样式：`liquid_glass`、`hyper_os`、`hyper_os_floating`、`harmony_os_floating`。

## 设置项

- 所有设置读写集中在 `btm.m.xthief.data.XThiefSettingsStore`，新增设置必须同时在 `XThiefSettings`、`read()`、`write()`、`normalized()` 四处登记。
- 不要另起一套 SharedPreferences 文件存放设置；历史记录用 `HistoryStore`，媒体库扫描用 `MediaLibrary`。

## 解析与下载

- 解析优先级必须保持：syndication（`cdn.syndication.twimg.com/tweet-result`）→ guest token GraphQL → 页面 OpenGraph 抓取。syndication 端点无需认证，且能一次返回全部 MP4 画质，是主要实现路径。
- 下载走 `XDownloadManager`，文件写入 MediaStore（`Movies/XThief`）；不要使用 `Material` 或第三方下载库。
- 网络访问只使用 `HttpURLConnection` 与 `org.json`，不引入 OkHttp / Gson / kotlinx-serialization。

## 构建验证

- 构建环节不用遵循临时文件放在 D 盘的规则。
- 验证改动时**统一使用** `.\gradlew.bat :app:assembleDebug`（不要用 `:app:compileDebugKotlin` 之类的分步任务），并报告生成的 APK 路径。
- 只使用裸命令 `.\gradlew.bat :app:assembleDebug` 做构建测试；严禁指定或重定向 `gradle-user-home`、`build-tmp` 等目录参数。
- 编译或打包失败时不得声称完成。
- 除非用户明确授权，不要中断正在进行的构建。

## 发布 APK 命名

- 交付 release 构建前，把 `app-release.apk` 重命名为 `XThief-v<VersionName>.apk`。
- `versionName` 取自 `app/build.gradle.kts`，把空格替换成连字符，文件名中不得出现空格。

## 发布流程

- 签名配置通过 Gradle 属性注入（`releaseStoreFile` / `releaseStorePassword` / `releaseKeyAlias` / `releaseKeyPassword`），不要在仓库中硬编码口令。
- 生成 release APK 后不要立即推送代码、标签或 Release；先按既有风格起草发行说明，交用户审核。
- 得到用户明确确认后才执行推送与上传。

## ADB 与设备限制

- **禁止使用任何命令操作用户的设备**：不要执行 adb 安装、启动/停止应用、模拟点击、截图、拉取文件等。构建出 APK 后交由用户自行安装验证即可。
- 除非用户明确要求，不要执行 ADB 安装、重启设备或重启系统服务的命令。
- 严禁执行 `adb reboot`（含 fastboot / recovery 重启）。
- 严禁执行任何重启或杀死 SystemUI 的命令。

## 存储与文件操作

- 不要在 `C:` 盘（含 `%TEMP%`、`C:\Users\...`）创建或输出临时文件、缓存与中间产物。
