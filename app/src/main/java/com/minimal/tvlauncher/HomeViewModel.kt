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
            apps = OrderLogic.arrange(installed, prefs.order.first(), prefs.hidden.first(), DEFAULT_APP_ORDER)
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

    fun toggleHidden(packageName: String) {
        viewModelScope.launch {
            prefs.setHidden(com.minimal.tvlauncher.data.toggleHidden(prefs.hidden.first(), packageName))
            refresh()
        }
    }

    fun recordLaunch(packageName: String) {
        viewModelScope.launch { recents.record(packageName) }
    }
}
