package com.minimal.tvlauncher.idle

import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtTest {
    private fun works(n: Int) = (1..n).map { ArtWork("art/a$it.jpg", "T$it", "A$it", "18$it") }

    @Test
    fun parseArtJson_readsValidEntries_skipsIncompleteOnes_andSurvivesGarbage() {
        val json = """[
            {"file":"a.jpg","title":"Uno","artist":"Pintor","year":"1872"},
            {"file":"b.jpg","title":"Sin artista"},
            {"title":"Sin archivo","artist":"X","year":"1900"}
        ]"""
        assertEquals(listOf(ArtWork("art/a.jpg", "Uno", "Pintor", "1872")), parseArtJson(json))
        assertEquals(emptyList<ArtWork>(), parseArtJson("esto no es json"))
        assertEquals(emptyList<ArtWork>(), parseArtJson(""))
    }

    @Test
    fun rotation_showsEveryWorkBeforeRepeating_andNeverRepeatsBackToBack() {
        val rotation = ArtRotation(works(5), Random(7))
        val firstRound = (1..5).map { rotation.next()!!.title }
        assertEquals(5, firstRound.toSet().size)

        var previous = firstRound.last()
        repeat(40) {
            val current = rotation.next()!!.title
            assertNotEquals(previous, current)
            previous = current
        }
    }

    @Test
    fun rotation_returnsNullWhenThereIsNoArt_andRepeatsTheOnlyWork() {
        assertNull(ArtRotation(emptyList(), Random(1)).next())
        val single = ArtRotation(works(1), Random(1))
        assertEquals("T1", single.next()!!.title)
        assertEquals("T1", single.next()!!.title)
    }
}
