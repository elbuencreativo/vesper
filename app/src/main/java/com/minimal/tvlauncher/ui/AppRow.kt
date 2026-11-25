package com.minimal.tvlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.tvlauncher.data.AppEntry

/**
 * Fila de apps: iconos en escala de grises que recuperan el color al enfocar.
 * OK abre, mantener OK cierra el app. El nombre del app enfocado aparece debajo.
 */
@Composable
fun AppRow(
    apps: List<AppEntry>,
    firstFocus: FocusRequester,
    onLaunch: (AppEntry) -> Unit,
    onClose: (AppEntry) -> Unit,
    onOpenAll: () -> Unit,
    onManage: () -> Unit,
    memory: FocusMemory,
    style: com.minimal.tvlauncher.data.AppBarStyle = com.minimal.tvlauncher.data.AppBarStyle.Icons,
    modifier: Modifier = Modifier,
) {
    val textStyle = style == com.minimal.tvlauncher.data.AppBarStyle.Text
    var focusedLabel by remember { mutableStateOf("") }
    Column(modifier) {
        Label("APPS", Modifier.padding(start = 48.dp), Palette.Accent.copy(alpha = 0.85f), 9.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(0.dp))
        LazyRow(
            // En texto la fila termina antes del reloj (abajo a la derecha) y se desplaza si no cabe.
            modifier = if (textStyle) Modifier.padding(end = 200.dp) else Modifier,
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(if (textStyle) 10.dp else 14.dp),
        ) {
            items(apps, key = { it.packageName }) { app ->
                val memoryKey = "a:${app.packageName}"
                val requester = remember(memoryKey) { if (app == apps.first()) firstFocus else FocusRequester() }
                DisposableEffect(memoryKey) {
                    memory.requesters[memoryKey] = requester
                    onDispose { memory.requesters.remove(memoryKey) }
                }
                if (textStyle) TextAppTile(
                    app,
                    onFocus = { focusedLabel = ""; memory.remember(memoryKey) }, // el nombre ya está en el texto
                    onLaunch = { onLaunch(app) },
                    onClose = { onClose(app) },
                    modifier = Modifier.focusRequester(requester),
                ) else AppTile(
                    app,
                    onFocus = { focusedLabel = app.label; memory.remember(memoryKey) },
                    onLaunch = { onLaunch(app) },
                    onClose = { onClose(app) },
                    modifier = Modifier.focusRequester(requester),
                )
            }
            item(key = "all-apps") {
                val plusRequester = remember { FocusRequester() }
                DisposableEffect(Unit) {
                    memory.requesters["plus"] = plusRequester
                    onDispose { memory.requesters.remove("plus") }
                }
                FocusTile(
                    CircleShape, Modifier.size(48.dp).focusRequester(plusRequester),
                    onFocusChange = { if (it) { focusedLabel = "Todas las apps"; memory.remember("plus") } },
                    onLongClick = onManage,
                    onClick = onOpenAll,
                ) { focused ->
                    Canvas(
                        Modifier.fillMaxSize().background(Palette.Surface.copy(alpha = 0.86f), CircleShape)
                            .border(1.dp, Palette.Line, CircleShape).padding(16.dp),
                    ) {
                        val c = if (focused) Palette.Accent else Palette.TextMuted
                        val w = size.width
                        drawLine(c, Offset(0f, w / 2), Offset(w, w / 2), 2.5f, StrokeCap.Round)
                        drawLine(c, Offset(w / 2, 0f), Offset(w / 2, w), 2.5f, StrokeCap.Round)
                    }
                }
            }
        }
        Label(
            focusedLabel, Modifier.padding(start = 48.dp).height(16.dp),
            Palette.TextMuted, 11.sp, letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun AppTile(
    app: AppEntry,
    onFocus: () -> Unit,
    onLaunch: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    val icons = LocalIconCache.current
    val icon by produceState<AppIcon?>(null, app.packageName) { value = icons.getAppIcon(app.packageName) }
    FocusTile(
        RoundedCornerShape(12.dp), modifier.size(52.dp),
        onFocusChange = { if (it) onFocus() },
        onLongClick = onClose,
        onClick = onLaunch,
    ) { focused ->
        // El marco redondeado se pinta del color de fondo del icono: una sola pieza, sin doble marco.
        AppIconBox(icon, lit = focused, modifier = Modifier.fillMaxSize().border(1.dp, Palette.Line, RoundedCornerShape(12.dp)))
    }
}

/** Estilo texto: punto del color de la marca + nombre; el foco lo marca un subrayado del mismo color. */
@Composable
private fun TextAppTile(
    app: AppEntry,
    onFocus: () -> Unit,
    onLaunch: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    val icons = LocalIconCache.current
    val icon by produceState<AppIcon?>(null, app.packageName) { value = icons.getAppIcon(app.packageName) }
    val brand = icon?.brand?.let { androidx.compose.ui.graphics.Color(it) } ?: Palette.Accent
    FocusTile(
        RoundedCornerShape(6.dp), modifier.height(52.dp),
        onFocusChange = { if (it) onFocus() },
        onLongClick = onClose,
        onClick = onLaunch,
        plain = true,
    ) { focused ->
        // Casilla de ancho fijo: todos los nombres ocupan lo mismo (hasta 2 líneas) y forman una rejilla.
        Column(
            Modifier.width(TEXT_TILE_WIDTH).fillMaxHeight().padding(start = 6.dp, end = 6.dp, bottom = 7.dp),
            verticalArrangement = Arrangement.Bottom, // el subrayado fija la base; el texto crece hacia arriba
        ) {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 6.dp).size(7.dp).background(brand, CircleShape))
                Spacer(Modifier.size(8.dp))
                Label(
                    app.label,
                    color = if (focused) Palette.Text else Palette.Text.copy(alpha = 0.6f),
                    size = 14.sp,
                    weight = if (focused) androidx.compose.ui.text.font.FontWeight.Medium else androidx.compose.ui.text.font.FontWeight.Normal,
                    maxLines = 2,
                )
            }
            Spacer(Modifier.height(3.dp))
            Box(Modifier.height(2.dp).fillMaxWidth().background(if (focused) brand else androidx.compose.ui.graphics.Color.Transparent))
        }
    }
}

private val TEXT_TILE_WIDTH = 124.dp
