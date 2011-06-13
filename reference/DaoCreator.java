package org.nilriri.LunaCalendar.dao;

import org.nilriri.LunaCalendar.R;
import org.nilriri.LunaCalendar.dao.Constants.Schedule;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

public class DaoCreator {
    private Context mContext;

    public void onCreate(Context context, SQLiteDatabase db) {

        mContext = context;

        onCreateSchedule(db);

        onCreateDays(db);

        onCreateIndex(db);

        onInsertAnniversary(context, db);

        onCreateDumy(db);

        onCreateTargetView(db);

        onCreateCalendarView(db);

    }

    public void onCreateSchedule(SQLiteDatabase db) {
        try {
            StringBuffer query = null;
            query = new StringBuffer();
            query.append("CREATE TABLE " + Schedule.SCHEDULE_TABLE_NAME + " (");
            query.append("    " + Schedule._ID + " INTEGER PRIMARY KEY AUTOINCREMENT ");
            query.append("    ," + Schedule.SCHEDULE_DATE + " TEXT NOT NULL");
            query.append("    ," + Schedule.SCHEDULE_TITLE + " TEXT ");
            query.append("    ," + Schedule.SCHEDULE_CONTENTS + " TEXT ");
            query.append("    ," + Schedule.SCHEDULE_REPEAT + " INTEGER ");
            query.append("    ," + Schedule.SCHEDULE_CHECK + " VARCHAR NOT NULL DEFAULT N ");
            query.append("    ," + Schedule.ALARM_LUNASOLAR + " INTEGER ");
            query.append("    ," + Schedule.ALARM_DATE + " VARCHR ");
            query.append("    ," + Schedule.ALARM_TIME + " VARCHR ");
            query.append("    ," + Schedule.ALARM_DAYOFWEEK + " INTEGER ");
            query.append("    ," + Schedule.ALARM_DAY + " INTEGER ");
            query.append("    ," + Schedule.DDAY_ALARMYN + " INTEGER ");
            query.append("    ," + Schedule.DDAY_ALARMDAY + " INTEGER ");
            query.append("    ," + Schedule.DDAY_ALARMSIGN + " VARCHAR ");
            query.append("    ," + Schedule.DDAY_DISPLAYYN + " INTEGER ");
            query.append("    ," + Schedule.GID + " VARCHAR ");
            query.append("    ," + Schedule.ANNIVERSARY + " VARCHAR ");
            query.append("    ," + Schedule.LUNARYN + " VARCHAR ");
            query.append("    ," + Schedule.SCHEDULE_LDATE + " VARCHAR ");
            query.append("    ," + Schedule.ALARM_DETAILINFO + " VARCHAR ");
            query.append("    ," + Schedule.DDAY_DETAILINFO + " VARCHAR ");
            query.append("    ," + Schedule.SCHEDULE_TYPE + " VARCHAR ");
            query.append("    ," + Schedule.BIBLE_BOOK + " INTEGER ");
            query.append("    ," + Schedule.BIBLE_CHAPTER + " INTEGER ");
            query.append("    ," + Schedule.ETAG + " VARCHAR ");
            query.append("    ," + Schedule.PUBLISHED + " VARCHAR ");
            query.append("    ," + Schedule.UPDATED + " VARCHAR ");
            query.append("    ," + Schedule.WHEN + " VARCHAR ");
            query.append("    ," + Schedule.WHO + " VARCHAR ");
            query.append("    ," + Schedule.RECURRENCE + " VARCHAR ");
            query.append("    ," + Schedule.SELFURL + " VARCHAR ");
            query.append("    ," + Schedule.EDITURL + " VARCHAR ");
            query.append("    ," + Schedule.ORIGINALEVENT + " VARCHAR ");
            query.append("    ," + Schedule.EVENTSTATUS + " VARCHAR ");
            query.append("    ) ");
            db.execSQL(query.toString());
        } catch (Exception e) {
            Log.e("onCreate", e.getMessage(), e);
        }
    }

    public void onCreateTargetView(SQLiteDatabase db) {
        try {
            StringBuffer query = new StringBuffer("create  view  if not exists vw_target as ");

            query.append("select /* 일정, 기념일, 반복일정 - 음력 */ ");
            query.append("        _id, ");
            query.append("        case when anniversary = 'Y' then  ");
            query.append("                case when strftime('%m-', date(schedule_date)) < '03' then strftime('%Y-', date('now', '-1 years'))  ");
            query.append("                        else strftime('%Y-', date('now')) end || substr(schedule_ldate, -5)  ");
            query.append("            when schedule_repeat = 1 then null ");
            query.append("            when schedule_repeat = 2 then null ");
            query.append("            when schedule_repeat = 3 then null ");
            query.append("            when schedule_repeat = 4 then strftime('%Y-%m-', date('now')) || alarm_day ");
            query.append("            when schedule_repeat = 5 then strftime('%Y-', date('now')) || substr(alarm_date, -5) ");
            query.append("            when schedule_repeat = 6 then null ");
            query.append("            when anniversary = 'N' and schedule_repeat = 0 then schedule_ldate ");
            query.append("            else schedule_ldate end as ltarget_date , ");
            query.append("        null starget_date, ");
            query.append("        case when schedule_repeat = 1 then null ");
            query.append("            when schedule_repeat = 2 then null ");
            query.append("            when schedule_repeat = 3 then null ");
            query.append("            when schedule_repeat = 4 then strftime('%Y-%m-', date('now')) || alarm_day ");
            query.append("            when schedule_repeat = 5 then strftime('%Y-', date('now')) || substr(alarm_date, -5) ");
            query.append("            when schedule_repeat = 6 then null ");
            query.append("            else null end as alarm_date , ");
            query.append("        case when schedule_repeat between 1 and 5 then alarm_time  ");
            query.append("            when schedule_repeat = 6 and  ");
            query.append("                cast (julianday('now', 'localtime') -  julianday(schedule_date, 'localtime') as integer) % alarm_day = 0  ");
            query.append("            then alarm_time  ");
            query.append("            else null end as alarm_time ");
            query.append("from schedule   ");
            query.append("where schedule_date > '1900-01-01'  ");
            query.append("    and (alarm_lunasolar = 1 or lunaryn = 'Y') ");
            query.append("union all ");
            query.append("select  /* 일정, 기념일, 반복일정 - 양력 */ ");
            query.append("        _id, ");
            query.append("        null ltarget_date, ");
            query.append("        case when anniversary = 'Y' then strftime('%Y-', date('now'))  || substr(schedule_date, -5)  ");
            query.append("            when schedule_repeat = 1 then alarm_date ");
            query.append("            when schedule_repeat = 2 then strftime('%Y-%m-%d', date('now'))  ");
            query.append("            when schedule_repeat = 3 then strftime('%Y-%m-%d', date('now', 'weekday '||alarm_days))  ");
            query.append("            when schedule_repeat = 4 then strftime('%Y-%m-', date('now')) || alarm_day ");
            query.append("            when schedule_repeat = 5 then strftime('%Y-', date('now')) || substr(alarm_date, -5) ");
            query.append("            when schedule_repeat = 6 and  ");
            query.append("                    cast (julianday('now', 'localtime') -  julianday(schedule_date, 'localtime') as integer) % alarm_day = 0  ");
            query.append("            then strftime('%Y-%m-%d', date('now'))  ");
            query.append("            when anniversary = 'N' and schedule_repeat = 0 then schedule_date ");
            query.append("            else schedule_date end as starget_date , ");
            query.append("        case when schedule_repeat = 1 then alarm_date ");
            query.append("            when schedule_repeat = 2 then strftime('%Y-%m-%d', date('now'))  ");
            query.append("            when schedule_repeat = 3 then strftime('%Y-%m-%d', date('now', 'weekday '||alarm_days))  ");
            query.append("            when schedule_repeat = 4 then strftime('%Y-%m-', date('now')) || alarm_day ");
            query.append("            when schedule_repeat = 5 then strftime('%Y-', date('now')) || substr(alarm_date, -5) ");
            query.append("            when schedule_repeat = 6 and  ");
            query.append("                    cast (julianday('now', 'localtime') -  julianday(schedule_date, 'localtime') as integer) % alarm_day = 0  ");
            query.append("            then strftime('%Y-%m-%d', date('now'))  ");
            query.append("            else null end as alarm_date , ");
            query.append("        case when schedule_repeat between 1 and 5 then alarm_time  ");
            query.append("            when schedule_repeat = 6 and  ");
            query.append("                    cast (julianday('now', 'localtime') -  julianday(schedule_date, 'localtime') as integer) % alarm_day = 0  ");
            query.append("            then alarm_time  ");
            query.append("            else null end as alarm_time ");
            query.append("from schedule   ");
            query.append("where schedule_date > '1900-01-01'  ");
            query.append("    and alarm_lunasolar = 0  ");
            query.append("    and lunaryn = 'N' ");

            db.execSQL(query.toString());
        } catch (Exception e) {
            Log.e("onCreateTargetView", e.getMessage(), e);
        }
    }

    public void onCreateCalendarView(SQLiteDatabase db) {
        try {
            StringBuffer query = new StringBuffer("create  view  if not exists vw_calendar as ");

            query.append("    select  ");
            query.append("        strftime('%Y', y.yyyy||'-01-01', 'localtime') year, ");
            query.append("        strftime('%m', y.yyyy||'-01-01', '+'||c.idx||' day', 'localtime') month, ");
            query.append("        strftime('%d', y.yyyy||'-01-01', '+'||c.idx||' day', 'localtime') dayofmonth, ");
            query.append("        strftime('%Y-%m-%d', y.yyyy||'-01-01', '+'||c.idx || ' day', 'localtime') basedate, ");
            query.append("        strftime('%W', y.yyyy||'-01-01', '+'||c.idx || ' day', 'localtime') weekofyear, ");
            query.append("        strftime('%w', y.yyyy||'-01-01', '+'||c.idx || ' day', 'localtime') dayofweek, ");
            query.append("        strftime('%j', y.yyyy||'-01-01', '+'||c.idx || ' day', 'localtime') dayofyear, ");
            query.append("        n.dayname, ");
            query.append("        n.dayname || '요일' as dayfullname, ");
            query.append("        c.idx ");
            query.append("    from dumy c ");
            query.append("        inner join ( ");
            query.append("            select ");
            query.append("                idx,  ");
            query.append("                strftime('%Y', '1900-01-01', 'start of year', '+'||d.idx || ' year', 'localtime') as yyyy, ");
            query.append("                strftime('%j', '1900-01-01', '+'||(d.idx+1)|| ' year', '-1 day', 'localtime') as dayofyear ");
            query.append("            from dumy d ) y on c.idx < y.dayofyear ");
            query.append("        inner join days n on n.days = strftime('%w', y.yyyy||'-01-01', '+'||c.idx || ' day', 'localtime') ");

            db.execSQL(query.toString());
        } catch (Exception e) {
            Log.e("onCreateTargetView", e.getMessage(), e);
        }
    }

    public void onCreateDays(SQLiteDatabase db) {
        try {
            db.execSQL(" DROP TABLE IF EXISTS days ");
            db.execSQL(" CREATE TABLE days ( days integer, dayname varchar) ");

            db.beginTransaction();

            db.execSQL(" insert into days values (0, '일') ");
            db.execSQL(" insert into days values (1, '월') ");
            db.execSQL(" insert into days values (2, '화') ");
            db.execSQL(" insert into days values (3, '수') ");
            db.execSQL(" insert into days values (4, '목') ");
            db.execSQL(" insert into days values (5, '금') ");
            db.execSQL(" insert into days values (6, '토') ");

            db.setTransactionSuccessful();

            db.endTransaction();
        } catch (Exception e) {
            Log.e("onCreate", e.getMessage(), e);
        }
    }

    public void onCreateDumy(SQLiteDatabase db) {
        try {
            db.execSQL(" CREATE  TABLE  IF NOT EXISTS dumy (_id INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL, idx INTEGER , str VARCHAR) ");

            db.beginTransaction();

            for (int i = 0; i <= 366; i++) {
                db.execSQL(" insert into dumy (idx, str) values (" + i + ", '" + i + "') ");
            }
            db.setTransactionSuccessful();

            db.endTransaction();
        } catch (Exception e) {
            Log.e("onCreate", e.getMessage(), e);
        }
    }

    public void onInsertAnniversary(Context context, SQLiteDatabase db) {
        try {
            db.beginTransaction();

            String Anniversary[] = context.getResources().getStringArray(R.array.array_anniversary);

            db.execSQL("delete from schedule where schedule_date = '1900-01-01' and schedule_repeat = 9 and alarm_time = '00:00'");
            for (int i = 0; i < Anniversary.length; i++) {
                db.execSQL(Anniversary[i]);
            }
            db.setTransactionSuccessful();
            db.endTransaction();
        } catch (Exception e) {
            Log.e("onCreate", e.getMessage(), e);
        }
    }

    public void onDeleteBiblePlan(Context context, SQLiteDatabase db) {
        try {
            db.execSQL("delete from schedule where schedule_date = '1900-01-01' and schedule_repeat = 8 ");
        } catch (Exception e) {
            Log.e("onCreate", e.getMessage(), e);
        }
    }

    public void onAlterTable(SQLiteDatabase db) {
        try {
            db.execSQL("alter table schedule add gid varchar");
        } catch (Exception e) {
            Log.e("onCreate", e.getMessage(), e);
        }
    }

    public void onAlterTable2(SQLiteDatabase db) {
        try {
            db.execSQL("alter table schedule add anniversary varchar");
            db.execSQL("alter table schedule add lunaryn varchar");
            db.execSQL("alter table schedule add schedule_ldate varchar");
        } catch (Exception e) {
            Log.e("onCreate", e.getMessage(), e);
        }
    }

    public void onAlterTable3(SQLiteDatabase db) {
        try {
            db.execSQL("alter table schedule add alarm_detailinfo varchar");
            db.execSQL("alter table schedule add dday_detailinfo varchar");
            db.execSQL("alter table schedule add schedule_type varchar");
        } catch (Exception e) {
            Log.e("onCreate", e.getMessage(), e);
        }
    }

    public void onAlterTable4(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD " + Schedule.ETAG + " VARCHAR ");
            db.execSQL("ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD " + Schedule.PUBLISHED + " VARCHAR ");
            db.execSQL("ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD " + Schedule.UPDATED + " VARCHAR ");
            db.execSQL("ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD " + Schedule.WHEN + " VARCHAR ");
            db.execSQL("ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD " + Schedule.WHO + " VARCHAR ");
            db.execSQL("ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD " + Schedule.RECURRENCE + " VARCHAR ");
            db.execSQL("ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD " + Schedule.ORIGINALEVENT + " VARCHAR ");
            db.execSQL("ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD " + Schedule.EVENTSTATUS + " VARCHAR ");
            db.execSQL("ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD " + Schedule.EDITURL + " VARCHAR ");
            db.execSQL("ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD " + Schedule.SELFURL + " VARCHAR ");
        } catch (Exception e) {
            Log.e("onCreate", e.getMessage(), e);
        }
    }

    public void onCreateIndex(SQLiteDatabase db) {
        try {
            StringBuffer query = null;

            query = new StringBuffer();
            query.append("CREATE INDEX idx_ddaydisplayyn ON schedule (dday_displayyn ASC) ");
            db.execSQL(query.toString());

            query = new StringBuffer();
            query.append("CREATE  INDEX idx_scheduledate ON schedule (schedule_date ASC) ");
            db.execSQL(query.toString());

            query = new StringBuffer();
            query.append("CREATE  INDEX idx_alarm_01 ON schedule (schedule_repeat ASC, schedule_check ASC, alarm_date ASC, alarm_time ASC) ");
            db.execSQL(query.toString());
        } catch (Exception e) {
            Log.e("onCreate", e.getMessage(), e);
        }
    }

    public void onUpgrade(Context context, SQLiteDatabase db, int oldVersion, int newVersion) {
        mContext = context;

        Toast.makeText(mContext, "DataBase Upgrading...", Toast.LENGTH_LONG).show();

        StringBuffer query = new StringBuffer();
        switch (newVersion) {
            case 1:

                onCreate(context, db);
                break;

            case 2:
                db.beginTransaction();

                db.execSQL(" DROP TABLE IF EXISTS sample ");

                db.execSQL(" ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD COLUMN schedule_check VARCHR NOT NULL DEFAULT N ");
                db.execSQL(" ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD COLUMN alarm_lunasolar INTEGER ");
                db.execSQL(" ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD COLUMN alarm_date VARCHR ");
                db.execSQL(" ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD COLUMN alarm_time VARCHR ");
                db.execSQL(" ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD COLUMN alarm_days INTEGER ");
                db.execSQL(" ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD COLUMN alarm_day INTEGER ");

                db.setTransactionSuccessful();
                db.endTransaction();
                break;
            case 3:
                onCreateDays(db);
                break;
            case 4:
            case 5: // date, time 형식 migration...

                db.beginTransaction();

                db.execSQL(" ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD COLUMN " + Schedule.DDAY_ALARMYN + " INTEGER ");
                db.execSQL(" ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD COLUMN " + Schedule.DDAY_ALARMDAY + " INTEGER ");
                db.execSQL(" ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD COLUMN " + Schedule.DDAY_ALARMSIGN + " VARCHR ");
                db.execSQL(" ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD COLUMN " + Schedule.DDAY_DISPLAYYN + " INTEGER ");

                query = new StringBuffer();
                query.append(" UPDATE SCHEDULE SET SCHEDULE_CHECK= ");
                query.append(" (DATE( REPLACE(SUBSTR(SCHEDULE_CHECK, 1, 4), '.', '') ||'-'|| ");
                query.append(" (CASE WHEN LENGTH(REPLACE(SUBSTR(SCHEDULE_CHECK, 6, 2), '.', '')) > 1 THEN REPLACE(SUBSTR(SCHEDULE_CHECK, 6, 2), '.', '') ELSE '0'||REPLACE(SUBSTR(SCHEDULE_CHECK, 6, 2), '.', '') END) ||'-'|| ");
                query.append(" (CASE WHEN LENGTH(REPLACE(SUBSTR(SCHEDULE_CHECK, 8), '.', '')) > 1 THEN REPLACE(SUBSTR(SCHEDULE_CHECK, 8), '.', '') ELSE '0'||REPLACE(SUBSTR(SCHEDULE_CHECK, 8), '.', '') END ) , 'LOCALTIME' )) ");
                query.append(" WHERE LENGTH(SCHEDULE_CHECK) >= 8 ");
                db.execSQL(query.toString());

                query = new StringBuffer();
                query.append(" UPDATE SCHEDULE SET SCHEDULE_DATE= ");
                query.append(" (DATE( REPLACE(SUBSTR(SCHEDULE_DATE, 1, 4), '.', '') ||'-'|| ");
                query.append(" (CASE WHEN LENGTH(REPLACE(SUBSTR(SCHEDULE_DATE, 6, 2), '.', '')) > 1 THEN REPLACE(SUBSTR(SCHEDULE_DATE, 6, 2), '.', '') ELSE '0'||REPLACE(SUBSTR(SCHEDULE_DATE, 6, 2), '.', '') END) ||'-'|| ");
                query.append(" (CASE WHEN LENGTH(REPLACE(SUBSTR(SCHEDULE_DATE, 8), '.', '')) > 1 THEN REPLACE(SUBSTR(SCHEDULE_DATE, 8), '.', '') ELSE '0'||REPLACE(SUBSTR(SCHEDULE_DATE, 8), '.', '') END ) , 'LOCALTIME' )) ");
                query.append(" WHERE LENGTH(SCHEDULE_DATE) >= 8 ");
                db.execSQL(query.toString());

                query = new StringBuffer();
                query.append(" UPDATE SCHEDULE SET ALARM_DATE= ");
                query.append(" (DATE( REPLACE(SUBSTR(ALARM_DATE, 1, 4), '.', '') ||'-'|| ");
                query.append(" (CASE WHEN LENGTH(REPLACE(SUBSTR(ALARM_DATE, 6, 2), '.', '')) > 1 THEN REPLACE(SUBSTR(ALARM_DATE, 6, 2), '.', '') ELSE '0'||REPLACE(SUBSTR(ALARM_DATE, 6, 2), '.', '') END) ||'-'|| ");
                query.append(" (CASE WHEN LENGTH(REPLACE(SUBSTR(ALARM_DATE, 8), '.', '')) > 1 THEN REPLACE(SUBSTR(ALARM_DATE, 8), '.', '') ELSE '0'||REPLACE(SUBSTR(ALARM_DATE, 8), '.', '') END ) , 'LOCALTIME' )) ");
                query.append(" WHERE LENGTH(ALARM_DATE) >= 8 ");
                db.execSQL(query.toString());

                query = new StringBuffer();
                query.append("UPDATE SCHEDULE ");
                query.append("   SET ALARM_DATE = ");
                query.append("             (CASE ");
                query.append("                 WHEN LENGTH (REPLACE (SUBSTR ('0000.' || ALARM_DATE, 6, 2),'.','')) > 1 ");
                query.append("                    THEN REPLACE (SUBSTR ('0000.' || ALARM_DATE, 6, 2),'.','') ");
                query.append("                 ELSE     ");
                query.append("                    '0'|| REPLACE (SUBSTR ('0000.' || ALARM_DATE, 6, 2),'.','') ");
                query.append("              END) || '-' ||  ");
                query.append("             (CASE ");
                query.append("                 WHEN LENGTH (REPLACE (SUBSTR ('0000.' || ALARM_DATE, 8),'.','')) > 1 ");
                query.append("                    THEN REPLACE (SUBSTR ('0000.' || ALARM_DATE, 8), '.', '') ");
                query.append("                 ELSE    ");
                query.append("                     '0'|| REPLACE (SUBSTR ('0000.' || ALARM_DATE, 8), '.', '') ");
                query.append("              END) ");
                query.append(" WHERE LENGTH (ALARM_DATE) BETWEEN 3 AND 5 ");
                db.execSQL(query.toString());

                query = new StringBuffer();
                query.append(" UPDATE SCHEDULE SET ALARM_TIME = ");
                query.append(" SUBSTR('0'||(CAST(REPLACE(SUBSTR((REPLACE(REPLACE(REPLACE(ALARM_TIME, 'AM:','0'), 'PM:', '0'),':',':0')),1,3),':','') AS INTEGER) ");
                query.append(" + CASE SUBSTR(ALARM_TIME, 1, 2) WHEN 'PM' THEN 12 WHEN 'AM' THEN 0 ELSE NULL END), -2) ||':'|| ");
                query.append(" SUBSTR( REPLACE(SUBSTR((REPLACE(REPLACE(REPLACE(ALARM_TIME, 'AM:','0'), 'PM:', '0'),':',':0')),4),':','') ,-2) ");
                query.append(" WHERE LENGTH(ALARM_TIME) >= 6 ");
                db.execSQL(query.toString());

                query = new StringBuffer();
                query.append("UPDATE SCHEDULE SET SCHEDULE_DATE = DATE(SCHEDULE_DATE,'1 MONTH', 'LOCALTIME')  ");
                db.execSQL(query.toString());

                query = new StringBuffer();
                query.append("UPDATE SCHEDULE SET ALARM_DATE = DATE(ALARM_DATE,'1 MONTH', 'LOCALTIME') WHERE LENGTH(ALARM_DATE) = 10 ");
                db.execSQL(query.toString());

                query = new StringBuffer();
                query.append("UPDATE SCHEDULE SET ALARM_DATE = STRFTIME('%M-%D', DATE('2010-'||ALARM_DATE, '+1 MONTH', 'LOCALTIME'), 'LOCALTIME') WHERE LENGTH(ALARM_DATE) = 5 ");
                db.execSQL(query.toString());

                db.setTransactionSuccessful();
                db.endTransaction();

                onCreateIndex(db);
                break;
            case 6:
            case 7:
            case 8:
            case 9:
                onAlterTable(db);
                break;
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
                onAlterTable2(db);
                onInsertAnniversary(context, db);
                break;
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
                onInsertAnniversary(context, db);
                break;
            case 27:
                // 맥체인 성경읽기 계획표를 추가하기 위한 컬럼추가.
                try {
                    db.execSQL(" ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD COLUMN " + Schedule.BIBLE_BOOK + " INTEGER ");
                    db.execSQL(" ALTER TABLE " + Schedule.SCHEDULE_TABLE_NAME + " ADD COLUMN " + Schedule.BIBLE_CHAPTER + " INTEGER ");
                } catch (Exception e) {
                    Log.e("onCreate", e.getMessage(), e);
                }
                //onInsertBiblePlan(context, db);
                break;
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
                onAlterTable3(db);
                //google gdata sync를 위한 etag컬럼 추가.
                onAlterTable4(db);
                onDeleteBiblePlan(context, db);
                onCreateDumy(db);
                onCreateDays(db);
                onCreateTargetView(db);
                onCreateCalendarView(db);
                break;
        }

    }

}
