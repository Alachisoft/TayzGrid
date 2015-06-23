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

import com.alachisoft.tayzgrid.persistence.EventType;
import com.alachisoft.tayzgrid.caching.queries.QueryChangeType;
import com.alachisoft.tayzgrid.caching.EventStatus;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.command.responsebuilders.SyncEventResponseBuilder;
import com.alachisoft.tayzgrid.socketserver.ICommandExecuter;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command;
import com.alachisoft.tayzgrid.common.protobuf.EventIdCommandProtocol.EventIdCommand;
import com.alachisoft.tayzgrid.common.protobuf.SyncEventsCommandProtocol;

public class SyncEventCommand extends CommandBase
{
    protected final static class CommandInfo
    {
        public java.util.HashMap EventsList;
        public int CommandVersion;
        public String RequestId;

        public CommandInfo clone()
        {
            CommandInfo varCopy = new CommandInfo();

            varCopy.EventsList = this.EventsList;
            varCopy.CommandVersion = this.CommandVersion;
            varCopy.RequestId = this.RequestId;

            return varCopy;
        }
    }

    private OperationResult _syncEventResult = getOperationResult().Success;

    @Override
    public OperationResult getOperationResult()
    {
        return _syncEventResult;
    }


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
            _syncEventResult = getOperationResult().Failure;
            if (!super.immatureId.equals("-2"))
            {
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
            }
            return;
        }

        byte[] data = null;

        try
        {
            ICommandExecuter tempVar = clientManager.getCmdExecuter();
            TayzGrid nCache = (TayzGrid)((tempVar instanceof TayzGrid) ? tempVar : null);
              
            EventStatus eventStatus = nCache.GetEventsStatus();
            java.util.ArrayList<com.alachisoft.tayzgrid.persistence.Event> syncEventResult = nCache.getCache().GetFilteredEvents(clientManager.getClientID(), cmdInfo.EventsList, eventStatus);
            SyncEventResponseBuilder.BuildResponse(syncEventResult, cmdInfo.RequestId, _serializedResponsePackets, clientManager.getClientID(), nCache.getCacheId());

        }
        catch (Exception exc)
        {
                _syncEventResult = getOperationResult().Failure;
                //PROTOBUF:RESPONSE
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
        }
    }

    //PROTOBUF
    private CommandInfo ParseCommand(Command command, ClientManager clientManager)
    {
        CommandInfo cmdInfo = new CommandInfo();
        java.util.HashMap eventList = new java.util.HashMap();
        SyncEventsCommandProtocol.SyncEventsCommand syncEventsCommand = command.getSyncEventsCommand();
        java.util.ArrayList<EventIdCommand> eventIds = (java.util.ArrayList)syncEventsCommand.getEventIdsList();
        com.alachisoft.tayzgrid.caching.EventId cacheEventId = null;
        
        for (EventIdCommand eventId : eventIds)
        {
                cacheEventId = new com.alachisoft.tayzgrid.caching.EventId();
                cacheEventId.setEventUniqueID(eventId.getEventUniqueId());
                cacheEventId.setEventCounter(eventId.getEventCounter());
                cacheEventId.setOperationCounter(eventId.getOperationCounter());
                cacheEventId.setEventType((EventType.forValue(eventId.getEventType())));
                cacheEventId.setQueryChangeType((QueryChangeType.forValue(eventId.getQueryChangeType())));
                cacheEventId.setQueryId(eventId.getQueryId());

                if (cacheEventId.getQueryId() != null)
                {
                    cacheEventId.setQueryId(null);
                }

                eventList.put(cacheEventId, null);
        }
        cmdInfo.EventsList = eventList;
        cmdInfo.RequestId = Long.toString(syncEventsCommand.getRequestId());
        return cmdInfo;
    }

}
