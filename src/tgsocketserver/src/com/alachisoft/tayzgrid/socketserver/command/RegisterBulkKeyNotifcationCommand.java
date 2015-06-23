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
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.RegisterBulkKeyNotifResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.RegisterBulkKeyNotifCommandProtocol;
import com.alachisoft.tayzgrid.caching.CallbackInfo;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;

import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;

import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import java.io.IOException;
public class RegisterBulkKeyNotifcationCommand extends CommandBase
{

    protected final static class CommandInfo
    {

        public int PackageSize;
        public String RequestId;
        public Object[] Keys;
        public short RemoveCallbackId;
        public short UpdateCallbackId;
        public boolean NotifyOnExpiration;
        public int dataFilter;

        public CommandInfo clone()
        {
            CommandInfo varCopy = new CommandInfo();

            varCopy.PackageSize = this.PackageSize;
            varCopy.RequestId = this.RequestId;
            varCopy.Keys = this.Keys;
            varCopy.RemoveCallbackId = this.RemoveCallbackId;
            varCopy.UpdateCallbackId = this.UpdateCallbackId;
            varCopy.NotifyOnExpiration = this.NotifyOnExpiration;
            varCopy.dataFilter = this.dataFilter;
            return varCopy;
        }
    }

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
                //PROTOBUF:RESPONSE
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
            }
            return;
        }

        try
        {
            CallbackInfo cbUpdate = null;
            CallbackInfo cbRemove = null;


            if(cmdInfo.dataFilter != -1) //Default value in protbuf set to -1
            {
                EventDataFilter datafilter = EventDataFilter.forValue(cmdInfo.dataFilter);

                cbUpdate = new CallbackInfo(clientManager.getClientID(), cmdInfo.UpdateCallbackId, datafilter);
                cbRemove = new CallbackInfo(clientManager.getClientID(), cmdInfo.RemoveCallbackId, datafilter, cmdInfo.NotifyOnExpiration);
            }
            else
            {
                cbUpdate = new CallbackInfo(clientManager.getClientID(), cmdInfo.UpdateCallbackId, EventDataFilter.None);
                cbRemove = new CallbackInfo(clientManager.getClientID(), cmdInfo.RemoveCallbackId,EventDataFilter.DataWithMetaData, cmdInfo.NotifyOnExpiration);
            }
            
            nCache.getCache().RegisterKeyNotificationCallback(cmdInfo.Keys,cbUpdate, cbRemove, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));

            //PROTOBUF:RESPONSE
            ResponseProtocol.Response response = ResponseProtocol.Response.newBuilder().setRegisterBulkKeyNotifResponse(RegisterBulkKeyNotifResponseProtocol.RegisterBulkKeyNotifResponse.newBuilder())
                    .setRequestId(Long.parseLong(cmdInfo.RequestId))
                    .setResponseType(ResponseProtocol.Response.Type.REGISTER_BULK_KEY_NOTIF).build();
            _serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));

        }
        catch (Exception exc)
        {
            //PROTOBUF:RESPONSE
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
        }
    }

    //PROTOBUF
    private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager, String serializationContext) throws IOException, ClassNotFoundException
    {
        CommandInfo cmdInfo = new CommandInfo();
        RegisterBulkKeyNotifCommandProtocol.RegisterBulkKeyNotifCommand registerBulkKeyNotifCommandBuild = command.getRegisterBulkKeyNotifCommand();
        Object[] keys = new Object[registerBulkKeyNotifCommandBuild.getKeysList().size()];
        for(int i =0;i<registerBulkKeyNotifCommandBuild.getKeysList().size();i++)
            keys[i]=CacheKeyUtil.Deserialize(registerBulkKeyNotifCommandBuild.getKeysList().get(i), serializationContext);
        cmdInfo.Keys = keys;
        cmdInfo.PackageSize = registerBulkKeyNotifCommandBuild.getKeysList().size();
        cmdInfo.RemoveCallbackId = (short) (registerBulkKeyNotifCommandBuild.getRemoveCallbackId());
        cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
        cmdInfo.UpdateCallbackId = (short) registerBulkKeyNotifCommandBuild.getUpdateCallbackId();
        cmdInfo.dataFilter = registerBulkKeyNotifCommandBuild.getDatafilter();
        cmdInfo.NotifyOnExpiration = registerBulkKeyNotifCommandBuild.getNotifyOnExpiration();
        return cmdInfo;
    }
}
