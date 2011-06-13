package org.nilriri.LunaCalendar.widget;

import java.util.Calendar;

import org.nilriri.LunaCalendar.LunarCalendar;
import org.nilriri.LunaCalendar.R;
import org.nilriri.LunaCalendar.dao.ScheduleDaoImpl;
import org.nilriri.LunaCalendar.tools.Common;
import org.nilriri.LunaCalendar.tools.Lunar2Solar;
import org.nilriri.LunaCalendar.tools.Prefs;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;

public class AppWidgetProvider extends android.appwidget.AppWidgetProvider {

    protected static final int DISPLAY_CONTENT = 1;
    protected static final int DISPLAY_SYNC = 2;
    protected static final int DISPLAY_UPDATE = 3;

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);

        Log.d(Common.TAG, "AppWidgetProvider.onEnabled(" + context + ")");

        Common.sendServiceAlarmStart(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);

        Log.d(Common.TAG, "AppWidgetProvider.onDisabled(" + context + ")");

        Common.sendServiceAlarmStop(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        Log.d(Common.TAG, "AppWidgetProvider.onUpdate(" + context + ", " + appWidgetManager + ", " + appWidgetIds + ")");

        //  doDisplay(context, DISPLAY_UPDATE);

        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];
            //String titlePrefix = AppWidgetConfigure.loadTitlePref(context, appWidgetId);
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Log.d(Common.TAG, "AppWidgetProvider.onReceive(" + context + ", " + intent + ")");

        intent.setClass(context, AppWidgetConfigure.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.setAction("android.appwidget.action.APPWIDGET_CONFIGURE");

        Log.e(Common.TAG, "---------------  Before  -----------------------");

        context.startActivity(intent);

        Log.e(Common.TAG, "---------------  After  -----------------------");

        // doDisplay(context, DISPLAY_CONTENT);

        Log.e(Common.TAG, "intent=" + intent.toString());

    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        //    public static void doDisplay(Context context, int display) {
        try {
            //     Log.d(Common.TAG, "AppWidgetProvider.doDisplay(" + context + ", " + display + ")");

            // AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), getWidgetLayout(context, appWidgetId));

            ScheduleDaoImpl dao = new ScheduleDaoImpl(context, null, Prefs.getSDCardUse(context));

            switch (getWidgetSize(context, appWidgetId).intValue()) {
                case Common.SIZE_1x1:
                case Common.SIZE_2x2:

                    String mDday_title = "";
                    String mDday_msg = "";
                    String mDday_date = "";

                    Cursor cursor = dao.queryDDay();
                    if (cursor.moveToNext()) {
                        String D_dayTitle = cursor.getString(0);
                        String D_dayDate = cursor.getString(1);
                        int D_Day = cursor.getInt(2);

                        if (D_Day == 0) {
                            mDday_title += "D day";
                        } else if (D_Day > 0) {
                            mDday_title += "D+" + D_Day + context.getResources().getString(R.string.day_label) + "";
                        } else {
                            mDday_title += "D-" + Math.abs(D_Day - 1) + context.getResources().getString(R.string.day_label) + "";
                        }

                        mDday_msg += D_dayTitle.length() >= 18 ? D_dayTitle.substring(0, 18) + "..." : D_dayTitle;
                        //mDday_msg += "\n" + D_dayDate;
                        mDday_date = D_dayDate;

                        mDday_msg = mDday_msg == null ? "" : mDday_msg;
                    } else {
                        String daynm[] = { "일", "월", "화", "수", "목", "금", "토" };
                        Calendar c = Calendar.getInstance();
                        mDday_title = daynm[c.get(Calendar.DAY_OF_WEEK) - 1];
                        mDday_title += ", W" + c.get(Calendar.WEEK_OF_YEAR);

                        mDday_msg = Common.fmtDate(c);
                        mDday_date = "음력 : " + Common.fmtDate(Lunar2Solar.s2l(c)).substring(5);
                    }
                    //cursor.close();
                    Log.d(Common.TAG, "mDday_title=" + mDday_title);
                    Log.d(Common.TAG, "mDday_msg=" + mDday_msg);
                    Log.d(Common.TAG, "mDday_date=" + mDday_date);
                    //RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_provider);
                    //views.setTextViewText(R.id.appwidget_title, mDday_title);
                    //views.setTextViewText(R.id.appwidget_text, mDday_msg);

                    Calendar c = Calendar.getInstance();

                    Time t = new Time();
                    t.set(c.getTimeInMillis());

                    Log.e("@@@@@@@@", "now=" + t.format3339(false));

                    mDday_msg += "\n" + t.format3339(false);

                    remoteViews.setTextViewText(R.id.text_dday, mDday_title);
                    remoteViews.setTextViewText(R.id.text_title, mDday_msg);
                    remoteViews.setTextViewText(R.id.text_contents, mDday_date);

                    break;
                case Common.SIZE_4x4:

                    remoteViews.setString(R.id.lunaCalendarView, "setToday", "2011.03.01");

                    break;

            }

            //remoteViews.setImageViewResource(R.id.sync, R.drawable.widget_background_orange);

            remoteViews.setOnClickPendingIntent(R.id.widget, PendingIntent.getActivity(context, 0, new Intent(context, LunarCalendar.class), 0));

            appWidgetManager.updateAppWidget(new ComponentName(context, getWidgetClass(context, appWidgetId)), remoteViews);

        } catch (Exception e) {
            Log.d(Common.TAG, "doDisplay error=" + e.getMessage());
        }

    }

    private static Long getWidgetSize(Context context, int appWidgetId) {

        return AppWidgetConfigure.getWidgetSize(context, appWidgetId);

    }

    private static int getWidgetLayout(Context context, int appWidgetId) {

        switch (getWidgetSize(context, appWidgetId).intValue()) {
            case Common.SIZE_1x1:

                switch (Prefs.getWidgetColor(context)) {

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

            case Common.SIZE_2x2:

                switch (Prefs.getWidgetColor(context)) {

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
            case Common.SIZE_4x4:

                switch (Prefs.getWidgetColor(context)) {

                    case 0:
                        return R.layout.widget4x4_transparent;

                    default:
                        return R.layout.widget4x4_transparent;
                }
        }

        throw new Error();
    }

    private static Class<?> getWidgetClass(Context context, int appWidgetId) {
        switch (getWidgetSize(context, appWidgetId).intValue()) {
            case Common.SIZE_1x1:
                return AppWidgetProvider1x1.class;
            case Common.SIZE_2x2:
                return AppWidgetProvider2x2.class;
            case Common.SIZE_4x4:
                return AppWidgetProvider4x4.class;
        }

        throw new Error();
    }
}
