/*
* Copyright (c) 2015, Alachisoft. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License aet
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
import com.alachisoft.tayzgrid.common.protobuf.AddResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;

public class AddCommand extends AddAndInsertCommandBase
{

    private OperationResult _addResult = OperationResult.Success;

    @Override
    public OperationResult getOperationResult()
    {
        return _addResult;
    }

    //PROTOBUF
    @Override
    public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) throws java.io.IOException, ClassNotFoundException
    {
        CommandInfo cmdInfo = new CommandInfo();
        ICommandExecuter tempVar = clientManager.getCmdExecuter();
        TayzGrid nCache = (TayzGrid) ((tempVar instanceof TayzGrid) ? tempVar : null);
        try
        {
            serializationContext = nCache.getCacheId();
            cmdInfo = super.ParseCommand(command, clientManager, serializationContext);
            if (ServerMonitor.getMonitorActivity())
            {
                ServerMonitor.LogClientActivity("AddCmd.Exec", "cmd parsed");
            }
        }
        catch (Exception exc)
        {
            _addResult = OperationResult.Failure;
            {
                //PROTOBUF:RESPONSE
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
            }

            return;
        }

        CallbackEntry callbackEntry = null;

        if (cmdInfo.UpdateCallbackId != -1 || cmdInfo.RemoveCallbackId != -1 || (!cmdInfo.RequestId.equals("-1") && cmdInfo.DoAsync) || cmdInfo.DsItemAddedCallbackId != -1)
        {
            callbackEntry = new CallbackEntry(clientManager.getClientID(), (cmdInfo.RequestId != null) ? Integer.decode(cmdInfo.RequestId) : 0, cmdInfo.value, cmdInfo.RemoveCallbackId, cmdInfo.UpdateCallbackId, (cmdInfo.RequestId
                    != null) ? (short) (cmdInfo.RequestId.equals("-1") ? - 1 : 0) : 0, cmdInfo.DsItemAddedCallbackId, cmdInfo.Flag, EventDataFilter.forValue(cmdInfo.UpdateDataFilter),
                    EventDataFilter.forValue(cmdInfo.RemoveDataFilter));
        }


        if (!cmdInfo.DoAsync)
        {
            try
            {
                OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                operationContext.Add(OperationContextFieldName.KeySize, cmdInfo.KeySize);
                operationContext.Add(OperationContextFieldName.ValueDataSize, ((UserBinaryObject)cmdInfo.value).getSize());
                
                nCache.getCache().Add(cmdInfo.Key, callbackEntry == null ? cmdInfo.value : (Object) callbackEntry, cmdInfo.ExpirationHint,  cmdInfo.EvictionHint, cmdInfo.Group, cmdInfo.SubGroup, cmdInfo.queryInfo, cmdInfo.Flag, cmdInfo.ProviderName, cmdInfo.ResyncProviderName, operationContext, null);

                ResponseProtocol.Response response = ResponseProtocol.Response.newBuilder().setAddResponse(AddResponseProtocol.AddResponse.newBuilder()).setRequestId(command.getRequestID()).setResponseType(ResponseProtocol.Response.Type.ADD).build();
                //PROTOBUF:RESPONSE.

                _serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));
            }
            catch (Exception exc)
            {
                _addResult = OperationResult.Failure;
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
            }
            if (ServerMonitor.getMonitorActivity())
            {
                ServerMonitor.LogClientActivity("AddCmd.Exec", "cmd executed on cache");
            }

        }
        else
        {
            OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
            operationContext.Add(OperationContextFieldName.KeySize, cmdInfo.KeySize);
            operationContext.Add(OperationContextFieldName.ValueDataSize, ((UserBinaryObject)cmdInfo.value).getSize());
            
            nCache.getCache().AddAsync(cmdInfo.Key, callbackEntry == null ? (Object) cmdInfo.value : (Object) callbackEntry, cmdInfo.ExpirationHint,  cmdInfo.EvictionHint, cmdInfo.Group, cmdInfo.SubGroup, cmdInfo.Flag, cmdInfo.queryInfo, cmdInfo.ProviderName, cmdInfo.ResyncProviderName, operationContext);
        }
    }
}
