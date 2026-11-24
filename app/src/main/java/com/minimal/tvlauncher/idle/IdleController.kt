package com.minimal.tvlauncher.idle

import com.minimal.tvlauncher.util.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class IdleState { Active, Art, Dark }

/**
 * Active -> Art tras [idleAfterMs] sin pulsaciones; Art -> Dark tras [darkAfterMs] **de arte**.
 * Solo corre mientras el launcher está al frente.
 */
class IdleController(
    scope: CoroutineScope,
    private val clock: Clock,
    private val idleAfterMs: Long = 300_000,
    private val darkAfterMs: Long = 1_200_000,
) {
    private val _state = MutableStateFlow(IdleState.Active)
    val state: StateFlow<IdleState> = _state

    private var lastInputAt = clock.nowMs()
    private var artSince = 0L
    private var paused = false

    init {
        scope.launch {
            while (true) {
                delay(TICK_MS)
                evaluate()
            }
        }
    }

    /** Devuelve true si la pulsación debe descartarse (solo despertó el reposo). */
    fun onUserInput(): Boolean {
        val wasIdle = _state.value != IdleState.Active
        lastInputAt = clock.nowMs()
        _state.value = IdleState.Active
        return wasIdle
    }

    fun onLauncherPaused() {
        paused = true
        _state.value = IdleState.Active
    }

    fun onLauncherResumed() {
        paused = false
        lastInputAt = clock.nowMs()
        _state.value = IdleState.Active
    }

    private fun evaluate() {
        if (paused) return
        val now = clock.nowMs()
        when (_state.value) {
            IdleState.Active -> if (now - lastInputAt >= idleAfterMs) {
                artSince = now
                _state.value = IdleState.Art
            }
            IdleState.Art -> if (now - artSince >= darkAfterMs) _state.value = IdleState.Dark
            IdleState.Dark -> Unit
        }
    }

    private companion object {
        const val TICK_MS = 1_000L
    }
}
