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
import com.alachisoft.tayzgrid.common.protobuf.ContainsCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ContainResponseProtocol;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;

import com.alachisoft.tayzgrid.common.util.ResponseHelper;

import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import java.io.IOException;


public class ContainsCommand extends CommandBase
{

    private final static class CommandInfo
    {

        public String RequestId;
        public Object Key;

        public CommandInfo clone()
        {
            CommandInfo varCopy = new CommandInfo();

            varCopy.RequestId = this.RequestId;
            varCopy.Key = this.Key;

            return varCopy;
        }
    }

    //PROTOBUF
    @Override
    public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command)
    {
        CommandInfo cmdInfo = new CommandInfo();

        byte[] data = null;
        ICommandExecuter tempVar = clientManager.getCmdExecuter();
        TayzGrid nCache = (TayzGrid) ((tempVar instanceof TayzGrid) ? tempVar : null);

        try
        {
            cmdInfo = ParseCommand(command, clientManager, nCache.getCacheId()).clone();
            if (ServerMonitor.getMonitorActivity())
            {
                ServerMonitor.LogClientActivity("ContCmd.Exec", "cmd parsed");
            }

        }
        catch (Exception exc)
        {
            if (!super.immatureId.equals("-2"))
            //PROTOBUF:RESPONSE
            {
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
            }
            return;
        }

        
        try
        {
            boolean exists = nCache.getCache().Contains(cmdInfo.Key, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));

            ResponseProtocol.Response response = ResponseProtocol.Response.newBuilder().setContain(ContainResponseProtocol.ContainResponse.newBuilder()
                                                            .setExists(exists).build())
                                                            .setRequestId(Long.parseLong(cmdInfo.RequestId))
                                                            .setResponseType(ResponseProtocol.Response.Type.CONTAINS).build();
            _serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));
        }
        catch (Exception exc)
        {
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
        }
        if (ServerMonitor.getMonitorActivity())
        {
            ServerMonitor.LogClientActivity("ContCmd.Exec", "cmd executed on cache");
        }

    }

    //PROTOBUF
    private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager, String serializationContext) throws IOException, ClassNotFoundException
    {
        CommandInfo cmdInfo = new CommandInfo();

        ContainsCommandProtocol.ContainsCommand containsCommand = command.getContainsCommand();

        cmdInfo.Key = CacheKeyUtil.Deserialize(containsCommand.getKey(), serializationContext);
        cmdInfo.RequestId = (new Long(command.getRequestID())).toString();

        return cmdInfo;
    }
}
