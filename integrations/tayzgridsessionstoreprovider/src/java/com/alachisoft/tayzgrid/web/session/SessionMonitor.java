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

package com.alachisoft.tayzgrid.web.session;

import java.util.HashMap;
import org.apache.log4j.Logger;

public class SessionMonitor
{
    private static HashMap localCache = new HashMap(); 
    private static Logger logger = Logger.getLogger(SessionMonitor.class);

    public static int getRequestCount(String key)
    {
        if (localCache.containsKey(key))
        {
            Integer count = (Integer) localCache.get(key);
            logger.debug("Request count is: " + count.intValue() + " for session (id = " + key + ")");
            return count.intValue();
        }
        logger.debug("No request (other than this) is currently using this session");
        return 0;
    }
    public static void addRequest(String key)
    {
        localCache.put(key, new Integer(getRequestCount(key) + 1));
        logger.debug("Another request is using the same session (id = " + key + ")");
    }
    public static void removeRequest(String key)
    {
        int count = getRequestCount(key) - 1;
        if (count <= 0)
        {
            localCache.remove(key);
        }
        else
        {
            localCache.put(key, new Integer(count));
        }
        logger.debug("A request completed for session (id = " + key + "), count is: " + count);
    }
    public static void resetRequestCount(String key)
    {
        localCache.remove(key);
        logger.debug("Session (id = " + key + ") removed from local cache");
    }
}
