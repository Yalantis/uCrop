package com.yalantis.ucrop.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 */
public class BitmapLoadUtils {

    private static final String TAG = "BitmapLoadUtils";

    @Nullable
    public static Bitmap decode(@NonNull Context context, @Nullable Uri uri,
                                int requiredWidth, int requiredHeight, @Nullable Matrix currentMatrix) throws Exception {
        if (uri == null) {
            return null;
        }

        final ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor;
        if (parcelFileDescriptor != null) {
            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        } else {
            return null;
        }

        final BitmapFactory.Options options = new BitmapFactory.Options();

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        options.inSampleSize = calculateInSampleSize(options, requiredWidth, requiredHeight);
        options.inJustDecodeBounds = false;

        Bitmap decodeSampledBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            close(parcelFileDescriptor);
        }

        int exifOrientation = getExifOrientation(context, uri);
        int exifDegrees = exifToDegrees(exifOrientation);
        int exifTranslation = exifToTranslation(exifOrientation);
        Matrix aMatrix = new Matrix();

        if (exifDegrees != 0) {
           aMatrix.preRotate(exifDegrees);
        }
        if (exifTranslation != 1) {
            aMatrix.postScale(exifTranslation,1);
        }
        RectF deviceR = new RectF();
        RectF dstR = new RectF(0, 0, requiredWidth, requiredHeight);
        aMatrix.mapRect(deviceR, dstR);
        aMatrix.postTranslate(-deviceR.left, -deviceR.top);
        currentMatrix.set(aMatrix);
        Log.d(TAG, "currentMatrx = " + currentMatrix.toString());
        // TODO Should not transform bitmap but initially apply needed angle and translation to the matrix
        return transformBitmap(decodeSampledBitmap, aMatrix);
    }

    public static Bitmap transformBitmap(@Nullable Bitmap bitmap, Matrix transformMatrix) {
            Bitmap converted = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), transformMatrix, true);
            if (bitmap != converted) {
                bitmap.recycle();
                bitmap = converted;
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

    private static int getExifOrientation(@NonNull Context context,
                                          @NonNull Uri imageUri) throws Exception {
        InputStream stream = context.getContentResolver().openInputStream(imageUri);
        if (stream == null) {
            return ExifInterface.ORIENTATION_UNDEFINED;
        }
        int orientation = new ImageHeaderParser(stream).getOrientation();
        stream.close();
        return orientation;
    }

    private static int exifToDegrees(int exifOrientation) {
        int rotation=0;
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
