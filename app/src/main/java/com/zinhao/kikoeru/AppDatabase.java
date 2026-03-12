package com.zinhao.kikoeru;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {User.class}, version = 1)
abstract class AppDatabase extends RoomDatabase{
    abstract UserDao userDao();
}
