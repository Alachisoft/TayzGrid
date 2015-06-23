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

import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.CompressedValueEntry;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.common.InsertResult;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.protobuf.InsertReponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.ICommandExecuter;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.google.protobuf.ByteString;

public class InsertCommand extends AddAndInsertCommandBase
{

    private OperationResult _insertResult = OperationResult.Success;

    @Override
    public OperationResult getOperationResult()
    {
        return _insertResult;
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
            serializationContext = nCache.getCacheId();
            cmdInfo = ParseCommand(command, clientManager, serializationContext);
            if (ServerMonitor.getMonitorActivity())
            {
                ServerMonitor.LogClientActivity("InsCmd.Exec", "cmd parsed");
            }

        }
        catch (Exception exc)
        {
            _insertResult = OperationResult.Failure;
            {
                //PROTOBUF:RESPONSE
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
            }
            return;
        }

        CallbackEntry callbackEntry = null;

        if (cmdInfo.UpdateCallbackId != -1 || cmdInfo.RemoveCallbackId != -1 || (!cmdInfo.RequestId.equals("-1") && cmdInfo.DoAsync) || cmdInfo.DsItemAddedCallbackId != -1)
        {
            callbackEntry = new CallbackEntry(clientManager.getClientID(), Integer.decode(cmdInfo.RequestId), cmdInfo.value, (short) cmdInfo.RemoveCallbackId, (short) cmdInfo.UpdateCallbackId, (short) (cmdInfo.RequestId.equals("-1") ? - 1 : 0), (short) cmdInfo.DsItemAddedCallbackId, cmdInfo.Flag,EventDataFilter.forValue(cmdInfo.UpdateDataFilter),
                    EventDataFilter.forValue(cmdInfo.RemoveDataFilter));
        }
        if (!cmdInfo.DoAsync)
        {
            try
            {
                OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                operationContext.Add(OperationContextFieldName.KeySize, cmdInfo.KeySize);
                operationContext.Add(OperationContextFieldName.ValueDataSize, ((UserBinaryObject)cmdInfo.value).getSize());
                
                if(cmdInfo.insertOptions != null)
                    operationContext.Add(OperationContextFieldName.InsertParams, cmdInfo.insertOptions);

                InsertResult result = nCache.getCache().Insert(cmdInfo.Key, callbackEntry == null ? (Object) cmdInfo.value : (Object) callbackEntry, cmdInfo.ExpirationHint,  cmdInfo.EvictionHint, cmdInfo.Group, cmdInfo.SubGroup, cmdInfo.queryInfo, cmdInfo.Flag, cmdInfo.LockId, cmdInfo.ItemVersion, cmdInfo.LockAccessTypes, cmdInfo.ProviderName, cmdInfo.ResyncProviderName, operationContext);

                
                CompressedValueEntry flagValueEntry = (CompressedValueEntry)result.ExistingValue;
                
                UserBinaryObject ubObject=null;
                if (flagValueEntry != null) {
                    if (flagValueEntry.Value instanceof CallbackEntry) {
                        ubObject = (UserBinaryObject) ((CallbackEntry) flagValueEntry.Value).getValue();
                    } else if (flagValueEntry.Value instanceof UserBinaryObject) {
                        ubObject = (UserBinaryObject) flagValueEntry.Value;
                    }
                }
                
               // UserBinaryObject ubObject = (flagValueEntry == null) ? null : (UserBinaryObject)flagValueEntry.Value;


                InsertReponseProtocol.InsertResponse.Builder insertResponseBuilder;
                
                if(result==null)
                    insertResponseBuilder = InsertReponseProtocol.InsertResponse.newBuilder().setVersion(0);
                else //--TODO-- set existing value
                    insertResponseBuilder = InsertReponseProtocol.InsertResponse.newBuilder().setVersion(result.Version).setSuccess(result.Success);
                                
                if (ubObject != null) 
                {
                    for (int i = 0; i < ubObject.getDataList().size(); i++)
                    {
                        Object[] test=ubObject.getData();
                        insertResponseBuilder.addExistingValue(ByteString.copyFrom((byte[])test[i]));
                    }

                    insertResponseBuilder.setFlag(flagValueEntry.Flag.getData());
		}

                //PROTOBUF:RESPONSE
                 ResponseProtocol.Response response = ResponseProtocol.Response.newBuilder().setInsert(insertResponseBuilder).setRequestId(command.getRequestID()).setResponseType(ResponseProtocol.Response.Type.INSERT).build();
                _serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));

            }
            catch (Exception exc)
            {
                _insertResult = OperationResult.Failure;
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
            }
            finally
            {
                if (ServerMonitor.getMonitorActivity())
                {
                    ServerMonitor.LogClientActivity("InsCmd.Exec", "cmd executed on cache");
                }
            }

        }
        else
        {
            OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
            operationContext.Add(OperationContextFieldName.KeySize, cmdInfo.KeySize);
            operationContext.Add(OperationContextFieldName.ValueDataSize, ((UserBinaryObject)cmdInfo.value).getSize());

            nCache.getCache().InsertAsync(cmdInfo.Key, callbackEntry == null ? (Object) cmdInfo.value : (Object) callbackEntry, cmdInfo.ExpirationHint, cmdInfo.EvictionHint, cmdInfo.Group, cmdInfo.SubGroup, cmdInfo.Flag, cmdInfo.queryInfo, cmdInfo.ProviderName, cmdInfo.ResyncProviderName, operationContext);
        }
    }
}
