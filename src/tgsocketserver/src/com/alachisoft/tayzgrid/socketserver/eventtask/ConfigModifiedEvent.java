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
import com.alachisoft.tayzgrid.caching.util.HotConfig;
import com.alachisoft.tayzgrid.common.protobuf.ConfigModifiedEventResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
public final class ConfigModifiedEvent implements IEventTask {
	private HotConfig _config;
	private String _cacheId;
	private String _clientid;

	public ConfigModifiedEvent(HotConfig config, String cacheId, String clientid) {
		_config = config;
		_cacheId = cacheId;
		_clientid = clientid;
	}

	public void Process() {
		ClientManager clientManager = null;

		synchronized (ConnectionManager.ConnectionTable) {
			clientManager = (ClientManager)ConnectionManager.ConnectionTable.get(_clientid);
		}
		if (clientManager != null) {
			ResponseProtocol.Response.Builder response = ResponseProtocol.Response.newBuilder();
			ConfigModifiedEventResponseProtocol.ConfigModifiedEventResponse.Builder configModified = ConfigModifiedEventResponseProtocol.ConfigModifiedEventResponse.newBuilder();

			configModified.setHotConfig(_config.toString());

			response.setConfigModified(configModified);
			response.setResponseType(ResponseProtocol.Response.Type.CONFIG_MODIFIED_EVENT);

			byte[] serializedResponse = ResponseHelper.SerializeResponse(response.build());

			ConnectionManager.AssureSend(clientManager, serializedResponse);
		}
	}
}
