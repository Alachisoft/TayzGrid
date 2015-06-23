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
import com.alachisoft.tayzgrid.common.protobuf.GetTagCommandProtocol;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;

import com.alachisoft.tayzgrid.caching.TagComparisonType;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;


public class GetTagCommand extends Command {

    private String[] _tags;
    private TagComparisonType _comparisonType;

    public GetTagCommand(String[] tags, TagComparisonType comparisonType) {
        super.name = "GETTAG ";
        this._comparisonType = comparisonType;
        this._tags = tags;
    }

    @Override
    protected void createCommand() throws CommandException {
        GetTagCommandProtocol.GetTagCommand.Builder builder =
                GetTagCommandProtocol.GetTagCommand.newBuilder();

        builder.setRequestId(this.getRequestId())
                .addAllTags(Arrays.asList(this._tags))
                .setTagComparisonType(this._comparisonType.getValue());


        try {
            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setGetTagCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.GET_TAG)
                    .setClientLastViewId(this.getClientLastViewId())
                    .setCommandVersion(1);

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());

        } catch (IOException ex) {
            throw new CommandException(ex.getMessage());
        }

    }

    public String trimEnd(String str) {
        String returnString;
        int len = str.length();
        returnString = str.substring(0, len - 1);
        return returnString;
 
    }

    @Override
    protected boolean parseCommand() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public CommandType getCommandType() {
         return CommandType.GET_TAG;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.BulkRead;
    }
}
