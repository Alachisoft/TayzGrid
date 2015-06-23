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

import com.alachisoft.tayzgrid.common.net.Address;

public final class ClusterNodeInformation {

    private int _serverPort;
    private int _connectedClients;
    private int _refCount;
    /**
     * partitioned to which clients are connected.
     */
    private Address _activePartitioned;

    public ClusterNodeInformation(int serverPort, int connectedClients) {
        this._serverPort = serverPort;
        this._connectedClients = connectedClients;
        AddRef();
    }

    public void AddRef() {
        synchronized (this) {
            _refCount++;
        }
    }

    /**
     * Decreases the reference count.
     *
     * @return True if no reference is left otherwise false
     */
    public boolean RemoveRef() {
        synchronized (this) {
            _refCount--;
            if (_refCount == 0) {
                return true;
            }
        }
        return false;
    }

    public int getRefCount() {
        return _refCount;
    }

    public int getServerPort() {
        return this._serverPort;
    }

    public Address getActivePartition() {
        return _activePartitioned;
    }

    public void setActivePartition(Address value) {
        _activePartitioned = value;
    }

    public int getConnectedClients() {
        return this._connectedClients;
    }

    public void setConnectedClients(int value) {
        this._connectedClients = value;
    }
}