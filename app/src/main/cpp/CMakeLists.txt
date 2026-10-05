cmake_minimum_required(VERSION 3.22.1)
project(faric_projectm_android LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)
find_package(Git REQUIRED)

# Pin the spike to a stable libprojectM release. Do not build from moving master.
set(BUILD_SHARED_LIBS ON CACHE BOOL "" FORCE)
set(BUILD_TESTING OFF CACHE BOOL "" FORCE)
set(ENABLE_SDL_UI OFF CACHE BOOL "" FORCE)

FetchContent_Declare(
    projectm
    GIT_REPOSITORY https://github.com/projectM-visualizer/projectm.git
    GIT_TAG v4.1.7
    GIT_SHALLOW TRUE
    PATCH_COMMAND
        ${CMAKE_COMMAND} -E chdir <SOURCE_DIR>
        ${GIT_EXECUTABLE} apply --whitespace=nowarn
        "${CMAKE_CURRENT_LIST_DIR}/patches/projectm-4.1.7-frame-time.patch"
)

FetchContent_MakeAvailable(projectm)

add_library(
    faric_projectm
    SHARED
    projectm_jni.cpp
)

set_target_properties(faric_projectm PROPERTIES
    OUTPUT_NAME "faric_projectm"
    DEBUG_POSTFIX ""
    RELEASE_POSTFIX ""
)

if (DEFINED projectm_SOURCE_DIR)
    target_include_directories(faric_projectm PRIVATE
        "${projectm_SOURCE_DIR}/src/playlist/api"
    )
endif()

if (DEFINED projectm_BINARY_DIR)
    target_include_directories(faric_projectm PRIVATE
        "${projectm_BINARY_DIR}/src/api/include"
        "${projectm_BINARY_DIR}/src/playlist/include"
    )
endif()

find_library(log-lib log)
find_library(android-lib android)
find_library(glesv2-lib GLESv2)
find_library(egl-lib EGL)

target_link_libraries(
    faric_projectm
    PRIVATE
        ${log-lib}
        ${android-lib}
        ${glesv2-lib}
        ${egl-lib}
        libprojectM::playlist
        projectM
)
