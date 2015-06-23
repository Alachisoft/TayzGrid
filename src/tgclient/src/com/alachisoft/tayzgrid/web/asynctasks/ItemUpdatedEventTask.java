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
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.web.caching.RemoteCache.Broker;
import com.alachisoft.tayzgrid.web.events.EventCacheItem;
import java.io.IOException;

public class ItemUpdatedEventTask implements AsyncProcessor.IAsyncTask {

    private Object _key;
    private boolean _notifyAsync;
    private Broker _parent;
    private UsageStats _stats;
    private EventCacheItem _item;
    private EventCacheItem _oldItem;
    private BitSet _flag;

    public ItemUpdatedEventTask(Broker parent, Object key, boolean notifyAsync, EventCacheItem item, EventCacheItem oldItem, BitSet flag) {
        this._parent = parent;
        this._key = key;
        this._notifyAsync = notifyAsync;
        this._item = item;
        this._oldItem = oldItem;
        this._flag = flag;
    }

    @Override
    public void Process() throws OperationFailedException, IOException, CacheException, LockingException {
        try {
            if (_parent != null) {
                _stats = new UsageStats();
                _stats.BeginSample();
                _parent._cache.getEventsListener().OnItemUpdated(_key, _notifyAsync, _item, _oldItem, _flag);

                _stats.EndSample();
            }
        } catch (Exception ex) {
            if (_parent != null && _parent.getLogger()!=null && _parent.getLogger().getIsErrorLogsEnabled()) {
                _parent.getLogger().getCacheLog().Error("Item Updated Task.Process", ex.getMessage());
            }
        }
    }
}
