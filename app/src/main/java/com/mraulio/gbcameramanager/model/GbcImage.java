package com.mraulio.gbcameramanager.model;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

public class GbcImage {
//    private Bitmap bitmap;
    private int paletteIndex;
    private int frameIndex;
    private String name;
    private List<String> tags = new ArrayList<>();
    public static int numImages= 0;
    private byte[] imageBytes;

    public GbcImage(){         //Add 1 image to the total
        paletteIndex = 0;
        frameIndex = 0;//I set the first palette as the default
    }

//    public GbcImage(Bitmap bitmap, int paletteIndex, String name) {
//        this.bitmap = bitmap;
//        this.paletteIndex = paletteIndex;
//        this.name = name;
//    }

//    public Bitmap getBitmap() {
//        return bitmap;
//    }
//
//    public void setBitmap(Bitmap bitmap) {
//        this.bitmap = bitmap;
//    }


    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void addTag(String newTag){
        this.tags.add(newTag);
    }

    public int getPaletteIndex() {
        return paletteIndex;
    }

    public void setPaletteIndex(int paletteIndex) {
        this.paletteIndex = paletteIndex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public void setFrameIndex(int frameIndex) {
        this.frameIndex = frameIndex;
    }

    public static int getNumImages() {
        return numImages;
    }

    public static void setNumImages(int numImages) {
        GbcImage.numImages = numImages;
    }
}
