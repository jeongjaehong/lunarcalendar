package org.nilriri.LunaCalendar.tools;

import android.net.Uri;

public class ContactEvent {
    public Long mContact_id;
    public String mDisplayName;
    public int mType;
    public String mStartDate;
    public Uri mUri;

    public ContactEvent(Long id, String name, Uri uri, String date, int type) {
        mContact_id = id;
        mDisplayName = name;
        mType = type;
        mUri = uri;
        mStartDate = date;
    }

    public int getDay() {
        return Integer.parseInt(mStartDate.substring(mStartDate.length() - 2));
    }

}
