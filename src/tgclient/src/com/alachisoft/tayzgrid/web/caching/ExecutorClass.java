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

package com.alachisoft.tayzgrid.web.caching;

import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.event.CacheEvent;
import com.alachisoft.tayzgrid.event.CacheListener;
import com.alachisoft.tayzgrid.event.Notifications;


public class ExecutorClass implements Runnable
{

    Object _listener;
    Object _key;
    int _notificationtype = 4;
    private CacheEvent _cacheEvent;

    public ExecutorClass(Object key, Object update, int i)
    {
        _listener = update;
        _key = key;
        _notificationtype = i;

    }

    public void run()
    {

        try
        {
            if (_notificationtype == 1)
            {
                ((CacheItemUpdatedCallback) _listener).itemUpdated(_key);
            }
            else if (_notificationtype == 2)
            {
                ((CacheItemRemovedCallback) _listener).itemRemoved(_key, _cacheEvent.getValue(), CacheItemRemovedReason.Removed);
            }

            else if (_notificationtype == 3)
            {
                ((CacheListener) _listener).cacheCleared();
            }
            else if (_notificationtype == 4)
            {
                ((CacheListener) _listener).cacheItemAdded(_cacheEvent);
            }
            else if (_notificationtype == 5)
            {
                ((CacheListener) _listener).cacheItemRemoved(_cacheEvent);
            }
            else if (_notificationtype == 6)
            {
                ((CacheListener) _listener).cacheItemUpdated(_cacheEvent);
            }
        }
        catch (NullPointerException ne)
        {
          
        }
        catch (Exception e)
        {
          
        }

    }

    void setCacheEvent(CacheEvent cacheEvent)
    {
        this._cacheEvent = cacheEvent;
    }
}
