package com.yalantis.ucrop.util;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by larin92 on 31.03.2016.
 * this class manages file interactions, contains file info,
 * can write bitmap to a file
 */

public class FileUtils {

    public String source = null;
    public File imageFile;
    public Bitmap bitmap; //  save pre-inSampleSize'd bitmap IN HERE
    public Boolean keepFile = false; // in case you want to keep the original image file
    private static final String TAG = "FileManager";

    public FileUtils(String Url) {
        source = Url;
        createFile(source);
    }

    public FileUtils() {
    }

    public void createFile(String mSource) {
        String downloadsDirectoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        String filename = String.format("ORIGIN_%s", Uri.parse(mSource).getLastPathSegment());
        imageFile = new File(downloadsDirectoryPath, filename);
    }

    //  This func writes bitmap to imageFile
    public void bitmapToFile(Bitmap bitmap) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(imageFile);
            if ( bitmap != null ){
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();}
            else {
                Log.v(TAG, "bitmap is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.v(TAG, "problems with FileOutputStream, error: ", e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.v(TAG, "IOException: ", e);
            }
        }
    }

    public Boolean deleteFile() {
        if (!keepFile)
            return imageFile.delete();
        else return false;
    }
}