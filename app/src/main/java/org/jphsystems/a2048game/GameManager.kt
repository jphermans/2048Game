package org.jphsystems.a2048game

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlin.random.Random

class GameManager(context: Context) {
    private val GRID_SIZE = 4
    private val WINNING_VALUE = 2048
    
    private var grid: Array<Array<Int>> = Array(GRID_SIZE) { Array(GRID_SIZE) { 0 } }
    private var score: Int = 0
    private var highScore: Int = 0
    private var hasWonGame: Boolean = false
    private var emptyCells: MutableSet<Pair<Int, Int>> = mutableSetOf()
    
    var onGridUpdated: ((Array<Array<Int>>) -> Unit)? = null
    var onScoreUpdated: ((Int) -> Unit)? = null
    var onGameOver: (() -> Unit)? = null
    var onGameWon: (() -> Unit)? = null

    private val prefs: SharedPreferences = context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        highScore = prefs.getInt("highScore", 0)
        loadGameState()
    }

    private fun saveGameState() {
        val gridJson = gson.toJson(grid)
        prefs.edit().apply {
            putString("grid", gridJson)
            putInt("score", score)
            putInt("highScore", highScore)
            putBoolean("hasWon", hasWonGame)
            apply()
        }
    }

    private fun loadGameState() {
        val gridJson = prefs.getString("grid", null)
        if (gridJson != null) {
            grid = gson.fromJson(gridJson, Array<Array<Int>>::class.java)
            score = prefs.getInt("score", 0)
            hasWonGame = prefs.getBoolean("hasWon", false)
            updateEmptyCells()
            onGridUpdated?.invoke(grid)
            onScoreUpdated?.invoke(score)
        } else {
            initializeGame()
        }
    }

    private fun updateEmptyCells() {
        emptyCells.clear()
        for (i in 0 until GRID_SIZE) {
            for (j in 0 until GRID_SIZE) {
                if (grid[i][j] == 0) {
                    emptyCells.add(Pair(i, j))
                }
            }
        }
    }

    fun initializeGame() {
        grid = Array(GRID_SIZE) { Array(GRID_SIZE) { 0 } }
        score = 0
        hasWonGame = false
        emptyCells.clear()
        // Add all cells as empty initially
        for (i in 0 until GRID_SIZE) {
            for (j in 0 until GRID_SIZE) {
                emptyCells.add(Pair(i, j))
            }
        }
        addNewTile()
        addNewTile()
        onGridUpdated?.invoke(grid)
        onScoreUpdated?.invoke(score)
        saveGameState()
    }

    private fun addNewTile() {
        if (emptyCells.isNotEmpty()) {
            val emptyCell = emptyCells.toList()[Random.nextInt(emptyCells.size)]
            val value = if (Random.nextFloat() < 0.9f) 2 else 4
            grid[emptyCell.first][emptyCell.second] = value
            emptyCells.remove(emptyCell)
        }
    }

    private fun updateHighScore(newScore: Int) {
        if (newScore > highScore) {
            highScore = newScore
            prefs.edit().putInt("highScore", highScore).apply()
            onScoreUpdated?.invoke(score)
        }
    }

    fun getHighScore(): Int = highScore

    fun move(direction: Direction): Boolean {
        var moved = false
        val beforeMove = grid.map { it.clone() }.toTypedArray()

        when (direction) {
            Direction.UP -> {
                for (col in 0 until GRID_SIZE) {
                    val column = getColumn(col)
                    if (moveAndMergeLine(column)) {
                        moved = true
                    }
                    setColumn(col, column)
                }
            }
            Direction.DOWN -> {
                for (col in 0 until GRID_SIZE) {
                    val column = getColumn(col)
                    column.reverse()
                    if (moveAndMergeLine(column)) {
                        moved = true
                    }
                    column.reverse()
                    setColumn(col, column)
                }
            }
            Direction.LEFT -> {
                for (row in 0 until GRID_SIZE) {
                    if (moveAndMergeLine(grid[row])) {
                        moved = true
                    }
                }
            }
            Direction.RIGHT -> {
                for (row in 0 until GRID_SIZE) {
                    grid[row].reverse()
                    if (moveAndMergeLine(grid[row])) {
                        moved = true
                    }
                    grid[row].reverse()
                }
            }
        }

        if (moved) {
            updateEmptyCellsFromMove(beforeMove)
            addNewTile()
            onGridUpdated?.invoke(grid)
            onScoreUpdated?.invoke(score)
            saveGameState()

            if (!hasWonGame && hasWon()) {
                hasWonGame = true
                onGameWon?.invoke()
            }
            
            if (isGameOver()) {
                onGameOver?.invoke()
            }
        }

        return moved
    }

    private fun updateEmptyCellsFromMove(beforeMove: Array<Array<Int>>) {
        for (i in 0 until GRID_SIZE) {
            for (j in 0 until GRID_SIZE) {
                val wasEmpty = beforeMove[i][j] == 0
                val isEmpty = grid[i][j] == 0
                val pos = Pair(i, j)
                
                if (wasEmpty && !isEmpty) {
                    emptyCells.remove(pos)
                } else if (!wasEmpty && isEmpty) {
                    emptyCells.add(pos)
                }
            }
        }
    }

    private fun canMoveVertically(col: Int): Boolean {
        // Check if there are any empty spaces above non-empty tiles
        for (row in 1 until GRID_SIZE) {
            if (grid[row][col] != 0 && grid[row - 1][col] == 0) {
                return true
            }
        }
        
        // Check if there are any adjacent tiles that can be merged
        for (row in 0 until GRID_SIZE - 1) {
            if (grid[row][col] != 0 && grid[row][col] == grid[row + 1][col]) {
                return true
            }
        }
        
        return false
    }

    private fun canMoveHorizontally(row: Int): Boolean {
        // Check if there are any empty spaces to the left of non-empty tiles
        for (col in 1 until GRID_SIZE) {
            if (grid[row][col] != 0 && grid[row][col - 1] == 0) {
                return true
            }
        }
        
        // Check if there are any adjacent tiles that can be merged
        for (col in 0 until GRID_SIZE - 1) {
            if (grid[row][col] != 0 && grid[row][col] == grid[row][col + 1]) {
                return true
            }
        }
        
        return false
    }

    private fun moveAndMergeLine(line: Array<Int>): Boolean {
        var moved = false
        
        // Step 1: Move all numbers to the beginning (left/up), removing gaps
        val numbers = line.filter { it != 0 }.toMutableList()
        
        // If we removed any zeros, that means some numbers will move
        if (numbers.size != line.count { it != 0 }) {
            moved = true
        }
        
        // Step 2: Merge numbers iteratively until no more merges are possible
        var i = 0
        while (i < numbers.size - 1) {
            if (numbers[i] == numbers[i + 1]) {
                numbers[i] *= 2
                score += numbers[i]
                updateHighScore(score)
                numbers.removeAt(i + 1)
                moved = true
                // Don't increment i here, so we can check if the newly merged number
                // can merge with the next number
            } else {
                i++
            }
        }
        
        // Step 3: Fill the remaining space with zeros
        while (numbers.size < line.size) {
            numbers.add(0)
        }
        
        // Step 4: Update the line with the new values
        for (i in line.indices) {
            if (line[i] != numbers[i]) {
                moved = true
                line[i] = numbers[i]
            }
        }
        
        return moved
    }

    private fun getColumn(col: Int): Array<Int> {
        return Array(GRID_SIZE) { row -> grid[row][col] }
    }

    private fun setColumn(col: Int, column: Array<Int>) {
        for (row in 0 until GRID_SIZE) {
            grid[row][col] = column[row]
        }
    }

    private fun hasWon(): Boolean {
        for (i in 0 until GRID_SIZE) {
            for (j in 0 until GRID_SIZE) {
                if (grid[i][j] >= WINNING_VALUE) {
                    return true
                }
            }
        }
        return false
    }

    private fun isGameOver(): Boolean {
        if (emptyCells.isNotEmpty()) return false
        
        // Check for possible merges horizontally and vertically
        for (i in 0 until GRID_SIZE) {
            for (j in 0 until GRID_SIZE - 1) {
                if (grid[i][j] == grid[i][j + 1] || grid[j][i] == grid[j + 1][i]) {
                    return false
                }
            }
        }
        
        return true
    }

    fun getGrid(): Array<Array<Int>> = grid.map { it.clone() }.toTypedArray()
    fun getScore(): Int = score

    enum class Direction {
        UP, DOWN, LEFT, RIGHT
    }
}
