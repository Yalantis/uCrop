package com.yalantis.ucrop.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;
import okio.Okio;
import okio.Sink;

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 */
public class BitmapLoadUtils {

    private static final String TAG = "BitmapLoadUtils";

    public interface BitmapLoadCallback {

        void onBitmapLoaded(@NonNull Bitmap bitmap);

        void onFailure(@NonNull Exception bitmapWorkerException);

    }

    public static void decodeBitmapInBackground(@NonNull Context context,
                                                @Nullable Uri uri, @Nullable Uri outputUri,
                                                int requiredWidth, int requiredHeight,
                                                BitmapLoadCallback loadCallback) {
        new BitmapWorkerTask(context, uri, outputUri, requiredWidth, requiredHeight, loadCallback).execute();
    }

    static class BitmapWorkerResult {

        Bitmap mBitmapResult;
        Exception mBitmapWorkerException;

        public BitmapWorkerResult(@Nullable Bitmap bitmapResult, @Nullable Exception bitmapWorkerException) {
            mBitmapResult = bitmapResult;
            mBitmapWorkerException = bitmapWorkerException;
        }

    }

    /**
     * Creates and returns a Bitmap for a given Uri(String url).
     * inSampleSize is calculated based on requiredWidth property. However can be adjusted if OOM occurs.
     * If any EXIF config is found - bitmap is transformed properly.
     */
    static class BitmapWorkerTask extends AsyncTask<Void, Void, BitmapWorkerResult> {

        private final Context mContext;
        private Uri mInputUri;
        private final Uri mOutputUri;
        private final int mRequiredWidth;
        private final int mRequiredHeight;

        private final BitmapLoadCallback mBitmapLoadCallback;

        public BitmapWorkerTask(@NonNull Context context,
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

            options.inSampleSize = calculateInSampleSize(options, mRequiredWidth, mRequiredHeight);
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
                close(parcelFileDescriptor);
            }

            int exifOrientation = getExifOrientation(mContext, mInputUri);
            int exifDegrees = exifToDegrees(exifOrientation);
            int exifTranslation = exifToTranslation(exifOrientation);

            Matrix matrix = new Matrix();
            if (exifDegrees != 0) {
                matrix.preRotate(exifDegrees);
            }
            if (exifTranslation != 1) {
                matrix.postScale(exifTranslation, 1);
            }
            if (!matrix.isIdentity()) {
                return new BitmapWorkerResult(transformBitmap(decodeSampledBitmap, matrix), null);
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
                close(source);
                close(sink);
                if (response != null) {
                    close(response.body());
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

    public static Bitmap transformBitmap(@NonNull Bitmap bitmap, @NonNull Matrix transformMatrix) {
        try {
            Bitmap converted = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), transformMatrix, true);
            if (bitmap != converted) {
                bitmap.recycle();
                bitmap = converted;
            }
        } catch (OutOfMemoryError error) {
            Log.e(TAG, "transformBitmap: ", error);
        }
        return bitmap;
    }

    public static int calculateInSampleSize(@NonNull BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width lower or equal to the requested height and width.
            while ((height / inSampleSize) > reqHeight || (width / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private static int getExifOrientation(@NonNull Context context, @NonNull Uri imageUri) {
        int orientation = ExifInterface.ORIENTATION_UNDEFINED;
        try {
            InputStream stream = context.getContentResolver().openInputStream(imageUri);
            if (stream == null) {
                return orientation;
            }
            orientation = new ImageHeaderParser(stream).getOrientation();
            close(stream);
        } catch (IOException e) {
            Log.e(TAG, "getExifOrientation: " + imageUri.toString(), e);
        }
        return orientation;
    }

    private static int exifToDegrees(int exifOrientation) {
        int rotation;
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
            case ExifInterface.ORIENTATION_TRANSPOSE:
                rotation = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                rotation = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
            case ExifInterface.ORIENTATION_TRANSVERSE:
                rotation = 270;
                break;
            default:
                rotation = 0;
        }
        return rotation;
    }

    private static int exifToTranslation(int exifOrientation) {
        int translation;
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
            case ExifInterface.ORIENTATION_TRANSPOSE:
            case ExifInterface.ORIENTATION_TRANSVERSE:
                translation = -1;
                break;
            default:
                translation = 1;
        }
        return translation;
    }

    @SuppressWarnings("ConstantConditions")
    public static void close(@Nullable Closeable c) {
        if (c != null && c instanceof Closeable) { // java.lang.IncompatibleClassChangeError: interface not implemented
            try {
                c.close();
            } catch (IOException e) {
                // silence
            }
        }
    }

}