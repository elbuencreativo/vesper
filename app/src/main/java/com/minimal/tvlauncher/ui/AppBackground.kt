package com.minimal.tvlauncher.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.minimal.tvlauncher.data.ArtRepository
import kotlinx.coroutines.delay

private const val WALLPAPER_ROTATE_MS = 600_000L // cambia de obra cada 10 minutos
private const val WALLPAPER_FADE_MS = 2_500

/**
 * Fondo permanente de todas las pantallas: una obra a pantalla completa bajo una capa de la paleta
 * (azul noche arriba, vino al centro) que mantiene legible la interfaz. Sin obras, queda el degradado.
 */
@Composable
fun AppBackground(art: ArtRepository, modifier: Modifier = Modifier) {
    var wallpaper by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            art.next()?.let { work -> art.loadBitmap(work)?.let { wallpaper = it } }
            delay(WALLPAPER_ROTATE_MS)
        }
    }
    Box(modifier.fillMaxSize().background(Palette.Background)) {
        Crossfade(wallpaper, Modifier.fillMaxSize(), tween(WALLPAPER_FADE_MS), label = "wallpaper") { image ->
            if (image != null) Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Palette.Navy.copy(alpha = 0.66f),
                    0.5f to Palette.Wine900.copy(alpha = 0.80f),
                    1f to Palette.Navy.copy(alpha = 0.95f),
                ),
            ),
        )
    }
}
