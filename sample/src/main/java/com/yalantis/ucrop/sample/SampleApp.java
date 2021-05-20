package com.yalantis.ucrop.sample;

import android.app.Application;

import com.yalantis.ucrop.UCropHttpClientStore;

import java.util.Collections;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;

public class SampleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        setUcropHttpClient();
    }

    private void setUcropHttpClient() {
        ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .allEnabledCipherSuites()
            .allEnabledTlsVersions()
            .build();

        OkHttpClient client = new OkHttpClient.Builder()
            .connectionSpecs(Collections.singletonList(cs))
            .build();

        UCropHttpClientStore.INSTANCE.setClient(client);
    }
}
