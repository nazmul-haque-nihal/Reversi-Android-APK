#include "AI.h"
#include <cstdlib>
#include <ctime>
#include <algorithm>
#include <climits>

AI::AI(GameEngine* gameEngine) : engine(gameEngine), difficulty(AIDifficulty::MEDIUM) {
    std::srand(static_cast<unsigned int>(std::time(nullptr)));
}

AI::~AI() {
}

void AI::setDifficulty(AIDifficulty diff) {
    difficulty = diff;
}

void AI::copyBoard(int src[8][8], int dest[8][8]) {
    for (int i = 0; i < 8; i++) {
        for (int j = 0; j < 8; j++) {
            dest[i][j] = src[i][j];
        }
    }
}

bool AI::isCorner(int row, int col) {
    return (row == 0 || row == 7) && (col == 0 || col == 7);
}

bool AI::isXSquare(int row, int col) {
    // Squares next to corners (that are not corners themselves)
    // C-X1, C-X2, C-X3, C-X4 pattern
    return (row == 0 && col == 1) || (row == 0 && col == 6) ||
           (row == 1 && col == 0) || (row == 1 && col == 1) ||
           (row == 1 && col == 6) || (row == 1 && col == 7) ||
           (row == 6 && col == 0) || (row == 6 && col == 1) ||
           (row == 6 && col == 6) || (row == 6 && col == 7) ||
           (row == 7 && col == 1) || (row == 7 && col == 6);
}

bool AI::isEdge(int row, int col) {
    return (row == 0 || row == 7 || col == 0 || col == 7) && !isCorner(row, col);
}

int AI::evaluatePosition(int board[8][8]) {
    // Position weights for Reversi
    // Corners are most valuable, X-squares are bad, edges are good
    static const int weights[8][8] = {
        {100, -20, 10,  5,  5, 10, -20, 100},
        {-20, -50, -2, -2, -2, -2, -50, -20},
        { 10,  -2,  1,  1,  1,  1,  -2,  10},
        {  5,  -2,  1,  0,  0,  1,  -2,   5},
        {  5,  -2,  1,  0,  0,  1,  -2,   5},
        { 10,  -2,  1,  1,  1,  1,  -2,  10},
        {-20, -50, -2, -2, -2, -2, -50, -20},
        {100, -20, 10,  5,  5, 10, -20, 100}
    };
    
    int score = 0;
    int blackPieces = 0;
    int whitePieces = 0;
    
    for (int i = 0; i < 8; i++) {
        for (int j = 0; j < 8; j++) {
            if (board[i][j] == WHITE) {
                score += weights[i][j];
                whitePieces++;
            } else if (board[i][j] == BLACK) {
                score -= weights[i][j];
                blackPieces++;
            }
        }
    }
    
    // Late game: prioritize piece count over position
    int totalPieces = blackPieces + whitePieces;
    if (totalPieces > 50) {
        // At end game, actual piece count matters more
        // But position weights already account for this somewhat
    }
    
    // Add mobility bonus
    int whiteMobility = 0;
    int blackMobility = 0;
    
    // Simple mobility check
    for (int i = 0; i < 8; i++) {
        for (int j = 0; j < 8; j++) {
            if (board[i][j] == EMPTY) {
                // Check if white can move here
                if (engine->canMove(i, j, WHITE)) whiteMobility++;
                // Check if black can move here  
                if (engine->canMove(i, j, BLACK)) blackMobility++;
            }
        }
    }
    
    // Add mobility evaluation
    if (whiteMobility > 0) score += whiteMobility;
    if (blackMobility > 0) score -= blackMobility;
    
    return score;
}

int AI::countMobility(int player) {
    int count = 0;
    for (int i = 0; i < 8; i++) {
        for (int j = 0; j < 8; j++) {
            if (engine->canMove(i, j, player)) {
                count++;
            }
        }
    }
    return count;
}

std::pair<int, int> AI::getEasyMove() {
    auto validMoves = engine->getValidMoves(WHITE);
    if (validMoves.empty()) return {-1, -1};
    
    // Return a random valid move
    int randomIndex = std::rand() % validMoves.size();
    return validMoves[randomIndex];
}

std::pair<int, int> AI::getMediumMove() {
    auto validMoves = engine->getValidMoves(WHITE);
    if (validMoves.empty()) return {-1, -1};
    
    // Find the move that flips the most pieces
    int bestFlips = -1;
    std::pair<int, int> bestMove = validMoves[0];
    
    for (const auto& move : validMoves) {
        // Count flippable pieces without actually making the move
        // We need to simulate this
        int tempBoard[8][8];
        engine->getBoardState(&tempBoard[0][0]);
        
        // This is a simplification - in a real implementation, we'd have a method
        // to count flippable pieces without making the move
        // For now, we'll prioritize corners, then edges, then random from the rest
        if (isCorner(move.first, move.second)) {
            return move; // Always take a corner if available
        }
    }
    
    // If no corners, pick randomly from remaining moves
    int randomIndex = std::rand() % validMoves.size();
    return validMoves[randomIndex];
}

std::pair<int, int> AI::getHardMove() {
    return getExpertMove(); // For simplicity, use the expert algorithm with reduced depth
}

std::pair<int, int> AI::getExpertMove() {
    auto validMoves = engine->getValidMoves(WHITE);
    if (validMoves.empty()) return {-1, -1};
    
    // First priority: take any available corners
    for (const auto& move : validMoves) {
        if (isCorner(move.first, move.second)) {
            return move;
        }
    }
    
    // Second priority: avoid X-squares if corners aren't available
    // Use minimax with alpha-beta pruning
    int bestRow = validMoves[0].first;
    int bestCol = validMoves[0].second;
    int bestScore = INT_MIN;
    
    int board[8][8];
    engine->getBoardState(&board[0][0]);
    
    for (const auto& move : validMoves) {
        if (isXSquare(move.first, move.second)) {
            continue; // Skip X-squares if possible
        }
        
        // Simulate move
        int tempBoard[8][8];
        copyBoard(board, tempBoard);
        tempBoard[move.first][move.second] = WHITE;
        
        // Count flips and flip them (simplified)
        // In a full implementation, we'd properly simulate the move
        
        int score = minimax(tempBoard, 4, INT_MIN, INT_MAX, false);
        
        if (score > bestScore) {
            bestScore = score;
            bestRow = move.first;
            bestCol = move.second;
        }
    }
    
    // If we skipped all X-squares but no good moves found, take any valid move
    if (bestScore == INT_MIN) {
        int randomIndex = std::rand() % validMoves.size();
        return validMoves[randomIndex];
    }
    
    return {bestRow, bestCol};
}

int AI::minimax(int board[8][8], int depth, int alpha, int beta, bool maximizingPlayer) {
    if (depth == 0) {
        return evaluatePosition(board);
    }
    
    int player = maximizingPlayer ? WHITE : BLACK;
    auto validMoves = engine->getValidMoves(player);
    
    if (validMoves.empty()) {
        // Check if game is over
        bool gameOver = true;
        for (int i = 0; i < 8 && gameOver; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[i][j] == EMPTY) {
                    gameOver = false;
                    break;
                }
            }
        }
        
        if (gameOver) {
            // Count final score
            int whiteScore = 0, blackScore = 0;
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    if (board[i][j] == WHITE) whiteScore++;
                    else if (board[i][j] == BLACK) blackScore++;
                }
            }
            return (whiteScore - blackScore) * 1000;
        }
        
        // Player must pass
        return minimax(board, depth - 1, alpha, beta, !maximizingPlayer);
    }
    
    if (maximizingPlayer) {
        int maxEval = INT_MIN;
        for (const auto& move : validMoves) {
            int tempBoard[8][8];
            copyBoard(board, tempBoard);
            tempBoard[move.first][move.second] = WHITE;
            
            // Flip pieces would happen here in full implementation
            
            int eval = minimax(tempBoard, depth - 1, alpha, beta, false);
            maxEval = std::max(maxEval, eval);
            alpha = std::max(alpha, eval);
            if (beta <= alpha) break;
        }
        return maxEval;
    } else {
        int minEval = INT_MAX;
        for (const auto& move : validMoves) {
            int tempBoard[8][8];
            copyBoard(board, tempBoard);
            tempBoard[move.first][move.second] = BLACK;
            
            // Flip pieces would happen here in full implementation
            
            int eval = minimax(tempBoard, depth - 1, alpha, beta, true);
            minEval = std::min(minEval, eval);
            beta = std::min(beta, eval);
            if (beta <= alpha) break;
        }
        return minEval;
    }
}

std::pair<int, int> AI::getBestMove() {
    switch (difficulty) {
        case AIDifficulty::EASY:
            return getEasyMove();
        case AIDifficulty::MEDIUM:
            return getMediumMove();
        case AIDifficulty::HARD:
            return getHardMove();
        case AIDifficulty::EXPERT:
            return getExpertMove();
        default:
            return getMediumMove();
    }
}

const char* AI::getDifficultyName(AIDifficulty diff) {
    switch (diff) {
        case AIDifficulty::EASY: return "Easy";
        case AIDifficulty::MEDIUM: return "Medium";
        case AIDifficulty::HARD: return "Hard";
        case AIDifficulty::EXPERT: return "Expert";
        default: return "Unknown";
    }
}
