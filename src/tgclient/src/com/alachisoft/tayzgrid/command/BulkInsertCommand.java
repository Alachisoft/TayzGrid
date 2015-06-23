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
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.common.protobuf.BulkInsertCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.InsertCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.NamedTagInfoProtocol;
import com.alachisoft.tayzgrid.common.protobuf.QueryInfoProtocol;
import com.alachisoft.tayzgrid.common.protobuf.TagInfoProtocol;
import com.alachisoft.tayzgrid.runtime.util.HelperFxn;
import com.alachisoft.tayzgrid.common.BitSetConstants;

import com.alachisoft.tayzgrid.util.UserBinaryObject;
import com.alachisoft.tayzgrid.common.protobuf.ObjectQueryInfoProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashMap;
import java.util.List;


public final class BulkInsertCommand extends Command {

    private Object[] keys;
    private CacheItem[] items;
    private String group = "";
    private String subGroup = "";
    private HashMap queryInfo[];
    private boolean isResyncExpiredItems = false;
    
    private int dsItemUpdatedtCallbackId;
    private String providerName;
    
    private int[] _updateCallbackIds;
    private int[] _removeCallbackIds;
    

    /**
     * Creates a new instance of AddBulkCommand
     */
    public BulkInsertCommand(Object[] keys, CacheItem[] items, int[] updateCallbackIds, int[] removeCallbackIds, int dsItemsUpdatedCallbackId, String providerName, String cacheId) {
        this.keys = keys;
        super.setBulkKeys(keys);
        this.items = items;
        this.keys = keys;
        this.name = "INSERTBULK";
        this.dsItemUpdatedtCallbackId = dsItemsUpdatedCallbackId;

        this._updateCallbackIds = updateCallbackIds;
        this._removeCallbackIds = removeCallbackIds;
        this.providerName = providerName;
    }

    protected void createCommand() throws CommandException {
        if (this.keys == null) {
            throw new NullPointerException("keys");
        }
        if (this.items == null) {
            throw new NullPointerException("items");
        }
        if (this.keys.length == 0) {
            throw new IllegalArgumentException(
                    "There is no key present in keys array");
        }
        if (this.items.length == 0) {
            throw new IllegalArgumentException(
                    "There is no cache item in items array");
        }
        if (this.keys.length != items.length) {
            throw new IllegalArgumentException(
                    "keys count is not equal to items count");
        }
        
        try {
            BulkInsertCommandProtocol.BulkInsertCommand.Builder builder =
                    BulkInsertCommandProtocol.BulkInsertCommand.newBuilder();

            builder = builder.setRequestId(this.getRequestId());
            if (providerName != null) {
                builder = builder.setProviderName(providerName);
            }
            builder = builder.setDatasourceUpdatedCallbackId(dsItemUpdatedtCallbackId);

            int keyLen = keys.length;
            long absExp = 0, sldExp = 0;

            for (int i = 0; i < keyLen; i++) {
                if (keys[i] == null) {
                    throw new NullPointerException("keys");
                }
                if (items[i].getValue() == null) {
                    throw new NullPointerException("values");
                }

                if (items[i].getAbsoluteExpiration() != Cache.DefaultAbsoluteExpiration) {

                    absExp = HelperFxn.getUTCTicks(items[i].getAbsoluteExpiration());
                } else {
                    absExp = 0;
                }

                group = items[i].getGroup();
                subGroup = items[i].getSubGroup();
                isResyncExpiredItems = items[i].getResyncExpiredItems();



                if (items[i].getSlidingExpiration() != Cache.DefaultSlidingExpiration) {
                    sldExp = items[i].getSlidingExpiration().getTotalTicks();
                } else {
                    sldExp = 0;
                }

                if (absExp != 0 && sldExp != 0) {
                    throw new NullPointerException("You cannot set both sliding and absolute expirations on the same cached item");
                }



                InsertCommandProtocol.InsertCommand.Builder insertBuilder =
                        InsertCommandProtocol.InsertCommand.newBuilder();

                insertBuilder = insertBuilder.setKey(CacheKeyUtil.toByteString(keys[i], this.getCacheId()))
                        .setAbsExpiration(absExp)
                        .setSldExpiration(sldExp)
                        .setPriority((items[i].getPriority().ordinal() - 2))
                        .setFlag(BitSetConstants.getBitSetData(items[i].getFlag()))
                        .setRemoveCallbackId(this._removeCallbackIds[i])
                        .setUpdateCallbackId(this._updateCallbackIds[i])
                        .setIsResync(isResyncExpiredItems);

                if (group != null) {
                    insertBuilder = insertBuilder.setGroup(group);
                    if (subGroup != null) {
                        insertBuilder = insertBuilder.setSubGroup(subGroup);
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


                    insertBuilder = insertBuilder.setObjectQueryInfo(objectQueryInfoBuilder.build());

            

                ///Get user data and add to protobuf data array
                UserBinaryObject userBin = UserBinaryObject.createUserBinaryObject((byte[]) items[i].getValue());
                List<byte[]> dataList = userBin.getDataList();
                int noOfChunks = dataList.size();

                ///Copy the chunks to protobuf list
                List<ByteString> dataChunks = new ArrayList<ByteString>();
                for (int j = 0; j < noOfChunks; j++) {
                    dataChunks.add(ByteString.copyFrom(dataList.get(j)));
                }
                insertBuilder = insertBuilder.addAllData(dataChunks);

                builder.addInsertCommand(insertBuilder.build());

            }

        
            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setBulkInsertCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.INSERT_BULK)
                    .setIntendedRecipient(this.getIntendedRecipient())
                    .setClientLastViewId(this.getClientLastViewId())
                    .setVersion("4200");

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());
        } catch (IOException ex) {
            throw new CommandException(ex.getMessage());
        }
    }

    protected boolean parseCommand() {
        return false;
    }

    public CommandType getCommandType() {
        return CommandType.INSERT_BULK;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.BulkWrite;
    }
}
