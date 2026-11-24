package com.minimal.tvlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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

private val GrayscaleFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

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
    modifier: Modifier = Modifier,
) {
    var focusedLabel by remember { mutableStateOf("") }
    Column(modifier) {
        Label("APPS", Modifier.padding(start = 48.dp), Palette.TextMuted, 9.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppTile(
                    app,
                    onFocus = { focusedLabel = app.label },
                    onLaunch = { onLaunch(app) },
                    onClose = { onClose(app) },
                    modifier = if (app == apps.first()) Modifier.focusRequester(firstFocus) else Modifier,
                )
            }
            item(key = "all-apps") {
                FocusTile(
                    RoundedCornerShape(12.dp), Modifier.size(48.dp),
                    onFocusChange = { if (it) focusedLabel = "Todas las apps" },
                    onClick = onOpenAll,
                ) { focused ->
                    Canvas(Modifier.fillMaxSize().padding(16.dp)) {
                        val c = if (focused) Palette.Text else Palette.TextMuted
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
    val icon by produceState<ImageBitmap?>(null, app.packageName) { value = icons.get(app.packageName) }
    FocusTile(
        RoundedCornerShape(12.dp), modifier.size(48.dp),
        onFocusChange = { if (it) onFocus() },
        onLongClick = onClose,
        onClick = onLaunch,
    ) { focused ->
        Box(Modifier.fillMaxSize().background(Palette.Surface), contentAlignment = Alignment.Center) {
            icon?.let {
                Image(
                    it, app.label, Modifier.size(30.dp),
                    alpha = if (focused) 1f else 0.7f,
                    colorFilter = if (focused) null else GrayscaleFilter,
                )
            }
        }
    }
}
