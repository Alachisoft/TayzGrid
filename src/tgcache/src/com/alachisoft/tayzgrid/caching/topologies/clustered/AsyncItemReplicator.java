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

package com.alachisoft.tayzgrid.caching.topologies.clustered;

import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.IGRShutDown;
import com.alachisoft.tayzgrid.caching.ShutDownStatus;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.datastructures.IOptimizedQueueOperation;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.threading.Latch;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.util.*;

public class AsyncItemReplicator implements Runnable, IGRShutDown {

 
    private CacheRuntimeContext _context = null;
    private TimeSpan _interval = new TimeSpan(0, 0, 2);
    private Thread runner = null;

    private OptimizedQueue _queue = new OptimizedQueue();
    private java.util.HashMap _updateIndexKeys = new java.util.HashMap(); 
    private long _uniqueKeyNumber;
    private int _updateIndexMoveThreshhold = 200;
    private int _moveCount;
    private boolean stopped = true;
    private int _bulkKeysToReplicate = 300;
    private Latch _shutdownStatusLatch = new Latch(ShutDownStatus.NONE);

    public AsyncItemReplicator(CacheRuntimeContext context, TimeSpan interval) {
        if (ServicePropValues.CacheServer_BulkItemsToReplicate != null) {
            _bulkKeysToReplicate = Integer.decode(ServicePropValues.CacheServer_BulkItemsToReplicate);
        }

        this._context = context;
        this._interval = interval;
    }

    /**
     * Creates a new Thread and Starts it.
     */
    public final void Start() {
        if (stopped) {
            stopped = false;
            runner = new Thread(this);
            runner.setDaemon(true);
            runner.setName("AsyncItemReplicationThread");
            runner.start();
        }
    }

    /**
     * An operation to update an index on the replica node is queued to be
     * replicate. These operations are send in bulk to the replica node.
     *
     * @param key
     */
    public final void AddUpdateIndexKey(Object key) {
        synchronized (_updateIndexKeys) {
            _updateIndexKeys.put(key, null);
            _context.PerfStatsColl.incrementSlidingIndexQueueSizeStats(_updateIndexKeys.size());
        }
    }

    public final void RemoveUpdateIndexKey(Object key) {
        synchronized (_updateIndexKeys) {
            _updateIndexKeys.remove(key);
            _context.PerfStatsColl.incrementSlidingIndexQueueSizeStats(_updateIndexKeys.size());
        }
    }

    public final void WindUpTask() {
        if (!stopped) {
            _context.getCacheLog().CriticalInfo("AsyncItemReplicator", "WindUp Task Started.");

            if (_queue != null) {
                _context.getCacheLog().CriticalInfo("AsyncItemReplicator", "Async Replicator Queue Count: " + _queue.getCount());
            }

            _interval = new TimeSpan(0, 0, 0);
            _shutdownStatusLatch.SetStatusBit(ShutDownStatus.SHUTDOWN_INPROGRESS, ShutDownStatus.NONE);
            _context.getCacheLog().CriticalInfo("AsyncItemReplicator", "WindUp Task Ended.");
        }
    }

    public final void WaitForShutDown(long interval) {
        if (!stopped) {
            _context.getCacheLog().CriticalInfo("AsyncItemReplicator", "Waiting for shutdown task completion.");

            if (_queue.getCount() > 0) {
                _shutdownStatusLatch.WaitForAny(ShutDownStatus.SHUTDOWN_COMPLETED, interval * 1000);
            }

            if (_queue != null && _queue.getCount() > 0) {
                _context.getCacheLog().CriticalInfo("AsyncItemReplicator", "Remaining Async Replicator operations: " + _queue.getCount());
            }

            _context.getCacheLog().CriticalInfo("AsyncItemReplicator", "Shutdown task completed.");
        }
    }

    /**
     * Add the key and entry in teh HashMap for Invalidation by preodic thread.
     *
     * @param key The key of the item to invalidate.
     * @param entry CacheEntry to Invalidate.
     */
    public final void EnqueueOperation(Object key, ReplicationOperation operation) {
        try {
            if (key == null) {
                tangible.RefObject<Long> tempRef__uniqueKeyNumber = new tangible.RefObject<Long>(_uniqueKeyNumber);
                key = UUID.randomUUID().toString() + tempRef__uniqueKeyNumber.argvalue++;
                _uniqueKeyNumber = tempRef__uniqueKeyNumber.argvalue;
            }

            _queue.Enqueue(key, operation);

            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("AsyncReplicator.Enque", "queue_size :" + _queue.getCount());
            }
            _context.PerfStatsColl.incrementMirrorQueueSizeStats(_queue.getCount());
        } catch (Exception e) {
            if (_context.getCacheLog().getIsErrorEnabled()) {
                _context.getCacheLog().Error("AsyncItemReplicator", String.format("Exception: %1$s", e.toString()));
            }
        }
    }

    /**
     * Clears the Queue of any keys for replication.
     */
    public final void Clear() {
        //keysQue.Clear();
        _queue.Clear();
        _context.PerfStatsColl.incrementMirrorQueueSizeStats(_queue.getCount());
    }

    /**
     * Clears the Queue of any keys for replication.
     */
    public final void EnqueueClear(ReplicationOperation operation) {
        //keysQue.Clear();
        _queue.Clear();
        this.EnqueueOperation("NcAcHe$Cl@Ea%R", operation);
    }

    private Object[] GetIndexOperations() {
        Object[] keys = null;

        synchronized (_updateIndexKeys) {
            _moveCount++;
            if (_updateIndexKeys.size() >= _updateIndexMoveThreshhold || _moveCount > 2) {
                if (_updateIndexKeys.size() > 0) {
                    keys = new Object[_updateIndexKeys.size()];
                    Iterator ide = _updateIndexKeys.entrySet().iterator();

                    int index = 0;
                    while (ide.hasNext()) {
                        keys[index] = ((Map.Entry) ide.next()).getKey();
                        index++;
                    }
                }
                _moveCount = 0;
                _updateIndexKeys.clear();
            }
        }
        return keys;
    }

    /**
     * replication thread function. note: While replicating operations, a dummy
     * '0' sequence id is passed. this sequence id is totally ignored by
     * asynchronous por, but we are keeping it to maintain the symmetry in API.
     */
    public final void run() {

        java.util.ArrayList opCodesToBeReplicated = new java.util.ArrayList(_bulkKeysToReplicate);
        java.util.ArrayList infoToBeReplicated = new java.util.ArrayList(_bulkKeysToReplicate);
        java.util.ArrayList compilationInfo = new java.util.ArrayList(_bulkKeysToReplicate);
        java.util.ArrayList userPayLoad = new java.util.ArrayList();
        try {
            while (!stopped || _queue.getCount() > 0) {
                java.util.Date startedAt = new java.util.Date();
                java.util.Date finishedAt = new java.util.Date();

                try {
                    for (int i = 0; _queue.getCount() > 0 && i < _bulkKeysToReplicate; i++) {
                        IOptimizedQueueOperation operation = null;
                        operation = _queue.Dequeue();

                        Map.Entry entry = (Map.Entry) operation.getData();
                        opCodesToBeReplicated.add(entry.getKey());
                        infoToBeReplicated.add(entry.getValue());

                        if (operation.getUserPayLoad() != null) {
                            for (int j = 0; j < operation.getUserPayLoad().length; j++) {
                                userPayLoad.add(operation.getUserPayLoad()[j]);
                            }
                        }

                        compilationInfo.add(operation.getPayLoadSize());
                    }
                    Object[] updateIndexKeys = GetIndexOperations();

                    if (!stopped) {
                        if (opCodesToBeReplicated.size() > 0 || updateIndexKeys != null) {
                            if (updateIndexKeys != null) {
                                opCodesToBeReplicated.add(new Byte(ClusterCacheBase.OpCodes.UpdateIndice.getValue()).intValue());
                                infoToBeReplicated.add(updateIndexKeys);
                            }

                            _context.getCacheImpl().ReplicateOperations(opCodesToBeReplicated.toArray(new Object[0]), infoToBeReplicated.toArray(new Object[0]), userPayLoad.toArray(new Object[0]), compilationInfo, _context.getCacheImpl().getOperationSequenceId(), _context.getCacheImpl().getCurrentViewId());
                        }
                    }
                    if (!stopped && _context.PerfStatsColl != null) {
                        _context.PerfStatsColl.incrementMirrorQueueSizeStats(_queue.getCount());
                    }
                } catch (Exception e) {
                    if (e.getMessage().toLowerCase().indexOf("operation timeout") >= 0 && !_shutdownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                        _context.getCacheLog().CriticalInfo("AsyncReplicator.Run", "Bulk operation timedout. Retrying the operation.");
                        try {
                            if (!stopped) {
                                _context.getCacheImpl().ReplicateOperations(opCodesToBeReplicated.toArray(new Object[0]), infoToBeReplicated.toArray(new Object[0]), userPayLoad.toArray(new Object[0]), compilationInfo, 0, 0);
                                _context.getCacheLog().CriticalInfo("AsyncReplicator.Run", "RETRY is successfull.");
                            }
                        } catch (Exception ex) {
                            if (_context.getCacheLog().getIsErrorEnabled()) {
                                _context.getCacheLog().Error("AsyncReplicator.RUN", "Error occured while retrying operation. " + ex.toString());
                            }
                        }
                    } else {
                        if (_context.getCacheLog().getIsErrorEnabled()) {
                            _context.getCacheLog().Error("AsyncReplicator.RUN", e.toString());
                        }
                    }
                } finally {
                    opCodesToBeReplicated.clear();
                    infoToBeReplicated.clear();
                    compilationInfo.clear();
                    userPayLoad.clear();
                    finishedAt = new java.util.Date();
                }

                if (_queue.getCount() > 0) {
                    continue;
                } else if (_queue.getCount() == 0 && _shutdownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                    _shutdownStatusLatch.SetStatusBit(ShutDownStatus.SHUTDOWN_COMPLETED, ShutDownStatus.SHUTDOWN_INPROGRESS);
                    return;
                }

                if (TimeSpan.Subtract(finishedAt, startedAt).getTotalTicks() < _interval.getTotalTicks()) {
                    Thread.sleep(_interval.getTotalMiliSeconds() - TimeSpan.Subtract(finishedAt, startedAt).getTotalMiliSeconds());
                } else {
                    Thread.sleep(_interval.getTotalMiliSeconds());
                }
            }
        } // Threads are not Aborted in Java
        catch (InterruptedException ti) {
        } catch (NullPointerException e) {
        } catch (Exception e) {
            if (!stopped) {
                _context.getCacheLog().Error("AsyncReplicator.RUN", "Async replicator stopped. " + e.toString());
            }
        }
    }

    /**
     * Stops and disposes the Repliaction thread. The thread can be started
     * using Start method.
     *
     * @param gracefulStop If true then operations pending in the queue are
     * performed on the passive node, otherwise stopped instantly
     */
    public final void Stop(boolean gracefulStop) throws InterruptedException {
        stopped = true;
        if (runner != null && runner.isAlive()) {
            if (gracefulStop) {
                runner.join();
            } else {
                try {
                    if (runner.isAlive()) {
                        _context.getCacheLog().Flush();
                        runner.stop();
                    }
                } catch (Exception e) {
                }

            }
            try {
                Clear();
            } catch (java.lang.Exception e2) {
            }
        }
    }

    /**
     * Returns the number of operations in the queue.
     */
    public final long getQueueCount() {
        return _queue.getCount();
    }

    /**
     * Terminates the replciation thread and Disposes the instance.
     */
    public final void dispose() {
        try {
            Stop(false);
            runner = null;
        } catch (InterruptedException interruptedException) {
        }
    }
}