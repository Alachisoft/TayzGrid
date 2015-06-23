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

package com.alachisoft.tayzgrid.caching.queries;

import com.alachisoft.tayzgrid.caching.EventContext;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.caching.MetaInformation;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.topologies.local.LocalCacheBase;
import com.alachisoft.tayzgrid.common.datastructures.RedBlackException;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;

/**
 Observes the cache operations that may cause changes in some
 of the registered active query result sets.
*/
public interface IQueryOperationsObserver
{
	void OnItemAdded(Object key, MetaInformation metaInfo, LocalCacheBase cache, String cacheContext, boolean notify, OperationContext operationContext, EventContext eventContext) throws RedBlackException, StateTransferException, CacheException;
	void OnItemUpdated(Object key, MetaInformation metaInfo, LocalCacheBase cache, String cacheContext, boolean notify, OperationContext operationContext, EventContext eventContext) throws RedBlackException, StateTransferException,CacheException;
	void OnItemRemoved(Object key, MetaInformation metaInfo, LocalCacheBase cache, String cacheContext, boolean notify, OperationContext operationContext, EventContext eventContext) throws RedBlackException, StateTransferException,CacheException;
}
