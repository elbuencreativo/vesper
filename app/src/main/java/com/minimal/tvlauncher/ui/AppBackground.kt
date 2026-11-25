package com.minimal.tvlauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.tvlauncher.data.ArtRepository
import com.minimal.tvlauncher.idle.ArtWork
import kotlinx.coroutines.delay

private const val WALLPAPER_ROTATE_MS = 600_000L // cambia de obra cada 10 minutos
private const val WALLPAPER_FADE_MS = 2_500

/** Opacidad base de la capa de la paleta sobre la obra: arriba clara, abajo (donde está la interfaz) más densa. */
private const val SCRIM_TOP = 0.34f
private const val SCRIM_MID = 0.46f
private const val SCRIM_BOTTOM = 0.86f

private class Wallpaper(val work: ArtWork, val image: ImageBitmap)

/**
 * Fondo permanente de todas las pantallas: una obra a pantalla completa bajo una capa de la paleta
 * que mantiene legible la interfaz. Con [clear] (modo "ver obra") la capa casi desaparece y se muestra la ficha.
 */
@Composable
fun AppBackground(art: ArtRepository, modifier: Modifier = Modifier, clear: Boolean = false) {
    var wallpaper by remember { mutableStateOf<Wallpaper?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            art.next()?.let { work -> art.loadBitmap(work)?.let { wallpaper = Wallpaper(work, it) } }
            delay(WALLPAPER_ROTATE_MS)
        }
    }
    // 1 = capa normal; 0.08 = casi transparente (obra a la vista).
    val strength by animateFloatAsState(if (clear) 0.08f else 1f, tween(600), label = "scrim")
    Box(modifier.fillMaxSize().background(Palette.Navy)) {
        Crossfade(wallpaper, Modifier.fillMaxSize(), tween(WALLPAPER_FADE_MS), label = "wallpaper") { w ->
            if (w != null) Image(w.image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Palette.Navy.copy(alpha = SCRIM_TOP * strength),
                    0.5f to Palette.Wine900.copy(alpha = SCRIM_MID * strength),
                    1f to Palette.Navy.copy(alpha = SCRIM_BOTTOM * strength),
                ),
            ),
        )
        AnimatedVisibility(
            visible = clear,
            enter = fadeIn(tween(600)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.BottomStart),
        ) {
            wallpaper?.let { ArtCaption(it.work) }
        }
    }
}

@Composable
private fun ArtCaption(work: ArtWork) {
    Box(
        Modifier.fillMaxWidth().height(120.dp)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Palette.Navy.copy(alpha = 0.78f)))),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column(Modifier.padding(start = 48.dp, bottom = 32.dp)) {
            Label(work.title, color = Palette.Text, size = 12.sp)
            Spacer(Modifier.height(4.dp))
            Label(
                "${work.artist} · ${work.year} · NATIONAL GALLERY OF ART".uppercase(),
                color = Palette.Text.copy(alpha = 0.62f), size = 7.sp, letterSpacing = 1.5.sp,
            )
        }
    }
}
