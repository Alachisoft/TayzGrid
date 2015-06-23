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

package com.alachisoft.tayzgrid.caching.datagrouping;

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

import java.util.Collections;

/**
 * This class is used to store data groups settings for a node in the cluster
 */
public class DataAffinity implements Cloneable, ICompactSerializable, java.io.Serializable {

    private java.util.ArrayList _groups = new java.util.ArrayList();
    private java.util.ArrayList _allBindedGroups = new java.util.ArrayList();
    private java.util.ArrayList _unbindedGroups = new java.util.ArrayList();
    private boolean _strict;

    private boolean _serialize;

    public DataAffinity() {
    }

    /**
     * Constructor
     *
     * @param groups
     * @param strict
     */
    public DataAffinity(java.util.ArrayList groups, boolean strict) {
        if (groups != null) {
            _groups = (java.util.ArrayList) groups.clone();
            Collections.sort(_groups);
        }
        _strict = strict;
    }

    /**
     * Constructor
     *
     * @param groups
     * @param strict
     */
    public DataAffinity(java.util.ArrayList groups, java.util.ArrayList allBindedGroups, java.util.ArrayList unbindGroups, boolean strict) {
        if (groups != null) {
            _groups = (java.util.ArrayList) groups.clone();
            Collections.sort(_groups);
        }
        if (allBindedGroups != null) {
            _allBindedGroups = (java.util.ArrayList) allBindedGroups.clone();
            Collections.sort(_allBindedGroups);
        }
        if (unbindGroups != null) {
            _unbindedGroups = (java.util.ArrayList) unbindGroups.clone();
            Collections.sort(_unbindedGroups);
        }

        _strict = strict;
    }

    /**
     * Overloaded Constructor
     *
     * @param props
     */
    public DataAffinity(java.util.Map props) {
        if (props.containsKey("strict")) {
            _strict = (Boolean) (props.get("strict"));
        }

        if (props.containsKey("data-groups")) {
            String groupsStr = (String) props.get("data-groups");
            if (groupsStr.trim().length() > 0) {
                String[] groups = groupsStr.split("[,]", -1);
                java.util.ArrayList list = new java.util.ArrayList();
                for (int i = 0; i < groups.length; i++) {
                    list.add(groups[i]);
                }
                Collections.sort(list);
                _groups = list;
            }
        }
        if (props.containsKey("binded-groups-list")) {
            String groupsStr = (String) props.get("binded-groups-list");
            if (groupsStr.trim().length() > 0) {
                String[] groups = groupsStr.split("[,]", -1);
                java.util.ArrayList list = new java.util.ArrayList();
                for (int i = 0; i < groups.length; i++) {
                    list.add(groups[i]);
                }
                Collections.sort(list);
                _allBindedGroups = list;
            }
        }
    }

    /**
     * list of groups
     */
    public final java.util.ArrayList getGroups() {
        return _groups == null ? null : (java.util.ArrayList) _groups.clone();
    }

    public final void setGroups(java.util.ArrayList value) {
        _groups = value;
        if (_groups != null) {
            Collections.sort(_groups);
        }
    }

    /**
     * list of all the binded groups
     */
    public final java.util.ArrayList getAllBindedGroups() {
        return _allBindedGroups == null ? null : (java.util.ArrayList) _allBindedGroups.clone();
    }

    public final void setAllBindedGroups(java.util.ArrayList value) {
        _allBindedGroups = value;
        if (_allBindedGroups != null) {
            Collections.sort(_allBindedGroups);
        }
    }

    /**
     * list of all the groups which are not binded to any node.
     */
    public final java.util.ArrayList getAllUndbindedGroups() {
        return _unbindedGroups == null ? null : (java.util.ArrayList) _unbindedGroups.clone();
    }

    public final void setAllUndbindedGroups(java.util.ArrayList value) {
        _unbindedGroups = value;
        if (_unbindedGroups != null) {
            Collections.sort(_unbindedGroups);
        }
    }

    /**
     * Allow data without any group or not
     */
    public final boolean getStrict() {
        return _strict;
    }

    /**
     * Is the specified group exists in the list
     *
     * @param group
     * @return
     */
    public final boolean IsExists(String group) {
        if (group == null) {
            return false;
        }
        if (_groups == null) {
            return false;
        }
        if (Collections.binarySearch(_groups, group) < 0) {
            return false;
        }
        return true;
    }

    /**
     * Determine whether the spceified group exist in unbinded groups list or
     * not.
     *
     * @param group
     * @return
     */
    public final boolean IsUnbindedGroups(String group) {
        if (group == null) {
            return false;
        }
        if (_unbindedGroups == null) {
            return false;
        }
        if (Collections.binarySearch(_unbindedGroups, group) < 0) {
            return false;
        }
        return true;
    }

    public final Object clone() {
        return new DataAffinity(_groups, _allBindedGroups, _unbindedGroups, _strict);
    }

    public final void serialize(CacheObjectOutput writer) throws IOException {
        writer.writeBoolean(true);
        writer.writeBoolean(_strict);
        writer.writeObject(_groups);
        writer.writeObject(_allBindedGroups);
        writer.writeObject(_unbindedGroups);

    }

    public final void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException {
        if (!reader.readBoolean()) {
            return;
        }
        _strict = (boolean) reader.readBoolean();
        _groups = (java.util.ArrayList) reader.readObject();
        _allBindedGroups = (java.util.ArrayList) reader.readObject();
        _unbindedGroups = (java.util.ArrayList) reader.readObject();
    }
}
