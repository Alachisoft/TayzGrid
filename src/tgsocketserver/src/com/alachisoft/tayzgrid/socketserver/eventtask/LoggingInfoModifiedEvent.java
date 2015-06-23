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
import com.alachisoft.tayzgrid.common.protobuf.LoggingInfoModifiedEventResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
public final class LoggingInfoModifiedEvent implements IEventTask {
	private boolean _enableErrorLog;
	private boolean _enableDetailedLog;
	private String _clientid;

	public LoggingInfoModifiedEvent(boolean enableErrorLogs, boolean enableDetailedLogs, String clientId) {
		this._enableErrorLog = enableErrorLogs;
		this._enableDetailedLog = enableDetailedLogs;
		this._clientid = clientId;
	}

	public void Process() {
		ClientManager clientManager = null;

		synchronized (ConnectionManager.ConnectionTable) {
			clientManager = (ClientManager)ConnectionManager.ConnectionTable.get(_clientid);
		}
		if (clientManager != null) {
			ResponseProtocol.Response.Builder response = ResponseProtocol.Response.newBuilder();
			LoggingInfoModifiedEventResponseProtocol.LoggingInfoModifiedEventResponse.Builder loggingInfoModified = LoggingInfoModifiedEventResponseProtocol.LoggingInfoModifiedEventResponse.newBuilder();

			loggingInfoModified.setEnableDetailedErrorsLog(_enableDetailedLog);
			loggingInfoModified.setEnableErrorsLog(_enableErrorLog);

			response.setLoggingInfoModified(loggingInfoModified);
			response.setResponseType(ResponseProtocol.Response.Type.LOGGING_INFO_MODIFIED_EVENT);

			byte[] serializedResponse =ResponseHelper.SerializeResponse(response.build());

			ConnectionManager.AssureSend(clientManager, serializedResponse);
		}
	}
}
