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
import com.alachisoft.tayzgrid.common.protobuf.ContainsCommandProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import java.io.IOException;


public final class ContainsCommand extends Command {

    /**
     * Creates a new instance of ContainsCommand
     *
     * @param key
     */
    public ContainsCommand(Object key) {
        this.key = key;
    }

    protected void createCommand() throws CommandException {
        if (key == null) {
            throw new NullPointerException("Key");
        }
        if (key.equals("")) {
            throw new IllegalArgumentException("key");
        }

        try {
            commandBytes = super.constructCommand(CommandProtocol.Command.newBuilder()
                    .setContainsCommand(ContainsCommandProtocol.ContainsCommand.newBuilder()
                                        .setKey(CacheKeyUtil.toByteString(key, this.getCacheId()))
                                        .setRequestId(this.getRequestId()))
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.CONTAINS).build().toByteArray());

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
         return CommandType.CONTAINS;
     }
     
     @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.AtomicRead;
    }
}
