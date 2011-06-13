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

public class Who {

    @Key("@email")
    public String email;

    @Key("@rel")
    public String rel;

    @Key("@valueString")
    public String valueString;

    public Who() {
    }

    public Who(String str) {
        String whoStr[] = Common.tokenFn(str, ",");

        for (int i = 0; i < whoStr.length; i++) {
            String whoItem[] = Common.tokenFn(whoStr[i], ":");

            if ("email".equals(whoItem[0])) {
                this.email = whoItem[1];
            } else if ("rel".equals(whoItem[0])) {
                this.rel = whoItem[1];
            } else if ("valueString".equals(whoItem[0])) {
                this.valueString = whoItem[1];
            }
        }

    }

    public String parseAsString() {
        StringBuilder result = new StringBuilder();

        result.append("email:").append(email).append(",");
        result.append("rel:").append(rel).append(",");
        result.append("valueString:").append(valueString);

        return result.toString();
    }

}
