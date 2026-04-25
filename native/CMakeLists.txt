# ============================================================================
# CMakeLists.txt for mdflib JNI bridge library
#
# This CMake script builds the mdflibjni shared library that serves as the
# JNI bridge between Java and the mdflib C++ native library.
#
# Build targets:
#   - mdflibjni.dll (Windows) / libmdflibjni.so (Linux)
#
# Dependencies:
#   - JDK (for jni.h header)
#   - mdflib static library
#   - zlib
#   - expat
#
# Usage:
#   cmake -B build -DJAVA_HOME=<path-to-jdk> -DCMAKE_TOOLCHAIN_FILE=<vcpkg-toolchain>
#   cmake --build build --config Release
# ============================================================================

cmake_minimum_required(VERSION 3.15)

# Project definition
project(mdflibjni VERSION 1.0.0 LANGUAGES CXX)

# C++17 standard required for modern C++ features used in mdflib
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ============================================================================
# Find JDK for JNI headers
#
# The JAVA_HOME environment variable or CMake variable must point to a
# valid JDK installation directory containing include/jni.h.
# ============================================================================
if(NOT DEFINED JAVA_HOME)
    if(DEFINED ENV{JAVA_HOME})
        set(JAVA_HOME $ENV{JAVA_HOME})
    else()
        message(FATAL_ERROR "JAVA_HOME is not set. Please set JAVA_HOME to your JDK installation directory.")
    endif()
endif()

message(STATUS "Using JAVA_HOME: ${JAVA_HOME}")

# JNI include directories (platform-specific)
set(JNI_INCLUDE_DIRS
    ${JAVA_HOME}/include
)

# Windows requires an additional platform-specific include directory
if(WIN32)
    list(APPEND JNI_INCLUDE_DIRS ${JAVA_HOME}/include/win32)
elseif(UNIX)
    list(APPEND JNI_INCLUDE_DIRS ${JAVA_HOME}/include/linux)
endif()

# ============================================================================
# Find required packages via vcpkg or system
# ============================================================================
find_package(ZLIB REQUIRED)
find_package(expat REQUIRED)

# ============================================================================
# Find mdflib library
#
# The mdflib library can be provided as:
# 1. A pre-built library via MDFLIB_DIR
# 2. Built from source as a subdirectory
# ============================================================================
if(DEFINED MDFLIB_DIR)
    # Use pre-built mdflib
    # MDFLIB_DIR should point to the mdflibrary build directory containing Release/ and Debug/
    find_library(MDFLIB_LIBRARY_RELEASE
        NAMES mdflibrary mdflibrarystatic
        PATHS ${MDFLIB_DIR}/Release ${MDFLIB_DIR}
        NO_DEFAULT_PATH
    )
    find_library(MDFLIB_LIBRARY_DEBUG
        NAMES mdflibraryd mdflibstaticd
        PATHS ${MDFLIB_DIR}/Debug ${MDFLIB_DIR}
        NO_DEFAULT_PATH
    )
    set(MDFLIB_INCLUDE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../mdflib_src/include" CACHE PATH "mdflib include directory")
    
    if(MDFLIB_LIBRARY_RELEASE OR MDFLIB_LIBRARY_DEBUG)
        message(STATUS "Found mdflib release: ${MDFLIB_LIBRARY_RELEASE}")
        message(STATUS "Found mdflib debug: ${MDFLIB_LIBRARY_DEBUG}")
    else()
        message(FATAL_ERROR "mdflib library not found in MDFLIB_DIR: ${MDFLIB_DIR}")
    endif()
else()
    # Build mdflib from source as subdirectory
    set(MDFLIB_SRC_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../mdflib_src" CACHE PATH "mdflib source root directory")
    if(EXISTS "${MDFLIB_SRC_DIR}/CMakeLists.txt")
        # Build the mdflib core library (not the shared library wrapper)
        add_subdirectory(${MDFLIB_SRC_DIR}/mdflib ${CMAKE_BINARY_DIR}/mdflib_core)
        
        # The mdflibrary subdirectory provides the C export shared library
        # We only need the static mdf library for JNI linking
        set(MDFLIB_LIBRARY mdf)
        set(MDFLIB_INCLUDE_DIR "${MDFLIB_SRC_DIR}/include" CACHE PATH "mdflib include directory")
    else()
        message(FATAL_ERROR "mdflib source not found at: ${MDFLIB_SRC_DIR}. Set MDFLIB_DIR or check MDFLIB_SRC_DIR.")
    endif()
endif()

# ============================================================================
# JNI bridge library source files
# ============================================================================
set(JNI_SOURCES
    src/mdflibjni.cpp
)

# ============================================================================
# Create shared library
#
# The shared library is named:
#   - mdflibjni.dll on Windows
#   - libmdflibjni.so on Linux
# ============================================================================
add_library(${PROJECT_NAME} SHARED ${JNI_SOURCES})

# Include directories for JNI headers, mdflib headers, and dependencies
target_include_directories(${PROJECT_NAME} PRIVATE
    ${JNI_INCLUDE_DIRS}
    ${MDFLIB_INCLUDE_DIR}
)

# Link against mdflib and its dependencies
if(TARGET mdf)
    target_link_libraries(${PROJECT_NAME} PRIVATE mdf)
elseif(TARGET mdflibrary)
    target_link_libraries(${PROJECT_NAME} PRIVATE mdflibrary)
elseif(MDFLIB_LIBRARY_RELEASE OR MDFLIB_LIBRARY_DEBUG)
    # Use pre-built mdflib with per-configuration selection
    target_link_libraries(${PROJECT_NAME} PRIVATE
        $<$<CONFIG:Release>:${MDFLIB_LIBRARY_RELEASE}>
        $<$<CONFIG:Debug>:${MDFLIB_LIBRARY_DEBUG}>
        $<$<CONFIG:RelWithDebInfo>:${MDFLIB_LIBRARY_RELEASE}>
        $<$<CONFIG:MinSizeRel>:${MDFLIB_LIBRARY_RELEASE}>
    )
else()
    target_link_libraries(${PROJECT_NAME} PRIVATE ${MDFLIB_LIBRARY})
endif()

target_link_libraries(${PROJECT_NAME} PRIVATE
    ${ZLIB_LIBRARIES}
    ${EXPAT_LIBRARIES}
)

# ============================================================================
# Platform-specific settings
# ============================================================================
if(WIN32)
    # Windows: Export all symbols for JNI functions
    # No __declspec(dllexport) prefix needed since we use JNI_EXPORT macro
    target_compile_definitions(${PROJECT_NAME} PRIVATE
        WIN32_LEAN_AND_MEAN
        NOMINMAX
    )
elseif(UNIX)
    # Linux: Enable visibility for shared library symbols
    target_compile_options(${PROJECT_NAME} PRIVATE -fvisibility=default)
endif()

# ============================================================================
# Output directory configuration
#
# Place built libraries in platform-specific directories for easy
# inclusion in the Java resources.
# ============================================================================
if(WIN32)
    set(PLATFORM_DIR "win32-x86-64")
elseif(UNIX AND CMAKE_SIZEOF_VOID_P EQUAL 8)
    set(PLATFORM_DIR "linux-x86-64")
else()
    set(PLATFORM_DIR "unknown")
endif()

set(NATIVE_RESOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../src/main/resources/native/${PLATFORM_DIR}")
message(STATUS "Native library output directory: ${NATIVE_RESOURCE_DIR}")

# Copy the built library to the Java resources directory after build
add_custom_command(TARGET ${PROJECT_NAME} POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        $<TARGET_FILE:${PROJECT_NAME}>
        ${NATIVE_RESOURCE_DIR}/$<TARGET_FILE_NAME:${PROJECT_NAME}>
    COMMENT "Copying ${PROJECT_NAME} to resources directory"
)

# Also copy dependency DLLs on Windows
if(WIN32)
    # Copy zlib and expat DLLs to the same resources directory
    add_custom_command(TARGET ${PROJECT_NAME} POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
            ${ZLIB_INCLUDE_DIR}/../bin/zlib1.dll
            ${NATIVE_RESOURCE_DIR}/zlib1.dll
        COMMENT "Copying zlib1.dll to resources directory"
    )
    add_custom_command(TARGET ${PROJECT_NAME} POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
            ${EXPAT_INCLUDE_DIR}/../bin/libexpat.dll
            ${NATIVE_RESOURCE_DIR}/libexpat.dll
        COMMENT "Copying libexpat.dll to resources directory"
    )
endif()

# ============================================================================
# Installation rules
# ============================================================================
install(TARGETS ${PROJECT_NAME}
    RUNTIME DESTINATION bin
    LIBRARY DESTINATION lib
    ARCHIVE DESTINATION lib
)
