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
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.io.Serializable;

public class ServerNode extends Node implements Cloneable, InternalCompactSerializable
{

    private boolean _isRepica;
    private boolean _inProcInstance;
    private int _clientPort;
    private String _nodeAt;

    public ServerNode()
    {
    }

    public ServerNode(String name, Address address)
    {
        super(name, address);
    }

    public final void setIsReplica(boolean value)
    {
        _isRepica = value;
    }

    /**
     * Gets/Sets the status of the node whether it is running as inproc or outproc.
     * @return
     */
    public final boolean getInProcInstance()
    {
        return _inProcInstance;
    }

    public final void setInProcInstance(boolean value)
    {
        _inProcInstance = value;
    }

    /**
     * Gets/Sets the socket server port of this node.
     * @return
     */
    public final int getClientPort()
    {
        return _clientPort;
    }

    public final void setClientPort(int value)
    {
        _clientPort = value;
    }

    /**
     * Gets/Sets node ip address where replica residing.
     * @return
     */
    public final String getNodeAt()
    {
        return _nodeAt;
    }

    public final void setNodeAt(String value)
    {
        _nodeAt = value;
    }

    @Override
    public final Object clone()
    {
        ServerNode node = new ServerNode();
        node.setInProcInstance(this.getInProcInstance());
        node.setNodeAt(this.getNodeAt());
        node.setName(this.getName());
        node.setAddress(this.getAddress() != null ? (Address) this.getAddress() : null);
        return node;
    }

    //<editor-fold defaultstate="collapsed" desc="CompactS">
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        _isRepica = reader.ReadBoolean();
        _inProcInstance = reader.ReadBoolean();
        _clientPort = reader.ReadInt32();
        _nodeAt = (String) Common.readAs(reader.ReadObject(),String.class);
        super.Deserialize(reader);
    }

    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.Write(_isRepica);
        writer.Write(_inProcInstance);
        writer.Write(_clientPort);
        writer.WriteObject(_nodeAt);
        super.Serialize(writer);
    }
    //</editor-fold>
}
