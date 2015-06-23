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

package com.alachisoft.tayzgrid.socketserver.command;

import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.ConnectionManager;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetLoggingInfoResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetLoggingInfoCommandProtocol;
import com.alachisoft.tayzgrid.common.LoggingInfo;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
public class GetLogginInfoCommand extends CommandBase {

	@Override
	public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) {
		/**Command:
		 GETLOGGINGINFO "requestId"

		*/
		String requestId = "";

		GetLoggingInfoCommandProtocol.GetLoggingInfoCommand getLoggingInfoCommand = command.getGetLoggingInfoCommand();
		requestId = (new Long(command.getRequestID())).toString();

		boolean errorEnabled = ConnectionManager.GetClientLoggingInfo(LoggingInfo.LoggingType.Error) == LoggingInfo.LogsStatus.Enable;
		boolean detailedEnabled = ConnectionManager.GetClientLoggingInfo(LoggingInfo.LoggingType.Detailed) == LoggingInfo.LogsStatus.Enable;

		if (!errorEnabled) {
			detailedEnabled = false;
		}

		try {
                        ResponseProtocol.Response response= ResponseProtocol.Response.newBuilder().setGetLoggingInfoResponse(GetLoggingInfoResponseProtocol.GetLoggingInfoResponse.newBuilder()
                                                                                                    .setErrorsEnabled(errorEnabled)
                                                                                                    .setDetailedErrorsEnabled(detailedEnabled))
                                                                                                    .setRequestId(command.getRequestID())
                                                                                                    .setResponseType(ResponseProtocol.Response.Type.GET_LOGGING_INFO).build();
			_serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));
		} catch (Exception exc) {
			_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
		}
	}


}
