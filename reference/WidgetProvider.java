/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * A widget provider.  We have a string that we pull from a preference in order to show
 * the configuration settings and the current time when the widget was updated.  We also
 * register a BroadcastReceiver for time-changed and timezone-changed broadcasts, and
 * update then too.
 *
 * <p>See also the following files:
 * <ul>
 *   <li>WidgetConfigure.java</li>
 *   <li>WidgetBroadcastReceiver.java</li>
 *   <li>res/layout/appwidget_configure.xml</li>
 *   <li>res/layout/appwidget_provider.xml</li>
 *   <li>res/xml/appwidget_provider.xml</li>
 * </ul>
 */
public class WidgetProvider extends AppWidgetProvider {
    // log tag
    private static final String TAG = "WidgetProvider";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");
        // For each widget that needs an update, get the text that we should display:
        //   - Create a RemoteViews object for it
        //   - Set the text in the RemoteViews object
        //   - Tell the AppWidgetManager to show that views object for the widget.
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];
            //String titlePrefix = WidgetConfigure.loadTitlePref(context, appWidgetId);
            Long widgetSize = WidgetConfigure.getWidgetSize(context, appWidgetId);
            updateAppWidget(context, appWidgetManager, appWidgetId, widgetSize);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d(TAG, "onDeleted");
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            WidgetConfigure.deleteTitlePref(context, appWidgetIds[i]);
        }
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled");
        // When the first widget is created, register for the TIMEZONE_CHANGED and TIME_CHANGED
        // broadcasts.  We don't want to be listening for these if nobody has our widget active.
        // This setting is sticky across reboots, but that doesn't matter, because this will
        // be called after boot if there is a widget instance for this provider.
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName("org.nilriri.LunarCalendar", ".widget.BroadcastReceiver"), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    @Override
    public void onDisabled(Context context) {
        // When the first widget is created, stop listening for the TIMEZONE_CHANGED and
        // TIME_CHANGED broadcasts.
        Log.d(TAG, "onDisabled");
        //Class clazz = WidgetBroadcastReceiver.class;

        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName("org.nilriri.LunarCalendar", ".widget.BroadcastReceiver"), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Long widgetSize) {
        //    public static void doDisplay(Context context, int display) {
        try {
            Log.e(Common.TAG, "*****************************************");

            Long widgetKind = WidgetConfigure.getWidgetKind(context, appWidgetId);

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), getWidgetLayout(context, widgetSize.intValue()));

            ScheduleDaoImpl dao = new ScheduleDaoImpl(context, null, Prefs.getSDCardUse(context));

            switch (widgetSize.intValue()) {
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

            appWidgetManager.updateAppWidget(new ComponentName(context, WidgetProvider.class), remoteViews);

        } catch (Exception e) {
            Log.d(Common.TAG, "doDisplay error=" + e.getMessage());
        }

    }

    private static int getWidgetLayout(Context context, int widgetSize) {

        switch (widgetSize) {
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

}
