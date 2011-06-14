package org.nilriri.LunaCalendar.schedule;

import java.util.Calendar;

import org.nilriri.LunaCalendar.R;
import org.nilriri.LunaCalendar.RefreshManager;
import org.nilriri.LunaCalendar.dao.ScheduleDaoImpl;
import org.nilriri.LunaCalendar.dao.Constants.Schedule;
import org.nilriri.LunaCalendar.tools.Common;
import org.nilriri.LunaCalendar.tools.Lunar2Solar;
import org.nilriri.LunaCalendar.tools.OldEvent;
import org.nilriri.LunaCalendar.tools.Prefs;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnTouchListener;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;

public class SearchResult extends ExpandableListActivity implements OnTouchListener, RefreshManager {

    private static final String DAYNAMES[] = { "토", "일", "월", "화", "수", "목", "금", "토" };
    public static final int MENU_ITEM_EDITCHEDULE = Menu.FIRST;
    public static final int MENU_ITEM_DELSCHEDULE = Menu.FIRST + 1;
    public static final int MENU_ITEM_SCHEDULELIST = Menu.FIRST + 2;
    public static final int MENU_ITEM_WEEKSCHEDULE = Menu.FIRST + 3;
    public static final int MENU_ITEM_MONTHSCHEDULE = Menu.FIRST + 4;
    public static final int MENU_ITEM_ALLSCHEDULE = Menu.FIRST + 5;
    public static final int MENU_ITEM_GCALADDEVENT = Menu.FIRST + 6;
    public static final int MENU_ITEM_ADDCHEDULE = Menu.FIRST + 7;
    public static final int MENU_ITEM_GCALIMPORT = Menu.FIRST + 8;
    public static final int MENU_ITEM_EXPAND = Menu.FIRST + 9;

    private Calendar mCalendar;
    private OldEvent mOldEvent;
    private String mSearchRange;
    private ViewGroup mContainer;
    private boolean isSearch;
    private boolean isExpand = false;
    private int mOperator;
    private String mKeyword1;
    private String mKeyword2;

    private ExpandableListAdapter mAdapter;
    private ScheduleDaoImpl dao = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContainer = this.getExpandableListView();

        mContainer.setOnTouchListener(this);

        Intent intent = getIntent();
        Bundle data = intent.getExtras();

        mOldEvent = new OldEvent(-1, -1);
        mCalendar = (Calendar) data.get("workday");
        mSearchRange = intent.getStringExtra("ScheduleRange");
        mSearchRange = mSearchRange == null ? "" : mSearchRange;
        isSearch = intent.getBooleanExtra("isSearch", false);

        mOperator = intent.getIntExtra("operator", 0);
        mKeyword1 = intent.getStringExtra("keyword1");
        mKeyword2 = intent.getStringExtra("keyword2");

        if (mCalendar == null)
            mCalendar = Calendar.getInstance();

        registerForContextMenu(getExpandableListView());

        this.getExpandableListView().setFastScrollEnabled(true);

        this.getExpandableListView().setOnChildClickListener(new myOnChildClickListener());
    }

    public boolean onTouch(View view, MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mOldEvent.set(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_UP:

                // 전체 일정목록일때는 이전, 이후자료 검색기능이 없다.
                if ("TODAY".equals(mSearchRange) || "WEEK".equals(mSearchRange) || "MONTH".equals(mSearchRange)) {
                    if (event.getX() - mOldEvent.getX() > 50 && Math.abs(event.getY() - mOldEvent.getY()) < 90) {//Right
                        ChangeScheduleList(-1);
                    } else if (event.getX() - mOldEvent.getX() < -50 && Math.abs(event.getY() - mOldEvent.getY()) < 90) {
                        ChangeScheduleList(1);
                    }
                }
                mOldEvent.set(event.getX(), event.getY());
                break;
            default:
                return false;
        }
        return false;
    }

    private void ChangeScheduleList(int arg) {

        if ("TODAY".equals(mSearchRange)) {
            mCalendar.add(Calendar.DAY_OF_MONTH, arg);
        } else if ("WEEK".equals(mSearchRange)) {
            mCalendar.add(Calendar.WEEK_OF_YEAR, arg);
        } else if ("MONTH".equals(mSearchRange)) {
            mCalendar.add(Calendar.MONTH, arg);
        }

        ScheduleLoading();
    }

    public class myOnChildClickListener implements OnChildClickListener {

        public boolean onChildClick(ExpandableListView expandablelistview, View view, int i, int j, long id) {
            Intent intent = new Intent();
            intent.setClass(getBaseContext(), ScheduleViewer.class);
            intent.putExtra("id", id);
            startActivity(intent);
            return false;
        }
    }

    private void ScheduleLoading() throws IllegalArgumentException {

        Cursor groupCursor = null;

        String date = "";

        if ("TODAY".equals(mSearchRange)) {
            date = Common.fmtDate(mCalendar);

            StringBuilder title = new StringBuilder(getResources().getString(R.string.schedule_todaylist_label));
            title.append(" (" + date + " / " + DAYNAMES[mCalendar.get(Calendar.DAY_OF_WEEK)]);
            title.append(" / 음력 " + Common.fmtDate(Lunar2Solar.s2l(mCalendar)).substring(5));
            title.append(")");
            this.setTitle(title.toString());

        } else if ("WEEK".equals(mSearchRange)) {
            date = Common.fmtDate(mCalendar);

            Calendar c = Calendar.getInstance();
            c.setFirstDayOfWeek(Calendar.SUNDAY);
            c.set(mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));

            c.add(Calendar.DAY_OF_MONTH, ((c.get(Calendar.DAY_OF_WEEK) - 1) * -1));

            String lStart = Common.fmtDate(c);
            c.add(Calendar.DAY_OF_MONTH, 6);
            String lEnd = Common.fmtDate(c);

            StringBuilder title = new StringBuilder(getResources().getString(R.string.schedule_weeklist_label));

            title.append("(" + lStart + "~" + lEnd);
            title.append(" / " + mCalendar.get(Calendar.WEEK_OF_YEAR) + "주");
            title.append(")");

            this.setTitle(title.toString());

        } else if ("MONTH".equals(mSearchRange)) {
            date = Common.fmtDate(mCalendar).substring(0, 7);
            this.setTitle(getResources().getString(R.string.schedule_monthlist_label) + " (" + date + ")");
        } else if (isSearch) {
            this.setTitle("검색결과");
        } else {
            this.setTitle(getResources().getString(R.string.schedule_alllist_label));
        }

        groupCursor = dao.queryGroup(mSearchRange, date, isSearch, mOperator, mKeyword1, mKeyword2);

        mAdapter = new MyExpandableListAdapter(groupCursor, this, //
                R.layout.search_groupitem, //
                R.layout.search_childitem, //
                new String[] { Schedule.SCHEDULE_DATE, Schedule.SCHEDULE_TITLE }, //
                new int[] { R.id.schedule_date, R.id.schedule_title }, //
                new String[] { Schedule.SCHEDULE_CONTENTS, Schedule.ALARM_DETAILINFO, Schedule.DDAY_DETAILINFO, },//
                new int[] { R.id.schedule_contents, R.id.alarm_detailinfo, R.id.dday_detailinfo });
        setListAdapter(mAdapter);

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

        dao = new ScheduleDaoImpl(this, null, Prefs.getSDCardUse(this));

        this.isExpand = false;

        ScheduleLoading();
    }

    private class MyExpandableListAdapter extends SimpleCursorTreeAdapter {
        private LayoutInflater mInflater;
        private Bitmap flag0;
        private Bitmap flag1;
        private Bitmap flag3;
        private Bitmap flag4;
        private Bitmap dday;
        private Bitmap pen;

        public MyExpandableListAdapter(//
                Cursor cursor, //
                Context context, //
                int groupLayout, int childLayout,// 
                String[] groupFrom, int[] groupTo, //
                String[] childrenFrom, int[] childrenTo) {
            super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childrenFrom, childrenTo);

            mInflater = LayoutInflater.from(context);

            // Icons bound to the rows.
            flag0 = BitmapFactory.decodeResource(context.getResources(), R.drawable.flag0);
            flag1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.flag1);
            flag3 = BitmapFactory.decodeResource(context.getResources(), R.drawable.flag3);
            flag4 = BitmapFactory.decodeResource(context.getResources(), R.drawable.flag4);
            dday = BitmapFactory.decodeResource(context.getResources(), R.drawable.dday);
            pen = BitmapFactory.decodeResource(context.getResources(), R.drawable.pen);

        }

        class GroupHolder {
           // RelativeLayout itemLine;
            ImageView clock;
            ImageView glass;
            TextView date;
            TextView title;
        }

        class ChildHolder {
            LinearLayout alarm_line;
            LinearLayout dday_line;
            ImageView icon;
            TextView contents;
            TextView alarm;
            TextView dday;
        }

        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {

            Long id = groupCursor.getLong(groupCursor.getColumnIndexOrThrow(Schedule._ID));
            Cursor childCursor = dao.querySearchChild(id);
            return childCursor;
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            GroupHolder groupHolder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.search_groupitem, null);

                groupHolder = new GroupHolder();

               // groupHolder.itemLine = (RelativeLayout) convertView.findViewById(R.id.schedule_group);
                groupHolder.clock = (ImageView) convertView.findViewById(R.id.clock_flags);
                groupHolder.glass = (ImageView) convertView.findViewById(R.id.glass_flags);
                groupHolder.date = (TextView) convertView.findViewById(R.id.schedule_date);
                groupHolder.title = (TextView) convertView.findViewById(R.id.schedule_title);

                convertView.setTag(groupHolder);
            } else {
                groupHolder = (GroupHolder) convertView.getTag();
            }

            Cursor c = super.getGroup(groupPosition);

            int repeat = c.getInt(c.getColumnIndexOrThrow(Schedule.SCHEDULE_REPEAT));

            if (repeat > 0 && repeat < 7) {
                groupHolder.clock.setVisibility(View.VISIBLE);
            } else {
                groupHolder.clock.setVisibility(View.GONE);
            }
            String glass = c.getString(c.getColumnIndexOrThrow(Schedule.ANNIVERSARY));

            if ("Y".equals(glass)) {
                groupHolder.glass.setVisibility(View.VISIBLE);
            } else {
                groupHolder.glass.setVisibility(View.GONE);
            }

            String lunaryn = c.getString(c.getColumnIndexOrThrow(Schedule.LUNARYN));
            //Log.d(Common.TAG, "lunaryn=" + lunaryn);
            String sdate = "";
            String ldate = "";
            if ("Y".equals(lunaryn)) {
                //Log.d(Common.TAG, "lunaryn=" + lunaryn);
                ldate = c.getString(c.getColumnIndexOrThrow(Schedule.SCHEDULE_LDATE));
                sdate = Common.fmtDate(Lunar2Solar.l2s(ldate));
                //Log.d(Common.TAG, "sdate=" + sdate);
                //Log.d(Common.TAG, "ldate=" + ldate);
            } else {
                sdate = c.getString(c.getColumnIndexOrThrow(Schedule.SCHEDULE_DATE));
                ldate = Lunar2Solar.s2l(sdate);
            }
            String schedule_date = "";

            String title = c.getString(c.getColumnIndexOrThrow(Schedule.SCHEDULE_TITLE));

            if ("ALL".equals(mSearchRange)) {
                schedule_date += sdate;

                int dayindex = c.getInt(c.getColumnIndexOrThrow("dayindex"));
                schedule_date += "\n" + DAYNAMES[dayindex];

                schedule_date += " (" + Common.fmtDate(Lunar2Solar.s2l(sdate)).substring(5) + ")";

            } else if ("MONTH".equals(mSearchRange)) {
                schedule_date += sdate.substring(8);
                if ("Y".equals(lunaryn)) {

                    schedule_date += " [" + DAYNAMES[Common.getCalValue(Calendar.DAY_OF_WEEK, sdate)] + "]";

                } else {

                    int dayindex = c.getInt(c.getColumnIndexOrThrow("dayindex"));
                    schedule_date += " [" + DAYNAMES[dayindex] + "]";

                }
                schedule_date += "\n (" + Common.fmtDate(ldate).substring(5) + ")";

            } else if ("WEEK".equals(mSearchRange)) {
                schedule_date += sdate.substring(8);

                if ("Y".equals(lunaryn)) {

                    schedule_date += " [" + DAYNAMES[Common.getCalValue(Calendar.DAY_OF_WEEK, sdate)] + "]";

                } else {
                    int dayindex = c.getInt(c.getColumnIndexOrThrow("dayindex"));
                    schedule_date += " [" + DAYNAMES[dayindex] + "]";
                }

                schedule_date += "\n (" + Common.fmtDate(ldate).substring(5) + ")";

            } else if ("TODAY".equals(mSearchRange)) {
                String time = c.getString(c.getColumnIndexOrThrow(Schedule.ALARM_TIME));

                if ("".equals(time) || "00:00".equals(time)) {
                    schedule_date = "";
                } else {
                    int hour = Integer.parseInt(time.substring(0, 2));
                    int minute = Integer.parseInt(time.substring(3));
                    if (hour > 12) {
                        schedule_date = "PM " + (hour - 12) + ":" + minute;
                    } else {
                        schedule_date = "AM " + hour + ":" + minute;
                    }
                }
            }
            groupHolder.date.setText(schedule_date);

            groupHolder.title.setText(title);

            return convertView;
        }

        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            ChildHolder childHolder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.search_childitem, null);

                childHolder = new ChildHolder();
                childHolder.alarm_line = (LinearLayout) convertView.findViewById(R.id.alarm_line);
                childHolder.dday_line = (LinearLayout) convertView.findViewById(R.id.dday_line);
                childHolder.icon = (ImageView) convertView.findViewById(R.id.flags);
                childHolder.contents = (TextView) convertView.findViewById(R.id.schedule_contents);
                childHolder.alarm = (TextView) convertView.findViewById(R.id.alarm_detailinfo);
                childHolder.dday = (TextView) convertView.findViewById(R.id.dday_detailinfo);

                convertView.setTag(childHolder);
            } else {
                childHolder = (ChildHolder) convertView.getTag();
            }

            Cursor childCursor = super.getChild(groupPosition, childPosition);
            String contents = childCursor.getString(childCursor.getColumnIndexOrThrow(Schedule.SCHEDULE_CONTENTS));
            if ("".equals(contents)) {
                // childHolder.contents_line.setVisibility(View.GONE);
                childHolder.contents.setTextColor(getResources().getColor(R.color.disable_text));
                childHolder.contents.setText("(내용없음)");
            } else {
                // childHolder.contents_line.setVisibility(View.VISIBLE);
                childHolder.contents.setTextColor(getResources().getColor(android.R.color.darker_gray));
                childHolder.contents.setText(contents);
            }

            String alarm = childCursor.getString(childCursor.getColumnIndexOrThrow(Schedule.ALARM_DETAILINFO));
            if ("".equals(alarm)) {
                childHolder.alarm_line.setVisibility(View.GONE);
            } else {
                childHolder.alarm_line.setVisibility(View.VISIBLE);
                childHolder.alarm.setText(alarm);
            }

            String ddayinfo = childCursor.getString(childCursor.getColumnIndexOrThrow(Schedule.DDAY_DETAILINFO));
            if ("".equals(ddayinfo)) {
                childHolder.dday_line.setVisibility(View.GONE);
            } else {
                childHolder.dday_line.setVisibility(View.VISIBLE);
                childHolder.dday.setText(ddayinfo);
            }

            childHolder.icon.setVisibility(View.VISIBLE);
            switch (childCursor.getInt(childCursor.getColumnIndexOrThrow(Schedule.SCHEDULE_KIND))) {
                case 0:
                    childHolder.icon.setImageBitmap(flag0);
                    break;
                case 1:
                    childHolder.icon.setImageBitmap(flag1);
                    break;
                case 3:
                    childHolder.icon.setImageBitmap(flag3);
                    break;
                case 5:
                    childHolder.icon.setImageBitmap(dday);
                    break;
                case 6:
                    childHolder.icon.setImageBitmap(pen);
                    break;
                default:
                    childHolder.icon.setImageBitmap(flag4);
            }

            return convertView;
        }

    }

    /*
    public class MyExpandableListAdapter extends SimpleCursorTreeAdapter {

        public MyExpandableListAdapter(//
                Cursor cursor, //
                Context context, //
                int groupLayout, int childLayout,// 
                String[] groupFrom, int[] groupTo, //
                String[] childrenFrom, int[] childrenTo) {
            super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childrenFrom, childrenTo);
        }

        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            Long id = groupCursor.getLong(mGroupIdColumnIndex);
            Cursor childCursor = dao.querySearchChild(id);
            return childCursor;
        }

        public TextView getGenericView() {
            AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 64);
            TextView textView = new TextView(SearchResult.this);
            textView.setLayoutParams(lp);
            textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            textView.setPadding(36, 0, 0, 0);
            return textView;
        }
    }
    */
    /*
     * Context Menu       
     */

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;

        Cursor cursor = null;
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        int groupPos = 0;
        int childPos = 0;
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            childPos = ExpandableListView.getPackedPositionChild(info.packedPosition);
            cursor = (Cursor) mAdapter.getChild(groupPos, childPos);
        } else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            cursor = (Cursor) mAdapter.getGroup(groupPos);

        }
        menu.setHeaderTitle(cursor.getString(cursor.getColumnIndexOrThrow(Schedule.SCHEDULE_TITLE)));

        String anniversary = getResources().getString(R.string.anniversary_label);

        Cursor c = (Cursor) mAdapter.getGroup(groupPos);
        if (!anniversary.equals(c.getString(1)) && !"B-Plan".equals(c.getString(1))) {
            menu.add(0, MENU_ITEM_EDITCHEDULE, 0, R.string.schedule_modify_action);
            menu.add(0, MENU_ITEM_DELSCHEDULE, 0, R.string.schedule_delete_label);
        }

        if ("F".equals(c.getString(3)) || "P".equals(c.getString(3))) {
            try {

                Intent intent = new Intent();

                intent.setAction("org.nilriri.webbibles.VIEW");
                intent.setType("vnd.org.nilriri/web-bible");

                intent.putExtra("VERSION", 0);
                intent.putExtra("VERSION2", 0);
                intent.putExtra("BOOK", c.getInt(4));
                intent.putExtra("CHAPTER", c.getInt(5));
                intent.putExtra("VERSE", 0);

                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(getBaseContext(), "온라인성경 앱일 설치되어있지 않거나 최신버젼이 아닙니다.", Toast.LENGTH_LONG).show();

            }

        }

        if ("auto".equals(Prefs.getSyncMethod(this)) // 동기화 방법 
                && !"".equals(Prefs.getSyncCalendar(this))) {
            menu.add(0, MENU_ITEM_GCALADDEVENT, 0, R.string.schedule_gcaladdevent_label);
            menu.add(0, MENU_ITEM_GCALIMPORT, 0, R.string.schedule_gcalimport_label);
        }

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
        Cursor cursor = null;

        Long id = new Long(-1);

        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            int childPos = ExpandableListView.getPackedPositionChild(info.packedPosition);
            cursor = (Cursor) mAdapter.getChild(groupPos, childPos);
            id = cursor.getLong(0);
        } else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            cursor = (Cursor) mAdapter.getGroup(groupPos);
            id = cursor.getLong(0);
        }

        switch (item.getItemId()) {

            case MENU_ITEM_EDITCHEDULE: {
                String anniversary = getResources().getString(R.string.anniversary_label);

                if (anniversary.equals(cursor.getString(1))) {
                    Intent intent = new Intent();
                    intent.setClass(this, ScheduleViewer.class);

                    intent.putExtra("id", id);

                    startActivity(intent);
                } else {
                    Intent intent = new Intent();
                    intent.setClass(this, ScheduleEditor.class);

                    intent.putExtra("SID", id);
                    intent.putExtra("STODAY", mCalendar);

                    startActivity(intent);
                }
                return true;
            }
            case MENU_ITEM_DELSCHEDULE: {
                dao.syncDelete(id, this);
                return true;
            }
            case MENU_ITEM_GCALADDEVENT: {
                dao.syncInsert(id, this);
                return true;
            }

            case MENU_ITEM_GCALIMPORT: {
                dao.syncImport(this);
                return true;
            }
        }
        return false;
    }

    /*
     * Option Menu
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // This is our one standard application action -- inserting a
        // new note into the list.
        MenuItem item0 = menu.add(0, MENU_ITEM_ADDCHEDULE, 0, R.string.schedule_add_label);
        item0.setIcon(android.R.drawable.ic_menu_add);

        if ("TODAY".equals(mSearchRange)) {
            MenuItem item1 = menu.add(0, MENU_ITEM_WEEKSCHEDULE, 0, R.string.schedule_weeklist_label);
            item1.setIcon(android.R.drawable.ic_menu_agenda);

            MenuItem item2 = menu.add(0, MENU_ITEM_MONTHSCHEDULE, 0, R.string.schedule_monthlist_label);
            item2.setIcon(android.R.drawable.ic_menu_agenda);

            MenuItem item3 = menu.add(0, MENU_ITEM_ALLSCHEDULE, 0, R.string.schedule_alllist_label);
            item3.setIcon(android.R.drawable.ic_menu_agenda);
        } else if ("WEEK".equals(mSearchRange)) {
            MenuItem item1 = menu.add(0, MENU_ITEM_SCHEDULELIST, 0, R.string.schedule_todaylist_label);
            item1.setIcon(android.R.drawable.ic_menu_agenda);

            MenuItem item2 = menu.add(0, MENU_ITEM_MONTHSCHEDULE, 0, R.string.schedule_monthlist_label);
            item2.setIcon(android.R.drawable.ic_menu_agenda);

            MenuItem item3 = menu.add(0, MENU_ITEM_ALLSCHEDULE, 0, R.string.schedule_alllist_label);
            item3.setIcon(android.R.drawable.ic_menu_agenda);
        } else if ("MONTH".equals(mSearchRange)) {
            MenuItem item1 = menu.add(0, MENU_ITEM_SCHEDULELIST, 0, R.string.schedule_todaylist_label);
            item1.setIcon(android.R.drawable.ic_menu_agenda);

            MenuItem item2 = menu.add(0, MENU_ITEM_WEEKSCHEDULE, 0, R.string.schedule_weeklist_label);
            item2.setIcon(android.R.drawable.ic_menu_agenda);

            MenuItem item3 = menu.add(0, MENU_ITEM_ALLSCHEDULE, 0, R.string.schedule_alllist_label);
            item3.setIcon(android.R.drawable.ic_menu_agenda);
        } else {
            MenuItem item1 = menu.add(0, MENU_ITEM_SCHEDULELIST, 0, R.string.schedule_todaylist_label);
            item1.setIcon(android.R.drawable.ic_menu_agenda);

            MenuItem item2 = menu.add(0, MENU_ITEM_WEEKSCHEDULE, 0, R.string.schedule_weeklist_label);
            item2.setIcon(android.R.drawable.ic_menu_agenda);

            MenuItem item3 = menu.add(0, MENU_ITEM_MONTHSCHEDULE, 0, R.string.schedule_monthlist_label);
            item3.setIcon(android.R.drawable.ic_menu_agenda);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case MENU_ITEM_ADDCHEDULE: {

                Intent intent = new Intent();
                intent.setClass(this, ScheduleEditor.class);

                intent.putExtra("SID", new Long(0));
                intent.putExtra("STODAY", mCalendar);
                startActivity(intent);

                return true;
            }
            case MENU_ITEM_SCHEDULELIST: {

                mSearchRange = "TODAY";

                this.ScheduleLoading();

                return true;
            }
            case MENU_ITEM_WEEKSCHEDULE: {

                mSearchRange = "WEEK";

                this.ScheduleLoading();

                return true;
            }
            case MENU_ITEM_MONTHSCHEDULE: {

                mSearchRange = "MONTH";

                this.ScheduleLoading();

                return true;
            }

            case MENU_ITEM_ALLSCHEDULE: {
                mSearchRange = "ALL";

                this.ScheduleLoading();

                return true;
            }

            case MENU_ITEM_GCALIMPORT: {

                dao.syncImport(this);

                return true;

            }

            case MENU_ITEM_EXPAND: {
                isExpand = !isExpand;
                for (int i = 0; i < getExpandableListAdapter().getGroupCount(); i++) {
                    if (isExpand) {
                        item.setTitle("축소");
                        getExpandableListView().expandGroup(i);
                    } else {
                        item.setTitle("확장");
                        getExpandableListView().collapseGroup(i);
                    }
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
        menu.removeItem(MENU_ITEM_GCALIMPORT);
        menu.removeItem(R.id.settings);

        if ("TODAY".equals(mSearchRange)) {
            menu.removeItem(MENU_ITEM_SCHEDULELIST);
            if (menu.findItem(MENU_ITEM_MONTHSCHEDULE) == null) {
                MenuItem item1 = menu.add(0, MENU_ITEM_MONTHSCHEDULE, 0, R.string.schedule_monthlist_label);
                item1.setIcon(android.R.drawable.ic_menu_agenda);
            }
            if (menu.findItem(MENU_ITEM_WEEKSCHEDULE) == null) {
                MenuItem item2 = menu.add(0, MENU_ITEM_WEEKSCHEDULE, 0, R.string.schedule_weeklist_label);
                item2.setIcon(android.R.drawable.ic_menu_agenda);
            }
            if (menu.findItem(MENU_ITEM_ALLSCHEDULE) == null) {
                MenuItem item3 = menu.add(0, MENU_ITEM_ALLSCHEDULE, 0, R.string.schedule_alllist_label);
                item3.setIcon(android.R.drawable.ic_menu_agenda);
            }
        } else if ("WEEK".equals(mSearchRange)) {
            menu.removeItem(MENU_ITEM_WEEKSCHEDULE);
            if (menu.findItem(MENU_ITEM_SCHEDULELIST) == null) {
                MenuItem item1 = menu.add(0, MENU_ITEM_SCHEDULELIST, 0, R.string.schedule_todaylist_label);
                item1.setIcon(android.R.drawable.ic_menu_agenda);
            }
            if (menu.findItem(MENU_ITEM_MONTHSCHEDULE) == null) {
                MenuItem item2 = menu.add(0, MENU_ITEM_MONTHSCHEDULE, 0, R.string.schedule_monthlist_label);
                item2.setIcon(android.R.drawable.ic_menu_agenda);
            }
            if (menu.findItem(MENU_ITEM_ALLSCHEDULE) == null) {
                MenuItem item3 = menu.add(0, MENU_ITEM_ALLSCHEDULE, 0, R.string.schedule_alllist_label);
                item3.setIcon(android.R.drawable.ic_menu_agenda);
            }
        } else if ("MONTH".equals(mSearchRange)) {
            menu.removeItem(MENU_ITEM_MONTHSCHEDULE);

            if (menu.findItem(MENU_ITEM_SCHEDULELIST) == null) {
                MenuItem item1 = menu.add(0, MENU_ITEM_SCHEDULELIST, 0, R.string.schedule_todaylist_label);
                item1.setIcon(android.R.drawable.ic_menu_agenda);
            }
            if (menu.findItem(MENU_ITEM_WEEKSCHEDULE) == null) {
                MenuItem item2 = menu.add(0, MENU_ITEM_WEEKSCHEDULE, 0, R.string.schedule_weeklist_label);
                item2.setIcon(android.R.drawable.ic_menu_agenda);
            }
            if (menu.findItem(MENU_ITEM_ALLSCHEDULE) == null) {
                MenuItem item3 = menu.add(0, MENU_ITEM_ALLSCHEDULE, 0, R.string.schedule_alllist_label);
                item3.setIcon(android.R.drawable.ic_menu_agenda);
            }
        } else {
            menu.removeItem(MENU_ITEM_ALLSCHEDULE);

            if (menu.findItem(MENU_ITEM_SCHEDULELIST) == null) {
                MenuItem item1 = menu.add(0, MENU_ITEM_SCHEDULELIST, 0, R.string.schedule_todaylist_label);
                item1.setIcon(android.R.drawable.ic_menu_agenda);
            }
            if (menu.findItem(MENU_ITEM_WEEKSCHEDULE) == null) {
                MenuItem item2 = menu.add(0, MENU_ITEM_WEEKSCHEDULE, 0, R.string.schedule_weeklist_label);
                item2.setIcon(android.R.drawable.ic_menu_agenda);
            }
            if (menu.findItem(MENU_ITEM_MONTHSCHEDULE) == null) {
                MenuItem item3 = menu.add(0, MENU_ITEM_MONTHSCHEDULE, 0, R.string.schedule_monthlist_label);
                item3.setIcon(android.R.drawable.ic_menu_agenda);
            }
        }

        if ("ALL".equals(mSearchRange)) {
            menu.removeItem(MENU_ITEM_EXPAND);
        } else {
            if (menu.findItem(MENU_ITEM_EXPAND) == null) {
                MenuItem expand = menu.add(0, MENU_ITEM_EXPAND, 0, "확장").setCheckable(true);
                expand.setIcon(android.R.drawable.ic_menu_slideshow);
            }
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return true;
    }

    public void refresh() {
        this.ScheduleLoading();
    }

}
