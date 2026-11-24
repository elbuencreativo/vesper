package com.minimal.tvlauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.minimal.tvlauncher.data.AppEntry
import com.minimal.tvlauncher.data.ArrangedApps

@Composable
fun HomeScreen(
    apps: ArrangedApps,
    cards: List<ContinueCard>,
    onAction: (QuickAction) -> Unit,
    onCard: (ContinueCard) -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onClose: (AppEntry) -> Unit,
    onOpenAll: () -> Unit,
    message: String? = null,
) {
    // Conserva el último texto para que el aviso no quede vacío mientras se desvanece.
    val lastMessage = remember { androidx.compose.runtime.mutableStateOf("") }
    if (message != null) lastMessage.value = message
    val cardFocus = remember { FocusRequester() }
    val appFocus = remember { FocusRequester() }

    // El foco inicial va a la primera tarjeta; si no hay, al primer app.
    LaunchedEffect(cards.isEmpty(), apps.row.isEmpty()) {
        runCatching {
            if (cards.isNotEmpty()) cardFocus.requestFocus() else if (apps.row.isNotEmpty()) appFocus.requestFocus()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(onAction, Modifier.padding(start = 48.dp, end = 48.dp, top = 32.dp))
            // "Seguir viendo" va pegado a la fila de apps, abajo, no arriba.
            Spacer(Modifier.weight(1f))
            if (cards.isNotEmpty()) {
                ContinueRow(cards, cardFocus, onCard)
                Spacer(Modifier.height(18.dp))
            }
            AppRow(apps.row, appFocus, onLaunch, onClose, onOpenAll, Modifier.padding(bottom = 24.dp))
        }
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(260)),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 48.dp, bottom = 40.dp),
        ) {
            Box(
                Modifier.clip(RoundedCornerShape(20.dp)).background(Palette.SurfaceHigh)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Label(lastMessage.value, color = Palette.Text, size = 12.sp)
            }
        }
    }
}
