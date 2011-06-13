package org.nilriri.LunaCalendar.dao;

import org.nilriri.LunaCalendar.tools.Common;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

public abstract class AbstractDao {

    private StorageSelector daoIf = null;

    public AbstractDao(Context context, CursorFactory factory, boolean sdcarduse) {
        if (sdcarduse && Common.isSdPresent()) {
            daoIf = new ExternalStorage(context, Constants.getExternalDatabaseName(), factory, Constants.DATABASE_VERSION);
        } else {
            daoIf = new InternalStorage(context, Constants.getDatabaseName(), factory, Constants.DATABASE_VERSION);
        }
    }

    public Context getContext() {
        return daoIf.getContext();
    }

    public SQLiteDatabase getWritableDatabase() {
        return daoIf.getWritableDatabase();
    }

    public SQLiteDatabase getReadableDatabase() {
        return daoIf.getReadableDatabase();
    }

    public void close() {
        if (daoIf != null)
            daoIf.close();
    }

    public void onDestroy() {
        if (daoIf != null) {
            daoIf.close();
        }
        daoIf.onDestroy();
    }

}
