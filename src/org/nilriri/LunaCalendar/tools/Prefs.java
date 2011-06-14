package org.nilriri.LunaCalendar.tools;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.nilriri.LunaCalendar.R;
import org.nilriri.LunaCalendar.gcal.CalendarEntry;
import org.nilriri.LunaCalendar.gcal.GoogleUtil;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;

public class Prefs extends PreferenceActivity {
    // Option names and default values
    private static final String OPT_LUNAICON = "lunaicon";
    private static final boolean OPT_LUNAICON_DEF = true;

    private static final String OPT_LUNADAYS = "lunadays";
    private static final boolean OPT_LUNADAYS_DEF = true;

    private static final String OPT_ALARMCHECK = "alarmcheck";
    private static final boolean OPT_ALARMCHECK_DEF = true;

    private static final String OPT_RINGTONE = "ringtone";

    private static final String OPT_SOUND = "sound";
    private static final boolean OPT_SOUND_DEF = true;

    private static final String OPT_VIBRATE = "vibrate";
    private static final boolean OPT_VIBRATE_DEF = false;

    private static final String OPT_LEDLIGHT = "ledlight";
    private static final boolean OPT_LEDLIGHT_DEF = false;

    private static final String OPT_ANNIVERSARY = "anniversary";
    private static final boolean OPT_ANNIVERSARY_DEF = false;

    private static final String OPT_SDCARDUSE = "sdcarduse";
    private static final boolean OPT_SDCARDUSE_DEF = false;

    private static final String OPT_WIDGETCOLOR = "widgetcolor";
    private static final String OPT_WIDGETCOLOR_DEF = "1";

    private static final String OPT_ACCOUNTS = "accounts";
    private static final String OPT_ACCOUNTS_DEF = "";

    private static final String OPT_CALENDARS = "calendars";
    private static final String OPT_CALENDARS_DEF = "";

    private static final String OPT_ONLINECALENDARS = "onlinecalendars";
    private static final String OPT_ONLINECALENDARS_DEF = "";

    private static final String OPT_AUTHTOKEN = "authtoken";
    private static final String OPT_CALLIST = "callist";

    private static final String OPT_SYNCMETHOD = "syncmethod";
    private static final String OPT_SYNCMETHOD_DEF = "stop";

 
    private ListPreference accounts;
    private ListPreference calendars;
    private ListPreference onlinecalendars;
    private ListPreference syncmethod;
    private CheckBoxPreference sound;

    protected AccountManager manager;

    private static final int DIALOG_ACCOUNTS = 0;
    private static final int REQUEST_AUTHENTICATE = 1;
    private static final int REQUEST_RINGTONE = 2;

    public static ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);

        accounts = (ListPreference) findPreference("accounts");
        CharSequence entries[];

        manager = AccountManager.get(this);
        Account[] acc = manager.getAccountsByType("com.google");

        final int size = acc.length;
        entries = new String[size];
        for (int i = 0; i < size; i++) {
            entries[i] = acc[i].name;
        }
        accounts.setEntries(entries);
        accounts.setEntryValues(entries);
        accounts.setSummary(getAccountName(getBaseContext()));
        accounts.setOnPreferenceChangeListener(new accountPreferenceChangeListener());

        // 동기화 대상 캘린더 목록을 설정한다.
        calendars = (ListPreference) findPreference("calendars");
        onlinecalendars = (ListPreference) findPreference("onlinecalendars");
        syncmethod = (ListPreference) findPreference("syncmethod");
        sound = (CheckBoxPreference) findPreference("sound");

        // 저장된 캘린더 목록을 조회한다.
        String entryValues[] = getCalendars(getBaseContext());

        // 캘린더를 목록에 설정한다.
        setCalendarList(entryValues);

        // 선택된 캘린더 명을 표시한다.
        setSummary(calendars);
        setSummary(onlinecalendars);
        setSummary(syncmethod);

        calendars.setOnPreferenceChangeListener(new myOnPreferenceChangeListener());
        onlinecalendars.setOnPreferenceChangeListener(new myOnPreferenceChangeListener());
        syncmethod.setOnPreferenceChangeListener(new myOnPreferenceChangeListener());

        sound.setOnPreferenceClickListener(new myOnPreferenceClickListener());
        if (!"".equals(Prefs.getRingtone(getBaseContext()))) {
            sound.setSummary(Prefs.getRingtone(getBaseContext()));
        }

        findPreference("sdcarduse").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                CheckBoxPreference cpf = (CheckBoxPreference) preference;
                if (cpf.isChecked()) {
                    //Log.d(Common.TAG, "==========외부메모리 사용=========");
                    if (!Common.isSdPresent()) { //sd카드 사용 불가능 상태이면...

                        PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().putBoolean(OPT_SDCARDUSE, false).commit();

                        Toast.makeText(getBaseContext(), getBaseContext().getResources().getString(R.string.sdcarduse_notinstall), Toast.LENGTH_LONG).show();
                    } else {

                        //Log.d(Common.TAG, "==========외부메모리 사용2=========");

                        Toast.makeText(getBaseContext(), "스케쥴 정보 복사중...", Toast.LENGTH_LONG).show();
                        // backup
                        DataManager.StartCopy(Prefs.this, true);
                    }
                } else {
                    // restore
                    //Log.d(Common.TAG, "==========내부메모리 사용=========");
                    DataManager.StartCopy(Prefs.this, false);
                }
                return false;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        Common.startAlarmNotifyService(getBaseContext());

    }

    public class myOnPreferenceClickListener implements OnPreferenceClickListener {
        public boolean onPreferenceClick(Preference preference) {
            CheckBoxPreference cpf = (CheckBoxPreference) preference;

            if (cpf.isChecked()) {

                String uri = Prefs.getRingtone(getBaseContext());
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);

                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "알림음 선택");

                if (uri != null) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(uri));
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                }
                startActivityForResult(intent, REQUEST_RINGTONE);
            }
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case REQUEST_AUTHENTICATE:
                if (resultCode == RESULT_OK) {
                    gotAccount(false);
                }
                break;
            case REQUEST_RINGTONE:
                if (resultCode == RESULT_OK) {
                    Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

                    if (uri != null) {
                        setRingtone(getBaseContext(), uri.toString());

                        sound.setSummary(uri.toString());
                    }
                }
                break;
        }
    }

    public class accountPreferenceChangeListener implements OnPreferenceChangeListener {
        public boolean onPreferenceChange(Preference preference, Object obj) {
            ListPreference listPref = (ListPreference) preference;

            if (!obj.toString().equals(getSyncCalendar(getBaseContext()))) {

                // 변경된 계정정보를 셋팅.
                setAccountName(getBaseContext(), obj.toString());

                new BackgroundTask().execute();

            }

            listPref.setSummary((String) obj.toString());
            return true;
        }
    }

    public class myOnPreferenceChangeListener implements OnPreferenceChangeListener {
        public boolean onPreferenceChange(Preference preference, Object obj) {
            ListPreference listPref = (ListPreference) preference;

            int idx = listPref.findIndexOfValue((String) obj.toString());
            listPref.setSummary(listPref.getEntries()[idx]);
            return true;
        }
    }

    public void setSummary(ListPreference lp) {
        int selectIndex = lp.findIndexOfValue(lp.getValue());
        if (selectIndex >= 0) {
            lp.setSummary(lp.getEntries()[selectIndex]);
        }
    }

    private void setCalendarList(String[] calArrays) {
        String calEntries[] = new String[calArrays.length];
        String calEntryValues[] = new String[calArrays.length];

        String calEntries2[] = new String[calEntries.length + 1];
        String calEntryValues2[] = new String[calEntryValues.length + 1];

        for (int i = 0; i < calArrays.length; i++) {
            String items[] = Common.tokenFn(calArrays[i], "|");
            if (items.length > 1) {
                calEntries[i] = items[0];
                calEntryValues[i] = items[1];

                calEntries2[i + 1] = items[0];
                calEntryValues2[i + 1] = items[1];
            } else {
                calendars.setEnabled(false);
                onlinecalendars.setEnabled(false);
                return;
            }
        }

        if (calEntries.length > 0) {
            calendars.setEntries(calEntries);
            calendars.setEntryValues(calEntryValues);
            calendars.setEnabled(true);

            calEntries2[0] = "구독취소";
            calEntryValues2[0] = "";

            onlinecalendars.setEntries(calEntries2);
            onlinecalendars.setEntryValues(calEntryValues2);
            onlinecalendars.setEnabled(true);
        } else {
            calendars.setEnabled(false);

            onlinecalendars.setEnabled(false);
        }

    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_ACCOUNTS:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Select a Google account");
                final AccountManager manager = AccountManager.get(this);
                final Account[] accounts = manager.getAccountsByType("com.google");

                final int size = accounts.length;
                String[] names = new String[size];
                for (int i = 0; i < size; i++) {
                    names[i] = accounts[i].name;
                }
                builder.setItems(names, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //loadCalendarList(manager, accounts[which]);
                    }
                });
                return builder.create();

        }
        return null;
    }

    public void gotAccount(boolean tokenExpired) {
        String accountName = getAccountName(getBaseContext());
        if (accountName != null) {
            AccountManager manager = AccountManager.get(getBaseContext());
            Account[] accounts = manager.getAccountsByType("com.google");
            int size = accounts.length;
            for (int i = 0; i < size; i++) {
                Account account = accounts[i];
                if (accountName.equals(account.name)) {
                    if (tokenExpired) {
                        manager.invalidateAuthToken("com.google", Prefs.getAuthToken(getBaseContext()));
                    }
                    new BackgroundTask().execute();
                    return;
                }
            }
        }
    }

    private class BackgroundTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;
        private String[] entryValues;
        List<CalendarEntry> cals;

        @Override
        protected void onPreExecute() {
            entryValues = new String[0];
            cals = new ArrayList<CalendarEntry>();

            dialog = ProgressDialog.show(Prefs.this, "", "Connecting to google...", true);

            // 기존 선택된 계정의 캘린더 목록초기화
            setCalendars(getBaseContext(), new String[] { "" });

            // 기존선택값 삭제.
            setSyncCalendar(getBaseContext(), "");

            calendars.setEnabled(false);
            calendars.setSummary("");

        }

        @Override
        protected Void doInBackground(Void... params) {

            try {

                final Bundle bundle = manager.getAuthToken(getAccounts(getBaseContext()), Common.AUTH_TOKEN_TYPE, true, null, null).getResult();
                if (bundle.containsKey(AccountManager.KEY_INTENT)) {
                    Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
                    int flags = intent.getFlags();
                    flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
                    intent.setFlags(flags);
                    startActivityForResult(intent, REQUEST_AUTHENTICATE);
                } else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
                    String authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    setAuthToken(getBaseContext(), authToken);

                    GoogleUtil gu = new GoogleUtil(getAuthToken(getBaseContext()));

                    cals.addAll(gu.getCalendarList());
                    int numCalendars = cals.size();
                    entryValues = new String[numCalendars];
                    for (int i = 0; i < numCalendars; i++) {
                        entryValues[i] = cals.get(i).title + "|" + cals.get(i).getEventFeedLink();
                    }

                }

            } catch (Exception e) {
                dialog.dismiss();
                cancel(true);
                handleException(e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // 계정이 바뀔때 로드된 캘린더 목록을 저장해 둔다.
            setCalendars(getBaseContext(), entryValues);

            // 캘린더 목록을 설정한다.
            setCalendarList(entryValues);

            // 첫번째 캘린더로 기본값을 지정한다.
            if (cals.size() > 0) {
                calendars.setValue(cals.get(0).getEventFeedLink());
                calendars.setSummary(cals.get(0).title);
            }
            dialog.dismiss();
        }
    }

    void handleException(Exception e) {
        if (e instanceof HttpResponseException) {
            HttpResponse response = ((HttpResponseException) e).response;
            int statusCode = response.statusCode;

            try {
                response.ignore();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            if (statusCode == 401 || statusCode == 403) {
                gotAccount(true);
                return;
            }
        } else if (e instanceof UnknownHostException) {
            //Log.e(Common.TAG, e.getMessage(), e);
        } else {
            e.printStackTrace();
        }
    }

    public static boolean getLunaIcon(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_LUNAICON, OPT_LUNAICON_DEF);
    }

    public static boolean getLunaDays(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_LUNADAYS, OPT_LUNADAYS_DEF);
    }

    public static boolean getAlarmCheck(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_ALARMCHECK, OPT_ALARMCHECK_DEF);
    }

    public static boolean getSound(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_SOUND, OPT_SOUND_DEF);
    }

    public static void setRingtone(Context context, String uri) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(OPT_RINGTONE, uri).commit();
    }

    public static String getRingtone(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_RINGTONE, "");
    }

    public static boolean getVibrate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_VIBRATE, OPT_VIBRATE_DEF);
    }

    public static boolean getLedlight(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_LEDLIGHT, OPT_LEDLIGHT_DEF);
    }

    public static boolean getAnniversary(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_ANNIVERSARY, OPT_ANNIVERSARY_DEF);
    }

    public static void setAccountName(Context context, String value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(OPT_ACCOUNTS, value).commit();
    }

    public static String getAccountName(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_ACCOUNTS, OPT_ACCOUNTS_DEF);
    }

    public static String getAuthToken(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_AUTHTOKEN, null);
    }

    public static void setAuthToken(Context context, String value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(OPT_AUTHTOKEN, value).commit();
    }

    public static void setSyncCalendar(Context context, String value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(OPT_CALENDARS, value).commit();
    }

    public static String getSyncCalendar(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_CALENDARS, OPT_CALENDARS_DEF);
    }

    public static String[] getSyncCalendarName(Context context) {

        String entryValues[] = getCalendars(context);
        String result[] = new String[entryValues.length + 1];
        result[0] = "달력신규생성 후 작업";
        for (int i = 0; i < entryValues.length; i++) {

            result[i + 1] = Common.tokenFn(entryValues[i], "|")[0];

        }
        return result;
    }

    public static String[] getSyncCalendarValue(Context context) {

        String entryValues[] = getCalendars(context);
        String result[] = new String[entryValues.length + 1];
        result[0] = "";
        for (int i = 0; i < entryValues.length; i++) {

            result[i + 1] = Common.tokenFn(entryValues[i], "|")[1];

        }
        return result;
    }

    public static String getOnlineCalendar(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_ONLINECALENDARS, OPT_ONLINECALENDARS_DEF);
    }

    public static String getSyncMethod(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_SYNCMETHOD, OPT_SYNCMETHOD_DEF);
    }

    public static void setCalendars(Context context, CharSequence[] values) {
        String value = "";
        for (int i = 0; i < values.length; i++) {
            if (i == 0) {
                value = values[i].toString();
            } else {
                value += "," + values[i].toString();
            }
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(OPT_CALLIST, value).commit();
    }

    public static String[] getCalendars(Context context) {
        String calendars[] = Common.tokenFn(PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_CALLIST, ""), ",");
        return calendars;
    }

    public static Account getAccounts(Context context) {
        String accName = PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_ACCOUNTS, OPT_ACCOUNTS_DEF);

        AccountManager manager = AccountManager.get(context);
        Account[] acc = manager.getAccountsByType("com.google");

        for (int i = 0; i < acc.length; i++) {
            if (accName.equals(acc[i].name))
                return acc[i];
        }
        return null;
    }

    public static boolean getSDCardUse(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_SDCARDUSE, OPT_SDCARDUSE_DEF);
    }

    public static void setWidgetColor(Context context, int color) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(OPT_WIDGETCOLOR, color).commit();
    }

    public static String getWidgetColor(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_WIDGETCOLOR, OPT_WIDGETCOLOR_DEF);
    }
    
    
   
    
}
