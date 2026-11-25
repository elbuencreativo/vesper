package com.minimal.tvlauncher.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Últimos apps abiertos desde el launcher (respaldo de "Seguir viendo"), el más reciente primero. */
class RecentsStore(private val prefs: Prefs) {

    suspend fun record(packageName: String) {
        val updated = (listOf(packageName) + prefs.recents.first().filter { it != packageName })
            .take(MAX_STORED)
        prefs.setRecents(updated)
    }

    suspend fun remove(packageName: String) {
        prefs.setRecents(prefs.recents.first().filter { it != packageName })
    }

    fun recent(limit: Int = 10): Flow<List<String>> = prefs.recents.map { it.take(limit) }

    private companion object {
        const val MAX_STORED = 30
    }
}
