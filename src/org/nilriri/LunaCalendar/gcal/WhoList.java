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

/**
 * @author Yaniv Inbar
 */
public class WhoList {

    private List<Who> list;

    public WhoList() {
        list = new ArrayList<Who>();
    }

    public void add(String email, String rel, String name) {
        Who who = new Who();

        who.email = email;
        who.rel = rel;
        who.valueString = name;

        list.add(who);
    }
    
    public List<Who> getList(){
        return list;
    }

}
