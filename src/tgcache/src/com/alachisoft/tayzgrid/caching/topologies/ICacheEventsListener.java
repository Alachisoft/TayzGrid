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

package com.alachisoft.tayzgrid.caching.topologies;

import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.OpCode;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.EventContext;
import com.alachisoft.tayzgrid.common.datastructures.NewHashmap;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;

/**
 * Events callback interface used by the listeners of Cache events.
 */
public interface ICacheEventsListener {

    /**
     * Fired when an item is added to the cache.
     *
     * @param key key of the cache item
     */
    void OnItemAdded(Object key, OperationContext operationContext, EventContext eventContext);

    /**
     * Fired when an item is updated in the cache.
     *
     * @param key key of the cache item
     */
    void OnItemUpdated(Object key, OperationContext operationContext, EventContext eventContext);

    /**
     * Fired when an item is removed from the cache.
     *
     * @param key key of the cache item
     * @param val item itself
     * @param reason reason the item was removed
     */
    void OnItemRemoved(Object key, Object val, ItemRemoveReason reason, OperationContext operationContext, EventContext eventContext) throws OperationFailedException, CacheException, LockingException;

    /**
     * Fired when one ar many items are removed from the cache.
     *
     * @param reason reason the item was removed
     */
    void OnItemsRemoved(Object[] keys, Object[] vals, ItemRemoveReason reason, OperationContext operationContext, EventContext[] eventContext) throws OperationFailedException, CacheException, LockingException;

    /**
     * Fire when the cache is cleared.
     */
    void OnCacheCleared(OperationContext operationContext, EventContext eventContext);
    
    //void OnMapReduceTaskCompleteCallback

    /**
     * Fire and make user happy.
     */
    void OnCustomEvent(Object notifId, Object data, OperationContext operationContext, EventContext eventContext);

    void OnCustomUpdateCallback(Object key, Object value, OperationContext operationContext, EventContext eventContext);

    void OnCustomRemoveCallback(Object key, Object value, ItemRemoveReason reason, OperationContext operationContext, EventContext eventContext) throws OperationFailedException;

    /**
     * Fire when hasmap changes when - new node joins - node leaves -
     * manual/automatic load balance
     *
     * @param newHashmap new hashmap
     */
    void OnHashmapChanged(NewHashmap newHashmap, boolean updateClientMap);

    void OnWriteBehindOperationCompletedCallback(OpCode operationCode, Object result, CallbackEntry cbEntry);

    void OnTaskCallback(Object taskID,Object value,OperationContext operationContext,EventContext eventContext);
}