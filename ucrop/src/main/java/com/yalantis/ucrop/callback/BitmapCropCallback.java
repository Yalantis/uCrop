package com.yalantis.ucrop.callback;

import android.graphics.RectF;
import android.net.Uri;
import android.support.annotation.NonNull;

public interface BitmapCropCallback {

    void onBitmapCropped(
            @NonNull Uri resultUri,
            int offsetX,
            int offsetY,
            int imageWidth,
            int imageHeight,
            float[] imageMatrixValues,
            RectF cropRect);

    void onCropFailure(@NonNull Throwable t);

}