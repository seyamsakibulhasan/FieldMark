package com.fieldmark.app.stereo

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Generates a red-cyan anaglyph 3D image from a 2D photo using
 * edge-based depth estimation (Sobel) and chromatic disparity.
 *
 * Algorithm:
 *  1. Convert to luminance (Rec. 601).
 *  2. Estimate per-pixel depth by blurring luminance and taking
 *     a high-pass residual. Edges / high-frequency content are
 *     treated as "closer" to the camera.
 *  3. Build a horizontal shift map in pixels (positive = right eye sees
 *     content from the left, producing the depth illusion).
 *  4. For each pixel, sample the red channel from the original and
 *     a horizontally-shifted version for green/blue, producing a
 *     standard red-cyan anaglyph viewable with cheap 3D glasses.
 */
object AnaglyphGenerator {

    data class Options(
        val maxShiftPx: Float = 14f,
        val depthGain: Float = 1.6f,
        val blurRadius: Int = 9
    )

    fun generate(src: Bitmap, options: Options = Options()): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= 1 || h <= 1) return src.copy(src.config ?: Bitmap.Config.ARGB_8888, false)

        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val lum = IntArray(w * h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            lum[i] = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
        }

        val blurred = boxBlur(lum, w, h, options.blurRadius)
        val highPass = IntArray(w * h) { i -> (lum[i] - blurred[i] + 128).coerceIn(0, 255) }
        val sobel = sobelMagnitude(lum, w, h)
        val depth = IntArray(w * h) { i ->
            val edgeWeight = sobel[i] / 255f
            val hpWeight = (highPass[i] - 128).toFloat() / 128f
            val combined = (edgeWeight * 0.65f + hpWeight * 0.35f).coerceIn(-1f, 1f)
            (combined * 255f * options.depthGain).toInt().coerceIn(-255, 255)
        }

        val maxShift = options.maxShiftPx.coerceAtLeast(0f)
        val shiftScale = maxShift / 255f

        val out = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val d = depth[idx]
                val shift = (d * shiftScale).toInt()
                val sx = (x + shift).coerceIn(0, w - 1)
                val sIdx = y * w + sx
                val srcPix = pixels[sIdx]
                val r = (srcPix shr 16) and 0xFF
                val g = (srcPix shr 8) and 0xFF
                val b = srcPix and 0xFF
                val a = (srcPix shr 24) and 0xFF
                out[idx] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }

    private fun boxBlur(src: IntArray, w: Int, h: Int, radius: Int): IntArray {
        if (radius <= 0) return src.copyOf()
        val tmp = IntArray(w * h)
        val out = IntArray(w * h)
        val size = 2 * radius + 1
        for (y in 0 until h) {
            var sum = 0
            for (k in -radius..radius) {
                val x = (k).coerceIn(0, w - 1)
                sum += src[y * w + x]
            }
            for (x in 0 until w) {
                tmp[y * w + x] = sum / size
                val outX = (x - radius).coerceIn(0, w - 1)
                val inX = (x + radius + 1).coerceIn(0, w - 1)
                sum += src[y * w + inX] - src[y * w + outX]
            }
        }
        for (x in 0 until w) {
            var sum = 0
            for (k in -radius..radius) {
                val y = (k).coerceIn(0, h - 1)
                sum += tmp[y * w + x]
            }
            for (y in 0 until h) {
                out[y * w + x] = sum / size
                val outY = (y - radius).coerceIn(0, h - 1)
                val inY = (y + radius + 1).coerceIn(0, h - 1)
                sum += tmp[inY * w + x] - tmp[outY * w + x]
            }
        }
        return out
    }

    private fun sobelMagnitude(src: IntArray, w: Int, h: Int): IntArray {
        val out = IntArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val tl = src[(y - 1) * w + (x - 1)]
                val t = src[(y - 1) * w + x]
                val tr = src[(y - 1) * w + (x + 1)]
                val l = src[y * w + (x - 1)]
                val r = src[y * w + (x + 1)]
                val bl = src[(y + 1) * w + (x - 1)]
                val b = src[(y + 1) * w + x]
                val br = src[(y + 1) * w + (x + 1)]
                val gx = -tl - 2 * l - bl + tr + 2 * r + br
                val gy = -tl - 2 * t - tr + bl + 2 * b + br
                val mag = kotlin.math.sqrt((gx * gx + gy * gy).toDouble()).toInt()
                out[y * w + x] = mag.coerceIn(0, 255)
            }
        }
        return out
    }
}
