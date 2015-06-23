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
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.web.caching.RemoteCache.Broker;
import com.alachisoft.tayzgrid.web.events.EventCacheItem;

public class ItemUpdatedCallbackTask implements AsyncProcessor.IAsyncTask {

    private Object _key;
    private boolean _notifyAsync;
    private short _callBackId;
    private Broker _parent;
    private UsageStats _stats;
    private EventCacheItem _item;
    private EventCacheItem _oldItem;
    private BitSet _flag;
    private EventDataFilter _dataFilter = EventDataFilter.None;

    public ItemUpdatedCallbackTask(Broker parent, Object key, short callBackId, boolean notifyAsync, EventCacheItem item, EventCacheItem oldItem, BitSet flag, EventDataFilter dataFilter) {
        this._parent = parent;
        this._key = key;
        this._callBackId = callBackId;
        this._notifyAsync = notifyAsync;
        this._item = item;
        this._oldItem = oldItem;
        this._flag = flag;
        this._dataFilter = dataFilter;
    }

    @Override
    public void Process() {
        try {
            if (_parent != null && _parent._cache.getAsyncEventsListener() != null) {
                _stats = new UsageStats();
                _stats.BeginSample();
                _parent._cache.getEventsListener().OnCustomUpdateCallback(_callBackId, _key, _notifyAsync, _item, _oldItem, _flag, _dataFilter);

                _stats.EndSample();
            }
        } catch (Exception ex) {
            if (_parent != null && _parent.getLogger()!=null && _parent.getLogger().getIsErrorLogsEnabled()) {
                _parent.getLogger().getCacheLog().Error("Item Updated Callback Task.Process", ex.getMessage());
            }
        }
    }
}
