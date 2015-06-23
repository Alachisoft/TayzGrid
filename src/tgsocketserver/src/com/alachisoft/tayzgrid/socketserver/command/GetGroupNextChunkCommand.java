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
import com.alachisoft.tayzgrid.common.protobuf.GetGroupNextChunkCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetGroupNextChunkResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.util.EnumerationPointerConversionUtil;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;
import com.alachisoft.tayzgrid.common.datastructures.GroupEnumerationPointer;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
public class GetGroupNextChunkCommand extends CommandBase {
	private final static class CommandInfo {
		public String RequestId;
		public GroupEnumerationPointer Pointer;
		public OperationContext OperationContext;

		public CommandInfo clone() {
			CommandInfo varCopy = new CommandInfo();

			varCopy.RequestId = this.RequestId;
			varCopy.Pointer = this.Pointer;
			varCopy.OperationContext = this.OperationContext;

			return varCopy;
		}
	}

	@Override
	public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) {
		CommandInfo cmdInfo = new CommandInfo();

		try {
			cmdInfo = ParseCommand(command, clientManager).clone();
		} catch (Exception exc) {
			if (!super.immatureId.equals("-2")) {
				_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
			}

			return;
		}

		try {
			ICommandExecuter tempVar = clientManager.getCmdExecuter();
			TayzGrid nCache = (TayzGrid)((tempVar instanceof TayzGrid) ? tempVar : null);
			EnumerationDataChunk nextChunk = nCache.getCache().GetNextChunk(cmdInfo.Pointer, cmdInfo.OperationContext);

                        GetGroupNextChunkResponseProtocol.GetGroupNextChunkResponse.Builder getNextChunkResponse=GetGroupNextChunkResponseProtocol.GetGroupNextChunkResponse.newBuilder();
                        ResponseProtocol.Response response= ResponseProtocol.Response.newBuilder().setGetGroupNextChunkResponse(getNextChunkResponse.build())
                                                                                                  .setRequestId(Long.parseLong(cmdInfo.RequestId))
                                                                                                  .setResponseType(ResponseProtocol.Response.Type.GET_GROUP_NEXT_CHUNK).build();

			getNextChunkResponse.getKeysList().addAll(nextChunk.getData());
			com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer tempVar2 = nextChunk.getPointer();
			getNextChunkResponse.setGroupEnumerationPointer(EnumerationPointerConversionUtil.ConvertToProtobufGroupEnumerationPointer((GroupEnumerationPointer)((tempVar2 instanceof GroupEnumerationPointer) ? tempVar2 : null)));

			_serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));
		} catch (Exception exc) {
			_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
		}
	}

	private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager) {
		CommandInfo cmdInfo = new CommandInfo();
		GetGroupNextChunkCommandProtocol.GetGroupNextChunkCommand getNextChunkCommand = command.getGetGroupNextChunkCommand();
		cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
		cmdInfo.Pointer = EnumerationPointerConversionUtil.GetFromProtobufGroupEnumerationPointer(getNextChunkCommand.getGroupEnumerationPointer());

		String intendedRecepient = command.getIntendedRecipient();
		long lastViewId = command.getClientLastViewId();

		cmdInfo.OperationContext = new OperationContext();
		cmdInfo.OperationContext.Add(OperationContextFieldName.IntendedRecipient, intendedRecepient);
		cmdInfo.OperationContext.Add(OperationContextFieldName.ClientLastViewId, lastViewId);

		return cmdInfo;
	}
}
