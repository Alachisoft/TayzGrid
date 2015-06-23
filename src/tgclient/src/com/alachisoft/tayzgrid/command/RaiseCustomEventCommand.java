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
import com.alachisoft.tayzgrid.common.protobuf.RaiseCustomEventCommandProtocol;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.google.protobuf.ByteString;

import java.io.IOException;

public final class RaiseCustomEventCommand extends com.alachisoft.tayzgrid.command.Command {

    private java.lang.Object notifId;
    private Object _notifData = null;

    public RaiseCustomEventCommand(java.lang.Object key,
            java.lang.Object value, boolean async) {
        name = "RAISECUSTOMNOTIF";
        notifId = key;
        this._notifData = value;
        isAsync = async;
    }

    public void createCommand() throws CommandException {
        if (notifId == null) {
            throw new NullPointerException("notifId");
        }
        if (this._notifData == null) {
            throw new NullPointerException("value");
        }


        RaiseCustomEventCommandProtocol.RaiseCustomEventCommand.Builder builder =
                RaiseCustomEventCommandProtocol.RaiseCustomEventCommand.newBuilder();

        builder = builder.setRequestId(this.getRequestId())
                .setNotifIf(ByteString.copyFrom((byte[])this.notifId))
                .setData(ByteString.copyFrom((byte[])this._notifData));


        try {

            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setRaiseCustomEventCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.RAISE_CUSTOM_EVENT);

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());
        } catch (IOException ex) {
            throw new CommandException(ex.getMessage());
        }
    }

    protected boolean parseCommand() {
        return true;
    }

    public CommandType getCommandType() {
        return CommandType.RAISE_CUSTOM_EVENT;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.AtomicWrite;
    }
}
