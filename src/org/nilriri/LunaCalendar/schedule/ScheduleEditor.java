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

import org.nilriri.LunaCalendar.DatePickerDialog;
import org.nilriri.LunaCalendar.R;
import org.nilriri.LunaCalendar.RefreshManager;
import org.nilriri.LunaCalendar.dao.ScheduleBean;
import org.nilriri.LunaCalendar.dao.ScheduleDaoImpl;
import org.nilriri.LunaCalendar.tools.Common;
import org.nilriri.LunaCalendar.tools.Lunar2Solar;
import org.nilriri.LunaCalendar.tools.LunarDatePicker;
import org.nilriri.LunaCalendar.tools.Prefs;
import org.nilriri.LunaCalendar.widget.WidgetUtil;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.AdapterView.OnItemSelectedListener;

public class ScheduleEditor extends Activity implements OnClickListener, RefreshManager {

    static final int TIME_DIALOG_ID = 0;
    static final int DATE_DIALOG_ID = 1;

    static final int REQUEST_LUNARDATE = 0;
    static final int REQUEST_REPEATDAY = 1;
    static final int REQUEST_LUNARFULLDATE = 2;

    public static final int MENU_ITEM_DELSCHEDULE = Menu.FIRST;
    public static final int MENU_ITEM_WORKCANCEL = Menu.FIRST + 1;

    private ScheduleDaoImpl dao = null;
    private int activecontrol;
    private boolean isShowDialog = false;
    private Intent resultIntent = null;
    private ScheduleBean scheduleBean = new ScheduleBean();
    private EditText mSchedule_date;
    private EditText mSchedule_ldate;
    private CheckBox mLunaryn;
    private CheckBox mAnniversary;

    private EditText mSchedule_title;
    private EditText mSchedule_conents;
    private Spinner mSchedule_repeat;

    private Spinner mAlarm_lunasolar;
    private EditText mAlarm_date;
    private EditText mAlarm_time;
    private Spinner mAlarm_days;
    private EditText mAlarm_DayofMonth;
    private Spinner mLunaAlarm_day;
    private EditText mAlarm_repeatday;

    private Spinner mSpecialday_alarmyn;
    private EditText mSpecial_alarmday;
    private Spinner mSpecialday_sign;
    private Spinner mSpecialday_displayyn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.schedule_editor);

        ArrayAdapter<CharSequence> adapter;

        mSchedule_date = (EditText) findViewById(R.id.schedule_date);
        mSchedule_ldate = (EditText) findViewById(R.id.schedule_ldate);
        mLunaryn = (CheckBox) findViewById(R.id.check_lunar);
        mAnniversary = (CheckBox) findViewById(R.id.check_anniversary);

        mSchedule_title = (EditText) findViewById(R.id.schedule_title);
        mSchedule_conents = (EditText) findViewById(R.id.schedule_contents);
        mSchedule_repeat = (Spinner) findViewById(R.id.schedule_repeat);

        mAlarm_lunasolar = (Spinner) findViewById(R.id.alarm_lunasolar);
        mAlarm_date = (EditText) findViewById(R.id.alarm_date);
        mAlarm_time = (EditText) findViewById(R.id.alarm_time);
        mAlarm_days = (Spinner) findViewById(R.id.alarm_days);
        mAlarm_DayofMonth = (EditText) findViewById(R.id.alarm_day);
        mAlarm_repeatday = (EditText) findViewById(R.id.alarm_repeatday);
        mLunaAlarm_day = (Spinner) findViewById(R.id.luna_alarm_day);

        mSchedule_date.setOnClickListener(this);
        mAlarm_date.setOnClickListener(this);
        mAlarm_time.setOnClickListener(this);
        mAlarm_DayofMonth.setOnClickListener(this);
        mAlarm_repeatday.setOnClickListener(this);
        mAnniversary.setOnClickListener(this);
        mLunaryn.setOnClickListener(this);
        mSchedule_ldate.setOnClickListener(this);

        adapter = ArrayAdapter.createFromResource(this, R.array.schedule_repeat, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSchedule_repeat.setAdapter(adapter);

        adapter = ArrayAdapter.createFromResource(this, R.array.repeat_lunasolar, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAlarm_lunasolar.setAdapter(adapter);

        adapter = ArrayAdapter.createFromResource(this, R.array.repeat_days, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAlarm_days.setAdapter(adapter);

        adapter = ArrayAdapter.createFromResource(this, R.array.luna_daylist, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mLunaAlarm_day.setAdapter(adapter);

        mSchedule_repeat.setOnItemSelectedListener(new ScheduleRepeatOnItemSelectedListener());
        mAlarm_lunasolar.setOnItemSelectedListener(new LunaSolarOnItemSelectedListener());
        mLunaAlarm_day.setOnItemSelectedListener(new LunaAlarmDayOnItemSelectedListener());
        mAlarm_days.setOnItemSelectedListener(new daysOnItemSelectedListener());

        mSpecialday_alarmyn = (Spinner) findViewById(R.id.specialday_alarmyn);
        mSpecial_alarmday = (EditText) findViewById(R.id.specialday_alarmday);
        mSpecialday_sign = (Spinner) findViewById(R.id.specialday_sign);
        mSpecialday_displayyn = (Spinner) findViewById(R.id.specialday_displayyn);

        adapter = ArrayAdapter.createFromResource(this, R.array.specialday_alarmyn, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpecialday_alarmyn.setAdapter(adapter);

        adapter = ArrayAdapter.createFromResource(this, R.array.specialday_sign, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpecialday_sign.setAdapter(adapter);

        adapter = ArrayAdapter.createFromResource(this, R.array.specialday_displayyn, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpecialday_displayyn.setAdapter(adapter);

        mSpecialday_alarmyn.setOnItemSelectedListener(new alarmynOnItemSelectedListener());
        mSpecialday_sign.setOnItemSelectedListener(new signOnItemSelectedListener());
        mSpecialday_displayyn.setOnItemSelectedListener(new displayynOnItemSelectedListener());

        /*    
        

               mAlarm_date.setOnTouchListener(new myOnTouchListener());
                mAlarm_time.setOnTouchListener(new myOnTouchListener());
                mAlarm_days.setOnTouchListener(new myOnTouchListener());
                mSchedule_date.setOnTouchListener(new myOnTouchListener());
                mSchedule_ldate.setOnTouchListener(new myOnTouchListener());
        */
        /*
                mAlarm_date.setOnFocusChangeListener(new myOnFocusChangeListener());
                mAlarm_time.setOnFocusChangeListener(new myOnFocusChangeListener());
                mAlarm_days.setOnFocusChangeListener(new myOnFocusChangeListener());
                mSchedule_date.setOnFocusChangeListener(new myOnFocusChangeListener());
                mSchedule_ldate.setOnFocusChangeListener(new myOnFocusChangeListener());
                */
    }

    /*
    public class myOnFocusChangeListener implements OnFocusChangeListener {
        public void onFocusChange(View view, boolean flag) {
            if (flag) {
                onClick(view);
            }
        }
    }
    */

    /*
    public class myOnTouchListener implements OnTouchListener {
         public boolean onTouch(View view, MotionEvent event) {            
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                onClick(view);                
            }
            return false;
        }
    }
    */

    public class ScheduleRepeatOnItemSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            scheduleBean.setRepeat(pos);

            ChangeViewOfRepeatMethod(pos);
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing. 
        }

    }

    public class LunaAlarmDayOnItemSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            scheduleBean.setAlarmDay(pos + 1);
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing. 
        }

    }

    public class daysOnItemSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            scheduleBean.setAlarmDays(pos);
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing. 
        }

    }

    public class alarmynOnItemSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            scheduleBean.setDday_alarmyn(pos);

            if (pos > 0) {
                findViewById(R.id.dday_settings).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.dday_settings).setVisibility(View.GONE);
            }
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing. 
        }

    }

    public class signOnItemSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            scheduleBean.setDday_alarmsign(pos);
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing. 
        }

    }

    public class displayynOnItemSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            scheduleBean.setDday_displayyn(pos);
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing. 
        }

    }

    public class LunaSolarOnItemSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            scheduleBean.setLunaSolar(pos);

            switch (scheduleBean.getSchedule_repeat()) {
                case 4:
                    if (pos == 0) { //양력
                        mAlarm_DayofMonth.setText(scheduleBean.getDisplayAlarmDay());

                        mLunaAlarm_day.setVisibility(View.GONE);
                        mAlarm_DayofMonth.setVisibility(View.VISIBLE);
                    } else {//음력
                        mLunaAlarm_day.setSelection(scheduleBean.getAlarm_day() - 1);

                        mAlarm_DayofMonth.setVisibility(View.GONE);
                        mLunaAlarm_day.setVisibility(View.VISIBLE);
                    }
                    break;

                case 5:
                    if (pos == 1) { //음력
                        mAlarm_date.setText(scheduleBean.getDisplayAlarmDate());
                    }
                    break;

                default:
                    mLunaAlarm_day.setVisibility(View.GONE);
                    mAlarm_DayofMonth.setVisibility(View.GONE);
            }

        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing. 
        }

    }

    private void ChangeViewOfRepeatMethod(int pos) {
        if (pos > 0) {
            findViewById(R.id.alarm_settings).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.alarm_settings).setVisibility(View.GONE);
        }

        mAlarm_lunasolar.setVisibility(View.GONE);
        mAlarm_date.setVisibility(View.GONE);
        mAlarm_time.setVisibility(View.GONE);
        mAlarm_days.setVisibility(View.GONE);
        mAlarm_DayofMonth.setVisibility(View.GONE);
        mLunaAlarm_day.setVisibility(View.GONE);
        mAlarm_repeatday.setVisibility(View.GONE);

        switch (pos) {
            case 1: // 한번:날짜, 시간   yyyy.mm.dd.hh.mm
                mAlarm_date.setVisibility(View.VISIBLE);
                mAlarm_time.setVisibility(View.VISIBLE);

                //mAlarm_date.setText(Common.fmtDate());
                mAlarm_date.setText(scheduleBean.getDisplayAlarmDate());
                break;
            case 2: // 매일 : 시간        hh.mm
                mAlarm_time.setVisibility(View.VISIBLE);
                break;
            case 3: // 매주:요일, 시간  days.hh.mm
                mAlarm_days.setVisibility(View.VISIBLE);
                mAlarm_time.setVisibility(View.VISIBLE);
                break;
            case 4: // 매달:음/양, 날짜, 시간 +-.dd.hh.mm
                mAlarm_lunasolar.setVisibility(View.VISIBLE);
                mAlarm_DayofMonth.setVisibility(scheduleBean.getAlarm_lunasolar() == 0 ? View.VISIBLE : View.GONE);
                mLunaAlarm_day.setVisibility(scheduleBean.getAlarm_lunasolar() == 1 ? View.VISIBLE : View.GONE);
                mAlarm_time.setVisibility(View.VISIBLE);

                mAlarm_DayofMonth.setText(Common.fmtDate().substring(8));
                break;
            case 5: // 매년:음/양, 날짜, 시간  +-.mm.dd.hh.mm
                mAlarm_lunasolar.setVisibility(View.VISIBLE);
                mAlarm_date.setVisibility(View.VISIBLE);
                mAlarm_time.setVisibility(View.VISIBLE);

                mAlarm_date.setText(scheduleBean.getDisplayAlarmDate());
                break;
            case 6: // 지정된 일수마다  +-.mm.dd.hh.mm
                mAlarm_repeatday.setVisibility(View.VISIBLE);
                mAlarm_time.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateDisplay() {

        mSchedule_date.setText(scheduleBean.getDisplayDate());
        mSchedule_ldate.setText(Common.fmtDate(Lunar2Solar.s2l(scheduleBean.getYear(), scheduleBean.getMonth(), scheduleBean.getDay())));
        mLunaryn.setChecked(scheduleBean.getLunaryn());
        mAnniversary.setChecked(scheduleBean.getAnniversary());

        mSchedule_title.setText(scheduleBean.getSchedule_title());
        mSchedule_conents.setText(scheduleBean.getSchedule_contents());

        if (scheduleBean.getSchedule_repeat() >= mSchedule_repeat.getCount()) {
            mSchedule_repeat.setSelection(0);
        } else {
            mSchedule_repeat.setSelection(scheduleBean.getSchedule_repeat());
        }

        mAlarm_lunasolar.setSelection(scheduleBean.getAlarm_lunasolar());
        mAlarm_DayofMonth.setText(scheduleBean.getDisplayAlarmDay());
        mAlarm_repeatday.setText(scheduleBean.getDisplayAlarmDay());
        mAlarm_date.setText(scheduleBean.getDisplayAlarmDate());
        mAlarm_time.setText(scheduleBean.getDisplayAlarmTime());
        mAlarm_days.setSelection(scheduleBean.getAlarm_days());
        mLunaAlarm_day.setSelection(scheduleBean.getAlarm_day() - 1);

        mSpecialday_alarmyn.setSelection(scheduleBean.getDday_alarmyn());
        mSpecial_alarmday.setText(scheduleBean.getDisplayDday_alarmday());
        mSpecialday_displayyn.setSelection(scheduleBean.getDday_displayyn());
        mSpecialday_sign.setSelection(scheduleBean.getDisplayDday_alarmsign());
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.schedule_date:
                activecontrol = R.id.schedule_date;
                showDialog(DATE_DIALOG_ID);
                break;
            case R.id.schedule_ldate:
                resultIntent = new Intent(this, LunarDatePicker.class);

                try {
                    String ldate[] = Common.tokenFn(this.mSchedule_ldate.getText().toString().trim(), "-");

                    int year = Integer.parseInt(ldate[0]);
                    int month = Integer.parseInt(ldate[1]);
                    int day = Integer.parseInt(ldate[2]);

                    resultIntent.putExtra("year", year);
                    resultIntent.putExtra("month", month);
                    resultIntent.putExtra("day", day);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                isShowDialog = true;
                startActivityForResult(resultIntent, REQUEST_LUNARFULLDATE);
                break;
            case R.id.alarm_date:
                // 양력일때만 날짜입력박스를 띄운다.                
                if (scheduleBean.getAlarm_lunasolar() == 0) {
                    activecontrol = R.id.alarm_date;
                    showDialog(DATE_DIALOG_ID);
                } else {
                    resultIntent = new Intent(this, LunarDatePicker.class);

                    try {
                        String ldate[] = Common.tokenFn(this.mAlarm_date.getText().toString().trim(), "-");

                        int month = Integer.parseInt(ldate[0]);
                        int day = Integer.parseInt(ldate[1]);

                        resultIntent.putExtra("year", 1900);
                        resultIntent.putExtra("month", month);
                        resultIntent.putExtra("day", day);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    isShowDialog = true;
                    startActivityForResult(resultIntent, REQUEST_LUNARDATE);

                }
                break;
            case R.id.alarm_time:
                activecontrol = R.id.alarm_time;
                showDialog(TIME_DIALOG_ID);
                break;
            case R.id.alarm_day:

                activecontrol = R.id.alarm_day;
                showDialog(DATE_DIALOG_ID);

                break;
            case R.id.alarm_repeatday:

                activecontrol = R.id.alarm_repeatday;
                mAlarm_repeatday.selectAll();

                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        isShowDialog = false;

        switch (requestCode) {

            case REQUEST_LUNARDATE:
            case REQUEST_LUNARFULLDATE:
                if (resultCode == RESULT_OK) {

                    resultIntent = intent;

                } else {
                    resultIntent = null;
                }
                //Resuming 될때 반영한다.
                break;
            case REQUEST_REPEATDAY:
                if (resultCode == RESULT_OK) {
                    //TODO:

                }
                break;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case TIME_DIALOG_ID:
                if (activecontrol == R.id.alarm_time) {
                    return new TimePickerDialog(this, mTimeSetListener, scheduleBean.getAlarmHour24(), scheduleBean.getAlarmMinute(), false);
                }
            case DATE_DIALOG_ID:
                if (activecontrol == R.id.alarm_day) {
                    return new DatePickerDialog(this, mDateSetListener, scheduleBean.getAlarmYear(), scheduleBean.getAlarmMonth() - 1, scheduleBean.getAlarm_day());
                } else if (activecontrol == R.id.alarm_date) {
                    return new DatePickerDialog(this, mDateSetListener, scheduleBean.getAlarmYear(), scheduleBean.getAlarmMonth() - 1, scheduleBean.getAlarmDayofMonth());
                } else {
                    return new DatePickerDialog(this, mDateSetListener, scheduleBean.getYear(), scheduleBean.getMonth() - 1, scheduleBean.getDay());
                }
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case TIME_DIALOG_ID:
                if (activecontrol == R.id.alarm_time) {
                    ((TimePickerDialog) dialog).updateTime(scheduleBean.getAlarmHour24(), scheduleBean.getAlarmMinute());
                }
                break;
            case DATE_DIALOG_ID:
                if (activecontrol == R.id.alarm_day) {
                    ((DatePickerDialog) dialog).updateDate(scheduleBean.getAlarmYear(), scheduleBean.getAlarmMonth() - 1, scheduleBean.getAlarm_day());
                } else if (activecontrol == R.id.alarm_date) {
                    ((DatePickerDialog) dialog).updateDate(scheduleBean.getAlarmYear(), scheduleBean.getAlarmMonth() - 1, scheduleBean.getAlarmDayofMonth());
                } else {
                    ((DatePickerDialog) dialog).updateDate(scheduleBean.getYear(), scheduleBean.getMonth() - 1, scheduleBean.getDay());
                }
                break;
        }
    }

    private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

            String date = Common.fmtDate(year, monthOfYear + 1, dayOfMonth);

            switch (activecontrol) {
                case R.id.schedule_date:
                    scheduleBean.setDate(date);
                    mSchedule_date.setText(scheduleBean.getDisplayDate());

                    String lDate = Common.fmtDate(Lunar2Solar.s2l(scheduleBean.getYear(), scheduleBean.getMonth(), scheduleBean.getDay()));

                    scheduleBean.setLDate(lDate);
                    mSchedule_ldate.setText(lDate);

                    break;
                case R.id.alarm_date:
                    scheduleBean.setAlarmDate(date);
                    mAlarm_date.setText(scheduleBean.getDisplayAlarmDate());
                    break;
                case R.id.alarm_day:
                    scheduleBean.setAlarmDay(dayOfMonth);

                    mAlarm_DayofMonth.setText(scheduleBean.getDisplayAlarmDay());

                    break;
            }

        }
    };

    private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            String time = Common.fmtTime(hourOfDay, minute);

            switch (activecontrol) {
                case R.id.alarm_time:
                    scheduleBean.setAlarmTime(time);
                    mAlarm_time.setText(scheduleBean.getDisplayAlarmTime());

                    break;

            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        if (dao == null) {
            dao = new ScheduleDaoImpl(this, null, Prefs.getSDCardUse(this));
        }

        if (null == resultIntent) {
            initScheduleEditor();
            mSchedule_title.requestFocus();
        } else {

            int year = resultIntent.getIntExtra("year", 1900);
            int month = resultIntent.getIntExtra("month", 1);
            int day = resultIntent.getIntExtra("day", 1);

            String ldate = Common.fmtDate(year, month, day);

            if (1900 == year) {
                scheduleBean.setAlarmDate(ldate.substring(5));
                this.mAlarm_date.setText(ldate.substring(5));

                //mAlarm_date.setText(ldate.substring(5));
            } else {

                String sdate = Lunar2Solar.l2s(year, month, day);

                scheduleBean.setDate(Common.fmtDate(sdate));
                scheduleBean.setLDate(ldate);

                this.mSchedule_date.setText(Common.fmtDate(sdate));
                this.mSchedule_ldate.setText(ldate);
            }

        }
    }

    /**
     * 
     */
    private void initScheduleEditor() {
        Intent intent = getIntent();
        Bundle data = intent.getExtras();
        Long id = (Long) data.get("SID");

        if (id > 0 && dao != null) {
            this.setTitle(getResources().getString(R.string.schedule_modify_label));
            scheduleBean = new ScheduleBean(dao.query(id));
        } else {
            this.setTitle(getResources().getString(R.string.schedule_add_label));

            Calendar c = (Calendar) data.get("STODAY");
            scheduleBean.setId(new Long(0));
            scheduleBean.setDate(c);
        }

        ChangeViewOfRepeatMethod(scheduleBean.getSchedule_repeat());

        updateDisplay();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (!isShowDialog) {

            scheduleBean.setDate(mSchedule_date.getText().toString());
            scheduleBean.setLDate(mSchedule_ldate.getText().toString());
            scheduleBean.setLunarYN(mLunaryn.isChecked());
            scheduleBean.setAnniversary(mAnniversary.isChecked());
            scheduleBean.setTitle(mSchedule_title.getText());
            scheduleBean.setContents(mSchedule_conents.getText());
            scheduleBean.setRepeat(mSchedule_repeat.getSelectedItemPosition());
            scheduleBean.setLunaSolar(mAlarm_lunasolar.getSelectedItemPosition());
            scheduleBean.setAlarmDate(mAlarm_date.getText().toString());
            scheduleBean.setAlarmDays(mAlarm_days.getSelectedItemPosition());

            if (this.mSchedule_repeat.getSelectedItemPosition() == 6) {
                if ("".equals(this.mAlarm_repeatday.getText().toString().trim())) {
                    scheduleBean.setAlarmDay(0);
                } else {
                    scheduleBean.setAlarmDay(Integer.parseInt(this.mAlarm_repeatday.getText().toString().trim()));
                }
            }

            scheduleBean.setDday_alarmday(this.mSpecial_alarmday.getText().toString());

            if (scheduleBean.getId() > 0) {
                scheduleBean.setScheduleCheck("1900-01-01");
                scheduleBean.setUpdated(Common.getTime3339Format());

                dao.syncUpdate(scheduleBean, this);
            } else {
                if (!"".equals(scheduleBean.getSchedule_title().trim())) {
                    dao.syncInsert(scheduleBean, this);
                }
            }
        }

        if (dao != null) {
            dao.close();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        WidgetUtil.refreshWidgets(getBaseContext());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // This is our one standard application action -- inserting a
        // new note into the list.

        if (scheduleBean.getId() > 0) {

            MenuItem item1 = menu.add(0, MENU_ITEM_DELSCHEDULE, 0, R.string.schedule_delete_label);
            item1.setIcon(android.R.drawable.ic_menu_delete);

            MenuItem item2 = menu.add(0, MENU_ITEM_WORKCANCEL, 0, R.string.work_cancel_label);
            item2.setIcon(android.R.drawable.ic_menu_revert);
        } else {
            MenuItem item2 = menu.add(0, MENU_ITEM_WORKCANCEL, 0, R.string.work_cancel_label);
            item2.setIcon(android.R.drawable.ic_menu_revert);
        }

        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        //Intent intent = new Intent(null, getIntent().getData());
        //intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        //menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0, new ComponentName(this, Keypad.class), null, intent, 0, null);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ITEM_DELSCHEDULE: {
                dao.syncDelete(scheduleBean.getId(), null);

                this.finish();

                return true;
            }

            case MENU_ITEM_WORKCANCEL: {

                if (scheduleBean.getId() > 0) {
                    initScheduleEditor();
                    this.mSchedule_title.requestFocus();

                } else {
                    this.mSchedule_title.setText("");

                    scheduleBean.setTitle("");

                    this.finish();
                }

                return true;
            }
            case R.id.settings: {
                startActivity(new Intent(this, Prefs.class));
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final boolean haveItems = true;//this.lunaCalendarView.getBaseline() > 0;

        // If there are any notes in the list (which implies that one of
        // them is selected), then we need to generate the actions that
        // can be performed on the current selection.  This will be a combination
        // of our own specific actions along with any extensions that can be
        // found.
        if (haveItems) {

            // 
            // ... is followed by whatever other actions are available...

            // Give a shortcut to the edit action.

        } else {
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }

        return true;
    }

    public void refresh() {
    }
}
