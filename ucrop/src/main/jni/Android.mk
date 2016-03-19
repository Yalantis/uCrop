LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE:= libjpeg
LOCAL_SRC_FILES := libjpeg.a
include $(PREBUILT_STATIC_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE:= libpng
LOCAL_SRC_FILES := libpng.a
include $(PREBUILT_STATIC_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE    := ucrop
LOCAL_SRC_FILES := main.cpp

LOCAL_C_INCLUDES := $(LOCAL_PATH)
LOCAL_LDLIBS    := -llog -lz
LOCAL_STATIC_LIBRARIES := libpng libjpeg
LOCAL_C_INCLUDES := $(LOCAL_PATH)/libpng/ $(LOCAL_PATH)/jpeglib/
include $(BUILD_SHARED_LIBRARY)