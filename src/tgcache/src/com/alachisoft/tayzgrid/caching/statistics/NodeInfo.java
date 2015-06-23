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

package com.alachisoft.tayzgrid.caching.statistics;

import com.alachisoft.tayzgrid.caching.datagrouping.DataAffinity;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The class that contains information specific to a single node in the cluster. Contains the address as well as statistics of the local cache.
 */
public class NodeInfo implements Cloneable, java.lang.Comparable, ICompactSerializable,Serializable
{
    /**
     * The IP address, port tuple; uniquely identifying the node.
     */
    private Address _address;
    /**
     * The name of the sub-cluster this node belongs to.
     */
    private String _subgroupName;
    /**
     * The statistics of the node.
     */
    private CacheStatistics _stats;
    /**
     * Up status of node.
     */
    private BitSet _status = new BitSet();
    /**
     * Data groups associated with this node
     */
    private DataAffinity _dataAffinity;
    private java.util.List _connectedClients = Collections.synchronizedList(new java.util.ArrayList());
    /**
     * Client/Server address of the node.
     */
    private Address _rendererAddress;
    private boolean _isInproc;
    private boolean _isStartedAsMirror;

    /**
     * Constructor.
     */
    private NodeInfo()
    {
    }

    /**
     * Overloaded Constructor
     *
     * @param address
     */
    public NodeInfo(Address address)
    {
        _address = address;
    }

    public NodeInfo(Address address, boolean isStartedAsMirror)
    {
        _address = address;
        _isStartedAsMirror = isStartedAsMirror;
    }

    /**
     * Copy constructor
     *
     * @param info
     */
    protected NodeInfo(NodeInfo info)
    {
        this._address = info._address == null ? null : (Address) ((info._address.clone() instanceof Address) ? info._address.clone() : null);
        if (_rendererAddress != null)
        {
            Object tempVar2 = info._rendererAddress.clone();
            this._rendererAddress = info._rendererAddress != null ? (Address) ((tempVar2 instanceof Address) ? tempVar2 : null) : null;
        }
        this._stats = info._stats == null ? null : (CacheStatistics) ((info._stats.clone() instanceof CacheStatistics) ? info._stats.clone() : null);
        this._status = info._status;
        this._subgroupName = info._subgroupName;
        this._isInproc = info._isInproc;

        this._dataAffinity = info._dataAffinity == null ? null : (DataAffinity) ((info._dataAffinity instanceof DataAffinity) ? info._dataAffinity : null);
        _isStartedAsMirror = info.getIsStartedAsMirror();
        if (info._connectedClients != null)
        {
            synchronized (info._connectedClients)
            {
                ArrayList list = new ArrayList();
                list.addAll(info._connectedClients);
                this._connectedClients = list;
            }
        }
    }

    /**
     * The IP address, port tuple; uniquely identifying the node.
     */
    public final Address getAddress()
    {
        return _address;
    }

    public final void setAddress(Address value)
    {
        _address = value;
    }

    public final Address getRendererAddress()
    {
        return _rendererAddress;
    }

    public final void setRendererAddress(Address value)
    {
        _rendererAddress = value;
    }

    public final boolean getIsStartedAsMirror()
    {
        return _isStartedAsMirror;
    }

    public final void setIsStartedAsMirror(boolean value)
    {
        _isStartedAsMirror = value;
    }

    /**
     * Gets/sets the status of the node whether a node is InProc or OutProc.
     */
    public final boolean getIsInproc()
    {
        return _isInproc;
    }

    public final void setIsInproc(boolean value)
    {
        _isInproc = value;
    }

    /**
     * The name of the sub-cluster this node belongs to.
     */
    public final String getSubgroupName()
    {
        return _subgroupName;
    }

    public final void setSubgroupName(String value)
    {
        _subgroupName = value;
    }

    /**
     * The data groups settings for the node.
     */
    public final DataAffinity getDataAffinity()
    {
        return _dataAffinity;
    }

    public final void setDataAffinity(DataAffinity value)
    {
        _dataAffinity = value;
    }

    /**
     * The number of nodes in the cluster that are providers or consumers, i.e., group members.
     */
    public final CacheStatistics getStatistics()
    {
        return _stats;
    }

    public final void setStatistics(CacheStatistics value)
    {
        _stats = value;
    }

    /**
     * The runtime status of node.
     */
    public final BitSet getStatus()
    {
        return _status;
    }

    public final void setStatus(BitSet value)
    {
        _status = value;
    }

    public final java.util.List getConnectedClients()
    {
        return _connectedClients;
    }

    public final void setConnectedClients(java.util.List value)
    {
        _connectedClients = value;
    }

    /**
     * Compares the current instance with another object of the same type.
     *
     * @param obj An object to compare with this instance.
     * @return A 32-bit signed integer that indicates the relative order of the comparands.
     */
    public final int compareTo(Object obj)
    {
        return _address.compareTo(((NodeInfo) obj).getAddress());
    }

    @Override
    public boolean equals(Object obj)
    {
        return compareTo(obj) == 0;
    }

    /**
     * Creates a new object that is a copy of the current instance.
     *
     * @return A new object that is a copy of this instance.
     */
    public final Object clone()
    {
        return new NodeInfo(this);
    }

    /**
     * returns the string representation of the statistics.
     *
     * @return
     */
    @Override
    public String toString()
    {
        StringBuilder ret = new StringBuilder();
        try
        {
            ret.append("Node[Adr:" + _address);
            if (_stats != null)
            {
                ret.append(", " + _stats);
            }
            ret.append("]");
        }
        catch (Exception e)
        {
        }
        return ret.toString();

    }

    public final void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        if (!reader.readBoolean())
        {
            _address = new Address();
            _address.deserialize(reader);
        }
        else
            _address = null;
        _subgroupName = (String) reader.readObject();
        _stats = CacheStatistics.ReadCacheStatistics(reader);
        _status = new BitSet(reader.readByte());
        if(!reader.readBoolean())
            _dataAffinity = (DataAffinity) reader.readObject();
        else
            _dataAffinity = null;

        ArrayList list = (ArrayList) reader.readObject();
        if (list != null)
            _connectedClients = Collections.synchronizedList(list);

        _isInproc = reader.readBoolean();

        if(!reader.readBoolean())
        {
            _rendererAddress = (Address) reader.readObject();
        }
        else
            _rendererAddress = null;
        _isStartedAsMirror = reader.readBoolean();
    }

    public final void serialize(CacheObjectOutput writer) throws IOException
    {
        if (getAddress() != null)
        {
            writer.writeBoolean(false);
           getAddress().serialize(writer);
        }
        else
            writer.writeBoolean(true);
        writer.writeObject(_subgroupName);
        CacheStatistics.WriteCacheStatistics(writer, _stats);
        writer.writeByte(_status.getData());
        if(getDataAffinity() == null)
        {
            writer.writeBoolean(true);
        }
        else
        {
            writer.writeBoolean(false);
            writer.writeObject(getDataAffinity());
        }

        ArrayList arr = null;
        if(_connectedClients != null)
        {
            arr = new ArrayList(_connectedClients);
        }
        writer.writeObject(arr);
        writer.writeBoolean(_isInproc);

        if (_rendererAddress == null)
        {
            writer.writeBoolean(true);
        }
        else
        {
            writer.writeBoolean(false);
            writer.writeObject(_rendererAddress);
        }
        writer.writeBoolean(_isStartedAsMirror);
    }

    public static NodeInfo ReadNodeInfo(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        byte isNull = reader.readByte();
        if (isNull == 1)
        {
            return null;
        }
        NodeInfo newInfo = new NodeInfo();
        newInfo.deserialize(reader);
        return newInfo;
    }
}
