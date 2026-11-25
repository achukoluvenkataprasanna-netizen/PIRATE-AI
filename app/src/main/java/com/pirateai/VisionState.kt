package com.pirateai

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DetectionBox(val label: String, val confidence: Float?, val left: Float, val top: Float, val right: Float, val bottom: Float)
data class VisionSnapshot(
    val running: Boolean = false,
    val status: String = "Ready",
    val bitmap: Bitmap? = null,
    val detections: List<DetectionBox> = emptyList(),
    val frameWidth: Int = 0,
    val frameHeight: Int = 0
)

object VisionState {
    private val mutable = MutableStateFlow(VisionSnapshot())
    val state = mutable.asStateFlow()
    fun setStatus(message: String) { mutable.value = mutable.value.copy(running = true, status = message) }
    fun publish(bitmap: Bitmap, boxes: List<DetectionBox>, width: Int, height: Int) {
        val old = mutable.value.bitmap
        mutable.value = VisionSnapshot(true, "Live analysis active", bitmap, boxes, width, height)
        if (old != null && old !== bitmap && !old.isRecycled) old.recycle()
    }
    fun stop() {
        val old = mutable.value.bitmap
        mutable.value = VisionSnapshot(status = "Capture stopped")
        if (old != null && !old.isRecycled) old.recycle()
    }
}
