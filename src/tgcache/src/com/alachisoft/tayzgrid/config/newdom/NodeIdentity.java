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

package com.alachisoft.tayzgrid.config.newdom;

import com.alachisoft.tayzgrid.common.net.DnsCache;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.net.UnknownHostException;

public class NodeIdentity implements InternalCompactSerializable
{

    private String _nodeName;
    private String _partitionId;
    private int _nodePriority = 1;

    public NodeIdentity(){}
    
    public NodeIdentity(String nodeName, String partitionId)
    {
        _nodeName = nodeName;
        _partitionId = partitionId;
    }

    public NodeIdentity(String nodeName, String partitionId, int nodePriority)
    {
        this(nodeName, partitionId);
        _nodePriority = nodePriority;
    }

    public final String getNodeName()
    {
        return _nodeName;
    }

    public final void setNodeName(String value)
    {
        _nodeName = value;
    }

    public final String getPartitionId()
    {
        return _partitionId;
    }

    public final void setPartitionId(String value)
    {
        _partitionId = value;
    }

    public final int getNodePriority()
    {
        return _nodePriority;
    }

    public final void setNodePriority(int value)
    {
        _nodePriority = value;
    }

    /**
     * if the nodeIdentity contains an IpAddress instead of the node name it returns an equalent node indentity with the node name.
     *
     * @param ?
     * @return
     */
    public final NodeIdentity Resolve() throws UnknownHostException
    {
        if (DnsCache.ResolveAddress(this.getNodeName()) != null)
        {
            String nodeName = DnsCache.ResolveAddress(this.getNodeName());
            return new NodeIdentity(nodeName, this.getPartitionId());
        }
        return this;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof NodeIdentity)
        {
            NodeIdentity other = (NodeIdentity) ((obj instanceof NodeIdentity) ? obj : null);

            if (getPartitionId() == null)
            {
                setPartitionId("");
            }

            if (other.getPartitionId() == null)
            {
                other.setPartitionId("");
            }

            if (other.getNodeName().equals(getNodeName()))
            {
                if (other.getPartitionId() != null && getPartitionId() != null)
                {
                    return other.getPartitionId().equals(getPartitionId());
                }

                return false;
            }
        }
        else if (obj instanceof String)
        {
            return (getNodeName().equals((String) obj));
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return (_nodeName.hashCode());
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("Node Name : " + getNodeName());
        sb.append(", Partition Id : ");
        sb.append(getPartitionId() == null ? "null" : getPartitionId());
        return sb.toString();
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        _nodeName = (String) Common.readAs(reader.ReadObject(), String.class);
        _partitionId = (String) Common.readAs(reader.ReadObject(), String.class);
        _nodePriority = reader.ReadInt32();
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(_nodeName);
        writer.WriteObject(_partitionId);
        writer.Write(_nodePriority);
    }
}
