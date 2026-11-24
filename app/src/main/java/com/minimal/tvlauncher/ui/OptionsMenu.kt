package com.minimal.tvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Menú modal de opciones sobre cualquier pantalla. Atrás lo cierra. El foco entra en la primera opción. */
@Composable
fun OptionsMenu(title: String, options: List<Pair<String, () -> Unit>>, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    BackHandler(onBack = onDismiss)
    LaunchedEffect(Unit) {
        repeat(8) {
            if (runCatching { first.requestFocus() }.isSuccess) return@LaunchedEffect
            delay(60)
        }
    }
    Box(Modifier.fillMaxSize().background(Palette.Navy.copy(alpha = 0.82f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(300.dp).background(Palette.SurfaceHigh, RoundedCornerShape(16.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Label(title, color = Palette.Text, size = 14.sp)
            Spacer(Modifier.height(6.dp))
            options.forEachIndexed { i, (label, action) ->
                MenuItem(label, action, if (i == 0) Modifier.focusRequester(first) else Modifier)
            }
        }
    }
}
