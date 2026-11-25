package com.minimal.tvlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WatchNextMappingTest {

    @Test
    fun mapRow_computesProgress_andDropsRowsWithoutTitle() {
        val item = mapRow(
            packageName = "com.netflix.ninja",
            title = "Serie",
            posterUri = "content://poster/1",
            positionMs = 300_000,
            durationMs = 1_200_000,
            intentUri = "intent:#Intent;end",
        )
        assertNotNull(item)
        assertEquals(0.25f, item!!.progress!!, 0.0001f)
        assertEquals("com.netflix.ninja", item.packageName)

        assertNull(mapRow("com.netflix.ninja", null, null, 0, 0, null))
        assertNull(mapRow("com.netflix.ninja", "   ", null, 0, 0, null))
    }

    @Test
    fun mapRow_keepsTheLastEngagementTime() {
        val item = mapRow("p", "t", null, 0, 0, null, lastEngagementMs = 12_345)
        assertEquals(12_345L, item!!.lastEngagementMs)
        assertEquals(0L, mapRow("p", "t", null, 0, 0, null)!!.lastEngagementMs)
    }

    @Test
    fun mapRow_progressIsNull_whenDurationUnknown_andClampedToOne() {
        assertNull(mapRow("p", "t", null, 5_000, 0, null)!!.progress)
        assertEquals(1f, mapRow("p", "t", null, 9_000, 3_000, null)!!.progress!!, 0.0001f)
    }

    @Test
    fun episodeDetail_showsRemainingTimeOrDuration() {
        assertEquals("T2 · E6 · faltan 27 min", episodeDetail("2", "6", 49_000, 1_665_000))
        assertEquals("T2 · E1 · 22 min", episodeDetail("2", "1", 0, 1_307_000))
        assertEquals("1 h 12 min", episodeDetail(null, null, 0, 4_320_000))
        assertEquals("T1 · E13 · casi terminado", episodeDetail("1", "13", 1_414_000, 1_414_999))
        assertNull(episodeDetail(null, null, 0, 0))
    }

    @Test
    fun mapRow_hidesProgressWhenNotStarted() {
        assertNull(mapRow("p", "t", null, 0, 1_000, null)!!.progress)
    }

    @Test
    fun episodeDetail_includesEpisodeName_forSeries() {
        assertEquals("T2 · E2 · Song 2 · faltan 5 min", episodeDetail("2", "2", 2_400_000, 2_700_000, "Song 2"))
        assertEquals("T1 · E4 · faltan 5 min", episodeDetail("1", "4", 2_400_000, 2_700_000, "null"))
    }

    @Test
    fun pickLogoPath_prefersOriginalLanguageThenEnglishAndSkipsSvg() {
        val logos = org.json.JSONArray(
            """[{"file_path":"/a.svg","iso_639_1":"es","vote_average":9},
                {"file_path":"/b.png","iso_639_1":"en","vote_average":8},
                {"file_path":"/c.png","iso_639_1":"es","vote_average":1},
                {"file_path":"/d.png","iso_639_1":null,"vote_average":10}]""",
        )
        assertEquals("/b.png", pickLogoPath(logos)) // original por defecto: inglés
        assertEquals("/c.png", pickLogoPath(logos, "es"))
        assertNull(pickLogoPath(org.json.JSONArray("""[{"file_path":"/x.svg","iso_639_1":"en"}]""")))
    }
}
