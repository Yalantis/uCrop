package com.yalantis.ucrop.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.yalantis.ucrop.R;
import com.yalantis.ucrop.callback.OverlayViewChangeListener;
import com.yalantis.ucrop.util.RectUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 * <p/>
 * This view is used for drawing the overlay on top of the image. It may have frame, crop guidelines and dimmed area.
 * This must have LAYER_TYPE_SOFTWARE to draw itself properly.
 */
public class OverlayView extends View {

    public static final int FREESTYLE_CROP_MODE_DISABLE = 0;
    public static final int FREESTYLE_CROP_MODE_ENABLE = 1;
    public static final int FREESTYLE_CROP_MODE_ENABLE_WITH_PASS_THROUGH = 2;

    public static final boolean DEFAULT_SHOW_CROP_FRAME = true;
    public static final boolean DEFAULT_SHOW_CROP_GRID = true;
    public static final boolean DEFAULT_CIRCLE_DIMMED_LAYER = false;
    public static final int DEFAULT_FREESTYLE_CROP_MODE = FREESTYLE_CROP_MODE_DISABLE;
    public static final int DEFAULT_CROP_GRID_ROW_COUNT = 2;
    public static final int DEFAULT_CROP_GRID_COLUMN_COUNT = 2;
    private static final float DEFAULT_LOCKED_CROP_MIN_SOURCE_SIZE = 100f;

    private static final int TOUCH_NONE = -1;
    private static final int TOUCH_TOP_LEFT = 0;
    private static final int TOUCH_TOP_RIGHT = 1;
    private static final int TOUCH_BOTTOM_RIGHT = 2;
    private static final int TOUCH_BOTTOM_LEFT = 3;
    private static final int TOUCH_INTERIOR = 4;
    private static final int TOUCH_LEFT_EDGE = 5;
    private static final int TOUCH_TOP_EDGE = 6;
    private static final int TOUCH_RIGHT_EDGE = 7;
    private static final int TOUCH_BOTTOM_EDGE = 8;

    private final RectF mCropViewRect = new RectF();
    private final RectF mTempRect = new RectF();
    private final RectF mImageBounds = new RectF();
    private boolean mImageLocked;
    private float mLockedCropMinWidth, mLockedCropMinHeight;
    private float mLockedCropMinSourceWidth = DEFAULT_LOCKED_CROP_MIN_SOURCE_SIZE;
    private float mLockedCropMinSourceHeight = DEFAULT_LOCKED_CROP_MIN_SOURCE_SIZE;
    private int mSourceImageWidth, mSourceImageHeight;

    protected int mThisWidth, mThisHeight;
    protected float[] mCropGridCorners;
    protected float[] mCropGridCenter;

    private int mCropGridRowCount, mCropGridColumnCount;
    private float mTargetAspectRatio;
    private float[] mGridPoints = null;
    private boolean mShowCropFrame, mShowCropGrid;
    private boolean mCircleDimmedLayer;
    private int mDimmedColor;
    private Path mCircularPath = new Path();
    private Paint mDimmedStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mCropGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mCropFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mCropFrameCornersPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @FreestyleMode
    private int mFreestyleCropMode = DEFAULT_FREESTYLE_CROP_MODE;
    private float mPreviousTouchX = -1, mPreviousTouchY = -1;
    private int mCurrentTouchTarget = TOUCH_NONE;
    private int mTouchPointThreshold;
    private int mCropRectMinSize;
    private int mCropRectCornerTouchAreaLineLength;

    private OverlayViewChangeListener mCallback;

    private boolean mShouldSetupCropBounds;

    {
        mTouchPointThreshold = getResources().getDimensionPixelSize(R.dimen.ucrop_default_crop_rect_corner_touch_threshold);
        mCropRectMinSize = getResources().getDimensionPixelSize(R.dimen.ucrop_default_crop_rect_min_size);
        mCropRectCornerTouchAreaLineLength = getResources().getDimensionPixelSize(R.dimen.ucrop_default_crop_rect_corner_touch_area_line_length);
    }

    public OverlayView(Context context) {
        this(context, null);
    }

    public OverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public OverlayViewChangeListener getOverlayViewChangeListener() {
        return mCallback;
    }

    public void setOverlayViewChangeListener(OverlayViewChangeListener callback) {
        mCallback = callback;
    }

    @NonNull
    public RectF getCropViewRect() {
        return mCropViewRect;
    }

    void setImageLocked(boolean imageLocked) {
        if (mImageLocked != imageLocked) {
            mImageLocked = imageLocked;
            mImageBounds.setEmpty();
            resetLockedCropMinimum();
            mCurrentTouchTarget = TOUCH_NONE;
            postInvalidate();
        }
    }

    void resetLockedCropMinimum() {
        mLockedCropMinSourceWidth = DEFAULT_LOCKED_CROP_MIN_SOURCE_SIZE;
        mLockedCropMinSourceHeight = DEFAULT_LOCKED_CROP_MIN_SOURCE_SIZE;
    }

    void setLockedCropMinimumSourceSize(float width, float height) {
        mLockedCropMinSourceWidth = Math.min(DEFAULT_LOCKED_CROP_MIN_SOURCE_SIZE, width);
        mLockedCropMinSourceHeight = Math.min(DEFAULT_LOCKED_CROP_MIN_SOURCE_SIZE, height);
        updateLockedMinimums();
    }

    boolean setImageBounds(@NonNull RectF imageBounds, int sourceImageWidth, int sourceImageHeight) {
        boolean changed = !mImageBounds.equals(imageBounds);
        mImageBounds.set(imageBounds);
        mSourceImageWidth = sourceImageWidth;
        mSourceImageHeight = sourceImageHeight;
        updateLockedMinimums();
        return changed;
    }

    /**
     * Sets an exact locked selection in this overlay's coordinates, without enlarging it to
     * the touch minimum. Notifies the same listener used by interactive edits.
     */
    public void setCropViewRect(@NonNull RectF rect) {
        // Permit only matrix round-off at the displayed boundary, never clamp the selection.
        final float epsilon = 0.002f;
        if (!mImageLocked || !RectUtils.isFiniteRect(rect) || rect.isEmpty()
                || mImageBounds.isEmpty() || rect.left < mImageBounds.left - epsilon
                || rect.top < mImageBounds.top - epsilon || rect.right > mImageBounds.right + epsilon
                || rect.bottom > mImageBounds.bottom + epsilon) {
            throw new IllegalArgumentException("Crop rectangle " + rect + " outside locked image " + mImageBounds);
        }
        mShouldSetupCropBounds = false;
        mCropViewRect.set(rect);
        mTargetAspectRatio = rect.width() / rect.height();
        updateLockedMinimums();
        updateGridPoints();
        if (mCallback != null) {
            mCallback.onCropRectUpdated(mCropViewRect);
        }
        postInvalidate();
    }

    private void updateLockedMinimums() {
        if (mImageBounds.isEmpty() || mSourceImageWidth <= 0 || mSourceImageHeight <= 0) {
            return;
        }
        mLockedCropMinWidth = Math.min(mImageBounds.width(),
                mImageBounds.width() * mLockedCropMinSourceWidth / mSourceImageWidth);
        mLockedCropMinHeight = Math.min(mImageBounds.height(),
                mImageBounds.height() * mLockedCropMinSourceHeight / mSourceImageHeight);
    }

    @Deprecated
    /***
     * Please use the new method {@link #getFreestyleCropMode() getFreestyleCropMode} method as we have more than 1 freestyle crop mode.
     */
    public boolean isFreestyleCropEnabled() {
        return getFreestyleCropMode() == FREESTYLE_CROP_MODE_ENABLE;
    }

    @Deprecated
    /***
     * Please use the new method {@link #setFreestyleCropMode setFreestyleCropMode} method as we have more than 1 freestyle crop mode.
     */
    public void setFreestyleCropEnabled(boolean freestyleCropEnabled) {
        mFreestyleCropMode = freestyleCropEnabled ? FREESTYLE_CROP_MODE_ENABLE : FREESTYLE_CROP_MODE_DISABLE;
    }

    @FreestyleMode
    public int getFreestyleCropMode() {
        return mImageLocked ? FREESTYLE_CROP_MODE_ENABLE : mFreestyleCropMode;
    }

    public void setFreestyleCropMode(@FreestyleMode int mFreestyleCropMode) {
        this.mFreestyleCropMode = mFreestyleCropMode;
        postInvalidate();
    }

    /**
     * Setter for {@link #mCircleDimmedLayer} variable.
     *
     * @param circleDimmedLayer - set it to true if you want dimmed layer to be an circle
     */
    public void setCircleDimmedLayer(boolean circleDimmedLayer) {
        mCircleDimmedLayer = circleDimmedLayer;
    }

    /**
     * Setter for crop grid rows count.
     * Resets {@link #mGridPoints} variable because it is not valid anymore.
     */
    public void setCropGridRowCount(@IntRange(from = 0) int cropGridRowCount) {
        mCropGridRowCount = cropGridRowCount;
        mGridPoints = null;
    }

    /**
     * Setter for crop grid columns count.
     * Resets {@link #mGridPoints} variable because it is not valid anymore.
     */
    public void setCropGridColumnCount(@IntRange(from = 0) int cropGridColumnCount) {
        mCropGridColumnCount = cropGridColumnCount;
        mGridPoints = null;
    }

    /**
     * Setter for {@link #mShowCropFrame} variable.
     *
     * @param showCropFrame - set to true if you want to see a crop frame rectangle on top of an image
     */
    public void setShowCropFrame(boolean showCropFrame) {
        mShowCropFrame = showCropFrame;
    }

    /**
     * Setter for {@link #mShowCropGrid} variable.
     *
     * @param showCropGrid - set to true if you want to see a crop grid on top of an image
     */
    public void setShowCropGrid(boolean showCropGrid) {
        mShowCropGrid = showCropGrid;
    }

    /**
     * Setter for {@link #mDimmedColor} variable.
     *
     * @param dimmedColor - desired color of dimmed area around the crop bounds
     */
    public void setDimmedColor(@ColorInt int dimmedColor) {
        mDimmedColor = dimmedColor;
    }

    /**
     * Setter for crop frame stroke width
     */
    public void setCropFrameStrokeWidth(@IntRange(from = 0) int width) {
        mCropFramePaint.setStrokeWidth(width);
    }

    /**
     * Setter for crop grid stroke width
     */
    public void setCropGridStrokeWidth(@IntRange(from = 0) int width) {
        mCropGridPaint.setStrokeWidth(width);
    }

    /**
     * Setter for crop frame color
     */
    public void setCropFrameColor(@ColorInt int color) {
        mCropFramePaint.setColor(color);
    }

    /**
     * Setter for crop grid color
     */
    public void setCropGridColor(@ColorInt int color) {
        mCropGridPaint.setColor(color);
    }

    /**
     * Setter for crop grid corner color
     */
    public void setCropGridCornerColor(@ColorInt int color) {
        mCropFrameCornersPaint.setColor(color);
    }

    /**
     * This method sets aspect ratio for crop bounds.
     *
     * @param targetAspectRatio - aspect ratio for image crop (e.g. 1.77(7) for 16:9)
     */
    public void setTargetAspectRatio(final float targetAspectRatio) {
        mTargetAspectRatio = targetAspectRatio;
        if (mThisWidth > 0 || (mImageLocked && !mImageBounds.isEmpty())) {
            setupCropBounds();
            postInvalidate();
        } else {
            mShouldSetupCropBounds = true;
        }
    }

    /**
     * This method setups crop bounds rectangles for given aspect ratio and view size.
     * {@link #mCropViewRect} is used to draw crop bounds - uses padding.
     */
    public void setupCropBounds() {
        if (mImageLocked) {
            if (mImageBounds.isEmpty() || mTargetAspectRatio <= 0) {
                return;
            }
            float width = Math.min(mImageBounds.width(), mImageBounds.height() * mTargetAspectRatio);
            float height = Math.min(mImageBounds.height(), width / mTargetAspectRatio);
            width = Math.min(mImageBounds.width(), Math.max(mLockedCropMinWidth, width));
            height = Math.min(mImageBounds.height(), Math.max(mLockedCropMinHeight, height));
            mCropViewRect.set(mImageBounds.centerX() - width / 2, mImageBounds.centerY() - height / 2,
                    mImageBounds.centerX() + width / 2, mImageBounds.centerY() + height / 2);
            mCropViewRect.intersect(mImageBounds);
            // Each axis has its own minimum so even a letterboxed panorama can be resized.
            updateLockedMinimums();
            updateGridPoints();
            if (mCallback != null) {
                mCallback.onCropRectUpdated(mCropViewRect);
            }
            return;
        }
        int height = (int) (mThisWidth / mTargetAspectRatio);
        if (height > mThisHeight) {
            int width = (int) (mThisHeight * mTargetAspectRatio);
            int halfDiff = (mThisWidth - width) / 2;
            mCropViewRect.set(getPaddingLeft() + halfDiff, getPaddingTop(),
                    getPaddingLeft() + width + halfDiff, getPaddingTop() + mThisHeight);
        } else {
            int halfDiff = (mThisHeight - height) / 2;
            mCropViewRect.set(getPaddingLeft(), getPaddingTop() + halfDiff,
                    getPaddingLeft() + mThisWidth, getPaddingTop() + height + halfDiff);
        }

        if (mCallback != null) {
            mCallback.onCropRectUpdated(mCropViewRect);
        }

        updateGridPoints();
    }

    private void updateGridPoints() {
        mCropGridCorners = RectUtils.getCornersFromRect(mCropViewRect);
        mCropGridCenter = RectUtils.getCenterFromRect(mCropViewRect);

        mGridPoints = null;
        mCircularPath.reset();
        mCircularPath.addCircle(mCropViewRect.centerX(), mCropViewRect.centerY(),
                Math.min(mCropViewRect.width(), mCropViewRect.height()) / 2.f, Path.Direction.CW);
    }

    protected void init() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            left = getPaddingLeft();
            top = getPaddingTop();
            right = getWidth() - getPaddingRight();
            bottom = getHeight() - getPaddingBottom();
            mThisWidth = right - left;
            mThisHeight = bottom - top;

            if (mShouldSetupCropBounds) {
                mShouldSetupCropBounds = false;
                setTargetAspectRatio(mTargetAspectRatio);
            }
        }
    }

    /**
     * Along with image there are dimmed layer, crop bounds and crop guidelines that must be drawn.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawDimmedLayer(canvas);
        drawCropGrid(canvas);
    }

    private boolean onLockedTouchEvent(MotionEvent event) {
        if (mCropViewRect.isEmpty() || mImageBounds.isEmpty()) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mCurrentTouchTarget = getCurrentTouchIndex(event.getX(), event.getY());
                if (mCurrentTouchTarget == TOUCH_NONE) {
                    return false;
                }
                mPreviousTouchX = event.getX();
                mPreviousTouchY = event.getY();
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 1 && mCurrentTouchTarget != TOUCH_NONE) {
                    updateLockedCropRect(event.getX() - mPreviousTouchX, event.getY() - mPreviousTouchY);
                    mPreviousTouchX = event.getX();
                    mPreviousTouchY = event.getY();
                }
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                // End the drag rather than jumping to another pointer when a finger is lifted.
                mCurrentTouchTarget = TOUCH_NONE;
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mCurrentTouchTarget = TOUCH_NONE;
                mPreviousTouchX = mPreviousTouchY = -1;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                return true;
            default:
                return true;
        }
    }

    private void updateLockedCropRect(float deltaX, float deltaY) {
        if (mCurrentTouchTarget == TOUCH_INTERIOR) {
            mCropViewRect.offset(
                    clamp(deltaX, mImageBounds.left - mCropViewRect.left, mImageBounds.right - mCropViewRect.right),
                    clamp(deltaY, mImageBounds.top - mCropViewRect.top, mImageBounds.bottom - mCropViewRect.bottom));
        } else {
            if (mCurrentTouchTarget == TOUCH_TOP_LEFT || mCurrentTouchTarget == TOUCH_BOTTOM_LEFT
                    || mCurrentTouchTarget == TOUCH_LEFT_EDGE) {
                mCropViewRect.left = clamp(mCropViewRect.left + deltaX,
                        mImageBounds.left, mCropViewRect.right - mLockedCropMinWidth);
            } else if (mCurrentTouchTarget == TOUCH_TOP_RIGHT || mCurrentTouchTarget == TOUCH_BOTTOM_RIGHT
                    || mCurrentTouchTarget == TOUCH_RIGHT_EDGE) {
                mCropViewRect.right = clamp(mCropViewRect.right + deltaX,
                        mCropViewRect.left + mLockedCropMinWidth, mImageBounds.right);
            }
            if (mCurrentTouchTarget == TOUCH_TOP_LEFT || mCurrentTouchTarget == TOUCH_TOP_RIGHT
                    || mCurrentTouchTarget == TOUCH_TOP_EDGE) {
                mCropViewRect.top = clamp(mCropViewRect.top + deltaY,
                        mImageBounds.top, mCropViewRect.bottom - mLockedCropMinHeight);
            } else if (mCurrentTouchTarget == TOUCH_BOTTOM_LEFT || mCurrentTouchTarget == TOUCH_BOTTOM_RIGHT
                    || mCurrentTouchTarget == TOUCH_BOTTOM_EDGE) {
                mCropViewRect.bottom = clamp(mCropViewRect.bottom + deltaY,
                        mCropViewRect.top + mLockedCropMinHeight, mImageBounds.bottom);
            }
        }
        updateGridPoints();
        postInvalidate();
        if (mCallback != null) {
            mCallback.onCropRectUpdated(mCropViewRect);
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mImageLocked) {
            return onLockedTouchEvent(event);
        }
        if (mCropViewRect.isEmpty() || mFreestyleCropMode == FREESTYLE_CROP_MODE_DISABLE) {
            return false;
        }

        float x = event.getX();
        float y = event.getY();

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
            mCurrentTouchTarget = getCurrentTouchIndex(x, y);
            boolean shouldHandle = mCurrentTouchTarget != TOUCH_NONE;
            if (!shouldHandle) {
                mPreviousTouchX = -1;
                mPreviousTouchY = -1;
            } else if (mPreviousTouchX < 0) {
                mPreviousTouchX = x;
                mPreviousTouchY = y;
            }
            return shouldHandle;
        }

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE) {
            if (event.getPointerCount() == 1 && mCurrentTouchTarget != TOUCH_NONE) {

                x = Math.min(Math.max(x, getPaddingLeft()), getWidth() - getPaddingRight());
                y = Math.min(Math.max(y, getPaddingTop()), getHeight() - getPaddingBottom());

                updateCropViewRect(x, y);

                mPreviousTouchX = x;
                mPreviousTouchY = y;

                return true;
            }
        }

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            mPreviousTouchX = -1;
            mPreviousTouchY = -1;
            mCurrentTouchTarget = TOUCH_NONE;

            if (mCallback != null) {
                mCallback.onCropRectUpdated(mCropViewRect);
            }
        }

        return false;
    }

    /**
     * * The order of the corners is:
     * 0------->1
     * ^        |
     * |   4    |
     * |        v
     * 3<-------2
     */
    private void updateCropViewRect(float touchX, float touchY) {
        mTempRect.set(mCropViewRect);

        switch (mCurrentTouchTarget) {
            // resize rectangle
            case TOUCH_TOP_LEFT:
                mTempRect.set(touchX, touchY, mCropViewRect.right, mCropViewRect.bottom);
                break;
            case TOUCH_TOP_RIGHT:
                mTempRect.set(mCropViewRect.left, touchY, touchX, mCropViewRect.bottom);
                break;
            case TOUCH_BOTTOM_RIGHT:
                mTempRect.set(mCropViewRect.left, mCropViewRect.top, touchX, touchY);
                break;
            case TOUCH_BOTTOM_LEFT:
                mTempRect.set(touchX, mCropViewRect.top, mCropViewRect.right, touchY);
                break;
            // move rectangle
            case TOUCH_INTERIOR:
                mTempRect.offset(touchX - mPreviousTouchX, touchY - mPreviousTouchY);
                if (mTempRect.left > getLeft() && mTempRect.top > getTop()
                        && mTempRect.right < getRight() && mTempRect.bottom < getBottom()) {
                    mCropViewRect.set(mTempRect);
                    updateGridPoints();
                    postInvalidate();
                }
                return;
        }

        boolean changeHeight = mTempRect.height() >= mCropRectMinSize;
        boolean changeWidth = mTempRect.width() >= mCropRectMinSize;
        mCropViewRect.set(
                changeWidth ? mTempRect.left : mCropViewRect.left,
                changeHeight ? mTempRect.top : mCropViewRect.top,
                changeWidth ? mTempRect.right : mCropViewRect.right,
                changeHeight ? mTempRect.bottom : mCropViewRect.bottom);

        if (changeHeight || changeWidth) {
            updateGridPoints();
            postInvalidate();
        }
    }

    /** Corners take priority, then locked edges, then the movable interior. */
    private int getCurrentTouchIndex(float touchX, float touchY) {
        int closestPointIndex = TOUCH_NONE;
        double closestPointDistance = mTouchPointThreshold;
        for (int i = 0; i < 8; i += 2) {
            if (mImageLocked) {
                boolean left = i / 2 == TOUCH_TOP_LEFT || i / 2 == TOUCH_BOTTOM_LEFT;
                boolean top = i / 2 == TOUCH_TOP_LEFT || i / 2 == TOUCH_TOP_RIGHT;
                float inwardX = left ? touchX - mCropViewRect.left : mCropViewRect.right - touchX;
                float inwardY = top ? touchY - mCropViewRect.top : mCropViewRect.bottom - touchY;
                // Limit inward corner reach independently on each axis. Even a tiny or
                // narrow crop retains edge midpoints and an interior; outward slop is unchanged.
                if (inwardX > mCropViewRect.width() / 4 || inwardY > mCropViewRect.height() / 4) {
                    continue;
                }
            }
            double distanceToCorner = Math.sqrt(Math.pow(touchX - mCropGridCorners[i], 2)
                    + Math.pow(touchY - mCropGridCorners[i + 1], 2));
            if (distanceToCorner < closestPointDistance) {
                closestPointDistance = distanceToCorner;
                closestPointIndex = i / 2;
            }
        }

        if (closestPointIndex != TOUCH_NONE) {
            return closestPointIndex;
        }
        if (mImageLocked) {
            closestPointIndex = getLockedEdgeTarget(touchX, touchY);
        }
        if (getFreestyleCropMode() == FREESTYLE_CROP_MODE_ENABLE && closestPointIndex == TOUCH_NONE
                && mCropViewRect.contains(touchX, touchY)) {
            return TOUCH_INTERIOR;
        }
        return closestPointIndex;
    }

    private int getLockedEdgeTarget(float touchX, float touchY) {
        int target = TOUCH_NONE;
        double closestDistance = mTouchPointThreshold;
        for (int edge = TOUCH_LEFT_EDGE; edge <= TOUCH_BOTTOM_EDGE; edge++) {
            boolean vertical = edge == TOUCH_LEFT_EDGE || edge == TOUCH_RIGHT_EDGE;
            float edgeX = edge == TOUCH_LEFT_EDGE ? mCropViewRect.left : mCropViewRect.right;
            float edgeY = edge == TOUCH_TOP_EDGE ? mCropViewRect.top : mCropViewRect.bottom;
            float inwardDistance = vertical
                    ? (edge == TOUCH_LEFT_EDGE ? touchX - edgeX : edgeX - touchX)
                    : (edge == TOUCH_TOP_EDGE ? touchY - edgeY : edgeY - touchY);
            // Shrink only the inward band, so the central half remains draggable without
            // stealing actual edge hits. Projection keeps hit testing on finite segments.
            if (inwardDistance > (vertical ? mCropViewRect.width() : mCropViewRect.height()) / 4) {
                continue;
            }
            float nearestX = vertical ? edgeX : clamp(touchX, mCropViewRect.left, mCropViewRect.right);
            float nearestY = vertical ? clamp(touchY, mCropViewRect.top, mCropViewRect.bottom) : edgeY;
            double distance = Math.hypot(touchX - nearestX, touchY - nearestY);
            // Strict comparison makes equal-distance ties stable in left/top/right/bottom order.
            if (distance < closestDistance) {
                closestDistance = distance;
                target = edge;
            }
        }
        return target;
    }

    /**
     * This method draws dimmed area around the crop bounds.
     *
     * @param canvas - valid canvas object
     */
    protected void drawDimmedLayer(@NonNull Canvas canvas) {
        canvas.save();
        if (mCircleDimmedLayer && !mImageLocked) {
            canvas.clipPath(mCircularPath, Region.Op.DIFFERENCE);
        } else {
            canvas.clipRect(mCropViewRect, Region.Op.DIFFERENCE);
        }
        canvas.drawColor(mDimmedColor);
        canvas.restore();

        if (mCircleDimmedLayer && !mImageLocked) { // Draw 1px stroke to fix antialias
            canvas.drawCircle(mCropViewRect.centerX(), mCropViewRect.centerY(),
                    Math.min(mCropViewRect.width(), mCropViewRect.height()) / 2.f, mDimmedStrokePaint);
        }
    }

    /**
     * This method draws crop bounds (empty rectangle)
     * and crop guidelines (vertical and horizontal lines inside the crop bounds) if needed.
     *
     * @param canvas - valid canvas object
     */
    protected void drawCropGrid(@NonNull Canvas canvas) {
        if (mShowCropGrid) {
            if (mGridPoints == null && !mCropViewRect.isEmpty()) {

                mGridPoints = new float[(mCropGridRowCount) * 4 + (mCropGridColumnCount) * 4];

                int index = 0;
                for (int i = 0; i < mCropGridRowCount; i++) {
                    mGridPoints[index++] = mCropViewRect.left;
                    mGridPoints[index++] = (mCropViewRect.height() * (((float) i + 1.0f) / (float) (mCropGridRowCount + 1))) + mCropViewRect.top;
                    mGridPoints[index++] = mCropViewRect.right;
                    mGridPoints[index++] = (mCropViewRect.height() * (((float) i + 1.0f) / (float) (mCropGridRowCount + 1))) + mCropViewRect.top;
                }

                for (int i = 0; i < mCropGridColumnCount; i++) {
                    mGridPoints[index++] = (mCropViewRect.width() * (((float) i + 1.0f) / (float) (mCropGridColumnCount + 1))) + mCropViewRect.left;
                    mGridPoints[index++] = mCropViewRect.top;
                    mGridPoints[index++] = (mCropViewRect.width() * (((float) i + 1.0f) / (float) (mCropGridColumnCount + 1))) + mCropViewRect.left;
                    mGridPoints[index++] = mCropViewRect.bottom;
                }
            }

            if (mGridPoints != null) {
                canvas.drawLines(mGridPoints, mCropGridPaint);
            }
        }

        if (mShowCropFrame) {
            canvas.drawRect(mCropViewRect, mCropFramePaint);
            if (mImageLocked && mCropFramePaint.getStrokeWidth() > 0) {
                float halfWidth = Math.min(mCropRectCornerTouchAreaLineLength / 2f, mCropViewRect.width() / 8);
                float halfHeight = Math.min(mCropRectCornerTouchAreaLineLength / 2f, mCropViewRect.height() / 8);
                float strokeWidth = mCropFramePaint.getStrokeWidth();
                mCropFramePaint.setStrokeWidth(strokeWidth * 3);
                canvas.drawLine(mCropViewRect.centerX() - halfWidth, mCropViewRect.top,
                        mCropViewRect.centerX() + halfWidth, mCropViewRect.top, mCropFramePaint);
                canvas.drawLine(mCropViewRect.centerX() - halfWidth, mCropViewRect.bottom,
                        mCropViewRect.centerX() + halfWidth, mCropViewRect.bottom, mCropFramePaint);
                canvas.drawLine(mCropViewRect.left, mCropViewRect.centerY() - halfHeight,
                        mCropViewRect.left, mCropViewRect.centerY() + halfHeight, mCropFramePaint);
                canvas.drawLine(mCropViewRect.right, mCropViewRect.centerY() - halfHeight,
                        mCropViewRect.right, mCropViewRect.centerY() + halfHeight, mCropFramePaint);
                mCropFramePaint.setStrokeWidth(strokeWidth);
            }
        }

        if (getFreestyleCropMode() != FREESTYLE_CROP_MODE_DISABLE && (!mImageLocked || mShowCropFrame)) {
            canvas.save();

            mTempRect.set(mCropViewRect);
            mTempRect.inset(mCropRectCornerTouchAreaLineLength, -mCropRectCornerTouchAreaLineLength);
            canvas.clipRect(mTempRect, Region.Op.DIFFERENCE);

            mTempRect.set(mCropViewRect);
            mTempRect.inset(-mCropRectCornerTouchAreaLineLength, mCropRectCornerTouchAreaLineLength);
            canvas.clipRect(mTempRect, Region.Op.DIFFERENCE);

            canvas.drawRect(mCropViewRect, mCropFrameCornersPaint);

            canvas.restore();
        }
    }

    /**
     * This method extracts all needed values from the styled attributes.
     * Those are used to configure the view.
     */
    @SuppressWarnings("deprecation")
    protected void processStyledAttributes(@NonNull TypedArray a) {
        mCircleDimmedLayer = a.getBoolean(R.styleable.ucrop_UCropView_ucrop_circle_dimmed_layer, DEFAULT_CIRCLE_DIMMED_LAYER);
        mDimmedColor = a.getColor(R.styleable.ucrop_UCropView_ucrop_dimmed_color,
                getResources().getColor(R.color.ucrop_color_default_dimmed));
        mDimmedStrokePaint.setColor(mDimmedColor);
        mDimmedStrokePaint.setStyle(Paint.Style.STROKE);
        mDimmedStrokePaint.setStrokeWidth(1);

        initCropFrameStyle(a);
        mShowCropFrame = a.getBoolean(R.styleable.ucrop_UCropView_ucrop_show_frame, DEFAULT_SHOW_CROP_FRAME);

        initCropGridStyle(a);
        mShowCropGrid = a.getBoolean(R.styleable.ucrop_UCropView_ucrop_show_grid, DEFAULT_SHOW_CROP_GRID);
    }

    /**
     * This method setups Paint object for the crop bounds.
     */
    @SuppressWarnings("deprecation")
    private void initCropFrameStyle(@NonNull TypedArray a) {
        int cropFrameStrokeSize = a.getDimensionPixelSize(R.styleable.ucrop_UCropView_ucrop_frame_stroke_size,
                getResources().getDimensionPixelSize(R.dimen.ucrop_default_crop_frame_stoke_width));
        int cropFrameColor = a.getColor(R.styleable.ucrop_UCropView_ucrop_frame_color,
                getResources().getColor(R.color.ucrop_color_default_crop_frame));

        mCropFramePaint.setStrokeWidth(cropFrameStrokeSize);
        mCropFramePaint.setColor(cropFrameColor);
        mCropFramePaint.setStyle(Paint.Style.STROKE);

        mCropFrameCornersPaint.setStrokeWidth(cropFrameStrokeSize * 3);
        mCropFrameCornersPaint.setColor(cropFrameColor);
        mCropFrameCornersPaint.setStyle(Paint.Style.STROKE);
    }

    /**
     * This method setups Paint object for the crop guidelines.
     */
    @SuppressWarnings("deprecation")
    private void initCropGridStyle(@NonNull TypedArray a) {
        int cropGridStrokeSize = a.getDimensionPixelSize(R.styleable.ucrop_UCropView_ucrop_grid_stroke_size,
                getResources().getDimensionPixelSize(R.dimen.ucrop_default_crop_grid_stoke_width));
        int cropGridColor = a.getColor(R.styleable.ucrop_UCropView_ucrop_grid_color,
                getResources().getColor(R.color.ucrop_color_default_crop_grid));
        mCropGridPaint.setStrokeWidth(cropGridStrokeSize);
        mCropGridPaint.setColor(cropGridColor);

        mCropGridRowCount = a.getInt(R.styleable.ucrop_UCropView_ucrop_grid_row_count, DEFAULT_CROP_GRID_ROW_COUNT);
        mCropGridColumnCount = a.getInt(R.styleable.ucrop_UCropView_ucrop_grid_column_count, DEFAULT_CROP_GRID_COLUMN_COUNT);
    }


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FREESTYLE_CROP_MODE_DISABLE, FREESTYLE_CROP_MODE_ENABLE, FREESTYLE_CROP_MODE_ENABLE_WITH_PASS_THROUGH})
    public @interface FreestyleMode {
    }

}
