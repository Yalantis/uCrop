//
// Created by Oleksii Shliama on 3/13/16.
//

#include <stdio.h>
#include <jni.h>
#include <vector>
#include <android/log.h>
#include "com_yalantis_ucrop_view_CropImageView.h"

using namespace std;

#define cimg_display 0
#define cimg_use_jpeg
#define cimg_use_png

#include "CImg.h"

using namespace cimg_library;

#define LOG_TAG "UCrop"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#ifdef __cplusplus
extern "C" {
#endif

void throwJavaException(JNIEnv *env, const char *msg);

JNIEXPORT jboolean JNICALL Java_com_yalantis_ucrop_view_CropImageView_cropFileOpenCV(JNIEnv *env, jobject obj, jstring pathSource, jstring pathResult, jint left, jint top, jint width, jint height, jfloat angle) {

    const char *file_source_path = env->GetStringUTFChars(pathSource, 0);
    const char *file_result_path = env->GetStringUTFChars(pathResult, 0);

    LOGD("file_source_path: %s \n file_result_path: %s",file_source_path, file_result_path);

/*
    Mat src = imread(file_source_path, CV_LOAD_IMAGE_UNCHANGED);

    // get rotation matrix for rotating the image around its center
    Point2f center(src.cols/2.0, src.rows/2.0);
    Mat rot = getRotationMatrix2D(center, angle, 1.0);
    // determine bounding rectangle
    Rect bbox = RotatedRect(center,src.size(), angle).boundingRect();
    // adjust transformation matrix
    rot.at<double>(0,2) += bbox.width/2.0 - center.x;
    rot.at<double>(1,2) += bbox.height/2.0 - center.y;

    const int JPEG_QUALITY = 80;

    try {

    Mat dst;
    warpAffine(src, dst, rot, bbox.size());

    // Setup a rectangle to define your region of interest
    Rect myROI(left, top, width, height);
    // Crop the full image to that image contained by the rectangle myROI
    // Note that this doesn't copy the data
    Mat croppedImage = dst(myROI);

    // CV_IMWRITE_PNG_COMPRESSION

    imwrite(file_result_path, croppedImage, vector<int>({CV_IMWRITE_JPEG_QUALITY, JPEG_QUALITY}));

    LOGD("imwrite DONE");
        return true;

    } catch (Exception& ex) {
        LOGE("Exception saving image: %s\n", ex.what());
        return false;
    }
*/
return false;
    // env->ReleaseStringUTFChars(pathSource, file_source_path);
    //  env->ReleaseStringUTFChars(pathResult, file_result_path);
}

JNIEXPORT jboolean JNICALL Java_com_yalantis_ucrop_view_CropImageView_cropFileCImg(JNIEnv *env, jobject obj, jstring pathSource, jstring pathResult, jint left, jint top, jint width, jint height, jfloat angle) {

    const char *file_source_path = env->GetStringUTFChars(pathSource, 0);
    const char *file_result_path = env->GetStringUTFChars(pathResult, 0);

    LOGD("cropFileCImg");

    LOGD("file_source_path: %s \nfile_result_path: %s",file_source_path, file_result_path);

    try {

        // Set input arguments.
        const CImg<unsigned char> img(file_source_path);
        const int
        x0 = left, y0 = top,
        x1 = left + width, y1 = top + height;

        // Create warp field.
        CImg<float> warp(cimg::abs(x1 - x0 + 1), cimg::abs(y1 - y0 + 1), 1, 2);

        const float
        rad = angle * cimg::PI/180,
        ca = std::cos(rad),
        sa = std::sin(rad);

        const float
        ux = cimg::abs(img.width() * ca), uy = cimg::abs(img.width() * sa),
        vx = cimg::abs(img.height() * sa), vy = cimg::abs(img.height() * ca);

        const float
        dw2 = 0.5f * (ux + vx), dh2 = 0.5f * (uy + vy);
        const float
        w2 = 0.5f * img.width(), h2 = 0.5f * img.height();

        LOGD("w2: %1.2f \nh2: %1.2f", w2, h2);

        cimg_forXY(warp, x, y) {
            const float
            u = x + x0 - dw2,
            v = y + y0 - dh2;

            warp(x, y, 0) = w2 + u*ca + v*sa;
            warp(x, y, 1) = h2 - u*sa + v*ca;
        }

        // Get cropped image.
        const CImg<unsigned char> crop = img.get_warp(warp, 0, 0, 0);
        crop.save(file_result_path);

        // Compare with crop on fully rotated image.
        //const CImg<unsigned char> full_crop = img.get_rotate(angle,w2,h2,1,0,0).crop(x0,y0,x1,y1);
        //full_crop.save(file_result_path);

        //CImg<unsigned char> imageToChange;
        //imageToChange.assign(file_source_path);
        //imageToChange.rotate(angle, 0, 0).crop(left, top, left + width, top + height).save(file_result_path);

        return true;

    } catch (CImgArgumentException e) {
        throwJavaException(env, e.what());
    } catch (CImgIOException e) {
        throwJavaException(env, e.what());
    }

    return false;

}

void throwJavaException(JNIEnv *env, const char *msg) {
	// You can put your own exception here
	jclass c = env->FindClass("java/lang/RuntimeException");

	if (NULL == c) {
		//B plan: null pointer ...
		c = env->FindClass("java/lang/NullPointerException");
	}

	env->ThrowNew(c, msg);
}

#ifdef __cplusplus
}
#endif