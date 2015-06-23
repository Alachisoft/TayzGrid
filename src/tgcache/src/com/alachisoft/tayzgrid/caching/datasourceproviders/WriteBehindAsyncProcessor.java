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

package com.alachisoft.tayzgrid.caching.datasourceproviders;

import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.caching.ShutDownStatus;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.threading.Latch;
import com.alachisoft.tayzgrid.runtime.datasourceprovider.OperationResult;
import com.alachisoft.tayzgrid.runtime.datasourceprovider.WriteOperation;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Processor to perform asynchronous operation, which will only be executed when
 * preemted.
 */
public class WriteBehindAsyncProcessor implements Runnable {

    public enum TaskState {

        Waite,
        Execute,
        Remove;

        public int getValue() {
            return this.ordinal();
        }

        public static TaskState forValue(int value) {
            return values()[value];
        }
    }

    /**
     * Defines the state of operation in queue
     */
    public enum OperationState {

        New,
        Requeue;

        public int getValue() {
            return this.ordinal();
        }

        public static OperationState forValue(int value) {
            return values()[value];
        }
    }

    /**
     * Defines the write behind mode
     */
    public enum WriteBehindMode {

        Batch,
        NonBatch;

        public int getValue() {
            return this.ordinal();
        }

        public static WriteBehindMode forValue(int value) {
            return values()[value];
        }
    }

    public static class WaitQueue implements Cloneable {

        private java.util.ArrayList _queue;

        /**
         * Initializes new instance of WaitQueue
         */
        public WaitQueue() {
            this._queue = new java.util.ArrayList();
        }

        /**
         * Initializes new instance of WaitQueue
         *
         * @param capacity capacity
         */
        public WaitQueue(int capacity) {
            this._queue = new java.util.ArrayList(capacity);
        }

        /**
         * Queue a write behing task
         *
         * @param task write behind task
         */
        public final void Enqueue(DSWriteBehindOperation writeOperations) {
            synchronized (this._queue) {
                this._queue.add(writeOperations);
            }
        }

        /**
         * Dequeue a write behind task
         *
         * @return write behind taks
         */
        public final DSWriteBehindOperation Dequeue() {
            synchronized (this._queue) {
                if (this._queue.isEmpty()) {
                    return null;
                }

                DSWriteBehindOperation value = (DSWriteBehindOperation) ((this._queue.get(0) instanceof DSWriteBehindOperation) ? this._queue.get(0) : null);
                this._queue.remove(0);
                return value;
            }
        }

        /**
         * Get the write behind task at top of queue
         *
         * @return write behind task
         */
        public final DSWriteBehindOperation Peek() {
            synchronized (this._queue) {
                if (this._queue.isEmpty()) {
                    return null;
                }
                return (DSWriteBehindOperation) ((this._queue.get(0) instanceof DSWriteBehindOperation) ? this._queue.get(0) : null);
            }
        }

        public final DSWriteBehindOperation getItem(int index) {
            synchronized (this._queue) {
                if (index >= _queue.size() || index < 0) {
                    throw new IndexOutOfBoundsException();
                }
                return (DSWriteBehindOperation) ((_queue.get(index) instanceof DSWriteBehindOperation) ? _queue.get(index) : null);
            }
        }

        public final void setItem(int index, DSWriteBehindOperation value) {
        }

        public final void RemoveAt(int index) {
            synchronized (this._queue) {
                if (index >= _queue.size() || index < 0) {
                    throw new IndexOutOfBoundsException();
                }
                _queue.remove(index);
            }
        }

        /**
         * Removes write behind, by searching the task with same taskId in queue
         *
         * @param taskId taskId
         */
        public final void Remove(String taskId) {
            synchronized (this._queue) {
                for (int i = this._queue.size() - 1; i >= 0; i--) {
                    DSWriteBehindOperation qTask = (DSWriteBehindOperation) ((this._queue.get(i) instanceof DSWriteBehindOperation) ? this._queue.get(i) : null);
                    if (qTask.getTaskId().contains(taskId)) {
                        this._queue.remove(i);
                        break;
                    }
                }
            }
        }

        /**
         * Clears the write behind queue
         */
        public final void Clear() {
            synchronized (this._queue) {
                this._queue.clear();
            }
        }

        /**
         *
         */
        public final int getCount() {
            synchronized (this._queue) {
                return _queue.size();
            }
        }

        /**
         *
         * @return
         */
        public final java.util.Iterator GetEnumerator() {
            synchronized (this._queue) {
                return _queue.iterator();
            }
        }

        /**
         *
         * @return
         */
        public final Object clone() {
            synchronized (this._queue) {
                return _queue.clone();
            }
        }
    }

    public static class WriteBehindQueue implements Cloneable, java.io.Serializable {

        private java.util.HashMap _queue = new java.util.HashMap(1000);
        private java.util.HashMap<Object, Integer> _keyToIndexMap = new java.util.HashMap<Object, Integer>(1000);
        private java.util.HashMap<Integer, Object> _indexToKeyMap = new java.util.HashMap<Integer, Object>(1000);
        private java.util.HashMap<String, Integer> _taskIDMap = new java.util.HashMap<String, Integer>(1000);
        private WaitQueue _waitQueue = new WaitQueue();
        private int _tail = -1;
        private int _head = -1;
        private boolean _tailMaxReached = false;
        private Object _sync_mutex = new Object();
        private int _requeueLimit = 0;
        private int _evictionRatio = 0; //requeue operations evictions ratio
        private float _ratio = 0.25F;
        private java.util.ArrayList _requeuedOps = new java.util.ArrayList();
        private CacheRuntimeContext _context;
        private Latch _shutdownStatusLatch = new Latch(ShutDownStatus.NONE);

        public final WaitQueue getWaitQueue() {
            return _waitQueue;
        }

        public WriteBehindQueue(CacheRuntimeContext context) {
            _context = context;
            _waitQueue = new WaitQueue();
            _requeuedOps = new java.util.ArrayList();
        }

        public final void WindUpTask() {
            _context.getCacheLog().CriticalInfo("WriteBehindQueue", "WindUp Task Started.");
            if (_queue != null) {
                _context.getCacheLog().CriticalInfo("WriteBehindQueue", "Write Behind Queue Count: " + _queue.size());
            }
            _shutdownStatusLatch.SetStatusBit(ShutDownStatus.SHUTDOWN_INPROGRESS, ShutDownStatus.NONE);

            synchronized (_sync_mutex) {
                _sync_mutex.notifyAll();
            }

            _context.getCacheLog().CriticalInfo("WriteBehindQueue", "WindUp Task Ended.");
        }

        public final void WaitForShutDown(long interval) {
            _context.getCacheLog().CriticalInfo("WriteBehindQueue", "Waiting for shutdown task completion.");

            if (_queue.size() > 0) {
                _shutdownStatusLatch.WaitForAny(ShutDownStatus.SHUTDOWN_COMPLETED, interval * 1000);
            }

            if (_queue != null && _queue.size() > 0) {
                _context.getCacheLog().CriticalInfo("WriteBehindQueue", "Remaining write behind queue operations: " + _queue.size());
            }

            _context.getCacheLog().CriticalInfo("WriteBehindQueue", "Shutdown task completed.");
        }

        /**
         * Enqueue opeartion, adds the opeartion at the end of the queue and
         * removes any previous operations on that key.
         *
         * @param operation
         */
        public final void Enqueue(Object cacheKey, boolean merge, DSWriteBehindOperation operation) throws Exception {
            boolean isNewItem = true;
            boolean isRequeueDisable = (operation.getOperationState() == OperationState.Requeue && _requeueLimit == 0);//for hot apply
            try {
                synchronized (_sync_mutex) {
                    if (operation.getState() == TaskState.Waite)
                    {
                        _waitQueue.Enqueue(operation);
                        return;
                    }
                    if (_tail == Integer.MAX_VALUE) {
                        _tail = -1;
                        _tailMaxReached = true;
                    }
                    if (!merge) {
                        if (_requeueLimit > 0 && operation.getOperationState() == OperationState.Requeue) {
                            if (_requeuedOps.size() > _requeueLimit) {
                                EvictRequeuedOps();
                            }
                        }
                        if (_keyToIndexMap.containsKey(cacheKey)) {
                            if (isRequeueDisable) {
                                return;
                            }
                            int queueIndex = _keyToIndexMap.get(cacheKey);
                            DSWriteBehindOperation oldOperation = (DSWriteBehindOperation) ((_queue.get(queueIndex) instanceof DSWriteBehindOperation) ? _queue.get(queueIndex) : null);
                            if (!oldOperation.getOperationState().equals(operation.getOperationState())) {
                                // we will keep old operation in case incoming operation is requeued
                                //if existing is requeued and incoming operation is new,than we will keep new
                                if (operation.getOperationState() == OperationState.New) {
                                    operation.setEnqueueTime(new java.util.Date());
                                    _taskIDMap.remove(oldOperation.getTaskId());
                                    _queue.put(queueIndex, operation); //update operation
                                    _taskIDMap.put(operation.getTaskId(), queueIndex);
                                }
                            } else {
                                if (operation.getOperationState() == OperationState.Requeue) {
                                    operation.setEnqueueTime(new java.util.Date()); //reset operation delay
                                    _requeuedOps.add(operation);
                                } else {
                                    operation.setEnqueueTime(oldOperation.getEnqueueTime()); //maintaining previous operation delay
                                }
                                _taskIDMap.remove(oldOperation.getTaskId());
                                _queue.put(queueIndex, operation); //update operation
                                _taskIDMap.put(operation.getTaskId(), queueIndex);
                            }
                            isNewItem = false;
                        }
                    }
                    if (isNewItem && !isRequeueDisable) {
                        int index = ++_tail;
                        operation.setEnqueueTime(new java.util.Date()); //starting operation delay
                        _queue.put(index, operation);
                        _keyToIndexMap.put(cacheKey, index);
                        _indexToKeyMap.put(index, cacheKey);
                        _taskIDMap.put((String) operation.getTaskId(), index);
                        com.alachisoft.tayzgrid.common.threading.Monitor.pulse(_sync_mutex);
                        if (operation.getOperationState() == OperationState.Requeue) {
                            _requeuedOps.add(operation);
                        }
                    }
                    //write behind queue counter
                    _context.PerfStatsColl.setWBQueueCounter(this._queue.size());
                    _context.PerfStatsColl.setWBFailureRetryCounter(_requeuedOps.size());
                }
            } catch (Exception exp) {
                throw exp;
            } finally {
            }
        }

        public final DSWriteBehindOperation Dequeue(boolean batchOperations, java.util.Date selectionTime) throws Exception {
            DSWriteBehindOperation operation = null;
            try {
                synchronized (_sync_mutex) {
                    if (this._queue.size() < 1) {
                        if (_shutdownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                            _shutdownStatusLatch.SetStatusBit(ShutDownStatus.SHUTDOWN_COMPLETED, ShutDownStatus.SHUTDOWN_INPROGRESS);
                            return null;
                        }
                        if (batchOperations) {
                            return null;
                        }
                        com.alachisoft.tayzgrid.common.threading.Monitor.wait(_sync_mutex);
                        _reset = true;
                    }
                    int index = 0;
                    do {
                        if (_head < _tail || _tailMaxReached) {
                            if (_head == Integer.MAX_VALUE) {
                                _head = -1;
                                _tailMaxReached = false;
                            }

                            index = ++_head;
                            operation = (DSWriteBehindOperation) ((_queue.get(index) instanceof DSWriteBehindOperation) ? _queue.get(index) : null);

                            if (operation != null) {
                                if (batchOperations) {
                                    if (!_shutdownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                                        if (!operation.getOperationDelayExpired()) {
                                            --_head;
                                            return null;
                                        } else {
                                            if (operation.getEnqueueTime().compareTo(selectionTime) > 0) {
                                                return null;
                                            }
                                        }
                                    }
                                }
                                Object cacheKey = _indexToKeyMap.get(index);
                                _taskIDMap.remove(operation.getTaskId());
                                _keyToIndexMap.remove(cacheKey);
                                _indexToKeyMap.remove(index);
                                _queue.remove(index);
                                if (operation.getOperationState() == OperationState.Requeue) {
                                    _requeuedOps.remove(operation);
                                }
                            }
                        } else {
                            break;
                        }
                    } while (operation == null);
                    _context.PerfStatsColl.setWBQueueCounter(this._queue.size());
                    _context.PerfStatsColl.setWBFailureRetryCounter(_requeuedOps.size());
                }
            } catch (Exception exp) {
                throw exp;
            }
            return operation;
        }

        /**
         * Get the write behind task at top of queue
         *
         * @return write behind task
         */
        public final DSWriteBehindOperation Peek() {
            synchronized (this._queue) {
                if (this._queue.isEmpty()) {
                    return null;
                }
                return (DSWriteBehindOperation) ((this._queue.get(this._head + 1) instanceof DSWriteBehindOperation) ? this._queue.get(this._head + 1) : null);
            }
        }

        public final DSWriteBehindOperation getItem(int index) {
            synchronized (this._queue) {
                if (index >= _queue.size() || index < 0) {
                    throw new IndexOutOfBoundsException();
                }
                return (DSWriteBehindOperation) ((_queue.get(index) instanceof DSWriteBehindOperation) ? _queue.get(index) : null);
            }
        }

        public final void setItem(int index, DSWriteBehindOperation value) {
        }

        /**
         * Evict Write behind requeued operations.
         */
        public final void EvictRequeuedOps() {
            synchronized (_sync_mutex) {
                if (_requeuedOps.size() > 0) {
                    int opsCountTobeRemoved = (int) Math.ceil((float) _requeuedOps.size() * _ratio);
                    java.util.ArrayList removableIndexes = new java.util.ArrayList();
                    Arrays.sort(_requeuedOps.toArray());//ascending order
                    for (int i = opsCountTobeRemoved; i > 0; i--) {
                        DSWriteBehindOperation operation = (DSWriteBehindOperation) ((_requeuedOps.get(i) instanceof DSWriteBehindOperation) ? _requeuedOps.get(i) : null);
                        Object cacheKey = operation.getKey();
                        int index = _keyToIndexMap.get(cacheKey);
                        _keyToIndexMap.remove(cacheKey);
                        _indexToKeyMap.remove(index);
                        _taskIDMap.remove(operation.getTaskId());
                        _queue.remove(index);
                        removableIndexes.add(i);
                        _context.PerfStatsColl.incrementWBEvictionRate();
                    }
                    for (int i = removableIndexes.size(); i > 0; i--) {
                        _requeuedOps.remove(i);
                    }
                }
                _context.PerfStatsColl.setWBQueueCounter(this._queue.size());
                _context.PerfStatsColl.setWBFailureRetryCounter(_requeuedOps.size());
            }
        }

        /**
         * Updates write behind task state, by searching the task with same
         * taskId in queue
         *
         * @param taskId taskId
         * @param state new state
         */
        public final void UpdateState(String taskId, TaskState state) throws Exception {
            synchronized (this._sync_mutex) {
                boolean found = false;
                int index = -1;
                for (int i = this._waitQueue.getCount() - 1; i >= 0; i--) {
                    DSWriteBehindOperation operation = (DSWriteBehindOperation) ((this._waitQueue.getItem(i) instanceof DSWriteBehindOperation) ? this._waitQueue.getItem(i) : null);
                    if (operation.getTaskId().contains(taskId)) {
                        if (state == TaskState.Execute) {
                            operation.setState(state); //move to write behind queue only if state is execute
                            this.Enqueue(operation.getKey(), false, operation);
                        }
                        found = true;
                        index = i;
                        break;
                    }
                }
                if (index >= 0) {
                    _waitQueue.RemoveAt(index);
                }
                if (!found) //for remove operation in main queue
                {
                    if (_taskIDMap.containsKey(taskId)) {
                        int queueIndex = _taskIDMap.get(taskId);
                        Object cachekey = _indexToKeyMap.get(queueIndex);
                        DSWriteBehindOperation operation = (DSWriteBehindOperation) ((_queue.get(queueIndex) instanceof DSWriteBehindOperation) ? _queue.get(queueIndex) : null);
                        _queue.remove(queueIndex);
                        _keyToIndexMap.remove(cachekey);
                        _indexToKeyMap.remove(queueIndex);
                        _taskIDMap.remove(taskId);
                        if (operation.getOperationState() == OperationState.Requeue) {
                            _requeuedOps.remove(operation);
                        }
                    }
                }
                _context.PerfStatsColl.setWBQueueCounter(this._queue.size());
                _context.PerfStatsColl.setWBFailureRetryCounter(_requeuedOps.size());
            }
        }

        /**
         * Updates write behind task state, by searching the task with same
         * taskId in queue
         *
         * @param taskId taskId
         * @param state new state
         * @param newBulkTable table that contains keys and value that succeded
         * bulk operation
         */
        public final void UpdateState(String taskId, TaskState state, java.util.HashMap newBulkTable) throws Exception {
            synchronized (this._sync_mutex) {
                boolean removeOps = false;
                int count = this._waitQueue.getCount();
                java.util.ArrayList removableIndexes = new java.util.ArrayList();
                //for waite status
                for (int i = 0, j = 0; i < count; i++) {
                    DSWriteBehindOperation queueOp = (DSWriteBehindOperation) ((this._waitQueue.getItem(i) instanceof DSWriteBehindOperation) ? this._waitQueue.getItem(i) : null);
                    if (newBulkTable.containsKey(queueOp.getKey()) && queueOp.getTaskId().contains(taskId)) {
                        if (state == TaskState.Execute) {
                            queueOp.setState(state);
                            this.Enqueue(queueOp.getKey(), false, queueOp);
                        }
                        removableIndexes.add(i);
                        if (newBulkTable.size() == ++j) {
                            removeOps = true;
                            break;
                        }
                    }
                }
                for (int i = removableIndexes.size() - 1; i >= 0; i--) {
                    _waitQueue.RemoveAt((Integer) removableIndexes.get(i));
                }
                //for remove status
                if (state == TaskState.Remove && !removeOps) {
                    Iterator bulkTable = newBulkTable.entrySet().iterator();
                    Map.Entry pair;
                    while (bulkTable.hasNext()) {
                        pair = (Map.Entry) bulkTable.next();
                        Object key = (String) pair.getKey();
                        if (_keyToIndexMap.containsKey(key)) {
                            int queueIndex = _keyToIndexMap.get(key);
                            Object cachekey = _indexToKeyMap.get(queueIndex);
                            DSWriteBehindOperation operation = (DSWriteBehindOperation) ((_queue.get(queueIndex) instanceof DSWriteBehindOperation) ? _queue.get(queueIndex) : null);
                            _queue.remove(queueIndex);
                            _keyToIndexMap.remove(cachekey);
                            _indexToKeyMap.remove(queueIndex);
                            _taskIDMap.remove(operation.getTaskId());
                            if (operation.getOperationState() == OperationState.Requeue) {
                                _requeuedOps.remove(operation);
                            }
                        }
                    }
                }
                _context.PerfStatsColl.setWBQueueCounter(this._queue.size());
                _context.PerfStatsColl.setWBFailureRetryCounter(_requeuedOps.size());
            }
        }

        /**
         * Search write behind tasks states, by searching the task with the same
         * taskId in the table.
         *
         * @param states key value pair of taskIds and there states
         */
        public final void UpdateState(java.util.HashMap states) throws Exception {
            synchronized (this._sync_mutex) {
                boolean removeOps = false;
                //for waite status
                for (int i = 0, j = 0; i < this._waitQueue.getCount(); i++) {
                    DSWriteBehindOperation queueOp = (DSWriteBehindOperation) ((this._waitQueue.getItem(i) instanceof DSWriteBehindOperation) ? this._waitQueue.getItem(i) : null);
                    if (states.containsKey(queueOp.getTaskId())) {
                        TaskState state = (TaskState) states.get(queueOp.getTaskId());
                        if (state == TaskState.Execute) {
                            queueOp.setState(state);
                            this.Enqueue(queueOp.getKey(), false, queueOp);
                        }
                        _waitQueue.Remove(queueOp.getTaskId());
                        if (states.size() == ++j) {
                            removeOps = true;
                            break;
                        }
                    }
                }
                //for remove status
                if (!removeOps) {
                    Iterator taskStaes = states.entrySet().iterator();
                    Map.Entry pair;
                    while (taskStaes.hasNext()) {
                        pair = (Map.Entry) taskStaes.next();
                        String taskId = (String) pair.getKey();
                        if (_taskIDMap.containsKey(taskId)) {
                            int queueIndex = _taskIDMap.get(taskId);
                            Object cachekey = _indexToKeyMap.get(queueIndex);
                            DSWriteBehindOperation operation = (DSWriteBehindOperation) ((_queue.get(queueIndex) instanceof DSWriteBehindOperation) ? _queue.get(queueIndex) : null);
                            _queue.remove(queueIndex);
                            _keyToIndexMap.remove(cachekey);
                            _indexToKeyMap.remove(queueIndex);
                            if (operation.getOperationState() == OperationState.Requeue) {
                                _requeuedOps.remove(operation);
                            }
                        }
                    }
                }
                _context.PerfStatsColl.setWBQueueCounter(this._queue.size());
                _context.PerfStatsColl.setWBFailureRetryCounter(_requeuedOps.size());
            }
        }

        /**
         * Search for the write behind tasks initiated from source address, and
         * move these operations from wait queue to ready queue
         *
         * @param source address of source node
         */
        public final void UpdateState(String source) throws Exception {
            synchronized (this._sync_mutex) {
                int index = -1;
                for (int i = 0; i < this._waitQueue.getCount(); i++) {
                    DSWriteBehindOperation queueOp = (DSWriteBehindOperation) ((this._waitQueue.getItem(i) instanceof DSWriteBehindOperation) ? this._waitQueue.getItem(i) : null);
                    if (queueOp.getSource().equals(source)) {
                        queueOp.setState(TaskState.Execute);
                        this.Enqueue(queueOp.getKey(), false, queueOp);
                        index = i;
                        break;
                    }
                }
                if (index >= 0) {
                    _waitQueue.RemoveAt(index);
                }
            }
        }

        /**
         * Search for the write behind task , and move this operation from wait
         * queue to ready queue
         *
         * @param source address of source node
         */
        public final boolean SearchWaitQueue(String taskId) {
            synchronized (this._sync_mutex) {
                boolean found = false;
                int index = -1;
                for (int i = 0; i < this._waitQueue.getCount(); i++) {
                    DSWriteBehindOperation queueOp = (DSWriteBehindOperation) ((this._waitQueue.getItem(i) instanceof DSWriteBehindOperation) ? this._waitQueue.getItem(i) : null);
                    if (queueOp.getTaskId().contains(taskId)) {
                        found = true;
                        index = i;
                        break;
                    }
                }
                if (index >= 0) {
                    _waitQueue.RemoveAt(index);
                }
                return found;
            }
        }

        /**
         * Clears the write behind queue
         */
        public final void clear() {
            synchronized (this._sync_mutex) {
                if (_queue != null) {
                    this._queue.clear();
                }
                if (_requeuedOps != null) {
                    this._requeuedOps.clear();
                }
                if (_taskIDMap != null) {
                    this._taskIDMap.clear();
                }
                if (_taskIDMap != null) {
                    this._taskIDMap.clear();
                }
                if (_keyToIndexMap != null) {
                    this._keyToIndexMap.clear();
                }
            }

            if (_context != null) {
                _context.PerfStatsColl.setWBQueueCounter(this._queue.size());
                _context.PerfStatsColl.setWBFailureRetryCounter(_requeuedOps.size());
            }
        }

        /**
         * *
         *
         * @return
         */
        public final Object clone() {
            synchronized (this._sync_mutex) {
                WriteBehindQueue queue = new WriteBehindQueue(this._context);
                queue._queue = this._queue;
                queue._keyToIndexMap = this._keyToIndexMap;
                queue._indexToKeyMap = this._indexToKeyMap;
                queue._taskIDMap = this._taskIDMap;
                queue._waitQueue = this._waitQueue;
                queue._tail = this._tail;
                queue._head = this._head;
                queue._tailMaxReached = this._tailMaxReached;

                queue._requeueLimit = this._requeueLimit;
                queue._evictionRatio = this._evictionRatio; //requeue operations evictions ratio
                queue._ratio = this._ratio;
                queue._requeuedOps = this._requeuedOps;
                return queue;
            }
        }

        public final int getCount() {
            synchronized (this._queue) {
                return _queue.size();
            }
        }

        /**
         *
         * @return
         */
        public final java.util.Iterator GetEnumerator() {
            synchronized (this._queue) {
                return _queue.entrySet().iterator();
            }
        }

        public final void MergeQueue(WriteBehindQueue chunkOfQueue) throws Exception {
            Iterator iterator = chunkOfQueue.GetEnumerator();
            Map.Entry pair;
            while (iterator.hasNext()) {
                pair = (Map.Entry) iterator.next();
                if (pair.getValue() instanceof DSWriteBehindOperation) {
                    DSWriteBehindOperation operation = (DSWriteBehindOperation) pair.getValue();
                    Enqueue(operation.getKey(), true, operation);
                }
            }
        }

        public final void SetConfigDefaults(int requeueLimit, int requeueEvcRatio) {
            //only increased requeue limit is acceptable for hot apply
            if (requeueLimit >= this._requeueLimit) {
                this._requeueLimit = requeueLimit;
            }
            if (this._requeueLimit > 0) {
                this._evictionRatio = requeueEvcRatio;
            }
            _ratio = this._evictionRatio / 100f;
        }

        public final void ExecuteWaitQueue() throws Exception {
            synchronized (this._sync_mutex) {
                for (int i = 0; i < this._waitQueue.getCount(); i++) {
                    DSWriteBehindOperation queueOp = (DSWriteBehindOperation) ((this._waitQueue.getItem(i) instanceof DSWriteBehindOperation) ? this._waitQueue.getItem(i) : null);
                    queueOp.setState(TaskState.Execute);
                    this.Enqueue(queueOp.getKey(), false, queueOp);
                    _waitQueue.Remove(queueOp.getTaskId());
                }
            }
        }
    }
    /**
     * The worker thread.
     */
    private Thread _worker;
    private WriteBehindQueue _queue;
    /**
     * Operation time out
     */
    private int _timeout;
    /**
     */
    private Object _statusMutex, _processMutex;
    /**
     */
    private boolean _isDisposing;
    private static boolean _isNotify;
    private java.util.Date _startTime;
    private int _operationCount = 0;
    private int test = 0;
    private static boolean _reset = false;
    private ILogger _ncacheLog;
    private String _mode;
    private int _throttleRate = 0;
    private int _batchInterval = 0;
    private int _operationDelay = 0;
    private int _requeueLimit = 0;
    private int _requeueEvcRatio = 0;
    private boolean _isSliding = false;
    private DatasourceMgr _dsManager;
    private CacheBase _cacheImpl;
    private CacheRuntimeContext _context;
    private boolean _isShutDown = false; 
    private java.util.HashMap<String, WriteThruProviderMgr> _writerProivder = new java.util.HashMap<String, WriteThruProviderMgr>();

    private ILogger getCacheLog() {
        return _context.getCacheLog();
    }

    /**
     * Constructor
     */
    public WriteBehindAsyncProcessor(DatasourceMgr dsManager, int rate, String mode, long batchInterval, long operationDelay, int requeueLimit, int evictionRatio, long taskWaiteTimeout, HashMap<String, WriteThruProviderMgr> writerProvider, CacheBase cacheImpl, CacheRuntimeContext context) {
        this._dsManager = dsManager;
        this._context = context;
        this._worker = null;
        this._timeout = (int) taskWaiteTimeout;
        this._statusMutex = new Object();
        this._processMutex = new Object();
        this._isDisposing = false;
        this._writerProivder = writerProvider;
        this._mode = mode;
        SetConfigDefaults(mode, rate, batchInterval, operationDelay, requeueLimit, evictionRatio);
        this._cacheImpl = cacheImpl;
        this._queue = new WriteBehindQueue(_context);
        this._queue.SetConfigDefaults(_requeueLimit, _requeueEvcRatio);
    }

    public final void WindUpTask() throws Exception {
        _context.getCacheLog().CriticalInfo("WriteBehindAsyncProcessor", "WindUp Task Started.");

        _isShutDown = true;
        _batchInterval = 0;
        _operationDelay = 0;

        ExecuteWaitQueue();

        if (_queue != null) {

            _queue.WindUpTask();
        }

        _context.getCacheLog().CriticalInfo("WriteBehindAsyncProcessor", "WindUp Task Ended.");
    }

    public final void WaitForShutDown(long interval) {
        _context.getCacheLog().CriticalInfo("WriteBehindAsyncProcessor", "Waiting for  Write Behind queue shutdown task completion.");

        java.util.Date startShutDown = new java.util.Date();

        if (_queue != null) {
            _queue.WaitForShutDown(interval);
        }
        _context.getCacheLog().CriticalInfo("WriteBehindAsyncProcessor", "Shutdown task completed.");
    }

    public final void SetConfigDefaults(String mode, int rate, long batchInterval, long operationDelay, int requeueLimit, int requeueEvcRatio) {

        if (rate < 0) {
            this._throttleRate = 500;
        } else {
            this._throttleRate = rate;
        }

        if (requeueLimit < 0) {
            this._requeueLimit = 5000;
        } else {
            this._requeueLimit = (int) requeueLimit;
        }

        if (this._requeueLimit > 0) {
            if (requeueEvcRatio < 0) {
                this._requeueEvcRatio = 5;
            } else {
                this._requeueEvcRatio = (int) requeueEvcRatio;
            }
        }

        String tempVar = (String) mode;
        if (tempVar.equals("batch")) {
            if (batchInterval < 0) {
                this._batchInterval = (5 * 1000); //in sec
            } else {
                this._batchInterval = (int) (batchInterval * 1000); //in sec
            }
            if (operationDelay < 0) {
                this._operationDelay = 0;
            } else {
                this._operationDelay = (int) operationDelay;
            }
            for (java.util.Map.Entry<String, WriteThruProviderMgr> kv : _writerProivder.entrySet()) {
                kv.getValue().HotApplyConfig(_operationDelay);
            }
            this._mode = mode;
        }
        else if (tempVar.equals("non-batch")) {
            this._mode = "non-batch";
        }
        if (_queue != null) {
            _queue.SetConfigDefaults(this._requeueLimit, this._requeueEvcRatio);
        }
    }

    /**
     * Get a value indicating if the processor is running
     */
    public final boolean getIsRunning() {
        return _worker != null && _worker.isAlive();
    }

    public final CacheBase getCacheImpl() {
        return _cacheImpl;
    }

    public final void setCacheImpl(CacheBase value) {
        _cacheImpl = value;
    }

    /**
     * Start processing
     */
    public final void Start() {
        synchronized (this) {
            if (this._worker == null) {
                this._worker = new Thread(this);
                this._worker.setDaemon(true);
                this._worker.setName("WriteBehindAsyncProcessor");
                this._worker.start();
            }
        }
    }

    @Override
    public void run() {
        Run();
    }

    /**
     * Stop processing.
     */
    public final void Stop() {
        synchronized (this) {
            com.alachisoft.tayzgrid.common.threading.Monitor.pulse(this);
            synchronized (this._processMutex) {
                this._isDisposing = true;

                if (this._worker != null && this._worker.isAlive()) {
                    this._worker.stop();
                    this._worker = null;
                }
            }
        }
    }

    /**
     * Thread function, keeps running.
     */
    protected final void Run() {
        int remainingInetrval = _batchInterval;
        while (this._worker != null && !this._isDisposing) {
            try {
                if (_mode.toLowerCase().equals("batch")) {
                    if (!_isShutDown && remainingInetrval > 0) {
                        Thread.sleep(remainingInetrval); //for long value
                    }
                    java.util.Date start = new java.util.Date();
                    ProcessQueue(WriteBehindMode.Batch);
                    TimeSpan interval = new TimeSpan();
                    try {
                        interval = TimeSpan.Subtract(new java.util.Date(), start);
                    } catch (ArgumentException e) {
                    }
                    int processTime = (int) interval.getTotalMiliSeconds();
                    if (_isSliding && (_batchInterval - processTime > 0)) {
                        remainingInetrval = _batchInterval - processTime;
                    } else {
                        remainingInetrval = _batchInterval; //for hot apply
                    }

                } else {
                    ProcessQueue(WriteBehindMode.NonBatch);
                }
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
            }
        }
    }

    public final void StartExecutionOfTasksForSource(String source, boolean execute) {
        final Object[] args = new Object[]{
            source, execute
        };
        Thread workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ExecuteAllTaskForSource(args);
            }
        });
        workerThread.start();
    }

    private void ProcessQueue(WriteBehindMode mode) throws Exception {
        DSWriteBehindOperation operation = null;
        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
        switch (mode) {
            case NonBatch:
                operation = this._queue.Dequeue(false, new java.util.Date());
                _context.PerfStatsColl.setWBCurrentBatchOpsCounter(1);
                synchronized (_processMutex) {
                    if (!_isDisposing) {
                        ExecuteWriteOperation(operation, operationContext);
                    }
                    if (!_isShutDown) {
                        ThrottleOperations(_throttleRate, false);
                    }

                }
                break;
            case Batch:
                java.util.ArrayList selectedOperations = new java.util.ArrayList();
                java.util.Date selectionTime = new java.util.Date(); //we will select all expired operations uptill this limit
                while (this._worker != null && !this._isDisposing) {
                    try {
                        operation = this._queue.Dequeue(true, selectionTime);
                        if (operation != null) {
                            selectedOperations.add(operation);
                        }
                        if (operation == null || this._queue.getCount() == 0) {
                            break;
                        }

                    } catch (InterruptedException e) {
                        return;
                    } catch (Exception e) {
                    }
                }

                _context.PerfStatsColl.setWBCurrentBatchOpsCounter(selectedOperations.size());
                int rate = _throttleRate;
                //apply to data source
                if (selectedOperations.size() > 0) {
                    _startTime = new java.util.Date();
                    java.util.Iterator<java.util.Map.Entry<String, WriteThruProviderMgr>> providers = _writerProivder.entrySet().iterator();
                    Map.Entry pair;
                    while (providers.hasNext()) {
                        pair = (Map.Entry) providers.next();
                        String provider = (String) pair.getKey();
                        DSWriteBehindOperation[] operations = SortProviders(selectedOperations, provider);
                        if (operations != null && operations.length > 0) {
                            int index = 0;
                            boolean getNext = true;
                            while (getNext) {
                                tangible.RefObject<Boolean> tempRef_getNext = new tangible.RefObject<Boolean>(getNext);
                                DSWriteBehindOperation[] opsBatch = CreateBatch(operations, rate, index, tempRef_getNext);
                                getNext = tempRef_getNext.argvalue;
                                if (opsBatch != null && opsBatch.length > 0) {
                                    ExecuteWriteOperation(opsBatch, provider, operationContext);
                                    if (!_isShutDown) {
                                        ThrottleOperations(opsBatch.length, true);
                                    }
                                }
                                if (getNext) {
                                    index += rate;
                                }
                            }
                        }
                    }
                }
                break;
            default:
                return;
        }
    }

    private void ExecuteWriteOperation(DSWriteBehindOperation operation, OperationContext context) {
        OperationResult result = null;
        java.util.HashMap opResult = new java.util.HashMap(1);
        boolean notify = false;
        if (operation != null) {
            try {
                result = _dsManager.WriteThru(operation, context);
                if (result != null) {
                    if (result.getDSOperationStatus() == OperationResult.Status.FailureRetry) {
                        _cacheImpl.getContext().getCacheLog().Info("Retrying Write Operation: " + operation.getOperationCode() + " for key:" + operation.getKey());
                        operation.setOperationState(OperationState.Requeue);
                        operation.setOperationDelay(_operationDelay); //for hot apply
                        operation.incrementRetryCount();
                        Enqueue(operation);
                        return;
                    }
                    _cacheImpl.DoWrite("Executing WriteBehindTask", "taskId=" + operation.getTaskId() + "operation result status=" + result.getDSOperationStatus(), new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));

                    opResult.put(operation.getKey(), result.getDSOperationStatus());
                } else {
                    notify = true;
                    opResult.put(operation.getKey(), OperationResult.Status.Success);
                }
            } catch (Exception excep) {
                notify = true;
                if (_cacheImpl.getContext().getCacheLog().getIsErrorEnabled()) {
                    _cacheImpl.getContext().getCacheLog().Error("Executing WriteBehindTask", excep.getMessage());
                }
                opResult.put(operation.getKey(), excep);
            } finally {
                if ((notify) || (result != null && result.getDSOperationStatus() != OperationResult.Status.FailureRetry)) {
                    NotifyWriteBehindCompletion(operation, opResult);
                }
            }
        }
        //return result;
    }

    private void ExecuteWriteOperation(DSWriteBehindOperation[] operations, String provider, OperationContext context) throws OperationFailedException {
        java.util.HashMap opResult = new java.util.HashMap();
        java.util.HashMap returnSet = new java.util.HashMap();
        CallbackEntry cbEntry = null;
        Exception exc = null;
        java.util.ArrayList retryOps = new java.util.ArrayList();
        java.util.ArrayList taskList = new java.util.ArrayList();
        String[] taskIds = null; //taskids of operations to be dequeued from other nodes
        if (operations != null && operations.length > 0 && _dsManager != null) {
            try {
                OperationResult[] returnOps = _dsManager.WriteThru(operations, provider, returnSet, context);
                if (returnOps != null && returnOps.length > 0) {
                    for (int i = 0; i < operations.length; i++) //iterate on passed operation array coz we dont have complete info to generate ds operation here.
                    {
                        if (returnSet.containsKey(operations[i].getKey()) && !(returnSet.get(operations[i].getKey()) instanceof Exception)) //for retry operations
                        {
                            OperationResult.Status status = (OperationResult.Status) returnSet.get(operations[i].getKey());
                            if (status == OperationResult.Status.FailureRetry) {
                                retryOps.add(operations[i].getKey());
                                _cacheImpl.getContext().getCacheLog().Info("Retrying Write Behind " + operations[i].getOperationCode() + " operation for key:" + operations[i].getKey());
                                operations[i].setOperationState(OperationState.Requeue);
                                operations[i].setOperationDelay(_operationDelay); //for hot apply
                                operations[i].incrementRetryCount();
                                Enqueue(operations[i]);
                            }
                        }

                    }
                }
            } catch (Exception excep) {
                _cacheImpl.getContext().getCacheLog().Error("Excecuting Write Behind batch operations ", exc.getMessage());
                exc = excep;
            } finally {
                for (int i = 0; i < operations.length; i++) {
                    //populating operations with callbacks entries
                    if (operations[i] != null && operations[i].getEntry() != null && operations[i].getEntry().getValue() instanceof CallbackEntry) {
                        cbEntry = (CallbackEntry) ((operations[i].getEntry().getValue() instanceof CallbackEntry) ? operations[i].getEntry().getValue() : null);
                    }
                    if (cbEntry != null) {
                        if (exc != null) {
                            operations[i].setException(exc);
                            opResult.put(operations[i].getKey(), operations[i]);
                            continue;
                        }
                        if (returnSet.containsKey(operations[i].getKey())) {
                            if (returnSet.get(operations[i].getKey()) instanceof Exception) {
                                operations[i].setException((Exception) ((returnSet.get(operations[i].getKey()) instanceof Exception) ? returnSet.get(operations[i].getKey()) : null));
                                opResult.put(operations[i].getKey(), operations[i]);
                            } else {
                                OperationResult.Status status = (OperationResult.Status) returnSet.get(operations[i].getKey());
                                if (status != OperationResult.Status.FailureRetry) {
                                    operations[i].setDSOpState(status);
                                    opResult.put(operations[i].getKey(), operations[i]);
                                }
                            }
                        } else {
                            operations[i].setDSOpState(OperationResult.Status.Success);
                            opResult.put(operations[i].getKey(), operations[i]);
                        }
                    }
                    //populating operations with taskids other than retry
                    if (!retryOps.contains(operations[i].getKey())) {
                        taskList.add(operations[i].getTaskId());
                    }
                }
                try {
                    if (taskList.size() > 0) {
                        taskIds = new String[taskList.size()];
                        System.arraycopy(taskList.toArray(), 0, taskIds, 0, taskList.size());
                    }
                    _cacheImpl.NotifyWriteBehindTaskStatus(opResult, taskIds, provider, context);
                } catch (java.lang.Exception e) {
                }
            }
        }
    }

    private java.util.HashMap<Object, WriteOperation> CompileResult(OperationResult[] returnOps) {
        java.util.HashMap<Object, WriteOperation> result = new java.util.HashMap<Object, WriteOperation>();
        if (returnOps == null) {
            return result;
        }
        for (int i = 0; i < returnOps.length; i++) {
            if (returnOps[i] != null) {
                result.put(returnOps[i].getOperation().getKey(), returnOps[i].getOperation());
            }
        }
        return result;
    }

    private void NotifyWriteBehindCompletion(DSWriteBehindOperation operation, java.util.HashMap result) {
        CallbackEntry cbEntry = null;
        if (operation.getEntry() != null && operation.getEntry().getValue() instanceof CallbackEntry) {
            cbEntry = (CallbackEntry) ((operation.getEntry().getValue() instanceof CallbackEntry) ? operation.getEntry().getValue() : null);
        }
        try {
            _cacheImpl.NotifyWriteBehindTaskStatus(operation.getOperationCode(), result, cbEntry, operation.getTaskId(), operation.getProviderName(), new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
        } catch (java.lang.Exception e) {
        }
    }

    private DSWriteBehindOperation[] SortProviders(java.util.ArrayList operations, String provider) {
        ArrayList selectedOps = new ArrayList();
        for (int i = 0; i < operations.size(); i++) {
            DSWriteBehindOperation operation = operations.get(i) instanceof DSWriteBehindOperation ? (DSWriteBehindOperation) operations.get(i) : null;
            if (operation.getProviderName() == provider.toLowerCase()) {
                selectedOps.add(operation);
            }
        }
        if (selectedOps.size() > 0) {
            DSWriteBehindOperation[] dsOps = new DSWriteBehindOperation[selectedOps.size()];
            System.arraycopy(selectedOps.toArray(), 0, dsOps, 0, selectedOps.size());
            return dsOps;
        }
        return null;
    }

    private DSWriteBehindOperation[] CreateBatch(DSWriteBehindOperation[] operations, int batchCount, int index, tangible.RefObject<Boolean> getNext) {
        int operationCount = operations.length;
        DSWriteBehindOperation[] result = null;
        if ((operationCount - index) <= 0) {
            getNext.argvalue = false;
            return null;
        }
        if ((operationCount - index) <= batchCount) {
            result = new DSWriteBehindOperation[operationCount - index];
            System.arraycopy(operations, index, result, 0, operationCount - index);
            getNext.argvalue = false;
            return result;
        }
        result = new DSWriteBehindOperation[batchCount];
        System.arraycopy(operations, index, result, 0, batchCount);
        getNext.argvalue = true;
        return result;

    }

    /**
     * Thread function, keeps running.
     */
    protected final void ExecuteAllTaskForSource(Object args) {
        DSWriteBehindOperation operation = null;
        Object[] objs = (Object[]) ((args instanceof Object[]) ? args : null);
        String source = (String) ((objs[0] instanceof String) ? objs[0] : null);
        boolean execute = (Boolean) objs[1];

        java.util.ArrayList removableIndexes = new java.util.ArrayList();
        WaitQueue waitQueue = _queue.getWaitQueue();
        for (int i = 0; i < waitQueue.getCount(); i++) {
            try {
                operation = waitQueue.getItem(i);

                if (operation == null) {
                    continue;
                }
                if (!operation.getSource().equals(source)) {
                    continue;
                }
                removableIndexes.add(i);
                if (!execute) {
                    continue;
                }
                operation.setState(TaskState.Execute); //move this operation to ready queue
                this.Enqueue(operation);
            }
            catch (Exception e) {
            }
        }

        for (int i = removableIndexes.size() - 1; i >= 0; i--) {
            _queue.getWaitQueue().RemoveAt((Integer) removableIndexes.get(i));
        }
    }

    /**
     * Add task to the queue
     *
     * @param task task
     */
    public final void Enqueue(DSWriteBehindOperation operation) throws Exception {
        synchronized (this) {
            this._queue.Enqueue(operation.getKey(), false, operation);
            if (_startTime == null) {
                _startTime = new java.util.Date();
            }
            com.alachisoft.tayzgrid.common.threading.Monitor.pulse(this);
        }
    }

    /**
     * Dequeue write behind task from queue with matching taskId
     *
     * @param taskId taskId
     */
    public final void Dequeue(String[] taskId) throws Exception {
        synchronized (this) {
            if (taskId == null) {
                return;
            }
            for (int j = 0; j < taskId.length; j++) {
                //for operations in por replica
                if (this._queue.SearchWaitQueue(taskId[j])) {
                    return;
                }
                DSWriteBehindOperation operation = this._queue.Peek();
                if (operation != null && operation.getTaskId().contains(taskId[j])) {
                    this._queue.Dequeue(false, new java.util.Date());
                } else //ensure that no such task exists in queue, and all task before it are removed
                {
                    WriteBehindQueue tempQ = (WriteBehindQueue) this._queue.clone();
                    for (int i = 0; i < this._queue.getCount(); i++) {
                        operation = tempQ.Dequeue(false, new java.util.Date());
                        if (operation != null && operation.getTaskId().contains(taskId[j])) {
                            this._queue = tempQ;
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Sets the states of all the task which are added from this source
     *
     * @param address address of the node which left the cluster
     */
    public final void NodeLeft(String address) {
        synchronized (this) {
            try {
                this._queue.UpdateState(address);
            } catch (Exception e) {
                getCacheLog().Error("WriteBehindAsyncProcessor.NodeLeft", e.getMessage());
            }
        }
    }

    /**
     * Update the state of write behing task
     *
     * @param taskId
     * @param state
     */
    public final void SetState(String taskId, TaskState state) throws Exception {
        synchronized (this) {
            this._queue.UpdateState(taskId, state);
        }
    }

    /**
     * Update the state of write behing task
     *
     * @param taskId
     * @param state
     */
    public final void SetState(String taskId, TaskState state, java.util.HashMap newTable) throws Exception {
        synchronized (this) {
            this._queue.UpdateState(taskId, state, newTable);
        }
    }

    /**
     * Get a clone of current queue
     *
     * @return write behind queue
     */
    public final WriteBehindQueue CloneQueue() {
        synchronized (this) {
            return (WriteBehindQueue) this._queue.clone();
        }
    }

    public final void ExecuteWaitQueue() throws Exception {
        synchronized (this) {
            this._queue.ExecuteWaitQueue();
        }
    }

    /**
     * *
     *
     * @param context
     * @param queue
     */
    public final void MergeQueue(CacheRuntimeContext context, WriteBehindAsyncProcessor.WriteBehindQueue queue) throws Exception {
        synchronized (this) {
            if (queue != null) {
                this._queue.MergeQueue(queue);
            }
        }
    }

    /**
     * to maintain throttling rate
     *
     * @param num of operations per second
     *
     */
    private void ThrottleOperations(int operationExecuted, boolean isBatch) {
        TimeSpan interval = new TimeSpan();
        try {
            interval = TimeSpan.Subtract(new java.util.Date(), _startTime);
        } catch (ArgumentException e) {
        }
        int processTime = (int) interval.getTotalMiliSeconds();
        if (processTime > 1000 || _reset) //reset start time
        {
            _startTime = new java.util.Date();
            _operationCount = 0;
        }
        if (!isBatch) {
            _operationCount++;
        } else {
            _operationCount += operationExecuted;
        }
        //wait for remaining interval
        if (_operationCount > (operationExecuted - 1)) {
            if (processTime < 1000) {
                try {
                    Thread.sleep(1000 - processTime);
                } catch (Exception e) {
                }
                _startTime = new java.util.Date();
            }
            _reset = true;
            return;
        }
        _reset = false;
    }
}
