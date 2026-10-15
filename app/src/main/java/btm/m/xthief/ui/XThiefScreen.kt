// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import btm.m.xthief.XThiefApplication
import btm.m.xthief.data.DownloadRecord
import btm.m.xthief.data.NavigationStyle
import btm.m.xthief.ui.components.CustomNavigation
import btm.m.xthief.ui.components.HostTab
import btm.m.xthief.ui.components.LocalDialogBackdrop
import btm.m.xthief.ui.pages.AboutScreen
import btm.m.xthief.ui.pages.DownloadTab
import btm.m.xthief.ui.pages.HistoryPage
import btm.m.xthief.ui.pages.LicenseScreen
import btm.m.xthief.ui.pages.LibraryPage
import btm.m.xthief.ui.pages.SettingsPage
import btm.m.xthief.x.DownloadStatus
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Scaffold

/** Second-level screens reachable from the tab content, mirroring HyperChanger's page stack. */
enum class DetailPage(val title: String) {
    ABOUT("关于"),
    LICENSE("LICENSE"),
    OPEN_SOURCE("开源代码声明"),
}

@Composable
fun XThiefScreen(
    container: XThiefApplication,
    sharedText: String? = null,
) {
    val settingsStore = container.settingsStore
    val settings by settingsStore.state.collectAsState()
    val history by container.historyStore.records.collectAsState()
    val library by container.mediaLibrary.entries.collectAsState()
    val tasks by container.downloadManager.tasks.collectAsStateWithLifecycle()
    val dark = rememberDarkTheme(settings.appColorMode)
    var tab by remember { mutableStateOf(MainTab.PARSE) }
    var pageStack by remember { mutableStateOf<List<DetailPage>>(emptyList()) }
    val backdrop = rememberLayerBackdrop()
    val selectedIndex = remember { mutableIntStateOf(tab.ordinal) }
    if (selectedIndex.intValue != tab.ordinal) selectedIndex.intValue = tab.ordinal
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val page = pageStack.lastOrNull()

    fun pushPage(target: DetailPage) {
        pageStack = pageStack + target
    }

    fun popPage() {
        pageStack = pageStack.dropLast(1)
    }

    // Persist finished downloads into the history log exactly once.
    LaunchedEffect(container) {
        val recorded = mutableSetOf<String>()
        container.downloadManager.tasks.collect { list ->
            list.filter { it.status == DownloadStatus.COMPLETED }.forEach { task ->
                if (recorded.add(task.id)) {
                    container.historyStore.add(
                        DownloadRecord(
                            id = task.id,
                            tweetId = task.tweetId,
                            author = task.title.trimStart('@'),
                            authorHandle = task.title,
                            text = task.subtitle,
                            variantLabel = task.variant.resolutionLabel,
                            bitrate = task.variant.bitrate,
                            fileName = task.fileName,
                            uri = task.savedUri.orEmpty(),
                            byteCount = task.bytesDownloaded,
                            createdAt = System.currentTimeMillis(),
                            completed = true,
                        ),
                    )
                    container.mediaLibrary.refresh(listOf(settingsStore.current.effectiveDirectory))
                }
            }
        }
    }

    LaunchedEffect(settings.effectiveDirectory, settings.usesCustomDirectory) {
        container.mediaLibrary.refresh(listOf(settings.effectiveDirectory))
    }

    LaunchedEffect(settings.keepHistoryDays) {
        container.historyStore.prune(settings.keepHistoryDays)
    }

    // Ask for the notification permission once, so finished downloads can be announced.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { },
    )
    LaunchedEffect(settings.showDownloadNotifications) {
        if (!settings.showDownloadNotifications) return@LaunchedEffect
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect
        val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    // Predictive back drives the second-level page away under the finger.
    val backState = rememberMomentumPredictiveBack(
        enabled = page != null,
        onBack = { popPage() },
        maxProgress = 0.85f,
    )
    LaunchedEffect(page) {
        if (page == null) backState.reset()
    }

    XThiefTheme(darkTheme = dark) {
        // MIUIX's Scaffold is the popup host: HyperChanger's glass dropdown and glass dialog
        // both render into the root scaffold, so the app must provide one at the top.
        // Page content is recorded exactly once (`layerBackdrop` below): nesting a second
        // recording node for the same LayerBackdrop re-enters RenderNode recording and
        // crashes with "Recording currently in progress".
        CompositionLocalProvider(LocalDialogBackdrop provides backdrop) {
            Scaffold(containerColor = Color.Transparent) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                        when (tab) {
                            MainTab.PARSE -> DownloadTab(
                                container = container,
                                settings = settings,
                                initialLink = sharedText,
                            )

                            MainTab.DOWNLOAD -> HistoryPage(
                                container = container,
                                records = history,
                                tasks = tasks,
                                onDelete = { container.historyStore.remove(it.id) },
                                onClear = { container.historyStore.clear() },
                                onOpen = { openUri(container, it.uri) },
                                onShare = { shareRecord(container, it) },
                            )

                            MainTab.LIBRARY -> LibraryPage(
                                entries = library,
                                directory = settings.effectiveDirectory,
                                onRefresh = {
                                    scope.launch {
                                        container.mediaLibrary.refresh(listOf(settings.effectiveDirectory))
                                    }
                                },
                                onOpen = { openUri(container, it.uri) },
                                onShare = { shareUri(container, it.uri, it.displayName) },
                                onDelete = { container.mediaLibrary.delete(it) },
                                onDeleteAll = { targets -> container.mediaLibrary.deleteAll(targets) },
                            )

                            MainTab.SETTINGS -> SettingsPage(
                                settings = settings,
                                update = { transform -> settingsStore.update(transform) },
                                onRefreshLibrary = {
                                    scope.launch {
                                        container.mediaLibrary.refresh(listOf(settings.effectiveDirectory))
                                    }
                                },
                                onClearHistory = { container.historyStore.clear() },
                                historyCount = history.size,
                                libraryCount = library.size,
                                backdrop = backdrop,
                                allFilesAccess = hasAllFilesAccess(context),
                                islandStatus = if (container.islandNotifier.isSupported) {
                                    "系统支持超级岛 · 焦点通知协议 ${container.islandNotifier.protocolVersion}"
                                } else {
                                    "当前系统未声明支持超级岛（persist.sys.feature.island=false）"
                                },
                                onRequestAllFilesAccess = { requestAllFilesAccess(context) },
                                onSimulateIsland = {
                                    val notifier = container.islandNotifier
                                    notifier.simulate(settings.islandSimulationSeconds * 1000L)
                                    toast(
                                        context,
                                        if (notifier.isSupported) {
                                            "已发送模拟超级岛（${settings.islandSimulationSeconds}s）"
                                        } else {
                                            "已发送，但系统未声明支持超级岛"
                                        },
                                    )
                                },
                                onOpenPage = { pushPage(it) },
                            )
                        }
                    }

                    // HyperChanger's own bottom navigation, reused verbatim: it owns the four
                    // OS styles, the damped drag pill and the glass materials. It samples the
                    // backdrop recorded above, so it must sit outside that node.
                    if (page == null) {
                        CustomNavigation(
                            sourceView = view,
                            tabs = MainTab.entries.mapIndexed { index, item ->
                                HostTab(
                                    label = item.label,
                                    className = "xthief.$index",
                                    icon = item.icon,
                                    onClick = { tab = item },
                                )
                            },
                            selectedIndex = selectedIndex,
                            // Material strength is fixed to HyperChanger's values: the liquid
                            // glass capsule uses a light 3dp blur, the other styles 18dp.
                            blurRadius = if (settings.navStyle == NavigationStyle.LIQUID_GLASS) 3 else 18,
                            labelMode = settings.navLabelMode.preferenceValue,
                            navigationStyle = settings.navStyle.preferenceValue,
                            advancedMaterial = true,
                            colorMode = settings.appColorMode.preferenceValue,
                            liquidBottomSpacingDp = 0,
                            // Let the whole bar be dragged: with only four tabs the default
                            // selection-pill-only drag feels like it lags behind the finger.
                            dragWholeFloatingBar = true,
                            onHostPreDraw = {},
                            backdropOverride = backdrop,
                            // Without this the liquid-glass style falls back to its built-in
                            // brand icon mapping and renders generic "dynamic" glyphs.
                            tabImageVector = { index -> MainTab.entries[index].icon },
                        )
                    }

                AnimatedVisibility(
                    visible = page != null,
                    enter = slideInHorizontally(
                        animationSpec = tween(252, easing = FastOutSlowInEasing),
                        initialOffsetX = { it / 7 },
                    ) + fadeIn(tween(189)) + scaleIn(
                        animationSpec = tween(252, easing = FastOutSlowInEasing),
                        initialScale = .985f,
                    ),
                    exit = slideOutHorizontally(
                        animationSpec = tween(207, easing = FastOutSlowInEasing),
                        targetOffsetX = { -it / 3 },
                    ) + fadeOut(tween(162)) + scaleOut(
                        animationSpec = tween(207, easing = FastOutSlowInEasing),
                        targetScale = .975f,
                    ),
                ) {
                    Box(Modifier.fillMaxSize().momentumBackTransform(backState)) {
                        AnimatedContent(
                            targetState = page,
                            transitionSpec = {
                                (slideInHorizontally(tween(252, easing = FastOutSlowInEasing)) { it / 7 } +
                                    fadeIn(tween(189))) togetherWith
                                    (slideOutHorizontally(tween(207, easing = FastOutSlowInEasing)) { -it / 3 } +
                                        fadeOut(tween(162)))
                            },
                            label = "detailPage",
                            modifier = Modifier.fillMaxSize(),
                        ) { current ->
                            when (current) {
                                null -> Box(Modifier.fillMaxSize())
                                DetailPage.ABOUT -> AboutScreen(
                                    onBack = { popPage() },
                                    onOpenPage = { pushPage(it) },
                                    onOpenUrl = { openExternalUrl(container, it) },
                                )

                                DetailPage.LICENSE -> LicenseScreen(
                                    title = "LICENSE",
                                    body = LICENSE_TEXT,
                                    onBack = { popPage() },
                                )

                                DetailPage.OPEN_SOURCE -> LicenseScreen(
                                    title = "开源代码声明",
                                    body = OPEN_SOURCE_TEXT,
                                    onBack = { popPage() },
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

private const val LICENSE_TEXT = """Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/

Copyright 2026 btm_m

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

（以下中文说明仅供参考，法律效力以英文原文为准）

你可以在遵守 Apache License 2.0 的前提下使用、修改和再分发本项目。
分发时需保留版权声明、许可证副本，并标注修改过的文件。
本软件按「原样」提供，不附带任何明示或暗示的担保。"""

private const val OPEN_SOURCE_TEXT = """XThief 使用了以下开源项目，感谢所有项目作者与贡献者。

MIUIX 0.9.3 · Apache License 2.0
HyperOS 风格界面、偏好设置、图标与模糊效果
https://github.com/compose-miuix-ui/miuix

Backdrop / AndroidLiquidGlass 2.0.0 · Apache License 2.0
液态玻璃渲染与底部导航交互
https://github.com/Kyant0/AndroidLiquidGlass

Compose Multiplatform 1.11.x · Apache License 2.0
声明式界面、布局与动画
https://github.com/JetBrains/compose-multiplatform

AndroidX · Apache License 2.0
Activity、Lifecycle、Core 等 Android 基础库
https://github.com/androidx/androidx"""
