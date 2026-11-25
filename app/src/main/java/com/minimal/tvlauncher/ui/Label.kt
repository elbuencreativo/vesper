package com.minimal.tvlauncher.ui

import android.content.Context
import android.util.LruCache
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Texto base del launcher: ligero, sin tema extra. */
@Composable
fun Label(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Palette.Text,
    size: TextUnit = 12.sp,
    weight: FontWeight = FontWeight.Light,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    maxLines: Int = 1,
) {
    BasicText(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(color = color, fontSize = size, fontWeight = weight, letterSpacing = letterSpacing),
    )
}

/** Icono de app a 128 px más el color de su borde (para pintar el marco sin doble contorno). */
class AppIcon(val image: ImageBitmap, val edge: Int?, val brand: Int? = null)

/** Iconos de apps a 128 px, cacheados en memoria. */
class IconCache(private val context: Context) {
    private val cache = LruCache<String, AppIcon>(64)

    suspend fun getAppIcon(packageName: String): AppIcon? {
        cache.get(packageName)?.let { return it }
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = context.packageManager.getApplicationIcon(packageName).toBitmap(128, 128)
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                AppIcon(bitmap.asImageBitmap(), edgeColor(pixels, bitmap.width, bitmap.height), brandColor(pixels, bitmap.width, bitmap.height))
            }.getOrNull()
        }
        if (loaded != null) cache.put(packageName, loaded)
        return loaded
    }

    suspend fun get(packageName: String): ImageBitmap? = getAppIcon(packageName)?.image
}

val LocalIconCache = compositionLocalOf<IconCache> { error("IconCache no provisto") }
