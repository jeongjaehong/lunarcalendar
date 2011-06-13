/*
 * Copyright (c) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.nilriri.LunaCalendar.gcal;

import org.nilriri.LunaCalendar.tools.Common;

import com.google.api.client.util.Key;

public class Reminder {

    @Key("@method")
    public String method;

    @Key("@minutes")
    public String minutes;

    public Reminder() {
    }

    public Reminder(String str) {
        String reminItem[] = Common.tokenFn(str, "|");

        this.method = reminItem[0];
        this.minutes = reminItem[1];

    }

    public String parseAsString() {
        StringBuilder result = new StringBuilder();

        result.append(method).append("|").append(minutes);

        return result.toString();
    }

}
