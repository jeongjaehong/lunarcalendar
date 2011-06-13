package org.nilriri.LunaCalendar;

import java.util.Calendar;

import org.nilriri.LunaCalendar.tools.Common;
import org.nilriri.LunaCalendar.tools.Lunar2Solar;

import android.content.Context;
import android.widget.DatePicker;

public class DatePickerDialog extends android.app.DatePickerDialog {

    private static final String DAYNAMES[] = { "토", "일", "월", "화", "수", "목", "금", "토" };

    public DatePickerDialog(Context context, OnDateSetListener callBack, int year, int monthOfYear, int dayOfMonth) {
        super(context, callBack, year, monthOfYear, dayOfMonth);
    }

    @Override
    public void updateDate(int year, int monthOfYear, int dayOfMonth) {
        super.updateDate(year, monthOfYear, dayOfMonth);

        changeTitle(year, monthOfYear, dayOfMonth);
    }

    @Override
    public void onDateChanged(DatePicker view, int year, int month, int day) {
        super.onDateChanged(view, year, month, day);
        changeTitle(year, month, day);
    }

    private void changeTitle(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.SUNDAY);
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);

        String title = Common.fmtDate(Lunar2Solar.s2l(c));

        title = "음력 " + title.substring(5).replace("-", "월 ") + "일 ";

        title += "(" + DAYNAMES[c.get(Calendar.DAY_OF_WEEK)] + "요일)";

        this.setTitle(title);

    }

}
