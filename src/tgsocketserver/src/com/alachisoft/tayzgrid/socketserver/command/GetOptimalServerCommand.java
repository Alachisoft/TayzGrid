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

import com.alachisoft.tayzgrid.socketserver.CacheProvider;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.ConnectionManager;
import com.alachisoft.tayzgrid.common.protobuf.GetOptimalServerCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetOptimalServerResponseProtocol;
import com.alachisoft.tayzgrid.caching.Cache;
import com.alachisoft.tayzgrid.management.CacheServer;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentNullException;

public class GetOptimalServerCommand extends CommandBase
{

    private final static class CommandInfo
    {

        public String RequestId;
        public String CacheId;
        
        public boolean IsDotNetClient;

        public CommandInfo clone()
        {
            CommandInfo varCopy = new CommandInfo();

            varCopy.RequestId = this.RequestId;
            varCopy.CacheId = this.CacheId;
            
            varCopy.IsDotNetClient = this.IsDotNetClient;

            return varCopy;
        }
    }

    //PROTOBUF
    @Override
    public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command)
    {
        CommandInfo cmdInfo = new CommandInfo();
        try
        {
            cmdInfo = ParseCommand(command, clientManager).clone();
        }
        catch (Exception exc)
        {
            if (!super.immatureId.equals("-2"))
            {
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
            }
            return;
        }

        Cache cache = null;

        try
        {
            String server = ConnectionManager.getServerIpAddress();
            int port = CacheServer.getSocketServerPort();
            byte[] bArr = null;
                cache = CacheProvider.getProvider().GetCacheInstanceIgnoreReplica(cmdInfo.CacheId, bArr, bArr);

            if (cache == null)
            {
                throw new Exception("Cache is not registered");
            }
            if (!cache.getIsRunning())
            {
                throw new Exception("Cache is not running");
            }

            ResponseProtocol.Response response = ResponseProtocol.Response.newBuilder().setGetOptimalServer(GetOptimalServerResponseProtocol.GetOptimalServerResponse.newBuilder().setServer(server).setPort(port)).setRequestId(Long.parseLong(cmdInfo.RequestId)).setResponseType(ResponseProtocol.Response.Type.GET_OPTIMAL_SERVER).build();
            
            _serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));
        }
        catch (ArgumentNullException exc)
        {
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
        }
        catch (Exception exc)
        {
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
        }
    }


    //PROTOBUF
    private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager)
    {
        CommandInfo cmdInfo = new CommandInfo();

        GetOptimalServerCommandProtocol.GetOptimalServerCommand getOptimalServerCommand = command.getGetOptimalServerCommand();

        cmdInfo.CacheId = getOptimalServerCommand.getCacheId();
        cmdInfo.IsDotNetClient = getOptimalServerCommand.getIsDotnetClient();
       
        cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
        
        return cmdInfo;
    }
    
}
