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
import com.alachisoft.tayzgrid.common.enums.CacheTopology;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.io.Serializable;

public class ConfiguredCacheInfo implements InternalCompactSerializable
{

    private String _cacheId;
    private CacheTopology _topology = CacheTopology.values()[0];
    private boolean _running;
    private long _dataCapacity;
    private String _cachePropStirng;
    private String _partId;
    private String _pid = "";
    
    public final String getPID()
    {
        return _pid;
    }
    public final void setPID(String value)
    {
        _pid = value;
    }
    
    /**
     * Gets the cache id
     */
    public final String getCacheId()
    {
        return _cacheId;
    }

    public final void setCacheId(String _cacheId)
    {
        this._cacheId = _cacheId;
    }

    /**
     * Gets/Sets the topology of the cache.
     */
    public final CacheTopology getTopology()
    {
        return _topology;
    }

    public final void setTopology(CacheTopology _topology)
    {
        this._topology = _topology;
    }

    /**
     * Gets/Sets the running status of cache.
     */
    public final boolean getIsRunning()
    {
        return _running;
    }

    public final void setIsRunning(boolean _running)
    {
        this._running = _running;
    }

    /**
     * Gets/Sets the data capacity of the cache in MB.
     */
    public final long getDataCapacity()
    {
        return _dataCapacity;
    }

    public final void setDataCapacity(long _dataCapacity)
    {
        this._dataCapacity = _dataCapacity;
    }

    public final String getCachePropString()
    {
        return _cachePropStirng;
    }

    public final void setCachePropString(String _cachePropStirng)
    {
        this._cachePropStirng = _cachePropStirng;
    }

    public final String getPartId()
    {
        return this._partId;
    }

    public final void setPartId(String _partId)
    {
        this._partId = _partId;
    }

    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        _cacheId = (String) Common.readAs(reader.ReadObject(), String.class);
        _topology = CacheTopology.forValue(reader.ReadInt32());
        _running = reader.ReadBoolean();
        _dataCapacity = reader.ReadInt64();
        this._cachePropStirng = (String) Common.readAs(reader.ReadObject(), String.class);
        _partId = (String) Common.readAs(reader.ReadObject(), String.class);
        _pid = (String) Common.readAs(reader.ReadObject(), String.class);
    }

    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(_cacheId);
        writer.Write(_topology.getValue());
        writer.Write(_running);
        writer.Write(_dataCapacity);
        writer.WriteObject(this._cachePropStirng);
        writer.WriteObject(_partId);
        writer.WriteObject(_pid);
    }
}
