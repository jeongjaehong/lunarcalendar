package org.nilriri.LunaCalendar.alarm;

import java.util.Calendar;

import org.nilriri.LunaCalendar.R;
import org.nilriri.LunaCalendar.dao.ScheduleDaoImpl;
import org.nilriri.LunaCalendar.schedule.AlarmViewer;
import org.nilriri.LunaCalendar.tools.Lunar2Solar;
import org.nilriri.LunaCalendar.tools.Music;
import org.nilriri.LunaCalendar.tools.Prefs;
import org.nilriri.LunaCalendar.widget.WidgetConfigure;
import org.nilriri.LunaCalendar.widget.WidgetUtil;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.widget.Toast;

public class CalendarAlarmService extends Service {

    public NotificationManager mNM;

    @Override
    public void onCreate() {
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // 3초후 서비스 자동 종료.
        Thread thr = new Thread(null, mTask, "CalendarAlarmService");
        thr.start();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        try {
            if (Prefs.getAlarmCheck(this)) {// 알람 사용에 체크했으면...
                showNotification();
            }
        } catch (CanceledException e) {
            mNM.cancelAll();
            Toast.makeText(this, R.string.alarm_service_finished, Toast.LENGTH_SHORT).show();
        }
        try {
            if (WidgetConfigure.getReceiver(getBaseContext())) {
                WidgetUtil.refreshWidgets(getBaseContext());
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onDestroy() {
        mNM.cancel(R.string.alarm_service_started);
    }

    Runnable mTask = new Runnable() {
        public void run() {
            long endTime = System.currentTimeMillis() + 1000 * 3;
            while (System.currentTimeMillis() < endTime) {
                synchronized (mBinder) {
                    try {
                        mBinder.wait(endTime - System.currentTimeMillis());
                    } catch (Exception e) {
                    }
                }
            }

            CalendarAlarmService.this.stopSelf();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new Binder() {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            return super.onTransact(code, data, reply, flags);
        }
    };

    private void showNotification() throws CanceledException {

        final Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.SUNDAY);
        String lDay = Lunar2Solar.s2l(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));

        displayNotify(c, lDay);

    }

    private void displayNotify(Calendar c, String lDay) {
        try {
            ScheduleDaoImpl dao = new ScheduleDaoImpl(this, null, Prefs.getSDCardUse(this));

            try {
                Cursor cursor = dao.queryAlarm(c, lDay);

                while (cursor.moveToNext()) {

                    Long id = cursor.getLong(0);
                    CharSequence title = cursor.getString(1);
                    CharSequence content = cursor.getString(2);

                    Notification notification = new Notification(R.drawable.clock, title, System.currentTimeMillis());
                    PendingIntent contentIntent = PendingIntent.getActivity(this, id.intValue(), new Intent(this, AlarmViewer.class).putExtra("id", id), 0);

                    notification.setLatestEventInfo(this, title, content, contentIntent);

                    if (Prefs.getAlarmCheck(this)) {
                        String uri = Prefs.getRingtone(this);
                        if (!"".equals(uri)) {
                            notification.sound = Uri.parse(uri);
                        } else {
                            Music.play(this, R.raw.ding);
                        }

                        if (Prefs.getVibrate(this)) {

                            long[] vibrate = { 0, 100, 200, 300 };
                            notification.vibrate = vibrate;
                        }

                        if (Prefs.getLedlight(this)) {
                            notification.ledARGB = 0xff00ff00;
                            notification.ledOnMS = 300;
                            notification.ledOffMS = 1000;
                            notification.flags = Notification.FLAG_SHOW_LIGHTS;
                        }
                    }

                    mNM.notify(id.intValue(), notification);
                }

                cursor.close();
                dao.close();
            } catch (Exception e) {
                dao.close();
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
