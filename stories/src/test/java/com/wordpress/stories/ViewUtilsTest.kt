package com.wordpress.stories

import com.wordpress.stories.util.ScreenSize
import com.wordpress.stories.util.isSizeRatio916
import com.wordpress.stories.util.normalizeSizeExportTo916
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ViewUtilsTest {
    @Test
    fun `size ratio of 9x16 is correctly identified`() {
        assertTrue(isSizeRatio916(1080, 1920))
    }

    @Test
    fun `size ratio other than 9x16 is correctly identified`() {
        assertFalse(isSizeRatio916(720, 1440))
    }

    @Test
    fun `conversion - size ratio of 9x16 is unaltered`() {
        val expectedSize = ScreenSize(1080, 1920)
        assertEquals(expectedSize, normalizeSizeExportTo916(1080, 1920))
    }

    @Test
    fun `conversion - size ratio of 9x18 is cropped vertically`() {
        val expectedSize = ScreenSize(1080, 1920)
        assertEquals(expectedSize, normalizeSizeExportTo916(1080, 2160))
    }

    @Test
    fun `conversion - size ratio of 3x5 is cropped horizontally`() {
        val expectedSize = ScreenSize(720, 1280)
        assertEquals(expectedSize, normalizeSizeExportTo916(768, 1280))
    }
}
