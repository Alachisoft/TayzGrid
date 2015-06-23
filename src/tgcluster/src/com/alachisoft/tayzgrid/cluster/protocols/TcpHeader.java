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

package com.alachisoft.tayzgrid.cluster.protocols;

import com.alachisoft.tayzgrid.cluster.Header;
import com.alachisoft.tayzgrid.common.net.IRentableObject;
import com.alachisoft.tayzgrid.common.net.ObjectProvider;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

 
public class TcpHeader extends Header implements ICompactSerializable, IRentableObject, Serializable
{

    public String group_addr = null;
    private int rentId;

    public TcpHeader()
    {
    } // used for externalization

    public TcpHeader(String n)
    {
        group_addr = n;
    }

    @Override
    public String toString()
    {
        return "[TCP:group_addr=" + group_addr + ']';
    }

 
    public final void deserialize(CacheObjectInput reader) throws IOException
    {
        group_addr = reader.readUTF();
    }

    public final void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeUTF(group_addr);
    }
 
    @Override
    public Object clone(ObjectProvider provider)
    {
        TcpHeader hdr = null;
        if (provider != null)
        {
            hdr = (TcpHeader) provider.RentAnObject();
        }
        else
        {
            hdr = new TcpHeader();
        }
        hdr.group_addr = group_addr;

        return hdr;
    }
 

    public final int getRentId()
    {
        return rentId;
    }

    public final void setRentId(int value)
    {
        rentId = value;
    }

 
    public static TcpHeader ReadTcpHeader(CacheObjectInput reader) throws IOException
    {
 
        byte isNull = reader.readByte();
        if (isNull == 1)
        {
            return null;
        }
        TcpHeader newHdr = new TcpHeader();
        newHdr.deserialize(reader);
        return newHdr;
    }

    public static void WriteTcpHeader(CacheObjectOutput writer, TcpHeader hdr) throws IOException
    {
 
        byte isNull = 1;
        if (hdr == null)
        {
            writer.writeByte(isNull);
        }
        else
        {
            isNull = 0;
            writer.writeByte(isNull);
            hdr.serialize(writer);
        }
        return;
    }

    public final void DeserializeLocal(ObjectInput reader) throws IOException
    {
        group_addr = reader.readUTF();
    }

    public final void SerializeLocal(ObjectOutput writer) throws IOException
    {
        writer.writeUTF(group_addr);
    }
 
}
