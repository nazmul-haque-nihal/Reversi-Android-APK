package com.example.reversi

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Main Activity for Reversi Game
 */
class MainActivity : Activity() {
    
    // UI Elements
    private lateinit var tvBlackScore: TextView
    private lateinit var tvWhiteScore: TextView
    private lateinit var tvTurnIndicator: TextView
    private lateinit var glSurfaceView: GameSurfaceView
    private lateinit var btnUndo: Button
    private lateinit var btnRedo: Button
    private lateinit var btnPass: Button
    private lateinit var btnNewGame: Button
    private lateinit var btnSettings: Button
    private lateinit var btnRules: Button
    
    // Game engine
    private lateinit var reversiLib: ReversiLib
    private lateinit var renderer: GameRenderer
    
    // Game state
    private var gameMode = GameMode.PLAYER_VS_PLAYER
    private var aiDifficulty = AIDifficulty.MEDIUM
    private var isProcessingMove = false
    private var currentLanguage = "en"
    
    // Thread for AI calculation
    private val aiExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Load saved language preference
        loadLanguagePreference()
        
        initializeViews()
        initializeGame()
        setupListeners()
    }
    
    private fun loadLanguagePreference() {
        val prefs = getSharedPreferences("reversi_prefs", MODE_PRIVATE)
        currentLanguage = prefs.getString("language", "en") ?: "en"
        updateLocale(currentLanguage)
    }
    
    private fun updateLocale(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
    
    private fun initializeViews() {
        tvBlackScore = findViewById(R.id.tvBlackScore)
        tvWhiteScore = findViewById(R.id.tvWhiteScore)
        tvTurnIndicator = findViewById(R.id.tvTurnIndicator)
        glSurfaceView = findViewById(R.id.glSurfaceView)
        btnUndo = findViewById(R.id.btnUndo)
        btnRedo = findViewById(R.id.btnRedo)
        btnPass = findViewById(R.id.btnPass)
        btnNewGame = findViewById(R.id.btnNewGame)
        btnSettings = findViewById(R.id.btnSettings)
        btnRules = findViewById(R.id.btnRules)
        
        // Setup OpenGL renderer
        renderer = GameRenderer()
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }
    
    private fun initializeGame() {
        reversiLib = ReversiLib(this)
        reversiLib.initGame()
        updateUI()
    }
    
    private fun setupListeners() {
        btnUndo.setOnClickListener {
            if (!isProcessingMove) {
                reversiLib.undo()
                updateUI()
                glSurfaceView.requestRender()
            }
        }
        
        btnRedo.setOnClickListener {
            if (!isProcessingMove) {
                reversiLib.redo()
                updateUI()
                glSurfaceView.requestRender()
            }
        }
        
        btnPass.setOnClickListener {
            if (!isProcessingMove) {
                reversiLib.passTurn()
                updateUI()
                glSurfaceView.requestRender()
                
                // Check if AI should make a move
                checkAITurn()
            }
        }
        
        btnNewGame.setOnClickListener {
            showGameSetupDialog()
        }
        
        btnSettings.setOnClickListener {
            showSettingsDialog()
        }
        
        btnRules.setOnClickListener {
            showRulesDialog()
        }

        // Set up game interaction listener for board touches
        glSurfaceView.setGameInteractionListener(object : GameSurfaceView.OnGameInteractionListener {
            override fun onCellClicked(row: Int, col: Int) {
                if (!isProcessingMove) {
                    handleBoardClick(row, col)
                }
            }

            override fun onPassClicked() {}
            override fun onUndoClicked() {}
            override fun onRedoClicked() {}
            override fun onNewGameClicked() {}
        })
    }
    
    private fun handleBoardClick(row: Int, col: Int) {
        val currentPlayer = reversiLib.getCurrentPlayer()

        // Check if it's a valid move
        if (reversiLib.canMove(row, col, currentPlayer)) {
            isProcessingMove = true

            if (reversiLib.makeMove(row, col, currentPlayer)) {
                updateUI()
                glSurfaceView.requestRender()

                // Check game over
                if (reversiLib.isGameOver()) {
                    showGameOverDialog()
                } else {
                    // Check if we need to pass
                    val nextPlayer = reversiLib.getCurrentPlayer()
                    if (!reversiLib.playerCanMove(nextPlayer)) {
                        reversiLib.passTurn()
                        updateUI()
                        glSurfaceView.requestRender()
                    }

                    // Check if AI should make a move
                    checkAITurn()
                }
            }

            isProcessingMove = false
        }
    }
    
    private fun checkAITurn() {
        if (gameMode == GameMode.PLAYER_VS_AI && 
            reversiLib.getCurrentPlayer() == Player.WHITE && 
            !reversiLib.isGameOver()) {
            
            // Disable buttons during AI thinking
            runOnUiThread {
                btnUndo.isEnabled = false
                btnRedo.isEnabled = false
                btnPass.isEnabled = false
                btnNewGame.isEnabled = false
            }
            
            aiExecutor.execute {
                // Small delay for better UX
                Thread.sleep(500)
                
                val aiMove = reversiLib.getAIMove()
                val row = aiMove[0]
                val col = aiMove[1]
                
                if (row >= 0 && col >= 0) {
                    reversiLib.makeMove(row, col, Player.WHITE)
                    
                    mainHandler.post {
                        updateUI()
                        glSurfaceView.requestRender()
                        
                        // Check game over
                        if (reversiLib.isGameOver()) {
                            showGameOverDialog()
                        } else {
                            // Check if player needs to pass
                            if (!reversiLib.playerCanMove(Player.BLACK)) {
                                reversiLib.passTurn()
                                updateUI()
                                glSurfaceView.requestRender()
                            }
                        }
                        
                        // Re-enable buttons
                        btnUndo.isEnabled = reversiLib.canUndo()
                        btnRedo.isEnabled = reversiLib.canRedo()
                        btnPass.isEnabled = reversiLib.playerCanMove(reversiLib.getCurrentPlayer())
                        btnNewGame.isEnabled = true
                    }
                }
            }
        }
    }
    
    private fun updateUI() {
        // Update scores
        val scores = reversiLib.getScores()
        tvBlackScore.text = getString(R.string.black_score_format, scores[0])
        tvWhiteScore.text = getString(R.string.white_score_format, scores[1])

        // Update turn indicator
        val currentPlayer = reversiLib.getCurrentPlayer()
        tvTurnIndicator.text = when (currentPlayer) {
            Player.BLACK -> getString(R.string.turn_black)
            Player.WHITE -> getString(R.string.turn_white)
            else -> getString(R.string.turn_unknown)
        }

        // Update button states
        btnUndo.isEnabled = reversiLib.canUndo() && !isProcessingMove
        btnRedo.isEnabled = reversiLib.canRedo() && !isProcessingMove
        btnPass.isEnabled = reversiLib.playerCanMove(currentPlayer) && !isProcessingMove

        // Update board display
        val boardState = reversiLib.getBoardState()
        renderer.updateBoardState(boardState)

        // Update valid move highlights for BOTH players
        val validMovesBlack = mutableSetOf<Int>()
        val validMovesWhite = mutableSetOf<Int>()

        // Get Black's valid moves
        if (reversiLib.playerCanMove(Player.BLACK)) {
            for (row in 0..7) {
                for (col in 0..7) {
                    if (reversiLib.canMove(row, col, Player.BLACK)) {
                        validMovesBlack.add(row * 8 + col)
                    }
                }
            }
        }

        // Get White's valid moves
        if (reversiLib.playerCanMove(Player.WHITE)) {
            for (row in 0..7) {
                for (col in 0..7) {
                    if (reversiLib.canMove(row, col, Player.WHITE)) {
                        validMovesWhite.add(row * 8 + col)
                    }
                }
            }
        }

        renderer.updateValidMoves(validMovesBlack, validMovesWhite)
    }
    
    private fun showGameSetupDialog() {
        val dialog = GameSetupDialog(this) { mode, difficulty ->
            gameMode = mode
            aiDifficulty = difficulty
            startNewGame()
        }
        dialog.show()
    }
    
    private fun startNewGame() {
        reversiLib.resetGame(gameMode, aiDifficulty)
        updateUI()
        glSurfaceView.requestRender()
    }
    
    private fun showSettingsDialog() {
        val dialog = SettingsDialog(this, currentLanguage) { language ->
            currentLanguage = language
            saveLanguagePreference(language)
            recreate()
        }
        dialog.show()
    }
    
    private fun saveLanguagePreference(language: String) {
        val prefs = getSharedPreferences("reversi_prefs", MODE_PRIVATE)
        prefs.edit().putString("language", language).apply()
    }
    
    private fun showRulesDialog() {
        val dialog = RulesDialog(this)
        dialog.show()
    }
    
    private fun showGameOverDialog() {
        val scores = reversiLib.getScores()
        val winner = reversiLib.getWinner()
        
        val dialog = GameOverDialog(this, winner, scores[0], scores[1]) {
            showGameSetupDialog()
        }
        dialog.show()
    }
    
    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        aiExecutor.shutdown()
    }
}
