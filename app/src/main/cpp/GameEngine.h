#ifndef REVERSI_GAMEENGINE_H
#define REVERSI_GAMEENGINE_H

#include <vector>
#include <string>

// Player constants
constexpr int EMPTY = 0;
constexpr int BLACK = 1;
constexpr int WHITE = 2;

// Game state structure for history
struct GameState {
    int board[8][8];
    int currentPlayer;
    int blackScore;
    int whiteScore;
    int lastMoveRow;
    int lastMoveCol;
};

// Direction vectors for move validation (8 directions)
const int DIRECTIONS[8][2] = {
    {-1, -1}, {-1, 0}, {-1, 1},
    {0, -1},          {0, 1},
    {1, -1},  {1, 0},  {1, 1}
};

class GameEngine {
private:
    int board[8][8];
    int currentPlayer;
    std::vector<GameState> history;
    int historyIndex;
    
    // Initialize the board with starting position
    void initializeBoard();
    
    // Check if a move is valid at (row, col)
    bool isValidMove(int row, int col, int player);
    
    // Get all pieces that would be flipped by a move
    std::vector<std::pair<int, int>> getFlippablePieces(int row, int col, int player);
    
    // Flip pieces on the board
    void flipPieces(const std::vector<std::pair<int, int>>& pieces);
    
    // Check if player has any valid moves
    bool hasValidMoves(int player);
    
    // Count scores
    void updateScores();
    
    int blackScore;
    int whiteScore;

public:
    GameEngine();
    ~GameEngine();
    
    // Initialize/Reset game
    void initGame();
    
    // Player move - returns true if move was successful
    bool makeMove(int row, int col, int player);
    
    // Check if a move is valid (for UI highlighting)
    bool canMove(int row, int col, int player);
    
    // Check if player can make any move
    bool playerCanMove(int player);
    
    // Pass turn
    void passTurn();
    
    // Get current board state (0=empty, 1=black, 2=white)
    void getBoardState(int* boardOut);
    
    // Get scores
    void getScores(int* blackScoreOut, int* whiteScoreOut);
    
    // Get current player
    int getCurrentPlayer();
    
    // Set current player
    void setCurrentPlayer(int player);
    
    // Undo last move
    bool undo();
    
    // Redo last undone move
    bool redo();
    
    // Can undo?
    bool canUndo();
    
    // Can redo?
    bool canRedo();
    
    // Check if game is over
    bool isGameOver();
    
    // Get winner (-1 = no winner yet, 0 = draw, 1 = black, 2 = white)
    int getWinner();
    
    // Save current state to history
    void saveState();
    
    // Get last move position
    void getLastMove(int* row, int* col);
    
    // Get all valid moves for a player
    std::vector<std::pair<int, int>> getValidMoves(int player);
};

#endif // REVERSI_GAMEENGINE_H
