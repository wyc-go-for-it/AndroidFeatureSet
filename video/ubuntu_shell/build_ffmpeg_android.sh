#!/bin/bash
#clear before compilation
make clean
#NDK path
export NDK=/home/wyc64/ndk/android-ndk-r21b
#compile toolchain
TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64
function build_android
{
#print
echo "Compiling FFmpeg for $CPU"
#invoke configure file

#export PKG_CONFIG_PATH="$X264_LIB/pkgconfig"
#export PKG_CONFIG_PATH="$FDK_LIB/pkgconfig":$PKG_CONFIG_PATH
#echo "PKG_CONFIG_PATH="$PKG_CONFIG_PATH
#pkg-config --list-all

./configure \
--prefix=$PREFIX \
--disable-debug \
--disable-doc \
--disable-static \
--enable-shared \
--enable-gpl \
--enable-version3 \
--enable-nonfree \
--disable-programs \
--disable-ffmpeg \
--disable-ffplay \
--disable-ffprobe \
--disable-decoders \
--enable-decoder=aac \
--enable-decoder=ac3 \
--enable-decoder=h264 \
--enable-decoder=hevc \
--enable-decoder=libfdk_aac \
--disable-encoders \
--enable-encoder=aac \
--enable-encoder=ac3 \
--enable-libx264 \
--enable-libfdk-aac \
--enable-encoder=libx264 \
--enable-encoder=libfdk_aac \
--disable-filters \
--disable-avdevice \
--disable-parsers \
--enable-parser=aac \
--enable-parser=ac3 \
--enable-parser=h264 \
--enable-parser=hevc \
--disable-muxers  \
--enable-muxer=mp4 \
--disable-demuxers \
--enable-demuxer=mov \
--disable-bsfs \
--disable-indevs \
--disable-outdevs \
--disable-small \
--disable-postproc \
--disable-protocols \
--enable-protocol=file \
--enable-protocol=udp \
--enable-protocol=http \
--enable-protocol=rtmp \
--enable-protocol=tcp \
--cross-prefix=$CROSS_PREFIX \
--target-os=android \
--arch=$ARCH \
--cpu=$CPU \
--cc=$CC \
--cxx=$CXX \
--enable-cross-compile \
--sysroot=$SYSROOT \
--extra-cflags="-I$X264_INCLUDE -I$FDK_INCLUDE -Os -fpic $OPTIMIZE_CFLAGS" \
--extra-ldflags="-lm -L$X264_LIB -L$FDK_LIB $OPTIMIZE_CFLAGS"

make clean
make -j16
make install

echo "---------------- build android " $ARCH " success------------------------"

}


#arm64-v8a setting
ARCH=arm64
CPU=armv8-a
API=21
CC=$TOOLCHAIN/bin/aarch64-linux-android$API-clang
CXX=$TOOLCHAIN/bin/aarch64-linux-android$API-clang++
#NDK header file
SYSROOT=$TOOLCHAIN/sysroot
CROSS_PREFIX=$TOOLCHAIN/bin/aarch64-linux-android-
#so file output path
PREFIX=$(pwd)/android/$CPU
OPTIMIZE_CFLAGS="-march=$CPU"

BASE_PATH=/home/wyc64/ffmpeg_sources
LIB_TARGET_ABI=armv8-a

#指定 fdk-aac 的头文件和静态库目录
FDK_INCLUDE=$BASE_PATH/fdk-aac/android/$LIB_TARGET_ABI/include
FDK_LIB=$BASE_PATH/fdk-aac/android/$LIB_TARGET_ABI/lib

#指定 x264 的头文件和静态库目录
X264_INCLUDE=$BASE_PATH/x264/android/$LIB_TARGET_ABI/include
X264_LIB=$BASE_PATH/x264/android/$LIB_TARGET_ABI/lib

build_android


#cross compile  relationship 
# armv8a -> arm64 -> aarch64-linux-android-
# armv7a -> arm -> arm-linux-androideabi-
# x86 -> x86 -> i686-linux-android-
# x86_64 -> x86_64 -> x86_64-linux-android-

#----- cpu architecture -----
#armv7-a
ARCH=arm
CPU=armv7-a
API=21
CC=$TOOLCHAIN/bin/armv7a-linux-androideabi$API-clang
CXX=$TOOLCHAIN/bin/armv7a-linux-androideabi$API-clang++
#NDK header file
SYSROOT=$TOOLCHAIN/sysroot
CROSS_PREFIX=$TOOLCHAIN/bin/arm-linux-androideabi-
#so file output path
PREFIX=$(pwd)/android/$CPU
OPTIMIZE_CFLAGS="-mfloat-abi=softfp -mfpu=vfp -marm -march=$CPU"

BASE_PATH=/home/wyc64/ffmpeg_sources
LIB_TARGET_ABI=armv7-a

#指定 fdk-aac 的头文件和静态库目录
FDK_INCLUDE=$BASE_PATH/fdk-aac/android/$LIB_TARGET_ABI/include
FDK_LIB=$BASE_PATH/fdk-aac/android/$LIB_TARGET_ABI/lib

#指定 x264 的头文件和静态库目录
X264_INCLUDE=$BASE_PATH/x264/android/$LIB_TARGET_ABI/include
X264_LIB=$BASE_PATH/x264/android/$LIB_TARGET_ABI/lib



build_android

#x86
ARCH=x86
CPU=x86
API=21
CC=$TOOLCHAIN/bin/i686-linux-android$API-clang
CXX=$TOOLCHAIN/bin/i686-linux-android$API-clang++
#NDK header file
SYSROOT=$TOOLCHAIN/sysroot
CROSS_PREFIX=$TOOLCHAIN/bin/i686-linux-android
#so file output path
PREFIX=$(pwd)/android/$CPU
OPTIMIZE_CFLAGS="-march=i686 -mtune=intel -mssse3 -mfpmath=sse -m32"

#build_android

#x86_64
ARCH=x86_64
CPU=x86_64
API=21
CC=$TOOLCHAIN/bin/x86_64-linux-android$API-clang
CXX=$TOOLCHAIN/bin/x86_64-linux-android$API-clang++
#NDK header file
SYSROOT=$TOOLCHAIN/sysroot
CROSS_PREFIX=$TOOLCHAIN/bin/x86_64-linux-android
#so file output path
PREFIX=$(pwd)/android/$CPU
OPTIMIZE_CFLAGS="-march=$CPU -mtune=intel -mssse4.2 -mpopcnt -m64"

#build_android


