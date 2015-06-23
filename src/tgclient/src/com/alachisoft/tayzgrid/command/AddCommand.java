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
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.common.protobuf.AddCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.NamedTagInfoProtocol;
import com.alachisoft.tayzgrid.common.protobuf.QueryInfoProtocol;
import com.alachisoft.tayzgrid.common.protobuf.TagInfoProtocol;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.runtime.util.HelperFxn;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.BitSetConstants;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.util.UserBinaryObject;
import com.alachisoft.tayzgrid.common.protobuf.ObjectQueryInfoProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ObjectQueryInfoProtocol.ObjectQueryInfo;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.google.protobuf.ByteString;
import com.sun.org.apache.bcel.internal.util.ByteSequence;
import java.io.ByteArrayInputStream;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public final class AddCommand extends Command {

    private String group;
    private String subGroup;
    private Date absoluteExpiration;
    private TimeSpan slidingExpiration;
    private boolean isResyncExpiredItems;
    private CacheItemPriority priority;
    private int remCallbackID;
    private int upCallbackID;
    private HashMap queryInfo;
    private int flagMap;
    //+ :20110330 ReadThru/WriteThru provider support in JAVA
    private String providerName;
    private String resyncProviderName;
    private int datasourceItemAddedCallbackID;
    private String cacheId;
    private short updateCallbackFilter;
    private short removeCallbackFilter;

    /**
     * Creates a new instance of AddCommand
     *
     * @param key
     * @param value
     * @param dependency
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
    public AddCommand(Object key, byte[] value, Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority, int remId, int upId, int itemAdded, int dsItemAddedCallback, boolean isResyncExpiredItems, String group, String subGroup, boolean async, HashMap queryInfo, BitSet flagMap, String providerName, String resyncProviderName, String cacheId, EventDataFilter updateDataFilter, EventDataFilter removeDataFilter) {
        name = "ADD";
        this.key = key;
        this.value = value;
        this.absoluteExpiration = absoluteExpiration;
        this.slidingExpiration = slidingExpiration;
        this.priority = priority;
        this.remCallbackID = remId;
        this.upCallbackID = upId;
        super.asyncCallbackId = itemAdded;
        this.datasourceItemAddedCallbackID = dsItemAddedCallback;
        this.isResyncExpiredItems = isResyncExpiredItems;
        this.group = group;
        this.subGroup = subGroup;
        this.isAsync = async;
        this.asyncCallbackSpecified = this.isAsync && super.asyncCallbackId != -1 ? true : false;
        this.queryInfo = queryInfo;
        this.flagMap = BitSetConstants.getBitSetData(flagMap);

        this.providerName = providerName;
        this.resyncProviderName = resyncProviderName;

        this.cacheId = cacheId;

        this.updateCallbackFilter = (short) updateDataFilter.getValue();
        this.removeCallbackFilter = (short) removeDataFilter.getValue();
    }

    public void createCommand() throws CommandException {
        long sldExp = 0, absExp = 0;
        if (absoluteExpiration != Cache.DefaultAbsoluteExpiration) //absExp = ticks(absoluteExpiration);
        {
            absExp = HelperFxn.getUTCTicks(absoluteExpiration);
        } else {
            absExp = 0;
        }

        if (slidingExpiration != Cache.DefaultSlidingExpiration) {
            sldExp = slidingExpiration.getTotalTicks();
        } else {
            sldExp = 0;
        }

        if (sldExp != 0 && absExp != 0) {
            throw new IllegalArgumentException(
                    "You cannot set both sliding and absolute expirations on the same cached item");
        }

        ///Get user data and add to protobuf data array
        UserBinaryObject userBin = UserBinaryObject.createUserBinaryObject(this.value);
        List<byte[]> dataList = userBin.getDataList();
        int noOfChunks = dataList.size();

        ///Copy the chunks to protobuf list
        List<ByteString> dataChunks = new ArrayList<ByteString>();
        for (int i = 0; i < noOfChunks; i++) {
            dataChunks.add(ByteString.copyFrom(dataList.get(i)));
        }

        try {

            AddCommandProtocol.AddCommand.Builder builder
                    = AddCommandProtocol.AddCommand.newBuilder()
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
                    .setDatasourceItemAddedCallbackId(datasourceItemAddedCallbackID)
                    .setUpdateDataFilter(this.updateCallbackFilter)
                    .setRemoveDataFilter(this.removeCallbackFilter);

            if (group != null) {
                builder = builder.setGroup(group);
                if (subGroup != null) {
                    builder = builder.setSubGroup(subGroup);
                }
            }

            if (providerName != null) {
                builder = builder.setProviderName(providerName);
            }

            if (resyncProviderName != null) {
                builder = builder.setResyncProviderName(resyncProviderName);
            }

            ObjectQueryInfoProtocol.ObjectQueryInfo.Builder objectQueryInfoBuilder
                    = ObjectQueryInfoProtocol.ObjectQueryInfo.newBuilder();

            if (queryInfo.get("query-info") != null) {
                objectQueryInfoBuilder
                        = objectQueryInfoBuilder.setQueryInfo(super.getQueryInfoObj((HashMap) queryInfo.get("query-info")));
            }

            if (queryInfo.get("tag-info") != null) {
                objectQueryInfoBuilder
                        = objectQueryInfoBuilder.setTagInfo(super.getTagInfo((HashMap) queryInfo.get("tag-info")));
            }

            if (queryInfo.get("named-tag-info") != null) {
                objectQueryInfoBuilder
                        = objectQueryInfoBuilder.setNamedTagInfo(super.GetNamedTagInfoObj((HashMap) queryInfo.get("named-tag-info")));
            }

            builder = builder.setObjectQueryInfo(objectQueryInfoBuilder.build());

            CommandProtocol.Command.Builder commandBuilder
                    = CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setAddCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.ADD)
                    .setVersion("4200");

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());
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
        return CommandType.ADD;
    }

    public int AsycItemAddedOpComplete() {
        return super.asyncCallbackId;
    }

    @Override
    public RequestType getCommandRequestType() {
        return RequestType.AtomicWrite;
    }

}
