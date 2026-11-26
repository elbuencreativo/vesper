package com.minimal.tvlauncher.data

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Lo que muestra la tarjeta de PlayStation. */
data class PsnInfo(
    val onlineId: String,
    val avatarUrl: String?,
    val game: String?,
    val gameImageUrl: String?,
    /** Tiempo jugado de ese juego, ya formateado ("228 h", "45 min"). */
    val played: String?,
)

/** "PT228H56M33S" → "228 h"; menos de una hora → "45 min". Formato roto → null. */
fun formatPlayed(iso: String?): String? {
    val d = runCatching { Duration.parse(iso ?: return null) }.getOrNull() ?: return null
    val hours = d.toHours()
    return if (hours >= 1) "$hours h" else "${d.toMinutes()} min"
}

/** El título con la fecha de último juego más reciente (la lista no garantiza el orden). */
fun pickLastPlayed(titles: JSONArray): JSONObject? {
    var best: JSONObject? = null
    var bestAt = Instant.MIN
    for (i in 0 until titles.length()) {
        val t = titles.optJSONObject(i) ?: continue
        val at = runCatching { Instant.parse(t.optString("lastPlayedDateTime")) }.getOrNull() ?: continue
        if (at > bestAt) { best = t; bestAt = at }
    }
    return best
}

/** El código de autorización viaja en la cabecera Location de la redirección: "...redirect/?code=v3.xxxx&cid=...". */
fun codeFromLocation(location: String?): String? =
    location?.substringAfter("code=", "")?.substringBefore('&')?.takeIf { it.isNotEmpty() }

/**
 * Última partida de PlayStation (API no oficial, la misma que usa la librería psn-api).
 *
 * El NPSSO (tu sesión de PlayStation) NUNCA va en el APK: se envía por ADB a la carpeta privada de la app
 * (`/sdcard/Android/data/<app>/files/npsso.txt`). La app lo cambia por un token de acceso y uno de renovación, guarda
 * solo estos en su almacenamiento interno y borra el archivo. Sin sesión válida, [load] devuelve null y la tarjeta no se muestra.
 */
class PsnRepository(private val context: Context) {
    private val tokensFile = File(context.filesDir, "psn_tokens.json")
    private var cached: PsnInfo? = null
    private var cachedAt = 0L

    private val npssoFile: File? get() = context.getExternalFilesDir(null)?.let { File(it, "npsso.txt") }

    suspend fun load(): PsnInfo? {
        val now = System.currentTimeMillis()
        if (cached != null && now - cachedAt < REFRESH_MS && npssoFile?.exists() != true) return cached
        val info = withContext(Dispatchers.IO) { runCatching { fetch() }.getOrNull() }
        if (info != null) { cached = info; cachedAt = now }
        return info ?: cached
    }

    private fun fetch(): PsnInfo? {
        val token = accessToken() ?: return null
        val profile = JSONObject(get("$LEGACY/me/profile2?fields=onlineId,accountId,avatarUrls", token)).optJSONObject("profile") ?: return null
        val onlineId = profile.optString("onlineId").ifEmpty { return null }
        val accountId = profile.optString("accountId")
        val avatar = profile.optJSONArray("avatarUrls")?.let { a ->
            (0 until a.length()).mapNotNull { a.optJSONObject(it) }.lastOrNull()?.optString("avatarUrl")
        }?.ifEmpty { null }?.replaceFirst("http://", "https://") // Sony lo entrega en http y Android bloquea el tráfico sin cifrar
        var game: JSONObject? = null
        if (accountId.isNotEmpty()) {
            val titles = JSONObject(get("$GAMES/$accountId/titles?limit=200&offset=0&categories=ps4_game,ps5_native_game", token)).optJSONArray("titles")
            game = titles?.let { pickLastPlayed(it) }
        }
        return PsnInfo(
            onlineId, avatar, game?.optString("name")?.ifEmpty { null },
            game?.optString("imageUrl")?.ifEmpty { null },
            formatPlayed(game?.optString("playDuration")),
        )
    }

    // --- Sesión ---

    private fun accessToken(): String? {
        val now = System.currentTimeMillis()
        val saved = runCatching { JSONObject(tokensFile.readText()) }.getOrNull()
        val npsso = npssoFile?.takeIf { it.exists() }?.readText()?.trim().orEmpty()
        // Un NPSSO recién enviado manda sobre lo guardado.
        if (npsso.isEmpty() && saved != null && saved.optLong("accessExp") > now + 60_000) return saved.optString("access")
        var fresh: JSONObject? = null
        if (npsso.isNotEmpty()) {
            fresh = runCatching { tokensFromNpsso(npsso) }.getOrNull()
            npssoFile?.delete()
        } else if (saved != null && saved.optLong("refreshExp") > now + 60_000) {
            fresh = runCatching { tokensFromRefresh(saved.optString("refresh")) }.getOrNull()
        }
        if (fresh == null) return null
        val stored = JSONObject()
            .put("access", fresh.getString("access_token"))
            .put("accessExp", now + fresh.optLong("expires_in", 3600) * 1000)
            .put("refresh", fresh.optString("refresh_token"))
            .put("refreshExp", now + fresh.optLong("refresh_token_expires_in", 0) * 1000)
        tokensFile.writeText(stored.toString())
        return stored.getString("access")
    }

    private fun tokensFromNpsso(npsso: String): JSONObject? {
        val url = "$AUTH/authorize?access_type=offline&client_id=$CLIENT_ID&response_type=code" +
            "&scope=${URLEncoder.encode(SCOPE, "UTF-8")}&redirect_uri=${URLEncoder.encode(REDIRECT, "UTF-8")}"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = TIMEOUT_MS; readTimeout = TIMEOUT_MS
            setRequestProperty("Cookie", "npsso=$npsso")
        }
        val code = try { codeFromLocation(conn.getHeaderField("Location")) } finally { conn.disconnect() } ?: return null
        return tokenRequest("code" to code, "redirect_uri" to REDIRECT, "grant_type" to "authorization_code", "token_format" to "jwt")
    }

    private fun tokensFromRefresh(refresh: String): JSONObject? =
        tokenRequest("refresh_token" to refresh, "grant_type" to "refresh_token", "token_format" to "jwt", "scope" to SCOPE)

    private fun tokenRequest(vararg form: Pair<String, String>): JSONObject? {
        val body = form.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
        val conn = (URL("$AUTH/token").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true
            connectTimeout = TIMEOUT_MS; readTimeout = TIMEOUT_MS
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Authorization", "Basic $BASIC")
        }
        return try {
            conn.outputStream.use { it.write(body.toByteArray()) }
            if (conn.responseCode != 200) null else JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally { conn.disconnect() }
    }

    private fun get(url: String, token: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS; readTimeout = TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer $token")
        }
        return try {
            check(conn.responseCode == 200) { "PSN ${conn.responseCode}" }
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally { conn.disconnect() }
    }

    private companion object {
        const val AUTH = "https://ca.account.sony.com/api/authz/v3/oauth"
        const val LEGACY = "https://us-prof.np.community.playstation.net/userProfile/v1/users"
        const val GAMES = "https://m.np.playstation.com/api/gamelist/v2/users"
        const val CLIENT_ID = "09515159-7237-4370-9b40-3806e67c0891"
        const val BASIC = "MDk1MTUxNTktNzIzNy00MzcwLTliNDAtMzgwNmU2N2MwODkxOnVjUGprYTV0bnRCMktxc1A="
        const val SCOPE = "psn:mobile.v2.core psn:clientapp"
        const val REDIRECT = "com.scee.psxandroid.scecompcall://redirect"
        const val TIMEOUT_MS = 10_000
        const val REFRESH_MS = 10L * 60 * 1000
    }
}

val LocalPsnRepository = compositionLocalOf<PsnRepository?> { null }
