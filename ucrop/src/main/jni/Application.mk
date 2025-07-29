APP_STL := gnustl_static
APP_ABI := armeabi armeabi-v7a x86 x86_64 arm64-v8a
APP_CPPFLAGS += -frtti
APP_CPPFLAGS += -fexceptions
APP_CPPFLAGS += -DANDROID
APP_PLATFORM := android-14
# Set the stack size to 16KB
APP_LDFLAGS += -Wl,--stack=16384