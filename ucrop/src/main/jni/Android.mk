PROJECT := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := ucrop
LOCAL_CFLAGS    := -std=c++11 -fno-permissive -Wno-int-to-pointer-cast
LOCAL_SRC_FILES := $(PROJECT)/main.cpp

LOCAL_LDLIBS := -llog -landroid

include $(BUILD_SHARED_LIBRARY)