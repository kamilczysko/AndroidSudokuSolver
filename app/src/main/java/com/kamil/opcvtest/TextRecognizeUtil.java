package com.kamil.opcvtest;

import android.content.Context;
import android.graphics.Bitmap;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TextRecognizeUtil {

    private TessBaseAPI tess;
    private Context context;

    public TextRecognizeUtil(Context context) {
        this.context = context;

        File tessData = context.getExternalFilesDir("/tessdata");
        if (!tessData.exists()) {
            tessData.mkdir();
        }

        File file = new File(tessData + "/end.traineddata");
        if (!file.exists()) {
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
    }

    public int getNumberFromRegion(Mat mat) {
        tess = new TessBaseAPI();
        String path = context.getExternalFilesDir("/").getPath() + "/";
        tess.setDebug(false);

        tess.init(path, "eng");
        String whitelist = "0123456789";
        tess.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, whitelist);
        Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmp);
        return getNumber(bmp);
    }

    private int getNumber(Bitmap bmp) {
        tess.setImage(bmp);
        String utf8Text = tess.getUTF8Text();
        tess.end();
        if (utf8Text.trim().equals("")) return 8; //not recognize eights :/
        return Integer.parseInt(utf8Text.trim().substring(utf8Text.length() - 1));
    }
}
