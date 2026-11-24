package com.minimal.tvlauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class AppCatalog(private val context: Context) {

    /** Apps lanzables en TV (LEANBACK_LAUNCHER), sin el propio launcher. */
    fun installed(): List<AppEntry> {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        // Google TV trae YouTube y otros "deshabilitados hasta usarse"; sin este flag no aparecen.
        @Suppress("DEPRECATION")
        val found = pm.queryIntentActivities(query, PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS)
        return found
            .asSequence()
            .map { it.activityInfo }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .map { info ->
                AppEntry(
                    packageName = info.packageName,
                    label = info.loadLabel(pm).toString(),
                    launchIntent = Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                        .setClassName(info.packageName, info.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            .toList()
    }
}
