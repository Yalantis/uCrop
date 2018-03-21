package com.yalantis.ucrop;

public interface UCropFragmentCallback {

    void loadingProgress(boolean showLoader);

    void onCropFinish(UCropFragment.UCropResult result);

}
