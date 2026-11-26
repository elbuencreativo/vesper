package com.minimal.tvlauncher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.UserManager
import com.minimal.tvlauncher.data.Cities
import com.minimal.tvlauncher.data.GreetingStyle
import com.minimal.tvlauncher.data.StoicQuotes
import com.minimal.tvlauncher.data.ThemeLogic
import com.minimal.tvlauncher.ui.Greeting
import com.minimal.tvlauncher.ui.Palette
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import com.minimal.tvlauncher.data.greetingNameFor
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import com.minimal.tvlauncher.actions.HomeKeyService
import com.minimal.tvlauncher.actions.isHomeKeyServiceEnabled
import com.minimal.tvlauncher.actions.STOCK_HOME_PACKAGE
import com.minimal.tvlauncher.actions.Shortcut
import com.minimal.tvlauncher.actions.SystemShortcuts
import com.minimal.tvlauncher.data.AppEntry
import com.minimal.tvlauncher.data.ArtRepository
import com.minimal.tvlauncher.idle.IdleController
import com.minimal.tvlauncher.idle.IdleState
import com.minimal.tvlauncher.ui.AllAppsScreen
import com.minimal.tvlauncher.ui.AppBackground
import com.minimal.tvlauncher.ui.ContinueCard
import com.minimal.tvlauncher.ui.HomeScreen
import com.minimal.tvlauncher.ui.IconCache
import com.minimal.tvlauncher.ui.IdleScreen
import com.minimal.tvlauncher.ui.LauncherSettingsScreen
import com.minimal.tvlauncher.ui.RemoteSettingsScreen
import com.minimal.tvlauncher.ui.OptionsMenu
import com.minimal.tvlauncher.ui.LocalIconCache
import com.minimal.tvlauncher.ui.LocalPosterLoader
import com.minimal.tvlauncher.ui.PosterLoader
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
    private var reorderFavorites by mutableStateOf(false)
    private var homeMenu by mutableStateOf(false)
    private var homeKeyActive by mutableStateOf(true)
    private var artMode by mutableStateOf(false)
    private var cardMenu by mutableStateOf<ContinueCard?>(null)
    private var menuTick by mutableIntStateOf(0)
    private var message by mutableStateOf<String?>(null)
    private var messageJob: Job? = null
    private var greetingVisible by mutableStateOf(false)
    private var greetingJob: Job? = null
    private var greetingLine by mutableStateOf(TRADITIONAL_LINE)

    /** Muestra el saludo unos segundos si está activado (los ajustes pueden tardar un instante en cargar). */
    private fun greet() {
        greetingJob?.cancel()
        greetingJob = lifecycleScope.launch {
            delay(900)
            if (!vm.display.greetingEnabled) return@launch
            greetingLine = if (vm.display.greetingStyle == GreetingStyle.Stoic) StoicQuotes.random(greetingLine) else TRADITIONAL_LINE
            greetingVisible = true
            delay(5_500)
            greetingVisible = false
        }
    }

    /** Nombre del perfil de Android en uso, o null si no se puede leer. */
    private fun profileName(): String? =
        runCatching { (getSystemService(Context.USER_SERVICE) as UserManager).userName }.getOrNull()

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

        if (savedInstanceState == null) greet()
        lifecycleScope.launch { guardAgainstCrashLoops() }
        if (checkSelfPermission(READ_TV_LISTINGS) != PackageManager.PERMISSION_GRANTED) {
            tvListingsPermission.launch(READ_TV_LISTINGS)
        }

        val icons = IconCache(applicationContext)
        val posters = PosterLoader(applicationContext)
        val logoRepo = com.minimal.tvlauncher.data.LogoRepository(applicationContext, BuildConfig.TMDB_KEY)
        val psnRepo = com.minimal.tvlauncher.data.PsnRepository(applicationContext)
        setContent {
            CompositionLocalProvider(LocalIconCache provides remember { icons }, LocalPosterLoader provides remember { posters }, com.minimal.tvlauncher.data.LocalLogoRepository provides logoRepo, com.minimal.tvlauncher.data.LocalPsnRepository provides psnRepo) {
                val idleState by idle.state.collectAsState()
                // Tema: claro de 6 am a 6 pm, oscuro de 6 pm a 6 am (según la hora del reloj principal), o fijo.
                LaunchedEffect(vm.display.themeMode, vm.display.primaryCity) {
                    val zone = Cities.byName(vm.display.primaryCity)?.zoneId ?: "UTC"
                    while (true) {
                        Palette.dark = ThemeLogic.isDarkNow(vm.display.themeMode, zone, System.currentTimeMillis())
                        delay(30_000)
                    }
                }
                // Saludo al encender y al despertar del reposo oscuro.
                var previousIdle by remember { mutableStateOf(IdleState.Active) }
                LaunchedEffect(idleState) {
                    if (previousIdle == IdleState.Dark && idleState == IdleState.Active) greet()
                    previousIdle = idleState
                }
                // Aviso de versión nueva (una vez por versión) y, al abrir los ajustes con una versión nueva conocida,
                // se carga su información para poder ofrecer "Instalar".
                LaunchedEffect(vm.updateNotice) {
                    vm.updateNotice?.let {
                        flash(it, 7000)
                        vm.consumeUpdateNotice()
                    }
                }
                LaunchedEffect(screen, vm.updateBadge) {
                    if (screen == Screen.Settings && vm.updateBadge && vm.availableUpdate == null && !vm.updateBusy) vm.checkForUpdate()
                }
                Box(Modifier.fillMaxSize()) {
                    AppBackground(art, clear = artMode)
                    // Modo "ver obra": toda la interfaz se desvanece y queda la obra; cualquier botón la devuelve.
                    AnimatedVisibility(
                        visible = !artMode,
                        enter = fadeIn(tween(500)),
                        exit = fadeOut(tween(500)),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                    key(homeTick) {
                        when (screen) {
                            Screen.All -> AllAppsScreen(
                                apps = vm.apps,
                                onLaunch = ::launch,
                                onSaveOrder = vm::saveOrder,
                                reorderFavorites = reorderFavorites,
                                onToggleFavorite = vm::toggleFavorite,
                                onToggleHidden = vm::toggleHidden,
                                onUninstall = actions::uninstall,
                                onBack = { screen = Screen.Home },
                            )
                            Screen.Remote -> RemoteSettingsScreen(
                                apps = vm.apps.all,
                                serviceActive = homeKeyActive,
                                onFixService = { if (!shortcuts.open(Shortcut.Accessibility)) flash("No se pudo abrir Accesibilidad") },
                                onBack = { screen = Screen.Settings },
                            )
                            Screen.Settings -> LauncherSettingsScreen(
                                hiddenCount = vm.apps.hiddenEntries.size,
                                versionName = BuildConfig.VERSION_NAME,
                                onUnhideAll = vm::unhideAll,
                                onChooseHome = { if (!shortcuts.open(Shortcut.ChooseHome)) flash("No se pudo abrir los ajustes") },
                                homeKeyActive = homeKeyActive,
                                onFixHomeKey = { if (!shortcuts.open(Shortcut.Accessibility)) flash("No se pudo abrir Accesibilidad") },
                                updateStatus = vm.updateStatus,
                                updateVersion = vm.availableUpdate?.versionName,
                                updateBusy = vm.updateBusy,
                                onCheckUpdate = vm::checkForUpdate,
                                onInstallUpdate = vm::installUpdate,
                                onOpenRemote = { screen = Screen.Remote },
                                display = vm.display,
                                profileName = profileName(),
                                onDisplay = vm::updateDisplay,
                                onBack = { screen = Screen.Home },
                            )
                            Screen.Home -> HomeScreen(
                                apps = vm.apps,
                                cards = vm.cards,
                                onAction = ::quickAction,
                                onCard = ::openCard,
                                onCardMenu = { cardMenu = it },
                                onLaunch = ::launch,
                                onClose = ::close,
                                onOpenAll = { reorderFavorites = false; screen = Screen.All },
                                onManage = { homeMenu = true },
                                attention = !homeKeyActive || vm.updateBadge,
                                refocusKey = menuTick,
                                focusBlocked = homeMenu || cardMenu != null,
                                message = message,
                                primaryCity = Cities.byName(vm.display.primaryCity) ?: Cities.all[0],
                                secondaryCity = Cities.byName(vm.display.secondaryCity),
                                appBarStyle = vm.display.appBarStyle,
                                showLogos = vm.display.showLogos,
                                showPosters = vm.display.showPosters,
                                showPsn = vm.display.showPsn,
                            )
                        }
                    }
                    }
                    if (homeMenu) {
                        OptionsMenu(
                            title = "Apps",
                            options = listOf(
                                "Ordenar favoritos" to { homeMenu = false; reorderFavorites = true; screen = Screen.All },
                                "Administrar apps" to { homeMenu = false; reorderFavorites = false; screen = Screen.All },
                                "Cancelar" to { homeMenu = false; menuTick++ },
                            ),
                            onDismiss = { homeMenu = false; menuTick++ },
                        )
                    }
                    cardMenu?.let { card ->
                        OptionsMenu(
                            title = card.title,
                            options = listOf(
                                "Quitar de Seguir viendo" to { vm.dismissCard(card); cardMenu = null; menuTick++ },
                                "Cancelar" to { cardMenu = null; menuTick++ },
                            ),
                            onDismiss = { cardMenu = null; menuTick++ },
                        )
                    }
                    Greeting(greetingVisible, greetingNameFor(vm.display, profileName()), greetingLine)
                    IdleScreen(idleState, art)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        idle.onLauncherResumed()
        homeKeyActive = isHomeKeyServiceEnabled(
            Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), packageName,
        )
        vm.refresh()
        vm.checkForUpdateIfDue()
    }

    override fun onPause() {
        idle.onLauncherPaused()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        idle.onUserInput()
        homeMenu = false
        cardMenu = null
        artMode = false
        reorderFavorites = false
        screen = Screen.Home
        homeTick++
    }

    /** La primera pulsación que despierta el reposo solo despierta; no actúa sobre la interfaz. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (greetingVisible) { greetingJob?.cancel(); greetingVisible = false }
                if (idle.onUserInput() && event.repeatCount == 0) swallowedKey = event.keyCode
                if (event.keyCode == swallowedKey) return true
                if (artMode && event.repeatCount == 0) {
                    artMode = false
                    swallowedKey = event.keyCode
                    return true
                }
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
            QuickAction.ShowArt -> {
                artMode = true
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

    private fun flash(text: String, durationMs: Long = 1800) {
        messageJob?.cancel()
        message = text
        messageJob = lifecycleScope.launch {
            delay(durationMs)
            message = null
        }
    }

    /** Salvavidas: tras varios arranques fallidos seguidos, cede el control al home original de Google TV. */
    private suspend fun guardAgainstCrashLoops() {
        val guard = CrashGuard(vm.prefs, MonotonicClock)
        guard.onStart()
        if (guard.shouldYieldToStock()) {
            guard.markHealthy()
            HomeKeyService.suppressRedirectFor(60_000) // que el captador de HOME no devuelva aquí al usuario
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

    private enum class Screen { Home, All, Settings, Remote }

    private companion object {
        const val TRADITIONAL_LINE = "¿Qué quieres ver hoy?"
        const val DEFAULT_IDLE_MS = 300_000L
        const val DEFAULT_DARK_MS = 1_200_000L
        const val HEALTHY_AFTER_MS = 8_000L
        const val READ_TV_LISTINGS = "android.permission.READ_TV_LISTINGS" // oculta en el SDK público
    }
}
