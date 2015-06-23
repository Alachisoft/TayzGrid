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

package com.alachisoft.tayzgrid.socketserver.callbacktasks;

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

public final class ItemRemoveCallback implements ICallbackTask {
    
private short _id;
private Object _key;
private UserBinaryObject _value;
private BitSet _flag;
private ItemRemoveReason _reason = ItemRemoveReason.values()[0];
private EventContext _eventContext;
private EventDataFilter _dataFilter;
private String _cacheContext;
private String _clientID;

    public ItemRemoveCallback(short id, Object key, Object value, ItemRemoveReason reason, String clientId, BitSet Flag, EventContext eventContext, EventDataFilter dataFilter, String cacheContext) 
    {
        _id = id;
        _key = key;
        _value = (UserBinaryObject)((value instanceof UserBinaryObject) ? value : null);
        _flag = Flag;
        _reason = reason;
        _clientID = clientId;
        _eventContext = eventContext;
        _dataFilter=dataFilter;
        _cacheContext = cacheContext;
    }

    public void Process() 
    {
        ClientManager clientManager = null;
        
        synchronized (ConnectionManager.ConnectionTable) 
        {
            clientManager = (ClientManager)ConnectionManager.ConnectionTable.get(_clientID);
        }
        
        if (clientManager != null) 
        {
            ResponseProtocol.Response.Builder response = ResponseProtocol.Response.newBuilder();
            
            BulkEventItemResponse.Builder eventItems=com.alachisoft.tayzgrid.common.protobuf.BulkEventItemResponseProtocol.BulkEventItemResponse.newBuilder();
            eventItems.setEventType(BulkEventItemResponse.EventType.ITEM_REMOVED_CALLBACK);
            try {
                eventItems.setItemRemoveCallback(EventHelper.GetItemRemovedCallbackResponse(_eventContext, _id, _key, _value, _flag, _reason, _dataFilter, _cacheContext));
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
