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

import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.TaskProgressCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.TaskProgressResponseProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.ICommandExecuter;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.google.protobuf.ByteString;

public class TaskProgressCommand extends CommandBase {

    int requestId;
    String taskId;

    @Override
    public void ExecuteCommand(ClientManager clientManager, CommandProtocol.Command command) throws Exception {
        try {
            TaskProgressCommandProtocol.TaskProgressCommand taskProgressCommand
                    = command.getTaskProgressCommand();
            requestId = (int) taskProgressCommand.getRequestId();
            taskId = taskProgressCommand.getTaskId();

        } catch (Exception ex) {
            if (!super.immatureId.equals("-2")) {
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(ex, command.getRequestID()));
            }
            return;
        }

        try {
            ICommandExecuter tmpVar = clientManager.getCmdExecuter();
            TayzGrid nCache = (TayzGrid) (tmpVar instanceof TayzGrid ? tmpVar : null);

            TaskStatus status = nCache.getCache().getTaskProgress(this.taskId);
            
            byte[] serializedProgress = CompactBinaryFormatter.toByteBuffer(status, nCache.getCache().getName());
            
            ResponseProtocol.Response response = ResponseProtocol.Response.newBuilder()
                    .setTaskProgressResponse(TaskProgressResponseProtocol.TaskProgressResponse.newBuilder()
                            .setProgresses(ByteString.copyFrom(serializedProgress)))
                    .setRequestId(command.getRequestID())
                    .setResponseType(ResponseProtocol.Response.Type.TASK_PROGRESS).build();

            _serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));

        } catch (Exception e) {
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(e, command.getRequestID()));
        }

    }

}
