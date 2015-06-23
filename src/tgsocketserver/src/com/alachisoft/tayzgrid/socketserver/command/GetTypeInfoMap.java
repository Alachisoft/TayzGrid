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
import com.alachisoft.tayzgrid.common.protobuf.GetTypeInfoMapCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetTypeMapResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.serialization.util.TypeInfoMap;
public class GetTypeInfoMap extends CommandBase {

	protected final static class CommandInfo {
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

		try {
			cmdInfo = ParseCommand(command, clientManager).clone();
		} catch (Exception exc) {
			if (!super.immatureId.equals("-2"))
			{
				_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
			}
			return;
		}

		try {
			String typeInfoMap = "";
			ICommandExecuter tempVar = clientManager.getCmdExecuter();
			TayzGrid nCache = (TayzGrid)((tempVar instanceof TayzGrid) ? tempVar : null);

			if (nCache.getCache().GetTypeInfoMap() != null) {
				typeInfoMap = nCache.getCache().GetTypeInfoMap().ToProtocolString();
			}

                        ResponseProtocol.Response response= ResponseProtocol.Response.newBuilder().setGetTypemap(GetTypeMapResponseProtocol.GetTypeMapResponse.newBuilder().setMap(typeInfoMap))
                                                                                                  .setRequestId(command.getRequestID())
                                                                                                  .setResponseType(ResponseProtocol.Response.Type.GET_TYPEINFO_MAP).build();
			_serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));
		} catch (Exception exc) {
			_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
		}
	}

	//PROTOBUF
	private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager) {
		CommandInfo cmdInfo = new CommandInfo();

		GetTypeInfoMapCommandProtocol.GetTypeInfoMapCommand getTypeInfoMapCommand = command.getGetTypeInfoMapCommand();

		cmdInfo.RequestId = (new Long(command.getRequestID())).toString();

		return cmdInfo;
	}

}
