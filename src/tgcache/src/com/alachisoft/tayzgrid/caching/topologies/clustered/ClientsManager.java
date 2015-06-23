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
import java.util.Collections;

/**
 this class mantains the information of cluster members list at any time.
 on the basis of this members list, it decides whether a client can connect
 to the cache or not.
*/
public class ClientsManager {
	private java.util.List _activeClusterMbrs = Collections.synchronizedList(new java.util.ArrayList());
	private java.util.List _tentativeClusterMbrs = Collections.synchronizedList(new java.util.ArrayList());
	private ClusterService _cluster;

	public ClientsManager(ClusterService cluster) {
		_cluster = cluster;
	}

	public final boolean AcceptClient(java.net.InetAddress clientAddress) {
		return false;
	}

	public final void OnMemberJoined(Address address) {
		if (!_activeClusterMbrs.contains(address.getIpAddress())) {
			_activeClusterMbrs.add(address.getIpAddress());
		}
	}

	public final void OnMemberLeft(Address address) {
	}
}