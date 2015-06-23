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
import com.alachisoft.tayzgrid.common.protobuf.CacheStoppedEventResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
/**
 This event is fired when cache is stoped
*/
public final class CacheStoppedEvent implements IEventTask {
	private String _cacheId;
	private String _clientId;

	public CacheStoppedEvent(String cacheId, String clientId) {
		_cacheId = cacheId;
		_clientId = clientId;
	}

	public void Process() {
		ClientManager clientManager = null;

		synchronized (ConnectionManager.ConnectionTable) {
			clientManager = (ClientManager)ConnectionManager.ConnectionTable.get(_clientId);
		}
		if (clientManager != null) {
			ResponseProtocol.Response.Builder response = ResponseProtocol.Response.newBuilder();
			CacheStoppedEventResponseProtocol.CacheStoppedEventResponse.Builder cacheStopped = CacheStoppedEventResponseProtocol.CacheStoppedEventResponse.newBuilder();
			cacheStopped.setCacheId(_cacheId);
			response.setCacheStopped(cacheStopped);
			response.setResponseType(ResponseProtocol.Response.Type.CACHE_STOPPED_EVENT);

			byte[] serializedResponse =ResponseHelper.SerializeResponse(response.build());

			ConnectionManager.AssureSend(clientManager, serializedResponse);
		}
	}
}
