// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import btm.m.xthief.data.VideoLinks
import btm.m.xthief.ui.XThiefScreen

class MainActivity : ComponentActivity() {

    private var sharedLink by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedLink = extractSharedLink(intent)
        setContent {
            val container = remember { application as XThiefApplication }
            val settings by container.settingsStore.state.collectAsStateWithLifecycle()
            XThiefScreen(container = container, sharedText = sharedLink)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractSharedLink(intent)?.let { sharedLink = it }
    }

    private fun extractSharedLink(intent: Intent?): String? {
        val action = intent?.action ?: return null
        if (action != Intent.ACTION_SEND) return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        // Any supported platform, not just X.
        return if (VideoLinks.isSupported(text)) text else null
    }
}
