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
import com.alachisoft.tayzgrid.common.protobuf.InvokeEntryProcessorProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public final class InvokeEntryProcessorCommand extends Command
{
    private final Object[] keys;
    private final byte[] entryProcessor;
    private final List<byte[]> arguments;
    private final String defaultReadThru;
    private final String defaultWriteThru;

    
    public InvokeEntryProcessorCommand(Object[] keys, byte[] ep, List<byte[]> arguments, String defaultReadThru, String defaultWriteThru) {
       this.keys = keys;        
        this.entryProcessor=ep;
        this.arguments=arguments;
        this.defaultReadThru=defaultReadThru;
        this.defaultWriteThru=defaultWriteThru;
        
        super.name = "INVOKEENTRYPROCESSOR";
    }

    @Override
    protected void createCommand() throws CommandException {
        if (keys == null) {
            throw new NullPointerException("keys");
        }
        if (keys.length == 0) {
            throw new IllegalArgumentException(
                    "There is no key present in keys array");
        }
        
        try {
            InvokeEntryProcessorProtocol.InvokeEntryProcessorCommand.Builder builder = InvokeEntryProcessorProtocol.InvokeEntryProcessorCommand.newBuilder();

            ArrayList<ByteString> keyList =(ArrayList<ByteString>) CacheKeyUtil.toByteStrings(keys, this.getCacheId());

            ArrayList<ByteString> argumentsList =null;
            
            if (arguments != null && arguments.size() > 0) 
            {
                 argumentsList = new ArrayList<ByteString>();
                for (byte[] argument : arguments) {
                    
                    if (null != argument)                     
                        argumentsList.add(ByteString.copyFrom(argument));
                    
                }
            }
            builder = builder.addAllKeys(keyList)
                    .setEntryprocessor(ByteString.copyFrom(entryProcessor));

            if(argumentsList!=null&& argumentsList.size()>0)builder = builder.addAllArguments(argumentsList);

            
            if(defaultReadThru != null) builder = builder.setDefaultReadThru(defaultReadThru);
            if(defaultWriteThru != null) builder = builder.setDefaultWriteThru(defaultWriteThru);

        
            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setInvokeEntryProcessorCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.INVOKE_ENTRYPROCESSOR)
                    .setIntendedRecipient(this.getIntendedRecipient())
                    .setClientLastViewId(this.getClientLastViewId())
                    .setCommandVersion(1);

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());
        } catch (IOException ex) {
            throw new CommandException(ex.getMessage());
        }
    }

    /**
     *
     * @return
     */
    @Override
    protected boolean parseCommand()
    {
        return true;
    }

    @Override
    public CommandType getCommandType()
    {
        return CommandType.INVOKE_ENTRYPROCESSOR;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.AtomicWrite;
    }
}
