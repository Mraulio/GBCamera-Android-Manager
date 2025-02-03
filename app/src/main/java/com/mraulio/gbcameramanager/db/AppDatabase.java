package com.mraulio.gbcameramanager.db;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;
import com.mraulio.gbcameramanager.model.ImageData;

@Database(entities = {GbcImage.class, GbcPalette.class, GbcFrame.class, ImageData.class}, version = 6, autoMigrations = {
        @AutoMigration(from = 5, to = 6)})
@androidx.room.TypeConverters(TypeConverters.class)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ImageDao imageDao();

    public abstract ImageDataDao imageDataDao();

    public abstract PaletteDao paletteDao();

    public abstract FrameDao frameDao();

}