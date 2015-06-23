/*
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

package com.alachisoft.tayzgrid.cluster.blocks;

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

public class ConnectInfo implements ICompactSerializable
{


    public static final byte CONNECT_FIRST_TIME = 1;

    public static final byte RECONNECTING = 2;

    private byte _connectStatus = CONNECT_FIRST_TIME;
    private int _id;

    public ConnectInfo()
    {
        _connectStatus = CONNECT_FIRST_TIME;
    }


    public ConnectInfo(byte connectStatus, int id)
    {
        _connectStatus = connectStatus;
        _id = id;
    }


    public final byte getConnectStatus()
    {
        return _connectStatus;
    }


    public final void setConnectStatus(byte value)
    {
        _connectStatus = value;
    }

    public final int getId()
    {
        return _id;
    }

    public final void setId(int value)
    {
        _id = value;
    }

    @Override
    public String toString()
    {
        String str = "[";

        str += _id;
        str += (_connectStatus == CONNECT_FIRST_TIME ? "CONNECT_FIRST_TIME" : "RECONNECTING");
        return str;
    }

    //<editor-fold defaultstate="collapsed" desc="ICompactSerializable Members">
    public final void deserialize(CacheObjectInput reader) throws IOException
    {
        _connectStatus = reader.readByte();
        _id = reader.readInt();
    }

    public final void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeByte(_connectStatus);
        writer.writeInt(_id);
    }
    //</editor-fold>
}
