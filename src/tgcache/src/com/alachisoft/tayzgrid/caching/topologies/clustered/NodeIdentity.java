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

package com.alachisoft.tayzgrid.caching.topologies.clustered;

import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;

/**
 * An info object that is passed as identity of the members, i.e., additional data with the Address object. This will help the partition determine legitimate members as well as
 * gather useful information about member configuration. Load balancer might be a good consumer of this information.
 */
public class NodeIdentity implements ICompactSerializable, Serializable
{
    /**
     * @deprecated Only to be used for CompactSerialization
     */
    public NodeIdentity()
    {
    }
    /**
     * Up status of node.
     */
    private String groupName;
    /**
     * Up status of node.
     */
    private BitSet status = new BitSet();
    private int rendererPort = -1;
    private InetAddress rendererAddress;
    /**
     * True if this cache instance is started as mirror cache. otherwise false.
     */
    private boolean isStartedAsMirror = false;
    private String partitionId;

    /**
     * Constructor
     *
     * @param hasStorage
     * @param renderPort
     * @param renderAddress
     */
    public NodeIdentity(boolean hasStorage, int renderPort, InetAddress renderAddress)
    {
        setHasStorage(hasStorage);
        rendererPort = renderPort;
        rendererAddress = renderAddress;
    }

    /**
     * Constructor
     *
     * @param hasStorage
     * @param renderPort
     * @param renderAddress
     * @param isStartedAsMirror
     * @param partitionId
     */
    public NodeIdentity(boolean hasStorage, int renderPort, InetAddress renderAddress, boolean isStartedAsMirror, String partitionId)
    {
        setHasStorage(hasStorage);
        rendererPort = renderPort;
        rendererAddress = renderAddress;
        this.isStartedAsMirror = isStartedAsMirror;
        setPartitionId( partitionId);
    }

    public String getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(String partitionId) {
        this.partitionId = partitionId;
    }
    

    /**
     * The number of backup caches configured with this instance
     * @return .
     */
    public final boolean getHasStorage()
    {
        return status.IsBitSet((byte) (0x01));
    }

    public final void setHasStorage(boolean value)
    {
        if (value)
        {
            status.SetBit((byte) (0x01));
        }
        else
        {
            status.UnsetBit((byte) (0x01));
        }
    }

    /**
     * Gets or sets the cache renderer port.
     * @return 
     */
    public final int getRendererPort()
    {
        return rendererPort;
    }

    /**
     *
     * @return
     */
    public final InetAddress getRendererAddress()
    {
        return rendererAddress;
    }

    public final String getSubGroupName()
    {
        return groupName;
    }

    public final void setSubGroupName(String value)
    {
        groupName = value;
    }

    /**
     * Get or Sets the value indicating weather this instance started as Mirror or not. True if started as mirror otherwise false.
     * @return 
     */
    public final boolean getIsStartedAsMirror()
    {
        return isStartedAsMirror;
    }

    @Override
    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        Object tempVar = reader.readObject();
        groupName = (String) ((tempVar instanceof String) ? tempVar : null);
        status = new BitSet(reader.readByte());
        rendererPort = reader.readInt();
        Object tempVar2 = reader.readObject();
        rendererAddress = (InetAddress) ((tempVar2 instanceof InetAddress) ? tempVar2 : null);
        isStartedAsMirror = reader.readBoolean();
        partitionId = (String) Common.as(reader.readObject(), String.class);
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeObject(groupName);
        writer.writeByte(status.getData());
        writer.writeInt(rendererPort);
        writer.writeObject(rendererAddress);
        writer.writeBoolean(isStartedAsMirror);
        writer.writeObject(partitionId);
    }

    @Override
    public String toString()
    {
        return "NodeIdentity: (" + groupName + ", " + (getIsStartedAsMirror() ? "IsMirror" : "IsActive") + ", " + (getHasStorage() ? "HasStorage" : "NoStorage" +" partition-id :" + partitionId);
    }
}
