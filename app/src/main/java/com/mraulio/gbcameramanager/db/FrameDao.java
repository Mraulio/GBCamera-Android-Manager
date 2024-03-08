package com.mraulio.gbcameramanager.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.mraulio.gbcameramanager.model.GbcFrame;
import java.util.List;

@Dao
public interface FrameDao {
    @Query("SELECT * FROM gbcframe")
    List<GbcFrame> getAll();

    @Query("SELECT * FROM gbcframe WHERE frame_id IN (:frameNameIds)")
    List<GbcFrame> loadAllByIds(int[] frameNameIds);

    @Query("SELECT * FROM gbcframe WHERE frame_id LIKE :first LIMIT 1")
    GbcFrame findByName(String first);

    @Insert
    void insertAll(GbcFrame... gbcframe);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(GbcFrame gbcframe);

    @Update
    void update(GbcFrame gbcframe);
    @Delete
    void delete(GbcFrame gbcframe);

    @Query("DELETE FROM gbcframe")
    void deleteAll();
}
