package com.minimal.tvlauncher.data

import android.content.Context
import android.util.Log
import androidx.tvprovider.media.tv.TvContractCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WatchNextRepository(private val context: Context) {

    /** Lee Watch Next del sistema. Devuelve vacío si el proveedor no deja leer o falla. */
    suspend fun load(limit: Int = 10): List<WatchNextItem> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            TvContractCompat.BaseTvColumns.COLUMN_PACKAGE_NAME,
            TvContractCompat.WatchNextPrograms.COLUMN_TITLE,
            TvContractCompat.WatchNextPrograms.COLUMN_POSTER_ART_URI,
            TvContractCompat.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
            TvContractCompat.WatchNextPrograms.COLUMN_DURATION_MILLIS,
            TvContractCompat.WatchNextPrograms.COLUMN_INTENT_URI,
            TvContractCompat.WatchNextPrograms.COLUMN_THUMBNAIL_URI,
            TvContractCompat.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS,
            TvContractCompat.WatchNextPrograms.COLUMN_SEASON_DISPLAY_NUMBER,
            TvContractCompat.WatchNextPrograms.COLUMN_EPISODE_DISPLAY_NUMBER,
        )
        val sort = "${TvContractCompat.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS} DESC"
        try {
            context.contentResolver.query(
                TvContractCompat.WatchNextPrograms.CONTENT_URI, projection, null, null, sort,
            )?.use { c ->
                buildList {
                    while (c.moveToNext() && size < limit) {
                        mapRow(
                            packageName = c.getString(0).orEmpty(),
                            title = c.getString(1),
                            posterUri = c.getString(2)?.takeIf { it.isNotBlank() } ?: c.getString(6),
                            positionMs = c.getLong(3),
                            durationMs = c.getLong(4),
                            intentUri = c.getString(5),
                            lastEngagementMs = c.getLong(7),
                            season = c.getString(8),
                            episode = c.getString(9),
                        )?.let(::add)
                    }
                }
            } ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Watch Next no disponible: ${e.javaClass.simpleName}: ${e.message}")
            emptyList()
        }
    }

    private companion object {
        const val TAG = "WatchNextSpike"
    }
}
