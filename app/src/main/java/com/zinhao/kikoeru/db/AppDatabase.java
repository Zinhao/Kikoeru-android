package com.zinhao.kikoeru.db;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {User.class,LocalWorkHistory.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase{
    public abstract UserDao userDao();
    public abstract LocalWorkHistoryDao historyDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 1. 创建表（注意 rjNumber 必须是 NOT NULL，id 必须匹配 Expected 里的定义）
            database.execSQL("CREATE TABLE IF NOT EXISTS `local_work_history` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT, " + // 注意：这里不要加 NOT NULL，匹配 Expected: notNull=false
                    "`rjNumber` INTEGER NOT NULL, " +             // 关键点：匹配 Expected: notNull=true
                    "`position` INTEGER NOT NULL, " +            // 基本类型 long 必须 NOT NULL
                    "`coverUrl` TEXT, " +                        // 包装类 String 默认为 NULL
                    "`title` TEXT)");                            // 包装类 String 默认为 NULL

            // 2. 创建唯一索引（Room 期望的是独立的索引声明，而不是建表时的 UNIQUE 约束）
            // 注意：索引名称必须完全等于报错信息里的 index_local_work_history_rjNumber
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_local_work_history_rjNumber` " +
                    "ON `local_work_history` (`rjNumber`)");
        }
    };
}
