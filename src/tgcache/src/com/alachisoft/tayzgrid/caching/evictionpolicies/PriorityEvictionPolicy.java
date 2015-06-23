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
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.Cache;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.common.ISizableIndex;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.util.MemoryUtil;
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Priority based Eviction Policy When needed objects with lowest priority are removed first
 */
public class PriorityEvictionPolicy implements IEvictionPolicy, ISizableIndex
{
    /**
     * default priority
     */
    private CacheItemPriority _priority = CacheItemPriority.Normal;
    private java.util.HashMap[] _index;
    private int[] _evictionIndexMaxCounts;
    private float _ratio = 0.25F;
    /**
     * It is the interval between two consecutive removal of items from the cluster so that user operation is not affected
     */
    private long _sleepInterval = 0; //milliseconds
    /**
     * No of items which can be removed in a single clustered operation.
     */
    private int _removeThreshhold = 10;

    /**
     * Constructor.
     */
    public PriorityEvictionPolicy()
    {
        Initialize();
    }

    /**
     * Overloaded constructor Initializes the object based on the properties specified in configuration and eviction ratio
     *
     * @param properties
     * @param ratio
     */
    public PriorityEvictionPolicy(java.util.Map properties, float ratio)
    {
        if (properties != null)
        {
            if (properties.containsKey("default-value"))
            {
                String defaultValue = (String) properties.get("default-value");
                _priority = GetPriorityValue(defaultValue);
            }
        }

        if (ServicePropValues.CacheServer_EvictionBulkRemoveDelay != null)
        {
            _sleepInterval = Long.decode(ServicePropValues.CacheServer_EvictionBulkRemoveDelay) * 1000;
        }
        if (ServicePropValues.CacheServer_EvictionBulkRemoveSize != null)
        {
            _removeThreshhold = Integer.decode(ServicePropValues.CacheServer_EvictionBulkRemoveSize);
        }
        _sleepInterval = Integer.parseInt(ServicePropValues.CacheServer_EvictionBulkRemoveDelay);
        _removeThreshhold = Integer.parseInt(ServicePropValues.CacheServer_EvictionBulkRemoveSize);

        _ratio = ratio / 100f;
        Initialize();
    }

    /**
     * Initialize policy
     */
    private void Initialize()
    {
        _index = new java.util.HashMap[5];
        _evictionIndexMaxCounts = new int[5];
    }

    /**
     * Check if the provided eviction hint is compatible with the policy and return the compatible eviction hint
     *
     * @param eh eviction hint.
     * @return a hint compatible to the eviction policy.
     */
    public final EvictionHint CompatibleHint(EvictionHint eh)
    {
        if (eh != null && eh instanceof PriorityEvictionHint)
        {
            return eh;
        }
        return new PriorityEvictionHint(_priority);
    }

    /**
     * Convert the string representation of Priority to PriorityVale enumeration
     *
     * @param priority
     * @return
     */
    private static CacheItemPriority GetPriorityValue(String priority)
    {
        priority = priority.toLowerCase();
        if (priority.equals("notremovable"))
        {
            return CacheItemPriority.NotRemovable;
        }
        else if (priority.equals("high"))
        {
            return CacheItemPriority.High;
        }
        else if (priority.equals("above-normal"))
        {
            return CacheItemPriority.AboveNormal;
        }
        else if (priority.equals("below-normal"))
        {
            return CacheItemPriority.BelowNormal;
        }
        else if (priority.equals("low"))
        {
            return CacheItemPriority.Low;
        }
        return CacheItemPriority.Normal;
    }

    public float getEvictRatio()
    {
        return _ratio;
    }

    public void setEvictRatio(float value)
    {
        _ratio = value;
    }

    public void Notify(Object key, EvictionHint oldhint, EvictionHint newHint)
    {
        //always use the new priority eviction hint.
        EvictionHint hint = newHint;
        if (hint != null)
        {
            CacheItemPriority hintPriority = ((PriorityEvictionHint) hint).getPriority();

            if (hintPriority == CacheItemPriority.Default)
            {
                hintPriority = this._priority; //set the default priority from the config.
                ((PriorityEvictionHint) hint).setPriority(this._priority);
            }
             if ((oldhint!=null))
                {
                    CacheItemPriority oldPriority = ((PriorityEvictionHint)oldhint).getPriority();
                    CacheItemPriority newPriority = ((PriorityEvictionHint)newHint).getPriority();
                    if (oldPriority != newPriority)
                    {                        
                        this.Remove(key, oldhint);
                    }
                }
            synchronized (_index)
            {
                int changedIndex = -1;
                switch (hintPriority)
                {
                    case Low:
                        if (_index[0] == null)
                        {
                            _index[0] = new java.util.HashMap(25000, 0.7f);
                        }
                        _index[0].put(key, hint);
                        changedIndex = 0;
                        break;
                    case BelowNormal:
                        if (_index[1] == null)
                        {
                            _index[1] = new java.util.HashMap(25000, 0.7f);
                        }
                        _index[1].put(key, hint);
                        changedIndex = 1;
                        break;
                    case Normal:
                        if (_index[2] == null)
                        {
                            _index[2] = new java.util.HashMap(25000, 0.7f);
                        }
                        _index[2].put(key, hint);
                        changedIndex = 2;
                        break;
                    case AboveNormal:
                        if (_index[3] == null)
                        {
                            _index[3] = new java.util.HashMap(25000, 0.7f);
                        }
                        _index[3].put(key, hint);
                        changedIndex = 3;
                        break;
                    case High:
                        if (_index[4] == null)
                        {
                            _index[4] = new java.util.HashMap(25000, 0.7f);
                        }
                        _index[4].put(key, hint);
                        changedIndex = 4;
                        break;
                }
                
                if(changedIndex > -1 && _index[changedIndex].size() > _evictionIndexMaxCounts[changedIndex])
                {
                    _evictionIndexMaxCounts[changedIndex] = _index[changedIndex].size();
                }
            }
        }
    }

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
                NCacheLog.Info("LocalCache.Evict()", "Cache Size: {0}" + (new Long(cache.getCount())).toString());
            }
            catch (CacheException cacheException)
            {
                context.getCacheLog().Info("PriorityEvictionPolicy LocalCache.Evict()", "Logging cache count throws Exception: " + cacheException.getMessage());
            }
        }

        //if user has updated the values in configuration file then new values will be reloaded.
        _sleepInterval = Integer.parseInt(ServicePropValues.CacheServer_EvictionBulkRemoveDelay);
        _removeThreshhold = Integer.parseInt(ServicePropValues.CacheServer_EvictionBulkRemoveSize);

        java.util.Date startTime = new java.util.Date();
        java.util.ArrayList selectedKeys = this.GetSelectedKeys(cache, (long) Math.ceil(evictSize * _ratio));
        java.util.Date endTime = new java.util.Date();

        if (NCacheLog.getIsInfoEnabled())
        {
            try
            {
                NCacheLog.Info("LocalCache.Evict()", String.format("Time Span for {0} Items: " + TimeSpan.Subtract(endTime, startTime), selectedKeys.size()));
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

        java.util.Iterator e = selectedKeys.iterator();

        int removedThreshhold = _removeThreshhold / 300;
        int remIteration = 0;
        while (e.hasNext())
        {
            Object key = e.next();
            if (key == null)
            {
                continue;
            }

            keysTobeRemoved.add(key);

            if (keysTobeRemoved.size() % 300 == 0)
            {
                try
                {
                    OperationContext priorityEvictionOperationContext = new OperationContext();
                    priorityEvictionOperationContext.Add(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                    
                    Object tempVar = cache.RemoveSync(keysTobeRemoved.toArray(new Object[0]), ItemRemoveReason.Underused, false, priorityEvictionOperationContext);
                    removedItems = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
                    context.PerfStatsColl.incrementEvictPerSecStatsBy(keysTobeRemoved.size());
                }
                catch (Exception ex)
                {
                    NCacheLog.Error("PriorityEvictionPolicy.Execute", "an error occured while removing items. Error " + ex.toString());
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
                        Thread.sleep(_sleepInterval * 1000);
                    }
                    catch (InterruptedException interruptedException)
                    {
                    }
                    remIteration = 0;
                }
            }
        }

        if (keysTobeRemoved.size() > 0)
        {
            try
            {
                OperationContext priorityEvictionOperationContext = new OperationContext();
                priorityEvictionOperationContext.Add(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                
                Object tempVar2 = cache.RemoveSync(keysTobeRemoved.toArray(new Object[0]), ItemRemoveReason.Underused, false,priorityEvictionOperationContext);
                removedItems = (java.util.ArrayList) ((tempVar2 instanceof java.util.ArrayList) ? tempVar2 : null);
                context.PerfStatsColl.incrementEvictPerSecStatsBy(keysTobeRemoved.size());
                if (removedItems != null && removedItems.size() > 0)
                {
                    dependentItems.addAll(removedItems);
                }
            }
            catch (Exception ex)
            {
                NCacheLog.Error("PriorityEvictionPolicy.Execute", "an error occured while removing items. Error " + ex.toString());
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
                            OperationContext priorityEvictionOperationContext = new OperationContext();
                            priorityEvictionOperationContext.Add(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                            
                            rootCache.CascadedRemove(removableList.toArray(new Object[0]), ItemRemoveReason.Underused, true, priorityEvictionOperationContext);
                            context.PerfStatsColl.incrementEvictPerSecStatsBy(removableList.size());
                        }
                        catch (Exception exc)
                        {
                            NCacheLog.Error("PriorityEvictionPolicy.Execute", "an error occured while removing dependent items. Error " + exc.toString());

                        }
                        removableList.clear();
                    }

                }
                if (removableList.size() > 0)
                {
                    try
                    {
                        OperationContext priorityEvictionOperationContext = new OperationContext();
                        priorityEvictionOperationContext.Add(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                        
                        rootCache.CascadedRemove(removableList.toArray(new Object[0]), ItemRemoveReason.Underused, true, priorityEvictionOperationContext);
                        context.PerfStatsColl.incrementEvictPerSecStatsBy(removableList.size());
                    }
                    catch (Exception exc)
                    {
                        NCacheLog.Error("PriorityEvictionPolicy.Execute", "an error occured while removing dependent items. Error " + exc.toString());

                    }
                    removableList.clear();
                }
            }
        }
        return;
    }

    /**
     *
     * @param cache
     * @param evictSize
     * @return
     */
    private java.util.ArrayList GetSelectedKeys(CacheBase cache, long evictSize)
    {
        java.util.ArrayList selectedKeys = new java.util.ArrayList(100);

        long sizeCount = 0;
        int prvsSize = 0;
        Object key = null;
        boolean selectionComplete = false;

        synchronized (_index)
        {
            for (int i = 0; i < 5; i++)
            {
                if (!selectionComplete)
                {
                    java.util.HashMap currentIndex = this._index[i];
                    if (currentIndex != null)
                    {
                        Iterator ide = currentIndex.entrySet().iterator();
                        while (ide.hasNext())
                        {
                            Map.Entry pair = (Map.Entry)ide.next();
                            key = pair.getKey();
                            if (key != null)
                            {
                                int itemSize = cache.GetItemSize(key);
                                if (sizeCount + itemSize >= evictSize && sizeCount > 0)
                                {
                                    if (evictSize - sizeCount > (itemSize + sizeCount) - evictSize)
                                    {
                                        selectedKeys.add(key);
                                    }

                                    selectionComplete = true;
                                    break;
                                }
                                else
                                {
                                    selectedKeys.add(key);
                                    sizeCount += itemSize;
                                    prvsSize = itemSize;
                                }
                            }
                        }
                    }
                }
                else
                {
                    //break the outer loop. we have already picked up
                    //the keys to be evicited.
                    break;
                }
            }
        }
        return selectedKeys;
    }

    public void Remove(Object key, EvictionHint hint)
    {
        synchronized (_index)
        {
            if (_index != null)
            {
                for (int i = 0; i < 5; i++)
                {
                    if (_index[i] != null)
                    {
                        if (_index[i].containsKey(key))
                        {
                            _index[i].remove(key);
                            if (_index[i].isEmpty())
                            {
                                _index[i] = null;
                                _evictionIndexMaxCounts[i] = 0;
                            }
                        }
                    }
                }
            }
        }
    }

    public void Clear()
    {
        synchronized (_index)
        {
            if (_index != null)
            {
                for (int i = 0; i < 5; i++)
                {
                    if (_index[i] != null)
                    {
                        _index[i] = new HashMap(25000, 0.7f);
                        _evictionIndexMaxCounts[i] = 0;
                    }
                }
            }
        }
    }

    @Override
    public long getIndexInMemorySize() {
        return getPriorityEvictionIndexSize();
    }
    
    private long getPriorityEvictionIndexSize()
    {
        int keysCount = 0;
        int evictionIndexMaxCounts = 0;
        long temp = 0;
        
        if(_index != null)
        {
            for(int i = 0; i < 5; i++)
            {
                if(_index[i] != null)
                {
                    keysCount += _index[i].size();
                }
                evictionIndexMaxCounts += _evictionIndexMaxCounts[i];
            }
        }
        temp += keysCount * PriorityEvictionHint.getInMemorySize();
        temp += evictionIndexMaxCounts * MemoryUtil.NetHashtableOverHead;
        return temp;
    }
}