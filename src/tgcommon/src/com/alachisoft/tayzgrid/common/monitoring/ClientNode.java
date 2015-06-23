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

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import com.alachisoft.tayzgrid.common.enums.RtContextValue;
public class ClientNode extends Node implements InternalCompactSerializable
{

    private String _clientId;
    private com.alachisoft.tayzgrid.common.enums.RtContextValue _clientContext;
    /**
     * Gets/Sets the client-id. Each client connecting to a cache server has unique id. If client opens multiple connections with the server, this id remains some.
     */
    public final String getClientID()
    {
        return _clientId;
    }

    public final void setClientID(String value)
    {
        _clientId = value;
    }
    public final RtContextValue getClientContext()
    {
        return _clientContext;
    }
    public final void setClientContext(RtContextValue clientContext)
    {
        _clientContext=clientContext;
    }
    @Override
    public boolean equals(Object obj)
    {
        ClientNode other = (ClientNode) ((obj instanceof ClientNode) ? obj : null);
        if (other != null)
        {
            if (other.getClientID().equals(getClientID()))
            {
                return true;
            }
        }
        return false;
    }

    //<editor-fold defaultstate="collapsed" desc="CompactS">
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        _clientId = (String) Common.readAs(reader.ReadObject(), String.class);
        _clientContext =(String) Common.readAs(reader.ReadObject(),String.class)== "1" ? RtContextValue.JVCACHE : RtContextValue.NCACHE;
        super.Deserialize(reader);
    }

    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(_clientId);
        writer.WriteObject(_clientContext== RtContextValue.JVCACHE ? "1" : "0");
        super.Serialize(writer);
    }
    //</editor-fold>
}
