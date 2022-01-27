package com.yalantis.ucrop;

import androidx.annotation.NonNull;

import okhttp3.OkHttpClient;

public class UCropHttpClientStore {

    private UCropHttpClientStore() {}

    public final static UCropHttpClientStore INSTANCE = new UCropHttpClientStore();

    private OkHttpClient client;

    @NonNull
    public OkHttpClient getClient() {
        if (client == null) {
            client = new OkHttpClient();
        }
        return client;
    }

    public void setClient(@NonNull OkHttpClient client) {
        this.client = client;
    }
}
