package com.pirateai

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * User-enabled accessibility bridge.
 * Gestures are deliberately exposed only through an explicit in-app confirmation call.
 * No background command listener, autonomous clicking loop, or transaction automation.
 */
class PirateAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally observe no text, passwords, notifications, or page contents.
    }

    override fun onInterrupt() = Unit

    fun performConfirmedTap(x: Float, y: Float, userConfirmed: Boolean): Boolean {
        if (!userConfirmed || !isTouchExplorationEnabledCompat()) return false
        if (x < 0f || y < 0f || x > resources.displayMetrics.widthPixels || y > resources.displayMetrics.heightPixels) return false
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    private fun isTouchExplorationEnabledCompat(): Boolean = Build.VERSION.SDK_INT >= 24
}
