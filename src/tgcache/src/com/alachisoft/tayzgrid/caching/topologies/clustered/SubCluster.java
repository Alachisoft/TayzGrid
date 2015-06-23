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

import com.alachisoft.tayzgrid.cluster.blocks.GroupRequest;
import com.alachisoft.tayzgrid.cluster.util.RspList;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.net.Address;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

public class SubCluster {

    private String _groupid;
    private ClusterService _service;
    private ILogger _ncacheLog;

    private ILogger getCacheLog() {
        return _ncacheLog;
    }
    protected java.util.List _members = Collections.synchronizedList(new java.util.ArrayList(11));
    protected java.util.List _servers = Collections.synchronizedList(new java.util.ArrayList(11));

    public final java.util.List getMembers() {
        return _members;
    }

    public final java.util.List getServers() {
        return _servers;
    }

    public SubCluster(String gpid, ClusterService service) {
        _groupid = gpid;
        _service = service;
        _ncacheLog = _service.getCacheLog();

    }

    public final String getName() {
        return _groupid;
    }

    public final boolean getIsCoordinator() {
        Address address = getCoordinator();
        if (address != null && _service.getLocalAddress().compareTo(address) == 0) {
            return true;
        }
        return false;
    }

    public final boolean IsMember(Address node) {
        if (getMembers().contains(node) || _servers.contains(node)) {
            return true;
        }
        return false;
    }

    public final Address getCoordinator() {
        synchronized (_servers) {
            if (_servers.size() > 0) {
                return (Address) ((_servers.get(0) instanceof Address) ? _servers.get(0) : null);
            }
        }
        return null;
    }

    public int OnMemberJoined(Address address, NodeIdentity identity) {
        if (!identity.getSubGroupName().equals(_groupid)) {
            return -1;
        }

        getCacheLog().Warn("SubCluster.OnMemberJoined()", "Memeber " + address + " added to sub-cluster " + _groupid);

        _members.add(address);
        if (identity.getHasStorage() && !identity.getIsStartedAsMirror()) {
            _servers.add(address);
        }
        return _members.size();
    }

    public int OnMemberLeft(Address address, java.util.HashMap bucketsOwnershipMap) {
        if (_members.contains(address)) {
            getCacheLog().Warn("SubCluster.OnMemberJoined()", "Memeber " + address + " left sub-cluster " + _groupid);
            _members.remove(address);
            _servers.remove(address);
            return _members.size();
        }
        return -1;
    }

    @Override
    public boolean equals(Object obj) {
        boolean result = false;

        if (obj instanceof SubCluster) {
            SubCluster other = (SubCluster) ((obj instanceof SubCluster) ? obj : null);
            result = this._groupid.compareTo(other._groupid) == 0;

            if (result) {
                result = this._members.size() == other._members.size();

                if (result) {
                    for (Iterator it = this._members.iterator(); it.hasNext();) {
                        Address mbr = (Address) it.next();
                        if (!other._members.contains(mbr)) {
                            result = false;
                            break;
                        }
                    }
                }
            }
        }

        return result;
    }

    public final RspList BroadcastMessage(Object msg, byte mode, long timeout) throws IOException, ClassNotFoundException {
        return _service.BroadcastToMultiple(_members, msg, mode, timeout);
    }

    public final void BroadcastNoReplyMessage(Object msg) throws IOException, ClassNotFoundException {
        _service.BroadcastToMultiple(_members, msg, GroupRequest.GET_NONE, -1);
    }
}
