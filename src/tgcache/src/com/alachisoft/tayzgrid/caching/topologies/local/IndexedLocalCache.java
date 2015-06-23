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

import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.TagComparisonType;
import com.alachisoft.tayzgrid.caching.autoexpiration.LockExpiration;
import com.alachisoft.tayzgrid.caching.datagrouping.GroupIndexManager;
import com.alachisoft.tayzgrid.caching.datagrouping.GroupInfo;
import com.alachisoft.tayzgrid.caching.enumeration.EnumerationIndex;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.caching.queries.NamedTagIndexManager;
import com.alachisoft.tayzgrid.caching.queries.QueryContext;
import com.alachisoft.tayzgrid.caching.queries.QueryIndexManager;
import com.alachisoft.tayzgrid.caching.queries.filters.Predicate;
import com.alachisoft.tayzgrid.caching.topologies.CacheAddResult;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResult;
import com.alachisoft.tayzgrid.caching.topologies.ICacheEventsListener;
import com.alachisoft.tayzgrid.caching.util.CacheHelper;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;
import com.alachisoft.tayzgrid.common.datastructures.RedBlackException;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.serialization.util.TypeInfoMap;
import java.util.Iterator;
import java.util.Map;

public class IndexedLocalCache extends LocalCache {

    private GroupIndexManager _grpIndexManager;
    private QueryIndexManager _queryIndexManager;
    private EnumerationIndex _enumerationIndex;

    public IndexedLocalCache(java.util.Map cacheClasses, CacheBase parentCache, java.util.Map properties, ICacheEventsListener listener, CacheRuntimeContext context) throws ConfigurationException {
        super(cacheClasses, parentCache, properties, listener, context);
        _grpIndexManager = new GroupIndexManager();

        java.util.Map props = null;
        if (properties.containsKey("indexes")) {
            props = (java.util.Map) ((properties.get("indexes") instanceof java.util.Map) ? properties.get("indexes") : null);
        }

        _queryIndexManager = new NamedTagIndexManager(props, this, _context.getCacheRoot().getName(), context._dataSharingKnownTypesforNet);

        if (!_queryIndexManager.Initialize()) {
            _queryIndexManager = null;
        }
        
        _cacheStore.setISizableQueryIndexManager(_queryIndexManager);
        _cacheStore.setISizableEvictionIndexManager(_evictionPolicy);
        _cacheStore.setISizableExpirationIndexManager(_context.ExpiryMgr);
        
        _stats.setMaxCount(_cacheStore.getMaxCount());
        _stats.setMaxSize(_cacheStore.getMaxSize());
        
        if(_context.PerfStatsColl != null)
        {
            _context.PerfStatsColl.SetQueryIndexSize(_queryIndexManager.getIndexInMemorySize());
        }
        
    }

    @Override
    public void dispose() {
        super.dispose();
        if (_queryIndexManager != null) {

            _queryIndexManager.dispose();
            _queryIndexManager = null;
        }
        if (_grpIndexManager != null) {
            _grpIndexManager.dispose();
            _grpIndexManager = null;
        }
    }

    public final QueryIndexManager getIndexManager() {
        return _queryIndexManager;
    }

    @Override
    public final TypeInfoMap getTypeInfoMap() {
        if (_queryIndexManager != null) {

            return _queryIndexManager.getTypeInfoMap();
        } else {
            return null;
        }
    }

    @Override
    public void ClearInternal() {
        super.ClearInternal();
        _grpIndexManager.Clear();
        if (_queryIndexManager != null) {

            _queryIndexManager.Clear();
            _context.PerfStatsColl.SetQueryIndexSize(_queryIndexManager.getIndexInMemorySize());
        }
    }

    @Override
    public QueryContext SearchInternal(Predicate pred, java.util.Map values) throws CacheException {
        QueryContext queryContext = new QueryContext(this);
        queryContext.setAttributeValues(values);
        queryContext.setCacheContext(_context.getCacheRoot().getName());

        try {
            pred.Execute(queryContext, null);
            return queryContext;

            //return null;
        } catch (com.alachisoft.tayzgrid.parser.AttributeIndexNotDefined e) {
            throw e;
        } catch (com.alachisoft.tayzgrid.parser.TypeIndexNotDefined e) {
            throw e;
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    @Override
    public QueryContext DeleteQueryInternal(Predicate pred, java.util.Map values)
            throws Exception {
        QueryContext queryContext = new QueryContext(this);
        queryContext.setAttributeValues(values);
        queryContext.setCacheContext(_context.getCacheRoot().getName());

        try {
            pred.Execute(queryContext, null);
            return queryContext;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public CacheEntry GetInternal(Object key, boolean isUserOperation, OperationContext operationContext) throws StateTransferException {
        CacheEntry entry = super.GetInternal(key, isUserOperation, operationContext);

        if (entry != null) {
            if (operationContext != null) {
                if (operationContext.Contains(OperationContextFieldName.GenerateQueryInfo)) {
                    if (entry.getObjectType() != null) {
                        CacheEntry clone = (CacheEntry) entry.clone();
                        clone.setQueryInfo(_queryIndexManager.GetQueryInfo(key, entry));
                        return clone;
                    }
                }
            }
        }

        return entry;
    }

    @Override
    public java.util.ArrayList GetGroupKeys(String group, String subGroup, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException {
        return _grpIndexManager.GetGroupKeys(group, subGroup);
    }

    @Override
    public CacheEntry GetGroup(Object key, String group, String subGroup, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        if (_grpIndexManager.KeyExists(key, group, subGroup)) {
            return Get(key, version, lockId, lockDate, lockExpiration, accessType, operationContext);
        }

        return null;
    }

    @Override
    public java.util.HashMap GetGroup(Object[] keys, String group, String subGroup, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException {
        java.util.HashMap result = new java.util.HashMap();

        for (int i = 0; i < keys.length; i++) {
            try {
                if (_grpIndexManager.KeyExists(keys[i], group, subGroup)) {
                    result.put(keys[i], Get(keys[i], operationContext));
                }
            } catch (StateTransferException se) {
                result.put(keys[i], se);
            } catch (Exception e) {
                throw new OperationFailedException(e);
            }
        }
        return result;
    }

    @Override
    public java.util.ArrayList getDataGroupList() {
        return _grpIndexManager != null ? _grpIndexManager.getDataGroupList() : null;
    }

    @Override
    public GroupInfo GetGroupInfo(Object key, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        CacheEntry entry = Get(key, operationContext);
        GroupInfo info = null;
        if (entry != null) {
            if (entry.getGroupInfo() != null) {
                info = new GroupInfo(entry.getGroupInfo().getGroup(), entry.getGroupInfo().getSubGroup());
            } else {
                info = new GroupInfo(null, null);
            }
        }

        return info;
    }

    @Override
    public java.util.HashMap GetGroupInfoBulk(Object[] keys, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        java.util.HashMap infoTahle = new java.util.HashMap();
        java.util.HashMap entries = Get(keys, operationContext);
        CacheEntry currentEntry;
        GroupInfo info;
        if (entries != null) {
            Iterator ide = entries.entrySet().iterator();
            while (ide.hasNext()) {
                Map.Entry pair = (Map.Entry) ide.next();
                info = null;
                currentEntry = (CacheEntry) pair.getValue();
                if (currentEntry != null) {
                    info = currentEntry.getGroupInfo();
                    if (info == null) {
                        info = new GroupInfo(null, null);
                    }
                }
                infoTahle.put(pair.getKey(), info);
            }
        }
        return infoTahle;
    }

    @Override
    public java.util.ArrayList GetTagKeys(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException {
        switch (comparisonType) {
            case BY_TAG:
                return ((NamedTagIndexManager) _queryIndexManager).GetByTag(tags[0]);

            case ANY_MATCHING_TAG:
                return ((NamedTagIndexManager) _queryIndexManager).GetAnyMatchingTag(tags);

            case ALL_MATCHING_TAGS:
                return ((NamedTagIndexManager) _queryIndexManager).GetAllMatchingTags(tags);
        }
        return null;
    }

    @Override
    public java.util.HashMap Remove(String group, String subGroup, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        java.util.ArrayList list = _grpIndexManager.GetGroupKeys(group, subGroup);
        Object[] keys = new Object[list.size()];

        int i = 0;
        for (Object key : list) {
            keys[i] = key;
            i++;
        }

        return Remove(keys, ItemRemoveReason.Removed, notify, operationContext);
    }

    @Override
    public CacheAddResult AddInternal(Object key, CacheEntry cacheEntry, boolean isUserOperation) throws StateTransferException, CacheException {
        CacheAddResult result = super.AddInternal(key, cacheEntry, isUserOperation);
        if (result == CacheAddResult.Success || result == CacheAddResult.SuccessNearEviction) {
            _grpIndexManager.AddToGroup(key, cacheEntry.getGroupInfo());

            if (_queryIndexManager != null && cacheEntry.getQueryInfo() != null) {
                try {
                    _queryIndexManager.AddToIndex(key, cacheEntry);
                } catch (ClassNotFoundException classNotFoundException) {
                    throw new CacheException(classNotFoundException);
                } catch (RedBlackException red) {
                    throw new CacheException(red);
                }
            }
            if(_context.PerfStatsColl != null)
            {
                _context.PerfStatsColl.SetQueryIndexSize(_queryIndexManager.getIndexInMemorySize());
            }                
        }
        return result;
    }

    @Override
    public CacheInsResult InsertInternal(Object key, CacheEntry cacheEntry, boolean isUserOperation, CacheEntry oldEntry, OperationContext operationContext) throws StateTransferException, CacheException {
        try {
            if (oldEntry != null) {

                if (!CacheHelper.CheckDataGroupsCompatibility(cacheEntry.getGroupInfo(), oldEntry.getGroupInfo())) {
                    return CacheInsResult.IncompatibleGroup; // throw new Exception("Data group of the inserted item does not match the existing item's data group");
                }
            }

            CacheInsResult result = super.InsertInternal(key, cacheEntry, isUserOperation, oldEntry, operationContext);
            if (result == CacheInsResult.Success || result == CacheInsResult.SuccessNearEvicition) {
                _grpIndexManager.AddToGroup(key, cacheEntry.getGroupInfo());

                //muds:
                if (_queryIndexManager != null && cacheEntry.getQueryInfo() != null) {
                    _queryIndexManager.AddToIndex(key, cacheEntry);
                }
            } else if (result == CacheInsResult.SuccessOverwrite || result == CacheInsResult.SuccessOverwriteNearEviction) {
                if (oldEntry != null) {
                    _grpIndexManager.RemoveFromGroup(key, oldEntry.getGroupInfo());
                }
                _grpIndexManager.AddToGroup(key, cacheEntry.getGroupInfo());

                if (_queryIndexManager != null) {
                    if (oldEntry != null && oldEntry.getObjectType() != null) {
                        _queryIndexManager.RemoveFromIndex(key, oldEntry.getObjectType());
                    }

                    if (cacheEntry.getQueryInfo() != null) {
                        _queryIndexManager.AddToIndex(key, cacheEntry);
                    }
                }
            }
            
            if(_context.PerfStatsColl != null)
            {
                _context.PerfStatsColl.SetQueryIndexSize(_queryIndexManager.getIndexInMemorySize());
            }
            
            return result;
        } catch (ClassNotFoundException classNotFoundException) {
            throw new CacheException(classNotFoundException);
        } catch (RedBlackException redBlackException) {
            throw new CacheException(redBlackException);
        }
    }

    @Override
    public CacheEntry RemoveInternal(Object key, ItemRemoveReason removalReason, boolean isUserOperation, OperationContext operationContext) throws StateTransferException, CacheException {
        CacheEntry e = super.RemoveInternal(key, removalReason, isUserOperation, operationContext);
        if (e != null) {
            _grpIndexManager.RemoveFromGroup(key, e.getGroupInfo());

            if (_queryIndexManager != null && e.getObjectType() != null) {
                try {
                    _queryIndexManager.RemoveFromIndex(key, e.getObjectType());
                } catch (RedBlackException redBlackException) {
                    throw new CacheException(redBlackException);
                }
            }
        }
        
        if(_context.PerfStatsColl != null)
        {
            _context.PerfStatsColl.SetQueryIndexSize(_queryIndexManager.getIndexInMemorySize());
        }
        
        return e;
    }

    @Override
    public EnumerationDataChunk GetNextChunk(EnumerationPointer pointer, OperationContext operationContext) throws OperationFailedException {
        if (_enumerationIndex == null) {
            _enumerationIndex = new EnumerationIndex(this);
        }

        try {
            EnumerationDataChunk nextChunk = _enumerationIndex.GetNextChunk(pointer);

            return nextChunk;
        } catch (Exception exception) {
            throw new OperationFailedException(exception);
        }
    }

    @Override
    public boolean HasEnumerationPointer(EnumerationPointer pointer) {
        if (_enumerationIndex == null) {
            return false;
        }

        return _enumerationIndex.Contains(pointer);
    }
}
