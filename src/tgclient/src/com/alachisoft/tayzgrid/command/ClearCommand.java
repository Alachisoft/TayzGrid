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
import com.alachisoft.tayzgrid.common.protobuf.ClearCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.BitSetConstants;
import java.io.IOException;



public final class ClearCommand extends Command {

    /**
     * Creates a new instance of ClearCommand
     *
     * @param isAsync
     */
    private int flagMap;
    private int onDsClearedId;
    private String providerName;

    public ClearCommand(boolean isAsync, BitSet flagMap, int asyncCallback, int onDsClearedId, String providerName) {

        super.asyncCallbackId = asyncCallback;
        super.isAsync = isAsync;
        this.asyncCallbackSpecified = this.isAsync && super.asyncCallbackId != -1 ? true : false;
        this.onDsClearedId = onDsClearedId;
        this.flagMap = BitSetConstants.getBitSetData(flagMap);
        this.providerName = providerName;
    }

    protected void createCommand() throws CommandException {
        ClearCommandProtocol.ClearCommand.Builder builder =
                ClearCommandProtocol.ClearCommand.newBuilder();

        builder.setDatasourceClearedCallbackId(this.onDsClearedId)
                .setRequestId(this.getRequestId())
                .setFlag(flagMap)
                .setIsAsync(isAsync);

        if(providerName != null && !providerName.equals("")) {
            builder.setProviderName(providerName);
        }

        try {

            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setClearCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.CLEAR);


            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());

        } catch (IOException ex) {
            throw new CommandException(ex.getMessage());
        }
    }

    /**
     *
     * @return
     */
    protected boolean parseCommand() {
        return true;
    }

    public CommandType getCommandType() {
        return CommandType.CLEAR;
    }

    public int AsyncCacheClearedOpComplete()
    {
        return super.asyncCallbackId;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.AtomicWrite;
    }
}
