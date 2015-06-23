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

import com.alachisoft.tayzgrid.caching.topologies.clustered.ClusterCacheBase;
import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.net.Address;

public class DataSourceReplicationManager 
{

    private ClusterCacheBase _parent;
    private DatasourceMgr _dsManager;
    private String _nextChunkId;
    private String _prevChunkId;
    WriteBehindAsyncProcessor.WriteBehindQueue _queue;
    private ILogger _ncacheLog;

    public final ILogger getCacheLog() {
        return _ncacheLog;
    }

    public DataSourceReplicationManager(ClusterCacheBase parent, DatasourceMgr dsMgr, ILogger NCacheLog) {
        _parent = parent;
        _dsManager = dsMgr;
        _ncacheLog = NCacheLog;
        _queue = new WriteBehindAsyncProcessor.WriteBehindQueue((_parent != null) ? _parent.getContext() : null);
    }

    public final void ReplicateWriteBehindQueue() throws Exception {
        if (_parent != null) {

            if (_dsManager != null && !_dsManager.getIsWriteBehindEnabled()) {
                return;
            }

            WriteBehindQueueRequest req = new WriteBehindQueueRequest(null, null);
            WriteBehindQueueResponse rsp = null;
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("DSReplicationMgr.ReplicatedQueue", "---->started replicating queue");
            }
            while (true) {
                Address coordinator = _parent.getCluster().getCoordinator();
                if (_parent.getCluster().getIsCoordinator()) {
                    break;
                }
                if (_parent.getCluster().getCurrentSubCluster() != null) {
                    if (_parent.getCluster().getCurrentSubCluster().getIsCoordinator()) {
                        break;
                    }
                }

                try {
                    if (coordinator != null) {
                        rsp = _parent.TransferQueue(coordinator, req);
                    }
                } catch (SuspectedException se) {
                    Thread.sleep(5); //wait untill view is changed properly
                    continue;
                }
                catch (com.alachisoft.tayzgrid.common.exceptions.TimeoutException te)
                {
                    continue;
                }

                if (rsp != null) {
                    WriteBehindAsyncProcessor.WriteBehindQueue chunkOfQueue = rsp.getQueue();
                    if (chunkOfQueue != null) {
                        _queue.MergeQueue(chunkOfQueue);
                    }

                    if (rsp.getNextChunkId() == null) {
                        break;
                    } else {
                        req = new WriteBehindQueueRequest(rsp.getNextChunkId(), null);
                    }

                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("DSReplicationMgr.ReplicatedQueue", "received chunk from " + coordinator + " nextchunkId :" + req.getNextChunkId());
                    }

                } else {
                    break;
                }

            }

            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("DSReplicationMgr.ReplicatedQueue", "queue has been transfered");
            }
            if (_queue.getCount() > 0) {
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("DSReplicationMgr.ReplicatedQueue", "queue count :" + _queue.getCount());
                }

                if (_dsManager._writeBehindAsyncProcess != null) {
                    _dsManager._writeBehindAsyncProcess.MergeQueue(_parent.getContext(), _queue);
                }
            }
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("DSReplicationMgr.ReplicatedQueue", "---->replication of queue completed");
            }
        }
    }

    public final void dispose() {
        if (_queue != null) {
            _queue.clear();
            _queue = null;
        }
    }
}
