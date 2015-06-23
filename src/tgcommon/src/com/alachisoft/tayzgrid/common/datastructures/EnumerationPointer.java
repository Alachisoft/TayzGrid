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

package com.alachisoft.tayzgrid.common.datastructures;

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.util.UUID;

public class EnumerationPointer implements ICompactSerializable
{
    private String _id = UUID.randomUUID().toString();
    private int _chunkId = -1;
    private boolean _isDisposable = false;
    private Address _nodeIpAddress;// = "";
    private boolean _isSocketServerDispose = false;

    public EnumerationPointer()
    {
    }

    public EnumerationPointer(String id, int chunkId)
    {
        _id = id;
        _chunkId = chunkId;
    }

    public boolean isGroupPointer()
    {
        return false;
    }

    public final boolean getHasFinished()
    {
        return _chunkId == -1;
    }

    public final Address getNodeIpAddress()
    {
        return _nodeIpAddress;
    }

    public final void setNodeIpAddress(Address value)
    {
        _nodeIpAddress = value;
    }

    public final String getId()
    {
        return _id;
    }

    public final int getChunkId()
    {
        return _chunkId;
    }

    public final void setChunkId(int value)
    {
        _chunkId = value;
    }

    public final boolean isDisposable()
    {
        return _isDisposable;
    }

    public final void setDisposable(boolean value)
    {
        _isDisposable = value;
    }

    public final boolean isSocketServerDispose()
    {
        return _isSocketServerDispose;
    }

    public final void setSocketServerDispose(boolean value)
    {
        _isSocketServerDispose = value;
    }

    public final void Reset()
    {
        _chunkId = -1;
    }

    @Override
    public boolean equals(Object obj)
    {
        boolean equals = false;

        if (obj instanceof EnumerationPointer)
        {
            EnumerationPointer other = (EnumerationPointer) ((obj instanceof EnumerationPointer) ? obj : null);
            equals = _id.equals(other._id);
        }

        return equals;
    }

    @Override
    public int hashCode()
    {
        if (_id != null)
        {
            return _id.hashCode();
        }
        else
        {
            return super.hashCode();
        }
    }

    @Override
    public void deserialize(CacheObjectInput reader) throws IOException, ClassNotFoundException
    {
        _id = reader.readUTF();
        _chunkId = reader.readInt();
        _isDisposable = reader.readBoolean();
        _nodeIpAddress = (Address) reader.readObject();
        _isSocketServerDispose = reader.readBoolean();
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeUTF(_id);
        writer.writeInt(_chunkId);
        writer.writeBoolean(_isDisposable);
        writer.writeObject(_nodeIpAddress);
        writer.writeBoolean(_isSocketServerDispose);
    }
}
