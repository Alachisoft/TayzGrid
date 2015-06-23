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
import com.alachisoft.tayzgrid.socketserver.ConnectionManager;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.alachisoft.tayzgrid.common.protobuf.GetRunningServersResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetOptimalServerCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.KeyValuePairProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.caching.Cache;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.management.CacheServer;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import java.util.Iterator;
import java.util.Map;
public class GetRunningServersCommand extends CommandBase
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
        java.util.HashMap<String, Integer> runningServers;
     
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
            
             runningServers = new java.util.HashMap<String, Integer>();
            ICommandExecuter tempVar = clientManager.getCmdExecuter();
            TayzGrid nCache = (TayzGrid)((tempVar instanceof TayzGrid) ? tempVar : null);
            runningServers = nCache.getCache().GetRunningServers(server, port);

               ResponseProtocol.Response.Builder resBuilder = ResponseProtocol.Response.newBuilder();
             GetRunningServersResponseProtocol.GetRunningServersResponse.Builder getRunningServerResponse = GetRunningServersResponseProtocol.GetRunningServersResponse.newBuilder();


            if (runningServers != null)
            {
                  Map.Entry entry;
                for (Object entryObj : (java.lang.Iterable) runningServers.entrySet())
                {
                    entry = (Map.Entry) entryObj;
                    KeyValuePairProtocol.KeyValuePair.Builder keyValue = KeyValuePairProtocol.KeyValuePair.newBuilder();
                    keyValue.setKey(entry.getKey().toString());
                    keyValue.setValue(entry.getValue().toString());

                    getRunningServerResponse.addKeyValuePair(keyValue.build());
                }
            }


            resBuilder.setGetRunningServer(getRunningServerResponse.build());
            resBuilder.setResponseType(ResponseProtocol.Response.Type.GET_RUNNING_SERVERS);
            resBuilder.setRequestId(command.getRequestID());
            _serializedResponsePackets.add(ResponseHelper.SerializeResponse(resBuilder.build()));
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
