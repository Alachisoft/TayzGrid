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
import com.alachisoft.tayzgrid.common.InsertParams;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.InsertCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.NamedTagInfoProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ObjectQueryInfoProtocol;
import com.alachisoft.tayzgrid.common.protobuf.QueryInfoProtocol;
import com.alachisoft.tayzgrid.common.protobuf.TagInfoProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.runtime.util.HelperFxn;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.util.UserBinaryObject;
import com.alachisoft.tayzgrid.web.caching.Cache;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public final class InsertCommand extends Command
{

    private String group;
    private String subGroup;
    private Date absoluteExpiration;
    private TimeSpan slidingExpiration;
    private boolean isResyncExpiredItems;
    private CacheItemPriority priority;
    private int remCallbackID;
    private int upCallbackID;
    private int dsItemUpdateCallbackID; 
    private HashMap queryInfo;
    private int flagMap;
    private String _lockId;
    private com.alachisoft.tayzgrid.caching.LockAccessType _accessType;
    private long _version;
    private String providerName;
    private String resyncProviderName;
    private String cacheId;
      private short updateCallbackFilter;
    private short removeCallbackFilter;
    private InsertParams options;

    /**
     * Creates a new instance of AddCommand
     *
     * @param key
     * @param value
     * @param absoluteExpiration
     * @param slidingExpiration
     * @param priority
     * @param isResyncExpiredItems
     * @param group
     * @param subGroup
     * @param async
     * @throws java.io.UnsupportedEncodingException
     * @throws java.io.IOException
     */
    public InsertCommand(Object key, byte[] value,  Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority, int remId, int upId, int asynOpCompleted, int dsItemUpdatedCallbackID, boolean isResyncExpiredItems, String group, String subGroup, boolean async, HashMap queryInfo, long version, BitSet flagMap, String lockId, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String providerName, String resyncProviderName, String cacheId, EventDataFilter itemUpdateDataFilter, EventDataFilter itemRemovedDataFilter, InsertParams options)
    {

        super.asyncCallbackId = asynOpCompleted;
        this.key = key;
        this.value = value;
        this.absoluteExpiration = absoluteExpiration;
        this.slidingExpiration = slidingExpiration;
        this.priority = priority;
        this.remCallbackID = remId;
        this.upCallbackID = upId;
        this.isResyncExpiredItems = isResyncExpiredItems;
        this.group = group;
        this.subGroup = subGroup;
        this.isAsync = async;
        this.asyncCallbackSpecified = this.isAsync && super.asyncCallbackId != -1 ? true : false;
        this.queryInfo = queryInfo;
        this.flagMap = BitSetConstants.getBitSetData(flagMap);
        this.dsItemUpdateCallbackID = dsItemUpdatedCallbackID;
        this._lockId = lockId;
        this._accessType = accessType;
        this._version = version;
        this.providerName = providerName;
        this.resyncProviderName = resyncProviderName;
        this.cacheId = cacheId; 
        this.updateCallbackFilter = (short)itemUpdateDataFilter.getValue();
        this.removeCallbackFilter = (short)itemRemovedDataFilter.getValue();
        this.options = options;
        super.name = "INSERT";
        
    }

    public void createCommand() throws CommandException
    {
        if (key == null)
        {
            throw new NullPointerException("key");
        }
        if (value == null)
        {
            throw new NullPointerException("values");
        }

        if (key.equals(""))
        {
            throw new IllegalArgumentException("key");
        }

        if (group == null)
        {
            if (subGroup != null)
            {
                throw new NullPointerException("group must be specified for sub group");
            }
        }

        long sldExp = 0, absExp = 0;
        if (absoluteExpiration != Cache.DefaultAbsoluteExpiration)
        //absExp = ticks(absoluteExpiration);
        {
            absExp = HelperFxn.getUTCTicks(absoluteExpiration);
            Date test=HelperFxn.getDateFromUTCTicks(absExp);

        }
        else
        {
            absExp = 0;
        }
        if (slidingExpiration != Cache.DefaultSlidingExpiration)
        {
            sldExp = slidingExpiration.getTotalTicks();
        }
        else
        {
            sldExp = 0;
        }
        if (sldExp != 0 && absExp != 0)
        {
            throw new NullPointerException("You cannot set both sliding and absolute expirations on the same cached item");
        }

        ///Get user data and add to protobuf data array
        UserBinaryObject userBin = UserBinaryObject.createUserBinaryObject(this.value);
        List<byte[]> dataList = userBin.getDataList();
        int noOfChunks = dataList.size();

        ///Copy the chunks to protobuf list
        List<ByteString> dataChunks = new ArrayList<ByteString>();
        for (int i = 0; i < noOfChunks; i++)
        {
            dataChunks.add(ByteString.copyFrom(dataList.get(i)));
        }

        try
        {
            InsertCommandProtocol.InsertCommand.Builder builder =
                    InsertCommandProtocol.InsertCommand.newBuilder()
                    .addAllData(dataChunks)
                    .setAbsExpiration(absExp)
                    .setSldExpiration(sldExp)
                    .setFlag(flagMap)
                    .setIsAsync(isAsync)
                    .setIsResync(isResyncExpiredItems)

                    .setPriority(priority.value())
                    .setKey(CacheKeyUtil.toByteString(key, this.getCacheId()))
                    .setRemoveCallbackId(remCallbackID)
                    .setRequestId(this.getRequestId())
                    .setUpdateCallbackId(upCallbackID)
                    .setDatasourceUpdatedCallbackId(dsItemUpdateCallbackID)
                    .setLockAccessType(this._accessType.getValue())
                    .setItemVersion(this._version)
                    .setUpdateDataFilter(this.updateCallbackFilter)
                    .setRemoveDataFilter(this.removeCallbackFilter);
            
            if(options!=null)
            {
                builder = builder.setReturnExisting(options.ReturnExistingValue)
                        .setIsReplace(options.IsReplaceOperation)
                        .setCompareOld(options.CompareOldValue);

                if(options.OldValue != null)
                {
                    ///Get user data and add to protobuf data array
                    userBin = UserBinaryObject.createUserBinaryObject((byte[])options.OldValue);
                    dataList = userBin.getDataList();
                    noOfChunks = dataList.size();

                    ///Copy the chunks to protobuf list
                    for (int i = 0; i < noOfChunks; i++)
                    {
                        builder.addOldValue(ByteString.copyFrom(dataList.get(i)));
                    }
                    builder.setOldValueFlag(BitSetConstants.getBitSetData(options.OldValueFlag));
                }
            }

            if (this._lockId != null)
            {
                builder = builder.setLockId(this._lockId);
            }

            if (group != null)
            {
                builder = builder.setGroup(group);
                if (subGroup != null)
                {
                    builder = builder.setSubGroup(subGroup);
                }
            }

            if (providerName != null)
            {
                builder = builder.setProviderName(providerName);
            }

            if (resyncProviderName != null)
            {
                builder = builder.setResyncProviderName(resyncProviderName);
            }

    

            ObjectQueryInfoProtocol.ObjectQueryInfo.Builder objectQueryInfoBuilder =
                    ObjectQueryInfoProtocol.ObjectQueryInfo.newBuilder();

            if (queryInfo.get("query-info") != null) {
                objectQueryInfoBuilder = 
                objectQueryInfoBuilder.setQueryInfo(super.getQueryInfoObj((HashMap) queryInfo.get("query-info")));
            }

            if (queryInfo.get("tag-info") != null) {
                objectQueryInfoBuilder = 
                objectQueryInfoBuilder.setTagInfo(super.getTagInfo((HashMap) queryInfo.get("tag-info")));
            }

            if (queryInfo.get("named-tag-info") != null) {
                objectQueryInfoBuilder = 
                objectQueryInfoBuilder.setNamedTagInfo(super.GetNamedTagInfoObj((HashMap) queryInfo.get("named-tag-info")));
            }         
                builder = builder.setObjectQueryInfo(objectQueryInfoBuilder.build());


            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setInsertCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.INSERT)
                    .setVersion("4200");

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());
        }
        catch (IOException ex)
        {
            throw new CommandException(ex.getMessage());
        }
    }

    /**
     *
     * @return
     */
    protected boolean parseCommand()
    {
        return true;
    }

    public CommandType getCommandType()
    {
        return CommandType.INSERT;
    }
        
    public int AsycItemUpdatedOpComplete()
    {
        return super.asyncCallbackId;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.AtomicWrite;
    }
}
