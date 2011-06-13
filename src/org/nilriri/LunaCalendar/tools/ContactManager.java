package org.nilriri.LunaCalendar.tools;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;

public class ContactManager {

    private Context mContext;

    public ContactManager(Context context) {
        mContext = context;
    }

    public ContactEvent[] getContactEvents(String month) {

        String selection = ContactsContract.Data.MIMETYPE + "=?";
        //selection += " and " + ContactsContract.CommonDataKinds.Event.TYPE + "=?";
        selection += " and substr(" + ContactsContract.CommonDataKinds.Event.START_DATE + ", -5) like '" + month.substring(5, 7) + "%' ";//            

        String value = ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE;
        //int value2 = ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY;
        String[] args = new String[] { value };//, value2 + "" };

        /*
        Cursor l = mContext.getContentResolver().query(//
                CallLog.Calls.CONTENT_URI, new String[] { //
                CallLog.Calls._ID, //
                        CallLog.Calls.CACHED_NAME, CallLog.Calls.DATE, CallLog.Calls._ID, CallLog.Calls._ID, CallLog.Calls.TYPE }, // projection
                null, null, null);

        while (l.moveToNext()) {
            for (int i = 0; i < l.getColumnCount(); i++)
                Log.e(Common.TAG, "log=" + l.getString(i));
        }
        */

        Cursor c = mContext.getContentResolver().query(//
                ContactsContract.Data.CONTENT_URI, //
                new String[] { //
                ContactsContract.CommonDataKinds.Event._ID, //
                        ContactsContract.CommonDataKinds.Event.DISPLAY_NAME,//
                        ContactsContract.CommonDataKinds.Event.START_DATE,//
                        ContactsContract.CommonDataKinds.Event.LOOKUP_KEY,//
                        ContactsContract.CommonDataKinds.Event.CONTACT_ID, //
                        ContactsContract.CommonDataKinds.Event.TYPE }, // projection
                selection, // selection - where절
                args, // selectionargs - parameter
                ContactsContract.Data.DATA4 + " ASC"); //sort by LAST_TIME_CONTACTED.

        if (c == null) {
            //error happen.
            return new ContactEvent[0];
        }

        ContactEvent[] result = new ContactEvent[c.getCount()];
        int ct = 0;
        while (c.moveToNext() == true) {

            /*
            for (int i = 0; i < c.getColumnCount(); i++) {
                Log.e("ContactEvent", "Name:" + c.getColumnName(i) + " Value: " + c.getString(i));
            }
            */

            Long id = c.getLong(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.CONTACT_ID));
            String name = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.DISPLAY_NAME));
            String date = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.START_DATE));
            int type = c.getInt(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.TYPE));

            if (date.length() >= 5) {
                // 월-일 형태로 가공한다.
                date = date.substring(date.length() - 5);
            } else {
                //날짜형태가 잘못되어 캘린더에 표시할수 없으므로  skip...
                continue;
            }

            String lookup = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.LOOKUP_KEY));
            Uri uri = ContactsContract.Contacts.getLookupUri(id, lookup);

            ContactEvent contact = new ContactEvent(id, name, uri, date, type);
            result[ct++] = contact;
        }
        c.close();

        return result;
    }

}
