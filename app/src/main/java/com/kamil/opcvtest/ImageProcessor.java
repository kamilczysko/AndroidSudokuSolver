package com.kamil.opcvtest;

import android.content.Context;
import android.graphics.Bitmap;

import com.googlecode.tesseract.android.TessBaseAPI;

public class ImageProcessor {

    private final TessBaseAPI tess;
    private Context context;

    ImageProcessor(Context context){
        this.context = context;
        this.tess = new TessBaseAPI();
        String path = context.getExternalFilesDir("/").getPath() + "/";

        tess.init(path, "eng");
        tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_CHAR);
        tess.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789");
    }

    public String readCell(Bitmap bitmap) {
        tess.setImage(bitmap);
        return tess.getUTF8Text();
    }
}
