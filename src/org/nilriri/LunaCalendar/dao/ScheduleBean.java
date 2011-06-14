package org.nilriri.LunaCalendar.dao;

import java.util.Calendar;
import java.util.StringTokenizer;

import org.nilriri.LunaCalendar.dao.Constants.Schedule;
import org.nilriri.LunaCalendar.gcal.EventEntry;
import org.nilriri.LunaCalendar.gcal.When;
import org.nilriri.LunaCalendar.tools.Common;

import android.database.Cursor;
import android.text.Editable;
import android.util.Log;

import com.google.api.client.util.DateTime;

public class ScheduleBean {

    private Long _id;
    private String schedule_date;
    private String schedule_ldate;
    private String lunaryn;
    private String anniversary;
    private String schedule_title;
    private String schedule_contents;
    private int schedule_repeat;
    private String schedule_check;
    private int alram_lunasolar;
    private String alarm_date;
    private String alarm_time;
    private int alarm_days;
    private int alarm_day;
    private int dday_alarmyn; // 알림여부
    private int dday_alarmday; // +- 일수
    private String dday_alarmsign; // +- 이전 or 이후
    private int dday_displayyn; // 표시여부
    private String gid; // google calendar icaluid
    private String alarm_detailinfo;
    private String dday_detailinfo;
    private String schedule_type;
    private String bible_book;
    private String bible_chapter;
    private String etag; // google calendar etag
    private String published;
    private String updated;
    private String gwhen;
    private String who;
    private String recurrence;
    private String selfurl;
    private String editurl;
    private String originalevent;
    private String eventstatus;

    public ScheduleBean() {
    }

    public ScheduleBean(Cursor cursor, boolean isCurrentRow) {

        for (int col = 0; col < Constants.mColumns.length; col++) {

            int colIndex = cursor.getColumnIndex(Constants.mColumns[col]);

            switch (colIndex) {
                case Schedule.COL_ID:
                    setId(cursor.getLong(Schedule.COL_ID));
                    break;
                case Schedule.COL_SCHEDULE_DATE:
                    setDate(cursor.getString(Schedule.COL_SCHEDULE_DATE));
                    break;
                case Schedule.COL_SCHEDULE_LDATE:
                    setLDate(cursor.getString(Schedule.COL_SCHEDULE_LDATE));
                    break;
                case Schedule.COL_LUNARYN:
                    setLunarYN("Y".equals(cursor.getString(Schedule.COL_LUNARYN)));
                    break;
                case Schedule.COL_ANNIVERSARY:
                    setAnniversary("Y".equals(cursor.getString(Schedule.COL_ANNIVERSARY)));
                    break;
                case Schedule.COL_SCHEDULE_TITLE:
                    setTitle(cursor.getString(Schedule.COL_SCHEDULE_TITLE));
                    break;
                case Schedule.COL_SCHEDULE_CONTENTS:
                    setContents(cursor.getString(Schedule.COL_SCHEDULE_CONTENTS));
                    break;
                case Schedule.COL_SCHEDULE_REPEAT:
                    setRepeat(cursor.getInt(Schedule.COL_SCHEDULE_REPEAT));
                    break;
                case Schedule.COL_SCHEDULE_CHECK:
                    setScheduleCheck(cursor.getString(Schedule.COL_SCHEDULE_CHECK));
                    break;
                case Schedule.COL_ALARM_LUNASOLAR:
                    setLunaSolar(cursor.getInt(Schedule.COL_ALARM_LUNASOLAR));
                    break;
                case Schedule.COL_ALARM_DATE:
                    setAlarmDate(cursor.getString(Schedule.COL_ALARM_DATE));
                    break;
                case Schedule.COL_ALARM_TIME:
                    setAlarmTime(cursor.getString(Schedule.COL_ALARM_TIME));
                    break;
                case Schedule.COL_ALARM_DAYS:
                    setAlarmDays(cursor.getInt(Schedule.COL_ALARM_DAYS));
                    break;
                case Schedule.COL_ALARM_DAY:
                    setAlarmDay(cursor.getInt(Schedule.COL_ALARM_DAY));
                    break;
                case Schedule.COL_DDAY_ALARMYN:
                    setDday_alarmyn(cursor.getInt(Schedule.COL_DDAY_ALARMYN));
                    break;
                case Schedule.COL_DDAY_ALARMDAY:
                    setDday_alarmday(cursor.getInt(Schedule.COL_DDAY_ALARMDAY));
                    break;
                case Schedule.COL_DDAY_ALARMSIGN:
                    setDday_alarmsign(cursor.getString(Schedule.COL_DDAY_ALARMSIGN));
                    break;
                case Schedule.COL_DDAY_DISPLAYYN:
                    setDday_displayyn(cursor.getInt(Schedule.COL_DDAY_DISPLAYYN));
                    break;
                case Schedule.COL_GID:
                    setGID(cursor.getString(Schedule.COL_GID));
                    break;
                case Schedule.COL_ALARM_DETAILINFO:
                    setAlarm_detailinfo(cursor.getString(Schedule.COL_ALARM_DETAILINFO));
                    break;
                case Schedule.COL_DDAY_DETAILINFO:
                    setDday_detailinfo(cursor.getString(Schedule.COL_DDAY_DETAILINFO));
                    break;
                case Schedule.COL_SCHEDULE_TYPE:
                    setSchedule_type(cursor.getString(Schedule.COL_SCHEDULE_TYPE));
                    break;
                case Schedule.COL_BIBLE_BOOK:
                    setBible_book(cursor.getString(Schedule.COL_BIBLE_BOOK));
                    break;
                case Schedule.COL_BIBLE_CHAPTER:
                    setBible_chapter(cursor.getString(Schedule.COL_BIBLE_CHAPTER));
                    break;
                case Schedule.COL_ETAG:
                    setEtag(cursor.getString(Schedule.COL_ETAG));
                    break;
                case Schedule.COL_PUBLISHED:
                    setPublished(cursor.getString(Schedule.COL_PUBLISHED));
                    break;
                case Schedule.COL_UPDATED:
                    setUpdated(cursor.getString(Schedule.COL_UPDATED));
                    break;
                case Schedule.COL_WHEN:
                    setWhen(cursor.getString(Schedule.COL_WHEN));
                    break;
                case Schedule.COL_WHO:
                    setWho(cursor.getString(Schedule.COL_WHO));
                    break;
                case Schedule.COL_RECURRENCE:
                    setRecurrence(cursor.getString(Schedule.COL_RECURRENCE));
                    break;
                case Schedule.COL_SELFURL:
                    setSelfUrl(cursor.getString(Schedule.COL_SELFURL));
                    break;
                case Schedule.COL_EDITURL:
                    setEditurl(cursor.getString(Schedule.COL_EDITURL));
                    break;
                case Schedule.COL_ORIGINALEVENT:
                    setOriginalevent(cursor.getString(Schedule.COL_ORIGINALEVENT));
                    break;
                case Schedule.COL_EVENTSTATUS:
                    setEventstatus(cursor.getString(Schedule.COL_EVENTSTATUS));
                    break;
            }
        }

    }

    public ScheduleBean(Cursor cursor) {
        if (cursor.moveToNext()) {

            for (int col = 0; col < Constants.mColumns.length; col++) {

                int colIndex = cursor.getColumnIndex(Constants.mColumns[col]);

                switch (colIndex) {
                    case Schedule.COL_ID:
                        setId(cursor.getLong(Schedule.COL_ID));
                        break;
                    case Schedule.COL_SCHEDULE_DATE:
                        setDate(cursor.getString(Schedule.COL_SCHEDULE_DATE));
                        break;
                    case Schedule.COL_SCHEDULE_LDATE:
                        setLDate(cursor.getString(Schedule.COL_SCHEDULE_LDATE));
                        break;
                    case Schedule.COL_LUNARYN:
                        setLunarYN("Y".equals(cursor.getString(Schedule.COL_LUNARYN)));
                        break;
                    case Schedule.COL_ANNIVERSARY:
                        setAnniversary("Y".equals(cursor.getString(Schedule.COL_ANNIVERSARY)));
                        break;
                    case Schedule.COL_SCHEDULE_TITLE:
                        setTitle(cursor.getString(Schedule.COL_SCHEDULE_TITLE));
                        break;
                    case Schedule.COL_SCHEDULE_CONTENTS:
                        setContents(cursor.getString(Schedule.COL_SCHEDULE_CONTENTS));
                        break;
                    case Schedule.COL_SCHEDULE_REPEAT:
                        setRepeat(cursor.getInt(Schedule.COL_SCHEDULE_REPEAT));
                        break;
                    case Schedule.COL_SCHEDULE_CHECK:
                        setScheduleCheck(cursor.getString(Schedule.COL_SCHEDULE_CHECK));
                        break;
                    case Schedule.COL_ALARM_LUNASOLAR:
                        setLunaSolar(cursor.getInt(Schedule.COL_ALARM_LUNASOLAR));
                        break;
                    case Schedule.COL_ALARM_DATE:
                        setAlarmDate(cursor.getString(Schedule.COL_ALARM_DATE));
                        break;
                    case Schedule.COL_ALARM_TIME:
                        setAlarmTime(cursor.getString(Schedule.COL_ALARM_TIME));
                        break;
                    case Schedule.COL_ALARM_DAYS:
                        setAlarmDays(cursor.getInt(Schedule.COL_ALARM_DAYS));
                        break;
                    case Schedule.COL_ALARM_DAY:
                        setAlarmDay(cursor.getInt(Schedule.COL_ALARM_DAY));
                        break;
                    case Schedule.COL_DDAY_ALARMYN:
                        setDday_alarmyn(cursor.getInt(Schedule.COL_DDAY_ALARMYN));
                        break;
                    case Schedule.COL_DDAY_ALARMDAY:
                        setDday_alarmday(cursor.getInt(Schedule.COL_DDAY_ALARMDAY));
                        break;
                    case Schedule.COL_DDAY_ALARMSIGN:
                        setDday_alarmsign(cursor.getString(Schedule.COL_DDAY_ALARMSIGN));
                        break;
                    case Schedule.COL_DDAY_DISPLAYYN:
                        setDday_displayyn(cursor.getInt(Schedule.COL_DDAY_DISPLAYYN));
                        break;
                    case Schedule.COL_GID:
                        setGID(cursor.getString(Schedule.COL_GID));
                        break;
                    case Schedule.COL_ALARM_DETAILINFO:
                        setAlarm_detailinfo(cursor.getString(Schedule.COL_ALARM_DETAILINFO));
                        break;
                    case Schedule.COL_DDAY_DETAILINFO:
                        setDday_detailinfo(cursor.getString(Schedule.COL_DDAY_DETAILINFO));
                        break;
                    case Schedule.COL_SCHEDULE_TYPE:
                        setSchedule_type(cursor.getString(Schedule.COL_SCHEDULE_TYPE));
                        break;
                    case Schedule.COL_BIBLE_BOOK:
                        setBible_book(cursor.getString(Schedule.COL_BIBLE_BOOK));
                        break;
                    case Schedule.COL_BIBLE_CHAPTER:
                        setBible_chapter(cursor.getString(Schedule.COL_BIBLE_CHAPTER));
                        break;
                    case Schedule.COL_ETAG:
                        setEtag(cursor.getString(Schedule.COL_ETAG));
                        break;
                    case Schedule.COL_PUBLISHED:
                        setPublished(cursor.getString(Schedule.COL_PUBLISHED));
                        break;
                    case Schedule.COL_UPDATED:
                        setUpdated(cursor.getString(Schedule.COL_UPDATED));
                        break;
                    case Schedule.COL_WHEN:
                        setWhen(cursor.getString(Schedule.COL_WHEN));
                        break;
                    case Schedule.COL_WHO:
                        setWho(cursor.getString(Schedule.COL_WHO));
                        break;
                    case Schedule.COL_RECURRENCE:
                        setRecurrence(cursor.getString(Schedule.COL_RECURRENCE));
                        break;
                    case Schedule.COL_SELFURL:
                        setSelfUrl(cursor.getString(Schedule.COL_SELFURL));
                        break;
                    case Schedule.COL_EDITURL:
                        setEditurl(cursor.getString(Schedule.COL_EDITURL));
                        break;
                    case Schedule.COL_ORIGINALEVENT:
                        setOriginalevent(cursor.getString(Schedule.COL_ORIGINALEVENT));
                        break;
                    case Schedule.COL_EVENTSTATUS:
                        setEventstatus(cursor.getString(Schedule.COL_EVENTSTATUS));
                        break;
                }
            }

        } else {
            setId(null);
        }
        cursor.close();
    }

    public ScheduleBean(EventEntry event) {

        try {
            this.setTitle(event.title);
            this.setDate(event.getStartDate());
            this.setContents(event.content);
            this.setGID(event.uid.value);
            this.setEtag(event.etag);
            this.setPublished(event.published);
            this.setUpdated(event.updated);

            if (null != event.when && !"".equals(event.when.parseAsString())) {
                this.setWhen(event.when.parseAsString());
            }

            this.setWho(event.getWhos());

            if (null != event.recurrence && !"".equals(event.recurrence)) {
                this.setRecurrence(event.recurrence);
            }

            this.setSelfUrl(event.getSelfLink());
            this.setEditurl(event.getEditLink());
            if (event.originalEvent != null) {
                this.setOriginalevent(event.originalEvent.parseAsString());
            }
            if (event.eventStatus != null) {
                this.setEventstatus(event.eventStatus.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(Common.TAG, "event is " + event.toString());
        }
    }

    public String getSchedule_title() {
        return this.schedule_title == null ? "" : this.schedule_title;
    }

    public void setTitle(String title) {
        this.schedule_title = title;
    }

    public void setTitle(Editable title) {
        this.schedule_title = title.toString();
    }

    public Long getId() {
        return this._id == null ? 0 : this._id;
    }

    public void setId(Long id) {
        this._id = (id == null ? 0 : id);
    }

    public String getSchedule_contents() {
        return this.schedule_contents == null ? "" : this.schedule_contents;
    }

    public int getSchedule_repeat() {

        return this.schedule_repeat;
    }

    public String getSchedule_date() {
        return this.schedule_date == null ? "" : this.schedule_date;
    }

    public DateTime getWhenStartDate() {
        return Common.toDateTime(getSchedule_date());
    }

    public DateTime getWhenEndDate() {
        return Common.toDateTime(getSchedule_date(), 1);
    }

    public When getWhenObject() {

        // 반복일정 정보가 없으면...
        if ("".equals(this.getRecurrence())) {
            // 일정정보가 없으면...
            if ("".equals(getWhen())) {
                When when = new When();
                when.startTime = getWhenStartDate();
                when.endTime = getWhenEndDate();
                return when;
            } else {
                return new When(getWhen());
            }
        } else {
            //반복일정이 있을때는 <gd:when>태그는 사용하지 않는다..
            return null;//new When();

        }

    }

    public String getSchedule_ldate() {
        return this.schedule_ldate == null ? "" : this.schedule_ldate;
    }

    public boolean getLunaryn() {

        return "Y".equals(this.lunaryn);
    }

    public boolean getAnniversary() {

        return "Y".equals(this.anniversary);
    }

    public String getDisplayDate() {
        String ret = Common.fmtDate(this.getYear(), this.getMonth(), this.getDay());

        return ret;
    }

    public int getYear() {

        StringTokenizer token = new StringTokenizer(this.schedule_date, "-");
        int rssult = Integer.parseInt(token.nextToken());

        return rssult;

    }

    public int getLMonth() {
        StringTokenizer token = new StringTokenizer(this.schedule_ldate, "-");

        token.nextToken();

        int rssult = Integer.parseInt(token.nextToken());
        return rssult;
    }

    public int getMonth() {
        StringTokenizer token = new StringTokenizer(this.schedule_date, "-");

        token.nextToken();

        int rssult = Integer.parseInt(token.nextToken());
        return rssult;
    }

    public int getLDay() {
        StringTokenizer token = new StringTokenizer(this.schedule_ldate, "-");

        token.nextToken();
        token.nextToken();

        int rssult = Integer.parseInt(token.nextToken());
        return rssult;
    }

    public int getDay() {
        StringTokenizer token = new StringTokenizer(this.schedule_date, "-");

        token.nextToken();
        token.nextToken();

        int rssult = Integer.parseInt(token.nextToken());
        return rssult;
    }

    public void setDate(Editable date) {
        setDate(date.toString());

    }

    public void setDate(Calendar c) {

        String date = Common.fmtDate(c);

        setDate(date);

    }

    public void setLDate(String date) {

        this.schedule_ldate = date;
    }

    public void setLunarYN(boolean lunaryn) {

        this.lunaryn = (lunaryn == true ? "Y" : "N");
    }

    public void setAnniversary(boolean anniversary) {

        this.anniversary = (anniversary == true ? "Y" : "N");
    }

    public void setDate(String date) {
        this.schedule_date = date;

        if ("".equals(this.getRecurrence())) {
            // 날짜가 바뀌면 When정보도 갱신한다.
            When when = new When(getWhen());
            when.startTime = Common.toDateTime(date);
            when.endTime = Common.toDateTime(date, 1);
            setWhen(when.parseAsString());
        } else {
            //TODO: 반복일정일 경우 반복일정 정보를 갱신한다.
            // 미구현...
            //Log.d(Common.TAG, "OLD Recurrence=" + this.getRecurrence());

            this.setWhen(null);

            /*

            this.setRecurrence(null);            

            // 1회성 일정으로 강제 변경한다.
            When when = new When(getWhen());
            when.startTime = Common.toDateTime(date);
            when.endTime = Common.toDateTime(date, 1);
            setWhen(when.parseAsString());
            */
        }

    }

    public void setContents(Editable contents) {
        this.schedule_contents = contents.toString();

    }

    public void setContents(String contents) {
        this.schedule_contents = contents;

    }

    public void setRepeat(int repeat) {
        this.schedule_repeat = repeat;

        if (this.schedule_repeat == 0) {
            alram_lunasolar = 0;
            alarm_date = null;
            alarm_time = null;
            alarm_days = 0;
            alarm_day = 0;
        }

    }

    public void setScheduleCheck(String schedule_check) {
        this.schedule_check = schedule_check;

    }

    public String getSchedule_check() {
        return this.schedule_check == null ? "" : this.schedule_check;
    }

    public int getAlarm_lunasolar() {
        return this.alram_lunasolar;
    }

    public String getAlarm_date() {
        if (getSchedule_repeat() > 0) {
            return this.alarm_date == null ? Common.fmtDate() : this.alarm_date;
        } else {
            return this.alarm_date == null ? "" : this.alarm_date;
        }
    }

    public int getAlarmYear() {
        String src = this.alarm_date == null ? "" : this.alarm_date;

        StringTokenizer token = new StringTokenizer(src, "-");

        switch (src.length()) {
            case 10: {
                return Integer.parseInt(token.nextToken());
            }
            default:
                return initToday(Calendar.YEAR);
        }
    }

    public int getAlarmMonth() {
        String src = this.alarm_date == null ? "" : this.alarm_date;

        StringTokenizer token = new StringTokenizer(src, "-");

        switch (src.length()) {
            case 10: {
                token.nextToken();
                return Integer.parseInt(token.nextToken());
            }
            case 5: {
                return Integer.parseInt(token.nextToken());
            }
            default:

                return initToday(Calendar.MONTH) + 1;
        }
    }

    public int getAlarmDayofMonth() {
        String src = this.alarm_date == null ? "" : this.alarm_date;

        StringTokenizer token = new StringTokenizer(src, "-");

        switch (src.length()) {
            case 10: {
                token.nextToken();
                token.nextToken();
                return Integer.parseInt(token.nextToken());

            }
            case 5: {
                token.nextToken();
                return Integer.parseInt(token.nextToken());
            }
            default:

                return Integer.parseInt(fmtToday(Calendar.DAY_OF_MONTH));
        }

    }

    public String fmtToday(int field) {
        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.SUNDAY);

        int ret = c.get(field);

        //if (field == Calendar.MONTH)            ret += 1;
        if (field == Calendar.HOUR && ret == 0)
            ret += 12;

        return ret >= 10 ? ret + "" : "0" + ret;
    }

    public Calendar initToday() {
        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.SUNDAY);

        return c;

    }

    public int initToday(int field) {
        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.SUNDAY);

        int ret = c.get(field);

        //if (field == Calendar.MONTH)            ret += 1;

        return ret;

    }

    public String getDisplayAlarmDate() {
        String ret = "";
        if (getSchedule_repeat() == 5) { // 매년 주기면 월과 일자만.

            ret = getAlarmMonth() > 9 ? getAlarmMonth() + "" : "0" + getAlarmMonth();
            ret += "-" + (getAlarmDayofMonth() > 9 ? getAlarmDayofMonth() : "0" + getAlarmDayofMonth());
            if ("".equals(ret))
                ret = fmtToday(Calendar.MONTH) + "-" + fmtToday(Calendar.DAY_OF_MONTH);
        } else {
            ret = getAlarmYear() + "-";
            ret += getAlarmMonth() > 9 ? getAlarmMonth() : "0" + getAlarmMonth();
            ret += "-" + (getAlarmDayofMonth() > 9 ? getAlarmDayofMonth() : "0" + getAlarmDayofMonth());
            if ("".equals(ret)) {
                ret = fmtToday(Calendar.YEAR) + "-" + fmtToday(Calendar.MONTH) + "-" + fmtToday(Calendar.DAY_OF_MONTH);
            }
        }

        return ret;

    }

    public String getAlarm_time() {
        if (getSchedule_repeat() > 0) {
            return this.alarm_time == null ? Common.fmtTime() : this.alarm_time;
        } else {
            return this.alarm_time == null ? "" : this.alarm_time;
        }
    }

    public String getDisplayAlarmTime() {
        String ret = this.getAlarmAmPm() + ":";
        int h = this.getAlarmHour() == 0 ? 12 : this.getAlarmHour();
        ret += h >= 10 ? h + ":" : "0" + h + ":";
        ret += this.getAlarmMinute() >= 10 ? this.getAlarmMinute() : "0" + this.getAlarmMinute();

        return ret;
    }

    public int getAlarm_days() {
        return this.alarm_days == 0 ? this.initToday(Calendar.DAY_OF_WEEK) : this.alarm_days;
    }

    public int getAlarm_day() {
        return this.alarm_day > 0 ? this.alarm_day : initToday(Calendar.DAY_OF_MONTH);
    }

    public String getDisplayAlarmDay() {
        String ret = this.alarm_day >= 10 ? this.alarm_day + "" : "0" + this.alarm_day;
        return ret == null ? fmtToday(Calendar.DAY_OF_MONTH) : ret;
    }

    public void setLunaSolar(int arg) {
        this.alram_lunasolar = arg;
    }

    public void setAlarmDate(String arg) {
        if (arg == null || "".equals(arg)) {
            final Calendar c = Calendar.getInstance();
            if (getSchedule_repeat() == 5) { // 매년 주기면 월과 일자만.
                arg = Common.fmtDate(c).substring(0, 7);
            } else {
                arg = Common.fmtDate(c);
            }
        }
        this.alarm_date = arg;
    }

    public void setAlarmTime(String arg) {

        if (arg == null || "".equals(arg)) {
            final Calendar c = Calendar.getInstance();
            arg = Common.fmtTime(c);
        }
        this.alarm_time = arg;
    }

    public void setAlarmDays(int arg) {
        if (arg == 0) {
            final Calendar c = Calendar.getInstance();
            c.setFirstDayOfWeek(Calendar.SUNDAY);
            arg = c.get(Calendar.DAY_OF_WEEK);
        }
        this.alarm_days = arg;
    }

    public void setAlarmDay(int arg) {
        if (arg == 0) {
            final Calendar c = Calendar.getInstance();
            arg = c.get(Calendar.DAY_OF_MONTH);
        }
        this.alarm_day = arg;
    }

    public String getAlarmAmPm() {
        String src = this.alarm_time == null ? "" : this.alarm_time;

        StringTokenizer token = new StringTokenizer(src, ":");

        if (token.hasMoreTokens())
            return Integer.parseInt(token.nextToken()) >= 12 ? "PM" : "AM";
        else
            return initToday(Calendar.AM_PM) == 0 ? "AM" : "PM";

    }

    public int getAlarmHour24() {
        String src = this.alarm_time == null ? "" : this.alarm_time;

        StringTokenizer token = new StringTokenizer(src, ":");

        if (token.countTokens() == 2) {
            return Integer.parseInt(token.nextToken());
        } else {
            return initToday(Calendar.HOUR_OF_DAY);
        }

    }

    public int getAlarmHour() {
        String src = this.alarm_time == null ? "" : this.alarm_time;

        StringTokenizer token = new StringTokenizer(src, ":");

        if (token.countTokens() == 2) {

            int ret = Integer.parseInt(token.nextToken());

            return ret > 12 ? ret - 12 : ret;
        } else {
            return initToday(Calendar.HOUR);
        }

    }

    public int getAlarmMinute() {
        String src = this.alarm_time == null ? "" : this.alarm_time;

        StringTokenizer token = new StringTokenizer(src, ":");
        if (token.countTokens() == 2) {
            token.nextToken(); //hour

            return Integer.parseInt(token.nextToken());
        } else {
            return initToday(Calendar.MINUTE);
        }

    }

    /**
     * @param dday_alarmyn the dday_alarmyn to set
     */
    public void setDday_alarmyn(int dday_alarmyn) {
        this.dday_alarmyn = dday_alarmyn;

        if (0 == this.dday_alarmyn) {
            dday_alarmday = 0;
            dday_alarmsign = null;
            dday_displayyn = 0;
        }
    }

    /**
     * @return the dday_alarmyn
     */
    public int getDday_alarmyn() {
        return dday_alarmyn;
    }

    /**
     * @param dday_alarmday the dday_alarmday to set
     */
    public void setDday_alarmday(int dday_alarmday) {
        this.dday_alarmday = dday_alarmday;
    }

    public void setDday_alarmday(String dday_alarmday) {
        if ("".equals(dday_alarmday))
            dday_alarmday = "0";
        this.dday_alarmday = Integer.parseInt(dday_alarmday.trim());
    }

    /**
     * @return the dday_alarmday
     */
    public int getDday_alarmday() {
        return dday_alarmday;
    }

    public String getDisplayDday_alarmday() {
        return dday_alarmday + "";
    }

    /**
     * @param dday_alarmsign the dday_alarmsign to set
     */
    public void setDday_alarmsign(int dday_alarmsign) {
        this.dday_alarmsign = dday_alarmsign == 0 ? "-" : "+";
    }

    public void setDday_alarmsign(String dday_alarmsign) {
        this.dday_alarmsign = dday_alarmsign;
    }

    /**
     * @return the dday_alarmsign
     */
    public String getDday_alarmsign() {
        return dday_alarmsign;
    }

    public int getDisplayDday_alarmsign() {
        return "-".equals(dday_alarmsign) ? 0 : 1;
    }

    /**
     * @param dday_displayyn the dday_displayyn to set
     */
    public void setDday_displayyn(int dday_displayyn) {
        this.dday_displayyn = dday_displayyn;
    }

    /**
     * @return the dday_displayyn
     */
    public int getDday_displayyn() {
        return this.dday_displayyn;
    }

    public void setGID(String arg) {
        this.gid = (arg == null ? "" : arg);
    }

    public String getGID() {
        return this.gid == null ? "" : this.gid;
    }

    public void setEtag(String arg) {
        this.etag = (arg == null ? "" : arg);
    }

    public String getEtag() {
        return this.etag == null ? "" : this.etag;
    }

    public void setPublished(String arg) {
        this.published = (arg == null ? Common.getTime3339Format() : Common.formatTime3339(arg));
    }

    public String getPublished() {
        return this.published == null ? Common.getTime3339Format() : this.published;
    }

    public void setUpdated(String arg) {

        this.updated = (arg == null ? Common.getTime3339Format() : Common.formatTime3339(arg));
    }

    public String getUpdated() {
        return this.updated == null ? Common.getTime3339Format() : this.updated;
    }

    /**
     * @param alarm_detailinfo the alarm_detailinfo to set
     */
    public void setAlarm_detailinfo(String alarm_detailinfo) {
        this.alarm_detailinfo = alarm_detailinfo;
    }

    /**
     * @return the alarm_detailinfo
     */
    public String getAlarm_detailinfo() {
        return (alarm_detailinfo == null ? "" : alarm_detailinfo);
    }

    /**
     * @param dday_detailinfo the dday_detailinfo to set
     */
    public void setDday_detailinfo(String dday_detailinfo) {
        this.dday_detailinfo = dday_detailinfo;
    }

    /**
     * @return the dday_detailinfo
     */
    public String getDday_detailinfo() {
        return (dday_detailinfo == null ? "" : dday_detailinfo);
    }

    /**
     * @param bible_book the bible_book to set
     */
    public void setBible_book(String bible_book) {
        this.bible_book = bible_book;
    }

    /**
     * @return the bible_book
     */
    public String getBible_book() {
        return (bible_book == null ? "" : bible_book);
    }

    /**
     * @param schedule_type the schedule_type to set
     */
    public void setSchedule_type(String schedule_type) {
        this.schedule_type = schedule_type;
    }

    /**
     * @return the schedule_type
     */
    public String getSchedule_type() {
        return (schedule_type == null ? "" : schedule_type);
    }

    /**
     * @param bible_chapter the bible_chapter to set
     */
    public void setBible_chapter(String bible_chapter) {
        this.bible_chapter = bible_chapter;
    }

    /**
     * @return the bible_chapter
     */
    public String getBible_chapter() {
        return (bible_chapter == null ? "" : bible_chapter);
    }

    /**
     * @param gwhen the gwhen to set
     */
    public void setWhen(String when) {
        this.gwhen = (when == null ? "" : when);
    }

    /**
     * @return the gwhen
     */
    public String getWhen() {
        return (gwhen == null ? "" : gwhen);
    }

    /**
     * @param who the who to set
     */
    public void setWho(String who) {
        this.who = who;
    }

    /**
     * @return the who
     */
    public String getWho() {
        return who;
    }

    /**
     * @param recurrence the recurrence to set
     */
    public void setRecurrence(String recurrence) {
        this.recurrence = (recurrence == null ? "" : recurrence);
    }

    /**
     * @return the recurrence
     */
    public String getRecurrence() {
        return (recurrence == null ? "" : recurrence);
    }

    /**
     * @param selfurl the selfurl to set
     */
    public void setSelfUrl(String url) {
        this.selfurl = url;
    }

    /**
     * @return the selfurl
     */
    public String getSelfurl() {
        return selfurl == null ? "" : selfurl;
    }

    /**
     * @param editurl the editurl to set
     */
    public void setEditurl(String editurl) {
        this.editurl = editurl;
    }

    /**
     * @return the editurl
     */
    public String getEditurl() {
        return editurl == null ? this.getSelfurl() : editurl;
    }

    /**
     * @param originalevent the originalevent to set
     */
    public void setOriginalevent(String originalevent) {
        this.originalevent = originalevent;
    }

    /**
     * @return the originalevent
     */
    public String getOriginalevent() {
        return originalevent;
    }

    /**
     * @param eventstatus the eventstatus to set
     */
    public void setEventstatus(String eventstatus) {
        this.eventstatus = eventstatus;
    }

    /**
     * @return the eventstatus
     */
    public String getEventstatus() {
        return eventstatus;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("ScheduleBean={");

        for (int col = 0; col < Constants.mColumns.length; col++) {
            result.append(Constants.mColumns[col] + "=");

            try {
                switch (col) {

                    case Schedule.COL_ID: //0
                        result.append(this.getId()).append(", ");
                        break;
                    case Schedule.COL_SCHEDULE_DATE: //1
                        result.append(this.getDisplayDate()).append(", ");
                        break;
                    case Schedule.COL_SCHEDULE_LDATE://2
                        result.append(this.getSchedule_ldate()).append(", ");
                        break;
                    case Schedule.COL_LUNARYN://3
                        result.append(this.getLunaryn()).append(", ");
                        break;
                    case Schedule.COL_ANNIVERSARY://4
                        result.append(this.getAnniversary()).append(", ");
                        break;
                    case Schedule.COL_SCHEDULE_TITLE://5
                        result.append(this.getSchedule_title()).append(", ");
                        break;
                    case Schedule.COL_SCHEDULE_CONTENTS://6
                        result.append(this.getSchedule_contents()).append(", ");
                        break;
                    case Schedule.COL_SCHEDULE_REPEAT://7
                        result.append(this.getSchedule_repeat()).append(", ");
                        break;
                    case Schedule.COL_SCHEDULE_CHECK://8
                        result.append(this.getSchedule_check()).append(", ");
                        break;
                    case Schedule.COL_ALARM_LUNASOLAR://9
                        result.append(this.getAlarm_lunasolar()).append(", ");
                        break;
                    case Schedule.COL_ALARM_DATE://10
                        result.append(this.getAlarm_date()).append(", ");
                        break;
                    case Schedule.COL_ALARM_TIME://11
                        result.append(this.getAlarm_time()).append(", ");
                        break;
                    case Schedule.COL_ALARM_DAYS://12
                        result.append(this.getAlarm_days()).append(", ");
                        break;
                    case Schedule.COL_ALARM_DAY://13
                        result.append(this.getAlarm_day()).append(", ");
                        break;
                    case Schedule.COL_DDAY_ALARMYN://14
                        result.append(this.getDday_alarmyn()).append(", ");
                        break;
                    case Schedule.COL_DDAY_ALARMDAY://15
                        result.append(this.getDday_alarmday()).append(", ");
                        break;
                    case Schedule.COL_DDAY_ALARMSIGN://16
                        result.append(this.getDday_alarmsign()).append(", ");
                        break;
                    case Schedule.COL_DDAY_DISPLAYYN://17
                        result.append(this.getDday_displayyn()).append(", ");
                        break;
                    case Schedule.COL_GID://18
                        result.append(this.getGID()).append(", ");
                        break;
                    case Schedule.COL_ALARM_DETAILINFO://19
                        result.append(this.getAlarm_detailinfo()).append(", ");
                        break;
                    case Schedule.COL_DDAY_DETAILINFO://20
                        result.append(this.getDday_detailinfo()).append(", ");
                        break;
                    case Schedule.COL_SCHEDULE_TYPE://21
                        result.append(this.getSchedule_type()).append(", ");
                        break;
                    case Schedule.COL_BIBLE_BOOK://22
                        result.append(this.getBible_book()).append(", ");
                        break;
                    case Schedule.COL_BIBLE_CHAPTER://23
                        result.append(this.getBible_chapter()).append(", ");
                        break;
                    case Schedule.COL_ETAG://24
                        result.append(this.getEtag()).append(", ");
                        break;
                    case Schedule.COL_PUBLISHED://25
                        result.append(this.getPublished()).append(", ");
                        break;
                    case Schedule.COL_UPDATED://26
                        result.append(this.getUpdated()).append(", ");
                        break;
                    case Schedule.COL_WHEN://27
                        result.append(this.getWhen()).append(", ");
                        break;
                    case Schedule.COL_WHO://28
                        result.append(this.getWho()).append(", ");
                        break;
                    case Schedule.COL_RECURRENCE://29
                        result.append(this.getRecurrence()).append(", ");
                        break;
                    case Schedule.COL_SELFURL://30
                        result.append(this.getSelfurl()).append(", ");
                        break;
                    case Schedule.COL_EDITURL://31
                        result.append(this.getEditurl()).append(", ");
                        break;
                    case Schedule.COL_ORIGINALEVENT://32
                        result.append(this.getOriginalevent()).append(", ");
                        break;
                    case Schedule.COL_EVENTSTATUS://33
                        result.append(this.getEventstatus()).append(", ");
                        break;
                    default:
                        result.append("*null*, ");
                }

            } catch (Exception e) {
                result.append("***null***, ");
            }
        }

        result.append("}");
        return result.toString();

    }

}
