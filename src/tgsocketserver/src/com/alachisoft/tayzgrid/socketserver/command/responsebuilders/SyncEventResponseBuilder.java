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


package com.alachisoft.tayzgrid.socketserver.command.responsebuilders;

import com.alachisoft.tayzgrid.caching.CallbackInfo;

import com.alachisoft.tayzgrid.persistence.Event;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;

import com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol;
import com.alachisoft.tayzgrid.common.protobuf.EventInfoProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.SyncEventsResponseProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.persistence.Event;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class SyncEventResponseBuilder extends ResponseBuilderBase
{
    public static java.util.List<byte[]> BuildResponse(java.util.ArrayList<Event> events, String requestId, java.util.List<byte[]> serializedResponse, String clientId, String serializationContext) throws IOException
    {
        long requestID = Long.parseLong(requestId);

        ResponseProtocol.Response.Builder response = ResponseProtocol.Response.newBuilder();
        com.alachisoft.tayzgrid.common.protobuf.SyncEventsResponseProtocol.SyncEventsResponse.Builder syncEventResponse = SyncEventsResponseProtocol.SyncEventsResponse.newBuilder();
        response.setSyncEventsResponse(syncEventResponse);
        response.setRequestId(requestID);
        response.setResponseType(com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol.Response.Type.SYNC_EVENTS);

        for (Event evt : events)
        {
            com.alachisoft.tayzgrid.common.protobuf.EventInfoProtocol.EventInfo.Builder evtInfo = EventInfoProtocol.EventInfo.newBuilder();
            com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId.Builder eventId = EventIdProtocol.EventId.newBuilder();

            eventId.setEventUniqueId(evt.getPersistedEventId().getEventUniqueID());
            eventId.setEventCounter (evt.getPersistedEventId().getEventCounter());
            eventId.setOperationCounter(evt.getPersistedEventId().getOperationCounter());

            evtInfo.setEventId(eventId);

            switch (evt.getPersistedEventId().getEventType())
            {
                case CACHE_CLEARED_EVENT:
                        evtInfo.setEventType(EventInfoProtocol.EventInfo.EventType.CACHE_CLEARED_EVENT);
                        break;

                case ITEM_ADDED_EVENT:
                        evtInfo.setKey(CacheKeyUtil.toByteString(evt.getPersistedEventInfo().getKey(), serializationContext));
                        evtInfo.setEventType(com.alachisoft.tayzgrid.common.protobuf.EventInfoProtocol.EventInfo.EventType.ITEM_ADDED_EVENT);
                        break;

                case ITEM_REMOVED_CALLBACK:
                        evtInfo.setKey(CacheKeyUtil.toByteString(evt.getPersistedEventInfo().getKey(), serializationContext));
                        evtInfo.setFlag(evt.getPersistedEventInfo().getFlag().getData());
                        for (Iterator it = evt.getPersistedEventInfo().getCallBackInfoList().iterator(); it.hasNext();) 
                        {
                            CallbackInfo cbInfo =(CallbackInfo)it.next();
                            if (cbInfo.getClient().equals(clientId))
                            {
                                 evtInfo.setCallbackId((Integer)cbInfo.getCallback());
                            }
                        }
                        
                        evtInfo.addAllValue((ArrayList)evt.getPersistedEventInfo().getValue());
                        evtInfo.setItemRemoveReason(evt.getPersistedEventInfo().getReason().getValue());
                        evtInfo.setEventType(com.alachisoft.tayzgrid.common.protobuf.EventInfoProtocol.EventInfo.EventType.ITEM_REMOVED_CALLBACK);
                        break;

                case ITEM_REMOVED_EVENT:
                        evtInfo.setKey(CacheKeyUtil.toByteString(evt.getPersistedEventInfo().getKey(), serializationContext));
                        evtInfo.setEventType(com.alachisoft.tayzgrid.common.protobuf.EventInfoProtocol.EventInfo.EventType.ITEM_REMOVED_EVENT);
                        break;

                case ITEM_UPDATED_CALLBACK:
                        evtInfo.setKey(CacheKeyUtil.toByteString(evt.getPersistedEventInfo().getKey(), serializationContext));
                        for (Iterator it = evt.getPersistedEventInfo().getCallBackInfoList().iterator(); it.hasNext();) 
                        {
                            CallbackInfo cbInfo = (CallbackInfo)it.next();
                            if (cbInfo.getClient().equals(clientId))
                            {
                                evtInfo.setCallbackId((Integer)cbInfo.getCallback());
                            }
                        }
                        evtInfo.setEventType(com.alachisoft.tayzgrid.common.protobuf.EventInfoProtocol.EventInfo.EventType.ITEM_UPDATED_CALLBACK);
                        break;

                case ITEM_UPDATED_EVENT:
                        evtInfo.setKey(CacheKeyUtil.toByteString(evt.getPersistedEventInfo().getKey(), serializationContext));
                        evtInfo.setEventType(com.alachisoft.tayzgrid.common.protobuf.EventInfoProtocol.EventInfo.EventType.ITEM_UPDATED_EVENT);
                        break;

            }

            response.setSyncEventsResponse(syncEventResponse.addEventInfo(evtInfo));

        }
        serializedResponse.add(ResponseHelper.SerializeResponse(response.build()));
        return serializedResponse;
    }

 }
