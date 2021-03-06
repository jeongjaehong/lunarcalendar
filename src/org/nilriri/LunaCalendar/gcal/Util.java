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

import com.google.api.client.xml.XmlNamespaceDictionary;

/**
 * @author Yaniv Inbar
 */
public class Util {
    public static final boolean DEBUG = false;

    public static final XmlNamespaceDictionary DICTIONARY = //namespaece ����
    new XmlNamespaceDictionary() //
            .set("", "http://www.w3.org/2005/Atom")//
            //.set("app", "http://www.w3.org/2007/app")//
            //.set("batch", "http://schemas.google.com/gdata/batch")//
            .set("openSearch", "http://a9.com/-/spec/opensearchrss/1.0/")//
            .set("gCal", "http://schemas.google.com/gCal/2005")//
            .set("gd", "http://schemas.google.com/g/2005");//
}
