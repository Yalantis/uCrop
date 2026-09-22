package com.yalantis.ucrop.callback;

import android.graphics.Bitmap;

import com.yalantis.ucrop.model.ExifInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface BitmapLoadCallback {

    void onBitmapLoaded(@NonNull Bitmap bitmap, @NonNull ExifInfo exifInfo, @NonNull String imageInputPath, @Nullable String imageOutputPath);

    /**
     * Supplies EXIF-corrected full-resolution dimensions, before preview sampling.
     * Existing callbacks continue to receive the original signature.
     */
    default void onBitmapLoaded(@NonNull Bitmap bitmap, @NonNull ExifInfo exifInfo,
                               @NonNull String imageInputPath, @Nullable String imageOutputPath,
                               int sourceWidth, int sourceHeight) {
        onBitmapLoaded(bitmap, exifInfo, imageInputPath, imageOutputPath);
    }

    void onFailure(@NonNull Exception bitmapWorkerException);

}