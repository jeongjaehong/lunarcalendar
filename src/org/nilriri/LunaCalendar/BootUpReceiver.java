package org.nilriri.LunaCalendar;

import org.nilriri.LunaCalendar.tools.Common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Common.startAlarmNotifyService(context);
    }
}
