package com.mraulio.gbcameramanager.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcPalette;

@Database(entities = {GbcPalette.class, GbcFrame.class}, version = 1)
@TypeConverters(BitmapConverter.class)//To convert the bitmap to byte[] to be able to store it in the database
public abstract class AppDatabase extends RoomDatabase {
    public abstract PaletteDao paletteDao();
    public abstract FrameDao frameDao();

}

