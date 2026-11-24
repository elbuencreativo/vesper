package com.minimal.tvlauncher.ui

import androidx.compose.foundation.background
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
) {
    val cardFocus = remember { FocusRequester() }
    val appFocus = remember { FocusRequester() }

    // El foco inicial va a la primera tarjeta; si no hay, al primer app.
    LaunchedEffect(cards.isEmpty(), apps.row.isEmpty()) {
        runCatching {
            if (cards.isNotEmpty()) cardFocus.requestFocus() else if (apps.row.isNotEmpty()) appFocus.requestFocus()
        }
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Palette.BackgroundTop, Palette.Background))),
    ) {
        Column(Modifier.fillMaxSize()) {
            TopBar(onAction, Modifier.padding(start = 48.dp, end = 48.dp, top = 32.dp))
            Spacer(Modifier.height(36.dp))
            if (cards.isNotEmpty()) {
                ContinueRow(cards, cardFocus, onCard)
            }
            Spacer(Modifier.weight(1f))
            AppRow(apps.row, appFocus, onLaunch, onClose, onOpenAll, Modifier.padding(bottom = 24.dp))
        }
    }
}
