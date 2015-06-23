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
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.io.Serializable;

 
public class PingHeader extends Header implements ICompactSerializable, Serializable
{
 

    public static final byte GET_MBRS_REQ = 1; // arg = null
 
    public static final byte GET_MBRS_RSP = 2; // arg = PingRsp(local_addr, coord_addr)
    public String group_addr = null;
 
    public byte[] userId = null;
 
    public byte[] password = null;
 
    public byte type = 0;
    public Object arg = null;

    public PingHeader()
    {
    } // for externalization

 
    public PingHeader(byte type, Object arg)
    {
        this.type = type;
        this.arg = arg;
    }
 

    public PingHeader(byte type, Object arg, String group_addr)
    {
        this.type = type;
        this.arg = arg;
        this.group_addr = group_addr;
    }

 
    public PingHeader(byte type, Object arg, String group_addr, byte[] userId, byte[] password)
    {
        this.type = type;
        this.arg = arg;
        this.group_addr = group_addr;
        this.userId = userId;
        this.password = password;
    }

    @Override
    public long size()
    {
        return 100;
    }

    @Override
    public String toString()
    {
        return "[PING: type=" + type2Str(type) + ", arg=" + arg + ']';
    }

    public String type2Str(int t)
    {
        switch (t)
        {

            case GET_MBRS_REQ:
                return "GET_MBRS_REQ";

            case GET_MBRS_RSP:
                return "GET_MBRS_RSP";

            default:
                return "<unkown type (" + t + ")>";

        }
    }

 
    @Override
    public final void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        type = reader.readByte();
        group_addr = (String) reader.readObject();
        arg = reader.readObject();
        userId = (byte[]) reader.readObject();
        password = (byte[]) reader.readObject();
    }

    @Override
    public final void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeByte(type);
        writer.writeObject((Object) group_addr);
        writer.writeObject(arg);
        writer.writeObject(userId);
        writer.writeObject(password);
    }
 
    public static PingHeader ReadPingHeader(CacheObjectInput reader) throws IOException, ClassNotFoundException
    {
 
        byte isNull = reader.readByte();
        if (isNull == 1)
        {
            return null;
        }
        PingHeader newHdr = new PingHeader();
        newHdr.deserialize(reader);
        return newHdr;
    }

    public static void WritePingHeader(CacheObjectOutput writer, PingHeader hdr) throws IOException
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
    }
}
