#include <jni.h>
#include <string>
#include "GameEngine.h"
#include "AI.h"

// Global references to Java objects
static JavaVM* javaVM = nullptr;
static jobject javaActivity = nullptr;
static GameEngine* gameEngine = nullptr;
static AI* ai = nullptr;

// JNI OnLoad - cache the JavaVM
JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    javaVM = vm;
    return JNI_VERSION_1_6;
}

// Helper function to get JNI environment
JNIEnv* getEnv() {
    JNIEnv* env;
    if (javaVM->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return nullptr;
    }
    return env;
}

// Helper function to attach thread and get JNI environment
JNIEnv* attachThread() {
    JNIEnv* env;
    if (javaVM->AttachCurrentThread(&env, nullptr) != JNI_OK) {
        return nullptr;
    }
    return env;
}

extern "C" {

// Initialize game engine
JNIEXPORT void JNICALL
Java_com_example_reversi_ReversiLib_initGame(JNIEnv* env, jobject thiz) {
    if (gameEngine != nullptr) {
        delete gameEngine;
    }
    gameEngine = new GameEngine();
    
    if (ai != nullptr) {
        delete ai;
    }
    ai = new AI(gameEngine);
    
    // Cache the activity reference
    javaActivity = env->NewGlobalRef(thiz);
}

// Reset game
JNIEXPORT void JNICALL
Java_com_example_reversi_ReversiLib_resetGame(JNIEnv* env, jobject thiz, jint gameMode, jint difficulty) {
    if (gameEngine != nullptr) {
        gameEngine->initGame();
        
        if (ai != nullptr) {
            ai->setDifficulty(static_cast<AIDifficulty>(difficulty));
        }
    }
}

// Make a player move
JNIEXPORT jboolean JNICALL
Java_com_example_reversi_ReversiLib_makeMove(JNIEnv* env, jobject thiz, jint row, jint col, jint player) {
    if (gameEngine == nullptr) return JNI_FALSE;
    return gameEngine->makeMove(row, col, player) ? JNI_TRUE : JNI_FALSE;
}

// Check if a move is valid
JNIEXPORT jboolean JNICALL
Java_com_example_reversi_ReversiLib_canMove(JNIEnv* env, jobject thiz, jint row, jint col, jint player) {
    if (gameEngine == nullptr) return JNI_FALSE;
    return gameEngine->canMove(row, col, player) ? JNI_TRUE : JNI_FALSE;
}

// Check if player can move
JNIEXPORT jboolean JNICALL
Java_com_example_reversi_ReversiLib_playerCanMove(JNIEnv* env, jobject thiz, jint player) {
    if (gameEngine == nullptr) return JNI_FALSE;
    return gameEngine->playerCanMove(player) ? JNI_TRUE : JNI_FALSE;
}

// Pass turn
JNIEXPORT void JNICALL
Java_com_example_reversi_ReversiLib_passTurn(JNIEnv* env, jobject thiz) {
    if (gameEngine != nullptr) {
        gameEngine->passTurn();
    }
}

// Get board state - returns flattened 64-element array
JNIEXPORT jintArray JNICALL
Java_com_example_reversi_ReversiLib_getBoardState(JNIEnv* env, jobject thiz) {
    if (gameEngine == nullptr) {
        jintArray result = env->NewIntArray(64);
        return result;
    }
    
    int board[64];
    gameEngine->getBoardState(board);
    
    jintArray result = env->NewIntArray(64);
    env->SetIntArrayRegion(result, 0, 64, board);
    return result;
}

// Get scores
JNIEXPORT jintArray JNICALL
Java_com_example_reversi_ReversiLib_getScores(JNIEnv* env, jobject thiz) {
    if (gameEngine == nullptr) {
        jintArray result = env->NewIntArray(2);
        return result;
    }
    
    int blackScore, whiteScore;
    gameEngine->getScores(&blackScore, &whiteScore);
    
    jintArray result = env->NewIntArray(2);
    jint scores[2] = {blackScore, whiteScore};
    env->SetIntArrayRegion(result, 0, 2, scores);
    return result;
}

// Get current player
JNIEXPORT jint JNICALL
Java_com_example_reversi_ReversiLib_getCurrentPlayer(JNIEnv* env, jobject thiz) {
    if (gameEngine == nullptr) return 0;
    return gameEngine->getCurrentPlayer();
}

// Set current player
JNIEXPORT void JNICALL
Java_com_example_reversi_ReversiLib_setCurrentPlayer(JNIEnv* env, jobject thiz, jint player) {
    if (gameEngine != nullptr) {
        gameEngine->setCurrentPlayer(player);
    }
}

// Undo move
JNIEXPORT jboolean JNICALL
Java_com_example_reversi_ReversiLib_undo(JNIEnv* env, jobject thiz) {
    if (gameEngine == nullptr) return JNI_FALSE;
    return gameEngine->undo() ? JNI_TRUE : JNI_FALSE;
}

// Redo move
JNIEXPORT jboolean JNICALL
Java_com_example_reversi_ReversiLib_redo(JNIEnv* env, jobject thiz) {
    if (gameEngine == nullptr) return JNI_FALSE;
    return gameEngine->redo() ? JNI_TRUE : JNI_FALSE;
}

// Can undo?
JNIEXPORT jboolean JNICALL
Java_com_example_reversi_ReversiLib_canUndo(JNIEnv* env, jobject thiz) {
    if (gameEngine == nullptr) return JNI_FALSE;
    return gameEngine->canUndo() ? JNI_TRUE : JNI_FALSE;
}

// Can redo?
JNIEXPORT jboolean JNICALL
Java_com_example_reversi_ReversiLib_canRedo(JNIEnv* env, jobject thiz) {
    if (gameEngine == nullptr) return JNI_FALSE;
    return gameEngine->canRedo() ? JNI_TRUE : JNI_FALSE;
}

// Is game over?
JNIEXPORT jboolean JNICALL
Java_com_example_reversi_ReversiLib_isGameOver(JNIEnv* env, jobject thiz) {
    if (gameEngine == nullptr) return JNI_FALSE;
    return gameEngine->isGameOver() ? JNI_TRUE : JNI_FALSE;
}

// Get winner
JNIEXPORT jint JNICALL
Java_com_example_reversi_ReversiLib_getWinner(JNIEnv* env, jobject thiz) {
    if (gameEngine == nullptr) return -1;
    return gameEngine->getWinner();
}

// Get AI move (returns int array with row and col)
JNIEXPORT jintArray JNICALL
Java_com_example_reversi_ReversiLib_getAIMove(JNIEnv* env, jobject thiz) {
    if (ai == nullptr || gameEngine == nullptr) {
        jintArray result = env->NewIntArray(2);
        jint init[2] = {-1, -1};
        env->SetIntArrayRegion(result, 0, 2, init);
        return result;
    }
    
    auto move = ai->getBestMove();
    jintArray result = env->NewIntArray(2);
    jint moveArray[2] = {move.first, move.second};
    env->SetIntArrayRegion(result, 0, 2, moveArray);
    return result;
}

// Get valid moves count for a player
JNIEXPORT jint JNICALL
Java_com_example_reversi_ReversiLib_getValidMovesCount(JNIEnv* env, jobject thiz, jint player) {
    if (gameEngine == nullptr) return 0;
    auto moves = gameEngine->getValidMoves(player);
    return static_cast<jint>(moves.size());
}

} // extern "C"
