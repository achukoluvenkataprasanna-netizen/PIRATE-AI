package com.pirateai.lab

/** Explicit user-controlled practice gate. No background or unattended action loop. */
class PracticeSafety {
    @Volatile private var armed = false
    @Volatile private var manualOverride = false
    @Volatile private var emergencyStopped = false

    fun arm(userConfirmed: Boolean, targetIsAllowlisted: Boolean): Boolean {
        if (!userConfirmed || !targetIsAllowlisted || manualOverride || emergencyStopped) return false
        armed = true
        return true
    }
    fun canPerformSingleConfirmedAction(): Boolean = armed && !manualOverride && !emergencyStopped
    fun manualTakeover() { manualOverride = true; armed = false }
    fun resumeAfterManualTakeover(userConfirmed: Boolean) { if (userConfirmed && !emergencyStopped) manualOverride = false }
    fun emergencyStop() { emergencyStopped = true; armed = false; manualOverride = true }
    fun resetEmergencyStop(userConfirmed: Boolean) { if (userConfirmed) { emergencyStopped = false; manualOverride = false; armed = false } }
    fun disarm() { armed = false }
}
