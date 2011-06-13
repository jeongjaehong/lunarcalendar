package org.nilriri.LunaCalendar.gcal;

import java.io.IOException;
import java.util.List;

import com.google.api.client.googleapis.xml.atom.GoogleAtom;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.Key;

public abstract class Feed {

    @Key("link")
    public List<Link> links;

    public String getNextLink() {
        return Link.find(links, "next");
    }

    public String getBatchLink() {
        return Link.find(links, "http://schemas.google.com/g/2005#batch");
    }

    public abstract List<? extends Entry> getEntries();

    static Feed executeGet(HttpTransport transport, CalendarUrl url, Class<? extends Feed> feedClass) throws IOException {
        url.fields = GoogleAtom.getFieldsFor(feedClass);
        HttpRequest request = transport.buildGetRequest();
        request.url = url;
        return RedirectHandler.execute(request).parseAs(feedClass);
    }
}
