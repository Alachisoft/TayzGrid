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
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.caching.EventContext;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.socketserver.util.EventHelper;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.common.protobuf.BulkEventItemResponseProtocol.BulkEventItemResponse;
import com.alachisoft.tayzgrid.common.protobuf.BulkEventResponseProtocol.BulkEventResponse;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 This event is fired when an item is added to cache
*/

/**
 This event is fired when an item is removed from cache
*/
public final class ItemRemovedEvent implements IEventTask 
{
    private Object _key;
    private String _cacheId;
    private ItemRemoveReason _reason = ItemRemoveReason.values()[0];
    private UserBinaryObject _value;
    private String _clientId;
    private BitSet _flag;
    private EventContext _eventContext;
    private EventDataFilter _removeDataFilter;

    public ItemRemovedEvent(Object key, String cacheId, ItemRemoveReason reason, UserBinaryObject value, String clientId, BitSet Flag, EventContext eventContext,EventDataFilter removeDataFilter)
    {
            _key = key;
            _cacheId = cacheId;
            _reason = reason;
            _value = value;
            _clientId = clientId;
            _flag = Flag;
            _eventContext = eventContext;
            _removeDataFilter=removeDataFilter;
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
                
                BulkEventItemResponse.Builder eventItems=com.alachisoft.tayzgrid.common.protobuf.BulkEventItemResponseProtocol.BulkEventItemResponse.newBuilder();
                
                eventItems.setEventType(BulkEventItemResponse.EventType.ITEM_REMOVED_EVENT);
            try {                                              
                eventItems.setItemRemovedEvent(EventHelper.GetItemRemovedEventResponse(_eventContext, _key, _removeDataFilter, _flag, _reason, (UserBinaryObject)_value, _cacheId));
            } catch (IOException ex) {
                //suppressing I/O exception,(caused by key serialization), not expected to occur
            }
                
                BulkEventResponse.Builder eventResponse=com.alachisoft.tayzgrid.common.protobuf.BulkEventResponseProtocol.BulkEventResponse.newBuilder();
                
                eventResponse.addEventList(eventItems);
                
               
                response.setBulkEventResponse(eventResponse);
                response.setResponseType(ResponseProtocol.Response.Type.BULK_EVENT);

                byte[] serializedResponse = ResponseHelper.SerializeResponse(response.build());

                ConnectionManager.AssureSend(clientManager, serializedResponse);

                
        }
    }
}
