package com.minimal.tvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.minimal.tvlauncher.data.Cities
import com.minimal.tvlauncher.data.City
import com.minimal.tvlauncher.data.DisplaySettings
import com.minimal.tvlauncher.data.ThemeMode
import com.minimal.tvlauncher.data.greetingNameFor
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
    homeKeyActive: Boolean,
    onFixHomeKey: () -> Unit,
    updateStatus: String?,
    updateVersion: String?,
    updateBusy: Boolean,
    onCheckUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    display: DisplaySettings,
    profileName: String?,
    onDisplay: ((DisplaySettings) -> DisplaySettings) -> Unit,
    onBack: () -> Unit,
) {
    val first = remember { FocusRequester() }
    var picker by remember { mutableStateOf<Picker?>(null) }
    var editingName by remember { mutableStateOf(false) }
    BackHandler(enabled = picker == null && !editingName, onBack = onBack)
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.padding(start = 48.dp, top = 40.dp).width(360.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Label("AJUSTES DEL LAUNCHER", color = Palette.Accent.copy(alpha = 0.85f), size = 10.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
            }
            item {
                MenuItem(
                    when {
                        updateBusy -> "Un momento…"
                        updateVersion != null -> "Instalar versión $updateVersion"
                        else -> "Buscar actualización"
                    },
                    if (updateVersion != null) onInstallUpdate else onCheckUpdate,
                    Modifier.focusRequester(first),
                )
            }
            updateStatus?.let { item { Label(it, Modifier.padding(horizontal = 14.dp), Palette.Accent, 11.sp, maxLines = 3) } }
            item { SectionLabel("RELOJES") }
            item {
                MenuItem("Reloj principal: ${cityLabel(Cities.byName(display.primaryCity))}", { picker = Picker.Primary })
            }
            item {
                MenuItem(
                    "Reloj secundario: " + (Cities.byName(display.secondaryCity)?.let(::cityLabel) ?: "ninguno"),
                    { picker = Picker.Secondary },
                )
            }
            item { SectionLabel("SALUDO") }
            item {
                MenuItem(
                    "Saludo al encender: " + if (display.greetingEnabled) "sí" else "no",
                    { onDisplay { it.copy(greetingEnabled = !it.greetingEnabled) } },
                )
            }
            item {
                MenuItem(
                    "Usar el nombre del perfil de Android: " + if (display.useProfileName) "sí" else "no",
                    { onDisplay { it.copy(useProfileName = !it.useProfileName) } },
                )
            }
            item {
                val shown = greetingNameFor(display, profileName)
                MenuItem("Nombre del saludo: $shown", { editingName = true })
            }
            item { SectionLabel("TEMA") }
            item {
                MenuItem(
                    "Tema: " + when (display.themeMode) {
                        ThemeMode.Auto -> "automático (claro 6 am – 6 pm)"
                        ThemeMode.Light -> "siempre claro"
                        ThemeMode.Dark -> "siempre oscuro"
                    },
                    { onDisplay { it.copy(themeMode = ThemeMode.entries[(it.themeMode.ordinal + 1) % ThemeMode.entries.size]) } },
                )
            }
            item { SectionLabel("SISTEMA") }
            item {
                MenuItem(
                    if (hiddenCount > 0) "Mostrar apps ocultas ($hiddenCount)" else "No hay apps ocultas",
                    onUnhideAll,
                )
            }
            item {
                if (homeKeyActive) {
                    Label("Botón HOME: activo", Modifier.padding(horizontal = 14.dp, vertical = 6.dp), Palette.TextMuted, 11.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MenuItem("Activar el botón HOME", onFixHomeKey)
                        Label(
                            "Inactivo: HOME no abre este launcher. Android lo apaga si se fuerza el cierre del app; actívalo en Accesibilidad.",
                            Modifier.padding(horizontal = 14.dp), Palette.Accent, 11.sp, maxLines = 3,
                        )
                    }
                }
            }
            item { MenuItem("Volver al launcher original", onChooseHome) }
            item {
                Spacer(Modifier.height(16.dp))
                Label("Vesper $versionName", color = Palette.TextMuted, size = 10.sp)
                Spacer(Modifier.height(32.dp))
            }
        }
        picker?.let { which ->
            CityPicker(
                title = if (which == Picker.Primary) "Reloj principal" else "Reloj secundario",
                allowNone = which == Picker.Secondary,
                onPick = { city ->
                    onDisplay {
                        if (which == Picker.Primary) it.copy(primaryCity = city?.name ?: it.primaryCity)
                        else it.copy(secondaryCity = city?.name)
                    }
                    picker = null
                },
                onDismiss = { picker = null },
            )
        }
        if (editingName) {
            NameEditor(
                initial = display.greetingName,
                onDone = { name -> onDisplay { it.copy(greetingName = name.trim().ifEmpty { "familia" }, useProfileName = false) }; editingName = false },
                onDismiss = { editingName = false },
            )
        }
    }
}

private enum class Picker { Primary, Secondary }

private fun cityLabel(c: City?): String = c?.let { "${it.name}, ${it.country}" } ?: "—"

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(10.dp))
    Label(text, Modifier.padding(horizontal = 14.dp), Palette.TextMuted.copy(alpha = 0.7f), 9.sp, letterSpacing = 2.sp)
}

/** Lista de ciudades en un panel modal; el foco entra en la ciudad actual no, en la primera. */
@Composable
private fun CityPicker(title: String, allowNone: Boolean, onPick: (City?) -> Unit, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    BackHandler(onBack = onDismiss)
    LaunchedEffect(Unit) {
        repeat(8) {
            if (runCatching { first.requestFocus() }.isSuccess) return@LaunchedEffect
            kotlinx.coroutines.delay(60)
        }
    }
    Box(Modifier.fillMaxSize().background(Palette.Navy.copy(alpha = 0.88f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(340.dp).fillMaxHeight(0.8f).background(Palette.SurfaceHigh, RoundedCornerShape(16.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Label(title, color = Palette.Text, size = 14.sp)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                var firstDone = false
                if (allowNone) {
                    item { MenuItem("Ninguno", { onPick(null) }, Modifier.focusRequester(first)) }
                    firstDone = true
                }
                itemsIndexed(Cities.all) { i, city ->
                    MenuItem(cityLabel(city), { onPick(city) }, if (!firstDone && i == 0) Modifier.focusRequester(first) else Modifier)
                }
            }
        }
    }
}

/** Campo de texto para el nombre del saludo (usa el teclado en pantalla del sistema). */
@Composable
private fun NameEditor(initial: String, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    val focus = remember { FocusRequester() }
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    BackHandler(onBack = onDismiss)
    LaunchedEffect(Unit) {
        repeat(8) {
            if (runCatching { focus.requestFocus() }.isSuccess) return@LaunchedEffect
            kotlinx.coroutines.delay(60)
        }
    }
    Box(Modifier.fillMaxSize().background(Palette.Navy.copy(alpha = 0.88f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(340.dp).background(Palette.SurfaceHigh, RoundedCornerShape(16.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Label("Nombre del saludo", color = Palette.Text, size = 14.sp)
            Label("Se leerá: Hola, ${value.text.ifBlank { "familia" }}", color = Palette.TextMuted, size = 11.sp)
            BasicTextField(
                value = value,
                onValueChange = { value = it.copy(text = it.text.take(24)) },
                singleLine = true,
                textStyle = TextStyle(color = Palette.Text, fontSize = 16.sp),
                cursorBrush = SolidColor(Palette.Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone(value.text) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
                    .background(Palette.Navy.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(12.dp),
            )
            Label("Pulsa Listo en el teclado para guardar. Atrás cancela.", color = Palette.TextMuted, size = 10.sp)
        }
    }
}
