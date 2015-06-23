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

package com.alachisoft.tayzgrid.socketserver.eventtask;

import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.ConnectionManager;
import com.alachisoft.tayzgrid.common.protobuf.NodeLeftEventResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
public final class NodeLeftEvent implements IEventTask {
	private String _cacheId;
	private Address _clusterAddress;
	private Address _serverAddress;
	private String _clientId;

	public NodeLeftEvent(String cacheId, Address clusterAddress, Address serverAddress, String clientid) {
		_cacheId = cacheId;
		_clusterAddress = clusterAddress;
		_serverAddress = serverAddress;
		_clientId = clientid;
	}

	public void Process() {
		ClientManager clientManager = null;

		synchronized (ConnectionManager.ConnectionTable) {
			clientManager = (ClientManager)ConnectionManager.ConnectionTable.get(_clientId);
		}
		if (clientManager != null) {
			ResponseProtocol.Response.Builder response = ResponseProtocol.Response.newBuilder();
			NodeLeftEventResponseProtocol.NodeLeftEventResponse.Builder nodeLeft = NodeLeftEventResponseProtocol.NodeLeftEventResponse.newBuilder();

			nodeLeft.setClusterIp(_clusterAddress.getIpAddress().getHostAddress());
			nodeLeft.setClusterPort((new Integer(_clusterAddress.getPort())).toString());
			nodeLeft.setServerIp(_serverAddress.getIpAddress().getHostAddress());
			nodeLeft.setServerPort((new Integer(_serverAddress.getPort())).toString());

			response.setNodeLeft(nodeLeft);
			response.setResponseType(ResponseProtocol.Response.Type.NODE_LEFT_EVENT);

			byte[] serializedResponse =ResponseHelper.SerializeResponse(response.build());

			ConnectionManager.AssureSend(clientManager, serializedResponse);
		}
	}
}
