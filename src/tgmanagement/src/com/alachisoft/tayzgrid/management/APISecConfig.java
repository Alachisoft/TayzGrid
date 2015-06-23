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

package com.alachisoft.tayzgrid.management;

import java.util.Iterator;
import java.util.Map;

public class APISecConfig
{

    public static java.util.HashMap oldCacheUsers = null;
    private static java.util.ArrayList _cacheList = new java.util.ArrayList();

    public static void fillCacheList()
    {
        if (oldCacheUsers != null)
        {
            Iterator ide = oldCacheUsers.entrySet().iterator();

            while (ide.hasNext())
            {
                Map.Entry current = (Map.Entry) ide.next();
                String cacheName = current.getKey().toString();
                _cacheList.add(cacheName);
            }
        }

    }

    public static java.util.HashMap UpdateCacheUserList(java.util.HashMap newCacheUsers)
    {
        try
        {
            Iterator ide = newCacheUsers.entrySet().iterator();
            while (ide.hasNext())
            {
                Map.Entry current = (Map.Entry) ide.next();
                String cacheName = current.getKey().toString();
                if (_cacheList.contains(cacheName))
                {
                    oldCacheUsers.put(cacheName.toLowerCase(), current.getValue());
                }
                else
                {
                    oldCacheUsers.put(cacheName.toLowerCase(), current.getValue());
                }
            }
            return oldCacheUsers;
        }
        catch (RuntimeException ex)
        {
            return null;
        }
    }
}
