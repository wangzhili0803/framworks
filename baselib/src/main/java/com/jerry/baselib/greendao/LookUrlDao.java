package com.jerry.baselib.greendao;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;

import com.jerry.baselib.common.bean.LookUrl;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "LOOK_URL".
*/
public class LookUrlDao extends AbstractDao<LookUrl, Long> {

    public static final String TABLENAME = "LOOK_URL";

    /**
     * Properties of entity LookUrl.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property Url = new Property(1, String.class, "url", false, "URL");
        public final static Property Title = new Property(2, String.class, "title", false, "TITLE");
        public final static Property Content = new Property(3, String.class, "content", false, "CONTENT");
        public final static Property Selected = new Property(4, boolean.class, "selected", false, "SELECTED");
        public final static Property Link = new Property(5, String.class, "link", false, "LINK");
        public final static Property PicPath = new Property(6, String.class, "picPath", false, "PIC_PATH");
    }


    public LookUrlDao(DaoConfig config) {
        super(config);
    }
    
    public LookUrlDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"LOOK_URL\" (" + //
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                "\"URL\" TEXT," + // 1: url
                "\"TITLE\" TEXT," + // 2: title
                "\"CONTENT\" TEXT," + // 3: content
                "\"SELECTED\" INTEGER NOT NULL ," + // 4: selected
                "\"LINK\" TEXT," + // 5: link
                "\"PIC_PATH\" TEXT);"); // 6: picPath
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"LOOK_URL\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, LookUrl entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
 
        String url = entity.getUrl();
        if (url != null) {
            stmt.bindString(2, url);
        }
 
        String title = entity.getTitle();
        if (title != null) {
            stmt.bindString(3, title);
        }
 
        String content = entity.getContent();
        if (content != null) {
            stmt.bindString(4, content);
        }
        stmt.bindLong(5, entity.getSelected() ? 1L: 0L);
 
        String link = entity.getLink();
        if (link != null) {
            stmt.bindString(6, link);
        }
 
        String picPath = entity.getPicPath();
        if (picPath != null) {
            stmt.bindString(7, picPath);
        }
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, LookUrl entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
 
        String url = entity.getUrl();
        if (url != null) {
            stmt.bindString(2, url);
        }
 
        String title = entity.getTitle();
        if (title != null) {
            stmt.bindString(3, title);
        }
 
        String content = entity.getContent();
        if (content != null) {
            stmt.bindString(4, content);
        }
        stmt.bindLong(5, entity.getSelected() ? 1L: 0L);
 
        String link = entity.getLink();
        if (link != null) {
            stmt.bindString(6, link);
        }
 
        String picPath = entity.getPicPath();
        if (picPath != null) {
            stmt.bindString(7, picPath);
        }
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    @Override
    public LookUrl readEntity(Cursor cursor, int offset) {
        LookUrl entity = new LookUrl( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1), // url
            cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2), // title
            cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3), // content
            cursor.getShort(offset + 4) != 0, // selected
            cursor.isNull(offset + 5) ? null : cursor.getString(offset + 5), // link
            cursor.isNull(offset + 6) ? null : cursor.getString(offset + 6) // picPath
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, LookUrl entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setUrl(cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1));
        entity.setTitle(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
        entity.setContent(cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3));
        entity.setSelected(cursor.getShort(offset + 4) != 0);
        entity.setLink(cursor.isNull(offset + 5) ? null : cursor.getString(offset + 5));
        entity.setPicPath(cursor.isNull(offset + 6) ? null : cursor.getString(offset + 6));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(LookUrl entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(LookUrl entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(LookUrl entity) {
        return entity.getId() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
}
