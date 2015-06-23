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
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.TaskCallbackCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.TaskCallbackResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.MapReduceTaskResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.mapreduce.notification.TaskCallbackInfo;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.ICommandExecuter;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.alachisoft.tayzgrid.socketserver.NotificationsType;

public class RegisterTaskCallbackCommand extends CommandBase {
 
    @Override
    public void ExecuteCommand(ClientManager clientManager, CommandProtocol.Command command) throws Exception {

        short callbackId = 0;
        String taskId = "";               

        try {
            TaskCallbackCommandProtocol.TaskCallbackCommand mapReduceCommand
                    = command.getTaskCallbackCommand();
            //cmdInfo.requestId = (new Long(command.getRequestID())).toString();

            if(mapReduceCommand.getCallbackId() != 0)
                callbackId = (short) mapReduceCommand.getCallbackId();
            if(mapReduceCommand.getTaskId() != null || mapReduceCommand.getTaskId().equals(""))
                taskId = mapReduceCommand.getTaskId();
            
        } catch (Exception exc) {
            if (!super.immatureId.equals("-2")) {
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
            }
            return;
        }

        try {
            ICommandExecuter tmpVar = clientManager.getCmdExecuter();
            TayzGrid nCache = (TayzGrid) (tmpVar instanceof TayzGrid ? tmpVar : null);

            nCache.getCache().RegisterTaskNotificationCallback(taskId,new TaskCallbackInfo(clientManager.getClientID(),callbackId), new OperationContext());

            ResponseProtocol.Response response = ResponseProtocol.Response.newBuilder()
                    .setTaskCallbackResponse(TaskCallbackResponseProtocol.TaskCallbackResponse.newBuilder())
                    .setRequestId(command.getRequestID())
                    .setResponseType(ResponseProtocol.Response.Type.MAP_REDUCE_TASK_CALLBACK).build();

            _serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));

        } catch (Exception e) {
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(e, command.getRequestID()));
        }

    }

}
