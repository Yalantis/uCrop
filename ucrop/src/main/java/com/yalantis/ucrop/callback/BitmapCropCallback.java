package com.yalantis.ucrop.callback;

import android.support.annotation.NonNull;

public interface BitmapCropCallback {

    void onBitmapCropped();

    void onCropFailure(@NonNull Exception bitmapCropException);

}