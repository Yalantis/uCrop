//
// Created by Oleksii Shliama on 3/13/16.
//

#include <jni.h>
#include "com_yalantis_ucrop_UCropActivity.h"

JNIEXPORT jstring JNICALL Java_com_yalantis_ucrop_UCropActivity_omfgCpp
  (JNIEnv * env, jobject obj){
    return env->NewStringUTF("Hello from JNI");
  }