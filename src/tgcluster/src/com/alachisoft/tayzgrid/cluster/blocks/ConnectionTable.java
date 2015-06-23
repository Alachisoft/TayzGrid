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

import com.alachisoft.tayzgrid.cluster.Event;
import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.cluster.Global;
import com.alachisoft.tayzgrid.cluster.Version;
import com.alachisoft.tayzgrid.cluster.Header;
import com.alachisoft.tayzgrid.cluster.protocols.TCP;
import com.alachisoft.tayzgrid.cluster.util.Util;
import com.alachisoft.tayzgrid.common.enums.Priority;
import com.alachisoft.tayzgrid.common.FlagsByte;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.net.MemoryManager;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.stats.HPTimeStats;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.runtime.exceptions.ConnectionException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import com.alachisoft.tayzgrid.serialization.standard.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import tangible.RefObject;

/**
 * Manages incoming and outgoing TCP connections. For each outgoing message to
 * destination P, if there is not yet a connection for P, one will be created.
 * Subsequent outgoing messages will use this connection. For incoming messages,
 * one server socket is created at startup. For each new incoming client
 * connecting, a new thread from a thread pool is allocated and listens for
 * incoming messages until the socket is closed by the peer.<br>Sockets/threads
 * with no activity will be killed after some time.<br> Incoming messages from
 * any of the sockets can be received by setting the message listener.
 *
 * <author> Bela Ban </author>
 */
public class ConnectionTable {

    /**
     *
     * @deprecated Only to be used for ICompact
     */
    @Deprecated
    public ConnectionTable() {
    }
//: IThreadRunnable

    public Address getLocalAddress() throws UnknownHostException {
        if (local_addr == null) {
            local_addr = bind_addr1 != null ? new Address(bind_addr1, srv_port) : null;
        }
        return local_addr;
    }

    public int getSendBufferSize() {
        return send_buf_size;
    }

    public void setSendBufferSize(int value) {
        this.send_buf_size = value;
    }

    public int getReceiveBufferSize() {
        return recv_buf_size;
    }

    public void setReceiveBufferSize(int value) {
        this.recv_buf_size = value;
    }

    private java.util.HashMap conns_NIC_1 = new java.util.HashMap(); // keys: Addresses (peer address), values: Connection
    private java.util.HashMap secondayrConns_NIC_1 = new java.util.HashMap(); // keys: Addresses (peer address), values: Connection
    private java.util.HashMap conns_NIC_2 = new java.util.HashMap(); // keys: Addresses (peer address), values: Connection
    private java.util.HashMap secondayrConns_NIC_2 = new java.util.HashMap(); // keys: Addresses (peer address), values: Connection
    private java.util.HashMap dedicatedSenders = new java.util.HashMap(); // keys: Addresses (peer address), values: Connection
    private ConnectionTable.Receiver receiver = null;

    private ServerSocket srv_sock1 = null;

    private ServerSocket srv_sock2 = null;
    private InetAddress bind_addr1 = null;
    private InetAddress bind_addr2 = null;
    private Address local_addr = null; // bind_addr + port of srv_sock
    private Address local_addr_s = null; // bind_addr + port of Secondary srv_sock.
    public int srv_port = 7800;
    private boolean stopped;
    private Object newcon_sync_lock = new Object();

    public int port_range = 1;
    private Thread acceptor1 = null; // continuously calls srv_sock.accept()
    private Thread acceptor2 = null; // continuously calls srv_sock.accept()

    private int recv_buf_size = 20000000;
    private int send_buf_size = 640000;
    private java.util.List conn_listeners = Collections.synchronizedList(new java.util.ArrayList(10)); // listeners to be notified when a conn is established/torn down
    private Object recv_mutex = new Object(); // to serialize simultaneous access to receive() from multiple Connections
    private Reaper reaper = null; // closes conns that have been idle for more than n secs
    private long reaper_interval = 60000; // reap unused conns once a minute
    private long conn_expire_time = 300000; // connections can be idle for 5 minutes before they are reaped
    private boolean use_reaper = false; // by default we don't reap idle conns
    private MemoryManager memManager;

    private ReentrantReadWriteLock conn_syn_lock = new ReentrantReadWriteLock();

    private ILogger _ncacheLog;

    public final ILogger getCacheLog() {
        return _ncacheLog;
    }
    public TCP enclosingInstance;
    private boolean useDualConnection = false;
    private Object con_selection_mutex = new Object();
    private boolean _usePrimary = true;
    private boolean enableMonitoring;
    private boolean useDedicatedSender = true;
    private boolean enableNaggling = true;
    private int nagglingSize = 500 * 1024;
    private Object con_reestablish_sync = new Object();
    private java.util.ArrayList _nodeRejoiningList;
    private int _retries;
    private int _retryInterval;
    private int _idGenerator = 0;
    private boolean isInproc;
    /**
     * Used for message reception
     */
    public interface Receiver {

        void receive(Message msg);
    }

    /**
     * Used to be notified about connection establishment and teardown
     */
    public interface ConnectionListener {

        void connectionOpened(Address peer_addr);

        void connectionClosed(Address peer_addr);

        void couldnotConnectTo(Address peer_addr);
    }

    /**
     * Regular ConnectionTable without expiration of idle connections
     *
     * @param srv_port The port on which the server will listen. If this port is
     * reserved, the next free port will be taken (incrementing srv_port).
     * @param NCacheLog
     *
     */
    public ConnectionTable(int srv_port, ILogger NCacheLog) throws ExtSocketException {
        this.srv_port = srv_port;
        this._ncacheLog = NCacheLog;
        start();
    }

    /**
     * ConnectionTable including a connection reaper. Connections that have been
     * idle for more than conn_expire_time milliseconds will be closed and
     * removed from the connection table. On next access they will be
     * re-created.
     *
     * @param srv_port The port on which the server will listen
     *
     * @param reaper_interval Number of milliseconds to wait for reaper between
     * attepts to reap idle connections
     *
     * @param conn_expire_time Number of milliseconds a connection can be idle
     * (no traffic sent or received until it will be reaped
     *
     *
     */
    public ConnectionTable(int srv_port, long reaper_interval, long conn_expire_time, ILogger NCacheLog) throws ExtSocketException {
        this.srv_port = srv_port;
        this.reaper_interval = reaper_interval;
        this.conn_expire_time = conn_expire_time;

        this._ncacheLog = NCacheLog;
        start();
    }

    /**
     * Create a ConnectionTable
     *
     * @param r A reference to a receiver of all messages received by this
     * class. Method <code>receive()</code> will be called.
     *
     * @param bind_addr The host name or IP address of the interface to which
     * the server socket will bind. This is interesting only in multi-homed
     * systems. If bind_addr is null, the server socket will bind to the first
     * available interface (e.g. /dev/hme0 on Solaris or /dev/eth0 on Linux
     * systems).
     *
     * @param srv_port The port to which the server socket will bind to. If this
     * port is reserved, the next free port will be taken (incrementing
     * srv_port).
     *
     *
     */
    public ConnectionTable(ConnectionTable.Receiver r, InetAddress bind_addr1, InetAddress bind_addr2, int srv_port, int port_range, ILogger NCacheLog, int retries, int retryInterval, boolean is_Inproc) throws ExtSocketException {
        setReceiver(r);
        enclosingInstance = (TCP) r;
        this.bind_addr1 = bind_addr1;
        this.bind_addr2 = bind_addr2;
        this.srv_port = srv_port;
        this.port_range = port_range;
        this._ncacheLog = NCacheLog;
        this._retries = retries;
        this._retryInterval = retryInterval;
        this.isInproc = is_Inproc;
        start();
    }

    /**
     * ConnectionTable including a connection reaper. Connections that have been
     * idle for more than conn_expire_time milliseconds will be closed and
     * removed from the connection table. On next access they will be
     * re-created.
     *
     *
     * @param srv_port The port on which the server will listen.If this port is
     * reserved, the next free port will be taken (incrementing srv_port).
     *
     * @param bind_addr The host name or IP address of the interface to which
     * the server socket will bind. This is interesting only in multi-homed
     * systems. If bind_addr is null, the server socket will bind to the first
     * available interface (e.g. /dev/hme0 on Solaris or /dev/eth0 on Linux
     * systems).
     *
     * @param srv_port The port to which the server socket will bind to. If this
     * port is reserved, the next free port will be taken (incrementing
     * srv_port).
     *
     * @param reaper_interval Number of milliseconds to wait for reaper between
     * attepts to reap idle connections
     *
     * @param conn_expire_time Number of milliseconds a connection can be idle
     * (no traffic sent or received until it will be reaped
     *
     *
     */
    public ConnectionTable(ConnectionTable.Receiver r, InetAddress bind_addr, int srv_port, long reaper_interval, long conn_expire_time, ILogger NCacheLog) throws ExtSocketException {
        setReceiver(r);
        this.bind_addr1 = bind_addr;
        this.srv_port = srv_port;
        this.reaper_interval = reaper_interval;
        this.conn_expire_time = conn_expire_time;

        this._ncacheLog = NCacheLog;
        start();
    }

    public void setReceiver(ConnectionTable.Receiver r) {
        receiver = r;
    }

    public void addConnectionListener(ConnectionTable.ConnectionListener l) {
        if (l != null && !conn_listeners.contains(l)) {
            conn_listeners.add(l);
        }
    }

    public void removeConnectionListener(ConnectionTable.ConnectionListener l) {
        if (l != null) {
            conn_listeners.remove(l);
        }
    }

    public final void publishBytesReceivedStats(long byteReceived) {

    }

    public final int GetConnectionId() {
        synchronized (this) {
            return _idGenerator++;
        }
    }

    /**
     * Creates connection with the members with which it has not connected
     * before
     *
     * @param members
     *
     */
    public final java.util.ArrayList synchronzeMembership(java.util.ArrayList members, boolean establishConnectionWithSecondaryNIC) throws InterruptedException {
        java.util.ArrayList failedNodes = new java.util.ArrayList();
        if (members != null) {
            int indexOfLocal = members.indexOf(local_addr);

            final java.util.ArrayList newConList = new java.util.ArrayList();
            for (Iterator it = members.iterator(); it.hasNext();) {
                Address memeber = (Address) it.next();
                try {
                    Connection con = null;
                    if (memeber.equals(local_addr)) {
                        continue;
                    }

                    int indexOfmember = members.indexOf(memeber);

                    if (!conns_NIC_1.containsKey(memeber)) {
                        if (indexOfLocal > indexOfmember) {
                            newConList.add(memeber);

                        }
                    } else {
                        con = (Connection) ((conns_NIC_1.get(memeber) instanceof Connection) ? conns_NIC_1.get(memeber) : null);
                        if (con != null) {
                            con.setIsPartOfCluster(true);
                        }
                    }
                } catch (Exception e) {
                    getCacheLog().Error("ConnectionTable.makeConnection", "member :" + memeber + " Exception:" + e.toString());
                }
            }
            if (newConList.size() > 0) {
                synchronized (newcon_sync_lock) {

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            MakeConnectionAsync(newConList);
                        }
                    }).start();
                    //System.Threading.ThreadPool.QueueUserWorkItem(new WaitCallback(MakeConnectionAsync), newConList);
                    //we wait for two seconds for connection to be established.
                    Monitor.wait(newcon_sync_lock, 2000);//newcon_sync_lock.wait(2000);
                }
            }

            members.remove(local_addr);
            Lock writeLock = null;
            try {
                writeLock = conn_syn_lock.writeLock();
                writeLock.lock();

                java.util.ArrayList leavingMembers = new java.util.ArrayList();
                for (Iterator it = conns_NIC_1.keySet().iterator(); it.hasNext();) {
                    Address oldMember = (Address) it.next();
                    if (!members.contains(oldMember)) {
                        leavingMembers.add(oldMember);
                    }
                }

                Connection con = null;
                for (Iterator it = leavingMembers.iterator(); it.hasNext();) {
                    Address leavingNode = (Address) it.next();
                    con = (Connection) ((conns_NIC_1.get(leavingNode) instanceof Connection) ? conns_NIC_1.get(leavingNode) : null);
                    if (con != null && con.getIsPartOfCluster() && !leavingNode.getIpAddress().equals(local_addr.getIpAddress())) {
                        if (con != null && con.getIsPartOfCluster()) {
                            getCacheLog().Error("ConnectionTable.synchronizeMembership", leavingNode.toString() + " is no more part of the membership");
                            RemoveDedicatedMessageSender(leavingNode);
                            con.DestroySilent();
                            conns_NIC_1.remove(leavingNode);
                        }
                    }
                }
            } catch (Exception e) {
                getCacheLog().Error("ConnectionTable.makeConnection", "destroying connection with member : Exception:" + e.toString());
            } finally {
                writeLock.unlock();

            }

            if (establishConnectionWithSecondaryNIC) {
                try {
                    java.util.HashMap primaryConnections = null;
                    try {
                        writeLock = conn_syn_lock.writeLock();
                        Object tempVar = conns_NIC_1.clone();
                        primaryConnections = (java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null);
                    } finally {
                        writeLock.unlock();
                    }

                    if (primaryConnections != null) {
                        Iterator ide = primaryConnections.entrySet().iterator();
                        while (ide.hasNext()) {
                            Map.Entry pair = (Map.Entry) ide.next();
                            Connection con = (Connection) ((pair.getValue() instanceof Connection) ? pair.getValue() : null);
                            if (!conns_NIC_2.containsKey(con.peer_addr)) {
                                ConnectToPeerOnSecondaryAddress(con);
                            }
                        }
                    }
                } catch (Exception e) {
                    getCacheLog().Error("ConnectionTable.makeConnection", "an error occured while establishing secondary connection. Exception:" + e.toString());
                }

            }

        }
        return failedNodes;
    }

    /**
     * We establish connection asynchronously in a dedictated threadpool thread.
     *
     * @param state
     */
    private void MakeConnectionAsync(Object state) {
        java.util.ArrayList nodeList = (java.util.ArrayList) ((state instanceof java.util.ArrayList) ? state : null);
        java.util.ArrayList failedNodes = new java.util.ArrayList();
        try {
            for (Iterator it = nodeList.iterator(); it.hasNext();) {
                Address member = (Address) it.next();
                if (stopped) {
                    return;
                }
                Connection con = GetConnection(member, null, true, useDualConnection, true);
                if (con == null) {
                    getCacheLog().Error("ConnectionTable.MakeConnectionAsync", "could not establish connection with " + member);
                    failedNodes.add(member);
                } else {
                    con.setIsPartOfCluster(true);
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("ConnectionTable.MakeConnectionAsync", "established connection with " + member);
                    }
                }
            }
        } catch (Exception e) {
            getCacheLog().Error("ConnectionTable.MakeConnectionAsync", " Exception:" + e.toString());
        } finally {
            synchronized (newcon_sync_lock) {
                Monitor.pulse(newcon_sync_lock);//newcon_sync_lock.notifyAll();
            }
        }
    }

    /**
     * Sends a message to a unicast destination. The destination has to be set
     *
     * @param msg The message to send
     *
     * <throws> SocketException Thrown if connection cannot be established
     * </throws>
     * @param reEstablishCon indicate that if connection is not found in
     * connectin table then re-establish the connection or not.
     *
     */
    public long send(Address dest, byte[] msg, boolean reEstablishCon, Object[] userPayload, Priority priority) throws ExtSocketException {
        Connection conn = null;
        long bytesSent = 0;
        if (dest == null) {
            getCacheLog().Error("msg is null or message's destination is null");
            return bytesSent;
        }

        // 1. Try to obtain correct Connection (or create one if not yet existent)
        try {
            conn = GetConnection(dest, reEstablishCon); //getConnection(dest, reEstablishCon,useDualConnection);
            if (conn == null) {

                if (useDedicatedSender) {
                    DedicatedMessageSendManager dmSenderMgr = (DedicatedMessageSendManager) ((dedicatedSenders.get(dest) instanceof DedicatedMessageSendManager) ? dedicatedSenders.get(dest) : null);
                    if (dmSenderMgr != null) {
                        int queueCount = dmSenderMgr.QueueMessage(msg, userPayload, priority);
                        return bytesSent;
                    }
                }
                return bytesSent;
            }
        } catch (SocketException se) {
            if (getCacheLog().getIsErrorEnabled()) {
                getCacheLog().Error("ConnectionTable.GetConnection", se.toString() +" at " +dest.toString());
            }
            for (Object conn_listener : conn_listeners) {
                ((ConnectionTable.ConnectionListener) conn_listener).couldnotConnectTo(dest);
            }

        } catch (Exception ex) {
            if (getCacheLog().getIsErrorEnabled()) {
                getCacheLog().Error("ConnectionTable.GetConnection", "connection to " + dest + " could not be established: " + ex);
            }
            throw new ExtSocketException(ex.toString());
        }

        // 2. Send the message using that connection
        try {
            if (useDedicatedSender) {
                DedicatedMessageSendManager dmSenderMgr = (DedicatedMessageSendManager) ((dedicatedSenders.get(dest) instanceof DedicatedMessageSendManager) ? dedicatedSenders.get(dest) : null);
                if (dmSenderMgr != null) {
                    int queueCount = dmSenderMgr.QueueMessage(msg, userPayload, priority);

                    return bytesSent;
                }
            }

            bytesSent = conn.send(msg, userPayload);

        } catch (Exception ex) {
            if (conn.getNeedReconnect()) {
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("ConnectionTable.send", local_addr + " re-establishing connection with " + dest);
                }

                conn = this.ReEstablishConnection(dest);
                if (conn != null) {
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("ConnectionTable.send", local_addr + " re-established connection successfully with " + dest);
                    }

                    try {
                        bytesSent = conn.send(msg, userPayload);
                        return bytesSent;
                    } catch (Exception e) {
                        getCacheLog().Error("ConnectionTable.send", "send failed after reconnect " + e.toString());
                    }
                } else {
                    getCacheLog().Error("ConnectionTable.send", local_addr + " failed to re-establish connection  with " + dest);
                }
            } else {
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("ConnectionTable.send", local_addr + " need not to re-establish connection with " + dest);
                }
            }

            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("Ct.send", "sending message to " + dest + " failed (ex=" + ex.getClass().getName() + "); removing from connection table");
            }
            throw new ExtSocketException(ex.toString());
        }
        return bytesSent;
    }

    public final java.util.ArrayList GetIdleMembers() {
        java.util.ArrayList idleMembers = new java.util.ArrayList();
        try {
            conn_syn_lock.readLock().lock();

            Iterator ide = conns_NIC_1.entrySet().iterator();
            Connection secondary = null;
            while (ide.hasNext()) {
                Map.Entry pair = (Map.Entry) ide.next();
                secondary = (Connection) ((secondayrConns_NIC_1.get(pair.getKey()) instanceof Connection) ? secondayrConns_NIC_1.get(pair.getKey()) : null);
                if (((Connection) pair.getValue()).IsIdle()) {
                    if (secondary != null) {
                        if (secondary.IsIdle()) {
                            idleMembers.add(pair.getKey());
                        }
                    } else {
                        idleMembers.add(pair.getKey());
                    }
                }
            }

        } finally {
            conn_syn_lock.readLock().unlock();
        }
        return idleMembers;
    }

    public final void SetConnectionsStatus(boolean idle) {
        try {
            conn_syn_lock.readLock().lock();

            Iterator ide = conns_NIC_1.entrySet().iterator();
            while (ide.hasNext()) {
                Map.Entry ent = (Map.Entry) ide.next();
                ((Connection) ent.getValue()).setIsIdle(idle);
            }
            ide = secondayrConns_NIC_1.entrySet().iterator();
            while (ide.hasNext()) {
                Map.Entry ent = (Map.Entry) ide.next();
                ((Connection) ent.getValue()).setIsIdle(idle);
            }
        } finally {
            conn_syn_lock.readLock().unlock();
        }
    }

    /**
     * Gets or sets the memory manager.
     */
    public final MemoryManager getMemManager() {
        return memManager;
    }

    public final void setMemManager(MemoryManager value) {
        memManager = value;
    }

    public final Socket Connect(Address node, boolean withFirstNIC) throws SocketException, IOException {
        Socket sock;
        InetSocketAddress ipRemoteEndPoint = new InetSocketAddress(((Address) node).getIpAddress(), ((Address) node).getPort());
        sock = new Socket();
        sock.setTcpNoDelay(true);
        sock.setSendBufferSize(send_buf_size);
        sock.setReceiveBufferSize(recv_buf_size);

        InetAddress bindAddr = bind_addr1;

        if (!withFirstNIC) {
            if (bind_addr2 != null) {
                bindAddr = bind_addr2;
            }
        }
        try {

            SocketAddress sockadd;
            sockadd = new InetSocketAddress(this.local_addr.getIpAddress(), 0);
            sock.bind(sockadd);

            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("ConnectionTable.Connect", "Opening socket connection with " + ipRemoteEndPoint.toString());
            }
            sock.connect(ipRemoteEndPoint);

        } catch (BindException se) {

            sock = new Socket();

            sock.setTcpNoDelay(true);
            sock.setSendBufferSize(send_buf_size);
            sock.setReceiveBufferSize(recv_buf_size);

            //we do not bind an IP address.
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("ConnectionTable.Connect", "Opening socket connection with " + ipRemoteEndPoint.toString());
            }
            sock.connect(ipRemoteEndPoint);

        }

        Object size = sock.getReceiveBufferSize();

        size = sock.getSendBufferSize();
        int lport = 0;
        if (!sock.isConnected()) {
            getCacheLog().Error("Connection.getConnection()", "can not be connected");
        } else {
            lport = sock.getPort();
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("CONNECTED at local port = " + lport);
            }
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("ConnectionTable.getConnection()", "client port local_port= " + sock.getPort() + "client port remote_port= "
                        + sock.getPort());
            }
        }
        return sock;
    }

    public final void ConnectToPeerOnSecondaryAddress(Connection con_with_NIC_1) throws InterruptedException, IOException, ExtSocketException, Exception {
        if (con_with_NIC_1 != null) {
            Address secondaryAddres = con_with_NIC_1.GetSecondaryAddressofPeer();
            if (secondaryAddres != null) {
                boolean establishConnection = true;
                if (local_addr_s != null) {
                    establishConnection = enclosingInstance.IsJuniorThan(con_with_NIC_1.peer_addr);
                }

                if (establishConnection) {
                    Connection con = GetConnection(secondaryAddres, con_with_NIC_1.peer_addr, true, useDualConnection, false);
                    if (con == null) {
                        getCacheLog().Error("ConnectionTable.ConnectToPeerOnSeconary", "failed to connect with " + con_with_NIC_1.peer_addr + " on second IP " + secondaryAddres);
                    }
                }
            }

        }
    }

    public final Connection GetConnection(Address dest, boolean reEstablish) throws IOException, SocketException  {
        Connection con = null;
        synchronized (con_selection_mutex) {
            if (_usePrimary || !useDualConnection) {
                con = (Connection) ((conns_NIC_1.get(dest) instanceof Connection) ? conns_NIC_1.get(dest) : null);
            } else {
                con = (Connection) ((secondayrConns_NIC_1.get(dest) instanceof Connection) ? secondayrConns_NIC_1.get(dest) : null);
                if (con == null && !reEstablish) {
                    con = (Connection) ((conns_NIC_1.get(dest) instanceof Connection) ? conns_NIC_1.get(dest) : null);
                }
            }

            _usePrimary = !_usePrimary;
        }
        if (con == null && reEstablish) {

            con = GetConnection(dest, null, reEstablish, useDualConnection, true);
        }
        return con;
    }

    /**
     * Gets the primary connection.
     *
     * @param dest
     * @param reEstablish
     * @return
     */
    public final Connection GetPrimaryConnection(Address dest, boolean reEstablish) throws IOException, SocketException  {
        Connection con;
        con = getConnection(dest, null, reEstablish, true, true, true);
        return con;
    }

    protected Connection GetConnection(Address dest, Address primaryAddress, boolean reEstablish, boolean getDualConnection, boolean withFirstNIC) throws IOException , SocketException{
        Connection con;
        con = getConnection(dest, primaryAddress, reEstablish, true, withFirstNIC, true);
        if (con != null && getDualConnection && reEstablish) {
            getConnection(dest, primaryAddress, reEstablish, false, withFirstNIC, true);
        }
        return con;
    }

    protected Connection GetConnection(Address dest, Address primaryAddress, boolean reEstablish) {
        Connection con;
        con = getConnection(dest, primaryAddress, reEstablish, true);
        return con;
    }

    /**
     * Try to obtain correct Connection (or create one if not yet existent)
     * @param dest
     */
    protected Connection getConnection(Address dest, Address primaryAddress, boolean reEstablishCon, boolean isPrimary, boolean withFirstNIC, boolean connectingFirstTime) throws IOException, SocketException {
        Connection conn = null;
        Socket sock;
        try {
            if (primaryAddress == null) {
                primaryAddress = dest;
            }
            if (withFirstNIC) {
                if (isPrimary) {
                    conn = (Connection) conns_NIC_1.get(dest);
                } else {
                    conn = (Connection) secondayrConns_NIC_1.get(dest);
                }
            } else {
                if (isPrimary) {
                    conn = (Connection) conns_NIC_2.get(primaryAddress);
                } else {
                    conn = (Connection) secondayrConns_NIC_2.get(primaryAddress);
                }
            }

            if ((conn == null || !connectingFirstTime) && reEstablishCon) {

                try {
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("ConnectionTable.getConnection()", "No Connetion was found found with " + dest.toString());
                    }
                    if (local_addr == null) { //cluster being stopped.
                        return null;
                    }

                    sock = Connect(dest, withFirstNIC);
                    getCacheLog().CriticalInfo("ConnectionTable.Run", "sock.isConnected = " + sock.isConnected());

                    conn = new Connection(this, sock, primaryAddress, this.getCacheLog(), isPrimary, nagglingSize, _retries, _retryInterval);
                    conn.setMemoryManager(getMemManager());
                    conn.setIamInitiater(true);
                    ConnectInfo conInfo = null;
                    try {

                        byte connectStatus = connectingFirstTime ? ConnectInfo.CONNECT_FIRST_TIME : ConnectInfo.RECONNECTING;

                        conn.sendLocalAddress(local_addr, connectingFirstTime);

                        if (((Address) local_addr).compareTo((Address) dest) > 0) {
                            conInfo = new ConnectInfo(connectStatus, GetConnectionId());
                            if (getCacheLog().getIsInfoEnabled()) {
                                getCacheLog().Info("ConnectionTable.getConnection", dest + " I should send connect_info");
                            }
                            conn.SendConnectInfo(conInfo);
                        } else {
                            conInfo = conn.ReadConnectInfo(sock);
                        }

                        conn.setConInfo(conInfo);
                    } catch (Exception e) {
                        getCacheLog().Error("ConnectionTable.getConnection()", e.getMessage());
                        conn.DestroySilent();
                        return null;
                    }
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("ConnectionTable.getConnection", "b4 lock conns.SyncRoot");
                    }
                    try {
                        conn_syn_lock.writeLock().lock();
                        if (isPrimary) {
                            if (withFirstNIC) {
                                if (conns_NIC_1.containsKey(dest)) {
                                    if (getCacheLog().getIsInfoEnabled()) {
                                        getCacheLog().Info("ConnectionTable.getConnection()", "connection is already in the table");
                                    }
                                    Connection tmpConn = (Connection) conns_NIC_1.get(dest);

                                    if (getCacheLog().getIsInfoEnabled()) {
                                        getCacheLog().Info("ConnectionTable.getConnection", "table_con id :" + tmpConn.getConInfo().getId() + " new_con id :"
                                                + conn.getConInfo().getId());
                                    }
                                    if (conn.getConInfo().getId() < tmpConn.getConInfo().getId()) {
                                        conn.Destroy();
                                        return tmpConn;
                                    } else {
                                        if (getCacheLog().getIsInfoEnabled()) {
                                            getCacheLog().Info("ConnectionTable.getConnection()", dest + "--->connection present in the table is terminated");
                                        }

                                        tmpConn.Destroy();
                                        conns_NIC_1.remove(dest);
                                    }
                                }

                                notifyConnectionOpened(dest);
                            } else {
                                if (conns_NIC_2.containsKey(primaryAddress)) {
                                    if (getCacheLog().getIsInfoEnabled()) {
                                        getCacheLog().Info("ConnectionTable.getConnection()", "connection is already in the table");
                                    }
                                    Connection tmpConn = (Connection) conns_NIC_1.get(dest);

                                    if (conn.getConInfo().getId() < tmpConn.getConInfo().getId()) {
                                        conn.Destroy();
                                        return tmpConn;
                                    } else {
                                        getCacheLog().Warn("ConnectionTable.getConnection()", dest + "connection present in the table is terminated");

                                        tmpConn.Destroy();
                                        conns_NIC_2.remove(primaryAddress);
                                    }
                                }
                            }
                        }
                        addConnection(primaryAddress, conn, isPrimary, withFirstNIC);
                        if (useDedicatedSender) {
                            AddDedicatedMessageSender(primaryAddress, conn, withFirstNIC);
                        }
                        conn.init();
                    } finally {
                        conn_syn_lock.writeLock().unlock();
                    }
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("ConnectionTable.getConnection", "after lock conns.SyncRoot");
                    }

                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("ConnectionTable.getConnection()", "connection is now working");
                    }
                } catch (SocketException se) {
                    getCacheLog().Warn("ConnectionTable.getConnection()", "connection is now working");
                    getCacheLog().DevTrace(se.toString()+ " at "+ dest.toString());
                    throw se;
                } catch (IOException ex) {
                    getCacheLog().Warn("ConnectionTable.getConnection()", "connection is now working");
                    getCacheLog().DevTrace(ex.toString()+ " at "+ dest.toString());
                    throw ex;
                }
            }
        } catch (SocketException se) {
            getCacheLog().Warn("ConnectionTable.getConnection()", "connection is now working");
            getCacheLog().DevTrace(se.toString()+ " at "+ dest.toString());
            throw se;
        } catch (IOException ex) {
            getCacheLog().Warn("ConnectionTable.getConnection()", "connection is now working");
            getCacheLog().DevTrace(ex.toString()+ " at "+ dest.toString());
            throw ex;
        } catch (Exception ex) {
            if (conn != null) {
                conn.Destroy();
            }
            conn = null;
        } finally {
        }
        return conn;
    }

    protected Connection getConnection(Address dest, Address primaryAddress, boolean reEstablishCon, boolean connectingFirstTime) {
        Connection conn = null;
        Socket sock;
        try {
            if (primaryAddress == null) {
                primaryAddress = dest;
            }

            conn = (Connection) conns_NIC_1.get(dest);

            if ((conn == null || !connectingFirstTime) && reEstablishCon) {

                try {
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("ConnectionTable.getConnection()", "No Connetion was found found with " + dest.toString());
                    }
                    if (local_addr == null) { //cluster being stopped.
                        return null;
                    }

                    sock = Connect(dest, true);

                    conn = new Connection(this, sock, primaryAddress, this.getCacheLog(), true, nagglingSize, 0, 0);
                    conn.setMemoryManager(getMemManager());
                    conn.setIamInitiater(true);
                    ConnectInfo conInfo = null;
                    try {
                        byte connectStatus = connectingFirstTime ? ConnectInfo.CONNECT_FIRST_TIME : ConnectInfo.RECONNECTING;

                        conn.sendLocalAddress(local_addr, connectingFirstTime);

                        if (((Address) local_addr).compareTo((Address) dest) > 0) {
                            conInfo = new ConnectInfo(connectStatus, GetConnectionId());
                            if (getCacheLog().getIsInfoEnabled()) {
                                getCacheLog().Info("ConnectionTable.getConnection", dest + " I should send connect_info");
                            }
                            conn.SendConnectInfo(conInfo);
                        } else {
                            conInfo = conn.ReadConnectInfo(sock);
                        }

                        conn.setConInfo(conInfo);
                    } catch (Exception e) {
                        getCacheLog().Error("ConnectionTable.getConnection()", e.getMessage());
                        conn.DestroySilent();
                        return null;
                    }

                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("ConnectionTable.getConnection", "b4 lock conns.SyncRoot");
                    }

                    try {
                        conn_syn_lock.writeLock().lock();

                        if (conns_NIC_1.containsKey(dest)) {
                            if (getCacheLog().getIsInfoEnabled()) {
                                getCacheLog().Info("ConnectionTable.getConnection()", "connection is already in the table");
                            }
                            Connection tmpConn = (Connection) conns_NIC_1.get(dest);

                            if (getCacheLog().getIsInfoEnabled()) {
                                getCacheLog().Info("ConnectionTable.getConnection", "table_con id :" + tmpConn.getConInfo().getId() + " new_con id :" + conn.getConInfo().getId());
                            }
                            if (conn.getConInfo().getId() < tmpConn.getConInfo().getId()) {
                                conn.Destroy();
                                return tmpConn;
                            } else {
                                if (getCacheLog().getIsInfoEnabled()) {
                                    getCacheLog().Info("ConnectionTable.getConnection()", dest + "--->connection present in the table is terminated");
                                }

                                tmpConn.Destroy();
                                conns_NIC_1.remove(dest);
                            }
                        }

                        notifyConnectionOpened(dest);

                        addConnection(primaryAddress, conn);
                        if (useDedicatedSender) {
                            AddDedicatedMessageSender(primaryAddress, conn, true);
                        }
                        conn.init();
                    } finally {
                        conn_syn_lock.writeLock().unlock();
                    }
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("ConnectionTable.getConnection", "after lock conns.SyncRoot");
                    }

                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("ConnectionTable.getConnection()", "connection is now working");
                    }
                } finally {
                }
            }
        } catch (Exception e) {

            if (conn != null) {
                conn.Destroy();
            }
            conn = null;
        } finally {
        }
        return conn;
    }

    /**
     * Re-Establishes the connection to a node in case an already existing
     * connection is broken disgracefully.
     *
     * @param addr
     * @return Connection
     */
    public final Connection ReEstablishConnection(Address addr) {
        Connection con = null;
        try {
            if (addr == null) {
                return null;
            }
            conn_syn_lock.writeLock().lock();
            con = (Connection) ((conns_NIC_1.get(addr) instanceof Connection) ? conns_NIC_1.get(addr) : null);

            //Another thread might have been able to re-establish the connnection.
            if (con != null && con.IsConnected()) {
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("ConnectionTable.ReEstablishConnection", "already re-established connection with " + addr);
                }
                return con;
            }

            con = getConnection(addr, null, true, true, true, false); //con = null;

        } catch (Exception ex) {
            con = null;
            getCacheLog().Error("ConnectionTable.ReEstablishConnection", "failed to re-establish connection with " + addr + " " + ex.toString());
        } finally {
            conn_syn_lock.writeLock().unlock();
        }

        return con;
    }

    protected final void AddDedicatedMessageSender(Address primaryAddress, Connection con, boolean onPrimaryNIC) //#else
    {
        if (con != null) {
            synchronized (dedicatedSenders) {
                DedicatedMessageSendManager dmSenderManager = (DedicatedMessageSendManager) ((dedicatedSenders.get(primaryAddress) instanceof DedicatedMessageSendManager) ? dedicatedSenders.get(primaryAddress) : null);
                if (dmSenderManager == null) {
                    dmSenderManager = new DedicatedMessageSendManager(_ncacheLog);
                    dedicatedSenders.put(primaryAddress, dmSenderManager);
                    dmSenderManager.AddDedicatedSenderThread(con, onPrimaryNIC, enableNaggling, nagglingSize);
                } else {
                    dmSenderManager.UpdateConnection(con);
                }
            }
        }
    }

    protected final void RemoveDedicatedMessageSender(Address node) {
        if (node != null) {
            synchronized (dedicatedSenders) {
                DedicatedMessageSendManager dmSenderManager = (DedicatedMessageSendManager) ((dedicatedSenders.get(node) instanceof DedicatedMessageSendManager) ? dedicatedSenders.get(node) : null);
                if (dmSenderManager != null) {
                    dedicatedSenders.remove(node);
                    dmSenderManager.dispose();
                }
            }
        }
    }

    private void StopDedicatedSenders() {
        synchronized (dedicatedSenders) {
            if (dedicatedSenders != null) {
                Iterator ide = dedicatedSenders.entrySet().iterator();
                while (ide.hasNext()) {
                    Map.Entry ent = (Map.Entry) ide.next();
                    DedicatedMessageSendManager dmSenderManager = (DedicatedMessageSendManager) ((ent.getValue() instanceof DedicatedMessageSendManager) ? ent.getValue() : null);
                    if (dmSenderManager != null) {
                        dmSenderManager.dispose();
                    }
                }
                dedicatedSenders.clear();
            }
        }
    }

    public void start() throws ExtSocketException {
        srv_sock1 = createServerSocket(bind_addr1, srv_port);

        if (bind_addr2 != null) {
            srv_sock2 = createServerSocket(bind_addr2, 0);
        }

        if (srv_sock1 == null) {
            throw new ExtSocketException("Cluster can not be started on the given server port. The port might be already in use.");
        }
        if (bind_addr1 != null) {
            local_addr = new Address(bind_addr1, srv_sock1.getLocalPort());
        } else {
            local_addr = new Address(srv_sock1.getInetAddress(), srv_sock1.getLocalPort());
        }
        if (srv_sock2 != null) {
            local_addr_s = new Address(bind_addr2, srv_sock2.getLocalPort());
        }

        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("ConnectionTable.start", "server socket created on " + local_addr);
        }
        if (ServicePropValues.CacheServer_EnableDebuggingCounters != null) {
            enableMonitoring = Boolean.parseBoolean(ServicePropValues.CacheServer_EnableDebuggingCounters);
        }
        if (ServicePropValues.CacheServer_EnableDualSocket != null) {
            useDualConnection = Boolean.parseBoolean(ServicePropValues.CacheServer_EnableDualSocket);
        }
        if (ServicePropValues.CacheServer_EnableNagling != null) {
            enableNaggling = Boolean.parseBoolean(ServicePropValues.CacheServer_EnableNagling);
        }
        if (ServicePropValues.CacheServer_NaglingSize != null) {
            nagglingSize = Integer.parseInt(ServicePropValues.CacheServer_NaglingSize) * 1024;
        }
        //Roland Kurmann 4/7/2003, build new thread group
        //Roland Kurmann 4/7/2003, put in thread_group
        //acceptor1 = new Thread(new ThreadStart(this.RunPrimary)); //, "ConnectionTable.AcceptorThread_p");
        acceptor1 = new Thread(new Runnable() {

            @Override
            public void run() {
                RunPrimary();
            }
        });
        acceptor1.setName("ConnectionTable.AcceptorThread_p");
        
        acceptor1.start();

        getCacheLog().CriticalInfo("ConnectionTable.Start", "operating parameters -> [bind_addr :" + local_addr + " ; dual_socket: " + useDualConnection + " ; nagling: "
                + enableNaggling + " ; nagling_size : " + nagglingSize + " ]");

        // start the connection reaper - will periodically remove unused connections
        if (use_reaper && reaper == null) {
            reaper = new Reaper(this);
            reaper.start();
        }
    }

    /**
     * Closes all open sockets, the server socket and all threads waiting for
     * incoming messages
     */
    public void stop() {
        stopped = true;
        java.util.Iterator it = null;
        Connection conn;
        ServerSocket tmp;
        if (disconThread != null) {
            //Flush: Buffer Appender can clear all Logs as reported by this thread
            getCacheLog().Flush();

            disconThread.stop();
            disconThread = null;
        }
        // 1. close the server socket (this also stops the acceptor thread)
        if (srv_sock1 != null) {
            try {
                tmp = srv_sock1;
                srv_sock1 = null;
                tmp.close();
            } catch (Exception e) {
            }
        }

        if (srv_sock2 != null) {
            try {
                tmp = srv_sock2;
                srv_sock2 = null;
                tmp.close();
            } catch (Exception e2) {
            }
        }

        local_addr = null;

        //2. Stop dedicated senders
        StopDedicatedSenders();

        // 3. then close the connections
        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("ConnectionTable.stop", "b4 lock conns.SyncRoot");
        }
        try {
            conn_syn_lock.writeLock().lock();

            Object tempVar = conns_NIC_1.clone();
            java.util.HashMap connsCopy = (java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null);
            it = connsCopy.values().iterator();

            while (it.hasNext()) {
                conn = (Connection) it.next();
                conn.SendLeaveNotification();

                conn.Destroy();
            }
            conns_NIC_1.clear();

            Object tempVar2 = secondayrConns_NIC_1.clone();
            connsCopy = (java.util.HashMap) ((tempVar2 instanceof java.util.HashMap) ? tempVar2 : null);
            it = connsCopy.values().iterator();

            while (it.hasNext()) {
                conn = (Connection) it.next();
                conn.DestroySilent();
            }
            secondayrConns_NIC_1.clear();

            Object tempVar3 = conns_NIC_2.clone();
            connsCopy = (java.util.HashMap) ((tempVar3 instanceof java.util.HashMap) ? tempVar3 : null);
            it = connsCopy.values().iterator();

            while (it.hasNext()) {
                conn = (Connection) it.next();
                conn.SendLeaveNotification();

                conn.Destroy();
            }
            conns_NIC_2.clear();

            Object tempVar4 = secondayrConns_NIC_2.clone();
            connsCopy = (java.util.HashMap) ((tempVar4 instanceof java.util.HashMap) ? tempVar4 : null);
            it = connsCopy.values().iterator();

            while (it.hasNext()) {
                conn = (Connection) it.next();
                conn.DestroySilent();
            }
            secondayrConns_NIC_2.clear();

        } finally {
            conn_syn_lock.writeLock().unlock();
        }
        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("ConnectionTable.stop", "after lock conns.SyncRoot");
        }
    }

    /**
     * Remove <code>addr</code>from connection table. This is typically
     * triggered when a member is suspected.
     *
     */
    public void remove(Address addr, boolean isPrimary) {

        Connection conn;
        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("ConnectionTable.remove", "b4 lock conns.SyncRoot");
        }
        try {
            conn_syn_lock.writeLock().lock();
            if (isPrimary) {
                conn = (Connection) conns_NIC_1.get(addr);

                if (conn != null) {
                    try {

                        conn.Destroy(); // won't do anything if already destroyed
                    } catch (Exception e) {
                    }
                    conns_NIC_1.remove(addr);
                }
            }

            conn = (Connection) secondayrConns_NIC_1.get(addr);

            if (conn != null) {
                try {

                    conn.Destroy(); // won't do anything if already destroyed
                } catch (Exception e2) {
                }
                secondayrConns_NIC_1.remove(addr);
            }

            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("addr=" + addr + ",   connections are " + toString());
            }
        } finally {
            conn_syn_lock.writeLock().unlock();
        }
        RemoveDedicatedMessageSender(addr);
        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("ConnectionTable.remove", "after lock conns.SyncRoot");
        }
    }
    private Thread disconThread;

    public final void ConfigureNodeRejoining(java.util.ArrayList list) {

        _nodeRejoiningList = list;

        boolean simulate = false;
        String str = ServicePropValues.CacheServer_SimulateSocketClose;

        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(str)) {
            try {
                simulate = Boolean.parseBoolean(str);
            } catch (Exception e) {
            }
        }

        if (simulate && disconThread == null) {

            disconThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    Disconnect();
                }
            });
            disconThread.start();
        }

    }

    private void Disconnect() {
        int interval = 60;

        getCacheLog().CriticalInfo("ConnectionTable.Disconnect", "simulating sudden disconnect " + Thread.currentThread().getId());

        String str = ServicePropValues.CacheServer_SocketCloseInterval;
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(str)) {
            try {
                interval = Integer.parseInt(str);
                if (interval < 20) {
                    interval = 20;
                }
            } catch (Exception e) {
            }
        }

        getCacheLog().CriticalInfo("ConnectionTable.Disconnect", "socket close interval :" + interval + " seconds");
        while (true) {
            try {
                java.util.Random random = new java.util.Random(10);

                int randomInterval = 3 + (random.nextInt(12 - 3));

                Thread.sleep(new TimeSpan(0, 0, (interval + randomInterval)).getTotalTicks());
                getCacheLog().CriticalInfo("ConnectionTable.Disconnect", "poling interval :" + (interval + randomInterval));
                if (_nodeRejoiningList != null && _nodeRejoiningList.size() > 0) {
                    int nextNode = -1;
                    if (_nodeRejoiningList.size() == 2) {
                        if (_nodeRejoiningList.get(0).equals(local_addr)) {
                            nextNode = 1;
                        }
                    } else {
                        for (int i = 0; i < _nodeRejoiningList.size(); i++) {
                            if (_nodeRejoiningList.get(i).equals(local_addr)) {
                                nextNode = i + 1;
                                if (nextNode == _nodeRejoiningList.size()) {
                                    nextNode = 0;
                                }
                                break;
                            }
                        }
                    }
                    if (nextNode != -1) {
                        Connection con = GetConnection((Address) ((_nodeRejoiningList.get(nextNode) instanceof Address) ? _nodeRejoiningList.get(nextNode) : null), false);
                        if (con != null) {
                            getCacheLog().CriticalInfo("ConnectionTable.Disconnect", "going to disconnect with " + con.peer_addr);
                            con.markedClose = true;
                            con.sock.close();
                        }
                    }
                }
            } catch (Exception e) {
                getCacheLog().CriticalInfo("ConnetionTable.Disconnect", e.toString());
            }
        }
    }

    private void InformConnectionClose(Address node) {
        if (!stopped) {
            Event evt = new Event(Event.CONNECTION_BREAKAGE, node, Priority.Critical);
            enclosingInstance.passUp(evt);
        }
    }

    private void InformConnectionReestablishment(Address node) {
        if (!stopped) {
            Event evt = new Event(Event.CONNECTION_RE_ESTABLISHED, node, Priority.Critical);
            enclosingInstance.passUp(evt);
        }
    }

    public final Connection Reconnect(final Address node) {
        Connection peerConnection = null;
        boolean shouldConnect = false;
        boolean initiateReconnection = false;

        if (node == null) {
            getCacheLog().Error("ConnectionTable.Reconnect", "node name is NULL");
            return null;
        }

        synchronized (con_reestablish_sync) {
            try {
                if (_nodeRejoiningList != null) {
                    synchronized (_nodeRejoiningList) {
                        int localNodeIndex = -1;
                        int nodeIndex = -1;
                        for (int i = 0; i < _nodeRejoiningList.size(); i++) {
                            Address listNode = (Address) ((_nodeRejoiningList.get(i) instanceof Address) ? _nodeRejoiningList.get(i) : null);
                            if (listNode.equals(node)) {
                                nodeIndex = i;
                            }
                            if (listNode.equals(getLocalAddress())) {
                                localNodeIndex = i;
                            }
                        }
                        if (nodeIndex >= 0 && localNodeIndex >= 0) {
                            shouldConnect = true;
                            if (nodeIndex > localNodeIndex) {
                                initiateReconnection = true;
                            }
                        }
                    }
                }
                if (shouldConnect) {
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("ConnectionTable.Reconnect", node.toString() + " is part of node rejoining list");
                    }
                    int connectionRetries = _retries;

                    while (connectionRetries-- > 0) {
                        peerConnection = ReEstablishConnection(node);
                        if (peerConnection == null) {
                            Thread.sleep(_retryInterval);
                        } else {
                            break;
                        }
                    }
                    if (peerConnection == null) {
                        if (getCacheLog().getIsErrorEnabled()) {
                            getCacheLog().Error("ConnectionTable.Reconnect", "Can not establish connection with " + node + " after " + _retries + " retries");
                        }
                        notifyConnectionClosed(node);
                    } else {
                        getCacheLog().CriticalInfo("ConnectionTable.Reconnect", "Connection re-establised with " + node);

                        if (peerConnection.IamInitiater()) {
                            //inform above layers about re-connection.
                            Thread workerThread = new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    InformAboutReconnection(node);
                                }
                            });
                            workerThread.start();

                        }
                    }
                } else {
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("ConnectionTable.Reconnect", node.toString() + " is not part of the node rejoining list");
                    }
                    notifyConnectionClosed(node);
                }
            } catch (Exception e) {
                getCacheLog().Error("ConnectionTable.Reconnect", "An error occured while reconnecting with " + node + " Error :" + e.toString());
                notifyConnectionClosed(node);
            }
        }

        return peerConnection;
    }

    private void InformAboutReconnection(Object state) {
        Address node = (Address) ((state instanceof Address) ? state : null);

        try {
            enclosingInstance.passUp(new Event(Event.NODE_REJOINING, node, com.alachisoft.tayzgrid.common.enums.Priority.Critical));
        } catch (Exception e) {

            getCacheLog().Error("ConnectionTable.InformAboutReconnection", e.toString());
        }
    }

    public final void RunPrimary() {
        Run(new Object[]{
            srv_sock1, true
        });
    }

    public final void RunSecondary() {
        Run(new Object[]{
            srv_sock2, false
        });
    }

    /**
     * Acceptor thread. Continuously accept new connections. Create a new thread
     * for each new connection and put it in conns. When the thread should stop,
     * it is interrupted by the thread creator.
     */
    public void Run(Object arg) {
        Object[] objArr = (Object[]) ((arg instanceof Object[]) ? arg : null);
        ServerSocket listener = (ServerSocket) ((objArr[0] instanceof ServerSocket) ? objArr[0] : null);
        boolean isPrimaryListener = (Boolean) objArr[1];

        Socket client_sock;
        Connection conn = null;
        Address peer_addr = null;

        while (listener != null) {
            try {
                client_sock = listener.accept();
                int cport = client_sock.getPort();

                //Console.WriteLine("------------------------------**********************-----------------");
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("ConnectionTable.Run()", "CONNECTION ACCPETED Remote port = " + cport);
                }

                client_sock.setTcpNoDelay(true);

                client_sock.setSendBufferSize(send_buf_size);
                client_sock.setReceiveBufferSize(recv_buf_size);

                // create new thread and add to conn table
                conn = new Connection(this, client_sock, null, _ncacheLog, true, nagglingSize, _retries, _retryInterval); // will call receive(msg)
                // get peer's address
                tangible.RefObject<Address> tempRef_peer_addr = new tangible.RefObject<Address>(peer_addr);
                boolean connectingFirstTime = conn.readPeerAddress(client_sock, tempRef_peer_addr);
                peer_addr = tempRef_peer_addr.argvalue;
                ConnectInfo conInfo = null;
                if (((Address) local_addr).compareTo((Address) peer_addr) > 0) {
                    conInfo = new ConnectInfo(ConnectInfo.CONNECT_FIRST_TIME, GetConnectionId());
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("ConnectionTable.Run", peer_addr + " I should send connect_info");
                    }
                    conn.SendConnectInfo(conInfo);
                } else {
                    conInfo = conn.ReadConnectInfo(client_sock);
                }

                conn.setConInfo(conInfo);
                conn.getConInfo().setConnectStatus(connectingFirstTime ? ConnectInfo.CONNECT_FIRST_TIME : ConnectInfo.RECONNECTING);

                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("ConnectionTable.Run()", "Read peer address " + peer_addr.toString() + "at port" + cport);
                }

                conn.setPeerAddress(peer_addr);

                if (conInfo.getConnectStatus() == ConnectInfo.RECONNECTING) {
                    //if other node is reconnecting then we should check for its member ship first.
                    boolean ismember = enclosingInstance.IsMember(peer_addr);
                    if (!ismember) {
                        getCacheLog().CriticalInfo("ConnectionTable.Run", "ConnectionTable.Run" + peer_addr + " has connected. but it is no more part of the membership");

                        conn.SendLeaveNotification();
                        Thread.sleep(1000L); //just to make sure that peer node receives the leave notification.
                        conn.Destroy();
                        continue;
                    }
                }

                boolean isPrimary = true;
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("ConnectionTable.run", "b4 lock conns.SyncRoot");
                }
                try {
                    conn_syn_lock.writeLock().lock();

                    if (isPrimaryListener) {

                        if (conns_NIC_1.containsKey(peer_addr)) {
                            if (!secondayrConns_NIC_1.containsKey(peer_addr) && useDualConnection) {
                                secondayrConns_NIC_1.put(peer_addr, conn);
                                isPrimary = false;

                            } else {

                                Connection tmpConn = (Connection) conns_NIC_1.get(peer_addr);

                                if (conn.getConInfo().getId() < tmpConn.getConInfo().getId() && conn.getConInfo().getConnectStatus() != ConnectInfo.CONNECT_FIRST_TIME) {

                                    getCacheLog().CriticalInfo("ConnectionTable.Run", "1. Destroying Connection (conn.ConInfo.Id < tmpConn.ConInfo.Id)"
                                            + (new Integer(conn.getConInfo().getId())).toString() + ":" + (new Integer(tmpConn.getConInfo().getId())).toString()
                                            + conn.toString());

                                    conn.Destroy();
                                    continue;
                                } else {
                                    if (getCacheLog().getIsInfoEnabled()) {
                                        getCacheLog().Info("ConnectionTable.Run()", "-->connection present in the talble is terminated");
                                    }

                                    tmpConn.Destroy();
                                    conns_NIC_1.remove(peer_addr);
                                }
                            }
                        }
                    } else {

                        if (conns_NIC_2.containsKey(peer_addr)) {
                            if (!secondayrConns_NIC_2.containsKey(peer_addr) && useDualConnection) {
                                secondayrConns_NIC_2.put(peer_addr, conn);
                                isPrimary = false;
                            } else {
                                if (getCacheLog().getIsInfoEnabled()) {
                                    getCacheLog().Info("ConnectionTable.Run()", "connection alrady exists in the table");
                                }
                                Connection tmpConn = (Connection) conns_NIC_2.get(peer_addr);

                                if (conn.getConInfo().getId() < tmpConn.getConInfo().getId()) {

                                    conn.Destroy();
                                    continue;
                                } else {

                                    getCacheLog().Error("ConnectionTable.Run()", "connection present in the talble is terminated");
                                    tmpConn.Destroy();
                                    conns_NIC_2.remove(peer_addr);
                                }
                            }
                        }
                    }
                    conn.setIsPrimary(isPrimary);
                    addConnection(peer_addr, conn, isPrimary, isPrimaryListener);

                    conn.setMemoryManager(memManager);

                    if (useDedicatedSender) {
                        AddDedicatedMessageSender(conn.peer_addr, conn, isPrimaryListener);
                    }

                    conn.init(); // starts handler thread on this socket

                } finally {

                    conn_syn_lock.writeLock().unlock();
                }
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("ConnectionTable.run", "after lock conns.SyncRoot");
                }

                if (isPrimary && isPrimaryListener) {
                    notifyConnectionOpened(peer_addr);
                }

                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("ConnectionTable.Run()", "connection working now");
                }
            } catch (ExtSocketException sock_ex) {
                getCacheLog().Error("ConnectionTable.Run", "exception is " + sock_ex);

                if (conn != null) {
                    conn.DestroySilent();
                }
                if (srv_sock1 == null) {
                    break; // socket was closed, therefore stop
                }
            } catch (Exception ex) {
                getCacheLog().Error("ConnectionTable.Run", "exception is " + ex);

                if (srv_sock1 == null) {
                    break; // socket was closed, therefore stop
                }

            } finally {

            }
        }
    }

    /**
     * Calls the receiver callback. We serialize access to this method because
     * it may be called concurrently by several Connection handler threads.
     * Therefore the receiver doesn't need to synchronize.
     */
    public void receive(Message msg) {
        if (receiver != null) {

            receiver.receive(msg);

        } else {
            getCacheLog().Error("receiver is null (not set) !");
        }
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        Address key;
        Connection val;

        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("ConnectionTable.ToString", "b4 lock conns.SyncRoot");
        }
        try {
            conn_syn_lock.readLock().lock();

            ret.append("connections (" + conns_NIC_1.size() + "):\n");
            for (java.util.Iterator e = conns_NIC_1.keySet().iterator(); e.hasNext();) {
                key = (Address) e.next();
                val = (Connection) conns_NIC_1.get(key);
                ret.append("key: " + key.toString() + ": " + val.toString() + '\n');
            }
        } finally {
            conn_syn_lock.readLock().unlock();
        }
        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("ConnectionTable.ToString", "b4 lock conns.SyncRoot");
        }
        ret.append('\n');
        return ret.toString();
    }

    public boolean ConnectionExist(Address member) {
        return conns_NIC_1 != null ? conns_NIC_1.containsKey(member) : false;
    }

    /**
     * Finds first available port starting at start_port and returns server
     * socket. Sets srv_port
     */
    protected ServerSocket createServerSocket(InetAddress bind_addr, int start_port) {
        ServerSocket ret = null;

        if(!isInproc)
            port_range =1;
        for (int i = 1; i <= port_range; i++) {
            try {
                if (bind_addr == null) {
                    ServerSocket temp_tcpListener;
                    temp_tcpListener = new ServerSocket(start_port);

                    ret = temp_tcpListener;
                } else {
                    ServerSocket temp_tcpListener2 = new ServerSocket();

                    temp_tcpListener2.bind(new InetSocketAddress(bind_addr, start_port));

                    ret = temp_tcpListener2;
                }
            } catch (BindException bind_ex) {

                start_port = start_port+6;
                ret = null;
                continue;
            } catch (IOException io_ex) {
                getCacheLog().Error("exception is " + io_ex);
                ret = null;
            }
            srv_port = start_port;
            break;
        }
        if (ret == null) {
            getCacheLog().Error("ConnectionTable.createServerSocket", "binding failed " + bind_addr == null ? "null" : bind_addr.toString() + " is not valid");
        }
        return ret;
    }

    public void notifyConnectionOpened(Address peer) {
        if (peer == null) {
            return;
        }
        for (int i = 0; i < conn_listeners.size(); i++) {
            ((ConnectionTable.ConnectionListener) conn_listeners.get(i)).connectionOpened(peer);
        }
    }

    public void notifyConnectionClosed(Address peer) {
        getCacheLog().CriticalInfo("ConnectionTable.notifyConnectionClosed", peer.toString() + " connection close notification");
        if (peer == null) {
            return;
        }
        for (int i = 0; i < conn_listeners.size(); i++) {
            ((ConnectionTable.ConnectionListener) conn_listeners.get(i)).connectionClosed(peer);
        }
    }

    public void addConnection(Address peer, Connection c, boolean isPrimary, boolean fromNIC1) {
        if (fromNIC1) {
            if (isPrimary) {
                conns_NIC_1.put(peer, c);
            } else {
                secondayrConns_NIC_1.put(peer, c);
            }
        } else {
            if (isPrimary) {
                conns_NIC_2.put(peer, c);
            } else {
                secondayrConns_NIC_2.put(peer, c);
            }
        }

        if (reaper != null && !reaper.isRunning()) {
            reaper.start();
        }
        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("ConnectionTable.addConnection", "Connection added to the table");
        }
    }

    public void addConnection(Address peer, Connection c) {
        conns_NIC_1.put(peer, c);

        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("ConnectionTable.addConnection", "Connection added to the table");
        }
    }

    public class Connection implements Runnable {

        /**
         *
         * @deprecated Only to be used for ICompact
         */
        @Deprecated
        public Connection() {
        }

        private ConnectionTable enclosingInstance;

        private void InitBlock(ConnectionTable enclosingInstance) {
            this.enclosingInstance = enclosingInstance;
        }

        public void setPeerAddress(Address value) {
            this.peer_addr = value;
        }

        public ConnectionTable getEnclosing_Instance() {
            return enclosingInstance;
        }
        public Socket sock = null; // socket to/from peer (result of srv_sock.accept() or new Socket())

        public Thread handler = null; // thread for receiving messages
        public Address peer_addr = null; // address of the 'other end' of the connection
        public Object send_mutex = new Object(); // serialize sends

        public long last_access = System.currentTimeMillis();

        public boolean self_close = false;

        public ByteArrayOutputStream inStream = new ByteArrayOutputStream(8000);

        private MemoryManager memManager;
        private boolean _isIdle = false;
        private boolean leavingGracefully = false;
        private boolean socket_error = false;
        private boolean isConnected = true;
        final long sendBufferSize = 1024 * 1024;
        final long receiveBufferSize = 1024 * 1024;
        private byte[] sendBuffer = new byte[(int) sendBufferSize];
        private byte[] receiveBuffer = null;

        private ILogger _ncacheLog;

        public ILogger getCacheLog() {
            return _ncacheLog;
        }

        final int LARGE_OBJECT_SIZE = 79 * 1024;
        public Socket _secondarySock;

        public boolean _isPrimary;

        Object get_addr_sync = new Object();
        Address secondaryAddress;
        Object initializationPhase_mutex = new Object();
        boolean inInitializationPhase = false;

        private int _retries;
        private int _retryInterval;

        boolean isMember;
        public boolean markedClose;
        private ConnectInfo conInfo;
        private boolean iaminitiater;
        private TimeSpan _worsRecvTime = new TimeSpan(0, 0, 0);
        private TimeSpan _worsSendTime = new TimeSpan(0, 0, 0);

        public Connection(ConnectionTable enclosingInstance, Socket s, Address peer_addr, ILogger Log, boolean isPrimary, int naglingSize, int retries, int retryInterval) {
            InitBlock(enclosingInstance);
            sock = s;
            this.peer_addr = peer_addr;
            this._retries = retries;
            this._retryInterval = retryInterval;
            this._ncacheLog = Log;

            _isPrimary = isPrimary;

            if (naglingSize > receiveBufferSize) {
                receiveBuffer = new byte[naglingSize + 8];
            } else {
                receiveBuffer = new byte[(int) receiveBufferSize];
            }
        }

        public ConnectInfo getConInfo() {
            return conInfo;
        }

        public void setConInfo(ConnectInfo value) {
            conInfo = value;
        }

        /**
         * Gets/Sets the flag which indicates that whether this node remained
         * part of the cluster at any time or not.
         */
        public boolean getIsPartOfCluster() {
            return isMember;
        }

        public void setIsPartOfCluster(boolean value) {
            isMember = value;
        }

        public boolean IamInitiater() {
            return iaminitiater;
        }

        public void setIamInitiater(boolean value) {
            iaminitiater = value;
        }

        public boolean IsPrimary() {
            return _isPrimary;
        }

        public void setIsPrimary(boolean value) {
            _isPrimary = value;
        }

        public boolean IsIdle() {
            return _isIdle;
        }

        public void setIsIdle(boolean value) {
            synchronized (send_mutex) {
                _isIdle = value;
            }
        }

        public boolean IsConnected() {
            return isConnected;
        }

        public void setIsConnected(boolean value) {
            isConnected = value;
        }

        public boolean established() {
            return handler != null;
        }

        public MemoryManager getMemManager() {
            return memManager;
        }

        public void setMemoryManager(MemoryManager value) {
            memManager = value;
        }

        public void updateLastAccessed() {

            last_access = System.currentTimeMillis();
        }

        public boolean getNeedReconnect() {
            return (!leavingGracefully && !self_close);
        }

        public void init() {
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("connection was created to " + peer_addr);
            }
            if (handler == null) {

                // Roland Kurmann 4/7/2003, put in thread_group
                handler = new Thread(this);
                handler.setName("ConnectionTable.Connection.HandlerThread");
                handler.setDaemon(true);
                handler.start();
            }

        }

        public void ConnectionDestructionSimulator() throws InterruptedException {
            Thread.sleep(2000);
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("ConnectionDestructionSimulator", "BREAKING THE CONNECTION WITH " + peer_addr);
            }
            Destroy();
        }

        public void Destroy() {

            closeSocket(); // should terminate handler as well
            if (handler != null && handler.isAlive()) {
                try {

                    getCacheLog().Flush();
                    handler.interrupt();
                } catch (Exception e) {
                }
            }
            getCacheLog().DevTrace("Connection.Destroy", "set handler = null");

            handler = null;
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException ex) {
                    if (getCacheLog().getIsErrorEnabled()) {
                        getCacheLog().Error("Connection.Destroy()", ex.toString());
                    }
                }
            }

        }

        public void DestroySilent() {
            DestroySilent(true);
        }

        public void DestroySilent(boolean sendNotification) {
            synchronized (send_mutex) { // we intentionally close the connection. no need to suspect for such close
                this.self_close = true;
            }
            if (IsConnected()) { //Inform the peer about closing the socket.
                SendSilentCloseNotification();
            }

            Destroy();
        }

        /**
         * Sends the notification to the peer that connection is being closed
         * silently.
         */
        private void SendSilentCloseNotification() {
            self_close = true;
            ConnectionHeader header = new ConnectionHeader(ConnectionHeader.CLOSE_SILENT);
            Message closeMsg = new Message(peer_addr, null, new byte[0]);
            closeMsg.putHeader("ConnectionHeader", header);
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("Connection.SendSilentCloseNotification", "sending silent close request");
            }
            try {

                byte[] binaryMsg = Util.serializeMessage(closeMsg);

                SendInternal(binaryMsg);
            } catch (Exception e) {
                getCacheLog().Error("Connection.SendSilentCloseNotification", e.toString());
            }

        }

        public boolean AreUinInitializationPhase() {
            self_close = true;
            ConnectionHeader header = new ConnectionHeader(ConnectionHeader.ARE_U_IN_INITIALIZATION_PHASE);
            Message closeMsg = new Message(peer_addr, null, new byte[0]);
            closeMsg.putHeader("ConnectionHeader", header);
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("Connection.SendSilentCloseNotification", "sending silent close request");
            }
            try {
                synchronized (initializationPhase_mutex) {

                    byte[] binaryMsg = Util.serializeMessage(closeMsg);
                    SendInternal(binaryMsg);
                    Monitor.wait(initializationPhase_mutex, 1000); //initializationPhase_mutex.wait(1000);
                    return inInitializationPhase;
                }
            } catch (Exception e) {
                getCacheLog().Error("Connection.SendSilentCloseNotification", e.toString());
            }
            return false;
        }

        public boolean SendInitializationPhaseRsp(boolean initializationPhase) {
            self_close = true;
            ConnectionHeader header = new ConnectionHeader(ConnectionHeader.INITIALIZATION_PHASE_RSP);
            header.setInitializationPhase(initializationPhase);
            Message closeMsg = new Message(peer_addr, null, new byte[0]);
            closeMsg.putHeader("ConnectionHeader", header);
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("Connection.SendSilentCloseNotification", "sending silent close request");
            }
            try {
                synchronized (initializationPhase_mutex) {

                    byte[] binaryMsg = Util.serializeMessage(closeMsg);
                    SendInternal(binaryMsg);
                    Monitor.wait(initializationPhase_mutex); //initializationPhase_mutex.wait();
                    return inInitializationPhase;
                }
            } catch (Exception e) {
                getCacheLog().Error("Connection.SendSilentCloseNotification", e.toString());
            }
            return false;
        }

        /**
         * Used to send the internal messages of the connection.
         *
         * @param binaryMsg
         */
        private void SendInternal(byte[] binaryMsg) throws ExtSocketException, Exception {
            if (binaryMsg != null) {

                byte[] buf = new byte[binaryMsg.length + 8];

                byte[] lenBuf = Util.WriteInt32(binaryMsg.length + 4);
                System.arraycopy(lenBuf, 0, buf, 0, 4);

                byte[] msgCount = Util.WriteInt32(1);
                System.arraycopy(msgCount, 0, buf, 4, msgCount.length);
                System.arraycopy(binaryMsg, 0, buf, 8, binaryMsg.length);
                send(buf, null);
            }
        }

        /**
         * Sends notification to other node about leaving.
         */
        public void SendLeaveNotification() {
            leavingGracefully = true;
            ConnectionHeader header = new ConnectionHeader(ConnectionHeader.LEAVE);
            Message leaveMsg = new Message(peer_addr, null, new byte[0]);
            leaveMsg.putHeader("ConnectionHeader", header);
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("Connection.SendSilentCloseNotification", "sending leave request");
            }
            try {

                byte[] binaryMsg = Util.serializeMessage(leaveMsg);
                SendInternal(binaryMsg);
            } catch (Exception e) {
                getCacheLog().Error("Connection.SendLeaveNotification", e.toString());
            }
        }

        public long send(byte[] msg, Object[] userPayload) throws ExtSocketException, Exception {
            return send(msg, userPayload, msg.length);
        }

        public long send(byte[] msg, Object[] userPayload, int bytesToSent) throws ExtSocketException, Exception {
            long bytesSent = 0;
            try {
                HPTimeStats socketSendTimeStats = null;
                if (enclosingInstance.enableMonitoring) {
                    socketSendTimeStats = new HPTimeStats();
                    // socketSendTimeStats.BeginSample();
                }
                bytesSent = doSend(msg, userPayload, bytesToSent);
                if (socketSendTimeStats != null) {

                }

            } catch (SocketException sock_exc) {
                synchronized (send_mutex) {
                    socket_error = true;
                    isConnected = false;
                }
                throw new ExtSocketException(sock_exc.getMessage());
            } catch (Exception ex) {
                getCacheLog().Error("exception is " + ex);
                throw ex;
            }
            return bytesSent;
        }

        public long doSend(byte[] msg, Object[] userPayload, int bytesToSent) throws Exception {
            long bytesSent = 0;

            Address dst_addr = (Address) peer_addr;

            byte[] buffie = null;

            if (dst_addr == null || dst_addr.getIpAddress() == null) {
                getCacheLog().Error("the destination address is null; aborting send");
                return bytesSent;
            }

            try {

                // we're using 'double-writes', sending the buffer to the destination in 2 pieces. this would
                // ensure that, if the peer closed the connection while we were idle, we would get an exception.
                // this won't happen if we use a single write (see Stevens, ch. 5.13).
                //if(nTrace.getIsInfoEnabled()) NCacheLog.Info("ConnectionTable.Connection.doSend()"," before writing to out put stream");
                if (sock != null) {

                    bytesSent = AssureSend(msg, userPayload, bytesToSent);

                }
            } catch (Exception ex) {
                synchronized (send_mutex) {
                    socket_error = true;
                    isConnected = false;
                }
                getCacheLog().Error(getEnclosing_Instance().local_addr + "to " + dst_addr + ",   exception is " + ex);

                throw ex;
            }
            return bytesSent;
        }

        private long AssureSend(byte[] buffer, Object[] userPayLoad, int bytesToSent) throws IOException {
            int totalDataLength = 0;

            synchronized (send_mutex) {
                int count = buffer.length;
                int bytesCopied = 0;
                int mainIndex = 0;

                totalDataLength += bytesToSent;

                if (userPayLoad == null) {
                    AssureSend(buffer, bytesToSent);
                } else {
                    while (bytesCopied < buffer.length) {
                        count = buffer.length - bytesCopied;
                        if (count > sendBuffer.length - mainIndex) {
                            count = sendBuffer.length - mainIndex;
                        }

                        System.arraycopy(buffer, bytesCopied, sendBuffer, mainIndex, count);

                        bytesCopied += count;
                        mainIndex += count;

                        if (mainIndex >= sendBuffer.length) {
                            AssureSend(sendBuffer, sendBuffer.length);
                            mainIndex = 0;
                        }

                    }

                    if (userPayLoad != null && userPayLoad.length > 0) {
                        for (int i = 0; i < userPayLoad.length; i++) {
                            Object tempVar = userPayLoad[i];

                            buffer = (byte[]) ((tempVar instanceof byte[]) ? tempVar : null);
                            bytesCopied = 0;
                            totalDataLength += buffer.length;

                            while (bytesCopied < buffer.length) {
                                count = buffer.length - bytesCopied;
                                if (count > sendBuffer.length - mainIndex) {
                                    count = sendBuffer.length - mainIndex;
                                }

                                System.arraycopy(buffer, bytesCopied, sendBuffer, mainIndex, count);
                                bytesCopied += count;
                                mainIndex += count;

                                if (mainIndex >= sendBuffer.length) {
                                    AssureSend(sendBuffer, sendBuffer.length);
                                    mainIndex = 0;
                                }
                            }

                            if (mainIndex >= sendBuffer.length) {
                                AssureSend(sendBuffer, sendBuffer.length);
                                mainIndex = 0;
                            }
                        }
                        if (mainIndex >= 0) {
                            AssureSend(sendBuffer, mainIndex);
                            mainIndex = 0;
                        }
                    } else {
                        AssureSend(sendBuffer, mainIndex);
                    }
                }
            }

            return totalDataLength;
        }

        private void AssureSend(byte[] buffer, int count) throws IOException {
            int bytesSent = 0;
            int noOfChunks = 0;
            java.util.Date startTime = new java.util.Date(0);
            synchronized (send_mutex) {

                startTime = new java.util.Date();

                try {
                    _isIdle = false;
                    noOfChunks++;
                    sock.getOutputStream().write(buffer, bytesSent, count - bytesSent);
                    sock.getOutputStream().flush();
                } catch (SocketException e) {

                    throw e;
                }

            }
        }

        /**
         * Reads the peer's address. First a cookie has to be sent which has to
         * match my own cookie, otherwise the connection will be refused
         *
         * @param client_sock
         * @throws ClassNotFoundException
         */
        public boolean readPeerAddress(Socket client_sock, RefObject<Address> peer_addr) throws ExtSocketException, IOException, ClassNotFoundException {
            //Address peer_addr = null;
            ConnectInfo info = null;

            byte[] version, buf; //, input_cookie = new byte[getEnclosing_Instance().cookie.Length];
            int len = 0;
            boolean connectingFirstTime = false;
            if (sock != null) {

                version = new byte[Version.version_id.length];
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("ConnectionTable.connection.readpeerAdress", "before reading from socket");
                }
                Util.ReadInput(sock, version, 0, version.length);
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("ConnectionTable.connection.readpeerAdress", "after reading from socket");
                }
                if (Version.compareTo(version) == false) {

                    getCacheLog().Warn("Cookie version is different");

                }

                byte[] lenBuff = new byte[4];
                Util.ReadInput(sock, lenBuff, 0, lenBuff.length);

                len = Util.convertToInt32(lenBuff);

                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("Connection.readPeerAddress()", "Address length = " + len);
                }
                // finally read the address itself
                buf = new byte[len];
                Util.ReadInput(sock, buf, 0, len);
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("ConnectionTable.connection.readpeerAdress", "before deserialization of adress");
                }
                Object[] args = (Object[]) CompactBinaryFormatter.fromByteBuffer(buf, "");
                peer_addr.argvalue = (Address) ((args[0] instanceof Address) ? args[0] : null);
                connectingFirstTime = (Boolean) args[1];
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("ConnectionTable.connection.readpeerAdress", "after deserialization of adress");
                }
                updateLastAccessed();
            }
            return connectingFirstTime;
        }

        /**
         * Send the cookie first, then the our port number. If the cookie
         * doesn't match the receiver's cookie, the receiver will reject the
         * connection and close it.
         */
        public void sendLocalAddress(Address local_addr, boolean connectingFirstTime) throws Exception {

            byte[] buf;

            if (local_addr == null) {
                getCacheLog().Warn("local_addr is null");
                throw new Exception("local address is null");
            }
            if (sock != null) {
                try {
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("Connection.sendLocaladress", "b4 serializing...");
                    }
                    Object[] objArray = new Object[]{
                        local_addr, connectingFirstTime
                    };
                    buf = CompactBinaryFormatter.toByteBuffer(objArray, "");
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("Connection.sendLocaladress", "after serializing...");
                    }

                    sock.getOutputStream().write(Version.version_id);

                    byte[] lenBuff;
                    lenBuff = Util.WriteInt32(buf.length);
                    sock.getOutputStream().write(lenBuff);

                    // and finally write the buffer itself
                    sock.getOutputStream().write(buf);
                    sock.getOutputStream().flush();
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("Connection.sendLocaladress", "after sending...");
                    }
                    //out_Renamed.Flush(); // needed ?
                    updateLastAccessed();
                } catch (Exception t) {
                    getCacheLog().Error("exception is " + t);
                    throw t;
                }
            }
        }

        /**
         * Reads the peer's address. First a cookie has to be sent which has to
         * match my own cookie, otherwise the connection will be refused
         */
        public ConnectInfo ReadConnectInfo(Socket client_sock) throws ExtSocketException, IOException, ClassNotFoundException {
            ConnectInfo info = null;

            byte[] version, buf; //, input_cookie = new byte[getEnclosing_Instance().cookie.Length];
            int len = 0;
            //				int len = 0, client_port = client_sock != null?client_sock.Port:0;
            //				System.Net.IPAddress client_addr = client_sock != null?client_sock.IPAddress:null;

            if (sock != null) {

                version = new byte[Version.version_id.length];
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("ConnectionTable.connection.readpeerAdress", "before reading from socket");
                }
                Util.ReadInput(sock, version, 0, version.length);
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("ConnectionTable.connection.readpeerAdress", "after reading from socket");
                }
                if (Version.compareTo(version) == false) {

                    getCacheLog().Warn("Cookie version is different");

                    getCacheLog().Error("Cookie version is different");
                    throw new ExtSocketException("ConnectionTable.Connection.readPeerAddress(): cookie sent by " + peer_addr + " does not match own cookie; terminating connection");

                }

                byte[] lenBuff = new byte[4];
                Util.ReadInput(sock, lenBuff, 0, lenBuff.length);

                len = Util.convertToInt32(lenBuff);

                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("Connection.readPeerAddress()", "Address length = " + len);
                }
                // finally read the address itself
                buf = new byte[len];
                Util.ReadInput(sock, buf, 0, len);
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("ConnectionTable.connection.readpeerAdress", "before deserialization of adress");
                }
                info = (ConnectInfo) CompactBinaryFormatter.fromByteBuffer(buf, "");
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("ConnectionTable.connection.readpeerAdress", "after deserialization of adress");
                }
                updateLastAccessed();
            }
            return info;
        }

        /**
         * Send the cookie first, then the our port number. If the cookie
         * doesn't match the receiver's cookie, the receiver will reject the
         * connection and close it.
         */
        public void SendConnectInfo(ConnectInfo info) throws Exception {

            byte[] buf;

            if (sock != null) {
                try {
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("Connection.sendLocaladress", "b4 serializing...");
                    }
                    buf = CompactBinaryFormatter.toByteBuffer(info, "");
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("Connection.sendLocaladress", "after serializing...");
                    }

                    sock.getOutputStream().write(Version.version_id);

                    byte[] lenBuff;
                    lenBuff = Util.WriteInt32(buf.length);
                    sock.getOutputStream().write(lenBuff);

                    // and finally write the buffer itself
                    sock.getOutputStream().write(buf);
                    sock.getOutputStream().flush();
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("Connection.sendLocaladress", "after sending...");
                    }
                    //out_Renamed.Flush(); // needed ?
                    updateLastAccessed();
                } catch (Exception t) {
                    getCacheLog().Error("exception is " + t);
                    throw t;
                }
            }
        }

        public String printCookie(byte[] c) {
            if (c == null) {
                return "";
            }
            return new String(Global.ToCharArray(c));
        }

        @Override
        public void run() {
            Message msg = null;
            byte[] buf = null;
            int len = 0;
            while (handler != null) {
                ByteArrayOutputStream stmIn = null;
                ObjectInput msgReader = null;
                try {
                    if (sock == null) {
                        getCacheLog().Error("input stream is null !");
                        //Console.WriteLine("Socket is Null");
                        break;
                    }

                    byte[] lenBuff = new byte[4];
                    buf = null;

                    Util.ReadInput(sock, lenBuff, 0, lenBuff.length);

                    len = Util.convertToInt32(lenBuff);

                    buf = receiveBuffer;
                    if (len > receiveBuffer.length) {
                        receiveBuffer = buf = new byte[len];
                    }

//                    HPTimeStats socketReceiveTimeStats = null;
//                    if (enclosingInstance.enableMonitoring) {
//                        socketReceiveTimeStats = new HPTimeStats();
//                        socketReceiveTimeStats.BeginSample();
//                    }
                    
                    int recLength = Util.ReadInput(sock, buf, 0, len);
//               if (socketReceiveTimeStats != null) {
//                    }

                    enclosingInstance.publishBytesReceivedStats(len + 4);

                    if (recLength == len) {
                        int noOfMessages = Util.convertToInt32(buf, 0);
                        int messageBaseIndex = 4;
                        for (int msgCount = 0; msgCount < noOfMessages; msgCount++) {
                            int totalMessagelength = Util.convertToInt32(buf, messageBaseIndex);
                            int messageLength = Util.convertToInt32(buf, messageBaseIndex + 4);

                            stmIn = new ByteArrayOutputStream();

                            stmIn.write(buf, messageBaseIndex + 8, messageLength);

                            msgReader = new ObjectInputStream(new ByteArrayInputStream(stmIn.toByteArray()), "");

                            FlagsByte flags = new FlagsByte();
                            flags.setDataByte((byte) msgReader.read());

                            if (flags.AnyOn(FlagsByte.Flag.TRANS)) {
                                Message tmpMsg = new Message();
                                tmpMsg.DeserializeLocal(msgReader);
                                msg = tmpMsg;
                            } else {

                                msg = (Message) msgReader.readObject();
                            }

                            if (msg != null) {
                                int payLoadLength = totalMessagelength - messageLength - 4;
                                if (payLoadLength > 0) {

                                    int noOfChunks = payLoadLength / LARGE_OBJECT_SIZE;
                                    noOfChunks += (payLoadLength - (noOfChunks * LARGE_OBJECT_SIZE)) != 0 ? 1 : 0;
                                    Object[] payload = new Object[noOfChunks];

                                    int nextChunk = 0;
                                    int nextChunkSize = 0;
                                    int startIndex = messageBaseIndex + 8 + messageLength;

                                    for (int i = 0; i < noOfChunks; i++) {
                                        nextChunkSize = payLoadLength - nextChunk;
                                        if (nextChunkSize > LARGE_OBJECT_SIZE) {
                                            nextChunkSize = LARGE_OBJECT_SIZE;
                                        }

                                        byte[] binaryChunk = new byte[nextChunkSize];
                                        System.arraycopy(buf, startIndex, binaryChunk, 0, nextChunkSize);
                                        nextChunk += nextChunkSize;
                                        startIndex += nextChunkSize;

                                        payload[i] = binaryChunk;

                                    }

                                    msg.setPayload(payload);

                                }
                                messageBaseIndex += (totalMessagelength + 4);
                                Header tempVar2 = msg.getHeader("ConnectionHeader");
                                ConnectionHeader hdr = (ConnectionHeader) ((tempVar2 instanceof ConnectionHeader) ? tempVar2 : null);
                                if (hdr != null) {
                                    switch (hdr.getType()) {
                                        case ConnectionHeader.CLOSE_SILENT:

                                            if (getCacheLog().getIsInfoEnabled()) {
                                                getCacheLog().Info("Connection.Run", "connection being closed silently");
                                            }
                                            this.self_close = true;
                                            handler = null;
                                            continue;

                                        case ConnectionHeader.LEAVE:
                                            //The node is leaving the cluster gracefully.
                                            leavingGracefully = true;
                                            if (getCacheLog().getIsInfoEnabled()) {
                                                getCacheLog().Info("Connection.Run", peer_addr.toString() + " is leaving gracefully");
                                            }
                                            handler = null;
                                            continue;

                                        case ConnectionHeader.GET_SECOND_ADDRESS_REQ:
                                            SendSecondaryAddressofPeer();
                                            continue;

                                        case ConnectionHeader.GET_SECOND_ADDRESS_RSP:
                                            synchronized (get_addr_sync) {
                                                secondaryAddress = hdr.MySecondaryAddress();
                                                Monitor.pulse(get_addr_sync);// get_addr_sync.notifyAll();
                                            }
                                            continue;

                                        case ConnectionHeader.ARE_U_IN_INITIALIZATION_PHASE:
                                            try {
                                                boolean iMinInitializationPhase = !enclosingInstance.enclosingInstance.getStack().getIsOperational();
                                                SendInitializationPhaseRsp(iMinInitializationPhase);
                                            } catch (Exception e) {
                                            }
                                            break;

                                        case ConnectionHeader.INITIALIZATION_PHASE_RSP:
                                            synchronized (initializationPhase_mutex) {
                                                inInitializationPhase = hdr.getInitializationPhase();
                                                Monitor.pulse(initializationPhase_mutex);// initializationPhase_mutex.notifyAll();
                                            }
                                            break;
                                    }
                                }
                            }
                            msg.setSrc(peer_addr);

                            msg.MarkArrived();
                            getEnclosing_Instance().receive(msg); // calls receiver.receiver(msg)
                        }

                    }
                } catch (InterruptedException e5) {
                    getCacheLog().DevTrace("Connection", "Interrupted Exception = " + e5.getMessage());
                    synchronized (send_mutex) {
                        socket_error = true;
                        isConnected = false;
                    }
                    break;

                } catch (OutOfMemoryError memExc) {
                    getCacheLog().DevTrace("Connection", "OutofMemory Exception = " + memExc.getMessage());
                    synchronized (send_mutex) {
                        isConnected = false;
                    }
                    getCacheLog().CriticalInfo("Connection.Run()", getEnclosing_Instance().local_addr + "-->" + peer_addr.toString() + " memory exception " + memExc.toString());
                    break; // continue;
                } catch (ExtSocketException sock_exp) {
                    getCacheLog().DevTrace("Connection", "SocketException = " + sock_exp.getMessage());
                    synchronized (send_mutex) {
                        socket_error = true;
                        isConnected = false;
                    }
                    // peer closed connection
                    getCacheLog().Error("Connection.Run()", getEnclosing_Instance().local_addr + "-->" + peer_addr.toString() + " exception is " + sock_exp.getMessage());
                    break;
                } catch (IOException eof_ex) {
                    //IOException occurs on socket breakage
                    getCacheLog().DevTrace("Connection", "IOException = " + eof_ex.getMessage());
                    synchronized (send_mutex) {
                        socket_error = true;
                        isConnected = false;
                    }
                    // peer closed connection
                    getCacheLog().Error("Connection.Run()", "data :" + len + getEnclosing_Instance().local_addr + "-->" + peer_addr.toString() + " exception is " + eof_ex);

                    break;
                } catch (IllegalArgumentException ex) {
                    getCacheLog().DevTrace("Connection", "IllegalArgument Exception = " + ex.getMessage());
                    synchronized (send_mutex) {
                        isConnected = false;
                    }
                    break;
                } catch (Exception e) {
                    getCacheLog().DevTrace("Connection", "Plain old Exception = " + e.getMessage());
                    synchronized (send_mutex) {
                        isConnected = false;
                    }
                    getCacheLog().Error("Connection.Run()", getEnclosing_Instance().local_addr + "-->" + peer_addr.toString() + " exception is " + e);
                    break;
                } finally {
                    if (stmIn != null) {
                        try {
                            stmIn.close();
                        } catch (IOException ex) {
                            if (getCacheLog().getIsErrorEnabled()) {
                                getCacheLog().Error("Connection.Run()", ex.toString());
                            }
                        }
                    }
                    if (msgReader != null) {
                        try {
                            msgReader.close();
                        } catch (IOException ex) {
                            if (getCacheLog().getIsErrorEnabled()) {
                                getCacheLog().Error("Connection.Run()", ex.toString());
                            }
                        }
                    }
                }
            }

            handler = null;

            if (LeavingGracefully()) {
                getCacheLog().DevTrace("Connection", "Leaving Gracefully = " + Boolean.toString(LeavingGracefully()));

                enclosingInstance.notifyConnectionClosed(peer_addr);
                enclosingInstance.remove(peer_addr, IsPrimary());
            }
        }

        public void HandleRequest(Object state) {
            getEnclosing_Instance().receive((Message) state);
        }

        public boolean IsSelfClosing() {
            return self_close;
        }

        public boolean LeavingGracefully() {
            return leavingGracefully;
        }

        public boolean IsSocketError() {
            return socket_error;
        }

        public Address GetSecondaryAddressofPeer() throws InterruptedException, IOException, ExtSocketException, Exception {
            Connection.ConnectionHeader header = new ConnectionHeader(ConnectionHeader.GET_SECOND_ADDRESS_REQ);
            Message msg = new Message(peer_addr, null, new byte[0]);
            msg.putHeader("ConnectionHeader", header);
            synchronized (get_addr_sync) {
                SendInternal(Util.serializeMessage(msg));
                Monitor.wait(get_addr_sync);// get_addr_sync.wait();
            }
            return secondaryAddress;
        }

        public void SendSecondaryAddressofPeer() throws ExtSocketException, Exception {
            Address secondaryAddress = null;
            Connection.ConnectionHeader header = new ConnectionHeader(ConnectionHeader.GET_SECOND_ADDRESS_RSP);
            header.setMySecondaryAddress(enclosingInstance.local_addr_s);

            Message msg = new Message(peer_addr, null, new byte[0]);
            msg.putHeader("ConnectionHeader", header);
            getCacheLog().Error("Connection.SendSecondaryAddress", "secondaryAddr: " + header.MySecondaryAddress());
            SendInternal(Util.serializeMessage(msg));

        }

        @Override
        public String toString() {
            StringBuilder ret = new StringBuilder();

            if (sock == null) {
                ret.append("<null socket>");
            } else {
                ret.append("<" + this.peer_addr.toString() + ">");
            }

            return ret.toString();
        }

        public void closeSocket() {
            if (sock != null) {
                try {
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("Connection.closeSocket()", "client port local_port= " + sock.getPort() + "client port remote_port= "
                                + sock.getPort());
                    }

                    sock.close(); // should actually close in/out (so we don't need to close them explicitly)
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("Connection.closeSocket()", "connection destroyed");
                    }
                } catch (Exception e) {

                }
                sock = null;
            }

        }

        /**
         *
         */
        public class ConnectionHeader extends Header implements ICompactSerializable {

            public static final int CLOSE_SILENT = 1;
            public static final int LEAVE = 2;
            public static final int GET_SECOND_ADDRESS_REQ = 3;
            public static final int GET_SECOND_ADDRESS_RSP = 4;
            public static final int ARE_U_IN_INITIALIZATION_PHASE = 5;
            public static final int INITIALIZATION_PHASE_RSP = 6;
            int _type;
            Address _secondaryAddress;
            boolean initializationPhase;

            /**
             *
             * @deprecated Only to be used for ICompact
             */
            @Deprecated
            public ConnectionHeader() {
            }

            public ConnectionHeader(int type) {
                _type = type;
            }

            public int getType() {
                return _type;
            }

            public boolean getInitializationPhase() {
                return initializationPhase;
            }

            public void setInitializationPhase(boolean value) {
                initializationPhase = value;
            }

            public Address MySecondaryAddress() {
                return _secondaryAddress;
            }

            public void setMySecondaryAddress(Address value) {
                _secondaryAddress = value;
            }

            @Override
            public String toString() {
                return "ConnectionHeader Type : " + _type;
            }

            //<editor-fold defaultstate="collapsed" desc="ICompactSerializable Members">
            public void deserialize(CacheObjectInput reader) throws IOException, ClassNotFoundException {
                _type = reader.readInt();
                Object tempVar3 = reader.readObject();
                _secondaryAddress = (Address) ((tempVar3 instanceof Address) ? tempVar3 : null);
                initializationPhase = reader.readBoolean();
            }

            public void serialize(CacheObjectOutput writer) throws IOException {
                writer.writeInt(_type);
                writer.writeObject(_secondaryAddress);
                writer.writeBoolean(initializationPhase);
            }
            //</editor-fold>
        }
    }

    public class Reaper implements Runnable {

        private void InitBlock(ConnectionTable enclosingInstance) {
            this.enclosingInstance = enclosingInstance;
        }
        private ConnectionTable enclosingInstance;

        public boolean isRunning() {
            return t != null;
        }

        public ConnectionTable getEnclosing_Instance() {
            return enclosingInstance;
        }
        public Thread t = null;

        private String _cacheName;

        public Reaper(ConnectionTable enclosingInstance) {
            InitBlock(enclosingInstance);
        }

        public void start() {
            if (getEnclosing_Instance().conns_NIC_1.size() == 0) {
                return;
            }
            if (t != null && !t.isAlive()) {
                t = null;
            }
            if (t == null) {
                //RKU 7.4.2003, put in threadgroup
                t = new Thread(this);
                t.setName("ConnectionTable.ReaperThread");
                t.setDaemon(true); // will allow us to terminate if all remaining threads are daemons
                t.start();
            }
        }

        public void stop() {
            if (t != null) {
                t = null;
            }
        }

        public void run() {
            Connection value_Renamed;
            java.util.Map.Entry entry;
            long curr_time;
            java.util.ArrayList temp = new java.util.ArrayList();

            if (enclosingInstance.getCacheLog().getIsInfoEnabled()) {
                enclosingInstance.getCacheLog().Info("connection reaper thread was started. Number of connections=" + getEnclosing_Instance().conns_NIC_1.size() + ", reaper_interval="
                        + getEnclosing_Instance().reaper_interval + ", conn_expire_time=" + getEnclosing_Instance().conn_expire_time);
            }

            while (getEnclosing_Instance().conns_NIC_1.size() > 0 && t != null) {
                try {
                    // first sleep
                    Util.sleep(getEnclosing_Instance().reaper_interval);
                } catch (InterruptedException ex) {
                    if (enclosingInstance.getCacheLog().getIsErrorEnabled()) {
                        enclosingInstance.getCacheLog().Error("ConnectionTable.Reaper", ex.toString());
                    }
                }

                if (enclosingInstance.getCacheLog().getIsInfoEnabled()) {
                    enclosingInstance.getCacheLog().Info("ConnectionTable.Reaper", "b4 lock conns.SyncRoot");
                }

                //: changing Ticks to System.currentTimeinMillis
                //curr_time = (new java.util.Date().Ticks - 621355968000000000) / 10000;
                curr_time = System.currentTimeMillis();
                for (java.util.Iterator it = getEnclosing_Instance().conns_NIC_1.entrySet().iterator(); it.hasNext();) {
                    entry = (java.util.Map.Entry) it.next();
                    value_Renamed = (Connection) entry.getValue();

                    if (enclosingInstance.getCacheLog().getIsInfoEnabled()) {
                        enclosingInstance.getCacheLog().Info("connection is " + ((curr_time - value_Renamed.last_access) / 1000) + " seconds old (curr-time=" + curr_time
                                + ", last_access=" + value_Renamed.last_access + ')');
                    }
                    if (value_Renamed.last_access + getEnclosing_Instance().conn_expire_time < curr_time) {
                        if (enclosingInstance.getCacheLog().getIsInfoEnabled()) {
                            enclosingInstance.getCacheLog().Info("connection " + value_Renamed + " has been idle for too long (conn_expire_time="
                                    + getEnclosing_Instance().conn_expire_time + "), will be removed");
                        }

                        value_Renamed.Destroy();
                        temp.add(it.next());
                    }
                }

                // Now  remove closed connection from the connection hashtable
                for (int i = 0; i < temp.size(); i++) {
                    if (getEnclosing_Instance().conns_NIC_1.containsKey((Address) temp.get(i))) {
                        getEnclosing_Instance().conns_NIC_1.remove((Address) temp.get(i));
                        temp.set(i, null);
                    }
                }

                if (enclosingInstance.getCacheLog().getIsInfoEnabled()) {
                    enclosingInstance.getCacheLog().Info("ConnectionTable.Reaper", "after lock conns.SyncRoot");
                }
            }

            if (enclosingInstance.getCacheLog().getIsInfoEnabled()) {
                enclosingInstance.getCacheLog().Info("reaper terminated");
            }
            t = null;
        }
    }
}
