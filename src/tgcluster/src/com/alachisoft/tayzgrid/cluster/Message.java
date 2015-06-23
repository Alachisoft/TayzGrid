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

import com.alachisoft.tayzgrid.cluster.blocks.RequestCorrelatorHDR;
import com.alachisoft.tayzgrid.cluster.protocols.TOTAL;
import com.alachisoft.tayzgrid.cluster.protocols.TcpHeader;
import com.alachisoft.tayzgrid.common.enums.Priority;
import com.alachisoft.tayzgrid.common.FlagsByte;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.net.IRentableObject;
import com.alachisoft.tayzgrid.common.net.MemoryManager;
import com.alachisoft.tayzgrid.common.net.ObjectProvider;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import com.alachisoft.tayzgrid.serialization.standard.io.ObjectInputStream;
import com.alachisoft.tayzgrid.serialization.standard.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * A Message encapsulates data sent to members of a group. It contains among other things the address of the sender, the destination address, a payload (byte buffer) and a list of
 * headers. Headers are added by protocols on the sender side and removed by protocols on the receiver's side.
 *
 *
 * Message passed between members of a group. <p><b>Author:</b> Chris Koiak, Bela Ban</p> <p><b>Date:</b> 12/03/2003</p>
 */
public class Message implements ICompactSerializable, IRentableObject, java.io.Serializable//Serializable so it me be deepcloned when placed in an DataStructure
{

    /**
     * Destination of the message
     */
    protected Address dest_addr;
    /**
     * A list containing addresses of the recepient nodes of an mcast message.
     */
    protected java.util.List dest_addrs;
    /**
     * The flag that if true then TOTAL ensures sequencing of the message. Otherwise TOTAL just bypass it.
     */
    private boolean _isSeqRequired;
    /**
     * Source of the message
     */
    protected Address src_addr;
    /**
     * Prioirty of this message during transit.
     */
    protected Priority prio = Priority.Normal;
    /**
     * Headers added to the message
     */
    private java.util.HashMap headers = null;
    /**
     * Byte buffer of payload associated with the message
     */

    protected byte[] buf = null;
    /**
     * Flag that separates transactional traffic from management one.
     */
    private boolean isUserMsg;
    public boolean responseExpected = false;

    private boolean isProfilable;
    private long profileid;
    /**
     * Stack stats
     */
    private long psTime;
    /**
     * transport layer stats
     */
    private long tnspTime;
    private int rentId;
    private String traceMsg = "";
    private long reqId;
    private boolean handledAsynchronously;

    public byte _type = MsgType.SEQUENCE_LESS;

    private int offset = 0;

    private int length = 0;

    public static final long ADDRESS_OVERHEAD = 400; // estimated size of Address (src and dest)

    private Object[] _payload;

    public java.util.HashMap _stackTrace;

    public java.util.HashMap _stackTrace2;
    private java.util.Date sendTime = new java.util.Date(0);
    private java.util.Date arrivalTime = new java.util.Date(0);




    /**
     * Only used for Externalization (creating an initial object)
     */
    public Message()
    {
        headers = new java.util.HashMap();
        isUserMsg = false;
    } // should not be called as normal constructor

    /**
     * Constructor
     *
     * @param dest Destination of the message
     * @param src Source of the message
     * @param buf Byte buffer of payload associated with the message
     */

    public Message(Address dest, Address src, byte[] buf)
    {
        dest_addr = dest;
        src_addr = src;
        setBuffer(buf);
        headers = new java.util.HashMap();
        isUserMsg = false;
    }

    /**
     * Constructs a message. The index and length parameters allow to provide a <em>reference</em> to a byte buffer, rather than a copy, and refer to a subset of the buffer. This
     * is important when we want to avoid copying. When the message is serialized, only the subset is serialized.
     *
     * @param dest Address of receiver. If it is <em>null</em> or a <em>string</em>, then it is sent to the group (either to current group or to the group as given in the string).
     * If it is a Vector, then it contains a number of addresses to which it must be sent. Otherwise, it contains a single destination.<p> Addresses are generally untyped (all are
     * of type <em>Object</em>. A channel instance must know what types of addresses it expects and downcast accordingly.
     *
     * @param src Address of sender
     *
     * @param buf A reference to a byte buffer
     *
     * @param offset The index into the byte buffer
     *
     * @param length The number of bytes to be used from <tt>buf</tt>. Both index and length are checked for array index violations and an ArrayIndexOutOfBoundsException will be
     * thrown if invalid
     *
     */

    public Message(Address dest, Address src, byte[] buf, int offset, int length)
    {
        dest_addr = dest;
        src_addr = src;
        headers = new java.util.HashMap();
        isUserMsg = false;
        setBuffer(buf, offset, length);
    }

    /**
     * Gets or sets the trace message.
     */
    public final String getTraceMsg()
    {
        return traceMsg;
    }

    public final void setTraceMsg(String value)
    {
        traceMsg = value;
    }


    public final byte getType()
    {
        return _type;
    }


    public final void setType(byte value)
    {
        _type = value;
    }

    /**
     * Gets or set the id of the request.
     */
    public final long getRequestId()
    {
        return reqId;
    }

    public final void setRequestId(long value)
    {
        reqId = value;
    }

    /**
     * Indicates wheter message should be processed asynchronously or not.
     */
    public final boolean getHandledAysnc()
    {
        return handledAsynchronously;
    }

    public final void setHandledAysnc(boolean value)
    {
        handledAsynchronously = value;
    }

    public final void MarkSent()
    {
        sendTime = new java.util.Date();
    }

    public final void MarkArrived()
    {
        arrivalTime = new java.util.Date();
    }

    /**
     * Constructor
     *
     * @param dest Destination of the message
     * @param src Source of the message
     * @param obj Serialisable payload OR array of <c>Message</c>s
     */
    public Message(Address dest, Address src, Object obj) throws Exception
    {
        dest_addr = dest;
        src_addr = src;
        headers = new java.util.HashMap();
        if (obj != null)
        {

            //: to check if the object is serializable or not
            if (obj.getClass() instanceof java.io.Serializable)
            {
                setObject(obj);
            }
            else
            {
                throw new Exception("Message can only contain an Array of messages or an ISerializable object");
            }
        }
    }

    public long timestamp = -1;

    public final void markTimeStamp()
    {
        //: using System.currentTimeMillis() but it has accuracy issues
        timestamp = System.currentTimeMillis();
        //timestamp = (System.currentTimeMillis() - 621355968000000000L) / 10000;
    }

    public final long getTimeTaken()
    {
        //: using System.currentTimeMillis() but it has accuracy issues
        return System.currentTimeMillis() - timestamp;
    }


    public final Address getDest()
    {
        return dest_addr;
    }

    public final void setDest(Address value)
    {
        dest_addr = value;
    }

    public final java.util.List getDests()
    {
        return dest_addrs;
    }

    public final void setDests(java.util.List value)
    {
        dest_addrs = value;
    }

    public final Address getSrc()
    {
        return src_addr;
    }

    public final void setSrc(Address value)
    {
        src_addr = value;
    }

    public final boolean getIsSeqRequired()
    {
        return _isSeqRequired;
    }

    public final void setIsSeqRequired(boolean value)
    {
        _isSeqRequired = value;
    }

    /**
     * Gets and sets the payload (bute buffer) of the message
     */

    public final byte[] getRawBuffer()
    {
        return buf;
    }


    public final void setRawBuffer(byte[] value)
    {
        buf = value;
    }

    /**
     * Returns the offset into the buffer at which the data starts
     */
    public final int getOffset()
    {
        return offset;
    }

    /**
     * Returns the number of bytes in the buffer
     */
    public final int getLength()
    {
        return length;
    }

    public final void setLength(int value)
    {
        length = value;
    }

    /**
     * The number of backup caches configured with this instance.
     */
    public final Priority getPriority()
    {
        return prio;
    }

    public final void setPriority(Priority value)
    {
        prio = value;
    }

    /**
     * Flag that separates transactional traffic from management one.
     */
    public final boolean getIsUserMsg()
    {
        return isUserMsg;
    }

    public final void setIsUserMsg(boolean value)
    {
        isUserMsg = value;
    }

    /**
     * Indicates whether the message is profilable or not. Flag used for performance benchmarking.
     */
    public final boolean getIsProfilable()
    {
        return isProfilable;
    }

    public final void setIsProfilable(boolean value)
    {
        isProfilable = value;
    }

    /**
     * Gets or sets the profile id.
     */
    public final long getProfileId()
    {
        return profileid;
    }

    public final void setProfileId(long value)
    {
        profileid = value;
    }

    public final long getStackTimeTaken()
    {
        return psTime;
    }

    public final void setStackTimeTaken(long value)
    {
        psTime = value;
    }

    public final long getTransportTimeTaken()
    {
        return tnspTime;
    }

    public final void setTransportTimeTaken(long value)
    {
        tnspTime = value;
    }

    /**
     * Gets the collection of Headers added to the message
     */
    public final java.util.HashMap getHeaders()
    {
        return headers;
    }

    public final void setHeaders(java.util.HashMap value)
    {
        headers = value;
    }

    /**
     * Compares a second <c>Message</c> for equality
     *
     * @param obj Second Message object
     * @return True if Messages are equal
     */
    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Message))
        {
            return false;
        }
        Message msg2 = (Message) obj;
        if ((dest_addr == null || msg2.getDest() == null) && (msg2.getDest() != null || dest_addr != null))
        {
            return false;
        }
        else if (dest_addr != null && !dest_addr.equals(msg2.getDest()))
        {
            return false;
        }

        if ((src_addr == null || msg2.getSrc() == null) && (msg2.getSrc() != null || src_addr != null))
        {
            return false;
        }
        else if (src_addr != null && !src_addr.equals(msg2.getSrc()))
        {
            return false;
        }

        if (buf != msg2.getRawBuffer() || headers.size() != msg2.getHeaders().size())
        {
            return false;
        }
        Set set = headers.keySet();

        for (Object h : set)
        {
            if (!msg2.getHeaders().containsKey(h))
            {
                return false;
            }
        }

        return true;
    }

    public final void AddTrace(Address node, String trace)
    {
        if (_stackTrace == null)
        {
            _stackTrace = new java.util.HashMap();
        }
        synchronized (_stackTrace)
        {
            java.util.ArrayList traceList = null;
            if (_stackTrace.containsKey(node))
            {
                traceList = (java.util.ArrayList) ((_stackTrace.get(node) instanceof java.util.ArrayList) ? _stackTrace.get(node) : null);
            }
            else
            {
                traceList = new java.util.ArrayList();
                _stackTrace.put(node, traceList);
            }
            traceList.add(new MessageTrace(trace));
        }
    }

    public final String GetTrace()
    {
        return GetTraceInternal(_stackTrace);
    }

    public final String GetTrace2()
    {
        return GetTraceInternal(_stackTrace2);
    }

    private String GetTraceInternal(java.util.HashMap traceTable)
    {
        String trace = "";
        StringBuilder sb = null;
        if (traceTable != null)
        {
            sb = new StringBuilder();
            synchronized (traceTable)
            {
                Iterator iterator = traceTable.entrySet().iterator();
                while (iterator.hasNext())
                {
                    Map.Entry ide = (Map.Entry) iterator.next();
                    Address node = (Address) ((ide.getKey() instanceof Address) ? ide.getKey() : null);
                    java.util.ArrayList traceList = (java.util.ArrayList) ((ide.getValue() instanceof java.util.ArrayList) ? ide.getValue() : null);
                    if (traceList != null)
                    {
                        sb.append("TraceNode : " + node + " [ ");
                        for (int i = 0; i < traceList.size(); i++)
                        {
                            sb.append(traceList.get(i).toString() + " ; ");
                        }
                        sb.append(" ] ");
                    }
                }
            }
            trace = sb.toString();
        }
        return trace;
    }

    /**
     * Returns the hash value for a Message
     *
     * @return The hash value for a Message
     */
    @Override
    public int hashCode()
    {
        int retValue = headers == null ? 0 : headers.hashCode();
        java.util.ArrayList hc = new java.util.ArrayList();
        if (dest_addr != null)
        {
            hc.add(dest_addr.hashCode());
        }
        if (src_addr != null)
        {
            hc.add(src_addr.hashCode());
        }
        if (buf != null)
        {
            hc.add(buf.hashCode());
        }

        for (int i = 0; i < hc.size(); i++)
        {
            retValue = (new Integer(retValue)).hashCode() ^ hc.get(i).hashCode();
        }

        return retValue;
    }

    /**
     * Returns a copy of the buffer if offset and length are used, otherwise a reference
     *
     * @return
     *
     */

    public byte[] getBuffer()
    {
        if (buf == null)
        {
            return null;
        }
        if (offset == 0 && length == buf.length)
        {
            return buf;
        }
        else
        {

            byte[] retval = new byte[length];
            System.arraycopy(buf, offset, retval, 0, length);
            return retval;
        }
    }

    public void setBuffer(byte[] b)
    {
        buf = b;
        if (buf != null)
        {
            offset = 0;
            length = buf.length;
        }
        else
        {
            offset = length = 0;
        }
    }

    /**
     * Set the internal buffer to point to a subset of a given buffer
     *
     * @param b The reference to a given buffer. If null, we'll reset the buffer to null
     *
     * @param offset The initial position
     *
     * @param length The number of bytes
     *
     */

    public void setBuffer(byte[] b, int offset, int length)
    {
        buf = b;
        if (buf != null)
        {
            if (offset < 0 || offset > buf.length)
            {
                throw new IndexOutOfBoundsException("Index of bound " + offset);
            }
            if ((offset + length) > buf.length)
            {
                throw new IndexOutOfBoundsException("Index of bound " + (offset + length));
            }
            this.offset = offset;
            this.length = length;
        }
        else
        {
            offset = length = 0;
        }
    }

    /**
     * Serialises an object in to the payload
     *
     * @param obj Object to serialise
     */
    public final void setObject(Object obj) throws IOException, Exception
    {
        if (buf != null)
        {
            return;
        }
        if (!(obj.getClass() instanceof Serializable))
        {
            throw new Exception("Specified object for message is not serializable");
        }
        setBuffer(CompactBinaryFormatter.toByteBuffer(obj, ""));
    }

    /**
     * Deserialises an Object from the payload
     *
     * @return Deserialised Object
     */
    public final Object getObject() throws IOException, ClassNotFoundException
    {
        if (buf == null)
        {
            return null;
        }
        return CompactBinaryFormatter.fromByteBuffer(buf, "");
    }

    /**
     * Get the pay load without deserializing it.
     *
     * @return Deserialised Object
     */
    public final Object getFlatObject()
    {
        return buf;
    }

    /**
     * Nulls all fields of this message so that the message can be reused. Removes all headers from the stack, but keeps the stack
     */
    public void reset()
    {
        dest_addr = src_addr = null;
        setBuffer(null);
        if (headers != null)
        {
            headers.clear();
        }
        prio = Priority.Normal;
        this.isProfilable = false;
        profileid = psTime = tnspTime = 0;
        traceMsg = "";
        isUserMsg = false;
    }

    /**
     * Creates a byte buffer representation of a <c>long</c>
     *
     * @param value <c>long</c> to be converted
     * @return Byte Buffer representation of a <c>long</c>
     */

    public final byte[] WriteInt64(long value)
    {

        byte[] _byteBuffer = new byte[8];

        _byteBuffer[0] = (byte) value;


        _byteBuffer[1] = (byte) (value >> 8);

        _byteBuffer[2] = (byte) (value >> 16);

        _byteBuffer[3] = (byte) (value >> 24);

        _byteBuffer[4] = (byte) (value >> 32);

        _byteBuffer[5] = (byte) (value >> 40);

        _byteBuffer[6] = (byte) (value >> 48);

        _byteBuffer[7] = (byte) (value >> 56);
        return _byteBuffer;
    } 

    /**
     * Creates a <c>long</c> from a byte buffer representation
     *
     * @param _byteBuffer Byte Buffer representation of a <c>long</c>
     * @return
     */

    private long convertToLong(byte[] _byteBuffer)
    {
        return (long) ((_byteBuffer[0] & 0xFF) | _byteBuffer[1] << 8 | _byteBuffer[2] << 16 | _byteBuffer[3] << 24 | _byteBuffer[4] << 32 | _byteBuffer[5] << 40 | _byteBuffer[6]
                << 48 | _byteBuffer[7] << 56);
    } 
    public Message copy()
    {
        return copy(true, null);
    }

    public Message copy(MemoryManager memManager)
    {
        return copy(true, memManager);
    }

    /**
     * Create a copy of the message. If offset and length are used (to refer to another buffer), the copy will contain only the subset offset and length point to, copying the
     * subset into the new copy.
     *
     * @param copy_buffer
     *
     * @return
     *
     */
    public Message copy(boolean copy_buffer, MemoryManager memManaager)
    {
        Message retval = null;
        if (memManaager != null)
        {
            ObjectProvider provider = memManaager.GetProvider(Message.class);
            if (provider != null)
            {
                retval = (Message) provider.RentAnObject();
            }
        }
        else
        {
            retval = new Message();
        }

        retval.dest_addr = dest_addr;
        retval.dest_addrs = dest_addrs;
        retval.src_addr = src_addr;
        retval.prio = prio;
        retval.isUserMsg = isUserMsg;
        retval.isProfilable = getIsProfilable();
        retval.setProfileId(profileid);
        retval.psTime = psTime;
        retval.tnspTime = tnspTime;
        retval.traceMsg = traceMsg;
        retval.setRequestId(reqId);
        retval.handledAsynchronously = handledAsynchronously;
        retval.responseExpected = responseExpected;

        if (copy_buffer && buf != null)
        {
            retval.setBuffer(buf, offset, length);
        }
        
        if (headers != null)
        {
            retval.headers = (java.util.HashMap) headers.clone();
            
        }
        retval.setPayload(this.getPayload());
        return retval;
    }

    
    public Object clone()
    {
        return copy();
    }

    public Message makeReply(ObjectProvider MsgProvider)
    {
        Message m = null;
        if (MsgProvider != null)
        {
            m = (Message) MsgProvider.RentAnObject();
        }
        else
        {
            m = new Message(src_addr, null, null);
        }

        m.dest_addr = src_addr;
        m.profileid = profileid;
        m.isProfilable = isProfilable;
        
        return m;
    }

    public Message makeReply()
    {
        Message m = new Message(src_addr, null, null);
        m.setIsUserMsg(getIsUserMsg());
        m.dest_addr = src_addr;
        m.profileid = profileid;
        m.isProfilable = isProfilable;
        if (getIsProfilable())
        {
            m.setTraceMsg(traceMsg + "-->complete");
        }
        return m;
    }

    /**
     * Returns size of buffer, plus some constant overhead for src and dest, plus number of headers time some estimated size/header. The latter is needed because we don't want to
     * marshal all headers just to find out their size requirements. If a header implements Sizeable, the we can get the correct size.<p> Size estimations don't have to be very
     * accurate since this is mainly used by FRAG to determine whether to fragment a message or not. Fragmentation will then serialize the message, therefore getting the correct
     * value.
     */
    public long size()
    {
        long retval = length;
        long hdr_size = 0;
        Header hdr;

        if (dest_addr != null)
        {
            retval += ADDRESS_OVERHEAD;
        }
        if (src_addr != null)
        {
            retval += ADDRESS_OVERHEAD;
        }

        if (headers != null)
        {
            for (java.util.Iterator it = headers.values().iterator(); it.hasNext();)
            {
                hdr = (Header) it.next();
                if (hdr == null)
                {
                    continue;
                }
                hdr_size = hdr.size();
                if (hdr_size <= 0)
                {
                    hdr_size = Header.HDR_OVERHEAD;
                }
                else
                {
                    retval += hdr_size;
                }
            }
        }
        return retval;
    }

    /*
     * ---------------------- Used by protocol layers ----------------------
     */
    /**
     * Gets a header associated with a Protocol layer
     *
     * @param key Protocol Name associated with the header
     * @return Implementation of the HDR class
     */
    public final Header getHeader(Object key)
    {
        if (headers != null && headers.containsKey(key))
        {
            return (Header) headers.get(key);
        }
        return null;
    }

    /**
     * Adds a header in to the Message
     *
     * @param key Protocol Name associated with the header
     * @param hdr Implementation of the HDR class
     */
    public final void putHeader(Object key, Header hdr)
    {
        try
        {
           
            headers.put(key, hdr);
        }
        catch (IllegalArgumentException e)
        {
            
        }
    }

    /**
     * Removes a header associated with a Protocol layer
     *
     * @param key Protocol Name associated with the header
     * @return Implementation of the HDR class
     */
    public final Header removeHeader(Object key)
    {
        Header retValue = (Header) headers.get(key);
        headers.remove(key);
        return retValue;
    }

    /**
     * Clears all Headers from message
     */
    public final void removeHeaders()
    {
        if (headers != null)
        {
            headers.clear();
        }
    }

    public String printObjectHeaders()
    {
        StringBuilder sb = new StringBuilder();
        Map.Entry entry;

        if (headers != null)
        {
            for (java.util.Iterator it = headers.entrySet().iterator(); it.hasNext();)
            {
                entry = (Map.Entry) it.next();
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
            }
        }
        return sb.toString();
    }


    public final Object[] getPayload()
    {
        return _payload;
    }

    public final void setPayload(Object[] value)
    {
        _payload = value;
    }


    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        dest_addr = (Address) reader.readObject();
        dest_addrs = (java.util.ArrayList) reader.readObject();
        src_addr = (Address) reader.readObject();
        prio = Priority.forValue((int)reader.readShort());
        boolean check = reader.readBoolean();
        if (check)
        {
            buf = (byte[]) reader.readObject();
        }
        headers = (java.util.HashMap) reader.readObject();
        handledAsynchronously = reader.readBoolean();
        responseExpected = reader.readBoolean();
        _type = reader.readByte();
        Object tempVar = reader.readObject();
        _stackTrace = (java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null);

        offset = 0;
        length = (buf != null) ? buf.length : 0;
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeObject(dest_addr);
        writer.writeObject(dest_addrs);
        writer.writeObject(src_addr);
        writer.writeShort((short) prio.getValue());
        if (buf == null || buf.length == 0)
        {
            writer.writeBoolean(false);
        }
        else
        {
            writer.writeBoolean(true);
            writer.writeObject(buf);
        }
        writer.writeObject(headers);
        writer.writeBoolean(handledAsynchronously);
        writer.writeBoolean(responseExpected);
        writer.writeByte(_type);
        writer.writeObject(_stackTrace);

    }


    public final int getRentId()
    {
        return rentId;
    }

    public final void setRentId(int value)
    {
        rentId = value;
    }


    public final void DeserializeLocal(ObjectInput reader) throws IOException, ClassNotFoundException
    {
        isUserMsg = true;

        byte flags = reader.readByte();
        FlagsByte bFlags = new FlagsByte();
        bFlags.setDataByte(flags);
      
        headers = new java.util.HashMap();
        if (bFlags.AnyOn(FlagsByte.Flag.COR))
        {
            RequestCorrelatorHDR corHdr = new RequestCorrelatorHDR();
            corHdr.DeserializeLocal((ObjectInputStream)reader);
            headers.put(HeaderType.REQUEST_COORELATOR, corHdr);
        }

        if (bFlags.AnyOn(FlagsByte.Flag.TOTAL))
        {
            TOTAL.HDR totalHdr = new TOTAL.HDR();
            totalHdr.deserialize((ObjectInputStream)reader);
            headers.put(HeaderType.TOTAL, totalHdr);

        }

        if (bFlags.AnyOn(FlagsByte.Flag.TCP))
        {
            TcpHeader tcpHdr = new TcpHeader();
            tcpHdr.DeserializeLocal((ObjectInputStream)reader);
            headers.put(HeaderType.TCP, tcpHdr);
        }

        prio = Priority.forValue(reader.readShort());
        handledAsynchronously = reader.readBoolean();
        long ticks = reader.readLong();
        arrivalTime = new java.util.Date(ticks);
        ticks = reader.readLong();
        sendTime = new java.util.Date(ticks);
        responseExpected = reader.readBoolean();
        _type = reader.readByte();
        
        length = reader.readInt();
        if(buf == null)
            buf = new byte[length];
        reader.readFully(buf,0,length);
      
      
    }

    public final void SerializeLocal(ObjectOutput writerStream) throws IOException
    {

       
        ByteArrayOutputStream bite = new ByteArrayOutputStream();
        ObjectOutputStream writer = new ObjectOutputStream(bite, "");


        FlagsByte bFlags = new FlagsByte();
        if (getIsUserMsg())
        {
            bFlags.SetOn(FlagsByte.Flag.TRANS);
        }

        Object tmpHdr;
        tmpHdr = (Header) headers.get((Object) (HeaderType.REQUEST_COORELATOR));
        if (tmpHdr != null)
        {
            RequestCorrelatorHDR corHdr = (RequestCorrelatorHDR) tmpHdr;
            corHdr.SerializeLocal((ObjectOutputStream)writer);
            bFlags.SetOn(FlagsByte.Flag.COR);
        }

        tmpHdr = (Header) headers.get((Object) (HeaderType.TOTAL));
        if (tmpHdr != null)
        {
            TOTAL.HDR totalHdr = (TOTAL.HDR) tmpHdr;
            totalHdr.SerializeLocal((ObjectOutputStream)writer);
            bFlags.SetOn(FlagsByte.Flag.TOTAL);
        }

        tmpHdr = (Header) headers.get((Object) (HeaderType.TCP));
        if (tmpHdr != null)
        {
            TcpHeader tcpHdr = (TcpHeader) tmpHdr;
            tcpHdr.SerializeLocal((ObjectOutputStream)writer);
            bFlags.SetOn(FlagsByte.Flag.TCP);
        }

        writer.writeShort((short) prio.getValue());
        writer.writeBoolean(handledAsynchronously);
        writer.writeLong(arrivalTime.getTime());
        writer.writeLong(sendTime.getTime());
        writer.writeBoolean(responseExpected);
        writer.writeByte(_type);
        
        int length = buf.length;
        writer.writeInt(length);
        writer.write(buf);
        writer.flush();

       byte[] data = bite.toByteArray();


       byte[] buffer = new byte[data.length + 1];
       buffer[0] = bFlags.getDataByte();
       System.arraycopy(data, 0, buffer, 1, data.length);

       writerStream.write(buffer);

    }

}
