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
import nl.dionsegijn.konfetti.xml.KonfettiView
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Spread
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Size
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val viewModel: GameViewModel by viewModels()
    private lateinit var gameGrid: GridLayout
    private lateinit var scoreTextView: TextView
    private lateinit var highScoreTextView: TextView
    private lateinit var gameOverText: TextView
    private lateinit var konfettiView: KonfettiView
    private lateinit var titleTextView: TextView
    private lateinit var undoButton: android.widget.Button // Added
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
        setupUndoButton() // Added
    }

    private fun initializeViews() {
        gameGrid = findViewById(R.id.gameGrid)
        scoreTextView = findViewById(R.id.scoreTextView)
        highScoreTextView = findViewById(R.id.highScoreTextView)
        gameOverText = findViewById(R.id.gameOverText)
        konfettiView = findViewById(R.id.konfettiView)
        titleTextView = findViewById(R.id.titleTextView)
        undoButton = findViewById(R.id.undoButton) // Added

        setupTitleLongPress()

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

        viewModel.gameWon.observe(this) { isGameWon ->
            if (isGameWon) {
                celebrateWin()
            }
        }

        viewModel.canUndo.observe(this) { canUndo -> // Added
            undoButton.isEnabled = canUndo
        }
    }

    private fun setupNewGameButton() {
        findViewById<View>(R.id.newGameButton).setOnClickListener {
            viewModel.startNewGame()
            gameOverText.visibility = View.GONE
            // Consider disabling undo button here as well, though GameViewModel should handle it
        }
    }

    private fun setupUndoButton() { // Added
        undoButton.setOnClickListener {
            viewModel.undoMove()
        }
        undoButton.isEnabled = false // Initially disabled
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
        val tileBgAttr = when (value) {
            2 -> R.attr.colorTile2
            4 -> R.attr.colorTile4
            8 -> R.attr.colorTile8
            16 -> R.attr.colorTile16
            32 -> R.attr.colorTile32
            64 -> R.attr.colorTile64
            128 -> R.attr.colorTile128
            256 -> R.attr.colorTile256
            512 -> R.attr.colorTile512
            1024 -> R.attr.colorTile1024
            2048 -> R.attr.colorTile2048
            else -> R.attr.colorTile2048 // Default for higher values or unexpected
        }

        val textColorAttr = if (value <= 4) R.attr.colorTileTextLight else R.attr.colorTileTextDark
        // For dark theme, this logic might need to be more sophisticated if tile backgrounds change drastically
        // For example, if tile 8 becomes dark background in dark theme, it would need light text.
        // The themes.xml for -night currently points colorTileTextDark to R.color.text_dark (a light color on dark bg in original design)
        // and colorTileTextLight to R.color.dark_tile_text_light (a light color).
        // This means R.attr.colorTileTextDark should resolve to a color suitable for light backgrounds,
        // and R.attr.colorTileTextLight for dark backgrounds.
        // The current light theme has tile_text_light (dark text for 2,4) and tile_text_dark (light text for 8+).
        // The dark theme has dark_tile_text_light (light text) and text_dark (which is R.color.text_dark from light theme - #776E65).
        // This setup is a bit confusing. Let's assume:
        // - colorTileTextLight is for tiles that have a darker background (e.g. 2, 4 in light theme; potentially many in dark theme if we use dark backgrounds for tiles).
        // - colorTileTextDark is for tiles that have a lighter background (e.g. 8+ in light theme).

        val typedValue = android.util.TypedValue()

        theme.resolveAttribute(tileBgAttr, typedValue, true)
        val tileBgColor = typedValue.data
        tile?.setBackgroundColor(tileBgColor)

        theme.resolveAttribute(textColorAttr, typedValue, true)
        val textColor = typedValue.data
        tile?.setTextColor(textColor)
    }

    private fun setupTitleLongPress() {
        titleTextView.setOnLongClickListener { view ->
            var pressStartTime = System.currentTimeMillis()
            
            view.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        pressStartTime = System.currentTimeMillis()
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val pressDuration = System.currentTimeMillis() - pressStartTime
                        if (pressDuration >= 5000) { // 5 seconds
                            celebrateWin()
                        }
                        v.performClick()
                        false
                    }
                    else -> false
                }
            }
            true
        }
    }

    private fun celebrateWin() {
        // Create multiple party sources for full screen coverage
        val leftParty = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = Spread.WIDE,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            position = Position.Relative(0.0, 0.0),
            emitter = Emitter(duration = 2000, TimeUnit.MILLISECONDS).max(100),
            size = listOf(Size(40))
        )
        
        val centerParty = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = Spread.WIDE,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            position = Position.Relative(0.5, 0.0),
            emitter = Emitter(duration = 2000, TimeUnit.MILLISECONDS).max(100),
            size = listOf(Size(40))
        )
        
        val rightParty = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = Spread.WIDE,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            position = Position.Relative(1.0, 0.0),
            emitter = Emitter(duration = 2000, TimeUnit.MILLISECONDS).max(100),
            size = listOf(Size(40))
        )

        konfettiView.start(leftParty)
        konfettiView.start(centerParty)
        konfettiView.start(rightParty)
    }
}