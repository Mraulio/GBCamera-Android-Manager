package com.mraulio.gbcameramanager;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcPalette;

import java.util.List;

@Dao
public interface FrameDao {
    @Query("SELECT * FROM gbcframe")
    List<GbcFrame> getAll();

    @Query("SELECT * FROM gbcframe WHERE frameName IN (:frameNameIds)")
    List<GbcFrame> loadAllByIds(int[] frameNameIds);

    @Query("SELECT * FROM gbcframe WHERE frameName LIKE :first LIMIT 1")
    GbcFrame findByName(String first);

    @Insert
    void insertAll(GbcFrame... gbcframe);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(GbcFrame gbcframe);

    @Delete
    void delete(GbcFrame gbcframe);

    @Query("DELETE FROM gbcframe")
    void deleteAll();
}
