package com.mraulio.gbcameramanager.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class GbcImage {

    @PrimaryKey
    @NonNull
    private String hashCode;

    @ColumnInfo(name = "image_name")
    private String name;

    @ColumnInfo(name = "palette_id")
    private String paletteId;

    @ColumnInfo(name = "lock_frame")
    private boolean lockFrame;

    @ColumnInfo(name = "invert_palette")
    private boolean invertPalette;

    @ColumnInfo(name = "frame_id")
    private String frameId;

    @ColumnInfo(name = "creation_date")
    private Date creationDate;

    @ColumnInfo(name = "tags_list")
    private List<String> tags = new ArrayList<>();

    public static int numImages = 0;

    private byte[] imageBytes;

    public GbcImage() {
        this.paletteId = "bw";
        this.frameId = "Nintendo_Frame";//I set the nintendo frame as the default
        this.lockFrame = false;
        this.invertPalette = false;
        this.creationDate = new Date(System.currentTimeMillis());;
    }

    @NonNull
    public String getHashCode() {
        return hashCode;
    }

    public void setHashCode(@NonNull String hashCode) {
        this.hashCode = hashCode;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public boolean isInvertPalette() {
        return invertPalette;
    }

    public void setInvertPalette(boolean invertPalette) {
        this.invertPalette = invertPalette;
    }

    public boolean isLockFrame() {
        return lockFrame;
    }

    public void setLockFrame(boolean lockFrame) {
        this.lockFrame = lockFrame;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void addTag(String newTag) {
        this.tags.add(newTag);
    }

    public String getPaletteId() {
        return paletteId;
    }

    public void setPaletteId(String paletteId) {
        this.paletteId = paletteId;
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

    public String getFrameId() {
        return frameId;
    }

    public void setFrameId(String frameId) {
        this.frameId = frameId;
    }

    public static int getNumImages() {
        return numImages;
    }

    public static void setNumImages(int numImages) {
        GbcImage.numImages = numImages;
    }
}
