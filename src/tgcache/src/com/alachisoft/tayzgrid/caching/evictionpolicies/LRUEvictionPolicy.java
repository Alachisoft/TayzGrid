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

package com.alachisoft.tayzgrid.caching.evictionpolicies;

import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.Cache;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Eviction policy based on the timestamp When needed objects with lowest timestamp are removed first.
 */
public class LRUEvictionPolicy implements IEvictionPolicy
{
    private EvictionIndex _index;
    private float _ratio = 0.25F;
    private java.util.Date _initTime = new java.util.Date(0);
    private int _minHint;
    private int _maxHint;
    /**
     * It is the interval between two consecutive removal of items from the cluster so that user operation is not affected
     */
    private long _sleepInterval = 0; //milliseconds
    /**
     * No of items which can be removed in a single clustered operation.
     */
    private int _removeThreshhold = 10;

    public static class TimestampComparer implements java.util.Comparator
    {

        private java.util.HashMap _unsortedList;

        public TimestampComparer(java.util.HashMap unsortedList)
        {
            _unsortedList = unsortedList;
        }

        public int compare(Object x, Object y)
        {
            Object first = _unsortedList.get(x);
            Object second = _unsortedList.get(y);

            return ((java.util.Date) first).compareTo((java.util.Date) second);
        }
    }

    /**
     * Default constructor
     */
    public LRUEvictionPolicy()
    {
        Initialize();
    }

    /**
     * Overloaded constructor Initializes the object based on the properties specified in configuration and eviction ratio
     *
     * @param properties
     * @param ?
     */
    public LRUEvictionPolicy(java.util.Map properties, float ratio)
    {
        _ratio = ratio / 100f;
        _sleepInterval = Integer.parseInt(ServicePropValues.CacheServer_EvictionBulkRemoveDelay);
        _removeThreshhold = Integer.parseInt(ServicePropValues.CacheServer_EvictionBulkRemoveSize);

        Initialize();
    }

    /**
     * Initialize Policy
     */
    private void Initialize()
    {
        _index = new EvictionIndex();
        TimeZone utc = TimeZone.getTimeZone("UTC");
        Calendar calendar = Calendar.getInstance(utc);
        calendar.set(1999, 1, 1, 0, 0, 0);
        _initTime = calendar.getTime();
        _minHint = _maxHint = 0;
    }

    /**
     * Check if the provided eviction hint is compatible with the policy and return the compatible eviction hint
     *
     * @param eh eviction hint.
     * @return a hint compatible to the eviction policy.
     */
    public final EvictionHint CompatibleHint(EvictionHint eh)
    {
        if (eh != null && eh instanceof TimestampHint)
        {
            return eh;
        }
        return new TimestampHint();
    }
    private long totalEvicted;

    /**
     *
     *
     * @param cache
     * @param count
     */
    
    public void Execute(CacheBase cache, CacheRuntimeContext context, long evictSize)
    {
        //notification is sent for a max of 100k data if multiple items...
        //otherwise if a single item is greater than 100k then notification is sent for
        //that item only...
        int notifThreshold = 30 * 1024;

        ILogger NCacheLog = cache.getContext().getCacheLog();

        if (NCacheLog.getIsInfoEnabled())
        {
            try
            {
                NCacheLog.Info("LRU LocalCache.Evict()", "Cache Size: {0}" + (new Long(cache.getCount())).toString());
            }
            catch (CacheException cacheException)
            {
                context.getCacheLog().Info("LRU LocalCache.Evict()", "Logging cache count throws Exception: " + cacheException.getMessage());
            }
        }

        _sleepInterval = Integer.parseInt(ServicePropValues.CacheServer_EvictionBulkRemoveDelay);
        _removeThreshhold = Integer.parseInt(ServicePropValues.CacheServer_EvictionBulkRemoveSize);

        java.util.Date startTime = new java.util.Date();
        java.util.ArrayList selectedKeys = GetSelectedKeys(cache, (long) Math.ceil(evictSize * _ratio));
        java.util.Date endTime = new java.util.Date();
        if (NCacheLog.getIsInfoEnabled())
        {
            try
            {
                NCacheLog.Info("LocalCache.Evict()", String.format("Time Span for {0} Items: " + (TimeSpan.Subtract(endTime, startTime)), selectedKeys.size()));
            }
            catch (ArgumentException argumentException)
            {
            }
        }

        startTime = new java.util.Date();
        Cache rootCache = context.getCacheRoot();

        java.util.ArrayList keysTobeRemoved = new java.util.ArrayList();
        java.util.ArrayList dependentItems = new java.util.ArrayList();
        java.util.ArrayList removedItems = null;


        totalEvicted += selectedKeys.size();

        java.util.Iterator e = selectedKeys.iterator();
        int removedThreshhold = _removeThreshhold / 300;
        int remIteration = 0;

        while (e.hasNext())
        {
            Object key = e.next();
            keysTobeRemoved.add(key);

            if (keysTobeRemoved.size() % 300 == 0)
            {
                try
                {
                    OperationContext lruEvictionOperationContext = new OperationContext();
                    lruEvictionOperationContext.Add(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                    
                    Object tempVar = cache.RemoveSync(keysTobeRemoved.toArray(new Object[0]), ItemRemoveReason.Underused, false, lruEvictionOperationContext);
                    removedItems = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
                    context.PerfStatsColl.incrementEvictPerSecStatsBy(keysTobeRemoved.size());
                }
                catch (Exception ex)
                {
                    NCacheLog.Error("LruEvictionPolicy.Execute", "an error occured while removing items. Error " + ex.toString());
                }
                keysTobeRemoved.clear();
                if (removedItems != null && removedItems.size() > 0)
                {
                    dependentItems.addAll(removedItems);
                }

                remIteration++;
                if (remIteration >= removedThreshhold)
                {
                    //put some delay so that user operations are not affected.
                    try
                    {
                        Thread.sleep(_sleepInterval * 1000L);
                    }
                    catch (InterruptedException interruptedException)
                    {
                        break;
                    }
                    remIteration = 0;
                }
            }
        }

        if (keysTobeRemoved.size() > 0)
        {
            try
            {
                 OperationContext lruEvictionOperationContext = new OperationContext();
                    lruEvictionOperationContext.Add(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                    
                Object tempVar2 = cache.RemoveSync(keysTobeRemoved.toArray(new Object[0]), ItemRemoveReason.Underused, false, lruEvictionOperationContext);
                removedItems = (java.util.ArrayList) ((tempVar2 instanceof java.util.ArrayList) ? tempVar2 : null);
                context.PerfStatsColl.incrementEvictPerSecStatsBy(keysTobeRemoved.size());
                if (removedItems != null && removedItems.size() > 0)
                {
                    dependentItems.addAll(removedItems);
                }
            }
            catch (Exception ex)
            {
                NCacheLog.Error("LruEvictionPolicy.Execute", "an error occured while removing items. Error " + ex.toString());
            }
        }

        if (dependentItems.size() > 0)
        {
            java.util.ArrayList removableList = new java.util.ArrayList();
            if (rootCache != null)
            {
                for (Object depenentItme : dependentItems)
                {
                    if (depenentItme == null)
                    {
                        continue;
                    }
                    removableList.add(depenentItme);
                    if (removableList.size() % 100 == 0)
                    {
                        try
                        {
                            OperationContext lruEvictionOperationContext = new OperationContext();
                            lruEvictionOperationContext.Add(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                            
                            rootCache.CascadedRemove(removableList.toArray(new Object[0]), ItemRemoveReason.Underused, true, lruEvictionOperationContext);
                            context.PerfStatsColl.incrementEvictPerSecStatsBy(removableList.size());
                        }
                        catch (Exception exc)
                        {
                            NCacheLog.Error("LruEvictionPolicy.Execute", "an error occured while removing dependent items. Error " + exc.toString());

                        }
                        removableList.clear();
                    }

                }
                if (removableList.size() > 0)
                {
                    try
                    {
                         OperationContext lruEvictionOperationContext = new OperationContext();
                            lruEvictionOperationContext.Add(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                            
                        rootCache.CascadedRemove(removableList.toArray(new Object[0]), ItemRemoveReason.Underused, true,lruEvictionOperationContext);
                        context.PerfStatsColl.incrementEvictPerSecStatsBy(removableList.size());
                    }
                    catch (Exception exc)
                    {
                        NCacheLog.Error("LruEvictionPolicy.Execute", "an error occured while removing dependent items. Error " + exc.toString());

                    }
                    removableList.clear();
                }
            }
        }
        return;

    }

    public float getEvictRatio()
    {
        return _ratio;
    }

    public void setEvictRatio(float value)
    {
        _ratio = value;
    }

    /**
     *
     * @param key
     * @param hint
     */
    public void Notify(Object key, EvictionHint oldhint, EvictionHint newHint)
    {
        synchronized (_index.getSyncRoot())
        {
            EvictionHint hint = oldhint == null ? newHint : oldhint;
            if (_index != null && key != null && hint != null)
            {
                TimeSpan diffTime = null;
                try
                {
                    diffTime = TimeSpan.Subtract(((TimestampHint) hint).getTimeStamp(), _initTime);
                }
                catch (ArgumentException argumentException)
                {
                }
                long indexKey = GetIndexKey(diffTime);

                if (_index.Contains(indexKey, key))
                {
                    _index.Remove(indexKey, key);

                    hint = newHint == null ? oldhint : newHint;
                    hint.Update(); //hint is not new so update.

                    try
                    {
                        diffTime = TimeSpan.Subtract(((TimestampHint) hint).getTimeStamp(), _initTime);
                    }
                    catch (ArgumentException argumentException)
                    {
                    }
                    indexKey = GetIndexKey(diffTime);
                    _index.Add(indexKey, key);
                }
                else
                {
                    _index.Add(indexKey, key);
                }
            }
        }
    }

    /**
     *
     * @param key
     */
    public void Remove(Object key, EvictionHint hint)
    {
        if (hint == null)
        {
            return;
        }

        synchronized (_index.getSyncRoot())
        {
            TimeSpan diffTime = null;
            try
            {
                diffTime = TimeSpan.Subtract(((TimestampHint) hint).getTimeStamp(), _initTime);
            }
            catch (ArgumentException argumentException)
            {
            }
            long indexKey = GetIndexKey(diffTime);
            _index.Remove(indexKey, key);
        }
    }

    public void Clear()
    {
        synchronized (_index.getSyncRoot())
        {
            _index.Clear();
        }
    }

    /**
     *
     * @param cache
     * @param evictCount
     * @return
     */
    private java.util.ArrayList GetSelectedKeys(CacheBase cache, long evictSize)
    {
        return _index.GetSelectedKeys(cache, evictSize);
    }

    /**
     *
     * @param diffTime
     * @return
     */
    private long GetIndexKey(TimeSpan diffTime)
    {
        return (long) diffTime.getTotalSeconds();
    }
        
    @Override
    public long getIndexInMemorySize() {
        return this.getLRUEvictionIndexSize();
    }
    
    private long getLRUEvictionIndexSize()
    {
        long temp = 0;
        if(_index != null)
        {
            temp += _index.getIndexInMemorySize();
            temp += _index.getKeysCount() * TimestampHint.getInMemorySize();
        }
        return temp;
    }
}

