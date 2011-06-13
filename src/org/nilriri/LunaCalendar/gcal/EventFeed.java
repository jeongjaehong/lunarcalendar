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

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.Key;
import com.google.api.client.xml.atom.AtomFeedContent;

public class EventFeed extends Feed {

    @Key("entry")
    public List<EventEntry> events = new ArrayList<EventEntry>();

    public static EventFeed executeGet(HttpTransport transport, CalendarUrl url) throws IOException {
        return (EventFeed) Feed.executeGet(transport, url, EventFeed.class);
    }

    public EventFeed executeBatch(HttpTransport transport, String eventFeedLink) throws IOException {
        // batch link
        CalendarUrl eventFeedUrl = new CalendarUrl(eventFeedLink);
        eventFeedUrl.maxResults = 0;
        EventFeed eventFeed = EventFeed.executeGet(transport, eventFeedUrl);
        CalendarUrl url = new CalendarUrl(eventFeed.getBatchLink());
        // execute request
        HttpRequest request = transport.buildPostRequest();
        request.url = url;
        AtomFeedContent content = new AtomFeedContent();
        content.namespaceDictionary = Util.DICTIONARY //  
                .set("app", "http://www.w3.org/2007/app")//
                .set("batch", "http://schemas.google.com/gdata/batch");//
        content.feed = this;
        request.content = content;
        return RedirectHandler.execute(request).parseAs(getClass());
    }

    @Override
    public List<EventEntry> getEntries() {
        return events;
    }
}
