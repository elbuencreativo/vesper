package com.minimal.tvlauncher

import com.minimal.tvlauncher.data.codeFromLocation
import com.minimal.tvlauncher.data.formatPlayed
import com.minimal.tvlauncher.data.pickLastPlayed
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PsnTest {
    @Test fun playedFormatsHoursAndMinutes() {
        assertEquals("228 h", formatPlayed("PT228H56M33S"))
        assertEquals("45 min", formatPlayed("PT45M10S"))
        assertNull(formatPlayed("basura"))
        assertNull(formatPlayed(null))
    }

    @Test fun picksMostRecentlyPlayed() {
        val titles = JSONArray(
            """[{"name":"A","lastPlayedDateTime":"2024-08-03T19:28:27.12Z"},
                {"name":"B","lastPlayedDateTime":"2025-01-02T10:00:00Z"},
                {"name":"C","lastPlayedDateTime":"2023-01-01T00:00:00Z"}]""",
        )
        assertEquals("B", pickLastPlayed(titles)?.getString("name"))
        assertNull(pickLastPlayed(JSONArray("[]")))
    }

    @Test fun codeComesFromRedirectLocation() {
        assertEquals("v3.abc", codeFromLocation("com.scee.psxandroid.scecompcall://redirect/?code=v3.abc&cid=1"))
        assertNull(codeFromLocation("https://x/login"))
        assertNull(codeFromLocation(null))
    }
}

class RemoteKeysTest {
    private val k = com.minimal.tvlauncher.actions.RemoteKeys

    @Test fun idsAndStorageRoundTrip() {
        assertEquals("k5", k.keyId(5, 99))
        assertEquals("s99", k.keyId(0, 99))
        val m = mapOf("k5" to "a.b", "s99" to "c.d")
        assertEquals(m, k.parse(k.serialize(m)))
        assertEquals(emptyMap<String, String>(), k.parse(null))
    }

    @Test fun navigationKeysAreNeverAssignable() {
        assertEquals(false, k.isAssignable(android.view.KeyEvent.KEYCODE_DPAD_CENTER))
        assertEquals(false, k.isAssignable(android.view.KeyEvent.KEYCODE_BACK))
        assertEquals(true, k.isAssignable(android.view.KeyEvent.KEYCODE_PROG_RED))
    }
}
