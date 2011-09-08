package org.nilriri.LunaCalendar.tools;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.nilriri.LunaCalendar.alarm.CalendarAlarmService;
import org.nilriri.LunaCalendar.widget.WidgetRefreshService;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeFormatException;

import com.google.api.client.util.DateTime;

public class Common extends Activity {
    public final static String TAG = "LunarCalendar";

    /** SDK 2.2 ("FroYo") version build number. */
    public static final int FROYO = 8;
    public static final String AUTH_TOKEN_TYPE = "cl";

    public static final int SIZE_1x1 = 0;
    public static final int SIZE_2x2 = 1;
    public static final int SIZE_4x4 = 2;

    public static final int D_DAY_WIDGET = 5;
    public static final int ANNIVERSARY_WIDGET = 3;
    public static final int ALLEVENT_WIDGET = 6;
    public static final int ONLINE_WIDGET = 0;

    public static final int ALARM_INTERVAL = 1000 * 60 * 5; // 5분
    public static final int WIDGET_REFRESH_INTERVAL = 1000 * 60 * 60 * 4; // 4시간 

    public static final String ACTION_ALARM_START = "org.nilriri.LunarCalendar.ALARM_START";
    public static final String ACTION_ALARM_STOP = "org.nilriri.LunarCalendar.ALARM_STOP";
    public static final String ACTION_REFRESH = "org.nilriri.LunarCalendar.REFRESH";
    public static final String ACTION_REFRESH_START = "org.nilriri.LunarCalendar.REFRESH_START";
    public static final String ACTION_REFRESH_FINISH = "org.nilriri.LunarCalendar.REFRESH_FINISH";
    public static final String ACTION_UPDATE = "org.nilriri.LunarCalendar.UPDATE";

    public static String formatTime3339(String value) {
        Time t = new Time();
        try {
            if (t.parse3339(value)) {
                return t.format3339(false);
            } else {
                return Common.getTime3339Format(false);
            }
        } catch (TimeFormatException e) {
            e.printStackTrace();
            return Common.getTime3339Format(false);
        }
    }

    public static String getTime3339Format() {
        return getTime3339Format(false);
    }

    public static String getTime3339Format(boolean allDay) {
        Time t = new Time(Time.TIMEZONE_UTC);
        t.setToNow();
        return t.format3339(allDay);
    }

    public static String fmtDate(int year, int month, int day) {
        String returnValue = "";
        returnValue = (new StringBuilder()).append(year).append("-").append(month > 9 ? month : "0" + month).append("-").append(day > 9 ? day : "0" + day).toString();
        return returnValue;
    }

    public static DateTime toDateTime(String date) {
        DateTime result = new DateTime(new Date());

        try {
            result = DateTime.parseRfc3339(date);
        } catch (Exception e) {
            Log.e(Common.TAG, "Date=" + date);
            e.printStackTrace();
        }

        return result;

    }

    public static DateTime toDateTime(String startDate, int day) {
        Calendar nowCal = Calendar.getInstance();

        nowCal.setTime(new Date(DateTime.parseRfc3339(startDate).value));

        nowCal.add(Calendar.DAY_OF_MONTH, day);

        return DateTime.parseRfc3339(Common.fmtDate(nowCal));

    }

    public static int getCalValue(int field, String sdate) {
        Calendar cc = Calendar.getInstance();
        cc.set(Calendar.YEAR, Integer.parseInt(sdate.substring(0, 4)));
        cc.set(Calendar.MONTH, Integer.parseInt(sdate.substring(5, 7)) - 1);
        cc.set(Calendar.DAY_OF_MONTH, Integer.parseInt(sdate.substring(8)));

        return cc.get(field);
    }

    public static String fmtDate() {
        Calendar c = Calendar.getInstance();
        return fmtDate(c);
    }

    public static String fmtDate(String date) {
        date = date.replace("-", "");
        String returnValue = "";
        returnValue = (new StringBuilder()).append(date.substring(0, 4)).append("-").append(date.substring(4, 6)).append("-").append(date.substring(6, 8)).toString();
        return returnValue;
    }

    public static String fmtDate(Calendar c) {
        c.setFirstDayOfWeek(Calendar.SUNDAY);
        return fmtDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static String fmtDateTime(Calendar c) {
        return (c.get(Calendar.YEAR) + "" + (c.get(Calendar.MONTH) + 1) + "" + c.get(Calendar.DAY_OF_MONTH) + "" + c.getTimeInMillis() + "");
    }

    public static String fmtTime(int hour, int minute) {
        String returnValue = "";
        returnValue = (new StringBuilder()).append(hour > 9 ? hour : "0" + hour).append(":").append(minute > 9 ? minute : "0" + minute).toString();
        return returnValue;
    }

    public static String fmtTime() {
        Calendar c = Calendar.getInstance();

        c.setFirstDayOfWeek(Calendar.SUNDAY);
        return fmtTime(c);
    }

    public static String fmtTime(Calendar c) {
        c.setFirstDayOfWeek(Calendar.SUNDAY);
        return fmtTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    public void showMessage() {
        final ProgressDialog dialog = ProgressDialog.show(this, "Title", "Message", true);
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                dialog.dismiss();
            }
        };
        Thread checkUpdate = new Thread() {
            @Override
            public void run() {
                //   
                // YOUR LONG CALCULATION (OR OTHER) GOES HERE   
                //   
                handler.sendEmptyMessage(0);
            }
        };
        checkUpdate.start();
    }

    public void callVibration() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // 1. Vibrate for 1000 milliseconds   
        long milliseconds = 1000;
        v.vibrate(milliseconds);

        // 2. Vibrate in a Pattern with 500ms on, 500ms off for 5 times   
        long[] pattern = { 500, 300 };
        v.vibrate(pattern, 5);
    }

    public boolean locactionServiceAvaiable() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);

        if (providers.size() > 0)
            return true;
        else
            return false;
    }

    public static Rect getExpandRect(Rect rect, int offset) {
        Rect target = new Rect();
        target.set(rect.left - offset, rect.top - (offset + 20), rect.right + offset, rect.bottom + (offset + 20));
        return target;
    }

    public static void srartWidgetRefreshService(Context context) {

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> proceses = activityManager.getRunningAppProcesses();
        boolean isRun = false;

        for (RunningAppProcessInfo process : proceses) {
            if (process.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                if (process.processName.indexOf("WidgetRefreshService") >= 0) {
                    isRun = true;
                    //Log.d(Common.TAG, "isRun=" + process.processName);
                    break;
                } else {
                    //Log.d(Common.TAG, "process=" + process.processName);
                }

            }
        }
        PendingIntent mAlarmSender = PendingIntent.getService(context, 0, new Intent(context, WidgetRefreshService.class), 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (isRun) { //실행중이면..
            if (!Prefs.getAlarmCheck(context)) {// 알람미사용
                // 알람서비스 중지.
                alarmManager.cancel(mAlarmSender);
            }
        } else { // 실행중이 아니면...
            if (Prefs.getAlarmCheck(context)) {// 알람사용이면
                // 알람시작.
                //long firstTime = System.currentTimeMillis();
                long firstTime = SystemClock.elapsedRealtime();
                //alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, Common.ALARM_INTERVAL, mAlarmSender);
                alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstTime, Common.WIDGET_REFRESH_INTERVAL, mAlarmSender);
            }
        }

        for (RunningAppProcessInfo process : proceses) {
            if (process.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                if (process.processName.indexOf("WidgetRefreshService") >= 0) {
                    isRun = true;
                    //Log.d(Common.TAG, "isRun=" + process.processName);
                    break;
                } else {
                    //Log.d(Common.TAG, "process=" + process.processName);
                }

            }
        }

    }

    public static void stopWidgetRefreshService(Context context) {

        PendingIntent mAlarmSender = PendingIntent.getService(context, 0, new Intent(context, WidgetRefreshService.class), 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // 알람서비스 중지.
        alarmManager.cancel(mAlarmSender);
    }

    public static void startAlarmNotifyService(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> proceses = activityManager.getRunningAppProcesses();
        boolean isRun = false;

        for (RunningAppProcessInfo process : proceses) {
            if (process.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                if (process.processName.indexOf("org.nilriri.LunaCalendar:remote") >= 0) {
                    isRun = true;
                    //Log.d(Common.TAG, "isRun=" + process.processName);
                    break;
                } else {
                    //Log.d(Common.TAG, "processName=" + process.processName);
                }
            }
        }
        PendingIntent mAlarmSender = PendingIntent.getService(context, 0, new Intent(context, CalendarAlarmService.class), 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (isRun) { //실행중이면..
            if (!Prefs.getAlarmCheck(context)) {// 알람미사용
                // 알람서비스 중지.
                alarmManager.cancel(mAlarmSender);
            }
        } else { // 실행중이 아니면...
            if (Prefs.getAlarmCheck(context)) {// 알람사용이면
                // 알람시작.
                //long firstTime = System.currentTimeMillis();
                long firstTime = SystemClock.elapsedRealtime();
                //alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, Common.ALARM_INTERVAL, mAlarmSender);
                alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstTime, Common.ALARM_INTERVAL, mAlarmSender);
            }
        }
    }

    public static boolean isConnectNetwork(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        boolean isWifiAvail = info.isAvailable();
        boolean isWifiConn = info.isConnected();

        info = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        boolean isMobileAvail = info.isAvailable();
        boolean isMobileConn = info.isConnected();

        if ((isWifiAvail && isWifiConn) || (isMobileAvail && isMobileConn)) {
            return true;
        } else {
            return false;
        }

    }

    public static boolean isSdPresent() {
        return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    }

    public static String[] tokenFn(String str, String token) {
        StringTokenizer st = null;
        String toStr[] = new String[0];
        int tokenCount = 0;
        int index = 0;
        int len = 0;
        try {
            len = str.length();
            for (int i = 0; i < len; i++)
                if ((index = str.indexOf((new StringBuilder(String.valueOf(token))).append(token).toString())) != -1)
                    str = (new StringBuilder(String.valueOf(str.substring(0, index)))).append(token).append(" ").append(token).append(str.substring(index + 2, str.length())).toString();

            st = new StringTokenizer(str, token);
            tokenCount = st.countTokens();
            toStr = new String[tokenCount];
            for (int i = 0; i < tokenCount; i++)
                toStr[i] = st.nextToken();

        } catch (Exception e) {
            toStr = new String[0];
        }
        return toStr;
    }
}
