package com.minimal.tvlauncher.data

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderLogicTest {

    private fun app(pkg: String, label: String = pkg) = AppEntry(pkg, label, Intent(pkg))

    private val installed = listOf(
        app("com.zeta", "Zeta"),
        app("com.netflix.ninja", "Netflix"),
        app("com.alpha", "Alpha"),
        app("com.google.android.youtube.tv", "YouTube"),
    )

    private val defaultFirst = listOf("com.netflix.ninja", "com.google.android.youtube.tv")

    @Test
    fun arrange_withoutSavedOrder_putsDefaultFirstThenAlphabetical() {
        val r = OrderLogic.arrange(installed, emptyList(), emptySet(), defaultFirst)
        assertEquals(
            listOf("com.netflix.ninja", "com.google.android.youtube.tv", "com.alpha", "com.zeta"),
            r.all.map { it.packageName },
        )
    }

    @Test
    fun arrange_appendsNewlyInstalledAppsAtTheEnd() {
        val saved = listOf("com.zeta", "com.netflix.ninja")
        val r = OrderLogic.arrange(installed, saved, emptySet(), defaultFirst)
        assertEquals(
            listOf("com.zeta", "com.netflix.ninja", "com.alpha", "com.google.android.youtube.tv"),
            r.all.map { it.packageName },
        )
    }

    @Test
    fun arrange_ignoresSavedPackagesThatAreNoLongerInstalled() {
        val saved = listOf("com.gone", "com.zeta", "com.also.gone", "com.alpha")
        val r = OrderLogic.arrange(installed, saved, emptySet(), defaultFirst)
        assertEquals("com.zeta", r.all[0].packageName)
        assertEquals("com.alpha", r.all[1].packageName)
        assertEquals(4, r.all.size)
    }

    @Test
    fun arrange_hiddenAreOutOfRowButStillInAllAndInHiddenEntries() {
        val r = OrderLogic.arrange(installed, emptyList(), setOf("com.netflix.ninja"), defaultFirst)
        assertEquals(false, r.row.any { it.packageName == "com.netflix.ninja" })
        assertEquals(true, r.all.any { it.packageName == "com.netflix.ninja" })
        assertEquals(listOf("com.netflix.ninja"), r.hiddenEntries.map { it.packageName })
    }

    @Test
    fun arrange_rowIsCappedAtEightVisibleApps() {
        val many = (1..12).map { app("com.app$it", "App%02d".format(it)) }
        val r = OrderLogic.arrange(many, emptyList(), emptySet(), emptyList())
        assertEquals(8, r.row.size)
        assertEquals(12, r.all.size)
    }
}
