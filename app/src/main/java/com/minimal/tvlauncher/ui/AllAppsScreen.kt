package com.minimal.tvlauncher.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.tvlauncher.data.AppEntry
import com.minimal.tvlauncher.data.moveItem

private const val COLUMNS = 8
private val Gray = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * Todas las apps en cuadrícula. "Editar" activa el modo de edición: OK sobre un app abre
 * Mover / Ocultar-Mostrar / Desinstalar. Al mover, el D-pad desplaza el app y OK lo suelta.
 */
@Composable
fun AllAppsScreen(
    apps: List<AppEntry>,
    hidden: Set<String>,
    onLaunch: (AppEntry) -> Unit,
    onSaveOrder: (List<String>) -> Unit,
    onToggleHidden: (String) -> Unit,
    onUninstall: (String) -> Unit,
    onBack: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var order by remember(apps) { mutableStateOf(apps) }
    var menuFor by remember { mutableStateOf<AppEntry?>(null) }
    var movingPkg by remember { mutableStateOf<String?>(null) }
    val gridState = rememberLazyGridState()
    val editFocus = remember { FocusRequester() }

    BackHandler {
        when {
            movingPkg != null -> { order = apps; movingPkg = null }
            menuFor != null -> menuFor = null
            editing -> editing = false
            else -> onBack()
        }
    }

    LaunchedEffect(Unit) { runCatching { editFocus.requestFocus() } }
    val movingIndex = order.indexOfFirst { it.packageName == movingPkg }
    LaunchedEffect(movingIndex) { if (movingIndex >= 0) gridState.animateScrollToItem(movingIndex) }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Palette.BackgroundTop, Palette.Background)))
            .onPreviewKeyEvent { e ->
                val k = e.nativeKeyEvent
                if (movingPkg == null) return@onPreviewKeyEvent false
                if (k.keyCode == AndroidKeyEvent.KEYCODE_BACK) return@onPreviewKeyEvent false
                if (e.type == KeyEventType.KeyDown) {
                    val delta = when (k.keyCode) {
                        AndroidKeyEvent.KEYCODE_DPAD_LEFT -> -1
                        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> 1
                        AndroidKeyEvent.KEYCODE_DPAD_UP -> -COLUMNS
                        AndroidKeyEvent.KEYCODE_DPAD_DOWN -> COLUMNS
                        else -> 0
                    }
                    if (delta != 0) order = moveItem(order, movingIndex, movingIndex + delta)
                } else if (e.type == KeyEventType.KeyUp &&
                    k.keyCode in setOf(AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER)
                ) {
                    onSaveOrder(order.map { it.packageName })
                    movingPkg = null
                }
                true
            },
    ) {
        Column(Modifier.fillMaxSize().focusProperties { canFocus = menuFor == null }) {
            Row(
                Modifier.fillMaxWidth().padding(start = 48.dp, end = 48.dp, top = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Label(
                    if (movingPkg != null) "MOVER · usa las flechas y pulsa OK para soltar"
                    else if (editing) "EDITAR APPS" else "TODAS LAS APPS",
                    color = Palette.TextMuted, size = 10.sp, letterSpacing = 2.sp,
                )
                FocusTile(
                    RoundedCornerShape(16.dp),
                    Modifier.focusRequester(editFocus),
                    onClick = { editing = !editing; movingPkg = null },
                ) { focused ->
                    Box(
                        Modifier.border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                    ) {
                        Label(if (editing) "Listo" else "Editar", color = if (focused) Palette.Text else Palette.TextMuted, size = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(COLUMNS),
                state = gridState,
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(order, key = { it.packageName }) { app ->
                    GridApp(
                        app = app,
                        dimmed = app.packageName in hidden,
                        grabbed = app.packageName == movingPkg,
                        onClick = { if (editing) menuFor = app else onLaunch(app) },
                    )
                }
            }
        }

        menuFor?.let { target ->
            ActionMenu(
                title = target.label,
                isHidden = target.packageName in hidden,
                onMove = { movingPkg = target.packageName; menuFor = null },
                onToggle = { onToggleHidden(target.packageName); menuFor = null },
                onUninstall = { onUninstall(target.packageName); menuFor = null },
                onCancel = { menuFor = null },
            )
        }
    }
}

@Composable
private fun GridApp(app: AppEntry, dimmed: Boolean, grabbed: Boolean, onClick: () -> Unit) {
    val icons = LocalIconCache.current
    val icon by produceState<ImageBitmap?>(null, app.packageName) { value = icons.get(app.packageName) }
    FocusTile(RoundedCornerShape(12.dp), Modifier.fillMaxWidth().height(86.dp), onClick = onClick) { focused ->
        val lit = focused || grabbed
        Column(
            Modifier.fillMaxSize().background(if (grabbed) Color(0xFF26262C) else Palette.Surface)
                .then(if (grabbed) Modifier.border(2.dp, Palette.Focus, RoundedCornerShape(12.dp)) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            icon?.let {
                Image(
                    it, app.label, Modifier.size(34.dp),
                    alpha = if (dimmed) 0.3f else if (lit) 1f else 0.75f,
                    colorFilter = if (lit && !dimmed) null else Gray,
                )
            }
            Spacer(Modifier.height(6.dp))
            Label(
                app.label, Modifier.padding(horizontal = 6.dp),
                if (dimmed) Palette.TextMuted.copy(alpha = 0.5f) else if (lit) Palette.Text else Palette.TextMuted,
                10.sp,
            )
        }
    }
}

@Composable
private fun ActionMenu(
    title: String,
    isHidden: Boolean,
    onMove: () -> Unit,
    onToggle: () -> Unit,
    onUninstall: () -> Unit,
    onCancel: () -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(260.dp).background(Palette.Surface, RoundedCornerShape(16.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Label(title, color = Palette.Text, size = 14.sp)
            Spacer(Modifier.height(6.dp))
            MenuItem("Mover", onMove, Modifier.focusRequester(first))
            MenuItem(if (isHidden) "Mostrar" else "Ocultar", onToggle)
            MenuItem("Desinstalar", onUninstall)
            MenuItem("Cancelar", onCancel)
        }
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusTile(RoundedCornerShape(10.dp), modifier.fillMaxWidth(), onClick = onClick) { focused ->
        Box(
            Modifier.fillMaxWidth().background(if (focused) Color(0xFF2A2A31) else Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Label(text, color = if (focused) Palette.Text else Palette.TextMuted, size = 12.sp)
        }
    }
}
