package com.minimal.tvlauncher.data

/** Mueve el elemento de [from] a [to]. Si algún índice está fuera de rango o son iguales, no cambia nada. */
fun <T> moveItem(list: List<T>, from: Int, to: Int): List<T> {
    if (from !in list.indices || to !in list.indices || from == to) return list
    val mutable = list.toMutableList()
    mutable.add(to, mutable.removeAt(from))
    return mutable
}

fun toggleHidden(hidden: Set<String>, packageName: String): Set<String> =
    if (packageName in hidden) hidden - packageName else hidden + packageName
