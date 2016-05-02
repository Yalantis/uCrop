APP_STL := gnustl_static
APP_ABI := armeabi-v7a x86 arm64-v8a
APP_CPPFLAGS += -frtti
APP_CPPFLAGS += -fexceptions
APP_CPPFLAGS += -DANDROID
APP_OPTIM := release
LOCAL_ARM_MODE := thumb