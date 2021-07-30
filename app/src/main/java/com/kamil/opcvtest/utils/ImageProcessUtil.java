package com.kamil.opcvtest.utils;

import android.os.Build;

import androidx.annotation.RequiresApi;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ImageProcessUtil {

    public static final double PERCENTAGE_OF_WHITE = 0.89;
    public static final double SUBMAT_RESIZE_FACTOR = 0.07;


    @RequiresApi(api = Build.VERSION_CODES.N)
    public List<Rect> findCellsContoursFromSudokuPlane(Mat sudokuPlane) {
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

    public List<Rect> findSudokuPlanesContours(Mat rawImage) {
        Mat cameraViewFrameToFirstProcess = getThreshedImageFromCamera(rawImage);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(cameraViewFrameToFirstProcess, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        List<Rect> foundRects = new ArrayList();
        for (MatOfPoint a : contours) {
            if (Imgproc.contourArea(a) > 200) {
                Rect rect = Imgproc.boundingRect(a);
                int width = rect.width;
                int height = rect.height;
                if (height > 200 && width > 200 && height < 500 && width < 500) {
                    foundRects.add(rect);
                }
            }
        }
        return foundRects;
    }

    public Mat getThreshedImageFromCamera(Mat originalRawInputImage) {
        Mat cameraViewFrameToFirstProcess = originalRawInputImage.clone();
        Imgproc.cvtColor(cameraViewFrameToFirstProcess, cameraViewFrameToFirstProcess, Imgproc.COLOR_RGB2GRAY);
        Imgproc.threshold(cameraViewFrameToFirstProcess, cameraViewFrameToFirstProcess, 120, 255, Imgproc.THRESH_BINARY);
        return cameraViewFrameToFirstProcess;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public List<List<Rect>> sortRects(List<Rect> cells) {
        List<Rect> sortedByY = getSortedRectsByY(cells);

        int size = (int) Math.sqrt(cells.size());

        List<List<Rect>> digitMap = new ArrayList<>();
        for (int a = 0; a < size; a++) {
            int index = a * size;
            digitMap.add(getSortedRectsByX(sortedByY.subList(index, index + size)));
        }
        return digitMap;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private List<Rect> getSortedRectsByX(List<Rect> rects) {
        return rects.stream()
                .sorted(Comparator.comparingInt(Rect::getX))
                .collect(Collectors.toList());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private List<Rect> getSortedRectsByY(List<Rect> cells) {
        return cells.stream()
                .sorted(Comparator.comparingInt(Rect::getY))
                .collect(Collectors.toList());
    }

    public boolean isEmpty(Mat selectedSudokuPlaneToProcess, Rect cell) {
        Mat croppedMat = getCroppedMap(selectedSudokuPlaneToProcess.submat(cell), 0.005);
        Mat mat = getThreshedImageFromCamera(croppedMat);
        int nonZero = Core.countNonZero(mat);
        long totalSize = mat.total();
        double coef = (double) nonZero / (double) totalSize;
        return coef > PERCENTAGE_OF_WHITE;
    }

    public boolean isEmptyWithoutThreshold(Mat threshedSudokuPlane, Rect cellRoi) {
        Mat mat = getCroppedMap(threshedSudokuPlane.submat(cellRoi), 0.005);
        int nonZero = Core.countNonZero(mat);
        long totalSize = mat.total();
        double coef = (double) nonZero / (double) totalSize;
        return coef > PERCENTAGE_OF_WHITE;
    }

    Mat getCroppedMap(Mat submat, double resizeFactor) {
        int rowOffset = (int) (submat.rows() * resizeFactor);
        int colOffset = (int) (submat.cols() * resizeFactor);
        return submat.submat(rowOffset, submat.rows() - rowOffset * 2, colOffset, submat.cols() - colOffset * 2);
    }

    public Mat getCroppedMap(Mat submat) {
        int rowOffset = (int) (submat.rows() * SUBMAT_RESIZE_FACTOR);
        int colOffset = (int) (submat.cols() * SUBMAT_RESIZE_FACTOR);
        return submat.submat(rowOffset, submat.rows() - rowOffset * 2, colOffset, submat.cols() - colOffset * 2);
    }

    public Mat getSelectedSudokuPlane(Mat frameToSubstract, Rect mapRect) {
        Mat originalSubmat = frameToSubstract.submat(mapRect).clone();
        Imgproc.resize(originalSubmat, originalSubmat, frameToSubstract.size(), 0, 0);
        return originalSubmat;
    }
}
