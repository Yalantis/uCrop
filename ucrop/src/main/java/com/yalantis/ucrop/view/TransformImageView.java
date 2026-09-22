package com.yalantis.ucrop.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;

import com.yalantis.ucrop.callback.BitmapLoadCallback;
import com.yalantis.ucrop.model.ExifInfo;
import com.yalantis.ucrop.util.BitmapLoadUtils;
import com.yalantis.ucrop.util.FastBitmapDrawable;
import com.yalantis.ucrop.util.RectUtils;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import java.io.File;

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 * <p/>
 * This class provides base logic to setup the image, transform it with matrix (move, scale, rotate),
 * and methods to get current matrix state.
 */
public class TransformImageView extends AppCompatImageView {

    private static final String TAG = "TransformImageView";

    private static final int RECT_CORNER_POINTS_COORDS = 8;
    private static final int RECT_CENTER_POINT_COORDS = 2;
    private static final int MATRIX_VALUES_COUNT = 9;

    protected final float[] mCurrentImageCorners = new float[RECT_CORNER_POINTS_COORDS];
    protected final float[] mCurrentImageCenter = new float[RECT_CENTER_POINT_COORDS];

    private final float[] mMatrixValues = new float[MATRIX_VALUES_COUNT];

    protected Matrix mCurrentImageMatrix = new Matrix();
    protected int mThisWidth, mThisHeight;

    protected TransformImageListener mTransformImageListener;

    private float[] mInitialImageCorners;
    private float[] mInitialImageCenter;

    protected boolean mBitmapDecoded = false;
    protected boolean mBitmapLaidOut = false;

    private int mMaxBitmapSize = 0;

    private String mImageInputPath, mImageOutputPath;
    private ExifInfo mExifInfo;
    private int mSourceImageWidth, mSourceImageHeight, mImageGeneration;
    protected Exception mImageLoadError;
    private boolean mLoadCompleteNotified;
    private boolean mRequestedSizeSampled;
    Runnable mImageChangedListener, mImageLayoutListener;

    /**
     * Interface for rotation and scale change notifying.
     */
    public interface TransformImageListener {

        void onLoadComplete();

        void onLoadFailure(@NonNull Exception e);

        void onRotate(float currentAngle);

        void onScale(float currentScale);

    }

    public TransformImageView(Context context) {
        this(context, null);
    }

    public TransformImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TransformImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void setTransformImageListener(TransformImageListener transformImageListener) {
        mTransformImageListener = transformImageListener;
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        if (scaleType == ScaleType.MATRIX) {
            super.setScaleType(scaleType);
        } else {
            Log.w(TAG, "Invalid ScaleType. Only ScaleType.MATRIX can be used");
        }
    }

    /**
     * Setter for {@link #mMaxBitmapSize} value.
     * Be sure to call it before {@link #setImageURI(Uri)} or other image setters.
     * Requested-size sampling is opt-in (enabled by CropImageView in locked mode);
     * otherwise only legacy OOM/100 MB safeguards trigger sampling.
     *
     * @param maxBitmapSize - max size for both width and height of bitmap that will be used in the view.
     */
    public void setMaxBitmapSize(int maxBitmapSize) {
        mMaxBitmapSize = maxBitmapSize;
    }

    public int getMaxBitmapSize() {
        if (mMaxBitmapSize <= 0) {
            mMaxBitmapSize = BitmapLoadUtils.calculateMaxBitmapSize(getContext());
        }
        return mMaxBitmapSize;
    }

    @Override
    public void setImageBitmap(final Bitmap bitmap) {
        beginImage();
        mSourceImageWidth = bitmap.getWidth();
        mSourceImageHeight = bitmap.getHeight();
        mBitmapDecoded = true;
        setImageDrawable(new FastBitmapDrawable(bitmap));
        requestLayout();
    }

    /** Returns EXIF-corrected full-resolution source pixels, or zero while loading. */
    public int getSourceImageWidth() {
        return mSourceImageWidth;
    }

    /** Returns EXIF-corrected full-resolution source pixels, or zero while loading. */
    public int getSourceImageHeight() {
        return mSourceImageHeight;
    }

    private void beginImage() {
        mImageGeneration++;
        mSourceImageWidth = mSourceImageHeight = 0;
        mBitmapDecoded = mBitmapLaidOut = mLoadCompleteNotified = false;
        mImageLoadError = null;
        mImageInputPath = mImageOutputPath = null;
        mExifInfo = null;
        mRequestedSizeSampled = false;
        if (mImageChangedListener != null) {
            mImageChangedListener.run();
        }
    }

    void reportImageFailure(@NonNull Exception error) {
        mImageLoadError = error;
        Log.e(TAG, "Image initialization failed", error);
        if (mTransformImageListener != null) {
            mTransformImageListener.onLoadFailure(error);
        }
    }

    void notifyImageLoaded() {
        if (mBitmapLaidOut && mImageLoadError == null && !mLoadCompleteNotified) {
            mLoadCompleteNotified = true;
            if (mTransformImageListener != null) {
                mTransformImageListener.onLoadComplete();
            }
        }
    }

    public String getImageInputPath() {
        return mImageInputPath;
    }

    public String getImageOutputPath() {
        return mImageOutputPath;
    }

    public ExifInfo getExifInfo() {
        return mExifInfo;
    }

    /**
     * Decodes a URI asynchronously, using requested-size sampling only when opted in.
     *
     * @param imageUri - image Uri
     * @throws Exception - can throw exception if having problems with decoding Uri or OOM.
     */
    public void setImageUri(@NonNull Uri imageUri, @Nullable Uri outputUri) throws Exception {
        loadImage(imageUri, outputUri, shouldSampleToRequiredSize());
    }

    protected boolean shouldSampleToRequiredSize() {
        return false;
    }

    protected boolean isRequestedSizeSampled() {
        return mRequestedSizeSampled;
    }

    /**
     * Reloads the resolved local source asynchronously, without downloading/copying it again.
     * The existing bitmap is not crop-ready until the replacement has decoded and laid out.
     */
    protected void reloadImageWithoutRequestedSampling() {
        Uri input = Uri.fromFile(new File(mImageInputPath));
        Uri output = mImageOutputPath == null ? null : Uri.fromFile(new File(mImageOutputPath));
        loadImage(input, output, false);
    }

    private void loadImage(@NonNull Uri imageUri, @Nullable Uri outputUri, boolean sampleToRequiredSize) {
        beginImage();
        final int generation = mImageGeneration;
        int maxBitmapSize = getMaxBitmapSize();

        BitmapLoadUtils.decodeBitmapInBackground(getContext(), imageUri, outputUri, maxBitmapSize, maxBitmapSize,
                new BitmapLoadCallback() {

                    @Override
                    public void onBitmapLoaded(@NonNull Bitmap bitmap, @NonNull ExifInfo exifInfo, @NonNull String imageInputPath, @Nullable String imageOutputPath) {
                        onFailure(new IllegalStateException("Original source image dimensions are required"));
                    }

                    @Override
                    public void onBitmapLoaded(@NonNull Bitmap bitmap, @NonNull ExifInfo exifInfo,
                                               @NonNull String imageInputPath, @Nullable String imageOutputPath,
                                               int sourceWidth, int sourceHeight) {
                        if (generation != mImageGeneration) {
                            return;
                        }
                        boolean sampled = sampleToRequiredSize
                                && (bitmap.getWidth() != sourceWidth || bitmap.getHeight() != sourceHeight);
                        if (sampled && !shouldSampleToRequiredSize()) {
                            // Lock may have been disabled while decoding. Never publish that preview
                            // to normal mode's scalar native rotation pipeline.
                            bitmap.recycle();
                            loadImage(Uri.fromFile(new File(imageInputPath)),
                                    imageOutputPath == null ? null : Uri.fromFile(new File(imageOutputPath)), false);
                            return;
                        }
                        mImageInputPath = imageInputPath;
                        mImageOutputPath = imageOutputPath;
                        mExifInfo = exifInfo;
                        mSourceImageWidth = sourceWidth;
                        mSourceImageHeight = sourceHeight;
                        mRequestedSizeSampled = sampled;
                        mBitmapDecoded = true;
                        setImageDrawable(new FastBitmapDrawable(bitmap));
                        requestLayout();
                    }

                    @Override
                    public void onFailure(@NonNull Exception bitmapWorkerException) {
                        if (generation == mImageGeneration) {
                            reportImageFailure(bitmapWorkerException);
                        }
                    }
                }, sampleToRequiredSize);
    }

    /**
     * @return - current image scale value.
     * [1.0f - for original image, 2.0f - for 200% scaled image, etc.]
     */
    public float getCurrentScale() {
        return getMatrixScale(mCurrentImageMatrix);
    }

    /**
     * This method calculates scale value for given Matrix object.
     */
    public float getMatrixScale(@NonNull Matrix matrix) {
        return (float) Math.sqrt(Math.pow(getMatrixValue(matrix, Matrix.MSCALE_X), 2)
                + Math.pow(getMatrixValue(matrix, Matrix.MSKEW_Y), 2));
    }

    /**
     * @return - current image rotation angle.
     */
    public float getCurrentAngle() {
        return getMatrixAngle(mCurrentImageMatrix);
    }

    /**
     * This method calculates rotation angle for given Matrix object.
     */
    public float getMatrixAngle(@NonNull Matrix matrix) {
        return (float) -(Math.atan2(getMatrixValue(matrix, Matrix.MSKEW_X),
                getMatrixValue(matrix, Matrix.MSCALE_X)) * (180 / Math.PI));
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        super.setImageMatrix(matrix);
        mCurrentImageMatrix.set(matrix);
        updateCurrentImagePoints();
    }

    @Nullable
    public Bitmap getViewBitmap() {
        if (getDrawable() == null || !(getDrawable() instanceof FastBitmapDrawable)) {
            return null;
        } else {
            return ((FastBitmapDrawable) getDrawable()).getBitmap();
        }
    }

    /**
     * This method translates current image.
     *
     * @param deltaX - horizontal shift
     * @param deltaY - vertical shift
     */
    public void postTranslate(float deltaX, float deltaY) {
        if (deltaX != 0 || deltaY != 0) {
            mCurrentImageMatrix.postTranslate(deltaX, deltaY);
            setImageMatrix(mCurrentImageMatrix);
        }
    }

    /**
     * This method scales current image.
     *
     * @param deltaScale - scale value
     * @param px         - scale center X
     * @param py         - scale center Y
     */
    public void postScale(float deltaScale, float px, float py) {
        if (deltaScale != 0) {
            mCurrentImageMatrix.postScale(deltaScale, deltaScale, px, py);
            setImageMatrix(mCurrentImageMatrix);
            if (mTransformImageListener != null) {
                mTransformImageListener.onScale(getMatrixScale(mCurrentImageMatrix));
            }
        }
    }

    /**
     * This method rotates current image.
     *
     * @param deltaAngle - rotation angle
     * @param px         - rotation center X
     * @param py         - rotation center Y
     */
    public void postRotate(float deltaAngle, float px, float py) {
        if (deltaAngle != 0) {
            mCurrentImageMatrix.postRotate(deltaAngle, px, py);
            setImageMatrix(mCurrentImageMatrix);
            if (mTransformImageListener != null) {
                mTransformImageListener.onRotate(getMatrixAngle(mCurrentImageMatrix));
            }
        }
    }

    protected void init() {
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mBitmapDecoded && (changed || !mBitmapLaidOut
                || mThisWidth != getWidth() - getPaddingLeft() - getPaddingRight()
                || mThisHeight != getHeight() - getPaddingTop() - getPaddingBottom())) {

            left = getPaddingLeft();
            top = getPaddingTop();
            right = getWidth() - getPaddingRight();
            bottom = getHeight() - getPaddingBottom();
            mThisWidth = right - left;
            mThisHeight = bottom - top;

            onImageLaidOut();
            if (mImageLayoutListener != null) {
                mImageLayoutListener.run();
            } else {
                notifyImageLoaded();
            }
        }
    }

    /**
     * When image is laid out {@link #mInitialImageCenter} and {@link #mInitialImageCenter}
     * must be set.
     */
    protected void onImageLaidOut() {
        final Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }

        float w = drawable.getIntrinsicWidth();
        float h = drawable.getIntrinsicHeight();

        Log.d(TAG, String.format("Image size: [%d:%d]", (int) w, (int) h));

        RectF initialImageRect = new RectF(0, 0, w, h);
        mInitialImageCorners = RectUtils.getCornersFromRect(initialImageRect);
        mInitialImageCenter = RectUtils.getCenterFromRect(initialImageRect);

        mBitmapLaidOut = true;

    }

    /**
     * This method returns Matrix value for given index.
     *
     * @param matrix     - valid Matrix object
     * @param valueIndex - index of needed value. See {@link Matrix#MSCALE_X} and others.
     * @return - matrix value for index
     */
    protected float getMatrixValue(@NonNull Matrix matrix, @IntRange(from = 0, to = MATRIX_VALUES_COUNT) int valueIndex) {
        matrix.getValues(mMatrixValues);
        return mMatrixValues[valueIndex];
    }

    /**
     * This method logs given matrix X, Y, scale, and angle values.
     * Can be used for debug.
     */
    @SuppressWarnings("unused")
    protected void printMatrix(@NonNull String logPrefix, @NonNull Matrix matrix) {
        float x = getMatrixValue(matrix, Matrix.MTRANS_X);
        float y = getMatrixValue(matrix, Matrix.MTRANS_Y);
        float rScale = getMatrixScale(matrix);
        float rAngle = getMatrixAngle(matrix);
        Log.d(TAG, logPrefix + ": matrix: { x: " + x + ", y: " + y + ", scale: " + rScale + ", angle: " + rAngle + " }");
    }

    /**
     * This method updates current image corners and center points that are stored in
     * {@link #mCurrentImageCorners} and {@link #mCurrentImageCenter} arrays.
     * Those are used for several calculations.
     */
    private void updateCurrentImagePoints() {
        mCurrentImageMatrix.mapPoints(mCurrentImageCorners, mInitialImageCorners);
        mCurrentImageMatrix.mapPoints(mCurrentImageCenter, mInitialImageCenter);
    }

}
