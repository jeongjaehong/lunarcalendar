package org.nilriri.LunaCalendar.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.os.SystemClock;

public class ExternalStorage extends SQLiteOpenHelper implements StorageSelector {
    private Context mContext;
    private CursorFactory mFactory;

    private SQLiteDatabase db;

    public ExternalStorage(Context context, String name, CursorFactory factory, int version) {
        super(context, name, factory, version);

        mContext = context;

        boolean isOpen = false;
        SQLiteDatabase db = null;
        while (!isOpen) {
            try {
                db = SQLiteDatabase.openOrCreateDatabase(Constants.getExternalDatabaseName(), factory);
                isOpen = true;
            } catch (SQLiteException e) {

                e.printStackTrace();
                SystemClock.sleep(200);
                isOpen = false;
            }
        }

        if (Constants.DATABASE_VERSION != db.getVersion()) {
            switch (db.getVersion()) {
                case 0:
                    onCreate(db);
                    break;
                default:
                    onUpgrade(db, db.getVersion(), Constants.DATABASE_VERSION);
                    break;
            }
            db.setVersion(Constants.DATABASE_VERSION);
        }
        //db.close();
        db = getWritableDatabase();
    }

    public SQLiteDatabase getReadableDatabase() {
        //SQLiteDatabase db = null;
        if (db == null) {
            db = SQLiteDatabase.openDatabase(Constants.getExternalDatabaseName(), mFactory, SQLiteDatabase.OPEN_READONLY);
        } else if (!db.isOpen()) {
            db = SQLiteDatabase.openDatabase(Constants.getExternalDatabaseName(), mFactory, SQLiteDatabase.OPEN_READONLY);
        }
        return db;
    }

    public SQLiteDatabase getWritableDatabase() {
        //SQLiteDatabase db = null;
        if (db == null) {
            db = SQLiteDatabase.openDatabase(Constants.getExternalDatabaseName(), mFactory, SQLiteDatabase.OPEN_READWRITE);
        } else if (db.isReadOnly()) {
            close();
            db = SQLiteDatabase.openDatabase(Constants.getExternalDatabaseName(), mFactory, SQLiteDatabase.OPEN_READWRITE);
        } else if (!db.isOpen()) {
            db = SQLiteDatabase.openDatabase(Constants.getExternalDatabaseName(), mFactory, SQLiteDatabase.OPEN_READWRITE);
        }
        return db;
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        new DaoCreator().onCreate(this.mContext, db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        new DaoCreator().onUpgrade(this.mContext, db, oldVersion, newVersion);
    }

    public Context getContext() {
        return mContext;
    }

    public void onDestroy() {
        close();
    }

}
