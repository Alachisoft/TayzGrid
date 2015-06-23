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
import com.alachisoft.tayzgrid.common.protobuf.ClearResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ClearCommandProtocol;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
public class ClearCommand extends CommandBase {
	protected final static class CommandInfo {
		public boolean DoAsync;
		public String RequestId;
		public BitSet FlagMap;
		public short DsClearedId;
		public String providerName;

		public CommandInfo clone() {
			CommandInfo varCopy = new CommandInfo();

			varCopy.DoAsync = this.DoAsync;
			varCopy.RequestId = this.RequestId;
			varCopy.FlagMap = this.FlagMap;
			varCopy.DsClearedId = this.DsClearedId;
			varCopy.providerName = this.providerName;

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
			if (!super.immatureId.equals("-2")) {

				//PROTOBUF:RESPONSE
				_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
			}
			return;
		}
		ICommandExecuter tempVar = clientManager.getCmdExecuter();
		TayzGrid nCache = (TayzGrid)((tempVar instanceof TayzGrid) ? tempVar : null);
		if (!cmdInfo.DoAsync) {
			try {
				CallbackEntry cbEntry = null;
				if (cmdInfo.DsClearedId != -1) {
					cbEntry = new CallbackEntry(clientManager.getClientID(), -1, null, (short)-1, (short)-1, (short)-1, cmdInfo.DsClearedId, cmdInfo.FlagMap, EventDataFilter.None,EventDataFilter.None);
				}
				OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
				operationContext.Add(OperationContextFieldName.ReadThruProviderName, cmdInfo.providerName);

				nCache.getCache().Clear(cmdInfo.FlagMap, cbEntry, operationContext);

                                ResponseProtocol.Response response=ResponseProtocol.Response.newBuilder().setClearResponse(ClearResponseProtocol.ClearResponse.newBuilder())
                                                                                                          .setRequestId(command.getRequestID())
				                                                                          .setResponseType(ResponseProtocol.Response.Type.CLEAR).build();

				_serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));

			} catch (Exception exc) {
				//PROTOBUF:RESPONSE
				_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
			}
		}
		else {
			OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
			operationContext.Add(OperationContextFieldName.ReadThruProviderName, cmdInfo.providerName);

			nCache.getCache().ClearAsync(cmdInfo.FlagMap, new CallbackEntry(clientManager.getClientID(), Integer.parseInt(cmdInfo.RequestId), null, (short)-1, (short)-1,(short) 0, cmdInfo.DsClearedId, cmdInfo.FlagMap, EventDataFilter.None,EventDataFilter.None), operationContext);
		}
	}



	//PROTOBUF
	private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager) {
		CommandInfo cmdInfo = new CommandInfo();

		ClearCommandProtocol.ClearCommand clearCommand = command.getClearCommand();

		cmdInfo.DoAsync = clearCommand.getIsAsync();
		cmdInfo.DsClearedId = (short) (clearCommand.getDatasourceClearedCallbackId());
		cmdInfo.FlagMap = new BitSet((byte)clearCommand.getFlag());
		cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
		cmdInfo.providerName = clearCommand.getProviderName();

		return cmdInfo;
	}


}
