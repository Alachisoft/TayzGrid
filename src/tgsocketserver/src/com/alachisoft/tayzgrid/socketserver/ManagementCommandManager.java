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


package com.alachisoft.tayzgrid.socketserver;

import com.alachisoft.tayzgrid.caching.SocketServerStats;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.command.ManagementCommand;
import com.alachisoft.tayzgrid.socketserver.command.ManagementCommandBase;
import com.alachisoft.tayzgrid.socketserver.command.OperationResult;
import com.alachisoft.tayzgrid.socketserver.ConnectionManager;
import com.alachisoft.tayzgrid.socketserver.ICommandManager;
import com.alachisoft.tayzgrid.socketserver.SocketServer;
import com.alachisoft.tayzgrid.common.protobuf.ManagementCommandProtocol;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Iterator;

public class ManagementCommandManager implements ICommandManager
{

    @Override
    public Object Deserialize(byte[] buffer, long CommandSize)
    {
        Object obj = null;
        try
        {
            return (Object) ManagementCommandProtocol.ManagementCommand.parseFrom(buffer);
        }
        catch (InvalidProtocolBufferException invalidProtocolBufferException)
        {
        }
        return obj;
    }

    @Override
    public void ProcessCommand(ClientManager clientManager, Object cmd) throws Exception
    {
        ManagementCommandProtocol.ManagementCommand command = cmd instanceof ManagementCommandProtocol.ManagementCommand ? (ManagementCommandProtocol.ManagementCommand) cmd : null;

        if (ServerMonitor.getMonitorActivity())
        {
            ServerMonitor.LogClientActivity("CmdMgr.PrsCmd", "enter");
        }
        if (ServerMonitor.getMonitorActivity())
        {
            ServerMonitor.LogClientActivity("CmdMgr.PrsCmd", "" + command);
        }

        ManagementCommandBase inComingCommand = new ManagementCommand();
        if(SocketServer.getIsServerCounterEnabled())
        {
            SocketServer.getPerfStatsColl().mSecPerCacheOperationBeginSample();
        }

        inComingCommand.ExecuteCommand(clientManager, command);

        if (SocketServer.getIsServerCounterEnabled())
        {
            SocketServer.getPerfStatsColl().mSecPerCacheOperationEndSample();
        }

        if (clientManager != null && inComingCommand.getOperationResult() == OperationResult.Success)
        {
            if (clientManager.getCmdExecuter() != null)
            {
                clientManager.getCmdExecuter().UpdateSocketServerStats(new SocketServerStats(clientManager.getClientsRequests(), clientManager.getClientsBytesSent(), clientManager.getClientsBytesRecieved()));
            }
        }

        if (clientManager != null && inComingCommand.getSerializedResponsePackets() != null && !clientManager.getIsCacheStopped())
        {

            if (SocketServer.getIsServerCounterEnabled())
            {
                SocketServer.getPerfStatsColl().incrementResponsesPerSecStats(1);
            }

            for (Iterator packet = inComingCommand.getSerializedResponsePackets().iterator(); packet.hasNext();)
            {
                byte[] response = (byte[]) packet.next();
                ConnectionManager.AssureSend(clientManager, response);
            }
        }
    }
}
