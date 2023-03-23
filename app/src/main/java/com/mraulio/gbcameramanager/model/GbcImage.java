package com.mraulio.gbcameramanager.model;

import android.graphics.Bitmap;

public class GbcImage {
//    private Bitmap bitmap;
    private int paletteIndex;
    private String name;
    boolean favorite;
    public static int numImages= 0;
    private byte[] imageBytes;

    public GbcImage(){
        numImages++; //Add 1 image to the total
        paletteIndex = 0;//I set the first palette as the default
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

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }
}
