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

import com.alachisoft.tayzgrid.common.protobuf.BulkEventItemResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.BulkEventResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.mapreduce.notification.TaskStatus;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.ConnectionManager;
import com.alachisoft.tayzgrid.socketserver.util.EventHelper;

public class TaskCallback implements ICallbackTask {
    
    String _clientId;
    String _taskId;
    int _taskStatus;
    short _callbackId;
    
    /**
     *
     * @param cacheId
     * @param clientId
     * @param taskId
     * @param taskStatus
     * @param callbackId
     */
    public TaskCallback(String clientId, String taskId, int taskStatus, short callbackId) {
        this._clientId = clientId;
        this._taskId = taskId;
        this._taskStatus = taskStatus;
        this._callbackId = callbackId;
    }
    @Override
    public void Process() {
        ClientManager clientManager = null;

        synchronized (ConnectionManager.ConnectionTable) {
            clientManager = (ClientManager) ConnectionManager.ConnectionTable.get(_clientId);
        }

        ResponseProtocol.Response.Builder response = ResponseProtocol.Response.newBuilder();

        BulkEventItemResponseProtocol.BulkEventItemResponse.Builder eventItems = com.alachisoft.tayzgrid.common.protobuf.BulkEventItemResponseProtocol.BulkEventItemResponse.newBuilder();
        
        eventItems.setEventType(BulkEventItemResponseProtocol.BulkEventItemResponse.EventType.MAP_REDUCE_TASK_CALLBACK);
        eventItems.setTaskCallback(EventHelper.GetMapReduceTaskCallbackResponse(null, _taskId, _taskStatus, _callbackId));
        
        BulkEventResponseProtocol.BulkEventResponse.Builder eventResponse = BulkEventResponseProtocol.BulkEventResponse.newBuilder();
        eventResponse.addEventList(eventItems);
        
        response.setBulkEventResponse(eventResponse);
        response.setResponseType(ResponseProtocol.Response.Type.BULK_EVENT);

        byte[] serializedResponse = ResponseHelper.SerializeResponse(response.build());

        ConnectionManager.AssureSend(clientManager, serializedResponse);
    }

}
