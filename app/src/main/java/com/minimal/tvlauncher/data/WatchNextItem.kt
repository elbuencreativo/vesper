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
)

fun mapRow(
    packageName: String,
    title: String?,
    posterUri: String?,
    positionMs: Long,
    durationMs: Long,
    intentUri: String?,
    lastEngagementMs: Long = 0,
): WatchNextItem? {
    val cleanTitle = title?.trim().orEmpty()
    if (cleanTitle.isEmpty()) return null
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else null
    return WatchNextItem(packageName, cleanTitle, posterUri, progress, intentUri, lastEngagementMs)
}
