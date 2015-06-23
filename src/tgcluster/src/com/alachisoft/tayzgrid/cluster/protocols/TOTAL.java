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

import com.alachisoft.tayzgrid.cluster.util.Util;
import com.alachisoft.tayzgrid.cluster.stack.Protocol;
import com.alachisoft.tayzgrid.cluster.stack.ProtocolStackType;
import com.alachisoft.tayzgrid.cluster.stack.AckSenderWindow;
import com.alachisoft.tayzgrid.cluster.HeaderType;
import com.alachisoft.tayzgrid.cluster.Event;
import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.cluster.Global;
import com.alachisoft.tayzgrid.cluster.MsgType;
import com.alachisoft.tayzgrid.cluster.ViewId;
import com.alachisoft.tayzgrid.cluster.View;
import com.alachisoft.tayzgrid.common.threading.TimeScheduler;
import com.alachisoft.tayzgrid.common.stats.HPTimeStats;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.net.IRentableObject;
import com.alachisoft.tayzgrid.common.net.ObjectProvider;
import com.alachisoft.tayzgrid.common.enums.Priority;
import com.alachisoft.tayzgrid.common.event.NEvent;
import com.alachisoft.tayzgrid.common.event.NEventEnd;
import com.alachisoft.tayzgrid.common.event.NEventStart;
import com.alachisoft.tayzgrid.common.GenericCopier;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.*;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// $Id: TOTAL.java,v 1.6 2004/07/05 14:17:16 belaban Exp $
 
/**
 * Implements the total ordering layer using a message sequencer <p>
 *
 * The protocol guarantees that all bcast sent messages will be delivered in the same order to all members. For that it uses a sequencer which assignes monotonically increasing
 * sequence ID to broadcasts. Then all group members deliver the bcasts in ascending sequence ID order. <p> <ul> <li> When a bcast message comes down to this layer, it is placed in
 * the pending down queue. A bcast request is sent to the sequencer.</li> <li> When the sequencer receives a bcast request, it creates a bcast reply message and assigns to it a
 * monotonically increasing seqID and sends it back to the source of the bcast request.</li> <li> When a broadcast reply is received, the corresponding bcast message is assigned
 * the received seqID. Then it is broadcasted.</li> <li> Received bcasts are placed in the up queue. The queue is sorted according to the seqID of the bcast. Any message at the
 * head of the up queue with a seqID equal to the next expected seqID is delivered to the layer above.</li> <li> Unicast messages coming from the layer below are forwarded
 * above.</li> <li> Unicast messages coming from the layer above are forwarded below.</li> </ul> <p> <i>Please note that once a
 * <code>BLOCK_OK</code> is acknowledged messages coming from above are discarded!</i> Either the application must stop sending messages when a
 * <code>BLOCK</code> event is received from the channel or a QUEUE layer should be placed above this one. Received messages are still delivered above though. <p> bcast requests
 * are retransmitted periodically until a bcast reply is received. In case a BCAST_REP is on its way during a BCAST_REQ retransmission, then the next BCAST_REP will be to a
 * non-existing BCAST_REQ. So, a nulll BCAST message is sent to fill the created gap in the seqID of all members.
 *
 *
 * <author> i.georgiadis@doc.ic.ac.uk </author>
 */
public class TOTAL extends Protocol
{

    /**
     * The header processed by the TOTAL layer and intended for TOTAL inter-stack communication
     */
    public static class HDR extends com.alachisoft.tayzgrid.cluster.Header implements ICompactSerializable, IRentableObject, Serializable
    {
        

        /**
         * Null value for the tag
         */
 
        public static final byte NULL_TYPE = 0;
        /**
         * Request to broadcast by the source
         */
 
        public static final byte REQ = 1;
        /**
         * Reply to broadcast request.
         */
 
        public static final byte REP = 2;
        /**
         * Unicast message
         */
 
        public static final byte UCAST = 3;
        /**
         * Broadcast Message
         */
 
        public static final byte BCAST = 4;
        /**
         * Multicast Message
         */
 
        public static final byte MCAST = 5;
        /**
         * Request to multicast by the source.
         */
 
        public static final byte REQMCAST = 6;
        /**
         * Reply to a multicast request.
         */
 
        public static final byte REPMCAST = 7;
        public int rentId;
        /**
         * The header's type tag
         */
 
        public byte type;
        /**
         * The ID used by the message source to match replies from the sequencer
         */
        public long localSeqID;
        /**
         * The ID imposing the total order of messages
         */
        public long seqID;
        public int viewId;

        /**
         * used for externalization
         */
        public HDR()
        {
        }

        /**
         * Create a header for the TOTAL layer
         *
         * @param type the header's type
         *
         * @param localSeqID the ID used by the sender of broadcasts to match requests with replies from the sequencer
         *
         * @param seqID the ID imposing the total order of messages
         *
         *
         * <throws> IllegalArgumentException if the provided header type is unknown</throws>
         *
         */
 
        public HDR(byte type, long localSeqID, long seqID, int viewId)
        {
            super();
            switch (type)
            {
                case REQ:
                case REP:
                case UCAST:
                case BCAST:
                case MCAST:
                case REQMCAST:
                case REPMCAST:
                    this.type = type;
                    break;

                default:
                    this.type = NULL_TYPE;
                    throw new IllegalArgumentException("Invalid header type.");
            }
            this.localSeqID = localSeqID;
            this.seqID = seqID;
            this.viewId = viewId;
        }

        @Override
        public Object clone(ObjectProvider provider)
        {
            HDR hdr = null;
            if (provider != null)
            {
                hdr = (HDR) provider.RentAnObject();
            }
            else
            {
                hdr = new HDR();
            }
            hdr.type = this.type;
            hdr.seqID = seqID;
            hdr.localSeqID = localSeqID;
            hdr.viewId = viewId;
            return hdr;
        }

        /**
         * For debugging purposes
         */
        @Override
        public String toString()
        {
            StringBuilder buffer = new StringBuilder();
            String typeName;
            buffer.append("[TOTAL.HDR");
            switch (type)
            {
                case REQ:
                    typeName = "REQ";
                    break;
                case REQMCAST:
                    typeName = "REQMCAST";
                    break;
                case REP:
                    typeName = "REP";
                    break;
                case REPMCAST:
                    typeName = "REPMCAST";
                    break;
                case UCAST:
                    typeName = "UCAST";
                    break;
                case BCAST:
                    typeName = "BCAST";
                    break;
                case MCAST:
                    typeName = "MCAST";
                    break;
                case NULL_TYPE:
                    typeName = "NULL_TYPE";
                    break;
                default:
                    typeName = "";
                    break;
            }
            buffer.append(", type=" + typeName);
            buffer.append(", " + "localID=" + localSeqID);
            buffer.append(", " + "seqID=" + seqID);
            buffer.append(", " + "viewId=" + viewId);
            buffer.append(']');

            return (buffer.toString());
        }

        public final void Reset()
        {
            seqID = localSeqID = NULL_ID;
            type = NULL_TYPE;
        }
 

        public final void deserialize(CacheObjectInput reader) throws IOException
        {
            type = reader.readByte();
            localSeqID = reader.readLong();
            seqID = reader.readLong();
            viewId = reader.readInt();
        }

        public final void serialize(CacheObjectOutput writer) throws IOException
        {
            writer.writeByte(type);
            writer.writeLong(localSeqID);
            writer.writeLong(seqID);
            writer.writeInt(viewId);
        }
 

        public static HDR ReadTotalHeader(CacheObjectInput reader) throws IOException
        {
 
            byte isNull = reader.readByte();
            if (isNull == 1)
            {
                return null;
            }
            HDR newHdr = new HDR();
            newHdr.deserialize(reader);
            return newHdr;
        }

        public static void WriteTotalHeader(CacheObjectOutput writer, HDR hdr) throws IOException
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

 
        public final int getRentId()
        {
            return rentId;
        }

        public final void setRentId(int value)
        {
            rentId = value;
        }
 
        public final void DeserializeLocal(ObjectInputStream reader) throws IOException
        {
            type = reader.readByte();
            localSeqID = reader.readLong();
            seqID = reader.readLong();
            viewId = reader.readInt();
        }

        public final void SerializeLocal(ObjectOutput writer) throws IOException
        {
            writer.write(type);
            writer.writeLong(localSeqID);
            writer.writeLong(seqID);
            writer.writeInt(viewId);
        }
 
    }

    /**
     * The retransmission listener - It is called by the
     * <code>AckSenderWindow</code> when a retransmission should occur
     */
    private static class Command implements AckSenderWindow.RetransmitCommand
    {

        private void InitBlock(TOTAL enclosingInstance)
        {
            this.enclosingInstance = enclosingInstance;
        }
        private TOTAL enclosingInstance;

        public final TOTAL getEnclosing_Instance()
        {
            return enclosingInstance;
        }

        public Command(TOTAL enclosingInstance)
        {
            InitBlock(enclosingInstance);
        }

        public void retransmit(long seqNo, Message msg)
        {
            getEnclosing_Instance()._retransmitBcastRequest(seqNo);
        }
    }

    /**
     * The retransmission listener - It is called by the
     * <code>AckSenderWindow</code> when a retransmission should occur
     */
    private static class MCastCommand implements AckSenderWindow.RetransmitCommand
    {

        private void InitBlock(TOTAL enclosingInstance)
        {
            this.enclosingInstance = enclosingInstance;
        }
        private TOTAL enclosingInstance;

        public final TOTAL getEnclosing_Instance()
        {
            return enclosingInstance;
        }

        public MCastCommand(TOTAL enclosingInstance)
        {
            InitBlock(enclosingInstance);
        }

        public void retransmit(long seqNo, Message msg)
        {
            Object tempVar = GenericCopier.DeepCopy(msg.getDests());//msg.getDests().clone();
            java.util.ArrayList mbrs = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);

            String subGroupID = (String) ((getEnclosing_Instance()._mbrsSubgroupMap.get(mbrs.get(0)) instanceof String) ? getEnclosing_Instance()._mbrsSubgroupMap.get(mbrs.get(0)) : null);

             
            java.util.ArrayList groupMbrs = (java.util.ArrayList) (((java.util.ArrayList) getEnclosing_Instance()._sequencerTbl.get(subGroupID) instanceof java.util.ArrayList) ? (java.util.ArrayList) getEnclosing_Instance()._sequencerTbl.get(subGroupID) : null);
            Address groupSequencerAddr = (Address) ((groupMbrs.get(0) instanceof Address) ? groupMbrs.get(0) : null);
            if (groupSequencerAddr != null)
            {
                getEnclosing_Instance()._retransmitMcastRequest(seqNo, groupSequencerAddr);
            }
        }
    }
    /**
     * Protocol name
     */
    private static final String PROT_NAME = "TOTAL";
    /**
     * Property names
     */
    private static final String TRACE_PROP = "trace";
    /**
     * Average time between broadcast request retransmissions private long[] AVG_RETRANSMIT_INTERVAL = new long[]{1000, 2000, 3000, 4000};
     */
    private long[] AVG_RETRANSMIT_INTERVAL = new long[]
    {
        55000, 65000, 70000, 75000
    };
    /**
     * Average time between broadcast request retransmissions
     */
    private long[] AVG_MCAST_RETRANSMIT_INTERVAL = new long[]
    {
        60000, 65000, 70000, 75000
    };
    /**
     * Null value for the IDs
     */
    private static final long NULL_ID = -1;
    // Layer sending states
    /**
     * No group has been joined yet
     */
    private static final int NULL_STATE = -1;
    /**
     * When set, all messages are sent/received
     */
    private static final int RUN = 0;
    /**
     * When set, only session-specific messages are sent/received, i.e. only messages essential to the session's integrity
     */
    private static final int FLUSH = 1;
    /**
     * No message is sent to the layer below
     */
    private static final int BLOCK = 2;
    /**
     * The state lock allowing multiple reads or a single write
     */
    private ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();
    private Lock readLock = stateLock.readLock();
    private Lock writeLock = stateLock.writeLock();
    /**
     * Protocol layer message-sending state
     */
    private int state = NULL_STATE;
    /**
     * The address of this stack
     */
    private Address addr = null;
    /**
     * The address of the sequencer
     */
    private Address sequencerAddr = null;
    /**
     * The sequencer's seq ID. The ID of the most recently broadcast reply message
     */
    private long sequencerSeqID = NULL_ID;
    /**
     * Mutex to make sequenceID generation atomic
     */
    private final Object seqIDMutex = new Object();
    /**
     * Mutex to make sequenceID generation atomic
     */
    private final Object localSeqIDMutex = new Object();
    /**
     * Mutex to make sequenceID generation atomic
     */
    private final Object localMcastSeqIDMutex = new Object();
    /**
     * The local sequence ID, i.e. the ID sent with the last broadcast request message. This is increased with every broadcast request sent to the sequencer and it's used to match
     * the requests with the sequencer's replies
     */
    private long localSeqID = NULL_ID;
    /**
     * The total order sequence ID. This is the ID of the most recently delivered broadcast message. As the sequence IDs are increasing without gaps, this is used to detect missing
     * broadcast messages
     */
    private long seqID = NULL_ID;
    //==========================================
    private long _mcastSequencerSeqID = NULL_ID;
    private long _mcastLocalSeqID = NULL_ID;
    private long _mcastSeqID = NULL_ID;
    private java.util.HashMap _sequencerTbl = new java.util.HashMap();
    private java.util.HashMap _mbrsSubgroupMap;
    private java.util.HashMap _mcastUpTbl;
    private java.util.HashMap _mcastReqTbl;
    private String subGroup_addr = null;
    private final Object _mcastSeqIDMutex = new Object();
    private Address _groupSequencerAddr = null;
    private int _groupMbrsCount = 0; //indicator of group mbrs change.
    private AckSenderWindow _mcastRetransmitter;
    private java.util.List _undeliveredMessages = Collections.synchronizedList(new java.util.ArrayList());
    //=============================================
    /**
     * The list of unanswered broadcast requests to the sequencer. The entries are stored in increasing local sequence ID, i.e. in the order they were
     *
     * sent localSeqID -> Broadcast msg to be sent.
     */
    private java.util.HashMap reqTbl;
    /**
     * it allows the sequencer itself to get next sequence directly.
     */
    //private bool shortcut = true;
    /**
     * The list of received broadcast messages that haven't yet been delivered to the layer above. The entries are stored in increasing sequence ID, i.e. in the order they must be
     * delivered above
     *
     * seqID -> Received broadcast msg
     */
    private java.util.HashMap upTbl;
    /**
     * Retranmitter for pending broadcast requests
     */
    private AckSenderWindow retransmitter;
    /**
     * Used to shortcircuit transactional messages from management messages.
     */
    private Protocol transport;
    private ReentrantReadWriteLock request_lock = new ReentrantReadWriteLock();
    private Lock requestReadLock = request_lock.readLock();
    private Lock requestWriteLock = request_lock.writeLock();
    private long start_time = 0;
    //private long timeout = 5000;
    private long start_time_bcast = 0;
    //private long time_left;
    /**
     * used for monitoring
     */
    private HPTimeStats _timeToTakeBCastSeq = new HPTimeStats();
    private HPTimeStats _timeToTakeMCastSeq = new HPTimeStats();
    private HPTimeStats _totalWaitTime = new HPTimeStats();
    /**
     * operation timeout as specified by the user. we wait for te missing message for this timeout
     */
    private long opTimeout = 60000;
    private ViewId currentViewId;

    /**
     * Print addresses in host_ip:port form to bypass DNS
     */
    private String _addrToString(Object addr)
    {
        return (addr == null ? "<null>" : ((addr instanceof Address) ? (((Address) addr).getIpAddress().toString() + ':' + ((Address) addr).getPort()) : addr.toString()));
    }

    @Override
    public String getName()
    {
        return PROT_NAME;
    }

    /**
     * Returns the next squence number.
     */
    private long getNextSequenceID()
    {
        synchronized (seqIDMutex)
        {
            return ++sequencerSeqID;
        }
    }

    /**
     * Returns the next mcast sequence number.
     */
    private long getNextMCastSeqID()
    {
        synchronized (_mcastSeqIDMutex)
        {
            return ++_mcastSequencerSeqID;
        }
    }

    /**
     * Configure the protocol based on the given list of properties
     *
     *
     * @param properties the list of properties to use to setup this layer
     *
     * @return false if there was any unrecognized property or a property with an invalid value
     *
     */
    private boolean _setProperties(java.util.HashMap properties) throws Exception
    {
        super.setProperties(properties);

        if (stack.getStackType() == ProtocolStackType.TCP)
        {
            this.up_thread = false;
            this.down_thread = false;
            if (getStack().getCacheLog().getIsInfoEnabled())
            {
                getStack().getCacheLog().Info(getName() + ".setProperties", "part of TCP stack");
            }
        }

        if (properties.containsKey("timeout"))
        {
            long[] tmp = Util.parseCommaDelimitedLongs((String) ((properties.get("timeout") instanceof String) ? properties.get("timeout") : null));
            properties.remove("timeout");
            if (tmp != null && tmp.length > 0)
            {
                AVG_RETRANSMIT_INTERVAL = tmp;
            }
        }

        if (properties.containsKey("op_timeout"))
        {
            long val = Long.decode((String)properties.get("op_timeout"));
            opTimeout = val;
            properties.remove("op_timeout");
        }

        if (properties.size() > 0)
        {
            getStack().getCacheLog().Error("The following properties are not " + "recognized: " + Global.CollectionToString(properties.keySet()));
            return (true);
        }
        return (true);
    }

    /**
     * Events that some layer below must handle
     *
     *
     * @return the set of
     * <code>Event</code>s that must be handled by some layer below
     *
     */
    public java.util.List _requiredDownServices()
    {
        java.util.List services = Collections.synchronizedList(new java.util.ArrayList(10));

        return (services);
    }

    /**
     * Events that some layer above must handle
     *
     *
     * @return the set of
     * <code>Event</code>s that must be handled by some layer above
     *
     */
    public java.util.List _requiredUpServices()
    {
        java.util.List services = Collections.synchronizedList(new java.util.ArrayList(10));

        return (services);
    }

    /**
     * Extract as many messages as possible from the pending up queue and send them to the layer above
     */
    private void _deliverBcast()
    {
        long time_left = opTimeout;
        do
        {
            Message msg = null;
            synchronized (upTbl)
            {
                _msgArrived++;

                msg = (Message) upTbl.get((long) (seqID + 1));
                if (upTbl.size() > 0)
                {
                    if (getStack().getCacheLog().getIsInfoEnabled())
                    {
                        getStack().getCacheLog().Info("Total._deliverBCast()", "UP table [" + upTbl.size() + "]");
                    }
                    if (getStack().getCacheLog().getIsInfoEnabled())
                    {
                        getStack().getCacheLog().Info("Total._deliverBCast()", "seq: " + (seqID + 1));
                    }
                }
                if (msg == null)
                {
                    if (upTbl.size() > 0)
                    {
                        if (start_time_bcast == 0)
                        {
                             
                            start_time_bcast = System.currentTimeMillis();
                            
                        }

                         
                        time_left = opTimeout - (System.currentTimeMillis() - start_time_bcast);
                        
                        if (getStack().getCacheLog().getIsInfoEnabled())
                        {
                            getStack().getCacheLog().Info("Total._deliverBCast()", "timeout[" + (seqID + 1) + "] ->" + time_left);
                        }
                    }

                    if (time_left <= 0)
                    {
                        if (getStack().getCacheLog().getIsErrorEnabled())
                        {
                            getStack().getCacheLog().Error("Total._deliverBCast()", "bypassed a missing message " + (seqID + 1) + " timeout :" + time_left);
                        }
                        if (getStack().getCacheLog().getIsErrorEnabled())
                        {
                            getStack().getCacheLog().Error("Total._deliverBCast()", "arrived msgs " + _msgArrived + " passed :" + _msgAfterReset);
                        }
                        ++seqID;
                        start_time_bcast = 0;
                        time_left = opTimeout;
                        continue;
                    }
                    break;
                }
                time_left = opTimeout;
                start_time_bcast = 0;
                upTbl.remove((long) (seqID + 1));
                _msgAfterReset++;
                ++seqID;
            }
            HDR header = (HDR) msg.removeHeader(HeaderType.TOTAL);

            if (header.localSeqID != NULL_ID)
            {
 
                passUp(new Event(Event.MSG, msg));
                if (getStack().getCacheLog().getIsInfoEnabled())
                {
                    getStack().getCacheLog().Info("TOTAL._deliverBcast()", (new Long(seqID)).toString() + " hdr = " + Global.CollectionToString(msg.getHeaders()));
                }
            }

        }
        while (true);
       
    }

    /**
     * Extract as many messages as possible from the pending up queue and send them to the layer above
     */
    private void _deliverMcast()
    {
        long time_left = opTimeout;
        do
        {
            Message msg = null;
            synchronized (_mcastUpTbl)
            {
                msg = (Message) _mcastUpTbl.get((long) (_mcastSeqID + 1));
                if (_mcastUpTbl.size() > 0)
                {
                    if (getStack().getCacheLog().getIsInfoEnabled())
                    {
                        getStack().getCacheLog().Info("Total._deliverMcast()", "UP table [" + _mcastUpTbl.size() + "]");
                    }
                    if (getStack().getCacheLog().getIsInfoEnabled())
                    {
                        getStack().getCacheLog().Info("Total._deliverMcast()", "mcast_seq: " + (_mcastSeqID + 1));
                    }
                }

                if (msg == null)
                {
                    if (_mcastUpTbl.size() > 0)
                    {

                        if (start_time == 0)
                        {
                         
                            start_time = System.currentTimeMillis();
                        }

                       
                        time_left = opTimeout -(System.currentTimeMillis() - start_time);
                        
                        if (getStack().getCacheLog().getIsInfoEnabled())
                        {
                            getStack().getCacheLog().Info("Total._deliverMcast()", "timeout[" + (_mcastSeqID + 1) + "] ->" + time_left);
                        }
                    }

                    if (time_left <= 0)
                    {
                        if (getStack().getCacheLog().getIsErrorEnabled())
                        {
                            getStack().getCacheLog().Error("Total._deliverMcast()", "bypassed a missing message " + (_mcastSeqID + 1));
                        }
                        ++_mcastSeqID;
                        start_time = 0;
                        time_left = opTimeout;

                        continue;
                    }
                    break;
                }
                start_time = 0;
                time_left = opTimeout;

                _mcastUpTbl.remove((long) (_mcastSeqID + 1));
                ++_mcastSeqID;
            }
            HDR header = (HDR) msg.removeHeader(HeaderType.TOTAL);

            if (header.localSeqID != NULL_ID)
            {
 
                passUp(new Event(Event.MSG, msg));
                if (getStack().getCacheLog().getIsInfoEnabled())
                {
                    getStack().getCacheLog().Info("TOTAL._deliverMcast()", (new Long(_mcastSeqID)).toString() + " hdr = " + Global.CollectionToString(msg.getHeaders()));
                }
            }

        }
        while (true);
         
    }

    /**
     * Add all undelivered bcasts sent by this member in the req queue and then replay this queue
     */
    private void _replayBcast()
    {
        Message msg;
        HDR header;

        // i. Remove all undelivered bcasts sent by this member and place them
        // again in the pending bcast req queue

        if (getStack().getCacheLog().getIsInfoEnabled())
        {
            getStack().getCacheLog().Info("TOTAL._replayBcast()", "upTabl size = " + upTbl.size());
        }
        synchronized (upTbl)
        {
            if (upTbl.size() > 0)
            {
                if (getStack().getCacheLog().getIsInfoEnabled())
                {
                    getStack().getCacheLog().Info("Replaying undelivered bcasts");
                }
            }

            Iterator it = upTbl.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry ent = (Map.Entry)it.next();
                msg = (Message) ent.getValue();
                if (!msg.getSrc().equals(addr))
                {
                    if (getStack().getCacheLog().getIsInfoEnabled())
                    {
                        getStack().getCacheLog().Info("During replay: " + "discarding BCAST[" + ((TOTAL.HDR) msg.getHeader(HeaderType.TOTAL)).seqID + "] from " + _addrToString(msg.getSrc()));
                    }
                    continue;
                }

                header = (HDR) msg.removeHeader(HeaderType.TOTAL);
                if (header.localSeqID == NULL_ID)
                {
                    continue;
                }
                _sendBcastRequest(msg, header.localSeqID);
            }
            start_time_bcast = 0;
            upTbl.clear();
        }  
    }

    /**
     * Add all undelivered mcasts sent by this member in the req queue and then replay this queue
     */
    private void _replayMcast()
    {
        Message msg;
        HDR header;

        // i. Remove all undelivered bcasts sent by this member and place them
        // again in the pending bcast req queue

        if (getStack().getCacheLog().getIsInfoEnabled())
        {
            getStack().getCacheLog().Info("TOTAL._replayBcast()", "upTabl size = " + Integer.toString(_mcastUpTbl.size()));
        }
        synchronized (_mcastUpTbl)
        {
             
            start_time = 0;
            _mcastUpTbl.clear();
        }  
    }

    /**
     * Send a unicast message: Add a
     * <code>UCAST</code> header
     *
     *
     * @param msg the message to unicast
     *
     * @return the message to send
     *
     */
    private Message _sendUcast(Message msg)
    {
        msg.putHeader(HeaderType.TOTAL, new HDR(HDR.UCAST, NULL_ID, NULL_ID, -1));
        return (msg);
    }

    /**
     * Receive a unicast message: Remove the
     * <code>UCAST</code> header
     *
     *
     * @param msg the received unicast message
     *
     */
    private void _recvUcast(Message msg)
    {
        msg.removeHeader(HeaderType.TOTAL);
    }

    /**
     * Replace the original message with a broadcast request sent to the sequencer. The original bcast message is stored locally until a reply to bcast is received from the
     * sequencer. This function has the side-effect of increasing the
     * <code>localSeqID</code>
     *
     *
     * @param msg the message to broadcast
     *
     */
    private void _sendBcastRequest(Message msg)
    {
        long seqId = -1;
        synchronized (localSeqIDMutex)
        {
            seqId = ++localSeqID;
        }

        _sendBcastRequest(msg, seqId);
    }

    private void _sendMcastRequest(Message msg)
    {
        long seqId = -1;
        synchronized (localMcastSeqIDMutex)
        {
            seqId = ++_mcastLocalSeqID;
        }

        _sendMcastRequest(msg, seqId);
    }

    private Address getGroupSequencer(java.util.List dests)
    {
        Address groupSequencerAddr = null;
       readLock.lock();
        try
        {
            if (dests != null)
            {
                for (int i = 0; i < dests.size(); i++)
                {
                    String subGroupID = (String) ((this._mbrsSubgroupMap.get(dests.get(i)) instanceof String) ? this._mbrsSubgroupMap.get(dests.get(i)) : null);

                    if (subGroupID == null)
                    { //probably this member has left and view has been changed.
                        continue;
                    }

                    java.util.List groupMbrs = (java.util.List) (((java.util.List) this._sequencerTbl.get(subGroupID) instanceof java.util.List) ? (java.util.List) this._sequencerTbl.get(subGroupID) : null);
                    return groupSequencerAddr = (Address) ((groupMbrs.get(0) instanceof Address) ? groupMbrs.get(0) : null);
                }
            }
        }
        finally
        {
           readLock.unlock();
        }
        return groupSequencerAddr;
    }

    private void _sendMcastRequest(Message msg, long id)
    {
        // i. Store away the message while waiting for the sequencer's reply
        // ii. Send a mcast request immediatelly and also schedule a
        // retransmission
        Address groupSequencerAddr = addr;
        java.util.List dests = msg.getDests();


        groupSequencerAddr = getGroupSequencer(dests);

        if (groupSequencerAddr == null)
        {
            return;
        }

        if (addr.compareTo(groupSequencerAddr) == 0)
        {
            long seqid = getNextMCastSeqID();

            int viewId = -1;
            try
            {
                readLock.lock();
                viewId = (int) (currentViewId != null ? currentViewId.getId() : -1);
            }
            finally
            {
                readLock.unlock();
            }
            //Rent the event
            Event evt = null;
             
            evt = new Event();
            evt.setType(Event.MSG);
            evt.setPriority(msg.getPriority());
            evt.setArg(msg);

            //Rent the header
             
            HDR hdr = new HDR();
            hdr.type = HDR.MCAST;
            hdr.localSeqID = id;
            hdr.seqID = seqid;
            hdr.viewId = viewId;
            msg.setType(MsgType.SEQUENCED);

            msg.putHeader(HeaderType.TOTAL, hdr);

          
            //===================================================
            //now the message will contain a list of addrs in case of multicast.
            //=======================================================

            passDown(evt);
            if (getStack().getCacheLog().getIsInfoEnabled())
            {
                getStack().getCacheLog().Info("TOTAL._sendMcastRequest()", "shortcut mcast seq# " + seqid);
            }
            return;
        }
       
        requestWriteLock.lock();
        try
        {
            _mcastReqTbl.put((long) id, msg);
        }
        finally
        {
            requestWriteLock.unlock();
        }
        _transmitMcastRequest(id, groupSequencerAddr);
        _mcastRetransmitter.add(id, msg);
    }

    /**
     * Replace the original message with a broadcast request sent to the sequencer. The original bcast message is stored locally until a reply to bcast is received from the
     * sequencer
     *
     *
     * @param msg the message to broadcast
     *
     * @param id the local sequence ID to use
     *
     */
    private void _sendBcastRequest(Message msg, long id)
    {

        // i. Store away the message while waiting for the sequencer's reply
        // ii. Send a bcast request immediatelly and also schedule a
        // retransmission
        msg.setDest(null); //:FIX: To make sure that this message will be broadcasted.
        if (addr.compareTo(this.sequencerAddr) == 0)
        {
            long seqid = getNextSequenceID();
            int viewId = -1;
            try
            {
                readLock.lock();
                viewId = (int) (currentViewId != null ? currentViewId.getId() : -1);
            }
            finally
            {
               readLock.unlock();
            }
            //Rent the event
            Event evt = null;
            
            evt = new Event();
            evt.setType(Event.MSG);
            evt.setPriority(msg.getPriority());
            evt.setArg(msg);

            //Rent the header
            
            HDR hdr = new HDR();
            hdr.type = HDR.BCAST;
            hdr.localSeqID = id;
            hdr.seqID = seqid;
            hdr.viewId = viewId;
            msg.putHeader(HeaderType.TOTAL, hdr);

            msg.setDest(null);
            msg.setType(MsgType.SEQUENCED);
            passDown(evt);
            if (getStack().getCacheLog().getIsInfoEnabled())
            {
                getStack().getCacheLog().Info("TOTAL._sendBcastRequest()", "shortcut bcast seq# " + seqid);
            }
            return;
        }
       
        requestWriteLock.lock();
        try
        {
            reqTbl.put((long) id, msg);
        }
        finally
        {
            requestWriteLock.unlock();
        }
        _transmitBcastRequest(id);
        retransmitter.add(id, msg);
    }

    /**
     * Send the bcast request with the given localSeqID
     *
     *
     * @param seqID the local sequence id of the
     *
     */
    private void _transmitBcastRequest(long seqID)
    {
        Message reqMsg;

        // i. If NULL_STATE, then ignore, just transient state before
        // shutting down the retransmission thread
        // ii. If blocked, be patient - reschedule
        // iii. If the request is not pending any more, acknowledge it
        // iv. Create a broadcast request and send it to the sequencer

        if (state == NULL_STATE)
        {
            if (getStack().getCacheLog().getIsInfoEnabled())
            {
                getStack().getCacheLog().Info("Transmit BCAST_REQ[" + seqID + "] in NULL_STATE");
            }
            return;
        }
        if (state == BLOCK)
        {
            return;
        }

        
        requestReadLock.lock();
        try
        {
            if (!reqTbl.containsKey((long) seqID))
            {
                retransmitter.ack(seqID);
                return;
            }
        }
        finally
        {
            requestReadLock.unlock();
        }

         
        reqMsg = new Message();
        reqMsg.setDest(sequencerAddr);
        reqMsg.setSrc(addr);
        reqMsg.setBuffer(new byte[0]);
        
        HDR hdr = new HDR();
        hdr.type = HDR.REQ;
        hdr.localSeqID = seqID;
        hdr.seqID = NULL_ID;

        reqMsg.putHeader(HeaderType.TOTAL, hdr);
        reqMsg.setIsUserMsg(true);
        reqMsg.setType(MsgType.TOKEN_SEEKING);
     
        Event evt = new Event();
        evt.setType(Event.MSG);
        evt.setArg(reqMsg);

        passDown(evt);
    }

    /**
     * Send the mcast request with the given localSeqID
     *
     *
     * @param seqID the local sequence id of the
     *
     */
    private void _transmitMcastRequest(long seqID, Address groupSequencerAddr)
    {
        Message reqMsg;

        // i. If NULL_STATE, then ignore, just transient state before
        // shutting down the retransmission thread
        // ii. If blocked, be patient - reschedule
        // iii. If the request is not pending any more, acknowledge it
        // iv. Create a broadcast request and send it to the sequencer

        if (state == NULL_STATE)
        {
            if (getStack().getCacheLog().getIsInfoEnabled())
            {
                getStack().getCacheLog().Info("Transmit MCAST_REQ[" + seqID + "] in NULL_STATE");
            }
            return;
        }
        if (state == BLOCK)
        {
            return;
        }

       
       requestReadLock.lock();
        try
        {
            if (!_mcastReqTbl.containsKey((long) seqID))
            {
                _mcastRetransmitter.ack(seqID);
                return;
            }
        }
        finally
        {
            requestReadLock.unlock();
        }

         
        reqMsg = new Message();
        
        reqMsg.setDest(groupSequencerAddr);
        reqMsg.setSrc(addr);
        reqMsg.setBuffer(new byte[0]);
         
        HDR hdr = new HDR();
        hdr.type = HDR.REQMCAST;
        hdr.localSeqID = seqID;
        hdr.seqID = NULL_ID;



        reqMsg.putHeader(HeaderType.TOTAL, hdr);
        reqMsg.setIsUserMsg(true);
        reqMsg.setType(MsgType.TOKEN_SEEKING);

        
        Event evt = new Event();
        evt.setType(Event.MSG);
        evt.setArg(reqMsg);

        passDown(evt);
    }

    /**
     * Receive a broadcast message: Put it in the pending up queue and then try to deliver above as many messages as possible
     *
     *
     * @param msg the received broadcast message
     *
     */
    private void _recvBcast(Message msg)
    {
        HDR header = (HDR) msg.getHeader(HeaderType.TOTAL);

        // i. Put the message in the up pending queue only if it's not
        // already there, as it seems that the event may be received
        // multiple times before a view change when all members are
        // negotiating a common set of stable msgs
        //
        // ii. Deliver as many messages as possible
        int existingViewId = -1;
        try
        {
            readLock.lock();
            existingViewId = (int) (currentViewId != null ? currentViewId.getId() : -1);
        }
        finally
        {
            readLock.unlock();
        }
        synchronized (upTbl)
        {
            if (header.seqID <= seqID)
            {
                if (header.viewId > existingViewId)
                {
                    //this messages is of latest view therefore we put it into the table.
                    synchronized (_undeliveredMessages)
                    {
                        _undeliveredMessages.add(new Event(Event.MSG, msg, msg.getPriority()));
                    }
                    return;
                }
                getStack().getCacheLog().CriticalInfo("TOTAL._recvBcast", header.seqID + " is already consumed");
                return;
            }
            else
            {
                if (header.viewId < existingViewId)
                {
                    //this messages is of an old view therefore we discard it
                    return;
                }
            }
            upTbl.put((long) header.seqID, msg);
        }
        
        _deliverBcast();
    }

    /**
     * Receive a multicast message: Put it in the pending up queue and then try to deliver above as many messages as possible
     *
     *
     * @param msg the received broadcast message
     *
     */
    private void _recvMcast(Message msg)
    {
        HDR header = (HDR) msg.getHeader(HeaderType.TOTAL);

        // i. Put the message in the up pending queue only if it's not
        // already there, as it seems that the event may be received
        // multiple times before a view change when all members are
        // negotiating a common set of stable msgs
        //
        // ii. Deliver as many messages as possible
        int existingViewId = -1;
        try
        {
            readLock.lock();
            existingViewId = (int) (currentViewId != null ? currentViewId.getId() : -1);
        }
        finally
        {
            readLock.unlock();
        }
        synchronized (_mcastUpTbl)
        {
            if (header.seqID <= _mcastSeqID)
            {
                if (header.viewId > existingViewId)
                {
                    //this messages is of latest view therefore we put it into the table.
                    
                    synchronized (_undeliveredMessages)
                    {
                        _undeliveredMessages.add(new Event(Event.MSG, msg, msg.getPriority()));
                    }
                    return;
                }
                if (getStack().getCacheLog().getIsErrorEnabled())
                {
                    getStack().getCacheLog().Error("TOTAL._recvMcast", header.seqID + " is already consumed");
                }
                return;
            }
            else
            {
                if (header.viewId < existingViewId)
                {
                    //this messages is of an old view therefore we discard it
                    return;
                }
            }
            _mcastUpTbl.put((long) header.seqID, msg);
        }

        if (getStack().getCacheLog().getIsInfoEnabled())
        {
            getStack().getCacheLog().Info("muds: delivering mcast a message with seq : " + header.seqID);
        }
        _deliverMcast();
    }

    /**
     * Received a bcast request - Ignore if not the sequencer, else send a bcast reply
     *
     *
     * @param msg the broadcast request message
     *
     */
    private void _recvBcastRequest(Message msg)
    {
        HDR header;
        Message repMsg;

        // i. If blocked, discard the bcast request
        // ii. Assign a seqID to the message and send it back to the requestor

        if (getStack().getCacheLog().getIsInfoEnabled())
        {
            getStack().getCacheLog().Info("TOTAL._recvBcastRequest()", "hdr = " + Global.CollectionToString(msg.getHeaders()));
        }

        if (!addr.equals(sequencerAddr))
        {
            getStack().getCacheLog().Error("Received bcast request " + "but not a sequencer");
            return;
        }
        if (state == BLOCK)
        {
            if (getStack().getCacheLog().getIsInfoEnabled())
            {
                getStack().getCacheLog().Info("Blocked, discard bcast req");
            }
            return;
        }

        header = (HDR) msg.getHeader(HeaderType.TOTAL);
        repMsg = new Message(msg.getSrc(), addr, new byte[0]);
        repMsg.setPriority(msg.getPriority());
        int viewId = -1;
        try
        {
            readLock.lock();
            viewId = (int) (currentViewId != null ? currentViewId.getId() : -1);
        }
        finally
        {
           readLock.unlock();
        }

        HDR rspHdr = new HDR(HDR.REP, header.localSeqID, getNextSequenceID(), viewId);
        repMsg.putHeader(HeaderType.TOTAL, rspHdr);
        repMsg.setIsUserMsg(true);
        repMsg.setType(MsgType.TOKEN_SEEKING);

        passDown(new Event(Event.MSG, repMsg));
    }

    /**
     * Received an mcast request - Ignore if not the sequencer, else send an mcast reply
     *
     *
     * @param msg the multicast request message
     *
     */
    private void _recvMcastRequest(Message msg)
    {
        HDR header;
        Message repMsg;

        // i. If blocked, discard the mcast request
        // ii. Assign a seqID to the message and send it back to the requestor

        if (getStack().getCacheLog().getIsInfoEnabled())
        {
            getStack().getCacheLog().Info("TOTAL._recvMcastRequest()", "hdr = " + Global.CollectionToString(msg.getHeaders()));
        }

        if (!addr.equals(_groupSequencerAddr))
        {
            getStack().getCacheLog().Error("Received mcast request from " + msg.getSrc().toString() + " but not a group sequencer");
            return;
        }
        if (state == BLOCK)
        {
            if (getStack().getCacheLog().getIsInfoEnabled())
            {
                getStack().getCacheLog().Info("Blocked, discard mcast req");
            }
            return;
        }

        header = (HDR) msg.getHeader(HeaderType.TOTAL);
        repMsg = new Message(msg.getSrc(), addr, new byte[0]);
        repMsg.setPriority(msg.getPriority());
        int viewId = -1;
        try
        {
            readLock.lock();
            viewId = (int) (currentViewId != null ? currentViewId.getId() : -1);
        }
        finally
        {
           readLock.unlock();
        }

        HDR reqHdr = new HDR(HDR.REPMCAST, header.localSeqID, getNextMCastSeqID(), viewId);
        repMsg.putHeader(HeaderType.TOTAL, reqHdr);
        repMsg.setIsUserMsg(true);
        repMsg.setType(MsgType.TOKEN_SEEKING);

        passDown(new Event(Event.MSG, repMsg));
    }

    /**
     * Received a bcast reply - Match with the pending bcast request and move the message in the list of messages to be delivered above
     *
     *
     * @param header the header of the bcast reply
     *
     */
    private void _recvBcastReply(HDR header, Message rspMsg)
    {
        Message msg;
        long id;

        // i. If blocked, discard the bcast reply
        //
        // ii. Assign the received seqID to the message and broadcast it
        //
        // iii.
        // - Acknowledge the message to the retransmitter
        // - If non-existent BCAST_REQ, send a fake bcast to avoid seqID gaps
        // - If localID == NULL_ID, it's a null BCAST, else normal BCAST
        // - Set the seq ID of the message to the one sent by the sequencer

        if (state == BLOCK)
        {
            if (getStack().getCacheLog().getIsInfoEnabled())
            {
                getStack().getCacheLog().Info("Blocked, discard bcast rep");
            }
            return;
        }
        
        requestWriteLock.lock();
        try
        {
            Object tempObject = reqTbl.get((long) header.localSeqID);
            reqTbl.remove((long) header.localSeqID);
            msg = (Message) tempObject;
        }
        finally
        {
            requestWriteLock.unlock();
        }
        if (msg != null)
        {
            retransmitter.ack(header.localSeqID);
            id = header.localSeqID;
        }
        else
        {
            if (getStack().getCacheLog().getIsInfoEnabled())
            {
                getStack().getCacheLog().Info("Bcast reply to " + "non-existent BCAST_REQ[" + header.localSeqID + "], Sending NULL bcast");
            }
            id = NULL_ID;
            msg = new Message(null, addr, new byte[0]);
            msg.setIsUserMsg(true);
        }


        //Rent the header
       
        HDR hdr = new HDR();
        hdr.type = HDR.BCAST;
        hdr.localSeqID = id;
        hdr.seqID = header.seqID;
        hdr.viewId = header.viewId;

        msg.putHeader(HeaderType.TOTAL, hdr);
        msg.setIsUserMsg(true);
        msg.setType(MsgType.SEQUENCED);
       
        Event evt = new Event();
        evt.setType(Event.MSG);
        evt.setArg(msg);
        evt.setPriority(msg.getPriority());
        msg.setDest(null);
        msg.setDests(null);

        passDown(evt);
    }

    /**
     * Received an mcast reply - Match with the pending mcast request and move the message in the list of messages to be delivered above
     *
     *
     * @param header the header of the bcast reply
     *
     */
    private void _recvMcastReply(HDR header, Address subgroupCoordinator)
    {
        Message msg;
        long id;

        // i. If blocked, discard the mcast reply
        //
        // ii. Assign the received seqID to the message and multicast it
        //
        // iii.
        // - Acknowledge the message to the retransmitter
        // - If non-existent MCAST_REQ, send a fake mcast to avoid seqID gaps
        // - If localID == NULL_ID, it's a null MCAST, else normal MCAST
        // - Set the seq ID of the message to the one sent by the group sequencer

        if (state == BLOCK)
        {
            if (getStack().getCacheLog().getIsInfoEnabled())
            {
                getStack().getCacheLog().Info("Blocked, discard mcast rep");
            }
            return;
        }
        
        requestWriteLock.lock();
        try
        {
            Object tempObject = _mcastReqTbl.get((long) header.localSeqID);
            _mcastReqTbl.remove((long) header.localSeqID);
            msg = (Message) tempObject;

        }
        finally
        {
            requestWriteLock.unlock();
        }

        if (msg != null)
        {
            _mcastRetransmitter.ack(header.localSeqID);
            id = header.localSeqID;
        }
        else
        {
            if (getStack().getCacheLog().getIsInfoEnabled())
            {
                getStack().getCacheLog().Info("Mcast reply to " + "non-existent MCAST_REQ[" + header.localSeqID + "], Sending NULL mcast");
            }
            id = NULL_ID;
            msg = new Message(null, addr, new byte[0]);
            msg.setIsUserMsg(true);

            String subGroupID = (String) ((this._mbrsSubgroupMap.get(subgroupCoordinator) instanceof String) ? this._mbrsSubgroupMap.get(subgroupCoordinator) : null);
            if (subGroupID != null)
            {
                java.util.ArrayList groupMbrs = (java.util.ArrayList) (((java.util.ArrayList) this._sequencerTbl.get(subGroupID) instanceof java.util.ArrayList) ? (java.util.ArrayList) this._sequencerTbl.get(subGroupID) : null);
                if (groupMbrs != null && groupMbrs.size() > 0)
                {
                    Object tempVar = groupMbrs.clone();
                    msg.setDests((java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null));
                }

                if (msg.getDests() == null)
                {
                    if (getStack().getCacheLog().getIsInfoEnabled())
                    {
                        getStack().getCacheLog().Info('[' + subGroupID + "]destination list is empty");
                    }
                }

            }
            else
            {
                return;
            }
        }


        
        HDR hdr = new HDR();
        hdr.type = HDR.MCAST;
        hdr.localSeqID = id;
        hdr.seqID = header.seqID;
        hdr.viewId = header.viewId;

        if (getStack().getCacheLog().getIsInfoEnabled())
        {
            getStack().getCacheLog().Info("TOTAL._recvMcastReply()", id + " hdr = " + Global.CollectionToString(msg.getHeaders()));
        }
        msg.putHeader(HeaderType.TOTAL, hdr);
        msg.setIsUserMsg(true);
        msg.setType(MsgType.SEQUENCED);
         
        Event evt = new Event();
        evt.setType(Event.MSG);
        evt.setArg(msg);
        evt.setPriority(msg.getPriority());

        passDown(evt);
    }

    /**
     * Resend the bcast request with the given localSeqID
     *
     *
     * @param seqID the local sequence id of the
     *
     */
    private void _retransmitBcastRequest(long seqID)
    {
        // *** Get a shared lock
//        try
//        {
           readLock.lock();
            try
            {

                if (getStack().getCacheLog().getIsInfoEnabled())
                {
                    getStack().getCacheLog().Info("TOTAL._retransmitBcastRequest()", "Retransmit BCAST_REQ[" + seqID + ']');
                }
                _transmitBcastRequest(seqID);

                // ** Revoke the shared lock
            }
            finally
            {
                readLock.unlock();
            }
 
    }

    /**
     * Resend the mcast request with the given localSeqID
     *
     *
     * @param seqID the local sequence id of the
     *
     */
    private void _retransmitMcastRequest(long seqID, Address groupSequencerAddr)
    {
        // *** Get a shared lock
 
            readLock.lock();
            try
            {

                if (getStack().getCacheLog().getIsInfoEnabled())
                {
                    getStack().getCacheLog().Info("TOTAL._retransmitMcastRequest()", "Retransmit MCAST_REQ[" + seqID + "] to : " + groupSequencerAddr.toString());
                }
                _transmitMcastRequest(seqID, groupSequencerAddr);

                // ** Revoke the shared lock
            }
            finally
            {
                readLock.unlock();
            }
 
    }


    /*
     * Up event handlers If the return value is true the event travels further up the stack else it won't be forwarded
     */
    /**
     * Prepare for a VIEW_CHANGE: switch to flushing state
     *
     *
     * @return true if the event is to be forwarded further up
     *
     */
    private boolean _upBlock()
    {
        // *** Get an exclusive lock
 
            writeLock.lock();
            try
            {

                state = FLUSH;

                // *** Revoke the exclusive lock
            }
            finally
            {
               writeLock.unlock();
            }
 

        return (true);
    }

    /**
     * Handle an up MSG event
     *
     *
     * @param event the MSG event
     *
     * @return true if the event is to be forwarded further up
     *
     */
    private boolean _upMsg(Event evt)
    {
        Message msg;
        Object obj;
        HDR header;

        // *** Get a shared lock
 
            try
            {
                msg = (Message) evt.getArg();

                // If NULL_STATE, shouldn't receive any msg on the up queue!
                if (state == NULL_STATE)
                {
                    readLock.lock();
                    try
                    {
                        String hdrToSting = msg != null ? Global.CollectionToString(msg.getHeaders()) : " Null header";
                        if (getStack().getCacheLog().getIsInfoEnabled())
                        {
                            getStack().getCacheLog().Info("TOTAL._upMsg" , "Up msg in NULL_STATE " + hdrToSting);
                        }
                        if (state == NULL_STATE)
                        {
                            synchronized (_undeliveredMessages)
                            {
                                _undeliveredMessages.add(evt);
                                return (false);
                            }
                        }
                    }
                    finally
                    {
                        readLock.unlock();
                    }
                }

                // Peek the header:
                //
                // (UCAST) A unicast message - Send up the stack
                // (BCAST) A broadcast message - Handle specially
                // (REQ) A broadcast request - Handle specially
                // (REP) A broadcast reply from the sequencer - Handle specially
                if (!((obj = msg.getHeader(HeaderType.TOTAL)) instanceof TOTAL.HDR))
                {
                     
                }
                else
                {
                    header = (HDR) obj;

                    switch (header.type)
                    {

                        case HDR.UCAST:
                            _recvUcast(msg);
                           
                            return (true);

                        case HDR.BCAST:
                            _recvBcast(msg);
                            
                            return (false);

                        case HDR.MCAST:
                            if (getStack().getCacheLog().getIsInfoEnabled())
                            {
                                getStack().getCacheLog().Info("muds: a command for mcast from " + msg.getSrc() + " to me[" + addr + "],  local-seq : " + header.localSeqID
                                        + " seq : " + header.seqID);
                            }
                            _recvMcast(msg);
                            return (false);

                        case HDR.REQ:
                            _recvBcastRequest(msg);
                           
                            return (false);

                        case HDR.REP:
                            _recvBcastReply(header, msg);
                            
                            return (false);

                        case HDR.REQMCAST:
                            if (getStack().getCacheLog().getIsInfoEnabled())
                            {
                                getStack().getCacheLog().Info("muds: recieved mcast request " + " local-seq : " + header.localSeqID + " seq : " + header.seqID);
                            }
                            _recvMcastRequest(msg);
                            return (false);

                        case HDR.REPMCAST:
                            _recvMcastReply(header, msg.getSrc());
                            return (false);

                        default:
                            getStack().getCacheLog().Error("Unknown header type");
                            return (false);

                    }
                }
                // ** Revoke the shared lock
            }
            finally
            {
                
            }
 

        return (true);
    }

    /**
     * Delivers the pending messages which were queued when stat was NULL_STATE.
     */
    private void deliverPendingMessages()
    {
        try
        {
             
            synchronized (_undeliveredMessages)
            {
                if (_undeliveredMessages.size() > 0)
                {
                    Object tempVar = GenericCopier.DeepCopy(_undeliveredMessages);//.clone();
                    final java.util.ArrayList clone = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
                    //TODO:: Currently we don't have substitue of aysnc method calls on threads.
                    //TODO: TODO: : chaning wring method call to Even since it supports callbacks plus calls method async
                    NEvent deliverPending = new NEvent("deliverPending", this.getStack().getCacheLog());
                    deliverPending.addNEventListners(new NEventStart()
                    {
                        @Override
                        public Object hanleEvent(Object... obj) throws SocketException, Exception
                        {
                            deliverPendingMessagesAsync(clone);
                            return null;
                        }
                    },
                            null);

                     
                    _undeliveredMessages.clear();
                    deliverPending.fireEvents(false, clone);
                }
            }
        }
        catch (Exception e)
        {
        }
    }

    private void deliverPendingMessagesAsync(Object msgs)
    {
        java.util.ArrayList pendingMessages = (java.util.ArrayList) ((msgs instanceof java.util.ArrayList) ? msgs : null);
        if (pendingMessages != null)
        {
            
            for (int i = 0; i < pendingMessages.size(); i++)
            {
                try
                {
                    up((Event) ((pendingMessages.get(i) instanceof Event) ? pendingMessages.get(i) : null));
                }
                catch (Exception e)
                {
                }
            }
        }
    }

    /**
     * Set the address of this group member
     *
     *
     * @param event the SET_LOCAL_ADDRESS event
     *
     * @return true if event should be forwarded further up
     *
     */
    private boolean _upSetLocalAddress(Event evt)
    {
        // *** Get an exclusive lock
//        try
//        {
            writeLock.lock();
            try
            {

                addr = (Address) evt.getArg();

                // *** Revoke the exclusive lock
            }
            finally
            {
               writeLock.unlock();
            }
 
        return (true);
    }

    /**
     * Handle view changes
     *
     * param event the VIEW_CHANGE event
     *
     * @return true if the event should be forwarded to the layer above
     *
     */
    private boolean _upViewChange(Event evt)
    {
        Object oldSequencerAddr;
        if (getStack().getCacheLog().getIsInfoEnabled())
        {
            getStack().getCacheLog().Info("Total._upViewChange()", "received VIEW_CHANGE");
        }
 
            writeLock.lock();
            try
            {

                state = RUN;

              
                oldSequencerAddr = sequencerAddr;
                sequencerAddr = (Address) ((View) evt.getArg()).getMembers().get(0);

                currentViewId = ((View) evt.getArg()).getVid().Copy();
           
                Object tempVar = ((View) evt.getArg()).getSequencerTbl().clone();
                this._sequencerTbl = (java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null);
                Object tempVar2 = ((View) evt.getArg()).getMbrsSubgroupMap().clone();
                this._mbrsSubgroupMap = (java.util.HashMap) ((tempVar2 instanceof java.util.HashMap) ? tempVar2 : null);
                if (getStack().getCacheLog().getIsInfoEnabled())
                {
                    getStack().getCacheLog().Info("Total._upViewChange()", "this._sequencerTbl.count = " + this._sequencerTbl.size());
                }
                if (getStack().getCacheLog().getIsInfoEnabled())
                {
                    getStack().getCacheLog().Info("Total._upViewChange()", "this._mbrsSubgroupMap.count = " + this._mbrsSubgroupMap.size());
                }

                java.util.List groupMbrs = (java.util.List) _sequencerTbl.get(subGroup_addr);
                if (groupMbrs != null)
                {
                    if (groupMbrs.size() != 0)
                    {
                        this._groupSequencerAddr = (Address) ((groupMbrs.get(0) instanceof Address) ? groupMbrs.get(0) : null);
                    }
                }

                if (getStack().getCacheLog().getIsInfoEnabled())
                {
                    getStack().getCacheLog().Info("Total._upViewChange()", subGroup_addr + " old mbrs count = " + _groupMbrsCount);
                }
                int newCount = groupMbrs.size();
                if (getStack().getCacheLog().getIsInfoEnabled())
                {
                    getStack().getCacheLog().Info("Total._upViewChange()", "new mbrs count = " + newCount);
                }
                if (newCount != _groupMbrsCount)
                {
                    // i.	the group this member belongs to, has changed
                    // ii.	therefore reset the  _mcastSeqID
                    // iii.	if this node is new group sequencer, reset the
                    //		_mcastSequencerSeqID.
                    // iii. Reset the last received mcast sequence ID
                    // iv.	Replay undelivered mcasts

                    if (addr.equals(_groupSequencerAddr))
                    {
                        _mcastSequencerSeqID = NULL_ID;
                        if (getStack().getCacheLog().getIsInfoEnabled())
                        {
                            getStack().getCacheLog().Info("Total._upViewChange()", "resetting _mcastSequencerSeqID");
                        }
                    }

                    _mcastSeqID = NULL_ID;
                    if (getStack().getCacheLog().getIsInfoEnabled())
                    {
                        getStack().getCacheLog().Info("Total._upViewChange()", "resetting _mcastSeqID");
                    }
                    _groupMbrsCount = newCount;

                    _replayMcast();
                }

                if (getStack().getCacheLog().getIsInfoEnabled())
                {
                    getStack().getCacheLog().Info("Total._upViewChange", "my group sequencer is " + this._groupSequencerAddr.toString());
                }
                //============================================

                if (addr.equals(sequencerAddr))
                {
                    sequencerSeqID = NULL_ID;
                    if ((oldSequencerAddr == null) || (!addr.equals(oldSequencerAddr)))
                    {
                        if (getStack().getCacheLog().getIsInfoEnabled())
                        {
                            getStack().getCacheLog().Info("TOTAL,_upViewChange","I'm the new sequencer");
                        }
                    }

                }
                synchronized (upTbl)
                {
                    seqID = NULL_ID;
                }
                _replayBcast();

                if (getStack().getCacheLog().getIsInfoEnabled())
                {
                    getStack().getCacheLog().Info("Total.upViewChange()", "VIEW_CHANGE_OK");
                }
                Event viewEvt = new Event(Event.VIEW_CHANGE_OK, null, Priority.Critical);
                passDown(viewEvt);

                // *** Revoke the exclusive lock
            }
            finally
            {
               writeLock.unlock();
            }
 
        return (true);
    }
    private int _seqResetRequestCount = 0;
    private int _msgAfterReset = 0;
    private int _msgArrived = 0;

    private boolean _upResetSequence(Event evt)
    {
        _seqResetRequestCount++;
        Object tempVar = evt.getArg();
        ViewId vid = (ViewId) ((tempVar instanceof ViewId) ? tempVar : null);
        if (getStack().getCacheLog().getIsInfoEnabled())
        {
            getStack().getCacheLog().Info("Total._upResetSequence()", "Sequence reset request received :" + _seqResetRequestCount);
        }
        // *** Get an exclusive lock
 
            writeLock.lock();
            try
            {
                currentViewId = vid.Copy();
                _msgAfterReset = 0;
                _msgArrived = 0;
                state = RUN;
                synchronized (_mcastSeqIDMutex)
                {
                    _mcastSequencerSeqID = NULL_ID;
                }
                synchronized (_mcastUpTbl)
                {
                    _mcastSeqID = NULL_ID;
                }
                _replayMcast();

                synchronized (seqIDMutex)
                {
                    sequencerSeqID = NULL_ID;
                }
                synchronized (upTbl)
                {
                    seqID = NULL_ID;
                }
                _replayBcast();
            }
            finally
            {
                writeLock.unlock();
            }
 ;
        return (true);
    }


    /*
     * Down event handlers If the return value is true the event travels further down the stack else it won't be forwarded
     */
    /**
     * Blocking confirmed - No messages should come from above until a VIEW_CHANGE event is received. Switch to blocking state.
     *
     *
     * @return true if event should travel further down
     *
     */
    private boolean _downBlockOk()
    {
        // *** Get an exclusive lock
        
            writeLock.lock();
            try
            {

                state = BLOCK;

                // *** Revoke the exclusive lock
            }
            finally
            {
                writeLock.unlock();
            }
  

        return (true);
    }

    /**
     * A MSG event travelling down the stack. Forward unicast messages, treat specially the broadcast messages.<br>
     *
     * If in
     * <code>BLOCK</code> state, i.e. it has replied to a
     * <code>BLOCk_OK</code> and hasn't yet received a
     * <code>VIEW_CHANGE</code> event, messages are discarded<br>
     *
     * If in
     * <code>FLUSH</code> state, forward unicast but queue broadcasts
     *
     *
     * @param event the MSG event
     *
     * @return true if event should travel further down
     *
     */
    private boolean _downMsg(Event evt)
    {
        Message msg;

        // *** Get a shared lock
 
            try
            {

                // i. Discard all msgs, if in NULL_STATE
                // ii. Discard all msgs, if blocked
                if (state == NULL_STATE)
                {
                    getStack().getCacheLog().Error("TOTAL._downMsg()", "Discard msg in NULL_STATE");
                    
                    return (false);
                }
                if (state == BLOCK)
                {
                    getStack().getCacheLog().Error("TOTAL._downMsg()", "Blocked, discard msg");
                     
                    return (false);
                }

                msg = (Message) evt.getArg();
                msg.setPriority(evt.getPriority());

                if (msg.getIsSeqRequired())
                {
                    if (msg.getDest() != null || msg.getDests() != null)
                    {
                        //if it is a unicast msg with a single destination.
                        if (msg.getDests() == null)
                        {
                            
                            evt.setArg(msg);
                        }
                        // if it is a multicast msg with multiple destinations.
                        else
                        {
                            _sendMcastRequest(msg);
                            return (false);
                        }
                    }
                    else
                    { //its a broadcast msg.
                        _sendBcastRequest(msg);
                        return (false);
                    }
                }
                else
                {
                    return true;
                }

                // ** Revoke the shared lock
            }
            finally
            {
                //stateLock.ReleaseReaderLock();
            }
 
        return (true);
    }

    /**
     * Prepare this layer to receive messages from above
     */
    @Override
    public void start()
    {
        TimeScheduler timer;

        // Incase of TCP stack we'll get a reference to TCP, which is the transport
        // protocol in our case. For udp stack we'll fail.
        transport = getStack().findProtocol("TCP");

 

        //HashMap is synced by default x4
        reqTbl = new java.util.HashMap();
        upTbl = new java.util.HashMap();

        //======================================================
        _mcastReqTbl = new java.util.HashMap();
        _mcastUpTbl = new java.util.HashMap();
        //======================================================

        //NewTrace nTrace = stack.nTrace;
        retransmitter = new AckSenderWindow(new Command(this), AVG_RETRANSMIT_INTERVAL, stack.getCacheLog());
        _mcastRetransmitter = new AckSenderWindow(new MCastCommand(this), AVG_MCAST_RETRANSMIT_INTERVAL, stack.getCacheLog());
    }

    /**
     * Handle the stop() method travelling down the stack. <p> The local addr is set to null, since after a Start->Stop->Start sequence this member's addr is not guaranteed to be
     * the same
     *
     */
    @Override
    public void stop()
    {
        // *** Get an exclusive lock
 
            writeLock.lock();
            try
            {
                getStack().getCacheLog().DevTrace("TOTAL.stop()", "stopping TOTAL protocol OMG");
                state = NULL_STATE;
                retransmitter.reset();
                _mcastRetransmitter.reset();
                reqTbl.clear();
                upTbl.clear();
                addr = null;


                // *** Revoke the exclusive lock
            }
            finally
            {
                writeLock.unlock();
                transport = null;
            }
 
    }

    /**
     * Process an event coming from the layer below
     *
     *
     * @param event the event to process
     *
     */
    private void _up(Event evt)
    {
        switch (evt.getType())
        {

            case Event.BLOCK:
                if (!_upBlock())
                {
                    return;
                }
                break;

            case Event.MSG:
                if (!_upMsg(evt))
                {
                    
                    return;
                }
                break;

            case Event.SET_LOCAL_ADDRESS:
                if (!_upSetLocalAddress(evt))
                {
                    return;
                }
                break;

            case Event.VIEW_CHANGE:
                if (!_upViewChange(evt))
                {
                    return;
                }
                deliverPendingMessages();
                break;

            case Event.RESET_SEQUENCE:
                _upResetSequence(evt);
                deliverPendingMessages();
                break;

            default:
                break;

        }

        passUp(evt);

    }

    /**
     * Process an event coming from the layer above
     *
     *
     * @param event the event to process
     *
     */
    private void _down(Event evt)
    {
        switch (evt.getType())
        {

            case Event.BLOCK_OK:
                if (!_downBlockOk())
                {
                    return;
                }
                break;

            case Event.MSG:
                if (!_downMsg(evt))
                {
                    
                    return;
                }
                break;

            case Event.CONNECT:
                Object[] addrs = ((Object[]) evt.getArg());
                subGroup_addr = (String) addrs[1];
                passDown(evt);
                break;

            default:
                break;

        }

        passDown(evt);
    }

  
    /**
     * Create the TOTAL layer
     */
    public TOTAL()
    {
    }
    // javadoc inherited from superclass

    @Override
    public boolean setProperties(java.util.HashMap properties) throws Exception
    {
        return (_setProperties(properties));
    }
    // javadoc inherited from superclass

    @Override
    public java.util.List requiredDownServices()
    {
        return (_requiredDownServices());
    }
    // javadoc inherited from superclass

    @Override
    public java.util.List requiredUpServices()
    {
        return (_requiredUpServices());
    }
    // javadoc inherited from superclass

    @Override
    public void up(Event evt)
    {
        _up(evt);
    }
    // javadoc inherited from superclass

    @Override
    public void down(Event evt)
    {
        _down(evt);
    }
}
