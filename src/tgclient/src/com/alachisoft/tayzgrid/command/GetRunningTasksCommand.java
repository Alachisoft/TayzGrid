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
import com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksCommandProtocol;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;

public class GetRunningTasksCommand extends Command {

    @Override
    public CommandType getCommandType() {
        return CommandType.GET_RUNNING_TASKS;
    }

    @Override
    public RequestType getCommandRequestType() {
        return RequestType.AtomicRead;
    }

    @Override
    protected void createCommand() throws CommandException {
        try {
            commandBytes = super.constructCommand(CommandProtocol.Command.newBuilder()
                    .setGetRunningTasksCommand(
                            GetRunningTasksCommandProtocol.GetRunningTasksCommand.newBuilder()
                            .setRequestId(this.getRequestId()))
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.GET_RUNNING_TASKS).build().toByteArray());

        } catch (Exception ex) {
            throw new CommandException(ex.getMessage());
        }
    }

    @Override
    protected boolean parseCommand() {
        return true;
    }

}
