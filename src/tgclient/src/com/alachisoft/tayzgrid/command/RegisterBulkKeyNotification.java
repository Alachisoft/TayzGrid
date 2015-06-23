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
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.RegisterBulkKeyNotifCommandProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import java.io.IOException;



public class RegisterBulkKeyNotification extends Command {

    Object[] _keys;
    int _updateCallbackId;
    int _removeCallabackId;
    boolean _notifyOnItemExpiration;
    int _dataFilter = -1;

    public RegisterBulkKeyNotification(Object[] keys, int updateCallbackid, int removeCallbackid) {
        _keys = keys;
        _updateCallbackId = updateCallbackid;
        _removeCallabackId = removeCallbackid;
        name = "BULKREGKEYNOTIF";

    }
    
    public RegisterBulkKeyNotification(Object[] keys, int updateCallbackid, int removeCallbackid, EventDataFilter eventDataFilter, boolean notifyOnItemExpiration) {
        _keys = keys;
        _updateCallbackId = updateCallbackid;
        _removeCallabackId = removeCallbackid;
        _dataFilter = eventDataFilter.getValue();
        _notifyOnItemExpiration = notifyOnItemExpiration;
        name = "BULKREGKEYNOTIF";

    }

    protected void createCommand() throws CommandException {

        RegisterBulkKeyNotifCommandProtocol.RegisterBulkKeyNotifCommand.Builder builder =
                RegisterBulkKeyNotifCommandProtocol.RegisterBulkKeyNotifCommand.newBuilder();

        builder = builder.setRequestId(this.getRequestId())
                .setRemoveCallbackId(_removeCallabackId)
                .setUpdateCallbackId(_updateCallbackId)
                .setDatafilter(_dataFilter)
                .setNotifyOnExpiration(_notifyOnItemExpiration);

        if (_keys != null) {

            try {
                for (Object key : _keys) {
                    if (key == null) {
                        continue;
                    }
                    builder = builder.addKeys(CacheKeyUtil.toByteString(key, this.getCacheId()));

                }
            
                CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

                commandBuilder = commandBuilder.setRegisterBulkKeyNotifCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.REGISTER_BULK_KEY_NOTIF);

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());

            } catch (IOException ex) {
                throw new CommandException(ex.getMessage());
            }
        }
    }

    protected boolean parseCommand() {
        return true;
    }

    public CommandType getCommandType() {
        return CommandType.REGISTER_BULK_KEY_NOTIF;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.BulkWrite;
    }
}
