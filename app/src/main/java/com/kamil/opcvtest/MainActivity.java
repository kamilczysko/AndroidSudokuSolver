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

import androidx.annotation.RequiresApi;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {

    private static final String TAG = "MainActivity";

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    private CameraBridgeViewBase camera;
    private List<Rect> foundRects = new ArrayList();
    private Rect selectedSudokuPlane = null;
    private boolean isSudokuPlaneSelected = false;
    private List<Rect> allCells;
    private Mat previousRawFrame;

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
        Mat tmpRawFrame = originalRawInputImage.clone();
        Mat cameraViewFrameToFirstProcess = originalRawInputImage.clone();

        Imgproc.cvtColor(cameraViewFrameToFirstProcess, cameraViewFrameToFirstProcess, Imgproc.COLOR_RGB2GRAY);
        Imgproc.threshold(cameraViewFrameToFirstProcess, cameraViewFrameToFirstProcess, 120, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C);

        if (isSudokuPlaneSelected) {
            return findSingleCells(getSelectedSudokuPlane(previousRawFrame));
        }

        findSudokuPlanesContours(cameraViewFrameToFirstProcess, tmpRawFrame)
                .forEach(sudokuPlaneBoudary ->
                        getColorMaskForSingleSudokuPlane(sudokuPlaneBoudary, tmpRawFrame));

        previousRawFrame  = originalRawInputImage.clone();
        return tmpRawFrame;
    }

    private Mat getSelectedSudokuPlane(Mat frameToSubstract) {
        Mat originalSubmat = frameToSubstract.submat(selectedSudokuPlane).clone();
        Imgproc.resize(originalSubmat, originalSubmat, getFrameSize(), 0, 0);
        return originalSubmat;
    }

    private Size getFrameSize() {
        return previousRawFrame.size();
    }

    private void getColorMaskForSingleSudokuPlane(Rect sudokuPlaneBoudary, Mat frame) {
        Mat dummyMat = Mat.ones(frame.rows(), frame.cols(), frame.type());
        Mat submat = dummyMat.submat(sudokuPlaneBoudary).clone();
        submat.setTo(new Scalar(255, 0, 0));
        submat.copyTo(frame.submat(sudokuPlaneBoudary), submat.clone());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Mat findSingleCells(Mat sudokuPlane) {
        allCells = findCellsContoursFromSudokuPlane(sudokuPlane);
        Mat resultMat = sudokuPlane.clone();
        int c = 0;
        for (Rect rect : allCells) {
            Imgproc.rectangle(resultMat, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255), 15);
//                    Imgproc.circle(resultMat, new Point(rect.x + rect.width / 2, rect.y + rect.height / 2), 3, new Scalar(0, 255, 255), 15);
            Imgproc.putText(resultMat, (c) + "", new Point(rect.x + rect.width / 2, rect.y + rect.height / 2), 1, 2, new Scalar(255, 0, 0), 15);
            c++;
        }
        return resultMat;
    }

    private List<Rect> findCellsContoursFromSudokuPlane(Mat sudokuPlane) {
        List<Rect> rects = new ArrayList<>();
        Mat matToFindContours = new Mat();
        Imgproc.cvtColor(sudokuPlane.clone(), matToFindContours, Imgproc.COLOR_RGB2GRAY);
        Imgproc.blur(matToFindContours, matToFindContours, new Size(5, 5), new Point(), 0);
        Imgproc.adaptiveThreshold(matToFindContours, matToFindContours, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 5, 2);
        Core.bitwise_not(matToFindContours, matToFindContours);
        Imgproc.dilate(matToFindContours, matToFindContours, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)), new Point(), 11);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(matToFindContours, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        for (MatOfPoint contour : contours) {
            if (Imgproc.contourArea(contour) > 2600 && Imgproc.contourArea(contour) < 40000) {
                Rect rect = Imgproc.boundingRect(contour);
                if (Math.abs(rect.height - rect.width) < 100) {
                    rects.add(rect);
                }
            }
        }
        return rects;
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int cols = previousRawFrame.cols();
        int rows = previousRawFrame.rows();

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