package com.yalantis.ucrop.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.yalantis.ucrop.R;
import com.yalantis.ucrop.util.RotationGestureDetector;

public class GestureUCropView extends UCropView {
    private static final int DOUBLE_TAP_ZOOM_DURATION = 200;
    private ScaleGestureDetector mScaleDetector;
    private RotationGestureDetector mRotateDetector;

    private GestureDetector mGestureDetector;

    private float mMidPntX, mMidPntY;
    private boolean mIsRotateEnabled = true, mIsScaleEnabled = true;

    private int mDoubleTapScaleSteps = 5;

    public GestureUCropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GestureUCropView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void prepareInnerView(Context context) {
        // super.prepareInnerView(context);
        int padding = getResources().getDimensionPixelSize(R.dimen.ucrop_padding_crop_frame);

        mCropImageView = new CropImageView(context);
        mCropImageView.setId(R.id.image_view_crop);
        mCropImageView.setPadding(padding, padding, padding, padding);

        mViewOverlay = new OverlayView(context);
        mViewOverlay.setId(R.id.view_overlay);
        mViewOverlay.setPadding(padding, padding, padding, padding);

        addView(mCropImageView);
        addView(mViewOverlay);
    }

    protected void init() {
        setupGestureListeners();
    }

    public void setScaleEnabled(boolean scaleEnabled) {
        mIsScaleEnabled = scaleEnabled;
    }

    public boolean isScaleEnabled() {
        return mIsScaleEnabled;
    }

    public void setRotateEnabled(boolean rotateEnabled) {
        mIsRotateEnabled = rotateEnabled;
    }

    public boolean isRotateEnabled() {
        return mIsRotateEnabled;
    }

    public void setDoubleTapScaleSteps(int doubleTapScaleSteps) {
        mDoubleTapScaleSteps = doubleTapScaleSteps;
    }

    public int getDoubleTapScaleSteps() {
        return mDoubleTapScaleSteps;
    }

    /**
     * If it's ACTION_DOWN event - user touches the screen and all current animation must be canceled.
     * If it's ACTION_UP event - user removed all fingers from the screen and current image position must be corrected.
     * If there are more than 2 fingers - update focal point coordinates.
     * Pass the event to the gesture detectors if those are enabled.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
            getCropImageView().cancelAllAnimations();
        }

        if (event.getPointerCount() > 1) {
            mMidPntX = (event.getX(0) + event.getX(1)) / 2;
            mMidPntY = (event.getY(0) + event.getY(1)) / 2;
        }

        mGestureDetector.onTouchEvent(event);

        if (mIsScaleEnabled) {
            mScaleDetector.onTouchEvent(event);
        }

        if (mIsRotateEnabled) {
            mRotateDetector.onTouchEvent(event);
        }

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            getCropImageView().setImageToWrapCropBounds();
        }
        return true;
    }

    /**
     * This method calculates target scale value for double tap gesture.
     * User is able to zoom the image from min scale value
     * to the max scale value with {@link #mDoubleTapScaleSteps} double taps.
     */
    protected float getDoubleTapTargetScale() {
        return getCropImageView().getCurrentScale() * (float) Math.pow(getCropImageView().getMaxScale() / getCropImageView().getMinScale(), 1.0f / mDoubleTapScaleSteps);
    }

    private void setupGestureListeners() {
        mGestureDetector = new GestureDetector(getContext(), new GestureListener(), null, true);
        mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        mRotateDetector = new RotationGestureDetector(new RotateListener());
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            getCropImageView().postScale(detector.getScaleFactor(), mMidPntX, mMidPntY);
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            getCropImageView().zoomImageToPosition(getDoubleTapTargetScale(), e.getX(), e.getY(), DOUBLE_TAP_ZOOM_DURATION);
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            getCropImageView().postTranslate(-distanceX, -distanceY);
            return true;
        }

    }

    private class RotateListener extends RotationGestureDetector.SimpleOnRotationGestureListener {

        @Override
        public boolean onRotation(RotationGestureDetector rotationDetector) {
            getCropImageView().postRotate(rotationDetector.getAngle(), mMidPntX, mMidPntY);
            return true;
        }

    }
}
