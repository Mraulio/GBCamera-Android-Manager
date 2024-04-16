package com.mraulio.gbcameramanager.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.mraulio.gbcameramanager.utils.StaticValues;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;

@Entity
public class GbcImage implements Cloneable{

    @PrimaryKey
    @NonNull
    private String hashCode;

    @ColumnInfo(name = "image_name")
    private String name;

    @ColumnInfo(name = "palette_id")
    private String paletteId;

    @ColumnInfo(name = "frame_palette_id")
    private String framePaletteId;

    @ColumnInfo(name = "lock_frame")
    private boolean lockFrame;

    @ColumnInfo(name = "invert_palette")
    private boolean invertPalette;

    @ColumnInfo(name = "image_metadata")
    private LinkedHashMap imageMetadata;

    @ColumnInfo(name = "invert_frame_palette", defaultValue = "false")
    private boolean invertFramePalette;

    @ColumnInfo(name = "frame_id")
    private String frameId;

    @ColumnInfo(name = "creation_date")
    private Date creationDate;

    @ColumnInfo(name = "tags_list")
    private HashSet<String> tags;

    public String getFramePaletteId() {
        return framePaletteId;
    }

    public boolean isInvertFramePalette() {
        return invertFramePalette;
    }

    public void setInvertFramePalette(boolean invertFramePalette) {
        this.invertFramePalette = invertFramePalette;
    }

    public void setFramePaletteId(String framePaletteId) {
        this.framePaletteId = framePaletteId;
    }

    @ColumnInfo(name = "rotation", defaultValue = "0")
    private int rotation;

    public static int numImages = 0;

    private byte[] imageBytes;

    public LinkedHashMap getImageMetadata() {
        return imageMetadata;
    }

    public void setImageMetadata(LinkedHashMap imageMetadata) {
        this.imageMetadata = imageMetadata;
    }

    public GbcImage() {
        this.paletteId = StaticValues.defaultPaletteId;
        this.framePaletteId = StaticValues.defaultPaletteId;
        this.frameId = null;
        this.lockFrame = false;
        this.invertPalette = false;
        this.invertFramePalette = false;
        this.creationDate = new Date(System.currentTimeMillis());
        this.rotation = 0;
        this.tags = new HashSet<>();
    }

    @Override
    public GbcImage clone() {
        try {
            return (GbcImage) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
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

    public HashSet<String> getTags() {
        return tags;
    }

    public void setTags(HashSet<String> tags) {
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

}
