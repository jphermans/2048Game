package org.jphsystems.a2048game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val gameManager = GameManager(application.applicationContext)
    
    private val _grid = MutableLiveData<Array<Array<Int>>>()
    val grid: LiveData<Array<Array<Int>>> = _grid
    
    private val _score = MutableLiveData<Int>()
    val score: LiveData<Int> = _score
    
    private val _highScore = MutableLiveData<Int>()
    val highScore: LiveData<Int> = _highScore
    
    private val _gameOver = MutableLiveData<Boolean>()
    val gameOver: LiveData<Boolean> = _gameOver
    
    private val _gameWon = MutableLiveData<Boolean>()
    val gameWon: LiveData<Boolean> = _gameWon

    init {
        setupGameCallbacks()
        startNewGame()
    }

    private fun setupGameCallbacks() {
        gameManager.onGridUpdated = { newGrid ->
            _grid.postValue(newGrid)
        }
        
        gameManager.onScoreUpdated = { newScore ->
            _score.postValue(newScore)
            _highScore.postValue(gameManager.getHighScore())
        }
        
        gameManager.onGameOver = {
            _gameOver.postValue(true)
        }
        
        gameManager.onGameWon = {
            _gameWon.postValue(true)
        }
    }

    fun startNewGame() {
        _gameOver.value = false
        _gameWon.value = false
        gameManager.initializeGame()
    }

    fun move(direction: GameManager.Direction) {
        gameManager.move(direction)
    }
}
