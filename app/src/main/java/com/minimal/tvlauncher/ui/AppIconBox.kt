package com.minimal.tvlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Icono de app sobre un fondo del color de su borde: así el marco redondeado y el icono cuadrado se
 * funden en una sola pieza (sin doble marco), con o sin foco. Solo los apps ocultos van apagados.
 * El recorte redondeado lo pone quien lo usa (FocusTile o clip).
 */
@Composable
fun AppIconBox(icon: AppIcon?, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    val edge = icon?.edge
    val base = when {
        edge == null -> Palette.Surface
        dimmed -> lerp(Color(warmMono(edge)), Palette.Surface, 0.45f)
        else -> Color(edge)
    }
    Box(modifier.background(if (dimmed) base.copy(alpha = 0.45f) else base), contentAlignment = Alignment.Center) {
        icon?.let {
            Image(
                it.image, null, Modifier.fillMaxSize(0.72f),
                alpha = if (dimmed) 0.3f else 1f,
                colorFilter = if (dimmed) WarmMonochrome else null,
            )
        }
    }
}
