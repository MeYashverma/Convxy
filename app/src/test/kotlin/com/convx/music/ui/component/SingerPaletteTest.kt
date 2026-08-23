/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.component

import com.convx.music.lyrics.LyricsEntry
import com.convx.music.lyrics.SingerInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the multi-singer color palette and the singer name resolver
 * (registry names, artist inference, shared vocals).
 */
class SingerPaletteTest {

    private fun entry(agent: String?, background: Boolean = false) =
        LyricsEntry(time = 0L, text = "x", agent = agent, isBackground = background)

    @Test
    fun `colors assigned by first appearance and only for lead voices`() {
        val entries = listOf(entry("v1"), entry("v2"), entry("v1"), entry("v1000"), entry(null))

        val colors = SingerPalette.assignColors(entries)

        assertEquals(2, colors.size)
        assertEquals(SingerPalette.LEAD_SINGER_COLORS[0], colors["v1"])
        assertEquals(SingerPalette.LEAD_SINGER_COLORS[1], colors["v2"])
        // Group ("both") lines keep the global accent instead of a palette slot
        assertFalse(colors.containsKey("v1000"))
    }

    @Test
    fun `single voice yields no palette`() {
        assertTrue(SingerPalette.assignColors(listOf(entry("v1"), entry("v1"))).isEmpty())
        assertTrue(SingerPalette.assignColors(listOf(entry(null))).isEmpty())
        assertTrue(SingerPalette.assignColors(emptyList()).isEmpty())
    }

    @Test
    fun `background vocals do not consume palette slots`() {
        val entries = listOf(entry("v1"), entry("v2", background = true), entry("v2"))

        val colors = SingerPalette.assignColors(entries)

        assertEquals(SingerPalette.LEAD_SINGER_COLORS[0], colors["v1"])
        assertEquals(SingerPalette.LEAD_SINGER_COLORS[1], colors["v2"])
    }

    @Test
    fun `palette cycles beyond eight singers`() {
        val entries = (1..10).map { entry("v$it") }

        val colors = SingerPalette.assignColors(entries)

        assertEquals(10, colors.size)
        assertEquals(SingerPalette.LEAD_SINGER_COLORS[0], colors["v1"])
        assertEquals(SingerPalette.LEAD_SINGER_COLORS[8 % 8], colors["v9"])
        assertEquals(SingerPalette.LEAD_SINGER_COLORS[9 % 8], colors["v10"])
        // The palette cycles, so only 8 distinct colors exist for 10 singers
        assertEquals(SingerPalette.LEAD_SINGER_COLORS.size, colors.values.toSet().size)
    }

    @Test
    fun `registry names win over inference`() {
        val singers = mapOf(
            "v1" to SingerInfo("v1", "Ryan Gosling"),
            "v1000" to SingerInfo("v1000", null, isGroup = true),
        )

        val display = resolveSingerDisplay("v1", singers, listOf("Someone Else"))
        assertEquals("Ryan Gosling", display.name)
        assertFalse(display.isGroup)

        val group = resolveSingerDisplay("v1000", singers, emptyList())
        assertTrue(group.isGroup)
        assertNull(group.name)
    }

    @Test
    fun `names inferred from track artists when registry is missing`() {
        val artists = listOf("Ariana Grande", "Mac Miller")

        assertEquals("Ariana Grande", resolveSingerDisplay("v1", emptyMap(), artists).name)
        assertEquals("Mac Miller", resolveSingerDisplay("v2", emptyMap(), artists).name)
        assertNull(resolveSingerDisplay("v3", emptyMap(), artists).name)
        assertNull(resolveSingerDisplay("v2", emptyMap(), emptyList()).name)
    }

    @Test
    fun `non voice ids are not inferred from artists`() {
        assertNull(resolveSingerDisplay("main", emptyMap(), listOf("Ariana Grande")).name)
    }

    @Test
    fun `named group agent keeps its name`() {
        val singers = mapOf("v1000" to SingerInfo("v1000", "Whole Choir", isGroup = true))
        val display = resolveSingerDisplay("v1000", singers, emptyList())
        assertEquals("Whole Choir", display.name)
        assertTrue(display.isGroup)
    }
}
