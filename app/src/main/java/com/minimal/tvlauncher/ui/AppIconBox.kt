package com.minimal.tvlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Icono de app sobre un fondo del color de su borde, para que el marco redondeado y el icono cuadrado
 * se fundan en una sola pieza (sin doble marco). Con foco va a todo color; sin foco (y apagado si está
 * oculto) en monocromo sepia. El fondo y el icono reciben exactamente el mismo tratamiento (mismo color
 * sepia y misma transparencia aplicada al conjunto), así no se forma un cuadro interior distinto.
 * El recorte redondeado lo pone quien lo usa (FocusTile o clip).
 */
@Composable
fun AppIconBox(icon: AppIcon?, lit: Boolean, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    val mono = !lit || dimmed
    val edge = icon?.edge
    val background = when {
        edge == null -> Palette.Surface
        mono -> Color(warmMono(edge))
        else -> Color(edge)
    }
    Box(
        modifier.graphicsLayer { alpha = if (dimmed) 0.4f else if (mono) 0.9f else 1f }.background(background),
        contentAlignment = Alignment.Center,
    ) {
        icon?.let {
            Image(
                it.image, null, Modifier.fillMaxSize(0.72f),
                colorFilter = if (mono) WarmMonochrome else null,
            )
        }
    }
}
