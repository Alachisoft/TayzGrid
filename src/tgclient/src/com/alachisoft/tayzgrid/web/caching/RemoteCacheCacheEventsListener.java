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

import com.alachisoft.tayzgrid.caching.CallbackInfo;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.web.events.EventCacheItem;


public class RemoteCacheCacheEventsListener implements IDisposable {

    private CacheEventsListener _listener;


    /**
     * Constructor.
     *
     * @param parent
     */
    public RemoteCacheCacheEventsListener(CacheEventsListener parent) {
        _listener = parent;
    }


    ///#region    /                 --- IDisposable ---           /
    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     *
     *
     *
     */
    public final void dispose() {
    }


    public final void OnItemAdded(Object key, boolean notifyAsync, EventCacheItem item, BitSet flag) {
        try {
            if (_listener != null) {
                _listener.OnItemAdded(key, notifyAsync, item, flag);
            }
        } catch (java.lang.Exception e) {
        }
    }

    public final void OnItemUpdated(Object key, boolean notifyAsync, EventCacheItem item, EventCacheItem oldItem, BitSet flag) {
        try {
            if (_listener != null) {
                _listener.OnItemUpdated(key, notifyAsync, item, oldItem, flag);
            }
        } catch (java.lang.Exception e) {
        }
    }

    public final void OnItemRemoved(Object key, Object value, CacheItemRemovedReason reason, BitSet flag, boolean notifyAsync, EventCacheItem item) {
        try {
            if (_listener != null) {
                _listener.OnItemRemoved(key, value, reason, flag, notifyAsync, item);
            }
        } catch (RuntimeException e) {
        }
    }

    public final void OnCustomNotification(Object notifId, Object data, boolean notifyAsync) {
        try {
            if (!_listener.equals(null)) {
                _listener.OnCustomNotification(notifId, data, notifyAsync);
            }
        } catch (java.lang.Exception e) {
        }
    }


    public final void OnCacheCleared(boolean notifyAsync) {
        try {
            if (_listener != null) {
                _listener.OnCacheCleared(notifyAsync);
            }
        } catch (java.lang.Exception e) {
        }
    }

    public final void OnCustomRemoveCallback(short callbackId, Object key, Object value, CacheItemRemovedReason reason, BitSet Flag, boolean notifyAsync, EventCacheItem item, EventDataFilter dataFilter) {
        Object[] val = new Object[]{value, new CallbackInfo(null, callbackId, dataFilter)};
        _listener.OnCustomRemoveCallback(key, val, reason, Flag, notifyAsync, item);
    }

    public final void OnCustomUpdateCallback(short callbackId, Object key, boolean notifyAsync, EventCacheItem item, EventCacheItem oldItem, BitSet flag, EventDataFilter dataFilter) {
        CallbackInfo cbInfo = new CallbackInfo(null, callbackId, dataFilter);
        _listener.OnCustomUpdateCallback(key, cbInfo, notifyAsync, item, oldItem, flag);
    }

    public final void OnCacheStopped(String cacheId, boolean notifyAsync) {
        try {
            if (_listener != null) {
                _listener.OnCacheStopped(cacheId, notifyAsync);
            }
        } catch (java.lang.Exception e) {
        }
    }

    public final void OnMapReduceCompleteCallback(String taskId, int taskStatus, short callbackId)
    {
        try {
            if(_listener != null) {
                _listener.OnMapReduceTaskCompleteCallback(taskId, taskStatus, callbackId);
            }
        } catch (java.lang.Exception ex) {
            
        }
    }

}
