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
import com.alachisoft.tayzgrid.common.protobuf.GetNextChunkCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.EnumerationPointerProtocol;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import java.io.IOException;


public class GetNextChunkCommand extends Command {

    private String id;
    private int chunckId;
    private boolean isDisposed;

    public GetNextChunkCommand(String id, int chunckId, boolean isDisposed) {
        this.id = id;
        this.chunckId = chunckId;
        this.isDisposed = isDisposed;
    }

    @Override
    protected void createCommand() throws CommandException {

        EnumerationPointerProtocol.EnumerationPointer.Builder pointerBuilder =
                EnumerationPointerProtocol.EnumerationPointer.newBuilder();
         pointerBuilder.setChunkId(chunckId)
                 .setId(id)
                 .setIsDisposed(isDisposed);

        GetNextChunkCommandProtocol.GetNextChunkCommand.Builder builder =
                GetNextChunkCommandProtocol.GetNextChunkCommand.newBuilder();

        builder = builder.setRequestId(this.getRequestId())
                .setEnumerationPointer(pointerBuilder);

        try {
            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setGetNextChunkCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.GET_NEXT_CHUNK);

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());
        } catch (IOException ex) {
            throw new CommandException(ex.getMessage());
        }
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.GET_NEXT_CHUNK;
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
