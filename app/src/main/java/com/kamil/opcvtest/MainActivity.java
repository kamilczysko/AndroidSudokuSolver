package com.kamil.opcvtest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.RequiresApi;

import com.kamil.opcvtest.utils.ImageProcessUtil;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {

    private static final String TAG = "MainActivity";

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    private CameraBridgeViewBase camera;
    private List<Rect> foundRects = new ArrayList();
    private Rect selectedSudokuPlaneRect = null;
    private boolean isSudokuPlaneSelected = false;
    private Mat previousRawFrame;

    private ImageProcessUtil imageProcessUtil;

    static int TOP_HEIGHT = 0;

    private ImageProcessor imageProcessor;

    BaseLoaderCallback loader = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV camera loaded");
                    camera.enableView();
                    camera.setOnTouchListener(MainActivity.this);
                    camera.setCvCameraViewListener(MainActivity.this);
                }
                break;
                default: {
                    Log.i(TAG, "OpenCV camera not loaded");
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    boolean debugView = false;
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        initOpenCV();

        Button debugViewButton = findViewById(R.id.debugView);
        debugViewButton.setOnClickListener(v -> debugView = !debugView);

        Button processButton = findViewById(R.id.processButton);
        processButton.setOnClickListener(v -> processCapturedMap());

        TOP_HEIGHT = processButton.getHeight();

        this.imageProcessUtil = new ImageProcessUtil();
    }

    private void processCapturedMap() {
        Intent sudokuIntent = new Intent(this, SudokuActivity.class);
        Parcelable wrap = Parcels.wrap(imageProcessor.getDigitalizedMap());
        sudokuIntent.putExtra("sudoku", wrap);
        startActivity(sudokuIntent);
    }

    private void initOpenCV() {
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, loader);

        camera = findViewById(R.id.sudoku_camera_view);
        camera.setCameraPermissionGranted();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (camera != null) {
            camera.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, loader);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @SuppressLint("NewApi")
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat originalRawInputImage = inputFrame.rgba();
        if (isSudokuPlaneSelected) {
            Mat resizedSudokuPlane = imageProcessUtil.getSelectedSudokuPlane(previousRawFrame, selectedSudokuPlaneRect);
            if (imageProcessor == null) {
                imageProcessor = new ImageProcessor(resizedSudokuPlane, this);
            }
            return drawHighlightedCells(debugView);
        }

        List<Rect> foundSudokuMaps = imageProcessUtil.findSudokuPlanesContours(originalRawInputImage.clone());
        Mat matWithRanges = drawRange(originalRawInputImage);
        Mat matWithSelectedSudokuMaps = drawMaskOnFoundSudokuMaps(matWithRanges, foundSudokuMaps);

        foundRects = new ArrayList<>(foundSudokuMaps);
        previousRawFrame = originalRawInputImage.clone();

        return matWithSelectedSudokuMaps;
    }

    private Mat drawRange(Mat mat){
        Mat res = mat.clone();
        int x = (int)(camera.getWidth() * 0.07);
        int y = (int)(camera.getHeight() * 0.04);

        int x1 = (int)(camera.getWidth() * 0.41);
        int y1 = (int)(camera.getHeight() * 0.04);

        int x2 = (int)(camera.getWidth() * 0.07);
        int y2 = (int)(camera.getHeight() * 0.81);

        int x3 = (int)(camera.getWidth() * 0.41);
        int y3 = (int)(camera.getHeight() * 0.81);

        Imgproc.line(res, new Point(x, y), new Point(x + 100, y), new Scalar(0, 255, 255), 3);
        Imgproc.line(res, new Point(x, y), new Point(x, y + 100), new Scalar(0, 255, 255), 3);

        Imgproc.line(res, new Point(x1, y1), new Point(x1 - 100, y1), new Scalar(0, 255, 255), 3);
        Imgproc.line(res, new Point(x1, y1), new Point(x1, y1 + 100), new Scalar(0, 255, 255), 3);

        Imgproc.line(res, new Point(x2, y2), new Point(x2 + 100, y2), new Scalar(0, 255, 255), 3);
        Imgproc.line(res, new Point(x2, y2), new Point(x2, y2 - 100), new Scalar(0, 255, 255), 3);

        Imgproc.line(res, new Point(x3, y3), new Point(x3 - 100, y3), new Scalar(0, 255, 255), 3);
        Imgproc.line(res, new Point(x3, y3), new Point(x3, y3 - 100), new Scalar(0, 255, 255), 3);

        return res;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Mat drawMaskOnFoundSudokuMaps(Mat rawFrame, List<Rect> foundSudokuMaps) {
        Mat resultMat = rawFrame.clone();
        foundSudokuMaps.forEach(sudokuPlaneBoudary ->
                drawColorMaskForSingleSudokuMap(sudokuPlaneBoudary, resultMat));
        return resultMat;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Mat drawHighlightedCells(boolean debugView) {
        if (debugView) {
            return drawSingleCellsDebug(imageProcessor.getSortedSudokuCellsWrappers(), imageProcessor.getSudokuThreshedMap().clone());
        }
        return drawHighlightedCells(imageProcessor.getSudoku(), imageProcessor.getSortedSudokuCells());
    }

    private void drawColorMaskForSingleSudokuMap(Rect sudokuPlaneBoudary, Mat rawImage) {
        Mat dummyMat = Mat.ones(rawImage.rows(), rawImage.cols(), rawImage.type());
        Mat submat = dummyMat.submat(sudokuPlaneBoudary).clone();
        submat.setTo(new Scalar(255, 0, 0));
        submat.copyTo(rawImage.submat(sudokuPlaneBoudary), submat.clone());
        int x = sudokuPlaneBoudary.x;
        int y = sudokuPlaneBoudary.y;
        int width = sudokuPlaneBoudary.width;
        int height = sudokuPlaneBoudary.height;
        Imgproc.rectangle(rawImage, new Point(x, y), new Point(x + width, y + height), new Scalar(0, 0, 255), 3);
        Imgproc.circle(rawImage, new Point(x + width / 2, y + height / 2), 3, new Scalar(0, 255, 255), 3);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Mat drawHighlightedCells(Mat sudokuPlane, List<List<Rect>> cells) {
        Mat resultMat = sudokuPlane.clone();
        int c = 0;
        for(List<Rect> row : cells){
            for(Rect rect : row){
                Imgproc.rectangle(resultMat, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255), 15);
                Imgproc.putText(resultMat, (c++) + "", new Point(rect.x + rect.width / 11.0, rect.y + rect.height / 3.0), 1, 1, new Scalar(255, 0, 255), 15);
            }
        }
        return resultMat;
    }

    private Mat drawSingleCellsDebug(List<List<RectWrapper>> cells, Mat threshedImageFromCamera) {
        for (List<RectWrapper> row : cells) {
            for (RectWrapper rect : row) {
                drawSingleCellSelected(threshedImageFromCamera, rect);
            }
        }
        return threshedImageFromCamera;
    }

    void drawSingleCellSelected(Mat threshedCanvas, RectWrapper rectWrapper) {
        Rect rect = rectWrapper.getRect();
        double percentage = getPercentage(threshedCanvas, rect);
        Imgproc.rectangle(threshedCanvas, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255), 15);
        if(rectWrapper.isNumber()) {
            Imgproc.putText(threshedCanvas, (String.format("%.2f", percentage)) , new Point(rect.x + rect.width / 11.0, rect.y + rect.height / 4.0), 1, 1, new Scalar(0, 0, 0), 5);
        }
    }

    private double getPercentage(Mat threshedCanvas, Rect rect) {
        Mat submat = threshedCanvas.submat(rect);
        int nonZero = Core.countNonZero(submat);
        long totalSize = submat.total();
        return (double) nonZero / (double) totalSize;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int xOffset = getCameraXOffset();
        int yOffset = getCameraYOffset();

        int x = (int) event.getX() - xOffset;
        int y = (int) event.getY() - yOffset + TOP_HEIGHT;

        if (isSudokuPlaneSelected && !debugView) {
            isSudokuPlaneSelected = false;
            imageProcessor = null;
            return false;
        }

        if (!isSudokuPlaneSelected) {
            if(foundRects.size() == 1){
                selectedSudokuPlaneRect = foundRects.get(0);
                isSudokuPlaneSelected = true;
            }
            return false;
        }

        if(debugView){
            for(List<RectWrapper> row : imageProcessor.getSortedSudokuCellsWrappers()){
                   for(RectWrapper rect : row){
                       if(rect.isTouched(x, y)){
                           Log.d(TAG, "before: "+rect.isNumber() );
                           rect.toggleIsNumber();
                           Log.d(TAG, "after: "+rect.isNumber() );
                           Log.d(TAG, "-------- ");
                       }
                   }
            }
        }
        return false;
    }

    private int getCameraYOffset() {
        int rows = previousRawFrame.rows();
        return (camera.getHeight() - rows) / 2;
    }

    private int getCameraXOffset() {
        int cols = previousRawFrame.cols();
        return (camera.getWidth() - cols) / 2;
    }
}