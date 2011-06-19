package org.nilriri.LunaCalendar;

import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

import org.nilriri.LunaCalendar.dao.Constants.Schedule;
import org.nilriri.LunaCalendar.tools.Common;
import org.nilriri.LunaCalendar.tools.ContactEvent;
import org.nilriri.LunaCalendar.tools.ContactManager;
import org.nilriri.LunaCalendar.tools.Lunar2Solar;
import org.nilriri.LunaCalendar.tools.Prefs;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class LunarCalendarView extends View {

    private static final String SELX = "selX";
    private static final String SELY = "selY";
    private static final String VIEW_STATE = "viewState";
    private static final int ID = 42;
    private static final int mIconSize = 16;
    //private static final int mPenIconSize = 10;
    private static final int mColorPenSize = 20;

    private float tileWidth; // tileWidth of one tile
    private float tileHeight; // tileHeight of one tile
    private int selX; // X index of selection
    private int selY; // Y index of selection
    private int dayofweek;
    private int endofmonth;
    private final Rect selRect = new Rect();
    private final RectF todayRect = new RectF();
    private String mLunadays[];

    public final Rect titleRect = new Rect();
    //public final Rect mPrevYearR = new Rect();
    //public final Rect mPrevMonthR = new Rect();
    //public final Rect mNextYearR = new Rect();
    //public final Rect mNextMonthR = new Rect();

    public String mToDay;

    private final LunarCalendar lunarCalendar;
    //private Cursor scheduleCursor;
    //public Cursor mCur_Schedules;
    //public Cursor mCur_Ddays;
    public String mDday_msg;
    private ContactEvent[] mMonthlyEvent = null;
    private ContactManager mContactManager = null;

    HashMap<Integer, Integer> mScheduleMap;
    HashMap<Integer, Integer> mDdaysMap;
    HashMap<Integer, Integer> mAnniversaryMap;

    private String[] ARRAY_SIPGAN;
    private String[] ARRAY_SIPEJIJI;
    private String[] DAYNAMES;
    private String mCurrentMonth;
    private String mGAPJA = "";

    //private Drawable drawableTitle;
    private Drawable drawableAnimal;

    //private Drawable drawPrevYear;
    //private Drawable drawPrevMonth;
    //private Drawable drawNextMonth;
    //private Drawable drawNextYear;

    public LunarCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.lunarCalendar = (LunarCalendar) context;

        mContactManager = new ContactManager(context);

        ARRAY_SIPGAN = context.getResources().getStringArray(R.array.array_sipgan);
        ARRAY_SIPEJIJI = context.getResources().getStringArray(R.array.array_sipejiji);
        DAYNAMES = context.getResources().getStringArray(R.array.days_label);

        initLunaCalendarView();
    }

    private void initLunaCalendarView() {
        setFocusable(true);

        //Drawable 
        //drawableTitle = getResources().getDrawable(R.drawable.title);

        //drawPrevYear = getResources().getDrawable(R.drawable.prevyear);
        //drawPrevMonth = getResources().getDrawable(R.drawable.prevmonth);
        //drawNextMonth = getResources().getDrawable(R.drawable.nextmonth);
        //drawNextYear = getResources().getDrawable(R.drawable.nextyear);

        //drawPrevMonth = getResources().getDrawable(R.drawable.prevyear);
        //drawNextMonth = getResources().getDrawable(R.drawable.nextyear);

        loadSchduleExistsInfo();
    }

    public void loadSchduleExistsInfo() {

        String queryMonth = Common.fmtDate(lunarCalendar.mYear, lunarCalendar.mMonth + 1, 1);

        Cursor cursor = lunarCalendar.dao.queryExistsSchedule(queryMonth.substring(0, 7));
        mScheduleMap = new HashMap<Integer, Integer>();
        while (cursor.moveToNext()) {
            mScheduleMap.put(cursor.getInt(0), 1);
        }
        cursor.close();

        // 주소록 생일 정보를 조회한다.
        mMonthlyEvent = mContactManager.getContactEvents(queryMonth);

        // 주소록 생일정보 유무를 map에 저장한다.
        for (int i = 0; i < mMonthlyEvent.length; i++) {
            if (mScheduleMap.containsKey(mMonthlyEvent[i].getDay())) {
                mScheduleMap.put(mMonthlyEvent[i].getDay(), 9);
            } else {
                mScheduleMap.put(mMonthlyEvent[i].getDay(), 2);
            }
        }

        Cursor cursor2 = lunarCalendar.dao.queryExistsSchedule2(queryMonth.substring(0, 7));
        while (cursor2.moveToNext()) {
            String date[] = Common.tokenFn(cursor2.getString(0), "-");

            //Log.d(Common.TAG, "Lunar===>" + cursor2.getString(0));

            boolean isChange = (queryMonth.substring(0, 7).compareTo(cursor2.getString(0).substring(0, 7)) >= 0);
            String sDay = "";
            if (!isChange) {
                int preYear = Integer.parseInt(queryMonth.substring(0, 4)) - 1;
                sDay = Lunar2Solar.l2s(preYear + "", date[1], date[2]);
            } else {
                sDay = Lunar2Solar.l2s(date[0], date[1], date[2]);
            }
            //Log.d(Common.TAG, "Solar===>" + sDay);

            int day = Integer.parseInt(sDay.substring(6));
            mScheduleMap.put(day, 1);

            //Log.d(Common.TAG, "Map===>" + mScheduleMap.toString());

        }
        cursor2.close();

        cursor = lunarCalendar.dao.queryExistsDday(queryMonth.substring(0, 7));
        mDdaysMap = new HashMap<Integer, Integer>();
        while (cursor.moveToNext()) {
            mDdaysMap.put(cursor.getInt(0), 1);
        }
        cursor.close();

        cursor = lunarCalendar.dao.queryDDay();
        if (cursor.moveToNext()) {
            String D_dayTitle = cursor.getString(0);
            //String D_dayDate = cursor.getString(1);
            int D_Day = cursor.getInt(2);

            if (D_Day == 0) {
                mDday_msg = " (D day)";
            } else if (D_Day > 0) {
                mDday_msg = " (D+" + (D_Day + 1) + getResources().getString(R.string.day_label) + ")";
            } else {
                mDday_msg = " (D-" + Math.abs(D_Day) + getResources().getString(R.string.day_label) + ")";
            }
            mDday_msg += D_dayTitle.length() >= 8 ? D_dayTitle.substring(0, 8) + "..." : D_dayTitle;

            mDday_msg = mDday_msg == null ? "" : mDday_msg;
        } else {
            mDday_msg = "";
        }
        cursor.close();

        // 새로 바뀐달의 첫날이 몇요일일지 그달의 말일이 몇일인지를 체크한다.
        Calendar calfirst = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"));
        calfirst.set(lunarCalendar.mYear, lunarCalendar.mMonth, 1);
        calfirst.setFirstDayOfWeek(Calendar.SUNDAY);
        endofmonth = calfirst.getActualMaximum(Calendar.DAY_OF_MONTH);
        dayofweek = calfirst.get(Calendar.DAY_OF_WEEK); // 첫날이 몇요일인지?

        // 달이 바뀌면 음력 날짜정보를 계산하여 배열에 담아둔다...
        StringBuffer ldaysource = new StringBuffer();
        ldaysource.append("0").append(",");
        String lDay = Lunar2Solar.s2l(lunarCalendar.mYear, lunarCalendar.mMonth + 1, 1);
        int ilday = Integer.parseInt(lDay.substring(6));
        ldaysource.append(lDay.substring(0, 6)).append(ilday > 9 ? "" + ilday : "0" + ilday);
        for (int day = 2; day <= endofmonth + 1; day++) {

            ilday++;
            if (ilday > 29) {
                lDay = Lunar2Solar.s2l(lunarCalendar.mYear, lunarCalendar.mMonth + 1, day);
                ilday = Integer.parseInt(lDay.substring(6));
            }
            ldaysource.append(",").append(lDay.substring(0, 6)).append(ilday > 9 ? "" + ilday : "0" + ilday);

        }

        //Log.d(Common.TAG, "ldaysource=" + ldaysource);
        mLunadays = Common.tokenFn(ldaysource.toString(), ",");

        // 음력기념일 정보를 보관한다.
        cursor = lunarCalendar.dao.queryExistsAnniversary(queryMonth.substring(0, 7), mLunadays[1], mLunadays[mLunadays.length - 1]);
        mAnniversaryMap = new HashMap<Integer, Integer>();
        while (cursor.moveToNext()) {
            int day = cursor.getInt(0);
            //음력날짜인 경우는 양력날짜로 변환하여 저장한다.
            if (day > 31) {
                String lday = (day > 999 ? "" + day : "0" + day);
                for (int i = 1; i < mLunadays.length; i++) {
                    if (mLunadays[i].substring(4).equals(lday)) {
                        if (mAnniversaryMap.containsKey(i)) {
                            if (mAnniversaryMap.get(i) > cursor.getInt(1)) {
                                mAnniversaryMap.put(i, cursor.getInt(1));
                            }
                        } else {
                            mAnniversaryMap.put(i, cursor.getInt(1));
                        }
                    }
                }

            } else {
                if (mAnniversaryMap.containsKey(day)) {
                    //기념일과 휴일이 중복하여 있을경우에는 휴일정보를 위한 깃발을 우선표시하도록 한다.
                    if (mAnniversaryMap.get(day) > cursor.getInt(1)) {
                        mAnniversaryMap.put(day, cursor.getInt(1));
                    }
                } else {
                    mAnniversaryMap.put(day, cursor.getInt(1));
                }
            }
        }
        cursor.close();

        // 타이틀에 찍을 년,월 정보를 조합한다.
        mCurrentMonth = lunarCalendar.mYear + getResources().getString(R.string.year_label) + (lunarCalendar.mMonth + 1) + getResources().getString(R.string.month_label);

        String lday = this.getLunaday(lunarCalendar.mDay);
        //육십갑자, 띠 계산
        //십간에서는 연도 마지막 숫자
        int lyear = Integer.parseInt(lday.substring(0, 4));
        int num = Integer.parseInt(lday.substring(3, 4));
        //십이지지와 띠는 연도와 12의 나눠서 나오는 나머지값
        int num2 = lyear % 12;

        String gapja = ARRAY_SIPGAN[num] + ARRAY_SIPEJIJI[num2] + getResources().getString(R.string.year_label);

        if (!mGAPJA.equals(gapja)) {
            mGAPJA = gapja;
            int[] ARRAY_DDI = new int[] { R.drawable.animal1, R.drawable.animal2, R.drawable.animal3, R.drawable.animal4, R.drawable.animal5, R.drawable.animal6, R.drawable.animal7, R.drawable.animal8, R.drawable.animal9, R.drawable.animal10, R.drawable.animal11, R.drawable.animal12 };
            drawableAnimal = getResources().getDrawable(ARRAY_DDI[num2]);
        }

        setSelection(getSelX(), getSelY());

    }

    public String getLunaday(int day) {
        if (day < 1) {
            return mLunadays[1];
        } else if (day > endofmonth) {
            return mLunadays[endofmonth];
        } else {

            return mLunadays[day];
        }

    }

    public LunarCalendarView(Context context) {

        super(context);
        this.lunarCalendar = (LunarCalendar) context;
        setFocusable(true);
        setFocusableInTouchMode(true);

        // ...
        setId(ID);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable p = super.onSaveInstanceState();
        Bundle bundle = new Bundle();
        bundle.putInt(SELX, getSelX());
        bundle.putInt(SELY, getSelY());
        bundle.putParcelable(VIEW_STATE, p);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;

        setSelection(bundle.getInt(SELX), bundle.getInt(SELY));

        super.onRestoreInstanceState(bundle.getParcelable(VIEW_STATE));
        return;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        setTileWidth(w / 7f);
        setTileHeight(h / 7f);
        getRect(getSelX(), getSelY(), selRect);
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        Paint background = new Paint();
        background.setColor(getResources().getColor(R.color.cal_background));
        canvas.drawRect(0, 0, getWidth(), getHeight(), background);

        // Define colors for the grid lines
        Paint dark = new Paint();
        dark.setColor(getResources().getColor(R.color.cal_dark));

        Paint hilite = new Paint();
        hilite.setColor(getResources().getColor(R.color.cal_hilite));

        Paint light = new Paint();
        light.setColor(getResources().getColor(R.color.cal_light));

        // 세로줄
        for (int i = 0; i < 7; i++) {
            canvas.drawLine(i * getTileWidth(), getTileHeight() - (getTileHeight() * 0.5f), i * getTileWidth(), 7 * getTileHeight(), light);
            canvas.drawLine(i * getTileWidth() + 1, getTileHeight() - (getTileHeight() * 0.5f), i * getTileWidth() + 1, 7 * getTileHeight(), hilite);
        }

        // 가로줄
        canvas.drawLine(0, (getTileHeight() * 0.5f), getWidth(), (getTileHeight() * 0.5f), light);
        canvas.drawLine(0, (getTileHeight() * 0.5f) + 1, getWidth(), (getTileHeight() * 0.5f) + 1, hilite);
        for (int i = 1; i < 7; i++) {
            canvas.drawLine(0, i * getTileHeight(), getWidth(), i * getTileHeight(), light);
            canvas.drawLine(0, i * getTileHeight() + 1, getWidth(), i * getTileHeight() + 1, hilite);
        }
        canvas.drawLine(0, 7 * getTileHeight() - 2, getWidth(), 7 * getTileHeight() - 2, light);
        canvas.drawLine(0, 7 * getTileHeight() - 1, getWidth(), 7 * getTileHeight() - 1, hilite);

        // Draw the numbers...
        // Define color and style for numbers
        Paint foreground = new Paint(Paint.ANTI_ALIAS_FLAG);
        foreground.setColor(getResources().getColor(R.color.cal_foreground));
        foreground.setStyle(Style.FILL);
        foreground.setTextSize(getTileHeight() * 0.45f);
        foreground.setTextScaleX(getTileWidth() / getTileHeight());
        //foreground.setTextSkewX(2);
        foreground.setTextAlign(Paint.Align.CENTER);

        // Draw the number in the center of the tile
        FontMetrics fm = foreground.getFontMetrics();
        // Centering in X: use alignment (and X at midpoint)
        float x = getTileHeight() / 3 - (fm.ascent + fm.descent) / 2;
        // Centering in Y: measure ascent/descent first
        float y = getTileWidth() / 2;

        int day = 1;

        RectF r = new RectF();

        titleRect.set(0, 0, (int) getTileWidth() * 2, (int) getTileHeight());

        Rect headR = new Rect();

        //headR.set(-50, (int) (getTileHeight() * 0.5f), getWidth() + 50, (int) getTileHeight());
        headR.set(0, 0, getWidth(), (int) (getTileHeight() * 0.5f) + 1);
        // Drawable drawableTitle = getResources().getDrawable(R.drawable.widget_background_blue);
        Drawable drawableTitle = getResources().getDrawable(R.drawable.background);
        drawableTitle.setBounds(headR);
        drawableTitle.setAlpha(120);
        drawableTitle.draw(canvas);

        headR.set(0, (int) (getTileHeight() * 0.5f), getWidth(), getHeight());
        drawableTitle = getResources().getDrawable(R.drawable.background_top);
        drawableTitle.setBounds(headR);
        drawableTitle.setAlpha(60);
        drawableTitle.draw(canvas);

        // 띠별 동물아이콘 그리기
        // TODO:
        Rect all = new Rect();
        all.set(getWidth() - (getWidth() / 7), getHeight() - (getHeight() / 6), getWidth() - 3, getHeight() - 1);
        //Drawable drawableAnimal = getResources().getDrawable(ARRAY_DDI[num2]);
        drawableAnimal.setBounds(all);
        drawableAnimal.setAlpha(100);
        drawableAnimal.draw(canvas);

        String lday = this.getLunaday(lunarCalendar.mDay);
        lday = "(" + mGAPJA + lday.substring(4, 6) + "." + lday.substring(6) + ")";

        // 년월과 함께 음력 날짜를 표시한다.
        foreground.setTextSize(getTileHeight() * 0.380f);
        //canvas.drawText(mCurrentMonth, (3 * getTileWidth() + y), (0 * getTileHeight() + (x * 2.0f)), foreground);
        canvas.drawText(mCurrentMonth, (getTileWidth() + (y * 0.2f)), (0 * getTileHeight() + (x * 0.9f)), foreground);
        foreground.setTextSize(getTileHeight() * 0.27f);
        canvas.drawText(lday, (3.1f * getTileWidth() + (y * 0.2f)), (0 * getTileHeight() + (x * 0.8f)), foreground);

        // 화면상단에 표시할 D-Day가 있으면 표시한다.        

        if (mDday_msg != null && !"".equals(mDday_msg)) {
            foreground.setTextSize(getTileHeight() * 0.27f);
            foreground.setColor(getResources().getColor(R.color.orange));
            canvas.drawText(mDday_msg, (5.15f * getTileWidth() + y), (0 * getTileHeight() + (x * 0.8f)), foreground);
        }

        //이전다음버튼
        int iconLeft = (int) (getTileWidth() * 0.5f);
        int iconTop = (int) (getTileHeight() * 0.6f);
        //mPrevYearR.set(iconLeft, iconTop, iconLeft + 40, iconTop + 40);
        //drawPrevYear.setBounds(mPrevYearR);
        //drawPrevYear.draw(canvas);

        //iconLeft = (int) (1 * getTileWidth() + (getTileWidth() * 0.2f));
        //Drawable drawPrevMonth = getResources().getDrawable(R.drawable.prevmonth);
        //mPrevMonthR.set(iconLeft, iconTop, iconLeft + 40, iconTop + 40);
        //drawPrevMonth.setBounds(mPrevMonthR);
        //drawPrevMonth.draw(canvas);

        //iconLeft = (int) (5 * getTileWidth() + (getTileWidth() * 0.2f));
        //iconLeft = (int) (5 * getTileWidth() + (getTileWidth() * 0.6f));
        //Drawable drawNextMonth = getResources().getDrawable(R.drawable.nextmonth);
        //mNextMonthR.set(iconLeft, iconTop, iconLeft + 40, iconTop + 40);
        //drawNextMonth.setBounds(mNextMonthR);
        //drawNextMonth.draw(canvas);

        //iconLeft = (int) (6 * getTileWidth() + (getTileWidth() * 0.2f));
        //mNextYearR.set(iconLeft, iconTop, iconLeft + 40, iconTop + 40);
        //drawNextYear.setBounds(mNextYearR);
        //drawNextYear.draw(canvas);

        // 일,월,화,수....
        for (int dayname = 0; dayname < 7; dayname++) {
            if (dayname == 0) {
                foreground.setColor(getResources().getColor(R.color.cal_sunday));
            } else if (dayname == 6) {
                foreground.setColor(getResources().getColor(R.color.cal_satday));
            } else {
                foreground.setColor(getResources().getColor(R.color.cal_normalday));
            }
            foreground.setTextSize(getTileHeight() * 0.30f);
            canvas.drawText(DAYNAMES[dayname], dayname * getTileWidth() + y, 0 * getTileHeight() + (x * 1.8f), foreground);
        }

        Drawable drawableICON;
        for (int week = 1; week < 9; week++) {
            for (int dayname = 0; dayname < 7; dayname++) {
                if ((week == 1 && dayname < dayofweek - 1) || day > endofmonth) {
                    canvas.drawText(" ", dayname * getTileWidth() + y, week * getTileHeight() + x, foreground);
                } else {

                    if (getSelY() == 0 || lunarCalendar.mDay == day) {
                        this.setSelX(dayname);
                        this.setSelY(week);
                        getRect(getSelX(), getSelY(), selRect);
                        getContractRect(getSelX(), getSelY(), todayRect, 2);
                    }

                    // ToDay Box
                    if (mToDay.equals(lunarCalendar.mYear + "." + lunarCalendar.mMonth + "." + day)) {
                        Rect todayRect = new Rect();
                        getRect(dayname, week, todayRect);
                        drawableICON = getResources().getDrawable(R.drawable.today);
                        drawableICON.setBounds(todayRect);
                        drawableICON.setAlpha(200);
                        drawableICON.draw(canvas);
                    }

                    // 등록된 스케쥴이나 D-day정보가 있는지 확인한다.
                    if (mScheduleMap.containsKey(day) || mDdaysMap.containsKey(day)) {

                        getRect(dayname, week, r);

                        if (mScheduleMap.containsKey(day) && mDdaysMap.containsKey(day)) {
                            drawableICON = getResources().getDrawable(R.drawable.dpen);
                        } else if (mScheduleMap.containsKey(day)) {

                            if (mScheduleMap.get(day) == 2) {
                                drawableICON = getResources().getDrawable(R.drawable.contactevent);

                            } else if (mScheduleMap.get(day) == 9) {

                                drawableICON = getResources().getDrawable(R.drawable.contactevent);

                                iconLeft = (int) (dayname * getTileWidth() + (getTileWidth() * 0.5f));
                                iconTop = (int) (week * getTileHeight() - (getTileHeight() * 0.02f));

                                drawableICON.setBounds(iconLeft, iconTop, iconLeft + mColorPenSize, iconTop + mColorPenSize);
                                drawableICON.draw(canvas);

                                drawableICON = getResources().getDrawable(R.drawable.colorpen);

                            } else {
                                drawableICON = getResources().getDrawable(R.drawable.colorpen);
                            }
                        } else {//if (mDdaysMap.containsKey(day)) {
                            drawableICON = getResources().getDrawable(R.drawable.dday);
                        }

                        iconLeft = (int) (dayname * getTileWidth() + (getTileWidth() * 0.7f));
                        iconTop = (int) (week * getTileHeight() - (getTileHeight() * 0.02f));
                        drawableICON.setBounds(iconLeft, iconTop, iconLeft + mColorPenSize, iconTop + mColorPenSize);
                        drawableICON.draw(canvas);

                    }

                    if (dayname == 0) {
                        foreground.setColor(getResources().getColor(R.color.cal_sunday));
                    } else if (dayname == 6) {
                        foreground.setColor(getResources().getColor(R.color.cal_satday));
                    } else {
                        foreground.setColor(getResources().getColor(R.color.cal_normalday));
                    }

                    if (mAnniversaryMap.containsKey(day)) {
                        getRect(dayname, week, r);

                        switch (mAnniversaryMap.get(day)) {
                            case 1:
                                drawableICON = getResources().getDrawable(R.drawable.flag1);
                                break;
                            case 2:
                                drawableICON = getResources().getDrawable(R.drawable.flag2);
                                break;
                            case 3:
                                drawableICON = getResources().getDrawable(R.drawable.flag3);
                                break;
                            case 4:
                                drawableICON = getResources().getDrawable(R.drawable.flag4);
                                break;
                            default:
                                foreground.setColor(getResources().getColor(R.color.cal_sunday));
                                drawableICON = getResources().getDrawable(R.drawable.flag0);

                        }

                        // 리본그리기(아이콘 - 기념일)
                        drawableICON.setBounds((int) r.left, (int) r.top, (int) r.left + mIconSize, (int) r.top + mIconSize);
                        drawableICON.draw(canvas);
                    }

                    // 기념일 리본 달기
                    /*
                    if (day == 5) {
                        getRect(dayname, week, r);
                        drawable = getResources().getDrawable(R.drawable.birthday);

                        // 리본그리기(아이콘 - 기념일)
                        drawable.setBounds((int) r.left, (int) r.top, (int) r.left + mIconSize, (int) r.top + mIconSize);
                        drawable.draw(canvas);
                    }
                    */

                    foreground.setTextSize(getTileHeight() * 0.40f);
                    canvas.drawText(Integer.toString(day), dayname * getTileWidth() + y, week * getTileHeight() + x, foreground);

                    // 음력날짜를 표시한다.
                    drawLunaDays(canvas, foreground, x, y, day, endofmonth, week, dayname);

                    // 달의 변화모양 아이콘을 표시한다.
                    drawLunaIcons(canvas, week, dayname, day);

                    day++;
                }
            }
        }

        drawableICON = getResources().getDrawable(R.drawable.selbox);
        drawableICON.setBounds(selRect);
        drawableICON.setAlpha(130);
        drawableICON.draw(canvas);

    }

    /**
     * @param canvas
     * @param week
     * @param dayname
     * @param ilday
     * @throws NotFoundException
     */
    private void drawLunaIcons(Canvas canvas, int week, int dayname, int day) throws NotFoundException {
        int iconLeft;
        int iconTop;

        //int ilday = Integer.parseInt(getLunaday(day).substring(6));
        //int ilday = Lunar2Solar.getLunarID(getLunaday(day));
        int ilday = Math.round(Lunar2Solar.CalculateMoonPhase(lunarCalendar.mYear, lunarCalendar.mMonth + 1, day));

        Drawable drawICON;
        if (Prefs.getLunaIcon(getContext())) {
            iconLeft = (int) (dayname * getTileWidth() + (getTileWidth() * 0.68f));
            iconTop = (int) (week * getTileHeight() + (getTileHeight() * 0.68f));

            // switch (Lunar2Solar.getLunarID(getLunaday(day))) {
            switch (ilday) {
                case 0:
                    drawICON = getResources().getDrawable(R.drawable.i0);
                    break;
                case 1:
                    drawICON = getResources().getDrawable(R.drawable.i1);
                    break;
                case 2:
                    drawICON = getResources().getDrawable(R.drawable.i2);
                    break;
                case 3:
                    drawICON = getResources().getDrawable(R.drawable.i3);
                    break;
                case 4:
                    drawICON = getResources().getDrawable(R.drawable.i4);
                    break;
                case 5:
                    drawICON = getResources().getDrawable(R.drawable.i5);
                    break;
                case 6:
                    drawICON = getResources().getDrawable(R.drawable.i6);
                    break;
                case 7:
                    drawICON = getResources().getDrawable(R.drawable.i7);
                    break;
                case 8:
                    drawICON = getResources().getDrawable(R.drawable.i8);
                    break;
                case 9:
                    drawICON = getResources().getDrawable(R.drawable.i9);
                    break;
                case 10:
                    drawICON = getResources().getDrawable(R.drawable.i10);
                    break;
                case 11:
                    drawICON = getResources().getDrawable(R.drawable.i11);
                    break;
                case 12:
                    drawICON = getResources().getDrawable(R.drawable.i12);
                    break;
                case 13:
                    drawICON = getResources().getDrawable(R.drawable.i13);
                    break;
                case 14:
                    drawICON = getResources().getDrawable(R.drawable.i14);
                    break;
                case 15:
                    drawICON = getResources().getDrawable(R.drawable.i15);
                    break;
                case 16:
                    drawICON = getResources().getDrawable(R.drawable.i16);
                    break;
                case 17:
                    drawICON = getResources().getDrawable(R.drawable.i17);
                    break;
                case 18:
                    drawICON = getResources().getDrawable(R.drawable.i18);
                    break;
                case 19:
                    drawICON = getResources().getDrawable(R.drawable.i19);
                    break;
                case 20:
                    drawICON = getResources().getDrawable(R.drawable.i20);
                    break;
                case 21:
                    drawICON = getResources().getDrawable(R.drawable.i21);
                    break;
                case 22:
                    drawICON = getResources().getDrawable(R.drawable.i22);
                    break;
                case 23:
                    drawICON = getResources().getDrawable(R.drawable.i23);
                    break;
                case 24:
                    drawICON = getResources().getDrawable(R.drawable.i24);
                    break;
                case 25:
                    drawICON = getResources().getDrawable(R.drawable.i25);
                    break;
                case 26:
                    drawICON = getResources().getDrawable(R.drawable.i26);
                    break;
                case 27:
                    drawICON = getResources().getDrawable(R.drawable.i27);
                    break;
                case 28:
                    drawICON = getResources().getDrawable(R.drawable.i28);
                    break;
                case 29:
                    drawICON = getResources().getDrawable(R.drawable.i29);
                    break;
                case 30:
                    drawICON = getResources().getDrawable(R.drawable.i30);
                    break;
                default:
                    drawICON = getResources().getDrawable(R.drawable.i0);
                    break;
            }

            //TODO: 
            Rect rectMoon = new Rect();
            rectMoon.set(iconLeft, iconTop, iconLeft + (getWidth() / 7 / 3), iconTop + (getWidth() / 7 / 3));
            drawICON.setBounds(rectMoon);
            drawICON.draw(canvas);
        }
    }

    /**
     * @param canvas
     * @param foreground
     * @param x
     * @param y
     * @param day
     * @param endofmonth
     * @param lday
     * @param week
     * @param dayname
     * @param ilday
     */
    private void drawLunaDays(Canvas canvas, Paint foreground, float x, float y, int day, int endofmonth, int week, int dayname) {

        String lday = getLunaday(day);
        int ilday = Integer.parseInt(lday.substring(6));

        if ("01".equals(lday.substring(6))) {
            lday = Integer.parseInt(lday.substring(4, 6)) + "." + Integer.parseInt(lday.substring(6));
        } else {
            lday = Integer.parseInt(lday.substring(6)) + "";
        }

        if (Prefs.getLunaDays(getContext())) {
            // 음력날짜는 5일 간격으로 표시한다.
            if (ilday % 5 == 0 || ilday == 1 || day == 1 || day == endofmonth) {
                foreground.setTextSize(getTileHeight() * 0.20f);
                canvas.drawText(lday, dayname * getTileWidth() + (y * 0.5f), week * getTileHeight() + (x * 1.7f), foreground);
            }
        }
    }

    /*
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_DOWN)
              return super.onTouchEvent(event);

            //Log.d(TAG, "event.getDownTime(): " + event.getDownTime());

            select((int) (event.getX() / tileWidth), (int) (event.getY() / tileHeight));
            //lunarCalendar.showKeypadOrError(selX, selY);

            if (this.mPrevYearR.contains((int) event.getX(), (int) event.getY())) {
                lunarCalendar.AddMonth(-12);
            } else if (this.mPrevMonthR.contains((int) event.getX(), (int) event.getY())) {
                lunarCalendar.AddMonth(-1);
            } else if (this.mNextYearR.contains((int) event.getX(), (int) event.getY())) {
                lunarCalendar.AddMonth(12);
            } else if (this.mNextMonthR.contains((int) event.getX(), (int) event.getY())) {
                lunarCalendar.AddMonth(1);
            } else if (this.titleRect.contains((int) event.getX(), (int) event.getY())) {
                lunarCalendar.showDialog(lunarCalendar.DATE_DIALOG_ID);
            }
            //Log.d(TAG, "onTouchEvent: x " + selX + ", y " + selY);
            return false;
        }
    */

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Log.d(TAG, "onKeyDown: keycode=" + keyCode + ", event=" + event);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                setSelection(getSelX(), getSelY() - 1);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                setSelection(getSelX(), getSelY() + 1);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                setSelection(getSelX() - 1, getSelY());
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                setSelection(getSelX() + 1, getSelY());
                break;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                break;
            default:
                return super.onKeyDown(keyCode, event);
        }
        return true;
    }

    private static class EfficientAdapter extends SimpleCursorAdapter {
        private LayoutInflater mInflater;
        private Bitmap flag0;
        private Bitmap flag1;
        private Bitmap flag3;
        private Bitmap flag4;
        private Bitmap dday;
        private Bitmap pen;
        private Bitmap event;

        public EfficientAdapter(Context context, int layout, Cursor c, String from[], int to[]) {
            // Cache the LayoutInflate to avoid asking for a new one each time.

            super(context, layout, c, from, to);
            mInflater = LayoutInflater.from(context);

            // Icons bound to the rows.
            flag0 = BitmapFactory.decodeResource(context.getResources(), R.drawable.flag0);
            flag1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.flag1);
            flag3 = BitmapFactory.decodeResource(context.getResources(), R.drawable.flag3);
            flag4 = BitmapFactory.decodeResource(context.getResources(), R.drawable.flag4);
            dday = BitmapFactory.decodeResource(context.getResources(), R.drawable.dday);
            pen = BitmapFactory.decodeResource(context.getResources(), R.drawable.pen);
            event = BitmapFactory.decodeResource(context.getResources(), R.drawable.contactevent);
        }

        /**
         * The number of items in the list is determined by the number of speeches
         * in our array.
         *
         * @see android.widget.ListAdapter#getCount()
         */
        public int getCount() {
            return super.getCount();
        }

        /**
         * Since the data comes from an array, just returning the index is
         * sufficent to get at the data. If we were using a more complex data
         * structure, we would return whatever object represents one row in the
         * list.
         *
         * @see android.widget.ListAdapter#getItem(int)
         */
        public Object getItem(int position) {
            return super.getItem(position);
        }

        /**
         * Use the array index as a unique id.
         *
         * @see android.widget.ListAdapter#getItemId(int)
         */
        @Override
        public long getItemId(int position) {
            return super.getItemId(position);
        }

        /**
         * Make a view to hold each row.
         *
         * @see android.widget.ListAdapter#getView(int, android.view.View,
         *      android.view.ViewGroup)
         */
        public View getView(int position, View convertView, ViewGroup parent) {
            // A ChildHolder keeps references to children views to avoid unneccessary calls
            // to findViewById() on each row.
            ViewHolder holder;

            // When convertView is not null, we can reuse it directly, there is no need
            // to reinflate it. We only inflate a new View when the convertView supplied
            // by ListView is null.
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.shcedule_item, null);

                // Creates a ChildHolder and store references to the two children views
                // we want to bind data to.
                holder = new ViewHolder();
                holder.text = (TextView) convertView.findViewById(R.id.schedule_title);
                holder.icon = (ImageView) convertView.findViewById(R.id.flags);
                holder.clock = (ImageView) convertView.findViewById(R.id.clock_flags);

                convertView.setTag(holder);
            } else {
                // Get the ChildHolder back to get fast access to the TextView
                // and the ImageView.
                holder = (ViewHolder) convertView.getTag();
            }

            // Bind the data efficiently with the holder.
            holder.text.setText(((Cursor) super.getItem(position)).getString(2));

            int kind = ((Cursor) super.getItem(position)).getInt(3);

            switch (kind) {
                case 0:
                    holder.icon.setImageBitmap(flag0);
                    break;
                case 1:
                    holder.icon.setImageBitmap(flag1);
                    break;
                case 3:
                    holder.icon.setImageBitmap(flag3);
                    break;
                case 5:
                    holder.icon.setImageBitmap(dday);
                    break;
                case 6:
                    holder.icon.setImageBitmap(pen);
                    break;
                case 9:
                    holder.icon.setImageBitmap(this.event);
                    break;
                default:
                    holder.icon.setImageBitmap(flag4);
            }

            int clock = ((Cursor) super.getItem(position)).getInt(6);
            if (clock > 0) {
                holder.clock.setVisibility(View.VISIBLE);
            } else {
                holder.clock.setVisibility(View.GONE);
            }

            return convertView;
        }

        static class ViewHolder {
            TextView text;
            ImageView icon;
            ImageView clock;
        }
    }

    public void setSelection(int x, int y) {

        setSelX(Math.min(Math.max(x, 0), 6));
        setSelY(Math.min(Math.max(y, 0), 8));

        if (y >= 1 && y <= 8) {

            int newDay = (getSelY() - 1) * 7 + (getSelX() + 1) - (7 - (8 - dayofweek));

            if (lunarCalendar.mDay != newDay) {
                lunarCalendar.todayEvents.clear();
            }

            lunarCalendar.mDay = newDay;

            getRect(getSelX(), getSelY(), selRect);

            Cursor cursor = null;
            if (this.mMonthlyEvent.length > 0) {
                cursor = lunarCalendar.dao.query(mMonthlyEvent, Common.fmtDate(lunarCalendar.mYear, lunarCalendar.mMonth + 1, lunarCalendar.mDay), this.getLunaday(lunarCalendar.mDay));
            } else {
                cursor = lunarCalendar.dao.query(Common.fmtDate(lunarCalendar.mYear, lunarCalendar.mMonth + 1, lunarCalendar.mDay), this.getLunaday(lunarCalendar.mDay));
            }
            SimpleCursorAdapter adapter = new EfficientAdapter(getContext(), R.layout.shcedule_item, cursor, new String[] { Schedule.SCHEDULE_TYPE, Schedule.SCHEDULE_TITLE }, new int[] { R.id.schedule_date, R.id.schedule_title });

            lunarCalendar.mListView.setAdapter(adapter);

            invalidate();
        }

    }

    private void getRect(int x, int y, Rect rect) {
        rect.set((int) (x * getTileWidth()), (int) (y * getTileHeight()), (int) (x * getTileWidth() + getTileWidth()), (int) (y * getTileHeight() + getTileHeight()));
    }

    private void getRect(int x, int y, RectF rect) {
        rect.set((int) (x * getTileWidth()), (int) (y * getTileHeight()), (int) (x * getTileWidth() + getTileWidth()), (int) (y * getTileHeight() + getTileHeight()));
    }

    private void getContractRect(int x, int y, RectF rect, int offSet) {
        rect.set((int) (x * getTileWidth() + offSet), (int) (y * getTileHeight() + offSet), (int) (x * getTileWidth() + getTileWidth() - offSet), (int) (y * getTileHeight() + getTileHeight() - offSet));
    }

    /**
     * @param tileHeight the tileHeight to set
     */
    public void setTileHeight(float tileHeight) {
        this.tileHeight = tileHeight;
    }

    /**
     * @return the tileHeight
     */
    public float getTileHeight() {
        return tileHeight;
    }

    /**
     * @param tileWidth the tileWidth to set
     */
    public void setTileWidth(float tileWidth) {
        this.tileWidth = tileWidth;
    }

    /**
     * @return the tileWidth
     */
    public float getTileWidth() {
        return tileWidth;
    }

    /**
     * @param selX the selX to set
     */
    public void setSelX(int selX) {
        this.selX = selX;
    }

    /**
     * @return the selX
     */
    public int getSelX() {
        return selX;
    }

    /**
     * @param selY the selY to set
     */
    public void setSelY(int selY) {
        this.selY = selY;
    }

    /**
     * @return the selY
     */
    public int getSelY() {
        return selY;
    }

}
