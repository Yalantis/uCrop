package com.yalantis.ucrop.task;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

    private final Context mContext;

    private Bitmap mViewBitmap;

    private final RectF mCropRect;
    private final RectF mCurrentImageRect;

    private float mCurrentScale, mCurrentAngle;
    private final int mMaxResultImageSizeX, mMaxResultImageSizeY;

    private final Bitmap.CompressFormat mCompressFormat;
    private final int mCompressQuality;
    private final String mImageInputPath, mImageOuputPath;
    private final BitmapCropCallback mCropCallback;

    public BitmapCropTask(@NonNull Context context, @Nullable Bitmap viewBitmap,
                          @NonNull RectF cropRect, @NonNull RectF currentImageRect,
                          float currentScale, float currentAngle,
                          int maxResultImageSizeX, int maxResultImageSizeY,
                          @NonNull Bitmap.CompressFormat compressFormat, int compressQuality,
                          @NonNull String imageInputPath, @NonNull String imageOuputPath, @Nullable BitmapCropCallback cropCallback) {

        mContext = context;

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
        mImageOuputPath = imageOuputPath;

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
        int top = Math.round((mCropRect.top - mCurrentImageRect.top) / mCurrentScale);
        int left = Math.round((mCropRect.left - mCurrentImageRect.left) / mCurrentScale);
        int width = Math.round(mCropRect.width() / mCurrentScale);
        int height = Math.round(mCropRect.height() / mCurrentScale);

        return cropCImg(mImageInputPath, mImageOuputPath,
                left, top, width, height, mCurrentAngle, resizeScale,
                mCompressFormat.ordinal(), mCompressQuality);
    }

    @SuppressWarnings("JniMissingFunction")
    native public boolean cropCImg(String inputPath, String outputPath,
                                   int left, int top, int width, int height, float angle, float resizeScale,
                                   int format, int quality) throws IOException, OutOfMemoryError;

    @Override
    protected void onPostExecute(@Nullable Throwable t) {
        if (mCropCallback != null) {
            if (t == null) {
                mCropCallback.onBitmapCropped(Uri.fromFile(new File(mImageOuputPath)));
            } else {
                mCropCallback.onCropFailure(t);
            }
        }
    }

}
