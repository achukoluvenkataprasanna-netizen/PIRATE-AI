package com.pirateai

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

class DetectionOverlayView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0, 235, 190); style = Paint.Style.STROKE; strokeWidth = dp(2f) }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = dp(12f); typeface = android.graphics.Typeface.DEFAULT_BOLD }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0, 105, 85) }
    @Volatile private var boxes: List<DetectionBox> = emptyList()
    @Volatile private var frameW = 1
    @Volatile private var frameH = 1
    fun setDetections(items: List<DetectionBox>, w: Int, h: Int) { if (w <= 0 || h <= 0) return; boxes = items.toList(); frameW = w; frameH = h; postInvalidateOnAnimation() }
    fun clear() { boxes = emptyList(); postInvalidateOnAnimation() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val scale = min(width.toFloat() / frameW, height.toFloat() / frameH)
        val ox = (width - frameW * scale) / 2f
        val oy = (height - frameH * scale) / 2f
        boxes.forEach { b ->
            val r = RectF(ox + b.left * scale, oy + b.top * scale, ox + b.right * scale, oy + b.bottom * scale)
            r.left = r.left.coerceIn(0f, width.toFloat()); r.right = r.right.coerceIn(0f, width.toFloat())
            r.top = r.top.coerceIn(0f, height.toFloat()); r.bottom = r.bottom.coerceIn(0f, height.toFloat())
            if (r.width() <= 0 || r.height() <= 0) return@forEach
            canvas.drawRoundRect(r, dp(4f), dp(4f), stroke)
            val confidence = b.confidence?.let { " " + (it.coerceIn(0f, 1f) * 100).toInt() + "%" } ?: ""
            val label = b.label + confidence
            val pad = dp(5f); val tagH = dp(21f); val top = max(0f, r.top - tagH)
            val right = min(width.toFloat(), r.left + text.measureText(label) + pad * 2)
            canvas.drawRoundRect(RectF(r.left, top, right, top + tagH), dp(4f), dp(4f), fill)
            canvas.drawText(label, r.left + pad, top + dp(15f), text)
        }
    }
    private fun dp(v: Float) = v * resources.displayMetrics.density
}
