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

package com.alachisoft.tayzgrid.command;

import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.MapReduceTaskCommandProtocol;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.web.mapreduce.MROutputOption;
import com.google.protobuf.ByteString;
import java.io.IOException;

public class MapReduceTaskCommand extends Command {

    byte[] mrTask = null;
    String taskId = null;
    short callbackId = 0;
    int outputOption = 0;
    
    public MapReduceTaskCommand(Object task, String taskId, MROutputOption outputOption, short callbackId)
     {
        if(task instanceof byte[])
            this.mrTask = (byte[]) task;
        if(taskId != null)
            this.taskId = taskId;
        this.outputOption = outputOption.getValue();
        if(callbackId != 0)
            this.callbackId = callbackId;
    }
    
    @Override
    public CommandType getCommandType() {
        return CommandType.MAPREDUCE_TASK;
    }

    @Override
    public RequestType getCommandRequestType() {
        return RequestType.AtomicRead;
    }

    @Override
    protected void createCommand() throws CommandException {
        try
        {            
            // TODO - InComplete :Farhan
            commandBytes = super.constructCommand(CommandProtocol.Command.newBuilder()
                .setMapReduceTaskCommand(MapReduceTaskCommandProtocol.MapReduceTaskCommand
                    .newBuilder().setRequestId(this.getRequestId())
                    .setTaskId(taskId)
                    .setCallbackId((int)callbackId)
                    .setMapReduceTask(ByteString.copyFrom(mrTask))
                    .setOutputOption(outputOption))
                .setRequestID(this.getRequestId())
                .setType(CommandProtocol.Command.Type.MAP_REDUCE_TASK).build().toByteArray());
        } catch (IOException ex) {
            throw new CommandException(ex.getMessage());
        }
    }

    @Override
    protected boolean parseCommand() {
        return true;
    }

}
