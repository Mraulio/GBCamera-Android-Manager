package com.mraulio.gbcameramanager.model;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class GbcFrame {
    @PrimaryKey
    @NonNull
    String frameName;

    @ColumnInfo(name = "frame_bitmap")
    Bitmap frameBitmap;

    @ColumnInfo(name = "wild_frame", defaultValue = "false")
    boolean isWildFrame;

    public GbcFrame() {
        this.isWildFrame = false;
    }

    public Bitmap getFrameBitmap() {
        return frameBitmap;
    }

    public void setFrameBitmap(Bitmap frameBitmap) {
        this.frameBitmap = frameBitmap;
    }

    public String getFrameName() {
        return frameName;
    }

    public boolean isWildFrame() {
        return isWildFrame;
    }

    public void setWildFrame(boolean wildFrame) {
        isWildFrame = wildFrame;
    }

    public void setFrameName(String frameName) {
        this.frameName = frameName;
    }
}
