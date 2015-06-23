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

import com.alachisoft.tayzgrid.socketserver.ICommandExecuter;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.alachisoft.tayzgrid.common.protobuf.RaiseCustomEventResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.RaiseCustomEventCommandProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
public class RaiseCustomNotifCommand extends CommandBase {
	private final static class CommandInfo {
		public String RequestId;

		public CommandInfo clone() {
			CommandInfo varCopy = new CommandInfo();

			varCopy.RequestId = this.RequestId;

			return varCopy;
		}
	}

	//PROTOBUF
	@Override
	public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) {
		CommandInfo cmdInfo = new CommandInfo();

		byte[] data = null;

		try {
			cmdInfo = ParseCommand(command, clientManager).clone();
		} catch (RuntimeException exc) {
			if (!super.immatureId.equals("-2")) {
				//PROTOBUF:RESPONSE
				_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
			}
			return;
		}
		RaiseCustomEventCommandProtocol.RaiseCustomEventCommand notifcommand = command.getRaiseCustomEventCommand();

		try {
			ICommandExecuter tempVar = clientManager.getCmdExecuter();
			TayzGrid nCache = (TayzGrid)((tempVar instanceof TayzGrid) ? tempVar : null);
			nCache.getCache().SendNotification(notifcommand.getNotifIf().toByteArray(), notifcommand.getData().toByteArray());


                        ResponseProtocol.Response response= ResponseProtocol.Response.newBuilder().setRaiseCustomEventResponse(RaiseCustomEventResponseProtocol.RaiseCustomEventResponse.newBuilder())
                                                                                                  .setRequestId(command.getRequestID())
			                                                                          .setResponseType(ResponseProtocol.Response.Type.RAISE_CUSTOM_EVENT).build();
			_serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));
		} catch (Exception exc) {
			//PROTOBUF:RESPONSE
			_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
		}
	}

	//PROTOBUF
	private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager) {
		CommandInfo cmdInfo = new CommandInfo();
		RaiseCustomEventCommandProtocol.RaiseCustomEventCommand raiseCustomEventCommand = command.getRaiseCustomEventCommand();
		cmdInfo.RequestId = (new Long(command.getRequestID())).toString();

		return cmdInfo;
	}


}
