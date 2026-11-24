package com.minimal.tvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Ajustes mínimos del launcher. Aquí crecerán las opciones futuras. */
@Composable
fun LauncherSettingsScreen(
    hiddenCount: Int,
    versionName: String,
    onUnhideAll: () -> Unit,
    onChooseHome: () -> Unit,
    onBack: () -> Unit,
) {
    val first = remember { FocusRequester() }
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 48.dp, top = 40.dp).width(360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label("AJUSTES DEL LAUNCHER", color = Palette.Accent.copy(alpha = 0.85f), size = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(16.dp))
            MenuItem(
                if (hiddenCount > 0) "Mostrar apps ocultas ($hiddenCount)" else "No hay apps ocultas",
                onUnhideAll,
                Modifier.focusRequester(first),
            )
            MenuItem("Volver al launcher original", onChooseHome)
            Spacer(Modifier.height(24.dp))
            Label("TV Launcher $versionName", color = Palette.TextMuted, size = 10.sp)
        }
    }
}
