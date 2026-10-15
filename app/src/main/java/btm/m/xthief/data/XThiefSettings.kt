// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Bottom navigation styles, mirroring HyperChanger's floating variants. */
enum class NavigationStyle(val preferenceValue: String, val displayName: String) {
    LIQUID_GLASS("liquid_glass", "液态玻璃底栏"),
    HYPER_OS("hyper_os", "HyperOS 底栏"),
    HYPER_OS_FLOATING("hyper_os_floating", "HyperOS 悬浮底栏"),
    HARMONY_OS_FLOATING("harmony_os_floating", "HarmonyOS 悬浮底栏");

    companion object {
        fun fromPreference(value: String?): NavigationStyle =
            entries.firstOrNull { it.preferenceValue == value } ?: HYPER_OS_FLOATING
    }
}

enum class LabelMode(val preferenceValue: String, val displayName: String) {
    ICON_AND_TEXT("icon_and_text", "图标和文本"),
    ICON_ONLY("icon_only", "仅图标"),
    TEXT_ONLY("text_only", "仅文本");

    companion object {
        fun fromPreference(value: String?): LabelMode =
            entries.firstOrNull { it.preferenceValue == value } ?: ICON_AND_TEXT
    }
}

enum class AppColorMode(val preferenceValue: String, val displayName: String) {
    SYSTEM("system", "跟随系统"),
    LIGHT("light", "浅色模式"),
    DARK("dark", "深色模式");

    companion object {
        fun fromPreference(value: String?): AppColorMode =
            entries.firstOrNull { it.preferenceValue == value } ?: SYSTEM
    }
}

enum class FileNameRule(val preferenceValue: String, val displayName: String) {
    AUTHOR_DATE("author_date", "作者 + 日期"),
    TWEET_ID("tweet_id", "推文 ID"),
    ORIGINAL("original", "原始文件名"),
    CUSTOM("custom", "自定义前缀");

    companion object {
        fun fromPreference(value: String?): FileNameRule =
            entries.firstOrNull { it.preferenceValue == value } ?: AUTHOR_DATE
    }
}

enum class DefaultQuality(val preferenceValue: String, val displayName: String) {
    ASK("ask", "每次询问"),
    BEST("best", "最高画质"),
    WORST("worst", "最低画质");

    companion object {
        fun fromPreference(value: String?): DefaultQuality =
            entries.firstOrNull { it.preferenceValue == value } ?: ASK
    }
}

/** Persisted user preferences for XThief. */
data class XThiefSettings(
    val navigationStyle: String = NavigationStyle.HYPER_OS_FLOATING.preferenceValue,
    val labelMode: String = LabelMode.ICON_AND_TEXT.preferenceValue,
    val colorMode: String = AppColorMode.SYSTEM.preferenceValue,
    /** SAVE_DIR_DEFAULT writes into the MediaStore Movies collection; SAVE_DIR_CUSTOM uses [customDirectory]. */
    val saveDirectoryMode: String = SAVE_DIR_DEFAULT,
    val customDirectory: String = "",
    val fileNameRule: String = FileNameRule.AUTHOR_DATE.preferenceValue,
    val customPrefix: String = "XThief",
    val defaultQuality: String = DefaultQuality.ASK.preferenceValue,
    val concurrentDownloads: Int = 2,
    val wifiOnly: Boolean = false,
    val keepHistoryDays: Int = 0,
    val showDownloadNotifications: Boolean = true,
    /** Show download progress and results on Xiaomi's Super Island. */
    val islandEnabled: Boolean = true,
    /** Simulated island duration in seconds; -1 means a custom value is used. */
    val islandSimulationSeconds: Int = 15,
    val saveThumbnail: Boolean = false,
    val autoPasteFromClipboard: Boolean = true,
    val dynamicColorFromThumbnail: Boolean = false,
    /** Sends the user's X login cookies while resolving, for age-gated or protected posts. */
    val cookieEnabled: Boolean = false,
    /** Required login credential (`auth_token`). */
    val cookieAuthToken: String = "",
    /** Required CSRF token (`ct0`); also sent as `x-csrf-token`. */
    val cookieCsrfToken: String = "",
    /** Optional user id (`twid`). */
    val cookieUserId: String = "",
    /** Optional guest id (`guest_id`). */
    val cookieGuestId: String = "",
    /**
     * YouTube cookies, entered field by field like the browser's cookie table lists them.
     * `SID` is the one that matters; the rest are what YouTube's account auth actually checks.
     */
    val youtubeSid: String = "",
    val youtubeHsid: String = "",
    val youtubeSsid: String = "",
    val youtubeApisid: String = "",
    val youtubeSapisid: String = "",
    /**
     * Bilibili cookies, entered one field at a time the way a browser's cookie table lists them.
     * `SESSDATA` alone is usually enough; `bili_jct` and `DedeUserID` are optional extras.
     */
    val bilibiliSessdata: String = "",
    val bilibiliJct: String = "",
    val bilibiliUserId: String = "",
    /** Media library layout: a plain list or a two-column staggered grid. */
    val libraryViewMode: String = LIBRARY_VIEW_LIST,
    /** Blurs the cover of videos whose source post was flagged sensitive. */
    val blurSensitiveCovers: Boolean = true,
    /** Full-screen, swipe-up/down-to-switch playback in the built-in player. */
    val shortVideoMode: Boolean = false,
) {
    val navStyle: NavigationStyle get() = NavigationStyle.fromPreference(navigationStyle)
    val navLabelMode: LabelMode get() = LabelMode.fromPreference(labelMode)
    val appColorMode: AppColorMode get() = AppColorMode.fromPreference(colorMode)
    val nameRule: FileNameRule get() = FileNameRule.fromPreference(fileNameRule)
    val quality: DefaultQuality get() = DefaultQuality.fromPreference(defaultQuality)
    val usesCustomDirectory: Boolean get() = saveDirectoryMode == SAVE_DIR_CUSTOM

    /** Directory shown in the UI: the picked path, or the default Movies folder. */
    val effectiveDirectory: String
        get() = if (usesCustomDirectory && customDirectory.isNotBlank()) customDirectory else DEFAULT_SAVE_DIR

    /** True when the two required cookie pieces are present. */
    val cookieConfigured: Boolean
        get() = cookieAuthToken.isNotBlank() && cookieCsrfToken.isNotBlank()

    /**
     * Assembled `Cookie` header for X requests, or null when cookie mode is off or the required
     * `auth_token` / `ct0` pair is missing. Optional cookies are only included when filled in.
     */
    val cookieHeader: String?
        get() {
            if (!cookieEnabled || !cookieConfigured) return null
            return buildList {
                add("auth_token=${cookieAuthToken.trim()}")
                add("ct0=${cookieCsrfToken.trim()}")
                // twid is stored URL-encoded as `u=<id>`; accept a bare id from the user.
                cookieUserId.filter(Char::isDigit).takeIf(String::isNotEmpty)?.let {
                    add("twid=u%3D$it")
                }
                cookieGuestId.trim().takeIf(String::isNotEmpty)?.let { add("guest_id=$it") }
            }.joinToString("; ")
        }

    /** Value for the `x-csrf-token` header, or null when cookie mode is not usable. */
    val cookieCsrfHeader: String?
        get() = cookieHeader?.let { cookieCsrfToken.trim() }

    /**
     * Assembled `Cookie:` header for Bilibili, or null when `SESSDATA` is missing.
     *
     * The fields are stored separately because that is how a browser lists them, and `SESSDATA`
     * values contain commas / `%2C` that must reach the server untouched, so nothing is re-encoded
     * here.
     */
    val bilibiliCookieHeader: String?
        get() {
            val sessdata = bilibiliSessdata.trim()
            if (sessdata.isEmpty()) return null
            return buildList {
                add("SESSDATA=$sessdata")
                bilibiliJct.trim().takeIf { it.isNotEmpty() }?.let { add("bili_jct=$it") }
                bilibiliUserId.trim().takeIf { it.isNotEmpty() }?.let { add("DedeUserID=$it") }
            }.joinToString("; ")
        }

    /**
     * Assembled `Cookie:` header for YouTube, or null when `SID` is missing.
     *
     * YouTube's account auth reads this whole set, so they are stored individually and joined here
     * rather than asking for one pasted string.
     */
    val youtubeCookieHeader: String?
        get() {
            val sid = youtubeSid.trim()
            if (sid.isEmpty()) return null
            return buildList {
                add("SID=$sid")
                youtubeHsid.trim().takeIf { it.isNotEmpty() }?.let { add("HSID=$it") }
                youtubeSsid.trim().takeIf { it.isNotEmpty() }?.let { add("SSID=$it") }
                youtubeApisid.trim().takeIf { it.isNotEmpty() }?.let { add("APISID=$it") }
                youtubeSapisid.trim().takeIf { it.isNotEmpty() }?.let { add("SAPISID=$it") }
            }.joinToString("; ")
        }
}

class XThiefSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_GROUP, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<XThiefSettings> = _state.asStateFlow()

    val current: XThiefSettings get() = _state.value

    fun update(transform: (XThiefSettings) -> XThiefSettings) {
        val next = transform(_state.value).normalized()
        _state.value = next
        write(next)
    }

    private fun read() = XThiefSettings(
        navigationStyle = prefs.getString(KEY_NAV_STYLE, null) ?: XThiefSettings().navigationStyle,
        labelMode = prefs.getString(KEY_LABEL_MODE, null) ?: XThiefSettings().labelMode,
        colorMode = prefs.getString(KEY_COLOR_MODE, null) ?: XThiefSettings().colorMode,
        saveDirectoryMode = prefs.getString(KEY_SAVE_DIR_MODE, null) ?: SAVE_DIR_DEFAULT,
        customDirectory = prefs.getString(KEY_CUSTOM_DIR, null).orEmpty(),
        fileNameRule = prefs.getString(KEY_NAME_RULE, null) ?: XThiefSettings().fileNameRule,
        customPrefix = prefs.getString(KEY_CUSTOM_PREFIX, null) ?: "XThief",
        defaultQuality = prefs.getString(KEY_DEFAULT_QUALITY, null) ?: XThiefSettings().defaultQuality,
        concurrentDownloads = prefs.getInt(KEY_CONCURRENCY, 2),
        wifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, false),
        keepHistoryDays = prefs.getInt(KEY_HISTORY_DAYS, 0),
        showDownloadNotifications = prefs.getBoolean(KEY_NOTIFICATIONS, true),
        islandEnabled = prefs.getBoolean(KEY_ISLAND, true),
        islandSimulationSeconds = prefs.getInt(KEY_ISLAND_SIM_SECONDS, 15),
        saveThumbnail = prefs.getBoolean(KEY_SAVE_THUMBNAIL, false),
        autoPasteFromClipboard = prefs.getBoolean(KEY_AUTO_PASTE, true),
        dynamicColorFromThumbnail = prefs.getBoolean(KEY_DYNAMIC_COLOR, false),
        cookieEnabled = prefs.getBoolean(KEY_COOKIE_ENABLED, false),
        cookieAuthToken = prefs.getString(KEY_COOKIE_AUTH, null).orEmpty(),
        cookieCsrfToken = prefs.getString(KEY_COOKIE_CSRF, null).orEmpty(),
        cookieUserId = prefs.getString(KEY_COOKIE_USER, null).orEmpty(),
        cookieGuestId = prefs.getString(KEY_COOKIE_GUEST, null).orEmpty(),
        youtubeSid = prefs.getString(KEY_COOKIE_YT_SID, null).orEmpty(),
        youtubeHsid = prefs.getString(KEY_COOKIE_YT_HSID, null).orEmpty(),
        youtubeSsid = prefs.getString(KEY_COOKIE_YT_SSID, null).orEmpty(),
        youtubeApisid = prefs.getString(KEY_COOKIE_YT_APISID, null).orEmpty(),
        youtubeSapisid = prefs.getString(KEY_COOKIE_YT_SAPISID, null).orEmpty(),
        bilibiliSessdata = prefs.getString(KEY_COOKIE_BILI_SESSDATA, null).orEmpty(),
        bilibiliJct = prefs.getString(KEY_COOKIE_BILI_JCT, null).orEmpty(),
        bilibiliUserId = prefs.getString(KEY_COOKIE_BILI_USERID, null).orEmpty(),
        libraryViewMode = prefs.getString(KEY_LIBRARY_VIEW, null) ?: LIBRARY_VIEW_LIST,
        blurSensitiveCovers = prefs.getBoolean(KEY_BLUR_SENSITIVE, true),
        shortVideoMode = prefs.getBoolean(KEY_SHORT_VIDEO, false),
    )

    private fun write(value: XThiefSettings) {
        prefs.edit()
            .putString(KEY_NAV_STYLE, value.navigationStyle)
            .putString(KEY_LABEL_MODE, value.labelMode)
            .putString(KEY_COLOR_MODE, value.colorMode)
            .putString(KEY_SAVE_DIR_MODE, value.saveDirectoryMode)
            .putString(KEY_CUSTOM_DIR, value.customDirectory)
            .putString(KEY_NAME_RULE, value.fileNameRule)
            .putString(KEY_CUSTOM_PREFIX, value.customPrefix)
            .putString(KEY_DEFAULT_QUALITY, value.defaultQuality)
            .putInt(KEY_CONCURRENCY, value.concurrentDownloads)
            .putBoolean(KEY_WIFI_ONLY, value.wifiOnly)
            .putInt(KEY_HISTORY_DAYS, value.keepHistoryDays)
            .putBoolean(KEY_NOTIFICATIONS, value.showDownloadNotifications)
            .putBoolean(KEY_ISLAND, value.islandEnabled)
            .putInt(KEY_ISLAND_SIM_SECONDS, value.islandSimulationSeconds)
            .putBoolean(KEY_SAVE_THUMBNAIL, value.saveThumbnail)
            .putBoolean(KEY_AUTO_PASTE, value.autoPasteFromClipboard)
            .putBoolean(KEY_DYNAMIC_COLOR, value.dynamicColorFromThumbnail)
            .putBoolean(KEY_COOKIE_ENABLED, value.cookieEnabled)
            .putString(KEY_COOKIE_AUTH, value.cookieAuthToken)
            .putString(KEY_COOKIE_CSRF, value.cookieCsrfToken)
            .putString(KEY_COOKIE_USER, value.cookieUserId)
            .putString(KEY_COOKIE_GUEST, value.cookieGuestId)
            .putString(KEY_COOKIE_YT_SID, value.youtubeSid)
            .putString(KEY_COOKIE_YT_HSID, value.youtubeHsid)
            .putString(KEY_COOKIE_YT_SSID, value.youtubeSsid)
            .putString(KEY_COOKIE_YT_APISID, value.youtubeApisid)
            .putString(KEY_COOKIE_YT_SAPISID, value.youtubeSapisid)
            .putString(KEY_COOKIE_BILI_SESSDATA, value.bilibiliSessdata)
            .putString(KEY_COOKIE_BILI_JCT, value.bilibiliJct)
            .putString(KEY_COOKIE_BILI_USERID, value.bilibiliUserId)
            .putString(KEY_LIBRARY_VIEW, value.libraryViewMode)
            .putBoolean(KEY_BLUR_SENSITIVE, value.blurSensitiveCovers)
            .putBoolean(KEY_SHORT_VIDEO, value.shortVideoMode)
            .apply()
    }

    private fun XThiefSettings.normalized() = copy(
        saveDirectoryMode = if (saveDirectoryMode == SAVE_DIR_CUSTOM) SAVE_DIR_CUSTOM else SAVE_DIR_DEFAULT,
        islandSimulationSeconds = islandSimulationSeconds.coerceIn(MIN_SIM_SECONDS, MAX_SIM_SECONDS),
        navigationStyle = NavigationStyle.fromPreference(navigationStyle).preferenceValue,
        labelMode = LabelMode.fromPreference(labelMode).preferenceValue,
        colorMode = AppColorMode.fromPreference(colorMode).preferenceValue,
        fileNameRule = FileNameRule.fromPreference(fileNameRule).preferenceValue,
        defaultQuality = DefaultQuality.fromPreference(defaultQuality).preferenceValue,
        concurrentDownloads = concurrentDownloads.coerceIn(1, 4),
        keepHistoryDays = keepHistoryDays.coerceIn(0, 365),
    )
}

private const val PREF_GROUP = "xthief_settings"
private const val KEY_NAV_STYLE = "navigation_style"
private const val KEY_LABEL_MODE = "navigation_label_mode"
private const val KEY_COLOR_MODE = "color_mode"
private const val KEY_SAVE_DIR_MODE = "save_directory_mode"
private const val KEY_CUSTOM_DIR = "custom_directory"
private const val KEY_NAME_RULE = "file_name_rule"
private const val KEY_CUSTOM_PREFIX = "custom_prefix"
private const val KEY_DEFAULT_QUALITY = "default_quality"
private const val KEY_CONCURRENCY = "concurrent_downloads"
private const val KEY_WIFI_ONLY = "wifi_only"
private const val KEY_HISTORY_DAYS = "history_keep_days"
private const val KEY_NOTIFICATIONS = "download_notifications"
private const val KEY_ISLAND = "island_enabled"
private const val KEY_ISLAND_SIM_SECONDS = "island_simulation_seconds"
private const val KEY_SAVE_THUMBNAIL = "save_thumbnail"
private const val KEY_AUTO_PASTE = "auto_paste_clipboard"
private const val KEY_DYNAMIC_COLOR = "dynamic_color_from_thumbnail"
private const val KEY_COOKIE_ENABLED = "cookie_enabled"
private const val KEY_COOKIE_AUTH = "cookie_auth_token"
private const val KEY_COOKIE_CSRF = "cookie_ct0"
private const val KEY_COOKIE_USER = "cookie_twid"
private const val KEY_COOKIE_GUEST = "cookie_guest_id"
private const val KEY_COOKIE_YT_SID = "cookie_yt_sid"
private const val KEY_COOKIE_YT_HSID = "cookie_yt_hsid"
private const val KEY_COOKIE_YT_SSID = "cookie_yt_ssid"
private const val KEY_COOKIE_YT_APISID = "cookie_yt_apisid"
private const val KEY_COOKIE_YT_SAPISID = "cookie_yt_sapisid"
private const val KEY_COOKIE_BILI_SESSDATA = "cookie_bili_sessdata"
private const val KEY_COOKIE_BILI_JCT = "cookie_bili_jct"
private const val KEY_COOKIE_BILI_USERID = "cookie_bili_userid"
private const val KEY_LIBRARY_VIEW = "library_view_mode"
private const val KEY_BLUR_SENSITIVE = "blur_sensitive_covers"
private const val KEY_SHORT_VIDEO = "short_video_mode"

/** Media library layouts. */
const val LIBRARY_VIEW_LIST = "list"
const val LIBRARY_VIEW_GRID = "grid"

/** Save-destination modes and the default MediaStore collection. */
const val SAVE_DIR_DEFAULT = "default"
const val SAVE_DIR_CUSTOM = "custom"
const val DEFAULT_SAVE_DIR = "Movies/XThief"

/** Bounds for the simulated Super Island duration in seconds. */
const val MIN_SIM_SECONDS = 5
const val MAX_SIM_SECONDS = 600
