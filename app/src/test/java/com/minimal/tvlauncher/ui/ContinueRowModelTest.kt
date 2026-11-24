package com.minimal.tvlauncher.ui

import android.content.Intent
import com.minimal.tvlauncher.data.AppEntry
import com.minimal.tvlauncher.data.WatchNextItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinueRowModelTest {

    private fun wn(i: Int, intentUri: String? = "intent:#Intent;end", pkg: String = "pkg$i") =
        WatchNextItem(pkg, "Titulo $i", null, 0.5f, intentUri)

    private fun app(pkg: String, label: String) = AppEntry(pkg, label, Intent(pkg))

    @Test
    fun buildContinueCards_capsWatchNextAtTen() {
        val cards = buildContinueCards((1..12).map { wn(it) }, emptyList(), cap = 10)
        assertEquals(10, cards.size)
        assertEquals("Titulo 1", cards.first().title)
    }

    @Test
    fun buildContinueCards_usesRecentsAsFallback_whenWatchNextIsEmpty() {
        val cards = buildContinueCards(emptyList(), listOf(app("a", "App A"), app("b", "App B")))
        assertEquals(listOf("App A", "App B"), cards.map { it.title })
        assertEquals(listOf("a", "b"), cards.map { it.packageName })
        assertTrue(cards.all { it.progress == null && it.posterUri == null })
    }

    @Test
    fun buildContinueCards_showsEachTitleOnce_keepingTheMostRecentRow() {
        val rows = listOf(
            wn(1, pkg = "org.jellyfin").copy(title = "MobLand"),
            wn(2, pkg = "org.wholphin").copy(title = "MobLand"),
            wn(3, pkg = "com.netflix").copy(title = "mobland "),
            wn(4, pkg = "com.netflix").copy(title = "Palomas Negras"),
        )
        val cards = buildContinueCards(rows, emptyList())
        assertEquals(listOf("MobLand", "Palomas Negras"), cards.map { it.title })
        assertEquals("org.jellyfin", cards.first().packageName)
    }

    @Test
    fun buildContinueCards_isEmpty_whenNothingToShow() {
        assertEquals(emptyList<ContinueCard>(), buildContinueCards(emptyList(), emptyList()))
    }

    @Test
    fun buildContinueCards_keepsWatchNextRowsWithoutIntent_butDropsRowsWithoutPackage() {
        val cards = buildContinueCards(
            listOf(wn(1, intentUri = null), wn(2, pkg = "")),
            emptyList(),
        )
        assertEquals(listOf("Titulo 1"), cards.map { it.title })
    }
}
