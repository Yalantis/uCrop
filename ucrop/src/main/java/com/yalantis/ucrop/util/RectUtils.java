package com.yalantis.ucrop.util;

import android.graphics.RectF;
import android.graphics.drawable.Drawable;

public class RectUtils {

    /**
     * Gets a float array of the 2D coordinates representing a rectangles
     * corners.
     * The order of the corners in the float array is:
     * 0------->1
     * ^        |
     * |        |
     * |        v
     * 3<-------2
     *
     * @param r the rectangle to get the corners of
     * @return the float array of corners (8 floats)
     */
    public static float[] getCornersFromRect(RectF r) {
        return new float[]{
                r.left, r.top,
                r.right, r.top,
                r.right, r.bottom,
                r.left, r.bottom
        };
    }

    /**
     * Gets a float array of two lengths representing a rectangles width and height
     * The order of the corners in the input float array is:
     * 0------->1
     * ^        |
     * |        |
     * |        v
     * 3<-------2
     *
     * @param corners the float array of corners (8 floats)
     * @return the float array of width and height (2 floats)
     */
    public static float[] getRectSidesFromCorners(float[] corners) {
        return new float[]{(float) Math.sqrt(Math.pow(corners[0] - corners[2], 2) + Math.pow(corners[1] - corners[3], 2)),
                (float) Math.sqrt(Math.pow(corners[2] - corners[4], 2) + Math.pow(corners[3] - corners[5], 2))};
    }

    public static float[] getCenterFromRect(RectF r) {
        return new float[]{r.centerX(), r.centerY()};
    }

    /**
     * Takes an array of 2D coordinates representing corners and returns the
     * smallest rectangle containing those coordinates.
     *
     * @param array array of 2D coordinates
     * @return smallest rectangle containing coordinates
     */
    public static RectF trapToRect(float[] array) {
        RectF r = new RectF(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        for (int i = 1; i < array.length; i += 2) {
            float x = Math.round(array[i - 1] * 10) / 10.f;
            float y = Math.round(array[i] * 10) / 10.f;
            r.left = (x < r.left) ? x : r.left;
            r.top = (y < r.top) ? y : r.top;
            r.right = (x > r.right) ? x : r.right;
            r.bottom = (y > r.bottom) ? y : r.bottom;
        }
        r.sort();
        return r;
    }

    /**
     * Takes an rect in image space (received rect can only be in image space, since you can't
     * know the view's width and height from the outside). So in case you want to process the image
     * outside uCrop, you will only have coordinates in image space and not in uCrop's view space.
     * Those need to be converted.
     *
     * @param rectInImageSpace RectF in image space
     * @param viewWidth The view's width
     * @param viewHeight The view's height
     * @param drawable The drawable object to retrieve drawable width and height
     *
     * @return A new RectF object representing how the RectF should be drawn on screen.
     */
    public static RectF convertImageSpaceRectToCropViewRect(RectF rectInImageSpace, int viewWidth, int viewHeight, Drawable drawable) {
        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();

        double widthAspectRatio = (double) viewWidth / drawableWidth;
        double heightAspectRatio = (double) viewHeight / drawableHeight;

        return new RectF(
                (float) (rectInImageSpace.left * widthAspectRatio),
                (float) (rectInImageSpace.top * heightAspectRatio),
                (float) (rectInImageSpace.right * widthAspectRatio),
                (float) (rectInImageSpace.bottom * heightAspectRatio)
        );
    }

}