PROJECT := $(call my-dir)

include $(CLEAR_VARS)

#opencv
OPENCVROOT:= /Users/oleksii/Downloads/OpenCV-android-sdk30
OPENCV_CAMERA_MODULES:=off
OPENCV_INSTALL_MODULES:=on
OPENCV_LIB_TYPE:=SHARED
include ${OPENCVROOT}/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := ucrop
LOCAL_CFLAGS    := -std=c++11 -fno-permissive -Wno-int-to-pointer-cast
LOCAL_SRC_FILES := $(PROJECT)/main.cpp

LOCAL_LDLIBS := -llog -landroid

include $(BUILD_SHARED_LIBRARY)