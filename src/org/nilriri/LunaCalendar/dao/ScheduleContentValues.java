package org.nilriri.LunaCalendar.dao;

import org.nilriri.LunaCalendar.dao.Constants.Schedule;

import android.content.ContentValues;

public class ScheduleContentValues {

    public ContentValues value = new ContentValues();

    public ScheduleContentValues(ScheduleBean scheduleBean) {

        value.put(Schedule.SCHEDULE_DATE, scheduleBean.getSchedule_date());
        value.put(Schedule.SCHEDULE_LDATE, scheduleBean.getSchedule_ldate());
        value.put(Schedule.LUNARYN, scheduleBean.getLunaryn() == true ? "Y" : "N");
        value.put(Schedule.ANNIVERSARY, scheduleBean.getAnniversary() == true ? "Y" : "N");
        value.put(Schedule.SCHEDULE_TITLE, scheduleBean.getSchedule_title());
        value.put(Schedule.SCHEDULE_CONTENTS, scheduleBean.getSchedule_contents());
        value.put(Schedule.SCHEDULE_REPEAT, scheduleBean.getSchedule_repeat());
        value.put(Schedule.ALARM_LUNASOLAR, scheduleBean.getAlarm_lunasolar());
        value.put(Schedule.SCHEDULE_CHECK, scheduleBean.getSchedule_check());

        switch (scheduleBean.getSchedule_repeat()) {
            case 5:
                // 매년주기 알람인경우 년도를 빼고 월과 일만 저장한다.
                String alarmdate = scheduleBean.getAlarm_date();
                if (alarmdate.length() > 5)
                    alarmdate = alarmdate.substring(5);
                value.put(Schedule.ALARM_DATE, alarmdate);
                break;
            default:
                value.put(Schedule.ALARM_DATE, scheduleBean.getAlarm_date());
        }

        value.put(Schedule.ALARM_TIME, scheduleBean.getAlarm_time());
        value.put(Schedule.ALARM_DAYOFWEEK, scheduleBean.getAlarm_days());
        value.put(Schedule.ALARM_DAY, scheduleBean.getAlarm_day());
        value.put(Schedule.DDAY_ALARMYN, scheduleBean.getDday_alarmyn());
        value.put(Schedule.DDAY_ALARMDAY, scheduleBean.getDday_alarmday());
        value.put(Schedule.DDAY_ALARMSIGN, scheduleBean.getDday_alarmsign());
        value.put(Schedule.DDAY_DISPLAYYN, scheduleBean.getDday_displayyn());
        value.put(Schedule.GID, scheduleBean.getGID());
        value.put(Schedule.ETAG, scheduleBean.getEtag());
        value.put(Schedule.PUBLISHED, scheduleBean.getPublished());
        value.put(Schedule.UPDATED, scheduleBean.getUpdated());
        value.put(Schedule.WHEN, scheduleBean.getWhen());
        value.put(Schedule.WHO, scheduleBean.getWho());
        value.put(Schedule.RECURRENCE, scheduleBean.getRecurrence());
        value.put(Schedule.SELFURL, scheduleBean.getSelfurl());
        value.put(Schedule.EDITURL, scheduleBean.getEditurl());
        value.put(Schedule.ORIGINALEVENT, scheduleBean.getOriginalevent());
        value.put(Schedule.EVENTSTATUS, scheduleBean.getEventstatus());
    }
}
