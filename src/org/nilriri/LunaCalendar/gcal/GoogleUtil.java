package org.nilriri.LunaCalendar.gcal;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.nilriri.LunaCalendar.dao.ScheduleBean;
import org.nilriri.LunaCalendar.tools.Common;
import org.nilriri.LunaCalendar.tools.Lunar2Solar;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.Build;
import android.util.Log;

import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.xml.atom.AtomParser;
import com.google.common.collect.Lists;

public class GoogleUtil {

    private final List<CalendarEntry> calendars = Lists.newArrayList();
    private final List<EventEntry> events = Lists.newArrayList();
    private static HttpTransport transport;

    public GoogleUtil(String authToken) {

        if (Build.VERSION.SDK_INT <= Common.FROYO) {
            transport = new ApacheHttpTransport();
        } else {
            transport = new NetHttpTransport();
        }

        GoogleHeaders headers = new GoogleHeaders();
        headers.setApplicationName("org.nilriri.LunaCalendar");
        headers.gdataVersion = "2";
        transport.defaultHeaders = headers;

        AtomParser parser = new AtomParser();
        parser.namespaceDictionary = Util.DICTIONARY;
        transport.addParser(parser);

        GoogleHeaders header = ((GoogleHeaders) transport.defaultHeaders);
        header.setGoogleLogin(authToken);

        RedirectHandler.resetSessionId(transport);
    }

    /*
     * Feed Retrieve
     */
    public List<CalendarEntry> getCalendarList() throws IOException {
        List<CalendarEntry> calendars = this.calendars;
        calendars.clear();
        try {
            CalendarUrl url = CalendarUrl.forAllCalendarsFeed();
            while (true) {
                CalendarFeed feed = CalendarFeed.executeGet(transport, url);
                if (feed.calendars != null) {
                    calendars.addAll(feed.calendars);
                }
                String nextLink = feed.getNextLink();
                if (nextLink == null) {
                    break;
                }
            }
        } catch (IOException e) {
            calendars.clear();
        }
        return calendars;
    }

    public List<EventEntry> getEvents(String feedUrl) throws IOException {
        List<EventEntry> events = this.events;
        events.clear();
        try {
            CalendarUrl url = new CalendarUrl(feedUrl);
            while (true) {
                EventFeed feed = EventFeed.executeGet(transport, url);
                if (feed.events != null) {
                    events.addAll(feed.events);
                }
                String nexturl = feed.getNextLink();
                if (nexturl == null) {
                    break;
                } else {
                    url = new CalendarUrl(nexturl);
                }
            }
        } catch (Exception e) {
            EventEntry event = new EventEntry();
            event.id = "0";
            event.title = "Sync fail...\n"+e.getMessage();
            event.when.startTime = Common.toDateTime(Common.fmtDate());
            event.content = e.getMessage();
            events.clear();
            
            events.add(event);
        }
        return events;
    }

    /*
     * Events Manage Transaction
     */
    public CalendarEntry addCalendar(String title) throws IOException {
        CalendarUrl url = CalendarUrl.forOwnCalendarsFeed();
        CalendarEntry calendar = new CalendarEntry();
        calendar.title = title;
        return calendar.executeInsert(transport, url);
    }

    public EventEntry insertEvent(String feedUrl, ScheduleBean scheduleBean, String account) throws IOException {
        CalendarUrl url = new CalendarUrl(feedUrl);
        EventEntry event = newEvent(scheduleBean);
        url = CalendarUrl.forUpdateEventFeed(account, event.uid.getValue().replace("@google.com", ""));
        return event.executeInsert(transport, url);
    }

    public EventEntry updateEvent(ScheduleBean bean) throws IOException {
        EventEntry event = newEvent(bean);
        return event.executeUpdate(transport);
    }

    public int deleteEvent(EventEntry event) throws IOException {
        return event.executeDelete(transport);
    }

    public int deleteEvent(ScheduleBean bean) throws IOException {
        EventEntry event = newEvent(bean);
        return event.executeDelete(transport);
    }

    private EventEntry newEvent(ScheduleBean scheduleBean) {
        EventEntry event = new EventEntry();

        event.links.add(new Link("edit", scheduleBean.getEditurl()));
        event.links.add(new Link("self", scheduleBean.getSelfurl()));
        if ("".equals(event.getEditLink())) {
            event.id = null;
        } else {
            event.id = new GenericUrl(event.getEditLink()).build();
        }
        event.summary = null;
        event.title = scheduleBean.getSchedule_title();
        event.updated = scheduleBean.getUpdated();
        event.published = scheduleBean.getPublished();
        //event.reminder = null;
        event.reminder.add(new Reminder());

        if ("".equals(scheduleBean.getRecurrence())) {
            event.recurrence = null;
            event.when = scheduleBean.getWhenObject();
        } else {
            event.recurrence = scheduleBean.getRecurrence();
            event.when = null;
        }

        event.setWhos(scheduleBean.getWho());
        event.content = scheduleBean.getSchedule_contents();
        event.uid = new AtomValue(scheduleBean.getGID());
        event.etag = scheduleBean.getEtag();
        event.eventStatus = null;//new AtomValue(scheduleBean.getEventstatus());
        event.originalEvent = null;//new OriginalEvent(scheduleBean.getOriginalevent());

        return event;
    }

    /*
     * batch Transaction
     */
    public void batchLunarEvents(String eventFeedLink, ProgressDialog pd) throws IOException {

        EventFeed feed = new EventFeed();
        Calendar e = Calendar.getInstance();
        e.set(Calendar.MONDAY, 11);
        e.set(Calendar.DAY_OF_MONTH, 31);

        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.SUNDAY);
        c.set(Calendar.MONDAY, 0);
        c.set(Calendar.DAY_OF_MONTH, 1);

        int oldMonth = 0;
        int interval = 1;

        for (int day = 1; c.getTime().compareTo(e.getTime()) < 0; day++) {

            pd.setProgress(c.get(Calendar.DAY_OF_YEAR));

            ScheduleBean bean = new ScheduleBean();

            c.add(Calendar.DAY_OF_MONTH, interval);

            if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                interval = 7;
            } else {
                continue;
            }

            String ldate = Common.fmtDate(Lunar2Solar.s2l(c)).substring(5).replace("-", "/");

            bean.setDate(c);
            bean.setTitle(ldate + " (-)");
            bean.setContents("gen:org.nilriri.lunarcalendar.auto;");

            EventEntry event = newEvent(bean);

            event.batchId = Integer.toString(day);
            event.batchOperation = BatchOperation.INSERT;
            feed.events.add(event);

            if (oldMonth != c.get(Calendar.MONTH)) {
                EventFeed batchResult = feed.executeBatch(transport, eventFeedLink);

                for (EventEntry newEvent : batchResult.events) {
                    BatchStatus batchStatus = newEvent.batchStatus;
                    if (batchStatus != null && !HttpResponse.isSuccessStatusCode(batchStatus.code)) {
                        Log.d(Common.TAG, "Error posting event: " + batchStatus.reason);
                    }
                }
                feed.events.clear();
                oldMonth = c.get(Calendar.MONTH);
            }
        }
    }

    public void batchLocalEvents(Cursor cursor, String eventFeedLink, ProgressDialog pd) throws IOException {

        EventFeed feed = new EventFeed();
        Calendar e = Calendar.getInstance();
        e.set(Calendar.MONDAY, 11);
        e.set(Calendar.DAY_OF_MONTH, 31);

        pd.setMax(cursor.getCount());

        while (cursor.moveToNext()) {

            pd.setProgress(cursor.getPosition());

            ScheduleBean bean = new ScheduleBean(cursor, true);

            EventEntry event = newEvent(bean);

            event.batchId = bean.getId().toString();
            event.batchOperation = BatchOperation.INSERT;
            feed.events.add(event);

        }

        EventFeed batchResult = feed.executeBatch(transport, eventFeedLink);

        for (EventEntry newEvent : batchResult.events) {
            BatchStatus batchStatus = newEvent.batchStatus;
            if (batchStatus != null && !HttpResponse.isSuccessStatusCode(batchStatus.code)) {
                Log.d(Common.TAG, "Error posting event: " + batchStatus.reason);
            }
        }

    }

    // 성경플랜달력 생성.
    public void batchBiblePlan(String eventFeedLink, ProgressDialog pd, String[] PlanList, String gb) throws IOException {

        EventFeed feed = new EventFeed();
        StringBuilder recurrence;

        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.SUNDAY);
        c.set(Calendar.MONDAY, 0);
        c.set(Calendar.DAY_OF_MONTH, 1);

        int oldMonth = 0;

        for (int j = 0; j < PlanList.length; j++) {

            pd.setProgress(c.get(Calendar.DAY_OF_YEAR));

            //가정:창세기 1장|01-01|0|0|
            String data[] = Common.tokenFn(PlanList[j], "|");

            if ("2".equals(gb)) {
                // 개인용 맥체인성경읽기일정. 생성.
                if (data[0].indexOf("개인:") < 0) {
                    continue;
                }
            } else {
                // 가정용 맥체인성경읽기일정. 생성.
                if (data[0].indexOf("가정:") < 0) {
                    continue;
                }
            }

            String mmdd[] = Common.tokenFn(data[1], "-");

            c.set(Calendar.MONTH, Integer.parseInt(mmdd[0]) - 1);
            c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(mmdd[1]));

            Calendar c2 = (Calendar) c.clone();
            c2.add(Calendar.DAY_OF_MONTH, 1);

            ScheduleBean bean = new ScheduleBean();

            bean.setDate(c);
            bean.setTitle(data[0]);//가정:창세기 1장
            bean.setContents("bindex:" + data[2] + "," + data[3]);

            recurrence = new StringBuilder();
            recurrence.append("DTSTART;VALUE=DATE:" + Common.fmtDate(c).replace("-", "") + "\n");
            recurrence.append("DTEND;VALUE=DATE:" + Common.fmtDate(c2).replace("-", "") + "\n");
            recurrence.append("RRULE:FREQ=YEARLY\n");
            recurrence.append("BEGIN:VTIMEZONE\n");
            recurrence.append("TZID:Asia/Seoul\n");
            recurrence.append("X-LIC-LOCATION:Asia/Seoul\n");
            recurrence.append("BEGIN:STANDARD\n");
            recurrence.append("TZOFFSETFROM:+0900\n");
            recurrence.append("TZOFFSETTO:+0900\n");
            recurrence.append("TZNAME:KST\n");
            recurrence.append("DTSTART:" + Common.fmtDate(c).replace("-", "") + "T000000\n");
            recurrence.append("END:STANDARD\n");
            recurrence.append("END:VTIMEZONE\n");

            EventEntry event = newEvent(bean);

            event.batchId = Integer.toString(j);
            event.batchOperation = BatchOperation.INSERT;
            event.when = null;
            event.recurrence = recurrence.toString();

            feed.events.add(event);

            if (oldMonth != c.get(Calendar.MONTH) || 11 == c.get(Calendar.MONTH)) {
                try {
                    EventFeed batchResult = feed.executeBatch(transport, eventFeedLink);

                    int s = 1;
                    for (EventEntry newEvent : batchResult.events) {
                        BatchStatus batchStatus = newEvent.batchStatus;
                        pd.setSecondaryProgress(s++);

                        if (batchStatus != null && !HttpResponse.isSuccessStatusCode(batchStatus.code)) {
                            Log.d(Common.TAG, "Error posting event: " + batchStatus.reason);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    feed.events.clear();
                    oldMonth = c.get(Calendar.MONTH);
                }
            }
        }
    }

    public void batchDelete(String feedUrl) throws IOException {
        try {
            EventFeed feed = null;

            CalendarUrl url = new CalendarUrl(feedUrl);
            while (true) {
                Log.d(Common.TAG, "Delete url: " + url.toString());
                feed = EventFeed.executeGet(transport, url);
                if (feed.events != null) {

                    for (int i = 0; i < feed.events.size(); i++) {
                        feed.events.get(i).batchId = feed.events.get(i).etag;
                        feed.events.get(i).batchOperation = BatchOperation.DELETE;
                    }

                    EventFeed batchResult = feed.executeBatch(transport, feedUrl);

                    for (EventEntry newEvent : batchResult.events) {
                        BatchStatus batchStatus = newEvent.batchStatus;

                        if (batchStatus != null && !HttpResponse.isSuccessStatusCode(batchStatus.code)) {
                            Log.d(Common.TAG, "Error posting event: " + batchStatus.reason);
                        } else {
                            Log.d(Common.TAG, "Delete Result: " + batchStatus.reason);
                        }
                    }
                }
                String nexturl = feed.getNextLink();
                Log.d(Common.TAG, "Delete getNextLink: " + nexturl);
                if (nexturl == null) {
                    break;
                } else {
                    url = new CalendarUrl(nexturl);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
