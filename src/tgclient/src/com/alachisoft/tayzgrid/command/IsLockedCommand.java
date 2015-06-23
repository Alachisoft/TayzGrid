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
import com.alachisoft.tayzgrid.common.protobuf.IsLockedCommandProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import java.io.IOException;

public class IsLockedCommand extends Command {

    private String _lockId;

     public IsLockedCommand(Object key, String lockId) {
        super.name = "ISLOCKED ";
        this.key = key;
        this._lockId = lockId;
    }
    @Override
    protected void createCommand() throws CommandException {
        try {
            IsLockedCommandProtocol.IsLockedCommand.Builder builder =
                    IsLockedCommandProtocol.IsLockedCommand.newBuilder();

            builder = builder.setRequestId(this.getRequestId())
                    .setKey(CacheKeyUtil.toByteString(key, this.getCacheId()));

            if(_lockId != null)
            {
                    builder = builder.setLockId(_lockId);
            }


            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setIsLockedCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.ISLOCKED);

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
         return CommandType.ISLOCKED;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.AtomicRead;
    }

}
