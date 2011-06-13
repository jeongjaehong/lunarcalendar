package org.nilriri.LunaCalendar.schedule;

import java.util.Calendar;

import org.nilriri.LunaCalendar.R;
import org.nilriri.LunaCalendar.dao.ScheduleBean;
import org.nilriri.LunaCalendar.dao.ScheduleDaoImpl;
import org.nilriri.LunaCalendar.tools.Common;
import org.nilriri.LunaCalendar.tools.Prefs;

import android.app.Activity;
import android.app.NotificationManager;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class AlarmViewer extends Activity implements OnClickListener {

    static final int TIME_DIALOG_ID = 0;
    static final int DATE_DIALOG_ID = 1;
    private ScheduleDaoImpl dao = null;

    ScheduleBean scheduleBean = new ScheduleBean();
    private NotificationManager mNotificationManager;
    private TextView mSchedule_conents;
    private TextView mSchedule_repeat;
    private TextView mSpecialday_displayyn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.alarm_viewer);

        //dao = new ScheduleDaoImpl(this, null, Prefs.getSDCardUse(this));

        mSchedule_conents = (TextView) findViewById(R.id.schedule_contents);
        mSchedule_repeat = (TextView) findViewById(R.id.schedule_repeat);
        mSpecialday_displayyn = (TextView) findViewById(R.id.specialday_displayyn);

        findViewById(R.id.btn_repeatalarm).setOnClickListener(this);
        findViewById(R.id.btn_stopalarm).setOnClickListener(this);

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

    }

    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.btn_repeatalarm:
                this.finish();
                break;
            case R.id.btn_stopalarm:
                Long id = getIntent().getLongExtra("id", -1);

                final Calendar c = Calendar.getInstance();
                c.setFirstDayOfWeek(Calendar.SUNDAY);

                scheduleBean.setScheduleCheck(Common.fmtDate(c));
                scheduleBean.setUpdated(Common.getTime3339Format());

                dao.localUpdate(scheduleBean);

                mNotificationManager.cancel(id.intValue());
                this.finish();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        dao = new ScheduleDaoImpl(this, null, Prefs.getSDCardUse(this));

        scheduleBean = new ScheduleBean(dao.query(getIntent().getLongExtra("id", 0)));

        if (scheduleBean.getId() <= 0) {
            this.finish();
            return;
        }

        String title = scheduleBean.getSchedule_title();
        title += " (" + scheduleBean.getDisplayDate() + ")";
        this.setTitle(title);
        ((TextView) findViewById(R.id.title)).setText(title);

        mSchedule_conents.setText(scheduleBean.getSchedule_contents());

        String repeat[] = getResources().getStringArray(R.array.schedule_repeat);
        String days[] = getResources().getStringArray(R.array.repeat_days);
        String lunarsolar[] = getResources().getStringArray(R.array.repeat_lunasolar);

        String repeatnm = repeat[scheduleBean.getSchedule_repeat()] + " \n";

        switch (scheduleBean.getSchedule_repeat()) {
            case 1:
                repeatnm += scheduleBean.getDisplayAlarmDate() + ", ";
                repeatnm += scheduleBean.getDisplayAlarmTime();
                break;
            case 2:
                repeatnm += scheduleBean.getDisplayAlarmTime();
                break;
            case 3:
                repeatnm += days[scheduleBean.getAlarm_days()] + ", ";
                repeatnm += scheduleBean.getDisplayAlarmTime();
                break;
            case 4:
                repeatnm += lunarsolar[scheduleBean.getAlarm_lunasolar()] + ", ";
                repeatnm += scheduleBean.getDisplayAlarmDay() + getResources().getString(R.string.schedule_dayname_viewer) + ", ";
                repeatnm += scheduleBean.getDisplayAlarmTime();
                break;
            case 5:
                repeatnm += lunarsolar[scheduleBean.getAlarm_lunasolar()] + ", ";
                repeatnm += scheduleBean.getDisplayAlarmDate() + getResources().getString(R.string.schedule_dayname_viewer) + ", ";
                repeatnm += scheduleBean.getDisplayAlarmTime();
                break;
            case 6:
                repeatnm += lunarsolar[scheduleBean.getAlarm_lunasolar()] + ", ";
                repeatnm += scheduleBean.getDisplayAlarmDay() + getResources().getString(R.string.schedule_dayname2_viewer) + ", ";
                repeatnm += scheduleBean.getDisplayAlarmTime();
                break;
        }

        mSchedule_repeat.setText(repeatnm);

        String ddaykind[] = getResources().getStringArray(R.array.specialday_displayyn);
        String ddayalarmyn[] = getResources().getStringArray(R.array.specialday_alarmyn);
        String ddaysign[] = getResources().getStringArray(R.array.specialday_sign);

        String ddayinfo = "";

        switch (scheduleBean.getDday_alarmyn()) {
            case 0:
                ddayinfo += ddayalarmyn[scheduleBean.getDday_alarmyn()];
                break;
            case 1:
                // d-day를
                ddayinfo += getResources().getString(R.string.dday_info_label) + " ";

                // ㅇㅇㅇㅇ-ㅇㅇ-ㅇㅇ부터
                ddayinfo += scheduleBean.getDisplayDate() + getResources().getString(R.string.dday_fromto_label) + " ";

                // ㅇ일 후로, 일 전으로 설정
                ddayinfo += scheduleBean.getDisplayAlarmDay();
                ddayinfo += ddaysign[scheduleBean.getDisplayDday_alarmsign()];

                String mDday_msg = "";
                Cursor cursor1 = dao.queryWidgetByID(getIntent().getLongExtra("id", 0));
                if (cursor1.moveToNext()) {
                    int D_Day = cursor1.getInt(2);

                    if (D_Day == 0) {
                        mDday_msg += " (D day)";
                    } else if (D_Day > 0) {
                        mDday_msg += " (D + " + D_Day + "일)";
                    } else {
                        mDday_msg += " (D - " + Math.abs(D_Day) + "일)";
                    }
                    mDday_msg = mDday_msg == null ? "" : mDday_msg;
                } else {
                    mDday_msg = "";
                }
                cursor1.close();

                ddayinfo += mDday_msg + "\n";
                ddayinfo += "\n" + getResources().getString(R.string.specialday_displayposigion) + " : ";
                ddayinfo += ddaykind[scheduleBean.getDday_displayyn()];

                break;
        }

        mSpecialday_displayyn.setText(ddayinfo);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dao != null) {
            dao.close();
        }
    }

}
