package com.pirateai.lab

enum class GameGenre { RACING, STRATEGY, PUZZLE, PLATFORMER, ACTION, SPORTS, SIMULATION, RPG, RHYTHM, CARD, UNKNOWN }

data class GameSignals(
    val visibleText: List<String> = emptyList(),
    val objectLabels: List<String> = emptyList(),
    val hasTimer: Boolean = false,
    val hasScore: Boolean = false,
    val hasGrid: Boolean = false,
    val hasVirtualJoystick: Boolean = false
)

data class GenreGuess(val genre: GameGenre, val confidence: Float, val reasons: List<String>)

object GenreDetector {
    fun detect(s: GameSignals): GenreGuess {
        val text = s.visibleText.joinToString(" ").lowercase()
        val labels = s.objectLabels.joinToString(" ").lowercase()
        val scores = mutableMapOf<GameGenre, Int>()
        val reasons = mutableMapOf<GameGenre, MutableList<String>>()
        fun add(g: GameGenre, n: Int, reason: String) {
            scores[g] = (scores[g] ?: 0) + n
            reasons.getOrPut(g) { mutableListOf() }.add(reason)
        }
        if (listOf("lap","speed","nitro","race","finish").any(text::contains)) add(GameGenre.RACING, 3, "Racing terms found")
        if (listOf("chess","turn","strategy","army","build").any(text::contains)) add(GameGenre.STRATEGY, 2, "Strategy terms found")
        if (s.hasGrid || listOf("match","level","puzzle","move").any(text::contains)) add(GameGenre.PUZZLE, 2, "Grid or puzzle indicators found")
        if (s.hasVirtualJoystick || labels.contains("character")) add(GameGenre.ACTION, 1, "Character or joystick detected")
        if (listOf("jump","platform","checkpoint").any(text::contains)) add(GameGenre.PLATFORMER, 3, "Platforming terms found")
        if (listOf("goal","team","score","period").any(text::contains)) add(GameGenre.SPORTS, 2, "Sports indicators found")
        if (listOf("inventory","quest","level up","mission").any(text::contains)) add(GameGenre.RPG, 3, "RPG indicators found")
        if (listOf("rhythm","combo","beat").any(text::contains)) add(GameGenre.RHYTHM, 3, "Rhythm indicators found")
        if (listOf("deck","draw","cards").any(text::contains)) add(GameGenre.CARD, 3, "Card indicators found")
        if (listOf("coins","resources","upgrade","factory").any(text::contains)) add(GameGenre.SIMULATION, 2, "Simulation indicators found")
        if (s.hasTimer) add(GameGenre.RACING, 1, "Timer visible")
        if (s.hasScore) add(GameGenre.SPORTS, 1, "Score visible")
        val best = scores.maxByOrNull { it.value } ?: return GenreGuess(GameGenre.UNKNOWN, 0f, listOf("Not enough signals; ask the user to choose a genre"))
        val total = scores.values.sum().coerceAtLeast(1)
        return GenreGuess(best.key, (best.value.toFloat() / total).coerceIn(0f, 1f), reasons[best.key].orEmpty())
    }
}
