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

import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.BitSetConstants;
import com.alachisoft.tayzgrid.common.protobuf.BulkGetCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.web.caching.JCacheLoadAllItem;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;



public final class BulkGetCommand extends Command {

    private Object[] keys;
    private BitSet flagMap;
    private String providerName;
    
     
    private boolean replaceExistingValues = false;

    /** Creates a new instance of BulkGetCommand */
    public BulkGetCommand(Object[] keys, BitSet flagMap, String providerName, short jCacheCompletionListener, boolean replaceExistingValues, boolean isAsync) {
        this.keys = keys;
        this.flagMap = flagMap;
        this.providerName = providerName;
        
        this.isAsync = isAsync;
        super.asyncCallbackId = jCacheCompletionListener;
        this.replaceExistingValues = replaceExistingValues;
        super.asyncCallbackSpecified = this.isAsync && this.asyncCallbackId > -1;
        super.setBulkKeys(keys);
    }

    protected void createCommand() throws CommandException {
        if (keys == null) {
            throw new NullPointerException("keys");
        }
        if (keys.length == 0) {
            throw new IllegalArgumentException(
                    "There is no key present in keys array");
        }
        
        try {
            BulkGetCommandProtocol.BulkGetCommand.Builder builder = BulkGetCommandProtocol.BulkGetCommand.newBuilder();

            ArrayList<ByteString> list = new ArrayList<ByteString>();
            for (int i = 0; i < keys.length; i++)
            {
                if(keys[i] != null)
                    list.add(CacheKeyUtil.toByteString(keys[i], this.getCacheId()));

            }
            builder = builder.addAllKeys(list)
                    .setRequestId(this.getRequestId())
                    .setFlag(BitSetConstants.getBitSetData(this.flagMap))
                    .setJCacheListenerId(super.asyncCallbackId)
                    .setReplaceExistingValues(this.replaceExistingValues)
                    .setIsAsync(this.isAsync);

            if(providerName != null) builder = builder.setProviderName(providerName);

        
            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setBulkGetCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.GET_BULK)
                    .setIntendedRecipient(this.getIntendedRecipient())
                    .setClientLastViewId(this.getClientLastViewId())
                    .setCommandVersion(1);

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());
        } catch (IOException ex) {
            throw new CommandException(ex.getMessage());
        }
    }

    protected boolean parseCommand() {
        return false;
    }

    public CommandType getCommandType() {
         return CommandType.GET_BULK;
    }
    
    public int JCacheLoaderOpComplete()
    {
        return super.asyncCallbackId;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.BulkRead;
    }
}
