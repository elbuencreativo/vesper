package com.minimal.tvlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.tvlauncher.data.LocalPsnRepository
import com.minimal.tvlauncher.data.PsnInfo
import kotlinx.coroutines.delay

/**
 * Tarjeta alargada con el usuario de PlayStation y su último juego (nombre y horas jugadas).
 * Solo informa: no toma el foco. Sin sesión o sin red no se dibuja nada.
 */
@Composable
fun PsnCard(modifier: Modifier = Modifier) {
    val repo = LocalPsnRepository.current ?: return
    val info by produceState<PsnInfo?>(null) {
        while (true) {
            value = repo.load()
            delay(10 * 60 * 1000L)
        }
    }
    val i = info ?: return
    val posters = LocalPosterLoader.current
    val avatar by produceState<ImageBitmap?>(null, i.avatarUrl) { value = i.avatarUrl?.let { posters.load(it) } }
    val cover by produceState<ImageBitmap?>(null, i.gameImageUrl) { value = i.gameImageUrl?.let { posters.load(it) } }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier.width(300.dp).height(52.dp).clip(shape)
            .background(Palette.Surface.copy(alpha = 0.7f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(34.dp).clip(CircleShape).background(Palette.SurfaceHigh)) {
            avatar?.let { Image(it, null, Modifier.fillMaxHeight(), contentScale = ContentScale.Crop) }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Label(i.onlineId, color = Palette.Text, size = 12.sp, weight = FontWeight.Medium, maxLines = 1)
            if (i.game != null) {
                Label(
                    i.game, color = Palette.TextMuted, size = 10.sp, maxLines = 1,
                )
                if (i.played != null) Label(i.played + " jugadas", color = Palette.Accent.copy(alpha = 0.85f), size = 9.sp, maxLines = 1)
            }
        }
        Spacer(Modifier.width(8.dp))
        cover?.let {
            Image(it, null, Modifier.size(52.dp), contentScale = ContentScale.Crop)
        }
    }
}
