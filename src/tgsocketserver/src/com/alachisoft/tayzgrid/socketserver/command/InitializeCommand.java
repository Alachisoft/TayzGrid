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
import com.alachisoft.tayzgrid.socketserver.SocketServer;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.alachisoft.tayzgrid.socketserver.ConnectionManager;
import com.alachisoft.tayzgrid.common.protobuf.InitializeCacheResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.InitCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationNotSupportedException;
import java.net.InetSocketAddress;

public class InitializeCommand extends CommandBase
{

    private final static class CommandInfo
    {

        public String RequestId;
        public String CacheId;
        public boolean IsDotNetClient;
        public String ClientID;
        public String LicenceCode;


        public CommandInfo clone()
        {
            CommandInfo varCopy = new CommandInfo();

            varCopy.RequestId = this.RequestId;
            varCopy.CacheId = this.CacheId;
            
            varCopy.IsDotNetClient = this.IsDotNetClient;
            varCopy.ClientID = this.ClientID;
            varCopy.LicenceCode = this.LicenceCode;


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
        catch (RuntimeException exc)
        {
            if (SocketServer.getLogger().getIsErrorLogsEnabled())
            {
                SocketServer.getLogger().getCacheLog().Error("InitializeCommand.Execute", clientManager.getClientSocket().getInetAddress().toString() + ":"
                        + clientManager.getClientSocket().getPort() + " parsing error " + exc.toString());
            }

            if (!super.immatureId.equals("-2"))
            {
                //PROTOBUF:RESPONSE
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
            }
            return;
        }

        try
        {
            clientManager.setClientID(cmdInfo.ClientID);
            clientManager.IsDotNetClient = cmdInfo.IsDotNetClient;

          
           

            byte[] bArr=null;
            clientManager.setCmdExecuter(new TayzGrid(cmdInfo.CacheId, cmdInfo.IsDotNetClient, clientManager, cmdInfo.LicenceCode, bArr, bArr));

          
            ClientManager cmgr = null;
            synchronized (ConnectionManager.ConnectionTable)
            {
                if (ConnectionManager.ConnectionTable.containsKey(clientManager.getClientID()))
                {
                    if (SocketServer.getLogger().getIsErrorLogsEnabled())
                    {
                        SocketServer.getLogger().getCacheLog().Error("InitializeCommand.Execute", "Another client with same clientID exists. Client ID is "
                                + clientManager.getClientID());
                    }
                    cmgr = (ClientManager) ((ConnectionManager.ConnectionTable.get(clientManager.getClientID()) instanceof ClientManager) ? ConnectionManager.ConnectionTable.get(clientManager.getClientID()) : null);
                    ConnectionManager.ConnectionTable.remove(clientManager.getClientID());
                }
                ConnectionManager.ConnectionTable.put(clientManager.getClientID(), clientManager);
                try
                {
                    if (cmgr != null)
                    {
                        cmgr.dispose();
                    }
                }
                catch (RuntimeException e)
                {
                    if (SocketServer.getLogger().getIsErrorLogsEnabled())
                    {
                        SocketServer.getLogger().getCacheLog().Error("InitializeCommand.Execute", " an error occured while forcefully disposing a client. " + e.toString());
                    }
                }

            }
            if (SocketServer.getLogger().getIsErrorLogsEnabled())
            {
                SocketServer.getLogger().getCacheLog().Error("InitializeCommand.Execute", clientManager.getClientID() + " is connected to " + cmdInfo.CacheId);
            }

            //PROTOBUF:RESPONSE

            ResponseProtocol.Response response = ResponseProtocol.Response.newBuilder().setInitCache(InitializeCacheResponseProtocol.InitializeCacheResponse.newBuilder().setCacheType(((TayzGrid) clientManager.getCmdExecuter()).getCache().getCacheType().toLowerCase()).setTargetCacheUniqueID(((TayzGrid) (clientManager.getCmdExecuter())).getCache().getTargetCacheUniqueID()).setExpirationType(((TayzGrid) (clientManager.getCmdExecuter())).getCache().getContext().ExpirationContract.getTypeOrdinal()).setDuration(((TayzGrid) (clientManager.getCmdExecuter())).getCache().getContext().ExpirationContract.getDuration()).setExpirationUnit(((TayzGrid) (clientManager.getCmdExecuter())).getCache().getContext().ExpirationContract.getTimeUnitOrdinal())).setRequestId(Long.parseLong(cmdInfo.RequestId)).setResponseType(ResponseProtocol.Response.Type.INIT).build();
            _serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));

            if (SocketServer.getLogger().getIsDetailedLogsEnabled())
            {
                SocketServer.getLogger().getCacheLog().Info("InitializeCommand.Execute", clientManager.getClientSocket().getInetAddress().toString() + ":"
                        + clientManager.getClientSocket().getPort() + " : " + clientManager.getClientID() + " connected to " + cmdInfo.CacheId);
            }
        }
        catch (Exception exc)
        {
            if (SocketServer.getLogger().getIsErrorLogsEnabled())
            {
                SocketServer.getLogger().getCacheLog().Error("InitializeCommand.Execute", clientManager.getClientSocket().getInetAddress().toString() + ":"
                        + clientManager.getClientSocket().getPort() + " : " + clientManager.getClientID() + " failed to connect to " + cmdInfo.CacheId + " Error: " + exc.toString());
            }
            //PROTOBUF:RESPONSE
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
        }
    }

    //PROTOBUF
    private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager)
    {
        CommandInfo cmdInfo = new CommandInfo();

        InitCommandProtocol.InitCommand initCommand = command.getInitCommand();
        cmdInfo.RequestId = String.valueOf(command.getRequestID());
        cmdInfo.CacheId = initCommand.getCacheId();
        cmdInfo.ClientID = initCommand.getClientId();
       
        cmdInfo.IsDotNetClient = initCommand.getIsDotnetClient();
        cmdInfo.LicenceCode = initCommand.getLicenceCode();
      

        cmdInfo.RequestId = (new Long(initCommand.getRequestId())).toString();

        return cmdInfo;
    }
}
