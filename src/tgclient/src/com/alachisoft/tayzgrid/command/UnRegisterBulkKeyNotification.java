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
import com.alachisoft.tayzgrid.common.protobuf.UnRegisterBulkKeyNotifCommandProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import java.io.IOException;

 
public final class UnRegisterBulkKeyNotification extends Command {

    Object[] _keys;
    int _updateCallbackId;
    int _removeCallabackId;

    public UnRegisterBulkKeyNotification(Object[] keys, int updateCallbackid, int removeCallbackid) {

        _keys = keys;
        _updateCallbackId = updateCallbackid;
        _removeCallabackId = removeCallbackid;
        name = "BULKUNREGKEYNOTIF";
    }

    protected void createCommand() throws CommandException {

 
        UnRegisterBulkKeyNotifCommandProtocol.UnRegisterBulkKeyNotifCommand.Builder builder =
                UnRegisterBulkKeyNotifCommandProtocol.UnRegisterBulkKeyNotifCommand.newBuilder();

        builder = builder.setRequestId(this.getRequestId());

        if (_keys != null) {
 
            try {
                builder = builder.setRemoveCallbackId(_removeCallabackId)
                        .setUpdateCallbackId(_updateCallbackId);

                for (Object key : _keys) {
                    if (key == null) {
                        continue;
                    }
                    builder = builder.addKeys(CacheKeyUtil.toByteString(key, this.getCacheId()));

                }
            
                CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

                commandBuilder = commandBuilder.setUnRegisterBulkKeyNotifCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.UNREGISTER_BULK_KEY_NOTIF);

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
         return CommandType.UNREGISTER_BULK_KEY_NOTIF;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.BulkWrite;
    }
}
