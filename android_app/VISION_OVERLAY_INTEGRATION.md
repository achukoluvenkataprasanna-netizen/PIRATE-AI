# Live detection overlay integration

The repository currently contains the README but not the Android Gradle project or
the previously described activity/capture-service source. This commit adds a reusable
overlay component; it is not yet wired into an app screen or verified by an Android build.

## Add it above the preview

Place an ImageView and DetectionOverlayView in the same FrameLayout. Set the
ImageView scaleType to FIT_CENTER so the coordinates align.

```kotlin
val preview = ImageView(context).apply {
    scaleType = ImageView.ScaleType.FIT_CENTER
}
val overlay = DetectionOverlayView(context)
val previewStack = FrameLayout(context).apply {
    addView(preview)
    addView(overlay)
}
```

## Pass ML Kit results to it

Call this in the detector success callback, using dimensions from the exact frame
that was analyzed:

```kotlin
val boxes = detectedObjects.map { detected ->
    val bounds = detected.boundingBox
    val label = detected.labels.firstOrNull()
    DetectionBox(
        label = label?.text ?: "Object",
        confidence = label?.confidence,
        left = bounds.left.toFloat(),
        top = bounds.top.toFloat(),
        right = bounds.right.toFloat(),
        bottom = bounds.bottom.toFloat()
    )
}
overlay.post {
    overlay.setDetections(boxes, sourceFrame.width, sourceFrame.height)
}
```

Clear stale boxes when capture stops with overlay.clearDetections(). If the preview
is rotated, cropped, or mirrored, apply the identical transform to the boxes.
Confidence is displayed only when supplied by the detector; no percentage is invented.
