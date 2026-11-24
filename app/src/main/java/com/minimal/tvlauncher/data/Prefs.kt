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
    }
}
