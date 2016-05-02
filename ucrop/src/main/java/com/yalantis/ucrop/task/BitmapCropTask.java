package com.yalantis.ucrop.task;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.yalantis.ucrop.callback.BitmapCropCallback;

import java.io.File;
import java.io.IOException;

/**
 * Crops part of image that fills the crop bounds.
 * <p/>
 * First image is downscaled if max size was set and if resulting image is larger that max size.
 * Then image is rotated accordingly.
 * Finally new Bitmap object is created and saved to file.
 */
public class BitmapCropTask extends AsyncTask<Void, Void, Throwable> {

    static {
        System.loadLibrary("ucrop");
    }

    private Bitmap mViewBitmap;

    private final RectF mCropRect;
    private final RectF mCurrentImageRect;

    private float mCurrentScale, mCurrentAngle;
    private final int mMaxResultImageSizeX, mMaxResultImageSizeY;

    private final Bitmap.CompressFormat mCompressFormat;
    private final int mCompressQuality;
    private final String mImageInputPath, mImageOutputPath;
    private final BitmapCropCallback mCropCallback;

    public BitmapCropTask(@Nullable Bitmap viewBitmap,
                          @NonNull RectF cropRect, @NonNull RectF currentImageRect,
                          float currentScale, float currentAngle,
                          int maxResultImageSizeX, int maxResultImageSizeY,
                          @NonNull Bitmap.CompressFormat compressFormat, int compressQuality,
                          @NonNull String imageInputPath, @NonNull String imageOutputPath,
                          @Nullable BitmapCropCallback cropCallback) {

        mViewBitmap = viewBitmap;
        mCropRect = cropRect;
        mCurrentImageRect = currentImageRect;

        mCurrentScale = currentScale;
        mCurrentAngle = currentAngle;
        mMaxResultImageSizeX = maxResultImageSizeX;
        mMaxResultImageSizeY = maxResultImageSizeY;

        mCompressFormat = compressFormat;
        mCompressQuality = compressQuality;

        mImageInputPath = imageInputPath;
        mImageOutputPath = imageOutputPath;

        mCropCallback = cropCallback;
    }

    @Override
    @Nullable
    protected Throwable doInBackground(Void... params) {
        if (mViewBitmap == null || mViewBitmap.isRecycled()) {
            return new NullPointerException("ViewBitmap is null or already recycled");
        }
        if (mCurrentImageRect.isEmpty()) {
            return new NullPointerException("CurrentImageRect is empty");
        }

        float resizeScale = resize();

        try {
            crop(resizeScale);
            mViewBitmap.recycle();
            mViewBitmap = null;
        } catch (Throwable throwable) {
            return throwable;
        }

        return null;
    }

    private float resize() {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mImageInputPath, options);

        float scaleX = options.outWidth / mViewBitmap.getWidth();
        float scaleY = options.outHeight / mViewBitmap.getHeight();

        float resizeScale = Math.min(scaleX, scaleY);

        mCurrentScale /= resizeScale;

        resizeScale = 1;
        if (mMaxResultImageSizeX > 0 && mMaxResultImageSizeY > 0) {
            float cropWidth = mCropRect.width() / mCurrentScale;
            float cropHeight = mCropRect.height() / mCurrentScale;

            if (cropWidth > mMaxResultImageSizeX || cropHeight > mMaxResultImageSizeY) {

                scaleX = mMaxResultImageSizeX / cropWidth;
                scaleY = mMaxResultImageSizeY / cropHeight;
                resizeScale = Math.min(scaleX, scaleY);

                mCurrentScale /= resizeScale;
            }
        }
        return resizeScale;
    }

    private boolean crop(float resizeScale) throws IOException {
        ExifInterface originalExif = new ExifInterface(mImageInputPath);

        int top = Math.round((mCropRect.top - mCurrentImageRect.top) / mCurrentScale);
        int left = Math.round((mCropRect.left - mCurrentImageRect.left) / mCurrentScale);
        int width = Math.round(mCropRect.width() / mCurrentScale);
        int height = Math.round(mCropRect.height() / mCurrentScale);

        boolean cropped = cropCImg(mImageInputPath, mImageOutputPath,
                left, top, width, height, mCurrentAngle, resizeScale,
                mCompressFormat.ordinal(), mCompressQuality);
        if (cropped) {
            copyExif(originalExif, width, height);
        }

        return cropped;
    }

    @SuppressWarnings("JniMissingFunction")
    native public boolean cropCImg(String inputPath, String outputPath,
                                   int left, int top, int width, int height, float angle, float resizeScale,
                                   int format, int quality) throws IOException, OutOfMemoryError;

    public void copyExif(ExifInterface originalExif, int width, int height) throws IOException {
        String[] attributes = new String[]{
                ExifInterface.TAG_APERTURE,
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_DATETIME_DIGITIZED,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_FLASH,
                ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_PROCESSING_METHOD,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_ISO,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_SUBSEC_TIME,
                ExifInterface.TAG_SUBSEC_TIME_DIG,
                ExifInterface.TAG_SUBSEC_TIME_ORIG,
                ExifInterface.TAG_WHITE_BALANCE
        };

        ExifInterface newExif = new ExifInterface(mImageOutputPath);
        String value;
        for (String attribute : attributes) {
            value = originalExif.getAttribute(attribute);
            if (!TextUtils.isEmpty(value)) {
                newExif.setAttribute(attribute, value);
            }
        }
        newExif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, String.valueOf(width));
        newExif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, String.valueOf(height));
        newExif.setAttribute(ExifInterface.TAG_ORIENTATION, "0");

        newExif.saveAttributes();
    }

    @Override
    protected void onPostExecute(@Nullable Throwable t) {
        if (mCropCallback != null) {
            if (t == null) {
                mCropCallback.onBitmapCropped(Uri.fromFile(new File(mImageOutputPath)));
            } else {
                mCropCallback.onCropFailure(t);
            }
        }
    }

}
