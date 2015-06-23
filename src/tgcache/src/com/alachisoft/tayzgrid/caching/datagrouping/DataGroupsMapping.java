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

import com.alachisoft.tayzgrid.common.net.Address;
import java.util.Iterator;

public class DataGroupsMapping implements Cloneable
{

    private java.util.HashMap _groupMap = new java.util.HashMap();

    public DataGroupsMapping()
    {
    }

    public DataGroupsMapping(String groupList)
    {
        Initialize(groupList);
    }

    public final void Initialize(String groupList)
    {
        if (groupList != null)
        {
            String[] gList = groupList.split("[,]", -1);

            if (gList != null)
            {
                for (int i = 0; i < gList.length; i++)
                {
                    if (_groupMap.containsKey(gList[i]))
                    {
                        _groupMap.put(gList[i], null);
                    }
                }
            }
        }
    }

    public final boolean HasDatGroup(String group)
    {
        return _groupMap.containsKey(group);
    }

    public final java.util.Collection getGroupList()
    {
        return _groupMap.keySet();
    }

    public final void AddDataGroup(String group, Address node)
    {
        if (group == null)
        {
            return;
        }

        java.util.ArrayList nodeList = null;
        if (node != null)
        {
            if (_groupMap.containsKey(group))
            {
                nodeList = (java.util.ArrayList) _groupMap.get(group);
            }
            else
            {
                nodeList = new java.util.ArrayList();
            }

            nodeList.add(node);
        }

        _groupMap.put(group, nodeList);
    }

    public final void AddDataGroup(DataAffinity affinity, Address node)
    {
        if (affinity == null || node == null)
        {
            return;
        }
        if (affinity.getGroups() != null)
        {
            for (Iterator it = affinity.getGroups().iterator(); it.hasNext();)
            {
                String group = (String)it.next();
                AddDataGroup(group, node);
            }
        }
    }

    public final Object clone()
    {
        return null;
    }
}
