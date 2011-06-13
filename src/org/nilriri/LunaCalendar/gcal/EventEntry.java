/*
 * Copyright (c) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.nilriri.LunaCalendar.gcal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nilriri.LunaCalendar.dao.Constants.Schedule;
import org.nilriri.LunaCalendar.tools.Common;

import android.database.Cursor;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.Key;
import com.google.api.client.xml.atom.AtomContent;

public class EventEntry extends Entry {

    @Key("gCal:uid")
    public AtomValue uid = new AtomValue();

    @Key("content")
    public String content;

    @Key("published")
    public String published;

    // 반복일정인 경우는 when 태그 대신에 recurrence태그만 존재함.
    @Key("gd:when")
    public When when = new When();

    // 반복일정에 알림이 있을경우.
    @Key("gd:reminder")
    public List<Reminder> reminder = new ArrayList<Reminder>();

    // 참석자 정보
    @Key("gd:who")
    public List<Who> who = new ArrayList<Who>();

    // 반복일정일 경우 when이 없다.
    @Key("gd:recurrence")
    public String recurrence;

    // 삭제된 이벤트인 경우 event.canceled
    @Key("gd:eventStatus")
    public AtomValue eventStatus = new AtomValue();

    // 반복일정 중에서 수정된 별도의 일정일 경우 원래 일정.
    @Key("gd:originalEvent")
    public OriginalEvent originalEvent = new OriginalEvent();

    /*
     * batch
     */
    @Key("batch:id")
    public String batchId;

    @Key("batch:status")
    public BatchStatus batchStatus;

    @Key("batch:operation")
    public BatchOperation batchOperation;

    /*
     * 
     */
    public EventEntry() {
    }

    public EventEntry(Cursor c) {

        if (c.moveToNext()) {

            links.add(new Link("edit", c.getString(Schedule.COL_EDITURL)));
            links.add(new Link("self", c.getString(Schedule.COL_SELFURL)));

            //id = getEditLink();
            etag = c.getString(Schedule.COL_ETAG);

        }
    }

    /*
     * (non-Javadoc)
     * @see org.nilriri.LunaCalendar.gcal.Entry#clone()
     */
    @Override
    public EventEntry clone() {
        return (EventEntry) super.clone();
    }

    @Override
    public EventEntry executeInsert(HttpTransport transport, CalendarUrl url) throws IOException {
        return (EventEntry) super.executeInsert(transport, url);
    }

    public EventEntry executeUpdate(HttpTransport transport) throws IOException {
        HttpRequest request = transport.buildPutRequest();

        request.headers.ifMatch = this.etag;
        request.headers.put("id", this.id);
        request.url = new CalendarUrl(getEditLink());

        AtomContent content = new AtomContent();
        content.namespaceDictionary = Util.DICTIONARY;
        content.entry = this;
        request.content = content;

        HttpResponse response = RedirectHandler.execute(request);

        // HTTP 리턴코드가 201 CREATED.일 경우 Google UID를 UPDATE한다.
        if (200 == response.statusCode || 201 == response.statusCode) {
            return response.parseAs(getClass());
        } else {
            return this;
        }
    }

    public static EventEntry executeGetOriginalEntry(HttpTransport transport, CalendarUrl url) throws IOException {
        return (EventEntry) Entry.executeGetOriginalEntry(transport, url, EventEntry.class);
    }

    public String getStartDate() {
        try {
            if (when != null && when.startTime != null) {
                return when.startTime.toString().substring(0, 10);
            } else {
                String values[] = Common.tokenFn(recurrence, "\n");
                String keys[] = Common.tokenFn(values[0], ":");
                return Common.fmtDate(keys[1]);
            }
        } catch (Exception e) {
            return "";
        }
    }

    public String getEndDate() {
        try {
            if (when != null && when.endTime != null) {
                return when.endTime.toString().substring(0, 10);
            } else {
                String values[] = Common.tokenFn(recurrence, "\n");
                String keys[] = Common.tokenFn(values[1], ":");
                return Common.fmtDate(keys[1]);
            }
        } catch (Exception e) {
            return "";
        }
    }

    public String getWhos() {
        StringBuilder whos = null;
        try {
            whos = new StringBuilder(this.who.get(0).parseAsString());
            for (int i = 1; i < this.who.size(); i++) {
                whos.append("!" + this.who.get(i).parseAsString());
            }
            return whos.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public void setWhos(String whos) {
        String whoArray[] = Common.tokenFn(whos, "!");

        this.who.clear();
        try {
            for (int i = 0; i < whoArray.length; i++) {
                this.who.add(new Who(whoArray[i]));
            }

        } catch (Exception e) {
            this.who.clear();
        }
    }
}
