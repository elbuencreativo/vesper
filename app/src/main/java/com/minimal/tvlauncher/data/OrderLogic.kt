package com.minimal.tvlauncher.data

/**
 * row: los primeros [ROW_SIZE] visibles (fila de inicio). all: todos en orden (ocultos incluidos).
 * hiddenEntries: los ocultos, para la opción "mostrar de nuevo".
 */
data class ArrangedApps(
    val row: List<AppEntry>,
    val all: List<AppEntry>,
    val hiddenEntries: List<AppEntry>,
)

object OrderLogic {
    const val ROW_SIZE = 8

    fun arrange(
        installed: List<AppEntry>,
        savedOrder: List<String>,
        hidden: Set<String>,
        defaultFirst: List<String>,
    ): ArrangedApps {
        val byPackage = installed.associateBy { it.packageName }
        val seed = if (savedOrder.isEmpty()) defaultFirst else savedOrder
        val head = seed.distinct().mapNotNull { byPackage[it] }
        val headPackages = head.mapTo(HashSet()) { it.packageName }
        val tail = installed
            .filter { it.packageName !in headPackages }
            .sortedBy { it.label.lowercase() }
        val all = head + tail
        return ArrangedApps(
            row = all.filter { it.packageName !in hidden }.take(ROW_SIZE),
            all = all,
            hiddenEntries = all.filter { it.packageName in hidden },
        )
    }
}
