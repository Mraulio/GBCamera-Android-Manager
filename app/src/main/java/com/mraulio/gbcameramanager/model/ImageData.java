package com.mraulio.gbcameramanager.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "image_data", foreignKeys = @ForeignKey(entity = GbcImage.class, parentColumns = "hashCode", childColumns = "image_id", onDelete = ForeignKey.CASCADE), indices = {@Index("image_id")})
public class ImageData {
    @PrimaryKey(autoGenerate = true)
    private int id;

    //This is the hashcode
    @ColumnInfo(name = "image_id")
    @NonNull
    private String imageId;

    @ColumnInfo(name = "data")
    private byte[] data;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}

