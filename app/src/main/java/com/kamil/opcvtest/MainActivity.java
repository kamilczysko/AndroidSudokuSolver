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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {

    private static final String TAG = "MainActivity";

    private ExecutorService executorService;

    List<List<Rect>> sortedRects = Collections.emptyList();

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    private CameraBridgeViewBase camera;
    private List<Rect> foundRects = new ArrayList();
    private List<Rect> sudokuMapCells = new ArrayList();
    private Rect selectedSudokuPlaneRect = null;
    private boolean isSudokuPlaneSelected = false;
    private Mat previousRawFrame;

    private TextRecognizeUtil textRecognizationService;
    private ImageProcessUtil imageProcessUtil;

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

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, loader);

        camera = findViewById(R.id.sudoku_camera_view);
        camera.setCameraPermissionGranted();

        Button debugViewButton = findViewById(R.id.debugView);
        debugViewButton.setOnClickListener(v -> debugView = !debugView);

        Button processButton = findViewById(R.id.processButton);
        processButton.setOnClickListener(v -> {
            Intent sudokuIntent = new Intent(this, SudokuActivity.class);
            Parcelable wrap = Parcels.wrap(getDigitalMap(sortedRects));
            sudokuIntent.putExtra("sudoku", wrap);
            startActivity(sudokuIntent);
        });

        this.textRecognizationService = new TextRecognizeUtil(this);
        this.imageProcessUtil = new ImageProcessUtil();

        executorService = Executors.newSingleThreadExecutor();
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

    boolean doitagain = true;

    @SuppressLint("NewApi")
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat originalRawInputImage = inputFrame.rgba();
        if (isSudokuPlaneSelected) {
            Mat resizedSudokuPlane = imageProcessUtil.getSelectedSudokuPlane(previousRawFrame, selectedSudokuPlaneRect);
            sudokuMapCells = imageProcessUtil.findCellsContoursFromSudokuPlane(resizedSudokuPlane);
            sortedRects = imageProcessUtil.sortRects(sudokuMapCells);
            return drawHighlightedCells(previousRawFrame, sortedRects, debugView);
        }

        List<Rect> foundSudokuMaps = imageProcessUtil.findSudokuPlanesContours(originalRawInputImage.clone());
        Mat matWithSelectedSudokuMaps = drawMaskOnFoundSudokuMaps(originalRawInputImage, foundSudokuMaps);

        foundRects = new ArrayList<>(foundSudokuMaps);
        previousRawFrame = originalRawInputImage.clone();

        return matWithSelectedSudokuMaps;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Mat drawMaskOnFoundSudokuMaps(Mat rawFrame, List<Rect> foundSudokuMaps) {
        Mat resultMat = rawFrame.clone();
        foundSudokuMaps.forEach(sudokuPlaneBoudary ->
                drawColorMaskForSingleSudokuMap(sudokuPlaneBoudary, resultMat));
        return resultMat;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Mat drawHighlightedCells(Mat previousFrame, List<List<Rect>> cellsContoursFromSudokuPlane, boolean debugView) {
        Mat sudokuMat = imageProcessUtil.getSelectedSudokuPlane(previousFrame.clone(), selectedSudokuPlaneRect);
        if (debugView) {
            return drawSingleCells(cellsContoursFromSudokuPlane, imageProcessUtil.getThreshedImageFromCamera(sudokuMat));
        }
        return drawHighlightedCells(sudokuMat, cellsContoursFromSudokuPlane);
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

    private List<List<Integer>> getDigitalMap(List<List<Rect>> mapOfRects) {
        Mat original = imageProcessUtil.getSelectedSudokuPlane(previousRawFrame.clone(), selectedSudokuPlaneRect).clone();
        List<List<Future<Integer>>> digitalMap = new ArrayList<>();
        for (List<Rect> row : mapOfRects) {
            ArrayList<Future<Integer>> singleRow = new ArrayList<>();
            for (Rect rect : row) {
                Future<Integer> submit =
                        executorService.submit(() -> getElementToRow(original, rect));
                singleRow.add(submit);
            }

            digitalMap.add(singleRow);
        }
        List<List<Integer>> resultList = new ArrayList<>();
        for(List<Future<Integer>> row : digitalMap){
            List<Integer> r = new ArrayList<>();
            for (Future<Integer> elem : row){
                try {
                    Integer number = elem.get(20, TimeUnit.SECONDS);
                    r.add(number);
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    e.printStackTrace();
                }
            }
            resultList.add(r);
        }
        return resultList;
    }

    private Integer getElementToRow(Mat original, Rect cellRegion) {
        if(imageProcessUtil.isEmpty(original, cellRegion))
            return 0;
        else {
            Mat resizedMat = imageProcessUtil.getCroppedMap(original.submat(cellRegion));
            return textRecognizationService.getNumberFromRegion(resizedMat.clone());
        }
    }

    private Mat drawSingleCells(List<List<Rect>> cells, Mat threshedImageFromCamera) {
        for (List<Rect> row : cells) {
            for (Rect rect : row) {
                drawSingleCellSelected(threshedImageFromCamera, rect);
            }
        }
        return threshedImageFromCamera;
    }

    void drawSingleCellSelected(Mat threshedCanvas, Rect rect) {
        double percentage = getPercentage(threshedCanvas, rect);
        Imgproc.rectangle(threshedCanvas, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255), 15);
        if(percentage < imageProcessUtil.PERCENTAGE_OF_WHITE) {
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
        int cols = previousRawFrame.cols();
        int rows = previousRawFrame.rows();

        int xOffset = (camera.getWidth() - cols) / 2;
        int yOffset = (camera.getHeight() - rows) / 2;

        int x = (int) event.getX() - xOffset;
        int y = (int) event.getY() - yOffset;

        if(isSudokuPlaneSelected && !debugView){
            isSudokuPlaneSelected = false;
            return false;
        }
        if (!isSudokuPlaneSelected) {
            selectedSudokuPlaneRect = getSelectedSudoku(x, y);
            doitagain = true;
            isSudokuPlaneSelected = true;
            return false;
        }

        if(debugView){
            for(Rect cell : sudokuMapCells){
                if(isTouched(cell, x,y)){
                    //todo se selectable cells
                    return false;
                }
            }
        }

        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Rect getSelectedSudoku(int x, int y) {
        return foundRects.stream()
                .filter(rect -> isTouched(rect, x, y))
                .findFirst()
                .orElse(null);
    }

    private boolean isTouched(Rect rect, int x, int y) {
        return (x > rect.x && x < rect.x + rect.width) && (y > rect.y && y < rect.y + rect.height);
    }
}