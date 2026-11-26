package com.minimal.tvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.tvlauncher.actions.RemoteKeys
import com.minimal.tvlauncher.data.AppEntry
import kotlinx.coroutines.delay

/**
 * Ajustes del control remoto: cada botón de acceso directo (YouTube, Paramount, Disney+, la estrella…) se
 * puede asignar a un app. El botón se "aprende" al pulsarlo. Funciona desde cualquier app, vía el servicio de accesibilidad.
 */
@Composable
fun RemoteSettingsScreen(apps: List<AppEntry>, serviceActive: Boolean, onFixService: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(RemoteKeys.isEnabled(context)) }
    var map by remember { mutableStateOf(RemoteKeys.load(context)) }
    var step by remember { mutableStateOf<Step>(Step.None) }
    val first = remember { FocusRequester() }
    fun update(new: Map<String, String>) { map = new; RemoteKeys.save(context, new) }
    fun label(pkg: String) = apps.firstOrNull { it.packageName == pkg }?.label ?: pkg

    BackHandler(enabled = step == Step.None, onBack = onBack)
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.padding(start = 48.dp, top = 40.dp).width(420.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Label("AJUSTES DEL CONTROL", color = Palette.Accent.copy(alpha = 0.85f), size = 10.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
            }
            item {
                MenuItem(
                    "Botones de acceso directo: " + if (enabled) "sí" else "no",
                    { enabled = !enabled; RemoteKeys.setEnabled(context, enabled) },
                    Modifier.focusRequester(first),
                )
            }
            if (!serviceActive) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MenuItem("Activar el servicio (Accesibilidad)", onFixService)
                    Label(
                        "Inactivo: sin el servicio de Vesper en Accesibilidad los botones no se pueden reasignar.",
                        Modifier.padding(horizontal = 14.dp), Palette.Accent, 11.sp, maxLines = 3,
                    )
                }
            }
            item {
                Label(
                    "Elige un botón del control (YouTube, Paramount, Disney+, la estrella…) y el app que debe abrir. Funciona en cualquier pantalla.",
                    Modifier.padding(horizontal = 14.dp, vertical = 4.dp), Palette.TextMuted.copy(alpha = 0.7f), 10.sp, maxLines = 3,
                )
            }
            item { MenuItem("+ Asignar un botón", { step = Step.Learning }) }
            items(map.entries.toList(), key = { it.key }) { (id, pkg) ->
                MenuItem("${RemoteKeys.label(id)}  →  ${label(pkg)}", { step = Step.Edit(id) })
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
        when (val s = step) {
            Step.None -> Unit
            Step.Learning -> LearnOverlay(
                onKey = { id -> step = Step.Pick(id) },
                onCancel = { step = Step.None },
            )
            is Step.Pick -> AppPicker(
                title = "${RemoteKeys.label(s.id)}  →  ¿qué app abre?",
                apps = apps,
                onPick = { app -> update(map + (s.id to app.packageName)); step = Step.None },
                onDismiss = { step = Step.None },
            )
            is Step.Edit -> OptionsMenu(
                title = RemoteKeys.label(s.id),
                options = listOf(
                    "Cambiar de app" to { step = Step.Pick(s.id) },
                    "Quitar" to { update(map - s.id); step = Step.None },
                    "Cancelar" to { step = Step.None },
                ),
                onDismiss = { step = Step.None },
            )
        }
    }
}

private sealed interface Step {
    data object None : Step
    data object Learning : Step
    data class Pick(val id: String) : Step
    data class Edit(val id: String) : Step
}

/** Espera la próxima pulsación de un botón del control (la captura el servicio de accesibilidad). */
@Composable
private fun LearnOverlay(onKey: (String) -> Unit, onCancel: () -> Unit) {
    BackHandler(onBack = onCancel)
    DisposableEffect(Unit) {
        RemoteKeys.learning = onKey
        onDispose { RemoteKeys.learning = null }
    }
    Box(Modifier.fillMaxSize().background(Palette.Navy.copy(alpha = 0.88f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(380.dp).background(Palette.SurfaceHigh, RoundedCornerShape(16.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Label("Pulsa el botón del control", color = Palette.Text, size = 16.sp)
            Label(
                "YouTube, Paramount, Disney+, la estrella… Atrás para cancelar.",
                color = Palette.TextMuted, size = 11.sp, maxLines = 3,
            )
        }
    }
}

@Composable
private fun AppPicker(title: String, apps: List<AppEntry>, onPick: (AppEntry) -> Unit, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    BackHandler(onBack = onDismiss)
    LaunchedEffect(Unit) {
        repeat(8) {
            if (runCatching { first.requestFocus() }.isSuccess) return@LaunchedEffect
            delay(60)
        }
    }
    Box(Modifier.fillMaxSize().background(Palette.Navy.copy(alpha = 0.88f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(360.dp).fillMaxHeight(0.8f).background(Palette.SurfaceHigh, RoundedCornerShape(16.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Label(title, color = Palette.Text, size = 14.sp)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(apps.size) { i ->
                    MenuItem(apps[i].label, { onPick(apps[i]) }, if (i == 0) Modifier.focusRequester(first) else Modifier)
                }
            }
        }
    }
}
