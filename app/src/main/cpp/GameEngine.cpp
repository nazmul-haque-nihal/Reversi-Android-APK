#include "GameEngine.h"
#include <algorithm>
#include <cstring>

GameEngine::GameEngine() : currentPlayer(BLACK), blackScore(0), whiteScore(0), historyIndex(-1) {
    initializeBoard();
}

GameEngine::~GameEngine() {
    history.clear();
}

void GameEngine::initializeBoard() {
    // Clear board
    for (int i = 0; i < 8; i++) {
        for (int j = 0; j < 8; j++) {
            board[i][j] = EMPTY;
        }
    }
    
    // Set starting position (center 4 cells)
    board[3][3] = WHITE;
    board[3][4] = BLACK;
    board[4][3] = BLACK;
    board[4][4] = WHITE;
    
    currentPlayer = BLACK;
    updateScores();
    
    // Clear history
    history.clear();
    historyIndex = -1;
    saveState();
}

bool GameEngine::isValidMove(int row, int col, int player) {
    // Check bounds and if cell is empty
    if (row < 0 || row >= 8 || col < 0 || col >= 8) return false;
    if (board[row][col] != EMPTY) return false;
    
    // Check if this move would flip any pieces
    auto flippable = getFlippablePieces(row, col, player);
    return !flippable.empty();
}

std::vector<std::pair<int, int>> GameEngine::getFlippablePieces(int row, int col, int player) {
    std::vector<std::pair<int, int>> flippable;
    int opponent = (player == BLACK) ? WHITE : BLACK;
    
    // Check all 8 directions
    for (int d = 0; d < 8; d++) {
        int dr = DIRECTIONS[d][0];
        int dc = DIRECTIONS[d][1];
        
        std::vector<std::pair<int, int>> potentialFlips;
        int r = row + dr;
        int c = col + dc;
        
        // Traverse in this direction
        while (r >= 0 && r < 8 && c >= 0 && c < 8) {
            if (board[r][c] == opponent) {
                potentialFlips.push_back({r, c});
            } else if (board[r][c] == player) {
                // Found a piece of our own, so we can flip the potential pieces
                if (!potentialFlips.empty()) {
                    flippable.insert(flippable.end(), potentialFlips.begin(), potentialFlips.end());
                }
                break;
            } else {
                // Empty cell, no flip possible in this direction
                break;
            }
            r += dr;
            c += dc;
        }
    }
    
    return flippable;
}

void GameEngine::flipPieces(const std::vector<std::pair<int, int>>& pieces) {
    for (const auto& piece : pieces) {
        board[piece.first][piece.second] = currentPlayer;
    }
}

void GameEngine::updateScores() {
    blackScore = 0;
    whiteScore = 0;
    for (int i = 0; i < 8; i++) {
        for (int j = 0; j < 8; j++) {
            if (board[i][j] == BLACK) blackScore++;
            else if (board[i][j] == WHITE) whiteScore++;
        }
    }
}

bool GameEngine::hasValidMoves(int player) {
    for (int i = 0; i < 8; i++) {
        for (int j = 0; j < 8; j++) {
            if (isValidMove(i, j, player)) {
                return true;
            }
        }
    }
    return false;
}

void GameEngine::initGame() {
    initializeBoard();
}

bool GameEngine::makeMove(int row, int col, int player) {
    if (!isValidMove(row, col, player)) {
        return false;
    }
    
    // Save state before making the move
    saveState();
    
    // Place the piece
    board[row][col] = player;
    
    // Flip opponent pieces
    auto flippable = getFlippablePieces(row, col, player);
    flipPieces(flippable);
    
    // Update scores
    updateScores();
    
    // Switch player
    currentPlayer = (player == BLACK) ? WHITE : BLACK;
    
    return true;
}

bool GameEngine::canMove(int row, int col, int player) {
    return isValidMove(row, col, player);
}

bool GameEngine::playerCanMove(int player) {
    return hasValidMoves(player);
}

void GameEngine::passTurn() {
    saveState();
    currentPlayer = (currentPlayer == BLACK) ? WHITE : BLACK;
}

void GameEngine::getBoardState(int* boardOut) {
    std::memcpy(boardOut, board, 64 * sizeof(int));
}

void GameEngine::getScores(int* blackScoreOut, int* whiteScoreOut) {
    *blackScoreOut = blackScore;
    *whiteScoreOut = whiteScore;
}

int GameEngine::getCurrentPlayer() {
    return currentPlayer;
}

void GameEngine::setCurrentPlayer(int player) {
    currentPlayer = player;
}

void GameEngine::saveState() {
    // Remove any states after current index (for redo)
    if (historyIndex < (int)history.size() - 1) {
        history.resize(historyIndex + 1);
    }
    
    GameState state;
    std::memcpy(state.board, board, 64 * sizeof(int));
    state.currentPlayer = currentPlayer;
    state.blackScore = blackScore;
    state.whiteScore = whiteScore;
    
    history.push_back(state);
    historyIndex++;
}

bool GameEngine::undo() {
    if (historyIndex <= 0) return false;
    
    historyIndex--;
    GameState& state = history[historyIndex];
    std::memcpy(board, state.board, 64 * sizeof(int));
    currentPlayer = state.currentPlayer;
    blackScore = state.blackScore;
    whiteScore = state.whiteScore;
    
    return true;
}

bool GameEngine::redo() {
    if (historyIndex >= (int)history.size() - 1) return false;
    
    historyIndex++;
    GameState& state = history[historyIndex];
    std::memcpy(board, state.board, 64 * sizeof(int));
    currentPlayer = state.currentPlayer;
    blackScore = state.blackScore;
    whiteScore = state.whiteScore;
    
    return true;
}

bool GameEngine::canUndo() {
    return historyIndex > 0;
}

bool GameEngine::canRedo() {
    return historyIndex < (int)history.size() - 1;
}

bool GameEngine::isGameOver() {
    // Check if board is full
    bool boardFull = true;
    for (int i = 0; i < 8 && boardFull; i++) {
        for (int j = 0; j < 8; j++) {
            if (board[i][j] == EMPTY) {
                boardFull = false;
                break;
            }
        }
    }
    
    if (boardFull) return true;
    
    // Check if neither player can move
    return !hasValidMoves(BLACK) && !hasValidMoves(WHITE);
}

int GameEngine::getWinner() {
    if (!isGameOver()) return -1;
    
    if (blackScore > whiteScore) return BLACK;
    if (whiteScore > blackScore) return WHITE;
    return 0; // Draw
}

void GameEngine::getLastMove(int* row, int* col) {
    // Find the last move from history
    if (historyIndex > 0) {
        // The move was made to reach current state, so look at the difference
        // For simplicity, we'll track this separately if needed
        *row = -1;
        *col = -1;
    } else {
        *row = -1;
        *col = -1;
    }
}

std::vector<std::pair<int, int>> GameEngine::getValidMoves(int player) {
    std::vector<std::pair<int, int>> moves;
    for (int i = 0; i < 8; i++) {
        for (int j = 0; j < 8; j++) {
            if (isValidMove(i, j, player)) {
                moves.push_back({i, j});
            }
        }
    }
    return moves;
}
