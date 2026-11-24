package com.minimal.tvlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Degradados de tarjeta con la paleta: siempre oscuros para que el título se lea.
private val CardGradients = listOf(
    Palette.Wine700 to Palette.Navy,
    Palette.Crimson.copy(alpha = 0.85f) to Palette.Wine900,
    Palette.Wine900 to Palette.Navy,
    Palette.Vermilion.copy(alpha = 0.65f) to Palette.Wine900,
    Palette.Orange.copy(alpha = 0.55f) to Palette.Wine700,
    Palette.Amber.copy(alpha = 0.45f) to Palette.Wine900,
)

@Composable
fun ContinueRow(
    cards: List<ContinueCard>,
    firstFocus: FocusRequester,
    onClick: (ContinueCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Label(
            "SEGUIR VIENDO", Modifier.padding(start = 48.dp).offset(y = 22.dp), Palette.Accent.copy(alpha = 0.85f), 9.sp,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 32.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(cards, key = { i, c -> "$i:${c.packageName}:${c.title}" }) { i, card ->
                ContinueCardView(
                    card,
                    onClick = { onClick(card) },
                    modifier = if (i == 0) Modifier.focusRequester(firstFocus) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun ContinueCardView(card: ContinueCard, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(8.dp)
    val (top, bottom) = CardGradients[Math.floorMod(card.packageName.hashCode(), CardGradients.size)]
    val icons = LocalIconCache.current
    val icon by produceState<ImageBitmap?>(null, card.packageName) { value = icons.get(card.packageName) }
    val posters = LocalPosterLoader.current
    val poster by produceState<ImageBitmap?>(null, card.posterUri) { value = card.posterUri?.let { posters.load(it) } }

    FocusTile(shape, modifier.width(184.dp).aspectRatio(16f / 9f), onClick = onClick) { focused ->
        Box(Modifier.fillMaxHeight().fillMaxWidth().background(Brush.linearGradient(listOf(top, bottom)))) {
            poster?.let {
                Image(it, card.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            if (poster == null) icon?.let {
                Image(
                    it, null,
                    Modifier.size(34.dp).align(Alignment.Center),
                    alpha = if (focused) 0.95f else 0.55f,
                    colorFilter = if (focused) null else WarmMonochrome,
                )
            }
            Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Palette.Navy.copy(alpha = 0.85f))))
                    .padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 8.dp),
            ) {
                Label(card.title, color = Palette.Text, size = 11.sp)
            }
            card.progress?.let { p ->
                Box(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().height(2.dp)
                        .background(Color(0x33FFFFFF)),
                ) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(p.coerceIn(0f, 1f)).background(Palette.Accent))
                }
            }
        }
    }
}
