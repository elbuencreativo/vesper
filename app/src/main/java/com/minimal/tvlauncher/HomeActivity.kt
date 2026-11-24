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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.minimal.tvlauncher.actions.AppActions
import com.minimal.tvlauncher.data.AppEntry
import com.minimal.tvlauncher.ui.ContinueCard
import com.minimal.tvlauncher.ui.HomeScreen
import com.minimal.tvlauncher.ui.IconCache
import com.minimal.tvlauncher.ui.LocalIconCache
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeActivity : ComponentActivity() {
    private val vm: HomeViewModel by viewModels()
    private val actions by lazy { AppActions(this, vm.recents) }

    /** Sube cada vez que se pulsa HOME estando ya aquí: reconstruye la pantalla y reinicia el foco. */
    private var homeTick by mutableIntStateOf(0)
    private var message by mutableStateOf<String?>(null)
    private var messageJob: Job? = null

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
                        onClose = ::close,
                        onOpenAll = { /* Task 7 */ },
                        message = message,
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
        lifecycleScope.launch {
            if (!actions.launch(entry)) flash("No se pudo abrir ${entry.label}")
        }
    }

    private fun close(entry: AppEntry) {
        if (!actions.close(entry.packageName)) flash("No se pudo abrir la información de ${entry.label}")
    }

    private fun openCard(card: ContinueCard) {
        val intent = card.intentUri
            ?.let { runCatching { Intent.parseUri(it, Intent.URI_INTENT_SCHEME) }.getOrNull() }
            ?: packageManager.getLeanbackLaunchIntentForPackage(card.packageName)
        if (intent == null) {
            flash("No se pudo abrir ${card.title}")
            return
        }
        runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onSuccess { vm.recordLaunch(card.packageName) }
            .onFailure { flash("No se pudo abrir ${card.title}") }
    }

    private fun flash(text: String) {
        messageJob?.cancel()
        message = text
        messageJob = lifecycleScope.launch {
            delay(1800)
            message = null
        }
    }
}
