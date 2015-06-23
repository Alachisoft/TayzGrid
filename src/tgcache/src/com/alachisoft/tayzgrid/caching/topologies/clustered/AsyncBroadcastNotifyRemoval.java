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

import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.EventContext;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.common.threading.AsyncProcessor;

/**
 * Asynchronous notification dispatcher.
 */
public class AsyncBroadcastNotifyRemoval implements AsyncProcessor.IAsyncTask {

    /**
     * The listener class
     */
    private ClusterCacheBase _parent;
    /**
     * Message to broadcast
     */
    private Object _key, _value;
    private ItemRemoveReason _reason = ItemRemoveReason.Removed;
    private OperationContext _operationContext;
    private EventContext[] _eventContexts;

    /**
     * Constructor
     *
     */
    public AsyncBroadcastNotifyRemoval(ClusterCacheBase parent, Object key, Object value, ItemRemoveReason reason, OperationContext operationContext, EventContext[] eventContexts) {
        _parent = parent;
        _key = key;
        _value = value;
        _reason = reason;
        _operationContext = operationContext;
        _eventContexts = eventContexts;
    }

    /**
     * Implementation of message sending.
     */
    public void Process() {
        try {
            if (_parent != null) {
                _parent.RaiseGeneric(new Function((ClusterCacheBase.OpCodes.NotifyBulkRemoval.getValue()), ((Object) new Object[]{_key, _value, _reason, _operationContext, _eventContexts})));
            }
        } catch (Exception e) {
        }
    }
}