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
package com.alachisoft.tayzgrid.common.caching.statistics.monitoring;

import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.BooleanCounter;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.PerformanceCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.operations.CacheOperations;
import java.util.EnumMap;
import org.weakref.jmx.Managed;
import javax.cache.management.*;


public class CacheMonitor extends ServerMonitor implements CacheMonitorMBean, CacheStatisticsMXBean, CacheMXBean
{
    private EnumMap cacheCounterStore;

    public CacheMonitor(String nodeName) {
        super(nodeName);
        cacheCounterStore = new EnumMap<CacheOperations, PerformanceCounter>(CacheOperations.class);
    }

    public CacheMonitor(String nodeName, ILogger logger, int port) {
        super(nodeName, logger, port);
        cacheCounterStore = new EnumMap<CacheOperations, PerformanceCounter>(CacheOperations.class);
    }
    
    
    @Override
    public EnumMap getCounterStore() {
        return cacheCounterStore;
    }

    @Managed(description = "Number of items in the cache.", name = "Count")
    @Override
    public double getCount() {
        return getCounter(CacheOperations.Count);
    }

    @Managed(description = "maximun size of cache im MB occpuied by items in the cache ", name = "Cache Size")
    @Override
    public double getCacheSize() {
        return getCounter(CacheOperations.CacheSize);
    }

    @Managed(description = "maximun size of cache im MB ", name = "Cache Max Size")
    @Override
    public double getCacheMaxSize() {
        return getCounter(CacheOperations.CacheMaxSize);
    }

    @Managed(description = "maximun Client Connected to the cache ", name = "Client Connected")
    @Override
    public double getClientConnected() {
        return getCounter(CacheOperations.ClientConnected);
    }

    @Managed(description = "Number of items which are older then the access interval specified in the config.", name = "CacheLastAccessCount")
    @Override
    public double getCacheLastAccessCount() {
        return getCounter(CacheOperations.CacheLastAccessCount);
    }

    @Managed(description = "Number of Add operations per second.", name = "Additions/sec")
    @Override
    public double getAddsPerSec() {
        return getCounter(CacheOperations.AddsPerSec);
    }

    @Managed(description = "Number of Insert operations per second.", name = "Updates/sec")
    @Override
    public double getInsertsPerSec() {
        return getCounter(CacheOperations.InsertsPerSec);
    }

    @Managed(description = "Number of failed Get operations per second.", name = "Misses/sec")
    @Override
    public double getMissPerSec() {
        return getCounter(CacheOperations.MissPerSec);
    }

    @Managed(description = "Number of Get operations per second.", name = "Fetches/sec")
    @Override
    public double getGetsPerSec() {
        return getCounter(CacheOperations.GetsPerSec);
    }

    @Managed(description = "Number of successful Get operations per second.", name = "Hits/sec")
    @Override
    public double getHitsPerSec() {
        return getCounter(CacheOperations.HitsPerSec);
    }

    @Managed(description = "Number of Remove operations per second.", name = "Deletes/sec")
    @Override
    public double getDelsPerSec() {
        return getCounter(CacheOperations.DelsPerSec);
    }

    @Managed(description = "Average time, in microseconds, taken to complete one add operation.", name = "Average µs/add")
    @Override
    public double getmSecPerAdd() {
        return getCounter(CacheOperations.MSecPerAddAverage);
    }

    @Managed(description = "Average time, in microseconds, taken to complete one insert operation.", name = "Average µs/insert")
    @Override
    public double getmSecPerInsert() {
        return getCounter(CacheOperations.MSecPerInsertAverage);
    }

    @Managed(description = "Average time, in microseconds, taken to complete one fetch operation.", name = "Avearge µs/fetch")
    @Override
    public double getmSecPerGet() {
        return getCounter(CacheOperations.MSecPerGetAverage);
    }

    @Managed(description = "Average time, in microseconds, taken to complete one remove operation.", name = "Average µs/remove")
    @Override
    public double getmSecPerDel() {
        return getCounter(CacheOperations.MSecPerDelAverage);
    }

    @Managed(description = "Ratio of number of successful Get operations per second and total number of Get operations per second", name = "Hit ratio/sec (%)")
    @Override
    public double getHitsRatioSec() {
        return getCounter(CacheOperations.HitsRatioSec);
    }

    @Managed(description = "Number of items being expired currently per second", name = "Expirations/sec")
    @Override
    public double getExpiryPerSec() {
        return getCounter(CacheOperations.ExpiryPerSec);
    }

    @Managed(description = "Number of items evicted per second.", name = "Evictions/sec")
    @Override
    public double getEvictionPerSec() {
        return getCounter(CacheOperations.EvictPerSec);
    }

    @Managed(description = "Number of items this node is either reading from other nodes or sending to other nodes during a state transfer mode.", name = "State transfer/sec")
    @Override
    public double getStateTransferPerSec() {
        return getCounter(CacheOperations.StateTransferPerSec);
    }

    @Managed(description = "Number of items this node is either reading from other nodes or sending to other nodes during a Data Load Balancing mode.", name = "Data balance/sec")
    @Override
    public double getDataBalPerSec() {
        return getCounter(CacheOperations.DataBalPerSec);
    }

    @Managed(description = "Number of items in the Mirror queue.", name = "Mirror queue size")
    @Override
    public double getMirrorQueueSize() {
        return getCounter(CacheOperations.MirrorQueueSize);
    }

    @Managed(description = "Number of Readthru operations per second.", name = "Readthru/sec")
    @Override
    public double getReadThruPerSec() {
        return getCounter(CacheOperations.ReadThruPerSec);
    }

    @Managed(description = "Number of Writethru operations per second.", name = "Writethru/sec")
    @Override
    public double getWriteThruPerSec() {
        return getCounter(CacheOperations.WriterThruPerSec);
    }

    @Override
    public String getExportString() {
        return getExportStringPrefix() + "cache." + getNodeName();
    }

    //write behind counters
    @Managed(description = "Number of Writebehind operations in queue.", name = "Write-behind queue count")
    @Override
    public double getWBQueueCount() {
        return getCounter(CacheOperations.WBQueueCount);
    }

    @Managed(description = "Number of Writebehind operations per second.", name = "Write-behind/sec")
    @Override
    public double getWriteBehindPerSec() {
        return getCounter(CacheOperations.WriteBehindPerSec);
    }

    @Managed(description = "Average time, in microseconds, taken to complete one data source write operation.", name = "Average µs/datasource write")
    @Override
    public double getmSecPerDSWriteAvg() {
        return getCounter(CacheOperations.MsecPerDSWriteAvg);
    }

    @Managed(description = "Number of Writebehind requeued operations.", name = "Write-behind failure retry count")
    @Override
    public double getWBFailureRertyCount() {
        return getCounter(CacheOperations.WBFailureRertyCount);
    }

    @Managed(description = "Number of Writebehind requeued operations evicted per second.", name = "Write-behind evictions/sec")
    @Override
    public double getWBEvicPerSec() {
        return getCounter(CacheOperations.WBEvicPerSec);
    }

    @Managed(description = "Number of Datasource updates operations per second.", name = "Datasource updates/sec")
    @Override
    public double getDSUpdatesPerSec() {
        return getCounter(CacheOperations.DSUpdatesPerSec);
    }

    @Managed(description = "Average time, in microseconds, taken to complete one data source update operation.", name = "Average µs/datasource update")
    @Override
    public double getmSecPerDSUpdateAvg() {
        return getCounter(CacheOperations.MsecPerDSUpdateAvg);
    }

    @Managed(description = "Number of datasource failed operations per second.", name = "Datasource failed operations/sec")
    @Override
    public double getDSFailedOpsPerSec() {
        return getCounter(CacheOperations.DSFailedOpsPerSec);
    }

    
    @Managed(description="Number of writebehind operations in selected batch.", name="Current batch operations count")
    @Override
    public double getWBCurrenBatchOpCount() {
        return getCounter(CacheOperations.WBCurrenBatchOpCount);
    }


    @Managed(description = "Cache Started as Mirror", name = "Mirror Started")
    @Override
    public String getStartedAsMirror() {
        return getCounterString(CacheOperations.StartedAsMirror);
    }

    @Managed(description = "Cache Started in InProc Mode ", name = "INProc Cache")
    @Override
    public String getIsINProc() {
        return getCounterString(CacheOperations.IsINProc);
    }

    @Managed(description = "Cache Node IP", name = "Node Name")
    @Override
    public String getNodeIP() {
        return getCounterString(CacheOperations.NodeName);
    }

    @Managed(description = "Node Status", name = "Node Status")
    @Override
    public double getnodeStatus() {
        return getCounter(CacheOperations.NodeStatus);
    }

    @Managed(description = "get Running Cache Servers ", name = "Running Cache Servers")
    @Override
    public String getRunningCacheServers() {
        return getCounterString(CacheOperations.RunningCacheServers);
    }
    
    @Managed(description = "get Local Address ", name = "LocalAddress")
    @Override
    public String getLocalAddresss() {
        return getCounterString(CacheOperations.LocalAddress);
    }
	
    @Managed(description = "get process id", name = "PID")
    @Override
    public String getProcessId() {
        return getCounterString(CacheOperations.Pid);
    }
    
    @Managed(description = "Cache Servers", name = "Cache Servers")
    @Override
    public String getCacheServers() {
        return getCounterString(CacheOperations.CacheServers);
    }
    
    //JCache counters implementation
    @Override
    public void clear() {
        resetCounter(CacheOperations.CacheEvictions);
        resetCounter(CacheOperations.CacheHits);
        resetCounter(CacheOperations.CacheMisses);
        resetCounter(CacheOperations.CachePuts);
        resetCounter(CacheOperations.CacheRemovals);
    }
    
    public void resetCounter(CacheOperations operation)
    {
        PerformanceCounter counter  = getCounterObject(operation);
        counter.reset();
    }

    @Managed(description="Total number of hits on the node.", name="Total Hits")
    @Override
    public long getCacheHits() {
        return (long) getCounter(CacheOperations.CacheHits);
    }

    @Managed(description="Percentage hits on the node.", name="% Hits")
    @Override
    public float getCacheHitPercentage() {
        double hits = getCacheHits();
        if(hits==0)
            return 0;
        return (float) (hits/getCacheGets() *100.0f);
    }

    @Managed(description="Total number of misses on the node.", name="Total Misses")
    @Override
    public long getCacheMisses() {
        return (long)getCounter(CacheOperations.CacheMisses);
    }

    @Managed(description="Percentage hits on the node.", name="% Misses")
    @Override
    public float getCacheMissPercentage() {
        double misses = getCacheMisses();
        if(misses==0)
            return 0;
        return (float) (misses/getCacheGets() *100.0f);
    }

    @Managed(description="Total number of gets on the node.", name="Total Gets")
    @Override
    public long getCacheGets() {
        return getCacheHits()+getCacheMisses();
    }

    @Managed(description="Total number of pust on the node.", name="Total Puts")
    @Override
    public long getCachePuts() {
        return (long)getCounter(CacheOperations.CachePuts);
    }
    
    @Managed(description="Total number of removals on the node.", name="Total Removals")
    @Override
    public long getCacheRemovals() {
        return (long)getCounter(CacheOperations.CacheRemovals);
    }

    @Managed(description="Total evicted items on the node.", name="Total Evictions")
    @Override
    public long getCacheEvictions() {
        return (long)getCounter(CacheOperations.CacheEvictions);
    }

    @Managed(description="Average get operations performed per microsecond.", name="Average Get/µs")
    @Override
    public float getAverageGetTime() {
        return (float) getmSecPerGet();
    }

    @Managed(description="Average put operations performed per microsecond.", name="Average Put/µs")
    @Override
    public float getAveragePutTime() {
        return (float) getmSecPerAdd();
    }

    @Managed(description="Average remove operations performed per microsecond.", name="Average Remove/µs")
    @Override
    public float getAverageRemoveTime() {
        return (float) getmSecPerDel();
    }
    
    @Managed(description="The object type of keys inserted.", name="Key Type")
    @Override
    public String getKeyType() {
        return Object.class.toString();
    }

    @Managed(description="The object type of values inserted.", name="Value Type")
    @Override
    public String getValueType() {
        return Object.class.toString();
    }

    @Managed(description="Flag that shows weather ReadThrough is enabled.", name="ReadThrough Enabled")
    @Override
    public boolean isReadThrough() {
        return ((BooleanCounter)getCounterObject(CacheOperations.IsReadThrough)).getBooleanValue();
    }

    @Managed(description="Flag that shows weather WriteThrough is enabled.", name="WriteThrough Enabled")
    @Override
    public boolean isWriteThrough() {
        return ((BooleanCounter)getCounterObject(CacheOperations.IsWriteThrough)).getBooleanValue();
    }

    @Managed(description="Flag that shows the cache items are stored by value.", name="Store By Value")
    @Override
    public boolean isStoreByValue() {
        return ((BooleanCounter)getCounterObject(CacheOperations.IsStoreByValue)).getBooleanValue();
    }

    @Managed(description="Flag that shows weather cache statistics are enabled.", name="Statistics Enabled")
    @Override
    public boolean isStatisticsEnabled() {
        return true;
    }

    @Managed(description="Flag that shows weather cache management is enabled.", name="Management Enabled")
    @Override
    public boolean isManagementEnabled() {
        return true;
    }

    @Managed(description = "Total number of running tasks", name = "M/R running tasks")
    @Override
    public long getRunningMRTasks() {
        return (long) getCounter(CacheOperations.MRRunningTasks);
    }

    @Managed(description = "Total number of waiting tasks", name = "M/R pending tasks")
    @Override
    public long getPendingMRTasks() {
        return (long) getCounter(CacheOperations.MRPendingTasks);
    }

    @Managed(description = "Number of items Mapped per sec", name = "M/R no. of records mapped/sec")
    @Override
    public long getMappedPerSec() {
        return (long) getCounter(CacheOperations.MRMappedPerSec);
    }

    @Managed(description = "Number of items Mapped per sec", name = "M/R no. of records combined/sec")
    @Override
    public long getCombinedPerSec() {
        return (long) getCounter(CacheOperations.MRCombinedPerSec);
    }

    @Managed(description = "Number of items Mapped per sec", name = "M/R no. of records reduced/sec")
    @Override
    public long getReducedPerSec() {
        return (long) getCounter(CacheOperations.MRReducedPerSec);
    }
  
  
    

}
