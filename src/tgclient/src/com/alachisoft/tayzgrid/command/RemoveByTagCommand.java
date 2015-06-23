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
import com.alachisoft.tayzgrid.common.protobuf.RemoveByTagCommandProtocol;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;

import com.alachisoft.tayzgrid.caching.TagComparisonType;
import java.io.IOException;

public final class RemoveByTagCommand extends Command {

    private String[] tags;
    private  TagComparisonType comaprisonType;


    public RemoveByTagCommand(String[] tags, TagComparisonType comaprisonType)
    {
        this.name = "REMOVE_BY_TAG";
        this.tags = tags;
        this.comaprisonType = comaprisonType;
    }

    @Override
    protected void createCommand() throws CommandException
    {
        RemoveByTagCommandProtocol.RemoveByTagCommand.Builder builder =
                RemoveByTagCommandProtocol.RemoveByTagCommand.newBuilder();

        builder = builder.setRequestId(this.getRequestId())
                .setTagComparisonType(comaprisonType.getValue());

        for(int index=0; index < tags.length; index++)
        {
            builder = builder.addTags(tags[index]);
        }

        try
        {
            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();
            commandBuilder = commandBuilder.setRemoveByTagCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.REMOVE_BY_TAG)
                    .setClientLastViewId(super.getClientLastViewId());
            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());
        }
        catch (IOException ex)
        {
            throw new CommandException(ex.getMessage());
        }
    }

    @Override
    protected boolean parseCommand() {
        return true;
    }

    public CommandType getCommandType() {
        return CommandType.REMOVE_BY_TAG;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.BulkWrite;
    }

}
