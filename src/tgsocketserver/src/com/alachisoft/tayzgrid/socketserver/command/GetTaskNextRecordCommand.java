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
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorPointer;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.protobuf.GetNextRecordCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.TaskEnumeratorPointerProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.ICommandExecuter;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.alachisoft.tayzgrid.socketserver.command.responsebuilders.TaskResponseBuilder;
import com.sun.org.apache.bcel.internal.generic.InstructionConstants;
public final class GetTaskNextRecordCommand extends CommandBase {
	
	//PROTOBUF
	@Override
	public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) {
            long requestId = 0;
            TaskEnumeratorPointerProtocol.TaskEnumeratorPointer  pointer;
            TaskEnumeratorPointer tePointer = new TaskEnumeratorPointer();
            String taskId = "";
            String intendedRecipient = "";
            short uniqueID = 0;
            long clientLastViewId = 0;

		try {
                    GetNextRecordCommandProtocol.GetNextRecordCommand getNextRecordCommand = command.getGetNextRecordCommand();
                    requestId = command.getRequestID();
                    if(getNextRecordCommand.getIntendedRecipient()!= null || !getNextRecordCommand.getIntendedRecipient().equals(""))
                        intendedRecipient = getNextRecordCommand.getIntendedRecipient();
                    if(getNextRecordCommand.getPointer() != null) {
                        pointer = getNextRecordCommand.getPointer();
                        uniqueID = (short) pointer.getCallbackId();
                        taskId = pointer.getTaskId();
                        clientLastViewId = command.getClientLastViewId();
                        tePointer = new TaskEnumeratorPointer(pointer.getClientId(), taskId, uniqueID);
                        tePointer.setClientAddress(new Address(pointer.getClientIp(),pointer.getClientPort()));
                        tePointer.setClusterAddress(new Address(pointer.getClusterIp(), pointer.getClusterPort()));
                    }
		} catch (Exception exc) {
			if (!super.immatureId.equals("-2")) {
				//PROTOBUF:RESPONSE
				_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
			}
			return;
		}

		int count = 0;
		String keyPackage = null;

		try {
			ICommandExecuter tempVar = clientManager.getCmdExecuter();
			TayzGrid nCache = (TayzGrid)((tempVar instanceof TayzGrid) ? tempVar : null);

                        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                        operationContext.Add(OperationContextFieldName.IntendedRecipient, intendedRecipient);
			operationContext.Add(OperationContextFieldName.ClientLastViewId, clientLastViewId);
                        TaskEnumeratorResult result = nCache.getCache().getTaskNextRecord(tePointer, operationContext);
                        
			//PROTOBUF:RESPONSE
                       TaskResponseBuilder.BuildGetTaskNextRecordResponse(result, "" + requestId, _serializedResponsePackets);

		} catch (Exception exc) {
			//PROTOBUF:RESPONSE
			_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
		}
	}


}
