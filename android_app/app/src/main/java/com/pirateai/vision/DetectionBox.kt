package com.pirateai.vision

/** Bounding box coordinates are pixels in the original captured frame. */
data class DetectionBox(
    val label: String,
    val confidence: Float?,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    init {
        require(label.isNotBlank())
        require(listOf(left, top, right, bottom).all { it.isFinite() })
        require(right >= left && bottom >= top)
    }
}
