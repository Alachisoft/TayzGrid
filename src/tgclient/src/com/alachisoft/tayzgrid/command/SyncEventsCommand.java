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

 
package com.alachisoft.tayzgrid.command;
	
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.EventIdCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId;
import com.alachisoft.tayzgrid.common.protobuf.SyncEventsCommandProtocol;

public final class SyncEventsCommand extends Command
{
    private com.alachisoft.tayzgrid.common.protobuf.SyncEventsCommandProtocol.SyncEventsCommand.Builder _syncEventCommand;
    private com.alachisoft.tayzgrid.common.protobuf.EventIdCommandProtocol _eventIdCommand;
 

    public SyncEventsCommand(java.util.ArrayList<com.alachisoft.tayzgrid.caching.EventId> eventIds)
    {
        super.name = "SyncEventsCommand";
        _syncEventCommand = SyncEventsCommandProtocol.SyncEventsCommand.newBuilder();
        _syncEventCommand.setRequestId(super.getRequestId());

        for (com.alachisoft.tayzgrid.caching.EventId eventId : eventIds)
        {
            com.alachisoft.tayzgrid.common.protobuf.EventIdCommandProtocol.EventIdCommand.Builder _eventIdCommand = EventIdCommandProtocol.EventIdCommand.newBuilder();
            _eventIdCommand.setEventType(eventId.getEventType().getValue());
            _eventIdCommand.setEventUniqueId(eventId.getEventUniqueID());
            _eventIdCommand.setEventCounter(eventId.getEventCounter());
            _eventIdCommand.setOperationCounter(eventId.getOperationCounter());
            _eventIdCommand.setQueryChangeType(eventId.getQueryChangeType().getValue());
            _eventIdCommand.setQueryId(eventId.getQueryId());

            _syncEventCommand.addEventIds(_eventIdCommand);
        }
    }

    @Override
    public CommandType getCommandType()
    {
            return getCommandType().SYNC_EVENTS;
    }

    @Override
    protected void createCommand() throws CommandException 
    {
        CommandProtocol.Command.Builder commandBuilder = CommandProtocol.Command.newBuilder();

        commandBuilder.setRequestID(this.getRequestId());
        commandBuilder.setSyncEventsCommand(_syncEventCommand);
        commandBuilder.setType(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command.Type.SYNC_EVENTS);
    }

    @Override
    protected boolean parseCommand() {
        return true;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.InternalCommand;
    }
}
