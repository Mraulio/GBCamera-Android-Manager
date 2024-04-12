package com.mraulio.gbcameramanager.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
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

    @Transaction
    @Delete
    void deleteItems(List<GbcFrame> gbcFrames);

    @Query("DELETE FROM gbcframe")
    void deleteAll();

    //If the frame id is modified I need to delete it and then insert again, because it's changing the Primary Key
    //Done in a transaction
    @Transaction
    default void updateFrameWithPrimaryKeyMod(GbcFrame gbcFrame,String newId,String newName) {

        delete(gbcFrame);

        gbcFrame.setFrameId(newId);
        gbcFrame.setFrameName(newName);

        insert(gbcFrame);
    }
}
