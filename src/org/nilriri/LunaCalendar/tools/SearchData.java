package org.nilriri.LunaCalendar.tools;

import java.util.Calendar;

import org.nilriri.LunaCalendar.R;
import org.nilriri.LunaCalendar.schedule.SearchResult;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemClickListener;

public class SearchData extends Activity implements OnClickListener {

    private static final String SEARCH_KEYWORD1 = "search_keyword1";
    private static final String SEARCH_KEYWORD2 = "search_keyword2";
    private static final String SEARCH_OPERATOR = "search_operator";

    private Spinner spin_operator;

    private EditText search_keyword1;
    private EditText search_keyword2;

    // Menu item ids    
    public static final int MENU_ITEM_SAVE = Menu.FIRST;
    public static final int MENU_ITEM_PREV = Menu.FIRST + 1;
    public static final int MENU_ITEM_NEXT = Menu.FIRST + 2;
    public static final int MENU_LOAD_WEB = Menu.FIRST + 3;
    public static final int MENU_LOAD_DB = Menu.FIRST + 4;
    public static final int MENU_ITEM_NOTELIST = Menu.FIRST + 5;
    public static final int MENU_ITEM_ADDMARK = Menu.FIRST + 6;
    public static final int MENU_ITEM_MARKLIST = Menu.FIRST + 7;
    public static final int MENU_ITEM_ADDNOTE = Menu.FIRST + 8;
    public static final int MENU_ITEM_SENDSMS = Menu.FIRST + 9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        setContentView(R.layout.search_box);

        spin_operator = (Spinner) findViewById(R.id.spin_searchop);
        ArrayAdapter<CharSequence> adapter_operation = ArrayAdapter.createFromResource(this, R.array.search_operation, android.R.layout.simple_spinner_item);
        adapter_operation.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin_operator.setAdapter(adapter_operation);

        search_keyword1 = (EditText) findViewById(R.id.keyword1);
        search_keyword2 = (EditText) findViewById(R.id.keyword2);

        findViewById(R.id.btn_search).setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferences(MODE_PRIVATE).edit().putString(SEARCH_KEYWORD1, ((EditText) findViewById(R.id.keyword1)).getText().toString()).commit();
        getPreferences(MODE_PRIVATE).edit().putString(SEARCH_KEYWORD2, ((EditText) findViewById(R.id.keyword2)).getText().toString()).commit();
        getPreferences(MODE_PRIVATE).edit().putInt(SEARCH_OPERATOR, ((Spinner) findViewById(R.id.spin_searchop)).getSelectedItemPosition()).commit();
    }

    protected void onResume() {
        super.onResume();
        ((EditText) findViewById(R.id.keyword1)).setText(getPreferences(MODE_PRIVATE).getString(SEARCH_KEYWORD1, ""));
        ((EditText) findViewById(R.id.keyword2)).setText(getPreferences(MODE_PRIVATE).getString(SEARCH_KEYWORD2, ""));
        ((Spinner) findViewById(R.id.spin_searchop)).setSelection(getPreferences(MODE_PRIVATE).getInt(SEARCH_OPERATOR, 0));
    }

    public class listOnItemClickListener implements OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
            if (pos < 0)
                return;
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_search:
                Intent intent = new Intent();

                intent.setClass(this, SearchResult.class);
                final Calendar c = Calendar.getInstance();

                intent.putExtra("org.nilriri.gscheduler.workday", c);
                intent.putExtra("ScheduleRange", "ALL");
                intent.putExtra("isSearch", true);
                intent.putExtra("operator", spin_operator.getSelectedItemPosition());
                intent.putExtra("keyword1", search_keyword1.getText().toString().trim());
                intent.putExtra("keyword2", search_keyword2.getText().toString().trim());

                startActivity(intent);
                break;
        }
    }

}
