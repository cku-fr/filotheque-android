package com.filotheque.app

import com.filotheque.app.data.local.entity.ThreadEntity
import com.filotheque.app.domain.color.ColorUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorUtilsTest {

    @Test
    fun `isLightColor detects white vs black`() {
        assertTrue(ColorUtils.isLightColor("#FFFFFF"))
        assertFalse(ColorUtils.isLightColor("#000000"))
        assertFalse(ColorUtils.isLightColor("#1B2853"))
    }

    @Test
    fun `classifyColorFamily groups primary colors accurately`() {
        assertEquals("Blanc", ColorUtils.classifyColorFamily("#FFFFFF"))
        assertEquals("Noir/Gris", ColorUtils.classifyColorFamily("#000000"))
        assertEquals("Rouge", ColorUtils.classifyColorFamily("#E31D42"))
        assertEquals("Vert", ColorUtils.classifyColorFamily("#47A72F"))
        assertEquals("Bleu", ColorUtils.classifyColorFamily("#30C2EC"))
    }

    @Test
    fun `colorSortKey orders neutrals before chromatic colors`() {
        val whiteKey = ColorUtils.colorSortKey("#FFFFFF")
        val blackKey = ColorUtils.colorSortKey("#000000")
        val redKey = ColorUtils.colorSortKey("#E31D42")
        val blueKey = ColorUtils.colorSortKey("#30C2EC")

        assertTrue(whiteKey < blackKey)
        assertTrue(blackKey < redKey)
        assertTrue(redKey < blueKey)
    }

    @Test
    fun `findClosestDmc picks nearest color`() {
        val candidates = listOf(
            ThreadEntity(id = 1, brandId = "DMC", brandName = "DMC", code = "310", name = "Noir", hexColor = "#000000", colorFamily = "Noir/Gris"),
            ThreadEntity(id = 2, brandId = "DMC", brandName = "DMC", code = "666", name = "Rouge Vif", hexColor = "#E31D42", colorFamily = "Rouge"),
            ThreadEntity(id = 3, brandId = "DMC", brandName = "DMC", code = "996", name = "Bleu", hexColor = "#30C2EC", colorFamily = "Bleu")
        )
        val match = ColorUtils.findClosestDmc("#E02040", candidates)
        assertEquals("666", match?.code)
    }

    @Test
    fun `rgbToLab and deltaE rank perceptually closest threads first`() {
        val whiteLab = ColorUtils.rgbToLab(ColorUtils.Rgb(255, 255, 255))
        val blackLab = ColorUtils.rgbToLab(ColorUtils.Rgb(0, 0, 0))
        assertTrue(whiteLab.l > 99.0)
        assertTrue(blackLab.l < 1.0)

        val candidates = listOf(
            ThreadEntity(id = 1, brandId = "DMC", brandName = "DMC", code = "310", name = "Noir", hexColor = "#000000", colorFamily = "Noir/Gris"),
            ThreadEntity(id = 2, brandId = "DMC", brandName = "DMC", code = "666", name = "Rouge Vif", hexColor = "#E31D42", colorFamily = "Rouge"),
            ThreadEntity(id = 3, brandId = "DMC", brandName = "DMC", code = "321", name = "Rouge Noël", hexColor = "#C72B3B", colorFamily = "Rouge"),
            ThreadEntity(id = 4, brandId = "DMC", brandName = "DMC", code = "996", name = "Bleu", hexColor = "#30C2EC", colorFamily = "Bleu")
        )
        val top2 = ColorUtils.findTopMatches("#DC2040", candidates, limit = 2)
        assertEquals(2, top2.size)
        assertEquals("666", top2[0].thread.code)
        assertEquals("321", top2[1].thread.code)
    }
}
