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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardGradients = listOf(
    Color(0xFF3A2424) to Color(0xFF16100F),
    Color(0xFF1F3342) to Color(0xFF0E141A),
    Color(0xFF2E3A22) to Color(0xFF12170E),
    Color(0xFF38294A) to Color(0xFF15101C),
    Color(0xFF3F3524) to Color(0xFF17130D),
    Color(0xFF243B3A) to Color(0xFF0E1717),
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
            "SEGUIR VIENDO", Modifier.padding(start = 48.dp), Palette.TextMuted, 9.sp,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
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

    FocusTile(shape, modifier.width(184.dp).aspectRatio(16f / 9f), onClick = onClick) { focused ->
        Box(Modifier.fillMaxHeight().fillMaxWidth().background(Brush.linearGradient(listOf(top, bottom)))) {
            icon?.let {
                Image(
                    it, null,
                    Modifier.size(34.dp).align(Alignment.Center),
                    alpha = if (focused) 0.95f else 0.55f,
                    colorFilter = if (focused) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                )
            }
            Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                    .padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 8.dp),
            ) {
                Label(card.title, color = Palette.Text, size = 11.sp)
            }
            card.progress?.let { p ->
                Box(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().height(2.dp)
                        .background(Color(0x33FFFFFF)),
                ) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(p.coerceIn(0f, 1f)).background(Palette.Text))
                }
            }
        }
    }
}
