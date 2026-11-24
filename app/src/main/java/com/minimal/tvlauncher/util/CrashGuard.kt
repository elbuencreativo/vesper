package com.minimal.tvlauncher.util

import com.minimal.tvlauncher.data.Prefs
import kotlinx.coroutines.flow.first

/**
 * Salvavidas: si el launcher arranca [limit] veces en [windowMs] sin llegar a "sano"
 * (markHealthy tras unos segundos vivo), se cede el control al home original.
 */
class CrashGuard(
    private val prefs: Prefs,
    private val clock: Clock,
    private val limit: Int = 3,
    private val windowMs: Long = 120_000,
) {
    suspend fun onStart() {
        val now = clock.nowMs()
        prefs.setStartTimes(recent(prefs.startTimes.first(), now) + now)
    }

    suspend fun markHealthy() = prefs.setStartTimes(emptyList())

    suspend fun shouldYieldToStock(): Boolean = recent(prefs.startTimes.first(), clock.nowMs()).size >= limit

    // Las marcas de antes de un reinicio (reloj monotónico vuelve a 0) quedan en el futuro y se ignoran.
    private fun recent(times: List<Long>, now: Long) = times.filter { it in (now - windowMs)..now }
}
