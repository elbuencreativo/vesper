package com.minimal.tvlauncher.idle

import java.util.Random
import org.json.JSONArray
import org.json.JSONException

data class ArtWork(val assetPath: String, val title: String, val artist: String, val year: String)

/** Lee assets/art/art.json. Entradas incompletas se descartan; un JSON roto da lista vacía. */
fun parseArtJson(text: String): List<ArtWork> = try {
    val array = JSONArray(text)
    buildList {
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val file = o.optString("file")
            val title = o.optString("title")
            val artist = o.optString("artist")
            if (file.isBlank() || title.isBlank() || artist.isBlank()) continue
            add(ArtWork("art/$file", title, artist, o.optString("year")))
        }
    }
} catch (_: JSONException) {
    emptyList()
}

/** Recorre todas las obras en orden aleatorio antes de repetir, sin repetir la misma dos veces seguidas. */
class ArtRotation(private val items: List<ArtWork>, private val random: Random = Random()) {
    private val queue = ArrayDeque<ArtWork>()
    private var last: ArtWork? = null

    fun next(): ArtWork? {
        if (items.isEmpty()) return null
        if (queue.isEmpty()) refill()
        return queue.removeFirst().also { last = it }
    }

    private fun refill() {
        val bag = items.shuffled(random).toMutableList()
        if (bag.size > 1 && bag.first() == last) bag.add(bag.removeAt(0))
        queue.addAll(bag)
    }
}
