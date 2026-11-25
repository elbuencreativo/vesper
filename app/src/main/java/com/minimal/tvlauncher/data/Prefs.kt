package com.minimal.tvlauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.launcherStore: DataStore<Preferences> by preferencesDataStore("launcher")

data class KnownUpdate(val code: Int, val name: String)

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

    /** Programas descartados de "Seguir viendo": clave -> fecha (ms) del descarte. */
    val dismissed: Flow<Map<String, Long>> = store.data.map { prefs ->
        prefs[DISMISSED].orEmpty().split(SEP).mapNotNull { line ->
            val i = line.lastIndexOf('\t')
            val at = if (i > 0) line.substring(i + 1).toLongOrNull() else null
            if (at == null) null else line.substring(0, i) to at
        }.toMap()
    }

    suspend fun dismiss(key: String, atMs: Long) {
        val clean = key.replace('\t', ' ').replace('\n', ' ')
        store.edit { prefs ->
            val current = prefs[DISMISSED].orEmpty().split(SEP).mapNotNull { line ->
                val i = line.lastIndexOf('\t')
                val at = if (i > 0) line.substring(i + 1).toLongOrNull() else null
                if (at == null) null else line.substring(0, i) to at
            }.toMap()
            val merged = (current + (clean to atMs)).entries.sortedByDescending { it.value }.take(MAX_DISMISSED)
            prefs[DISMISSED] = merged.joinToString(SEP) { "${it.key}\t${it.value}" }
        }
    }

    /** Última vez que se buscó una actualización (ms, reloj de pared); 0 = nunca. */
    val lastUpdateCheckMs: Flow<Long> = store.data.map { it[LAST_UPDATE_CHECK] ?: 0L }

    /** Versión más nueva encontrada en la última búsqueda, o null si estás al día. */
    val knownUpdate: Flow<KnownUpdate?> = store.data.map { prefs ->
        prefs[KNOWN_UPDATE]?.split('\t', limit = 2)?.let { parts ->
            val code = parts.getOrNull(0)?.toIntOrNull()
            val name = parts.getOrNull(1)
            if (code != null && name != null) KnownUpdate(code, name) else null
        }
    }

    /** Mayor versión (versionCode) de la que ya se avisó al usuario, para no repetir el aviso. */
    val announcedUpdateCode: Flow<Int> = store.data.map { it[ANNOUNCED_UPDATE] ?: 0 }

    suspend fun setAnnouncedUpdateCode(code: Int) {
        store.edit { it[ANNOUNCED_UPDATE] = code }
    }

    suspend fun saveUpdateCheck(atMs: Long, known: KnownUpdate?) {
        store.edit {
            it[LAST_UPDATE_CHECK] = atMs
            if (known == null) it.remove(KNOWN_UPDATE) else it[KNOWN_UPDATE] = "${known.code}\t${known.name}"
        }
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

    companion object {
        const val MAX_DISMISSED = 100
        private const val SEP = "\n"
        private val ORDER = stringPreferencesKey("order")
        private val HIDDEN = stringSetPreferencesKey("hidden")
        private val RECENTS = stringPreferencesKey("recents")
        private val STARTS = stringPreferencesKey("starts")
        private val FAVORITES = stringPreferencesKey("favorites")
        private val DISMISSED = stringPreferencesKey("dismissed")
        private val LAST_UPDATE_CHECK = longPreferencesKey("last_update_check")
        private val KNOWN_UPDATE = stringPreferencesKey("known_update")
        private val ANNOUNCED_UPDATE = intPreferencesKey("announced_update")
    }
}
