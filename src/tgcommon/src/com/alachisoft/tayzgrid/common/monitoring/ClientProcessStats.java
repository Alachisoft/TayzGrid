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

package com.alachisoft.tayzgrid.common.monitoring;

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class ClientProcessStats extends ClientNode implements java.lang.Comparable, InternalCompactSerializable
{

    private String _processID;
    private float _bytesSent;
    private float _bytesReceived;
    private String _serverIPAddress;

    public final String getServerIPAddress()
    {
        return _serverIPAddress;
    }

    public final void setServerIPAddress(String value)
    {
        _serverIPAddress = value;
    }

    public final String getProcessID()
    {
        return _processID;
    }

    public final float getBytesSent()
    {
        return _bytesSent;
    }

    public final void setBytesSent(float value)
    {
        _bytesSent = value;
    }

    public final float getBytesReceived()
    {
        return _bytesReceived;
    }

    public final void setBytesReceived(float value)
    {
        _bytesReceived = value;
    }

    public ClientProcessStats(String clientID, Address address, float byteSent, float byteReceived, String serverIPAddress)
    {
        setAddress(address);
        setClientID(clientID);
        
         String[] parsedClientId = clientID.split(":", 4);
         if(parsedClientId != null && parsedClientId.length >=3){
            _processID = parsedClientId[2];
         }
        
        _bytesReceived = byteReceived;
        _bytesSent = byteSent;
        _serverIPAddress = serverIPAddress;
    }

    public final int compareTo(Object obj)
    {
        ClientProcessStats clientProcessStats = (ClientProcessStats) ((obj instanceof ClientProcessStats) ? obj : null);
        if (clientProcessStats == null)
        {
            return -1;
        }

        int result = 0;

        result = _processID.compareTo(clientProcessStats._processID);

        if (result != 0)
        {
            result = getAddress().getIpAddress().toString().compareTo(clientProcessStats.getAddress().getIpAddress().toString());
        }

        if (result != 0)
        {
            result = (new Integer(getAddress().getPort())).toString().compareTo((new Integer(getAddress().getPort())).toString());
        }

        if (result != 0)
        {
            result = _serverIPAddress.compareTo(clientProcessStats.getServerIPAddress());
        }

        if (result != 0)
        {
            result = (new Float(_bytesSent)).compareTo(clientProcessStats.getBytesSent());
        }

        if (result != 0)
        {
            result = (new Float(_bytesReceived)).compareTo(clientProcessStats.getBytesReceived());
        }

        return result;
    }

    //<editor-fold defaultstate="collapsed" desc="CompactS">
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        _processID = (String) Common.readAs(reader.ReadObject(),String.class);
        _bytesSent = reader.ReadSingle();
        _bytesReceived = reader.ReadSingle();
        _serverIPAddress = (String) Common.readAs(reader.ReadObject(), String.class);
        super.Deserialize(reader);
    }

    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(_processID);
        writer.Write(_bytesSent);
        writer.Write(_bytesReceived);
        writer.WriteObject(_serverIPAddress);
        super.Serialize(writer);
    }
    //</editor-fold>
}
