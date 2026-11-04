package com.example.data.matching

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap

/**
 * Lokaler Bildvergleich per Difference-Hash (dHash), komplett auf dem Gerät.
 * Bilder werden nur geladen, verkleinert und zu einem 64-Bit-Fingerabdruck verrechnet –
 * kein Bild und keine Produktdaten gehen an Dritte.
 */
class ImageFingerprint(private val http: OkHttpClient) {

    private val cache = ConcurrentHashMap<String, Long>()
    private val failed: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun clearCache() {
        cache.clear()
        failed.clear()
    }

    /** 64-Bit-dHash des Bildes oder null, wenn es nicht geladen werden kann. Ergebnisse werden pro Scan gecacht. */
    fun hashOf(url: String): Long? {
        if (url.isBlank()) return null
        cache[url]?.let { return it }
        if (url in failed) return null

        val thumb = thumbnailUrl(url)
        val hash = downloadAndHash(thumb) ?: if (thumb != url) downloadAndHash(url) else null
        if (hash != null) cache[url] = hash else failed.add(url)
        return hash
    }

    private fun downloadAndHash(url: String): Long? {
        return try {
            http.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                if (!response.isSuccessful) return null
                val bytes = response.body?.bytes() ?: return null
                if (bytes.isEmpty() || bytes.size > 8 * 1024 * 1024) return null

                // Erst nur die Maße lesen, dann stark verkleinert dekodieren (spart Speicher)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= 64 && bounds.outHeight / (sample * 2) >= 64) sample *= 2

                val bitmap = BitmapFactory.decodeByteArray(
                    bytes, 0, bytes.size,
                    BitmapFactory.Options().apply { inSampleSize = sample }
                ) ?: return null
                val small = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
                val pixels = IntArray(9 * 8)
                small.getPixels(pixels, 0, 9, 0, 0, 9, 8)
                if (small !== bitmap) small.recycle()
                bitmap.recycle()
                dHash(pixels)
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /** dHash aus 9x8 ARGB-Pixeln: je Zeile 8 Bits, Bit = linkes Pixel heller als rechtes. */
        fun dHash(argb9x8: IntArray): Long {
            require(argb9x8.size == 72) { "9x8 Pixel erwartet" }
            var hash = 0L
            var bit = 0
            for (y in 0 until 8) {
                for (x in 0 until 8) {
                    val left = luma(argb9x8[y * 9 + x])
                    val right = luma(argb9x8[y * 9 + x + 1])
                    if (left > right) hash = hash or (1L shl bit)
                    bit++
                }
            }
            return hash
        }

        private fun luma(argb: Int): Int {
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            return (r * 299 + g * 587 + b * 114) / 1000
        }

        fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

        /** 0 = zufällig verschieden (≥ 32 abweichende Bits), 100 = identisch. */
        fun imageScore(a: Long, b: Long): Int = (32 - hammingDistance(a, b)).coerceAtLeast(0) * 100 / 32

        /** Kleine Vorschaubilder anfordern statt Originale (schneller, weniger Datenvolumen). */
        fun thumbnailUrl(url: String): String = when {
            url.contains("ebayimg.com") -> url.replace(Regex("s-l\\d+"), "s-l225")
            url.contains("alicdn.com") && !Regex("_\\d+x\\d+").containsMatchIn(url) -> "${url}_220x220.jpg"
            else -> url
        }
    }
}
