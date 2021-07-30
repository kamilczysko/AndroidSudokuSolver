package com.kamil.opcvtest;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.kamil.opcvtest.utils.ImageProcessUtil;
import com.kamil.opcvtest.utils.TextRecognizationUtil;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ImageProcessor {

    private ExecutorService executorService;

    private List<List<Rect>> sortedRects = Collections.emptyList();
    private List<List<RectWrapper>> sortedRectWrappers = Collections.emptyList();

    private final TextRecognizationUtil textRecognizationService;
    private final ImageProcessUtil imageProcessUtil;

    private final Mat sudoku;
    private final Mat sudokuThreshed;

    @RequiresApi(api = Build.VERSION_CODES.N)
    ImageProcessor(Mat sudoku, Context context) {
        this.textRecognizationService = new TextRecognizationUtil(context);
        this.imageProcessUtil = new ImageProcessUtil();
        this.executorService = Executors.newSingleThreadExecutor();

        this.sudoku = sudoku;
        this.sudokuThreshed = imageProcessUtil.getThreshedImageFromCamera(sudoku);

        prepareData();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void prepareData(){
        List<Rect> sudokuMapCells = imageProcessUtil.findCellsContoursFromSudokuPlane(sudoku);
        sortedRects = imageProcessUtil.sortRects(sudokuMapCells);
        sortedRectWrappers = getCellsWrappers(sudokuThreshed, sortedRects);
    }

    private List<List<RectWrapper>> getCellsWrappers(Mat resizedSudokuPlane, List<List<Rect>> rois) {
        List<List<RectWrapper>> list = new ArrayList<>();
        for (List<Rect> row : rois) {
            List<RectWrapper> wrapperRow = new ArrayList<>();
            for (Rect r : row) {
                if(imageProcessUtil.isEmptyWithoutThreshold(resizedSudokuPlane, r)){
                    wrapperRow.add(new RectWrapper(r, false));
                }else {
                    wrapperRow.add(new RectWrapper(r, true));
                }
            }
            list.add(wrapperRow);
        }
        return list;
    }

    public List<List<Rect>> getSortedSudokuCells() {
        return sortedRects;
    }

    public List<List<RectWrapper>> getSortedSudokuCellsWrappers() {
        return sortedRectWrappers;
    }

    public List<List<Integer>> getDigitalizedMap(){
        return getDigitalMap(sortedRectWrappers);
    }

    private List<List<Integer>> getDigitalMap(List<List<RectWrapper>> mapOfRects) {
        List<List<Future<Integer>>> digitalMap = new ArrayList<>();
        for (List<RectWrapper> row : mapOfRects) {
            ArrayList<Future<Integer>> singleRow = new ArrayList<>();
            for (RectWrapper rect : row) {
                Future<Integer> submit =
                        executorService.submit(() -> getElementToRow(sudoku, rect));
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

    private Integer getElementToRow(Mat original, RectWrapper cellRegion) {
        if (!cellRegion.isNumber())
            return 0;
        Mat resizedMat = imageProcessUtil.getCroppedMap(original.submat(cellRegion.getRect()));
        return textRecognizationService.getNumberFromRegion(resizedMat.clone());
    }

    public Mat getSudokuThreshedMap() {
        return sudokuThreshed;
    }

    public Mat getSudoku() {
        return sudoku;
    }
}
