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

package com.alachisoft.tayzgrid.caching.topologies.local;

import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.datagrouping.GroupInfo;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.caching.queries.QueryContext;
import com.alachisoft.tayzgrid.caching.queries.filters.Predicate;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.topologies.ICacheEventsListener;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.serialization.util.TypeInfoMap;
import com.alachisoft.tayzgrid.caching.util.MiscUtil;

public class IndexedOverflowCache extends OverflowCache {

    public IndexedOverflowCache(java.util.Map cacheClasses, CacheBase parentCache, java.util.Map properties, ICacheEventsListener listener, CacheRuntimeContext context) throws ConfigurationException {
        super(cacheClasses, parentCache, properties, listener, context);
    }

    @Override
    protected LocalCacheBase CreateLocalCache(CacheBase parentCache, java.util.Map cacheClasses, java.util.Map schemeProps) throws ConfigurationException {
        return new IndexedLocalCache(cacheClasses, parentCache, schemeProps, null, _context);
    }

    @Override
    protected LocalCacheBase CreateOverflowCache(java.util.Map cacheClasses, java.util.Map schemeProps) throws ConfigurationException {
        return new IndexedOverflowCache(cacheClasses, this, schemeProps, null, _context);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public final TypeInfoMap getTypeInfoMap() {
        return _primary.getTypeInfoMap();
    }

    @Override
    public java.util.ArrayList GetGroupKeys(String group, String subGroup, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException {

        return null;
    }

    @Override
    public java.util.HashMap GetGroupData(String group, String subGroup, OperationContext operationContext) {

        return null;
    }

    @Override
    public java.util.ArrayList getDataGroupList() {
        java.util.ArrayList list = new java.util.ArrayList();

        if (_primary != null) {
            java.util.Collection primarylist = _primary.getDataGroupList();
            if (primarylist != null) {
                list.addAll(primarylist);
            }
        }
        if (_secondary != null) {
            java.util.Collection secondarylist = _secondary.getDataGroupList();
            if (secondarylist != null) {
                java.util.Iterator ie = secondarylist.iterator();
                while (ie.hasNext()) {
                    if (!list.contains(ie.next())) {
                        list.addAll(secondarylist);
                    }
                }
            }
        }
        return list;
    }

    @Override
    public GroupInfo GetGroupInfo(Object key, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException {
        GroupInfo info = null;

        return info;
    }

    @Override
    public java.util.HashMap GetGroupInfoBulk(Object[] keys, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException {
        return null;
    }

    @Override
    public java.util.HashMap Remove(String group, String subGroup, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException {
        java.util.ArrayList list = GetGroupKeys(group, subGroup, operationContext);
        Object[] keys = MiscUtil.GetArrayFromCollection(list);
        return null;
    }

    @Override
    public QueryContext SearchInternal(Predicate pred, java.util.Map values) {
        return null;
    }
}