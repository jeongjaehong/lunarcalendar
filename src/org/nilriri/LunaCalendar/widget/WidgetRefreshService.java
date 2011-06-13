package org.nilriri.LunaCalendar.widget;

import org.nilriri.LunaCalendar.R;
import org.nilriri.LunaCalendar.tools.Common;

import android.app.NotificationManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

public class WidgetRefreshService extends Service {

    public NotificationManager mNM;

    @Override
    public void onCreate() {
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Log.d(Common.TAG, "WidgetRefreshService Create...");

        // 3초후 서비스 자동 종료.
        Thread thr = new Thread(null, mTask, "WidgetAlarmService");
        thr.start();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        try {
            if (Common.isConnectNetwork(WidgetRefreshService.this)) {

                Log.d(Common.TAG, "onStart=" + intent);
                if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
                    //WidgetUtil.refreshWidgets(context);
                    AppWidgetManager awm = AppWidgetManager.getInstance(getBaseContext());

                    int appWidgetId = intent.getIntExtra("WidgetId", AppWidgetManager.INVALID_APPWIDGET_ID);

                    WidgetProvider.updateAppWidget(getBaseContext(), awm, appWidgetId);

                    Log.d(Common.TAG, "Refresh Call=" + appWidgetId);
                } else {
                    WidgetUtil.refreshWidgets(getBaseContext());
                }
            }
        } catch (Exception e) {
        } finally {
            WidgetRefreshService.this.stopSelf();
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
            WidgetRefreshService.this.stopSelf();
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
}
