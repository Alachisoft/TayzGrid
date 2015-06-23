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
import com.alachisoft.tayzgrid.common.protobuf.RegisterNotifCommandProtocol;
import java.io.IOException;


public final class RegisterNotification extends Command {

    private int modifiers;
    private int dataFilter;
    private short sequenceNumber;

    /** Creates a new instance of RegisterNotification */
    public RegisterNotification(int modifiers, int dataFilter, short sequenceNumber) {
        this.modifiers = modifiers;
        this.isAsync = false;
        this.dataFilter = dataFilter;
        this.sequenceNumber = sequenceNumber;
    }
    
    public RegisterNotification(int modifiers, short sequenceNumber)
    {
        this(modifiers, 0, sequenceNumber);
    }

    protected void createCommand() throws CommandException {
        RegisterNotifCommandProtocol.RegisterNotifCommand.Builder builder =
                RegisterNotifCommandProtocol.RegisterNotifCommand.newBuilder();

        builder = builder.setRequestId(this.getRequestId())
                .setNotifMask(modifiers)
                .setDatafilter(dataFilter)
                .setSequence(sequenceNumber);
        
        try {
            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setRegisterNotifCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.REGISTER_NOTIF);

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());

        } catch (IOException ex) {
            throw new CommandException(ex.getMessage());
        }
    }

    protected boolean parseCommand() {
        return true;
    }

    public int getModifiers() {
        return modifiers;
    }

    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }

    public CommandType getCommandType() {
        return CommandType.REGISTER_NOTIF;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.AtomicWrite;
    }
}
