package org.nilriri.LunaCalendar.tools;

import java.util.Calendar;

import org.nilriri.LunaCalendar.R;
import org.nilriri.LunaCalendar.tools.NumberPicker.OnChangedListener;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class LunarDatePicker extends Activity implements OnClickListener {
    private static final String DAYNAMES[] = { "토", "일", "월", "화", "수", "목", "금", "토" };

    private NumberPicker mYear;
    private NumberPicker mMonth;
    private NumberPicker mDay;
    private TextView mTitle;

    private Button mOk;
    private Button mCancel;

    private Intent mIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.lunar_date);

        mYear = (NumberPicker) this.findViewById(R.id.year);
        mMonth = (NumberPicker) this.findViewById(R.id.month);
        mDay = (NumberPicker) this.findViewById(R.id.day);

        mTitle = (TextView) this.findViewById(R.id.alertTitle);

        mYear.setRange(1901, 2043);
        mMonth.setRange(1, 12);
        mDay.setRange(1, 30);

        mYear.setEditable(true);
        mMonth.setEditable(false);
        mDay.setEditable(false);

        mYear.setFocusable(true);
        mMonth.setFocusable(false);
        mDay.setFocusable(false);

        mOk = (Button) this.findViewById(R.id.btn_ok);
        mCancel = (Button) this.findViewById(R.id.btn_cancel);

        mOk.setOnClickListener(this);
        mCancel.setOnClickListener(this);

        mYear.setOnChangeListener(new dateOnChangedListener());
        mMonth.setOnChangeListener(new dateOnChangedListener());
        mDay.setOnChangeListener(new dateOnChangedListener());

        mIntent = this.getIntent();

        Calendar c = Calendar.getInstance();

        Log.e(Common.TAG, "month=" + mIntent.getIntExtra("month", -99));
        Log.e(Common.TAG, "day=" + mIntent.getIntExtra("day", -88));

        int year = mIntent.getIntExtra("year", 1900);
        if (1900 == year) {
            mYear.setVisibility(View.GONE);
        } else {
            mYear.setVisibility(View.VISIBLE);
            mYear.setCurrent(year);
        }
        mMonth.setCurrent(mIntent.getIntExtra("month", c.get(Calendar.MONTH) + 1));
        mDay.setCurrent(mIntent.getIntExtra("day", c.get(Calendar.DAY_OF_MONTH)));

    }

    @Override
    public void onResume() {
        super.onRestart();
        changeTitle(mYear.mCurrent, mMonth.mCurrent, mDay.mCurrent);
    }

    public class dateOnChangedListener implements OnChangedListener {

        public void onChanged(NumberPicker picker, int oldVal, int newVal) {
            changeTitle(mYear.mCurrent, mMonth.mCurrent, mDay.mCurrent);
        }
    };

    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.btn_ok:

                Intent intent = this.getIntent();

                if (mYear.getVisibility() == View.GONE) {
                    intent.putExtra("year", 1900);
                } else {
                    intent.putExtra("year", mYear.getCurrent());
                }
                intent.putExtra("month", mMonth.getCurrent());
                intent.putExtra("day", mDay.getCurrent());

                Log.e(Common.TAG, "month=" + mMonth.getCurrent());
                Log.e(Common.TAG, "day=" + mDay.getCurrent());

                setResult(RESULT_OK, intent);
                finish();

                break;
            case R.id.btn_cancel:

                setResult(RESULT_CANCELED, new Intent());
                finish();

                break;
        }

    }

    private void changeTitle(int year, int month, int day) {

        String sdate = Lunar2Solar.l2s(year, month, day);

        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.SUNDAY);
        c.set(Calendar.YEAR, Integer.parseInt(sdate.substring(0, 4)));
        c.set(Calendar.MONTH, Integer.parseInt(sdate.substring(4, 6)) - 1);
        c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(sdate.substring(6, 8)));

        String title = Common.fmtDate(c);

        title = "양력 " + title.substring(5).replace("-", "월 ") + "일 ";

        title += "(" + DAYNAMES[c.get(Calendar.DAY_OF_WEEK)] + "요일)";

        mTitle.setText(title);

    }

}
