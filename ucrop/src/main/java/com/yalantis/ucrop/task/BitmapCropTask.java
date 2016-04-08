package com.yalantis.ucrop.task;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.yalantis.ucrop.callback.BitmapCropCallback;
import com.yalantis.ucrop.util.BitmapLoadUtils;

import java.io.OutputStream;

/**
 * Crops part of image that fills the crop bounds.
 * <p/>
 * First image is downscaled if max size was set and if resulting image is larger that max size.
 * Then image is rotated accordingly.
 * Finally new Bitmap object is created and saved to file.
 */
public class BitmapCropTask extends AsyncTask<Void, Void, Exception> {

    private final Context mContext;

    private Bitmap mViewBitmap;

    private final RectF mCropRect;
    private final RectF mCurrentImageRect;
    private final Matrix mTempMatrix = new Matrix();

    private float mCurrentScale, mCurrentAngle;
    private final int mMaxResultImageSizeX, mMaxResultImageSizeY;

    private final Bitmap.CompressFormat mCompressFormat;
    private final int mCompressQuality;

    private final Uri mOutputUri;

    private final BitmapCropCallback mCropCallback;

    public BitmapCropTask(@NonNull Context context, @Nullable Bitmap viewBitmap,
                          @NonNull RectF cropRect, @NonNull RectF currentImageRect,
                          float currentScale, float currentAngle,
                          int maxResultImageSizeX, int maxResultImageSizeY,
                          @NonNull Bitmap.CompressFormat compressFormat, int compressQuality,
                          @NonNull Uri outputUri, @Nullable BitmapCropCallback cropCallback) {

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

        mOutputUri = outputUri;

        mCropCallback = cropCallback;
    }

    @Override
    @Nullable
    protected Exception doInBackground(Void... params) {
        if (mViewBitmap == null || mViewBitmap.isRecycled()) {
            return new NullPointerException("ViewBitmap is null or already recycled");
        }
        if (mCurrentImageRect.isEmpty()) {
            return new NullPointerException("CurrentImageRect is empty");
        }

        if (mMaxResultImageSizeX > 0 && mMaxResultImageSizeY > 0) {
            resize();
        }

        if (mCurrentAngle != 0) {
            rotate();
        }

        crop();

        OutputStream outputStream = null;
        try {
            outputStream = mContext.getContentResolver().openOutputStream(mOutputUri);
            mViewBitmap.compress(mCompressFormat, mCompressQuality, outputStream);
            mViewBitmap.recycle();
            mViewBitmap = null;
        } catch (Exception e) {
            return e;
        } finally {
            BitmapLoadUtils.close(outputStream);
        }
        return null;
    }

    private void resize() {
        float cropWidth = mCropRect.width() / mCurrentScale;
        float cropHeight = mCropRect.height() / mCurrentScale;

        if (cropWidth > mMaxResultImageSizeX || cropHeight > mMaxResultImageSizeY) {

            float scaleX = mMaxResultImageSizeX / cropWidth;
            float scaleY = mMaxResultImageSizeY / cropHeight;
            float resizeScale = Math.min(scaleX, scaleY);

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(mViewBitmap,
                    Math.round(mViewBitmap.getWidth() * resizeScale),
                    Math.round(mViewBitmap.getHeight() * resizeScale), false);
            if (mViewBitmap != resizedBitmap) {
                mViewBitmap.recycle();
            }
            mViewBitmap = resizedBitmap;

            mCurrentScale /= resizeScale;
        }
    }

    private void rotate() {
        mTempMatrix.reset();
        mTempMatrix.setRotate(mCurrentAngle, mViewBitmap.getWidth() / 2, mViewBitmap.getHeight() / 2);

        Bitmap rotatedBitmap = Bitmap.createBitmap(mViewBitmap, 0, 0, mViewBitmap.getWidth(), mViewBitmap.getHeight(),
                mTempMatrix, true);
        if (mViewBitmap != rotatedBitmap) {
            mViewBitmap.recycle();
        }
        mViewBitmap = rotatedBitmap;
    }

    private void crop() {
        int top = Math.round((mCropRect.top - mCurrentImageRect.top) / mCurrentScale);
        int left = Math.round((mCropRect.left - mCurrentImageRect.left) / mCurrentScale);
        int width = Math.round(mCropRect.width() / mCurrentScale);
        int height = Math.round(mCropRect.height() / mCurrentScale);

        mViewBitmap = Bitmap.createBitmap(mViewBitmap, left, top, width, height);
    }

    @Override
    protected void onPostExecute(@Nullable Exception result) {
        if (mCropCallback != null) {
            if (result == null) {
                mCropCallback.onBitmapCropped();
            } else {
                mCropCallback.onCropFailure(result);
            }
        }
    }

}
