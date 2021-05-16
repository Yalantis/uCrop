package com.yalantis.ucrop;

import android.net.Uri;

public class RatioSingleton {
    private static int x, y;

    public static int getX() {
        return x;
    }

    public static void setX(int x) {
        RatioSingleton.x = x;
    }

    public static int getY() {
        return y;
    }

    public static void setY(int y) {
        RatioSingleton.y = y;
    }
}
