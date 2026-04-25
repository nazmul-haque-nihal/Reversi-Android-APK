package com.example.reversi

import android.opengl.GLSurfaceView
import android.opengl.GLU
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * OpenGL ES 1.0 Renderer for the Reversi board
 */
class GameRenderer : GLSurfaceView.Renderer {
    
    // Board constants
    private val BOARD_SIZE = 8
    private val CELL_SIZE = 1.0f
    private val PIECE_RADIUS = 0.4f
    
    // Board colors
    private val COLOR_BOARD_DARK = floatArrayOf(0.0f, 0.39f, 0.0f, 1.0f)  // Forest green
    private val COLOR_BOARD_LIGHT = floatArrayOf(0.0f, 0.5f, 0.0f, 1.0f)
    private val COLOR_GRID = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
    private val COLOR_PIECE_BLACK = floatArrayOf(0.1f, 0.1f, 0.1f, 1.0f)
    private val COLOR_PIECE_WHITE = floatArrayOf(0.95f, 0.95f, 0.95f, 1.0f)
    private val COLOR_HIGHLIGHT_BLACK = floatArrayOf(0.0f, 1.0f, 1.0f, 0.8f)  // Black moves - neon cyan
    private val COLOR_HIGHLIGHT_WHITE = floatArrayOf(1.0f, 0.5f, 0.0f, 0.8f)  // White moves - neon orange
    private val COLOR_LAST_MOVE = floatArrayOf(1.0f, 0.0f, 0.0f, 0.7f)  // Red for last move
    
    // Game state
    private var boardState = IntArray(64) { 0 }
    private var validMovesBlack = mutableSetOf<Int>()
    private var validMovesWhite = mutableSetOf<Int>()
    private var lastMoveRow = -1
    private var lastMoveCol = -1

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Set clear color
        gl?.glClearColor(0.2f, 0.2f, 0.2f, 1.0f)
        
        // Enable vertex arrays
        gl?.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        gl?.glEnableClientState(GL10.GL_COLOR_ARRAY)
        
        // Enable blending for transparency
        gl?.glEnable(GL10.GL_BLEND)
        gl?.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        gl?.glViewport(0, 0, width, height)
        
        // Set up orthographic projection
        gl?.glMatrixMode(GL10.GL_PROJECTION)
        gl?.glLoadIdentity()
        
        // Calculate aspect ratio to keep board square
        val aspect = width.toFloat() / height.toFloat()
        val boardSize = BOARD_SIZE * CELL_SIZE.toDouble()
        
        if (aspect > 1.0) {
            // Landscape - wider than tall
            val viewWidth = boardSize * aspect
            GLU.gluOrtho2D(gl, (-viewWidth / 2).toFloat(), (viewWidth / 2).toFloat(), 
                          (-boardSize / 2).toFloat(), (boardSize / 2).toFloat())
        } else {
            // Portrait - taller than wide
            val viewHeight = boardSize / aspect
            GLU.gluOrtho2D(gl, (-boardSize / 2).toFloat(), (boardSize / 2).toFloat(),
                          (-viewHeight / 2).toFloat(), (viewHeight / 2).toFloat())
        }
        
        gl?.glMatrixMode(GL10.GL_MODELVIEW)
        gl?.glLoadIdentity()
    }
    
    override fun onDrawFrame(gl: GL10?) {
        gl?.glClear(GL10.GL_COLOR_BUFFER_BIT)
        
        // Draw the board
        drawBoard(gl)
        
        // Draw grid lines
        drawGrid(gl)
        
        // Draw valid move highlights
        drawHighlights(gl)
        
        // Draw last move indicator
        drawLastMoveMarker(gl)
        
        // Draw pieces
        drawPieces(gl)
    }
    
    private fun drawBoard(gl: GL10?) {
        val halfBoard = (BOARD_SIZE * CELL_SIZE) / 2
        
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                val left = col * CELL_SIZE - halfBoard
                val right = (col + 1) * CELL_SIZE - halfBoard
                val top = (BOARD_SIZE - 1 - row) * CELL_SIZE - halfBoard
                val bottom = (BOARD_SIZE - row) * CELL_SIZE - halfBoard
                
                // Alternate colors for checkerboard pattern
                val color = if ((row + col) % 2 == 0) COLOR_BOARD_DARK else COLOR_BOARD_LIGHT
                
                drawRectangle(gl, left, top, right, bottom, color)
            }
        }
    }
    
    private fun drawRectangle(gl: GL10?, left: Float, top: Float, right: Float, bottom: Float, color: FloatArray) {
        val vertices = floatArrayOf(
            left, top, 0.0f,
            right, top, 0.0f,
            left, bottom, 0.0f,
            right, bottom, 0.0f
        )

        val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)

        // Create color buffer for this rectangle
        val rectColors = FloatArray(4 * 4) // 4 vertices, 4 components each
        for (i in 0..3) {
            for (j in 0..3) {
                rectColors[i * 4 + j] = color[j]
            }
        }
        val localColorBuffer = ByteBuffer.allocateDirect(rectColors.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        localColorBuffer.put(rectColors)
        localColorBuffer.position(0)

        gl?.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer)
        gl?.glColorPointer(4, GL10.GL_FLOAT, 0, localColorBuffer)
        gl?.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4)
    }
    
    private fun drawGrid(gl: GL10?) {
        val halfBoard = (BOARD_SIZE * CELL_SIZE) / 2

        // Create color buffer for grid
        val gridColors = FloatArray(4 * 4) // 4 vertices, 4 components each
        for (i in 0..3) {
            for (j in 0..3) {
                gridColors[i * 4 + j] = COLOR_GRID[j]
            }
        }
        val localColorBuffer = ByteBuffer.allocateDirect(gridColors.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        localColorBuffer.put(gridColors)
        localColorBuffer.position(0)

        // Draw vertical lines
        for (i in 0..BOARD_SIZE) {
            val x = i * CELL_SIZE - halfBoard
            val vertices = floatArrayOf(
                x, -halfBoard, 0.0f,
                x, halfBoard, 0.0f
            )

            val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            vertexBuffer.put(vertices)
            vertexBuffer.position(0)

            gl?.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer)
            gl?.glColorPointer(4, GL10.GL_FLOAT, 0, localColorBuffer)
            gl?.glDrawArrays(GL10.GL_LINES, 0, 2)
        }

        // Draw horizontal lines
        for (i in 0..BOARD_SIZE) {
            val y = i * CELL_SIZE - halfBoard
            val vertices = floatArrayOf(
                -halfBoard, y, 0.0f,
                halfBoard, y, 0.0f
            )

            val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            vertexBuffer.put(vertices)
            vertexBuffer.position(0)

            gl?.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer)
            gl?.glDrawArrays(GL10.GL_LINES, 0, 2)
        }
    }
    
    private fun drawHighlights(gl: GL10?) {
        val halfBoard = (BOARD_SIZE * CELL_SIZE) / 2

        // Draw Black player's valid moves (blue circles)
        for (moveIndex in validMovesBlack) {
            val row = moveIndex / BOARD_SIZE
            val col = moveIndex % BOARD_SIZE

            val left = col * CELL_SIZE - halfBoard
            val right = (col + 1) * CELL_SIZE - halfBoard
            val top = (BOARD_SIZE - 1 - row) * CELL_SIZE - halfBoard
            val bottom = (BOARD_SIZE - row) * CELL_SIZE - halfBoard

            // Draw circle for black moves
            val centerX = (left + right) / 2
            val centerY = (top + bottom) / 2
            drawCircle(gl, centerX, centerY, PIECE_RADIUS, COLOR_HIGHLIGHT_BLACK)
        }

        // Draw White player's valid moves (orange circles)
        for (moveIndex in validMovesWhite) {
            val row = moveIndex / BOARD_SIZE
            val col = moveIndex % BOARD_SIZE

            val left = col * CELL_SIZE - halfBoard
            val right = (col + 1) * CELL_SIZE - halfBoard
            val top = (BOARD_SIZE - 1 - row) * CELL_SIZE - halfBoard
            val bottom = (BOARD_SIZE - row) * CELL_SIZE - halfBoard

            // Draw circle for white moves
            val centerX = (left + right) / 2
            val centerY = (top + bottom) / 2
            drawCircle(gl, centerX, centerY, PIECE_RADIUS, COLOR_HIGHLIGHT_WHITE)
        }
    }
    
    private fun drawLastMoveMarker(gl: GL10?) {
        if (lastMoveRow < 0 || lastMoveCol < 0) return
        
        val halfBoard = (BOARD_SIZE * CELL_SIZE) / 2
        val row = lastMoveRow
        val col = lastMoveCol
        
        val left = col * CELL_SIZE - halfBoard
        val right = (col + 1) * CELL_SIZE - halfBoard
        val top = (BOARD_SIZE - 1 - row) * CELL_SIZE - halfBoard
        val bottom = (BOARD_SIZE - row) * CELL_SIZE - halfBoard
        
        // Draw semi-transparent red marker
        drawRectangle(gl, left + 0.05f, top + 0.05f, right - 0.05f, bottom - 0.05f, COLOR_LAST_MOVE)
    }
    
    private fun drawPieces(gl: GL10?) {
        val halfBoard = (BOARD_SIZE * CELL_SIZE) / 2
        val centerOffset = CELL_SIZE / 2
        
        for (i in 0 until 64) {
            val row = i / BOARD_SIZE
            val col = i % BOARD_SIZE
            val piece = boardState[i]
            
            if (piece == Player.BLACK || piece == Player.WHITE) {
                val cx = col * CELL_SIZE + centerOffset - halfBoard
                val cy = (BOARD_SIZE - 1 - row) * CELL_SIZE + centerOffset - halfBoard
                
                val color = if (piece == Player.BLACK) COLOR_PIECE_BLACK else COLOR_PIECE_WHITE
                drawCircle(gl, cx, cy, PIECE_RADIUS * CELL_SIZE, color)
            }
        }
    }
    
    private fun drawCircle(gl: GL10?, cx: Float, cy: Float, radius: Float, color: FloatArray) {
        val segments = 32
        val vertices = FloatArray((segments + 2) * 3)

        // Center point
        vertices[0] = cx
        vertices[1] = cy
        vertices[2] = 0.0f

        // Circle points
        for (i in 0..segments) {
            val angle = (i.toFloat() / segments.toFloat()) * 2.0f * Math.PI.toFloat()
            vertices[(i + 1) * 3] = cx + radius * kotlin.math.cos(angle)
            vertices[(i + 1) * 3 + 1] = cy + radius * kotlin.math.sin(angle)
            vertices[(i + 1) * 3 + 2] = 0.0f
        }

        val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)

        // Create color buffer for this circle
        val circleColors = FloatArray((segments + 2) * 4)
        for (i in 0 until segments + 2) {
            for (j in 0..3) {
                circleColors[i * 4 + j] = color[j]
            }
        }
        val localColorBuffer = ByteBuffer.allocateDirect(circleColors.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        localColorBuffer.put(circleColors)
        localColorBuffer.position(0)

        gl?.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer)
        gl?.glColorPointer(4, GL10.GL_FLOAT, 0, localColorBuffer)
        gl?.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, segments + 2)
    }
    
    // Public methods to update game state
    fun updateBoardState(newState: IntArray) {
        boardState = newState.copyOf()
    }

    fun updateValidMoves(blackMoves: Set<Int>, whiteMoves: Set<Int>) {
        validMovesBlack = blackMoves.toMutableSet()
        validMovesWhite = whiteMoves.toMutableSet()
    }

    fun setLastMove(row: Int, col: Int) {
        lastMoveRow = row
        lastMoveCol = col
    }
}
