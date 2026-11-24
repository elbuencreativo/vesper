package com.minimal.tvlauncher.ui

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteContrastTest {

    private fun channel(c: Float) = if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun luminance(c: Color) = 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = luminance(a).let { la -> luminance(b).let { lb -> maxOf(la, lb) to minOf(la, lb) } }
        return (hi + 0.05) / (lo + 0.05)
    }

    @Test
    fun palette_usesTheTenSwatchesTheUserChose() {
        assertEquals(Color(0xFF04071D), Palette.Background)
        assertEquals(Color(0xFF320A17), Palette.SurfaceHigh)
        assertEquals(Color(0xFFF5BD42), Palette.Accent)
        assertEquals(Color(0xFFD7662A), Palette.AccentGlow)
    }

    @Test
    fun bodyText_isReadableOnEverySurface() {
        listOf(Palette.Background, Palette.Surface, Palette.SurfaceHigh).forEach { surface ->
            assertTrue("Text sobre $surface", contrast(Palette.Text, surface) >= 7.0)
            assertTrue("TextMuted sobre $surface", contrast(Palette.TextMuted, surface) >= 4.5)
        }
    }

    @Test
    fun accent_standsOutOnTheDarkBackground() {
        assertTrue(contrast(Palette.Accent, Palette.Background) >= 7.0)
        assertTrue(contrast(Palette.Accent, Palette.SurfaceHigh) >= 4.5)
    }
}
