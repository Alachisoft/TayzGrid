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
import com.alachisoft.tayzgrid.web.caching.CacheItem;
import com.alachisoft.tayzgrid.common.protobuf.AddCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.BulkAddCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.NamedTagInfoProtocol;
import com.alachisoft.tayzgrid.common.protobuf.QueryInfoProtocol;
import com.alachisoft.tayzgrid.common.protobuf.TagInfoProtocol;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.runtime.util.HelperFxn;
import com.alachisoft.tayzgrid.common.BitSetConstants;
import com.alachisoft.tayzgrid.util.UserBinaryObject;
import com.alachisoft.tayzgrid.common.protobuf.ObjectQueryInfoProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;


public final class BulkAddCommand extends Command
{

    private Object keys[];
    private CacheItem[] items;
    private String group = "";
    private String subGroup = "";
    private boolean isResyncExpiredItems = false;
    
    private int dsItemsAddedCallbackId;
    private String providerName;
 
    private int[] _updateCallbackIds;
    private int[] _removeCallbackIds;
    private String cacheId;

    /**
     * Creates a new instance of BulkAddCommand
     */
    public BulkAddCommand(Object[] keys, CacheItem[] items, int[] updateCallbackIds, int[] removeCallbackIds, int dsItemsAddedCallbackId, String providerName, String cacheId)
    {
        this.keys = keys;
        this.items = items;
        this.name = "ADDBULK";
        super.setBulkKeys(keys);
        this.dsItemsAddedCallbackId = dsItemsAddedCallbackId;
        this.providerName = providerName;
        this._updateCallbackIds = updateCallbackIds;
        this._removeCallbackIds = removeCallbackIds;
        this.cacheId = cacheId;
    }

    protected void createCommand() throws CommandException
    {
        if (this.keys == null)
        {
            throw new NullPointerException("keys");
        }
        if (this.items == null)
        {
            throw new NullPointerException("items");
        }
        if (this.keys.length == 0)
        {
            throw new IllegalArgumentException(
                    "There is no key present in keys array");
        }
        if (this.items.length == 0)
        {
            throw new IllegalArgumentException(
                    "There is no cache item in items array");
        }
        if (this.keys.length != items.length)
        {
            throw new IllegalArgumentException(
                    "keys count is not equal to items count");
        }
        try
        {

        BulkAddCommandProtocol.BulkAddCommand.Builder builder =
                BulkAddCommandProtocol.BulkAddCommand.newBuilder();

        builder = builder.setRequestId(this.getRequestId());
        if (providerName != null)
        {
            builder = builder.setProviderName(providerName);
        }

        builder = builder.setDatasourceItemAddedCallbackId(dsItemsAddedCallbackId);

        int keyLen = keys.length;
        long absExp = 0, sldExp = 0;


        for (int i = 0; i < keyLen; i++)
        {
            if (keys[i] == null)
            {
                throw new NullPointerException("keys");
            }
            if (items[i].getValue() == null)
            {
                throw new NullPointerException("values");
            }

            if (items[i].getAbsoluteExpiration() != Cache.DefaultAbsoluteExpiration)
            {
               
                absExp = HelperFxn.getUTCTicks(items[i].getAbsoluteExpiration());
            }
            else
            {
                absExp = 0;
            }
            if (items[i].getSlidingExpiration() != Cache.DefaultSlidingExpiration)
            {
                sldExp = items[i].getSlidingExpiration().getTotalTicks();
            }
            else
            {
                sldExp = 0;
            }

            if (absExp != 0 && sldExp != 0)
            {
                throw new NullPointerException(
                        "You cannot set both sliding and absolute expirations on the same cached item");
            }

            if (items[i].getGroup() != null)
            {
                group = items[i].getGroup();
            }


            subGroup = (items[i].getSubGroup() != null) ? items[i].getSubGroup() : "";
            isResyncExpiredItems= items[i].getResyncExpiredItems();
            

            AddCommandProtocol.AddCommand.Builder addBuilder =
                    AddCommandProtocol.AddCommand.newBuilder();

            addBuilder = addBuilder.setKey(CacheKeyUtil.toByteString(keys[i], this.getCacheId()))
                    .setAbsExpiration(absExp)
                    .setSldExpiration(sldExp)
                    .setPriority((items[i].getPriority().ordinal() - 2))
                    .setFlag(BitSetConstants.getBitSetData(items[i].getFlag()))
                    .setRemoveCallbackId(this._removeCallbackIds[i])
                    .setUpdateCallbackId(this._updateCallbackIds[i])
                    .setIsResync(isResyncExpiredItems);
            
            if (items[i].getResyncProviderName() != null)
            {
                addBuilder.setResyncProviderName(items[i].getResyncProviderName());
            }
            
            if (group != null)
            {
                addBuilder = addBuilder.setGroup(group);
                if (subGroup != null)
                {
                    addBuilder = addBuilder.setSubGroup(subGroup);
                }
            }
      
            
        ObjectQueryInfoProtocol.ObjectQueryInfo.Builder objectQueryInfoBuilder =
                ObjectQueryInfoProtocol.ObjectQueryInfo.newBuilder();
        
        if (items[i].getQueryInfo().get("query-info") != null) {
            objectQueryInfoBuilder = 
            objectQueryInfoBuilder.setQueryInfo(super.getQueryInfoObj((HashMap) items[i].getQueryInfo().get("query-info")));
        }

        if (items[i].getQueryInfo().get("tag-info") != null) {
            objectQueryInfoBuilder = 
            objectQueryInfoBuilder.setTagInfo(super.getTagInfo((HashMap) items[i].getQueryInfo().get("tag-info")));
        }

        if (items[i].getQueryInfo().get("named-tag-info") != null) {
            objectQueryInfoBuilder = 
            objectQueryInfoBuilder.setNamedTagInfo(super.GetNamedTagInfoObj((HashMap) items[i].getQueryInfo().get("named-tag-info")));
        }
                   
            addBuilder = addBuilder.setObjectQueryInfo(objectQueryInfoBuilder.build());
            
    

            ///Get user data and add to protobuf data array
            UserBinaryObject userBin = UserBinaryObject.createUserBinaryObject((byte[]) items[i].getValue());
            List<byte[]> dataList = userBin.getDataList();
            int noOfChunks = dataList.size();

            ///Copy the chunks to protobuf list
            List<ByteString> dataChunks = new ArrayList<ByteString>();
            for (int j = 0; j < noOfChunks; j++)
            {
                dataChunks.add(ByteString.copyFrom(dataList.get(j)));
            }
            addBuilder = addBuilder.addAllData(dataChunks);

            builder.addAddCommand(addBuilder.build());

        }


            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setBulkAddCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.ADD_BULK)
                    .setIntendedRecipient(this.getIntendedRecipient())
                    .setClientLastViewId(this.getClientLastViewId())
                    .setVersion("4200");

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());
        }
        catch (IOException ex)
        {
            throw new CommandException(ex.getMessage());
        }

    }

    protected boolean parseCommand()
    {
        return false;
    }

    public CommandType getCommandType()
    {
        return CommandType.ADD_BULK;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.BulkWrite;
    }
}
