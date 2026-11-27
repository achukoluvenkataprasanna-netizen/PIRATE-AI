package com.pirateai.lab

/**
 * Small deterministic practice game owned by PIRATE-AI.
 * The planner returns suggestions only; the player applies each move.
 */
enum class GridMove(val dx: Int, val dy: Int, val label: String) {
    UP(0, -1, "UP"), DOWN(0, 1, "DOWN"), LEFT(-1, 0, "LEFT"), RIGHT(1, 0, "RIGHT")
}

data class GridPoint(val x: Int, val y: Int)

data class TreasureGridState(
    val player: GridPoint = GridPoint(0, 0),
    val treasure: GridPoint = GridPoint(4, 4),
    val walls: Set<GridPoint> = setOf(
        GridPoint(1, 1), GridPoint(1, 2), GridPoint(1, 3),
        GridPoint(3, 1), GridPoint(3, 2)
    ),
    val moves: Int = 0
) {
    val completed: Boolean get() = player == treasure

    fun apply(move: GridMove): TreasureGridState {
        if (completed) return this
        val next = GridPoint(player.x + move.dx, player.y + move.dy)
        if (next.x !in 0..4 || next.y !in 0..4 || next in walls) return this
        return copy(player = next, moves = moves + 1)
    }

    fun snapshot(): String =
        "player=" + player.x + "," + player.y +
            ";treasure=" + treasure.x + "," + treasure.y +
            ";moves=" + moves + ";complete=" + completed
}

data class PlannedGridAction(
    val move: GridMove,
    val remainingSteps: Int,
    val explanation: String
)

object TreasureGridPlanner {
    private val order = listOf(GridMove.RIGHT, GridMove.DOWN, GridMove.LEFT, GridMove.UP)

    /** Breadth-first search over legal cells; returns null if no route exists. */
    fun plan(state: TreasureGridState): List<GridMove> {
        if (state.completed) return emptyList()
        val queue = ArrayDeque<GridPoint>()
        val previous = mutableMapOf<GridPoint, Pair<GridPoint, GridMove>?>()
        queue.add(state.player)
        previous[state.player] = null

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == state.treasure) break
            for (move in order) {
                val next = GridPoint(current.x + move.dx, current.y + move.dy)
                if (next.x !in 0..4 || next.y !in 0..4 || next in state.walls || previous.containsKey(next)) continue
                previous[next] = current to move
                queue.addLast(next)
            }
        }
        if (!previous.containsKey(state.treasure)) return emptyList()
        val reversed = mutableListOf<GridMove>()
        var cursor = state.treasure
        while (cursor != state.player) {
            val link = previous[cursor] ?: return emptyList()
            reversed.add(link.second)
            cursor = link.first
        }
        return reversed.asReversed()
    }

    fun suggest(state: TreasureGridState): PlannedGridAction? {
        val route = plan(state)
        val first = route.firstOrNull() ?: return null
        return PlannedGridAction(
            first,
            route.size,
            "Safe shortest path found. Review and press APPLY to take this one move."
        )
    }
}
