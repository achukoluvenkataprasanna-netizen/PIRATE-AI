package com.pirateai

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import com.pirateai.lab.PracticeSafety

/** Accessibility bridge for a single explicitly confirmed gesture in a user-owned test app. */
class PirateAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    fun performConfirmedTap(x: Float, y: Float, userConfirmed: Boolean, targetIsUserOwnedTestApp: Boolean): Boolean {
        if (!userConfirmed || !targetIsUserOwnedTestApp ||
            !PracticeSafety.canPerformSingleConfirmedAction(userConfirmed)) return false
        if (x < 0f || y < 0f || x > resources.displayMetrics.widthPixels || y > resources.displayMetrics.heightPixels) return false
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80)).build()
        return dispatchGesture(gesture, null, null)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        PracticeSafety.disarm()
        super.onDestroy()
    }

    companion object {
        @Volatile var instance: PirateAccessibilityService? = null
            private set
    }
}
