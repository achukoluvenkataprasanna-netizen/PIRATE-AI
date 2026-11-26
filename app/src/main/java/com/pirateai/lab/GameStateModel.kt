package com.pirateai.lab

/** Normalised state snapshot supplied by a genre adapter after the user reviews the screen. */
data class GameStateModel(
    val genre: GameGenre,
    val objective: String,
    val visibleEntities: List<String> = emptyList(),
    val resources: Map<String, Int> = emptyMap(),
    val score: Int? = null,
    val timerSeconds: Int? = null,
    val boardDescription: String? = null,
    val playerNotes: String = ""
)

object StateAwareStrategyAdvisor {
    fun recommend(state: GameStateModel, metrics: PracticeMetrics): List<StrategySuggestion> {
        val context = buildString {
            append(state.objective.take(120))
            if (state.score != null) append(" Score: ${state.score}.")
            if (state.timerSeconds != null) append(" Time remaining: ${state.timerSeconds}s.")
            if (state.resources.isNotEmpty()) append(" Resources: ${state.resources.entries.take(5).joinToString { "${it.key}=${it.value}" }}.")
            if (state.visibleEntities.isNotEmpty()) append(" Visible: ${state.visibleEntities.take(8).joinToString()}.")
            if (!state.boardDescription.isNullOrBlank()) append(" Board: ${state.boardDescription.take(100)}.")
            append(" ").append(state.playerNotes.take(100))
        }
        return StrategyAdvisor.suggest(state.genre, context, metrics)
    }
}
