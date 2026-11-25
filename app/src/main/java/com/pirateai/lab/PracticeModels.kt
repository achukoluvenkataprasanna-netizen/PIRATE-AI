package com.pirateai.lab

data class PracticeMetrics(
    val attempts: Int = 0,
    val successes: Int = 0,
    val mistakes: Int = 0,
    val elapsedSeconds: Long = 0
) {
    val successRate: Float get() = if (attempts == 0) 0f else successes.toFloat() / attempts
}

data class StrategySuggestion(val title: String, val explanation: String, val confidence: Float)

object StrategyAdvisor {
    fun suggest(genre: GameGenre, notes: String, metrics: PracticeMetrics): List<StrategySuggestion> {
        val base = when (genre) {
            GameGenre.PUZZLE -> listOf(StrategySuggestion("Plan before moving", "Compare legal moves and avoid committing until the next state is clear.", .55f))
            GameGenre.STRATEGY -> listOf(StrategySuggestion("Prioritise objectives", "Compare resource cost, objective progress and likely consequences.", .55f))
            GameGenre.RACING -> listOf(StrategySuggestion("Prefer consistent control", "Use short, measured steering inputs in a practice environment.", .5f))
            GameGenre.PLATFORMER, GameGenre.ACTION -> listOf(StrategySuggestion("Observe timing", "Study obstacle and movement timing before trying a new input.", .5f))
            GameGenre.SPORTS -> listOf(StrategySuggestion("Watch positioning", "Evaluate space, timing and objective progress before acting.", .5f))
            else -> listOf(StrategySuggestion("Clarify the objective", "Record the goal, available actions and what counts as success.", .35f))
        }
        val feedback = when {
            metrics.attempts == 0 -> "Record a few practice attempts to establish a baseline."
            metrics.successRate < .5f -> "Review recent mistakes and change one variable at a time."
            else -> "Keep the successful approach and test it across another practice session."
        }
        return base + StrategySuggestion("Practice feedback", "$feedback Notes: $notes".take(240), .4f)
    }
}
