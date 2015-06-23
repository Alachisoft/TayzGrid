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
import com.alachisoft.tayzgrid.common.protobuf.GetOptimalServerCommandProtocol;
import java.io.IOException;


public final class GetOptimalServer extends Command
{

    private String cacheId;

    /**
     * Creates a new instance of GetOptimalServer
     *
     * @param id
     */
    public GetOptimalServer(String id)
    {
        name = "GETOPTIMALSERVER";
        this.cacheId = id;
    }

    /**
     *
     * @return
     */
    @Override
    protected boolean parseCommand()
    {
        return true;
    }

    @Override
    protected void createCommand() throws CommandException
    {

        GetOptimalServerCommandProtocol.GetOptimalServerCommand.Builder builder =
                GetOptimalServerCommandProtocol.GetOptimalServerCommand.newBuilder();

        builder = builder.setRequestId(this.getRequestId())
                .setIsDotnetClient(false)
                .setCacheId(cacheId);

        try
        {
            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setGetOptimalServerCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.GET_OPTIMAL_SERVER);

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());

        }
        catch (IOException ex)
        {
            throw new CommandException(ex.getMessage());
        }
    }

    /**
     *
     * @return
     */
    @Override
    public CommandType getCommandType()
    {
        return CommandType.GET_OPTIMAL_SERVER;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.InternalCommand;
    }
}
