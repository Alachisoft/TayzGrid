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

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.net.IRentableObject;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.*;


public class RequestCorrelatorHDR extends com.alachisoft.tayzgrid.cluster.Header implements ICompactSerializable, IRentableObject, Serializable
{


    public static final byte REQ = 0;

    public static final byte RSP = 1;

    public static final byte GET_REQ_STATUS = 3;

    public static final byte GET_REQ_STATUS_RSP = 4;

    public static final byte NHOP_REQ = 5;

    public static final byte NHOP_RSP = 6;
    public int rentid;
    /**
     * Type of header: request or reply
     */

    public byte type = REQ;
    /**
     * The id of this request to distinguish among other requests from the same <tt>RequestCorrelator</tt>
     */
    public long id = 0;
    /**
     * msg is synchronous if true
     */
    public boolean rsp_expected = true;
    /**
     * The unique name of the associated <tt>RequestCorrelator</tt>
     */
    //public string name = null;
    /**
     * Contains senders (e.g. P --> Q --> R)
     */
    public java.util.ArrayList call_stack = null;
    /**
     * Contains a list of members who should receive the request (others will drop). Ignored if null
     */
    public java.util.List dest_mbrs = null;
    public boolean serializeFlag = true;
    public RequestStatus reqStatus;
    public long status_reqId;
    public Address whomToReply;
    public Address expectResponseFrom;

    public boolean doProcess = true;

    /**
     * Used for externalization
     */
    public RequestCorrelatorHDR()
    {
    }

    /**
     * @param type type of header (<tt>REQ</tt>/<tt>RSP</tt>)
     *
     * @param id id of this header relative to ids of other requests originating from the same correlator
     *
     * @param rsp_expected whether it's a sync or async request
     *
     * @param name the name of the <tt>RequestCorrelator</tt> from which this header originates
     *
     */

    public RequestCorrelatorHDR(byte type, long id, boolean rsp_expected, String name)
    {
        this.type = type;
        this.id = id;
        this.rsp_expected = rsp_expected;
     
    }

    /**
     * @param type type of header (<tt>REQ</tt>/<tt>RSP</tt>)
     *
     * @param id id of this header relative to ids of other requests originating from the same correlator
     *
     * @param rsp_expected whether it's a sync or async request
     *
     * @param name the name of the <tt>RequestCorrelator</tt> from which this header originates
     * @param apptimeTaken Time taken to complete an operation by the receiving application.
     *
     */

    public RequestCorrelatorHDR(byte type, long id, boolean rsp_expected, String name, long apptimeTaken)
    {
        this.type = type;
        this.id = id;
        this.rsp_expected = rsp_expected;
        
    }

    @Override
    public String toString()
    {
        StringBuilder ret = new StringBuilder();
        
        String typeStr = "<unknown>";
        switch (type)
        {
            case REQ:
                typeStr = "REQ";
                break;

            case RSP:
                typeStr = "RSP";
                break;

            case GET_REQ_STATUS:
                typeStr = "GET_REQ_STATUS";
                break;

            case GET_REQ_STATUS_RSP:
                typeStr = "GET_REQ_STATUS_RSP";
                break;


        }
        ret.append(typeStr);
        ret.append(", id=" + id);
        ret.append(", rsp_expected=" + rsp_expected + ']');
        if (dest_mbrs != null)
        {
            ret.append(", dest_mbrs=").append(dest_mbrs);
        }
        return ret.toString();
    }

    public final void DeserializeLocal(ObjectInput reader) throws IOException
    {
        type = reader.readByte();
        id = reader.readLong();
        rsp_expected = reader.readBoolean();
        doProcess = reader.readBoolean();

        boolean getWhomToReply = reader.readBoolean();

        if (getWhomToReply)
        {
            this.whomToReply = new Address();
            this.whomToReply.DeserializeLocal(reader);
        }

        boolean getExpectResponseFrom = reader.readBoolean();
        if (getExpectResponseFrom)
        {
            this.expectResponseFrom = new Address();
            this.expectResponseFrom.DeserializeLocal(reader);
        }
    }

    public final void SerializeLocal(ObjectOutput writer) throws IOException
    {
        writer.write(type);
        writer.writeLong(id);
        writer.writeBoolean(rsp_expected);
        writer.writeBoolean(doProcess);

        if (whomToReply != null)
        {
            writer.writeBoolean(true);
            whomToReply.SerializeLocal(writer);
        }
        else
        {
            writer.writeBoolean(false);
        }

        if (expectResponseFrom != null)
        {
            writer.writeBoolean(true);
            expectResponseFrom.SerializeLocal(writer);
        }
        else
        {
            writer.writeBoolean(false);
        }
    }


    public final void deserialize(CacheObjectInput reader) throws IOException, ClassNotFoundException
    {
        type = reader.readByte();
        id = reader.readLong();
        rsp_expected = reader.readBoolean();
        Object tempVar = reader.readObject();
        reqStatus = (RequestStatus) ((tempVar instanceof RequestStatus) ? tempVar : null);
        status_reqId = reader.readLong();
      
        dest_mbrs = (java.util.ArrayList) reader.readObject();
        doProcess = reader.readBoolean();
        whomToReply = (Address) reader.readObject();
        expectResponseFrom = (Address) reader.readObject();

     
    }

    public final void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeByte(type);
        writer.writeLong(id);
        writer.writeBoolean(rsp_expected);
        writer.writeObject(reqStatus);
        writer.writeLong(status_reqId);
      
        if (serializeFlag)
        {
            writer.writeObject(dest_mbrs);
        }
        else
        {
            writer.writeObject(null);
        }

        writer.writeBoolean(doProcess);
        writer.writeObject(whomToReply);
        writer.writeObject(expectResponseFrom);
     
    }

    public static RequestCorrelatorHDR ReadCorHeader(CacheObjectInput reader) throws IOException, ClassNotFoundException
    {

        byte isNull = reader.readByte();
        if (isNull == 1)
        {
            return null;
        }
        RequestCorrelatorHDR newHdr = new RequestCorrelatorHDR();
        newHdr.deserialize(reader);
        return newHdr;
    }

    /**
     *
     * @param writer
     * @param hdr
     * @throws IOException
     */
    public static void WriteCorHeader(CacheObjectOutput writer, RequestCorrelatorHDR hdr) throws IOException
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

  
    public final void Reset()
    {
        dest_mbrs = call_stack = null;
        doProcess = rsp_expected = true;

   

        type = RequestCorrelatorHDR.REQ;
    }

    //<editor-fold defaultstate="collapsed" desc="IRentableObject Members">
    public final int getRentId()
    {
        return rentid;
    }

    public final void setRentId(int value)
    {
        rentid = value;
    }
    //</editor-fold>
}
