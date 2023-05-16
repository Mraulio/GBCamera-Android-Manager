package com.mraulio.gbcameramanager.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.mraulio.gbcameramanager.model.GbcPalette;

import java.util.List;

//https://developer.android.com/training/data-storage/room?hl=es-419#java

@Dao
public interface PaletteDao {
    @Query("SELECT * FROM gbcpalette")
    List<GbcPalette> getAll();

    @Query("SELECT * FROM gbcpalette WHERE paletteId IN (:paletteIds)")
    List<GbcPalette> loadAllByIds(int[] paletteIds);

    @Query("SELECT * FROM gbcpalette WHERE paletteId LIKE :first LIMIT 1")
    GbcPalette findByName(String first);

    @Insert
    void insertAll(GbcPalette... gbcpalette);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(GbcPalette gbcPalette);

    @Delete
    void delete(GbcPalette gbcpalette);

    @Query("DELETE FROM gbcpalette")
    void deleteAll();

}
