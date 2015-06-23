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

import com.alachisoft.tayzgrid.caching.CompressedValueEntry;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.socketserver.command.CommandBase;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.caching.evictionpolicies.PriorityEvictionHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.evictionpolicies.PriorityEvictionHint;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.InsertParams;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.protobuf.AddCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.InsertCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ObjectQueryInfoProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.command.CommandBase;
import com.google.protobuf.ByteString;
import java.util.List;

public class AddAndInsertCommandBase extends CommandBase {

    public final static class CommandInfo {

        public boolean DoAsync;
        public String RequestId;
        public Object Key;
        public String Group;
        public String SubGroup;
        public String ProviderName;
        public String ResyncProviderName;
        public BitSet Flag;
        public Object LockId;
        public LockAccessType LockAccessTypes = LockAccessType.values()[0];
        public ExpirationHint ExpirationHint;
        public PriorityEvictionHint EvictionHint;
        public short RemoveCallbackId;
        public short UpdateCallbackId;
        public short DsItemAddedCallbackId;
        public java.util.HashMap queryInfo;
        public long ItemVersion;
        public Object value;
        public int UpdateDataFilter;
        public int RemoveDataFilter;
        public long KeySize;
        public InsertParams insertOptions;

        public CommandInfo clone() {
            CommandInfo varCopy = new CommandInfo();

            varCopy.DoAsync = this.DoAsync;
            varCopy.RequestId = this.RequestId;
            varCopy.Key = this.Key;
            varCopy.KeySize = this.KeySize;
            varCopy.Group = this.Group;
            varCopy.SubGroup = this.SubGroup;
            varCopy.ProviderName = this.ProviderName;
            varCopy.ResyncProviderName = this.ResyncProviderName;
            varCopy.Flag = this.Flag;
            varCopy.LockId = this.LockId;
            varCopy.LockAccessTypes = this.LockAccessTypes;
            varCopy.ExpirationHint = this.ExpirationHint;
            varCopy.EvictionHint = this.EvictionHint;
            varCopy.RemoveCallbackId = this.RemoveCallbackId;
            varCopy.UpdateCallbackId = this.UpdateCallbackId;
            varCopy.DsItemAddedCallbackId = this.DsItemAddedCallbackId;
            varCopy.queryInfo = this.queryInfo;
            varCopy.ItemVersion = this.ItemVersion;
            varCopy.value = this.value;
            varCopy.UpdateDataFilter = this.UpdateDataFilter;
            varCopy.RemoveDataFilter = this.RemoveDataFilter;
            varCopy.insertOptions = this.insertOptions;
            return varCopy;
        }
    }
    public static String NC_NULL_VAL = "NLV";
    protected String serializationContext;

    @Override
    public boolean getCanHaveLargedata() {
        return true;
    }

    @Override
    public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) throws java.io.IOException, ClassNotFoundException {
    }

    //PROTOBUF
    protected CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager, String cacheId) throws Exception {

        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("AddInsertCmd.Parse", "enter");
        }

        CommandInfo cmdInfo = new CommandInfo();

        java.util.HashMap queryInfoHashMap = null;
        java.util.HashMap tagHashtable = null;
        java.util.HashMap namedTagHashtable = null;
        Object[] biteString = null;
        Object[] p = null;

        switch (command.getType()) {

            case ADD:
                AddCommandProtocol.AddCommand addCommand = command.getAddCommand();
                cmdInfo.Key = CacheKeyUtil.Deserialize(addCommand.getKey(), cacheId);
                cmdInfo.KeySize = addCommand.getKey().size();
                cmdInfo.DoAsync = addCommand.getIsAsync();
                cmdInfo.DsItemAddedCallbackId = (short) addCommand.getDatasourceItemAddedCallbackId();
                cmdInfo.EvictionHint = new PriorityEvictionHint(CacheItemPriority.forValue(addCommand.getPriority()));
                cmdInfo.ExpirationHint = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetExpirationHintObj( addCommand.getAbsExpiration(), addCommand.getSldExpiration(), addCommand.getIsResync(), serializationContext);
                cmdInfo.Flag = new BitSet((byte) addCommand.getFlag());
                if (addCommand.getGroup() != null) {
                    cmdInfo.Group = addCommand.getGroup().length() == 0 ? null : addCommand.getGroup();
                }
                cmdInfo.ProviderName = addCommand.getProviderName().length() == 0 ? null : addCommand.getProviderName();
                cmdInfo.queryInfo = new java.util.HashMap();

                String version = command.getVersion();
                if (version == null) {

                    if (addCommand.getQueryInfo().getAttributesCount() > 0) {
                        queryInfoHashMap = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromQueryInfoObj(addCommand.getQueryInfo());
                    }

                    if (queryInfoHashMap != null) {
                        if (cmdInfo.queryInfo == null) {
                            cmdInfo.queryInfo = new java.util.HashMap();
                        }
                        cmdInfo.queryInfo.put("query-info", queryInfoHashMap);
                    }

                    if (addCommand.getTagInfo().getTagsCount() > 0) {
                        tagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromTagInfoObj(addCommand.getTagInfo());
                    }

                    if (tagHashtable != null) {
                        if (cmdInfo.queryInfo == null) {
                            cmdInfo.queryInfo = new java.util.HashMap();
                        }
                        cmdInfo.queryInfo.put("tag-info", tagHashtable);
                    }

                    if (addCommand.getNamedTagInfo().getNamesCount() > 0) {
                        if (clientManager.IsDotNetClient) {
                            namedTagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromNamedTagInfoObjFromDotNet(addCommand.getNamedTagInfo());
                        } else {
                            namedTagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromNamedTagInfoObjFromJava(addCommand.getNamedTagInfo());
                        }
                    }
                    if (namedTagHashtable != null) {
                        if (cmdInfo.queryInfo == null) {
                            cmdInfo.queryInfo = new java.util.HashMap();
                        }
                        cmdInfo.queryInfo.put("named-tag-info", namedTagHashtable);
                    }

                } else {
                    ObjectQueryInfoProtocol.ObjectQueryInfo objectQueryInfo;
                    objectQueryInfo = addCommand.getObjectQueryInfo();
                    

                    queryInfoHashMap = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromQueryInfoObj(objectQueryInfo.getQueryInfo());
                    tagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromTagInfoObj(objectQueryInfo.getTagInfo());
                    if (clientManager.IsDotNetClient) {
                        namedTagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromNamedTagInfoObjFromDotNet(addCommand.getObjectQueryInfo().getNamedTagInfo());
                    }
                     else {
                        
                        namedTagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromNamedTagInfoObjFromJava(objectQueryInfo.getNamedTagInfo());
                    }

                    if (queryInfoHashMap != null) {
                        if (cmdInfo.queryInfo == null) {
                            cmdInfo.queryInfo = new java.util.HashMap();
                        }
                        cmdInfo.queryInfo.put("query-info", queryInfoHashMap);
                    }

                    if (tagHashtable != null) {
                        if (cmdInfo.queryInfo == null) {
                            cmdInfo.queryInfo = new java.util.HashMap();
                        }
                        cmdInfo.queryInfo.put("tag-info", tagHashtable);
                    }

                    if (namedTagHashtable != null) {
                        if (cmdInfo.queryInfo == null) {
                            cmdInfo.queryInfo = new java.util.HashMap();
                        }
                        cmdInfo.queryInfo.put("named-tag-info", namedTagHashtable);
                    }
                }

                cmdInfo.RemoveCallbackId = (short) addCommand.getRemoveCallbackId();
                
                 if (addCommand.getRemoveDataFilter() != -1)
                        cmdInfo.RemoveDataFilter = (int)addCommand.getRemoveDataFilter();
                    else
                        cmdInfo.RemoveDataFilter = (int)com.alachisoft.tayzgrid.runtime.events.EventDataFilter.DataWithMetaData.getValue();
                
                cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
                cmdInfo.ResyncProviderName = addCommand.getResyncProviderName().length() == 0 ? null : addCommand.getResyncProviderName();
                if (addCommand.getSubGroup() != null) {
                    cmdInfo.SubGroup = addCommand.getSubGroup().length() == 0 ? null : addCommand.getSubGroup();
                }
               
                cmdInfo.UpdateCallbackId = (short) addCommand.getUpdateCallbackId();
                
                if (addCommand.getUpdateDataFilter() != -1)
                {
                    cmdInfo.UpdateDataFilter = addCommand.getUpdateDataFilter();
                }
                else
                    cmdInfo.UpdateDataFilter = (int)com.alachisoft.tayzgrid.runtime.events.EventDataFilter.None.getValue();

                biteString = addCommand.getDataList().toArray();
                p = new Object[biteString.length];

                for (int i = 0; i < biteString.length; i++) {
                    p[i] = ((ByteString) biteString[i]).toByteArray();
                }
                
                cmdInfo.value = new UserBinaryObject(p);
                break;

            case INSERT:
                InsertCommandProtocol.InsertCommand insertCommand = command.getInsertCommand();
                cmdInfo.Key = CacheKeyUtil.Deserialize(insertCommand.getKey(), cacheId);
                cmdInfo.KeySize = insertCommand.getKey().size();
                cmdInfo.DoAsync = insertCommand.getIsAsync();
                cmdInfo.DsItemAddedCallbackId = (short) insertCommand.getDatasourceUpdatedCallbackId();
                cmdInfo.EvictionHint = new PriorityEvictionHint(CacheItemPriority.forValue(insertCommand.getPriority()));
                cmdInfo.ExpirationHint = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetExpirationHintObj(insertCommand.getAbsExpiration(), insertCommand.getSldExpiration(), insertCommand.getIsResync(), serializationContext);
                cmdInfo.Flag = new BitSet((byte) insertCommand.getFlag());
                if (insertCommand.getGroup() != null) {
                    cmdInfo.Group = insertCommand.getGroup().length() == 0 ? null : insertCommand.getGroup();
                }
                cmdInfo.ProviderName = insertCommand.getProviderName().length() == 0 ? null : insertCommand.getProviderName();
                version = command.getVersion();

                if (version == null) {
                    if (insertCommand.getQueryInfo().getAttributesCount() > 0) {
                        queryInfoHashMap = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromQueryInfoObj(insertCommand.getQueryInfo());
                    }

                    if (queryInfoHashMap != null) {
                        if (cmdInfo.queryInfo == null) {
                            cmdInfo.queryInfo = new java.util.HashMap();
                        }
                        cmdInfo.queryInfo.put("query-info", queryInfoHashMap);
                    }

                    if (insertCommand.getTagInfo().getTagsCount() > 0) {
                        tagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromTagInfoObj(insertCommand.getTagInfo());
                    }
                    if (tagHashtable != null) {
                        if (cmdInfo.queryInfo == null) {
                            cmdInfo.queryInfo = new java.util.HashMap();
                        }
                        cmdInfo.queryInfo.put("tag-info", tagHashtable);
                    }


                    if (insertCommand.getNamedTagInfo().getTypesCount() > 0) {
                        if (clientManager.IsDotNetClient) {
                            namedTagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromNamedTagInfoObjFromDotNet(insertCommand.getNamedTagInfo());
                        } else {
                            namedTagHashtable = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetHashtableFromNamedTagInfoObjFromJava(insertCommand.getNamedTagInfo());
                        }
                    }
                    if (namedTagHashtable != null) {
                        if (cmdInfo.queryInfo == null) {
                            cmdInfo.queryInfo = new java.util.HashMap();
                        }
                        cmdInfo.queryInfo.put("named-tag-info", namedTagHashtable);
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
                        if (cmdInfo.queryInfo == null) {
                            cmdInfo.queryInfo = new java.util.HashMap();
                        }
                        cmdInfo.queryInfo.put("query-info", queryInfoHashMap);
                    }

                    if (tagHashtable != null) {
                        if (cmdInfo.queryInfo == null) {
                            cmdInfo.queryInfo = new java.util.HashMap();
                        }
                        cmdInfo.queryInfo.put("tag-info", tagHashtable);
                    }

                    if (namedTagHashtable != null) {
                        if (cmdInfo.queryInfo == null) {
                            cmdInfo.queryInfo = new java.util.HashMap();
                        }
                        cmdInfo.queryInfo.put("named-tag-info", namedTagHashtable);
                    }
                }

                cmdInfo.RemoveCallbackId = (short) insertCommand.getRemoveCallbackId();
                
                if (insertCommand.getRemoveDataFilter() != -1)
                        cmdInfo.RemoveDataFilter = (int)insertCommand.getRemoveDataFilter();
                    else
                        cmdInfo.RemoveDataFilter = (int)com.alachisoft.tayzgrid.runtime.events.EventDataFilter.DataWithMetaData.getValue();
    
                cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
                cmdInfo.ResyncProviderName = insertCommand.getResyncProviderName().length() == 0 ? null : insertCommand.getResyncProviderName();
                if (insertCommand.getSubGroup() != null) {
                    cmdInfo.SubGroup = insertCommand.getSubGroup().length() == 0 ? null : insertCommand.getSubGroup();
                }
             
                cmdInfo.UpdateCallbackId = (short) insertCommand.getUpdateCallbackId();
                 
                if (insertCommand.getUpdateDataFilter() != -1)
                {
                    cmdInfo.UpdateDataFilter = insertCommand.getUpdateDataFilter();
                }
                else
                    cmdInfo.UpdateDataFilter = (int)com.alachisoft.tayzgrid.runtime.events.EventDataFilter.None.getValue();

                cmdInfo.ItemVersion = insertCommand.getItemVersion();
                cmdInfo.LockAccessTypes = LockAccessType.forValue(insertCommand.getLockAccessType());
                cmdInfo.LockId = insertCommand.getLockId();

                biteString = insertCommand.getDataList().toArray();
                p = new Object[biteString.length];
                for (int i = 0; i < biteString.length; i++) {
                    p[i] = ((ByteString) biteString[i]).toByteArray();
                }
                cmdInfo.value = new UserBinaryObject(p);
                //No need for insert params if no special paramerter required
                if(insertCommand.getIsReplace() | insertCommand.getReturnExisting() | insertCommand.getCompareOld())
                {
                    cmdInfo.insertOptions = new InsertParams();
                    cmdInfo.insertOptions.IsReplaceOperation = insertCommand.getIsReplace();
                    cmdInfo.insertOptions.ReturnExistingValue = insertCommand.getReturnExisting();
                    cmdInfo.insertOptions.CompareOldValue = insertCommand.getCompareOld();
                    //--TODO-- Create user binary object from old value
                    if(insertCommand.getOldValueCount()> 0)
                    {
                        p = new Object[insertCommand.getOldValueCount()];
                        List<ByteString> oldValue = insertCommand.getOldValueList();
                        int i=0;
                        for(ByteString bs : oldValue)
                        {
                            p[i++] = bs.toByteArray();
                        }
                        cmdInfo.insertOptions.OldValue = new UserBinaryObject(p);
                        cmdInfo.insertOptions.OldValueFlag = new BitSet((byte)insertCommand.getOldValueFlag());
                    }
                }
                break;
        }
        return cmdInfo;

    }
    
}
