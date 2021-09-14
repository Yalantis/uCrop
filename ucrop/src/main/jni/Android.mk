LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := ucrop
LOCAL_SRC_FILES := uCrop.cpp

LOCAL_LDLIBS    := -landroid -llog -lz
LOCAL_STATIC_LIBRARIES := libpng libjpeg

include $(BUILD_SHARED_LIBRARY)
$(call import-add-path, /Users/bradylarson/Development/uCrop/ucrop/src/main/jni)
$(call import-module,libpng)
$(call import-module,libjpeg)
