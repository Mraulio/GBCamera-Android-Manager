package com.mraulio.gbcameramanager.model;

import android.graphics.Bitmap;

public class GbcFrame {
    Bitmap frameBitmap;
    String frameName;

    public GbcFrame(){}

    public Bitmap getFrameBitmap() {
        return frameBitmap;
    }

    public void setFrameBitmap(Bitmap frameBitmap) {
        this.frameBitmap = frameBitmap;
    }

    public String getFrameName() {
        return frameName;
    }

    public void setFrameName(String frameName) {
        this.frameName = frameName;
    }
}
