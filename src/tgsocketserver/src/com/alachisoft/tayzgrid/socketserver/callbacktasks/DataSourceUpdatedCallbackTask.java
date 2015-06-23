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

import com.alachisoft.tayzgrid.socketserver.util.EventHelper;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.ConnectionManager;
import com.alachisoft.tayzgrid.common.protobuf.BulkEventItemResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.BulkEventResponseProtocol;
import com.alachisoft.tayzgrid.caching.OpCode;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;

import java.util.ArrayList;

import java.io.IOException;

public class DataSourceUpdatedCallbackTask implements ICallbackTask {
	private short _id;
	private Object _result;
	private OpCode _opCode = OpCode.values()[0];
	private String _clientId;
        private String _cacheContext;

	public DataSourceUpdatedCallbackTask(short id, Object result, OpCode opCode, String clientId, String cacheContext) {
		_id = id;
		_result = result;
		_opCode = opCode;
		_clientId = clientId;
                _cacheContext = cacheContext;
	}

	public final void Process() {
		StringBuilder keyPackage = new StringBuilder();
		keyPackage.append(String.format("DSUPDATECALLBACK \"%1$s\"%2$s\"%3$s\"", _id, _opCode.getValue(), ((java.util.HashMap)_result).size()));


		ClientManager clientManager = null;

		synchronized (ConnectionManager.ConnectionTable) {
			clientManager = (ClientManager)ConnectionManager.ConnectionTable.get(_clientId);
		}

		if (clientManager != null) {
			ResponseProtocol.Response.Builder response = ResponseProtocol.Response.newBuilder();

                        BulkEventItemResponseProtocol.BulkEventItemResponse.Builder eventItems =com.alachisoft.tayzgrid.common.protobuf.BulkEventItemResponseProtocol.BulkEventItemResponse.newBuilder();
                        eventItems.setEventType(BulkEventItemResponseProtocol.BulkEventItemResponse.EventType.DS_UPDATED_CALLBACK);
                    try {
                        eventItems.setDSUpdatedCallback(EventHelper.GetDSUPdateCallbackResponse(_id, _opCode, _result, _cacheContext));
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
