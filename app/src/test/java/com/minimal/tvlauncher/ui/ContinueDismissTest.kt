package com.minimal.tvlauncher.ui

import com.minimal.tvlauncher.data.WatchNextItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ContinueDismissTest {

    private fun card(title: String, pkg: String = "com.netflix.ninja", engagedAt: Long = 1_000) =
        ContinueCard(title, null, null, 0.4f, null, pkg, engagedAt)

    @Test
    fun dismissKey_ignoresCaseAndSurroundingSpaces_butNotThePackage() {
        assertEquals(dismissKey("com.netflix.ninja", "MobLand"), dismissKey("com.netflix.ninja", "  mobland "))
        assertNotEquals(dismissKey("com.netflix.ninja", "MobLand"), dismissKey("org.jellyfin", "MobLand"))
    }

    @Test
    fun filterDismissed_hidesDismissedCards_untilTheyAreEngagedWithAgain() {
        val moblandKey = dismissKey("com.netflix.ninja", "MobLand")
        val cards = listOf(card("MobLand", engagedAt = 1_000), card("Palomas Negras", engagedAt = 2_000))

        // Descartado después de verlo: se oculta.
        assertEquals(listOf("Palomas Negras"), filterDismissed(cards, mapOf(moblandKey to 1_500)).map { it.title })
        // Mismo instante: sigue oculto.
        assertEquals(listOf("Palomas Negras"), filterDismissed(cards, mapOf(moblandKey to 1_000)).map { it.title })
        // Lo volviste a ver después de descartarlo: reaparece.
        val watchedAgain = listOf(card("MobLand", engagedAt = 9_000), card("Palomas Negras", engagedAt = 2_000))
        assertEquals(listOf("MobLand", "Palomas Negras"), filterDismissed(watchedAgain, mapOf(moblandKey to 1_500)).map { it.title })
    }

    @Test
    fun filterDismissed_leavesFallbackCardsAndUnrelatedCardsAlone() {
        val fallback = ContinueCard("Netflix", null, null, null, null, "com.netflix.ninja", FALLBACK_ENGAGEMENT)
        val cards = listOf(fallback, card("Otra"))
        val dismissedEverything = mapOf(dismissKey("com.netflix.ninja", "Netflix") to Long.MAX_VALUE)
        assertEquals(cards, filterDismissed(cards, dismissedEverything))
        assertEquals(cards, filterDismissed(cards, emptyMap()))
    }

    @Test
    fun buildContinueCards_filtersDismissedBeforeCapping_andDoesNotFallBackToRecentsWhenAllAreDismissed() {
        val items = (1..4).map { WatchNextItem("com.netflix.ninja", "Serie $it", null, 0.1f, null, lastEngagementMs = 1_000) }
        val dismissed = mapOf(dismissKey("com.netflix.ninja", "Serie 1") to 5_000L)
        val cards = buildContinueCards(items, emptyList(), cap = 3, dismissed = dismissed)
        assertEquals(listOf("Serie 2", "Serie 3", "Serie 4"), cards.map { it.title }) // entra la siguiente

        val everything = items.associate { dismissKey("com.netflix.ninja", it.title) to 5_000L }
        val recents = listOf(com.minimal.tvlauncher.data.AppEntry("com.netflix.ninja", "Netflix", android.content.Intent("x")))
        assertEquals(emptyList<ContinueCard>(), buildContinueCards(items, recents, dismissed = everything))
    }

    @Test
    fun buildContinueCards_carriesTheLastEngagementTime_andMarksTheFallback() {
        val item = WatchNextItem("com.netflix.ninja", "MobLand", null, 0.5f, null, lastEngagementMs = 7_000)
        assertEquals(7_000L, buildContinueCards(listOf(item), emptyList()).first().engagedAt)
    }
}
