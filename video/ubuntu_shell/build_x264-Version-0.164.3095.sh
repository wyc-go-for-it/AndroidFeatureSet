#!/bin/bash
export NDK=/home/wyc64/ndk/android-ndk-r21b
TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64
ANDROID_LIB_PATH="$(pwd)/android"
export API=21
 
function build_android
{
./configure \
    --prefix="$ANDROID_LIB_PATH/$CPU" \
      --disable-cli \
    --disable-shared \
    --enable-static \
    --enable-pic \
    --host=$my_host \
	--extra-ldflags="-lm"
      --cross-prefix=$CROSS_PREFIX \
    --sysroot=$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot \

make clean
make -j8
make install
}

#arm64-v8a
CPU=armv8-a
PREFIX=./android/arm64-v8a
my_host=aarch64-linux-android
export TARGET=aarch64-linux-android
export CC=$TOOLCHAIN/bin/$TARGET$API-clang
export CXX=$TOOLCHAIN/bin/$TARGET$API-clang++
CROSS_PREFIX=$TOOLCHAIN/bin/aarch64-linux-android-
build_android

#armeabi-v7a
CPU=armv7-a
PREFIX=./android/armeabi-v7a
my_host=armv7a-linux-android
export TARGET=armv7a-linux-androideabi
export CC=$TOOLCHAIN/bin/$TARGET$API-clang
export CXX=$TOOLCHAIN/bin/$TARGET$API-clang++
CROSS_PREFIX=$TOOLCHAIN/bin/arm-linux-androideabi-
build_android
