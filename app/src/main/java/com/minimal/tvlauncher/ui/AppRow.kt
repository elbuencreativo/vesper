package com.minimal.tvlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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
        val appTile: @Composable (AppEntry, Dp) -> Unit = { app, tileWidth ->
            val memoryKey = "a:${app.packageName}"
            val requester = remember(memoryKey) { if (app == apps.first()) firstFocus else FocusRequester() }
            DisposableEffect(memoryKey) {
                memory.requesters[memoryKey] = requester
                onDispose { memory.requesters.remove(memoryKey) }
            }
            if (textStyle) TextAppTile(
                app, tileWidth,
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
        val plusButton: @Composable () -> Unit = {
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
        if (textStyle) {
            // Dos hileras: todos los nombres a la vista, sin desplazamiento. El "+" queda centrado entre ambas.
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cols = ((apps.size + 1) / 2).coerceAtLeast(1)
                val avail = maxWidth - 48.dp - 24.dp - 48.dp - 190.dp // margen, hueco, botón, reloj
                val tileWidth = ((avail - TEXT_GAP * (cols - 1)) / cols).coerceIn(88.dp, TEXT_TILE_WIDTH)
                androidx.compose.foundation.layout.Row(
                    Modifier.padding(start = 48.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf(apps.take(cols), apps.drop(cols)).forEach { line ->
                            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(TEXT_GAP)) {
                                line.forEach { app -> androidx.compose.runtime.key(app.packageName) { appTile(app, tileWidth) } }
                            }
                        }
                    }
                    Spacer(Modifier.size(24.dp))
                    plusButton()
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(apps, key = { it.packageName }) { app -> appTile(app, TEXT_TILE_WIDTH) }
                item(key = "all-apps") { plusButton() }
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
    tileWidth: Dp,
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
            Modifier.width(tileWidth).fillMaxHeight().padding(start = 6.dp, end = 6.dp, bottom = 7.dp),
            verticalArrangement = Arrangement.Bottom, // el subrayado fija la base; el texto crece hacia arriba
        ) {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 6.dp).size(7.dp).background(if (focused) brand else TEXT_SEPIA, CircleShape))
                Spacer(Modifier.size(8.dp))
                Label(
                    app.label,
                    color = if (focused) Palette.Text else TEXT_SEPIA_TEXT,
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
private val TEXT_GAP = 6.dp
/** Sepia oscuro del punto y del texto de las apps sin foco. */
private val TEXT_SEPIA = Color(0xFF7D6650)
private val TEXT_SEPIA_TEXT = Color(0xFFA08A74)
