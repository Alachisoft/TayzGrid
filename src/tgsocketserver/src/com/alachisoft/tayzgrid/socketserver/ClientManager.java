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

import com.alachisoft.tayzgrid.socketserver.util.HelperFxn;
import com.alachisoft.tayzgrid.common.util.BufferPool;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.event.NEventStart;
import com.alachisoft.tayzgrid.common.event.NEventEnd;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;
import com.alachisoft.tayzgrid.common.rpcframework.RPCService;
import com.alachisoft.tayzgrid.management.MonitorServer;
import com.alachisoft.tayzgrid.socketserver.util.*;
import com.alachisoft.tayzgrid.common.net.*;
//import Alachisoft.NCache.Management.*;
import com.alachisoft.tayzgrid.common.util.*;
import com.alachisoft.tayzgrid.common.datastructures.*;
import com.alachisoft.tayzgrid.common.event.NEvent;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;
import tangible.RefObject;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Date;

/**
 * One instance of this class is created per client connection, and it remains valid as long as the client is connected.
 */
public final class ClientManager
{

    /**
     * True if socket has read the size of incomming command and data, false otherwise
     */
    public boolean IsSizeRead = true;
    public int bytesRead = 0;
    /**
     * True if client connection is closed, flase otherwise
     */
    public boolean ConnectionLost = false;
    /**
     * True if this client is a .Net client, flase otherwise
     */
    public boolean IsDotNetClient = false;
    /**
     * Holds the data packet recieved from cleint
     */
    public byte[] Buffer = null;
    public byte[] PinnedBuffer = null;
    public byte[] tempDataBuffer = null;
    public byte[] discardingBuffer = new byte[20];
    public byte[] sendBuffer = null; //new byte[1024 * 1024];
    /**
     * Underlying client socket object
     */
    /**
     * Holds the client socket's id
     */
    //private string _clientSocketId = null;
    /**
     * Unique clientId holder
     */
    private String _clientSocketId = null;
    /**
     * A unique id for the each client connected to the socket server.
     */
    private String _clientID = "NULL";
    private NEvent _clientDisposed;
    private ICommandExecuter _cmdExecuter;
    private boolean _cacheStopped;
    private Object _disposeSync = new Object();
    private Object _send_mutex = new Object();
    private String _toString;
    private float _clientsRequest = 0;
    private float _clientsBytesSent = 0;
    private float _clientsBytesRecieved = 0;
    public boolean _leftGracefully;
    private java.util.Date _cmdStartTime = new java.util.Date(0);
    private TimeSpan _cmdExecurionTime = new TimeSpan();
    private boolean _disposed;
    private java.util.Date _lastActivityTime = new java.util.Date(0);
    private int maxIdleTimeAllowed = 1; // time in minutes till which client can remain idle
    private String _uniqueCacheID = "";
    private boolean _isBridgeSourceClient = false;
    private ByteBuffer lengthBuffer = ByteBuffer.allocate(30);
    private ByteBuffer dataBuffer;
    private SocketChannel _socketChannel;

    private java.util.HashMap<String, EnumerationPointer> _enumerationPointers = new java.util.HashMap<String, EnumerationPointer>();

    public java.util.HashMap<String, EnumerationPointer> getEnumerationPointers()
    {
        return _enumerationPointers;
    }

    public void setEnumerationPointers(java.util.HashMap<String, EnumerationPointer> value)
    {
        _enumerationPointers = value;
    }

    private InetAddress _clientAddress = null;

    /**
     * Construct client connection object and initialized the data buffer
     *
     * @param clientSocketId Underlying client socket connection object
     * @param size Size of the data packet to recieve in buffer.
     */
    public ClientManager(SocketChannel clientChannel, int size, int pinnedBufferSize) throws java.net.UnknownHostException
    {
        InitializeBuffer(size, pinnedBufferSize);

        _socketChannel = clientChannel;
        _clientSocketId = (new Address(clientChannel.socket().getInetAddress().getHostAddress(), clientChannel.socket().getPort())).toString();
        _clientDisposed = new NEvent("Client Disposed", null);
    }

	public InetAddress getClientAddress() {
		return _clientAddress;
	}
    public void setIsBridgeSourceClient(boolean value)
    {
        _isBridgeSourceClient = value;
    }

    public boolean getIsBridgeSourceClient()
    {
        return _isBridgeSourceClient;
    }

    public String getClientSocketId()
    {
        return _clientSocketId;
    }

    /**
     * Gets/sets the ICommandExecuter.
     */
    public ICommandExecuter getCmdExecuter()
    {
        return _cmdExecuter;
    }

    public ByteBuffer getLengthBuffer(){
        return lengthBuffer;
    }

    public ByteBuffer getDataBuffer(){
        return dataBuffer;
    }

    public void setDataBuffer(ByteBuffer buffer){
        dataBuffer = buffer;
    }

    public boolean hasPendingDataRead(){
        return dataBuffer != null;
    }
    public void setCmdExecuter(ICommandExecuter value)
    {
        _cmdExecuter = value;
    }
    private RPCService<MonitorServer> _monitorRPCService;

    public RPCService<MonitorServer> getMonitorRPCService()
    {
        return _monitorRPCService;
    }

    public void setMonitorRPCService(RPCService<MonitorServer> value)
    {
        _monitorRPCService = value;
    }

    public void addClientDisposedListner(NEventStart start, NEventEnd end)
    {
        _clientDisposed.addNEventListners(start, end);
    }

    public void removeClientDisposedListner(NEventStart start, NEventEnd end)
    {
        _clientDisposed.removeNEventListners(start);
    }

    /**
     * Initializes buffer to hold new data packet.
     *
     * @param size Size of the data packet to recieve in buffer.
     */
    public void InitializeBuffer(int size, int pinnedBufferSize)
    {
        PinnedBuffer = BufferPool.CheckoutBuffer(-1);
        sendBuffer = BufferPool.CheckoutBuffer(-1);
        Buffer = new byte[size];
    }

    /**
     * Increment the client requests by specified value
     *
     * @param value
     */
    public void AddToClientsRequest(long value)
    {
       this._clientsRequest = value;
    }

    /**
     * Increment the bytes sent to clients by specified value
     *
     * @param value
     */
    public void AddToClientsBytesSent(long value)
    {
        this._clientsBytesSent = value;
    }

    /**
     * Increment the bytes recieved from clients by specified value
     *
     * @param value
     */
    public void AddToClientsBytesRecieved(long value)
    {
        this._clientsBytesRecieved = value;
    }

    /**
     * Get number of clients requests
     */
    public float getClientsRequests()
    {
        return this._clientsRequest;
    }

    /**
     * Get number of bytes sent to clients
     */
    public float getClientsBytesSent()
    {
        return this._clientsBytesSent;
    }

    /**
     * Get number of bytes sent to clients
     */
    public float getClientsBytesRecieved()
    {
        return this._clientsBytesRecieved;
    }

    public Object getSendMutex()
    {
        return _send_mutex;
    }

    /**
     * Unique source cache id which the
     */
    public String getUniqueCacheID()
    {
        return _uniqueCacheID;
    }

    public void setUniqueCacheID(String value)
    {
        _uniqueCacheID = value;
    }

    /**
     * Gets the value indicating whether client has disocnnected from server or not.
     */
    public boolean getIsDisposed()
    {
        return _disposed;
    }

    /**
     * Gets whether cache is running or stopped
     */
    public boolean getIsCacheStopped()
    {
        return _cacheStopped;
    }

    /**
     * Get underlying client socket connection object
     */
    public Socket getClientSocket()
    {
        return this.getSocketChannel().socket();
    }

      public SocketChannel getSocketChannel()
    {
        return this._socketChannel;
    }
    /**
     * Gets/sets the id of the client. This id must be unique for the client. No two clients can have the same id.
     */
    public String getClientID()
    {
        return _clientID;
    }

    public void setClientID(String value)
    {
        _clientID = value;
    }

    public void ReinitializeBuffer()
    {
        if (Buffer != null)
        {
            for (int i = 0; i < Buffer.length; i++)
            {
                Buffer[i] = 0;
            }
        }
    }

    /**
     * this function is called when cache stops
     *
     * @param cacheId
     */
    public void OnCacheStopped(String cacheId)
    {
        _cacheStopped = true;
        dispose(false);
    }

    /**
     * Construct the value package that can be send to the client
     *
     * @param result result string
     * @param resultData result value
     * @return constructed packet
     */
    public byte[] ReplyPacket(String result, byte[] resultData)
    {
        byte[] command = HelperFxn.ToBytes(result);
        byte[] buffer = new byte[ConnectionManager.cmdSizeHolderBytesCount + ConnectionManager.valSizeHolderBytesCount + command.length + resultData.length];

        byte[] commandSize = HelperFxn.ToBytes(new Integer(command.length).toString());
        byte[] dataSize = HelperFxn.ToBytes(new Integer(resultData.length).toString());

        System.arraycopy(commandSize, 0, buffer, 0, commandSize.length);
        System.arraycopy(dataSize, 0, buffer, ConnectionManager.cmdSizeHolderBytesCount, dataSize.length);
        System.arraycopy(command, 0, buffer, ConnectionManager.totSizeHolderBytesCount, command.length);
        System.arraycopy(resultData, 0, buffer, ConnectionManager.totSizeHolderBytesCount + command.length, resultData.length);

        return buffer;
    }

    /**
     * Construct the value package that can be send to the client
     *
     * @param result result string
     * @param dataLength datalength int
     * @return constructed packet
     */
    public byte[] ReplyPacket(String result, int dataLength)
    {
        byte[] command = HelperFxn.ToBytes(result);
        byte[] buffer = new byte[ConnectionManager.cmdSizeHolderBytesCount + ConnectionManager.valSizeHolderBytesCount + command.length];

        byte[] commandSize = HelperFxn.ToBytes((new Integer(command.length)).toString());
        byte[] dataSize = HelperFxn.ToBytes((new Integer(dataLength)).toString());

        System.arraycopy(commandSize, 0, buffer, 0, commandSize.length);
        System.arraycopy(dataSize, 0, buffer, ConnectionManager.cmdSizeHolderBytesCount, dataSize.length);
        System.arraycopy(command, 0, buffer, ConnectionManager.totSizeHolderBytesCount, command.length);

        return buffer;
    }

    /**
     * Dispose client manager and connection objects
     */
    public void dispose()
    {
        dispose(true);
    }

    /**
     * Dispose client manager and connection objects
     */
    public void dispose(boolean disposingIntentionally)
    {
        synchronized (_disposeSync)
        {
            _disposed = true;
            try
            {
                if (_enumerationPointers != null)
                {
                    for (String key : _enumerationPointers.keySet())
                    {
                        _cmdExecuter.DisposeEnumerator(_enumerationPointers.get(key));
                    }
                    _enumerationPointers = null;
                }
            }
            catch (Exception e)
            {
            }


             if (_socketChannel != null)
            {
                try
                {
                    if (_socketChannel.isConnected())
                    {

                        _socketChannel.close();


                    }
                    _socketChannel = null;
                }
                catch (Exception e)
                {
                }
            }
            Buffer = null;
            BufferPool.CheckinBuffer(PinnedBuffer);
            BufferPool.CheckinBuffer(sendBuffer);
            PinnedBuffer = null;
            sendBuffer = null;
            
            if(SocketServer.getIsServerCounterEnabled())SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
           
            
            if (_cmdExecuter != null)
            {

                if (!_cacheStopped)
                {
                    try
                    {
                        _cmdExecuter.OnClientDisconnected(getClientID(), getUniqueCacheID());
                    }
                    catch (Exception e)
                    {
                        if (SocketServer.getLogger().getIsErrorLogsEnabled())
                        {
                            SocketServer.getLogger().getCacheLog().Error("ClientManager.Dispose", e.toString());
                        }
                    }
                }
                if (_cmdExecuter != null)
                {
                    _cmdExecuter.dispose();
                    _cmdExecuter = null;
                }
            }
            if (!disposingIntentionally)
            {
                _clientDisposed.fireEvents(false, this.getClientID());
            }
        }
    }

    @Override
    public String toString()
    {
        return "[" + _clientSocketId + " ->" + _clientID + "]";
    }

    public void StartCommandExecution()
    {
        _cmdStartTime = new java.util.Date();
        _lastActivityTime = new java.util.Date();
    }

    public void MarkActivity()
    {
        synchronized (this)
        {
            _lastActivityTime = new Date();
        }
    }

    public boolean getIsIdle()
    {

        java.util.Date currentTime = new java.util.Date();
        TimeSpan idleTime = new TimeSpan();
        try
        {
            idleTime = TimeSpan.Subtract(currentTime, _lastActivityTime);
        }
        catch (Exception e)
        {
        }
        if (idleTime.getTotalMinutes() > maxIdleTimeAllowed)
        {
            return true;
        }
        else
        {
            return false;
        }

    }

    public void StopCommandExecution()
    {
        try
        {
            java.util.Date now = new java.util.Date();
            _cmdExecurionTime = TimeSpan.Subtract(now, _cmdStartTime);
        }
        catch (Exception e)
        {
        }
    }

    /**
     * Returns the pinned buffer. if the size of the command is grater then new buffer is created and returned.
     *
     * @param size Size required
     * @return The PinnedBuffer
     */
    public byte[] GetPinnedBuffer(long size)
    {
        if (this.PinnedBuffer.length < size)
        {
            this.PinnedBuffer = new byte[(int) size];
        }
        return this.PinnedBuffer;
    }

    /**
     * Returns the pinned buffer. if the size of the command is grater then new buffer is created and returned.
     *
     * @param size Size required
     * @return The PinnedBuffer
     */
    public byte[] GetTempPinnedBuffer(int size)
    {
        if (this.tempDataBuffer == null || this.tempDataBuffer.length < size)
        {
            this.tempDataBuffer = new byte[size];
        }
        return this.tempDataBuffer;
    }
}
