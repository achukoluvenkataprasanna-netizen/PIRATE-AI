package com.pirateai.lab

/** Central fail-closed gate shared by the dashboard and AccessibilityService. */
object PracticeSafety {
    @Volatile private var armed = false
    @Volatile private var manualOverride = false
    @Volatile private var emergencyStopped = false

    @Synchronized fun arm(userConfirmed: Boolean, targetIsUserOwnedTestApp: Boolean): Boolean {
        if (!userConfirmed || !targetIsUserOwnedTestApp || manualOverride || emergencyStopped) return false
        armed = true
        return true
    }
    fun canPerformSingleConfirmedAction(userConfirmed: Boolean): Boolean =
        userConfirmed && armed && !manualOverride && !emergencyStopped
    @Synchronized fun manualTakeover() { manualOverride = true; armed = false }
    @Synchronized fun emergencyStop() { emergencyStopped = true; armed = false; manualOverride = true }
    @Synchronized fun resetEmergencyStop(userConfirmed: Boolean) {
        if (userConfirmed) { emergencyStopped = false; manualOverride = false; armed = false }
    }
    @Synchronized fun disarm() { armed = false }
    fun isStopped(): Boolean = emergencyStopped
}
