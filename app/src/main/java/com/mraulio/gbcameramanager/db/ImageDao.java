package com.mraulio.gbcameramanager.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;

import java.util.List;

@Dao
public interface ImageDao {
    @Query("SELECT * FROM gbcimage")
    List<GbcImage> getAll();

    @Query("SELECT * FROM gbcimage WHERE hashcode IN (:nameIds)")
    List<GbcImage> loadAllByIds(int[] nameIds);

    @Query("SELECT * FROM gbcimage WHERE hashcode LIKE :first LIMIT 1")
    GbcImage findByName(String first);

    @Insert
    void insertAll(GbcImage... gbcimage);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(GbcImage gbcimage);

    @Delete
    void delete(GbcImage gbcimage);

    @Query("DELETE FROM gbcimage")
    void deleteAll();
}
