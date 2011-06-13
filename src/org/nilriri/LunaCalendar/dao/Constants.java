package org.nilriri.LunaCalendar.dao;

import android.os.Environment;
import android.provider.BaseColumns;

/**
 * Convenience definitions for NotePadProvider
 */
public final class Constants {

    private static final String DATABASE_NAME = "lunacalendar.sqlite";
    public static final int DATABASE_VERSION = 34;

    public static final String DEFAULT_SORT_ORDER = "_id DESC";

    public static String mColumns[] = new String[] { //
    Schedule._ID, //0
            Schedule.SCHEDULE_DATE, //1 
            Schedule.SCHEDULE_TITLE, //2
            Schedule.SCHEDULE_CONTENTS, //3 
            Schedule.SCHEDULE_REPEAT, //4
            Schedule.SCHEDULE_CHECK, //5
            Schedule.ALARM_LUNASOLAR, //6
            Schedule.ALARM_DATE, //7
            Schedule.ALARM_TIME, //8
            Schedule.ALARM_DAYOFWEEK, //9
            Schedule.ALARM_DAY, //10
            Schedule.DDAY_ALARMYN, //11
            Schedule.DDAY_ALARMDAY, //12
            Schedule.DDAY_ALARMSIGN, //13
            Schedule.DDAY_DISPLAYYN,//14
            Schedule.GID, //15
            Schedule.ANNIVERSARY, //16
            Schedule.LUNARYN, //17
            Schedule.SCHEDULE_LDATE, //18
            Schedule.ALARM_DETAILINFO, //19
            Schedule.DDAY_DETAILINFO, //20
            Schedule.SCHEDULE_TYPE, //21
            Schedule.BIBLE_BOOK,//22
            Schedule.BIBLE_CHAPTER,//23
            Schedule.ETAG,//24
            Schedule.PUBLISHED,//25
            Schedule.UPDATED,//26
            Schedule.WHEN,//27
            Schedule.WHO,//28
            Schedule.RECURRENCE,//29
            Schedule.SELFURL,//30
            Schedule.EDITURL,//31
            Schedule.ORIGINALEVENT,//32
            Schedule.EVENTSTATUS,//33
    };

    // This class cannot be instantiated
    private Constants() {
    }

    public static String getDatabaseName() {
        return DATABASE_NAME;
    }

    public static String getExternalDatabaseName() {
        return Environment.getExternalStorageDirectory() + "/" + DATABASE_NAME;
    }

    /**
     * Schedule table
     */
    public static final class Schedule implements BaseColumns {
        private Schedule() {
        }

        public static final String SCHEDULE_TABLE_NAME = "schedule";
        public static final String SCHEDULE_DAYS_JOIN_TABLE = "schedule s left join days d on s.alarm_days = d.days ";

        public static final String _ID = "_id";
        public static final String SCHEDULE_DATE = "schedule_date";
        public static final String SCHEDULE_TITLE = "schedule_title";
        public static final String SCHEDULE_CONTENTS = "schedule_contents";
        public static final String SCHEDULE_REPEAT = "schedule_repeat";
        public static final String SCHEDULE_CHECK = "schedule_check";
        public static final String ALARM_LUNASOLAR = "alarm_lunasolar";
        public static final String ALARM_DATE = "alarm_date";
        public static final String ALARM_TIME = "alarm_time";
        public static final String ALARM_DAYOFWEEK = "alarm_days";
        public static final String ALARM_DAY = "alarm_day";
        public static final String DDAY_ALARMYN = "dday_alarmyn";
        public static final String DDAY_ALARMDAY = "dday_alarmday";
        public static final String DDAY_ALARMSIGN = "dday_alarmsign";
        public static final String DDAY_DISPLAYYN = "dday_displayyn";
        public static final String GID = "gid";
        public static final String ANNIVERSARY = "anniversary";
        public static final String LUNARYN = "lunaryn";
        public static final String SCHEDULE_LDATE = "schedule_ldate";
        public static final String ALARM_DETAILINFO = "alarm_detailinfo";
        public static final String DDAY_DETAILINFO = "dday_detailinfo";
        public static final String SCHEDULE_TYPE = "schedule_type";
        public static final String BIBLE_BOOK = "bible_book";
        public static final String BIBLE_CHAPTER = "bible_chapter";
        public static final String ETAG = "etag";
        public static final String PUBLISHED = "published";
        public static final String UPDATED = "updated";
        public static final String WHEN = "gwhen";
        public static final String WHO = "who";
        public static final String RECURRENCE = "recurrence";
        public static final String SELFURL = "selfurl";
        public static final String EDITURL = "editurl";
        public static final String ORIGINALEVENT = "originalevent";
        public static final String EVENTSTATUS = "eventstatus";

        public static final String SCHEDULE_MMDD = "schedule_mmdd";
        public static final String SCHEDULE_KIND = "kind";

        public static final int COL_ID = 0;
        public static final int COL_SCHEDULE_DATE = 1;
        public static final int COL_SCHEDULE_TITLE = 2;
        public static final int COL_SCHEDULE_CONTENTS = 3;
        public static final int COL_SCHEDULE_REPEAT = 4;
        public static final int COL_SCHEDULE_CHECK = 5;
        public static final int COL_ALARM_LUNASOLAR = 6;
        public static final int COL_ALARM_DATE = 7;
        public static final int COL_ALARM_TIME = 8;
        public static final int COL_ALARM_DAYS = 9;
        public static final int COL_ALARM_DAY = 10;
        public static final int COL_DDAY_ALARMYN = 11;
        public static final int COL_DDAY_ALARMDAY = 12;
        public static final int COL_DDAY_ALARMSIGN = 13;
        public static final int COL_DDAY_DISPLAYYN = 14;
        public static final int COL_GID = 15;
        public static final int COL_ANNIVERSARY = 16;
        public static final int COL_LUNARYN = 17;
        public static final int COL_SCHEDULE_LDATE = 18;
        public static final int COL_ALARM_DETAILINFO = 19;
        public static final int COL_DDAY_DETAILINFO = 20;
        public static final int COL_SCHEDULE_TYPE = 21;
        public static final int COL_BIBLE_BOOK = 22;
        public static final int COL_BIBLE_CHAPTER = 23;
        public static final int COL_ETAG = 24;
        public static final int COL_PUBLISHED = 25;
        public static final int COL_UPDATED = 26;
        public static final int COL_WHEN = 27;
        public static final int COL_WHO = 28;
        public static final int COL_RECURRENCE = 29;
        public static final int COL_SELFURL = 30;
        public static final int COL_EDITURL = 31;
        public static final int COL_ORIGINALEVENT = 32;
        public static final int COL_EVENTSTATUS = 33;

    }

}
