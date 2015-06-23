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

package com.alachisoft.tayzgrid.socketserver.command;

import com.alachisoft.tayzgrid.socketserver.ICommandExecuter;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.alachisoft.tayzgrid.caching.autoexpiration.FixedExpiration;
import com.alachisoft.tayzgrid.caching.autoexpiration.AggregateExpirationHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.IdleExpiration;
import com.alachisoft.tayzgrid.common.protobuf.GetCacheItemCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetCacheItemResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.BitSetConstants;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.DataSourceReadOptions;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;

import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;

import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.runtime.util.HelperFxn;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.google.protobuf.ByteString;
import java.io.IOException;

public class GetCacheItemCommand extends CommandBase
{
    private final static class CommandInfo
    {

        public String RequestId;
        public Object Key;
        public String Group;
        public String SubGroup;
        public BitSet FlagMap;
        public LockAccessType LockAccessTypes = LockAccessType.values()[0];
        public Object LockId;
        public TimeSpan LockTimeout = new TimeSpan();
        public long CacheItemVersion;
        public String ProviderName;

        public CommandInfo clone()
        {
            CommandInfo varCopy = new CommandInfo();

            varCopy.RequestId = this.RequestId;
            varCopy.Key = this.Key;
            varCopy.Group = this.Group;
            varCopy.SubGroup = this.SubGroup;
            varCopy.FlagMap = this.FlagMap;
            varCopy.LockAccessTypes = this.LockAccessTypes;
            varCopy.LockId = this.LockId;
            varCopy.LockTimeout = this.LockTimeout;
            varCopy.CacheItemVersion = this.CacheItemVersion;
            varCopy.ProviderName = this.ProviderName;

            return varCopy;
        }
    }

    @Override
    public boolean getCanHaveLargedata()
    {
        return true;
    }

    //PROTOBUF
    @Override
    public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command)
    {
        CommandInfo cmdInfo = new CommandInfo();
        ICommandExecuter tempVar = clientManager.getCmdExecuter();
        TayzGrid nCache = (TayzGrid) ((tempVar instanceof TayzGrid) ? tempVar : null);

        try
        {
            cmdInfo = ParseCommand(command, clientManager, nCache.getCacheId()).clone();
        }
        catch (Exception exc)
        {
            if (!super.immatureId.equals("-2"))
            {
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
            }
            return;
        }

        
        try
        {
            Object lockId = cmdInfo.LockId;
            long version = cmdInfo.CacheItemVersion;
            DataSourceReadOptions readOptions = DataSourceReadOptions.None;
            NCDateTime time = new NCDateTime(1970, 1, 1, 0, 0, 0, 0);
            java.util.Date lockDate = time.getDate();
            OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
//            operationContext.Add(OperationContextFieldName.ReadThru, cmdInfo.FlagMap.IsBitSet((byte) BitSetConstants.ReadThru));
            operationContext.Add(OperationContextFieldName.ReaderBitsetEnum, cmdInfo.FlagMap);
             
            operationContext.Add(OperationContextFieldName.GenerateQueryInfo, true);
            if (cmdInfo.ProviderName != null)
            {
                operationContext.Add(OperationContextFieldName.ReadThruProviderName, cmdInfo.ProviderName);
            }
            tangible.RefObject<Long> tempRef_version = new tangible.RefObject<Long>(version);
            tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
            tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
            CacheEntry entry = (CacheEntry) nCache.getCache().GetCacheEntry(cmdInfo.Key, cmdInfo.Group, cmdInfo.SubGroup, tempRef_lockId, tempRef_lockDate, cmdInfo.LockTimeout, tempRef_version, cmdInfo.LockAccessTypes, operationContext);
            lockId = tempRef_lockId.argvalue;
            lockDate = tempRef_lockDate.argvalue;

            GetCacheItemResponseProtocol.GetCacheItemResponse.Builder build = GetCacheItemResponseProtocol.GetCacheItemResponse.newBuilder();

            ResponseProtocol.Response.Builder resBuilder = ResponseProtocol.Response.newBuilder();

            resBuilder.setRequestId(Long.parseLong(cmdInfo.RequestId));
            resBuilder.setResponseType(ResponseProtocol.Response.Type.GET_CACHE_ITEM);


            if (entry == null)
            {
                build.setLockId(lockId == null ? "" : lockId.toString());
                build.setLockTicks(new NCDateTime(lockDate).getTicks());
                GetCacheItemResponseProtocol.GetCacheItemResponse getCacheItemResponse = build.build();
                resBuilder.setGetItem(getCacheItemResponse);
                ResponseProtocol.Response response = resBuilder.build();
                _serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));
                return;
            }
            build = PopulateResponse(entry, build, clientManager, nCache.getCacheId());
            GetCacheItemResponseProtocol.GetCacheItemResponse getCacheItemResponse = build.build();
            resBuilder.setGetItem(getCacheItemResponse);

            ResponseProtocol.Response response = resBuilder.build();
            _serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));
        }
        catch (Exception exc)
        {
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
        }
    }

    private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager, String serializationContext) throws IOException, ClassNotFoundException
    {
        CommandInfo cmdInfo = new CommandInfo();

        GetCacheItemCommandProtocol.GetCacheItemCommand getCacheItemCommand = command.getGetCacheItemCommand();

        cmdInfo.CacheItemVersion = getCacheItemCommand.getVersion();
        cmdInfo.FlagMap = new BitSet((byte) getCacheItemCommand.getFlag());

        cmdInfo.Group = getCacheItemCommand.getGroup().length() == 0 ? null : getCacheItemCommand.getGroup();
        cmdInfo.Key = CacheKeyUtil.Deserialize(getCacheItemCommand.getKey(), serializationContext);
        cmdInfo.LockAccessTypes = LockAccessType.forValue(getCacheItemCommand.getLockInfo().getLockAccessType());
        cmdInfo.LockId = getCacheItemCommand.getLockInfo().getLockId();
        cmdInfo.LockTimeout = new TimeSpan(getCacheItemCommand.getLockInfo().getLockTimeout());
        cmdInfo.ProviderName = getCacheItemCommand.getProviderName().length() == 0 ? null : getCacheItemCommand.getProviderName();
        cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
        cmdInfo.SubGroup = getCacheItemCommand.getSubGroup().length() == 0 ? null : getCacheItemCommand.getSubGroup();


        return cmdInfo;
    }

    private GetCacheItemResponseProtocol.GetCacheItemResponse.Builder PopulateResponse(CacheEntry entry, GetCacheItemResponseProtocol.GetCacheItemResponse.Builder response, ClientManager clientManager, String serializationContext) throws com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException, IOException
    {
        if (entry.getExpirationHint() instanceof AggregateExpirationHint)
        {
            ExpirationHint[] hints = ((AggregateExpirationHint) entry.getExpirationHint()).getHints();
            /**
             * All hints are on same level now. There will be no AggregateExpirationHint within an AggregateExpirationHint
             */
            for (int i = 0; i < hints.length; i++)
            {
                if (hints[i] instanceof FixedExpiration)
                {
                    response.setAbsExp((((FixedExpiration) hints[i]).getTicks()));
                }
                else if (hints[i] instanceof IdleExpiration)
                {
                    response.setSldExp(((IdleExpiration) hints[i]).getSlidingTime().getTotalTicks());
                }
            }
        }
        else
        {
            if (entry.getExpirationHint() instanceof FixedExpiration)
            {
                response.setAbsExp(((FixedExpiration) entry.getExpirationHint()).getTicks());
            }
            else if (entry.getExpirationHint() instanceof IdleExpiration)
            {
                response.setSldExp(((IdleExpiration) entry.getExpirationHint()).getSlidingTime().getTotalTicks());
            }
        }

        /**
         * Fixed and Idle expiration hints are not included in making of protobuf dependency object
         */
   

        if (entry.getPriority() != null)
        {
            response.setPriority(entry.getPriority().value());
        }


        if (entry.getQueryInfo() != null)
        {
            if (entry.getQueryInfo().get("tag-info") != null)
            {
                response.setTagInfo(com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetTagInfoObj((java.util.HashMap) ((entry.getQueryInfo().get("tag-info") instanceof java.util.HashMap) ? entry.getQueryInfo().get("tag-info") : null)));
            }

            if (entry.getQueryInfo().get("named-tag-info") != null)
            {
                response.setNamedTagInfo(com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetNamedTagInfoObj((java.util.HashMap) ((entry.getQueryInfo().get("named-tag-info") instanceof java.util.HashMap) ? entry.getQueryInfo().get("named-tag-info") : null), clientManager.IsDotNetClient));
            }

        }

        if (entry.getExpirationHint() != null)
        {
            response.setHasExpired(entry.getExpirationHint().getHasExpired());
            response.setNeedsResync(entry.getExpirationHint().getNeedsReSync());
        }
        response.setFlag(entry.getFlag().getData());
        response.setVersion(entry.getVersion());
        response.setCreationTime(new NCDateTime(entry.getCreationTime()).getTicks());
        response.setLastModifiedTime(new NCDateTime(entry.getLastModifiedTime()).getTicks());
        if (entry.getLockId() != null)
        {
            response.setLockId((entry.getLockId() != null ? entry.getLockId().toString() : null));
        }
        response.setLockTicks(new NCDateTime(entry.getLockDate()).getTicks());
        if (entry.getGroupInfo() != null)
        {
            response.setGroup(entry.getGroupInfo().getGroup() != null ? entry.getGroupInfo().getGroup() : "");
            response.setSubGroup(entry.getGroupInfo().getSubGroup() != null ? entry.getGroupInfo().getSubGroup() : "");
        }

        Object userValue = entry.getValue();
        if (userValue instanceof CallbackEntry)
        {
            userValue = ((CallbackEntry) userValue).getValue();
        }

        ArrayList<byte[]> arr = ((UserBinaryObject) userValue).getDataList();
        for (int i = 0; i < arr.size(); i++)
        {
            response.addValue(ByteString.copyFrom(arr.get(i)));
        }

        return response;
    }
}
