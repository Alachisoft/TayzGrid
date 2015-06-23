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
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.common.protobuf.BlockActivityEventResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;

 public final class BlockActivityEvent implements IEventTask
 {
    private String _uniquekey;
    private String _cacheId;
    private String _clientid;
    private String _serverIP;
    private long _timeoutInterval;
    private int _port;


    public BlockActivityEvent(String key, String cacheId, String clientid, String serverIP, long timeoutInterval, int port)
    {
            _uniquekey = key;
            _cacheId = cacheId;
            _clientid = clientid;
            _serverIP = serverIP;
            _timeoutInterval = timeoutInterval;
            _port = port;

    }

    public void Process()
    {
            ClientManager clientManager = null;

            synchronized (ConnectionManager.ConnectionTable)
            {
                    clientManager = (ClientManager)ConnectionManager.ConnectionTable.get(_clientid);
            }
            if (clientManager != null)
            {
               ResponseProtocol.Response.Builder response = ResponseProtocol.Response.newBuilder();
               BlockActivityEventResponseProtocol.BlockActivityEventResponse.Builder blockActivityResponse = BlockActivityEventResponseProtocol.BlockActivityEventResponse.newBuilder();

               
                blockActivityResponse.setUniqueKey(_uniquekey);
                blockActivityResponse.setServerIP(_serverIP);
                blockActivityResponse.setPort(_port);
                blockActivityResponse.setTimeoutInterval(_timeoutInterval);

                response.setBlockActivityEvent(blockActivityResponse);
                response.setResponseType(ResponseProtocol.Response.Type.BLOCK_ACTIVITY);

                byte[] serializedResponse = ResponseHelper.SerializeResponse(response.build());
                ConnectionManager.AssureSend(clientManager, serializedResponse);
            
            }
    }
 }
