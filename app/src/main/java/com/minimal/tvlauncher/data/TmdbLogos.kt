package com.minimal.tvlauncher.data

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Elige el mejor logo de la lista `logos` de TMDB: solo PNG (los SVG no se decodifican), preferido en el idioma
 * original del programa ([originalLanguage]), luego en inglés, luego sin idioma; dentro de cada idioma, el de mejor voto.
 * Nada se traduce: se muestra el título tal como lo publicó su país de origen.
 */
fun pickLogoPath(logos: JSONArray, originalLanguage: String = "en"): String? {
    var best: JSONObject? = null
    var bestScore = Double.NEGATIVE_INFINITY
    for (i in 0 until logos.length()) {
        val l = logos.optJSONObject(i) ?: continue
        val path = l.optString("file_path")
        if (!path.endsWith(".png", ignoreCase = true)) continue
        val lang = if (l.isNull("iso_639_1")) "" else l.optString("iso_639_1")
        val langScore = when (lang) { originalLanguage -> 300.0; "en" -> 200.0; "" -> 100.0; else -> 0.0 }
        val score = langScore + l.optDouble("vote_average", 0.0)
        if (score > bestScore) { best = l; bestScore = score }
    }
    return best?.optString("file_path")
}

/**
 * Logos de programas desde TMDB (https://www.themoviedb.org). Busca por título, baja el logo y lo guarda
 * en disco (también guarda "no hay logo" una semana para no repetir la búsqueda). Sin clave o sin red devuelve null.
 */
class LogoRepository(context: Context, private val apiKey: String) {
    private val dir = File(context.cacheDir, "logos-orig").apply { mkdirs() }
    private val memory = LruCache<String, ImageBitmap>(32)

    val available: Boolean get() = apiKey.isNotBlank()

    suspend fun get(title: String): ImageBitmap? {
        if (!available) return null
        val name = title.trim().lowercase()
        if (name.isEmpty()) return null
        memory.get(name)?.let { return it }
        val loaded = withContext(Dispatchers.IO) { runCatching { load(name) }.getOrNull() }
        if (loaded != null) memory.put(name, loaded)
        return loaded
    }

    private fun load(name: String): ImageBitmap? {
        val id = MessageDigest.getInstance("SHA-1").digest(name.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)
        val png = File(dir, "$id.png")
        val none = File(dir, "$id.none")
        if (png.exists()) return BitmapFactory.decodeFile(png.path)?.asImageBitmap()
        if (none.exists() && System.currentTimeMillis() - none.lastModified() < NONE_TTL_MS) return null

        val q = URLEncoder.encode(name, "UTF-8")
        val results = JSONObject(fetch("$API/search/multi?api_key=$apiKey&language=en-US&query=$q")).optJSONArray("results")
        var hit: JSONObject? = null
        if (results != null) for (i in 0 until results.length()) {
            val r = results.optJSONObject(i) ?: continue
            if (r.optString("media_type") == "tv" || r.optString("media_type") == "movie") { hit = r; break }
        }
        val original = hit?.optString("original_language").orEmpty().ifEmpty { "en" }
        val path = hit?.let {
            val images = JSONObject(fetch("$API/${it.getString("media_type")}/${it.getInt("id")}/images?api_key=$apiKey&include_image_language=$original,en,null"))
            images.optJSONArray("logos")?.let { l -> pickLogoPath(l, original) }
        }
        if (path == null) { none.writeText(""); return null }
        val bytes = URL("$IMAGES$path").openConnection().run {
            connectTimeout = TIMEOUT_MS; readTimeout = TIMEOUT_MS
            getInputStream().use { it.readBytes() }
        }
        png.writeBytes(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }

    private fun fetch(url: String): String =
        (URL(url).openConnection() as HttpURLConnection).run {
            connectTimeout = TIMEOUT_MS; readTimeout = TIMEOUT_MS
            inputStream.bufferedReader().use { it.readText() }
        }

    private companion object {
        const val API = "https://api.themoviedb.org/3"
        const val IMAGES = "https://image.tmdb.org/t/p/w300"
        const val TIMEOUT_MS = 8_000
        const val NONE_TTL_MS = 7L * 24 * 3600 * 1000
    }
}

val LocalLogoRepository = compositionLocalOf<LogoRepository?> { null }
