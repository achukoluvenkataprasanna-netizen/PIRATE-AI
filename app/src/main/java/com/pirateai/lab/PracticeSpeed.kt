package com.pirateai.lab

/** Observation cadence only; does not bypass game timing or accelerate game actions. */
enum class PracticeSpeed(val intervalMs: Long) {
    SLOW(1500), NORMAL(750), FAST(400)
}
