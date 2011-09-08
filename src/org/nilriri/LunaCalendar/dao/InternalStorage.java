package org.nilriri.LunaCalendar.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.os.SystemClock;

public class InternalStorage extends SQLiteOpenHelper implements StorageSelector {

    private Context mContext;

    private SQLiteDatabase db;

    public InternalStorage(Context context, String name, CursorFactory factory, int version) {
        super(context, name, factory, version);
        mContext = context;
        //SQLiteDatabase db = null;

        boolean isOpen = false;
        while (!isOpen) {
            try {
                db = getWritableDatabase();
                isOpen = true;
            } catch (SQLiteException e) {

                e.printStackTrace();
                SystemClock.sleep(200);
                isOpen = false;
            }
        }
        //db.close();
        db = getWritableDatabase();

    }

    public Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        new DaoCreator().onCreate(this.mContext, db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        new DaoCreator().onUpgrade(this.mContext, db, oldVersion, newVersion);
    }

    public SQLiteDatabase getReadableDatabase() {
        //SQLiteDatabase db = null;
        if (db == null) {
            db = super.getReadableDatabase();
        } else if (!db.isOpen()) {
            db = super.getReadableDatabase();
        }
        return db;
    }

    public SQLiteDatabase getWritableDatabase() {
        //SQLiteDatabase db = null;
        if (db == null) {
            db = super.getWritableDatabase();
        } else if (db.isReadOnly()) {
            close();
            db = super.getWritableDatabase();
        } else if (!db.isOpen()) {
            db = super.getWritableDatabase();
        }
        return db;
    }

    @Override
    public void close() {
        super.close();
    }

    public void onDestroy() {
        close();
    }

}
