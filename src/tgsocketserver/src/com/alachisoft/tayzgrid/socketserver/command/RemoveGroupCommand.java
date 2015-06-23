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
import com.alachisoft.tayzgrid.common.protobuf.RemoveGroupResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.RemoveGroupCommandProtocol;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
public class RemoveGroupCommand extends CommandBase {
	private final static class CommandInfo {
		public String RequestId;
		public String Group;
		public String SubGroup;
		public long ClientLastViewId;

		public CommandInfo clone() {
			CommandInfo varCopy = new CommandInfo();

			varCopy.RequestId = this.RequestId;
			varCopy.Group = this.Group;
			varCopy.SubGroup = this.SubGroup;
			varCopy.ClientLastViewId = this.ClientLastViewId;

			return varCopy;
		}
	}

	//PROTOBUF
	@Override
	public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) {
		CommandInfo cmdInfo = new CommandInfo();

		try {
			cmdInfo = ParseCommand(command, clientManager).clone();
		} catch (RuntimeException exc) {
			if (!super.immatureId.equals("-2")) {
				//PROTOBUF:RESPONSE
				_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
			}
			return;
		}

		try {
			ICommandExecuter tempVar = clientManager.getCmdExecuter();
			TayzGrid nCache = (TayzGrid)((tempVar instanceof TayzGrid) ? tempVar : null);

			OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
			operationContext.Add(OperationContextFieldName.ClientLastViewId, cmdInfo.ClientLastViewId);

			nCache.getCache().Remove(cmdInfo.Group, cmdInfo.SubGroup, operationContext);

			//PROTOBUF:RESPONSE
                        ResponseProtocol.Response response=ResponseProtocol.Response.newBuilder().setRemoveGroupResponse(RemoveGroupResponseProtocol.RemoveGroupResponse.newBuilder())
                                                                                                 .setRequestId(Long.parseLong(cmdInfo.RequestId))
                                                                                                 .setResponseType(ResponseProtocol.Response.Type.REMOVE_GROUP).build();
			_serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));

		} catch (Exception exc) {
			//PROTOBUF:RESPONSE
			_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
		}
	}


	//PROTOBUF
	private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager) {
		CommandInfo cmdInfo = new CommandInfo();

		RemoveGroupCommandProtocol.RemoveGroupCommand removeGroupCommand = command.getRemoveGroupCommand();
		cmdInfo.Group = removeGroupCommand.getGroup();
		cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
                cmdInfo.SubGroup = removeGroupCommand.getSubGroup().length() == 0 ? null : removeGroupCommand.getSubGroup();
		cmdInfo.ClientLastViewId = command.getClientLastViewId();

		return cmdInfo;
	}

}
