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

package com.alachisoft.tayzgrid.caching.topologies.clustered;

import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.EventContext;
import com.alachisoft.tayzgrid.common.threading.AsyncProcessor;

/**
 * Asynchronous notification dispatcher.
 */
public class AsyncBroadcastCustomNotifyRemoval implements AsyncProcessor.IAsyncTask {

    /**
     * The listener class
     */
    private ClusterCacheBase _parent;
    /**
     * Message to broadcast
     */
    private Object _key;
    private ItemRemoveReason _reason = ItemRemoveReason.Removed;
    private OperationContext _operationContext;
    private EventContext _eventContext;
    private CacheEntry _entry;

    /**
     * Constructor
     *
     */
    public AsyncBroadcastCustomNotifyRemoval(ClusterCacheBase parent, Object key, CacheEntry entry, ItemRemoveReason reason, OperationContext operationContext, EventContext eventContext) {
        _parent = parent;
        _key = key;
        _entry = entry;
        _reason = reason;
        _operationContext = operationContext;
        _eventContext = eventContext;
    }

    /**
     * Implementation of message sending.
     */
    public void Process() {
        try {
            if (_parent != null) {
                _parent.RaiseCustomRemoveCalbackNotifier(_key, _entry, _reason, false, _operationContext, _eventContext);
            }
        } catch (Exception e) {
        }
    }
}