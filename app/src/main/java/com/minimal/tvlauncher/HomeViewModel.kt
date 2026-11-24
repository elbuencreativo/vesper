package com.minimal.tvlauncher

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minimal.tvlauncher.data.AppCatalog
import com.minimal.tvlauncher.data.ArrangedApps
import com.minimal.tvlauncher.data.DEFAULT_APP_ORDER
import com.minimal.tvlauncher.data.OrderLogic
import com.minimal.tvlauncher.data.Prefs
import com.minimal.tvlauncher.data.RecentsStore
import com.minimal.tvlauncher.data.WatchNextRepository
import com.minimal.tvlauncher.update.UpdateChecker
import com.minimal.tvlauncher.update.UpdateInfo
import com.minimal.tvlauncher.update.UpdateStatus
import com.minimal.tvlauncher.ui.ContinueCard
import com.minimal.tvlauncher.ui.buildContinueCards
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val catalog = AppCatalog(app)
    val prefs = Prefs(app)
    val recents = RecentsStore(prefs)
    private val watchNext = WatchNextRepository(app)

    var apps by mutableStateOf(ArrangedApps(emptyList(), emptyList(), emptyList()))
        private set
    var cards by mutableStateOf<List<ContinueCard>>(emptyList())
        private set

    fun refresh() {
        viewModelScope.launch {
            val installed = withContext(Dispatchers.IO) { catalog.installed() }
            apps = OrderLogic.arrange(installed, prefs.order.first(), prefs.hidden.first(), DEFAULT_APP_ORDER, prefs.favorites.first())
            val byPackage = installed.associateBy { it.packageName }
            val recentEntries = recents.recent().first().mapNotNull { byPackage[it] }
            cards = buildContinueCards(watchNext.load(), recentEntries)
        }
    }

    fun saveOrder(order: List<String>) {
        viewModelScope.launch {
            prefs.setOrder(order)
            refresh()
        }
    }

    /** Agrega/quita un favorito. Si aún no se habían elegido, parte de los que se ven hoy en la fila. */
    fun toggleFavorite(packageName: String) {
        viewModelScope.launch {
            val current = prefs.favorites.first() ?: apps.row.mapTo(HashSet()) { it.packageName }
            prefs.setFavorites(com.minimal.tvlauncher.data.toggleFavorite(current, packageName))
            refresh()
        }
    }

    fun toggleHidden(packageName: String) {
        viewModelScope.launch {
            prefs.setHidden(com.minimal.tvlauncher.data.toggleHidden(prefs.hidden.first(), packageName))
            refresh()
        }
    }

    private val updates = UpdateChecker(app, BuildConfig.UPDATE_URL)
    var updateStatus by mutableStateOf<String?>(null)
        private set
    var availableUpdate by mutableStateOf<UpdateInfo?>(null)
        private set
    var updateBusy by mutableStateOf(false)
        private set

    fun checkForUpdate() {
        if (updateBusy) return
        if (!updates.isConfigured) {
            updateStatus = "No hay dirección de actualizaciones configurada."
            return
        }
        viewModelScope.launch {
            updateBusy = true
            availableUpdate = null
            updateStatus = "Buscando actualización…"
            updateStatus = when (val s = updates.check(BuildConfig.VERSION_CODE)) {
                UpdateStatus.UpToDate -> "Estás al día (versión ${BuildConfig.VERSION_NAME})."
                is UpdateStatus.Available -> {
                    availableUpdate = s.info
                    "Nueva versión ${s.info.versionName} disponible." + (s.info.notes?.let { " $it" } ?: "")
                }
                UpdateStatus.Failed -> "No se pudo comprobar. ¿Está encendido el servidor de actualizaciones?"
            }
            updateBusy = false
        }
    }

    fun installUpdate() {
        val info = availableUpdate ?: return
        if (updateBusy) return
        viewModelScope.launch {
            updateBusy = true
            updateStatus = "Descargando ${info.versionName}…"
            val apk = updates.download(info)
            updateStatus = when {
                apk == null -> "La descarga falló o el archivo no coincide con su checksum."
                !updates.canInstall() -> {
                    updates.openInstallPermission()
                    "Permite instalar apps desde Vesper y vuelve a pulsar Instalar."
                }
                updates.install(apk) -> "Confirma la instalación en la pantalla del sistema."
                else -> "No se pudo abrir el instalador."
            }
            updateBusy = false
        }
    }

    fun unhideAll() {
        viewModelScope.launch {
            prefs.setHidden(emptySet())
            refresh()
        }
    }

    fun recordLaunch(packageName: String) {
        viewModelScope.launch { recents.record(packageName) }
    }
}
