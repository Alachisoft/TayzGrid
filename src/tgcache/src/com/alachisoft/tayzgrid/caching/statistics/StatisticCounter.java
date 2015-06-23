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

import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.caching.statistics.monitoring.Monitor;

public interface StatisticCounter {

    /**
     * Creates Instance name For out proc instanceName = CacheID For inProc
     * instanceNAme = CacheID +"-" + ProcessID + ":" +port
     *
     * @param instanceName
     * @param port
     * @param inProc
     * @return
     */
    String getInstanceName(String instanceName, int port, boolean inProc);

    /**
     * Returns true if the current user has the rights to read/write to
     * performance counters under the category of object cache.
     *
     * @return
     */
    String getInstanceName();

    void setInstanceName(String value);

    /**
     * Returns true if the current user has the rights to read/write to
     * performance counters under the category of object cache.
     *
     * @return
     */
    boolean getUserHasAccessRights();

    int getSNMPPort();
    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    void dispose();

    /**
     * Initializes the counter instances and category.
     *
     * @param inproc
     */
    void initializePerfCounters(boolean inproc);

    void initializePerfCounters(boolean inproc, boolean clusteredCache);

    void initializePerfCounters(boolean inproc, boolean clusteredCache, int port);
    
    /**
     * increment the performance counter for Cache item count by one.
     *
     */
    void incrementCountStats();

    void setCountStats(long count);

    /**
     * increment the performance counter for Cache size by one
     *
     * @param count
     */
    void incrementCountStatsBy(long count);

    void incrementCacheSizeStats();

    void incrementCacheSizeStatsBy(long size);

    void setCacheSizeStats(long size);

    void setCacheMaxSizeStats(long size);

    void setClientConnectedStats(int size);

    /**
     * increment the performance counter for Cache item count by one.
     *
     * @param count
     */
    void incrementCacheLastAccessCountStats(long count);

    /**
     * increment the performance counter for Cache hits per second.
     */
    void incrementHitsPerSecStats();

    /**
     * increment the performance counter for Cache misses per second.
     */
    void incrementMissPerSecStats();

    void incrementHitsRatioPerSecStats();

    void incrementHitsRatioPerSecBaseStats();

    /**
     * increment the performance counter for Cache get operations per second.
     */
    void incrementGetPerSecStats();

    /**
     * increment the performance counter for Cache get-group data operations per
     * second.
     */
    void incrementGetPerSecStatsBy(double value);

    /**
     * increment the performance counter for Cache add operations per second.
     */
    void incrementAddPerSecStats();
    
    void incrementAddPerSecStats(long value);

    /**
     * increment the performance counter for Cache update operations per second.
     */
    void incrementUpdPerSecStats();
    
    void incrementUpdPerSecStats(long value);
    /**
     * increment the performance counter for Cache remove operations per second.
     */
    void incrementDelPerSecStats();

    /**
     * Increment the performance counter by value for Cache remove operations
     * per second.
     */
    void incrementDelPerSecStatsBy(int value);

    /**
     * increment the performance counter for Cache evictions per second.
     */
    void incrementEvictPerSecStats();

    /**
     * increment the performance counter for Cache expirations per second.
     */
    void incrementExpiryPerSecStats();

    /**
     * increment the performance counter for read thru operation per sec
     */
    void incrementReadThruPerSec();

    /**
     * increment the performance counter for read thru operation per sec by
     * given amount
     *
     * @param value
     */
    void incrementReadThruPerSecBy(long value);

    /**
     * increment the performance counter for write thru operation per sec
     */
    void incrementWriteThruPerSec();

    /**
     * increment the performance counter for write thru operation per sec by
     * given amount
     *
     * @param value
     */
    void incrementWriteThruPerSecBy(long value);

    /**
     * increments the performance counter for Cache evictions per second by
     * given value.
     *
     * @param value
     */
    void incrementEvictPerSecStatsBy(long value);

    /**
     * increment the performance counter for Cache expirations per second by
     * given value.
     *
     * @param value
     */
    void incrementExpiryPerSecStatsBy(long value);

    /**
     * Timestamps the start of sampling interval for Cache avg. and max. per
     * mill-second time of fetch operations.
     */
    void mSecPerGetBeginSample();

    /**
     * Timestample and updates the counter for Cache avg. and max. per
     * mill-second time of fetch operations.
     */
    void mSecPerGetEndSample();

    /**
     * Timestamps the start of sampling interval for Cache avg. and max. per
     * mill-second time of add operations.
     */
    void mSecPerAddBeginSample();

    /**
     * Timestample and updates the counter for Cache avg. and max. per
     * mill-second time of add operations.
     */
    void mSecPerAddEndSample();

    /**
     * Timestamps the start of sampling interval for Cache avg. and max. per
     * mill-second time of update operations.
     */
    void mSecPerUpdBeginSample();

    /**
     * Timestample and updates the counter for Cache avg. and max. per
     * mill-second time of update operations.
     */
    void mSecPerUpdEndSample();

    /**
     * Timestamps the start of sampling interval for Cache avg. and max. per
     * mill-second time of remove operations.
     */
    void mSecPerDelBeginSample();

    /**
     * Timestample and updates the counter for Cache avg. and max. per
     * mill-second time of remove operations.
     */
    void mSecPerDelEndSample();

    /**
     * increment the performance counter for State Txfr's per second.
     */
    void incrementStateTxfrPerSecStats();

    /**
     * increment the performance counter for State Txfr's per second by given
     * value.
     *
     * @param value
     */
    void incrementStateTxfrPerSecStatsBy(long value);

    /**
     * increment the performance counter for Data Balance per second.
     */
    void incrementDataBalPerSecStats();

    /**
     * increment the performance counter for Data Balance per second by given
     * value.
     *
     * @param value
     */
    void incrementDataBalPerSecStatsBy(long value);

    /**
     * Count of items that are expired till now
     *
     * @return
     */
    float getExpirations();

    /**
     * Count of items that are evicted till now
     *
     * @return
     */
    float getEviction();

    /**
     * Count of items that are state transfered till now
     *
     * @return
     */
    float getStateXfer();

    /**
     * Count of items that are balanced till now
     *
     * @return
     */
    float getDataBalance();

    /**
     * increment the performance counter for Mirror Queue size by one.
     *
     * @param count
     */
    void incrementMirrorQueueSizeStats(long count);

    /**
     * increment the performance counter for SlidingIndex Queue size by one.
     *
     * @param count
     */
    void incrementSlidingIndexQueueSizeStats(long count);

    ILogger getCacheLog();

    void setCacheLog(ILogger value);

    Monitor getMonitor();
    //write behind counter

    void incrementWriteBehindPerSec();

    void incrementWriteBehindPerSecBy(long value);

    void setWBFailureRetryCounter(long value);

    void decrementWBFailureRetryCounter();

    void incrementWBEvictionRate();

    void incrementDSFailedOpsPerSec();

    void incrementDSFailedOpsPerSecBy(long value);

    void incrementDSUpdatePerSec();

    void incrementDSUpdatePerSecBy(long p);

    void mSecPerDSWriteBeginSample();

    void mSecPerDSWriteEndSample();

    void mSecPerDSWriteEndSample(long bulkCount);

    void mSecPerDSUpdBeginSample();

    void mSecPerDSUpdEndSample();

    void mSecPerDSUpdEndSample(long bulkCount);

    void setWBCurrentBatchOpsCounter(long value);

    void setWBQueueCounter(long value);

    
    //MapReduce Methods
    void setPendingTasksCount(long value);
    void incrementPendingTasks();
    void decrementPendingTasks();
    void decrementPendingTasksBy(long value);
    void incrementPendingTasksBy(long value);
    
    void setRunningTasksCount(long value);
    void incrementRunningTasks();
    void decrementRunningTasks();
    void decrementRunningTasksBy(long value);
    void incrementRunningTasksBy(long value);
    
    void setMappedPerSec(long value);
    void incrementMappedPerSecRate();
    
    void setCombinedPerSec(long value);
    void incrementCombinedPerSecRate();
    
    void setReducedPerSec(long value);
    void incrementReducedPerSecRate();
    

    void SetQueryIndexSize(long value);

    void IncrementQueryIndexSizeBy(long value);

    void IncrementQueryIndexSize();

    void SetEvictionIndexSize(long value);

    void IncrementEvictionIndexSizeBy(long value);

    void IncrementEvictionIndexSize();

    void SetExpirationIndexSize(long value);

    void IncrementExpirationIndexSizeBy(long value);

    void IncrementExpirationIndexSize();

    void SetQueryPerSec(long value);

    void IncrementQueryPerSecBy(long value);

    void IncrementQueryPerSec();

    void MsecPerQueryExecutionTimeBeginSample();

    void MsecPerQueryExecutionTimeEndSample();

    void IncrementAvgQuerySize(long value);

    void startedAsMirror(String value);

    void isProc(String value);

    void nodeStatus(double value);

    void nodeName(String value);
 
    void serverNodes(String value);

    void setRunningCacheServers(String value);
    
    void setLocalAddress(String value);
    
    void setPID(String value);
    
    void setCacheServers(String value);

    void setIsReadThrough(boolean value);
    void setIsWriteThrough(boolean value);

    
    void setIsStoreByValue(boolean value);
    
}
