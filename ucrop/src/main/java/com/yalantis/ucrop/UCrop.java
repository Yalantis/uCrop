package com.yalantis.ucrop;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 * <p/>
 * Builder class to ease Intent setup.
 */
public class UCrop {

    public static final int REQUEST_CROP = 69;
    public static final int RESULT_ERROR = 96;

    public static final String EXTRA_INPUT_URI = "InputUri";
    public static final String EXTRA_OUTPUT_URI = "OutputUri";
    public static final String EXTRA_ERROR = "Error";

    public static final String EXTRA_ASPECT_RATIO_SET = "AspectRatioSet";
    public static final String EXTRA_ASPECT_RATIO_X = "AspectRatioX";
    public static final String EXTRA_ASPECT_RATIO_Y = "AspectRatioY";

    public static final String EXTRA_MAX_SIZE_SET = "MaxSizeSet";
    public static final String EXTRA_MAX_SIZE_X = "MaxSizeX";
    public static final String EXTRA_MAX_SIZE_Y = "MaxSizeY";

    public static final String EXTRA_OPTIONS = "Options";

    private Intent mCropIntent;

    /**
     * This method creates new Intent builder and sets both source and destination image URIs.
     *
     * @param source      Uri for image to crop
     * @param destination Uri for saving the cropped image
     */
    public static UCrop of(@NonNull Uri source, @NonNull Uri destination) {
        return new UCrop(source, destination);
    }

    private UCrop(@NonNull Uri source, @NonNull Uri destination) {
        mCropIntent = new Intent();
        mCropIntent.putExtra(EXTRA_INPUT_URI, source);
        mCropIntent.putExtra(EXTRA_OUTPUT_URI, destination);
    }

    /**
     * Set an aspect ratio for crop bounds.
     * User won't see the menu with other ratios options.
     *
     * @param x aspect ratio X
     * @param y aspect ratio Y
     */
    public UCrop withAspectRatio(@IntRange(from = 1) int x, @IntRange(from = 1) int y) {
        mCropIntent.putExtra(EXTRA_ASPECT_RATIO_SET, true);
        mCropIntent.putExtra(EXTRA_ASPECT_RATIO_X, x);
        mCropIntent.putExtra(EXTRA_ASPECT_RATIO_Y, y);
        return this;
    }

    /**
     * Set an aspect ratio for crop bounds that is evaluated from source image width and height.
     * User won't see the menu with other ratios options.
     */
    public UCrop useSourceImageAspectRatio() {
        mCropIntent.putExtra(EXTRA_ASPECT_RATIO_SET, true);
        mCropIntent.putExtra(EXTRA_ASPECT_RATIO_X, 0);
        mCropIntent.putExtra(EXTRA_ASPECT_RATIO_Y, 0);
        return this;
    }

    /**
     * Set maximum size for result cropped image.
     *
     * @param width  max cropped image width
     * @param height max cropped image height
     */
    public UCrop withMaxResultSize(@IntRange(from = 100) int width, @IntRange(from = 100) int height) {
        mCropIntent.putExtra(EXTRA_MAX_SIZE_SET, true);
        mCropIntent.putExtra(EXTRA_MAX_SIZE_X, width);
        mCropIntent.putExtra(EXTRA_MAX_SIZE_Y, height);
        return this;
    }

    public UCrop withOptions(@NonNull Options options) {
        mCropIntent.putExtra(EXTRA_OPTIONS, options);
        return this;
    }

    /**
     * Send the crop Intent from an Activity
     *
     * @param activity Activity to receive result
     */
    public void start(@NonNull Activity activity) {
        start(activity, REQUEST_CROP);
    }

    /**
     * Send the crop Intent from an Activity with a custom request code
     *
     * @param activity    Activity to receive result
     * @param requestCode requestCode for result
     */
    public void start(@NonNull Activity activity, int requestCode) {
        activity.startActivityForResult(getIntent(activity), requestCode);
    }

    /**
     * Send the crop Intent from a Fragment
     *
     * @param fragment Fragment to receive result
     */
    public void start(@NonNull Context context, @NonNull Fragment fragment) {
        start(context, fragment, REQUEST_CROP);
    }

    /**
     * Send the crop Intent from a support library Fragment
     *
     * @param fragment Fragment to receive result
     */
    public void start(@NonNull Context context, @NonNull android.support.v4.app.Fragment fragment) {
        start(context, fragment, REQUEST_CROP);
    }

    /**
     * Send the crop Intent with a custom request code
     *
     * @param fragment    Fragment to receive result
     * @param requestCode requestCode for result
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void start(@NonNull Context context, @NonNull Fragment fragment, int requestCode) {
        fragment.startActivityForResult(getIntent(context), requestCode);
    }

    /**
     * Send the crop Intent with a custom request code
     *
     * @param fragment    Fragment to receive result
     * @param requestCode requestCode for result
     */
    public void start(@NonNull Context context, @NonNull android.support.v4.app.Fragment fragment, int requestCode) {
        fragment.startActivityForResult(getIntent(context), requestCode);
    }

    /**
     * Get Intent to start {@link UCropActivity}
     *
     * @return Intent for {@link UCropActivity}
     */
    public Intent getIntent(@NonNull Context context) {
        mCropIntent.setClass(context, UCropActivity.class);
        return mCropIntent;
    }

    /**
     * Retrieve cropped image Uri from the result Intent
     *
     * @param intent crop result intent
     */
    @Nullable
    public static Uri getOutput(@NonNull Intent intent) {
        return intent.getParcelableExtra(EXTRA_OUTPUT_URI);
    }

    /**
     * Method retrieves error from the result intent.
     *
     * @param result crop result Intent
     * @return Throwable that could happen while image processing
     */
    @Nullable
    public static Throwable getError(@NonNull Intent result) {
        return (Throwable) result.getSerializableExtra(EXTRA_ERROR);
    }


    /**
     * Class that helps to setup advanced configs that are not commonly used.
     * Use it with method {@link #withOptions(Options)}
     */
    public static class Options implements Parcelable {

        private int mMaxBitmapSize;
        private String mCompressionFormatName;
        private int mCompressionQuality;
        private boolean mGesturesAlwaysEnabled;

        public Options() {
            // Set default values
            mMaxBitmapSize = 0;
            mCompressionFormatName = UCropActivity.DEFAULT_COMPRESS_FORMAT.name();
            mCompressionQuality = UCropActivity.DEFAULT_COMPRESS_QUALITY;
            mGesturesAlwaysEnabled = false;
        }

        public void setMaxBitmapSize(@IntRange(from = 100) int maxBitmapSize) {
            mMaxBitmapSize = maxBitmapSize;
        }

        public void setCompressionFormat(@NonNull Bitmap.CompressFormat format) {
            mCompressionFormatName = format.name();
        }

        public void setCompressionQuality(@IntRange(from = 1) int compressQuality) {
            mCompressionQuality = compressQuality;
        }

        public void setGesturesAlwaysEnabled(boolean gesturesAlwaysEnabled) {
            mGesturesAlwaysEnabled = gesturesAlwaysEnabled;
        }

        public int getMaxBitmapSize() {
            return mMaxBitmapSize;
        }

        public String getCompressionFormatName() {
            return mCompressionFormatName;
        }

        public int getCompressionQuality() {
            return mCompressionQuality;
        }

        public boolean isGesturesAlwaysEnabled() {
            return mGesturesAlwaysEnabled;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mMaxBitmapSize);
            dest.writeString(mCompressionFormatName);
            dest.writeInt(mCompressionQuality);
            dest.writeByte((byte) (mGesturesAlwaysEnabled ? 1 : 0));
        }

        protected Options(Parcel in) {
            mMaxBitmapSize = in.readInt();
            mCompressionFormatName = in.readString();
            mCompressionQuality = in.readInt();
            mGesturesAlwaysEnabled = in.readByte() != 0;
        }

        public static final Creator<Options> CREATOR = new Creator<Options>() {
            @Override
            public Options createFromParcel(Parcel in) {
                return new Options(in);
            }

            @Override
            public Options[] newArray(int size) {
                return new Options[size];
            }
        };

    }

}