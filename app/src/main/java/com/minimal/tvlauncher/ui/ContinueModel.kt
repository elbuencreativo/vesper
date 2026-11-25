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
    /** Última interacción (ms UTC) o [FALLBACK_ENGAGEMENT] si es una tarjeta de "vistos recientemente". */
    val engagedAt: Long = 0,
) {
    val key: String get() = dismissKey(packageName, title)
}

const val FALLBACK_ENGAGEMENT = -1L

/** Identifica un programa para descartarlo de "Seguir viendo": mismo app y mismo título, sin importar mayúsculas. */
fun dismissKey(packageName: String, title: String): String = "$packageName|${title.trim().lowercase()}"

/**
 * Quita de la fila lo que el usuario descartó, salvo que lo haya vuelto a ver después de descartarlo
 * (su última interacción es más nueva que la fecha del descarte). Las tarjetas de respaldo no se filtran aquí.
 */
fun filterDismissed(cards: List<ContinueCard>, dismissed: Map<String, Long>): List<ContinueCard> =
    cards.filter { card ->
        val dismissedAt = dismissed[card.key]
        card.engagedAt == FALLBACK_ENGAGEMENT || dismissedAt == null || card.engagedAt > dismissedAt
    }

/**
 * Watch Next del sistema primero (hasta [cap]), sin lo descartado por el usuario. Si el sistema no tiene nada,
 * "vistos recientemente" como respaldo (icono + nombre). Si tampoco hay, lista vacía y la fila se oculta.
 */
fun buildContinueCards(
    watchNext: List<WatchNextItem>,
    recents: List<AppEntry>,
    cap: Int = 10,
    dismissed: Map<String, Long> = emptyMap(),
): List<ContinueCard> {
    // Varios apps pueden publicar la misma serie (p. ej. Jellyfin y Wholphin): se muestra una vez, la más reciente.
    val candidates = watchNext
        .filter { it.packageName.isNotBlank() }
        .distinctBy { it.title.trim().lowercase() }
        .map { ContinueCard(it.title, null, it.posterUri, it.progress, it.intentUri, it.packageName, it.lastEngagementMs) }
    // Lo descartado se quita antes de recortar (así entra el siguiente) y, si el sistema sí tenía contenido,
    // no se cae a los "recientes": si descartaste todo, la fila se oculta.
    if (candidates.isNotEmpty()) return filterDismissed(candidates, dismissed).take(cap)
    return recents.take(cap).map { ContinueCard(it.label, null, null, null, null, it.packageName, FALLBACK_ENGAGEMENT) }
}
