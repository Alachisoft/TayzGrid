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


import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.RegisterKeyNotifCommandProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import java.io.IOException;



public class RegisterKeyNotification extends Command {

    int _updateCallbackId;
    int _removeCallabackId;
    boolean _notifyOnItemExpiration;
    int _dataFilter = -1;

    public RegisterKeyNotification(Object key, int updateCallbackid, int removeCallbackid, boolean notifyOnItemExpiration) {
        super.key = key;
        _updateCallbackId = updateCallbackid;
        _removeCallabackId = removeCallbackid;
        _notifyOnItemExpiration = notifyOnItemExpiration;
        name = "REGKEYNOTIF";
    }
    
    public RegisterKeyNotification(Object key, int updatedCallbackid, int removeCallbackid, EventDataFilter dataFilter, boolean notifyOnItemExpiration)
    {
        this(key, updatedCallbackid, removeCallbackid, notifyOnItemExpiration);
        _dataFilter = dataFilter.getValue();
    }

    protected void createCommand() throws CommandException {
        try {
            RegisterKeyNotifCommandProtocol.RegisterKeyNotifCommand.Builder builder =
                    RegisterKeyNotifCommandProtocol.RegisterKeyNotifCommand.newBuilder();

            builder = builder.setRequestId(this.getRequestId())
                    .setKey(CacheKeyUtil.toByteString(key, this.getCacheId()))
                    .setRemoveCallbackId(_removeCallabackId)
                    .setUpdateCallbackId(_updateCallbackId)
                    .setNotifyOnExpiration(_notifyOnItemExpiration);

            if(_dataFilter != -1)
                builder = builder.setDatafilter(_dataFilter);


            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setRegisterKeyNotifCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.REGISTER_KEY_NOTIF);

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());

        } catch (IOException ex) {
            throw new CommandException(ex.getMessage());
        }
    }

    protected boolean parseCommand() {
        return true;
    }

    public CommandType getCommandType() {
        return CommandType.REGISTER_KEY_NOTIF;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.AtomicWrite;
    }
}
