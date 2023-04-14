package com.mraulio.gbcameramanager;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.mraulio.gbcameramanager.model.GbcPalette;

@Database(entities = {GbcPalette.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PaletteDao paletteDao();

}
