package com.example.reversi

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup

/**
 * Dialog for selecting game mode and AI difficulty
 */
class GameSetupDialog(context: Context, private val onGameStart: (gameMode: Int, difficulty: Int) -> Unit) : Dialog(context) {
    
    private lateinit var radioGroupMode: RadioGroup
    private lateinit var radioGroupDifficulty: RadioGroup
    private lateinit var radioEasy: RadioButton
    private lateinit var radioMedium: RadioButton
    private lateinit var radioHard: RadioButton
    private lateinit var radioExpert: RadioButton
    private lateinit var btnStart: Button
    private lateinit var btnCancel: Button
    
    private var selectedMode = GameMode.PLAYER_VS_PLAYER
    private var selectedDifficulty = AIDifficulty.MEDIUM
    
    init {
        setContentView(R.layout.dialog_game_setup)
        setupViews()
        setupListeners()
    }
    
    private fun setupViews() {
        radioGroupMode = findViewById(R.id.radioGroupMode)
        radioGroupDifficulty = findViewById(R.id.radioGroupDifficulty)
        radioEasy = findViewById(R.id.radioEasy)
        radioMedium = findViewById(R.id.radioMedium)
        radioHard = findViewById(R.id.radioHard)
        radioExpert = findViewById(R.id.radioExpert)
        btnStart = findViewById(R.id.btnStart)
        btnCancel = findViewById(R.id.btnCancel)
        
        // Set default selections
        radioMedium.isChecked = true
    }
    
    private fun setupListeners() {
        radioGroupMode.setOnCheckedChangeListener { _, checkedId ->
            selectedMode = when (checkedId) {
                R.id.radioPvP -> GameMode.PLAYER_VS_PLAYER
                R.id.radioPvAI -> GameMode.PLAYER_VS_AI
                else -> GameMode.PLAYER_VS_PLAYER
            }
            
            // Show/hide difficulty selection based on mode
            radioGroupDifficulty.visibility = if (selectedMode == GameMode.PLAYER_VS_AI) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        
        radioGroupDifficulty.setOnCheckedChangeListener { _, checkedId ->
            selectedDifficulty = when (checkedId) {
                R.id.radioEasy -> AIDifficulty.EASY
                R.id.radioMedium -> AIDifficulty.MEDIUM
                R.id.radioHard -> AIDifficulty.HARD
                R.id.radioExpert -> AIDifficulty.EXPERT
                else -> AIDifficulty.MEDIUM
            }
        }
        
        btnStart.setOnClickListener {
            onGameStart(selectedMode, selectedDifficulty)
            dismiss()
        }
        
        btnCancel.setOnClickListener {
            cancel()
        }
    }
}

/**
 * Dialog for settings (language selection)
 */
class SettingsDialog(context: Context, private val currentLanguage: String, 
                     private val onSettingsChanged: (language: String) -> Unit) : Dialog(context) {
    
    private lateinit var radioGroupLanguage: RadioGroup
    private lateinit var btnOk: Button
    
    private var selectedLanguage = currentLanguage
    
    init {
        setContentView(R.layout.dialog_settings)
        setupViews()
        setupListeners()
    }
    
    private fun setupViews() {
        radioGroupLanguage = findViewById(R.id.radioGroupLanguage)
        btnOk = findViewById(R.id.btnOk)
        
        // Set current selection
        when (currentLanguage) {
            "ja" -> radioGroupLanguage.check(R.id.radioJapanese)
            else -> radioGroupLanguage.check(R.id.radioEnglish)
        }
    }
    
    private fun setupListeners() {
        radioGroupLanguage.setOnCheckedChangeListener { _, checkedId ->
            selectedLanguage = when (checkedId) {
                R.id.radioEnglish -> "en"
                R.id.radioJapanese -> "ja"
                else -> "en"
            }
        }
        
        btnOk.setOnClickListener {
            onSettingsChanged(selectedLanguage)
            dismiss()
        }
    }
}

/**
 * Dialog for showing game rules
 */
class RulesDialog(context: Context) : Dialog(context) {
    
    private lateinit var btnClose: Button
    
    init {
        setContentView(R.layout.dialog_rules)
        setupViews()
        setupListeners()
    }
    
    private fun setupViews() {
        btnClose = findViewById(R.id.btnClose)
    }
    
    private fun setupListeners() {
        btnClose.setOnClickListener {
            dismiss()
        }
    }
}

/**
 * Dialog for game over announcement
 */
class GameOverDialog(context: Context, private val winner: Int, 
                     private val blackScore: Int, private val whiteScore: Int,
                     private val onNewGame: () -> Unit) : Dialog(context) {
    
    private lateinit var tvResult: android.widget.TextView
    private lateinit var btnNewGame: Button
    private lateinit var btnClose: Button
    
    init {
        setContentView(R.layout.dialog_game_over)
        setupViews()
        setupListeners()
    }
    
    private fun setupViews() {
        tvResult = findViewById(R.id.tvResult)
        btnNewGame = findViewById(R.id.btnNewGame)
        btnClose = findViewById(R.id.btnClose)
        
        // Display result
        val resultText = when (winner) {
            1 -> context.getString(R.string.winner_black, blackScore, whiteScore)
            2 -> context.getString(R.string.winner_white, blackScore, whiteScore)
            0 -> context.getString(R.string.game_draw, blackScore, whiteScore)
            else -> context.getString(R.string.game_over)
        }
        tvResult.text = resultText
    }
    
    private fun setupListeners() {
        btnNewGame.setOnClickListener {
            onNewGame()
            dismiss()
        }
        
        btnClose.setOnClickListener {
            dismiss()
        }
    }
}
