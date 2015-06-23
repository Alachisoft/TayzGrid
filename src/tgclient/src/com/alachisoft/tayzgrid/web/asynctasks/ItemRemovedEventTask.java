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

package com.alachisoft.tayzgrid.web.asynctasks;

import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.stats.UsageStats;
import com.alachisoft.tayzgrid.common.threading.AsyncProcessor;
import com.alachisoft.tayzgrid.web.caching.CacheItemRemovedReason;
import com.alachisoft.tayzgrid.web.caching.RemoteCache.Broker;
import com.alachisoft.tayzgrid.web.events.EventCacheItem;

public class ItemRemovedEventTask implements AsyncProcessor.IAsyncTask
{
        private Object _key;
        private boolean _notifyAsync;
        private Object _value;
        private CacheItemRemovedReason _reason;
        private BitSet _flag;
        private Broker _parent;
        private UsageStats _stats;
        private EventCacheItem _item;

        public ItemRemovedEventTask(Broker parent, Object key, Object value, CacheItemRemovedReason reason, BitSet flag, boolean notifyAsync, EventCacheItem item)
        {
            this._parent = parent;
            this._key = key;
            this._value = value;
            this._reason = reason;
            this._flag = flag;
            this._notifyAsync = notifyAsync;
            this._item = item;
        }

        public void Process()
        {
            try
            {
                if (_parent != null)
                {
                    _stats = new UsageStats();
                    _stats.BeginSample();
                    _parent._cache.getEventsListener().OnItemRemoved(_key, _value, _reason, _flag, _notifyAsync, _item);

                    _stats.EndSample();
                }
            }
            catch (Exception ex)
            {
                if (_parent != null && _parent.getLogger()!=null && _parent.getLogger().getIsErrorLogsEnabled()) 
                    _parent.getLogger().getCacheLog().Error("Item Remove Event Task.Process", ex.getMessage());
            }
        }
    
}
