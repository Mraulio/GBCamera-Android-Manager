package com.mraulio.gbcameramanager.db;

import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;
import com.mraulio.gbcameramanager.model.ImageData;

@Database(entities = {GbcImage.class, GbcPalette.class, GbcFrame.class, ImageData.class}, version = 3, autoMigrations = {
        @AutoMigration(from = 1, to = 2), @AutoMigration(from = 2, to = 3)})
@androidx.room.TypeConverters(TypeConverters.class)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ImageDao imageDao();

    public abstract ImageDataDao imageDataDao();

    public abstract PaletteDao paletteDao();

    public abstract FrameDao frameDao();
}