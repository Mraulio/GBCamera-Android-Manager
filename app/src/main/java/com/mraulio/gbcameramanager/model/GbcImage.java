package com.mraulio.gbcameramanager.model;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;
@Entity
public class GbcImage {

    @PrimaryKey
    @NonNull
    private String hashCode;

    @ColumnInfo(name = "image_name")
    private String name;

    @ColumnInfo(name = "palette_index")
    private int paletteIndex;

    @ColumnInfo(name = "frame_index")
    private int frameIndex;

    @ColumnInfo(name = "tags_list")
    private List<String> tags = new ArrayList<>();

    public static int numImages= 0;//Should probably store this on the sharedPreferences

    @ColumnInfo(name = "image_bytes")
    private byte[] imageBytes;

    public GbcImage(){
        paletteIndex = 0;
        frameIndex = 0;//I set the first palette as the default
    }

    @NonNull
    public String getHashCode() {
        return hashCode;
    }

    public void setHashCode(@NonNull String hashCode) {
        this.hashCode = hashCode;
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
