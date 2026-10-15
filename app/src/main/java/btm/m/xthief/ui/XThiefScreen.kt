// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import btm.m.xthief.XThiefApplication
import btm.m.xthief.data.DownloadRecord
import btm.m.xthief.data.LIBRARY_VIEW_GRID
import btm.m.xthief.data.LIBRARY_VIEW_LIST
import btm.m.xthief.data.NavigationStyle
import btm.m.xthief.ui.components.CustomNavigation
import btm.m.xthief.ui.components.HostTab
import btm.m.xthief.ui.components.LocalDialogBackdrop
import btm.m.xthief.ui.components.NavAnimationTiming
import btm.m.xthief.ui.components.ToolbarAnimationTiming
import btm.m.xthief.ui.components.XThiefDialog
import btm.m.xthief.ui.momentumBackTransform
import btm.m.xthief.ui.rememberMomentumPredictiveBack
import btm.m.xthief.ui.pages.AboutScreen
import btm.m.xthief.ui.pages.DownloadTab
import btm.m.xthief.ui.pages.HistoryPage
import btm.m.xthief.ui.pages.LicenseScreen
import btm.m.xthief.ui.pages.LibraryPage
import btm.m.xthief.ui.pages.PlayerScreen
import btm.m.xthief.ui.pages.PlayerSource
import btm.m.xthief.ui.pages.SettingsPage
import btm.m.xthief.x.DownloadStatus
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay
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
    // True while the media library is in multi-select: the bottom navigation slides away so the
    // floating selection toolbar has the area to itself.
    var librarySelecting by remember { mutableStateOf(false) }
    // Staged so the two never overlap: the bar finishes leaving before the toolbar appears, and
    // the toolbar finishes leaving before the bar comes back.
    var libraryNavHidden by remember { mutableStateOf(false) }
    var libraryToolbarVisible by remember { mutableStateOf(false) }

    LaunchedEffect(librarySelecting) {
        if (librarySelecting) {
            libraryNavHidden = true
            delay(NavAnimationTiming.HIDE_MS.toLong())
            libraryToolbarVisible = true
        } else {
            libraryToolbarVisible = false
            delay(ToolbarAnimationTiming.EXIT_MS.toLong())
            libraryNavHidden = false
        }
    }

    // Same staging for the download tab's selection toolbar.
    var downloadSelecting by remember { mutableStateOf(false) }
    var downloadNavHidden by remember { mutableStateOf(false) }
    var downloadToolbarVisible by remember { mutableStateOf(false) }

    LaunchedEffect(downloadSelecting) {
        if (downloadSelecting) {
            downloadNavHidden = true
            delay(NavAnimationTiming.HIDE_MS.toLong())
            downloadToolbarVisible = true
        } else {
            downloadToolbarVisible = false
            delay(ToolbarAnimationTiming.EXIT_MS.toLong())
            downloadNavHidden = false
        }
    }
    val dark = rememberDarkTheme(settings.appColorMode)
    var tab by remember { mutableStateOf(MainTab.PARSE) }
    var pageStack by remember { mutableStateOf<List<DetailPage>>(emptyList()) }
    // Non-null while the built-in player covers the screen; it is not a DetailPage because it
    // carries the clip it is playing. `lastPlayerSource` keeps the content alive through the exit
    // animation, after `playerSource` has already become null.
    var playerSource by remember { mutableStateOf<PlayerSource?>(null) }
    var lastPlayerSource by remember { mutableStateOf<PlayerSource?>(null) }
    if (playerSource != null) lastPlayerSource = playerSource
    val density = LocalDensity.current
    val playerBackState = rememberMomentumPredictiveBack(
        enabled = playerSource != null,
        onBack = { playerSource = null },
        maxProgress = 0.85f,
    )
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

    // Notification permission is requested from the settings switch itself, so launching the app
    // (or being handed a shared link) never throws a system dialog at the user.

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
                                onOpen = { record ->
                                    playerSource = PlayerSource(
                                        uri = record.uri,
                                        title = record.fileName.ifBlank { "未命名视频" },
                                        mediaFileName = record.fileName,
                                        recordId = record.id,
                                        subtitle = record.variantLabel,
                                    )
                                },
                                onShare = { shareRecord(container, it) },
                                toolbarVisible = downloadToolbarVisible,
                                onSelectionModeChange = { active -> downloadSelecting = active },
                            )

                            MainTab.LIBRARY -> LibraryPage(
                                entries = library,
                                directory = settings.effectiveDirectory,
                                onRefresh = {
                                    scope.launch {
                                        container.mediaLibrary.refresh(listOf(settings.effectiveDirectory))
                                    }
                                },
                                onOpen = { entry ->
                                    playerSource = PlayerSource(
                                        uri = entry.uri,
                                        title = entry.displayName,
                                        mediaFileName = entry.fileName,
                                        // Library entries are matched to their history record by
                                        // file name, so favouriting has a record to write to.
                                        recordId = history
                                            .firstOrNull { it.fileName == entry.fileName }
                                            ?.id,
                                        subtitle = buildString {
                                            append(fileSizeLabel(entry.sizeBytes))
                                            if (entry.width > 0 && entry.height > 0) {
                                                append(" · ${entry.width}×${entry.height}")
                                            }
                                            if (entry.durationMs > 0) {
                                                val total = entry.durationMs / 1000
                                                append(" · %d:%02d".format(total / 60, total % 60))
                                            }
                                        },
                                    )
                                },
                                onShare = { shareUri(container, it.uri, it.displayName) },
                                onDelete = { container.mediaLibrary.delete(it) },
                                onDeleteAll = { targets -> container.mediaLibrary.deleteAll(targets) },
                                onSelectionModeChange = { active -> librarySelecting = active },
                                toolbarVisible = libraryToolbarVisible,
                                viewMode = settings.libraryViewMode,
                                onToggleViewMode = {
                                    settingsStore.update {
                                        it.copy(
                                            libraryViewMode = if (
                                                it.libraryViewMode == LIBRARY_VIEW_GRID
                                            ) {
                                                LIBRARY_VIEW_LIST
                                            } else {
                                                LIBRARY_VIEW_GRID
                                            },
                                        )
                                    }
                                },
                                blurSensitiveCovers = settings.blurSensitiveCovers,
                                thumbnails = container.thumbnailCache,
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
                            visible = !(tab == MainTab.LIBRARY && libraryNavHidden) &&
                                !(tab == MainTab.DOWNLOAD && downloadNavHidden),
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

                // Built-in player: covers the whole screen while a clip is open. It shares the
                // detail page's transition and predictive-back transform, plus a blur so the two
                // layers dissolve into each other.
                val playerVisible = playerSource != null
                val playerBlur by animateFloatAsState(
                    targetValue = if (playerVisible) 0f else PLAYER_EXIT_BLUR,
                    animationSpec = tween(
                        durationMillis = if (playerVisible) 252 else 207,
                        easing = FastOutSlowInEasing,
                    ),
                    label = "playerBlur",
                )
                AnimatedVisibility(
                    visible = playerVisible,
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
                    // Kept alive through the exit animation so the content does not vanish early.
                    val shown = playerSource ?: lastPlayerSource
                    shown?.let { source ->
                        val record = source.recordId?.let { id ->
                            history.firstOrNull { it.id == id }
                        } ?: history.firstOrNull { it.uri == source.uri }
                            ?: history.firstOrNull { it.fileName == source.mediaFileName }
                        var confirmPlayerDelete by remember(source.uri) { mutableStateOf(false) }
                        Box(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val px = with(density) { playerBlur.dp.toPx() }
                                    renderEffect =
                                        if (px > .1f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            BlurEffect(px, px, TileMode.Clamp)
                                        } else {
                                            null
                                        }
                                }
                                .momentumBackTransform(playerBackState),
                        ) {
                            PlayerScreen(
                                source = source,
                                favorite = record?.favorite == true,
                                onToggleFavorite = {
                                    record?.let {
                                        container.historyStore.setFavorite(it.id, !it.favorite)
                                    }
                                },
                                onOpenWith = { openUri(container, source.uri) },
                                onInfo = {
                                    toast(
                                        context,
                                        buildString {
                                            append(source.title)
                                            if (source.subtitle.isNotBlank()) {
                                                append("\n${source.subtitle}")
                                            }
                                            append("\n${source.uri}")
                                        },
                                    )
                                },
                                onShare = { shareUri(container, source.uri, source.title) },
                                onDelete = { confirmPlayerDelete = true },
                                onBack = { playerSource = null },
                            )
                        }
                        if (confirmPlayerDelete) {
                            XThiefDialog(
                                title = "删除视频",
                                message = "「${source.title}」将从设备存储中永久删除。",
                                confirmText = "删除",
                                destructive = true,
                                onConfirm = {
                                    confirmPlayerDelete = false
                                    val target = library.firstOrNull {
                                        it.fileName == source.mediaFileName
                                    }
                                    if (target != null) {
                                        scope.launch {
                                            container.mediaLibrary.delete(target)
                                            container.mediaLibrary.refresh(
                                                listOf(settings.effectiveDirectory),
                                            )
                                        }
                                    }
                                    playerSource = null
                                },
                                onDismiss = { confirmPlayerDelete = false },
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

private const val PLAYER_EXIT_BLUR = 22f

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
