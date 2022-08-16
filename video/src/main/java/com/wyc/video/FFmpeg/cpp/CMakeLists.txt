# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.10.2)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

set(jnilibs ${CMAKE_SOURCE_DIR}/../jniLibs)

set(libname FFmpegApi)

include_directories(../include)
include_directories(../include/libyuv)

link_directories(${jnilibs}/${CMAKE_ANDROID_ARCH_ABI})

#file(GLOB src-files ${CMAKE_SOURCE_DIR}/*.cpp)
aux_source_directory(./ src-files)

add_library( # Sets the name of the library.
        ${libname}

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${src-files}
        )

set(third-party-libs
        avformat
        avcodec
        avfilter
        swresample
        swscale
        avutil
        )

#[[add_library(avformat SHARED IMPORTED)
set_target_properties(avformat PROPERTIES IMPORTED_LOCATION ${jnilibs}/${ANDROID_ABI}/libavformat.so)

add_library(avcodec SHARED IMPORTED)
set_target_properties(avcodec PROPERTIES IMPORTED_LOCATION ${jnilibs}/${ANDROID_ABI}/libavcodec.so)]]

set(native-libs
        android
        EGL
        GLESv3
        OpenSLES
        log
        )

target_link_libraries( # Specifies the target library.
        ${libname}

        # Links the target library to the log library
        # included in the NDK.
        ${third-party-libs}
        ${native-libs}
        )


aux_source_directory(./utils utils-files)
set(yuvlib YUVUtils)
add_library( # Sets the name of the library.
        ${yuvlib}

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${utils-files}
        )
target_link_libraries(
        ${yuvlib}
        yuv
        log
)