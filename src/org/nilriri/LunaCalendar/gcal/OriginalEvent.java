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

import java.util.ArrayList;
import java.util.List;

import org.nilriri.LunaCalendar.tools.Common;

import com.google.api.client.util.Key;

public class OriginalEvent {

    @Key("@href")
    public String href;

    @Key("@id")
    public String id;

    @Key("gd:when")
    public List<When> when = new ArrayList<When>();

    public OriginalEvent() {
    }

    public OriginalEvent(String str) {
        String whenStr[] = Common.tokenFn(str, ",");

        for (int i = 0; i < whenStr.length; i++) {
            String whenItem[] = Common.tokenFn(whenStr[i], ";");

            if ("href".equals(whenItem[0])) {
                this.href = whenItem[1];
            } else if ("id".equals(whenItem[0])) {
                this.id = whenItem[1];
            } else if ("when".equals(whenItem[0])) {
                this.when.add(new When(whenItem[1]));
            }
        }

    }

    public String parseAsString() {
        StringBuilder result = new StringBuilder();

        try {
            result.append("href;").append(href).append(",");
            result.append("id;").append(id).append(",");

            for (int i = 0; i < this.when.size(); i++) {
                if (i == 0) {
                    result.append(",when;").append(this.when.get(i).parseAsString());
                } else {
                    result.append("*").append(this.when.get(i).parseAsString());
                }
            }
        } catch (Exception e) {
        }
        return result.toString();
    }
}
