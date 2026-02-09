#ifndef REVERSI_AI_H
#define REVERSI_AI_H

#include "GameEngine.h"
#include <vector>
#include <utility>

// AI Difficulty Levels
enum class AIDifficulty {
    EASY = 0,
    MEDIUM = 1,
    HARD = 2,
    EXPERT = 3
};

class AI {
private:
    GameEngine* engine;
    AIDifficulty difficulty;
    
    // Evaluate board position (positive = good for AI/white, negative = bad)
    int evaluatePosition(int board[8][8]);
    
    // Count mobility (number of valid moves)
    int countMobility(int player);
    
    // Check if a position is a corner
    bool isCorner(int row, int col);
    
    // Check if a position is next to a corner (X-square)
    bool isXSquare(int row, int col);
    
    // Check if a position is on an edge (but not corner)
    bool isEdge(int row, int col);
    
    // Get corner mobility (prioritize corners)
    int getCornerMobility(int board[8][8]);
    
    // Easy: Random valid move
    std::pair<int, int> getEasyMove();
    
    // Medium: Greedy - maximize immediate pieces flipped
    std::pair<int, int> getMediumMove();
    
    // Hard: Minimax with depth 3
    std::pair<int, int> getHardMove();
    
    // Expert: Minimax with Alpha-Beta pruning and depth 5-6
    std::pair<int, int> getExpertMove();
    
    // Minimax algorithm
    int minimax(int board[8][8], int depth, int alpha, int beta, bool maximizingPlayer);
    
    // Copy board state
    void copyBoard(int src[8][8], int dest[8][8]);

public:
    AI(GameEngine* gameEngine);
    ~AI();
    
    // Set AI difficulty
    void setDifficulty(AIDifficulty diff);
    
    // Get the best move for the AI (returns row, col)
    std::pair<int, int> getBestMove();
    
    // Get difficulty name
    static const char* getDifficultyName(AIDifficulty diff);
};

#endif // REVERSI_AI_H
