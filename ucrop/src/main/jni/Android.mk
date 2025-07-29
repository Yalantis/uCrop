LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := ucrop
LOCAL_SRC_FILES := uCrop.cpp

# Set the stack size to 16KB
LOCAL_LDFLAGS := -Wl,--stack=16384

LOCAL_LDLIBS    := -landroid -llog -lz
LOCAL_STATIC_LIBRARIES := libpng libjpeg_static

include $(BUILD_SHARED_LIBRARY)

$(call import-module,libpng)
$(call import-module,libjpeg)