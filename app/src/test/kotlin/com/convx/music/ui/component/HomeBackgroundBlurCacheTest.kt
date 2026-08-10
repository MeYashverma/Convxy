package com.convx.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The decode step has to land at or above the target width — sampling past it
 * would bake a picture blurrier than the radius asked for, and the error would
 * only ever show up as a soft background nobody can trace back to here.
 */
class HomeBackgroundBlurCacheTest {

    @Test
    fun `sample size never takes the decode below the target`() {
        val target = 1080
        listOf(640, 1080, 1600, 2160, 3000, 4320, 8000).forEach { source ->
            val sample = HomeBackgroundBlurCache.sampleSizeFor(source, target)
            assertTrue(
                "source=$source sampled to ${source / sample}, below target $target",
                source / sample >= target || source < target,
            )
        }
    }

    @Test
    fun `sample size halves at each doubling of the source`() {
        assertEquals(1, HomeBackgroundBlurCache.sampleSizeFor(1080, 1080))
        assertEquals(2, HomeBackgroundBlurCache.sampleSizeFor(2160, 1080))
        assertEquals(4, HomeBackgroundBlurCache.sampleSizeFor(4320, 1080))
    }

    @Test
    fun `undersized sources are decoded whole`() {
        assertEquals(1, HomeBackgroundBlurCache.sampleSizeFor(400, 1080))
    }
}
