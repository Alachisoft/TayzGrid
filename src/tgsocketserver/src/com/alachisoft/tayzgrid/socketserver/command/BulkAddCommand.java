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
import com.alachisoft.tayzgrid.common.protobuf.BulkAddResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.KeyExceptionPackageResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.common.protobuf.BulkAddResponseProtocol.BulkAddResponse.Builder;

public class BulkAddCommand extends BulkAddAndInsertCommandBase
{

    private OperationResult _addBulkResult = OperationResult.Success;

    @Override
    public OperationResult getOperationResult()
    {
        return _addBulkResult;
    }

    @Override
    public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) throws Exception
    {
        CommandInfo cmdInfo = new CommandInfo();
        setClientId(clientManager.getClientID());

        ICommandExecuter tempVar = clientManager.getCmdExecuter();
        TayzGrid nCache = (TayzGrid) ((tempVar instanceof TayzGrid) ? tempVar : null);
        try
        {
            serailizationContext = nCache.getCacheId();
            cmdInfo = super.ParseCommand(command, clientManager, serailizationContext);
            if (ServerMonitor.getMonitorActivity())
            {
                ServerMonitor.LogClientActivity("BulkAddCmd.Exec", "cmd parsed");
            }
        }
        catch (Exception exc)
        {
            _addBulkResult = OperationResult.Failure;
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
            return;
        }

        byte[] dataPackage = null;

        try
        {
            //PROTOBUF:RESPONSE
            OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
            
            operationContext.Add(OperationContextFieldName.ClientLastViewId, cmdInfo.ClientLastViewId);

            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(cmdInfo.IntendedRecipient))
            {
                operationContext.Add(OperationContextFieldName.IntendedRecipient, cmdInfo.IntendedRecipient);
            }
            operationContext.Add(OperationContextFieldName.KeySize, cmdInfo.KeySizes);
            
            long[] size = new long[cmdInfo.Values.length];            
            for(int i=0; i<cmdInfo.Values.length; i++)  {
                size[i] =  ((UserBinaryObject)cmdInfo.Values[i]).getSize();
            }           
           
            operationContext.Add(OperationContextFieldName.ValueDataSize, size);
            
            java.util.HashMap addResult = (java.util.HashMap) nCache.getCache().Add(cmdInfo.Keys, cmdInfo.Values, cmdInfo.CallbackEnteries, cmdInfo.ExpirationHint,  cmdInfo.EvictionHint, cmdInfo.groupInfos, cmdInfo.QueryInfo, cmdInfo.Flags, cmdInfo.ProviderName, cmdInfo.resyncProviderName, operationContext);

            KeyExceptionPackageResponseProtocol.KeyExceptionPackageResponse.Builder keyExc = KeyExceptionPackageResponseProtocol.KeyExceptionPackageResponse.newBuilder();
            com.alachisoft.tayzgrid.socketserver.util.KeyPackageBuilder.PackageKeysExceptions(addResult, keyExc, nCache.getCacheId());


            Builder bulkAddResponse = BulkAddResponseProtocol.BulkAddResponse.newBuilder().setKeyExceptionPackage(keyExc);

            ResponseProtocol.Response response = ResponseProtocol.Response.newBuilder().setBulkAdd(bulkAddResponse.build())
                    .setRequestId(Long.parseLong(cmdInfo.RequestId))
                    .setIntendedRecipient(cmdInfo.IntendedRecipient)
                    .setResponseType(ResponseProtocol.Response.Type.ADD_BULK).build();

            _serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));


        }
        catch (Exception exc)
        {
            _addBulkResult = OperationResult.Failure;
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
        }
        if (ServerMonitor.getMonitorActivity())
        {
            ServerMonitor.LogClientActivity("BulkAddCmd.Exec", "cmd executed on cache");
        }

    }
}
