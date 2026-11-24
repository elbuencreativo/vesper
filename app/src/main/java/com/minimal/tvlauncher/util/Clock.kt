package com.minimal.tvlauncher.util

import android.os.SystemClock

fun interface Clock {
    fun nowMs(): Long
}

/** Reloj monotónico del sistema (no salta con cambios de hora). */
object MonotonicClock : Clock {
    override fun nowMs(): Long = SystemClock.elapsedRealtime()
}
