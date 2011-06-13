package org.nilriri.LunaCalendar.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.nilriri.LunaCalendar.R;
import org.nilriri.LunaCalendar.RefreshManager;
import org.nilriri.LunaCalendar.dao.Constants.Schedule;
import org.nilriri.LunaCalendar.gcal.CalendarEntry;
import org.nilriri.LunaCalendar.gcal.EventEntry;
import org.nilriri.LunaCalendar.gcal.GoogleUtil;
import org.nilriri.LunaCalendar.tools.Common;
import org.nilriri.LunaCalendar.tools.ContactEvent;
import org.nilriri.LunaCalendar.tools.Lunar2Solar;
import org.nilriri.LunaCalendar.tools.Prefs;
import org.nilriri.LunaCalendar.tools.WhereClause;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

public class ScheduleDaoImpl extends AbstractDao {

    private SQLiteDatabase db;
    private Context mContext;
    private ScheduleBean oldBean;
    public final boolean mSdcarduse;

    protected RefreshManager refreshManager;

    public ScheduleDaoImpl(Context context, CursorFactory factory, boolean sdcarduse) {
        super(context, factory, sdcarduse);

        mSdcarduse = sdcarduse;

        mContext = context;

        db = getWritableDatabase();
    }

    private void CallerRefresh() {
        try {
            if (refreshManager != null) {
                refreshManager.refresh();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Sync Task
     */
    public void syncInsert(Long id, RefreshManager caller) {
        this.refreshManager = caller;
        if ("auto".equals(Prefs.getSyncMethod(this.mContext)) // 동기화 방법 
                && !"".equals(Prefs.getSyncCalendar(mContext))) {
            oldBean = new ScheduleBean();
            oldBean.setId(id);

            new googleInsert().execute();
        }
    }

    public void syncInsert(ScheduleBean scheduleBean, RefreshManager caller) {
        this.refreshManager = caller;
        if ("auto".equals(Prefs.getSyncMethod(this.mContext)) // 동기화 방법 
                && !"".equals(Prefs.getSyncCalendar(mContext))) {
            oldBean = scheduleBean;

            new googleInsert().execute();
        } else {
            localInsert(scheduleBean);
            CallerRefresh();
        }

    }

    public void syncUpdate(ScheduleBean scheduleBean, RefreshManager caller) {
        this.refreshManager = caller;
        if ("auto".equals(Prefs.getSyncMethod(this.mContext)) // 동기화 방법 
                && !"".equals(Prefs.getSyncCalendar(mContext))) {
            oldBean = scheduleBean;

            new googleUpdate().execute();

        } else {
            localUpdate(scheduleBean);
            CallerRefresh();
        }

    }

    public void syncDelete(Long deleteId, RefreshManager caller) {
        this.refreshManager = caller;
        if ("auto".equals(Prefs.getSyncMethod(mContext)) // 동기화 방법 
                && !"".equals(Prefs.getSyncCalendar(mContext))) {

            // EventEntry event = new EventEntry(query(deleteId));

            oldBean = new ScheduleBean(query(deleteId));

            new googleDelete().execute();
        } else {
            localDelete(deleteId);
            CallerRefresh();
        }
    }

    public void syncImport(RefreshManager caller) {
        this.refreshManager = caller;
        new googleImport().execute();
    }

    public void doImport(List<EventEntry> events, ProgressDialog dialog) {

        for (int i = 0; i < events.size(); i++) {
            try {
                dialog.setProgress(i);
                ScheduleBean scheduleBean = new ScheduleBean(events.get(i));
                doImport(scheduleBean);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void doImport(ScheduleBean scheduleBean) {

        WhereClause whereClause = new WhereClause();

        whereClause.put(Schedule.GID, scheduleBean.getGID());
        whereClause.put(Schedule.ETAG, scheduleBean.getEtag());

        String sql = "SELECT _id FROM " + Schedule.SCHEDULE_TABLE_NAME + " WHERE " + whereClause.getClause();

        Cursor c = getReadableDatabase().rawQuery(sql, null);

        int updateCnt = 0;
        if (c.moveToNext()) {// uid와 etag가 동이한 자료가 존해하면...

            scheduleBean.setId(c.getLong(Schedule.COL_ID));
            c.close();

            // 구글 캘린더로 부터 가져온 자료로 local 데이터를 수정한다.
            updateCnt = localUpdate(scheduleBean);

            // 업데이트된 기존 자료가 없으면 ...
            // 싱크했지만 로컬에서 수정한 자료인 경우..
            if (updateCnt <= 0) {

                try {
                    // local data가 수정 되었을때...
                    // google캘린더 데이터를 로컬데이터와 동기화 한다. 
                    GoogleUtil gu = new GoogleUtil(Prefs.getAuthToken(mContext));

                    // 구글 캘린더자료로 부터 받은 수정일자. 
                    String googleUpdated = scheduleBean.getUpdated();

                    // 로컬 데이터의 수정일자.
                    scheduleBean = new ScheduleBean(query(scheduleBean.getId()));

                    // 구글캘린더의 업데이트 시간과 로컬 업데이트 시간이 같으면 서버로 재전송 금지.
                    if (!googleUpdated.equals(scheduleBean.getUpdated())) {
                        gu.updateEvent(scheduleBean);
                        //gu.insertEvent(Prefs.getSyncCalendar(mContext), scheduleBean, Prefs.getAccountName(mContext));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else {

            WhereClause whereClause2 = new WhereClause();

            whereClause2.put(Schedule.GID, scheduleBean.getGID());

            String sql2 = "SELECT _id FROM " + Schedule.SCHEDULE_TABLE_NAME + " WHERE " + whereClause2.getClause();

            Cursor c2 = getReadableDatabase().rawQuery(sql2, null);

            int updateCnt2 = 0;
            if (c2.moveToNext()) {// uid만 동일한 자료가 존해하면...
                ScheduleBean localBean = new ScheduleBean(c2, true);

                scheduleBean.setId(c2.getLong(Schedule.COL_ID));
                c2.close();
                Log.d(Common.TAG, "GUID동일건 UPDATE..");

                // 로컬에 없는 정보를 동기화 요청 결과로 변경하여 로컬에 저장한다.
                localBean.setTitle(scheduleBean.getSchedule_title());
                localBean.setDate(scheduleBean.getSchedule_date());
                localBean.setContents(scheduleBean.getSchedule_contents());
                localBean.setGID(scheduleBean.getGID());
                localBean.setEtag(scheduleBean.getEtag());
                localBean.setPublished(scheduleBean.getPublished());
                localBean.setUpdated(scheduleBean.getUpdated());
                localBean.setWhen(scheduleBean.getWhen());
                localBean.setWho(scheduleBean.getWho());
                localBean.setRecurrence(scheduleBean.getRecurrence());
                localBean.setSelfUrl(scheduleBean.getSelfurl());
                localBean.setEditurl(scheduleBean.getEditurl());
                localBean.setOriginalevent(scheduleBean.getOriginalevent());
                localBean.setEventstatus(scheduleBean.getEventstatus());

                updateCnt2 = localUpdate(localBean);

                // 업데이트된 기존 자료가 없으면 ...
                if (updateCnt2 <= 0) {
                    // 서버에서 변경된 자료를 로컬에 생성한다.
                    localInsert(scheduleBean);
                }
            } else {
                c.close();

                // etag와 uid가 동일한 자료가 
                // local db에 없는 일정이므로 신규등록한다.
                localInsert(scheduleBean);
            }
        }

    }

    /*
     * 배치작업
     */
    public void batchMakeCalendar(RefreshManager caller, String target) {
        if ("auto".equals(Prefs.getSyncMethod(this.mContext)) // 동기화 방법 
                && !"".equals(Prefs.getSyncCalendar(mContext))) {
            this.refreshManager = caller;

            new googleMakeCalendar().execute(target);

        } else {
            Toast.makeText(mContext, "구글캘린더 계정설정 또는 동기화 대상 캘린더가 지정되지 않았습니다.", Toast.LENGTH_LONG).show();
        }

    }

    public void batchUpload(RefreshManager caller, String target) {
        if ("auto".equals(Prefs.getSyncMethod(this.mContext)) // 동기화 방법 
                && !"".equals(Prefs.getSyncCalendar(mContext))) {
            this.refreshManager = caller;

            new googleUploadCalendar().execute(target);

        } else {
            Toast.makeText(mContext, "구글캘린더 계정설정 또는 동기화 대상 캘린더가 지정되지 않았습니다.", Toast.LENGTH_LONG).show();
        }

    }

    public void batchBibleCalendar(RefreshManager caller, String target, String gb) {
        if ("auto".equals(Prefs.getSyncMethod(this.mContext)) // 동기화 방법 
                && !"".equals(Prefs.getSyncCalendar(mContext))) {
            this.refreshManager = caller;

            //Bible Reading Plan생성.   
            String[] param = new String[] { target, gb };
            new googleMakeBiblePlan().execute(param);
        } else {
            Toast.makeText(mContext, "구글캘린더 계정설정 또는 동기화 대상 캘린더가 지정되지 않았습니다.", Toast.LENGTH_LONG).show();
        }

    }

    /*
     * AsyncTask     
     */
    private class googleInsert extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;
        EventEntry event;

        @Override
        protected void onPreExecute() {

            if ("".equals(oldBean.getSchedule_title())) {
                dialog = ProgressDialog.show(mContext, "", "일정을 복사 후 추가하고 있습니다...", true);
            } else {
                oldBean.setId(localInsert(oldBean));
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                GoogleUtil gu = new GoogleUtil(Prefs.getAuthToken(mContext));

                if ("".equals(oldBean.getSchedule_title())) {
                    oldBean = new ScheduleBean(query(oldBean.getId()));

                    // 기존에 구글캘린더에 존재하는 일정을 다시 추가하면 복제한다.
                    if (!"".equals(oldBean.getSelfurl())) {
                        oldBean.setTitle("사본_" + oldBean.getSchedule_title());

                        oldBean.setId(localInsert(oldBean));
                        oldBean.setEditurl(null);
                        oldBean.setSelfUrl(null);
                        oldBean.setEtag(null);
                        oldBean.setGID(null);
                    }
                }

                event = gu.insertEvent(Prefs.getSyncCalendar(mContext), oldBean, Prefs.getAccountName(mContext));

                if (!"".equals(event.title)) {
                    ScheduleBean eventBean = new ScheduleBean(event);
                    // eventBean.setId(oldBean.getId());

                    // 변경된 내용을 구글 캘린더에 반영한 후에 결과정보를 로컬에 다시 반영한다.
                    oldBean.setTitle(eventBean.getSchedule_title());
                    oldBean.setDate(eventBean.getSchedule_date());
                    oldBean.setContents(eventBean.getSchedule_contents());
                    oldBean.setGID(eventBean.getGID());
                    oldBean.setEtag(eventBean.getEtag());
                    oldBean.setPublished(eventBean.getPublished());
                    oldBean.setUpdated(eventBean.getUpdated());
                    oldBean.setWhen(eventBean.getWhen());
                    oldBean.setWho(eventBean.getWho());
                    oldBean.setRecurrence(eventBean.getRecurrence());
                    oldBean.setSelfUrl(eventBean.getSelfurl());
                    oldBean.setEditurl(eventBean.getEditurl());
                    oldBean.setOriginalevent(eventBean.getOriginalevent());
                    oldBean.setEventstatus(eventBean.getEventstatus());

                    localUpdate(oldBean);
                }

            } catch (IOException e) {
                cancel(true);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            CallerRefresh();
            if (dialog != null && dialog.isShowing())
                dialog.dismiss();
        }

    }

    private class googleUpdate extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;
        EventEntry event;

        @Override
        protected void onPreExecute() {
            // 변경사항을 먼저 저장한다.
            localUpdate(oldBean);

            //dialog = ProgressDialog.show(this, "", "Add event...", true);
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                if ("".equals(oldBean.getEtag())) {
                    cancel(true);
                }

                GoogleUtil gu = new GoogleUtil(Prefs.getAuthToken(mContext));
                event = gu.updateEvent(oldBean);

                if (event == null) {

                } else {
                    if (!"".equals(event.getEditLink())) {

                        ScheduleBean eventBean = new ScheduleBean(event);

                        // 변경된 내용을 구글 캘린더에 반영한 후에 결과정보를 로컬에 다시 반영한다.
                        oldBean.setTitle(eventBean.getSchedule_title());
                        oldBean.setDate(eventBean.getSchedule_date());
                        oldBean.setContents(eventBean.getSchedule_contents());
                        oldBean.setGID(eventBean.getGID());
                        oldBean.setEtag(eventBean.getEtag());
                        oldBean.setPublished(eventBean.getPublished());
                        oldBean.setUpdated(eventBean.getUpdated());
                        oldBean.setWhen(eventBean.getWhen());
                        oldBean.setWho(eventBean.getWho());
                        oldBean.setRecurrence(eventBean.getRecurrence());
                        oldBean.setSelfUrl(eventBean.getSelfurl());
                        oldBean.setEditurl(eventBean.getEditurl());
                        oldBean.setOriginalevent(eventBean.getOriginalevent());
                        oldBean.setEventstatus(eventBean.getEventstatus());

                        localUpdate(oldBean);
                    }
                }

            } catch (IOException e) {
                cancel(true);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            CallerRefresh();

            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        }

    }

    private class googleDelete extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            //dialog = ProgressDialog.show(mContext, "", "Delete event from google...", true);
            localDelete(oldBean.getId());
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                if ("".equals(oldBean.getEtag())) {
                    cancel(true);
                }

                GoogleUtil gu = new GoogleUtil(Prefs.getAuthToken(mContext));
                gu.deleteEvent(oldBean);
            } catch (IOException e) {
                cancel(true);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            CallerRefresh();
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        }

    }

    private class googleImport extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;
        List<EventEntry> events;

        @Override
        protected void onPreExecute() {
            //dialog = ProgressDialog.show(mContext, "", "구글캘린더에서 일정을 가져오고 있습니다...", true);
            dialog = new ProgressDialog(mContext);
            dialog.setTitle("일정 가져오기");
            dialog.setMessage("구글캘린더에서 일정을 가져오고 있습니다...");
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMax(100);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                GoogleUtil gu = new GoogleUtil(Prefs.getAuthToken(mContext));
                String url = Prefs.getSyncCalendar(mContext);
                events = gu.getEvents(url);

                dialog.setMax(events.size());
                doImport(events, dialog);
            } catch (IOException e) {
                cancel(true);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            CallerRefresh();
            dialog.dismiss();
        }

    }

    @SuppressWarnings("unused")
    private class googleInit extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(mContext, "", "구글캘린더의 모든 일정을 삭제하고 있습니다...", true);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                GoogleUtil gu = new GoogleUtil(Prefs.getAuthToken(mContext));
                String url = Prefs.getSyncCalendar(mContext);
                gu.batchDelete(url);
            } catch (IOException e) {
                cancel(true);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            CallerRefresh();
            dialog.dismiss();
        }

    }

    private class googleMakeCalendar extends AsyncTask<String, Void, Void> {
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(mContext);
            dialog.setTitle("일정생성!");
            dialog.setMessage("구글캘린더에 음력일정을 생성하고 있습니다...");
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMax(366);
            dialog.show();
        }

        @Override
        protected Void doInBackground(String... params) {

            try {
                GoogleUtil gu = new GoogleUtil(Prefs.getAuthToken(mContext));
                String url = "";
                if ("".equals(params[0])) {
                    String calName = "음력_" + Common.fmtDate().substring(0, 4);

                    CalendarEntry entry = gu.addCalendar(calName);

                    url = entry.getEventFeedLink();

                    if ("".equals(url)) {
                        cancel(true);
                        Log.e(Common.TAG, "===== 신규 달력생성 실패!!! =====");
                    }
                } else {
                    url = params[0];
                }
                gu.batchLunarEvents(url, dialog);
            } catch (IOException e) {
                cancel(true);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            CallerRefresh();
            dialog.dismiss();
        }

    }

    private class googleUploadCalendar extends AsyncTask<String, Void, Void> {
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(mContext);
            dialog.setTitle("일정생성!");
            dialog.setMessage("구글캘린더에 일정을 생성하고 있습니다...");
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMax(100);
            dialog.show();
        }

        @Override
        protected Void doInBackground(String... params) {

            try {
                GoogleUtil gu = new GoogleUtil(Prefs.getAuthToken(mContext));
                String url = "";
                if ("".equals(params[0])) {
                    String calName = "로컬일정_" + Common.fmtDate().substring(5);

                    CalendarEntry entry = gu.addCalendar(calName);

                    url = entry.getEventFeedLink();

                    if ("".equals(url)) {
                        cancel(true);
                        Log.e(Common.TAG, "===== 신규 달력생성 실패!!! =====");
                    }
                } else {
                    url = params[0];
                }

                gu.batchLocalEvents(queryAllLocal(), url, dialog);
            } catch (IOException e) {
                cancel(true);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            CallerRefresh();
            dialog.dismiss();
        }

    }

    private class googleMakeBiblePlan extends AsyncTask<String, Void, Void> {
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(mContext);
            dialog.setTitle("일정생성!");
            dialog.setMessage("구글캘린더에 맥체인성경읽기 일정을 생성하고 있습니다...");
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setCancelable(true);
            dialog.setMax(366);
            dialog.show();
        }

        @Override
        protected Void doInBackground(String... params) {
            try {

                GoogleUtil gu = new GoogleUtil(Prefs.getAuthToken(mContext));
                String url = "";
                if ("".equals(params[0])) {
                    String calName = "성경읽기" + ("2".equals(params[1]) ? "(개인)" : "(가정)");

                    CalendarEntry entry = gu.addCalendar(calName);

                    url = entry.getEventFeedLink();

                    if ("".equals(url)) {
                        cancel(true);
                        Log.e(Common.TAG, "===== 신규 달력생성 실패!!! =====");
                    }
                } else {
                    url = params[0];
                }

                String[] PlanList = mContext.getResources().getStringArray(R.array.array_bibleplan);

                gu.batchBiblePlan(url, dialog, PlanList, params[1]);
            } catch (IOException e) {
                cancel(true);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            CallerRefresh();
            dialog.dismiss();
        }

    }

    /*
     * Local Task
     */
    public Long localInsert(ScheduleBean scheduleBean) {
        Long newId;
        db.beginTransaction();
        if (1 == scheduleBean.getDday_displayyn()) {
            // 기존에 상단에 표시하도록 되어있던 D-day정보는 목록표시로 변경한다.
            db.execSQL("update schedule set " + Schedule.DDAY_DISPLAYYN + " = 0 where " + Schedule.DDAY_DISPLAYYN + " = 1");
        }

        newId = db.insert(Schedule.SCHEDULE_TABLE_NAME, null, new ScheduleContentValues(scheduleBean).value);
        db.setTransactionSuccessful();
        db.endTransaction();
        return newId;
    }

    public int localUpdate(ScheduleBean scheduleBean) {
        WhereClause whereClause = new WhereClause(true);
        whereClause.put(Schedule._ID, scheduleBean.getId());
        whereClause.put(Schedule.UPDATED, scheduleBean.getUpdated(), "<=");

        db.beginTransaction();
        if (1 == scheduleBean.getDday_displayyn()) {
            // 기존에 상단에 표시하도록 되어있던 D-day정보는 목록표시로 변경한다.
            db.execSQL("update schedule set " + Schedule.DDAY_DISPLAYYN + " = 0 where " + Schedule.DDAY_DISPLAYYN + " = 1");
        }

        int result = db.update(Schedule.SCHEDULE_TABLE_NAME, new ScheduleContentValues(scheduleBean).value, whereClause.getClause(), whereClause.getParam());

        db.setTransactionSuccessful();
        db.endTransaction();
        return result;
    }

    public void localDelete(Long id) {
        String sql = "DELETE FROM " + Schedule.SCHEDULE_TABLE_NAME + " WHERE " + Schedule._ID + "=" + id;
        getWritableDatabase().execSQL(sql);
    }

    public void deleteAll() {
        String sql = "DELETE FROM " + Schedule.SCHEDULE_TABLE_NAME;
        getWritableDatabase().execSQL(sql);
    }

    /*
     * Query
     */
    public Cursor query(Long id) {

        StringBuilder query;
        query = new StringBuilder();

        query.append("SELECT " + Schedule._ID);
        query.append("    ," + Schedule.SCHEDULE_DATE);
        query.append("    ," + Schedule.SCHEDULE_TITLE);
        query.append("    ,case when " + Schedule.SCHEDULE_REPEAT + " = 9 then " + Schedule.ALARM_DATE);
        query.append("    else " + Schedule.SCHEDULE_CONTENTS + " end " + Schedule.SCHEDULE_CONTENTS);
        query.append("    ," + Schedule.SCHEDULE_REPEAT);
        query.append("    ," + Schedule.SCHEDULE_CHECK);
        query.append("    ," + Schedule.ALARM_LUNASOLAR);
        query.append("    ," + Schedule.ALARM_DATE);
        query.append("    ," + Schedule.ALARM_TIME);
        query.append("    ," + Schedule.ALARM_DAYOFWEEK);
        query.append("    ," + Schedule.ALARM_DAY);
        query.append("    ," + Schedule.DDAY_ALARMYN);
        query.append("    ," + Schedule.DDAY_ALARMDAY);
        query.append("    ," + Schedule.DDAY_ALARMSIGN);
        query.append("    ," + Schedule.DDAY_DISPLAYYN);
        query.append("    ," + Schedule.GID);
        query.append("    ," + Schedule.ANNIVERSARY);
        query.append("    ," + Schedule.LUNARYN);
        query.append("    ," + Schedule.SCHEDULE_LDATE);
        query.append("    ," + Schedule.ALARM_DETAILINFO);
        query.append("    ," + Schedule.DDAY_DETAILINFO);
        query.append("    ," + Schedule.SCHEDULE_TYPE);
        query.append("    ," + Schedule.BIBLE_BOOK);
        query.append("    ," + Schedule.BIBLE_CHAPTER);
        query.append("    ," + Schedule.ETAG);
        query.append("    ," + Schedule.PUBLISHED);
        query.append("    ," + Schedule.UPDATED);
        query.append("    ," + Schedule.WHEN);
        query.append("    ," + Schedule.WHO);
        query.append("    ," + Schedule.RECURRENCE);
        query.append("    ," + Schedule.SELFURL);
        query.append("    ," + Schedule.EDITURL);
        query.append("    ," + Schedule.ORIGINALEVENT);
        query.append("    ," + Schedule.EVENTSTATUS);
        query.append(" FROM " + Schedule.SCHEDULE_TABLE_NAME);
        query.append(" WHERE 1 = 1 ");
        query.append(" AND " + Schedule._ID + " = " + id.toString());

        return getReadableDatabase().rawQuery(query.toString(), null);

    }

    public Cursor query(String date, String lDay) {
        return query(new ContactEvent[0], date, lDay);
    }

    public Cursor query(ContactEvent[] events, String date, String lDay) {

        String sday = date.substring(5);
        String lday = lDay.substring(4, 6) + "-" + lDay.substring(6);

        StringBuilder query = new StringBuilder();

        //양력, 음력 일반 일정
        query.append("SELECT   ");
        query.append("    _id ");
        query.append("    ,'Schedule' " + Schedule.SCHEDULE_TYPE + " ");
        query.append("    ," + Schedule.SCHEDULE_TITLE + " ");
        query.append("    ,6 kind ");
        query.append("    ,'' as displayname ");
        query.append("    ,'' as uri ");
        query.append("    ,case when " + Schedule.SCHEDULE_DATE + " > '1900-01-01' then " + Schedule.SCHEDULE_REPEAT + " else 0 end as " + Schedule.SCHEDULE_REPEAT);
        query.append(" FROM  schedule ");
        query.append(" WHERE " + Schedule.SCHEDULE_DATE + " = ? and lunaryn <> 'Y' and  anniversary <> 'Y' ");
        query.append(" OR ( schedule_ldate = '" + Common.fmtDate(lDay) + "' and lunaryn = 'Y' and anniversary <> 'Y' )");

        //사용자 양력기념일, 음력기념일
        query.append("union all  ");
        query.append("select _id, 'Anniversary' " + Schedule.SCHEDULE_TYPE + ", " + Schedule.SCHEDULE_TITLE + " ");
        query.append("    ,3 kind ");
        query.append("    ,'' as displayname ");
        query.append("    ,'' as uri ");
        query.append("    ,case when " + Schedule.SCHEDULE_DATE + " > '1900-01-01' then " + Schedule.SCHEDULE_REPEAT + " else 0 end as " + Schedule.SCHEDULE_REPEAT);
        query.append(" from schedule ");
        query.append("where 1=1 ");
        query.append(" AND " + Schedule.SCHEDULE_DATE + " <= STRFTIME('%Y', '" + date + "', 'localtime')||'-12-31' ");
        query.append(" and  " + Schedule.SCHEDULE_DATE + " like '%" + date.substring(5) + "' and lunaryn <> 'Y' and  anniversary = 'Y' ");
        query.append(" OR ( " + Schedule.SCHEDULE_DATE + " <= STRFTIME('%Y', '" + date + "', 'localtime')||'-12-31' ");
        query.append(" and schedule_ldate like '%" + Common.fmtDate(lDay).substring(5) + "' and lunaryn = 'Y' and anniversary = 'Y' )");

        // system 기념일
        query.append("union all  ");
        query.append("select _id, 'Anniversary' " + Schedule.SCHEDULE_TYPE + ", " + Schedule.SCHEDULE_TITLE + " ");
        query.append("    ," + Schedule.ALARM_DAY + " kind "); //붉은깃발 혹은 녹색깃발.
        query.append("    ,'' as displayname ");
        query.append("    ,'' as uri ");
        query.append("    ,case when " + Schedule.SCHEDULE_DATE + " > '1900-01-01' then " + Schedule.SCHEDULE_REPEAT + " else 0 end as " + Schedule.SCHEDULE_REPEAT);
        query.append(" from schedule ");
        query.append("where " + Schedule.SCHEDULE_REPEAT + " = 9 ");
        query.append("and " + Schedule.SCHEDULE_DATE + " = '1900-01-01' ");
        query.append("and (   (" + Schedule.ALARM_LUNASOLAR + " = 0 and " + Schedule.ALARM_DATE + " = '" + sday + "') ");
        query.append("     or (" + Schedule.ALARM_LUNASOLAR + " = 1 and " + Schedule.ALARM_DATE + " = '" + lday + "') ) ");
        query.append("and " + Schedule.ALARM_TIME + " = '00:00' ");

        //d-day
        query.append("union all  ");
        query.append("SELECT   ");
        query.append("    _id ");
        query.append("    ,'D-day' " + Schedule.SCHEDULE_TYPE + " ");
        query.append("    ,substr(" + Schedule.SCHEDULE_TITLE + ", 1, 15) ");
        query.append("    ||'('|| ");
        query.append("case when cast(JULIANDAY(?, 'localtime') - JULIANDAY(DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime'), 'localtime') as integer) > 0  ");
        query.append("then 'D + ' || cast(JULIANDAY(?, 'localtime') - JULIANDAY(DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime'), 'localtime') as integer)  ");
        query.append(" when cast(JULIANDAY(?, 'localtime') - JULIANDAY(DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime'), 'localtime') as integer) = 0  ");
        query.append("then 'D day' else 'D ' ||  cast(JULIANDAY(?, 'localtime') - JULIANDAY(DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime'), 'localtime') as integer) end ");
        query.append("    ||')' " + Schedule.SCHEDULE_TITLE + " ");
        query.append("    ,5 kind ");
        query.append("    ,'' as displayname ");
        query.append("    ,'' as uri ");
        query.append("    ,case when " + Schedule.SCHEDULE_DATE + " > '1900-01-01' then " + Schedule.SCHEDULE_REPEAT + " else 0 end as " + Schedule.SCHEDULE_REPEAT);
        query.append(" FROM  schedule ");
        query.append("WHERE 1=1 ");
        query.append("and " + Schedule.DDAY_ALARMYN + " = 1  ");
        query.append("and " + Schedule.DDAY_DISPLAYYN + " = 2  ");
        query.append("or (" + Schedule.DDAY_ALARMYN + " = 1  ");
        query.append("and " + Schedule.DDAY_DISPLAYYN + " in (0, 1)  ");
        query.append("and strftime('%Y-%m-%d', DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime'), 'localtime') = ? ) ");

        for (ContactEvent event : events) {
            if (date.substring(5).equals(event.mStartDate)) {
                query.append(" UNION ALL ");
                query.append("SELECT   ");
                query.append("    " + event.mContact_id + " as _id ");
                query.append("    ,'Contact' " + Schedule.SCHEDULE_TYPE + " ");
                query.append("    ,'" + event.mDisplayName + "님의 ");
                switch (event.mType) {
                    case ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY:
                        query.append("기념일");
                        break;
                    case ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY:
                        query.append("생일");
                        break;
                    case ContactsContract.CommonDataKinds.Event.TYPE_OTHER:
                        query.append("기타일정");
                        break;
                    default:
                        query.append("일정");
                        break;
                }
                query.append(" 입니다.' as " + Schedule.SCHEDULE_TITLE + " ");
                query.append("    ,9 kind ");
                query.append("    ,'" + event.mDisplayName + "' as displayname ");
                query.append("    ,'" + event.mUri + "' as uri ");
                query.append("    ,0 " + Schedule.SCHEDULE_REPEAT + " ");
            }
        }

        query.append(" ORDER BY 1 ");

        String selectionArgs[] = new String[] { date, date, date, date, date, date, };

        Log.d(Common.TAG, "query=" + query.toString());

        return getReadableDatabase().rawQuery(query.toString(), selectionArgs);

    }

    public Cursor queryAll() {

        StringBuilder query = new StringBuilder("SELECT " + Schedule._ID);
        query.append("   ," + Schedule.SCHEDULE_DATE);
        query.append("   ," + Schedule.SCHEDULE_TITLE);
        query.append("   ," + Schedule.SCHEDULE_CONTENTS);
        query.append("   ," + Schedule.SCHEDULE_REPEAT);
        query.append("   ," + Schedule.SCHEDULE_CHECK);
        query.append("   ," + Schedule.ALARM_LUNASOLAR);
        query.append("   ," + Schedule.ALARM_DATE);
        query.append("   ," + Schedule.ALARM_TIME);
        query.append("   ," + Schedule.ALARM_DAYOFWEEK);
        query.append("   ," + Schedule.ALARM_DAY);
        query.append("   ," + Schedule.DDAY_ALARMYN);
        query.append("   ," + Schedule.DDAY_ALARMDAY);
        query.append("   ," + Schedule.DDAY_ALARMSIGN);
        query.append("   ," + Schedule.DDAY_DISPLAYYN);
        query.append("   ," + Schedule.GID);
        query.append("   ," + Schedule.ANNIVERSARY);
        query.append("   ," + Schedule.LUNARYN);
        query.append("   ," + Schedule.SCHEDULE_LDATE);
        query.append("   ," + Schedule.ALARM_DETAILINFO);
        query.append("   ," + Schedule.DDAY_DETAILINFO);
        query.append("   ," + Schedule.SCHEDULE_TYPE);
        query.append("   ," + Schedule.BIBLE_BOOK);
        query.append("   ," + Schedule.BIBLE_CHAPTER);
        query.append("   ," + Schedule.ETAG);
        query.append("   ," + Schedule.PUBLISHED);
        query.append("   ," + Schedule.UPDATED);
        query.append("   ," + Schedule.WHEN);
        query.append("   ," + Schedule.WHO);
        query.append("   ," + Schedule.RECURRENCE);
        query.append("   ," + Schedule.SELFURL);
        query.append("   ," + Schedule.EDITURL);
        query.append("   ," + Schedule.ORIGINALEVENT);
        query.append("   ," + Schedule.EVENTSTATUS);
        query.append(" FROM  schedule ");
        query.append(" WHERE " + Schedule.SCHEDULE_DATE + " > '1900-01-01' ");
        query.append("   AND " + Schedule.SCHEDULE_REPEAT + " < 9 ");
        return getReadableDatabase().rawQuery(query.toString(), null);

    }

    public Cursor queryAllLocal() {

        StringBuilder query = new StringBuilder("SELECT " + Schedule._ID);
        query.append("   ," + Schedule.SCHEDULE_DATE);
        query.append("   ," + Schedule.SCHEDULE_TITLE);
        query.append("   ," + Schedule.SCHEDULE_CONTENTS);
        query.append("   ," + Schedule.SCHEDULE_REPEAT);
        query.append("   ," + Schedule.SCHEDULE_CHECK);
        query.append("   ," + Schedule.ALARM_LUNASOLAR);
        query.append("   ," + Schedule.ALARM_DATE);
        query.append("   ," + Schedule.ALARM_TIME);
        query.append("   ," + Schedule.ALARM_DAYOFWEEK);
        query.append("   ," + Schedule.ALARM_DAY);
        query.append("   ," + Schedule.DDAY_ALARMYN);
        query.append("   ," + Schedule.DDAY_ALARMDAY);
        query.append("   ," + Schedule.DDAY_ALARMSIGN);
        query.append("   ," + Schedule.DDAY_DISPLAYYN);
        query.append("   ," + Schedule.GID);
        query.append("   ," + Schedule.ANNIVERSARY);
        query.append("   ," + Schedule.LUNARYN);
        query.append("   ," + Schedule.SCHEDULE_LDATE);
        query.append("   ," + Schedule.ALARM_DETAILINFO);
        query.append("   ," + Schedule.DDAY_DETAILINFO);
        query.append("   ," + Schedule.SCHEDULE_TYPE);
        query.append("   ," + Schedule.BIBLE_BOOK);
        query.append("   ," + Schedule.BIBLE_CHAPTER);
        query.append("   ," + Schedule.ETAG);
        query.append("   ," + Schedule.PUBLISHED);
        query.append("   ," + Schedule.UPDATED);
        query.append("   ," + Schedule.WHEN);
        query.append("   ," + Schedule.WHO);
        query.append("   ," + Schedule.RECURRENCE);
        query.append("   ," + Schedule.SELFURL);
        query.append("   ," + Schedule.EDITURL);
        query.append("   ," + Schedule.ORIGINALEVENT);
        query.append("   ," + Schedule.EVENTSTATUS);
        query.append(" FROM  schedule ");
        query.append(" WHERE " + Schedule.SCHEDULE_DATE + " > '1900-01-01' ");
        query.append("   AND " + Schedule.SCHEDULE_REPEAT + " < 9 ");
        query.append("   AND trim(" + Schedule.GID + ") = ''");
        return getReadableDatabase().rawQuery(query.toString(), null);

    }

    /*
     * Data Manger Query
     */
    public boolean export(Cursor cursor) {

        String path = "/sdcard/";
        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.SUNDAY);
        //String datetime = Common.fmtDateTime(c);
        File file = new File(path + "lunarcalendar.auto.bak");
        try {
            FileOutputStream fos = new FileOutputStream(file);

            StringBuilder buf = new StringBuilder();

            buf.append("\t" + Schedule.SCHEDULE_DATE);
            buf.append("\t" + Schedule.SCHEDULE_TITLE);
            buf.append("\t" + Schedule.SCHEDULE_CONTENTS);
            buf.append("\t" + Schedule.SCHEDULE_REPEAT);
            buf.append("\t" + Schedule.SCHEDULE_CHECK);
            buf.append("\t" + Schedule.ALARM_LUNASOLAR);
            buf.append("\t" + Schedule.ALARM_DATE);
            buf.append("\t" + Schedule.ALARM_TIME);
            buf.append("\t" + Schedule.ALARM_DAYOFWEEK);
            buf.append("\t" + Schedule.ALARM_DAY);
            buf.append("\t" + Schedule.DDAY_ALARMYN);
            buf.append("\t" + Schedule.DDAY_ALARMDAY);
            buf.append("\t" + Schedule.DDAY_ALARMSIGN);
            buf.append("\t" + Schedule.DDAY_DISPLAYYN);
            buf.append("\t" + Schedule.GID);
            buf.append("\t" + Schedule.ANNIVERSARY);
            buf.append("\t" + Schedule.LUNARYN);
            buf.append("\t" + Schedule.SCHEDULE_LDATE);
            buf.append("\t" + Schedule.ALARM_DETAILINFO);
            buf.append("\t" + Schedule.DDAY_DETAILINFO);
            buf.append("\t" + Schedule.SCHEDULE_TYPE);
            buf.append("\t" + Schedule.BIBLE_BOOK);
            buf.append("\t" + Schedule.BIBLE_CHAPTER);
            buf.append("\t" + Schedule.ETAG);
            buf.append("\t" + Schedule.PUBLISHED);
            buf.append("\t" + Schedule.UPDATED);
            buf.append("\t" + Schedule.WHEN);
            buf.append("\t" + Schedule.WHO);
            buf.append("\t" + Schedule.RECURRENCE);
            buf.append("\t" + Schedule.SELFURL);
            buf.append("\t" + Schedule.EDITURL);
            buf.append("\t" + Schedule.ORIGINALEVENT);
            buf.append("\t" + Schedule.EVENTSTATUS);
            buf.append("\n");

            cursor.moveToFirst();
            while (cursor.getCount() > 0) {

                for (int col = 0; col < cursor.getColumnCount(); col++) {
                    buf.append(cursor.getString(col)).append("||");
                }
                buf.append("\n");

                if (!cursor.moveToNext())
                    break;

            }

            fos.write(buf.toString().getBytes());

            fos.close();

            return true;
        } catch (IOException e) {
            Log.e("Export", e.getMessage(), e);
            return false;
        }
    }

    public boolean exportdata(Cursor cursor, ProgressDialog pd) {
        String path = android.os.Environment.getExternalStorageDirectory().toString() + "/";
        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.SUNDAY);
        String datetime = Common.fmtDateTime(c);
        File file = new File(path + "lunarcalendar.backup");
        pd.setMax(cursor.getCount() + 10);
        if (file.exists()) {
            if (file.renameTo(new File(path + "lunarcalendar.backup." + datetime))) {
                file = new File(path + "lunarcalendar.backup");
            } else {
                file = new File(path + "lunarcalendar." + datetime + ".backup");
                if (file.exists()) {
                    return false;
                }
            }
        }
        pd.setProgress(10);
        try {
            FileOutputStream fos = new FileOutputStream(file);

            StringBuilder buf = new StringBuilder(Schedule._ID);
            buf.append("\t" + Schedule.SCHEDULE_DATE);
            buf.append("\t" + Schedule.SCHEDULE_TITLE);
            buf.append("\t" + Schedule.SCHEDULE_CONTENTS);
            buf.append("\t" + Schedule.SCHEDULE_REPEAT);
            buf.append("\t" + Schedule.SCHEDULE_CHECK);
            buf.append("\t" + Schedule.ALARM_LUNASOLAR);
            buf.append("\t" + Schedule.ALARM_DATE);
            buf.append("\t" + Schedule.ALARM_TIME);
            buf.append("\t" + Schedule.ALARM_DAYOFWEEK);
            buf.append("\t" + Schedule.ALARM_DAY);
            buf.append("\t" + Schedule.DDAY_ALARMYN);
            buf.append("\t" + Schedule.DDAY_ALARMDAY);
            buf.append("\t" + Schedule.DDAY_ALARMSIGN);
            buf.append("\t" + Schedule.DDAY_DISPLAYYN);
            buf.append("\t" + Schedule.GID);
            buf.append("\t" + Schedule.ANNIVERSARY);
            buf.append("\t" + Schedule.LUNARYN);
            buf.append("\t" + Schedule.SCHEDULE_LDATE);
            buf.append("\t" + Schedule.ALARM_DETAILINFO);
            buf.append("\t" + Schedule.DDAY_DETAILINFO);
            buf.append("\t" + Schedule.SCHEDULE_TYPE);
            buf.append("\t" + Schedule.BIBLE_BOOK);
            buf.append("\t" + Schedule.BIBLE_CHAPTER);
            buf.append("\t" + Schedule.ETAG);
            buf.append("\t" + Schedule.PUBLISHED);
            buf.append("\t" + Schedule.UPDATED);
            buf.append("\t" + Schedule.WHEN);
            buf.append("\t" + Schedule.WHO);
            buf.append("\t" + Schedule.RECURRENCE);
            buf.append("\t" + Schedule.SELFURL);
            buf.append("\t" + Schedule.EDITURL);
            buf.append("\t" + Schedule.ORIGINALEVENT);
            buf.append("\t" + Schedule.EVENTSTATUS);
            buf.append("\n\r");

            cursor.moveToFirst();
            while (cursor.getCount() > 0) {
                pd.setProgress(cursor.getPosition() + 10);
                for (int col = 0; col < cursor.getColumnCount(); col++) {
                    buf.append(cursor.getString(col)).append("\t");
                }
                buf.append("\n\r");
                if (!cursor.moveToNext())
                    break;
            }
            fos.write(buf.toString().getBytes());
            fos.close();
            return true;
        } catch (Exception e) {
            Log.e("Export", e.getMessage(), e);
            return false;
        }
    }

    public boolean importdata(String backupFile) {

        String path = android.os.Environment.getExternalStorageDirectory().toString() + "/";
        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.SUNDAY);
        //File file = new File(path + "lunarcalendar.backup");
        File file = new File(path + backupFile);

        if (!file.exists()) {
            return true;
        }

        try {
            String[] CheckData = null;
            FileInputStream fis = new FileInputStream(file);
            int size = fis.available();
            byte[] buf = new byte[size];

            fis.read(buf);
            String filedata = new String(buf);
            fis.close();

            CheckData = filedata.split("\n\r");

            db.beginTransaction();
            ContentValues val = null;
            for (int row = 0; row < CheckData.length; row++) {
                if (row == 0)
                    continue;

                String data[] = CheckData[row].toString().split("\t");
                val = new ContentValues();

                for (int col = 0; col < data.length; col++) {
                    val.put(Constants.mColumns[col], "null".equals(data[col]) ? "" : data[col]);
                }

                if (!queryExists(data[Schedule.COL_SCHEDULE_DATE], data[Schedule.COL_SCHEDULE_TITLE], data[Schedule.COL_ETAG])) {
                    val.remove(Schedule._ID);
                    db.insert(Schedule.SCHEDULE_TABLE_NAME, null, val);
                }
            }

            db.setTransactionSuccessful();
            db.endTransaction();

            return true;
        } catch (Exception e) {
            db.endTransaction();
            Log.e("Import", e.getMessage(), e);
            return false;
        }
    }

    public boolean copy(Cursor cursor) {
        ContentValues val = null;
        if (!export(cursor)) {
            return false;
        }

        db.beginTransaction();
        boolean isFirst = true;

        Log.d(Common.TAG, "cursor=" + cursor.getCount());

        Log.d(Common.TAG, " cursor.moveToFirst()=" + cursor.moveToFirst());
        Log.d(Common.TAG, " moveToNext()=" + cursor.moveToNext());

        cursor.moveToFirst();
        while (cursor.getCount() > 0) {

            if (isFirst) {
                StringBuilder query = new StringBuilder();
                // 시스템기념일을 제외한 일정을 모두 삭제한다.
                query.append("DELETE FROM schedule ");
                query.append("WHERE schedule_date > '1900-01-01' ");
                query.append("and schedule_repeat < 9 ");
                db.execSQL(query.toString());
            }

            val = new ContentValues();

            for (int col = 0; col < cursor.getColumnCount(); col++) {
                val.put(Constants.mColumns[col], cursor.getString(col));
            }

            val.remove(Schedule._ID);

            Log.d(Common.TAG, "Contents=" + val);

            db.insert("schedule", null, val);

            isFirst = false;
            if (!cursor.moveToNext())
                break;
        }

        db.setTransactionSuccessful();
        db.endTransaction();

        return true;
    }

    public Cursor queryExistsAnniversary(String month, String lfromday, String ltoday) {

        StringBuilder query = new StringBuilder();

        Log.d(Common.TAG, "month=" + month + ", lfromday=" + lfromday + ", ltoday=" + ltoday);

        lfromday = lfromday.substring(4, 8);
        ltoday = ltoday.substring(4, 8);

        boolean isChange = (Integer.parseInt(lfromday.substring(0, 2)) != Integer.parseInt(ltoday.substring(0, 2)));

        lfromday = String.format("%2s-%2s", lfromday.substring(0, 2), lfromday.substring(2));
        ltoday = String.format("%2s-%2s", ltoday.substring(0, 2), ltoday.substring(2));

        query.append("select case when " + Schedule.ALARM_LUNASOLAR + " = 0 then cast(substr(" + Schedule.ALARM_DATE + ", -2) as integer)  ");
        query.append("      else cast(replace(substr(" + Schedule.ALARM_DATE + ", -5), '-', '') as integer) end lday ");
        query.append("      , cast(" + Schedule.ALARM_DAY + " as integer) flag ");
        query.append("from schedule ");
        query.append("where " + Schedule.SCHEDULE_REPEAT + " = 9 ");
        query.append("and " + Schedule.SCHEDULE_DATE + " = '1900-01-01' ");
        query.append("and (   (" + Schedule.ALARM_LUNASOLAR + " = 0 and " + Schedule.ALARM_DATE + " like '" + month.substring(5, 7) + "%') ");
        if (isChange) {
            query.append("     or (" + Schedule.ALARM_LUNASOLAR + " = 1 and " + Schedule.ALARM_DATE + " between '" + lfromday + "' and '" + lfromday.substring(0, 2) + "-31')  ");
            query.append("     or (" + Schedule.ALARM_LUNASOLAR + " = 1 and " + Schedule.ALARM_DATE + " between '" + ltoday.substring(0, 2) + "-01' and '" + ltoday + "') ) ");
        } else {
            query.append("     or (" + Schedule.ALARM_LUNASOLAR + " = 1 and " + Schedule.ALARM_DATE + " between '" + lfromday + "' and '" + ltoday + "') ) ");
        }
        query.append("and " + Schedule.ALARM_TIME + " = '00:00' ");

        Log.d(Common.TAG, "query=" + query.toString());
        return getReadableDatabase().rawQuery(query.toString(), null);

    }

    public boolean queryExists(String date, String title, String etag) {

        StringBuilder query = new StringBuilder();

        query.append(" select count(*) ");
        query.append(" from schedule ");
        query.append(" where " + Schedule.SCHEDULE_DATE + " = '" + date + "' ");
        query.append(" and " + Schedule.SCHEDULE_TITLE + " = '" + title + "' ");
        query.append(" or etag = '" + etag + "' ");

        Cursor c = getReadableDatabase().rawQuery(query.toString(), null);

        if (c.moveToNext()) {
            return (c.getInt(0) > 0);
        } else {
            return false;
        }

    }

    public Cursor queryExistsSchedule(String month) {

        StringBuilder query = new StringBuilder();

        //   String sDate[] = Common.tokenFn(month + "-01", "-");
        //  Calendar c = Calendar.getInstance();
        //  c.setFirstDayOfWeek(Calendar.SUNDAY);
        //   c.set(Integer.parseInt(sDate[0]), Integer.parseInt(sDate[1]) -1, 1);
        //  c.add(Calendar.DAY_OF_MONTH, -1);

        // 양력일정
        query.append("SELECT  ");
        query.append("    DISTINCT CAST(STRFTIME('%d', " + Schedule.SCHEDULE_DATE + ", 'localtime') AS INTEGER) DAY  ");
        query.append("FROM " + Schedule.SCHEDULE_TABLE_NAME + " ");
        query.append("WHERE 1 = 1 ");
        query.append(" AND " + Schedule.SCHEDULE_DATE + " LIKE '" + month + "%' ");
        query.append(" AND " + Schedule.LUNARYN + " <> 'Y' ");
        query.append(" AND " + Schedule.ANNIVERSARY + " <> 'Y' ");

        // 양력기념일
        query.append(" UNION ALL  ");
        query.append("SELECT  ");
        query.append("    DISTINCT CAST(STRFTIME('%d', " + Schedule.SCHEDULE_DATE + ", 'localtime') AS INTEGER) DAY  ");
        query.append("FROM " + Schedule.SCHEDULE_TABLE_NAME + " ");
        query.append("WHERE 1 = 1 ");
        query.append(" AND substr(" + Schedule.SCHEDULE_DATE + ",6,2) = '" + month.substring(5) + "' ");
        query.append(" AND " + Schedule.ANNIVERSARY + " = 'Y' ");
        query.append(" AND " + Schedule.LUNARYN + " <> 'Y' ");
        query.append(" AND " + Schedule.SCHEDULE_DATE + " <= STRFTIME('%Y', '" + month + "-01" + "', 'localtime')||'-12-31' ");
        query.append(" ORDER BY 1 ");

        return getReadableDatabase().rawQuery(query.toString(), null);

    }

    public Cursor queryExistsSchedule2(String month) {

        StringBuilder query = new StringBuilder();

        String sDate[] = Common.tokenFn(month + "-01", "-");
        String lStart = Common.fmtDate(Lunar2Solar.s2l(Integer.parseInt(sDate[0]), Integer.parseInt(sDate[1]), 1));

        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.SUNDAY);
        c.set(Integer.parseInt(sDate[0]), Integer.parseInt(sDate[1]), 1);
        c.add(Calendar.DAY_OF_MONTH, -1);

        int lastDay = c.get(Calendar.DAY_OF_MONTH);
        String lEnd = Common.fmtDate(Lunar2Solar.s2l(Integer.parseInt(sDate[0]), Integer.parseInt(sDate[1]), lastDay));

        //양력 한달 범위내에 음력 두개 달이 걸쳐 있는지...

        boolean isChange = lStart.substring(5).equals(lEnd.substring(5));

        // 음력일정
        query.append("SELECT  ");
        //query.append("    DISTINCT CAST(STRFTIME('%d', " + Schedule.SCHEDULE_LDATE + ", 'localtime') AS INTEGER) DAY  ");
        query.append(" STRFTIME('%Y', '" + month + "-01" + "', 'localtime')||'-'|| substr(" + Schedule.SCHEDULE_LDATE + ", -5) DAY  ");
        query.append("FROM " + Schedule.SCHEDULE_TABLE_NAME + " ");
        query.append("WHERE 1 = 1 ");
        query.append(" AND " + Schedule.ANNIVERSARY + " <> 'Y' ");
        query.append(" AND " + Schedule.LUNARYN + " = 'Y' ");
        if (isChange) {
            query.append(" AND (" + Schedule.SCHEDULE_LDATE + " between '" + lStart + "' and '" + lStart.substring(0, 4) + "-12-31'");
            query.append(" OR " + Schedule.SCHEDULE_LDATE + " between '" + lEnd.substring(0, 4) + "-01-01' and '" + lEnd + "')");
        } else {
            query.append(" AND ( " + Schedule.SCHEDULE_LDATE + " between '" + lStart + "' and '" + lEnd + "')");
        }
        // 음력기념일
        query.append(" UNION ALL  ");
        query.append("SELECT  ");
        //query.append("    DISTINCT CAST(STRFTIME('%d', " + Schedule.SCHEDULE_LDATE + ", 'localtime') AS INTEGER) DAY  ");
        query.append("  STRFTIME('%Y', '" + month + "-01" + "', 'localtime')||'-'|| substr(" + Schedule.SCHEDULE_LDATE + ", -5) DAY  ");
        query.append("FROM " + Schedule.SCHEDULE_TABLE_NAME + " ");
        query.append("WHERE 1 = 1 ");
        query.append(" AND " + Schedule.ANNIVERSARY + " = 'Y' ");
        query.append(" AND " + Schedule.LUNARYN + " = 'Y' ");
        query.append(" AND " + Schedule.SCHEDULE_DATE + " <= STRFTIME('%Y', '" + month + "-01" + "', 'localtime')||'-12-31' ");

        if (isChange) {
            query.append(" AND (substr(" + Schedule.SCHEDULE_LDATE + ",6,5) between '" + lStart.substring(5) + "' and '12-31'");
            query.append(" OR substr(" + Schedule.SCHEDULE_LDATE + ",6,5) between '01-01' and '" + lEnd.substring(5) + "')");
        } else {
            query.append(" AND ( substr(" + Schedule.SCHEDULE_LDATE + ",6,5) between '" + lStart.substring(5) + "' and '" + lEnd.substring(5) + "')");
        }
        query.append(" ORDER BY 1 ");

        return getReadableDatabase().rawQuery(query.toString(), null);

    }

    public Cursor queryExistsDday(String month) {

        StringBuilder query = new StringBuilder();

        query.append("SELECT  ");
        query.append("    DISTINCT CAST(STRFTIME('%d',  DATE(" + Schedule.SCHEDULE_DATE + " ," + Schedule.DDAY_ALARMSIGN + "|| " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime')) AS INTEGER) DAY ");
        query.append("FROM " + Schedule.SCHEDULE_TABLE_NAME + " ");
        query.append("WHERE " + Schedule.DDAY_ALARMYN + " = 1 ");
        query.append("    AND STRFTIME('%Y-%m',  DATE(" + Schedule.SCHEDULE_DATE + " ," + Schedule.DDAY_ALARMSIGN + "|| " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime')) LIKE ?  ");
        query.append("    AND " + Schedule.DDAY_DISPLAYYN + " IN (0, 1, 2)  ");
        query.append("ORDER BY 1 ");

        String selectionArgs[] = new String[] { month + "%" };

        return getReadableDatabase().rawQuery(query.toString(), selectionArgs);

    }

    public Cursor queryDDay() {

        StringBuilder query = new StringBuilder();

        query.append("SELECT ");
        query.append(" " + Schedule.SCHEDULE_TITLE + " ");
        query.append(",DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime') " + Schedule.SCHEDULE_DATE + " ");
        query.append(",cast(JULIANDAY('now', 'localtime') - JULIANDAY(DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime'), 'localtime') as integer) dday  ");
        query.append(" FROM " + Schedule.SCHEDULE_TABLE_NAME + " ");
        query.append(" WHERE " + Schedule.DDAY_DISPLAYYN + " = 1 ");

        String selectionArgs[] = null;

        return getReadableDatabase().rawQuery(query.toString(), selectionArgs);

    }

    public Cursor queryWidget(int kind) {

        StringBuilder query = new StringBuilder();

        query.append("SELECT ");
        query.append(" " + Schedule._ID + " ");
        //query.append(",DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime') " + Schedule.SCHEDULE_DATE + " ");
        query.append(" ," + Schedule.SCHEDULE_DATE);
        query.append(" ," + Schedule.SCHEDULE_TITLE + " ");
        query.append(",cast(JULIANDAY('now', 'localtime') - JULIANDAY(DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime'), 'localtime') as integer) dday  ");
        query.append(" ,'' url ");
        query.append(" FROM " + Schedule.SCHEDULE_TABLE_NAME + " ");
        query.append(" WHERE 1 = 1 ");

        switch (kind) {
            case 0: // dday
                query.append(" AND " + Schedule.DDAY_ALARMYN + " = 1 ");
                query.append(" AND " + Schedule.ANNIVERSARY + " != 'Y' ");
                break;
            case 1: // 기념일
                query.append(" AND " + Schedule.DDAY_ALARMYN + " != 1 ");
                query.append(" AND " + Schedule.ANNIVERSARY + " = 'Y' ");
                break;
            case 2: // 일반일정
                query.append(" AND " + Schedule.DDAY_ALARMYN + " != 1 ");
                query.append(" AND " + Schedule.ANNIVERSARY + " != 'Y' ");
                break;
            case 3: // 모든일정
                break;
            case 4: // 온라인구독일정 
                query = new StringBuilder("select -1 _id, '' schedule_date, '기본 온라인 구독 캘린더' schedule_title, 0 dday, '' url ");

                String calArrays[] = Prefs.getCalendars(mContext);

                for (int i = 0; i < calArrays.length; i++) {
                    String items[] = Common.tokenFn(calArrays[i], "|");
                    if (items.length > 1) {
                        query.append(" union all select -1, '', '" + items[0] + "', 0, '" + items[1] + "' ");
                    }
                }

                break;
            default:
                break;
        }
        if (3 > kind) {
            query.append(" AND " + Schedule.SCHEDULE_DATE + " > '1900-01-01' ");
            query.append(" AND " + Schedule.SCHEDULE_REPEAT + " < 9 ");
        }
        query.append(" ORDER BY " + Schedule.SCHEDULE_DATE + " DESC ");

        String selectionArgs[] = null;

        Log.d(Common.TAG, query.toString());

        return getReadableDatabase().rawQuery(query.toString(), selectionArgs);

    }

    public Cursor queryWidgetByID(Long id) {

        StringBuilder query = new StringBuilder();

        query.append("SELECT ");
        query.append(" " + Schedule.SCHEDULE_TITLE + " ");
        query.append(",case when schedule_repeat = 9 then (case when alarm_lunasolar = 1 then '음력 ' else '' end)|| alarm_date else (" + Schedule.SCHEDULE_DATE + " || ");
        query.append(" case when " + Schedule.LUNARYN + " = 'Y' then '\n(음력 '||substr(" + Schedule.SCHEDULE_LDATE + ",6,5)||')'");
        query.append("  else ' ' end)end as " + Schedule.SCHEDULE_DATE);
        query.append(",cast(JULIANDAY('now', 'localtime') - JULIANDAY(DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime'), 'localtime') as integer) dday  ");
        query.append(", case when " + Schedule.ANNIVERSARY + " = 'Y' then 3 ");
        query.append("       when " + Schedule.DDAY_ALARMYN + " = 1 then 5 ");
        query.append("       when " + Schedule.SCHEDULE_DATE + " <= '1900-01-01' then " + Schedule.ALARM_DAY);
        query.append("       else 6 end as " + Schedule.SCHEDULE_KIND);
        query.append(", " + Schedule.SCHEDULE_REPEAT);
        query.append(", " + Schedule.SCHEDULE_CONTENTS);
        query.append(", case when schedule_repeat < 9 and " + Schedule.ANNIVERSARY + " = 'Y' then cast(strftime('%Y', date('now')) as int) - cast(strftime('%Y', date(schedule_date)) as int) else '' end as years ");
        query.append(" FROM " + Schedule.SCHEDULE_TABLE_NAME + " ");
        query.append(" WHERE 1 = 1 ");
        //query.append(" WHERE " + Schedule.DDAY_DISPLAYYN + " = 1 ");
        query.append(" AND " + Schedule._ID + " = " + id);

        String selectionArgs[] = null;

        Log.d(Common.TAG, query.toString());

        return getReadableDatabase().rawQuery(query.toString(), selectionArgs);

    }

    public Cursor queryAlarm(Calendar c, String lDay) {

        String date = Common.fmtDate(c);
        String time = c.get(Calendar.HOUR_OF_DAY) > 9 ? "" + c.get(Calendar.HOUR_OF_DAY) : "0" + c.get(Calendar.HOUR_OF_DAY);

        String sday = Common.fmtDate(c).substring(5);
        String lday = lDay.substring(4, 6) + "-" + lDay.substring(6);
        int ilday = Integer.parseInt(lDay.substring(6).trim());

        StringBuilder query = new StringBuilder();

        query.append("SELECT ");
        query.append(" " + Schedule._ID + " ");
        query.append("," + Schedule.SCHEDULE_TITLE + " ");
        query.append("," + Schedule.SCHEDULE_CONTENTS + " ");
        query.append(" FROM " + Schedule.SCHEDULE_TABLE_NAME + " ");
        query.append(" WHERE " + Schedule.SCHEDULE_REPEAT + "=1"); // 한번 ㅇㅇ년ㅇㅇ월ㅇㅇ일ㅇ시 ㅇ분
        query.append(" AND " + Schedule.ALARM_DATE + " = '" + date + "'");
        query.append(" AND " + Schedule.ALARM_TIME + " >= '" + time + ":0'");
        query.append(" AND " + Schedule.ALARM_TIME + " <= '" + time + ":59'");
        query.append(" AND " + Schedule.SCHEDULE_CHECK + " != '" + date + "'");

        query.append(" UNION ALL ");

        query.append("SELECT ");
        query.append(" " + Schedule._ID + " ");
        query.append("," + Schedule.SCHEDULE_TITLE + " ");
        query.append("," + Schedule.SCHEDULE_CONTENTS + " ");
        query.append(" FROM " + Schedule.SCHEDULE_TABLE_NAME + " ");
        query.append(" WHERE " + Schedule.SCHEDULE_REPEAT + "=2"); // 매일 ㅇ시 ㅇ분
        query.append(" AND " + Schedule.ALARM_TIME + " >= '" + time + ":0'");
        query.append(" AND " + Schedule.ALARM_TIME + " <= '" + time + ":59'");
        query.append(" AND " + Schedule.SCHEDULE_CHECK + " != '" + date + "'");

        query.append(" UNION ALL ");

        query.append("SELECT ");
        query.append(" " + Schedule._ID + " ");
        query.append("," + Schedule.SCHEDULE_TITLE + " ");
        query.append("," + Schedule.SCHEDULE_CONTENTS + " ");
        query.append(" FROM " + Schedule.SCHEDULE_TABLE_NAME + " ");
        query.append(" WHERE " + Schedule.SCHEDULE_REPEAT + "=3"); // 매주 ㅇ요일 ㅇ시 ㅇ분
        query.append(" AND " + Schedule.ALARM_DAYOFWEEK + " = " + c.get(Calendar.DAY_OF_WEEK));
        query.append(" AND " + Schedule.ALARM_TIME + " >= '" + time + ":0'");
        query.append(" AND " + Schedule.ALARM_TIME + " <= '" + time + ":59'");
        query.append(" AND " + Schedule.SCHEDULE_CHECK + " != '" + date + "'");

        query.append(" UNION ALL ");

        query.append("SELECT ");
        query.append(" " + Schedule._ID + " ");
        query.append("," + Schedule.SCHEDULE_TITLE + " ");
        query.append("," + Schedule.SCHEDULE_CONTENTS + " ");
        query.append(" FROM " + Schedule.SCHEDULE_TABLE_NAME + " ");
        query.append(" WHERE " + Schedule.SCHEDULE_REPEAT + "=4"); // 매월 ㅇ일 ㅇ시 ㅇ분
        query.append(" AND ((" + Schedule.ALARM_LUNASOLAR + " = 0 AND " + Schedule.ALARM_DAY + " = " + c.get(Calendar.DAY_OF_MONTH));
        query.append(") OR (" + Schedule.ALARM_LUNASOLAR + " = 1 AND " + Schedule.ALARM_DAY + " = " + ilday + "))");
        query.append(" AND " + Schedule.ALARM_TIME + " >= '" + time + ":0'");
        query.append(" AND " + Schedule.ALARM_TIME + " <= '" + time + ":59'");
        query.append(" AND " + Schedule.SCHEDULE_CHECK + " != '" + date + "'");

        query.append(" UNION ALL ");

        query.append("SELECT ");
        query.append(" " + Schedule._ID + " ");
        query.append("," + Schedule.SCHEDULE_TITLE + " ");
        query.append("," + Schedule.SCHEDULE_CONTENTS + " ");
        query.append(" FROM " + Schedule.SCHEDULE_TABLE_NAME + " ");
        query.append(" WHERE " + Schedule.SCHEDULE_REPEAT + "=5"); // 매년 ㅇ일 ㅇ시 ㅇ분
        query.append(" AND ((" + Schedule.ALARM_LUNASOLAR + " = 0 AND " + Schedule.ALARM_DATE + " = '" + sday + "'");
        query.append(") OR (" + Schedule.ALARM_LUNASOLAR + " = 1 AND " + Schedule.ALARM_DATE + " = '" + lday + "'))");
        query.append(" AND " + Schedule.ALARM_TIME + " >= '" + time + ":0'");
        query.append(" AND " + Schedule.ALARM_TIME + " <= '" + time + ":59'");
        query.append(" AND " + Schedule.SCHEDULE_CHECK + " != '" + date + "'");

        query.append(" UNION ALL ");

        query.append("SELECT ");
        query.append(" " + Schedule._ID + " ");
        query.append("," + Schedule.SCHEDULE_TITLE + " ");
        query.append("," + Schedule.SCHEDULE_CONTENTS + " ");
        query.append(" FROM " + Schedule.SCHEDULE_TABLE_NAME + " ");
        query.append(" WHERE " + Schedule.SCHEDULE_REPEAT + " = 6 ");
        query.append(" and " + Schedule.SCHEDULE_DATE + " > strftime('%Y-%m-%d', 'now', 'localtime') ");
        query.append(" and cast (julianday('now', 'localtime') -  julianday(" + Schedule.SCHEDULE_DATE + ", 'localtime') as integer) % " + Schedule.ALARM_DAY + "  = 0 ");

        String selectionArgs[] = null;

        Log.d(Common.TAG, "qroup query=" + query.toString());

        return getReadableDatabase().rawQuery(query.toString(), selectionArgs);

    }

    public Cursor queryGroup(String range, String date, boolean isSearch, int operator, String keyword1, String keyword2) {

        StringBuilder query;
        query = new StringBuilder();
        String baseDate = date;

        if ("".equals(baseDate)) {
            Calendar c = Calendar.getInstance();
            c.setFirstDayOfWeek(Calendar.SUNDAY);
            baseDate = Common.fmtDate(c);
        } else if (baseDate.length() < 10) {
            baseDate += "-01";
        }

        int lYear = Integer.parseInt(Lunar2Solar.s2l(baseDate.substring(0, 7) + "-01").substring(0, 4));

        query.append("SELECT " + Schedule._ID);
        query.append(" ,CASE WHEN " + Schedule.SCHEDULE_REPEAT + " = 9 AND " + Schedule.ALARM_LUNASOLAR + " = 1 THEN ");
        query.append("      null ");
        query.append("   WHEN " + Schedule.SCHEDULE_REPEAT + " = 9 AND " + Schedule.ALARM_LUNASOLAR + " = 0 THEN ");
        query.append("      '" + baseDate.substring(0, 4) + "-'||" + Schedule.ALARM_DATE);
        query.append("   ELSE " + Schedule.SCHEDULE_DATE + " END " + Schedule.SCHEDULE_DATE);
        query.append(" ,CASE WHEN " + Schedule.SCHEDULE_REPEAT + " = 9 THEN " + Schedule.SCHEDULE_TITLE + "||'('||" + Schedule.ALARM_DATE + "||')'");
        query.append(" when  " + Schedule.SCHEDULE_REPEAT + " < 9 and " + Schedule.DDAY_ALARMYN + " = 1 THEN ");
        query.append(" substr(" + Schedule.SCHEDULE_TITLE + ", 1, 15) ||'('|| ");
        query.append(" case when cast(JULIANDAY('" + baseDate + "', 'localtime') - JULIANDAY(DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime'), 'localtime') as integer) > 0  ");
        query.append(" then 'D + ' || cast(JULIANDAY('" + baseDate + "', 'localtime') - JULIANDAY(DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime'), 'localtime') as integer)  ");
        query.append(" when cast(JULIANDAY('" + baseDate + "', 'localtime') - JULIANDAY(DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime'), 'localtime') as integer) = 0  ");
        query.append(" then 'D day' else 'D ' ||  cast(JULIANDAY('" + baseDate + "', 'localtime') - JULIANDAY(DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime'), 'localtime') as integer) end ");
        query.append("    ||')' ");
        query.append("    ELSE " + Schedule.SCHEDULE_TITLE + " END " + Schedule.SCHEDULE_TITLE);
        query.append(" ,CASE WHEN " + Schedule.SCHEDULE_REPEAT + " = 9 AND " + Schedule.ALARM_LUNASOLAR + " = 1 THEN ");
        query.append("      '" + lYear + "-'||" + Schedule.ALARM_DATE);
        query.append("   WHEN " + Schedule.SCHEDULE_REPEAT + " = 9 AND " + Schedule.ALARM_LUNASOLAR + " = 0 THEN ");
        query.append("     null ");
        query.append("   ELSE " + Schedule.SCHEDULE_LDATE + " END " + Schedule.SCHEDULE_LDATE);
        query.append(" ,CASE WHEN " + Schedule.SCHEDULE_REPEAT + " = 9 AND " + Schedule.ALARM_LUNASOLAR + " = 1 THEN 'Y' ");
        query.append("       WHEN " + Schedule.SCHEDULE_REPEAT + " = 9 AND " + Schedule.ALARM_LUNASOLAR + " = 0 THEN 'N' ");
        query.append("       ELSE " + Schedule.LUNARYN + " END " + Schedule.LUNARYN);
        query.append(" ,cast(strftime('%w', date(" + Schedule.SCHEDULE_DATE + ")) as integer) as dayindex ");
        query.append(" ,cast(strftime('%W', date(" + Schedule.SCHEDULE_DATE + ")) as integer) as weekindex ");
        query.append(" ," + Schedule.ALARM_DATE);
        query.append(" ," + Schedule.ALARM_TIME);
        query.append(" ," + Schedule.ALARM_LUNASOLAR);
        query.append(" ," + Schedule.ALARM_DAY);
        query.append(" ," + Schedule.ALARM_DAYOFWEEK);
        query.append(" ," + Schedule.SCHEDULE_REPEAT);
        query.append(" ,case when " + Schedule.ANNIVERSARY + " = 'Y' and " + Schedule.SCHEDULE_REPEAT + " < 9 then 'Y' else 'N' end as " + Schedule.ANNIVERSARY);
        query.append(" ,case when " + Schedule.ANNIVERSARY + " = 'Y' then 3 ");
        query.append("       when " + Schedule.DDAY_ALARMYN + " = 1 then 5 ");
        query.append("       when " + Schedule.SCHEDULE_DATE + " <= '1900-01-01' then " + Schedule.ALARM_DAY);
        query.append("       else 6 end as " + Schedule.SCHEDULE_KIND);
        query.append(" FROM " + Schedule.SCHEDULE_TABLE_NAME);
        query.append(" WHERE 1 = 1 ");

        if (!"".equals(date)) {
            String sDate[] = Common.tokenFn(date, "-");

            String lDay = "";
            if (sDate.length > 2) {
                lDay = Common.fmtDate(Lunar2Solar.s2l(Integer.parseInt(sDate[0]), Integer.parseInt(sDate[1]), Integer.parseInt(sDate[2])));
            } else {
                lDay = Common.fmtDate(Lunar2Solar.s2l(Integer.parseInt(sDate[0]), Integer.parseInt(sDate[1]), 1));
            }

            if ("TODAY".equals(range)) {

                String lStart = lDay;
                String lEnd = lDay;

                //양력일정
                query.append(" AND (( " + Schedule.SCHEDULE_DATE + " = '" + date + "' ");
                query.append(" AND " + Schedule.LUNARYN + " <> 'Y' ");
                query.append(" AND " + Schedule.ANNIVERSARY + " <> 'Y' )");

                //양력기념일
                query.append(" or ( " + Schedule.ANNIVERSARY + " = 'Y' ");
                query.append(" AND " + Schedule.LUNARYN + " <> 'Y' ");
                query.append(" AND substr(" + Schedule.SCHEDULE_DATE + ", -5) = '" + date.substring(5) + "' ) ");

                //음력일정
                query.append(" or ( " + Schedule.ANNIVERSARY + " <> 'Y' ");
                query.append(" AND " + Schedule.LUNARYN + " = 'Y' ");
                query.append(" AND " + Schedule.SCHEDULE_LDATE + " between '" + lStart + "' and '" + lEnd + "')");

                // 음력기념일
                query.append(" or( " + Schedule.ANNIVERSARY + " = 'Y' ");
                query.append(" AND " + Schedule.LUNARYN + " = 'Y' ");
                query.append(" AND " + Schedule.SCHEDULE_DATE + " <= STRFTIME('%Y', '" + date.substring(0, 7) + "-01" + "', 'localtime')||'-12-31' ");
                query.append(" AND ( substr(" + Schedule.SCHEDULE_LDATE + ",6,5) between '" + lStart.substring(5) + "' and '" + lEnd.substring(5) + "'))");

                // dday
                query.append(" or ( " + Schedule.DDAY_ALARMYN + " = 1  ");
                query.append("and " + Schedule.DDAY_DISPLAYYN + " = 2  ");
                query.append("or (" + Schedule.DDAY_ALARMYN + " = 1  ");
                query.append("and " + Schedule.DDAY_DISPLAYYN + " in (0, 1)  ");
                query.append("and strftime('%Y-%m-%d', DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime'), 'localtime') = '" + date + "'  ) )");

                if (Prefs.getAnniversary(this.mContext)) {

                    // 시스템 기념일
                    query.append("or ( " + Schedule.SCHEDULE_REPEAT + " = 9 ");
                    query.append("and " + Schedule.SCHEDULE_DATE + " = '1900-01-01' ");
                    query.append("and (   (" + Schedule.ALARM_LUNASOLAR + " = 0 and " + Schedule.ALARM_DATE + " like '" + date.substring(5, 10) + "%') ");
                    query.append("     or (" + Schedule.ALARM_LUNASOLAR + " = 1 and " + Schedule.ALARM_DATE + " = '" + lDay.substring(5) + "') ) ");
                    query.append("and " + Schedule.ALARM_TIME + " = '00:00' ))");

                } else {
                    // 시스템 기념일
                    query.append(") and " + Schedule.SCHEDULE_REPEAT + " < 8 ");
                    query.append("and " + Schedule.SCHEDULE_DATE + " > '1900-01-01' ");
                }

            } else if ("WEEK".equals(range)) {

                Calendar c = Calendar.getInstance();
                c.setFirstDayOfWeek(Calendar.SUNDAY);
                c.set(Integer.parseInt(sDate[0]), Integer.parseInt(sDate[1]) - 1, Integer.parseInt(sDate[2]));
                c.add(Calendar.DAY_OF_MONTH, (c.get(Calendar.DAY_OF_WEEK) - 1) * -1);
                String Start = Common.fmtDate(c);
                c.add(Calendar.DAY_OF_MONTH, 6);
                String End = Common.fmtDate(c);

                c = Calendar.getInstance();
                c.setFirstDayOfWeek(Calendar.SUNDAY);
                c.set(Integer.parseInt(sDate[0]), Integer.parseInt(sDate[1]) - 1, Integer.parseInt(sDate[2]));
                c.add(Calendar.DAY_OF_MONTH, ((c.get(Calendar.DAY_OF_WEEK) - 1) * -1));

                String lStart = Common.fmtDate(Lunar2Solar.s2l(c));
                c.add(Calendar.DAY_OF_MONTH, 6);
                String lEnd = Common.fmtDate(Lunar2Solar.s2l(c));
                boolean isChange = (lStart.substring(5).compareTo(lEnd.substring(5)) > 0);

                //양력일정
                query.append(" AND (( " + Schedule.SCHEDULE_DATE + " between '" + Start + "' and '" + End + "'");
                query.append(" AND " + Schedule.LUNARYN + " <> 'Y' ");
                query.append(" AND " + Schedule.ANNIVERSARY + " <> 'Y' )");

                //양력기념일
                query.append(" or ( substr(" + Schedule.SCHEDULE_DATE + ",6,5) between '" + Start.substring(5, 10) + "' and '" + End.substring(5, 10) + "'");
                query.append(" AND " + Schedule.ANNIVERSARY + " = 'Y' ");
                query.append(" AND " + Schedule.LUNARYN + " <> 'Y' ");
                query.append(" AND " + Schedule.SCHEDULE_DATE + " <= STRFTIME('%Y', '" + date.substring(0, 7) + "-01" + "', 'localtime')||'-12-31') ");

                //음력일정
                query.append(" or ( " + Schedule.ANNIVERSARY + " <> 'Y' ");
                query.append(" AND " + Schedule.LUNARYN + " = 'Y' ");
                query.append(" AND ( " + Schedule.SCHEDULE_LDATE + " between '" + lStart + "' and '" + lEnd + "'))");

                // 음력기념일
                query.append(" or( " + Schedule.ANNIVERSARY + " = 'Y' ");
                query.append(" AND " + Schedule.LUNARYN + " = 'Y' ");
                query.append(" AND " + Schedule.SCHEDULE_DATE + " <= STRFTIME('%Y', '" + date.substring(0, 7) + "-01" + "', 'localtime')||'-12-31' ");

                if (isChange) {
                    query.append(" AND (substr(" + Schedule.SCHEDULE_LDATE + ",6,5) between '" + lStart.substring(5) + "' and '12-31'");
                    query.append(" OR substr(" + Schedule.SCHEDULE_LDATE + ",6,5) between '01-01' and '" + lEnd.substring(5) + "'))");
                } else {
                    query.append(" AND ( substr(" + Schedule.SCHEDULE_LDATE + ",6,5) between '" + lStart.substring(5) + "' and '" + lEnd.substring(5) + "'))");
                }

                // dday
                query.append(" or ( " + Schedule.DDAY_ALARMYN + " = 1  ");
                query.append("and " + Schedule.DDAY_DISPLAYYN + " = 2  ");
                query.append("or (" + Schedule.DDAY_ALARMYN + " = 1  ");
                query.append("and " + Schedule.DDAY_DISPLAYYN + " in (0, 1)  ");
                query.append("and strftime('%Y-%m-%d', DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime'), 'localtime') between '" + Start + "' and '" + End + "' ) )");

                if (Prefs.getAnniversary(this.mContext)) {

                    // 시스템 기념일
                    query.append("or ( " + Schedule.SCHEDULE_REPEAT + " = 9 ");
                    query.append("and " + Schedule.SCHEDULE_DATE + " = '1900-01-01' ");
                    query.append("and ((" + Schedule.ALARM_LUNASOLAR + " = 0 and " + Schedule.ALARM_DATE + " between '" + Start.substring(5, 10) + "' and '" + End.substring(5, 10) + "') ");
                    if (isChange) {
                        query.append("     or (" + Schedule.ALARM_LUNASOLAR + " = 1 and " + Schedule.ALARM_DATE + " between '" + lStart.substring(5) + "' and '" + "12-31')  ");
                        query.append("     or (" + Schedule.ALARM_LUNASOLAR + " = 1 and " + Schedule.ALARM_DATE + " between '" + "01-01' and '" + lEnd.substring(5) + "'))  ");
                    } else {
                        query.append("     or (" + Schedule.ALARM_LUNASOLAR + " = 1 and " + Schedule.ALARM_DATE + " between '" + lStart.substring(5) + "' and '" + lEnd.substring(5) + "'))  ");
                    }
                    query.append("and " + Schedule.ALARM_TIME + " = '00:00' ))");

                } else {
                    // 시스템 기념일
                    query.append(") and " + Schedule.SCHEDULE_REPEAT + " < 8 ");
                    query.append("and " + Schedule.SCHEDULE_DATE + " > '1900-01-01' ");
                }

            } else if ("MONTH".equals(range)) {
                String lStart = Common.fmtDate(Lunar2Solar.s2l(Integer.parseInt(sDate[0]), Integer.parseInt(sDate[1]), 1));
                Calendar c = Calendar.getInstance();
                c.setFirstDayOfWeek(Calendar.SUNDAY);
                c.set(Integer.parseInt(sDate[0]), Integer.parseInt(sDate[1]), 1);
                c.add(Calendar.DAY_OF_MONTH, -1);
                int lastDay = c.get(Calendar.DAY_OF_MONTH);
                String lEnd = Common.fmtDate(Lunar2Solar.s2l(Integer.parseInt(sDate[0]), Integer.parseInt(sDate[1]), lastDay));

                boolean isChange = (lStart.substring(5).compareTo(lEnd.substring(5)) > 0);

                //양력일정
                query.append(" AND (( " + Schedule.SCHEDULE_DATE + " LIKE '" + date.substring(0, 7) + "%' ");
                query.append(" AND " + Schedule.LUNARYN + " <> 'Y' ");
                query.append(" AND " + Schedule.ANNIVERSARY + " <> 'Y' )");

                //양력기념일
                query.append(" or ( substr(" + Schedule.SCHEDULE_DATE + ",6,2) = '" + date.substring(5, 7) + "' ");
                query.append(" AND " + Schedule.ANNIVERSARY + " = 'Y' ");
                query.append(" AND " + Schedule.LUNARYN + " <> 'Y' ");
                query.append(" AND " + Schedule.SCHEDULE_DATE + " <= STRFTIME('%Y', '" + date.substring(0, 7) + "-01" + "', 'localtime')||'-12-31') ");

                //음력일정
                query.append(" or ( " + Schedule.ANNIVERSARY + " <> 'Y' ");
                query.append(" AND " + Schedule.LUNARYN + " = 'Y' ");
                query.append(" AND " + Schedule.SCHEDULE_LDATE + " between '" + lStart + "' and '" + lEnd + "')");

                // 음력기념일
                query.append(" or ( " + Schedule.ANNIVERSARY + " = 'Y' ");
                query.append(" AND " + Schedule.LUNARYN + " = 'Y' ");
                query.append(" AND " + Schedule.SCHEDULE_DATE + " <= STRFTIME('%Y', '" + date.substring(0, 7) + "-01" + "', 'localtime')||'-12-31' ");

                if (isChange) {
                    query.append(" AND (substr(" + Schedule.SCHEDULE_LDATE + ",6,5) between '" + lStart.substring(5) + "' and '12-31'");
                    query.append(" OR substr(" + Schedule.SCHEDULE_LDATE + ",6,5) between '01-01' and '" + lEnd.substring(5) + "' ) ) ");
                } else {
                    query.append(" AND ( substr(" + Schedule.SCHEDULE_LDATE + ",6,5) between '" + lStart.substring(5) + "' and '" + lEnd.substring(5) + "'))");
                }

                // dday
                query.append(" or ( " + Schedule.DDAY_ALARMYN + " = 1  ");
                query.append("and " + Schedule.DDAY_DISPLAYYN + " = 2  ");
                query.append("or (" + Schedule.DDAY_ALARMYN + " = 1  ");
                query.append("and " + Schedule.DDAY_DISPLAYYN + " in (0, 1)  ");
                query.append("and strftime('%Y-%m-%d', DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime'), 'localtime') like '" + date.substring(0, 7) + "%'  ) )");

                if (Prefs.getAnniversary(this.mContext)) {
                    // 시스템 기념일
                    query.append("or ( " + Schedule.SCHEDULE_REPEAT + " = 9 ");
                    query.append("and " + Schedule.SCHEDULE_DATE + " = '1900-01-01' ");
                    query.append("and   ( (" + Schedule.ALARM_LUNASOLAR + " = 0 and " + Schedule.ALARM_DATE + " like '" + date.substring(5, 7) + "%') ");
                    if (isChange) {
                        query.append("     or (" + Schedule.ALARM_LUNASOLAR + " = 1 and " + Schedule.ALARM_DATE + " between '" + lStart.substring(5) + "' and '" + "12-31')  ");
                        query.append("     or (" + Schedule.ALARM_LUNASOLAR + " = 1 and " + Schedule.ALARM_DATE + " between '" + "01-01' and '" + lEnd.substring(5) + "')) ");
                    } else {
                        query.append("     or (" + Schedule.ALARM_LUNASOLAR + " = 1 and " + Schedule.ALARM_DATE + " between '" + lStart.substring(5) + "' and '" + lEnd.substring(5) + "'))  ");
                    }
                    query.append("and " + Schedule.ALARM_TIME + " = '00:00' ))");
                } else {
                    // 시스템 기념일
                    query.append(") and " + Schedule.SCHEDULE_REPEAT + " < 8 ");
                    query.append("and " + Schedule.SCHEDULE_DATE + " > '1900-01-01' ");
                }
            } else {

                if (!Prefs.getAnniversary(this.mContext)) {
                    // 시스템 기념일
                    query.append("and " + Schedule.SCHEDULE_REPEAT + " < 8 ");
                    query.append("and " + Schedule.SCHEDULE_DATE + " > '1900-01-01' ");
                }
            }
        } else if (isSearch) {

            query.append("and (" + Schedule.SCHEDULE_TITLE + "||ifnull(" + Schedule.SCHEDULE_CONTENTS + ",'') like '%" + keyword1 + "%')");

            if (!"".equals(keyword2)) {

                if (0 == operator || 2 == operator)//and
                    query.append(" and ");
                else if (1 == operator)//or
                    query.append(" or ");

                query.append(" (" + Schedule.SCHEDULE_TITLE + "||ifnull(" + Schedule.SCHEDULE_CONTENTS + ",'') ");
                if (2 == operator)
                    query.append(" not ");
                query.append(" like '%" + keyword2 + "%')");

            }
        } else {
            if (!Prefs.getAnniversary(this.mContext)) {
                // 시스템 기념일
                query.append("and " + Schedule.SCHEDULE_REPEAT + " < 8 ");
                query.append("and " + Schedule.SCHEDULE_DATE + " > '1900-01-01' ");
            }
        }

        query.append(" ORDER BY 2 ASC ");

        Log.d(Common.TAG, query.toString());

        return getReadableDatabase().rawQuery(query.toString(), null);

    }

    public Cursor querySearchChild(Long id) {

        String selectionArgs[] = new String[] { id.toString() };

        StringBuilder query = new StringBuilder();

        query.append("SELECT " + Schedule._ID);
        query.append("    ,case when " + Schedule.SCHEDULE_REPEAT + " = 9 then " + Schedule.ALARM_DATE);
        query.append("    else ifnull(" + Schedule.SCHEDULE_CONTENTS + ", '') end as " + Schedule.SCHEDULE_CONTENTS);
        query.append("    ,ifnull(case " + Schedule.SCHEDULE_REPEAT + " ");
        query.append("    when 1 then  " + Schedule.ALARM_DATE + " || ' ' || case when substr(" + Schedule.ALARM_TIME + ",1,2) > '12' then 'PM ' || (cast(substr(" + Schedule.ALARM_TIME + ",  1, 2) as int) - 12)  || substr(" + Schedule.ALARM_TIME + ",  3) else 'AM ' || " + Schedule.ALARM_TIME + " end ");
        query.append("    when 2 then '" + mContext.getResources().getString(R.string.every_day_label) + "' || ' ' || case when substr(" + Schedule.ALARM_TIME + ",1,2) > '12' then 'PM ' || (cast(substr(" + Schedule.ALARM_TIME + ",  1, 2) as int) - 12)  || substr(" + Schedule.ALARM_TIME + ",  3) else 'AM ' || " + Schedule.ALARM_TIME + " end ");
        query.append("    when 3 then '" + mContext.getResources().getString(R.string.every_week_label) + "' || d.dayname || ' '|| case when substr(" + Schedule.ALARM_TIME + ",1,2) > '12' then 'PM ' || (cast(substr(" + Schedule.ALARM_TIME + ",  1, 2) as int) - 12)  || substr(" + Schedule.ALARM_TIME + ",  3) else 'AM ' || " + Schedule.ALARM_TIME + " end ");
        query.append("    when 4 then '" + mContext.getResources().getString(R.string.every_month_label) + "' || ( ");
        query.append("        case when " + Schedule.ALARM_LUNASOLAR + " = 0 then '" + mContext.getResources().getString(R.string.gregorian) + "' else '" + mContext.getResources().getString(R.string.lunar) + "' end)|| ' '|| " + Schedule.ALARM_DAY + " || '" + mContext.getResources().getString(R.string.day_label) + "' ");
        query.append("    when 5 then '" + mContext.getResources().getString(R.string.every_year_label) + "' || ( ");
        query.append("        case when " + Schedule.ALARM_LUNASOLAR + " = 0 then '" + mContext.getResources().getString(R.string.gregorian) + "' else '" + mContext.getResources().getString(R.string.lunar) + "' end)|| ' '|| " + Schedule.ALARM_DATE + " || ' ' || case when substr(" + Schedule.ALARM_TIME + ",1,2) > '12' then 'PM ' || (cast(substr(" + Schedule.ALARM_TIME + ",  1, 2) as int) - 12)  || substr(" + Schedule.ALARM_TIME + ",  3) else 'AM ' || " + Schedule.ALARM_TIME + " end ");
        query.append("    else '' end, '') alarm_detailinfo ");
        query.append("    ,ifnull(case when cast(JULIANDAY('now', 'localtime') - JULIANDAY(DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime'), 'localtime') as integer) < 0   ");
        query.append("          then 'D ' || cast(JULIANDAY('now', 'localtime') - JULIANDAY(DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime'), 'localtime') as integer) || 'day'  ");
        query.append("          when cast (JULIANDAY('now', 'localtime') - JULIANDAY(DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime'), 'localtime') as integer) = 0 then 'D day'  ");
        query.append("          else 'D +' || cast(JULIANDAY('now', 'localtime') - JULIANDAY(DATE(" + Schedule.SCHEDULE_DATE + ", " + Schedule.DDAY_ALARMSIGN + " || " + Schedule.DDAY_ALARMDAY + " ||' DAY', 'localtime'), 'localtime') as integer) || 'day' end, '') " + Schedule.DDAY_DETAILINFO + "  ");
        query.append("    , " + Schedule.SCHEDULE_REPEAT);
        query.append("    , " + Schedule.BIBLE_BOOK + ", " + Schedule.BIBLE_CHAPTER + " ");
        query.append("    , case when " + Schedule.ANNIVERSARY + " = 'Y' then 3 ");
        query.append("       when " + Schedule.DDAY_ALARMYN + " = 1 then 5 ");
        query.append("       when " + Schedule.SCHEDULE_DATE + " <= '1900-01-01' then " + Schedule.ALARM_DAY);
        query.append("       else 6 end as " + Schedule.SCHEDULE_KIND);
        query.append("    ," + Schedule.SCHEDULE_TITLE);
        query.append(" FROM " + Schedule.SCHEDULE_DAYS_JOIN_TABLE);
        query.append(" WHERE _id = ? ");

        Log.d(Common.TAG, "query=" + query.toString());

        return getReadableDatabase().rawQuery(query.toString(), selectionArgs);

    }

    public boolean isClose() {
        if (db != null) {
            return !db.isOpen();
        }
        return false;
    }

    @Override
    public void close() {
        if (db != null) {
            db.close();
        }
        super.close();
    }

    @Override
    public void onDestroy() {
        close();
        super.onDestroy();
    }

}
