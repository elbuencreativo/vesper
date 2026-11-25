package com.minimal.tvlauncher.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp

/** Si es false, ningún FocusTile bajo este proveedor puede tomar el foco (p. ej. mientras hay un menú modal abierto). */
val LocalFocusEnabled = androidx.compose.runtime.compositionLocalOf { true }

private val CONFIRM_KEYS = setOf(
    AndroidKeyEvent.KEYCODE_DPAD_CENTER,
    AndroidKeyEvent.KEYCODE_ENTER,
    AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
)

/**
 * Elemento enfocable con el control: OK = onClick, mantener OK = onLongClick.
 * Al enfocar crece un 8 % y muestra un contorno blanco fino.
 */
@Composable
fun FocusTile(
    shape: Shape,
    modifier: Modifier = Modifier,
    onFocusChange: (Boolean) -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    focusScale: Float = 1.08f,
    onClick: () -> Unit,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val press = remember { OkPressTracker() }
    val focusEnabled = LocalFocusEnabled.current
    val scale by animateFloatAsState(if (focused) focusScale else 1f, tween(180), label = "tileScale")

    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            // Resplandor cálido (naranja de la paleta) solo al enfocar; el contorno es el ámbar de acento.
            .then(
                if (focused) Modifier.shadow(14.dp, shape, ambientColor = Palette.AccentGlow, spotColor = Palette.AccentGlow)
                else Modifier,
            )
            .clip(shape)
            .then(if (focused) Modifier.border(2.5.dp, Palette.Focus, shape) else Modifier)
            .onKeyEvent { e ->
                val k = e.nativeKeyEvent
                if (k.keyCode !in CONFIRM_KEYS) return@onKeyEvent false
                when (e.type) {
                    KeyEventType.KeyDown -> {
                        if (press.onDown(k.repeatCount, k.isLongPress, onLongClick != null)) onLongClick?.invoke()
                        true
                    }
                    KeyEventType.KeyUp -> {
                        if (press.onUp()) onClick()
                        true
                    }
                    else -> false
                }
            }
            .onFocusChanged {
                focused = it.isFocused
                onFocusChange(it.isFocused)
            }
            .focusable(enabled = focusEnabled),
    ) { content(focused) }
}
