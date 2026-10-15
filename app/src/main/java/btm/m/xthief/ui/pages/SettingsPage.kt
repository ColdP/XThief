// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui.pages

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import btm.m.xthief.BuildConfig
import btm.m.xthief.data.AppColorMode
import btm.m.xthief.data.DefaultQuality
import btm.m.xthief.data.FileNameRule
import btm.m.xthief.data.LabelMode
import btm.m.xthief.data.MAX_SIM_SECONDS
import btm.m.xthief.data.MIN_SIM_SECONDS
import btm.m.xthief.data.NavigationStyle
import btm.m.xthief.data.SAVE_DIR_CUSTOM
import btm.m.xthief.data.SAVE_DIR_DEFAULT
import btm.m.xthief.data.XThiefSettings
import btm.m.xthief.ui.AppPage
import btm.m.xthief.ui.DetailPage
import btm.m.xthief.ui.components.ActionRow
import btm.m.xthief.ui.components.BlurFadeVisibility
import btm.m.xthief.ui.components.EditableSliderPreference
import btm.m.xthief.ui.components.GlassChoiceRow
import btm.m.xthief.ui.components.GlassDialogButton
import btm.m.xthief.ui.components.GlassDropdownPreference
import btm.m.xthief.ui.components.SectionCard
import btm.m.xthief.ui.components.WindowDialog
import btm.m.xthief.ui.components.XThiefDialog
import btm.m.xthief.ui.components.XThiefTextField
import btm.m.xthief.ui.folderPathFromUri
import btm.m.xthief.ui.hasAllFilesAccess
import btm.m.xthief.ui.toast
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class ChoiceKey { NAV_STYLE, LABEL_MODE, COLOR_MODE, QUALITY, NAME_RULE }

/** Preset Super Island simulation durations, in seconds. */
private val SIM_DURATIONS = listOf(5, 15, 30, 60, 90, 120)

/** Index of the current duration, or the trailing "自定义" entry when it is not a preset. */
private fun simDurationIndex(seconds: Int): Int {
    val preset = SIM_DURATIONS.indexOf(seconds)
    return if (preset >= 0) preset else SIM_DURATIONS.size
}

private fun choicesFor(key: ChoiceKey): List<Pair<String, String>> = when (key) {
    ChoiceKey.NAV_STYLE -> NavigationStyle.entries.map { it.preferenceValue to it.displayName }
    ChoiceKey.LABEL_MODE -> LabelMode.entries.map { it.preferenceValue to it.displayName }
    ChoiceKey.COLOR_MODE -> AppColorMode.entries.map { it.preferenceValue to it.displayName }
    ChoiceKey.QUALITY -> DefaultQuality.entries.map { it.preferenceValue to it.displayName }
    ChoiceKey.NAME_RULE -> FileNameRule.entries.map { it.preferenceValue to it.displayName }
}

private fun currentValueFor(key: ChoiceKey, settings: XThiefSettings): String = when (key) {
    ChoiceKey.NAV_STYLE -> settings.navigationStyle
    ChoiceKey.LABEL_MODE -> settings.labelMode
    ChoiceKey.COLOR_MODE -> settings.colorMode
    ChoiceKey.QUALITY -> settings.defaultQuality
    ChoiceKey.NAME_RULE -> settings.fileNameRule
}

private fun applyChoice(
    key: ChoiceKey,
    value: String,
    update: ((XThiefSettings) -> XThiefSettings) -> Unit,
) {
    update {
        when (key) {
            ChoiceKey.NAV_STYLE -> it.copy(navigationStyle = value)
            ChoiceKey.LABEL_MODE -> it.copy(labelMode = value)
            ChoiceKey.COLOR_MODE -> it.copy(colorMode = value)
            ChoiceKey.QUALITY -> it.copy(defaultQuality = value)
            ChoiceKey.NAME_RULE -> it.copy(fileNameRule = value)
        }
    }
}

/** Single-page settings, grouped in MIUIX cards like HyperChanger. */
@Composable
fun SettingsPage(
    settings: XThiefSettings,
    update: ((XThiefSettings) -> XThiefSettings) -> Unit,
    onRefreshLibrary: () -> Unit,
    onClearHistory: () -> Unit,
    historyCount: Int,
    libraryCount: Int,
    backdrop: Backdrop? = null,
    allFilesAccess: Boolean = false,
    islandStatus: String? = null,
    onRequestAllFilesAccess: () -> Unit = {},
    onSimulateIsland: () -> Unit = {},
    onOpenPage: (DetailPage) -> Unit = {},
) {
    var directoryDraft by remember { mutableStateOf(settings.customDirectory) }
    var editingDirectory by remember { mutableStateOf(false) }
    var confirmClearHistory by remember { mutableStateOf(false) }
    var editingCookie by remember { mutableStateOf(false) }
    var cookieAuthDraft by remember { mutableStateOf(settings.cookieAuthToken) }
    var cookieCsrfDraft by remember { mutableStateOf(settings.cookieCsrfToken) }
    var cookieUserDraft by remember { mutableStateOf(settings.cookieUserId) }
    var cookieGuestDraft by remember { mutableStateOf(settings.cookieGuestId) }
    val context = LocalContext.current
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

    AppPage(title = "设置") {
        item {
            SectionCard(title = "外观") {
                GlassChoiceDropdown("深色模式", settings, ChoiceKey.COLOR_MODE, backdrop, update)
                GlassChoiceDropdown("导航栏样式", settings, ChoiceKey.NAV_STYLE, backdrop, update)
                GlassChoiceDropdown("标签显示", settings, ChoiceKey.LABEL_MODE, backdrop, update)
            }
        }
        item {
            SectionCard(title = "下载") {
                GlassChoiceDropdown("默认画质", settings, ChoiceKey.QUALITY, backdrop, update)
                GlassChoiceDropdown("命名规则", settings, ChoiceKey.NAME_RULE, backdrop, update)
                if (settings.nameRule == FileNameRule.CUSTOM) {
                    LabeledInput(
                        label = "自定义前缀",
                        value = settings.customPrefix,
                        onValueChange = { value -> update { it.copy(customPrefix = value) } },
                    )
                }
                EditableSliderPreference(
                    value = settings.concurrentDownloads.toFloat(),
                    onValueChange = { value -> update { it.copy(concurrentDownloads = value.toInt()) } },
                    onValueChangeFinished = { },
                    title = "同时下载数",
                    valueText = settings.concurrentDownloads.toString(),
                    valueRange = 1f..4f,
                    steps = 2,
                    defaultValue = 2f,
                )
                SwitchPreference(
                    checked = settings.wifiOnly,
                    onCheckedChange = { value -> update { it.copy(wifiOnly = value) } },
                    title = "仅 Wi-Fi 下载",
                    summary = "移动网络下暂停下载任务",
                )
                SwitchPreference(
                    checked = settings.showDownloadNotifications,
                    onCheckedChange = { value -> update { it.copy(showDownloadNotifications = value) } },
                    title = "下载完成通知",
                    summary = "任务完成时发送系统通知",
                )
                SwitchPreference(
                    checked = settings.islandEnabled,
                    onCheckedChange = { value -> update { it.copy(islandEnabled = value) } },
                    title = "通过小米超级岛显示下载任务",
                    summary = "在超级岛显示下载进度与结果（摘要态与展开态）",
                )
                // Only meaningful while the island switch is on; revealed with the same blur
                // fade used by the collapsible download groups.
                BlurFadeVisibility(visible = settings.islandEnabled) {
                    Column(Modifier.fillMaxWidth()) {
                        GlassChoiceRow(
                            title = "模拟下载时长",
                            items = SIM_DURATIONS.map { "${it}s" } + "自定义",
                            selectedIndex = simDurationIndex(settings.islandSimulationSeconds),
                            backdrop = backdrop,
                            onSelectedIndexChange = { index ->
                                val value = SIM_DURATIONS.getOrNull(index)
                                if (value != null) {
                                    update { it.copy(islandSimulationSeconds = value) }
                                } else {
                                    // Entering custom mode keeps the current value as a starting
                                    // point, clamped into the allowed slider range.
                                    update {
                                        it.copy(
                                            islandSimulationSeconds = it.islandSimulationSeconds
                                                .coerceIn(MIN_SIM_SECONDS, MAX_SIM_SECONDS),
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        BlurFadeVisibility(
                            visible = !SIM_DURATIONS.contains(settings.islandSimulationSeconds),
                        ) {
                            EditableSliderPreference(
                                value = settings.islandSimulationSeconds.toFloat(),
                                onValueChange = { value ->
                                    update { it.copy(islandSimulationSeconds = value.toInt()) }
                                },
                                onValueChangeFinished = { },
                                title = "自定义时长",
                                valueText = "${settings.islandSimulationSeconds}s",
                                valueRange = MIN_SIM_SECONDS.toFloat()..MAX_SIM_SECONDS.toFloat(),
                                steps = MAX_SIM_SECONDS - MIN_SIM_SECONDS - 1,
                                defaultValue = 15f,
                            )
                        }
                        ActionRow(
                            title = "小米超级岛模拟",
                            subtitle = islandStatus
                                ?: "发送一条模拟的进度超级岛，用于验证岛上显示效果",
                            onClick = onSimulateIsland,
                        )
                    }
                }
                SwitchPreference(
                    checked = settings.saveThumbnail,
                    onCheckedChange = { value -> update { it.copy(saveThumbnail = value) } },
                    title = "同时保存封面",
                    summary = "把视频封面一并保存到媒体库（实验性）",
                )
            }
        }
        item {
            SectionCard(title = "保存位置") {
                GlassChoiceRow(
                    title = "保存目录",
                    items = listOf("默认", "自定义"),
                    selectedIndex = if (settings.usesCustomDirectory) 1 else 0,
                    backdrop = backdrop,
                    onSelectedIndexChange = { index ->
                        if (index == 1) {
                            update { it.copy(saveDirectoryMode = SAVE_DIR_CUSTOM) }
                            editingDirectory = true
                        } else {
                            update { it.copy(saveDirectoryMode = SAVE_DIR_DEFAULT) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (settings.usesCustomDirectory) {
                    ActionRow(
                        title = "自定义下载目录",
                        subtitle = settings.customDirectory.ifBlank { "未选择" },
                        onClick = { editingDirectory = true },
                    )
                } else {
                    ActionRow(
                        title = "默认目录",
                        subtitle = settings.effectiveDirectory,
                        showChevron = false,
                        onClick = null,
                    )
                }
                SwitchPreference(
                    checked = settings.autoPasteFromClipboard,
                    onCheckedChange = { value -> update { it.copy(autoPasteFromClipboard = value) } },
                    title = "自动读取剪贴板",
                    summary = "打开下载页时自动填入剪贴板中的 X 链接",
                )
            }
        }
        item {
            SectionCard(title = "记录与媒体") {
                EditableSliderPreference(
                    value = settings.keepHistoryDays.toFloat(),
                    onValueChange = { value -> update { it.copy(keepHistoryDays = value.toInt()) } },
                    onValueChangeFinished = { },
                    title = "历史自动清理",
                    valueText = if (settings.keepHistoryDays == 0) "永久" else "${settings.keepHistoryDays} 天",
                    valueRange = 0f..90f,
                    steps = 89,
                    defaultValue = 0f,
                )
                ActionRow(
                    title = "重新扫描媒体库",
                    subtitle = "当前 $libraryCount 个视频",
                    onClick = onRefreshLibrary,
                )
                ActionRow(
                    title = "清空下载历史",
                    subtitle = "当前 $historyCount 条记录，不影响已下载文件",
                    onClick = { confirmClearHistory = true },
                )
            }
        }
        item {
            SectionCard(title = "权限") {
                ActionRow(
                    title = "所有文件访问权限",
                    subtitle = if (allFilesAccess) "已授予" else "未授予 · 点击前往系统设置授权",
                    onClick = onRequestAllFilesAccess,
                )
                SwitchPreference(
                    checked = settings.cookieEnabled,
                    onCheckedChange = { value -> update { it.copy(cookieEnabled = value) } },
                    title = "使用 Cookies 解析",
                    summary = "带上 X 的登录 Cookie，解析仅登录可见的内容",
                )
                // Off by default; when off the row is hidden and no cookie is ever sent.
                BlurFadeVisibility(visible = settings.cookieEnabled) {
                    ActionRow(
                        title = "Cookies 内容",
                        subtitle = when {
                            !settings.cookieConfigured -> "未设置 · 需要 auth_token 与 ct0"
                            else -> buildString {
                                append("已设置 · auth_token、ct0")
                                if (settings.cookieUserId.isNotBlank()) append("、twid")
                                if (settings.cookieGuestId.isNotBlank()) append("、guest_id")
                            }
                        },
                        onClick = { editingCookie = true },
                    )
                }
            }
        }
        item {
            SectionCard(title = "其他") {
                ActionRow(
                    title = "关于",
                    subtitle = "版本 ${BuildConfig.VERSION_NAME}",
                    onClick = { onOpenPage(DetailPage.ABOUT) },
                )
                ActionRow(
                    title = "开源代码声明",
                    subtitle = "本项目使用的开源项目",
                    onClick = { onOpenPage(DetailPage.OPEN_SOURCE) },
                )
                ActionRow(
                    title = "LICENSE",
                    subtitle = "Apache License 2.0",
                    onClick = { onOpenPage(DetailPage.LICENSE) },
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    if (editingDirectory) {
        DirectoryDialog(
            initial = directoryDraft.ifBlank { settings.customDirectory },
            onPickFolder = { folderPicker.launch(null) },
            onDismiss = { editingDirectory = false },
            onSave = { value ->
                val path = value.trim()
                if (path.isNotBlank()) {
                    if (!hasAllFilesAccess(context)) {
                        toast(context, "建议先授予「所有文件访问权限」")
                    }
                    update { it.copy(saveDirectoryMode = SAVE_DIR_CUSTOM, customDirectory = path) }
                    directoryDraft = path
                }
                editingDirectory = false
            },
        )
    }
    if (confirmClearHistory) {
        XThiefDialog(
            title = "清空下载历史",
            message = "所有历史记录都会被删除，已下载的视频文件不受影响。",
            confirmText = "清空",
            destructive = true,
            onConfirm = {
                confirmClearHistory = false
                onClearHistory()
            },
            onDismiss = { confirmClearHistory = false },
        )
    }

    if (editingCookie) {
        CookieDialog(
            authToken = cookieAuthDraft,
            csrfToken = cookieCsrfDraft,
            userId = cookieUserDraft,
            guestId = cookieGuestDraft,
            onDismiss = { editingCookie = false },
            onSave = { auth, csrf, user, guest ->
                cookieAuthDraft = auth
                cookieCsrfDraft = csrf
                cookieUserDraft = user
                cookieGuestDraft = guest
                update {
                    it.copy(
                        cookieAuthToken = auth,
                        cookieCsrfToken = csrf,
                        cookieUserId = user,
                        cookieGuestId = guest,
                    )
                }
                editingCookie = false
            },
        )
    }
}

/**
 * Editor for the X login cookies. `auth_token` and `ct0` are required, `twid` / `guest_id` may be
 * left empty; a pasted whole cookie string is split into the individual fields.
 */
@Composable
private fun CookieDialog(
    authToken: String,
    csrfToken: String,
    userId: String,
    guestId: String,
    onDismiss: () -> Unit,
    onSave: (auth: String, csrf: String, user: String, guest: String) -> Unit,
) {
    val context = LocalContext.current
    var auth by remember(authToken) { mutableStateOf(authToken) }
    var csrf by remember(csrfToken) { mutableStateOf(csrfToken) }
    var user by remember(userId) { mutableStateOf(userId) }
    var guest by remember(guestId) { mutableStateOf(guestId) }
    // Only the required pair gates saving; optional fields may stay empty.
    val canSave = auth.isNotBlank() && csrf.isNotBlank()

    WindowDialog(
        show = true,
        title = "Cookies 内容",
        summary = if (canSave) "auth_token 与 ct0 已填写" else "auth_token 与 ct0 为必填",
        onDismissRequest = onDismiss,
        imeBottomSafetyFraction = .32f,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "在浏览器登录 X 后，从开发者工具的 Application → Cookies 中复制对应值；带 * 的为必填，可选项留空即可。",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            CookieField(label = "auth_token *", value = auth, hint = "登录凭证（必需）") { auth = it }
            CookieField(label = "ct0 *", value = csrf, hint = "CSRF 令牌（必需）") { csrf = it }
            CookieField(label = "twid", value = user, hint = "用户 ID（可选）") { user = it }
            CookieField(label = "guest_id", value = guest, hint = "访客标识（可选）") { guest = it }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassDialogButton(
                    onClick = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val text = clipboard?.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                        if (text.isBlank()) {
                            toast(context, "剪贴板为空")
                        } else {
                            val parsed = parseCookieString(text)
                            parsed["auth_token"]?.let { auth = it }
                            parsed["ct0"]?.let { csrf = it }
                            parsed["twid"]?.let { user = it }
                            parsed["guest_id"]?.let { guest = it }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("粘贴整串") }
                GlassDialogButton(
                    onClick = {
                        auth = ""
                        csrf = ""
                        user = ""
                        guest = ""
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("清空") }
            }
            GlassDialogButton(
                onClick = { onSave(auth.trim(), csrf.trim(), user.trim(), guest.trim()) },
                enabled = canSave,
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

/** One labelled cookie input inside [CookieDialog]. */
@Composable
private fun CookieField(
    label: String,
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        XThiefTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = hint,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Best-effort split of a pasted `a=1; b=2` cookie string, so the user can either fill the fields
 * by hand or paste the whole header at once.
 */
private fun parseCookieString(raw: String): Map<String, String> {
    val values = mutableMapOf<String, String>()
    raw.split(';').forEach { part ->
        val trimmed = part.trim()
        val separator = trimmed.indexOf('=')
        if (separator > 0) {
            val key = trimmed.substring(0, separator).trim()
            val value = trimmed.substring(separator + 1).trim()
            if (key.isNotEmpty() && value.isNotEmpty()) values[key] = value
        }
    }
    // twid arrives URL-encoded as `u%3D<id>`; keep just the digits for display/editing.
    values["twid"]?.let { raw ->
        val digits = raw.filter(Char::isDigit)
        if (digits.isNotEmpty()) values["twid"] = digits
    }
    return values
}

/** MIUIX dropdown + HyperChanger's own Backdrop glass menu, reused as-is. */
@Composable
private fun GlassChoiceDropdown(
    title: String,
    settings: XThiefSettings,
    key: ChoiceKey,
    backdrop: Backdrop?,
    update: ((XThiefSettings) -> XThiefSettings) -> Unit,
) {
    val options = choicesFor(key)
    val current = currentValueFor(key, settings)
    val index = options.indexOfFirst { it.first == current }.coerceAtLeast(0)
    GlassDropdownPreference(
        title = title,
        items = options.map { it.second },
        selectedIndex = index,
        backdrop = backdrop,
        showValueOnEnd = true,
        onSelectedIndexChange = { selected ->
            options.getOrNull(selected)?.let { applyChoice(key, it.first, update) }
        },
    )
}

@Composable
private fun LabeledInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(Modifier.height(6.dp))
        XThiefTextField(value = value, onValueChange = onValueChange)
    }
}
