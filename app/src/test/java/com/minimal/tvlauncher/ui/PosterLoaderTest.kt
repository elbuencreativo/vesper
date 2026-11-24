package com.minimal.tvlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PosterLoaderTest {

    @Test
    fun sampleSizeFor_picksTheLargestPowerOfTwoThatStaysAtOrAboveTheTarget() {
        assertEquals(2, sampleSizeFor(width = 1920, height = 1080, targetWidth = 640))
        assertEquals(1, sampleSizeFor(width = 640, height = 360, targetWidth = 640))
        assertEquals(8, sampleSizeFor(width = 4000, height = 2000, targetWidth = 500))
        assertEquals(1, sampleSizeFor(width = 300, height = 200, targetWidth = 640))
    }

    @Test
    fun sampleSizeFor_survivesBrokenDimensions() {
        assertEquals(1, sampleSizeFor(width = 0, height = 0, targetWidth = 640))
        assertEquals(1, sampleSizeFor(width = -5, height = 100, targetWidth = 640))
        assertEquals(1, sampleSizeFor(width = 1000, height = 1000, targetWidth = 0))
    }
}
