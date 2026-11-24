package com.minimal.tvlauncher.data

/** Mueve el elemento de [from] a [to]. Si algún índice está fuera de rango o son iguales, no cambia nada. */
fun <T> moveItem(list: List<T>, from: Int, to: Int): List<T> {
    if (from !in list.indices || to !in list.indices || from == to) return list
    val mutable = list.toMutableList()
    mutable.add(to, mutable.removeAt(from))
    return mutable
}

/**
 * Mueve un elemento solo entre los de su sección ([inSection]); los demás conservan su lugar.
 * [from] y [to] son posiciones dentro de la sección. Fuera de rango o iguales no cambia nada.
 */
fun <T> reorderWithin(list: List<T>, inSection: (T) -> Boolean, from: Int, to: Int): List<T> {
    val slots = list.indices.filter { inSection(list[it]) }
    if (from !in slots.indices || to !in slots.indices || from == to) return list
    val moved = moveItem(slots.map { list[it] }, from, to)
    val result = list.toMutableList()
    slots.forEachIndexed { i, slot -> result[slot] = moved[i] }
    return result
}

/** Agrega o quita un favorito; si ya hay [max], no agrega más. */
fun toggleFavorite(favorites: Set<String>, packageName: String, max: Int = OrderLogic.MAX_FAVORITES): Set<String> = when {
    packageName in favorites -> favorites - packageName
    favorites.size < max -> favorites + packageName
    else -> favorites
}

fun toggleHidden(hidden: Set<String>, packageName: String): Set<String> =
    if (packageName in hidden) hidden - packageName else hidden + packageName
