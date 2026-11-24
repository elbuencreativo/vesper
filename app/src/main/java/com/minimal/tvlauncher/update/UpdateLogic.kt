package com.minimal.tvlauncher.update

import java.security.MessageDigest
import org.json.JSONException
import org.json.JSONObject

/** Lo que publica el servidor de actualizaciones en update.json. */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String?,
    val notes: String?,
)

sealed interface UpdateStatus {
    data object UpToDate : UpdateStatus
    data class Available(val info: UpdateInfo) : UpdateStatus
    data object Failed : UpdateStatus
}

/** Lee update.json. Devuelve null si falta algo esencial, el JSON está roto o el APK no es http(s). */
fun parseUpdateManifest(text: String): UpdateInfo? = try {
    val o = JSONObject(text)
    val code = o.optInt("versionCode", 0)
    val name = o.optString("versionName")
    val url = o.optString("apkUrl")
    if (code <= 0 || name.isBlank() || !(url.startsWith("http://") || url.startsWith("https://"))) null
    else UpdateInfo(
        versionCode = code,
        versionName = name,
        apkUrl = url,
        sha256 = o.optString("sha256").trim().lowercase().ifEmpty { null },
        notes = o.optString("notes").trim().ifEmpty { null },
    )
} catch (_: JSONException) {
    null
}

/** Solo se ofrece una versión estrictamente más nueva. */
fun decideUpdate(currentCode: Int, remote: UpdateInfo?): UpdateStatus = when {
    remote == null -> UpdateStatus.Failed
    remote.versionCode > currentCode -> UpdateStatus.Available(remote)
    else -> UpdateStatus.UpToDate
}

fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

/** Si el servidor publica un checksum, el APK descargado debe coincidir; si no, no hay nada que comparar. */
fun checksumMatches(expected: String?, actualHex: String): Boolean =
    expected == null || expected.equals(actualHex, ignoreCase = true)
