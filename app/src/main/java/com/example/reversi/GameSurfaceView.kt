package com.example.reversi

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent

/**
 * Custom GLSurfaceView for Reversi board with touch handling
 */
class GameSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {
    
    interface OnGameInteractionListener {
        fun onCellClicked(row: Int, col: Int)
        fun onPassClicked()
        fun onUndoClicked()
        fun onRedoClicked()
        fun onNewGameClicked()
    }

    private var listener: OnGameInteractionListener? = null
    private var lastClickRow = -1
    private var lastClickCol = -1
    private var lastClickTime = 0L

    init {
        // Set OpenGL ES 1.0 context
        setEGLContextClientVersion(1)
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
    }
    
    fun setGameInteractionListener(listener: OnGameInteractionListener) {
        this.listener = listener
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP -> {
                val x = event.x
                val y = event.y
                val (row, col) = screenToBoard(x, y, width, height)
                
                // Debounce: Only click if it's a new cell or enough time has passed
                val currentTime = System.currentTimeMillis()
                val isSameCell = (row == lastClickRow && col == lastClickCol)
                val isDebounced = (currentTime - lastClickTime) > 300
                
                if (row >= 0 && col >= 0 && (!isSameCell || isDebounced)) {
                    listener?.onCellClicked(row, col)
                    lastClickRow = row
                    lastClickCol = col
                    lastClickTime = currentTime
                }
                return true
            }
        }
        return false
    }

    
    /**
     * Convert screen coordinates to board cell coordinates
     */
    fun screenToBoard(screenX: Float, screenY: Float, viewWidth: Int, viewHeight: Int): Pair<Int, Int> {
        // Calculate board dimensions in screen pixels
        val boardSizeInPixels: Int
        val boardOffsetX: Float
        val boardOffsetY: Float

        val aspect = viewWidth.toFloat() / viewHeight.toFloat()

        if (aspect > 1.0f) {
            // Landscape mode - width is limiting factor
            boardSizeInPixels = viewWidth
            boardOffsetX = 0f
            boardOffsetY = (viewHeight - viewWidth) / 2f
        } else {
            // Portrait mode - height is limiting factor
            boardSizeInPixels = viewHeight
            boardOffsetX = (viewWidth - viewHeight) / 2f
            boardOffsetY = 0f
        }

        // Adjust screen coordinates to board-relative coordinates
        val relativeX = screenX - boardOffsetX
        val relativeY = screenY - boardOffsetY

        // Check if touch is within board bounds
        if (relativeX < 0 || relativeX > boardSizeInPixels ||
            relativeY < 0 || relativeY > boardSizeInPixels) {
            return Pair(-1, -1)
        }

        // Convert to cell coordinates (board is 8x8)
        // Add small margin to ensure accurate cell detection
        val cellSizeInPixels = boardSizeInPixels / 8f
        val col = (relativeX / cellSizeInPixels).toInt()
        val row = (relativeY / cellSizeInPixels).toInt()

        // Clamp to valid range [0-7]
        val clampedRow = row.coerceIn(0, 7)
        val clampedCol = col.coerceIn(0, 7)

        return Pair(clampedRow, clampedCol)
    }
    
    companion object {
        private const val CELL_SIZE = 1.0f
    }
}
