package com.minimal.tvlauncher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
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
import com.minimal.tvlauncher.actions.Shortcut
import com.minimal.tvlauncher.actions.SystemShortcuts
import com.minimal.tvlauncher.ui.QuickAction
import com.minimal.tvlauncher.data.AppEntry
import com.minimal.tvlauncher.ui.AllAppsScreen
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
    private val shortcuts by lazy { SystemShortcuts(this) }

    // Tras responder al permiso se abre igual: si se negó, SystemShortcuts cae a los ajustes generales.
    private val bluetoothPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            shortcuts.open(Shortcut.Bluetooth)
        }

    /** Sube cada vez que se pulsa HOME estando ya aquí: reconstruye la pantalla y reinicia el foco. */
    private var homeTick by mutableIntStateOf(0)
    private var showAll by mutableStateOf(false)
    private var message by mutableStateOf<String?>(null)
    private var messageJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val icons = IconCache(applicationContext)
        setContent {
            CompositionLocalProvider(LocalIconCache provides remember { icons }) {
                key(homeTick) {
                    if (showAll) {
                        AllAppsScreen(
                            apps = vm.apps.all,
                            hidden = vm.apps.hiddenEntries.mapTo(HashSet()) { it.packageName },
                            onLaunch = ::launch,
                            onSaveOrder = vm::saveOrder,
                            onToggleHidden = vm::toggleHidden,
                            onUninstall = actions::uninstall,
                            onBack = { showAll = false },
                        )
                    } else {
                        HomeScreen(
                            apps = vm.apps,
                            cards = vm.cards,
                            onAction = ::quickAction,
                            onCard = ::openCard,
                            onLaunch = ::launch,
                            onClose = ::close,
                            onOpenAll = { showAll = true },
                            message = message,
                        )
                    }
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
        showAll = false
        homeTick++
    }

    private fun quickAction(action: QuickAction) {
        val shortcut = when (action) {
            QuickAction.Wifi -> Shortcut.Wifi
            QuickAction.Bluetooth -> {
                // La pantalla de accesorios de Google TV exige BLUETOOTH_CONNECT (Android 12+).
                if (Build.VERSION.SDK_INT >= 31 &&
                    checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                ) {
                    bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    return
                }
                Shortcut.Bluetooth
            }
            QuickAction.AndroidSettings -> Shortcut.AndroidSettings
            QuickAction.LauncherSettings -> return // Task 11
        }
        if (!shortcuts.open(shortcut)) flash("No se pudo abrir los ajustes")
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
