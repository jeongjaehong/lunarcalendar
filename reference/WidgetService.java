package org.nilriri.LunaCalendar.widget;

import org.nilriri.LunaCalendar.tools.Common;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

public class WidgetService extends android.app.Service {

    protected AlarmManager alarm;
    protected PendingIntent alarmOperation;
    protected PendingIntent alarmAnimationOperation;
    protected int alarmInterval;
    protected int animation;
    protected boolean running;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(Common.TAG, "WidgetService.onCreate()");

        alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, WidgetService.class);
        intent.setAction(Common.ACTION_REFRESH);

        alarmOperation = PendingIntent.getService(this, 0, intent, 0);

        doAlarmStart();

        Intent intentAnimation = new Intent(this, WidgetService.class);

        alarmAnimationOperation = PendingIntent.getService(this, 0, intentAnimation, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(Common.TAG, "WidgetService.onDestroy()");

        doAlarmStop();
    }

    @Override
    public void onStart(final Intent intent, int startId) {
        super.onStart(intent, startId);

        Log.d(Common.TAG, "WidgetService.onStart(" + intent + ", " + startId + ")");

        if (intent == null) {
            //Toast.makeText(getBaseContext(), "앱을 외부 메모리에 설치하면 위젯을 사용할 수 없습니다.", Toast.LENGTH_LONG).show();

            return;
        }

        String action = intent.getAction();

        if (Common.ACTION_ALARM_START.equals(action)) {
            doAlarmStart();
            return;
        }
        if (Common.ACTION_ALARM_STOP.equals(action)) {
            doAlarmStop();
            return;
        }
        if (Common.ACTION_REFRESH.equals(action)) {
            if (running)
                return;

            new Thread(new Runnable() {

                public void run() {
                    Log.d(Common.TAG, "doRefresh();");
                    doRefresh();
                }
            }).start();
            return;
        }
        if (Common.ACTION_UPDATE.equals(action)) {
            Common.sendWidgetUpdate(this);
            return;
        }

        return;
    }

    protected synchronized void doRefresh() {
        Log.d(Common.TAG, "Service.doRefresh() - begin");

        running = true;

        sendBroadcast(new Intent(Common.ACTION_REFRESH_START));

        Intent startWidget1 = new Intent(getBaseContext(), AppWidgetProvider1x1.class);
        startWidget1.setAction(Common.ACTION_REFRESH_START);
        sendBroadcast(startWidget1);

        Intent startWidget2 = new Intent(getBaseContext(), AppWidgetProvider2x2.class);
        startWidget2.setAction(Common.ACTION_REFRESH_START);
        sendBroadcast(startWidget2);

        Common.sendRefreshFinish(this);

        running = false;

        Log.d(Common.TAG, "Service.doRefresh() - end");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected void doAlarmStart() {
        Log.d(Common.TAG, "doAlarmStart");
        alarm.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), Common.WIDGET_REFRESH_INTERVAL, alarmOperation);

    }

    protected void doAlarmStop() {
        Log.d(Common.TAG, "doAlarmStop");

        alarm.cancel(alarmOperation);

    }

}
