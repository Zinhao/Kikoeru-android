package com.zinhao.kikoeru.db;

import androidx.room.*;

import java.util.List;

@Dao
public interface UserDao {
    @Query("SELECT * FROM users")
    List<User> getAllUser();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(User user);

    @Delete
    void delete(User user);

    @Update
    void update(User user);
}
