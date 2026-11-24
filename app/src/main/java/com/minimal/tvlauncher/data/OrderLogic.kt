package com.minimal.tvlauncher.data

/**
 * row: favoritos visibles (fila de inicio). others: apps visibles que no son favoritas (el listado).
 * all: todos en orden (ocultos incluidos). hiddenEntries: los ocultos, para volver a mostrarlos.
 */
data class ArrangedApps(
    val row: List<AppEntry>,
    val all: List<AppEntry>,
    val hiddenEntries: List<AppEntry>,
    val others: List<AppEntry> = emptyList(),
)

object OrderLogic {
    /** Favoritos por defecto del primer arranque (antes de que el usuario elija). */
    const val DEFAULT_ROW_SIZE = 8
    const val MAX_FAVORITES = 10

    /**
     * [favorites] == null: aún no se eligieron; los primeros [DEFAULT_ROW_SIZE] visibles son favoritos.
     * Con un conjunto, la fila son solo esos apps (los que sigan instalados y visibles), hasta [MAX_FAVORITES].
     */
    fun arrange(
        installed: List<AppEntry>,
        savedOrder: List<String>,
        hidden: Set<String>,
        defaultFirst: List<String>,
        favorites: Set<String>? = null,
    ): ArrangedApps {
        val byPackage = installed.associateBy { it.packageName }
        val seed = if (savedOrder.isEmpty()) defaultFirst else savedOrder
        val head = seed.distinct().mapNotNull { byPackage[it] }
        val headPackages = head.mapTo(HashSet()) { it.packageName }
        val tail = installed
            .filter { it.packageName !in headPackages }
            .sortedBy { it.label.lowercase() }
        val all = head + tail
        val visible = all.filter { it.packageName !in hidden }
        val row = if (favorites == null) visible.take(DEFAULT_ROW_SIZE)
        else visible.filter { it.packageName in favorites }.take(MAX_FAVORITES)
        val rowPackages = row.mapTo(HashSet()) { it.packageName }
        return ArrangedApps(
            row = row,
            all = all,
            hiddenEntries = all.filter { it.packageName in hidden },
            others = visible.filter { it.packageName !in rowPackages },
        )
    }
}
