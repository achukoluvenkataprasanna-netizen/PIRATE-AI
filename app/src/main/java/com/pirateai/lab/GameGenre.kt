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
    private val rules: Map<GameGenre, List<String>> = mapOf(
        GameGenre.RACING to listOf("lap", "nitro", "race", "racing", "finish", "speedometer", "steering", "drift", "checkpoint"),
        GameGenre.STRATEGY to listOf("chess", "turn", "strategy", "army", "troop", "base", "territory", "resource", "build"),
        GameGenre.PUZZLE to listOf("match", "puzzle", "tile", "grid", "rotate", "pattern", "block", "word", "hint"),
        GameGenre.PLATFORMER to listOf("jump", "platform", "checkpoint", "run", "ledge", "coin"),
        GameGenre.ACTION to listOf("attack", "defend", "weapon", "enemy", "boss", "health", "joystick", "character"),
        GameGenre.SPORTS to listOf("goal", "team", "score", "period", "player", "match", "foul", "kickoff"),
        GameGenre.SIMULATION to listOf("coins", "resources", "upgrade", "factory", "shop", "build", "manage", "farm"),
        GameGenre.RPG to listOf("inventory", "quest", "level up", "mission", "equipment", "dialogue", "skill"),
        GameGenre.RHYTHM to listOf("rhythm", "combo", "beat", "perfect", "tap", "music"),
        GameGenre.CARD to listOf("deck", "draw", "cards", "hand", "discard", "trump")
    )

    fun detect(s: GameSignals): GenreGuess {
        val evidence = (s.visibleText + s.objectLabels).joinToString(" ").lowercase()
        val scores = mutableMapOf<GameGenre, Int>()
        val reasons = mutableMapOf<GameGenre, MutableList<String>>()
        fun add(g: GameGenre, points: Int, reason: String) {
            scores[g] = (scores[g] ?: 0) + points
            reasons.getOrPut(g) { mutableListOf() }.add(reason)
        }
        rules.forEach { (genre, keywords) ->
            val hits = keywords.filter { evidence.contains(it) }
            if (hits.isNotEmpty()) add(genre, hits.size * 2, "Signals: ${hits.take(4).joinToString()}")
        }
        if (s.hasTimer) add(GameGenre.RACING, 1, "Timer detected")
        if (s.hasScore && scores[GameGenre.SPORTS] == null) add(GameGenre.SPORTS, 1, "Score indicator detected")
        if (s.hasGrid) add(GameGenre.PUZZLE, 3, "Grid layout detected")
        if (s.hasVirtualJoystick) add(GameGenre.ACTION, 2, "Virtual joystick detected")
        val best = scores.maxByOrNull { it.value }
            ?: return GenreGuess(GameGenre.UNKNOWN, 0f, listOf("Insufficient evidence; ask the player to select a genre"))
        val total = scores.values.sum().coerceAtLeast(1)
        val confidence = (best.value.toFloat() / total).coerceIn(0f, 1f)
        return GenreGuess(best.key, confidence, reasons[best.key].orEmpty())
    }
}
