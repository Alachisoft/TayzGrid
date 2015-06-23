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

public class CacheHealth {

    private String _name;
    private CacheTopology _topology = CacheTopology.values()[0];
    private CacheStatus _status = CacheStatus.values()[0];
    private NodeStatus[] _serverNodesStatus;

    public final void setCacheName(String value) {
        _name = value;
    }

    public final String getCacheName() {
        return _name;
    }

    public final void setTopology(CacheTopology value) {
        _topology = value;
    }

    public final CacheTopology getTopology() {
        return _topology;
    }

    public final void setStatus(CacheStatus value) {
        _status = value;
    }

    public final CacheStatus getStatus() {
        return _status;
    }

    public final void setServerNodesStatus(NodeStatus[] value) {
        _serverNodesStatus = value;
    }

    public final NodeStatus[] getServerNodesStatus() {
        return _serverNodesStatus;
    }
}