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
    val frameHeight: Int = 0,
    val visibleText: List<String> = emptyList()
)

object VisionState {
    private val mutable = MutableStateFlow(VisionSnapshot())
    val state = mutable.asStateFlow()

    fun setStatus(message: String, running: Boolean = false) {
        mutable.value = mutable.value.copy(status = message, running = running)
    }

    fun publishFrame(bitmap: Bitmap, width: Int, height: Int) {
        mutable.value = mutable.value.copy(
            running = true,
            status = "Frame received — object detection starting",
            bitmap = bitmap,
            frameWidth = width,
            frameHeight = height,
            detections = emptyList()
        )
    }

    fun publish(bitmap: Bitmap, boxes: List<DetectionBox>, width: Int, height: Int) {
        mutable.value = VisionSnapshot(true, "Live analysis active", bitmap, boxes, width, height)
    }

    fun publishAnalysis(boxes: List<DetectionBox>, text: List<String>) {
        mutable.value = mutable.value.copy(
            running = true,
            status = "Live analysis active",
            detections = boxes,
            visibleText = text
        )
    }

    fun stop() {
        mutable.value = VisionSnapshot(status = "Capture stopped")
    }
}
