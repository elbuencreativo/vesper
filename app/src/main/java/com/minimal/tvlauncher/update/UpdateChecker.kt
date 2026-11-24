package com.minimal.tvlauncher.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Consulta update.json, descarga el APK verificando su checksum y lo entrega al instalador del sistema. */
class UpdateChecker(private val context: Context, private val manifestUrl: String) {

    val isConfigured: Boolean get() = manifestUrl.isNotBlank()

    suspend fun check(currentCode: Int): UpdateStatus = withContext(Dispatchers.IO) {
        runCatching { decideUpdate(currentCode, parseUpdateManifest(fetchText(manifestUrl))) }
            .getOrDefault(UpdateStatus.Failed)
    }

    /** Devuelve el APK descargado y verificado, o null si falló la red o el checksum no coincide. */
    suspend fun download(info: UpdateInfo): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val bytes = open(info.apkUrl).use { it.readBytes() }
            if (bytes.size > MAX_APK_BYTES || !checksumMatches(info.sha256, sha256Hex(bytes))) return@runCatching null
            File(dir, "update.apk").also { it.writeBytes(bytes) }
        }.getOrNull()
    }

    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** Abre la pantalla del sistema donde se permite instalar apps desde TV Launcher. */
    fun openInstallPermission(): Boolean = try {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    fun install(apk: File): Boolean = try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }

    private fun fetchText(url: String): String = open(url).use { it.readBytes().toString(Charsets.UTF_8) }

    private fun open(url: String) = (URL(url).openConnection() as HttpURLConnection).run {
        connectTimeout = TIMEOUT_MS
        readTimeout = TIMEOUT_MS
        if (responseCode != HttpURLConnection.HTTP_OK) throw java.io.IOException("HTTP $responseCode")
        inputStream
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
        const val MAX_APK_BYTES = 150 * 1024 * 1024
    }
}
