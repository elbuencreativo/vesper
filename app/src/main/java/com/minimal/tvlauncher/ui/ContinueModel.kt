package com.minimal.tvlauncher.ui

import com.minimal.tvlauncher.data.AppEntry
import com.minimal.tvlauncher.data.WatchNextItem

/** Tarjeta de "Seguir viendo". Si intentUri es nulo se abre el app por su paquete. */
data class ContinueCard(
    val title: String,
    val subtitle: String?,
    val posterUri: String?,
    val progress: Float?,
    val intentUri: String?,
    val packageName: String,
)

/**
 * Watch Next del sistema primero (hasta [cap]). Si no hay nada, "vistos recientemente"
 * como respaldo (icono + nombre). Si tampoco hay, lista vacía y la fila se oculta.
 */
fun buildContinueCards(
    watchNext: List<WatchNextItem>,
    recents: List<AppEntry>,
    cap: Int = 10,
): List<ContinueCard> {
    // Varios apps pueden publicar la misma serie (p. ej. Jellyfin y Wholphin): se muestra una vez, la más reciente.
    val fromSystem = watchNext
        .filter { it.packageName.isNotBlank() }
        .distinctBy { it.title.trim().lowercase() }
        .take(cap)
        .map { ContinueCard(it.title, null, it.posterUri, it.progress, it.intentUri, it.packageName) }
    if (fromSystem.isNotEmpty()) return fromSystem
    return recents.take(cap).map { ContinueCard(it.label, null, null, null, null, it.packageName) }
}
