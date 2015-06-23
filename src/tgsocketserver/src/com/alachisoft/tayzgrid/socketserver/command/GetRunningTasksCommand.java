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
import com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.ICommandExecuter;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import java.util.ArrayList;
import java.util.List;

public class GetRunningTasksCommand extends CommandBase {

    @Override
    public void ExecuteCommand(ClientManager clientManager, CommandProtocol.Command command) throws Exception {

        int requestId;

        try {
            GetRunningTasksCommandProtocol.GetRunningTasksCommand getRunningTasksCommand
                    = command.getGetRunningTasksCommand();

            requestId = (int) getRunningTasksCommand.getRequestId();

        } catch (Exception ex) {
            if (!super.immatureId.equals("-2")) {
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(ex, command.getRequestID()));
            }
            return;
        }

        try {
            ICommandExecuter tmpVar = clientManager.getCmdExecuter();
            TayzGrid nCache = (TayzGrid) (tmpVar instanceof TayzGrid ? tmpVar : null);

            ArrayList runningTasks = nCache.getCache().getRunningTasks();

            ResponseProtocol.Response response = ResponseProtocol.Response.newBuilder()
                    .setGetRunningTasksResponse(GetRunningTasksResponseProtocol.GetRunningTasksResponse.newBuilder()
                            .addAllRunningTasks((java.lang.Iterable<java.lang.String>) runningTasks))
                    .setRequestId(command.getRequestID())
                    .setResponseType(ResponseProtocol.Response.Type.GET_RUNNING_TASKS).build();

            _serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));

        } catch (Exception e) {
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(e, command.getRequestID()));
        }

    }

}
