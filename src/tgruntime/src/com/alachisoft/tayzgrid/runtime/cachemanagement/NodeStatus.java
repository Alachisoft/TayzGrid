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

package com.alachisoft.tayzgrid.runtime.cachemanagement;

public class NodeStatus {

    private ServerNode _nodeInfo;
    private ConnectivityStatus _connectivityStatus = getConnectivityStatus().values()[0];
    private ServerNode[] _connectedNodesInfo;

    public final void setNodeInfo(ServerNode value) {
        _nodeInfo = value;
    }

    public final ServerNode getNodeInfo() {
        return _nodeInfo;
    }

    public final void setConnectivityStatus(ConnectivityStatus value) {
        _connectivityStatus = value;
    }

    public final ConnectivityStatus getConnectivityStatus() {
        return _connectivityStatus;
    }

    public final void setConnectedNodes(ServerNode[] value) {
        _connectedNodesInfo = value;
    }

    public final ServerNode[] getConnectedNodes() {
        return _connectedNodesInfo;
    }
}