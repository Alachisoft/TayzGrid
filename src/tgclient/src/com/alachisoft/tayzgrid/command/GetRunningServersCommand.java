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
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.common.protobuf.GetRunningServersCommandProtocol;
import com.google.protobuf.ByteString;
import java.io.IOException;


public class GetRunningServersCommand extends Command {


        String cacheid;
        
        public GetRunningServersCommand(String id) {
		name = "GetRunningServersCommand";
		this.cacheid = id;

	}
        
    @Override
    protected void createCommand() throws CommandException {
        
        GetRunningServersCommandProtocol.GetRunningServersCommand.Builder builder =
                        GetRunningServersCommandProtocol.GetRunningServersCommand.newBuilder()
                        .setCacheId(this.cacheid);

                        
        try {
             CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

                commandBuilder = commandBuilder.setGetRunningServersCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.GET_RUNNING_SERVERS);
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
         return CommandType.GET_RUNNING_SERVERS;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.InternalCommand;
    }
}
