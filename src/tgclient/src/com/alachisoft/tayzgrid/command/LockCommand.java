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
import com.alachisoft.tayzgrid.common.protobuf.LockCommandProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.web.caching.Cache;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.io.IOException;


public class LockCommand extends Command {

    private TimeSpan _lockDate;

    public LockCommand(Object key, TimeSpan lockDate) {
        super.name = "LOCK ";
        super.key = key;
        this._lockDate = lockDate;
    }

    @Override
    protected void createCommand() throws CommandException {
        long ticks = 0;
        if (this._lockDate != Cache.NoLockingExpiration) {
            
            ticks = this._lockDate.getTotalTicks();
        }
        try {
            LockCommandProtocol.LockCommand.Builder builder =
                    LockCommandProtocol.LockCommand.newBuilder();

            builder = builder.setRequestId(this.getRequestId())
                    .setKey(CacheKeyUtil.toByteString(key, this.getCacheId()))
                    .setLockTimeout(ticks);


            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setLockCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.LOCK);

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());

        } catch (IOException ex) {
            throw new CommandException(ex.getMessage());
        }
    }

    @Override
    protected boolean parseCommand() {
        return true;
    }

    public CommandType getCommandType() {
         return CommandType.LOCK;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.AtomicWrite;
    }
}
