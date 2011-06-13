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

import com.google.api.client.googleapis.xml.atom.AtomPatchRelativeToOriginalContent;
import com.google.api.client.googleapis.xml.atom.GoogleAtom;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.DataUtil;
import com.google.api.client.util.Key;
import com.google.api.client.xml.atom.AtomContent;

/**
 * @author Yaniv Inbar
 */
public class Entry implements Cloneable {

    @Key
    public String id;

    @Key
    public String summary;

    @Key
    public String title;

    @Key
    public String updated;

    @Key("@gd:etag")
    public String etag;

    @Key("link")
    public List<Link> links = new ArrayList<Link>();

    @Override
    protected Entry clone() {
        return DataUtil.clone(this);
    }

    public int executeDelete(HttpTransport transport) throws IOException {
        HttpRequest request = transport.buildDeleteRequest();
        if ("".equals(getEditLink()) || getEditLink() == null) {
            return 0;
        }
        request.headers.ifMatch = this.etag;
        request.setUrl(getEditLink());
        return RedirectHandler.execute(request).statusCode;
    }

    public Entry executeInsert(HttpTransport transport, CalendarUrl url) throws IOException {
        HttpRequest request = transport.buildPostRequest();
        request.url = url;
        AtomContent content = new AtomContent();
        content.namespaceDictionary = Util.DICTIONARY;
        content.entry = this;
        request.content = content;

        HttpResponse response = RedirectHandler.execute(request);

        // HTTP 리턴코드가 200 OK.일 경우 Google UID를 UPDATE한다.
        // HTTP 리턴코드가 201 CREATED.일 경우 Google UID를 UPDATE한다.
        if (200 == response.statusCode || 201 == response.statusCode) {
            return response.parseAs(getClass());
        } else {
            return this;
        }
    }

    static Entry executeGetOriginalEntry(HttpTransport transport, CalendarUrl url, Class<? extends Entry> entryClass) throws IOException {
        url.fields = GoogleAtom.getFieldsFor(entryClass);
        HttpRequest request = transport.buildGetRequest();
        request.url = url;
        return RedirectHandler.execute(request).parseAs(entryClass);
    }

    public Entry executePatchRelativeToOriginal(HttpTransport transport, Entry original) throws IOException {
        HttpRequest request = transport.buildPatchRequest();
        request.setUrl(getEditLink());
        AtomPatchRelativeToOriginalContent content = new AtomPatchRelativeToOriginalContent();
        content.namespaceDictionary = Util.DICTIONARY;
        content.originalEntry = original;
        content.patchedEntry = this;
        request.content = content;
        return RedirectHandler.execute(request).parseAs(getClass());
    }

    public String getEditLink() {
        String link = Link.find(links, "edit");
        if ("".equals(link) || link == null) {
            link = (this.id == null ? "" :this.id);
        }
        return link;
    }

    public String getWebContentsLink() {
        String link = Link.find(links, "http://schemas.google.com/gCal/2005/webContent");
        return (link == null ? "" : link);
    }

    public String getSelfLink() {
        return Link.find(links, "self");
    }
}
