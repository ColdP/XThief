// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui.pages

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import btm.m.xthief.XThiefApplication
import btm.m.xthief.data.DefaultQuality
import btm.m.xthief.data.SAVE_DIR_CUSTOM
import btm.m.xthief.data.SAVE_DIR_DEFAULT
import btm.m.xthief.data.TweetMedia
import btm.m.xthief.data.VideoVariant
import btm.m.xthief.data.XThiefSettings
import btm.m.xthief.ui.AppPage
import btm.m.xthief.ui.components.ActionRow
import btm.m.xthief.ui.components.EmptyState
import btm.m.xthief.ui.components.GlassChoiceRow
import btm.m.xthief.ui.components.GlassDialogButton
import btm.m.xthief.ui.components.GlassPrimaryButton
import btm.m.xthief.ui.components.InfoRow
import btm.m.xthief.ui.components.LocalPageBackdrop
import btm.m.xthief.ui.components.SectionCard
import btm.m.xthief.ui.components.WindowDialog
import btm.m.xthief.ui.components.XThiefTextField
import btm.m.xthief.ui.folderPathFromUri
import btm.m.xthief.ui.hasAllFilesAccess
import btm.m.xthief.ui.toast
import btm.m.xthief.x.TweetLink
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Backup
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The 解析 tab: paste or share an X (Twitter) post link, pick a quality and a destination, then
 * enqueue a download. Queued and finished tasks live in the 下载 tab.
 */
@Composable
fun DownloadTab(
    container: XThiefApplication,
    settings: XThiefSettings,
    initialLink: String? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf(initialLink.orEmpty()) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var media by remember { mutableStateOf<TweetMedia?>(null) }
    var selected by remember { mutableStateOf<VideoVariant?>(null) }
    var showDirectoryDialog by remember { mutableStateOf(false) }
    var directoryDraft by remember { mutableStateOf(settings.customDirectory) }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            directoryDraft = folderPathFromUri(context, uri)
        }
    }

    fun resolve(target: String) {
        val tweetId = TweetLink.extractId(target)
        if (tweetId == null) {
            error = "请输入有效的 X / Twitter 视频链接"
            return
        }
        scope.launch {
            loading = true
            error = null
            media = null
            selected = null
            runCatching { container.extractor.resolve(tweetId) }
                .onSuccess { result ->
                    media = result
                    selected = when (settings.quality) {
                        DefaultQuality.WORST -> result.sortedVariants.lastOrNull()
                        else -> result.sortedVariants.firstOrNull()
                    }
                }
                .onFailure { error = it.message ?: "解析失败" }
            loading = false
        }
    }

    LaunchedEffect(initialLink) {
        val shared = initialLink?.takeIf { TweetLink.isXLink(it) }
        if (shared != null) {
            link = shared
            resolve(shared)
        }
    }

    LaunchedEffect(settings.autoPasteFromClipboard) {
        if (settings.autoPasteFromClipboard && link.isBlank()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val text = clipboard?.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
            if (TweetLink.isXLink(text)) {
                link = text
                resolve(text)
            }
        }
    }

    AppPage(title = "解析") {
        item {
            SectionCard(title = "视频链接") {
                ActionRow(
                    title = "下载链接",
                    subtitle = link.ifBlank { "未输入" },
                    onClick = { showLinkDialog = true },
                )
                ActionRow(
                    title = if (loading) "解析中…" else "解析视频",
                    subtitle = "获取该帖子的全部可用画质",
                    showChevron = false,
                    onClick = { resolve(link) },
                )
            }
        }

        val current = media
        item {
            AnimatedVisibility(
                visible = current != null,
                enter = fadeIn(tween(189)) +
                    scaleIn(tween(252, easing = FastOutSlowInEasing), initialScale = .96f) +
                    expandVertically(tween(252, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(140)) + shrinkVertically(tween(180)),
            ) {
                val shown = current
                if (shown != null) {
                    ResultCard(
                        media = shown,
                        settings = settings,
                        selected = selected,
                        onSelect = { selected = it },
                        onPickDirectory = { showDirectoryDialog = true },
                        onSelectDefault = {
                            container.settingsStore.update { it.copy(saveDirectoryMode = SAVE_DIR_DEFAULT) }
                        },
                        onChooseCustom = {
                            container.settingsStore.update { it.copy(saveDirectoryMode = SAVE_DIR_CUSTOM) }
                            showDirectoryDialog = true
                        },
                        onDownload = {
                            val chosen = selected ?: return@ResultCard
                            container.downloadManager.enqueue(shown, chosen, container.settingsStore.current)
                            toast(context, "已加入下载队列，可在「下载」页查看进度")
                        },
                    )
                }
            }
        }

        if (current == null && !loading) {
            item {
                EmptyState(
                    title = if (error == null) "还没有解析任务" else "解析失败",
                    description = error
                        ?: "点上方「下载链接」填入 X（Twitter）视频链接，再点「解析视频」获取全部画质；也可以直接从 X 分享链接给 XThief。",
                    icon = {
                        // Icon tints from the theme, so it stays visible in dark mode.
                        Icon(
                            MiuixIcons.Regular.Backup,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    },
                )
            }
        }
    }

    if (showLinkDialog) {
        LinkInputDialog(
            initial = link,
            onDismiss = { showLinkDialog = false },
            onSave = { value ->
                link = value.trim()
                showLinkDialog = false
                if (link.isNotBlank()) resolve(link)
            },
        )
    }

    if (showDirectoryDialog) {
        DirectoryDialog(
            initial = directoryDraft.ifBlank { settings.customDirectory },
            onPickFolder = { folderPicker.launch(null) },
            onDismiss = { showDirectoryDialog = false },
            onSave = { value ->
                val path = value.trim()
                if (path.isBlank()) {
                    toast(context, "请先选择或填写目录")
                } else {
                    if (!hasAllFilesAccess(context)) {
                        toast(context, "建议先在设置中授予「所有文件访问权限」")
                    }
                    container.settingsStore.update {
                        it.copy(saveDirectoryMode = SAVE_DIR_CUSTOM, customDirectory = path)
                    }
                    directoryDraft = path
                    showDirectoryDialog = false
                }
            },
        )
    }
}

/** Parsed-post card: quality picker, destination picker, then the post metadata. */
@Composable
private fun ResultCard(
    media: TweetMedia,
    settings: XThiefSettings,
    selected: VideoVariant?,
    onSelect: (VideoVariant) -> Unit,
    onPickDirectory: () -> Unit,
    onSelectDefault: () -> Unit,
    onChooseCustom: () -> Unit,
    onDownload: () -> Unit,
) {
    // The page's own recorded backdrop, so the glass dropdowns really sample the page content.
    val backdrop = LocalPageBackdrop.current
    val variants = media.sortedVariants
    val qualityLabels = variants.map { variant ->
        buildString {
            append(variant.resolutionLabel)
            if (variant.bitrate > 0) append(" · ${variant.bitrate / 1000}k")
        }
    }
    val qualityIndex = variants.indexOfFirst { it.url == selected?.url }.coerceAtLeast(0)
    val destinationIndex = if (settings.usesCustomDirectory) 1 else 0

    SectionCard {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            GlassChoiceRow(
                title = "清晰度",
                items = qualityLabels,
                selectedIndex = qualityIndex,
                backdrop = backdrop,
                onSelectedIndexChange = { index -> variants.getOrNull(index)?.let(onSelect) },
                modifier = Modifier.fillMaxWidth(),
            )
            GlassChoiceRow(
                title = "保存目录",
                items = listOf("默认", "自定义"),
                selectedIndex = destinationIndex,
                backdrop = backdrop,
                // Show where the file will actually land, whichever mode is selected.
                summary = settings.effectiveDirectory,
                onSelectedIndexChange = { index ->
                    if (index == 1) onChooseCustom() else onSelectDefault()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (settings.usesCustomDirectory) {
                ActionRow(
                    title = "自定义下载目录",
                    subtitle = settings.customDirectory.ifBlank { "未选择" },
                    onClick = onPickDirectory,
                )
            }

            Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                InfoRow("作者", TweetLink.displayAuthor(media.authorHandle, media.author))
                InfoRow("推文 ID", media.tweetId)
                if (media.text.isNotBlank()) {
                    InfoRow("内容", media.text.take(60))
                }
                InfoRow("可用画质", "${media.videos.size} 个")
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                GlassPrimaryButton(
                    text = selected?.let { "下载 ${it.resolutionLabel}" } ?: "请选择画质",
                    enabled = selected != null,
                    onClick = onDownload,
                )
            }
        }
    }
}

/** Dialog for entering the X post link, with clipboard helpers. */
@Composable
private fun LinkInputDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val context = LocalContext.current
    var draft by remember(initial) { mutableStateOf(initial) }
    WindowDialog(
        show = true,
        title = "下载链接",
        summary = initial.ifBlank { "未输入" },
        onDismissRequest = onDismiss,
        imeBottomSafetyFraction = .175f,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            XThiefTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = "https://x.com/…/status/…",
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassDialogButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val text = clipboard?.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                        if (text.isBlank()) toast(context, "剪贴板为空") else draft = text.trim()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("粘贴") }
                GlassDialogButton(
                    onClick = { draft = "" },
                    modifier = Modifier.weight(1f),
                ) { Text("清除") }
            }
            GlassDialogButton(
                onClick = { onSave(draft) },
                enabled = draft.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) { Text("保存") }
            GlassDialogButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("取消") }
        }
    }
}

/** Dialog for entering or picking the custom download directory. */
@Composable
fun DirectoryDialog(
    initial: String,
    onPickFolder: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    WindowDialog(
        show = true,
        title = "自定义下载目录",
        summary = initial.ifBlank { "未选择" },
        onDismissRequest = onDismiss,
        imeBottomSafetyFraction = .175f,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            XThiefTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = "/storage/emulated/0/Download",
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassDialogButton(
                    onClick = onPickFolder,
                    modifier = Modifier.weight(1f),
                ) { Text("选择") }
                GlassDialogButton(
                    onClick = { draft = "" },
                    modifier = Modifier.weight(1f),
                ) { Text("清除") }
            }
            GlassDialogButton(
                onClick = { onSave(draft) },
                enabled = draft.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) { Text("保存") }
            GlassDialogButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("取消") }
        }
    }
}
