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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp

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
    onClick: () -> Unit,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var longFired by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, tween(140), label = "tileScale")

    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .border(if (focused) 2.dp else 0.dp, Palette.Focus, shape)
            .onKeyEvent { e ->
                val k = e.nativeKeyEvent
                if (k.keyCode !in CONFIRM_KEYS) return@onKeyEvent false
                when (e.type) {
                    KeyEventType.KeyDown -> {
                        if (k.repeatCount == 0) {
                            longFired = false
                        } else if (!longFired && onLongClick != null && k.isLongPress) {
                            longFired = true
                            onLongClick()
                        }
                        true
                    }
                    KeyEventType.KeyUp -> {
                        if (!longFired) onClick()
                        longFired = false
                        true
                    }
                    else -> false
                }
            }
            .onFocusChanged {
                focused = it.isFocused
                onFocusChange(it.isFocused)
            }
            .focusable(),
    ) { content(focused) }
}
