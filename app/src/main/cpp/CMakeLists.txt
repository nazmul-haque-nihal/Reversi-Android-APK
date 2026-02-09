cmake_minimum_required(VERSION 3.22.1)

project("reversi")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find Android NDK libraries
find_library(log-lib log)

# Create the shared library
add_library(reversi-lib SHARED
    native-lib.cpp
    GameEngine.cpp
    AI.cpp
)

# Link libraries
target_link_libraries(reversi-lib
    ${log-lib}
    android
)
