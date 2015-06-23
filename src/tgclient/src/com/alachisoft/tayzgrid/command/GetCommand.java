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

import com.alachisoft.tayzgrid.web.caching.Cache;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.LockInfoProtocol;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.BitSetConstants;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import java.io.IOException;


public final class GetCommand extends Command {

    private String group;
    private String subGroup;
    private int flagMap;
    private com.alachisoft.tayzgrid.caching.LockAccessType _accessType;
    private String _lockId;
    private TimeSpan _lockTimeout;
    private long _version;
    
    private String providerName;
    
    /**
     * Creates a new instance of GetCommand
     *
     * @param key
     * @param group
     * @param subGroup
     * @param isAsync
     */
    public GetCommand(Object key, BitSet flagMap, String group, String subGroup, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String lockId, TimeSpan lockTimeout, long version, boolean isAsync, String providerName) {
        this.name = "GET ";
        this.key = key;
        this.group = group;
        this.subGroup = subGroup;
        this.isAsync = isAsync;
        this.flagMap = BitSetConstants.getBitSetData(flagMap);
        this._accessType = accessType;
        this._lockId = lockId;
        this._lockTimeout = lockTimeout;
        this._version = version;
        this.providerName = providerName;
    }

    /**
     *
     * @return
     */
    protected boolean parseCommand() {
        return true;
    }

    protected void createCommand() throws CommandException {
        if (key == null) {
            throw new NullPointerException("Key");
        }
        if (key.equals("")) {
            throw new IllegalArgumentException("key");
        }
        if (group == null) {
            if (subGroup != null) {
                throw new NullPointerException(
                        "group must be specified for sub group");
            } 
        }
    
        long ticks = 0;
        if (this._lockTimeout != Cache.NoLockingExpiration) {
            
            ticks = this._lockTimeout.getTotalTicks();
            if (ticks < 0) {
                ticks = 0;
            }
        }
        
        try {
            GetCommandProtocol.GetCommand.Builder builder =
                    GetCommandProtocol.GetCommand.newBuilder();

            builder = builder.setFlag(this.flagMap)
                    .setKey(CacheKeyUtil.toByteString(key, this.getCacheId()))
                    .setRequestId(this.getRequestId())
                    .setVersion(this._version);

            LockInfoProtocol.LockInfo.Builder lockInfoBuilder =
                    LockInfoProtocol.LockInfo.newBuilder();

            if(this._lockId != null){
                lockInfoBuilder = lockInfoBuilder.setLockId(this._lockId);
            }
            lockInfoBuilder = lockInfoBuilder.setLockAccessType(this._accessType.getValue()).setLockTimeout(ticks);
            builder = builder.setLockInfo(lockInfoBuilder);

            if (group != null) {
                builder = builder.setGroup(group);
                if (subGroup != null) {
                    builder = builder.setSubGroup(subGroup);
                }
            }

            if(providerName != null)
            {
                 builder = builder.setProviderName(providerName);
            }



            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setGetCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.GET);

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());

        } catch (IOException ex) {
            throw new CommandException(ex.getMessage());
        }
    }

    public CommandType getCommandType() {
         return CommandType.GET;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.AtomicRead;
    }
}
