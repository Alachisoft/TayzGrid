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
import com.alachisoft.tayzgrid.socketserver.SocketServer;
import com.alachisoft.tayzgrid.common.protobuf.HashmapChangedEventResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.datastructures.NewHashmap;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.google.protobuf.ByteString;

public final class HashmapChangedEvent implements IEventTask
{

    private String _cacheId;
    private String _clientId;
    private NewHashmap _newmap;
    private boolean _isDotNetClient;

    public HashmapChangedEvent(String cacheId, String clientId, NewHashmap newHashmap, boolean isDotNetClient)
    {
        this._cacheId = cacheId;
        this._clientId = clientId;
        this._newmap = newHashmap;
        this._isDotNetClient = isDotNetClient;
    }

    public void Process()
    {
        try
        {
            ClientManager clientManager = null;
            synchronized (ConnectionManager.ConnectionTable)
            {
                clientManager = (ClientManager) ConnectionManager.ConnectionTable.get(this._clientId);
            }
            if (clientManager != null)
            {
                byte[] table = CompactBinaryFormatter.toByteBuffer(this._newmap,this._cacheId);

                ResponseProtocol.Response.Builder response = ResponseProtocol.Response.newBuilder();
                HashmapChangedEventResponseProtocol.HashmapChangedEventResponse.Builder hashmapChangedResponse = HashmapChangedEventResponseProtocol.HashmapChangedEventResponse.newBuilder();

                hashmapChangedResponse.setTable(ByteString.copyFrom(table));

                response.setHashmapChanged(hashmapChangedResponse);
                response.setResponseType(ResponseProtocol.Response.Type.HASHMAP_CHANGED_EVENT);

                byte[] serializedResponse = ResponseHelper.SerializeResponse(response.build());

                ConnectionManager.AssureSend(clientManager, serializedResponse);

            }
        }
        catch (RuntimeException exc)
        {
            if (SocketServer.getLogger().getIsErrorLogsEnabled())
            {
                SocketServer.getLogger().getCacheLog().Error("HashmapChangedEvent.Process", exc.toString());
            }
        }
        catch (Exception exc)
        {
            if (SocketServer.getLogger().getIsErrorLogsEnabled())
            {
                SocketServer.getLogger().getCacheLog().Error("HashmapChangedEvent.Process , Configuration Error: ", exc.toString());
            }
        }
    }
}
