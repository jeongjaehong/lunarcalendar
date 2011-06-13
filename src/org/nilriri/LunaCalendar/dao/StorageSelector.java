package org.nilriri.LunaCalendar.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public interface StorageSelector {

    public abstract Context getContext();

    public abstract void close();

    public abstract SQLiteDatabase getWritableDatabase();

    public abstract SQLiteDatabase getReadableDatabase();

    abstract void onDestroy();

}
