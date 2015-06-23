/*
* Copyright (c) 2015, Alachisoft. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.alachisoft.tayzgrid.web;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

public class CacheSession implements Serializable {

    private String sessionId = "";
    private long maxInactiveInterval = 0;
    private boolean isNew = true;
    private HashMap attributes = new HashMap();
    private String prefix = null;

    public CacheSession() {
    }

    public CacheSession(String prefix) {
        this.prefix = prefix;
    }

    public boolean isIsNew() {
        return isNew;
    }

    public void reset() {
        maxInactiveInterval = 0;
        isNew = true;
        clearAttributes();
    }

    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    public void clearAttributes() {
        attributes.clear();
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public Object getAttribute(String key) {
        if (attributes.containsKey(key)) {
            return attributes.get(key);
        }
        return null;
    }

    public Set getKeys() {
        return attributes.keySet();
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return prefix + sessionId;
    }

    public void setMaxInactiveInterval(long interval) {
        this.maxInactiveInterval = interval;
    }

    public long getMaxInactiveInterval() {
        return maxInactiveInterval;
    }
    //setting current primary cache prefix in case of multi region

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean updateToCache() {
        return true;
    }
}
