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

import com.alachisoft.tayzgrid.socketserver.command.CommandBase;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.caching.evictionpolicies.PriorityEvictionHint;
import com.alachisoft.tayzgrid.caching.datagrouping.GroupInfo;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.common.protobuf.AddCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.BulkInsertCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ObjectQueryInfoProtocol;
import com.alachisoft.tayzgrid.common.protobuf.InsertCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.BulkAddCommandProtocol;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.caching.CallbackEntry;

import com.alachisoft.tayzgrid.common.*;
import com.alachisoft.tayzgrid.caching.datagrouping.*;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.socketserver.*;
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.google.protobuf.ByteString;

public class BulkAddAndInsertCommandBase extends CommandBase {

    public final static class CommandInfo {

        public int PackageSize;
        public Object[] Values;
        public String RequestId;
        public String ProviderName;
        public Object[] Keys;
        public String Group;
        public String SubGroup;
        public short OnDsItemsAddedCallback;
        public short[] RemoveCallbackId;
        public short[] UpdateCallbackId;
        public int[] RemoveDataFilter;
        public int[] UpdateDataFilter;
        public ExpirationHint[] ExpirationHint;
        public PriorityEvictionHint[] EvictionHint;
        public CallbackEntry[] CallbackEnteries;
        public java.util.HashMap[] QueryInfo;
        public BitSet[] Flags;
        public GroupInfo[] groupInfos;
        public long ClientLastViewId;
        public String IntendedRecipient;
        public String resyncProviderName;
        public long[] KeySizes;

        public CommandInfo clone() {
            CommandInfo varCopy = new CommandInfo();

            varCopy.PackageSize = this.PackageSize;
            varCopy.Values = this.Values;
            varCopy.RequestId = this.RequestId;
            varCopy.ProviderName = this.ProviderName;
            varCopy.Keys = this.Keys;
            varCopy.KeySizes = this.KeySizes;
            varCopy.Group = this.Group;
            varCopy.SubGroup = this.SubGroup;
            varCopy.OnDsItemsAddedCallback = this.OnDsItemsAddedCallback;
            varCopy.RemoveCallbackId = this.RemoveCallbackId;
            varCopy.UpdateCallbackId = this.UpdateCallbackId;
            varCopy.ExpirationHint = this.ExpirationHint;
            varCopy.EvictionHint = this.EvictionHint;
            varCopy.CallbackEnteries = this.CallbackEnteries;
            varCopy.QueryInfo = this.QueryInfo;
            varCopy.Flags = this.Flags;
            varCopy.groupInfos = this.groupInfos;
            varCopy.ClientLastViewId = this.ClientLastViewId;
            varCopy.IntendedRecipient = this.IntendedRecipient;
            varCopy.UpdateDataFilter = this.UpdateDataFilter;
            varCopy.RemoveDataFilter = this.RemoveDataFilter;
            return varCopy;
        }
    }

    @Override
    public boolean getCanHaveLargedata() {
        return true;
    }

    @Override
    public boolean getIsBulkOperation() {
        return true;
    }
    public static String NC_NULL_VAL = "NLV";
    protected String serailizationContext;
    private String _clientId = ""; 

    public final String getClientId() {
        return _clientId;
    }

    public final void setClientId(String value) {
        _clientId = value;
    }

    @Override
    public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) throws Exception {
    }

    //PROTOBUF
    protected final CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager, String cacheId) throws Exception {
        CommandInfo cmdInfo = new CommandInfo();
        int packageSize = 0;
        int index = 0;
        java.util.HashMap queryInfoHashMap = null;
        java.util.HashMap tagHashtable = null;
        java.util.HashMap namedTagHashtable = null;
        Object[] biteString = null;
        Object[] p = null;
        try {
            switch (command.getType()) {
                case ADD_BULK:
                    BulkAddCommandProtocol.BulkAddCommand bulkAddCommand = command.getBulkAddCommand();

                    packageSize = bulkAddCommand.getAddCommandList().size();

                    cmdInfo.Keys = new Object[packageSize];
                    cmdInfo.KeySizes = new long[packageSize];
                    cmdInfo.UpdateCallbackId = new short[packageSize];
                    cmdInfo.RemoveCallbackId = new short[packageSize];
                    cmdInfo.UpdateDataFilter = new int[packageSize];
                    cmdInfo.RemoveDataFilter = new int[packageSize];
                    cmdInfo.CallbackEnteries = new CallbackEntry[packageSize];
                    cmdInfo.EvictionHint = new PriorityEvictionHint[packageSize];
                    cmdInfo.ExpirationHint = new ExpirationHint[packageSize];
                    cmdInfo.Flags = new BitSet[packageSize];
                    cmdInfo.Values = new Object[packageSize];
                    cmdInfo.groupInfos = new GroupInfo[packageSize];
                    cmdInfo.QueryInfo = new java.util.HashMap[packageSize];
                    cmdInfo.OnDsItemsAddedCallback = (short) bulkAddCommand.getDatasourceItemAddedCallbackId();
                    cmdInfo.ProviderName = bulkAddCommand.getProviderName().length() == 0 ? null : bulkAddCommand.getProviderName();
                    cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
                    cmdInfo.ClientLastViewId = command.getClientLastViewId();
                    cmdInfo.IntendedRecipient = command.getIntendedRecipient();
                    for (AddCommandProtocol.AddCommand addCommand : bulkAddCommand.getAddCommandList()) {
                        cmdInfo.Keys[index] = CacheKeyUtil.Deserialize(addCommand.getKey(), cacheId);
                        cmdInfo.KeySizes[index] = addCommand.getKey().size();
                        cmdInfo.UpdateCallbackId[index] = (short) addCommand.getUpdateCallbackId();
                        cmdInfo.RemoveCallbackId[index] = (short) addCommand.getRemoveCallbackId();
                        
                       if (addCommand.getUpdateDataFilter() != -1)
                            cmdInfo.UpdateDataFilter[index] = addCommand.getUpdateDataFilter();
                        else
                            cmdInfo.UpdateDataFilter[index] = (int) EventDataFilter.None.getValue();

                        
                        if (addCommand.getRemoveDataFilter() != -1)
                            cmdInfo.RemoveDataFilter[index] = addCommand.getRemoveDataFilter();
                        else
                            cmdInfo.RemoveDataFilter[index] = (int)EventDataFilter.DataWithMetaData.getValue();

                        cmdInfo.EvictionHint[index] = new PriorityEvictionHint(CacheItemPriority.forValue(addCommand.getPriority()));
                        cmdInfo.ExpirationHint[index] = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetExpirationHintObj(addCommand.getAbsExpiration(), addCommand.getSldExpiration(), addCommand.getIsResync(), serailizationContext);
                        cmdInfo.Flags[index] = new BitSet((byte) addCommand.getFlag());

                        CallbackEntry cbEntry = null;
                        if (cmdInfo.UpdateCallbackId[index] != -1 || cmdInfo.RemoveCallbackId[index] != -1 || cmdInfo.OnDsItemsAddedCallback != -1) {
                            cbEntry = new CallbackEntry(getClientId(), Integer.parseInt(cmdInfo.RequestId), cmdInfo.Values[index], cmdInfo.RemoveCallbackId[index], cmdInfo.UpdateCallbackId[index], (short) (cmdInfo.RequestId.equals("-1") ? -1 : 0), cmdInfo.OnDsItemsAddedCallback, cmdInfo.Flags[index]
                             ,EventDataFilter.forValue(cmdInfo.UpdateDataFilter[index]),
                              EventDataFilter.forValue(cmdInfo.RemoveDataFilter[index]));
                        }

                        cmdInfo.CallbackEnteries[index] = cbEntry;

                        if (addCommand.getGroup() != null) {
                            cmdInfo.Group = addCommand.getGroup().length() == 0 ? null : addCommand.getGroup();
                        }
                        if (addCommand.getSubGroup() != null) {
                            cmdInfo.SubGroup = addCommand.getSubGroup().length() == 0 ? null : addCommand.getSubGroup();
                        }
                        if (addCommand.getResyncProviderName() != null && addCommand.getResyncProviderName().length() > 0) {
                            cmdInfo.resyncProviderName = addCommand.getResyncProviderName();
                        }
                        cmdInfo.groupInfos[index] = new GroupInfo(cmdInfo.Group, cmdInfo.SubGroup);

                        java.util.HashMap queryInfo = new java.util.HashMap();

                        String version = command.getVersion();

                        if (version == null) {
                            if (addCommand.getQueryInfo().getAttributesCount() > 0) {
                                queryInfoHashMap = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromQueryInfoObj(addCommand.getQueryInfo());
                            }

                            if (queryInfoHashMap != null) {
                                if (queryInfo == null) {
                                    queryInfo = new java.util.HashMap();
                                }
                                queryInfo.put("query-info", queryInfoHashMap);
                            }

                            if (addCommand.getTagInfo().getTagsCount() > 0) {
                                tagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromTagInfoObj(addCommand.getTagInfo());
                            }
                            if (tagHashtable != null) {
                                if (queryInfo == null) {
                                    queryInfo = new java.util.HashMap();
                                }
                                queryInfo.put("tag-info", tagHashtable);
                            }


                            if (addCommand.getNamedTagInfo().getTypesCount() > 0) {
                                if (clientManager.IsDotNetClient) {
                                    namedTagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromNamedTagInfoObjFromDotNet(addCommand.getNamedTagInfo());
                                } else {
                                    namedTagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromNamedTagInfoObjFromJava(addCommand.getNamedTagInfo());
                                }
                            }
                            if (namedTagHashtable != null) {
                                if (queryInfo == null) {
                                    queryInfo = new java.util.HashMap();
                                }
                                queryInfo.put("named-tag-info", namedTagHashtable);
                            }
                        } else {
                            ObjectQueryInfoProtocol.ObjectQueryInfo objectQueryInfo;
                            
                                objectQueryInfo = addCommand.getObjectQueryInfo();
                            

                            queryInfoHashMap = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromQueryInfoObj(objectQueryInfo.getQueryInfo());
                            tagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromTagInfoObj(objectQueryInfo.getTagInfo());
                            if (clientManager.IsDotNetClient) {
                                namedTagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromNamedTagInfoObjFromDotNet(objectQueryInfo.getNamedTagInfo());
                            } else {
                               namedTagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromNamedTagInfoObjFromJava(objectQueryInfo.getNamedTagInfo());
                            }

                            if (queryInfoHashMap != null) {
                                if (queryInfo == null) {
                                    queryInfo = new java.util.HashMap();
                                }
                                queryInfo.put("query-info", queryInfoHashMap);
                            }

                            if (tagHashtable != null) {
                                if (queryInfo == null) {
                                    queryInfo = new java.util.HashMap();
                                }
                                queryInfo.put("tag-info", tagHashtable);
                            }

                            if (namedTagHashtable != null) {
                                if (queryInfo == null) {
                                    queryInfo = new java.util.HashMap();
                                }
                                queryInfo.put("named-tag-info", namedTagHashtable);
                            }
                        }
                        cmdInfo.QueryInfo[index] = queryInfo;

                        cmdInfo.SubGroup = addCommand.getSubGroup().length() == 0 ? null : addCommand.getSubGroup();
                        biteString = addCommand.getDataList().toArray();
                        p = new Object[biteString.length];

                        for (int i = 0; i < biteString.length; i++) {
                            p[i] = ((ByteString) biteString[i]).toByteArray();
                        }
                        cmdInfo.Values[index] = new UserBinaryObject(p);

                        index++;
                    }

                    break;

                case INSERT_BULK:
                    BulkInsertCommandProtocol.BulkInsertCommand bulkInsertCommand = command.getBulkInsertCommand();

                    packageSize = bulkInsertCommand.getInsertCommandList().size();

                    cmdInfo.Keys = new Object[packageSize];
                    cmdInfo.KeySizes = new long[packageSize];
                    cmdInfo.UpdateCallbackId = new short[packageSize];
                    cmdInfo.RemoveCallbackId = new short[packageSize];
                    cmdInfo.UpdateDataFilter = new int[packageSize];
                    cmdInfo.RemoveDataFilter = new int[packageSize];
                    cmdInfo.CallbackEnteries = new CallbackEntry[packageSize];
                    cmdInfo.EvictionHint = new PriorityEvictionHint[packageSize];
                    cmdInfo.ExpirationHint = new ExpirationHint[packageSize];
                    cmdInfo.Flags = new BitSet[packageSize];
                    cmdInfo.Values = new Object[packageSize];
                    cmdInfo.groupInfos = new GroupInfo[packageSize];
                    cmdInfo.QueryInfo = new java.util.HashMap[packageSize];
                    cmdInfo.OnDsItemsAddedCallback = (short) bulkInsertCommand.getDatasourceUpdatedCallbackId();
                    cmdInfo.ProviderName = bulkInsertCommand.getProviderName().length() == 0 ? null : bulkInsertCommand.getProviderName();
                    cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
                    cmdInfo.ClientLastViewId = command.getClientLastViewId();
                    cmdInfo.IntendedRecipient = command.getIntendedRecipient();
                    for (InsertCommandProtocol.InsertCommand insertCommand : bulkInsertCommand.getInsertCommandList()) {
                        cmdInfo.Keys[index] = CacheKeyUtil.Deserialize(insertCommand.getKey(), cacheId);
                        cmdInfo.KeySizes[index] = insertCommand.getKey().size();
                        cmdInfo.UpdateCallbackId[index] = (short) insertCommand.getUpdateCallbackId();
                        cmdInfo.RemoveCallbackId[index] = (short) insertCommand.getRemoveCallbackId();
                        
                       if (insertCommand.getUpdateDataFilter() != -1)
                            cmdInfo.UpdateDataFilter[index] = insertCommand.getUpdateDataFilter();
                        else
                            cmdInfo.UpdateDataFilter[index] = (int)EventDataFilter.None.getValue();
         
                       if (insertCommand.getRemoveDataFilter() != -1)
                            cmdInfo.RemoveDataFilter[index] = insertCommand.getRemoveDataFilter();
                        else
                            cmdInfo.RemoveDataFilter[index] = (int) EventDataFilter.DataWithMetaData.getValue();

                        
                        cmdInfo.EvictionHint[index] = new PriorityEvictionHint(CacheItemPriority.forValue(insertCommand.getPriority()));
                        cmdInfo.ExpirationHint[index] = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetExpirationHintObj( insertCommand.getAbsExpiration(), insertCommand.getSldExpiration(), insertCommand.getIsResync(), serailizationContext);
                        cmdInfo.Flags[index] = new BitSet((byte) insertCommand.getFlag());

                        CallbackEntry cbEntry = null;
                        if (cmdInfo.UpdateCallbackId[index] != -1 || cmdInfo.RemoveCallbackId[index] != -1 || cmdInfo.OnDsItemsAddedCallback != -1) {
                            cbEntry = new CallbackEntry(getClientId(), Integer.parseInt(cmdInfo.RequestId), cmdInfo.Values[index], cmdInfo.RemoveCallbackId[index], cmdInfo.UpdateCallbackId[index], (short) (cmdInfo.RequestId.equals("-1") ? -1 : 0), cmdInfo.OnDsItemsAddedCallback, cmdInfo.Flags[index]
                            ,EventDataFilter.forValue(cmdInfo.UpdateDataFilter[index]),
                            EventDataFilter.forValue(cmdInfo.RemoveDataFilter[index]));
                        }

                        cmdInfo.CallbackEnteries[index] = cbEntry;

                        if (insertCommand.getGroup() != null) {
                            cmdInfo.Group = insertCommand.getGroup().length() == 0 ? null : insertCommand.getGroup();
                        }
                        if (insertCommand.getSubGroup() != null) {
                            cmdInfo.SubGroup = insertCommand.getSubGroup().length() == 0 ? null : insertCommand.getSubGroup();
                        }
                        cmdInfo.groupInfos[index] = new GroupInfo(cmdInfo.Group, cmdInfo.SubGroup);

                        java.util.HashMap queryInfo = new java.util.HashMap();

                        String version = command.getVersion();

                        if (version == null) {
                            if (insertCommand.getQueryInfo().getAttributesCount() > 0) {
                                queryInfoHashMap = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromQueryInfoObj(insertCommand.getQueryInfo());
                            }

                            if (queryInfoHashMap != null) {
                                if (queryInfo == null) {
                                    queryInfo = new java.util.HashMap();
                                }
                                queryInfo.put("query-info", queryInfoHashMap);
                            }

                            if (insertCommand.getTagInfo().getTagsCount() > 0) {
                                tagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromTagInfoObj(insertCommand.getTagInfo());
                            }
                            if (tagHashtable != null) {
                                if (queryInfo == null) {
                                    queryInfo = new java.util.HashMap();
                                }
                                queryInfo.put("tag-info", tagHashtable);
                            }


                            if (insertCommand.getNamedTagInfo().getTypesCount() > 0) {
                                if (clientManager.IsDotNetClient) {
                                    namedTagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromNamedTagInfoObjFromDotNet(insertCommand.getNamedTagInfo());
                                } else {
                                    namedTagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromNamedTagInfoObjFromJava(insertCommand.getNamedTagInfo());
                                }
                            }
                            if (namedTagHashtable != null) {
                                if (queryInfo == null) {
                                    queryInfo = new java.util.HashMap();
                                }
                                queryInfo.put("named-tag-info", namedTagHashtable);
                            }
                        } else {
                            ObjectQueryInfoProtocol.ObjectQueryInfo objectQueryInfo;
                            
                                objectQueryInfo = insertCommand.getObjectQueryInfo();
                            

                            queryInfoHashMap = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromQueryInfoObj(objectQueryInfo.getQueryInfo());
                            tagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromTagInfoObj(objectQueryInfo.getTagInfo());
                            if (clientManager.IsDotNetClient) {
                                namedTagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromNamedTagInfoObjFromDotNet(objectQueryInfo.getNamedTagInfo());
                            } else {
                                namedTagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromNamedTagInfoObjFromJava(objectQueryInfo.getNamedTagInfo());
                            }

                            if (queryInfoHashMap != null) {
                                if (queryInfo == null) {
                                    queryInfo = new java.util.HashMap();
                                }
                                queryInfo.put("query-info", queryInfoHashMap);
                            }

                            if (tagHashtable != null) {
                                if (queryInfo == null) {
                                    queryInfo = new java.util.HashMap();
                                }
                                queryInfo.put("tag-info", tagHashtable);
                            }

                            if (namedTagHashtable != null) {
                                if (queryInfo == null) {
                                    queryInfo = new java.util.HashMap();
                                }
                                queryInfo.put("named-tag-info", namedTagHashtable);
                            }
                        }
                        cmdInfo.QueryInfo[index] = queryInfo;
                        cmdInfo.SubGroup = insertCommand.getSubGroup().length() == 0 ? null : insertCommand.getSubGroup();
                    
                        biteString = insertCommand.getDataList().toArray();
                        p = new Object[biteString.length];
                        for (int i = 0; i < biteString.length; i++) {
                            p[i] = ((ByteString) biteString[i]).toByteArray();
                        }
                        cmdInfo.Values[index] = new UserBinaryObject(p);
                        index++;
                    }

                    break;
            }
        } catch (Exception e) {
            throw e;
        }
        return cmdInfo;
    }
}
