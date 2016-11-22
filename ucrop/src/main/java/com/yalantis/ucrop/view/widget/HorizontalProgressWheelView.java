package com.yalantis.ucrop.view.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.yalantis.ucrop.R;

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 */
public class HorizontalProgressWheelView extends View {

    private final Rect mCanvasClipBounds = new Rect();

    private ScrollingListener mScrollingListener;
    private float mLastTouchedPosition;

    private Paint mProgressLinePaint;
    private int mProgressLineHeight;

    private boolean mScrollStarted;
    private float mTotalScrollDistance;

    private int mMiddleLineColor;
    private int mProgressLineColor;
    private int mLinesCount;
    private int mProgressLineTotalWidth;

    public HorizontalProgressWheelView(Context context) {
        this(context, null);
    }

    public HorizontalProgressWheelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HorizontalProgressWheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public HorizontalProgressWheelView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setScrollingListener(ScrollingListener scrollingListener) {
        mScrollingListener = scrollingListener;
    }

    public void setMiddleLineColor(@ColorInt int middleLineColor) {
        mMiddleLineColor = middleLineColor;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchedPosition = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                if (mScrollingListener != null) {
                    mScrollStarted = false;
                    mScrollingListener.onScrollEnd();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float distance = event.getX() - mLastTouchedPosition;
                if (distance != 0) {
                    if (!mScrollStarted) {
                        mScrollStarted = true;
                        if (mScrollingListener != null) {
                            mScrollingListener.onScrollStart();
                        }
                    }
                    onScrollEvent(event, distance);
                }
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.getClipBounds(mCanvasClipBounds);

        if (mLinesCount == 0) {
            mLinesCount = mCanvasClipBounds.width() / mProgressLineTotalWidth;

            // Make the count be odd number so we can center the lines.
            if (mLinesCount % 2 == 0) {
                if (mLinesCount == 0) {
                    mLinesCount = 1;
                } else {
                    mLinesCount--;
                }
            }
        }

        float deltaX = mTotalScrollDistance % (float) mProgressLineTotalWidth;

        // Make the value of deltaX to be between (-mProgressLineTotalWidth / 2, mProgressLineTotalWidth / 2).
        if (deltaX < -mProgressLineTotalWidth / 2) {
            deltaX += mProgressLineTotalWidth;
        } else if (deltaX > mProgressLineTotalWidth / 2) {
            deltaX -= mProgressLineTotalWidth;
        }

        // Set center progress line properties first and all other lines are relative to center progress line.
        int centerProgressLine = mLinesCount / 2;
        float centerProgressLineX = mCanvasClipBounds.centerX() - deltaX;

        mProgressLinePaint.setColor(mProgressLineColor);

        for (int i = 0; i < mLinesCount; i++) {

            // Relative to center progress line.
            float progressLineX = (i - centerProgressLine) * mProgressLineTotalWidth + centerProgressLineX;

            if (i < (mLinesCount / 4)) {
                mProgressLinePaint.setAlpha((int) (255 * (i / (float) (mLinesCount / 4))));
            } else if (i > (mLinesCount * 3 / 4)) {
                mProgressLinePaint.setAlpha((int) (255 * ((mLinesCount - i) / (float) (mLinesCount / 4))));
            } else {
                mProgressLinePaint.setAlpha(255);
            }

            canvas.drawLine(
                    progressLineX,
                    mCanvasClipBounds.centerY() - mProgressLineHeight / 4.0f,
                    progressLineX,
                    mCanvasClipBounds.centerY() + mProgressLineHeight / 4.0f, mProgressLinePaint);
        }

        mProgressLinePaint.setColor(mMiddleLineColor);
        canvas.drawLine(mCanvasClipBounds.centerX(),
                        mCanvasClipBounds.centerY() - mProgressLineHeight / 2.0f,
                        mCanvasClipBounds.centerX(),
                        mCanvasClipBounds.centerY() + mProgressLineHeight / 2.0f,
                        mProgressLinePaint);

    }

    private void onScrollEvent(MotionEvent event, float distance) {
        mTotalScrollDistance -= distance;
        postInvalidate();
        mLastTouchedPosition = event.getX();
        if (mScrollingListener != null) {
            mScrollingListener.onScroll(-distance, mTotalScrollDistance);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Allow recalculate of lines count when on draw.
        mLinesCount = 0;
    }

    private void init() {
        mMiddleLineColor = ContextCompat.getColor(getContext(), R.color.ucrop_color_progress_wheel_line);
        mProgressLineColor = ContextCompat.getColor(getContext(), R.color.ucrop_color_progress_wheel_line);

        int mProgressLineWidth = getContext().getResources().getDimensionPixelSize(R.dimen.ucrop_width_horizontal_wheel_progress_line);
        mProgressLineHeight = getContext().getResources().getDimensionPixelSize(R.dimen.ucrop_height_horizontal_wheel_progress_line);
        int mProgressLineMargin = getContext().getResources().getDimensionPixelSize(R.dimen.ucrop_margin_horizontal_wheel_progress_line);

        mProgressLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mProgressLinePaint.setStyle(Paint.Style.STROKE);
        mProgressLinePaint.setStrokeWidth(mProgressLineWidth);

        mProgressLineTotalWidth = mProgressLineWidth + mProgressLineMargin;
    }

    public interface ScrollingListener {

        void onScrollStart();

        void onScroll(float delta, float totalDistance);

        void onScrollEnd();
    }

}
