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

import com.alachisoft.tayzgrid.web.caching.runnable.CacheStoppedRunnable;
import com.alachisoft.tayzgrid.web.caching.runnable.CacheClearedCallbackRunnable;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.CallbackInfo;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.stats.UsageStats;
import com.alachisoft.tayzgrid.common.threading.ThreadPool;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.runtime.events.EventType;
import com.alachisoft.tayzgrid.web.events.EventCacheItem;
import com.alachisoft.tayzgrid.web.events.EventHandle;
import com.alachisoft.tayzgrid.web.events.EventManager;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class CacheEventsListener implements IDisposable {

    /**
     * Underlying implementation of NCache.
     */
    private Cache _parent;
    private EventManager _eventManager;

    public CacheEventsListener(Cache parent, EventManager eventManager) {
        _parent = parent;
        _eventManager = eventManager;
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     *
     *
     *
     */
    public void dispose() {
        try {
        } catch (java.lang.Exception e) {
        }
    }

    public void OnCacheStopped(String cacheId, boolean notifyAsync) {
        try {
            List<CacheStoppedCallback> cacheStoppedCallbackList = _parent.getCacheStoppedCallbackList();

            if (cacheStoppedCallbackList != null && !cacheStoppedCallbackList.isEmpty()) {

                for (CacheStoppedCallback callback : cacheStoppedCallbackList) {
                    if (notifyAsync) {
                        CacheStoppedRunnable cacheStoppedRunnable = new CacheStoppedRunnable(cacheId, callback);
                        new Thread(cacheStoppedRunnable).start();
                    } else {
                        callback.cacheStopped(cacheId);
                    }
                }
            }

        } catch (java.lang.Exception e) {
        }
    }

    public void OnCustomRemoveCallback(Object key, Object value, CacheItemRemovedReason reason, BitSet flag, boolean notifyAsync, EventCacheItem item) {
        try {
            Object[] args = (Object[]) ((value instanceof Object[]) ? value : null);
            if (args != null) {
                Object val = args[0];


                CallbackInfo cbInfo = (CallbackInfo) ((args[1] instanceof CallbackInfo) ? args[1] : null);
                if (cbInfo != null) {

                    if (_parent._perfStatsCollector != null) {
                        _parent._perfStatsCollector.incrementEventProcessedPerSec();
                    }

                    if (item != null) {
                        item.setValue(GetObject(item.getValue(), flag));
                    }

                    if (cbInfo.getCallback() != null && !cbInfo.getCallback().equals("")) {
                        EventHandle handle = new EventHandle(Short.parseShort(cbInfo.getCallback().toString()));
                        _parent.getEventManager().raiseSelectiveCacheNotification(key, EnumSet.of(EventType.ItemRemoved), item, null, reason, notifyAsync, handle, cbInfo.getDataFilter());
                    }
                }
            }
        } catch (java.lang.Exception e) {
        }
    }

    public void OnCacheCleared(boolean notifyAsync) {
        try {
            this._eventManager.raiseGeneralCacheNotification(null, EnumSet.of(EventType.CacheCleared), null, null, CacheItemRemovedReason.Removed, notifyAsync);
        } catch (java.lang.Exception e) {
        }
    }

    public void OnCustomUpdateCallback(Object key, Object value, boolean notifyAsync, EventCacheItem item, EventCacheItem oldItem, BitSet flag) {
        try {

            CallbackInfo cbInfo = (CallbackInfo) ((value instanceof CallbackInfo) ? value : null);
            if (cbInfo != null) {
       
                if (item != null) {
                    item.setValue(GetObject(item.getValue(), flag));
                }
                if (oldItem != null) {
                    oldItem.setValue(GetObject(oldItem.getValue(), flag));
                }

                if (_parent._perfStatsCollector != null) {
                    _parent._perfStatsCollector.incrementEventProcessedPerSec();
                }

                EventHandle handle = new EventHandle(Short.parseShort(cbInfo.getCallback().toString()));
                this._eventManager.raiseSelectiveCacheNotification(key, EnumSet.of(EventType.ItemUpdated), item, oldItem, CacheItemRemovedReason.Underused, notifyAsync, handle, cbInfo.getDataFilter());
            }
        } catch (java.lang.Exception e) {
        }
    }
  
    public void OnItemAdded(Object key, boolean notifyAsync, EventCacheItem item, BitSet flag) {
        try {
            if (key != null) {   
                if (item != null && item.getValue() != null) {
                    item.setValue(GetObject(item.getValue(), flag));
                }
                _eventManager.raiseGeneralCacheNotification(key, EnumSet.of(EventType.ItemAdded), item, null, CacheItemRemovedReason.Underused, notifyAsync);
            }
        } catch (java.lang.Exception e) {
        }
    }

    public void OnItemUpdated(Object key, boolean notifyAsync, EventCacheItem item, EventCacheItem oldItem, BitSet flag) throws Exception {
        try {
            if (key != null) {              
                if (item != null && item.getValue() != null) {
                    item.setValue(GetObject(item.getValue(), flag));
                }
                if (oldItem != null && oldItem.getValue() != null) {
                    oldItem.setValue(GetObject(oldItem.getValue(), flag));
                }
                this._eventManager.raiseGeneralCacheNotification(key, EnumSet.of(EventType.ItemUpdated), item, oldItem, CacheItemRemovedReason.Underused, notifyAsync);
            }
        } catch (RuntimeException e) {
        }
    }


    public void OnItemRemoved(Object key, Object value, ItemRemoveReason reason, BitSet Flag, boolean notifyAsync, EventCacheItem item) {
        try {
          
            if (item != null && item.getValue() != null) {
                item.setValue(value = GetObject(value, Flag));
            }
         
            this._eventManager.raiseGeneralCacheNotification(key, EnumSet.of(EventType.ItemRemoved), item, null, WebCacheHelper.GetWebItemRemovedReason(reason), notifyAsync);
        } catch (java.lang.Exception e) {
        }
    }

    public void OnItemRemoved(Object key, Object value, CacheItemRemovedReason reason, BitSet Flag, boolean notifyAsync, EventCacheItem item) {
        try {        
            if (item != null && value != null) {
                value = GetObject(value, Flag);
                item.setValue(value);
            }
            this._eventManager.raiseGeneralCacheNotification(key, EnumSet.of(EventType.ItemRemoved), item, null, reason, notifyAsync);
        } catch (java.lang.Exception e) {
        }
    }
  
    public void OnMapReduceTaskCompleteCallback(String taskId, int taskStatus, short callbackId)
    {
        if(taskId != null && !taskId.equals(""))
        {
            this._eventManager.fireMapReduceCallback(taskId, taskStatus, callbackId);
        }
    }
    
    public void OnCustomNotification(Object notifId, Object data, boolean notifyAsync) {
        try {
            BitSet flag = new BitSet();
            notifId = _parent.safeDeserialize(notifId, _parent.getSerializationContext(), flag);
            data = _parent.safeDeserialize(data, _parent.getSerializationContext(), flag);
            ArrayList<ICustomEvent> customEventList = _parent.getCustomEventList();
            if (customEventList != null && !customEventList.isEmpty()) {
                
                for(ICustomEvent event : (ICustomEvent[])customEventList.toArray())
                {
                    try
                    {
                        if(notifyAsync)
                            event.invoke(notifId, data, notifyAsync);
                        else
                            ThreadPool.executeTask(event);
                        
                        if (_parent._perfStatsCollector != null) {
                            _parent._perfStatsCollector.incrementEventProcessedPerSec();
                        }
                    }
                      catch (RuntimeException e) {
                    }
                }             
            }
        } catch (java.lang.Exception e) {
        }
    }

    private Object GetObject(Object value, BitSet Flag) throws Exception {
        try {
            if (value instanceof CallbackEntry) {
                value = ((CallbackEntry) value).getValue();
            }

            if (value instanceof UserBinaryObject) {
                value = ((UserBinaryObject) value).GetFullObject();
            }

            return _parent.safeDeserialize(value, _parent.getSerializationContext(), Flag);
                        
        } catch (RuntimeException ex) {
            return value;
        }
    }  
}
