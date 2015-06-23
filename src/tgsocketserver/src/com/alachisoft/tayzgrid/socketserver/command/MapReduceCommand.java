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
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.MapReduceTaskCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.MapReduceTaskResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.mapreduce.notification.TaskCallbackInfo;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentNullException;
import com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceTask;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.ICommandExecuter;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MapReduceCommand extends CommandBase {

    @Override
    public void ExecuteCommand(ClientManager clientManager, CommandProtocol.Command command) throws Exception {

        String taskId = "";
        short callbackId = 0;
        byte[] serializedTask = null;

        try {
            MapReduceTaskCommandProtocol.MapReduceTaskCommand mapReduceCommand
                    = command.getMapReduceTaskCommand();
            //cmdInfo.requestId = (new Long(command.getRequestID())).toString();

            if(mapReduceCommand.getMapReduceTask() != null)
                serializedTask = mapReduceCommand.getMapReduceTask().toByteArray();
            if(mapReduceCommand.getTaskId() != null || !mapReduceCommand.getTaskId().equals(""))
                taskId = mapReduceCommand.getTaskId();
            if(mapReduceCommand.getCallbackId() != 0)
                callbackId = (short)mapReduceCommand.getCallbackId();
            
        } catch (Exception exc) {
            if (!super.immatureId.equals("-2")) {
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
            }
            return;
        }

        try {
            ICommandExecuter tmpVar = clientManager.getCmdExecuter();
            TayzGrid nCache = (TayzGrid) (tmpVar instanceof TayzGrid ? tmpVar : null);
                   
            //Deserlize Filter,Mapper,Combiner,Reducer
            MapReduceTask taskk = getMapReduceTask(taskId, serializedTask, nCache.getCache().getName());
           
            if(taskk != null) {

                nCache.getCache().submitMapReduceTask(taskk, taskId, new TaskCallbackInfo(clientManager.getClientID(),callbackId),

                        new OperationContext(OperationContextFieldName.OperationType,
                                             OperationContextOperationType.CacheOperation));
            }
            
            ResponseProtocol.Response response = ResponseProtocol.Response.newBuilder()
                    .setMapReduceTaskResponse(MapReduceTaskResponseProtocol.MapReduceTaskResponse.newBuilder())
                    .setRequestId(command.getRequestID())
                    .setResponseType(ResponseProtocol.Response.Type.MAP_REDUCE_TASK).build();

            _serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));

        } catch (Exception e) {
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(e, command.getRequestID()));
        }

    }
    
    public MapReduceTask getMapReduceTask(String taskId, byte[] taskArray, String cacheName) throws IOException, ClassNotFoundException
    {
        MapReduceTask task = null;
        
        // may throw Exception
        task = (MapReduceTask) CompactBinaryFormatter.fromByteBuffer(taskArray, cacheName);
//        task.setTaskID(taskId);
        
        return task;
    }

}
