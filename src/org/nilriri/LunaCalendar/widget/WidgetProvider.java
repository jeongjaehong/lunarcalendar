package org.nilriri.LunaCalendar.widget;

import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.nilriri.LunaCalendar.LunarCalendar;
import org.nilriri.LunaCalendar.R;
import org.nilriri.LunaCalendar.dao.ScheduleDaoImpl;
import org.nilriri.LunaCalendar.dao.Constants.Schedule;
import org.nilriri.LunaCalendar.gcal.EventEntry;
import org.nilriri.LunaCalendar.gcal.GoogleUtil;
import org.nilriri.LunaCalendar.tools.Common;
import org.nilriri.LunaCalendar.tools.Lunar2Solar;
import org.nilriri.LunaCalendar.tools.Prefs;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class WidgetProvider extends AppWidgetProvider {

    private static Context mContext;
    private static AppWidgetManager mAppWidgetManager;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(Common.TAG, "onUpdate");
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d(Common.TAG, "onReceive=" + intent);

    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d(Common.TAG, "onDeleted");
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            WidgetConfigure.removePrefData(context, appWidgetIds[i]);
        }
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(//
                new ComponentName("org.nilriri.LunaCalendar", //
                        ".widget.WidgetBroadcastReceiver"), //
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, //
                PackageManager.DONT_KILL_APP);

        super.onEnabled(context);

        Common.srartWidgetRefreshService(context);
    }

    @Override
    public void onDisabled(Context context) {
        Log.d(Common.TAG, "onDisabled");
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(//
                new ComponentName("org.nilriri.LunaCalendar",//
                        ".widget.WidgetBroadcastReceiver"), //
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, //
                PackageManager.DONT_KILL_APP);

        super.onDisabled(context);

        Common.stopWidgetRefreshService(context);
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        mContext = context;
        mAppWidgetManager = appWidgetManager;

        try {

            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {

                AppWidgetProviderInfo wf = appWidgetManager.getAppWidgetInfo(appWidgetId);

                Log.d(Common.TAG, "widget provider=" + wf.provider);

                if (wf.provider.toString().indexOf("org.nilriri.LunaCalendar.widget") > 0) {
                    new ShowOnlineCalendar().execute(appWidgetId);
                }
            }
        } catch (Exception e) {
        }
    }

    private static class ShowOnlineCalendar extends AsyncTask<Integer, Void, Void> {
        private List<EventEntry> todayEvents = new ArrayList<EventEntry>();
        private RemoteViews views;
        private ScheduleDaoImpl dao;
        private int mAppWidgetId;

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(Integer... params) {

            mAppWidgetId = params[0];

            int layout = getWidgetLayout(mContext, mAppWidgetManager, mAppWidgetId);
            views = new RemoteViews(mContext.getPackageName(), layout);

            views.setImageViewResource(R.id.widget_icon, android.R.drawable.stat_notify_sync);
            mAppWidgetManager.updateAppWidget(mAppWidgetId, views);

            Long dataPK = WidgetConfigure.getDataPk(mContext, mAppWidgetId);

            try {

                String mDday_title = "";
                String mDday_msg = "";
                String mDday_content = "";
                String mDday_date = "";
                int kind = 6;
                Bitmap bitmap = null;
                String imguri = null;

                //dataPK 가 0보다 작업경우...  
                // 온라인 일정을 조회한다.
                if (0 > dataPK) {

                    Intent serviceIntent = new Intent(mContext, WidgetRefreshService.class);
                    serviceIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    serviceIntent.putExtra("WidgetId", mAppWidgetId);

                    StringBuilder eventdata = new StringBuilder();
                    String url = WidgetConfigure.getWidgetUrl(mContext, mAppWidgetId);

                    if ("".equals(url)) {
                        url = Prefs.getOnlineCalendar(mContext);
                    } else if (url.indexOf("&start-max=") >= 0) {
                        int pos = url.indexOf("&start-max=");
                        url = url.substring(0, pos - 1);

                        WidgetConfigure.setWidgetUrl(mContext, mAppWidgetId, url);
                    }

                    Calendar c = Calendar.getInstance();

                    StringBuilder where = new StringBuilder("?start-min=");
                    where.append(Common.fmtDate(c));
                    where.append("&start-max=");
                    c.add(Calendar.DAY_OF_MONTH, 1);
                    where.append(Common.fmtDate(c));
                    url += where.toString();

                    GoogleUtil gu = new GoogleUtil(Prefs.getAuthToken(mContext));
                    todayEvents = gu.getEvents(url);
                    // 조회된 온라인 일정이 없는경우.
                    if (todayEvents.size() <= 0) {

                        views.setTextViewText(R.id.text_dday, "OnLine");
                        views.setTextViewText(R.id.text_title2, Common.fmtDate() + "\n일정없음.");
                        views.setViewVisibility(R.id.text_title, View.GONE);
                        views.setViewVisibility(R.id.text_title2, View.VISIBLE);
                        views.setTextViewText(R.id.text_contents, "");
                        views.setImageViewResource(R.id.widget_icon, R.drawable.flag4);

                        views.setOnClickPendingIntent(R.id.widget, //
                                PendingIntent.getService(mContext, mAppWidgetId, serviceIntent, 0));

                    } else {
                        if ("0".equals(todayEvents.get(0).id)) {
                            // 일정조회중 네트워크 오류나 기타 타임아웃같은 오류가 발생했을경우..
                            // 터치 했을때 리프레쉬 가능하도록 설정만 해준다.
                            views.setOnClickPendingIntent(R.id.widget, //
                                    PendingIntent.getService(mContext, mAppWidgetId, serviceIntent, 0));
                        } else {
                            String names[] = new String[todayEvents.size()];
                            String contents[] = new String[todayEvents.size()];

                            for (int i = 0; i < todayEvents.size() && i < 10; i++) {
                                names[i] = todayEvents.get(i).getStartDate().substring(5, 10) + " : " + todayEvents.get(i).title;
                                contents[i] = todayEvents.get(i).content;
                                eventdata.append(names[i]).append("\n");
                                imguri = todayEvents.get(i).getWebContentsLink();
                            }

                            views.setTextViewText(R.id.text_dday, "OnLine");
                            views.setTextViewText(R.id.text_title2, eventdata.toString());
                            views.setViewVisibility(R.id.text_title, View.GONE);
                            views.setViewVisibility(R.id.text_title2, View.VISIBLE);
                            views.setTextViewText(R.id.text_contents, "");

                            // 이미지가 없는경우 기본 아이콘을 사용한다.
                            if ("".equals(imguri)) {
                                views.setImageViewResource(R.id.widget_icon, R.drawable.flag4);

                            } else { // 이미지 정보가 있는경우 온라인 이미지를 아이콘으로 사용한다.
                                URL u = new URL(imguri);
                                bitmap = BitmapFactory.decodeStream(u.openStream());
                                views.setImageViewBitmap(R.id.widget_icon, bitmap);
                            }

                            // 일정의 상세내용이 없는경우...
                            if (contents.length == 0 || contents == null) {

                                views.setOnClickPendingIntent(R.id.widget,//
                                        PendingIntent.getService(mContext, mAppWidgetId, serviceIntent, 0));
                            } else { // 일정의 상세내용이 있는경우 내용에 따라서 작업한다.
                                String eventContent = contents[0] == null ? "" : contents[0];

                                if (eventContent.indexOf("bindex:") >= 0) {

                                    Intent bibleIntent = new Intent();//mContext, LunarCalendar.class);

                                    bibleIntent.setAction("org.nilriri.webbibles.VIEW");
                                    bibleIntent.setType("vnd.org.nilriri/web-bible");

                                    bibleIntent.putExtra("VERSION", 0);
                                    bibleIntent.putExtra("VERSION2", 0);
                                    bibleIntent.putExtra("BOOK", 0);
                                    bibleIntent.putExtra("CHAPTER", 0);
                                    bibleIntent.putExtra("VERSE", 0);
                                    bibleIntent.putExtra("BPLANT", names);
                                    bibleIntent.putExtra("BPLANI", contents);

                                    views.setImageViewResource(R.id.widget_icon, R.drawable.ic_bible);

                                    views.setOnClickPendingIntent(R.id.widget, //
                                            PendingIntent.getActivity(mContext, mAppWidgetId, bibleIntent, 0));
                                } else {

                                    views.setOnClickPendingIntent(R.id.widget, //
                                            PendingIntent.getService(mContext, mAppWidgetId, serviceIntent, 0));

                                }
                            }

                        }
                    }
                } else { // db에서 일정을 조회한다.

                    dao = new ScheduleDaoImpl(mContext, null, Prefs.getSDCardUse(mContext));
                    Cursor cursor = dao.queryWidgetByID(dataPK);
                    if (cursor.moveToNext()) {

                        String D_dayTitle = cursor.getString(cursor.getColumnIndexOrThrow(Schedule.SCHEDULE_TITLE));
                        String D_dayDate = cursor.getString(cursor.getColumnIndexOrThrow(Schedule.SCHEDULE_DATE));
                        int D_Day = cursor.getInt(cursor.getColumnIndexOrThrow("dday"));
                        kind = cursor.getInt(cursor.getColumnIndexOrThrow(Schedule.SCHEDULE_KIND));

                        views.setViewVisibility(R.id.text_title2, View.GONE);

                        switch (kind) {
                            case Common.D_DAY_WIDGET: {//dday
                                if (D_Day == 0) {
                                    mDday_title += "D day";
                                } else if (D_Day > 0) {
                                    mDday_title += "D+" + (D_Day + 1) + mContext.getResources().getString(R.string.day_label) + "";
                                } else {
                                    mDday_title += "D-" + Math.abs(D_Day) + mContext.getResources().getString(R.string.day_label) + "";
                                }

                                if (WidgetConfigure.getReceiver(mContext)) {
                                    Calendar c = Calendar.getInstance();
                                    Time t = new Time();
                                    t.set(c.getTimeInMillis());
                                    mDday_date += "\n" + t.format3339(false).substring(11);

                                } else {
                                    mDday_msg += D_dayTitle.length() >= 18 ? D_dayTitle.substring(0, 18) + "..." : D_dayTitle;
                                    mDday_msg = mDday_msg == null ? "" : mDday_msg;
                                    mDday_date = D_dayDate;
                                }

                                break;
                            }
                            default:
                            case Common.ALLEVENT_WIDGET: {// 모든일정
                                mDday_title = "일정";
                                mDday_msg += D_dayTitle;//.length() >= 18 ? D_dayTitle.substring(0, 18) + "..." : D_dayTitle;

                                AppWidgetProviderInfo wf = mAppWidgetManager.getAppWidgetInfo(mAppWidgetId);

                                Log.d(Common.TAG, "wf.initialLayout=" + wf.initialLayout);
                                Log.d(Common.TAG, "dataPK=" + wf);

                                if (wf.initialLayout == R.layout.widget2x2_transparent) {
                                    views.setViewVisibility(R.id.text_title, View.GONE);
                                    views.setViewVisibility(R.id.text_title2, View.VISIBLE);
                                    mDday_content = mDday_msg + "\n";
                                    mDday_content += cursor.getString(cursor.getColumnIndexOrThrow(Schedule.SCHEDULE_CONTENTS));
                                    Log.d(Common.TAG, "mDday_content=" + mDday_content);
                                }

                                mDday_date = D_dayDate;
                                mDday_msg = mDday_msg == null ? "" : mDday_msg;
                                break;
                            }
                            case Common.ANNIVERSARY_WIDGET: {//기념일
                                mDday_title = "기념일";
                                mDday_msg += D_dayTitle.length() >= 18 ? D_dayTitle.substring(0, 18) + "..." : D_dayTitle;
                                mDday_date = D_dayDate;

                                if (cursor.getInt(cursor.getColumnIndexOrThrow(Schedule.SCHEDULE_REPEAT)) < 9) {
                                    mDday_date += "\n" + cursor.getInt(cursor.getColumnIndexOrThrow("years")) + "주년";
                                }

                                mDday_msg = mDday_msg == null ? "" : mDday_msg;
                                break;
                            }
                        }

                    } else {
                        String daynm[] = { "일", "월", "화", "수", "목", "금", "토" };
                        Calendar c = Calendar.getInstance();
                        mDday_title = daynm[c.get(Calendar.DAY_OF_WEEK) - 1];
                        mDday_title += ", W" + c.get(Calendar.WEEK_OF_YEAR);
                        mDday_msg = Common.fmtDate(c);
                        mDday_date = "음력 : " + Common.fmtDate(Lunar2Solar.s2l(c)).substring(5);
                    }

                    switch (kind) {
                        case Common.ANNIVERSARY_WIDGET:
                            views.setImageViewResource(R.id.widget_icon, R.drawable.flag3);
                            break;
                        case Common.D_DAY_WIDGET:
                            views.setImageViewResource(R.id.widget_icon, R.drawable.dday);
                            break;
                        default:
                        case Common.ALLEVENT_WIDGET:
                            views.setImageViewResource(R.id.widget_icon, R.drawable.pen);
                            break;
                    }
                    views.setTextViewText(R.id.text_dday, mDday_title);
                    views.setTextViewText(R.id.text_title, mDday_msg);
                    views.setTextViewText(R.id.text_title2, mDday_content);
                    views.setTextViewText(R.id.text_contents, mDday_date);

                    cursor.close();
                    Intent intent = new Intent(mContext, LunarCalendar.class).putExtra("DataPk", dataPK);
                    PendingIntent contentIntent = PendingIntent.getActivity(mContext, dataPK.intValue(), intent, 0);
                    views.setOnClickPendingIntent(R.id.widget, contentIntent);

                    dao.close();
                }
            } catch (Exception e) {
                if (dao != null)
                    dao.close();

                views.setViewVisibility(R.id.text_title2, View.GONE);
                views.setViewVisibility(R.id.text_contents, View.GONE);

                views.setViewVisibility(R.id.text_title, View.VISIBLE);
                views.setTextViewText(R.id.text_title, "Refresh error...\n" + e.getMessage());

                cancel(true);
                e.printStackTrace();
            }

            int fontcolor = WidgetConfigure.getFontColor(mContext, mAppWidgetId);
            views.setTextColor(R.id.text_dday, fontcolor);
            views.setTextColor(R.id.text_title, fontcolor);
            views.setTextColor(R.id.text_title2, fontcolor);
            views.setTextColor(R.id.text_contents, fontcolor);

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mAppWidgetManager.updateAppWidget(mAppWidgetId, views);
        }
    }

    private static int getWidgetColor(Context context, int appWidgetId) {
        return WidgetConfigure.getWidgetColor(context, appWidgetId);
    }

    public static int getWidgetLayout(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        System.out.println("---------------------------------------------");
        System.out.println("appWidgetId=" + appWidgetId);
        System.out.println("getWidgetColor(context, appWidgetId)=" + getWidgetColor(context, appWidgetId));

        AppWidgetProviderInfo wf = appWidgetManager.getAppWidgetInfo(appWidgetId);

        System.out.println("wf.minWidth=" + wf.minWidth);
        System.out.println("wf.minHeight=" + wf.minHeight);
        System.out.println("wf.label=" + wf.label);
        System.out.println("---------------------------------------------");

        switch (wf.initialLayout) {

            default:
            case R.layout.widget1x1_transparent:

                switch (getWidgetColor(context, appWidgetId)) {

                    case 0:
                        return R.layout.widget1x1_transparent;
                    case 1:
                        return R.layout.widget1x1_black;
                    case 2:
                        return R.layout.widget1x1_orange;
                    case 3:
                        return R.layout.widget1x1_green;
                    case 4:
                        return R.layout.widget1x1_blue;
                    default:
                        return R.layout.widget1x1_black;
                }

            case R.layout.widget1x2_transparent:

                switch (getWidgetColor(context, appWidgetId)) {

                    case 0:
                        return R.layout.widget1x2_transparent;
                    case 1:
                        return R.layout.widget1x2_black;
                    case 2:
                        return R.layout.widget1x2_orange;
                    case 3:
                        return R.layout.widget1x2_green;
                    case 4:
                        return R.layout.widget1x2_blue;
                    default:
                        return R.layout.widget1x2_black;
                }

            case R.layout.widget2x2_transparent:

                switch (getWidgetColor(context, appWidgetId)) {

                    case 0:
                        return R.layout.widget2x2_transparent;
                    case 1:
                        return R.layout.widget2x2_black;
                    case 2:
                        return R.layout.widget2x2_orange;
                    case 3:
                        return R.layout.widget2x2_green;
                    case 4:
                        return R.layout.widget2x2_blue;
                    default:
                        return R.layout.widget2x2_transparent;
                }

            case R.layout.widget3x1_transparent:

                switch (getWidgetColor(context, appWidgetId)) {

                    case 0:
                        return R.layout.widget3x1_transparent;
                    case 1:
                        return R.layout.widget3x1_black;
                    case 2:
                        return R.layout.widget3x1_orange;
                    case 3:
                        return R.layout.widget3x1_green;
                    case 4:
                        return R.layout.widget3x1_blue;
                    default:
                        return R.layout.widget3x1_black;
                }

            case R.layout.widget4x4_transparent:

                switch (getWidgetColor(context, appWidgetId)) {

                    case 0:
                        return R.layout.widget4x4_transparent;

                    default:
                        return R.layout.widget4x4_transparent;
                }
        }

    }

}
