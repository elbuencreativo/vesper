package com.minimal.tvlauncher.data

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.minimal.tvlauncher.idle.ArtRotation
import com.minimal.tvlauncher.idle.ArtWork
import com.minimal.tvlauncher.idle.parseArtJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Obras de la National Gallery empaquetadas en assets/art. Si faltan o están rotas, no hay obras (nunca falla). */
class ArtRepository(private val context: Context) {
    private val works: List<ArtWork> by lazy {
        runCatching {
            parseArtJson(context.assets.open("art/art.json").bufferedReader().use { it.readText() })
        }.getOrDefault(emptyList())
    }
    private val rotation by lazy { ArtRotation(works) }

    fun all(): List<ArtWork> = works

    /** Siguiente obra de la rotación, o null si no hay arte. */
    fun next(): ArtWork? = rotation.next()

    suspend fun loadBitmap(work: ArtWork): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching {
            context.assets.open(work.assetPath).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        }.getOrNull()
    }
}
