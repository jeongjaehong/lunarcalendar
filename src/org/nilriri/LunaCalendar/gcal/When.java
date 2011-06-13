package org.nilriri.LunaCalendar.gcal;

import java.util.ArrayList;
import java.util.List;

import org.nilriri.LunaCalendar.tools.Common;

import com.google.api.client.util.DateTime;
import com.google.api.client.util.Key;

public class When {

    @Key("@startTime")
    public DateTime startTime;

    @Key("@endTime")
    public DateTime endTime;

    @Key("gd:reminder")
    public List<Reminder> reminder = new ArrayList<Reminder>();

    public When() {

    }

    public When(String str) {
        try {
            String whenStr[] = Common.tokenFn(str, ",");
            for (int i = 0; i < whenStr.length; i++) {
                String whenItem[] = Common.tokenFn(whenStr[i], ";");
                if ("startTime".equals(whenItem[0])) {
                    this.startTime = Common.toDateTime(whenItem[1]);
                } else if ("endTime".equals(whenItem[0])) {
                    this.endTime = Common.toDateTime(whenItem[1]);
                } else if ("reminder".equals(whenItem[0])) {
                    String remainstr[] = Common.tokenFn(whenItem[1], "*");
                    for (int j = 0; j < remainstr.length; j++) {
                        reminder.add(new Reminder(remainstr[j]));
                    }
                }
            }
        } catch (Exception e) {
            startTime = Common.toDateTime("");
            endTime = Common.toDateTime("", 1);
            reminder = new ArrayList<Reminder>();
        }
    }

    public String parseAsString() {
        StringBuilder result = new StringBuilder();

        result.append("startTime;").append(startTime).append(",");
        result.append("endTime;").append(endTime);

        for (int i = 0; i < this.reminder.size(); i++) {
            if (i == 0) {
                result.append(",reminder;").append(this.reminder.get(i).parseAsString());
            } else {
                result.append("*").append(this.reminder.get(i).parseAsString());
            }
        }
        return result.toString();
    }

}
