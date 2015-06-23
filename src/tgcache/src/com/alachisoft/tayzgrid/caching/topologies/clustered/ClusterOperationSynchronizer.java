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

import com.alachisoft.tayzgrid.common.IDisposable;
import java.io.IOException;

public class ClusterOperationSynchronizer implements IDisposable {

    private java.util.HashMap _lockTable = new java.util.HashMap();
    private java.util.HashMap _pendingRequests = new java.util.HashMap();
    private Object _sync = new Object();
    private ClusterService _cluster;

    public ClusterOperationSynchronizer(ClusterService cluster) {
        _cluster = cluster;

    }

    public final void HandleRequest(AsyncRequest operation) throws IOException, java.lang.Exception {
        boolean pooled = false;
        boolean poolReq = false;
        boolean allowOperation = false;
        java.util.ArrayList pendingOperations;

        if (operation != null) {
            if (operation.getSyncKey() != null) {
                synchronized (_sync) {
                    if (!_lockTable.containsKey(operation.getSyncKey())) {
                        _lockTable.put(operation.getSyncKey(), operation.getRequsetId());
                        allowOperation = true;
                    } else {
                        if (_pendingRequests.containsKey(operation.getSyncKey())) {
                            pendingOperations = (java.util.ArrayList) _pendingRequests.get(operation.getSyncKey());
                        } else {
                            pendingOperations = new java.util.ArrayList();
                            _pendingRequests.put(operation.getSyncKey(), pendingOperations);
                        }
                        pendingOperations.add(operation);
                        _pendingRequests.put(operation.getSyncKey(), pendingOperations);
                    }
                }
            } else {
                allowOperation = true;
            }
        }
        if (allowOperation) {
            ProcessRequest(operation);
        }
    }

    public final void ProcessRequest(Object request) throws IOException, java.lang.Exception {
        AsyncRequest synRequest = null;
        java.util.ArrayList pendingRequests = null;
        AsyncRequest pendingRequest = null;
        Object result = null;

        try {
            if (request != null && request instanceof AsyncRequest) {
                synRequest = (AsyncRequest) ((request instanceof AsyncRequest) ? request : null);
                result = _cluster.handleFunction(synRequest.getSrc(), (Function) synRequest.getOperation());

            }

        } catch (Exception e) {
            result = e;
        } finally {
            if (synRequest != null) {
                if (synRequest.getRequsetId() >= 0) {
                    _cluster.SendResponse(synRequest.getSrc(), result, synRequest.getRequsetId());
                }

                if (synRequest.getSyncKey() != null) {
                    synchronized (_sync) {

                        if (_pendingRequests.containsKey(synRequest.getSyncKey())) {
                            pendingRequests = (java.util.ArrayList) _pendingRequests.get(synRequest.getSyncKey());
                            pendingRequest = (AsyncRequest) pendingRequests.get(0);
                            _lockTable.put(synRequest.getSyncKey(), pendingRequest.getRequsetId());
                            pendingRequests.remove(0);
                            if (pendingRequests.isEmpty()) {
                                _pendingRequests.remove(synRequest.getSyncKey());
                            }

                        } else {
                            _lockTable.remove(synRequest.getSyncKey());
                        }
                    }

                    if (pendingRequest != null) {
                        ProcessRequest(pendingRequest);
                    }
                }
            }
        }
    }

    public final void dispose() {
        if (_lockTable != null) {
            _lockTable.clear();
            _lockTable = null;
        }

        if (_pendingRequests != null) {
            _pendingRequests.clear();
            _pendingRequests = null;
        }

        _cluster = null;
    }
}