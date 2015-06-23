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
package com.alachisoft.tayzgrid.caching.statistics;

import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.AverageCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.BooleanCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.FlipManager;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.NumberOfItemCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.PerformanceCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.RateOfCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.StringCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.SystemCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.SystemCpuUsage;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.SystemFreeMemory;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.SystemMemoryUsage;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.SystemNetworkUsage;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.VMCommittedlMemory;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.VMCpuUsage;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.VMMaxMemory;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.VMMemoryUsage;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.VMNetworkUsage;
import com.alachisoft.tayzgrid.common.caching.statistics.monitoring.CacheMonitor;
import com.alachisoft.tayzgrid.common.caching.statistics.monitoring.ClusteredCacheMonitor;
import com.alachisoft.tayzgrid.common.caching.statistics.monitoring.Monitor;
import com.alachisoft.tayzgrid.common.caching.statistics.operations.CacheOperations;
import com.alachisoft.tayzgrid.common.caching.statistics.operations.ServerOperations;
import com.alachisoft.tayzgrid.common.enums.Time;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.net.NetworkData;
import com.alachisoft.tayzgrid.common.stats.HPTime;
import com.alachisoft.tayzgrid.common.stats.UsageStats;
import com.google.common.util.concurrent.AtomicDouble;
import java.lang.management.ManagementFactory;
public class PerfStatsCollector implements StatisticCounter, IDisposable {

    private HPTime initTime;
    /**
     * Instance name.
     */
    private String instanceName;
    /**
     * Snmp Port number.
     */
    private int snmpPort;
    /**
     * whether extended cache monitoring is enabled or not.
     */
    private boolean isClustered;
    /**
     * performance counter for Cache item count.
     */
    private PerformanceCounter pcCount = null;
    private PerformanceCounter pcCacheSize = null;
    private PerformanceCounter pcCacheMaxSize = null;

    private PerformanceCounter pcClientConnected = null;

    /**
     * performance counter for Cache item count.
     */
    private PerformanceCounter pcCachelastAccessCount = null;
    /**
     * performance counter for Cache hits per second.
     */
    private PerformanceCounter pcHitsPerSec = null;
    /**
     * performance counter for Cache misses per second.
     */
    private PerformanceCounter pcMissPerSec = null;
    private AverageCounter pcHitsRatioSec = null;
//    private PerformanceCounter pcHitsRatioSecBase = null;
    /**
     * performance counter for Cache get operations per second.
     */
    private PerformanceCounter pcGetPerSec = null;
    /**
     * performance counter for Cache add operations per second.
     */
    private PerformanceCounter pcAddPerSec = null;
    /**
     * performance counter for Cache update operations per second.
     */
    private PerformanceCounter pcUpdPerSec = null;
    /**
     * performance counter for Cache remove operations per second.
     */
    private PerformanceCounter pcDelPerSec = null;
    /**
     * performance counter for Cache evictions per second.
     */
    private PerformanceCounter pcEvictPerSec = null;
    /**
     * performance counter for Cache expirations per second.
     */
    private PerformanceCounter pcExpiryPerSec = null;
    /**
     * performance counter for State Txfr's per second.
     */
    private PerformanceCounter pcStateTxfrPerSec = null;
    /**
     * performance counter for Data Balance per second for Partitioned and
     * Partioned Replica.
     */
    private PerformanceCounter pcDataBalPerSec = null;
    /**
     * performance counter for Mirror Queue size.
     */
    private PerformanceCounter pcMirrorQueueSize = null;
    private PerformanceCounter pcSlidingIndexQueueSize = null;
    /**
     * performance counter for Cache avg. per milli-second time of get
     * operations.
     */
    private PerformanceCounter pcMSecPerGetAvg = null;
    /**
     * performance counter for Cache avg. per milli-second time of add
     * operations.
     */
    private PerformanceCounter pcMSecPerAddAvg = null;
    /**
     * performance counter for Cache avg. per milli-second time of update
     * operations.
     */
    private PerformanceCounter pcMSecPerUpdAvg = null;
    /**
     * performance counter for Cache avg. per milli-second time of remove
     * operations.
     */
    private PerformanceCounter pcMSecPerDelAvg = null;

    /**
     * performance counter for read thru per sec
     */
    private PerformanceCounter pcReadThruPerSec = null;
    /**
     * performance counter for write thru per sec
     */
    private PerformanceCounter pcWriteThruPerSec = null;
    /**
     * usage statistics for Cache per milli-second time of get operations.
     */
    private UsageStats usMSecPerGet = null;
    /**
     * usage statistics for Cache per milli-second time of add operations.
     */
    private UsageStats usMSecPerAdd = null;
    /**
     * usage statistics for Cache per milli-second time of update operations.
     */
    private UsageStats usMSecPerUpd = null;
    /**
     * usage statistics for Cache per milli-second time of remove operations.
     */
    private UsageStats usMSecPerDel = null;

    /**
     * Performance counter for Size of query indices defined on cache.
     */
    private PerformanceCounter _pcQueryIndexSize = null;
    /**
     * Performance counter for Size of expiration indices defined on cache.
     */
    private PerformanceCounter _pcExpirationIndexSize = null;
    /**
     * Performance counter for Size of eviction indeces defined on cache.
     */
    private PerformanceCounter _pcEvictionIndexSize = null;
    /**
     * Performance counter for number of queries per sec on cache
     */
    private PerformanceCounter _pcQueryPerSec = null;
    /**
     * Performance Counter for Average time query take while executing
     */
    private PerformanceCounter _pcAvgQueryExecutionTime = null;
    private UsageStats _pcMsecPerQueryExecutionTime = null;
    /**
     * Performance counter for average Number of items returned by queries
     */
    private PerformanceCounter _pcAvgQuerySize = null;
    private UsageStats _pcMsecPerQuerySize = null;

    private PerformanceCounter pcWBQueueCount = null;
    private PerformanceCounter pcWriteBehindPerSec = null;
    private UsageStats usMsecPerDSWrite = null;
    private AverageCounter pcMsecPerDSWriteAvg = null;
    private PerformanceCounter pcWBFailureRertyCount = null;
    private PerformanceCounter pcWBEvicPerSec = null;
    private PerformanceCounter pcDSUpdatesPerSec = null;
    private AverageCounter pcMsecPerDSUpdateAvg = null;
    private UsageStats usMsecPerDSUp = null;
    private PerformanceCounter pcDSFailedOpsPerSec = null;
    private PerformanceCounter pcWBCurrenBatchOpCount = null;

    private StringCounter pcStartedAsMirror = null;
    private PerformanceCounter pcNodeStatus = null;
    private StringCounter pcIsInProc = null;
    private StringCounter pcNodeName = null;
    private StringCounter pcRunningCacheServers = null;
    private StringCounter pcPID = null;
    private StringCounter pcLocalAddress = null;
    private StringCounter pcCacheServers = null;

    private ILogger logger;
    private static Thread flipManager;
    private static int flipManagerRefCount;
    Monitor monitor;
    
    //Map Reduce
    private PerformanceCounter _mrPendingTasks = null;
    private PerformanceCounter _mrRunningTasks = null;
    private PerformanceCounter _mrMappedPerSec = null;
    private PerformanceCounter _mrCombinedPerSec = null;
    private PerformanceCounter _mrReducedPerSec = null;
    /**
     * Category name of counter performance data.
     *
     */
    //float to atomidouble
    private AtomicDouble expirations = new AtomicDouble(0);
    private AtomicDouble evictions = new AtomicDouble(0);
    private AtomicDouble stateXfer = new AtomicDouble(0);
    private AtomicDouble dataBalance = new AtomicDouble(0);
    
    //Usman:
    // JCache specific counters
    private PerformanceCounter cacheRemovals = null;
    private PerformanceCounter cachePuts = null;
    private PerformanceCounter cacheHits = null;
    private PerformanceCounter cacheMisses = null;
    private PerformanceCounter cacheEvictions = null;
    private PerformanceCounter readThrough = new BooleanCounter("Is Read Through", instanceName);
    private PerformanceCounter writeThrough = new BooleanCounter("Is Write Through", instanceName);
    private PerformanceCounter isStoreByValue = new BooleanCounter("Store By Value", instanceName);
    
    @Override
    public Monitor getMonitor() {
        return monitor;
    }

    /**
     * Constructor
     *
     * @param instanceName
     * @param inProc
     */
    public PerfStatsCollector(String instanceName, boolean inProc) {
        this.instanceName = GetInstanceName(instanceName, 0, inProc);
         pcNodeStatus = new NumberOfItemCounter("Node Status", instanceName); 
         pcLocalAddress = new StringCounter("LocalAddress", instanceName);
         pcCacheServers = new StringCounter("Cache Servers", instanceName);
         pcRunningCacheServers = new StringCounter("Running Cache Servers", instanceName);
    }

    /**
     * Constructor
     *
     * @param instanceName
     * @param port
     * @param inProc
     */
    public PerfStatsCollector(String instanceName, int port, boolean inProc) {
        this.instanceName = GetInstanceName(instanceName, port, inProc);
         pcNodeStatus = new NumberOfItemCounter("Node Status", instanceName); 
         pcLocalAddress = new StringCounter("LocalAddress", instanceName);
         pcCacheServers = new StringCounter("Cache Servers", instanceName);
         pcRunningCacheServers = new StringCounter("Running Cache Servers", instanceName);
    }

    /**
     * Creates Instance name For out proc instanceName = CacheID For inProc
     * instanceNAme = CacheID +"-" + ProcessID + ":" +port
     *
     * @param instanceName
     * @param port
     * @param inProc
     * @return
     */
    public final String GetInstanceName(String instanceName, int port, boolean inProc) {
        return instanceName;//!inProc ? instanceName : instanceName + " - " + ManagementFactory.getRuntimeMXBean().getName() + "-" + port;
    }

    /**
     * Returns true if the current user has the rights to read/write to
     * performance counters under the category of object cache.
     */
    @Override
    public final String getInstanceName() {
        return instanceName;
    }

    @Override
    public final void setInstanceName(String value) {
        instanceName = value;
    }

    /**
     * Returns true if the current user has the rights to read/write to
     * performance counters under the category of object cache.
     */
    @Override
    public final boolean getUserHasAccessRights() {
        return true;
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    @Override
    public final void dispose() {
        synchronized (this) {

            flipManagerRefCount--;
           if(flipManager!=null && flipManagerRefCount ==0){
                    if(flipManager.isAlive())
                    {
                        flipManager.interrupt();
                    }
                    flipManager = null;
                }
            if (pcCount != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.Count);
                }
                pcCount = null;
            }
            if (pcCachelastAccessCount != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.CacheLastAccessCount);
                }
                pcCachelastAccessCount = null;
            }
            if (pcHitsPerSec != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.HitsPerSec);
                }
                pcHitsPerSec = null;
            }
            if (pcMissPerSec != null) {
                {
                    monitor.unRegisterCounter(CacheOperations.MissPerSec);
                }
                pcMissPerSec = null;
            }
            if (pcHitsRatioSec != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.HitsRatioSec);
                }
                pcHitsRatioSec = null;
            }
            if (pcAddPerSec != null) {
                {
                    monitor.unRegisterCounter(CacheOperations.AddsPerSec);
                }
                pcAddPerSec = null;
            }
            if (pcGetPerSec != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.GetsPerSec);
                }
                pcGetPerSec = null;
            }
            if (pcUpdPerSec != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.InsertsPerSec);
                }
                pcUpdPerSec = null;
            }
            if (pcDelPerSec != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.DelsPerSec);
                }
                pcDelPerSec = null;
            }
            if (pcMSecPerAddAvg != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.MSecPerAddAverage);
                }
                pcMSecPerAddAvg = null;
            }
            if (pcMSecPerDelAvg != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.MSecPerDelAverage);
                }
                pcMSecPerDelAvg = null;
            }
            if (pcMSecPerGetAvg != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.MSecPerGetAverage);
                }
                pcMSecPerGetAvg = null;
            }
            if (pcMSecPerUpdAvg != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.MSecPerInsertAverage);
                }
                pcMSecPerUpdAvg = null;
            }
            if (pcEvictPerSec != null) {
                monitor.unRegisterCounter(CacheOperations.EvictPerSec);
                pcEvictPerSec = null;
            }
            if (pcExpiryPerSec != null) {
                monitor.unRegisterCounter(CacheOperations.ExpiryPerSec);
                pcExpiryPerSec = null;
            }
            if (pcStateTxfrPerSec != null) {
                monitor.unRegisterCounter(CacheOperations.StateTransferPerSec);
                pcStateTxfrPerSec = null;
            }
            if (pcDataBalPerSec != null) {
                monitor.unRegisterCounter(CacheOperations.DataBalPerSec);
                pcDataBalPerSec = null;
            }
            if (pcMirrorQueueSize != null) {
                monitor.unRegisterCounter(CacheOperations.MirrorQueueSize);
                pcMirrorQueueSize = null;
            }
            if (pcSlidingIndexQueueSize != null) {
                monitor.unRegisterCounter(CacheOperations.SlidingIndexQueueSize);
                pcSlidingIndexQueueSize = null;
            }
            if (this.pcReadThruPerSec != null) {
                monitor.unRegisterCounter(CacheOperations.ReadThruPerSec);
                this.pcReadThruPerSec = null;
            }
            if (this.pcWriteThruPerSec != null) {
                monitor.unRegisterCounter(CacheOperations.WriterThruPerSec);
                this.pcWriteThruPerSec = null;
            }
            if (this.pcWBQueueCount != null) {
                monitor.unRegisterCounter(CacheOperations.WBQueueCount);
                this.pcWBQueueCount = null;
            }
            if (this.pcWBCurrenBatchOpCount != null) {
                monitor.unRegisterCounter(CacheOperations.WBCurrenBatchOpCount);
                this.pcWBCurrenBatchOpCount = null;
            }

            if (this.pcCacheMaxSize != null) {
                monitor.unRegisterCounter(CacheOperations.CacheMaxSize);
                this.pcCacheMaxSize = null;
            }

            if (this.pcClientConnected != null) {
                monitor.unRegisterCounter(CacheOperations.ClientConnected);
                this.pcClientConnected = null;
            }
            if (this._pcQueryIndexSize != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.QueryIndexSize);
                    this._pcQueryIndexSize = null;
                }
            }
            if (this._pcExpirationIndexSize != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.ExpirationIndexSize);
                    this._pcExpirationIndexSize = null;
                }
            }
            if (this._pcEvictionIndexSize != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.EvictionIndexSize);
                    this._pcEvictionIndexSize = null;
                }
            }
            if (this._pcQueryPerSec != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.QuerysPerSec);
                    this._pcQueryPerSec = null;
                }
            }
            if (this._pcAvgQueryExecutionTime != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.AvgQueryExecutionTime);
                    this._pcAvgQueryExecutionTime = null;
                }
            }
            if (this._pcAvgQuerySize != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.AvgQuerySize);
                    this._pcAvgQuerySize = null;
                }
            }

            if (pcStartedAsMirror != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.StartedAsMirror);
                    this.pcStartedAsMirror = null;
                }
            }
            if (pcStartedAsMirror != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.StartedAsMirror);
                    this.pcStartedAsMirror = null;
                }
            }
            if (pcIsInProc != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.IsINProc);
                    this.pcIsInProc = null;
                }
            }
            if (pcNodeName != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.NodeName);
                    this.pcNodeName = null;
                }
            }
            if (pcNodeStatus != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.NodeStatus);
                    this.pcNodeStatus = null;
                }
            }

            if (pcRunningCacheServers != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.RunningCacheServers);
                    this.pcRunningCacheServers = null;
                }
            }
            
              if (pcLocalAddress != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.LocalAddress);
                    this.pcLocalAddress = null;
                }
            }
            
            if(pcPID != null){
                if(monitor != null){
                    monitor.unRegisterCounter(CacheOperations.Pid);
                    this.pcPID = null;
                }
            }
            
              if (pcCacheServers != null) {
                if (monitor != null) {
                    monitor.unRegisterCounter(CacheOperations.CacheServers);
                    this.pcCacheServers = null;
                }
            }


            
            if(this.cacheEvictions!=null){
                if(monitor!=null){
                    monitor.unRegisterCounter(CacheOperations.CacheEvictions);
                    this.cacheEvictions=null;
                }
            }
            
            if(this.cacheHits!=null){
                if(monitor!=null){
                    monitor.unRegisterCounter(CacheOperations.CacheHits);
                    this.cacheHits=null;
                }
            }
            
            if(this.cacheMisses!=null){
                if(monitor!=null){
                    monitor.unRegisterCounter(CacheOperations.CacheMisses);
                    this.cacheMisses=null;
                }
            }
            
            if(this.cachePuts!=null){
                if(monitor!=null){
                    monitor.unRegisterCounter(CacheOperations.CachePuts);
                    this.cachePuts=null;
                }
            }
            
            if(this.cacheRemovals!=null){
                if(monitor!=null){
                    monitor.unRegisterCounter(CacheOperations.CacheRemovals);
                    this.cacheRemovals=null;
                }
            }
            
            if(this.writeThrough!=null)
            {
                if(monitor!=null){
                    monitor.unRegisterCounter(CacheOperations.IsWriteThrough);
                    this.writeThrough=null;
                }
            }
            if(this.readThrough!=null)
            {
                if(monitor!=null){
                    monitor.unRegisterCounter(CacheOperations.IsReadThrough);
                    this.readThrough=null;
                }
            }
            


            if(this._mrPendingTasks != null)
            {
                if(monitor != null)
                {
                    monitor.unRegisterCounter(CacheOperations.MRPendingTasks);
                    this._mrPendingTasks = null;
                }
            }
            if(this._mrRunningTasks != null)
            {
                if(monitor != null)
                {
                    monitor.unRegisterCounter(CacheOperations.MRRunningTasks);
                    this._mrRunningTasks = null;
                }
            }
            if(this._mrMappedPerSec != null)
            {
                if(monitor != null)
                {
                    monitor.unRegisterCounter(CacheOperations.MRMappedPerSec);
                    this._mrMappedPerSec = null;
                }
            }
            if(this._mrCombinedPerSec != null)
            {
                if(monitor != null)
                {
                    monitor.unRegisterCounter(CacheOperations.MRCombinedPerSec);
                    this._mrCombinedPerSec = null;
                }
            }
            if(this._mrReducedPerSec != null)
            {
                if(monitor != null)
                {
                    monitor.unRegisterCounter(CacheOperations.MRReducedPerSec);
                    this._mrReducedPerSec = null;
                }
            }

            if (monitor != null) {
                try {
                    monitor.stopSNMP();
                } catch (Exception ex) {
                }
                monitor.unRegisterNode();
                monitor = null;
            }
        }
    }

    @Override
    public void initializePerfCounters(boolean inproc) {
        this.initializePerfCounters(inproc, false);
    }

    @Override
    public final void initializePerfCounters(boolean inProc, boolean clusteredCache) {
        this.initializePerfCounters(inProc, clusteredCache, 0);
    }
    /**
     * Initializes the counter instances and category.
     *
     * @param inProc
     * @param clusteredCache
     */
    @Override
    public final void initializePerfCounters(boolean inProc, boolean clusteredCache, int port) {
        monitor = clusteredCache ? new ClusteredCacheMonitor(instanceName, logger, port) : new CacheMonitor(instanceName, logger, port);
        monitor.registerNode();
        monitor.startSNMP();
        try {
            pcAddPerSec = new RateOfCounter("Additions/sec", instanceName);
            monitor.registerCounter(CacheOperations.AddsPerSec, pcAddPerSec);

            pcMSecPerAddAvg = new AverageCounter("Average µs/add", instanceName);
            monitor.registerCounter(CacheOperations.MSecPerAddAverage, pcMSecPerAddAvg);

            usMSecPerAdd = new UsageStats();

            pcUpdPerSec = new RateOfCounter("Updates/sec", instanceName);
            monitor.registerCounter(CacheOperations.InsertsPerSec, pcUpdPerSec);

            pcMSecPerUpdAvg = new AverageCounter("Average µs/insert", instanceName);
            monitor.registerCounter(CacheOperations.MSecPerInsertAverage, pcMSecPerUpdAvg);

            usMSecPerUpd = new UsageStats();

            pcGetPerSec = new RateOfCounter("Average get/sec", instanceName);
            monitor.registerCounter(CacheOperations.GetsPerSec, pcGetPerSec);

            pcMSecPerGetAvg = new AverageCounter("Avearge µs/fetch", instanceName);
            monitor.registerCounter(CacheOperations.MSecPerGetAverage, pcMSecPerGetAvg);

            usMSecPerGet = new UsageStats();

            pcDelPerSec = new RateOfCounter("Deletes/sec", instanceName);
            monitor.registerCounter(CacheOperations.DelsPerSec, pcDelPerSec);

            pcMSecPerDelAvg = new AverageCounter("Average µs/remove", instanceName);
            monitor.registerCounter(CacheOperations.MSecPerDelAverage, pcMSecPerDelAvg);

            usMSecPerDel = new UsageStats();

            pcHitsPerSec = new RateOfCounter("Hits/sec", instanceName);
            monitor.registerCounter(CacheOperations.HitsPerSec, pcHitsPerSec);

            pcHitsRatioSec = new AverageCounter("Hits ratio/sec (%)", instanceName);
            monitor.registerCounter(CacheOperations.HitsRatioSec, pcHitsRatioSec);

            pcMissPerSec = new RateOfCounter("Misses/sec", instanceName);
            monitor.registerCounter(CacheOperations.MissPerSec, pcMissPerSec);

            pcCount = new NumberOfItemCounter("Count", instanceName);
            monitor.registerCounter(CacheOperations.Count, pcCount);

            pcCacheSize = new NumberOfItemCounter("Cache Size", instanceName);
            monitor.registerCounter(CacheOperations.CacheSize, pcCacheSize);

            pcClientConnected = new NumberOfItemCounter("Client Connected", instanceName);
            monitor.registerCounter(CacheOperations.ClientConnected, pcClientConnected);

            pcCacheMaxSize = new NumberOfItemCounter("Cache Max Size", instanceName);
            monitor.registerCounter(CacheOperations.CacheMaxSize, pcCacheMaxSize);

            pcCachelastAccessCount = new NumberOfItemCounter("CacheLastAccessCount", instanceName);
            monitor.registerCounter(CacheOperations.CacheLastAccessCount, pcCount);

            pcReadThruPerSec = new RateOfCounter("Readthru/sec", instanceName);
            monitor.registerCounter(CacheOperations.ReadThruPerSec, pcReadThruPerSec);

            pcWriteThruPerSec = new RateOfCounter("Writethru/sec", instanceName);
            monitor.registerCounter(CacheOperations.WriterThruPerSec, pcWriteThruPerSec);

            pcExpiryPerSec = new RateOfCounter("Expirations/sec", instanceName);
            monitor.registerCounter(CacheOperations.ExpiryPerSec, pcExpiryPerSec);

            pcEvictPerSec = new RateOfCounter("Evictions/sec", instanceName);
            monitor.registerCounter(CacheOperations.EvictPerSec, pcEvictPerSec);
            if (clusteredCache) {

                pcStateTxfrPerSec = new RateOfCounter("State transfer/sec", instanceName);
                monitor.registerCounter(CacheOperations.StateTransferPerSec, pcStateTxfrPerSec);

                pcDataBalPerSec = new RateOfCounter("Data balance/sec", instanceName);
                monitor.registerCounter(CacheOperations.DataBalPerSec, pcDataBalPerSec);

                pcSlidingIndexQueueSize = new NumberOfItemCounter("Sliding Index queue size", instanceName);
                monitor.registerCounter(CacheOperations.SlidingIndexQueueSize, pcSlidingIndexQueueSize);

                pcMirrorQueueSize = new NumberOfItemCounter("Mirror Queue Size", instanceName);
                monitor.registerCounter(CacheOperations.MirrorQueueSize, pcMirrorQueueSize);
            }
            //Write behind counters
            pcWBQueueCount = new NumberOfItemCounter("Write-behind queue count", instanceName);
            monitor.registerCounter(CacheOperations.WBQueueCount, pcWBQueueCount);

            pcWriteBehindPerSec = new RateOfCounter("Write-behind/sec", instanceName);
            monitor.registerCounter(CacheOperations.WriteBehindPerSec, pcWriteBehindPerSec);

            pcMsecPerDSWriteAvg = new AverageCounter("Average µs/datasource write", instanceName);
            monitor.registerCounter(CacheOperations.MsecPerDSWriteAvg, pcMsecPerDSWriteAvg);

            pcWBFailureRertyCount = new NumberOfItemCounter("Write-behind failure retry count", instanceName);
            monitor.registerCounter(CacheOperations.WBFailureRertyCount, pcWBFailureRertyCount);

            pcWBEvicPerSec = new RateOfCounter("Write-behind evictions/sec", instanceName);
            monitor.registerCounter(CacheOperations.WBEvicPerSec, pcWBEvicPerSec);

            pcDSUpdatesPerSec = new RateOfCounter("Datasource updates/sec", instanceName);
            monitor.registerCounter(CacheOperations.DSUpdatesPerSec, pcDSUpdatesPerSec);

            pcMsecPerDSUpdateAvg = new AverageCounter("Average µs/datasource update", instanceName);
            monitor.registerCounter(CacheOperations.MsecPerDSUpdateAvg, pcMsecPerDSUpdateAvg);

            pcDSFailedOpsPerSec = new RateOfCounter("Datasource failed operations/sec", instanceName);
            monitor.registerCounter(CacheOperations.DSFailedOpsPerSec, pcDSFailedOpsPerSec);

            pcWBCurrenBatchOpCount = new NumberOfItemCounter("Current batch operations count", instanceName);
            monitor.registerCounter(CacheOperations.WBCurrenBatchOpCount, pcWBCurrenBatchOpCount);

            
            _mrPendingTasks = new NumberOfItemCounter("M/R pending tasks", instanceName);
            monitor.registerCounter(CacheOperations.MRPendingTasks, _mrPendingTasks);
            _mrRunningTasks = new NumberOfItemCounter("M/R running tasks", instanceName);
            monitor.registerCounter(CacheOperations.MRRunningTasks, _mrRunningTasks);
            
            _mrMappedPerSec = new RateOfCounter("M/R no. of records mapped/sec", instanceName);
            monitor.registerCounter(CacheOperations.MRMappedPerSec, _mrMappedPerSec);
            _mrCombinedPerSec = new RateOfCounter("M/R no. of records combined/sec", instanceName);
            monitor.registerCounter(CacheOperations.MRCombinedPerSec, _mrCombinedPerSec);
            _mrReducedPerSec = new RateOfCounter("M/R no. of records reduced/sec", instanceName);
            monitor.registerCounter(CacheOperations.MRReducedPerSec, _mrReducedPerSec);

            _pcQueryIndexSize = new NumberOfItemCounter("Query Index Size", instanceName);
            monitor.registerCounter(CacheOperations.QueryIndexSize, _pcQueryIndexSize);

            _pcEvictionIndexSize = new NumberOfItemCounter("Eviction Index Size", instanceName);
            monitor.registerCounter(CacheOperations.EvictionIndexSize, _pcEvictionIndexSize);

            _pcExpirationIndexSize = new NumberOfItemCounter("Expiration Index Size", instanceName);
            monitor.registerCounter(CacheOperations.ExpirationIndexSize, _pcExpirationIndexSize);

            _pcQueryPerSec = new RateOfCounter("Queries/sec", instanceName);
            monitor.registerCounter(CacheOperations.QuerysPerSec, _pcQueryPerSec);

            _pcAvgQueryExecutionTime = new AverageCounter("Average µs/Query Execution", instanceName);
            monitor.registerCounter(CacheOperations.AvgQueryExecutionTime, _pcAvgQueryExecutionTime);

            _pcMsecPerQueryExecutionTime = new UsageStats();

            _pcAvgQuerySize = new AverageCounter("Average Query Size", instanceName);
            monitor.registerCounter(CacheOperations.AvgQuerySize, _pcAvgQuerySize);

            pcStartedAsMirror = new StringCounter("Mirror Started", instanceName);
            monitor.registerCounter(CacheOperations.StartedAsMirror, pcStartedAsMirror);

            pcIsInProc = new StringCounter("InProc", instanceName);
            monitor.registerCounter(CacheOperations.IsINProc, pcIsInProc);
            

            pcNodeName = new StringCounter("Node Name", instanceName);
            monitor.registerCounter(CacheOperations.NodeName, pcNodeName);
            
           
            monitor.registerCounter(CacheOperations.NodeStatus, pcNodeStatus);
            
            //pcLocalAddress = new StringCounter("LocalAddress", instanceName);
            monitor.registerCounter(CacheOperations.LocalAddress, pcLocalAddress);

            //pcRunningCacheServers = new StringCounter("Running Cache Servers", instanceName);
            monitor.registerCounter(CacheOperations.RunningCacheServers, pcRunningCacheServers);
            
            pcPID = new StringCounter("PID", instanceName);
            monitor.registerCounter(CacheOperations.Pid, pcPID);
            
            //pcCacheServers = new StringCounter("Cache Servers", instanceName);
            monitor.registerCounter(CacheOperations.CacheServers, pcCacheServers);
            
            _pcMsecPerQuerySize = new UsageStats();

            usMsecPerDSWrite = new UsageStats();
            usMsecPerDSUp = new UsageStats();

            cacheEvictions = new NumberOfItemCounter("Total Cache Evications", instanceName);
            monitor.registerCounter(CacheOperations.CacheEvictions, cacheEvictions);
            
            cacheHits = new NumberOfItemCounter("Total Hits",instanceName);
            monitor.registerCounter(CacheOperations.CacheHits, cacheHits);
            
            cacheMisses = new NumberOfItemCounter("Total Misses", instanceName);
            monitor.registerCounter(CacheOperations.CacheMisses, cacheMisses);
            
            cachePuts = new NumberOfItemCounter("Total Puts", instanceName);
            monitor.registerCounter(CacheOperations.CachePuts, cachePuts);
            
            cacheRemovals = new NumberOfItemCounter("Total Removals", instanceName);
            monitor.registerCounter(CacheOperations.CacheRemovals, cacheRemovals);
            
            monitor.registerCounter(CacheOperations.IsReadThrough, readThrough);
            ((BooleanCounter)readThrough).setInstanceName(instanceName);
            
            monitor.registerCounter(CacheOperations.IsWriteThrough, writeThrough);
            ((BooleanCounter)writeThrough).setInstanceName(instanceName);
            
            monitor.registerCounter(CacheOperations.IsStoreByValue, isStoreByValue);
            ((BooleanCounter)isStoreByValue).setInstanceName(instanceName);
            
            _pcMsecPerQuerySize = new UsageStats();
            
            usMsecPerDSWrite=new UsageStats();
            usMsecPerDSUp=new UsageStats();  
            
            NetworkData.registerIPToMonitor(ServicePropValues.BIND_ToCLUSTER_IP);
            NetworkData.registerIPToMonitor(ServicePropValues.BIND_toClient_IP);

            
            if(inProc){
                if(flipManager == null){
                    flipManager = new Thread(new FlipManager());
                    flipManager.setDaemon(false);
                    flipManager.setName("FlipManager");
                    flipManager.start();
                }
                flipManagerRefCount++;
                
                
                RateOfCounter _pcRequestsPerSec = new RateOfCounter("Requests/sec", instanceName);
                monitor.registerServerCounter(ServerOperations.RequestsPerSec, _pcRequestsPerSec);

                SystemCounter _pcSystemCpuUsage = new SystemCpuUsage("Total Cpu Consumption", instanceName);
                monitor.registerServerCounter(ServerOperations.SystemCpuUsage, _pcSystemCpuUsage);

                SystemCounter _pcSystemFreeMemory = new SystemFreeMemory("Total Free Memory", instanceName);
                monitor.registerServerCounter(ServerOperations.SystemFreeMemory, _pcSystemFreeMemory);

                SystemCounter _pcSystemMemoryUsage = new SystemMemoryUsage("Total Memory Usage", instanceName);
                monitor.registerServerCounter(ServerOperations.SystemMemoryUsage, _pcSystemMemoryUsage);

                SystemCounter _pcSystemNetworkUsage = new SystemNetworkUsage("Totao NetworkUsage", instanceName);
                monitor.registerServerCounter(ServerOperations.SystemNetworkUsage, _pcSystemNetworkUsage);

                SystemCounter _pcVMCpuUsage = new VMCpuUsage("NCache Cpu Consumpiton", instanceName);
                monitor.registerServerCounter(ServerOperations.VMCpuUsage, _pcVMCpuUsage);

                SystemCounter _pcVMCommittedMemory = new VMCommittedlMemory("Committed Virtual Memory", instanceName);
                monitor.registerServerCounter(ServerOperations.VMCommittedMemory, _pcVMCommittedMemory);

                SystemCounter _pcVMMemoryUsage = new VMMemoryUsage("NCache Memory Usage", instanceName);
                monitor.registerServerCounter(ServerOperations.VMMemoryUsage, _pcVMMemoryUsage);

                SystemCounter _pcVMMaxMemory = new VMMaxMemory("Maximum NCache Memory Usage", instanceName);
                monitor.registerServerCounter(ServerOperations.VMMaxMemory, _pcVMMaxMemory);

                SystemCounter _pcVMNetworkUsage = new VMNetworkUsage("TayzGrid Network Usage", instanceName);
                monitor.registerServerCounter(ServerOperations.VMNetworkUsage, _pcVMNetworkUsage);
            }

        } catch (Exception exception) {
            getCacheLog().Error(PerfStatsCollector.class.getCanonicalName().toString(), exception.getMessage());
        }
    }

    public int getSNMPPort(){
        if(monitor != null){
            return monitor.getSnmpPort();
        }
        return 0;
    }
    /**
     * increment the performance counter for Cache item count by one.
     */
    @Override
    public final void incrementCountStats() {
        if (pcCount != null) {
            synchronized (pcCount) {
                pcCount.increment();
            }
        }
    }

    /**
     * increment the performance counter for Cache item count by one.
     */
    @Override
    public final void setCountStats(long count) {
        if (pcCount != null) {
            synchronized (pcCount) {
                pcCount.setValue(count);
            }
        }
    }

    /**
     * increment the performance counter for Cache item count by one.
     */
    @Override
    public final void incrementCountStatsBy(long count) {
        if (pcCount != null) {
            synchronized (pcCount) {
                pcCount.incrementBy(count);
            }
        }
    }

    /**
     * increment the performance counter for Cache size by one
     */
    @Override
    public final void incrementCacheSizeStats() {
        if (pcCacheSize != null) {
            synchronized (pcCacheSize) {
                pcCacheSize.increment();
            }
        }
    }

    /**
     * increment the performance counter for Cache size by given value
     */
    @Override
    public final void incrementCacheSizeStatsBy(long size) {
        if (pcCacheSize != null) {
            synchronized (pcCacheSize) {
                pcCacheSize.incrementBy(size);
            }
        }
    }

    /**
     * overwrites the values for cacheSize by given value
     *
     * @param size
     */
    @Override
    public final void setCacheSizeStats(long size) {
        if (pcCacheSize != null) {
            synchronized (pcCacheSize) {
                pcCacheSize.setValue(size);
            }
        }
    }

    /**
     * overwrites the values for cacheSize by given value
     *
     * @param size
     */
    @Override
    public final void setCacheMaxSizeStats(long size) {
        if (pcCacheMaxSize != null) {
            synchronized (pcCacheMaxSize) {
                pcCacheMaxSize.setValue(size);
            }
        }
    }

    /**
     * overwrites the values for cacheSize by given value
     *
     * @param clientConnected
     */
    @Override
    public final void setClientConnectedStats(int clientConnected) {
        if (pcClientConnected != null) {
            synchronized (pcClientConnected) {
                pcClientConnected.setValue(clientConnected);
            }
        }
    }

    /**
     * increment the performance counter for Cache item count by one.
     */
    @Override
    public final void incrementCacheLastAccessCountStats(long count) {
        if (pcCachelastAccessCount != null) {
            synchronized (pcCachelastAccessCount) {
                pcCachelastAccessCount.setValue(count);
            }
        }
    }

    /**
     * increment the performance counter for Cache hits per second.
     */
    @Override
    public final void incrementHitsPerSecStats() {
        if (pcHitsPerSec != null) {
            synchronized (pcHitsPerSec) {
                pcHitsPerSec.increment();
            }            
        }
        if(cacheHits!=null){
            synchronized(cacheHits)
            {
                cacheHits.increment();
            }
        }
    }

    /**
     * increment the performance counter for Cache misses per second.
     */
    @Override
    public final void incrementMissPerSecStats() {
        if (pcMissPerSec != null) {
            synchronized (pcMissPerSec) {
                pcMissPerSec.increment();
            }
        }
        if(cacheMisses!=null){
            synchronized(cacheMisses){
                cacheMisses.increment();
            }
        }
    }

    @Override
    public final void incrementHitsRatioPerSecStats() {
        if (pcHitsRatioSec != null) {
            synchronized (pcHitsRatioSec) {
                pcHitsRatioSec.incrementBy(1, 0);
            }
        }
    }

    @Override
    public final void incrementHitsRatioPerSecBaseStats() {
        if (pcHitsRatioSec != null) {
            synchronized (pcHitsRatioSec) {
                pcHitsRatioSec.incrementBy(0, 1);
            }
        }
    }

    /**
     * increment the performance counter for Cache get operations per second.
     */
    @Override
    public final void incrementGetPerSecStats() {
        if (pcGetPerSec != null) {
            synchronized (pcGetPerSec) {
                pcGetPerSec.increment();
            }
        }
    }

    @Override
    public final void incrementGetPerSecStatsBy(double value) {
        if (pcGetPerSec != null) {
            synchronized (pcGetPerSec) {
                pcGetPerSec.incrementBy(value);
            }
        }
    }

    /**
     * increment the performance counter for Cache add operations per second.
     */
    @Override
    public final void incrementAddPerSecStats() {
        if (pcAddPerSec != null) {
            synchronized (pcAddPerSec) {
                pcAddPerSec.increment();
            }
        }
        if(cachePuts!=null){
            synchronized(cachePuts){
                cachePuts.increment();
            }
        }
    }
    
    @Override
    public final void incrementAddPerSecStats(long value) {
        if (pcAddPerSec != null) {
            synchronized (pcAddPerSec) {
                double addCounter = pcAddPerSec.getValue();
                addCounter += value;
                pcAddPerSec.setValue(addCounter);
            }
        }
        if(cachePuts!=null){
            synchronized(cachePuts){
                cachePuts.incrementBy(value);
            }
        }
    }

    /**
     * increment the performance counter for Cache update operations per second.
     */
    @Override
    public final void incrementUpdPerSecStats() {
        if (pcUpdPerSec != null) {
            synchronized (pcUpdPerSec) {
                pcUpdPerSec.increment();
            }
        }
        if(cachePuts!=null){
            synchronized(cachePuts){
                cachePuts.increment();
            }
        }
    }
    
    @Override
    public final void incrementUpdPerSecStats(long value) {
        if (pcUpdPerSec != null) {
            synchronized (pcUpdPerSec) {
                double updateCounter = pcUpdPerSec.getValue();
                updateCounter += value;
                pcUpdPerSec.setValue(updateCounter);
            }
        }
        if(cachePuts!=null){
            synchronized(cachePuts){
                cachePuts.incrementBy(value);
            }
        }
    }
    
    /**
     * increment the performance counter for Cache remove operations per second.
     */
    @Override
    public final void incrementDelPerSecStats() {
        if (pcDelPerSec != null) {
            synchronized (pcDelPerSec) {
                pcDelPerSec.increment();
            }
        }
        if(cacheRemovals!=null){
            synchronized(cacheRemovals){
                cacheRemovals.increment();
            }
        }
    }

    /**
     * increment the performance counter for Cache evictions per second.
     */
    @Override
    public final void incrementEvictPerSecStats() {
        //float to atomic double
        this.evictions.addAndGet(1);
        if (pcEvictPerSec != null) {
            synchronized (pcEvictPerSec) {
                pcEvictPerSec.increment();
            }
        }
        if(cacheEvictions!=null)
        {
            synchronized(cacheEvictions){
                cacheEvictions.increment();
            }
        }
    }

    /**
     * increment the performance counter for Cache expirations per second.
     */
    @Override
    public final void incrementExpiryPerSecStats() {
        this.expirations.addAndGet(1);
        if (pcExpiryPerSec != null) {
            synchronized (pcExpiryPerSec) {
                pcExpiryPerSec.increment();
            }
        }
    }

    /**
     * increment the performance counter for read thru operation per sec
     */
    @Override
    public final void incrementReadThruPerSec() {
        this.incrementReadThruPerSecBy(1);
    }

    /**
     * increment the performance counter for read thru operation per sec by
     * given amount
     */
    @Override
    public final void incrementReadThruPerSecBy(long value) {
        if (this.pcReadThruPerSec != null) {
            synchronized (this.pcReadThruPerSec) {
                this.pcReadThruPerSec.incrementBy(value);
            }
        }
    }

    /**
     * increment the performance counter for write thru operation per sec
     */
    @Override
    public final void incrementWriteThruPerSec() {
        this.incrementWriteThruPerSecBy(1);
    }

    /**
     * increment the performance counter for write thru operation per sec by
     * given amount
     */
    @Override
    public final void incrementWriteThruPerSecBy(long value) {
        if (this.pcWriteThruPerSec != null) {
            synchronized (this.pcWriteThruPerSec) {
                this.pcWriteThruPerSec.incrementBy(value);
            }
        }
    }

    /**
     * increments the performance counter for Cache evictions per second by
     * given value.
     */
    @Override
    public final void incrementEvictPerSecStatsBy(long value) {
        this.evictions.addAndGet(value);
        if (pcEvictPerSec != null) {
            synchronized (pcEvictPerSec) {
                pcEvictPerSec.incrementBy(value);
            }
        }
        if(cacheEvictions!=null){
            synchronized(cacheEvictions){
                cacheEvictions.incrementBy(value);
            }
        }
    }

    /**
     * increment the performance counter for Cache expirations per second by
     * given value.
     */
    @Override
    public final void incrementExpiryPerSecStatsBy(long value) {
        this.expirations.addAndGet(value);
        if (pcExpiryPerSec != null) {
            synchronized (pcExpiryPerSec) {
                pcExpiryPerSec.incrementBy(value);
            }
        }
    }

    /**
     * Timestamps the startJMX of sampling interval for Cache avg. and max. per
     * mill-second time of fetch operations.
     */
    @Override
    public final void mSecPerGetBeginSample() {
        if (pcMSecPerGetAvg != null) {
            synchronized (usMSecPerGet) {
                usMSecPerGet.BeginSample();
            }
        }
    }

    /**
     * Timestample and updates the counter for Cache avg. and max. per
     * mill-second time of fetch operations.
     */
    @Override
    public final void mSecPerGetEndSample() {
        if (pcMSecPerGetAvg != null) {
            synchronized (pcMSecPerGetAvg) {
                usMSecPerGet.EndSample();
                pcMSecPerGetAvg.incrementBy(Time.toMicroSeconds(usMSecPerGet.getCurrent(), Time.nSEC)); // ts.Milliseconds);
            }
        }
    }

    /**
     * Timestamps the startJMX of sampling interval for Cache avg. and max. per
     * mill-second time of add operations.
     */
    @Override
    public final void mSecPerAddBeginSample() {
        if (pcMSecPerAddAvg != null) {
            synchronized (pcMSecPerAddAvg) {
                usMSecPerAdd.BeginSample();
            }
        }
    }

    /**
     * Timestample and updates the counter for Cache avg. and max. per
     * mill-second time of add operations.
     */
    @Override
    public final void mSecPerAddEndSample() {
        if (pcMSecPerAddAvg != null) {
            synchronized (pcMSecPerAddAvg) {
                usMSecPerAdd.EndSample();
                pcMSecPerAddAvg.incrementBy(Time.toMicroSeconds(usMSecPerAdd.getCurrent(), Time.nSEC)); //ts.Milliseconds);
            }
        }
    }

    /**
     * Timestamps the startJMX of sampling interval for Cache avg. and max. per
     * mill-second time of update operations.
     */
    @Override
    public final void mSecPerUpdBeginSample() {
        if (pcMSecPerUpdAvg != null) {
            synchronized (pcMSecPerUpdAvg) {
                usMSecPerUpd.BeginSample();
            }
        }
    }

    /**
     * Timestample and updates the counter for Cache avg. and max. per
     * mill-second time of update operations.
     */
    @Override
    public final void mSecPerUpdEndSample() {
        if (pcMSecPerUpdAvg != null) {
            synchronized (pcMSecPerUpdAvg) {
                usMSecPerUpd.EndSample();
                pcMSecPerUpdAvg.incrementBy(Time.toMicroSeconds(usMSecPerUpd.getCurrent(), Time.nSEC)); //ts.Milliseconds);
            }
        }
    }

    /**
     * Timestamps the startJMX of sampling interval for Cache avg. and max. per
     * mill-second time of remove operations.
     */
    @Override
    public final void mSecPerDelBeginSample() {
        if (pcMSecPerDelAvg != null) {
            synchronized (pcMSecPerDelAvg) {
                usMSecPerDel.BeginSample();
            }
        }
    }

    /**
     * Timestample and updates the counter for Cache avg. and max. per
     * mill-second time of remove operations.
     */
    @Override
    public final void mSecPerDelEndSample() {
        if (pcMSecPerDelAvg != null) {
            synchronized (pcMSecPerDelAvg) {
                usMSecPerDel.EndSample();
                pcMSecPerDelAvg.incrementBy(Time.toMicroSeconds(usMSecPerDel.getCurrent(), Time.nSEC)); //ts.Milliseconds);
            }
        }
    }

    /**
     * increment the performance counter for State Txfr's per second.
     */
    @Override
    public final void incrementStateTxfrPerSecStats() {
        this.stateXfer.addAndGet(1);
        if (pcStateTxfrPerSec != null) {
            synchronized (pcStateTxfrPerSec) {
                pcStateTxfrPerSec.increment();
            }
        }
    }

    /**
     * increment the performance counter for State Txfr's per second by given
     * value.
     */
    @Override
    public final void incrementStateTxfrPerSecStatsBy(long value) {
        this.stateXfer.addAndGet(value);
        if (pcStateTxfrPerSec != null) {
            synchronized (pcStateTxfrPerSec) {
                pcStateTxfrPerSec.incrementBy(value);
            }
        }
    }

    /**
     * increment the performance counter for Data Balance per second.
     */
    @Override
    public final void incrementDataBalPerSecStats() {
        this.dataBalance.addAndGet(1);
        if (pcDataBalPerSec != null) {
            synchronized (pcDataBalPerSec) {
                pcDataBalPerSec.increment();
            }
        }
    }

    /**
     * increment the performance counter for Data Balance per second by given
     * value.
     */
    @Override
    public final void incrementDataBalPerSecStatsBy(long value) {
        this.dataBalance.addAndGet(value);
        if (pcDataBalPerSec != null) {
            synchronized (pcDataBalPerSec) {
                pcDataBalPerSec.incrementBy(value);
            }
        }
    }

    /**
     * Count of items that are expired till now
     */
    @Override
    public final float getExpirations() {
        return (float) this.expirations.getAndSet(0);
    }

    /**
     * Count of items that are evicted till now
     */
    @Override
    public final float getEviction() {
        return (float) this.expirations.getAndSet(0);
    }

    /**
     * Count of items that are state transfered till now
     */
    @Override
    public final float getStateXfer() {
        return (float) this.expirations.getAndSet(0);
    }

    /**
     * Count of items that are balanced till now
     */
    @Override
    public final float getDataBalance() {
        return (float) this.expirations.getAndSet(0);
    }

    /**
     * increment the performance counter for Mirror Queue size by one.
     */
    @Override
    public final void incrementMirrorQueueSizeStats(long count) {
        if (pcMirrorQueueSize != null) {
            synchronized (pcMirrorQueueSize) {
                pcMirrorQueueSize.setValue(count);
            }
        }
    }

    /**
     * increment the performance counter for SlidingIndex Queue size by one.
     */
    @Override
    public final void incrementSlidingIndexQueueSizeStats(long count) {
        if (pcSlidingIndexQueueSize != null) {
            synchronized (pcSlidingIndexQueueSize) {
                pcSlidingIndexQueueSize.setValue(count);
            }
        }
    }

    @Override
    public final ILogger getCacheLog() {
        return logger;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setCacheLog(ILogger value) {
        logger = value;
    }

    @Override
    public String getInstanceName(String instanceName, int port, boolean inProc) {
        return !inProc ? instanceName : instanceName + " - " + ManagementFactory.getRuntimeMXBean().getName() + "-" + port;
    }

    @Override
    public void incrementDelPerSecStatsBy(int value) {
        if (pcDelPerSec != null) {
            synchronized (pcDelPerSec) {
                pcDelPerSec.incrementBy(value);
            }
        }
    }

    public void incrementDSFailedOpsPerSec() {
        incrementDSFailedOpsPerSecBy(1);
    }

    public void incrementDSFailedOpsPerSecBy(long value) {
        if (pcDSFailedOpsPerSec != null) {
            synchronized (pcDSFailedOpsPerSec) {
                pcDSFailedOpsPerSec.incrementBy(value);
            }
        }
    }

    public void incrementWBEvictionRate() {
        if (pcWBEvicPerSec != null) {
            synchronized (pcWBEvicPerSec) {
                pcWBEvicPerSec.increment();
            }
        }
    }

    public void incrementWriteBehindPerSec() {
        this.incrementWriteBehindPerSecBy(1);
    }

    public void incrementWriteBehindPerSecBy(long value) {
        if (this.pcWriteBehindPerSec != null) {
            synchronized (this.pcWriteBehindPerSec) {
                this.pcWriteBehindPerSec.incrementBy(value);
            }
        }
    }

    public void setWBQueueCounter(long value) {
        if (this.pcWBQueueCount != null) {
            synchronized (this.pcWBQueueCount) {
                this.pcWBQueueCount.setValue(value);
            }
        }
    }

    public void setWBFailureRetryCounter(long value) {
        if (this.pcWBFailureRertyCount != null) {
            synchronized (this.pcWBFailureRertyCount) {
                this.pcWBFailureRertyCount.setValue(value);
            }
        }
    }

    public void decrementWBFailureRetryCounter() {
        if (this.pcWBFailureRertyCount != null) {
            synchronized (this.pcWBFailureRertyCount) {
                this.pcWBFailureRertyCount.decrement();
            }
        }
    }

    public void setWBCurrentBatchOpsCounter(long value) {
        if (this.pcWBCurrenBatchOpCount != null) {
            synchronized (this.pcWBCurrenBatchOpCount) {
                this.pcWBCurrenBatchOpCount.setValue(value);
            }
        }
    }

    public void mSecPerDSWriteBeginSample() {
        if (pcMsecPerDSWriteAvg != null) {
            synchronized (pcMsecPerDSWriteAvg) {
                usMsecPerDSWrite.BeginSample();
            }
        }
    }

    public void mSecPerDSWriteEndSample() {
        mSecPerDSWriteEndSample(1);
    }

    public void mSecPerDSWriteEndSample(long bulkCount) {
        if (pcMsecPerDSWriteAvg != null) {
            synchronized (pcMsecPerDSWriteAvg) {
                usMsecPerDSWrite.EndSample();
                pcMsecPerDSWriteAvg.incrementBy(Time.toMicroSeconds(usMsecPerDSWrite.getCurrent(), Time.nSEC), bulkCount); //ts.Milliseconds);
            }
        }
    }

    public void mSecPerDSUpdBeginSample() {
        if (pcMsecPerDSUpdateAvg != null) {
            synchronized (pcMsecPerDSUpdateAvg) {
                usMsecPerDSUp.BeginSample();
            }
        }
    }

    public void mSecPerDSUpdEndSample() {
        mSecPerDSUpdEndSample(1);
    }

    public void mSecPerDSUpdEndSample(long bulkCount) {
        if (pcMsecPerDSUpdateAvg != null) {
            synchronized (pcMsecPerDSUpdateAvg) {
                usMsecPerDSUp.EndSample();
                pcMsecPerDSUpdateAvg.incrementBy(Time.toMicroSeconds(usMsecPerDSUp.getCurrent(), Time.nSEC), bulkCount); //ts.Milliseconds);
            }
        }
    }

    public void incrementDSUpdatePerSec() {
        if (pcDSUpdatesPerSec != null) {
            synchronized (pcDSUpdatesPerSec) {
                pcDSUpdatesPerSec.increment();
            }
        }
    }

    public void incrementDSUpdatePerSecBy(long value) {
        if (pcDSUpdatesPerSec != null) {
            synchronized (pcDSUpdatesPerSec) {
                pcDSUpdatesPerSec.incrementBy(value);
            }
        }
    }

    @Override
    public void SetQueryIndexSize(long value) {
        if (_pcQueryIndexSize != null) {
            synchronized (_pcQueryIndexSize) {
                _pcQueryIndexSize.setValue(value);
            }
        }
    }

    @Override
    public void IncrementQueryIndexSizeBy(long value) {
        if (_pcQueryIndexSize != null) {
            synchronized (_pcQueryIndexSize) {
                _pcQueryIndexSize.incrementBy(value);
            }
        }
    }

    @Override
    public void IncrementQueryIndexSize() {
        if (_pcQueryIndexSize != null) {
            synchronized (_pcQueryIndexSize) {
                IncrementQueryIndexSizeBy(1);
            }
        }
    }

    @Override
    public void SetEvictionIndexSize(long value) {
        if (_pcEvictionIndexSize != null) {
            synchronized (_pcEvictionIndexSize) {
                _pcEvictionIndexSize.setValue(value);
            }
        }
    }

    @Override
    public void IncrementEvictionIndexSizeBy(long value) {
        if (_pcEvictionIndexSize != null) {
            synchronized (_pcEvictionIndexSize) {
                _pcEvictionIndexSize.incrementBy(value);
            }
        }
    }

    @Override
    public void IncrementEvictionIndexSize() {
        if (_pcEvictionIndexSize != null) {
            synchronized (_pcEvictionIndexSize) {
                IncrementEvictionIndexSizeBy(1);
            }
        }
    }

    @Override
    public void SetExpirationIndexSize(long value) {
        if (_pcExpirationIndexSize != null) {
            synchronized (_pcExpirationIndexSize) {
                _pcExpirationIndexSize.setValue(value);
            }
        }
    }

    @Override
    public void IncrementExpirationIndexSizeBy(long value) {
        if (_pcExpirationIndexSize != null) {
            synchronized (_pcExpirationIndexSize) {
                _pcExpirationIndexSize.incrementBy(value);
            }
        }
    }

    @Override
    public void IncrementExpirationIndexSize() {
        if (_pcExpirationIndexSize != null) {
            synchronized (_pcExpirationIndexSize) {
                IncrementExpirationIndexSizeBy(1);
            }
        }
    }

    @Override
    public void SetQueryPerSec(long value) {
        if (_pcQueryPerSec != null) {
            synchronized (_pcQueryPerSec) {
                _pcQueryPerSec.setValue(value);
            }
        }
    }

    @Override
    public void IncrementQueryPerSecBy(long value) {
        if (_pcQueryPerSec != null) {
            synchronized (_pcQueryPerSec) {
                _pcQueryPerSec.incrementBy(value);
            }
        }
    }

    @Override
    public void IncrementQueryPerSec() {
        if (_pcQueryPerSec != null) {
            synchronized (_pcQueryPerSec) {
                IncrementQueryPerSecBy(1);
            }
        }
    }

    @Override
    public void MsecPerQueryExecutionTimeBeginSample() {
        if (_pcAvgQueryExecutionTime != null) {
            synchronized (_pcAvgQueryExecutionTime) {
                _pcMsecPerQueryExecutionTime.BeginSample();
            }
        }
    }

    @Override
    public void MsecPerQueryExecutionTimeEndSample() {
        if (_pcAvgQueryExecutionTime != null) {
            synchronized (_pcAvgQueryExecutionTime) {
                _pcMsecPerQueryExecutionTime.EndSample();

                _pcAvgQueryExecutionTime.incrementBy(_pcMsecPerQueryExecutionTime.getCurrent() * 1000000);
                _pcAvgQueryExecutionTime.increment();
            }
        }
    }

    @Override
    public void IncrementAvgQuerySize(long value) {
        if (_pcAvgQuerySize != null) {
            synchronized (_pcAvgQuerySize) {
                _pcAvgQuerySize.incrementBy(value);
                // pcAvgQuerySizeBase.IncrementBy(1);
            }
        }
    }

    @Override
    public void startedAsMirror(String value) {
        if (pcStartedAsMirror != null) {
            synchronized (pcStartedAsMirror) {
                pcStartedAsMirror.setValue(value);
			}
		}
	}
    @Override
    public void incrementPendingTasks() {
        if(_mrPendingTasks != null)
        {
            synchronized(_mrPendingTasks)
            {
                _mrPendingTasks.increment();
            }
        }
    }
    
    @Override
    public void decrementPendingTasks() {
        if(_mrPendingTasks != null)
        {
            synchronized(_mrPendingTasks)
            {
                _mrPendingTasks.decrement();
            }
        }
    }
    
    @Override
    public void decrementPendingTasksBy(long value)
    {
        if(_mrPendingTasks != null)
        {
            synchronized(_mrPendingTasks)
            {
                _mrPendingTasks.decrementBy(value);
            }
        }
    }

    @Override
    public void isProc(String value) {
        if (pcIsInProc != null) {
            synchronized (pcIsInProc) {
                pcIsInProc.setValue(value);
			}
        }
    }
    @Override	
    public void incrementPendingTasksBy(long value) {
        if(_mrPendingTasks != null)
        {
            synchronized(_mrPendingTasks)
            {
                _mrPendingTasks.incrementBy(value);
            }
        }
    }

    @Override
    public void nodeStatus(double value) {
        if (pcNodeStatus != null) {
            synchronized (pcNodeStatus) {
                pcNodeStatus.setValue(value);
            }
        }
    }
    
    @Override
    public void setPendingTasksCount(long value) {
        if(_mrPendingTasks != null) {
            synchronized(_mrPendingTasks) {
                _mrPendingTasks.setValue(value);
            }
        }
    }

    @Override
    public void setRunningTasksCount(long value) {
        if(_mrRunningTasks != null) {
            synchronized(_mrRunningTasks) {
                _mrRunningTasks.setValue(value);
            }
        }
    }
    
    @Override
    public void incrementRunningTasks() {
        if(_mrRunningTasks != null)
        {
            synchronized(_mrRunningTasks)
            {
                _mrRunningTasks.increment();
            }
        }
    }

    @Override
    public void nodeName(String value) {
        if (pcNodeName != null) {
            synchronized (pcNodeName) {
                pcNodeName.setValue(value);
            }
        }
    }
    
    @Override
    public void decrementRunningTasks()
    {
        if(_mrRunningTasks != null)
        {
            synchronized(_mrRunningTasks)
            {
                _mrRunningTasks.decrement();

            }
        }
    }
    
    @Override
    public void decrementRunningTasksBy(long value)
    {
        if(_mrRunningTasks != null)
        {
            synchronized(_mrRunningTasks)
            {
                _mrRunningTasks.decrementBy(value);

            }
        }
    }
    
    @Override
    public void incrementRunningTasksBy(long value) {
        if(_mrRunningTasks != null)
        {
            synchronized(_mrRunningTasks)
            {
                _mrRunningTasks.increment();

            }
        }
    }

    @Override
    public void setRunningCacheServers(String value) {
        if (pcRunningCacheServers != null) {
            synchronized (pcRunningCacheServers) {
                pcRunningCacheServers.setValue(value);
            }
        }
    }
    
      @Override
    public void setLocalAddress(String value) {
        if (pcLocalAddress != null) {
            synchronized (pcLocalAddress) {
                pcLocalAddress.setValue(value);
            }
        }
    }
    
    public void setPID(String value){
        if(pcPID != null){
            synchronized(pcPID){
                pcPID.setValue(value);
            }
        }
    }
    
    
     @Override
    public void setCacheServers(String value) {
        if (pcCacheServers != null) {
            synchronized (pcCacheServers) {
                pcCacheServers.setValue(value);
            }
        }
    }
    @Override
    public void incrementMappedPerSecRate() {
        if(_mrMappedPerSec != null)
        {
            synchronized(_mrMappedPerSec)
            {
                _mrMappedPerSec.increment();

            }
        }
    }



    public void setIsReadThrough(boolean itReallyIs)
    {
        ((BooleanCounter)readThrough).setBooleanValue(itReallyIs);
    }
    
    public void setIsWriteThrough(boolean itReallyIs)
    {
        ((BooleanCounter)writeThrough).setBooleanValue(itReallyIs);
    }


    @Override
    public void incrementCombinedPerSecRate() {
        if(_mrCombinedPerSec != null)
        {
            synchronized(_mrCombinedPerSec)
            {
                _mrCombinedPerSec.increment();
            }
        }
    }

    @Override
    public void incrementReducedPerSecRate() {
        if(_mrReducedPerSec != null)
        {
            synchronized(_mrReducedPerSec)
            {
                _mrReducedPerSec.increment();
            }
        }
    }

    @Override
    public void setMappedPerSec(long value) {
        if(_mrMappedPerSec != null) {
            synchronized(_mrMappedPerSec) {
                _mrMappedPerSec.setValue(value);
            }
        }
    }

    @Override
    public void setCombinedPerSec(long value) {
        if(_mrCombinedPerSec != null) {
            synchronized(_mrCombinedPerSec) {
                _mrCombinedPerSec.setValue(value);
            }
        }
    }

    @Override
    public void setReducedPerSec(long value) {
        if(_mrReducedPerSec != null) {
            synchronized(_mrReducedPerSec) {
                _mrReducedPerSec.setValue(value);
            }
        }
    }

    @Override
    public void setIsStoreByValue(boolean value) {
        ((BooleanCounter)writeThrough).setBooleanValue(value);
    }

    @Override
    public void serverNodes(String value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


}
