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

import com.alachisoft.tayzgrid.cluster.Event;
import com.alachisoft.tayzgrid.cluster.Global;
import com.alachisoft.tayzgrid.cluster.Header;
import com.alachisoft.tayzgrid.cluster.HeaderType;
import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.cluster.MsgType;
import com.alachisoft.tayzgrid.cluster.View;
import com.alachisoft.tayzgrid.cluster.blocks.ConnectionTable;
import com.alachisoft.tayzgrid.cluster.blocks.ExtSocketException;
import com.alachisoft.tayzgrid.cluster.protocols.pbcast.GMS;
import com.alachisoft.tayzgrid.cluster.stack.Protocol;
import com.alachisoft.tayzgrid.cluster.util.BoundedList;
import com.alachisoft.tayzgrid.cluster.util.Util;

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.GenericCopier;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.enums.Priority;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.stats.HPTimeStats;
import com.alachisoft.tayzgrid.common.stats.TimeStats;
import com.alachisoft.tayzgrid.common.threading.AsyncProcessor;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

// $Id: TOTAL.java,v 1.6 2004/07/05 14:17:16 belaban Exp $
/**
 * TCP based protocol. Creates a server socket, which gives us the local address
 * of this group member. For each accept() on the server socket, a new thread is
 * created that listens on the socket. For each outgoing message m, if m.dest is
 * in the ougoing hashtable, the associated socket will be reused to send
 * message, otherwise a new socket is created and put in the hashtable. When a
 * socket connection breaks or a member is removed from the group, the
 * corresponding items in the incoming and outgoing hashtables will be removed
 * as well.<br> This functionality is in ConnectionTable, which is used by TCP.
 * TCP sends messages using ct.send() and registers with the connection table to
 * receive all incoming messages.
 *
 * <author> Bela Ban </author>
 */
public class TCP extends Protocol implements ConnectionTable.Receiver, ConnectionTable.ConnectionListener {
    

    @Override
    public String getName() {
        return "TCP";
    }

    /**
     * If the sender is null, set our own address. We cannot just go ahead and
     * set the address anyway, as we might be sending a message on behalf of
     * someone else ! E.g. in case of retransmission, when the original sender
     * has crashed, or in a FLUSH protocol when we have to return all unstable
     * messages with the FLUSH_OK response.
     */
    private void setSourceAddress(Message value) {
        if (value.getSrc() == null) {
            value.setSrc(local_addr);
        }
    }
    private ConnectionTable ct = null;
    private Address local_addr = null;
    private String group_addr = null;
    private String subGroup_addr = null;

    private InetAddress bind_addr1 = null; // local IP address to bind srv sock to (m-homed systems)
    private InetAddress bind_addr2 = null; // local IP address to bind srv sock to (m-homed systems)

    private int start_port = 7800; // find first available port starting at this port

    private int port1 = 0; // find first available port starting at this port
    private int port2 = 0; // find first available port starting at this port

    private java.util.List members = Collections.synchronizedList(new java.util.ArrayList(11));

    private int port_range = 1;
    private long reaper_interval = 0; // time in msecs between connection reaps
    private long conn_expire_time = 0; // max time a conn can be idle before being reaped
    private int _retries;
    private int _retryInterval;
    public boolean isStarting = true;

    public boolean loopback = true; // loops back msgs to self if true
    public boolean isClosing = false;
    /**
     * If set it will be added to <tt>local_addr</tt>. Used to implement for
     * example transport independent addresses
     */

    public byte[] additional_data = null;
    /**
     * List the maintains the currently suspected members. This is used so we
     * don't send too many SUSPECT events up the stack (one per message !)
     */
    public BoundedList suspected_mbrs = new BoundedList(20);

    /**
     * Should we drop unicast messages to suspected members or not
     */
    public boolean skip_suspected_members = false;

    public int recv_buf_size = 20000000;
    public int send_buf_size = 640000;
    public AsyncProcessor _asyncProcessor;
    /**
     * Used to shortcircuit transactional messages from management messages.
     */
    private Protocol upper;
    public TimeStats stats = new TimeStats(1);
    private java.util.HashMap syncTable = new java.util.HashMap();
    private ReentrantReadWriteLock lock_members = new ReentrantReadWriteLock();

    private ConnectionKeepAlive _keepAlive;

    private java.util.ArrayList asyncThreads = new java.util.ArrayList();
    private boolean asyncPassup = false;
    private Object async_mutex = new Object();

    private int _heartBeatInterval = 32000;
    private boolean _useKeepAlive = true;

    private boolean synchronizeConnections = true;
    public DownHandler _unicastDownHandler;
    public DownHandler _multicastDownHandler;
    public DownHandler _tokenSeekingMsgDownHandler;
    public com.alachisoft.tayzgrid.common.datastructures.Queue _unicastDownQueue;
    public com.alachisoft.tayzgrid.common.datastructures.Queue _multicastDownQueue;
    public com.alachisoft.tayzgrid.common.datastructures.Queue _tokenSeekingMsgDownQueue;
    public UpHandler _tokenSeekingUpHandler;
    public UpHandler _sequencedMsgUpHandler;
    public UpHandler _sequencelessMsgUpHandler;
    public com.alachisoft.tayzgrid.common.datastructures.Queue _sequencelessMsgUpQueue;
    public com.alachisoft.tayzgrid.common.datastructures.Queue _sequenecedMsgUpQueue;
    public com.alachisoft.tayzgrid.common.datastructures.Queue _tokenMsgUpQueue;
    private java.util.ArrayList _nodeRejoiningList;
    private HPTimeStats time;
    private HPTimeStats loopTime;
    private HPTimeStats _unicastSendTimeStats;
    private HPTimeStats _multicastSendTimeStats;
    private HPTimeStats _totalToTcpDownStats;
    private boolean _leaving;
    private boolean isInproc;
    
    public TCP() {
        time = new HPTimeStats();
        loopTime = new HPTimeStats();
        _totalToTcpDownStats = new HPTimeStats();
    }

    @Override
    public String toString() {
        return "Protocol TCP(local address: " + local_addr + ')';
    }

    @Override
    public void receiveDownEvent(Event evt) {
        int type = evt.getType();

        if (evt.getType() == Event.I_AM_LEAVING) {
            //Set leaving flag to true
            _leaving = true;
            return;
        }

        if (_unicastDownHandler == null) {
            if (type == Event.ACK || type == Event.START || type == Event.STOP) {
                if (handleSpecialDownEvent(evt) == false) {
                    return;
                }
            }

            if (evt.getType() == Event.HAS_STARTED) {
                HasStarted();
                return;
            }

            if (_printMsgHdrs && type == Event.MSG) {
                printMsgHeaders(evt, "down()");
            }
            down(evt);
            return;
        }
        try {
            if (type == Event.STOP || type == Event.VIEW_BCAST_MSG) {
                if (handleSpecialDownEvent(evt) == false) {
                    return;
                }
                if (down_prot != null) {
                    down_prot.receiveDownEvent(evt);
                }
                return;
            }

            if (evt.getType() == Event.HAS_STARTED) {
                HasStarted();
                return;
            }

            if (evt.getType() == Event.MSG || evt.getType() == Event.MSG_URGENT) {
                Object tempVar = evt.getArg();
                Message msg = (Message) ((tempVar instanceof Message) ? tempVar : null);
                if (msg != null) {
                    if ((msg.getType() & MsgType.TOKEN_SEEKING) == MsgType.TOKEN_SEEKING) {
                        _tokenSeekingMsgDownQueue.add(evt, evt.getPriority());
                        return;
                    }

                    if (msg.getDests() != null || msg.getDest() == null) {
                        _multicastDownQueue.add(evt, evt.getPriority());
                        return;
                    } else {
                        _unicastDownQueue.add(evt, evt.getPriority());
                        return;
                    }
                }
            }
            _unicastDownQueue.add(evt, evt.getPriority());
        } catch (Exception e) {
            getStack().getCacheLog().Info("Protocol.receiveDownEvent():2", e.toString());
        }
    }

    private void HasStarted() {

        getStack().getCacheLog().CriticalInfo("TCP.HasStarted()", "HasStarted");

        this.isStarting = false;
    }

    @Override
    public void receiveUpEvent(final Event evt) {
        int type = evt.getType();
        if (_printMsgHdrs && type == Event.MSG) {
            printMsgHeaders(evt, "up()");
        }

        if (!up_thread) {
            up(evt);
            return;
        }
        try {

            if (evt.getType() == Event.MSG || evt.getType() == Event.MSG_URGENT) {
                Object tempVar = evt.getArg();
                Message msg = (Message) ((tempVar instanceof Message) ? tempVar : null);

                //We don't queue the critical priority events
                if (evt.getPriority() == Priority.Critical) {
                    Header tempVar2 = msg.getHeader(HeaderType.GMS);
                    GMS.HDR hdr = (GMS.HDR) ((tempVar2 instanceof GMS.HDR) ? tempVar2 : null);
                    boolean allowAsyncPassup = false;
                    if (hdr != null) {
                        switch (hdr.type) {
                            case GMS.HDR.VIEW:
                                if (msg.getSrc() != null && msg.getSrc().equals(local_addr)) {
                                    allowAsyncPassup = true;
                                }
                                break;

                            case GMS.HDR.IS_NODE_IN_STATE_TRANSFER:
                            case GMS.HDR.IS_NODE_IN_STATE_TRANSFER_RSP:
                            case GMS.HDR.VIEW_RESPONSE:
                                allowAsyncPassup = true;
                                break;
                        }

                    }
                    //: passing up GET_MBRS_REQ for faster delivery
                    PingHeader pingHdr = (PingHeader)Common.as(msg.getHeader(HeaderType.TCPPING), PingHeader.class);
                    if (pingHdr != null && pingHdr.type == PingHeader.GET_MBRS_REQ) {
                        allowAsyncPassup = true;
                    }
                    if (allowAsyncPassup) {
                        Thread tmp = new Thread(new Runnable() {

                            @Override
                            public void run() {
                                ThreadPoolPassup(evt);
                            }
                        });
                        tmp.start();

                        return;
                    }
                }

                switch (msg.getType()) {
                    case MsgType.TOKEN_SEEKING:
                        _tokenMsgUpQueue.add(evt, evt.getPriority());
                        break;

                    case MsgType.SEQUENCED:
                        _sequenecedMsgUpQueue.add(evt, evt.getPriority());
                        break;

                    case MsgType.SEQUENCE_LESS:
                        _sequencelessMsgUpQueue.add(evt, evt.getPriority());
                        break;

                    default:
                        _sequencelessMsgUpQueue.add(evt, evt.getPriority());
                        break;
                }
            }

        } catch (Exception e) {
            getStack().getCacheLog().Info("Protocol.receiveUpEvent()", e.toString());
        }
    }

    @Override
    public void startDownHandler() {
        if (down_thread) {
            if (_unicastDownHandler == null) {
                _unicastDownQueue = new com.alachisoft.tayzgrid.common.datastructures.Queue();
                _unicastDownHandler = new DownHandler(_unicastDownQueue, this, getName() + ".unicast.DownHandler", 1);
                if (down_thread_prio >= 0) {
                    try {

                    } catch (Exception t) {
                        getStack().getCacheLog().Error("Protocol.startDownHandler()", "priority " + down_thread_prio + " could not be set for thread: " + t.getStackTrace());
                    }
                }
                _unicastDownHandler.Start();
            }
            if (_multicastDownHandler == null) {
                _multicastDownQueue = new com.alachisoft.tayzgrid.common.datastructures.Queue();
                _multicastDownHandler = new DownHandler(_multicastDownQueue, this, getName() + ".muticast.DownHandler", 2);
                if (down_thread_prio >= 0) {
                    try {

                    } catch (Exception t) {
                        getStack().getCacheLog().Error("Protocol.startDownHandler()", "priority " + down_thread_prio + " could not be set for thread: " + t.getStackTrace());
                    }
                }
                _multicastDownHandler.Start();
            }

            if (_tokenSeekingMsgDownHandler == null) {
                _tokenSeekingMsgDownQueue = new com.alachisoft.tayzgrid.common.datastructures.Queue();
                _tokenSeekingMsgDownHandler = new DownHandler(_tokenSeekingMsgDownQueue, this, getName() + ".token.DownHandler", 3);
                if (down_thread_prio >= 0) {
                    try {

                    } catch (Exception t) {
                        getStack().getCacheLog().Error("Protocol.startDownHandler()", "priority " + down_thread_prio + " could not be set for thread: " + t.getStackTrace());
                    }
                }
                _tokenSeekingMsgDownHandler.Start();
            }
        }
    }

    @Override
    public void startUpHandler() {
        if (up_thread) {

            if (_tokenSeekingUpHandler == null) {
                _tokenMsgUpQueue = new com.alachisoft.tayzgrid.common.datastructures.Queue();
                _tokenSeekingUpHandler = new UpHandler(_tokenMsgUpQueue, this, getName() + ".token.UpHandler", 3);
                if (up_thread_prio >= 0) {
                    try {

                    } catch (Exception t) {
                        getStack().getCacheLog().Error("Protocol", "priority " + up_thread_prio + " could not be set for thread: " + t.getStackTrace());
                    }
                }
                _tokenSeekingUpHandler.Start();
            }

            if (_sequencedMsgUpHandler == null) {
                _sequenecedMsgUpQueue = new com.alachisoft.tayzgrid.common.datastructures.Queue();
                _sequencedMsgUpHandler = new UpHandler(_sequenecedMsgUpQueue, this, getName() + ".seq.UpHandler", 2);
                if (up_thread_prio >= 0) {
                    try {

                    } catch (Exception t) {
                        getStack().getCacheLog().Error("Protocol", "priority " + up_thread_prio + " could not be set for thread: " + t.getStackTrace());
                    }
                }
                _sequencedMsgUpHandler.Start();
            }

            if (_sequencelessMsgUpHandler == null) {
                _sequencelessMsgUpQueue = new com.alachisoft.tayzgrid.common.datastructures.Queue();
                _sequencelessMsgUpHandler = new UpHandler(_sequencelessMsgUpQueue, this, getName() + ".seqless.UpHandler", 1);
                if (up_thread_prio >= 0) {
                    try {

                    } catch (Exception t) {
                        getStack().getCacheLog().Error("Protocol", "priority " + up_thread_prio + " could not be set for thread: " + t.getStackTrace());
                    }
                }
                _sequencelessMsgUpHandler.Start();
            }
        }
    }

    @Override
    public void stopInternal() {

        //stop up handlers
        if (_sequencelessMsgUpQueue != null) {
            _sequencelessMsgUpQueue.close(false); // this should terminate up_handler thread
        }

        if (_sequencelessMsgUpHandler != null && _sequencelessMsgUpHandler.getIsAlive()) {
            try {
                _sequencelessMsgUpHandler.Join(THREAD_JOIN_TIMEOUT);
            } catch (Exception e) {
                getStack().getCacheLog().Error("Protocol.stopInternal()", "up_handler.Join " + e.getMessage());
            }
            if (_sequencelessMsgUpHandler != null && _sequencelessMsgUpHandler.getIsAlive()) {
                _sequencelessMsgUpHandler.Interrupt(); // still alive ? let's just kill it without mercy...
                try {
                    _sequencelessMsgUpHandler.Join(THREAD_JOIN_TIMEOUT);
                } catch (Exception e) {
                    getStack().getCacheLog().Error("Protocol.stopInternal()", "up_handler.Join " + e.getMessage());
                }
                if (_sequencelessMsgUpHandler != null && _sequencelessMsgUpHandler.getIsAlive()) {
                    getStack().getCacheLog().Error("Protocol", "up_handler thread for " + getName() + " was interrupted (in order to be terminated), but is still alive");
                }
            }
        }
        _sequencelessMsgUpHandler = null;

        if (_sequenecedMsgUpQueue != null) {
            _sequenecedMsgUpQueue.close(false); // this should terminate down_handler thread
        }
        if (_sequencedMsgUpHandler != null && _sequencedMsgUpHandler.getIsAlive()) {
            try {
                _sequencedMsgUpHandler.Join(THREAD_JOIN_TIMEOUT);
            } catch (Exception e) {
                getStack().getCacheLog().Error("Protocol.stopInternal()", "down_handler.Join " + e.getMessage());
            }
            if (_sequencedMsgUpHandler != null && _sequencedMsgUpHandler.getIsAlive()) {
                _sequencedMsgUpHandler.Interrupt(); // still alive ? let's just kill it without mercy...
                try {
                    _sequencedMsgUpHandler.Join(THREAD_JOIN_TIMEOUT);
                } catch (Exception e) {
                    getStack().getCacheLog().Error("Protocol.stopInternal()", "down_handler.Join " + e.getMessage());
                }
                if (_sequencedMsgUpHandler != null && _sequencedMsgUpHandler.getIsAlive()) {
                    getStack().getCacheLog().Error("Protocol", "down_handler thread for " + getName() + " was interrupted (in order to be terminated), but is is still alive");
                }
            }
        }
        _sequencedMsgUpHandler = null;

        if (_tokenMsgUpQueue != null) {
            _tokenMsgUpQueue.close(false); // this should terminate down_handler thread
        }
        if (_tokenSeekingUpHandler != null && _tokenSeekingUpHandler.getIsAlive()) {
            try {
                _tokenSeekingUpHandler.Join(THREAD_JOIN_TIMEOUT);
            } catch (Exception e) {
                getStack().getCacheLog().Error("Protocol.stopInternal()", "down_handler.Join " + e.getMessage());
            }
            if (_tokenSeekingUpHandler != null && _tokenSeekingUpHandler.getIsAlive()) {
                _tokenSeekingUpHandler.Interrupt(); // still alive ? let's just kill it without mercy...
                try {
                    _tokenSeekingUpHandler.Join(THREAD_JOIN_TIMEOUT);
                } catch (Exception e) {
                    getStack().getCacheLog().Error("Protocol.stopInternal()", "down_handler.Join " + e.getMessage());
                }
                if (_tokenSeekingUpHandler != null && _tokenSeekingUpHandler.getIsAlive()) {
                    getStack().getCacheLog().Error("Protocol", "down_handler thread for " + getName() + " was interrupted (in order to be terminated), but is is still alive");
                }
            }
        }
        _tokenSeekingUpHandler = null;

        /**
         * stop down handler now.
         */
        if (_unicastDownQueue != null) {
            _unicastDownQueue.close(false); // this should terminate down_handler thread
        }
        if (_unicastDownHandler != null && _unicastDownHandler.getIsAlive()) {
            try {
                _unicastDownHandler.Join(THREAD_JOIN_TIMEOUT);
            } catch (Exception e) {
                getStack().getCacheLog().Error("Protocol.stopInternal()", "down_handler.Join " + e.getMessage());
            }
            if (_unicastDownHandler != null && _unicastDownHandler.getIsAlive()) {
                _unicastDownHandler.Interrupt(); // still alive ? let's just kill it without mercy...
                try {
                    _unicastDownHandler.Join(THREAD_JOIN_TIMEOUT);
                } catch (Exception e) {
                    getStack().getCacheLog().Error("Protocol.stopInternal()", "down_handler.Join " + e.getMessage());
                }
                if (_unicastDownHandler != null && _unicastDownHandler.getIsAlive()) {
                    getStack().getCacheLog().Error("Protocol", "down_handler thread for " + getName() + " was interrupted (in order to be terminated), but is is still alive");
                }
            }
        }
        _unicastDownHandler = null;

        if (_multicastDownQueue != null) {
            _multicastDownQueue.close(false); // this should terminate down_handler thread
        }
        if (_multicastDownHandler != null && _multicastDownHandler.getIsAlive()) {
            try {
                _multicastDownHandler.Join(THREAD_JOIN_TIMEOUT);
            } catch (Exception e) {
                getStack().getCacheLog().Error("Protocol.stopInternal()", "down_handler.Join " + e.getMessage());
            }
            if (_multicastDownHandler != null && _multicastDownHandler.getIsAlive()) {
                _multicastDownHandler.Interrupt(); // still alive ? let's just kill it without mercy...
                try {
                    _multicastDownHandler.Join(THREAD_JOIN_TIMEOUT);
                } catch (Exception e) {
                    getStack().getCacheLog().Error("Protocol.stopInternal()", "down_handler.Join " + e.getMessage());
                }
                if (_multicastDownHandler != null && _multicastDownHandler.getIsAlive()) {
                    getStack().getCacheLog().Error("Protocol", "down_handler thread for " + getName() + " was interrupted (in order to be terminated), but is is still alive");
                }
            }
        }
        _multicastDownHandler = null;

        if (_tokenSeekingMsgDownQueue != null) {
            _tokenSeekingMsgDownQueue.close(false); // this should terminate down_handler thread
        }
        if (_tokenSeekingMsgDownHandler != null && _tokenSeekingMsgDownHandler.getIsAlive()) {
            try {
                _tokenSeekingMsgDownHandler.Join(THREAD_JOIN_TIMEOUT);
            } catch (Exception e) {
                getStack().getCacheLog().Error("Protocol.stopInternal()", "down_handler.Join " + e.getMessage());
            }
            if (_tokenSeekingMsgDownHandler != null && _tokenSeekingMsgDownHandler.getIsAlive()) {
                _tokenSeekingMsgDownHandler.Interrupt(); // still alive ? let's just kill it without mercy...
                try {
                    _tokenSeekingMsgDownHandler.Join(THREAD_JOIN_TIMEOUT);
                } catch (Exception e) {
                    getStack().getCacheLog().Error("Protocol.stopInternal()", "down_handler.Join " + e.getMessage());
                }
                if (_tokenSeekingMsgDownHandler != null && _tokenSeekingMsgDownHandler.getIsAlive()) {
                    getStack().getCacheLog().Error("Protocol", "down_handler thread for " + getName() + " was interrupted (in order to be terminated), but is is still alive");
                }
            }
        }
        _tokenSeekingMsgDownHandler = null;

    }

    @Override
    public void start() throws ExtSocketException {
        // Incase of TCP stack we'll get a reference to TOTAL, which is the top
        // protocol in our case.
        upper = getStack().findProtocol("TOTAL");
        try {

            ct = getConnectionTable(reaper_interval, conn_expire_time, bind_addr1, bind_addr2, start_port, _retries, _retryInterval, isInproc);

        } catch (ExtSocketException ex) {
            if (getStack().getCacheLog().getIsErrorEnabled()) {
                getStack().getCacheLog().Error("TCP.start()", ex.toString());
            }
            throw ex;
        }

        ct.addConnectionListener(this);
        ct.setReceiveBufferSize(recv_buf_size);
        ct.setSendBufferSize(send_buf_size);
        try {
            local_addr = ct.getLocalAddress();
        } catch (UnknownHostException ex) {
            if (getStack().getCacheLog().getIsErrorEnabled()) {
                getStack().getCacheLog().Error("TCP.start()", ex.toString());
            }
        }
        if (additional_data != null) {
            ((Address) local_addr).setAdditionalData(additional_data);
        }
        passUp(new Event(Event.SET_LOCAL_ADDRESS, local_addr, Priority.Critical));
        _asyncProcessor = new AsyncProcessor(stack.getCacheLog());
        _asyncProcessor.Start();

        _keepAlive = new ConnectionKeepAlive(this, ct, _heartBeatInterval);
        if (_useKeepAlive) {
            _keepAlive.Start();
        }

        getStack().getCacheLog().CriticalInfo("TCP.start", "operating parameters -> [ heart_beat:" + _useKeepAlive + " ;heart_beat_interval: " + _heartBeatInterval
                + " ;async_up_deliver: " + asyncPassup + " ;connection_retries: " + _retries + " ;connection_retry_interval: " + _retryInterval + " ]");

    }

    /**
     * @param ri
     *
     * @param cet
     *
     * @param b_addr
     *
     * @param s_port
     *
     * <throws> Exception </throws>
     * @return ConnectionTable Sub classes overrides this method to initialize a
     * different version of ConnectionTable.
     *
     *
     */
    protected ConnectionTable getConnectionTable(long ri, long cet, InetAddress b_addr1, InetAddress b_addr2, int s_port, int retries, int retryInterval, boolean isInproc) throws ExtSocketException {
        ConnectionTable cTable = null;

        if (ri == 0 && cet == 0) {
            cTable = new ConnectionTable(this, b_addr1, bind_addr2, start_port, port_range, stack.getCacheLog(), retries, retryInterval, isInproc);
        } else {
            if (ri == 0) {
                ri = 5000;
                getStack().getCacheLog().Info("reaper_interval was 0, set it to " + ri);
            }
            if (cet == 0) {
                cet = 1000 * 60 * 5;
                getStack().getCacheLog().Info("conn_expire_time was 0, set it to " + cet);
            }
            cTable = new ConnectionTable(this, b_addr1, s_port, ri, cet, this.getStack().getCacheLog());
        }

        return cTable;
    }

    @Override
    public void stop() {
        isClosing = true;
        local_addr = null;
        if (_asyncProcessor != null) {
            _asyncProcessor.Stop();
        }
        _asyncProcessor = null;

        if (_keepAlive != null) {
            _keepAlive.Stop();
        }

        ct.stop();
        upper = null;
    }

    @Override
    protected boolean handleSpecialDownEvent(Event evt) {
        //We handle the view message differently to handle the situation
        //where coordinator itself is leaving
        if (evt.getType() == Event.VIEW_BCAST_MSG) {
            getStack().getCacheLog().Error("TCP.handleSpecialDownEvent", evt.toString());
            down(new Event(Event.MSG, evt.getArg(), evt.getPriority()));
            getStack().getCacheLog().Error("TCP.handleSpecialDownEvent", "view broadcast is complete");
            return false;
        }

        return super.handleSpecialDownEvent(evt);
    }

    /**
     * Sent to destination(s) using the ConnectionTable class.
     */
    @Override
    public void down(Event evt) {
        Message msg;
        Object dest_addr;
        boolean reEstablishCon = false;

        stats = new TimeStats(1);

        if (evt.getType() != Event.MSG && evt.getType() != Event.MSG_URGENT) {
            try {
                handleDownEvent(evt);
            } catch (UnknownHostException unknownHostException) {
                getStack().getCacheLog().Error("TCP.down", "UnknownHostException: " + unknownHostException.toString());
            }

            return;
        }

        reEstablishCon = evt.getType() == Event.MSG_URGENT ? true : false;

        msg = (Message) evt.getArg();
        msg.setPriority(evt.getPriority());

        if (getStack().getCacheLog().getIsInfoEnabled()) {
            stack.getCacheLog().Info("Tcp.down()", " message headers = " + Global.CollectionToString(msg.getHeaders()));
        }

        if (group_addr != null) {

            msg.putHeader(HeaderType.TCP, new TcpHeader(group_addr));
        }

        dest_addr = msg.getDest();

        try {
            if (dest_addr == null) {

                if (group_addr == null) {
                    getStack().getCacheLog().Info("dest address of message is null, and " + "sending to default address fails as group_addr is null, too !"
                            + " Discarding message.");
                    return;
                } else {
                    if (reEstablishCon && _asyncProcessor != null) {
                        synchronized (async_mutex) {
                            if (asyncThreads != null) {
                                final TCP.AsnycMulticast asyncMcast = new TCP.AsnycMulticast(this, msg, reEstablishCon);
                                Thread asyncThread = new Thread(new Runnable() {

                                    @Override
                                    public void run() {
                                        asyncMcast.Process();
                                    }
                                });
//                                asyncThread.start();

                                asyncThreads.add(asyncThread);
                                asyncThread.start();
                            }
                        }
                        if (getStack().getCacheLog().getIsInfoEnabled()) {
                            stack.getCacheLog().Info("Tcp.down", "broadcasting message asynchronously ");
                        }

                    } else {
                        try {
                            sendMulticastMessage(msg, reEstablishCon, evt.getPriority()); // send to current membership
                        } catch (IOException ex) {
                            if (getStack().getCacheLog().getIsErrorEnabled()) {
                                getStack().getCacheLog().Error("TCP.down()", ex.toString());
                            }
                        }
                    }
                }
            } else {
                if (getStack().getCacheLog().getIsInfoEnabled()) {
                    stack.getCacheLog().Info("Tcp.down()", " destination address " + msg.getDest().toString());
                }
                if (reEstablishCon && _asyncProcessor != null) {
                    synchronized (async_mutex) {
                        if (asyncThreads != null) {
                            final TCP.AsnycUnicast asyncUcast = new TCP.AsnycUnicast(this, msg, reEstablishCon);
                            Thread asyncThread = new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    asyncUcast.Process();
                                }
                            });

                            asyncThread.start();
                        }
                    }

                } else {
                    sendUnicastMessage(msg, reEstablishCon, msg.getPayload(), evt.getPriority()); // send to a single member
                }
            }
        } catch (Exception ex) {
            stack.getCacheLog().DevTrace("TCP.down", ex.getMessage());
        } finally {
        }

    }

    @Override
    public void PublishDownQueueStats(long count, int queueId) {

        switch (queueId) {
            case 1:

                break;
        }
    }

    @Override
    public void PublishUpQueueStats(long count, int queueId) {

    }

    /**
     * ConnectionTable.Receiver interface
     */
    public void receive(Message msg) {
        TcpHeader hdr = null;
        msg.setDest(local_addr);

        final Event evt = new Event();
        evt.setArg(msg);
        evt.setPriority(msg.getPriority());
        evt.setType(Event.MSG);

        Header tempVar = msg.removeHeader(HeaderType.KEEP_ALIVE);
        HearBeat hrtBeat = (HearBeat) ((tempVar instanceof HearBeat) ? tempVar : null);
        if (hrtBeat != null && _keepAlive != null) {
            _keepAlive.ReceivedHeartBeat(msg.getSrc(), hrtBeat);
            return;
        }

        Header tempVar2 = msg.getHeader(HeaderType.TOTAL);
        TOTAL.HDR totalhdr = (TOTAL.HDR) ((tempVar2 instanceof TOTAL.HDR) ? tempVar2 : null);

        if (totalhdr != null) {

        }

        if (!asyncPassup) {
            this.receiveUpEvent(evt);
        } else {
            Thread workerThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    ThreadPoolPassup(evt);
                }
            });
            workerThread.start();

        }
    }

    public final void ThreadPoolLocalPassUp(final Event evt) {
        Thread workerThread = new Thread(new Runnable() {

            @Override
            public void run() {
                ThreadPoolPassup(evt);
            }
        });
        workerThread.start();

    }

    public final void ThreadPoolPassup(Object evt) {
        try {
            this.up((Event) evt);
        } catch (Exception e) {
            getStack().getCacheLog().Error("ThreadPoolPassUp", e.toString());
        }
    }

    /**
     *
     * @param peer_addr
     */
    @Override
    public void connectionOpened(Address peer_addr) {
        if (getStack().getCacheLog().getIsInfoEnabled()) {
            getStack().getCacheLog().Info("opened connection to " + peer_addr);
        }
    }

    @Override
    public void connectionClosed(Address peer_addr) {
        if (peer_addr != null && local_addr != null) {
            if (getStack().getCacheLog().getIsInfoEnabled()) {
                getStack().getCacheLog().Info("closed connection to " + peer_addr + " added to the suspected list");
            }
            suspected_mbrs.add(peer_addr);
            Event evt = new Event(Event.SUSPECT, peer_addr, Priority.Critical);
            evt.setReason("Connection closed called for suspect event");
            passUp(evt);
        }
    }
    
    /**
     *
     * @param peer_addr
     */
    @Override
    public void couldnotConnectTo(Address peer_addr) {
        passUp(new Event(Event.CONNECTION_NOT_OPENED, peer_addr, Priority.Critical));
    }

    @Override
    public void up(Event evt) {
        TcpHeader hdr = null;
        Message msg = null;

        switch (evt.getType()) {
            case Event.MSG:
                msg = (Message) evt.getArg();

                if (msg.getIsProfilable()) {
                    stack.getCacheLog().Error("--------------------------------------", " ---------------Request.Add-->" + msg.getTraceMsg() + "----------");
                    stack.getCacheLog().Error("TCP", msg.getTraceMsg() + " received from ---> " + msg.getSrc().toString());
                }
                if (getStack().getCacheLog().getIsInfoEnabled()) {
                    getStack().getCacheLog().Info("TCP.up()", "src: " + msg.getSrc() + " ,priority: " + evt.getPriority() + ", hdrs: "
                            + Global.CollectionToString(msg.getHeaders()));
                }
                if (msg.getIsProfilable()) {

                }
                hdr = (TcpHeader) msg.removeHeader(HeaderType.TCP);

                if (hdr != null) {
                    /*
                     * Discard all messages destined for a channel with a different name
                     */
                    String ch_name = null;

                    if (hdr.group_addr != null) {
                        ch_name = hdr.group_addr;
                    }

                    if (ch_name != null && !group_addr.equals(ch_name)) {
                        getStack().getCacheLog().Info("discarded message from different group (" + ch_name + "). Sender was " + msg.getSrc());
                        return;
                    }

                }

                passUp(evt);
                break;

        }

    }

    /**
     * Setup the Protocol instance acording to the configuration string
     *
     * @param props
     * @return
     * @throws java.lang.Exception
     */
    @Override
    public boolean setProperties(java.util.HashMap props) throws Exception {
        String str;

        super.setProperties(props);

        if (ServicePropValues.asyncTcpUpQueue != null) {
            asyncPassup = Boolean.parseBoolean(ServicePropValues.asyncTcpUpQueue);
        }
        down_thread = false;

        if (props.containsKey("start_port")) {

            start_port = Integer.decode((String) props.get("start_port"));
            props.remove("start_port");

        }
        
        if (props.containsKey("is_inproc")) {

            isInproc = Boolean.parseBoolean((String) props.get("is_inproc"));
            props.remove("is_inproc");

        }

        if (props.containsKey("port_range")) {
            port_range = Integer.decode((String) props.get("port_range"));
            if (port_range <= 0) {
                port_range = 1;
            }
            props.remove("port_range");
        }

        if (props.containsKey("heart_beat_interval")) {

            props.remove("heart_beat_interval");
        }

        if (props.containsKey("connection_retries")) {
            _retries = Integer.decode((String) props.get("connection_retries"));
            props.remove("connection_retries");
        }

        if (props.containsKey("connection_retry_interval")) {
            _retryInterval = Integer.decode((String) props.get("connection_retry_interval"));
            props.remove("connection_retry_interval");
        }

        if (ServicePropValues.CacheServer_HeartbeatInterval != null) {
            _heartBeatInterval = Integer.parseInt(ServicePropValues.CacheServer_HeartbeatInterval);
            _heartBeatInterval = _heartBeatInterval * 1000;
        }

        // It is supposed that bind_addr will be provided only through props.
        String ip = ServicePropValues.BIND_ToCLUSTER_IP;

       
        if (ip != null && !ip.equals("")) {
            try {
                String[] str1 = ip.split("\\.");
                byte[] bite = new byte[4];
                bite[0] = new Integer(str1[0]).byteValue();
                bite[1] = new Integer(str1[1]).byteValue();
                bite[2] = new Integer(str1[2]).byteValue();
                bite[3] = new Integer(str1[3]).byteValue();
                bind_addr1 = InetAddress.getByAddress(bite);

            } catch (Exception e) {
                throw new Exception("Invalid BindToClusterIP address specified");
            }

        } else {
            try {
                str = InetAddress.getLocalHost().getHostName();
                bind_addr1 = InetAddress.getByName(str);

                if (getStack().getCacheLog().getIsInfoEnabled()) {
                    getStack().getCacheLog().Info("TCP.SetProperties()", "Bind address = " + bind_addr1.toString());
                }
            } catch (Exception ex) {
                stack.getCacheLog().Error("TCP.SetProperties()", "bind address failure :" + ex.toString());
            }
        }

        if (props.containsKey("bind_addr")) {
            props.remove("bind_addr");
        }

        if (props.containsKey("reaper_interval")) {
            reaper_interval = Long.parseLong((String) props.get("reaper_interval"));
            props.remove("reaper_interval");
        }

        if (props.containsKey("conn_expire_time")) {
            conn_expire_time = Long.parseLong((String) props.get("conn_expire_time"));
            props.remove("conn_expire_time");
        }

        if (props.containsKey("recv_buf_size")) {
            recv_buf_size = Integer.parseInt((String) props.get("recv_buf_size"));
            props.remove("recv_buf_size");
        }

        if (props.containsKey("send_buf_size")) {
            send_buf_size = Integer.parseInt((String) props.get("send_buf_size"));
            props.remove("send_buf_size");
        }

        if (props.containsKey("loopback")) {
            loopback = Boolean.parseBoolean((String) props.get("loopback"));
            props.remove("loopback");
        }
        if (props.containsKey("use_heart_beat")) {
            _useKeepAlive = Boolean.parseBoolean((String) props.get("use_heart_beat"));
            props.remove("use_heart_beat");
        }

        if (props.containsKey("skip_suspected_members")) {
            skip_suspected_members = Boolean.parseBoolean((String) props.get("skip_suspected_members"));
            props.remove("skip_suspected_members");
        }

        if (props.size() > 0) {
            stack.getCacheLog().Error("TCP.setProperties()", "the following properties are not recognized:");
            return true;
        }

        return true;
    }

    /**
     * Determines if the local node is junior than the other then the other
     * node.
     *
     * @param address
     * @return
     */
    public final boolean IsJuniorThan(Address address) {
        boolean isJunior = true;
        if (members != null) {
            int myIndex = members.indexOf(local_addr);
            int otherIndex = members.indexOf(address);

            isJunior = myIndex > otherIndex ? true : false;
        }
        return isJunior;
    }

    /**
     * Send a message to the address specified in msg.dest
     */
    private void sendLocalMessage(Message msg) {
        Message copy;
        Object hdr;
        final Event evt;

        setSourceAddress(msg);

        /*
         * Don't send if destination is local address. Instead, switch dst and src and put in up_queue
         */
        if (loopback && local_addr != null) {
            copy = msg.copy();
            copy.setType(msg.getType());

            hdr = copy.getHeader(HeaderType.TCP);
            if (hdr != null && hdr instanceof TcpHeader) {
                copy.removeHeader(HeaderType.TCP);

            }

            copy.setSrc(local_addr);
            copy.setDest(local_addr);

            if (msg.getIsProfilable()) {
                stack.getCacheLog().Error("TCP", msg.getTraceMsg() + " sending to ---> " + msg.getDest().toString());
            }
            evt = new Event(Event.MSG, copy, copy.getPriority());

            if (!asyncPassup) {
                this.receiveUpEvent(evt);
            } else {
                Thread workerThread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        ThreadPoolPassup(evt);
                    }
                });
                workerThread.start();

            }

            if (msg.getIsProfilable()) {
                stack.getCacheLog().Error("TCP", msg.getTraceMsg() + " sent to ---> " + msg.getDest().toString());
            }
            return;
        }

    }

    private void sendUnicastMessage(Message msg, boolean reEstablishConnection, Object[] UserPayLoad, Priority priority) {
        Address dest = msg.getDest();

        try {
            if (dest == null) {
                return;
            }

            if (dest.equals(local_addr)) {
                sendLocalMessage(msg);
            } else {

                byte[] binaryMessage = Util.serializeMessage(msg);
                sendUnicastMessage(dest, binaryMessage, reEstablishConnection, msg.getPayload(), priority);
            }
        } catch (Exception e) {

        }
    }

    /**
     * Send a message to the address specified in msg.dest
     */
    private void sendUnicastMessage(Address dest, byte[] msg, boolean reEstablishCon, Object[] UserPayload, Priority priority) {

        try {

            if (skip_suspected_members) {
                if (suspected_mbrs.contains(dest)) {
                    if (getStack().getCacheLog().getIsInfoEnabled()) {
                        getStack().getCacheLog().Info("will not send unicast message to " + dest + " as it is currently suspected");
                    }
                    return;
                }
            }

            long bytesSent = ct.send(dest, msg, reEstablishCon, UserPayload, priority);
        } catch (ExtSocketException e) {
            if (members.contains(dest)) {
                if (!suspected_mbrs.contains(dest) && local_addr != null) {
                    suspected_mbrs.add(dest);
                    Event evt = new Event(Event.SUSPECT, dest, Priority.Critical);
                    evt.setReason("Tcp.sendUnicastMesssage caused suspect event");
                    passUp(evt);
                }
            }
            stack.getCacheLog().Error("TCP.sendUnicastMessage()", e.toString());
        }
    }

    private void sendMulticastMessage(Message msg, boolean reEstablishCon, Priority priority) throws IOException {
        if (msg.getIsProfilable()) {
            stack.getCacheLog().Error("Tcp.sendMulticastMessage", msg.getTraceMsg() + " :started");
        }
        Address dest;
        java.util.List dest_addrs = msg.getDests();
        java.util.List mbrs = null;

        boolean deliverLocal = false;

        //if not intended for a list of destinations
        if (dest_addrs == null || dest_addrs.isEmpty()) {
            lock_members.readLock().lock();
            try {
                mbrs = (java.util.List) GenericCopier.DeepCopy(members);//.clone();
            } finally {
                lock_members.readLock().unlock();
            }
            mbrs.remove(local_addr);
            deliverLocal = true;

            if (getStack().getCacheLog().getIsInfoEnabled()) {
                getStack().getCacheLog().Info("Tcp.sendmulticastmessage()", "members count " + mbrs.size());
            }
        } else {
            Object tempVar = GenericCopier.DeepCopy(dest_addrs);//dest_addrs.clone();
            dest_addrs = (java.util.List) ((tempVar instanceof java.util.List) ? tempVar : null);
            if (dest_addrs.contains(local_addr)) {
                dest_addrs.remove(local_addr);

                deliverLocal = true;
            }
        }

        byte[] binaryMsg = Util.serializeMessage(msg);

        if (mbrs != null) {
            if (getStack().getCacheLog().getIsInfoEnabled()) {
                getStack().getCacheLog().Info("Tcp.sendmulticastmessage()", "Sending to all members -- broadcasting");
            }
            for (int i = 0; i < mbrs.size(); i++) {
                dest = (Address) mbrs.get(i);
                msg.setDest(dest);
                if (getStack().getCacheLog().getIsInfoEnabled()) {
                    getStack().getCacheLog().Info("Tcp.sendmulticastmessage()", "Sending to " + dest.toString());
                }
                if (getStack().getCacheLog().getIsInfoEnabled()) {
                    getStack().getCacheLog().Info("TCP.sendUnicastMessage()", "dest=" + dest + ", hdrs:" + Global.CollectionToString(msg.getHeaders()));
                }

                sendUnicastMessage(dest, binaryMsg, reEstablishCon, msg.getPayload(), priority);
            }
        } else {
            if (getStack().getCacheLog().getIsInfoEnabled()) {
                getStack().getCacheLog().Info("Tcp.sendmulticastmessage()", "Sending to selective members -- multicasting");
            }
            for (int i = 0; i < dest_addrs.size(); i++) {
                dest = (Address) dest_addrs.get(i);
                msg.setDest(dest);
                if (getStack().getCacheLog().getIsInfoEnabled()) {
                    getStack().getCacheLog().Info("Tcp.sendmulticastmessage()", "Sending to " + dest.toString());
                }
                if (getStack().getCacheLog().getIsInfoEnabled()) {
                    getStack().getCacheLog().Info("TCP.sendUnicastMessage()", "dest=" + dest + ", hdrs:" + Global.CollectionToString(msg.getHeaders()));
                }
                sendUnicastMessage(dest, binaryMsg, reEstablishCon, msg.getPayload(), priority);
            }
        }

        if (deliverLocal) {
            sendLocalMessage(msg);
        }

        if (msg.getIsProfilable()) {
            stack.getCacheLog().Error("Tcp.sendMulticastMessage", msg.getTraceMsg() + " :end");
        }
    }

    private void handleDownEvent(Event evt) throws UnknownHostException {
        switch (evt.getType()) {

            case Event.GET_NODE_STATUS:

                Object tempVar = evt.getArg();
                Address suspect = (Address) ((tempVar instanceof Address) ? tempVar : null);

                if (_keepAlive != null) {
                    _keepAlive.CheckStatus(suspect);
                }

                break;

            case Event.FIND_INITIAL_MBRS:

                synchronized (async_mutex) {
                    getStack().getCacheLog().Flush();
                    for (int i = 0; i < asyncThreads.size(); i++) {
                        Thread t = (Thread) ((asyncThreads.get(i) instanceof Thread) ? asyncThreads.get(i) : null);
                        if (t != null && t.isAlive()) {
                            t.stop();
                        }
                    }
                    asyncThreads.clear();
                }
                break;

            case Event.GET_STATE:
                passUp(new Event(Event.GET_STATE_OK));
                break;

            case Event.TMP_VIEW:
            case Event.VIEW_CHANGE:

                java.util.ArrayList temp_mbrs = new java.util.ArrayList();
                java.util.ArrayList nodeJoiningList = new java.util.ArrayList();
                lock_members.writeLock().lock();
                try {
                    members.clear();
                    java.util.List tmpvec = ((View) evt.getArg()).getMembers();
                    if (getStack().getCacheLog().getIsInfoEnabled()) {
                        getStack().getCacheLog().Info("Tcp.down()", ((evt.getType() == Event.TMP_VIEW) ? "TMP_VIEW" : "VIEW_CHANGE"));
                    }
                    if (getStack().getCacheLog().getIsInfoEnabled()) {
                        getStack().getCacheLog().Info("Tcp.down()", " View change members count" + tmpvec.size());
                    }

                    Address address = null;

                    for (int i = 0; i < tmpvec.size(); i++) {
                        address = (Address) ((tmpvec.get(i) instanceof Address) ? tmpvec.get(i) : null);
                        if (address != null && !(isStarting && address.getIpAddress().equals(local_addr.getIpAddress()))) {
                            temp_mbrs.add(tmpvec.get(i));
                        }
                        members.add(tmpvec.get(i));
                        nodeJoiningList.add(tmpvec.get(i));
                    }
                } finally {
                    lock_members.writeLock().unlock();
                }

                if (_asyncProcessor != null) {
                    _asyncProcessor.Stop();
                }

                synchronized (async_mutex) {
                    if (getStack().getCacheLog() != null) {
                        getStack().getCacheLog().Flush();
                    }
                    for (int i = 0; i < asyncThreads.size(); i++) {
                        Thread t = (Thread) ((asyncThreads.get(i) instanceof Thread) ? asyncThreads.get(i) : null);
                        if (t != null && t.isAlive()) {
                            t.stop();
                        }
                    }
                    asyncThreads.clear();
                }
                ct.ConfigureNodeRejoining(nodeJoiningList);

                java.util.ArrayList failedNodes = new ArrayList();
                try {
                    failedNodes = ct.synchronzeMembership(temp_mbrs, false);
                } catch (InterruptedException interruptedException) {
                    getStack().getCacheLog().Error("TCP.HandleDownEvent()", " ct.synchronzeMembership interrupted");
                }

                passUp(evt);

                if (failedNodes.size() > 0) {
                    if (getStack().getCacheLog().getIsInfoEnabled()) {
                        getStack().getCacheLog().Info("TCP.HandleDownEvent()", " can not establish connection with all the nodes ");
                    }

                    passUp(new Event(Event.CONNECTION_FAILURE, failedNodes, Priority.Critical));
                }

                break;
            case Event.GET_LOCAL_ADDRESS: // return local address -> Event(SET_LOCAL_ADDRESS, local)
                passUp(new Event(Event.SET_LOCAL_ADDRESS, local_addr));
                break;

            case Event.CONNECT:
                Object[] addrs = ((Object[]) evt.getArg());

                group_addr = (String) addrs[0];
                subGroup_addr = (String) addrs[1];
                boolean twoPhaseConnect = (Boolean) addrs[2];
                synchronizeConnections = !twoPhaseConnect;
                if (getStack().getCacheLog().getIsInfoEnabled()) {
                    getStack().getCacheLog().Info("TCP.HandleDownEvent()", " group_address is : " + group_addr);
                }

                Address addr = new Address(ct.srv_port);

                passUp(new Event(Event.CONNECT_OK, (Object) addr));
                break;

            case Event.DISCONNECT:
                passUp(new Event(Event.DISCONNECT_OK));
                break;

            case Event.CONFIG:
                if (getStack().getCacheLog().getIsInfoEnabled()) {
                    getStack().getCacheLog().Info("TCP.handleDownEvent", "received CONFIG event: " + evt.getArg());
                }
                handleConfigEvent((java.util.HashMap) evt.getArg());
                break;

            case Event.ACK:
                passUp(new Event(Event.ACK_OK));
                break;
        }
    }

    public void handleConfigEvent(java.util.HashMap map) {
        if (map == null) {
            return;
        }
        if (map.containsKey("additional_data")) {
            additional_data = (byte[]) map.get("additional_data");
        }
    }

    public final boolean IsMember(Address node) {
        if (node != null) {
            lock_members.readLock().lock();
            try {

                if (members != null && members.contains(node)) {
                    return true;
                }

            } finally {
                lock_members.readLock().unlock();
            }
        }
        return false;
    }

    ///#region /                       --- AsnycUnicast  ---                   /
    /**
     * Asynchronously sends the unicast message.
     */
    public static class AsnycUnicast implements AsyncProcessor.IAsyncTask {

        private TCP _parent;
        private Message _message;
        private boolean _reEstablishConnection;

        public AsnycUnicast(TCP parent, Message m, boolean reEstablish) {
            _parent = parent;
            _message = m;
            _reEstablishConnection = reEstablish;
        }

        /**
         * Sends the message.
         */
        public final void Process() {
            if (_parent != null) {
                if (_parent.getStack().getCacheLog().getIsInfoEnabled()) {
                    _parent.getStack().getCacheLog().Info("TCP.AsnycUnicast.Process", "sending message to " + _message.getDest().toString());
                }
                _parent.sendUnicastMessage(_message, _reEstablishConnection, _message.getPayload(), Priority.Critical);
            }
        }

    }

    /**
     * Asynchronously sends the multicast message.
     */
    public static class AsnycMulticast implements AsyncProcessor.IAsyncTask {

        private TCP _parent;
        private Message _message;
        private boolean _reEstablishConnection;

        public AsnycMulticast(TCP parent, Message m, boolean reEstablish) {
            _parent = parent;
            _message = m;
            _reEstablishConnection = reEstablish;
        }

        /**
         * broadcasts the message.
         */
        public final void Process() {
            if (_parent != null) {
                if (_parent.getStack().getCacheLog().getIsInfoEnabled()) {
                    _parent.getStack().getCacheLog().Info("TCP.AsnycMulticast.Process", "broadcasting message");
                }
                try {
                    _parent.sendMulticastMessage(_message, _reEstablishConnection, Priority.Critical);
                } catch (IOException ex) {
                    if (_parent.getStack().getCacheLog().getIsErrorEnabled()) {
                        _parent.getStack().getCacheLog().Error("TCP.AsnycMulticast.Process()", ex.toString());
                    }
                }
            }
        }

    }

    public static class ConnectionKeepAlive implements Runnable {

        private TCP _enclosingInstance;
        private ConnectionTable _ct;
        private int _interval = 8000;
        private java.util.HashMap _idleConnections = new java.util.HashMap();
        private Thread _thread;
        private Thread _statusCheckingThread;
        private int _maxAttempts = 5;
        private java.util.ArrayList _checkStatusList = new java.util.ArrayList();
        private Object _status_mutex = new Object();
        private Object _statusReceived;
        private Address _currentSuspect;
        private int _statusTimeout = 45000;

        public ConnectionKeepAlive(TCP enclosingInsatnce, ConnectionTable connectionTable, int interval) {
            _enclosingInstance = enclosingInsatnce;
            _ct = connectionTable;
            _interval = interval;
        }

        public final void Start() {
            if (_thread == null) {
                _thread = new Thread(this);
                _thread.setName("TCP.ConnectionKeepAlive");
                _thread.start();
            }
        }

        public final void Stop() {
            if (_enclosingInstance.getStack().getCacheLog() != null) {
                _enclosingInstance.getStack().getCacheLog().Flush();
            }
            if (_thread != null) {
                _thread.stop();
                _thread = null;
            }
            if (_statusCheckingThread != null) {
                _statusCheckingThread.stop();
                _statusCheckingThread = null;
            }
        }

        @Override
        public final void run() {
            try {
                java.util.ArrayList idleMembers;
                java.util.ArrayList suspectedList = new java.util.ArrayList();
                _ct.SetConnectionsStatus(true);
                Thread.sleep(_interval);

                while (_thread != null) {
                    idleMembers = _ct.GetIdleMembers();

                    if (idleMembers.size() > 0) {
                        synchronized (_idleConnections) {
                            for (int i = 0; i < idleMembers.size(); i++) {
                                Address member = (Address) ((idleMembers.get(i) instanceof Address) ? idleMembers.get(i) : null);

                                if (_enclosingInstance.getStack().getCacheLog().getIsInfoEnabled()) {
                                    _enclosingInstance.getStack().getCacheLog().Info("ConnectionKeepAlive.Run", "pining idle member ->:" + member.toString());
                                }

                                if (!_idleConnections.containsKey(member)) {
                                    _idleConnections.put(idleMembers.get(i), (int) 1);
                                } else {
                                    int attemptCount = (Integer) _idleConnections.get(member);
                                    attemptCount++;

                                    if (attemptCount > _maxAttempts) {
                                        _idleConnections.remove(member);
                                        suspectedList.add(member);

                                    } else {
                                        if (_enclosingInstance.getStack().getCacheLog().getIsErrorEnabled()) {
                                            _enclosingInstance.getStack().getCacheLog().Error("ConnectionKeepAlive.Run", attemptCount + " did not received any heart beat ->:"
                                                    + member.toString());
                                        }

                                        _idleConnections.put(member, attemptCount);
                                    }

                                }

                            }
                        }
                        AskHeartBeats(idleMembers);
                    }

                    if (_enclosingInstance.getStack().getCacheLog().getIsInfoEnabled()) {
                        _enclosingInstance.getStack().getCacheLog().Info("ConnectionKeepAlive.Run", "setting connections status to idle ->:");
                    }

                    _ct.SetConnectionsStatus(true);
                    for (Iterator it = suspectedList.iterator(); it.hasNext();) {
                        Address suspected = (Address) it.next();
                        if (_enclosingInstance.getStack().getCacheLog().getIsErrorEnabled()) {
                            _enclosingInstance.getStack().getCacheLog().Error("ConnectionKeepAlive.Run", "member being suspected ->:" + suspected.toString());
                        }
                        _ct.remove(suspected, true);
                        _enclosingInstance.connectionClosed(suspected);
                    }
                    suspectedList.clear();

                    Thread.sleep(_interval);
                }

            } catch (Exception e) {
                _enclosingInstance.getStack().getCacheLog().Error("ConnectionKeepAlive.Run", e.toString());
            }
            if (_enclosingInstance.getStack().getCacheLog().getIsInfoEnabled()) {
                _enclosingInstance.getStack().getCacheLog().Info("ConnectionKeepAlive.Run", "exiting keep alive thread");
            }
        }

        /**
         * Cheks the status of a node in a separate thread.
         *
         * @param node
         */
        public final void CheckStatus(Address node) {

            synchronized (_checkStatusList) {
                _checkStatusList.add(node);
                if (_statusCheckingThread == null) {
                    _statusCheckingThread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            CheckStatus();
                        }
                    });

                    _statusCheckingThread.setName("ConnectioKeepAlive.CheckStatus");

                    _statusCheckingThread.start();
                }
            }

        }

        /**
         * Checks the status of a node whether he is running or not. We send a
         * status request message and wait for the response for a particular
         * timeout. If the node is alive it sends backs its status otherwise
         * timeout occurs and we consider hime DEAD.
         */
        private void CheckStatus() {

            while (_statusCheckingThread != null) {
                synchronized (_checkStatusList) {
                    if (_checkStatusList.size() > 0) {
                        _currentSuspect = (Address) ((_checkStatusList.get(0) instanceof Address) ? _checkStatusList.get(0) : null);
                        _checkStatusList.remove(_currentSuspect);
                    } else {
                        _currentSuspect = null;
                    }

                    if (_currentSuspect == null) {
                        _statusCheckingThread = null;
                        continue;
                    }
                }

                synchronized (_status_mutex) {
                    try {
                        NodeStatus nodeStatus = null;
                        if (_enclosingInstance.ct.ConnectionExist(_currentSuspect)) {
                            Message msg = new Message(_currentSuspect, null, new byte[0]);
                            msg.putHeader(HeaderType.KEEP_ALIVE, new HearBeat(HearBeat.ARE_YOU_ALIVE));

                            if (_enclosingInstance.getStack().getCacheLog().getIsInfoEnabled()) {
                                _enclosingInstance.getStack().getCacheLog().Info("ConnectionKeepAlive.CheckStatus", "sending status request to " + _currentSuspect);
                            }

                            _enclosingInstance.sendUnicastMessage(msg, false, msg.getPayload(), Priority.Critical);
                            _statusReceived = null;

                            Monitor.wait(_status_mutex, _statusTimeout);//_status_mutex.wait(_statusTimeout);

                            if (_statusReceived != null) {
                                HearBeat status = (HearBeat) ((_statusReceived instanceof HearBeat) ? _statusReceived : null);

                                if (_enclosingInstance.getStack().getCacheLog().getIsInfoEnabled()) {
                                    _enclosingInstance.getStack().getCacheLog().Info("ConnectionKeepAlive.CheckStatus", "received status " + status + " from " + _currentSuspect);
                                }

                                if (status.getType() == HearBeat.I_AM_NOT_DEAD) {
                                    nodeStatus = new NodeStatus(_currentSuspect, NodeStatus.IS_ALIVE);
                                } else if (status.getType() == HearBeat.I_AM_LEAVING) {
                                    nodeStatus = new NodeStatus(_currentSuspect, NodeStatus.IS_LEAVING);
                                } else if (status.getType() == HearBeat.I_AM_STARTING) {
                                    nodeStatus = new NodeStatus(_currentSuspect, NodeStatus.IS_DEAD);
                                }

                            } else {
                                nodeStatus = new NodeStatus(_currentSuspect, NodeStatus.IS_DEAD);
                                if (_enclosingInstance.getStack().getCacheLog().getIsInfoEnabled()) {
                                    _enclosingInstance.getStack().getCacheLog().Info("ConnectionKeepAlive.CheckStatus", "did not receive status from " + _currentSuspect
                                            + "; consider him DEAD");
                                }
                            }
                        } else {
                            if (_enclosingInstance.getStack().getCacheLog().getIsInfoEnabled()) {
                                _enclosingInstance.getStack().getCacheLog().Info("ConnectionKeepAlive.CheckStatus", "no connection exists for " + _currentSuspect);
                            }
                            nodeStatus = new NodeStatus(_currentSuspect, NodeStatus.IS_DEAD);
                        }

                        Event statusEvent = new Event(Event.GET_NODE_STATUS_OK, nodeStatus);
                        _enclosingInstance.passUp(statusEvent);
                    } catch (Exception e) {
                        _enclosingInstance.getStack().getCacheLog().Error("ConnectionKeepAlive.CheckStatus", e.toString());
                    } finally {
                        _currentSuspect = null;
                        _statusReceived = null;
                    }
                }

            }
        }

        private void AskHeartBeats(java.util.ArrayList idleMembers) throws IOException {
            Message msg = new Message(null, null, new byte[0]);
            msg.putHeader(HeaderType.KEEP_ALIVE, new HearBeat(HearBeat.SEND_HEART_BEAT));
            Object tempVar = idleMembers.clone();
            msg.setDests((java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null));
            _enclosingInstance.sendMulticastMessage(msg, false, Priority.Critical);

        }

        private boolean CheckConnected(Address member) {
            boolean isConnected = false;
            try {
                ConnectionTable ct = _enclosingInstance.ct;
                ConnectionTable.Connection con;
                con = ct.GetPrimaryConnection(member, false);
                if (con != null) {
                    isConnected = con.IsConnected();
                }
            } catch (IOException ex) {
                if (_enclosingInstance.getStack().getCacheLog().getIsInfoEnabled()) {
                        _enclosingInstance.getStack().getCacheLog().Info("CheckConnected()", ex.toString());
                    }
            }
            return isConnected;
        }

        public final void ReceivedHeartBeat(Address sender, HearBeat hrtBeat) {
            Message rspMsg;
            switch (hrtBeat.getType()) {

                case HearBeat.HEART_BEAT:
                    if (_enclosingInstance.getStack().getCacheLog().getIsInfoEnabled()) {
                        _enclosingInstance.getStack().getCacheLog().Info("ConnectionKeepAlive.ReceivedHeartBeat", "received heartbeat from ->:" + sender.toString());
                    }

                    synchronized (_idleConnections) {
                        _idleConnections.remove(sender);
                    }
                    break;

                case HearBeat.SEND_HEART_BEAT:

                    rspMsg = new Message(sender, null, new byte[0]);
                    rspMsg.putHeader(HeaderType.KEEP_ALIVE, new HearBeat(HearBeat.HEART_BEAT));
                    if (_enclosingInstance.getStack().getCacheLog().getIsInfoEnabled()) {
                        _enclosingInstance.getStack().getCacheLog().Info("ConnectionKeepAlive.ReceivedHeartBeat", "seding heartbeat to ->:" + sender.toString());
                    }

                    _enclosingInstance.sendUnicastMessage(rspMsg, false, rspMsg.getPayload(), Priority.Critical);
                    break;

                case HearBeat.ARE_YOU_ALIVE:

                    rspMsg = new Message(sender, null, new byte[0]);

                    HearBeat rsphrtBeat = (_enclosingInstance.isClosing || _enclosingInstance._leaving) ? new HearBeat(HearBeat.I_AM_LEAVING) : new HearBeat(HearBeat.I_AM_NOT_DEAD);
                    rsphrtBeat = _enclosingInstance.isStarting ? new HearBeat(HearBeat.I_AM_STARTING) : rsphrtBeat;
                    rspMsg.putHeader(HeaderType.KEEP_ALIVE, rsphrtBeat);
                    if (_enclosingInstance.getStack().getCacheLog().getIsInfoEnabled()) {
                        _enclosingInstance.getStack().getCacheLog().Info("ConnectionKeepAlive.ReceivedHeartBeat", "seding status" + rsphrtBeat + " to ->:" + sender.toString());
                    }

                    _enclosingInstance.sendUnicastMessage(rspMsg, false, rspMsg.getPayload(), Priority.Critical);
                    break;

                case HearBeat.I_AM_STARTING:
                case HearBeat.I_AM_LEAVING:
                case HearBeat.I_AM_NOT_DEAD:

                    synchronized (_status_mutex) {
                        if (_currentSuspect != null && _currentSuspect.equals(sender)) {
                            _statusReceived = hrtBeat;
                            Monitor.pulse(_status_mutex);//_status_mutex.notify();
                        }
                    }

                    break;

            }
        }
    }

    public static class HearBeat extends Header implements ICompactSerializable {

        private byte _type;

        public static final byte SEND_HEART_BEAT = 1;

        public static final byte HEART_BEAT = 2;

        public static final byte ARE_YOU_ALIVE = 3;

        public static final byte I_AM_NOT_DEAD = 4;

        public static final byte I_AM_LEAVING = 5;

        public static final byte I_AM_STARTING = 6;

        public HearBeat() {
        }

        public HearBeat(byte type) {
            _type = type;
        }

        public final byte getType() {
            return _type;
        }

        public final void setType(byte value) {
            _type = value;
        }

        public final void deserialize(CacheObjectInput reader) throws IOException {
            _type = reader.readByte();
        }

        public final void serialize(CacheObjectOutput writer) throws IOException {
            writer.writeByte(_type);
        }

        @Override
        public String toString() {
            String toString = "NA";
            switch (_type) {
                case SEND_HEART_BEAT:
                    toString = "SEND_HEART_BEAT";
                    break;

                case HEART_BEAT:
                    toString = "HEART_BEAT";
                    break;

                case ARE_YOU_ALIVE:
                    toString = "ARE_YOU_ALIVE";
                    break;

                case I_AM_NOT_DEAD:
                    toString = "I_AM_NOT_DEAD";
                    break;

                case I_AM_LEAVING:
                    toString = "I_AM_LEAVING";
                    break;

                case I_AM_STARTING:
                    toString = "I_AM_STARTING";
                    break;

            }
            return toString;
        }
    }

}
