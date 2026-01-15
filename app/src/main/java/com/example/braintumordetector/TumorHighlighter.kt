package com.example.braintumordetector

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.min

/**
 * Tumor area ko highlight karne ke liye helper class
 */
class TumorHighlighter {

    /**
     * Image par heatmap overlay add karta hai based on tumor detection
     * @param originalBitmap Original MRI scan image
     * @param tumorLabel Detected tumor type (meningioma, glioma, pituitary, no_tumor)
     * @param confidence Detection confidence (0.0 to 1.0)
     * @return Highlighted bitmap with heatmap overlay
     */
    fun highlightTumorArea(
        originalBitmap: Bitmap,
        tumorLabel: String,
        confidence: Float
    ): Bitmap {
        // Agar no tumor hai to original image return karo
        if (tumorLabel.equals("no_tumor", ignoreCase = true)) {
            return originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        // Confidence kam hai to highlighting weak rakho
        if (confidence < 0.3f) {
            return originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        // Naya mutable bitmap banao
        val resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        val width = resultBitmap.width
        val height = resultBitmap.height

        // Tumor type ke according highlighting position decide karo
        val (centerX, centerY, radius) = getTumorPosition(width, height, tumorLabel)

        // Heatmap paint setup
        val heatmapPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL

            // Confidence ke according transparency adjust karo
            val alpha = (confidence * 120).toInt().coerceIn(60, 150)

            // Radial gradient for smooth heatmap effect
            shader = RadialGradient(
                centerX, centerY, radius,
                intArrayOf(
                    Color.argb(alpha, 255, 0, 0),      // Center: Red
                    Color.argb(alpha / 2, 255, 165, 0), // Middle: Orange
                    Color.argb(0, 255, 255, 0)          // Edge: Transparent yellow
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }

        // Heatmap draw karo
        canvas.drawCircle(centerX, centerY, radius, heatmapPaint)

        // Optional: Border/outline add karo for better visibility
        val outlinePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = Color.RED  // Pure RED for visibility
            strokeWidth = 4f  // ✅ Reduced to 4f (thinner outline)
        }
        canvas.drawCircle(centerX, centerY, radius, outlinePaint)

        return resultBitmap
    }

    /**
     * Tumor type ke according position determine karta hai
     * Different tumor types brain ke different areas mein hote hain
     */
    private fun getTumorPosition(
        width: Int,
        height: Int,
        tumorLabel: String
    ): Triple<Float, Float, Float> {

        val centerX: Float
        val centerY: Float
        val baseRadius = min(width, height) * 0.12f  // ✅✅ MAXIMUM SHRINK: 0.12f (only 12% of image!)

        when (tumorLabel.lowercase()) {
            "meningioma" -> {
                // Meningioma: Brain ke outer covering mein hota hai
                // Top-right area highlight karo
                centerX = width * 0.6f
                centerY = height * 0.35f
            }
            "pituitary" -> {
                // Pituitary: Brain ke base/center mein hota hai
                // Center-bottom area highlight karo
                centerX = width * 0.5f
                centerY = height * 0.55f
            }
            "glioma" -> {
                // Glioma: Brain tissue mein kahin bhi ho sakta hai
                // Center-left area highlight karo
                centerX = width * 0.45f
                centerY = height * 0.4f
            }
            else -> {
                // Default: Center highlight karo
                centerX = width * 0.5f
                centerY = height * 0.45f
            }
        }

        return Triple(centerX, centerY, baseRadius)
    }

    /**
     * Advanced version: Multiple hotspots ke saath
     */
    fun highlightTumorAreaAdvanced(
        originalBitmap: Bitmap,
        tumorLabel: String,
        confidence: Float
    ): Bitmap {
        if (tumorLabel.equals("no_tumor", ignoreCase = true) || confidence < 0.3f) {
            return originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        val resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        val width = resultBitmap.width
        val height = resultBitmap.height

        // Multiple hotspots for realistic effect
        val hotspots = getMultipleHotspots(width, height, tumorLabel)

        hotspots.forEach { (x, y, radius, intensity) ->
            val alpha = (confidence * intensity * 100).toInt().coerceIn(40, 120)

            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                shader = RadialGradient(
                    x, y, radius,
                    intArrayOf(
                        Color.argb(alpha, 255, 50, 50),
                        Color.argb(alpha / 2, 255, 100, 0),
                        Color.argb(0, 255, 200, 0)
                    ),
                    floatArrayOf(0f, 0.6f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(x, y, radius, paint)
        }

        return resultBitmap
    }

    /**
     * Multiple hotspot positions return karta hai
     */
    private fun getMultipleHotspots(
        width: Int,
        height: Int,
        tumorLabel: String
    ): List<Hotspot> {
        val baseRadius = min(width, height) * 0.25f

        return when (tumorLabel.lowercase()) {
            "meningioma" -> listOf(
                Hotspot(width * 0.6f, height * 0.35f, baseRadius, 1.0f),
                Hotspot(width * 0.55f, height * 0.4f, baseRadius * 0.6f, 0.7f)
            )
            "pituitary" -> listOf(
                Hotspot(width * 0.5f, height * 0.55f, baseRadius, 1.0f)
            )
            "glioma" -> listOf(
                Hotspot(width * 0.45f, height * 0.4f, baseRadius, 1.0f),
                Hotspot(width * 0.5f, height * 0.35f, baseRadius * 0.7f, 0.8f)
            )
            else -> listOf(
                Hotspot(width * 0.5f, height * 0.45f, baseRadius, 1.0f)
            )
        }
    }

    data class Hotspot(
        val x: Float,
        val y: Float,
        val radius: Float,
        val intensity: Float
    )
}