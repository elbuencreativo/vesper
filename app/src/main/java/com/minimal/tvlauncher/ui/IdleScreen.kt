package com.minimal.tvlauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.tvlauncher.data.ArtRepository
import com.minimal.tvlauncher.idle.ArtWork
import com.minimal.tvlauncher.idle.IdleState
import kotlinx.coroutines.delay

private const val ROTATE_MS = 45_000L
private const val FADE_MS = 1_500
private val SoftWhite = Color(0xDDFFFFFF)

private data class Shown(val work: ArtWork, val bitmap: ImageBitmap)

/**
 * Reposo: la interfaz se desvanece y entra una obra a pantalla completa con zoom muy lento,
 * hora pequeña y ficha. En Dark queda en negro. Sin obras, queda negro con la hora.
 */
@Composable
fun IdleScreen(state: IdleState, art: ArtRepository, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = state != IdleState.Active,
        enter = fadeIn(tween(FADE_MS)),
        exit = fadeOut(tween(350)),
        modifier = modifier,
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AnimatedVisibility(
                visible = state == IdleState.Art,
                enter = fadeIn(tween(FADE_MS)),
                exit = fadeOut(tween(FADE_MS)),
            ) {
                ArtSlideshow(art)
            }
        }
    }
}

@Composable
private fun ArtSlideshow(art: ArtRepository) {
    var shown by remember { mutableStateOf<Shown?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            val work = art.next()
            val bitmap = work?.let { art.loadBitmap(it) }
            if (work != null && bitmap != null) shown = Shown(work, bitmap)
            delay(ROTATE_MS)
        }
    }
    Box(Modifier.fillMaxSize()) {
        Crossfade(shown, Modifier.fillMaxSize(), tween(FADE_MS), label = "art") { s ->
            if (s != null) ArtFrame(s)
        }
        Clock(
            Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 48.dp),
            color = SoftWhite,
        )
    }
}

@Composable
private fun ArtFrame(s: Shown) {
    val zoom = remember(s) { Animatable(1f) }
    LaunchedEffect(s) { zoom.animateTo(1.06f, tween(ROTATE_MS.toInt() + 2_000, easing = LinearEasing)) }
    Box(Modifier.fillMaxSize()) {
        Image(
            s.bitmap, s.work.title, Modifier.fillMaxSize().graphicsLayer { scaleX = zoom.value; scaleY = zoom.value },
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier.align(Alignment.TopStart).fillMaxWidth().height(120.dp)
                .background(Brush.verticalGradient(listOf(Color(0x88000000), Color.Transparent))),
        )
        Box(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().height(170.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA000000)))),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 48.dp, bottom = 40.dp)) {
            Label(s.work.title, color = SoftWhite, size = 15.sp)
            Spacer(Modifier.height(4.dp))
            Label(
                "${s.work.artist} · ${s.work.year} · NATIONAL GALLERY OF ART".uppercase(),
                color = Color(0x99FFFFFF), size = 9.sp, letterSpacing = 1.5.sp,
            )
        }
    }
}
