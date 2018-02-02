package com.yalantis.ucrop.callback;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface BitmapCropCallback {

    void onBitmapCropped(@Nullable Uri resultUri, int offsetX, int offsetY, int imageWidth, int imageHeight, int originWidth, int originHeight, Uri originUri);

    void onCropFailure(@NonNull Throwable t);

}