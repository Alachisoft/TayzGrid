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

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.io.Serializable;

 
public class PingRsp implements ICompactSerializable, Serializable
{

    /**
     * Local Address
     */
    private Address own_addr;
    /**
     * Coordinator Address
     */
    private Address coord_addr;
    /**
     * Coordinator Address
     */
    private boolean is_server;
    private boolean started = true;

    public PingRsp()
    {
    }

    

    /**
     * Constructor
     *
     * @param own_addr Local Address
     * @param coord_addr Coordinator Address
     * @param is_server if the node is a participant of group
     * @param started
     */
    public PingRsp(Address own_addr, Address coord_addr, boolean is_server, boolean started)
    {
        this.own_addr = own_addr;
        this.coord_addr = coord_addr;
        this.is_server = is_server;
        this.started = started;

    }

    /**
     * Gets the Local Address
     * @return
     */
    public final Address getOwnAddress()
    {
        return own_addr;
    }

    /**
     * Gets the Coordinator Address
     * @return
     */
    public final Address getCoordAddress()
    {
        return coord_addr;
    }

    /**
     * Gets the Coordinator Address
     * @return
     */
    public final boolean getHasJoined()
    {
        return is_server;
    }

    /**
     * Checks if the response is from the coordinator
     *
     * @return True if the response is from the coordinator
     */
    public final boolean getIsCoord()
    {
        if (own_addr != null && coord_addr != null)
        {
            return own_addr.equals(coord_addr);
        }
        return false;
    }

    public final boolean getIsStarted()
    {
        return started;
    }

    @Override
    public boolean equals(Object obj)
    {
        PingRsp rsp = (PingRsp) ((obj instanceof PingRsp) ? obj : null);
        if (rsp == null)
        {
            return false;
        }

        if (own_addr != null)
        {
            return coord_addr.equals(rsp.coord_addr) && own_addr.equals(rsp.own_addr);
        }
        else
        {
            return (coord_addr == rsp.coord_addr) && (own_addr == rsp.own_addr);
        }
    }

    /**
     * Returns a string representation of the current object
     *
     * @return A string representation of the current object
     */
    @Override
    public String toString()
    {
        return "[own_addr=" + own_addr + ", coord_addr=" + coord_addr + "]";
    }

    /**
     * Retruns base hash code
     *
     * @return Base hash code
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }
 
    @Override
    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        own_addr = (Address) reader.readObject();
        coord_addr = (Address) reader.readObject();
        is_server = reader.readBoolean();
        started = reader.readBoolean();
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeObject(own_addr);
        writer.writeObject(coord_addr);
        writer.writeBoolean(is_server);
        writer.writeBoolean(started);
    }
 
}
