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
import com.alachisoft.tayzgrid.common.protobuf.BulkEventItemResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.BulkEventResponseProtocol;
import com.alachisoft.tayzgrid.caching.AsyncCallbackInfo;
import com.alachisoft.tayzgrid.socketserver.util.EventHelper;
import com.alachisoft.tayzgrid.serialization.util.SerializationUtil;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import java.io.IOException;



public final class AsyncOpCompletedCallback implements ICallbackTask
{

    private Object _opCode;
    private Object _result;
    private String _cacheContext;

    public AsyncOpCompletedCallback(Object opCode, Object result, String cacheContext)
    {
        _opCode = opCode;
        _result = result;
        _cacheContext = cacheContext;
    }

    public void Process()
    {

        Object[] package_Renamed = null;
        try
        {
            package_Renamed = (Object[]) SerializationUtil.safeDeserialize(_result, _cacheContext,null);
        }
        catch (Exception ex)
        {
            //Suppressing safeDeserialize IOException
        }
        Object key = package_Renamed[0];
        AsyncCallbackInfo cbInfo = (AsyncCallbackInfo) package_Renamed[1];
        Object opResult = package_Renamed[2];

        ClientManager clientManager = null;

        synchronized (ConnectionManager.ConnectionTable)
        {
            clientManager = (ClientManager) ConnectionManager.ConnectionTable.get(cbInfo.getClient());
        }

        if (clientManager != null)
        {
            ResponseProtocol.Response.Builder response = ResponseProtocol.Response.newBuilder();
            response.setRequestId(cbInfo.getRequestID());
            
            BulkEventItemResponseProtocol.BulkEventItemResponse.Builder eventItems =com.alachisoft.tayzgrid.common.protobuf.BulkEventItemResponseProtocol.BulkEventItemResponse.newBuilder();
            eventItems.setEventType(BulkEventItemResponseProtocol.BulkEventItemResponse.EventType.ASYNC_OP_COMPLETED_EVENT);
            try {
                eventItems.setAsyncOperationCompletedCallback(EventHelper.GetAsyncOpCompletedResponse(clientManager, cbInfo, opResult, _opCode, key, _cacheContext));
            } catch (IOException ex) {
                //suppressing I/O exception,(caused by key serialization), not expected to occur
            }
            
            BulkEventResponseProtocol.BulkEventResponse.Builder eventResponse=com.alachisoft.tayzgrid.common.protobuf.BulkEventResponseProtocol.BulkEventResponse.newBuilder();            
            eventResponse.addEventList(eventItems);
            
            response.setBulkEventResponse(eventResponse);
            response.setResponseType(ResponseProtocol.Response.Type.BULK_EVENT);
            
            byte[] serializedResponse = ResponseHelper.SerializeResponse(response.build());

            ConnectionManager.AssureSend(clientManager, serializedResponse);

        }
    }

}
