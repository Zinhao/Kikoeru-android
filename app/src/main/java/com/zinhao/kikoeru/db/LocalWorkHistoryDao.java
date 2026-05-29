package com.zinhao.kikoeru.db;

import androidx.room.*;

import java.util.List;

@Dao
public interface LocalWorkHistoryDao {
    @Query("SELECT * FROM local_work_history ORDER BY position")
    List<LocalWorkHistory> getAllHis();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(LocalWorkHistory his);

    @Update
    void update(LocalWorkHistory his);

    @Delete
    void delete(LocalWorkHistory his);
}
