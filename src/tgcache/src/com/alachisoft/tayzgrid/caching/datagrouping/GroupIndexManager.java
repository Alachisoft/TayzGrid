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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class manages the indexes for data groups
 */
public class GroupIndexManager
{

    private java.util.HashMap _groups = new java.util.HashMap();

    public GroupIndexManager()
    {
    }

    /**
     * Add group information to the index
     *
     * @param key
     * @param grpInfo
     */
    public final void AddToGroup(Object key, GroupInfo grpInfo)
    {
        if (grpInfo == null)
        {
            return;
        }

        String group = grpInfo.getGroup();
        String subGroup = grpInfo.getSubGroup();

        if (group == null)
        {
            return;
        }

        java.util.HashMap subGrpTable = null;
        synchronized (_groups)
        {
            if (subGroup == null)
            {
                subGroup = "_DEFAULT_SUB_GRP_";
            }
            if (_groups.containsKey(group))
            {
                java.util.HashMap grpTable = (java.util.HashMap) _groups.get(group);
                if (grpTable.containsKey(subGroup))
                {
                    subGrpTable = (java.util.HashMap) grpTable.get(subGroup);
                }
                else
                {
                    subGrpTable = new java.util.HashMap();
                    grpTable.put(subGroup, subGrpTable);
                }
            }
            else
            {
                java.util.HashMap grpTable = new java.util.HashMap();
                subGrpTable = new java.util.HashMap();
                grpTable.put(subGroup, subGrpTable);
                _groups.put(group, grpTable);
            }
            subGrpTable.put(key, "");
        }
    }

    /**
     * Remove a specific key from a group or a sub group
     *
     * @param key
     * @param group
     * @param subGroup
     */
    public final void RemoveFromGroup(Object key, GroupInfo grpInfo)
    {
        if (grpInfo == null)
        {
            return;
        }
        String group = grpInfo.getGroup();
        String subGroup = grpInfo.getSubGroup();
        if (group == null)
        {
            return;
        }

        java.util.HashMap subGrpTable = null;
        if (subGroup == null)
        {
            subGroup = "_DEFAULT_SUB_GRP_";
        }

        synchronized (_groups)
        {
            if (_groups.containsKey(group))
            {
                java.util.HashMap grpTable = (java.util.HashMap) _groups.get(group);
                if (grpTable.containsKey(subGroup))
                {
                    subGrpTable = (java.util.HashMap) grpTable.get(subGroup);
                    if (subGrpTable.containsKey(key))
                    {
                        subGrpTable.remove(key);
                        ////count

                        if (subGrpTable.isEmpty())
                        {
                            grpTable.remove(subGroup);

                            if (grpTable.isEmpty())
                            {
                                _groups.remove(group);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * returns the shallow copy of the group
     *
     * @param group
     * @param subGroup
     * @return
     */
    public final java.util.HashMap GetGroup(String group, String subGroup)
    {
        if (group == null)
        {
            return null;
        }

        synchronized (_groups)
        {
            if (_groups.containsKey(group))
            {
                java.util.HashMap grpTable = (java.util.HashMap) _groups.get(group);
                return (java.util.HashMap) grpTable.clone();
            }
        }

        return null;
    }

    private java.util.HashMap GetGroup(String group)
    {
        if (group == null)
        {
            return null;
        }


        java.util.HashMap table = new java.util.HashMap();
        synchronized (_groups)
        {
            if (_groups.containsKey(group))
            {
                java.util.HashMap grpTable = (java.util.HashMap) _groups.get(group);
                for (Iterator it = grpTable.values().iterator(); it.hasNext();)
                {
                    java.util.HashMap data = (HashMap)it.next();
                    Iterator ide = data.entrySet().iterator();
                    while (ide.hasNext())
                    {
                        Map.Entry pair = (Map.Entry)ide.next();
                        table.put(pair.getKey(), pair.getValue());
                    }
                }
            }
        }

        return table;
    }

    /**
     * Return all the keys in the group. If a sub group is specified keys related to that subGroup is returned only otherwise all the keys included group and subgroup is returned.
     *
     * @param group
     * @param subGroup
     * @return
     */
    public final java.util.ArrayList GetGroupKeys(String group, String subGroup)
    {
        if (group == null)
        {
            return null;
        }

        java.util.ArrayList list = new java.util.ArrayList();
        synchronized (_groups)
        {
            if (_groups.containsKey(group))
            {
                java.util.HashMap grpTable = (java.util.HashMap) _groups.get(group);
                if (subGroup != null)
                {
                    if (grpTable.containsKey(subGroup))
                    {
                        java.util.HashMap subGrpTable = (java.util.HashMap) grpTable.get(subGroup);
                        list.addAll(subGrpTable.keySet());
                    }
                }
                else
                {
                    Iterator ide = grpTable.entrySet().iterator();
                    while (ide.hasNext())
                    {
                        Map.Entry pair = (Map.Entry)ide.next();
                        java.util.Map subgroup = (java.util.Map) ((pair.getValue() instanceof java.util.Map) ? pair.getValue() : null);
                        if (subgroup != null)
                        {
                            list.addAll(subgroup.keySet());
                        }
                    }
                }
            }
        }
        return list;
    }

    /**
     * Checks whether a data groups exists or not.
     *
     * @param group
     * @return
     */
    public final boolean GroupExists(String group)
    {
        return _groups != null ? _groups.containsKey(group) : false;
    }

    public final boolean KeyExists(Object key, String group, String subGroup)
    {
        if (GroupExists(group))
        {
            java.util.HashMap grpTable = (java.util.HashMap) _groups.get(group);
            if (subGroup != null)
            {
                if (grpTable.containsKey(subGroup))
                {
                    java.util.HashMap subgrpTable = (java.util.HashMap) grpTable.get(subGroup);
                    if (subgrpTable.containsKey(key))
                    {
                        return true;
                    }
                    return false;
                }
                return false;
            }
            else
            {
                Iterator ide = grpTable.entrySet().iterator();
                while (ide.hasNext())
                {
                    Map.Entry pair = (Map.Entry)ide.next();
                    if (((java.util.HashMap) pair.getValue()).containsKey(key))
                    {
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    /**
     * Gets the list of data groups.
     */
    public final java.util.ArrayList getDataGroupList()
    {
        if (_groups != null)
        {
            synchronized (_groups)
            {
                java.util.ArrayList list = new java.util.ArrayList();
                Iterator ide = _groups.entrySet().iterator();
                while (ide.hasNext())
                {
                    Map.Entry pair = (Map.Entry)ide.next();
                    list.add(pair.getKey());
                }
                return list;
            }
        }
        return null;
    }

    /**
     * Clear all the keys from index
     */
    public final void Clear()
    {
        if (_groups != null)
        {
            synchronized (_groups)
            {
                _groups.clear();
            }
        }
    }

    public final void dispose()
    {
        Clear();
    }
}
