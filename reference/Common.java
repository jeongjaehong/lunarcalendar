package org.nilriri.LunaCalendar.tools;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.nilriri.LunaCalendar.widget.AppWidgetProvider1x1;
import org.nilriri.LunaCalendar.widget.AppWidgetProvider2x2;
import org.nilriri.LunaCalendar.widget.WidgetService;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.format.Time;
import android.util.TimeFormatException;

import com.google.api.client.util.DateTime;

public class Common extends Activity {
    public final static String TAG = "LunarCalendar";

    /** SDK 2.2 ("FroYo") version build number. */
    public static final int FROYO = 8;
    public static final String AUTH_TOKEN_TYPE = "cl";

    public static final int SIZE_1x1 = 11;
    public static final int SIZE_2x2 = 22;
    public static final int SIZE_4x4 = 44;

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

    public static void sendServiceAlarmStart(Context context) {
        Intent intent = new Intent(context, WidgetService.class);
        intent.setAction(Common.ACTION_ALARM_START);

        context.startService(intent);
    }

    public static void sendServiceAlarmStop(Context context) {
        Intent intent = new Intent(context, WidgetService.class);
        intent.setAction(Common.ACTION_ALARM_STOP);

        context.stopService(intent);
    }

    public static void sendRefreshFinish(Context context) {
        Intent finish = new Intent(Common.ACTION_REFRESH_FINISH);
        context.sendBroadcast(finish);

        Intent finishWidget1 = new Intent(context, AppWidgetProvider1x1.class);
        finishWidget1.setAction(Common.ACTION_REFRESH_FINISH);
        context.sendBroadcast(finishWidget1);

        Intent finishWidget2 = new Intent(context, AppWidgetProvider2x2.class);
        finishWidget2.setAction(Common.ACTION_REFRESH_FINISH);
        context.sendBroadcast(finishWidget2);

        Intent finishService = new Intent(context, WidgetService.class);
        finishService.setAction(Common.ACTION_REFRESH_FINISH);
        context.startService(finishService);
    }

    public static void sendWidgetUpdate(Context context) {
        Intent i1 = new Intent(context, AppWidgetProvider1x1.class);
        i1.setAction(Common.ACTION_UPDATE);
        context.sendBroadcast(i1);

        Intent i2 = new Intent(context, AppWidgetProvider2x2.class);
        i2.setAction(Common.ACTION_UPDATE);
        context.sendBroadcast(i2);
    }

    public static String fmtDate(int year, int month, int day) {
        String returnValue = "";
        returnValue = (new StringBuilder()).append(year).append("-").append(month > 9 ? month : "0" + month).append("-").append(day > 9 ? day : "0" + day).toString();
        return returnValue;
    }

    public static DateTime toDateTime(String date) {
        DateTime result = new DateTime(new Date());

        result = DateTime.parseRfc3339(date);

        return result;

    }

    public static DateTime toDateTime(String startDate, int day) {
        Calendar nowCal = Calendar.getInstance();

        nowCal.setTime(new Date(DateTime.parseRfc3339(startDate).value));

        nowCal.add(Calendar.DAY_OF_MONTH, day);

        return DateTime.parseRfc3339(Common.fmtDate(nowCal));

    }

    public static String fmtDate() {
        Calendar c = Calendar.getInstance();
        return fmtDate(c);
    }

    public static String fmtDate(String date) {
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
