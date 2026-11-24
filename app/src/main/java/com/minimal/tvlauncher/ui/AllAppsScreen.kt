package com.minimal.tvlauncher.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.tvlauncher.data.AppEntry
import com.minimal.tvlauncher.data.ArrangedApps
import com.minimal.tvlauncher.data.OrderLogic
import com.minimal.tvlauncher.data.reorderWithin
import kotlinx.coroutines.delay

private const val COLUMNS = 8

private enum class Section { Favorites, Rest, Hidden }

private sealed interface GridEntry {
    data class Header(val title: String) : GridEntry
    data class AppItem(val app: AppEntry, val section: Section) : GridEntry
}

/**
 * Todas las apps en tres secciones: Favoritos (los de la fila de inicio), Todas las apps y Ocultas.
 * OK abre el app; mantener OK abre sus opciones (mover, favorito, ocultar, desinstalar).
 * Al mover, el D-pad desplaza el app dentro de su sección y OK lo suelta.
 */
@Composable
fun AllAppsScreen(
    apps: ArrangedApps,
    onLaunch: (AppEntry) -> Unit,
    onSaveOrder: (List<String>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleHidden: (String) -> Unit,
    onUninstall: (String) -> Unit,
    onBack: () -> Unit,
) {
    val favorites = remember(apps) { apps.row.mapTo(HashSet()) { it.packageName } }
    val hidden = remember(apps) { apps.hiddenEntries.mapTo(HashSet()) { it.packageName } }
    var order by remember(apps) { mutableStateOf(apps.all) }
    var menuFor by remember { mutableStateOf<AppEntry?>(null) }
    var movingPkg by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val gridState = rememberLazyGridState()
    val firstFocus = remember { FocusRequester() }

    fun sectionOf(app: AppEntry) = when {
        app.packageName in hidden -> Section.Hidden
        app.packageName in favorites -> Section.Favorites
        else -> Section.Rest
    }

    val entries = buildList<GridEntry> {
        val favs = order.filter { sectionOf(it) == Section.Favorites }
        val rest = order.filter { sectionOf(it) == Section.Rest }
        val hid = order.filter { sectionOf(it) == Section.Hidden }
        add(GridEntry.Header("FAVORITOS · ${favs.size}/${OrderLogic.MAX_FAVORITES}  (los de la pantalla de inicio)"))
        favs.forEach { add(GridEntry.AppItem(it, Section.Favorites)) }
        add(GridEntry.Header("TODAS LAS APPS"))
        rest.forEach { add(GridEntry.AppItem(it, Section.Rest)) }
        if (hid.isNotEmpty()) {
            add(GridEntry.Header("OCULTAS"))
            hid.forEach { add(GridEntry.AppItem(it, Section.Hidden)) }
        }
    }
    val firstApp = entries.firstOrNull { it is GridEntry.AppItem } as GridEntry.AppItem?

    BackHandler {
        when {
            movingPkg != null -> { order = apps.all; movingPkg = null }
            menuFor != null -> menuFor = null
            else -> onBack()
        }
    }

    // El foco inicial va al primer app (la lista se compone tras el primer fotograma: se reintenta).
    LaunchedEffect(firstApp?.app?.packageName) {
        repeat(8) {
            if (runCatching { firstFocus.requestFocus() }.isSuccess) return@LaunchedEffect
            delay(60)
        }
    }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(2200)
            notice = null
        }
    }
    val movingEntryIndex = entries.indexOfFirst { it is GridEntry.AppItem && it.app.packageName == movingPkg }
    LaunchedEffect(movingEntryIndex) { if (movingEntryIndex >= 0) gridState.animateScrollToItem(movingEntryIndex) }

    Box(
        Modifier.fillMaxSize().onPreviewKeyEvent { e ->
            val k = e.nativeKeyEvent
            val moving = movingPkg ?: return@onPreviewKeyEvent false
            if (k.keyCode == AndroidKeyEvent.KEYCODE_BACK) return@onPreviewKeyEvent false
            val item = entries.firstOrNull { it is GridEntry.AppItem && it.app.packageName == moving } as GridEntry.AppItem?
                ?: return@onPreviewKeyEvent true
            if (e.type == KeyEventType.KeyDown) {
                val delta = when (k.keyCode) {
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT -> -1
                    AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> 1
                    AndroidKeyEvent.KEYCODE_DPAD_UP -> -COLUMNS
                    AndroidKeyEvent.KEYCODE_DPAD_DOWN -> COLUMNS
                    else -> 0
                }
                if (delta != 0) {
                    val sectionApps = order.filter { sectionOf(it) == item.section }
                    val from = sectionApps.indexOfFirst { it.packageName == moving }
                    order = reorderWithin(order, { sectionOf(it) == item.section }, from, from + delta)
                }
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
            ) {
                Label("APPS", color = Palette.Accent.copy(alpha = 0.85f), size = 10.sp, letterSpacing = 2.sp)
                Spacer(Modifier.width(24.dp))
                Label(
                    notice ?: if (movingPkg != null) "Mover: usa las flechas y pulsa OK para soltar"
                    else "Mantén OK sobre un app para ver sus opciones",
                    color = if (notice != null) Palette.Accent else Palette.TextMuted, size = 11.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(COLUMNS),
                state = gridState,
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    entries,
                    key = { if (it is GridEntry.AppItem) it.app.packageName else (it as GridEntry.Header).title },
                    span = { if (it is GridEntry.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
                ) { entry ->
                    when (entry) {
                        is GridEntry.Header -> Label(
                            entry.title, Modifier.padding(top = 10.dp),
                            Palette.Accent.copy(alpha = 0.85f), 10.sp, letterSpacing = 2.sp,
                        )
                        is GridEntry.AppItem -> GridApp(
                            app = entry.app,
                            dimmed = entry.section == Section.Hidden,
                            grabbed = entry.app.packageName == movingPkg,
                            onClick = { onLaunch(entry.app) },
                            onLongClick = { menuFor = entry.app },
                            modifier = if (entry == firstApp) Modifier.focusRequester(firstFocus) else Modifier,
                        )
                    }
                }
            }
        }

        menuFor?.let { target ->
            val isHidden = target.packageName in hidden
            val isFavorite = target.packageName in favorites
            ActionMenu(
                title = target.label,
                isHidden = isHidden,
                isFavorite = isFavorite,
                onMove = { movingPkg = target.packageName; menuFor = null },
                onToggleFavorite = {
                    menuFor = null
                    if (!isFavorite && favorites.size >= OrderLogic.MAX_FAVORITES) {
                        notice = "Ya tienes ${OrderLogic.MAX_FAVORITES} favoritos: quita uno primero"
                    } else {
                        onToggleFavorite(target.packageName)
                    }
                },
                onToggleHidden = { onToggleHidden(target.packageName); menuFor = null },
                onUninstall = { onUninstall(target.packageName); menuFor = null },
                onCancel = { menuFor = null },
            )
        }
    }
}

@Composable
private fun GridApp(
    app: AppEntry,
    dimmed: Boolean,
    grabbed: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
) {
    val icons = LocalIconCache.current
    val icon by produceState<AppIcon?>(null, app.packageName) { value = icons.getAppIcon(app.packageName) }
    FocusTile(
        RoundedCornerShape(12.dp), modifier.fillMaxWidth().height(86.dp),
        onLongClick = onLongClick, onClick = onClick,
    ) { focused ->
        val lit = focused || grabbed
        Column(
            Modifier.fillMaxSize().background(if (grabbed) Palette.SurfaceHigh else Palette.Surface.copy(alpha = 0.86f))
                .then(if (grabbed) Modifier.border(2.dp, Palette.Focus, RoundedCornerShape(12.dp)) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AppIconBox(icon, lit = lit, dimmed = dimmed, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)))
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
    isFavorite: Boolean,
    onMove: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleHidden: () -> Unit,
    onUninstall: () -> Unit,
    onCancel: () -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        repeat(8) {
            if (runCatching { first.requestFocus() }.isSuccess) return@LaunchedEffect
            delay(60)
        }
    }
    Box(Modifier.fillMaxSize().background(Palette.Navy.copy(alpha = 0.82f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(280.dp).background(Palette.SurfaceHigh, RoundedCornerShape(16.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Label(title, color = Palette.Text, size = 14.sp)
            Spacer(Modifier.height(6.dp))
            MenuItem("Mover", onMove, Modifier.focusRequester(first))
            if (!isHidden) MenuItem(if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos", onToggleFavorite)
            MenuItem(if (isHidden) "Mostrar" else "Ocultar", onToggleHidden)
            MenuItem("Desinstalar", onUninstall)
            MenuItem("Cancelar", onCancel)
        }
    }
}

@Composable
internal fun MenuItem(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusTile(RoundedCornerShape(10.dp), modifier.fillMaxWidth(), onClick = onClick) { focused ->
        Box(
            Modifier.fillMaxWidth().background(if (focused) Palette.Wine700.copy(alpha = 0.7f) else Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Label(text, color = if (focused) Palette.Text else Palette.TextMuted, size = 12.sp)
        }
    }
}
