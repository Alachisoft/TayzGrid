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

package com.alachisoft.tayzgrid.socketserver.eventtask;

import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.ConnectionManager;
import com.alachisoft.tayzgrid.common.protobuf.BulkEventItemResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.BulkEventResponseProtocol;
import com.alachisoft.tayzgrid.caching.EventContext;
import com.alachisoft.tayzgrid.socketserver.util.EventHelper;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
/**
 This event is fired when cache is cleared
*/
public final class CacheClearedEvent implements IEventTask 
{
    private String _cacheId;
    private String _clientId;
    private EventContext _eventContext;

    public CacheClearedEvent(String cacheId, String clientId, EventContext eventContext) 
    {
        _cacheId = cacheId;
        _clientId = clientId;
        _eventContext = eventContext;
    }

    public void Process() 
    {
        ClientManager clientManager = null;

        synchronized (ConnectionManager.ConnectionTable) 
        {
            clientManager = (ClientManager)ConnectionManager.ConnectionTable.get(_clientId);
        }

        if (clientManager != null)
        {
            ResponseProtocol.Response.Builder response = ResponseProtocol.Response.newBuilder();
            
            BulkEventItemResponseProtocol.BulkEventItemResponse.Builder eventItems =com.alachisoft.tayzgrid.common.protobuf.BulkEventItemResponseProtocol.BulkEventItemResponse.newBuilder();
            eventItems.setEventType(BulkEventItemResponseProtocol.BulkEventItemResponse.EventType.CACHE_CLEARED_EVENT);
            eventItems.setCacheClearedEvent(EventHelper.GetCacheClearedResponse(_eventContext));            
            
            BulkEventResponseProtocol.BulkEventResponse.Builder eventResponse=com.alachisoft.tayzgrid.common.protobuf.BulkEventResponseProtocol.BulkEventResponse.newBuilder();            
            eventResponse.addEventList(eventItems);
            
            response.setBulkEventResponse(eventResponse);
            response.setResponseType(ResponseProtocol.Response.Type.BULK_EVENT);

            byte[] serializedResponse = ResponseHelper.SerializeResponse(response.build());

            ConnectionManager.AssureSend(clientManager, serializedResponse);
        }
    }
}
