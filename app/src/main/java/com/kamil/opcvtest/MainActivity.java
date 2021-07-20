package com.kamil.opcvtest;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {

    private static final String TAG = "MainActivity";

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    private CameraBridgeViewBase camera;
    private SeekBar threshSlide;
    private TextView threshValue;
    private int threshVal = 3000;
    List<Rect> foundRects = new ArrayList();
    Mat originalRawInputImage;
    Rect selectedSudokuPlane = null;
    boolean isSudokuPlaneSelected = false;
    Mat mat;
    Mat previousFrame;
    Mat previousFrameFromProcessedImage;

    Mat sudoku;

    boolean triger = false;

    Button processButton;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, loader);

        processButton = findViewById(R.id.processButton);
        processButton.setVisibility(View.INVISIBLE);

        processButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triger = !triger;
            }
        });

        camera = findViewById(R.id.sudoku_camera_view);
        camera.setCameraPermissionGranted();
//
        threshSlide = findViewById(R.id.threshSlide);
        threshValue = findViewById(R.id.threshValue);
        threshSlide.setMax(60000);

        threshValue.setText(threshVal + " ");
        threshSlide.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        threshVal = progress;
                        threshValue.setText(threshVal + "");
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                }
        );

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

    Mat processedImage;
    List<Rect> allCells;
    Mat previousRawFrame;

    @SuppressLint("NewApi")
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mat = new Mat();
        processedImage = new Mat();

        originalRawInputImage = inputFrame.rgba();
        Mat tmpRawFrame = originalRawInputImage.clone();
        processedImage = originalRawInputImage.clone();

        Imgproc.cvtColor(processedImage, processedImage, Imgproc.COLOR_RGB2GRAY);
        Imgproc.threshold(processedImage, processedImage, 120, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C);

        if (isSudokuPlaneSelected) {
            Mat processedSubmat = previousFrame.submat(selectedSudokuPlane).clone();
            Mat originalSubmat = previousRawFrame.submat(selectedSudokuPlane).clone();

            Imgproc.resize(processedSubmat, processedSubmat, processedImage.size(), 0, 0);
            Imgproc.resize(originalSubmat, originalSubmat, processedImage.size(), 0, 0);

            setProcessButtonVisible();
            return findSingleCells(processedSubmat, originalSubmat);
        }

        findSudokuPlanesContours(processedImage, tmpRawFrame)
                .forEach(sudokuPlaneBoudary ->
                        getColorMaskForSingleSudokuPlane(sudokuPlaneBoudary, tmpRawFrame));

        previousFrame = inputFrame.gray().clone();
        previousFrameFromProcessedImage = processedImage.clone();
        previousRawFrame  = originalRawInputImage.clone();

        setProcessButtonInvisible();

        return tmpRawFrame;
    }

    private void getColorMaskForSingleSudokuPlane(Rect sudokuPlaneBoudary, Mat frame) {
        Mat dummyMat = Mat.ones(frame.rows(), frame.cols(), frame.type());
        Mat submat = dummyMat.submat(sudokuPlaneBoudary).clone();
        submat.setTo(new Scalar(255, 0, 0));
        submat.copyTo(frame.submat(sudokuPlaneBoudary), submat.clone());
    }



    @RequiresApi(api = Build.VERSION_CODES.N)
    private Mat findSingleCells(Mat sudoku, Mat original) {
        Mat res = new Mat();
        allCells = new ArrayList<>();
        Imgproc.blur(sudoku, res, new Size(5, 5), new Point(), 0);
        Imgproc.adaptiveThreshold(res, res, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 5, 2);
        Core.bitwise_not(res, res);
        Imgproc.dilate(res, res, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)), new Point(), 11);
        if (triger)
            return res;
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(res, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        Mat m = new Mat(sudoku.rows(), sudoku.cols(), CvType.CV_16F);
        original.copyTo(m);
        int c = 0;
        for (MatOfPoint contour : contours) {
            if (Imgproc.contourArea(contour) > 2600 && Imgproc.contourArea(contour) < 40000) {
                Rect rect = Imgproc.boundingRect(contour);
                if (Math.abs(rect.height - rect.width) < 100) {
                    allCells.add(rect);
                }
            }
        }
        List<Rect> rects = allCells.stream()
//                .sorted(Comparator.comparingInt(Rect::getY).thenComparing(Rect::getX))
                .collect(Collectors.toList());
        for (Rect rect : rects) {
            Imgproc.rectangle(m, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255), 15);
//                    Imgproc.circle(m, new Point(rect.x + rect.width / 2, rect.y + rect.height / 2), 3, new Scalar(0, 255, 255), 15);
            Imgproc.putText(m, (c) + "", new Point(rect.x + rect.width / 2, rect.y + rect.height / 2), 1, 2, new Scalar(255, 0, 0), 15);
            c++;
        }
        return m;
    }

    private List<Rect> findSudokuPlanesContours(Mat input, Mat img) {
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(input, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        foundRects = new ArrayList();
        for (MatOfPoint a : contours) {
            if (Imgproc.contourArea(a) > 200) {
                Rect rect = Imgproc.boundingRect(a);
                int width = rect.width;
                int height = rect.height;
                if (height > 200 && width > 200 && height < 500 && width < 500) {
                    foundRects.add(rect);
                    int x = rect.x;
                    int y = rect.y;
                    Imgproc.rectangle(img, new Point(x, y), new Point(x + width, y + height), new Scalar(0, 0, 255), 3);
                    Imgproc.circle(img, new Point(x + width / 2, y + height / 2), 3, new Scalar(0, 255, 255), 3);
                }
            }
        }
        return foundRects;
    }

    private void setProcessButtonVisible() {
        this.runOnUiThread(() -> processButton.setVisibility(View.VISIBLE));
    }

    private void setProcessButtonInvisible() {
        this.runOnUiThread(() -> processButton.setVisibility(View.INVISIBLE));
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int cols = originalRawInputImage.cols();
        int rows = originalRawInputImage.rows();

        int xOffset = (camera.getWidth() - cols) / 2;
        int yOffset = (camera.getHeight() - rows) / 2;

        int x = (int) event.getX() - xOffset;
        int y = (int) event.getY() - yOffset;

        selectedSudokuPlane = getSelectedSudoku(x, y);

        isSudokuPlaneSelected = selectedSudokuPlane != null;

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