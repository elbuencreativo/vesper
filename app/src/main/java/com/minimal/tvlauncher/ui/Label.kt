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

/** Iconos de apps a 128 px, cacheados en memoria. */
class IconCache(private val context: Context) {
    private val cache = LruCache<String, ImageBitmap>(64)

    suspend fun get(packageName: String): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName).toBitmap(128, 128).asImageBitmap()
            }.getOrNull()
        }
        if (loaded != null) cache.put(packageName, loaded)
        return loaded
    }
}

val LocalIconCache = compositionLocalOf<IconCache> { error("IconCache no provisto") }
