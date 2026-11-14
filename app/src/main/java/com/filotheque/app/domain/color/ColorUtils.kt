package com.filotheque.app.domain.color

import com.filotheque.app.data.local.entity.ThreadEntity
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object ColorUtils {

    data class Rgb(val r: Int, val g: Int, val b: Int)
    data class Lab(val l: Double, val a: Double, val b: Double)
    data class ThreadMatch(val thread: ThreadEntity, val deltaE: Double)

    fun parseHex(hex: String): Rgb {
        val clean = hex.removePrefix("#").trim()
        if (clean.length != 6) return Rgb(128, 128, 128)
        return try {
            Rgb(
                r = clean.substring(0, 2).toInt(16),
                g = clean.substring(2, 4).toInt(16),
                b = clean.substring(4, 6).toInt(16)
            )
        } catch (_: NumberFormatException) {
            Rgb(128, 128, 128)
        }
    }

    fun isLightColor(hex: String): Boolean {
        val (r, g, b) = parseHex(hex)
        val luminance = r * 0.299 + g * 0.587 + b * 0.114
        return luminance > 150.0
    }

    fun classifyColorFamily(hex: String): String {
        val (rInt, gInt, bInt) = parseHex(hex)
        val r = rInt / 255.0
        val g = gInt / 255.0
        val b = bInt / 255.0

        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val delta = maxC - minC

        if (maxC > 0.90 && delta < 0.10) return "Blanc"
        if (delta < 0.12) return "Noir/Gris"

        val hue = when {
            delta == 0.0 -> 0.0
            maxC == r -> 60.0 * (((g - b) / delta) % 6.0)
            maxC == g -> 60.0 * (((b - r) / delta) + 2.0)
            else -> 60.0 * (((r - g) / delta) + 4.0)
        }.let { if (it < 0) it + 360.0 else it }

        return when {
            hue < 18 || hue >= 345 -> "Rouge"
            hue < 42 -> if (maxC < 0.55) "Marron" else "Orange"
            hue < 68 -> "Jaune"
            hue < 165 -> "Vert"
            hue < 255 -> "Bleu"
            hue < 305 -> "Violet"
            else -> "Rose"
        }
    }

    fun rgbToLab(rgb: Rgb): Lab {
        fun linearize(c: Int): Double {
            val v = c / 255.0
            return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        val r = linearize(rgb.r)
        val g = linearize(rgb.g)
        val b = linearize(rgb.b)

        // sRGB -> XYZ (D65)
        val x = (r * 0.4124564 + g * 0.3575761 + b * 0.1804375) / 0.95047
        val y = (r * 0.2126729 + g * 0.7151522 + b * 0.0721750) / 1.00000
        val z = (r * 0.0193339 + g * 0.1191920 + b * 0.9503041) / 1.08883

        fun f(t: Double): Double = if (t > 0.008856) t.pow(1.0 / 3.0) else (7.787 * t) + (16.0 / 116.0)
        val fx = f(x)
        val fy = f(y)
        val fz = f(z)

        return Lab(
            l = 116.0 * fy - 16.0,
            a = 500.0 * (fx - fy),
            b = 200.0 * (fy - fz)
        )
    }

    fun deltaE(lab1: Lab, lab2: Lab): Double {
        return sqrt(
            (lab1.l - lab2.l).pow(2) +
                (lab1.a - lab2.a).pow(2) +
                (lab1.b - lab2.b).pow(2)
        )
    }

    fun findTopMatches(
        hex: String,
        candidates: List<ThreadEntity>,
        limit: Int = 3
    ): List<ThreadMatch> {
        if (candidates.isEmpty()) return emptyList()
        val targetLab = rgbToLab(parseHex(hex))
        return candidates
            .map { candidate ->
                val candLab = rgbToLab(parseHex(candidate.hexColor))
                ThreadMatch(candidate, deltaE(targetLab, candLab))
            }
            .sortedBy { it.deltaE }
            .take(limit)
    }

    fun findClosestDmc(hex: String, dmcThreads: List<ThreadEntity>): ThreadEntity? {
        return findTopMatches(hex, dmcThreads, limit = 1).firstOrNull()?.thread
    }

    /**
     * Clé de tri chromatique : regroupe d'abord les neutres (Blanc -> Gris -> Noir),
     * puis les couleurs par teinte (arc-en-ciel) et luminosité décroissante.
     */
    fun colorSortKey(hex: String): Long {
        val (rInt, gInt, bInt) = parseHex(hex)
        val r = rInt / 255.0
        val g = gInt / 255.0
        val b = bInt / 255.0

        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val delta = maxC - minC
        val lightness = ((maxC + minC) / 2.0 * 1000).toInt().coerceIn(0, 999)

        // Neutres (Blanc / Gris / Noir) en premier, du plus clair au plus foncé
        if (delta < 0.12) {
            return (1000 - lightness).toLong()
        }

        val hue = when {
            maxC == r -> 60.0 * (((g - b) / delta) % 6.0)
            maxC == g -> 60.0 * (((b - r) / delta) + 2.0)
            else -> 60.0 * (((r - g) / delta) + 4.0)
        }.let { if (it < 0) it + 360.0 else it }

        // Regrouper par tranche de teinte de 12° pour éviter les sauts brusques, puis luminosité
        val hueBucket = (hue / 12.0).toInt().coerceIn(0, 30) + 1
        return hueBucket * 10_000L + (1000 - lightness)
    }
}
