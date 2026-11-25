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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
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
    onCardMenu: (ContinueCard) -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onClose: (AppEntry) -> Unit,
    onOpenAll: () -> Unit,
    onManage: () -> Unit,
    attention: Boolean,
    refocusKey: Int = 0,
    focusBlocked: Boolean = false,
    message: String? = null,
) {
    // Conserva el último texto para que el aviso no quede vacío mientras se desvanece.
    val lastMessage = remember { androidx.compose.runtime.mutableStateOf("") }
    if (message != null) lastMessage.value = message
    val cardFocus = remember { FocusRequester() }
    val appFocus = remember { FocusRequester() }
    val memory = remember { FocusMemory() }
    memory.frozen = focusBlocked
    // Con el foco en la barra de apps, las tarjetas de "Seguir viendo" se reducen.
    var inApps by remember { androidx.compose.runtime.mutableStateOf(false) }

    // Al cerrar un menú modal, el foco vuelve al elemento que lo tenía.
    LaunchedEffect(refocusKey) {
        if (refocusKey == 0) return@LaunchedEffect
        repeat(8) {
            if (memory.restore(if (cards.isNotEmpty()) cardFocus else appFocus)) return@LaunchedEffect
            kotlinx.coroutines.delay(60)
        }
    }

    // El foco inicial va a la primera tarjeta; si no hay, al primer app.
    LaunchedEffect(cards.isEmpty(), apps.row.isEmpty()) {
        runCatching {
            if (cards.isNotEmpty()) cardFocus.requestFocus() else if (apps.row.isNotEmpty()) appFocus.requestFocus()
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Con un menú abierto, lo que hay detrás no puede tomar el foco (si no, las flechas se escapan al fondo).
        androidx.compose.runtime.CompositionLocalProvider(LocalFocusEnabled provides !focusBlocked) {
        Column(Modifier.fillMaxSize()) {
            TopBar(onAction, Modifier.padding(start = 48.dp, end = 48.dp, top = 32.dp), attention = attention)
            // "Seguir viendo" va pegado a la fila de apps, abajo, no arriba.
            Spacer(Modifier.weight(1f))
            if (cards.isNotEmpty()) {
                ContinueRow(cards, cardFocus, onCard, onCardMenu, memory, shrink = inApps)
            }
            AppRow(
                apps.row, appFocus, onLaunch, onClose, onOpenAll, onManage, memory,
                Modifier.padding(bottom = 14.dp).onFocusChanged { if (!focusBlocked) inApps = it.hasFocus },
            )
        }
        }
        // Reloj abajo a la derecha, centrado con la barra de apps.
        Clock(Modifier.align(Alignment.BottomEnd).padding(end = 48.dp, bottom = 58.dp))
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(260)),
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 48.dp, top = 34.dp),
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
