package com.minimal.tvlauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.minimal.tvlauncher.data.AppEntry
import com.minimal.tvlauncher.ui.ContinueCard
import com.minimal.tvlauncher.ui.HomeScreen
import com.minimal.tvlauncher.ui.IconCache
import com.minimal.tvlauncher.ui.LocalIconCache

class HomeActivity : ComponentActivity() {
    private val vm: HomeViewModel by viewModels()

    /** Sube cada vez que se pulsa HOME estando ya aquí: reconstruye la pantalla y reinicia el foco. */
    private var homeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val icons = IconCache(applicationContext)
        setContent {
            CompositionLocalProvider(LocalIconCache provides remember { icons }) {
                key(homeTick) {
                    HomeScreen(
                        apps = vm.apps,
                        cards = vm.cards,
                        onAction = { /* Task 8 / Task 11 */ },
                        onCard = ::openCard,
                        onLaunch = ::launch,
                        onClose = { /* Task 6 */ },
                        onOpenAll = { /* Task 7 */ },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeTick++
    }

    private fun launch(entry: AppEntry) {
        runCatching { startActivity(entry.launchIntent) }
            .onSuccess { vm.recordLaunch(entry.packageName) }
    }

    private fun openCard(card: ContinueCard) {
        val intent = card.intentUri
            ?.let { runCatching { Intent.parseUri(it, Intent.URI_INTENT_SCHEME) }.getOrNull() }
            ?: packageManager.getLeanbackLaunchIntentForPackage(card.packageName)
        if (intent != null) {
            runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                .onSuccess { vm.recordLaunch(card.packageName) }
        }
    }
}
