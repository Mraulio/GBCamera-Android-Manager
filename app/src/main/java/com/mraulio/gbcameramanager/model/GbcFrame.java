package com.mraulio.gbcameramanager.model;

import static com.mraulio.gbcameramanager.utils.StaticValues.DEFAULT_FRAME_MARGIN;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.HashSet;
import java.util.LinkedHashMap;

@Entity
public class GbcFrame {

    @PrimaryKey
    @ColumnInfo(name = "frame_id")
    @NonNull
    String frameId;

    @ColumnInfo(name = "frame_name")
    String frameName;

    @ColumnInfo(name = "frame_hash")
    String frameHash;

    @ColumnInfo(name = "frame_bitmap")
    Bitmap frameBitmap;

    @ColumnInfo(name = "wild_frame", defaultValue = "false")
    boolean isWildFrame;

    @ColumnInfo(name = "frame_groups_names")
    LinkedHashMap<String, String> frameGroupsNames;

    @Nullable @ColumnInfo(name = "image_margin")
    public Integer imageMargin;

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
        this.imageMargin = DEFAULT_FRAME_MARGIN; //16 for the default frame (Wild Frames has 40 margin)
    }

    public String getFrameId() {
        return frameId;
    }

    public void setFrameId(String frameId) {
        this.frameId = frameId;
    }

    public String getFrameHash() {
        return frameHash;
    }

    public void setFrameHash(String frameHash) {
        this.frameHash = frameHash;
    }

    public Bitmap getFrameBitmap() {
        return frameBitmap;
    }

    public void setFrameBitmap(Bitmap frameBitmap) {
        this.frameBitmap = frameBitmap;
    }

    @Nullable
    public Integer getImageMargin() {
        return imageMargin;
    }

    public void setImageMargin(@Nullable Integer imageMargin) {
        this.imageMargin = imageMargin;
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

    public LinkedHashMap<String, String> getFrameGroupsNames() {
        return frameGroupsNames;
    }

    public void setFrameGroupsNames(LinkedHashMap<String, String> frameGroupsNames) {
        this.frameGroupsNames = frameGroupsNames;
    }
}
