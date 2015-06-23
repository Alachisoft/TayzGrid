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

package com.alachisoft.tayzgrid.cluster;

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.io.Serializable;


/**
 * Identificator used to tell which View is first. <p><b>Author:</b> Chris Koiak, Bela Ban</p> <p><b>Date:</b> 12/03/2003</p>
 */

public class ViewId implements java.lang.Comparable, Cloneable, ICompactSerializable, Serializable
{

    /**
     * Address of the issuer of this view
     */
    private Address coord_addr;
    /**
     * Lamport time of the view
     */
    private long id;

    public ViewId()
    {
    }

    /**
     * Creates a ViewID with the coordinator address and a Lamport timestamp of 0.
     *
     *
     * Constructor
     *
     * @param coord_addr The address of the member that issued this view
     */
    public ViewId(Address coord_addr)
    {
        this.coord_addr = coord_addr;
    }

    /**
     * Creates a ViewID with the coordinator address and the given Lamport timestamp.
     *
     *
     * Constructor
     *
     * @param coord_addr The address of the member that issued this view
     * @param id The Lamport timestamp of the view
     */
    public ViewId(Address coord_addr, long id)
    {
        this.coord_addr = coord_addr;
        this.id = id;
    }

    /**
     * returns the lamport time of the view
     *
     * @return the lamport time timestamp
     *
     */
    public final long getId()
    {
        return id;
    }

    /**
     * returns the address of the member that issued this view
     *
     * @return the Address of the the issuer
     *
     */
    public final Address getCoordAddress()
    {
        return coord_addr;
    }

    /**
     * Returns a string representation of the ViewId
     *
     * @return A string representation of the ViewId
     */
    @Override
    public String toString()
    {
        return "[" + coord_addr + "|" + id + "]";
    }

    /**
     * Cloneable interface Returns a new ViewID object containing the same address and lamport timestamp as this view
     *
     */
    public final Object clone()
    {
        return new ViewId(coord_addr, id);
    }

    /**
     *
     * @return Clone-able interface Returns a new ViewID object containing the same address and lamport timestamp as this view
     */
    public final ViewId Copy()
    {
        return (ViewId) clone();
    }

    /**
     * Establishes an order between 2 ViewIds. First compare on id. <em>Compare on coord_addr only if necessary</em> (i.e. ids are equal) !
     *
     * @param other Second ViewId to compare to
     * @return 0 for equality, value less than 0 if smaller, greater than 0 if greater.
     */
    @Override
    public final int compareTo(Object other)
    {
        if (other == null || !(other instanceof ViewId))
        {
            return 1;
        }
        if (id > ((ViewId) other).id)
        {
            return 1;
        }
        else if (id < ((ViewId) other).id)
        {
            return -1;
        }
        else
        {
            return 0;
        }
    }

    /**
     * Determines if two ViewIds are equal
     *
     * @param other_view Second ViewId to compare to
     * @return True if ViewIds are equal
     */
    @Override
    public boolean equals(Object other_view)
    {
        return (compareTo(other_view) == 0);
    }

    /**
     * Returns the hascode of the ViewId
     *
     * @return The hascode of the ViewId
     */
    @Override
    public int hashCode()
    {
        if (coord_addr == null)
        {
            return (new Long(id)).hashCode();
        }
        return coord_addr.hashCode() ^ (new Long(id)).hashCode();
    }

    @Override
    //<editor-fold defaultstate="collapsed" desc="ICompactSerializable Members">
    public final void deserialize(CacheObjectInput reader) throws IOException, ClassNotFoundException
    {

        if(reader.readBoolean())
            coord_addr = (Address) reader.readObject();
        else
            coord_addr = null;
        id = reader.readLong();
    }

    @Override
    public final void serialize(CacheObjectOutput writer) throws IOException
    {

        if(coord_addr != null)
        {
            writer.writeBoolean(true);
            writer.writeObject(coord_addr);
        }
        else
            writer.writeBoolean(false);

        writer.writeLong(id);
    }
    //</editor-fold>

    public static ViewId ReadViewId(CacheObjectInput reader) throws IOException, ClassNotFoundException
    {

        byte isNull = reader.readByte();
        if (isNull == 1)
        {
            return null;
        }
        ViewId newId = new ViewId();
        newId.deserialize(reader);
        return newId;
    }

    public static void WriteViewId(CacheObjectOutput writer, ViewId vId) throws IOException
    {
        byte isNull = 1;
        if (vId == null)
        {
            writer.writeByte(isNull);
        }
        else
        {
            isNull = 0;
            writer.writeByte(isNull);
            vId.serialize(writer);
        }
    }
}
