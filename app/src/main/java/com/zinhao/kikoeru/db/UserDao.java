package com.zinhao.kikoeru.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import com.zinhao.kikoeru.User;
import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;
import org.greenrobot.greendao.internal.DaoConfig;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * DAO for table "USER".
 */
public class UserDao extends AbstractDao<User, Long> {

    public static final String TABLENAME = "USER";

    /**
     * Properties of entity User.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property Name = new Property(1, String.class, "name", false, "NAME");
        public final static Property Password = new Property(2, String.class, "password", false, "PASSWORD");
        public final static Property Host = new Property(3, String.class, "host", false, "HOST");
        public final static Property Token = new Property(4, String.class, "token", false, "TOKEN");
        public final static Property LastUpdateTime = new Property(5, long.class, "lastUpdateTime", false, "LAST_UPDATE_TIME");
    }


    public UserDao(DaoConfig config) {
        super(config);
    }

    public UserDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /**
     * Creates the underlying database table.
     */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists ? "IF NOT EXISTS " : "";
        db.execSQL("CREATE TABLE " + constraint + "\"USER\" (" + //
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                "\"NAME\" TEXT," + // 1: name
                "\"PASSWORD\" TEXT," + // 2: password
                "\"HOST\" TEXT," + // 3: host
                "\"TOKEN\" TEXT," + // 4: token
                "\"LAST_UPDATE_TIME\" INTEGER NOT NULL );"); // 5: lastUpdateTime
    }

    /**
     * Drops the underlying database table.
     */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"USER\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, User entity) {
        stmt.clearBindings();

        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }

        String name = entity.getName();
        if (name != null) {
            stmt.bindString(2, name);
        }

        String password = entity.getPassword();
        if (password != null) {
            stmt.bindString(3, password);
        }

        String host = entity.getHost();
        if (host != null) {
            stmt.bindString(4, host);
        }

        String token = entity.getToken();
        if (token != null) {
            stmt.bindString(5, token);
        }
        stmt.bindLong(6, entity.getLastUpdateTime());
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, User entity) {
        stmt.clearBindings();

        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }

        String name = entity.getName();
        if (name != null) {
            stmt.bindString(2, name);
        }

        String password = entity.getPassword();
        if (password != null) {
            stmt.bindString(3, password);
        }

        String host = entity.getHost();
        if (host != null) {
            stmt.bindString(4, host);
        }

        String token = entity.getToken();
        if (token != null) {
            stmt.bindString(5, token);
        }
        stmt.bindLong(6, entity.getLastUpdateTime());
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }

    @Override
    public User readEntity(Cursor cursor, int offset) {
        User entity = new User( //
                cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
                cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1), // name
                cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2), // password
                cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3), // host
                cursor.isNull(offset + 4) ? null : cursor.getString(offset + 4), // token
                cursor.getLong(offset + 5) // lastUpdateTime
        );
        return entity;
    }

    @Override
    public void readEntity(Cursor cursor, User entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setName(cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1));
        entity.setPassword(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
        entity.setHost(cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3));
        entity.setToken(cursor.isNull(offset + 4) ? null : cursor.getString(offset + 4));
        entity.setLastUpdateTime(cursor.getLong(offset + 5));
    }

    @Override
    protected final Long updateKeyAfterInsert(User entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }

    @Override
    public Long getKey(User entity) {
        if (entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(User entity) {
        return entity.getId() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }

}
