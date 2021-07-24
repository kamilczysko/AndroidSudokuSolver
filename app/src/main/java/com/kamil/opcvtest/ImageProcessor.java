package com.kamil.opcvtest;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.googlecode.tesseract.android.TessBaseAPI.PageSegMode.PSM_AUTO;

public class ImageProcessor {

    private final TessBaseAPI tess;
    private Context context;

    public ImageProcessor(Context context) {
        this.context = context;

        File tessData = context.getExternalFilesDir("/tessdata");
        if (!tessData.exists()) {
            tessData.mkdir();
        }

        File file = new File(tessData + "/end.traineddata");
        if (!file.exists()) {
            Log.d("ABCD", "not exists");

            try {
                InputStream in = context.getAssets().open("tessdata/eng.traineddata");
                FileOutputStream fileOutputStream = new FileOutputStream(tessData + "/eng.traineddata");
                byte[] buff = new byte[1024];
                int len;
                while ((len = in.read(buff)) > 0) {
                    fileOutputStream.write(buff, 0, len);
                }
                in.close();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        tess = new TessBaseAPI();
        String path = context.getExternalFilesDir("/").getPath() + "/";
        tess.setDebug(true);

        tess.init(path, "eng");
        String whitelist = "0123456789";
        tess.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, whitelist);
        tess.setPageSegMode(PSM_AUTO);

    }
}
