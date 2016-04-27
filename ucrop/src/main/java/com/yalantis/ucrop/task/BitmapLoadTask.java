package com.yalantis.ucrop.task;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.yalantis.ucrop.callback.BitmapLoadCallback;
import com.yalantis.ucrop.util.BitmapLoadUtils;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;
import okio.Okio;
import okio.Sink;

/**
 * Creates and returns a Bitmap for a given Uri(String url).
 * inSampleSize is calculated based on requiredWidth property. However can be adjusted if OOM occurs.
 * If any EXIF config is found - bitmap is transformed properly.
 */
public class BitmapLoadTask extends AsyncTask<Void, Void, BitmapLoadTask.BitmapWorkerResult> {

    private static final String TAG = "BitmapWorkerTask";

    private final Context mContext;
    private Uri mInputUri;
    private final Uri mOutputUri;
    private final int mRequiredWidth;
    private final int mRequiredHeight;

    private final BitmapLoadCallback mBitmapLoadCallback;

    public static class BitmapWorkerResult {

        Bitmap mBitmapResult;
        Exception mBitmapWorkerException;

        public BitmapWorkerResult(@Nullable Bitmap bitmapResult, @Nullable Exception bitmapWorkerException) {
            mBitmapResult = bitmapResult;
            mBitmapWorkerException = bitmapWorkerException;
        }

    }

    public BitmapLoadTask(@NonNull Context context,
                          @Nullable Uri inputUri, @Nullable Uri outputUri,
                          int requiredWidth, int requiredHeight,
                          BitmapLoadCallback loadCallback) {
        mContext = context;
        mInputUri = inputUri;
        mOutputUri = outputUri;
        mRequiredWidth = requiredWidth;
        mRequiredHeight = requiredHeight;
        mBitmapLoadCallback = loadCallback;
    }

    @Override
    @NonNull
    protected BitmapWorkerResult doInBackground(Void... params) {
        if (mInputUri == null || mOutputUri == null) {
            return new BitmapWorkerResult(null, new NullPointerException("Uri cannot be null"));
        }

        if ("http".equals(mInputUri.getScheme()) || "https".equals(mInputUri.getScheme())) {
            try {
                downloadFile(mInputUri, mOutputUri);
            } catch (NullPointerException | IOException e) {
                Log.e(TAG, "Downloading failed", e);
                return new BitmapWorkerResult(null, e);
            }
        }

        final ParcelFileDescriptor parcelFileDescriptor;
        try {
            parcelFileDescriptor = mContext.getContentResolver().openFileDescriptor(mInputUri, "r");
        } catch (FileNotFoundException e) {
            return new BitmapWorkerResult(null, e);
        }

        final FileDescriptor fileDescriptor;
        if (parcelFileDescriptor != null) {
            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        } else {
            return new BitmapWorkerResult(null, new NullPointerException("ParcelFileDescriptor was null for given Uri"));
        }

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        if (options.outWidth == -1 || options.outHeight == -1) {
            return new BitmapWorkerResult(null, new IllegalArgumentException("Bounds for bitmap could not be retrieved from Uri"));
        }

        options.inSampleSize = BitmapLoadUtils.calculateInSampleSize(options, mRequiredWidth, mRequiredHeight);
        options.inJustDecodeBounds = false;

        Bitmap decodeSampledBitmap = null;

        boolean decodeAttemptSuccess = false;
        while (!decodeAttemptSuccess) {
            try {
                decodeSampledBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
                decodeAttemptSuccess = true;
            } catch (OutOfMemoryError error) {
                Log.e(TAG, "doInBackground: BitmapFactory.decodeFileDescriptor: ", error);
                options.inSampleSize++;
            }
        }

        if (decodeSampledBitmap == null) {
            return new BitmapWorkerResult(null, new IllegalArgumentException("Bitmap could not be decoded from Uri"));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            BitmapLoadUtils.close(parcelFileDescriptor);
        }

        int exifOrientation = BitmapLoadUtils.getExifOrientation(mContext, mInputUri);
        int exifDegrees = BitmapLoadUtils.exifToDegrees(exifOrientation);
        int exifTranslation = BitmapLoadUtils.exifToTranslation(exifOrientation);

        Matrix matrix = new Matrix();
        if (exifDegrees != 0) {
            matrix.preRotate(exifDegrees);
        }
        if (exifTranslation != 1) {
            matrix.postScale(exifTranslation, 1);
        }
        if (!matrix.isIdentity()) {
            return new BitmapWorkerResult(BitmapLoadUtils.transformBitmap(decodeSampledBitmap, matrix), null);
        }

        return new BitmapWorkerResult(decodeSampledBitmap, null);
    }

    private void downloadFile(@NonNull Uri inputUri, @NonNull Uri outputUri) throws NullPointerException, IOException {
        OkHttpClient client = new OkHttpClient();

        BufferedSource source = null;
        Sink sink = null;
        Response response = null;
        try {
            Request request = new Request.Builder()
                    .url(inputUri.toString())
                    .build();
            response = client.newCall(request).execute();
            source = response.body().source();

            OutputStream outputStream = mContext.getContentResolver().openOutputStream(outputUri);
            if (outputStream != null) {
                sink = Okio.sink(outputStream);
                source.readAll(sink);
            } else {
                throw new NullPointerException("OutputStream for given output Uri was null");
            }
        } finally {
            BitmapLoadUtils.close(source);
            BitmapLoadUtils.close(sink);
            if (response != null) {
                BitmapLoadUtils.close(response.body());
            }
        }

        client.dispatcher().cancelAll();

        // swap uris, because input image was downloaded to the output destination
        // (cropped image will override it later)
        mInputUri = mOutputUri;
    }

    @Override
    protected void onPostExecute(@NonNull BitmapWorkerResult result) {
        if (result.mBitmapWorkerException == null) {
            mBitmapLoadCallback.onBitmapLoaded(result.mBitmapResult);
        } else {
            mBitmapLoadCallback.onFailure(result.mBitmapWorkerException);
        }
    }

}