package com.mraulio.gbcameramanager.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;
import com.mraulio.gbcameramanager.model.ImageData;

@Database(entities = {GbcImage.class, GbcPalette.class, GbcFrame.class, ImageData.class}, version = 1)
@androidx.room.TypeConverters(TypeConverters.class)
//To convert the bitmap to byte[] to be able to store it in the database
public abstract class AppDatabase extends RoomDatabase {
    public abstract PaletteDao paletteDao();
    public abstract FrameDao frameDao();
    public abstract ImageDao imageDao();
    public abstract ImageDataDao imageDataDao();
}

