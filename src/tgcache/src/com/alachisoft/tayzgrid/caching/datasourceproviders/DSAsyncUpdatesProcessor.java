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

import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.caching.ShutDownStatus;
import com.alachisoft.tayzgrid.common.threading.Latch;

import com.alachisoft.tayzgrid.runtime.datasourceprovider.WriteOperation;

/**
 * Manager class for Async updates after DS write operations
 */
public class DSAsyncUpdatesProcessor implements Runnable {

    public static class UpdateQueue implements Cloneable {

        private java.util.ArrayList _queue;

        /**
         * Initializes new instance of UpdateQueue
         */
        public UpdateQueue() {
            this._queue = new java.util.ArrayList();
        }

        /**
         * Initializes new instance of UpdateQueue
         *
         * @param capacity capacity
         */
        public UpdateQueue(int capacity) {
            this._queue = new java.util.ArrayList(capacity);
        }

        /**
         * Queue a write operation
         *
         * @param task write operation
         */
        public final void Enqueue(WriteOperation operationResult) {
            synchronized (this._queue) {
                this._queue.add(operationResult);
            }
        }

        /**
         * Dequeue a write operation
         *
         * @return write operation
         */
        public final WriteOperation Dequeue() {
            synchronized (this._queue) {
                if (this._queue.isEmpty()) {
                    return null;
                }
                WriteOperation value = (WriteOperation) ((this._queue.get(0) instanceof WriteOperation) ? this._queue.get(0) : null);
                this._queue.remove(0);
                return value;
            }
        }

        /**
         * Get the write operation at top of queue
         *
         * @return write operation
         */
        public final WriteOperation Peek() {
            synchronized (this._queue) {
                if (this._queue.isEmpty()) {
                    return null;
                }
                return (WriteOperation) ((this._queue.get(0) instanceof WriteOperation) ? this._queue.get(0) : null);
            }
        }

        public final WriteOperation getItem(int index) {
            synchronized (this._queue) {
                if (index >= _queue.size() || index < 0) {
                    throw new IndexOutOfBoundsException();
                }
                return (WriteOperation) ((_queue.get(index) instanceof WriteOperation) ? _queue.get(index) : null);
            }
        }

        public final void setItem(int index, WriteOperation value) {
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
         * *
         *
         * @return
         */
        public final java.util.Iterator GetEnumerator() {
            synchronized (this._queue) {
                return _queue.iterator();
            }
        }

        /**
         * *
         */
        public final int getCount() {
            synchronized (this._queue) {
                return _queue.size();
            }
        }

        /**
         * *
         *
         * @return
         */
        public final Object clone() {
            synchronized (this._queue) {
                return _queue.clone();
            }
        }
    }
    /**
     * The worker thread.
     */
    private Thread _worker;
    private DSAsyncUpdatesProcessor.UpdateQueue _updateQueue;
    private Object _processMutex;
    private boolean _isDisposing;
    private DatasourceMgr _dsMgr;
    private ILogger _ncacheLog;
    private Latch _shutdownStatusLatch = new Latch(ShutDownStatus.NONE);

    private ILogger getCacheLog() {
        return _ncacheLog;
    }

    /**
     * Constructor
     */
    public DSAsyncUpdatesProcessor(DatasourceMgr dsMgr, ILogger cacheLog) {
        this._ncacheLog = cacheLog;
        this._worker = null;
        this._processMutex = new Object();
        this._isDisposing = false;
        this._dsMgr = dsMgr;
        this._updateQueue = new DSAsyncUpdatesProcessor.UpdateQueue();
    }

    /**
     * Get a value indicating if the processor is running
     */
    public final boolean getIsRunning() {
        return _worker != null && _worker.isAlive();
    }

    /**
     * Start processing
     */
    public final void Start() {
        synchronized (this) {
            if (this._worker == null) {
                this._worker = new Thread(this);
                this._worker.setDaemon(true);
                this._worker.setName("DataSourceAsyncUpdatesProcessor");
                this._worker.start();
            }
        }
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

    @Override
    public void run() {
        Run();
    }

    /**
     * Thread function, keeps running.
     */
    protected final void Run() {
        while (this._worker != null && !this._isDisposing) {
            WriteOperation operation = null;
            try {
                synchronized (this) {
                    if (this._updateQueue.getCount() < 1) {
                        if (_shutdownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                            _shutdownStatusLatch.SetStatusBit(ShutDownStatus.SHUTDOWN_COMPLETED, ShutDownStatus.SHUTDOWN_INPROGRESS);
                            return;
                        }
                        com.alachisoft.tayzgrid.common.threading.Monitor.wait(this);
                    }

                    if (this._updateQueue.getCount() > 0) {
                        operation = this._updateQueue.Dequeue();
                    } else if (_updateQueue.getCount() == 0 && _shutdownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                        break;
                    }
                }

                if (operation == null) {
                    continue;
                }
                synchronized (_processMutex) {
                    if (!_isDisposing) {
                        if (_dsMgr != null) {
                            _dsMgr.DSAsyncUpdateInCache(operation);
                        }
                    }
                }
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
            }
        }

    }

    public final void Enqueue(WriteOperation operation) {
        synchronized (this) {
            this._updateQueue.Enqueue(operation);
            com.alachisoft.tayzgrid.common.threading.Monitor.pulse(this);
        }
    }

    public final void WindUpTask() {
        _ncacheLog.CriticalInfo("DSAsyncUpdatesProcessor", "WindUp Task Started.");
        if (_updateQueue != null) {
            _ncacheLog.CriticalInfo("DSAsyncUpdatesProcessor", "Update processor Queue Count: " + _updateQueue.getCount());
        }
        _shutdownStatusLatch.SetStatusBit(ShutDownStatus.SHUTDOWN_INPROGRESS, ShutDownStatus.NONE);
        synchronized (this) {
            com.alachisoft.tayzgrid.common.threading.Monitor.pulse(this);
        }
        _ncacheLog.CriticalInfo("DSAsyncUpdatesProcessor", "WindUp Task Ended.");
    }

    public final void WaitForShutDown(long interval) {
        _ncacheLog.CriticalInfo("DSAsyncUpdatesProcessor", "Waiting for shutdown task completion.");

        if (_updateQueue.getCount() > 0) {
            _shutdownStatusLatch.WaitForAny(ShutDownStatus.SHUTDOWN_COMPLETED, interval * 1000);
        }

        if (_updateQueue != null && _updateQueue.getCount() > 0) {
            _ncacheLog.CriticalInfo("DSAsyncUpdatesProcessor", "Remaining update processor queue operations: " + _updateQueue.getCount());
        }

        _ncacheLog.CriticalInfo("DSAsyncUpdatesProcessor", "Shutdown task completed.");
    }
}
