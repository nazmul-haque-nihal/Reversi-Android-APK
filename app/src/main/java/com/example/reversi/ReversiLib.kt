package com.example.reversi

import android.content.Context

/**
 * JNI wrapper class for the native C++ game engine
 */
class ReversiLib(context: Context) {
    
    companion object {
        private var isLoaded = false
        
        fun loadLibrary() {
            if (!isLoaded) {
                System.loadLibrary("reversi-lib")
                isLoaded = true
            }
        }
    }
    
    private val context: Context = context.applicationContext
    
    init {
        loadLibrary()
    }
    
    /**
     * Initialize the game engine
     */
    external fun initGame()
    
    /**
     * Reset the game
     * @param gameMode 0 = PvP, 1 = PvAI
     * @param difficulty 0 = Easy, 1 = Medium, 2 = Hard, 3 = Expert
     */
    external fun resetGame(gameMode: Int, difficulty: Int)
    
    /**
     * Make a player move
     * @param row Row index (0-7)
     * @param col Column index (0-7)
     * @param player 1 = Black, 2 = White
     * @return true if move was successful
     */
    external fun makeMove(row: Int, col: Int, player: Int): Boolean
    
    /**
     * Check if a move is valid
     */
    external fun canMove(row: Int, col: Int, player: Int): Boolean
    
    /**
     * Check if player can make any move
     */
    external fun playerCanMove(player: Int): Boolean
    
    /**
     * Pass turn
     */
    external fun passTurn()
    
    /**
     * Get board state
     * @return IntArray of size 64 (0=empty, 1=black, 2=white)
     */
    external fun getBoardState(): IntArray
    
    /**
     * Get scores
     * @return IntArray [blackScore, whiteScore]
     */
    external fun getScores(): IntArray
    
    /**
     * Get current player
     * @return 1 = Black, 2 = White
     */
    external fun getCurrentPlayer(): Int
    
    /**
     * Set current player
     */
    external fun setCurrentPlayer(player: Int)
    
    /**
     * Undo last move
     */
    external fun undo(): Boolean
    
    /**
     * Redo last undone move
     */
    external fun redo(): Boolean
    
    /**
     * Can undo?
     */
    external fun canUndo(): Boolean
    
    /**
     * Can redo?
     */
    external fun canRedo(): Boolean
    
    /**
     * Is game over?
     */
    external fun isGameOver(): Boolean
    
    /**
     * Get winner
     * @return -1 = no winner yet, 0 = draw, 1 = black, 2 = white
     */
    external fun getWinner(): Int
    
    /**
     * Get AI move
     * @return IntArray [row, col]
     */
    external fun getAIMove(): IntArray
    
    /**
     * Get valid moves count for a player
     */
    external fun getValidMovesCount(player: Int): Int
}

// Game mode constants
object GameMode {
    const val PLAYER_VS_PLAYER = 0
    const val PLAYER_VS_AI = 1
}

// AI difficulty constants
object AIDifficulty {
    const val EASY = 0
    const val MEDIUM = 1
    const val HARD = 2
    const val EXPERT = 3
}

// Player constants
object Player {
    const val EMPTY = 0
    const val BLACK = 1
    const val WHITE = 2
}
