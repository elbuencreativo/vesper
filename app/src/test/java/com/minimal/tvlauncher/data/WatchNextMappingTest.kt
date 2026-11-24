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
    fun mapRow_progressIsNull_whenDurationUnknown_andClampedToOne() {
        assertNull(mapRow("p", "t", null, 5_000, 0, null)!!.progress)
        assertEquals(1f, mapRow("p", "t", null, 9_000, 3_000, null)!!.progress!!, 0.0001f)
    }
}
