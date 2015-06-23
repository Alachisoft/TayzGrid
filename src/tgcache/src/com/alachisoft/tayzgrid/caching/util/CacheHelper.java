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

package com.alachisoft.tayzgrid.caching.util;

import com.alachisoft.tayzgrid.caching.topologies.clustered.ClusterCacheBase;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResult;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResultWithEntry;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHintType;
import com.alachisoft.tayzgrid.caching.autoexpiration.AggregateExpirationHint;
import com.alachisoft.tayzgrid.caching.EventCacheEntry;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CallbackInfo;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.caching.datagrouping.GroupInfo;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import java.util.Iterator;
import java.util.Map;

/**
 * Class to help in common cache operations
 */
public class CacheHelper
{

    /**
     * Returns the number of items in local instance of the cache.
     *
     * @param cache
     * @return
     * @throws GeneralFailureException
     * @throws OperationFailedException
     * @throws CacheException
     */
    public static long GetLocalCount(CacheBase cache) throws GeneralFailureException, OperationFailedException, CacheException
    {
        if (cache != null && cache.getInternalCache() != null)
        {
            return cache.getInternalCache().getCount();
        }
        return 0;
    }

    public static long getCacheSize(CacheBase cache)
    {
        if (cache != null && cache.getInternalCache() != null)
        {
            return cache.getInternalCache().getSize();
        }
        return 0L;
    }

    /**
     * Tells if the cache is a clustered cache or a local one
     *
     * @param cache
     * @return
     */
    public static boolean IsClusteredCache(CacheBase cache)
    {
        if (cache == null)
        {
            return false;
        }
        else
        {
            return ClusterCacheBase.class.isAssignableFrom(cache.getClass());
        }
    }

    /**
     * Copies the entries of a cache into an array. Used for state transfer.
     *
     * @param cache cache object
     * @param count number of entries to return
     * @return array of cache entries
     */
    public static CacheEntry[] GetCacheEntries(CacheBase cache, long count)
    {
        int index = 0;
        CacheEntry[] entArr = null;
        CacheEntry ent = null;
        cache.getSync().AcquireReaderLock();
        try
        {
            if (count == 0 || count > cache.getCount())
            {
                count = cache.getCount();
            }
            entArr = new CacheEntry[(int) count];
            Iterator i = cache.GetEnumerator();
            while (index < count && i.hasNext())
            {
                Object cacheEntry = ((Map.Entry) i.next()).getValue();
                ent = (CacheEntry) ((cacheEntry instanceof CacheEntry) ? cacheEntry : null);
                entArr[index++] = ent.RoutableClone(null);
            }
        }
        catch (Exception e)
        {
            cache.getContext().getCacheLog().Error("CacheHelper.CreateLocalEntry()", e.getMessage());
            return null;
        }
        finally
        {
            cache.getSync().ReleaseReaderLock();
        }
        return entArr;
    }

    /**
     * Merge the first entry i.e. c1 into c2
     *
     * @param c1
     * @param c2
     * @return returns merged entry c2
     */
    public static CacheEntry MergeEntries(CacheEntry c1, CacheEntry c2)
    {
        if (c1 != null && c1.getValue() instanceof CallbackEntry)
        {
            CallbackEntry cbEtnry = null;
            Object tempVar = c1.getValue();
            cbEtnry = (CallbackEntry) ((tempVar instanceof CallbackEntry) ? tempVar : null);

            if (cbEtnry.getItemRemoveCallbackListener() != null)
            {
                for (Iterator it = cbEtnry.getItemRemoveCallbackListener().iterator(); it.hasNext();)
                {
                    CallbackInfo cbInfo = (CallbackInfo) it.next();
                    c2.AddCallbackInfo(null, cbInfo);
                }

            }
            if (cbEtnry.getItemUpdateCallbackListener() != null)
            {
                for (Iterator it = cbEtnry.getItemUpdateCallbackListener().iterator(); it.hasNext();)
                {
                    CallbackInfo cbInfo = (CallbackInfo) it.next();
                    c2.AddCallbackInfo(cbInfo, null);
                }

            }
        }
        if (c1 != null && c1.getEvictionHint() != null)
        {
            if (c2.getEvictionHint() == null)
            {
                c2.setEvictionHint(c1.getEvictionHint());
            }
        }
        return c2;
    }

    public static Object[] GetKeyDependencyTable(ExpirationHint hint)
    {
        java.util.ArrayList keyList = null;
        if (hint != null)
        {
            if (hint._hintType == ExpirationHintType.AggregateExpirationHint)
            {
                ExpirationHint[] hints = ((AggregateExpirationHint) hint).getHints();
          
                if (keyList != null && keyList.size() > 0)
                {
                    Object[] cacheKeys = new Object[keyList.size()];
                    System.arraycopy(keyList.toArray(), 0, cacheKeys, 0, keyList.size());
                    return cacheKeys;
                }
            }
           
        }
        return null;
    }

    public static EventCacheEntry CreateCacheEventEntry(java.util.List listeners, CacheEntry cacheEntry)
    {
        EventDataFilter maxFilter = EventDataFilter.None;
        for (Object temp : listeners)
        {
            CallbackInfo cbInfo = (CallbackInfo)temp;
            if (cbInfo.getDataFilter().getValue() > maxFilter.getValue())
            {
                    maxFilter = cbInfo.getDataFilter();
            }
            if (maxFilter == EventDataFilter.DataWithMetaData)
            {
                    break;
            }
        }

        return CreateCacheEventEntry(maxFilter, cacheEntry);
    }


    public static EventCacheEntry CreateCacheEventEntry(EventDataFilter filter, CacheEntry cacheEntry)
    {
        if (filter != EventDataFilter.None && cacheEntry != null)
        {
                cacheEntry = (CacheEntry)cacheEntry.clone();
                EventCacheEntry entry = new EventCacheEntry(cacheEntry);
                entry.setFlags(cacheEntry.getFlag());

                if (filter == EventDataFilter.DataWithMetaData)
                {
                        if (cacheEntry.getValue() instanceof CallbackEntry)
                        {
                                entry.setValue(((CallbackEntry)cacheEntry.getValue()).getValue());
                        }
                        else
                        {
                                entry.setValue( cacheEntry.getValue());
                        }

                }
                return entry;
        }
        return null;
    }

    public static boolean ReleaseLock(CacheEntry existingEntry, CacheEntry newEntry)
    {
        if (CheckLockCompatibility(existingEntry, newEntry))
        {
            existingEntry.ReleaseLock();
            newEntry.ReleaseLock();
            return true;
        }
        return false;
    }

    public static boolean CheckLockCompatibility(CacheEntry existingEntry, CacheEntry newEntry)
    {
        Object lockId = null;
        java.util.Date lockDate = new java.util.Date(0);
        tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
        tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
        boolean tempVar = existingEntry.IsLocked(tempRef_lockId, tempRef_lockDate);
        lockId = tempRef_lockId.argvalue;
        lockDate = tempRef_lockDate.argvalue;
        if (tempVar)
        {
            return existingEntry.getLockId().equals(newEntry.getLockId());
        }
        return true;
    }

    /**
     * Determines weather two data groups are compatible or not.
     *
     * @param g1
     * @param g2
     * @return
     */
    public static boolean CheckDataGroupsCompatibility(GroupInfo g1, GroupInfo g2)
    {

        boolean compatible = false;
        if (g1 == null && g2 == null)
        {
            compatible = true;
        }
        else if (g1 != null && g2 != null)
        {
            boolean G1Compatibility = g1.getGroup() == null ? g2.getGroup() == null : g1.getGroup().equals(g2.getGroup());
            boolean SubGCompatibility = g1.getSubGroup() == null ? g2.getSubGroup() == null : g1.getSubGroup().equals(g2.getSubGroup());
            compatible = G1Compatibility && SubGCompatibility;
        }
        else if (g1 != null)
        {
            if (g1.getGroup() == null && g1.getSubGroup() == null)
            {
                compatible = true;
            }
        }
        else if (g2.getGroup() == null && g2.getSubGroup() == null)
        {
            compatible = true;
        }

        return compatible;

    }

    /**
     * Gives the list of insertable items. An item to be inserted is said to be insertable if its data groups match the existing items data groups.
     *
     * @param existingItems Table of the exsiting items data group info
     * @param newItems Table of the data group info of the items to be inserted
     * @return Items which have no data grop conflicts
     */
    public static java.util.HashMap GetInsertableItems(java.util.HashMap existingItems, java.util.HashMap newItems)
    {
        java.util.HashMap insertable = new java.util.HashMap();
        Object key;
        Object value;
        Map.Entry KeyValue;
        GroupInfo newInfo;
        GroupInfo oldInfo;
        if (existingItems != null)
        {
            Iterator ide = existingItems.entrySet().iterator();
            while (ide.hasNext())
            {
                KeyValue = (Map.Entry) ide.next();
                key = KeyValue.getKey();
                value = KeyValue.getValue();
                GroupInfo tempVar = ((CacheEntry) newItems.get(key)).getGroupInfo();
                newInfo = (GroupInfo) ((tempVar instanceof GroupInfo) ? tempVar : null);
                oldInfo = (GroupInfo) ((value instanceof GroupInfo) ? value : null);
                if (CacheHelper.CheckDataGroupsCompatibility(newInfo, oldInfo))
                {
                    insertable.put(key, newItems.get(key));
                }
            }
        }
        return insertable;
    }

    /**
     * Gets the list of failed items.
     *
     * @param insertResults
     * @return
     */
    public static java.util.HashMap CompileInsertResult(java.util.HashMap insertResults)
    {
        java.util.HashMap failedTable = new java.util.HashMap();
        Map.Entry KeyValue;
        Object key;
        if (insertResults != null)
        {
            CacheInsResult result;
            Iterator ide = insertResults.entrySet().iterator();
            while (ide.hasNext())
            {
                KeyValue = (Map.Entry) ide.next();
                key = KeyValue.getKey();
                if (KeyValue.getValue() instanceof CacheInsResultWithEntry)
                {
                    result = ((CacheInsResultWithEntry) KeyValue.getValue()).getResult();
                    switch (result)
                    {
                        case SuccessOverwrite:
                            failedTable.put(key, KeyValue.getValue());
                            break;
                        case Failure:
                            failedTable.put(key, new OperationFailedException("Generic operation failure; not enough information is available."));
                            break;
                        case NeedsEviction:
                            failedTable.put(key, new OperationFailedException("The cache is full and not enough items could be evicted."));
                            break;
                        case IncompatibleGroup:
                            failedTable.put(key, new OperationFailedException("Data group of the inserted item does not match the existing item's data group"));
                            break;
                        case DependencyKeyNotExist:
                            failedTable.put(key, new OperationFailedException("One of the dependency keys does not exist."));
                            break;
                    }
                }
            }
        }
        return failedTable;
    }
}