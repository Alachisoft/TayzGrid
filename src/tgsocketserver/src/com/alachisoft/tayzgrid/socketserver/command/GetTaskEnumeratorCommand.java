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

import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorPointer;
import com.alachisoft.tayzgrid.common.protobuf.GetTaskEnumeratorCommandProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.ICommandExecuter;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.alachisoft.tayzgrid.socketserver.command.responsebuilders.TaskResponseBuilder;
import java.util.List;
public final class GetTaskEnumeratorCommand extends CommandBase {
	private final static class CommandInfo {
		public String RequestId;
                public String TaskID;
                public Short  callbackID;
                public long ClientLastViewId;
                private int CommandVersion;


		public CommandInfo clone() {
			CommandInfo varCopy = new CommandInfo();

			varCopy.RequestId = this.RequestId;
                        varCopy.TaskID = this.TaskID;
                        varCopy.callbackID = this.callbackID;
                        varCopy.ClientLastViewId = this.ClientLastViewId;
                        varCopy.CommandVersion = this.CommandVersion;
			return varCopy;
		}
	}

	//PROTOBUF
	@Override
	public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) {
		CommandInfo cmdInfo = new CommandInfo();

		try {
			cmdInfo = ParseCommand(command, clientManager);
		} catch (Exception exc) {
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
                        List<TaskEnumeratorResult> resultSets = (java.util.List)nCache.getCache().getTaskEnumerator(new TaskEnumeratorPointer(clientManager.getClientID(), cmdInfo.TaskID,cmdInfo.callbackID), operationContext);
                        
			//PROTOBUF:RESPONSE
                        TaskResponseBuilder.BuildGetTaskEnumeratorResponse(resultSets, cmdInfo.CommandVersion, cmdInfo.RequestId, _serializedResponsePackets);		

		} catch (Exception exc) {
			//PROTOBUF:RESPONSE
			_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
		}
	}


	//PROTOBUF

	private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager) {
		CommandInfo cmdInfo = new CommandInfo();

		GetTaskEnumeratorCommandProtocol.GetTaskEnumeratorCommand getTaskEnumeratorCommand = command.getGetTaskEnumeratorCommand();

		cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
                cmdInfo.CommandVersion=command.getCommandVersion();
                cmdInfo.TaskID=getTaskEnumeratorCommand.getPointer().getTaskId();
                cmdInfo.callbackID=(short)getTaskEnumeratorCommand.getPointer().getCallbackId();
                cmdInfo.ClientLastViewId = command.getClientLastViewId();

		return cmdInfo;
	}
}
