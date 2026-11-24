package com.minimal.tvlauncher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.minimal.tvlauncher.actions.AppActions
import com.minimal.tvlauncher.actions.Shortcut
import com.minimal.tvlauncher.actions.SystemShortcuts
import com.minimal.tvlauncher.data.AppEntry
import com.minimal.tvlauncher.data.ArtRepository
import com.minimal.tvlauncher.idle.IdleController
import com.minimal.tvlauncher.idle.IdleState
import com.minimal.tvlauncher.ui.AllAppsScreen
import com.minimal.tvlauncher.ui.ContinueCard
import com.minimal.tvlauncher.ui.HomeScreen
import com.minimal.tvlauncher.ui.IconCache
import com.minimal.tvlauncher.ui.IdleScreen
import com.minimal.tvlauncher.ui.LauncherSettingsScreen
import com.minimal.tvlauncher.ui.LocalIconCache
import com.minimal.tvlauncher.util.CrashGuard
import com.minimal.tvlauncher.ui.QuickAction
import com.minimal.tvlauncher.util.MonotonicClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeActivity : ComponentActivity() {
    private val vm: HomeViewModel by viewModels()
    private val actions by lazy { AppActions(this, vm.recents) }
    private val shortcuts by lazy { SystemShortcuts(this) }
    private val art by lazy { ArtRepository(applicationContext) }
    private lateinit var idle: IdleController

    // Tras responder al permiso se abre igual: si se negó, SystemShortcuts cae a los ajustes generales.
    private val bluetoothPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            shortcuts.open(Shortcut.Bluetooth)
        }

    // Leer "Seguir viendo" real (Watch Next) exige READ_TV_LISTINGS en tiempo de ejecución; al responder se recarga.
    private val tvListingsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refresh() }

    /** Sube cada vez que se pulsa HOME estando ya aquí: reconstruye la pantalla y reinicia el foco. */
    private var homeTick by mutableIntStateOf(0)
    private var screen by mutableStateOf(Screen.Home)
    private var message by mutableStateOf<String?>(null)
    private var messageJob: Job? = null

    /** Tecla que despertó el reposo: se descarta también su "soltar" (y repeticiones). */
    private var swallowedKey = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Solo en debug: `am start ... --el idle_ms 10000 --el dark_ms 20000` acorta los tiempos para probar.
        val idleMs = if (BuildConfig.DEBUG) intent.getLongExtra("idle_ms", DEFAULT_IDLE_MS) else DEFAULT_IDLE_MS
        val darkMs = if (BuildConfig.DEBUG) intent.getLongExtra("dark_ms", DEFAULT_DARK_MS) else DEFAULT_DARK_MS
        idle = IdleController(lifecycleScope, MonotonicClock, idleMs, darkMs)

        // La pantalla solo se mantiene encendida durante el arte; en negro el sistema puede dormir.
        lifecycleScope.launch {
            idle.state.collect { state ->
                if (state == IdleState.Art) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        lifecycleScope.launch { guardAgainstCrashLoops() }
        if (checkSelfPermission(READ_TV_LISTINGS) != PackageManager.PERMISSION_GRANTED) {
            tvListingsPermission.launch(READ_TV_LISTINGS)
        }

        val icons = IconCache(applicationContext)
        setContent {
            CompositionLocalProvider(LocalIconCache provides remember { icons }) {
                val idleState by idle.state.collectAsState()
                Box(Modifier.fillMaxSize()) {
                    key(homeTick) {
                        when (screen) {
                            Screen.All -> AllAppsScreen(
                                apps = vm.apps.all,
                                hidden = vm.apps.hiddenEntries.mapTo(HashSet()) { it.packageName },
                                onLaunch = ::launch,
                                onSaveOrder = vm::saveOrder,
                                onToggleHidden = vm::toggleHidden,
                                onUninstall = actions::uninstall,
                                onBack = { screen = Screen.Home },
                            )
                            Screen.Settings -> LauncherSettingsScreen(
                                hiddenCount = vm.apps.hiddenEntries.size,
                                versionName = BuildConfig.VERSION_NAME,
                                onUnhideAll = vm::unhideAll,
                                onChooseHome = { if (!shortcuts.open(Shortcut.ChooseHome)) flash("No se pudo abrir los ajustes") },
                                onBack = { screen = Screen.Home },
                            )
                            Screen.Home -> HomeScreen(
                                apps = vm.apps,
                                cards = vm.cards,
                                onAction = ::quickAction,
                                onCard = ::openCard,
                                onLaunch = ::launch,
                                onClose = ::close,
                                onOpenAll = { screen = Screen.All },
                                message = message,
                            )
                        }
                    }
                    IdleScreen(idleState, art)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        idle.onLauncherResumed()
        vm.refresh()
    }

    override fun onPause() {
        idle.onLauncherPaused()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        idle.onUserInput()
        screen = Screen.Home
        homeTick++
    }

    /** La primera pulsación que despierta el reposo solo despierta; no actúa sobre la interfaz. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (idle.onUserInput() && event.repeatCount == 0) swallowedKey = event.keyCode
                if (event.keyCode == swallowedKey) return true
            }
            KeyEvent.ACTION_UP -> if (event.keyCode == swallowedKey) {
                swallowedKey = -1
                return true
            }
        }
        return super.dispatchKeyEvent(event)
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
            QuickAction.LauncherSettings -> {
                screen = Screen.Settings
                return
            }
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

    /** Salvavidas: tras varios arranques fallidos seguidos, cede el control al home original de Google TV. */
    private suspend fun guardAgainstCrashLoops() {
        val guard = CrashGuard(vm.prefs, MonotonicClock)
        guard.onStart()
        if (guard.shouldYieldToStock()) {
            guard.markHealthy()
            val stock = Intent(Intent.ACTION_MAIN)
            stock.addCategory(Intent.CATEGORY_HOME)
            stock.setPackage(STOCK_HOME_PACKAGE)
            stock.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { startActivity(stock) }.isSuccess) finish()
            return
        }
        delay(HEALTHY_AFTER_MS)
        guard.markHealthy()
    }

    private enum class Screen { Home, All, Settings }

    private companion object {
        const val DEFAULT_IDLE_MS = 300_000L
        const val DEFAULT_DARK_MS = 1_200_000L
        const val HEALTHY_AFTER_MS = 8_000L
        const val READ_TV_LISTINGS = "android.permission.READ_TV_LISTINGS" // oculta en el SDK público
        const val STOCK_HOME_PACKAGE = "com.google.android.apps.tv.launcherx"
    }
}
