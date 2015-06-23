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
import com.alachisoft.tayzgrid.common.protobuf.GetGroupNextChunkCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GroupEnumerationPointerProtocol;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import java.io.IOException;

/**
 *
 * 
 */
public class GetGroupNextChunkCommand extends Command {

    private String group;
    private String subGroup;
    private String id;
    private int chunckId;

    public GetGroupNextChunkCommand(String id, int chunckId, String group, String subGroup) {
        this.group = group;
        this.subGroup = subGroup;
        this.id = id;
        this.chunckId = chunckId;
    }

    @Override
    protected void createCommand() throws CommandException {
         if (group == null) {
            if (subGroup != null) {
                throw new NullPointerException(
                        "group must be specified for sub group");
            } else {
                subGroup = "";
            }
            group = "";
        }

         GroupEnumerationPointerProtocol.GroupEnumerationPointer.Builder pointerBuilder =
                GroupEnumerationPointerProtocol.GroupEnumerationPointer.newBuilder();
         pointerBuilder.setChunkId(chunckId)
                 .setId(id);

          if (group != null) {
            pointerBuilder = pointerBuilder.setGroup(group);
            if (subGroup != null) {
                pointerBuilder = pointerBuilder.setSubGroup(subGroup);
            }
        }

        GetGroupNextChunkCommandProtocol.GetGroupNextChunkCommand.Builder builder =
                GetGroupNextChunkCommandProtocol.GetGroupNextChunkCommand.newBuilder();

        builder = builder.setRequestId(this.getRequestId())
                .setGroupEnumerationPointer(pointerBuilder);

        try {
            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setGetGroupNextChunkCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.GET_GROUP_NEXT_CHUNK);

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());
        } catch (IOException ex) {
            throw new CommandException(ex.getMessage());
        }
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.GETGROUP_NEXT_CHUNK;
    }

    @Override
    protected boolean parseCommand() {
        return true;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.ChunkRead;
    }

}
