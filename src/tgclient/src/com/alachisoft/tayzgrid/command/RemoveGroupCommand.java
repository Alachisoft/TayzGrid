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

import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.RemoveGroupCommandProtocol;
import java.io.IOException;


 
public final class RemoveGroupCommand extends Command {

    private String group;
    private String subGroup;

    /**
     * Creates a new instance of GetCommand
     *
     * @param key
     * @param group
     * @param subGroup
     * @param isAsync
     */
    public RemoveGroupCommand(String group, String subGroup, boolean isAsync) {
        this.name = "REMOVEGROUP ";
        this.group = group;
        this.subGroup = subGroup;
        this.isAsync = isAsync;
    }

    /**
     *
     * @return
     */
    protected boolean parseCommand() {
        return true;
    }

    protected void createCommand() throws CommandException {
        if (group == null) {
            if (subGroup != null) {
                throw new IllegalArgumentException(
                        "group must be specified for sub group");
            }  
 
        }

        RemoveGroupCommandProtocol.RemoveGroupCommand.Builder builder =
                RemoveGroupCommandProtocol.RemoveGroupCommand.newBuilder();

        builder.setRequestId(this.getRequestId());

        if (group != null) {
            builder = builder.setGroup(group);
            if (subGroup != null) {
                builder = builder.setSubGroup(subGroup);
            }
        }


         
        try {
            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setRemoveGroupCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.REMOVE_GROUP)
                    .setClientLastViewId(this.getClientLastViewId());
            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());
 
        } catch (IOException ex) {
            throw new CommandException(ex.getMessage());
        }
    }

    public CommandType getCommandType() {
         return CommandType.REMOVE_GROUP;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.BulkWrite;
    }
}
