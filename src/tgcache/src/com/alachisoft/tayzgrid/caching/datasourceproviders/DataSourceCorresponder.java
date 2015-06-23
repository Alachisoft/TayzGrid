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
import com.alachisoft.tayzgrid.caching.topologies.clustered.ClusterCacheBase;

public class DataSourceCorresponder 
{

    private ClusterCacheBase _parent;
    private DatasourceMgr _dsManager;
    private String _nextChunkId;
    private String _prevChunkId;
    WriteBehindAsyncProcessor.WriteBehindQueue _queue;
    private static final long CHUNK_SIZE = 1024 * 100;
    private ILogger _ncacheLog;

    public final ILogger getCacheLog() {
        return _ncacheLog;
    }

    public DataSourceCorresponder(DatasourceMgr dsManager, ILogger cacheLog) {
        _dsManager = dsManager;
        _ncacheLog = cacheLog;
    }

    public final WriteBehindQueueResponse GetWriteBehindQueue(WriteBehindQueueRequest req) throws Exception {
        WriteBehindQueueResponse rsp = null;

        if (_dsManager != null && !_dsManager.getIsWriteBehindEnabled()) {
            return rsp;
        }
        WriteBehindAsyncProcessor.WriteBehindQueue queueChunk = new WriteBehindAsyncProcessor.WriteBehindQueue((_parent != null) ? _parent.getContext() : null);
        int indexOfNextTask = 0;
        long currentChunkSize = 0;
        String nextChunkId = null;

        if (req != null) {
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("DSReplicationCorr.GetWriteBehindQueue", "received chunk request; nextchunkId :" + req.getNextChunkId());
            }

            DSWriteBehindOperation operation = null;
            if (_queue == null) {
                WriteBehindAsyncProcessor.WriteBehindQueue queue = null;
                if (_dsManager._writeBehindAsyncProcess != null) {
                    queue = _dsManager._writeBehindAsyncProcess.CloneQueue();
                }
                if (queue != null) {
                    _queue = new WriteBehindAsyncProcessor.WriteBehindQueue((_parent != null) ? _parent.getContext() : null);
                    _queue.MergeQueue(queue);
                } else {
                    return null;
                }
            }

            if (req.getNextChunkId() != null) {
                for (int i = 0; i < _queue.getCount(); i++) {

                    operation = (DSWriteBehindOperation) ((_queue.getItem(i) instanceof DSWriteBehindOperation) ? _queue.getItem(i) : null);
                    if (operation != null) {
                        if (operation.getTaskId().equals(req.getNextChunkId())) {
                            indexOfNextTask = i;
                            break;
                        }
                    }
                }
            }

            for (int i = indexOfNextTask; i < _queue.getCount(); i++) {
                operation = (DSWriteBehindOperation) ((_queue.getItem(i) instanceof DSWriteBehindOperation) ? _queue.getItem(i) : null);
                if (operation != null) {
                    if (currentChunkSize >= CHUNK_SIZE) {
                        nextChunkId = operation.getTaskId();
                        break;
                    }
                    currentChunkSize += operation.getSize();
                    queueChunk.Enqueue(operation.getKey(), true, operation);
                }
            }

            if (nextChunkId == null) {
                _queue.clear();
                _queue = null;
            }
            if (queueChunk.getCount() > 0) {
                rsp = new WriteBehindQueueResponse(queueChunk, nextChunkId, null);
            }
        }

        return rsp;
    }

    public final void dispose() {
        if (_queue != null) {
            _queue.clear();
            _queue = null;
        }
    }
}
