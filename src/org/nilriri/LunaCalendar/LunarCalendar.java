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
import org.nilriri.LunaCalendar.tools.SearchData;
import org.nilriri.LunaCalendar.tools.QuickContactViewer;

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
            Log.d(Common.TAG, "dataPK=" + dataPK);
            ScheduleBean s = new ScheduleBean(dao.query(dataPK));

            if (dataPK > 0) {
                if (s.getLunaryn()) {
                    String sdate = Lunar2Solar.l2s(s.getYear() + "", s.getLMonth() + "", s.getLDay() + "");

                    Log.d(Common.TAG, "sdate=" + sdate);

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
        // ���� �ٲ�鼭 ������ ���õ� ������ ���� �ٲ�޿����� ��¥ ������ �ƴѰ�� ������ �߻���.
        // TODO: ���ο� ���� ��¥������ �Ѿ�� 1���̳� ������ ��¥�� ��ȯ.
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

            // ���� �ٲ𶧴� ȭ����ü�� �ٽ� �׸���.
            lunarCalendarView.invalidate();

            // ��¥�� �ٲ�� �ٽ� ��ȸ�ϱ� ���ؼ� �ʱ�ȭ�Ѵ�.
            todayEvents.clear();
        } catch (Exception e) {
            Log.e(Common.TAG, e.getMessage(), e);
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

        // ����ȭ�鿡�� sdī�� ��뿩�θ� �����ϸ� dao�� ������ ��ġ�� db�� �ٽ� �����Ѵ�.
        if (dao.mSdcarduse != Prefs.getSDCardUse(this)) {
            dao = new ScheduleDaoImpl(this, null, Prefs.getSDCardUse(this));
        }

        //ȭ������ �����Ҷ� ���� ��ϵǰų� ������ ���������� ȭ�鿡 �����Ѵ�.
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
     * ��¥ ���� ��ȭ���� ������ ǥ��.
     */

    private class ShowOnlineCalendar extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;
        private AlertDialog.Builder builder;

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(LunarCalendar.this, "", "����Ķ�������� ������ �������� �ֽ��ϴ�...", true);
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
                builder.setTitle("Ȯ���� ������ �����Ͻʽÿ�.");
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
                            Toast.makeText(LunarCalendar.this, "�¶��μ��� ���� ��ġ�Ǿ����� �ʰų� �ֽŹ����� �ƴմϴ�.", Toast.LENGTH_LONG).show();
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
                Toast.makeText(LunarCalendar.this, "�����ϴ� �޷¿� ��ϵ� ������ �����ϴ�.", Toast.LENGTH_LONG).show();
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
        builder.setTitle("��ġ �۾��� ������ �޷��� �����Ͻʽÿ�.");
        builder.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                try {

                    switch (mChoice) {
                        case 0: // <item>�������� �ϰ�����</item>

                            dao.batchMakeCalendar(LunarCalendar.this, values[which]);

                            break;
                        case 1: // <item>��ü�μ����б� ��������(����)</item>

                            dao.batchBibleCalendar(LunarCalendar.this, values[which], which + "");

                            break;
                        case 2: // <item>��ü�μ����б� ��������(����)</item>
                            dao.batchBibleCalendar(LunarCalendar.this, values[which], which + "");

                            break;
                        case 3: // <item>�������� �ϰ� ����</item>

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
        builder.setTitle("��ġ �۾��� �����Ͻʽÿ�.");
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
        builder.setTitle("�۾��� �����Ͻʽÿ�.");

        builder.setItems(dataworks, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

                try {
                    switch (which) {
                        case 0: // ���

                            DataManager.StartBackup(LunarCalendar.this);

                            break;
                        case 1: // ����

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
        builder.setTitle("������ ��������� �����Ͻʽÿ�.");
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
     * �ɼǸ޴� ����.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuItem itemAdd = menu.add(0, MENU_ITEM_ADDSCHEDULE, 0, R.string.schedule_add_label);
        itemAdd.setIcon(android.R.drawable.ic_menu_add);

        //MenuItem itemAllList = menu.add(0, MENU_ITEM_ALLSCHEDULE, 0, R.string.schedule_alllist_label);
        //itemAllList.setIcon(android.R.drawable.ic_menu_agenda);

        SubMenu subMenu = menu.addSubMenu("�������").setIcon(android.R.drawable.ic_menu_agenda);
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
                    Toast.makeText(getBaseContext(), "Google ���� �� ����ȭ ������ Ȯ�� �Ͻʽÿ�.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, Prefs.class));
                } else {
                    dao.syncImport(this);
                }
                return true;
            }
            case MENU_ITEM_SEARCH: { // �ڷ�˻�
                Intent intent = new Intent();
                intent.setClass(this, SearchData.class);
                startActivity(intent);
                return true;
            }
            case R.id.datawork: { // �ڷ����
                onDataWork();
                return true;
            }
            case R.id.batchjob: { // ��ġ�۾�
                onBatchJob();
                return true;
            }

            case R.id.settings: { // �����޴�
                startActivity(new Intent(this, Prefs.class));
                return true;
            }
            case R.id.about: { // ���α׷� ����
                startActivity(new Intent(this, About.class));
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
    * �˾��޴� ����.
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
                Log.e("LunarCalendar", "bad menuInfo", e);
                return;
            }

            // ����ڰ� ����� �����ϰ�� �����޴� ǥ��
            Cursor cursor = (Cursor) this.mListView.getItemAtPosition(info.position);
            if (cursor != null && "Schedule".equals(cursor.getString(1))) {
                menu.add(0, MENU_ITEM_DELSCHEDULE, 0, R.string.schedule_delete_label);
            }
        }

        SubMenu subMenu = menu.addSubMenu("������Ϻ���").setIcon(android.R.drawable.ic_menu_agenda);
        subMenu.add(0, MENU_ITEM_ALLSCHEDULE, 0, R.string.schedule_alllist_label);
        subMenu.add(0, MENU_ITEM_TODAYSCHEDULE, 0, R.string.schedule_todaylist_label);
        subMenu.add(0, MENU_ITEM_WEEKSCHEDULE, 0, R.string.schedule_weeklist_label);
        subMenu.add(0, MENU_ITEM_MONTHSCHEDULE, 0, R.string.schedule_monthlist_label);

        menu.add(0, MENU_ITEM_GOTOTODAY, 0, R.string.goto_today);
        menu.add(0, MENU_ITEM_NEXTYEAR, 0, (this.mYear + 1) + "�� " + (this.mMonth + 1) + "�� ����");
        menu.add(0, MENU_ITEM_GOTO, 0, "�ٷΰ���(��¥�˻�)");

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
                    Log.e("LunarCalendar", "bad menuInfo", e);
                    return false;
                }
                return true;
            }
            case MENU_ITEM_ONLINECAL: {
                if (!"".equals(Prefs.getOnlineCalendar(LunarCalendar.this))) {
                    new ShowOnlineCalendar().execute();
                } else {
                    Toast.makeText(getBaseContext(), "����ȭ�鿡�� �¶��� ���� �޷��� �����Ͻʽÿ�.", Toast.LENGTH_LONG).show();
                }
                return true;
            }
        }
        return false;
    }

    /*
    * (non-Javadoc)
    * @see org.nilriri.LunaCalendar.RefreshManager#refresh()
    * ��ũ �۾� ������ ȭ�� ��������.
    */
    public void refresh() {
        this.AddMonth(0);
    }

}
