package org.jphsystems.a2048game

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private val viewModel: GameViewModel by viewModels()
    private lateinit var gameGrid: GridLayout
    private lateinit var scoreTextView: TextView
    private lateinit var highScoreTextView: TextView
    private lateinit var gameOverText: TextView
    private val tiles = Array(4) { Array<TextView?>(4) { null } }
    private var lastMoveTime = 0L
    private val MOVE_DELAY = 150L // Minimum time between moves in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupTouchHandling()
        setupObservers()
        setupNewGameButton()
    }

    private fun initializeViews() {
        gameGrid = findViewById(R.id.gameGrid)
        scoreTextView = findViewById(R.id.scoreTextView)
        highScoreTextView = findViewById(R.id.highScoreTextView)
        gameOverText = findViewById(R.id.gameOverText)

        gameGrid.post {
            setupGrid()
        }
    }

    private fun setupGrid() {
        gameGrid.removeAllViews()
        val gridWidth = gameGrid.width
        val marginSize = 8.dp
        val cellSize = (gridWidth - (marginSize * 5)) / 4

        for (i in 0 until 4) {
            for (j in 0 until 4) {
                val tileView = layoutInflater.inflate(R.layout.tile_layout, gameGrid, false) as ViewGroup
                val tileText = tileView.findViewById<TextView>(R.id.tileValue)
                
                val params = GridLayout.LayoutParams()
                params.width = cellSize
                params.height = cellSize
                params.rowSpec = GridLayout.spec(i)
                params.columnSpec = GridLayout.spec(j)
                params.setMargins(4.dp, 4.dp, 4.dp, 4.dp)
                
                tileView.layoutParams = params
                tiles[i][j] = tileText
                gameGrid.addView(tileView)
            }
        }

        viewModel.grid.value?.let { updateGrid(it) }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private fun setupTouchHandling() {
        var startX = 0f
        var startY = 0f
        val minDistance = 50.dp.toFloat()

        gameGrid.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastMoveTime >= MOVE_DELAY) {
                        val dx = event.x - startX
                        val dy = event.y - startY

                        if (abs(dx) > minDistance || abs(dy) > minDistance) {
                            if (abs(dx) > abs(dy)) {
                                if (dx > 0) {
                                    viewModel.move(GameManager.Direction.RIGHT)
                                } else {
                                    viewModel.move(GameManager.Direction.LEFT)
                                }
                            } else {
                                if (dy > 0) {
                                    viewModel.move(GameManager.Direction.DOWN)
                                } else {
                                    viewModel.move(GameManager.Direction.UP)
                                }
                            }
                            lastMoveTime = currentTime
                        }
                    }
                    true
                }
                else -> true
            }
        }
    }

    private fun setupObservers() {
        viewModel.grid.observe(this) { grid ->
            updateGrid(grid)
        }

        viewModel.score.observe(this) { score ->
            scoreTextView.text = score.toString()
        }

        viewModel.highScore.observe(this) { highScore ->
            highScoreTextView.text = highScore.toString()
        }

        viewModel.gameOver.observe(this) { isGameOver ->
            gameOverText.visibility = if (isGameOver) View.VISIBLE else View.GONE
        }
    }

    private fun setupNewGameButton() {
        findViewById<View>(R.id.newGameButton).setOnClickListener {
            viewModel.startNewGame()
            gameOverText.visibility = View.GONE
        }
    }

    private fun updateGrid(grid: Array<Array<Int>>) {
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                val value = grid[i][j]
                val tile = tiles[i][j]
                
                if (value == 0) {
                    tile?.text = ""
                    tile?.setBackgroundResource(R.drawable.empty_tile_background)
                } else {
                    tile?.text = value.toString()
                    updateTileAppearance(tile, value)
                }
            }
        }
    }

    private fun updateTileAppearance(tile: TextView?, value: Int) {
        val colorResId = when (value) {
            2 -> R.color.tile_2
            4 -> R.color.tile_4
            8 -> R.color.tile_8
            16 -> R.color.tile_16
            32 -> R.color.tile_32
            64 -> R.color.tile_64
            128 -> R.color.tile_128
            256 -> R.color.tile_256
            512 -> R.color.tile_512
            1024 -> R.color.tile_1024
            2048 -> R.color.tile_2048
            else -> R.color.tile_2048
        }

        tile?.setBackgroundColor(ContextCompat.getColor(this, colorResId))
        tile?.setTextColor(ContextCompat.getColor(this, 
            if (value <= 4) R.color.tile_text_light else R.color.tile_text_dark))
    }
}