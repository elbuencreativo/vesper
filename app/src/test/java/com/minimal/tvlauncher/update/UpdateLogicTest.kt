package com.minimal.tvlauncher.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateLogicTest {

    private val manifest = """{"versionCode":3,"versionName":"0.3.0","apkUrl":"http://192.168.1.30:8000/app.apk","sha256":"ABCDEF","notes":"Nuevo"}"""

    @Test
    fun parseUpdateManifest_readsAllFields_andRejectsIncompleteOrBrokenJson() {
        assertEquals(
            UpdateInfo(3, "0.3.0", "http://192.168.1.30:8000/app.apk", "abcdef", "Nuevo"),
            parseUpdateManifest(manifest),
        )
        assertNull(parseUpdateManifest("""{"versionCode":3,"versionName":"0.3.0"}"""))
        assertNull(parseUpdateManifest("""{"versionCode":0,"versionName":"x","apkUrl":"http://a/b.apk"}"""))
        assertNull(parseUpdateManifest("<html>no es json</html>"))
        assertNull(parseUpdateManifest(""))
    }

    @Test
    fun parseUpdateManifest_refusesNonHttpApkUrls() {
        assertNull(parseUpdateManifest("""{"versionCode":3,"versionName":"0.3.0","apkUrl":"file:///sdcard/app.apk"}"""))
    }

    @Test
    fun decideUpdate_offersOnlyStrictlyNewerVersions() {
        val newer = UpdateInfo(3, "0.3.0", "http://a/b.apk", null, null)
        assertEquals(UpdateStatus.Available(newer), decideUpdate(currentCode = 2, remote = newer))
        assertEquals(UpdateStatus.UpToDate, decideUpdate(currentCode = 3, remote = newer))
        assertEquals(UpdateStatus.UpToDate, decideUpdate(currentCode = 9, remote = newer))
        assertEquals(UpdateStatus.Failed, decideUpdate(currentCode = 2, remote = null))
    }

    @Test
    fun checksum_isRequiredToMatchWhenThemanifestGivesOne() {
        val hex = sha256Hex("hola".toByteArray())
        assertEquals("b221d9dbb083a7f33428d7c2a3c3198ae925614d70210e28716ccaa7cd4ddb79", hex)
        assertTrue(checksumMatches("B221D9DBB083A7F33428D7C2A3C3198AE925614D70210E28716CCAA7CD4DDB79", hex))
        assertFalse(checksumMatches("0000", hex))
        assertTrue(checksumMatches(null, hex))
    }
}
