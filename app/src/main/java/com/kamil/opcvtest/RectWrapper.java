package com.kamil.opcvtest;

import org.opencv.core.Rect;

public class RectWrapper {

    private Rect rect;
    private boolean isNumber = false;
    private Integer number;

    public RectWrapper(Rect rect, boolean isNumber){
        this.rect = rect;
        this.isNumber = isNumber;
    }

    private RectWrapper(){ }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Integer getNumber() {
        return number;
    }

    public Rect getRect(){
        return rect;
    }

    public void setIfNumber(boolean isNumber) {
        this.isNumber = isNumber;
    }

    public void toggleIsNumber(){
        this.isNumber = !this.isNumber;
    }

    public boolean isNumber() {
        return isNumber;
    }

    public boolean isTouched(int x, int y) {
        return (x >= rect.getX() && x <= rect.getX() + rect.width)
                && (y >= rect.getY() && y <= rect.getY() + rect.height);
    }

}
