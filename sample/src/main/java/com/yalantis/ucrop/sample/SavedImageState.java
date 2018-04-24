package com.yalantis.ucrop.sample;

import android.graphics.RectF;

import com.yalantis.ucrop.UCrop;

/**
 * @author azri92
 * Model class to demonstrate how the saved image state could be stored and reused.
 */
public class SavedImageState {

    private String originalPath;
    private String destinationPath;
    private float[] imageMatrixValues; // 9 values of matrix
    private RectF cropRect;
    private UCrop ucropConfig;

    public SavedImageState() {}

    public SavedImageState(String originalPath, String destinationPath) {
        this.originalPath = originalPath;
        this.destinationPath = destinationPath;
    }

    public void setImageMatrixValues(float[] imageMatrixValues) {
        this.imageMatrixValues = imageMatrixValues;
    }

    public void setCropRect(RectF cropRect) {
        this.cropRect = cropRect;
    }

    public void setUcropConfig(UCrop ucropConfig) {
        this.ucropConfig = ucropConfig;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public float[] getImageMatrixValues() {
        return imageMatrixValues;
    }

    public RectF getCropRect() {
        return cropRect;
    }

    public UCrop getUcropConfig() {
        return ucropConfig;
    }
}
