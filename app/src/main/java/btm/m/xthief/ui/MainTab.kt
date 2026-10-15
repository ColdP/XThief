// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui

import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Backup
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Photos
import top.yukonga.miuix.kmp.icon.extended.Settings

/** The four primary destinations of XThief. */
enum class MainTab(val label: String, val icon: ImageVector) {
    PARSE("解析", MiuixIcons.Regular.Backup),
    DOWNLOAD("下载", MiuixIcons.Regular.Download),
    LIBRARY("媒体库", MiuixIcons.Regular.Photos),
    SETTINGS("设置", MiuixIcons.Regular.Settings),
}
