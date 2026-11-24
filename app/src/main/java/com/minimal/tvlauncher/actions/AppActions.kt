package com.minimal.tvlauncher.actions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.minimal.tvlauncher.data.AppEntry
import com.minimal.tvlauncher.data.RecentsStore

class AppActions(private val context: Context, private val recents: RecentsStore) {

    /** Abre el app y lo registra en "vistos recientemente". Devuelve false si no se pudo abrir. */
    suspend fun launch(entry: AppEntry): Boolean = try {
        context.startActivity(entry.launchIntent)
        recents.record(entry.packageName)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    /**
     * Pide cerrar un app abriendo su pantalla de información del sistema, donde está "Forzar detención".
     * Desde Android 14 un app normal ya no puede matar procesos de otros apps
     * (killBackgroundProcesses solo afecta al propio), así que el cierre real lo hace el sistema.
     */
    fun close(packageName: String): Boolean {
        if (packageName == context.packageName) return false
        val spec = appInfoSpec(packageName)
        val intent = Intent(spec.action, Uri.parse(spec.uri))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    fun uninstall(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}

/** Acción y URI de la pantalla de información de un app (separado para poder probarlo sin Android). */
data class AppInfoSpec(val action: String, val uri: String)

fun appInfoSpec(packageName: String) =
    AppInfoSpec(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName")
