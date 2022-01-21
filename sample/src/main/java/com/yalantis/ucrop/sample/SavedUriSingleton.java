package com.yalantis.ucrop.sample;

import android.net.Uri;

public class SavedUriSingleton {
    private static Uri uri;

    public static void setUri(Uri uri) {
        SavedUriSingleton.uri = uri;
    }

    public static Uri getUri() {
        return uri;
    }
}
