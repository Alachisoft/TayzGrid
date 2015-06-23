/*
 * Copyright (c) 2015, Alachisoft. All Rights Reserved.
 *
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
package com.alachisoft.tayzgrid.socketserver;

import com.alachisoft.tayzgrid.common.LoggingInfo;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.event.NEventStart;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.common.threading.ThreadPool;
import com.alachisoft.tayzgrid.common.util.ReaderWriterLock;
import com.alachisoft.tayzgrid.socketserver.callbacktasks.ICallbackTask;

import com.alachisoft.tayzgrid.socketserver.eventtask.IEventTask;
import com.alachisoft.tayzgrid.socketserver.util.HelperFxn;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.org.apache.bcel.internal.generic.BREAKPOINT;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is responsible for maitaining all clients connection with server.
 */
public final class ConnectionManager implements Runnable {

    /**
     * Number of maximam pending connections
     */
    private int _maxClient = 100;
    /**
     * Number of bytes that will hold the incomming command size
     */
    public static final int cmdSizeHolderBytesCount = 10;
    /**
     * Number of bytes that will hold the incomming data size
     */
    public static final int valSizeHolderBytesCount = 10;
    /**
     * Total number of bytes that will hold both command and data size
     */
    public static final int totSizeHolderBytesCount = cmdSizeHolderBytesCount + valSizeHolderBytesCount;
    /**
     * Total buffer size used as pinned buffer for asynchronous socket io.
     */
    public static final int pinnedBufferSize = 200 * 1024;
    /**
     * Total number of milliseconds after which we check for idle clients and
     * send heart beat to them.
     */
    public static final int waitIntervalHeartBeat = 30000;
    /**
     * Underlying server socket object
     */
    //private ServerSocket _serverSocket = null;
    /**
     * Send buffer size of connected client socket
     */
    private int _clientSendBufferSize = 0;
    /**
     * Receive buffer size of connected client socket
     */
    private int _clientReceiveBufferSize = 0;
    /**
     * Command manager object to process incomming commands
     */
    private ICommandManager cmdManager;
    /**
     * Stores the client connections
     */
    public static java.util.HashMap ConnectionTable = new java.util.HashMap(10);
    /**
     * Thread to send callback and notification responces to client
     */
    private Thread _callbacksThread = null;
    /**
     * Reade writer lock instance used to synchronize access to socket
     */
    private static ReaderWriterLock _readerWriterLock = new ReaderWriterLock();
    /**
     * Hold server ip address
     */
    private static String _serverIpAddress = "";
    /**
     * Holds server port
     */
    private static int _serverPort = -1;
    private ILogger _logger;
    private static LoggingInfo _clientLogginInfo = new LoggingInfo();
    private static java.util.LinkedList _callbackQueue = new java.util.LinkedList();
    private Selector socketSelector;

    public static java.util.LinkedList getCallbackQueue() {
        return _callbackQueue;
    }
    private static Object _client_hearbeat_mutex = new Object();

    public static Object getclient_hearbeat_mutex() {
        return _client_hearbeat_mutex;
    }
  
    private ServerSocketChannel _serverChannel;
    private Thread callbackThread;
    private Thread _mainThread;

    /**
     * Start the socket server and start listening for clients
     *
     * @param port port at which the server will be listening
     */
    public void Start(InetAddress bindIP, int port, int sendBuffer, int receiveBuffer, ILogger logger, CommandManagerType cmdMgrType) throws Exception {
        _logger = logger;
        _clientSendBufferSize = sendBuffer;
        _clientReceiveBufferSize = receiveBuffer;
        cmdManager = GetCommandManager(cmdMgrType);

        String maxPendingCon = ServicePropValues.Cache_MaxPendingConnections;
        if (maxPendingCon != null && !maxPendingCon.equals("")) {
            try {
                _maxClient = Integer.parseInt(maxPendingCon);
            } catch (Exception e) {
                throw new Exception("Invalid value specified for NCache.MaxPendingConnections.");
            }
        }

        String enablePerfCounters = ServicePropValues.Cache_EnableServerCounters;
        if (enablePerfCounters != null && !enablePerfCounters.equals("")) {
            try {
                SocketServer.setIsServerCounterEnabled(Boolean.parseBoolean(enablePerfCounters));
            } catch (Exception e) {
                throw new Exception("Invalid value specified for NCache.EnableServerCounters.");
            }
        }

        if (bindIP == null) {
            try {
                bindIP = InetAddress.getLocalHost();
            } catch (Exception e) {
            }
        }

        try {

            if (bindIP != null) {

                socketSelector = SelectorProvider.provider().openSelector();

                // Create a new non-blocking server soc6ket channel
                this._serverChannel = ServerSocketChannel.open();
                _serverChannel.configureBlocking(false);

                InetSocketAddress isa = new InetSocketAddress(bindIP.getHostAddress(), port);
                _serverChannel.socket().setReuseAddress(true);
                _serverChannel.socket().bind(isa);
                _serverChannel.socket().setReceiveBufferSize(1024 * 50);

                // Register the server socket channel, indicating an interest in
                // accepting new connections
                _serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

            }
        } catch (IOException se) {
            throw new Exception("The address " + bindIP + " specified for Server.BindToClientServerIP is not valid");

        }

        _callbacksThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    CallbackThread();
                } catch (Exception exception) {
                    if(_logger!=null && _logger.getIsErrorEnabled())
                    {
                     _logger.Error(exception.getMessage());
                    }
                }
            }
        });
        _callbacksThread.start();

        _serverIpAddress = _serverChannel.socket().getInetAddress().getHostAddress();
        _serverPort = _serverChannel.socket().getLocalPort();

        _mainThread = new Thread(this);
        _mainThread.setPriority(Thread.NORM_PRIORITY);
        _mainThread.start();
      

    }

    public void Start(InetSocketAddress bindIP, int sendBuffer, int receiveBuffer) throws Exception {
        _clientSendBufferSize = sendBuffer;
        _clientReceiveBufferSize = receiveBuffer;

        String maxPendingCon = ServicePropValues.Cache_MaxPendingConnections;
        if (maxPendingCon != null && !maxPendingCon.equals("")) {
            try {
                _maxClient = Integer.parseInt(maxPendingCon);
            } catch (Exception e) {
                throw new Exception("Invalid value specified for NCache.MaxPendingConnections.");
            }
        }

        String enablePerfCounters = ServicePropValues.Cache_EnableServerCounters;
        if (enablePerfCounters != null && !enablePerfCounters.equals("")) {
            try {
                SocketServer.setIsServerCounterEnabled(Boolean.parseBoolean(enablePerfCounters));
            } catch (Exception e) {
                throw new Exception("Invalid value specified for NCache.EnableServerCounters.");
            }
        }

        try {
            socketSelector = SelectorProvider.provider().openSelector();

            this._serverChannel = ServerSocketChannel.open();
            _serverChannel.configureBlocking(false);

            // Bind the server socket to the specified address and port
            _serverChannel.socket().setReuseAddress(true);
            _serverChannel.socket().bind(bindIP);
            _serverChannel.socket().setReceiveBufferSize(1024 * 50);

            // Register the server socket channel, indicating an interest in
            // accepting new connections
            _serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
        } catch (java.io.IOException se) {
            throw new Exception("The address " + bindIP + " specified for Server.BindToClientServerIP is not valid");

        }

        _callbacksThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    CallbackThread();
                } catch (Exception exception) {
                    if(_logger!=null && _logger.getIsErrorEnabled())
                    {
                     _logger.Error(exception.getMessage());
                    }
                }
            }
        });
        _callbacksThread.start();

        _serverIpAddress = _serverChannel.socket().getInetAddress().getHostAddress();
        _serverPort = _serverChannel.socket().getLocalPort();

        _mainThread = new Thread(this);
        _mainThread.setPriority(Thread.NORM_PRIORITY);
        _mainThread.start();
       
    }

    /**
     * Dipose socket server and all the allocated resources
     */
    public void Stop() throws IOException {
        DisposeServer();
    }

    /**
     * Dispose the client
     *
     * @param clientManager Client manager object representing the client to be
     * diposed
     */
    public static void DisposeClient(ClientManager clientManager) {
        try {
            if (clientManager != null) {
                if (clientManager._leftGracefully) {
                    if (SocketServer.getLogger().getIsErrorLogsEnabled()) {
                        SocketServer.getLogger().getCacheLog().Error("ConnectionManager.ReceiveCallback", clientManager.toString() + " left gracefully");
                    }
                } else {
                    if (SocketServer.getLogger().getIsErrorLogsEnabled()) {
                        SocketServer.getLogger().getCacheLog().Error("ConnectionManager.ReceiveCallback", "Connection lost with client (" + clientManager.toString() + ")");
                    }
                }

                if (clientManager.getClientID() != null) {
                    synchronized (ConnectionTable) {
                        ConnectionTable.remove(clientManager.getClientID());
                    }
                }

                clientManager.dispose();
                clientManager = null;
            }
        } catch (Exception e) {
        }
    }

    //Java NIO Section
    private void runNIO() {

        while (true) {
            try {

                int countSelect = socketSelector.select();

                Iterator ite = socketSelector.selectedKeys().iterator();
                while (ite.hasNext()) {
                    SelectionKey key = (SelectionKey) ite.next();
                    try {
                        boolean useSingleThread = true;

                        if (key.isValid() && key.isAcceptable()) {
                            // For an accept to be pending the channel must be a server socket channel.
                            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                            acceptClient(serverSocketChannel);

                        } else if (key.isValid() && key.isReadable()) {
                            final ClientManager client = (ClientManager) key.attachment();
                            processRequest(client);

                        }//end read
                    } finally {
                        ite.remove();
                    }

                }//while end

            } catch (IOException e) {
            } catch (Exception e) {
            }
        }
    }

    private void acceptClient(ServerSocketChannel serverSocketChannel) throws ClosedChannelException, IOException {
        // Accept the connection and make it non-blocking
        SocketChannel socketChannel = serverSocketChannel.accept();
        if (socketChannel != null) {

            Socket socket = socketChannel.socket();

            socket.setReceiveBufferSize(this._clientReceiveBufferSize);
            socket.setSendBufferSize(this._clientSendBufferSize);
            socket.setTcpNoDelay(true);
            socketChannel.configureBlocking(false);
            ClientManager client = new ClientManager(socketChannel, totSizeHolderBytesCount, pinnedBufferSize);

            client.addClientDisposedListner(new NEventStart() {
                @Override
                public Object hanleEvent(Object... obj) throws SocketException, Exception {
                    OnClientDisposed((String) obj[1]);
                    return null;
                }
            }, null);

            if (SocketServer.getLogger().getIsDetailedLogsEnabled()) {
                SocketServer.getLogger().getCacheLog().Info("ConnectionManager.AcceptCallback", "accepted client : " + socket.getInetAddress().toString());
            }

            socketChannel.register(socketSelector, SelectionKey.OP_READ, client);

        }

    }

    private void processRequest(final ClientManager client) throws Exception {

        ByteBuffer lengthBuffer = null;
        String remoteAddresss = client.getSocketChannel().socket().getRemoteSocketAddress().toString();
        try {
            lengthBuffer = client.getLengthBuffer();
            if (client.getDataBuffer() == null) {
                if (!lengthBuffer.hasRemaining()) {
                    lengthBuffer.rewind();
                }

                if (!FillBuffer(client, lengthBuffer)) {
                    return;
                }
            }
        } catch (IOException ex) {
            // The remote forcibly closed the connection, cancel
            // the selection key and close the channel.
            if (SocketServer.getLogger().getIsErrorLogsEnabled()) {
                SocketServer.getLogger().getCacheLog().Error("ConnectionManager.ReceiveCallback", client.toString() + " Error " + ex.toString());
            }

            DisposeClient(client);
            return;
        }
        client.MarkActivity();
        ByteBuffer messageBuffer = client.getDataBuffer();
        try {

            if (messageBuffer == null) {
                //get message length
                byte[] buffer = new byte[10];
                System.arraycopy(client.getLengthBuffer().array(), 20, buffer, 0, cmdSizeHolderBytesCount);
                int messageLength = HelperFxn.ToInt32(buffer, 0, cmdSizeHolderBytesCount, "Command");//ToInt32(lengthBuffer.array(), 0, lengthBuffer.capacity(), null);

                //read the actual message
                messageBuffer = ByteBuffer.allocate(messageLength);
                client.setDataBuffer(messageBuffer);
            }

            if (!FillBuffer(client, messageBuffer)) {
                return;
            }

            //data is read completely
            client.setDataBuffer(null);
            final byte[] commandBuffer = messageBuffer.array();
            //Executes command on thread pool
            ThreadPool.executeTask(new Runnable() {
                @Override
                public void run() {
                    Object command = cmdManager.Deserialize(commandBuffer, _maxClient);
                    try {
                        if (ServerMonitor.getMonitorActivity()) {
                            ServerMonitor.LogClientActivity("ConMgr.RecvClbk", "cmd_size :" + commandBuffer.length);
                        }
                        client.AddToClientsRequest(1);

                        if (SocketServer.getIsServerCounterEnabled()) {
                            SocketServer.getPerfStatsColl().incrementRequestsPerSecStats(1);
                        }
                        client.StartCommandExecution();
                        Date dte = new Date();

                        if (ServerMonitor.getMonitorActivity()) {
                            ServerMonitor.RegisterClient(client.getClientID(), client.getClientSocketId());
                            ServerMonitor.StartClientActivity(client.getClientID());
                            ServerMonitor.LogClientActivity("ConMgr.RecvClbk", "enter");
                        }
                        cmdManager.ProcessCommand(client, command);
                    } catch (Exception ex) {
                        if (ServerMonitor.getMonitorActivity()) {
                            ServerMonitor.LogClientActivity("ConMgr.RecvClbk", "Error :" + ex.toString());
                        }
                        if (SocketServer.getLogger().getIsErrorLogsEnabled()) {
                            SocketServer.getLogger().getCacheLog().Error("ConnectionManager.ReceiveCallback", client.toString() + command + " Error " + ex.toString());
                        }
                        DisposeClient(client);
                    }
                }
            });

        } catch (IOException ex) {
            // The remote forcibly closed the connection, cancel
            // the selection key and close the channel.
            if (SocketServer.getLogger().getIsErrorLogsEnabled()) {
                SocketServer.getLogger().getCacheLog().Error("ConnectionManager.ReceiveCallback", client.toString() + " Error " + ex.toString());
            }
            DisposeClient(client);
            return;
        } catch (Exception ex) {
            DisposeClient(client);
        }

    }

    private boolean FillBuffer(ClientManager client, ByteBuffer buffer) throws IOException {
        SocketChannel channel = client.getSocketChannel();
        int runcount = 0;
        while (buffer.remaining() > 0) {
            if (!channel.isConnected()) {
                throw new IOException("ConnectionManager - Fill Buffer: Channel is not connected");
            }

            int byteRead = channel.read(buffer);

            if (runcount == 0 && byteRead < 0) {
                throw new IOException("ConnectionManager - Fill Buffer: Channel is not connected");
            }
            runcount++;

            if (byteRead > 0) {
                client.AddToClientsBytesRecieved(byteRead);
                if (SocketServer.getIsServerCounterEnabled()) {
                    SocketServer.getPerfStatsColl().incrementBytesReceivedPerSecStats(byteRead);
                    SocketServer.getPerfStatsColl().incrementByteReceivePerSecStatsBy(byteRead);
                }
            }

            if (byteRead <= 0) {
                return false;
            }

        }

        return true;
    }

    public void OnClientDisposed(String clientSocketId) {
        if (ConnectionTable != null && clientSocketId != null) {
            synchronized (ConnectionTable) {
                ConnectionTable.remove(clientSocketId);
            }
        }
    }

    public static void AssureSend(ClientManager clientManager, byte[] buffer) {
        AssureSend(clientManager, buffer, null);
    }

    public static void AssureSend(ClientManager clientManager, byte[] buffer, Object[] userPayLoad) {
        if (clientManager != null) {
            synchronized (clientManager.getSendMutex()) {
                try {
                    clientManager.MarkActivity();
                    int count = buffer.length;
                    int bytesCopied = 0;
                    int mainIndex = 0;
                    byte[] sendBuffer = clientManager.sendBuffer;

                    while (bytesCopied < buffer.length) {
                        count = buffer.length - bytesCopied;
                        if (count > sendBuffer.length - mainIndex) {
                            count = sendBuffer.length - mainIndex;
                        }
                        System.arraycopy(buffer, bytesCopied, sendBuffer, mainIndex, count);
                        bytesCopied += count;
                        mainIndex += count;

                        if (mainIndex >= sendBuffer.length) {
                            AssureSend(clientManager, sendBuffer, sendBuffer.length);
                            mainIndex = 0;
                        }
                    }

                    if (userPayLoad != null && userPayLoad.length > 0) {
                        for (int i = 0; i < userPayLoad.length; i++) {
                            Object tempVar = userPayLoad[i];
                            buffer = (byte[]) ((tempVar instanceof byte[]) ? tempVar : null);
                            bytesCopied = 0;

                            while (bytesCopied < buffer.length) {
                                count = buffer.length - bytesCopied;
                                if (count > sendBuffer.length - mainIndex) {
                                    count = sendBuffer.length - mainIndex;
                                }

                                System.arraycopy(buffer, bytesCopied, sendBuffer, mainIndex, count);
                                bytesCopied += count;
                                mainIndex += count;

                                if (mainIndex >= sendBuffer.length) {
                                    AssureSend(clientManager, sendBuffer, sendBuffer.length);
                                    mainIndex = 0;
                                }
                            }

                            if (mainIndex >= sendBuffer.length) {
                                AssureSend(clientManager, sendBuffer, sendBuffer.length);
                                mainIndex = 0;
                            }
                        }
                        if (mainIndex >= 0) {
                            AssureSend(clientManager, sendBuffer, mainIndex);
                            mainIndex = 0;
                        }
                    } else {
                        AssureSend(clientManager, sendBuffer, mainIndex);
                    }
                } catch (Exception se) {
                    DisposeClient(clientManager);
                }
            }
        }
    }

    private static void AssureSend(ClientManager clientManager, byte[] buffer, int count) throws IOException {
        int bytesSent = 0;
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, count);
        synchronized (clientManager.getSendMutex()) {
            SocketChannel channel = clientManager.getSocketChannel();
            while (byteBuffer.hasRemaining()) {
                try {
                    bytesSent = count - bytesSent;

                    if (!channel.isConnected()) {
                        throw new IOException("Connection Manager - AssureSend: Channel not connected");
                    }
                    channel.write(byteBuffer);

                    clientManager.AddToClientsBytesSent(bytesSent);
                    if (SocketServer.getIsServerCounterEnabled()) {
                        SocketServer.getPerfStatsColl().incrementBytesSentPerSecStats(bytesSent);
                        SocketServer.getPerfStatsColl().incrementByteSentPerSecStatsBy(bytesSent);
                    }
                } catch (IOException e) {
                    throw e;
                }
            }
        }
    }

    private void AssureRecieve(ClientManager clientManager, tangible.RefObject<Object> command, tangible.RefObject<byte[]> value, tangible.RefObject<Long> tranSize) throws InvalidProtocolBufferException {
        value.argvalue = null;

        int commandSize = -1;
        try {
            commandSize = HelperFxn.ToInt32(clientManager.Buffer, 0, cmdSizeHolderBytesCount, "Command");
        } catch (Exception ex) {
            Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        tranSize.argvalue = (long) commandSize;

        byte[] buffer = clientManager.GetPinnedBuffer(tranSize.argvalue);

        tangible.RefObject<byte[]> tempRef_buffer = new tangible.RefObject<byte[]>(buffer);
        AssureRecieve(clientManager, tempRef_buffer, tranSize.argvalue);
        buffer = tempRef_buffer.argvalue;

        command.argvalue = cmdManager.Deserialize(buffer, commandSize);
    }

    /**
     * Receives data equal to the buffer length.
     *
     * @param clientManager
     * @param buffer
     */
    private static void AssureRecieve(ClientManager clientManager, tangible.RefObject<byte[]> buffer) {
        int bytesRecieved = 0;
        int totalBytesReceived = 0;
        do {
            try {
                bytesRecieved = clientManager.getClientSocket().getInputStream().read(buffer.argvalue, totalBytesReceived, buffer.argvalue.length - totalBytesReceived);
                totalBytesReceived += bytesRecieved;

                clientManager.AddToClientsBytesRecieved(bytesRecieved);
                if (SocketServer.getIsServerCounterEnabled()) {
                    SocketServer.getPerfStatsColl().incrementBytesReceivedPerSecStats(bytesRecieved);
                    SocketServer.getPerfStatsColl().incrementByteReceivePerSecStatsBy(bytesRecieved);
                }
            } catch (Exception se) {
            }
        } while (totalBytesReceived < buffer.argvalue.length && bytesRecieved > 0);
    }

    /**
     * The Receives data less than the buffer size. i.e buffer length and size
     * may not be equal. (used to avoid pinning of unnecessary byte buffers.)
     *
     * @param clientManager
     * @param buffer
     * @param size
     */
    private static void AssureRecieve(ClientManager clientManager, tangible.RefObject<byte[]> buffer, long size) {
        int bytesRecieved = 0;
        int totalBytesReceived = 0;

        do {
            try {
                bytesRecieved = clientManager.getClientSocket().getInputStream().read(buffer.argvalue, totalBytesReceived, (int) (size - totalBytesReceived));
                totalBytesReceived += bytesRecieved;

                clientManager.AddToClientsBytesRecieved(bytesRecieved);

                if (SocketServer.getIsServerCounterEnabled()) {
                    SocketServer.getPerfStatsColl().incrementBytesReceivedPerSecStats(bytesRecieved);
                    SocketServer.getPerfStatsColl().incrementByteReceivePerSecStatsBy(bytesRecieved);
                }
            } catch (Exception se) {
            }
        } while (totalBytesReceived < size && bytesRecieved > 0);
        if (buffer.argvalue.length != size) {
            byte[] bite = new byte[totalBytesReceived];
            System.arraycopy(buffer.argvalue, 0, bite, 0, totalBytesReceived);
            buffer.argvalue = bite;
        }

    }

    private void CallbackThread() {
        Object returnVal = null;

        try {
            while (true && !Thread.currentThread().isInterrupted()) {
                try{ 
                while (getCallbackQueue().size() > 0) {
                    synchronized (getCallbackQueue()) {
                        returnVal = getCallbackQueue().poll();
                        if (SocketServer.getIsServerCounterEnabled()) {
                            SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
                        }

                    }
                   
                    if (returnVal instanceof ICallbackTask) {
                        ((ICallbackTask) returnVal).Process();
                    } else if (returnVal instanceof IEventTask) {
                        ((IEventTask) returnVal).Process();
                    }
                  }
                 
                
                try {
                    synchronized (getCallbackQueue()) {
                        Monitor.wait(getCallbackQueue());
                    }
                } catch (InterruptedException exception) {
                    break;
                }
                }catch(Exception ex){
                  }
            }
            
        } catch (Exception e3) {
        }
    }

    public void StopListening() throws IOException {
        if (_serverChannel != null) {
            _serverChannel.close();
            _serverChannel = null;
        }
    }

    private void DisposeServer() throws IOException {
        StopListening();
        if (_callbacksThread != null) {
            if (_callbacksThread.isAlive()) {
                _callbacksThread.interrupt();
                
            }
        }

        if (ConnectionTable != null) {
            synchronized (ConnectionTable) {
                Object tempVar = ConnectionTable.clone();
                java.util.HashMap cloneTable = (java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null);
                Iterator tableEnu = cloneTable.entrySet().iterator();
                Map.Entry pair;
                while (tableEnu.hasNext()) {
                    pair = (Map.Entry) tableEnu.next();
                    ((ClientManager) pair.getValue()).dispose();
                }
            }
            ConnectionTable = null;
        }
        
        if (this._mainThread != null) {
            _mainThread.stop();
        }
        ThreadPool.Stop(true);

    }

    /**
     * Set client logging info
     *
     * @param type
     * @param status
     */
    public static boolean SetClientLoggingInfo(LoggingInfo.LoggingType type, LoggingInfo.LogsStatus status) {
        synchronized (_clientLogginInfo) {
            if (_clientLogginInfo.GetStatus(type) != status) {
                _clientLogginInfo.SetStatus(type, status);
                return true;
            }
            return false;
        }
    }

    /**
     * Get client logging information
     */
    public static LoggingInfo.LogsStatus GetClientLoggingInfo(LoggingInfo.LoggingType type) {
        synchronized (_clientLogginInfo) {
            return _clientLogginInfo.GetStatus(type);
        }
    }

    public static void UpdateClients() throws Exception {
        boolean errorOnly = false;
        boolean detailed = false;

        synchronized (_clientLogginInfo) {
            errorOnly = GetClientLoggingInfo(LoggingInfo.LoggingType.Error) == LoggingInfo.LogsStatus.Enable;
            detailed = GetClientLoggingInfo(LoggingInfo.LoggingType.Detailed) == LoggingInfo.LogsStatus.Enable;
        }

        UpdateClients(errorOnly, detailed);

    }

    /**
     * Update logging info on all connected clients
     */
    private static void UpdateClients(boolean errorOnly, boolean detailed) throws Exception {
        java.util.Collection clients = null;
        synchronized (ConnectionTable) {
            clients = ConnectionTable.values();
        }

        try {
            for (Object obj : clients) {
                ClientManager client = (ClientManager) ((obj instanceof ClientManager) ? obj : null);
                if (client != null) {
                    ICommandExecuter tempVar = client.getCmdExecuter();
                    TayzGrid executor = (TayzGrid) ((tempVar instanceof TayzGrid) ? tempVar : null);
                    if (executor != null) {
                        executor.OnLoggingInfoModified(errorOnly, detailed, client.getClientID());
                    }
                }
            }
        } catch (Exception exc) {
            throw exc;
        }
    }

    /**
     * Get the ip address of server
     */
    public static String getServerIpAddress() {
        return _serverIpAddress;
    }

    /**
     * Get the port at which server is running
     */
    public static int getServerPort() {
        return _serverPort;
    }

    @Override
    public void run() {
        runNIO();
    }

    private ICommandManager GetCommandManager(CommandManagerType cmdMgrType) {
        ICommandManager cmdMgr;
        switch (cmdMgrType) {
            case TayzGridClient:
                cmdMgr = new CommandManager();
                break;
            case TayzGridManagement:
                cmdMgr = new ManagementCommandManager();
                break;
            default:
                cmdMgr = new CommandManager();
                break;
        }
        return cmdMgr;

    }


      
    

}
