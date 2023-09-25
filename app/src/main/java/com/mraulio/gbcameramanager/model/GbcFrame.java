package com.mraulio.gbcameramanager.model;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.HashMap;
import java.util.HashSet;

@Entity
public class GbcFrame {
    @PrimaryKey
    @NonNull
    String frameName;

    @ColumnInfo(name = "frame_bitmap")
    Bitmap frameBitmap;

    @ColumnInfo(name = "wild_frame", defaultValue = "false")
    boolean isWildFrame;

    @Ignore
    HashSet<int[]> transparentPixelPositions = new HashSet<>();

    @Ignore
    Bitmap transparencyMask;

    @Ignore
    byte[] frameBytes;

    public Bitmap getTransparencyMask() {
        return transparencyMask;
    }

    public void setTransparencyMask(Bitmap transparencyMask) {
        this.transparencyMask = transparencyMask;
    }

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

    public HashSet<int[]> getTransparentPixelPositions() {
        return transparentPixelPositions;
    }

    public void setTransparentPixelPositions(HashSet<int[]> transparentPixelPositions) {
        this.transparentPixelPositions = transparentPixelPositions;
    }

    public byte[] getFrameBytes() {
        return frameBytes;
    }

    public void setFrameBytes(byte[] frameBytes) {
        this.frameBytes = frameBytes;
    }
}
