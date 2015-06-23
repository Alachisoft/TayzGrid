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
import com.alachisoft.tayzgrid.common.protobuf.GetCacheBindingCommandProtocol;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.google.protobuf.ByteString;
import java.io.IOException;



public class GetCacheBinding extends Command{
    
    String _cacheID;
    String _bindingIP;
    int _port;
    
    public GetCacheBinding(String id){
        
        name = "GetCacheBinding";
        _cacheID = id;
        
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.GETCACHEBINDING;
    }

    @Override
    public RequestType getCommandRequestType() {
        return RequestType.InternalCommand;
    }

    @Override
    protected void createCommand() throws CommandException {
        GetCacheBindingCommandProtocol.GetCacheBindingCommand.Builder  builder = GetCacheBindingCommandProtocol.GetCacheBindingCommand.newBuilder().setCacheId(_cacheID);
        
        try{
            
            CommandProtocol.Command.Builder commandBuilder = CommandProtocol.Command.newBuilder();
            commandBuilder.setGetCacheBindingCommand(builder).setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.GET_CACHE_BINDING);
            
            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());
        }
         catch (IOException ex) {
            throw new CommandException(ex.getMessage());
        }
        
    }

    @Override
    protected boolean parseCommand() {
        return true;
    }
    
}
