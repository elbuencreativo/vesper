package com.minimal.tvlauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.launcherStore: DataStore<Preferences> by preferencesDataStore("launcher")

class Prefs(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.launcherStore)

    val order: Flow<List<String>> = store.data.map { it[ORDER].toList() }
    val hidden: Flow<Set<String>> = store.data.map { it[HIDDEN] ?: emptySet() }

    /** Favoritos elegidos (fila de inicio). null = el usuario aún no eligió (se usan los primeros apps). */
    val favorites: Flow<Set<String>?> = store.data.map { prefs ->
        prefs[FAVORITES]?.let { if (it.isEmpty()) emptySet() else it.split(SEP).toSet() }
    }

    suspend fun setFavorites(favorites: Set<String>) {
        store.edit { it[FAVORITES] = favorites.joinToString(SEP) }
    }

    val recents: Flow<List<String>> = store.data.map { it[RECENTS].toList() }

    /** Marcas de tiempo (reloj monotónico) de los últimos arranques, para el salvavidas de arranque. */
    val startTimes: Flow<List<Long>> =
        store.data.map { prefs -> prefs[STARTS].toList().mapNotNull { it.toLongOrNull() } }

    suspend fun setStartTimes(times: List<Long>) {
        store.edit { it[STARTS] = times.joinToString(SEP) }
    }

    suspend fun setRecents(recents: List<String>) {
        store.edit { it[RECENTS] = recents.joinToString(SEP) }
    }

    suspend fun setOrder(order: List<String>) {
        store.edit { it[ORDER] = order.joinToString(SEP) }
    }

    suspend fun setHidden(hidden: Set<String>) {
        store.edit { it[HIDDEN] = hidden }
    }

    private fun String?.toList(): List<String> =
        if (isNullOrEmpty()) emptyList() else split(SEP)

    private companion object {
        const val SEP = "\n"
        val ORDER = stringPreferencesKey("order")
        val HIDDEN = stringSetPreferencesKey("hidden")
        val RECENTS = stringPreferencesKey("recents")
        val STARTS = stringPreferencesKey("starts")
        val FAVORITES = stringPreferencesKey("favorites")
    }
}
