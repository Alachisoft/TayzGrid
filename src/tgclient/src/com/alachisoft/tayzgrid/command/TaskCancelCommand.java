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
import com.alachisoft.tayzgrid.common.protobuf.TaskCancelCommandProtocol;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import java.io.IOException;

public class TaskCancelCommand extends Command {

    String _taskId = "";
    boolean _cancelAll = false;
    
    public TaskCancelCommand(String taskId, boolean cancellAll) {
        if(taskId != null)
            this._taskId = taskId;
        this._cancelAll = cancellAll;
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.MAPREDUCE_TASK_CANCEL;
    }

    @Override
    public RequestType getCommandRequestType() {
        return RequestType.AtomicRead;
    }

    @Override
    protected void createCommand() throws CommandException {

        try {
            if(_cancelAll) {
                
                commandBytes = super.constructCommand(
                    CommandProtocol.Command.newBuilder()
                    .setTaskCancelCommand(
                            TaskCancelCommandProtocol.TaskCancelCommand.newBuilder()
                            .setTaskId(_taskId)
                            .setCancelAll(true))
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.MAP_REDUCE_TASK_CANCEL)
                    .build().toByteArray());
            } else {
                
                commandBytes = super.constructCommand(
                    CommandProtocol.Command.newBuilder()
                    .setTaskCancelCommand(
                            TaskCancelCommandProtocol.TaskCancelCommand.newBuilder()
                            .setTaskId(_taskId))
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.MAP_REDUCE_TASK_CANCEL)
                    .build().toByteArray());
            }
            
        } catch (IOException ex) {
            throw new CommandException(ex.getMessage());
        }
    }

    @Override
    protected boolean parseCommand() {
        return true;
    }

}
