package org.nilriri.LunaCalendar;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.nilriri.LunaCalendar.dao.ScheduleBean;
import org.nilriri.LunaCalendar.dao.ScheduleDaoImpl;
import org.nilriri.LunaCalendar.dao.Constants.Schedule;
import org.nilriri.LunaCalendar.gcal.EventEntry;
import org.nilriri.LunaCalendar.gcal.GoogleUtil;
import org.nilriri.LunaCalendar.schedule.ScheduleEditor;
import org.nilriri.LunaCalendar.schedule.ScheduleViewer;
import org.nilriri.LunaCalendar.schedule.SearchResult;
import org.nilriri.LunaCalendar.tools.About;
import org.nilriri.LunaCalendar.tools.Common;
import org.nilriri.LunaCalendar.tools.DataManager;
import org.nilriri.LunaCalendar.tools.Lunar2Solar;
import org.nilriri.LunaCalendar.tools.OldEvent;
import org.nilriri.LunaCalendar.tools.Prefs;
import org.nilriri.LunaCalendar.tools.QuickContactViewer;
import org.nilriri.LunaCalendar.tools.SearchData;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class LunarCalendar extends Activity implements RefreshManager {

    static final int DATE_DIALOG_ID = 1;

    private OldEvent oldEvent;

    // Menu item ids    
    public static final int MENU_ITEM_TODAYSCHEDULE = Menu.FIRST;
    public static final int MENU_ITEM_ADDSCHEDULE = Menu.FIRST + 1;
    public static final int MENU_ITEM_ALLSCHEDULE = Menu.FIRST + 2;
    public static final int MENU_ITEM_WEEKSCHEDULE = Menu.FIRST + 3;
    public static final int MENU_ITEM_MONTHSCHEDULE = Menu.FIRST + 4;
    public static final int MENU_ITEM_GCALADDEVENT = Menu.FIRST + 5;
    public static final int MENU_ITEM_GCALIMPORT = Menu.FIRST + 6;
    public static final int MENU_ITEM_ABOUT = Menu.FIRST + 7;
    public static final int MENU_ITEM_DELSCHEDULE = Menu.FIRST + 8;
    public static final int MENU_ITEM_BACKUP = Menu.FIRST + 9;
    public static final int MENU_ITEM_RESTORE = Menu.FIRST + 10;
    public static final int MENU_ITEM_MAKECAL = Menu.FIRST + 11;
    public static final int MENU_ITEM_ONLINECAL = Menu.FIRST + 12;
    public static final int MENU_ITEM_SEARCH = Menu.FIRST + 13;
    public static final int MENU_ITEM_EDITSCHEDULE = Menu.FIRST + 14;
    public static final int MENU_ITEM_GOTOTODAY = Menu.FIRST + 15;
    public static final int MENU_ITEM_NEXTYEAR = Menu.FIRST + 16;
    public static final int MENU_ITEM_GOTO = Menu.FIRST + 17;

    // date and time
    public int mYear;
    public int mMonth;
    public int mDay;
    public ListView mListView;
    public List<EventEntry> todayEvents = new ArrayList<EventEntry>();

    public ScheduleDaoImpl dao = null;

    private LunarCalendarView lunarCalendarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Common.startAlarmNotifyService(LunarCalendar.this);

        dao = new ScheduleDaoImpl(this, null, Prefs.getSDCardUse(this));
        oldEvent = new OldEvent(-1, -1);

        Intent intent = getIntent();

        final Calendar c = Calendar.getInstance();
        if (intent.hasExtra("DataPk")) {

            Long dataPK = intent.getLongExtra("DataPk", new Long(0));
            //Log.d(Common.TAG, "dataPK=" + dataPK);
            Cursor cursor = dao.query(dataPK);

            if (dataPK > 0 && cursor.getCount() > 0) {

                ScheduleBean s = new ScheduleBean(cursor);

                if (s.getLunaryn()) {
                    String sdate = Lunar2Solar.l2s(s.getYear() + "", s.getLMonth() + "", s.getLDay() + "");

                    //Log.d(Common.TAG, "sdate=" + sdate);

                    mYear = Integer.parseInt(sdate.substring(0, 4));
                    mMonth = Integer.parseInt(sdate.substring(4, 6)) - 1;
                    mDay = Integer.parseInt(sdate.substring(6, 8));

                } else {
                    mYear = s.getYear();
                    mMonth = s.getMonth() - 1;
                    mDay = s.getDay();
                }
            } else {
                mYear = c.get(Calendar.YEAR);
                mMonth = c.get(Calendar.MONTH);
                mDay = c.get(Calendar.DAY_OF_MONTH);
            }
        } else {
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);
        }

        setContentView(R.layout.animations_main_screen);

        lunarCalendarView = (LunarCalendarView) findViewById(R.id.lunaCalendarView);
        lunarCalendarView.requestFocus();

        lunarCalendarView.mToDay = c.get(Calendar.YEAR) + "." + c.get(Calendar.MONTH) + "." + c.get(Calendar.DAY_OF_MONTH);

        lunarCalendarView.setOnCreateContextMenuListener(this);
        lunarCalendarView.setFocusableInTouchMode(true);
        lunarCalendarView.setFocusable(true);
        //lunarCalendarView.setLongClickable(true);

        lunarCalendarView.setDrawingCacheEnabled(true);

        lunarCalendarView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        oldEvent.set(event.getX(), event.getY());
                        break;
                    case MotionEvent.ACTION_UP:

                        /*if (Common.getExpandRect(lunarCalendarView.mPrevMonthR, 20).contains((int) event.getX(), (int) event.getY())) {
                            AddMonth(-1);
                        } else if (Common.getExpandRect(lunarCalendarView.mNextMonthR, 20).contains((int) event.getX(), (int) event.getY())) {
                            AddMonth(1);
                        } else*/
                        if (lunarCalendarView.titleRect.contains((int) event.getX(), (int) event.getY())) {
                            showDialog(LunarCalendar.DATE_DIALOG_ID);
                        } else {
                            if (event.getX() - oldEvent.getX() > 50 && Math.abs(event.getY() - oldEvent.getY()) < 90) {//Right
                                AddMonth(-1);
                            } else if (event.getX() - oldEvent.getX() < -50 && Math.abs(event.getY() - oldEvent.getY()) < 90) {
                                AddMonth(1);
                            } else {
                                lunarCalendarView.setSelection((int) (event.getX() / lunarCalendarView.getTileWidth()), (int) (event.getY() / lunarCalendarView.getTileHeight()));
                            }
                        }
                        oldEvent.set(event.getX(), event.getY());
                        break;

                    default:

                        return false;

                }
                return false;
            }

        });

        mListView = (ListView) findViewById(R.id.ContentsListView);

        mListView.setOnCreateContextMenuListener(this);
        mListView.setOnItemClickListener(new ScheduleOnItemClickListener());

    }

    public class ScheduleOnItemClickListener implements OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {

            if (pos < 0)
                return;

            Cursor c = (Cursor) parent.getItemAtPosition(pos);

            if (c != null) {

                if ("Contact".equals(c.getString(c.getColumnIndexOrThrow(Schedule.SCHEDULE_TYPE)))) {

                    Uri uri = Uri.parse(c.getString(c.getColumnIndexOrThrow("uri")));

                    Intent intent = new Intent(LunarCalendar.this, QuickContactViewer.class);
                    intent.setAction(QuickContactViewer.ACTION_QUICKVIEW);
                    intent.setData(uri);
                    intent.putExtra(ContactsContract.Contacts.DISPLAY_NAME, c.getString(c.getColumnIndexOrThrow("displayname")));

                    startActivity(intent);

                } else {

                    Intent intent = new Intent();
                    intent.setClass(getBaseContext(), ScheduleViewer.class);
                    intent.putExtra("id", new Long(id));
                    startActivity(intent);
                }
            }

        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing. 
        }

    }

    public void AddMonth(int offset) {
        // 달이 바뀌면서 기존에 선택된 영역이 새로 바뀐달에서는 날짜 영역이 아닌경우 에러가 발생함.
        // TODO: 새로운 달의 날짜범위를 넘어가면 1일이나 마지막 날짜로 변환.
        try {
            final Calendar c = Calendar.getInstance();
            c.setFirstDayOfWeek(Calendar.SUNDAY);
            c.set(mYear, mMonth, 1);
            c.add(Calendar.MONTH, 1);
            c.add(Calendar.DAY_OF_MONTH, -1);

            if (mDay < 1 || mDay > c.get(Calendar.DAY_OF_MONTH)) {
                mDay = 1;
                lunarCalendarView.setSelX(c.get(Calendar.DAY_OF_WEEK) - 1);
                lunarCalendarView.setSelY(2);
            }

            c.set(mYear, mMonth, mDay);
            c.add(Calendar.MONTH, offset);
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);

            lunarCalendarView.loadSchduleExistsInfo();

            // 달이 바뀔때는 화면전체를 다시 그린다.
            lunarCalendarView.invalidate();

            // 날짜가 바뀌면 다시 조회하기 위해서 초기화한다.
            todayEvents.clear();
        } catch (Exception e) {
            //Log.e(Common.TAG, e.getMessage(), e);
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
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 설정화면에서 sd카드 사용여부를 변경하면 dao를 지정한 위치의 db로 다시 연결한다.
        if (dao.mSdcarduse != Prefs.getSDCardUse(this)) {
            dao = new ScheduleDaoImpl(this, null, Prefs.getSDCardUse(this));
        }

        //화면으로 복귀할때 새로 등록되거나 삭제된 일정정보를 화면에 갱신한다.
        lunarCalendarView.loadSchduleExistsInfo();

        updateDisplay();
    }

    public void updateDisplay() {
        AddMonth(0);
        lunarCalendarView.setSelection(lunarCalendarView.getSelX(), lunarCalendarView.getSelY());
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     * 날짜 선택 대화상자 생성및 표시.
     */

    private class ShowOnlineCalendar extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;
        private AlertDialog.Builder builder;

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(LunarCalendar.this, "", "구글캘린더에서 일정을 가져오고 있습니다...", true);
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                String url = Prefs.getOnlineCalendar(LunarCalendar.this);

                Calendar c = Calendar.getInstance();
                c.set(mYear, mMonth, mDay);
                c.add(Calendar.DAY_OF_MONTH, -1);

                StringBuilder where = new StringBuilder("?start-min=");
                where.append(Common.fmtDate(c));
                where.append("&start-max=");
                c.add(Calendar.DAY_OF_MONTH, 2);
                where.append(Common.fmtDate(c));
                url += where.toString();

                if (todayEvents.size() <= 0) {
                    GoogleUtil gu = new GoogleUtil(Prefs.getAuthToken(LunarCalendar.this));
                    todayEvents = gu.getEvents(url);
                    if (todayEvents.size() <= 0) {
                        cancel(true);
                    }
                }

                String names[] = new String[todayEvents.size()];
                final String index[] = new String[todayEvents.size()];

                for (int i = 0; i < todayEvents.size(); i++) {
                    names[i] = todayEvents.get(i).getStartDate().substring(5, 10) + " : " + todayEvents.get(i).title;
                    index[i] = todayEvents.get(i).content;
                }

                builder = new AlertDialog.Builder(LunarCalendar.this);
                builder.setTitle("확인할 일정을 선택하십시오.");
                builder.setItems(names, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        try {

                            if (index[which].indexOf("bindex:") >= 0) {

                                String bindex = index[which].replace("bindex:", "");
                                String data[] = Common.tokenFn(bindex, ",");

                                Intent intent = new Intent();
                                intent.setAction("org.nilriri.webbibles.VIEW");
                                intent.setType("vnd.org.nilriri/web-bible");

                                intent.putExtra("VERSION", 0);
                                intent.putExtra("VERSION2", 0);
                                intent.putExtra("BOOK", Integer.parseInt(data[0]));
                                intent.putExtra("CHAPTER", Integer.parseInt(data[1]));
                                intent.putExtra("VERSE", 0);

                                startActivity(intent);
                            } else {
                                Toast.makeText(LunarCalendar.this, index[which], Toast.LENGTH_LONG).show();
                            }

                        } catch (Exception e) {
                            Toast.makeText(LunarCalendar.this, "온라인성경 앱이 설치되어있지 않거나 최신버젼이 아닙니다.", Toast.LENGTH_LONG).show();
                        }
                    }
                });

            } catch (IOException e) {
                cancel(true);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            dialog.dismiss();

            if (todayEvents.size() > 0) {
                builder.show();
            } else {
                Toast.makeText(LunarCalendar.this, "구독하는 달력에 등록된 일정이 없습니다.", Toast.LENGTH_LONG).show();
            }

        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DATE_DIALOG_ID:
                return new DatePickerDialog(this, mDateSetListener, mYear, mMonth, mDay);
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case DATE_DIALOG_ID:
                ((DatePickerDialog) dialog).updateDate(mYear, mMonth, mDay);
                break;
        }
    }

    private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            mYear = year;
            mMonth = monthOfYear;
            mDay = dayOfMonth;
            lunarCalendarView.loadSchduleExistsInfo();
            lunarCalendarView.invalidate();
            updateDisplay();
        }
    };

    public void onSelectTargetCalendar(int choice) {

        final int mChoice = choice;

        final String names[] = Prefs.getSyncCalendarName(LunarCalendar.this);
        final String values[] = Prefs.getSyncCalendarValue(LunarCalendar.this);

        AlertDialog.Builder builder = new AlertDialog.Builder(LunarCalendar.this);
        builder.setTitle("배치 작업을 실행할 달력을 선택하십시오.");
        builder.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                try {

                    switch (mChoice) {
                        case 0: // <item>음력일정 일괄생성</item>

                            dao.batchMakeCalendar(LunarCalendar.this, values[which]);

                            break;
                        case 1: // <item>맥체인성경읽기 일정생성(가정)</item>

                            dao.batchBibleCalendar(LunarCalendar.this, values[which], which + "");

                            break;
                        case 2: // <item>맥체인성경읽기 일정생성(개인)</item>
                            dao.batchBibleCalendar(LunarCalendar.this, values[which], which + "");

                            break;
                        case 3: // <item>로컬일정 일괄 생성</item>

                            dao.batchUpload(LunarCalendar.this, values[which]);

                            break;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(LunarCalendar.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }).show();

    }

    public void onBatchJob() {

        String dataworks[] = getResources().getStringArray(R.array.entries_batchjobs);

        AlertDialog.Builder builder = new AlertDialog.Builder(LunarCalendar.this);
        builder.setTitle("배치 작업을 선택하십시오.");
        builder.setItems(dataworks, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                try {

                    onSelectTargetCalendar(which);

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(LunarCalendar.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }).show();

    }

    class BackupFileSearchFilter implements FilenameFilter {
        private String fileName;
        private String delimiter;

        public BackupFileSearchFilter() {
            this.fileName = "lunarcalendar";
            this.delimiter = "backup";
        }

        public boolean accept(File dir, String name) {
            if (name != null) {
                return name.startsWith(fileName) && name.contains(delimiter);// .startsWith(name);
            }
            return false;
        }

    }

    public void onDataWork() {

        String dataworks[] = getResources().getStringArray(R.array.entries_dataworks);

        AlertDialog.Builder builder = new AlertDialog.Builder(LunarCalendar.this);
        builder.setTitle("작업을 선택하십시오.");

        builder.setItems(dataworks, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

                try {
                    switch (which) {
                        case 0: // 백업

                            DataManager.StartBackup(LunarCalendar.this);

                            break;
                        case 1: // 복원

                            onSelectBackupFile();

                            break;
                        case 2: // csv export
                            break;
                        case 3: // csv import
                            break;
                    }

                } catch (Exception e) {
                    Toast.makeText(LunarCalendar.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }).show();

    }

    public void onSelectBackupFile() {

        String backupPath = android.os.Environment.getExternalStorageDirectory().toString() + "/";

        File dir = new File(backupPath);
        FilenameFilter filenameFilter = new BackupFileSearchFilter();
        File[] files = dir.listFiles(filenameFilter);

        final String names[] = new String[files.length];
        int i = 0;
        for (File file : files) {
            names[i++] = file.getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(LunarCalendar.this);
        builder.setTitle("복원할 백업파일을 선택하십시오.");
        builder.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                try {
                    String targetFile = names[which];

                    DataManager.StartRestore(LunarCalendar.this, targetFile);
                    updateDisplay();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(LunarCalendar.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }).show();
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     * 옵션메뉴 생성.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuItem itemAdd = menu.add(0, MENU_ITEM_ADDSCHEDULE, 0, R.string.schedule_add_label);
        itemAdd.setIcon(android.R.drawable.ic_menu_add);

        //MenuItem itemAllList = menu.add(0, MENU_ITEM_ALLSCHEDULE, 0, R.string.schedule_alllist_label);
        //itemAllList.setIcon(android.R.drawable.ic_menu_agenda);

        SubMenu subMenu = menu.addSubMenu("일정목록").setIcon(android.R.drawable.ic_menu_agenda);
        subMenu.add(0, MENU_ITEM_ALLSCHEDULE, 0, R.string.schedule_alllist_label);
        subMenu.add(0, MENU_ITEM_TODAYSCHEDULE, 0, R.string.schedule_todaylist_label);
        subMenu.add(0, MENU_ITEM_WEEKSCHEDULE, 0, R.string.schedule_weeklist_label);
        subMenu.add(0, MENU_ITEM_MONTHSCHEDULE, 0, R.string.schedule_monthlist_label);

        MenuItem itemImport = menu.add(0, MENU_ITEM_GCALIMPORT, 0, R.string.schedule_gcalimport_menu);
        itemImport.setIcon(android.R.drawable.ic_popup_sync);

        MenuItem itemSearch = menu.add(0, MENU_ITEM_SEARCH, 0, R.string.eventsearch_label);
        itemSearch.setIcon(android.R.drawable.ic_menu_search);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ITEM_ALLSCHEDULE:
            case MENU_ITEM_TODAYSCHEDULE:
            case MENU_ITEM_WEEKSCHEDULE:
            case MENU_ITEM_MONTHSCHEDULE:
                this.openScheduleList(item.getItemId());
                return true;
            case MENU_ITEM_ADDSCHEDULE: {
                Intent intent = new Intent();
                intent.setClass(this, ScheduleEditor.class);

                intent.putExtra("SID", new Long(0));
                final Calendar c = Calendar.getInstance();
                c.set(mYear, mMonth, mDay);
                intent.putExtra("STODAY", c);
                startActivity(intent);
                return true;
            }
            case MENU_ITEM_GCALIMPORT: {
                if ("".equals(Prefs.getSyncCalendar(this)) || Prefs.getSyncCalendar(this) == null) {
                    Toast.makeText(getBaseContext(), "Google 계정 및 동기화 정보를 확인 하십시오.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, Prefs.class));
                } else {
                    dao.syncImport(this);
                }
                return true;
            }
            case MENU_ITEM_SEARCH: { // 자료검색
                Intent intent = new Intent();
                intent.setClass(this, SearchData.class);
                startActivity(intent);
                return true;
            }
            case R.id.datawork: { // 자료관리
                onDataWork();
                return true;
            }
            case R.id.batchjob: { // 배치작업
                onBatchJob();
                return true;
            }

            case R.id.settings: { // 설정메뉴
                startActivity(new Intent(this, Prefs.class));
                return true;
            }
            case R.id.about: { // 프로그램 정보
                startActivity(new Intent(this, About.class));
                return true;
            }

            case R.id.blog: {
                Intent browserIntent = new Intent(android.content.Intent.ACTION_VIEW);

                browserIntent.setAction(Intent.ACTION_VIEW);
                browserIntent.setData(Uri.parse("http://nilriri.blogspot.com/search/label/LunarCalendar"));

                startActivity(browserIntent);

                return true;
            }
            case R.id.email: {
                final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);

                /* Fill it with Data */
                emailIntent.setType("plain/text");
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { "jeongjaehong@gmail.com" });
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "[LunarCalendar]Feedback...");
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "* 폰 모델:\n* 버젼:\n* 증상:\n* 상황설명:\n\n[LunarCalendar]");

                /* Send it off to the Activity-Chooser */
                startActivity(Intent.createChooser(emailIntent, "Send mail..."));

                return true;
            }

        }
        return super.onOptionsItemSelected(item);
    }

    private void eventAddorModity(Long itemid) {
        Intent intent = new Intent();
        intent.setClass(this, ScheduleEditor.class);
        intent.putExtra("SID", itemid);
        final Calendar c = Calendar.getInstance();
        c.set(mYear, mMonth, mDay);
        intent.putExtra("STODAY", c);
        startActivity(intent);
        return;
    }

    private void openScheduleList(int choice) {
        switch (choice) {

            case MENU_ITEM_ALLSCHEDULE: {
                Intent intent = new Intent();
                intent.setClass(this, SearchResult.class);
                final Calendar c = Calendar.getInstance();
                c.set(mYear, mMonth, mDay);
                intent.putExtra("org.nilriri.gscheduler.workday", c);
                intent.putExtra("ScheduleRange", "ALL");
                startActivity(intent);
                return;
            }
            case MENU_ITEM_TODAYSCHEDULE: {
                Intent intent = new Intent();
                intent.setClass(this, SearchResult.class);
                final Calendar c = Calendar.getInstance();
                c.set(mYear, mMonth, mDay);
                intent.putExtra("workday", c);
                intent.putExtra("ScheduleRange", "TODAY");

                startActivity(intent);

                return;
            }
            case MENU_ITEM_WEEKSCHEDULE: {
                Intent intent = new Intent();
                intent.setClass(this, SearchResult.class);
                final Calendar c = Calendar.getInstance();
                c.set(mYear, mMonth, mDay);
                intent.putExtra("workday", c);
                intent.putExtra("ScheduleRange", "WEEK");

                startActivity(intent);

                return;
            }
            case MENU_ITEM_MONTHSCHEDULE: {
                Intent intent = new Intent();
                intent.setClass(this, SearchResult.class);
                final Calendar c = Calendar.getInstance();
                c.set(mYear, mMonth, mDay);
                intent.putExtra("workday", c);
                intent.putExtra("ScheduleRange", "MONTH");

                startActivity(intent);

                return;
            }
        }
    }

    /*
    * (non-Javadoc)
    * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
    * 팝업메뉴 생성.
    */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

        super.onCreateContextMenu(menu, view, menuInfo);

        menu.setHeaderTitle(getResources().getString(R.string.app_name));
        menu.setHeaderIcon(R.drawable.icon);

        menu.add(0, MENU_ITEM_ADDSCHEDULE, 0, R.string.add_schedule);

        if (view.getId() == R.id.ContentsListView) {
            menu.add(0, MENU_ITEM_EDITSCHEDULE, 0, R.string.schedule_modify_label);
        }
        if (view.equals(this.mListView)) {
            AdapterView.AdapterContextMenuInfo info;
            try {
                info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            } catch (ClassCastException e) {
                //Log.e("LunarCalendar", "bad menuInfo", e);
                return;
            }

            // 사용자가 등록한 일정일경우 삭제메뉴 표시
            Cursor cursor = (Cursor) this.mListView.getItemAtPosition(info.position);
            if (cursor != null && "Schedule".equals(cursor.getString(1))) {
                menu.add(0, MENU_ITEM_DELSCHEDULE, 0, R.string.schedule_delete_label);
            }
        }

        SubMenu subMenu = menu.addSubMenu("일정목록보기").setIcon(android.R.drawable.ic_menu_agenda);
        subMenu.add(0, MENU_ITEM_ALLSCHEDULE, 0, R.string.schedule_alllist_label);
        subMenu.add(0, MENU_ITEM_TODAYSCHEDULE, 0, R.string.schedule_todaylist_label);
        subMenu.add(0, MENU_ITEM_WEEKSCHEDULE, 0, R.string.schedule_weeklist_label);
        subMenu.add(0, MENU_ITEM_MONTHSCHEDULE, 0, R.string.schedule_monthlist_label);

        menu.add(0, MENU_ITEM_GOTOTODAY, 0, R.string.goto_today);
        menu.add(0, MENU_ITEM_NEXTYEAR, 0, (this.mYear + 1) + "년 " + (this.mMonth + 1) + "월 보기");
        menu.add(0, MENU_ITEM_GOTO, 0, "바로가기(날짜검색)");

        menu.add(0, MENU_ITEM_ONLINECAL, 0, R.string.onlinecalendar_label);

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case MENU_ITEM_GOTOTODAY: {

                Calendar c = Calendar.getInstance();
                mYear = c.get(Calendar.YEAR);
                mMonth = c.get(Calendar.MONTH);
                mDay = c.get(Calendar.DAY_OF_MONTH);
                this.AddMonth(0);

                return true;
            }
            case MENU_ITEM_GOTO: {
                showDialog(DATE_DIALOG_ID);
                return true;
            }

            case MENU_ITEM_NEXTYEAR: {
                ++mYear;
                this.AddMonth(0);

                return true;
            }
            case MENU_ITEM_ALLSCHEDULE:
            case MENU_ITEM_TODAYSCHEDULE:
            case MENU_ITEM_WEEKSCHEDULE:
            case MENU_ITEM_MONTHSCHEDULE:
                this.openScheduleList(item.getItemId());
                return true;
            case MENU_ITEM_ADDSCHEDULE: {
                eventAddorModity(new Long(0));
                return true;
            }
            case MENU_ITEM_EDITSCHEDULE: {
                AdapterView.AdapterContextMenuInfo info;
                try {
                    info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                    eventAddorModity(info.id);
                } catch (ClassCastException e) {
                    Log.e("LunarCalendar", "bad menuInfo", e);
                    return false;
                }
                return true;
            }
            case MENU_ITEM_DELSCHEDULE: {
                AdapterView.AdapterContextMenuInfo info;
                try {
                    info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                    dao.syncDelete(info.id, this);
                } catch (ClassCastException e) {
                    //Log.e("LunarCalendar", "bad menuInfo", e);
                    return false;
                }
                return true;
            }
            case MENU_ITEM_ONLINECAL: {
                if (!"".equals(Prefs.getOnlineCalendar(LunarCalendar.this))) {
                    new ShowOnlineCalendar().execute();
                } else {
                    Toast.makeText(getBaseContext(), "설정화면에서 온라인 구독 달력을 지정하십시오.", Toast.LENGTH_LONG).show();
                }
                return true;
            }

        }
        return false;
    }

    /*
    * (non-Javadoc)
    * @see org.nilriri.LunaCalendar.RefreshManager#refresh()
    * 싱크 작업 종료후 화면 리프레쉬.
    */
    public void refresh() {
        this.AddMonth(0);
    }

}
