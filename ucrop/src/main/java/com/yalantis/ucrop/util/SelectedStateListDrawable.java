package com.yalantis.ucrop.util;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;

/**
 * Hack class to properly support state drawable back to Android 1.6
 */
public class SelectedStateListDrawable extends StateListDrawable {

    private int mSelectionColor;
    private int mUnselectedColor = -1;

    public SelectedStateListDrawable(Drawable drawable, int selectionColor) {
        super();
        this.mSelectionColor = selectionColor;
        addState(new int[]{android.R.attr.state_selected}, drawable);
        addState(new int[]{}, drawable);
    }

    public SelectedStateListDrawable(Drawable drawable, int selectionColor, int unselectedColor) {
        super();
        this.mSelectionColor = selectionColor;
        this.mUnselectedColor = unselectedColor;
        addState(new int[]{android.R.attr.state_selected}, drawable);
        addState(new int[]{}, drawable);
    }

    @Override
    protected boolean onStateChange(int[] states) {
        boolean isStatePressedInArray = false;
        for (int state : states) {
            if (state == android.R.attr.state_selected) {
                isStatePressedInArray = true;
            }
        }
        if (isStatePressedInArray) {
            super.setColorFilter(mSelectionColor, PorterDuff.Mode.SRC_ATOP);
        } else if (mUnselectedColor != -1) {
            super.setColorFilter(mUnselectedColor, PorterDuff.Mode.SRC_ATOP);
        } else {
            super.clearColorFilter();
        }
        return super.onStateChange(states);
    }

    @Override
    public boolean isStateful() {
        return true;
    }

}
