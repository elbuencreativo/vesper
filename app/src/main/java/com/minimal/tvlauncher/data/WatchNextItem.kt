package com.minimal.tvlauncher.data

/**
 * Una fila de la fila "Watch Next" del sistema. posterUri e intentUri se guardan como texto
 * para mantener el mapeo puro (testeable en JVM); se convierten a Uri/Intent al usarlos.
 */
data class WatchNextItem(
    val packageName: String,
    val title: String,
    val posterUri: String?,
    val progress: Float?,
    val intentUri: String?,
    /** Última vez que el usuario interactuó con este contenido (ms UTC); sirve para volver a mostrarlo tras descartarlo. */
    val lastEngagementMs: Long = 0,
    /** "T2 · E6 · faltan 27 min": episodio y tiempo restante (o duración si no se ha empezado). */
    val detail: String? = null,
)

fun mapRow(
    packageName: String,
    title: String?,
    posterUri: String?,
    positionMs: Long,
    durationMs: Long,
    intentUri: String?,
    lastEngagementMs: Long = 0,
    season: String? = null,
    episode: String? = null,
): WatchNextItem? {
    val cleanTitle = title?.trim().orEmpty()
    if (cleanTitle.isEmpty()) return null
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else null
    return WatchNextItem(
        packageName, cleanTitle, posterUri, if (positionMs > 0) progress else null, intentUri, lastEngagementMs,
        detail = episodeDetail(season, episode, positionMs, durationMs),
    )
}

private fun formatMinutes(ms: Long): String {
    val m = ((ms + 59_999) / 60_000).toInt().coerceAtLeast(1) // hacia arriba: 26,9 min -> 27
    return if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min"
}

/**
 * Línea de detalle de una tarjeta: temporada y episodio (si los hay) y el tiempo que falta.
 * Si aún no se ha empezado, muestra la duración. Devuelve null cuando no hay nada útil que decir.
 */
fun episodeDetail(season: String?, episode: String?, positionMs: Long, durationMs: Long): String? {
    val tag = listOfNotNull(
        season?.trim()?.takeIf { it.isNotEmpty() }?.let { "T$it" },
        episode?.trim()?.takeIf { it.isNotEmpty() }?.let { "E$it" },
    ).joinToString(" · ")
    val time = when {
        durationMs <= 0 -> null
        positionMs > 0 && durationMs - positionMs < 60_000 -> "casi terminado"
        positionMs > 0 -> "faltan ${formatMinutes(durationMs - positionMs)}"
        else -> formatMinutes(durationMs)
    }
    return listOfNotNull(tag.ifEmpty { null }, time).joinToString(" · ").ifEmpty { null }
}
