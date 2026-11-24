package com.minimal.tvlauncher.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Mayor potencia de dos que mantiene el ancho decodificado en [targetWidth] o más. Dimensiones rotas → 1. */
fun sampleSizeFor(width: Int, height: Int, targetWidth: Int): Int {
    if (width <= 0 || height <= 0 || targetWidth <= 0) return 1
    var sample = 1
    while (width / (sample * 2) >= targetWidth) sample *= 2
    return sample
}

/**
 * Carga pósters de "Seguir viendo" (https o content://) reducidos a ~640 px y cacheados en memoria.
 * Cualquier fallo (red, formato, archivo) devuelve null y la tarjeta queda con su icono.
 */
class PosterLoader(private val context: Context) {
    private val cache = LruCache<String, ImageBitmap>(24)

    suspend fun load(uri: String): ImageBitmap? {
        cache.get(uri)?.let { return it }
        val loaded = withContext(Dispatchers.IO) { runCatching { decode(uri) }.getOrNull() }
        if (loaded != null) cache.put(uri, loaded)
        return loaded
    }

    private fun decode(uri: String): ImageBitmap? {
        val bytes = open(uri)?.use { it.readBytes() } ?: return null
        if (bytes.size > MAX_BYTES) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, TARGET_WIDTH)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
    }

    private fun open(uri: String): InputStream? {
        val parsed = Uri.parse(uri)
        return when (parsed.scheme) {
            "http", "https" -> (URL(uri).openConnection() as HttpURLConnection).run {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                inputStream
            }
            "content", "file", "android.resource" -> context.contentResolver.openInputStream(parsed)
            else -> null
        }
    }

    private companion object {
        const val TARGET_WIDTH = 640
        const val TIMEOUT_MS = 8_000
        const val MAX_BYTES = 6 * 1024 * 1024
    }
}

val LocalPosterLoader = compositionLocalOf<PosterLoader> { error("PosterLoader no provisto") }
