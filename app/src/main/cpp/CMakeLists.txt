cmake_minimum_required(VERSION 3.18.1)

project("vectra_core" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(
    vectra_core
    SHARED
    vectra_engine.cpp
    jni_bridge.cpp
)

find_library(
    log-lib
    log
)

target_compile_options(
    vectra_core
    PRIVATE
    -O3
    -Wall
    -Wextra
    -ffunction-sections
    -fdata-sections
    -fvisibility=hidden
)

target_link_options(
    vectra_core
    PRIVATE
    -Wl,--gc-sections
)

target_link_libraries(
    vectra_core
    ${log-lib}
)
