// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief

import android.app.Application
import btm.m.xthief.data.HistoryStore
import btm.m.xthief.data.MediaLibrary
import btm.m.xthief.data.XThiefSettingsStore
import btm.m.xthief.x.IslandNotifier
import btm.m.xthief.x.XDownloadManager
import btm.m.xthief.x.XVideoExtractor

/** Process-wide singletons shared by every screen. */
class XThiefApplication : Application() {
    val settingsStore: XThiefSettingsStore by lazy { XThiefSettingsStore(this) }
    val historyStore: HistoryStore by lazy { HistoryStore(this) }
    val mediaLibrary: MediaLibrary by lazy { MediaLibrary(this, historyStore) }
    val extractor: XVideoExtractor by lazy { XVideoExtractor(settingsStore) }
    val downloadManager: XDownloadManager by lazy { XDownloadManager(this, settingsStore) }
    val islandNotifier: IslandNotifier by lazy { IslandNotifier(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: XThiefApplication
            private set
    }
}
