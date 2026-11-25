package com.pirateai.vision

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Transparent detection overlay. Place above a FIT_CENTER preview in a FrameLayout.
 * Detection coordinates must refer to the exact source frame shown underneath.
 */
class DetectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 230, 180)
        style = Paint.Style.STROKE
        strokeWidth = dp(2.2f)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = dp(12f)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 95, 80)
        style = Paint.Style.FILL
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = dp(13f)
    }

    @Volatile private var detections: List<DetectionBox> = emptyList()
    @Volatile private var frameWidth = 1
    @Volatile private var frameHeight = 1

    fun setDetections(items: List<DetectionBox>, sourceWidth: Int, sourceHeight: Int) {
        if (sourceWidth <= 0 || sourceHeight <= 0) return
        detections = items.toList()
        frameWidth = sourceWidth
        frameHeight = sourceHeight
        postInvalidateOnAnimation()
    }

    fun clearDetections() {
        detections = emptyList()
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        // Match ImageView.ScaleType.FIT_CENTER.
        val scale = min(width.toFloat() / frameWidth, height.toFloat() / frameHeight)
        val offsetX = (width - frameWidth * scale) / 2f
        val offsetY = (height - frameHeight * scale) / 2f
        val current = detections

        if (current.isEmpty()) {
            canvas.drawText("No objects detected", dp(12f), dp(24f), emptyPaint)
            return
        }

        current.forEach { item ->
            val rect = RectF(
                offsetX + item.left * scale,
                offsetY + item.top * scale,
                offsetX + item.right * scale,
                offsetY + item.bottom * scale
            )
            rect.left = rect.left.coerceIn(0f, width.toFloat())
            rect.top = rect.top.coerceIn(0f, height.toFloat())
            rect.right = rect.right.coerceIn(0f, width.toFloat())
            rect.bottom = rect.bottom.coerceIn(0f, height.toFloat())
            if (rect.width() <= 0f || rect.height() <= 0f) return@forEach

            canvas.drawRoundRect(rect, dp(5f), dp(5f), boxPaint)
            val confidence = item.confidence
                ?.takeIf { it.isFinite() }
                ?.coerceIn(0f, 1f)
                ?.let { " " + (it * 100f).toInt() + "%" } ?: ""
            val label = item.label + confidence
            val pad = dp(6f)
            val tagHeight = dp(22f)
            val tagTop = max(0f, rect.top - tagHeight)
            val tagRight = min(width.toFloat(), rect.left + labelPaint.measureText(label) + pad * 2)
            canvas.drawRoundRect(
                RectF(rect.left, tagTop, tagRight, tagTop + tagHeight),
                dp(4f), dp(4f), tagPaint
            )
            canvas.drawText(label, rect.left + pad, tagTop + dp(15f), labelPaint)
        }
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density
}
