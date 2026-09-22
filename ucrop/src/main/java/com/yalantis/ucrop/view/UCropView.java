package com.yalantis.ucrop.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.yalantis.ucrop.R;
import com.yalantis.ucrop.callback.CropBoundsChangeListener;
import com.yalantis.ucrop.callback.OverlayViewChangeListener;
import com.yalantis.ucrop.util.RectUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UCropView extends FrameLayout {

    private GestureCropImageView mGestureCropImageView;
    private final OverlayView mViewOverlay;
    private boolean mLayingOut, mPendingLockedCropBounds;
    private float mPendingCropRatio;
    private RectF mInitialCropRect, mCurrentSourceCropRect;
    private boolean mInitialCropApplied, mRestoringCrop;

    public UCropView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UCropView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.ucrop_view, this, true);
        mGestureCropImageView = findViewById(R.id.image_view_crop);
        mViewOverlay = findViewById(R.id.view_overlay);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ucrop_UCropView);
        mViewOverlay.processStyledAttributes(a);
        mGestureCropImageView.processStyledAttributes(a);
        a.recycle();


        setListenersToViews();
    }

    private void setListenersToViews() {
        mGestureCropImageView.mImageChangedListener = () -> {
            mCurrentSourceCropRect = null;
            mInitialCropApplied = false;
            mPendingLockedCropBounds = false;
            mViewOverlay.resetLockedCropMinimum();
        };
        mGestureCropImageView.mImageLayoutListener = () -> {
            if (!mLayingOut) {
                synchronizeCrop();
            }
        };
        mGestureCropImageView.mBeforeCropListener = () -> {
            synchronizeCrop();
            return mInitialCropRect == null || mInitialCropApplied;
        };
        mGestureCropImageView.setCropBoundsChangeListener(new CropBoundsChangeListener() {
            @Override
            public void onCropAspectRatioChanged(float cropRatio) {
                if (mGestureCropImageView.isImageLocked()) {
                    if (mLayingOut) {
                        mPendingLockedCropBounds = true;
                        mPendingCropRatio = cropRatio;
                        return;
                    }
                    updateLockedImageBounds();
                    if (mInitialCropRect != null && !mInitialCropApplied) {
                        synchronizeCrop();
                        return;
                    }
                }
                mViewOverlay.setTargetAspectRatio(cropRatio);
            }
        });
        mViewOverlay.setOverlayViewChangeListener(new OverlayViewChangeListener() {
            @Override
            public void onCropRectUpdated(RectF cropRect) {
                if (mGestureCropImageView.isImageLocked()) {
                    if (mLayingOut) {
                        return;
                    }
                    RectF imageCropRect = new RectF(cropRect);
                    imageCropRect.offset(mViewOverlay.getLeft() - mGestureCropImageView.getLeft(),
                            mViewOverlay.getTop() - mGestureCropImageView.getTop());
                    mGestureCropImageView.setCropRect(imageCropRect);
                    if (!mRestoringCrop && mGestureCropImageView.mBitmapLaidOut) {
                        Matrix inverse = new Matrix();
                        if (sourceToOverlayMatrix().invert(inverse)) {
                            mCurrentSourceCropRect = new RectF(cropRect);
                            inverse.mapRect(mCurrentSourceCropRect);
                        }
                    }
                    return;
                }
                mGestureCropImageView.setCropRect(cropRect);
            }
        });
    }

    private boolean updateLockedImageBounds() {
        RectF imageBounds = mGestureCropImageView.getImageBounds();
        imageBounds.offset(mGestureCropImageView.getLeft() + mGestureCropImageView.getPaddingLeft()
                        - mViewOverlay.getLeft(),
                mGestureCropImageView.getTop() + mGestureCropImageView.getPaddingTop()
                        - mViewOverlay.getTop());
        return mViewOverlay.setImageBounds(imageBounds,
                mGestureCropImageView.getSourceImageWidth(),
                mGestureCropImageView.getSourceImageHeight());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mLayingOut = true;
        try {
            super.onLayout(changed, left, top, right, bottom);
        } finally {
            mLayingOut = false;
        }
        synchronizeCrop();
    }

    /**
     * Copies an optional initial rectangle in EXIF-corrected full-resolution source pixels,
     * not raw sensor, preview, or view coordinates. Requires image-locked mode when loaded.
     * Validation failures are delivered to the image's onLoadFailure listener and block saving.
     * Applies once per image; replacing the bitmap/URI revalidates this input. Resizing preserves
     * the current edited source selection instead. Null removes the initial-rectangle option.
     */
    public void setInitialCropRect(@Nullable RectF rect) {
        mInitialCropRect = rect == null ? null : new RectF(rect);
        mInitialCropApplied = false;
        mCurrentSourceCropRect = null;
        requestLayout();
    }

    private Matrix sourceToOverlayMatrix() {
        Bitmap bitmap = mGestureCropImageView.getViewBitmap();
        Matrix matrix = new Matrix();
        matrix.setScale(bitmap.getWidth() / (float) mGestureCropImageView.getSourceImageWidth(),
                bitmap.getHeight() / (float) mGestureCropImageView.getSourceImageHeight());
        matrix.postConcat(mGestureCropImageView.getImageMatrix());
        matrix.postTranslate(mGestureCropImageView.getLeft() + mGestureCropImageView.getPaddingLeft()
                        - mViewOverlay.getLeft(),
                mGestureCropImageView.getTop() + mGestureCropImageView.getPaddingTop()
                        - mViewOverlay.getTop());
        return matrix;
    }

    private void synchronizeCrop() {
        GestureCropImageView image = mGestureCropImageView;
        if (mLayingOut || !image.mBitmapLaidOut || image.getSourceImageWidth() <= 0
                || image.getSourceImageHeight() <= 0 || image.mImageLoadError != null
                || image.mThisWidth <= 0 || image.mThisHeight <= 0
                || mViewOverlay.getWidth() <= 0 || mViewOverlay.getHeight() <= 0) {
            return;
        }
        try {
            if (mInitialCropRect != null) {
                if (!image.isImageLocked()) {
                    throw new IllegalArgumentException("Initial crop rectangle requires image-locked mode");
                }
                if (!mInitialCropApplied) {
                    RectUtils.validateSourceRect(mInitialCropRect);
                    if (mInitialCropRect.right > image.getSourceImageWidth()
                            || mInitialCropRect.bottom > image.getSourceImageHeight()) {
                        throw new IllegalArgumentException("Initial crop rectangle " + mInitialCropRect
                                + " outside source image " + image.getSourceImageWidth()
                                + "x" + image.getSourceImageHeight());
                    }
                    mCurrentSourceCropRect = new RectF(mInitialCropRect);
                    mViewOverlay.setLockedCropMinimumSourceSize(
                            mInitialCropRect.width(), mInitialCropRect.height());
                }
            }
            if (image.isImageLocked()) {
                // Layout callbacks may propose a centered ratio crop. Restore source state only
                // after both siblings and the fit-center matrix are final.
                boolean boundsChanged = updateLockedImageBounds();
                if (mCurrentSourceCropRect != null) {
                    RectF mapped = new RectF(mCurrentSourceCropRect);
                    sourceToOverlayMatrix().mapRect(mapped);
                    mRestoringCrop = true;
                    try {
                        mViewOverlay.setCropViewRect(mapped);
                    } finally {
                        mRestoringCrop = false;
                    }
                } else if (mPendingLockedCropBounds || boundsChanged) {
                    mViewOverlay.setTargetAspectRatio(mPendingLockedCropBounds
                            ? mPendingCropRatio : image.getTargetAspectRatio());
                }
            }
            mInitialCropApplied = mInitialCropRect != null;
            mPendingLockedCropBounds = false;
            image.notifyImageLoaded();
        } catch (IllegalArgumentException e) {
            image.reportImageFailure(e);
        }
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @NonNull
    public GestureCropImageView getCropImageView() {
        return mGestureCropImageView;
    }


    /**
     * Locks the fit-centered image and enables rectangular cropping inside its displayed bounds.
     */
    public void setImageLocked(boolean imageLocked) {
        if (mGestureCropImageView.isImageLocked() != imageLocked) {
            mCurrentSourceCropRect = null;
        }
        mViewOverlay.setImageLocked(imageLocked);
        mGestureCropImageView.setImageLocked(imageLocked);
        requestLayout();
    }

    @NonNull
    public OverlayView getOverlayView() {
        return mViewOverlay;
    }

    /**
     * Method for reset state for UCropImageView such as rotation, scale, translation.
     * Be careful: this method recreate UCropImageView instance and reattach it to layout.
     */
    public void resetCropImageView() {
        boolean imageLocked = mGestureCropImageView.isImageLocked();
        removeView(mGestureCropImageView);
        mGestureCropImageView = new GestureCropImageView(getContext());
        mCurrentSourceCropRect = null;
        mInitialCropApplied = false;
        mGestureCropImageView.setImageLocked(imageLocked);
        mGestureCropImageView = new GestureCropImageView(getContext());
        setListenersToViews();
        mGestureCropImageView.setCropRect(getOverlayView().getCropViewRect());
        addView(mGestureCropImageView, 0);
    }
}