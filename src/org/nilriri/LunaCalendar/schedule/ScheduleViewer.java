/*
 * Copyright (C) 2007 The Android Open Source Project
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

package org.nilriri.LunaCalendar.schedule;

import java.util.Calendar;

import org.nilriri.LunaCalendar.R;
import org.nilriri.LunaCalendar.dao.ScheduleBean;
import org.nilriri.LunaCalendar.dao.ScheduleDaoImpl;
import org.nilriri.LunaCalendar.tools.Common;
import org.nilriri.LunaCalendar.tools.Lunar2Solar;
import org.nilriri.LunaCalendar.tools.Prefs;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class ScheduleViewer extends Activity implements OnClickListener {

    static final String DAYNAMES[] = { "토", "일", "월", "화", "수", "목", "금", "토" };
    static final int TIME_DIALOG_ID = 0;
    static final int DATE_DIALOG_ID = 1;
    private ScheduleDaoImpl dao = null;

    ScheduleBean scheduleBean = new ScheduleBean();

    private TextView mSchedule_date;
    private TextView mSchedule_conents;
    private TextView mSchedule_repeat;
    private TextView mSpecialday_displayyn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //dao = new ScheduleDaoImpl(this, null, Prefs.getSDCardUse(this));
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.schedule_viewer);

        mSchedule_date = (TextView) findViewById(R.id.schedule_date);
        mSchedule_conents = (TextView) findViewById(R.id.schedule_contents);
        mSchedule_repeat = (TextView) findViewById(R.id.schedule_repeat);
        mSpecialday_displayyn = (TextView) findViewById(R.id.specialday_displayyn);

        findViewById(R.id.btn_ok).setOnClickListener(this);
        findViewById(R.id.btn_editalarm).setOnClickListener(this);

    }

    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.btn_ok:
                this.finish();
                break;
            case R.id.btn_editalarm:
                Long id = getIntent().getLongExtra("id", -1);

                final Calendar cal = Calendar.getInstance();
                cal.setFirstDayOfWeek(Calendar.SUNDAY);

                Intent intent = new Intent();
                intent.setClass(getBaseContext(), ScheduleEditor.class);
                intent.putExtra("SID", new Long(id));
                intent.putExtra("STODAY", cal);
                startActivity(intent);

                this.finish();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dao != null) {
            dao.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        dao = new ScheduleDaoImpl(this, null, Prefs.getSDCardUse(this));

        scheduleBean = new ScheduleBean(dao.query(getIntent().getLongExtra("id", 0)));

        if (scheduleBean.getId() <= 0) {

            this.finish();
        }

        String title = scheduleBean.getSchedule_title();
        this.setTitle(title);
        ((TextView) findViewById(R.id.title)).setText(title);

        String schedule_date = scheduleBean.getSchedule_date();
        int day = Common.getCalValue(Calendar.DAY_OF_WEEK, schedule_date);
        schedule_date += " " + DAYNAMES[day] + "요일";

        if (scheduleBean.getSchedule_repeat() == 9) {
            if (scheduleBean.getAlarm_lunasolar() == 1) {
                schedule_date = "음력 " + scheduleBean.getAlarm_date();
            } else {
                schedule_date = scheduleBean.getAlarm_date();
            }
        } else {//if (scheduleBean.getSchedule_repeat() != 9) {
            if (scheduleBean.getLunaryn()) {
                String ldate = Common.fmtDate(Lunar2Solar.s2l(schedule_date));
                schedule_date += " (" + ldate.substring(5) + ")";
            }
        }

        mSchedule_date.setText(schedule_date);
        mSchedule_conents.setText(scheduleBean.getSchedule_contents());
        if ("".equals(scheduleBean.getSchedule_contents()))
            mSchedule_conents.setVisibility(View.GONE);

        String repeat[] = getResources().getStringArray(R.array.schedule_repeat);
        String days[] = getResources().getStringArray(R.array.repeat_days);
        String lunarsolar[] = getResources().getStringArray(R.array.repeat_lunasolar);

        String repeatnm = "";
        if (repeat.length > scheduleBean.getSchedule_repeat()) {
            repeatnm = repeat[scheduleBean.getSchedule_repeat()] + " \n";
        }

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
            case 9:
                repeatnm = getResources().getString(R.string.schedule_anniversary_label);
                if (scheduleBean.getAnniversary()) {
                    repeatnm += ", " + getResources().getString(R.string.schedule_holiday_label);
                }

                findViewById(R.id.btn_editalarm).setVisibility(View.GONE);
                break;
        }

        mSchedule_repeat.setText(repeatnm);
        if ("".equals(repeatnm))
            mSchedule_repeat.setVisibility(View.GONE);

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
                ddayinfo += scheduleBean.getDisplayDday_alarmday();
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
                        mDday_msg += " (D - " + Math.abs(D_Day - 1) + "일)";
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
        if ("".equals(ddayinfo))
            mSpecialday_displayyn.setVisibility(View.GONE);

    }

}
