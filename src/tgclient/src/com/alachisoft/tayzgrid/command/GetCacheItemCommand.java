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
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetCacheItemCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.LockInfoProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.web.caching.Cache;
import com.alachisoft.tayzgrid.web.caching.CacheItemVersion;
import com.alachisoft.tayzgrid.web.caching.DSReadOption;
import com.alachisoft.tayzgrid.web.caching.LockHandle;
import java.io.IOException;

public final class GetCacheItemCommand extends Command
{

    private String group;
    private String subGroup;
    private com.alachisoft.tayzgrid.caching.LockAccessType _accessType;
    private String _lockId;
    private TimeSpan _lockTimeout;
    private String _providerName;
   // private DSReadOption _dsReadOptions;
    private CacheItemVersion _version;
    private int _flagMap;

    /**
     * Creates a new instance of GetCommand
     *
     * @param key
     * @param group
     * @param subGroup
     */
    public GetCacheItemCommand(Object key, String group, String subGroup, com.alachisoft.tayzgrid.caching.LockAccessType accessType, LockHandle lock, TimeSpan lockTimeout, CacheItemVersion cacheItemVersion, String providerName, BitSet flagMap)
    {
        this.name = "GETCACHEITEM ";
        this.key = key;
        this.group = group;
        this.subGroup = subGroup;
        this._accessType = accessType;
        this._lockId = lock.getLockId();
        this._lockTimeout = lockTimeout;
        this._providerName = providerName;
        this._flagMap = BitSetConstants.getBitSetData(flagMap);
        this._version = cacheItemVersion;
    }

    /**
     *
     * @return
     */
    protected boolean parseCommand()
    {
        return true;
    }

    protected void createCommand() throws CommandException
    {
        if (key == null)
        {
            throw new NullPointerException("Key");
        }
        if (key.equals(""))
        {
            throw new IllegalArgumentException("key");
        }

        if (group == null)
        {
            if (subGroup != null)
            {
                throw new NullPointerException(
                        "group must be specified for sub group");
            }
            else
            {
                subGroup = "";
            }
            group = "";
        }

        try
        {
            GetCacheItemCommandProtocol.GetCacheItemCommand.Builder builder =
                    GetCacheItemCommandProtocol.GetCacheItemCommand.newBuilder();

            builder = builder.setKey(CacheKeyUtil.toByteString(key, this.getCacheId())).setRequestId(this.getRequestId());

            if (group != null)
            {
                builder = builder.setGroup(group);
                if (subGroup != null)
                {
                    builder = builder.setSubGroup(subGroup);
                }
            }

            long ticks = 0;
            if (this._lockTimeout != Cache.NoLockingExpiration)
            {

                ticks = this._lockTimeout.getTotalTicks();
                if (ticks < 0)
                {
                    ticks = 0;
                }
            }

            LockInfoProtocol.LockInfo.Builder lockInfoBuilder =
                    LockInfoProtocol.LockInfo.newBuilder();

            if (this._lockId != null)
            {
                lockInfoBuilder = lockInfoBuilder.setLockId(this._lockId);
            }

            if(this._version != null)
                builder = builder.setVersion(this._version.getVersion());

            if(this._providerName != null)
                builder = builder.setProviderName(this._providerName);
            

//            if (_dsReadOptions != DSReadOption.None)
//            {
//                builder = builder.setFlag(16);
//            }
            builder.setFlag(_flagMap);

            lockInfoBuilder = lockInfoBuilder.setLockAccessType(this._accessType.getValue()).setLockTimeout(ticks);
            builder = builder.setLockInfo(lockInfoBuilder);



            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setGetCacheItemCommand(builder).setRequestID(this.getRequestId()).setType(CommandProtocol.Command.Type.GET_CACHE_ITEM);

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());
        }
        catch (IOException ex)
        {
            throw new CommandException(ex.getMessage());
        }
    }

    public CommandType getCommandType() {
         return CommandType.GET_CACHE_ITEM;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.AtomicRead;
    }
}
