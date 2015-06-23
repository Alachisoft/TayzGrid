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
import com.alachisoft.tayzgrid.common.protobuf.InitCommandProtocol;
import com.google.protobuf.ByteString;
import java.io.IOException;


public final class InitCommand extends Command
{

    private String cacheId;
    private String clientId;

    private String licenseCode = "";
    private String _clientEditionId;

    /**
     * Creates a new instance of InitCommand
     *
     * @param id
     */
    public InitCommand(String id, String clientId, String licenseCode)
    {

        this.clientId = clientId;
        this.cacheId = id;
        this.licenseCode = licenseCode;
        super.name = "InitCommand";
        _clientEditionId = "JV-INITOS";
        




    }

    /**
     *
     * @return
     */
    protected boolean parseCommand()
    {
        return true;
    }

    public void createCommand() throws CommandException
    {


        InitCommandProtocol.InitCommand.Builder builder =
                InitCommandProtocol.InitCommand.newBuilder();

        builder = builder.setCacheId(cacheId).setRequestId(this.getRequestId())
                .setClientEditionId(_clientEditionId)
                .setClientId(clientId)
                .setIsDotnetClient(false)
                .setIsBridgeClient(false)
                .setLicenceCode(this.licenseCode)
                .setLicenceInfo("1,1,3.8,00c12610ec7c,,,,ata");

        try
        {

            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setInitCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.INIT);

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());

        }
        catch (IOException ex)
        {
            throw new CommandException(ex.getMessage());
        }
    }

    public CommandType getCommandType()
    {
        return CommandType.INIT;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.InternalCommand;
    }
}
