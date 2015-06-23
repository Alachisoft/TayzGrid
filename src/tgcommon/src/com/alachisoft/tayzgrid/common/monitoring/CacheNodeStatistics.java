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
import java.io.Serializable;

public class CacheNodeStatistics implements InternalCompactSerializable
{

    private ServerNode _node;
    private long _itemCount;
    private long _dataSize;
    private short _clientsConnected;
    private long _totalCacheSize;
    private CacheNodeStatus _nodeStatus = CacheNodeStatus.Stopped;

    public CacheNodeStatistics()
    {
    }

    

    public CacheNodeStatistics(ServerNode node)
    {
        _node = node;
    }

    /**
     * Gets/Sets the status of the cache node.
     */
    public final CacheNodeStatus getStatus()
    {
        return _nodeStatus;
    }

    public final void setStatus(CacheNodeStatus value)
    {
        _nodeStatus = value;
    }

    /**
     * Gets/Sets the item count.
     */
    public final long getItemCount()
    {
        return _itemCount;
    }

    public final void setItemCount(long value)
    {
        _itemCount = value;
    }

    /**
     * Gets/Sets the data size on the cache server node.
     */
    public final long getDataSize()
    {
        return _dataSize;
    }

    public final void setDataSize(long value)
    {
        _dataSize = value;
    }

    /**
     * Gets/Sets the total size on the cache server node.
     */
    public final long getTotalCacheSize()
    {
        return _totalCacheSize;
    }

    public final void setTotalCacheSize(long value)
    {
        _totalCacheSize = value;
    }

    /**
     * Gets/Sets the no of clients connected to a serve node.
     */
    public final short getClientCount()
    {
        return _clientsConnected;
    }

    public final void setClientCount(short value)
    {
        _clientsConnected = value;
    }

    public final ServerNode getNode()
    {
        return _node;
    }

    public final void setNode(ServerNode value)
    {
        _node = value;
    }

    //<editor-fold defaultstate="collapsed" desc="CompactS">
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        _node = (ServerNode) Common.readAs(reader.ReadObject(), ServerNode.class);
        _itemCount = reader.ReadInt64();
        _dataSize = reader.ReadInt64();
        _clientsConnected = ((Integer)reader.ReadUInt16()).shortValue();
        _totalCacheSize = reader.ReadInt64();
        _nodeStatus = CacheNodeStatus.forValue(reader.ReadInt32());
    }

    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(_node);
        writer.Write(_itemCount);
        writer.Write(_dataSize);
        writer.Write(_clientsConnected);
        writer.Write(_totalCacheSize);
        writer.Write(_nodeStatus.getValue());
    }
    //</editor-fold>
}
