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
import com.alachisoft.tayzgrid.common.protobuf.UnlockCommandProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import java.io.IOException;

public final class UnlockCommand extends Command {

    private String _lockId;
    private boolean _preemptive;

    public UnlockCommand(Object key) {
        super.name = "UNLOCK ";
        super.key = key;
        this._preemptive = true;
    }

    public UnlockCommand(Object key, String lockId) {
        super.name = "UNLOCK ";
        super.key = key;
        this._lockId = lockId;
        this._preemptive = false;
    }

    @Override
    protected void createCommand() throws CommandException {
 
        this._lockId = (this._lockId == null) ? "" : this._lockId;
        
        try {
            UnlockCommandProtocol.UnlockCommand.Builder builder =
                    UnlockCommandProtocol.UnlockCommand.newBuilder();

            builder.setRequestId(this.getRequestId())
                    .setKey(CacheKeyUtil.toByteString(key, this.getCacheId()))
                    .setLockId(this._lockId)
                    .setPreemptive(this._preemptive);


            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setUnlockCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.UNLOCK);

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());
 
        } catch (IOException e) {
            throw new CommandException(e.getMessage());
        }
    }

    @Override
    protected boolean parseCommand() {
        return true;
    }

    public CommandType getCommandType() {
         return CommandType.UNLOCK;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.AtomicWrite;
    }
}
