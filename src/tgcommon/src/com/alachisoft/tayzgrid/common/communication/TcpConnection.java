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

package com.alachisoft.tayzgrid.common.communication;

import com.alachisoft.tayzgrid.common.communication.exceptions.ConnectionException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import javax.net.ssl.SSLServerSocket;

public class TcpConnection implements IConnection
{

    private Socket _socket;
    private boolean _connected;
    private Object _sync_lock = new Object();
    private InetAddress _bindIP;
    private ObjectInputStream in = null;

    public final boolean Connect(String serverIP, int port) throws UnknownHostException,java.io.IOException
    {
        boolean connected = false;

        try
        {

            SocketAddress sockaddr = new InetSocketAddress(serverIP, port);
            Socket socket = new Socket();

            if (_bindIP != null)
            {
                socket.bind(sockaddr);
            }
            socket.connect(sockaddr, 1000);
            connected = socket.isConnected();
            _socket = socket;
        }
        catch (Exception e)
        {
            throw new ConnectionException("[" + serverIP + "] " + e.getMessage());
        }

        _connected = connected;
        return connected;
    }

    public final void Disconnect()
    {
        try
        {
            if (_connected && _socket != null && _socket.isConnected())
            {
                _socket.close();
            }
        }
        catch (Exception e)
        {
        }
    }

    public final boolean Send(byte[] buffer, int offset, int count) throws ConnectionException
    {
        boolean sent = false;

        synchronized (_sync_lock)
        {
            if (_connected)
            {
                int dataSent = 0;

                while (count > 0)
                {
                    try
                    {
                        com.alachisoft.tayzgrid.serialization.standard.io.ObjectOutputStream os = new com.alachisoft.tayzgrid.serialization.standard.io.ObjectOutputStream(_socket.getOutputStream(), "");
                        os.write(buffer, offset, count);
                        os.flush();
                        dataSent = count;
                        offset += dataSent;
                        count = count - dataSent;
                    }
                    catch (java.io.IOException se)
                    {
                        _connected = false;
                        throw new ConnectionException(se.getMessage(), se);
                    }
                }
                sent = true;
            }
            else
            {
                throw new ConnectionException();
            }
        }

        return sent;
    }

    public final boolean Receive(byte[] buffer, int count) throws ConnectionException
    {
        boolean received = false;
        {
            if (_connected)
            {
                int receivedCount = 0;
                int offset = 0;
                while (count > 0)
                {
                    try
                    {
                        receivedCount = _socket.getInputStream().read(buffer, offset, count);
                        offset += receivedCount;
                        count = count - receivedCount;
                    }
                    catch (java.io.IOException se)
                    {
                        _connected = false;
                        throw new ConnectionException(se.getMessage(), se);
                    }
                }
                received = true;
            }
            else
            {
                throw new ConnectionException();
            }
        }
        return received;
    }

    public final boolean getIsConnected()
    {
        if (_connected)
        {
            _connected = _socket.isConnected();
        }

        return _connected;
    }

    public final void Bind(String address) throws UnknownHostException
    {
        if (address == null)
        {
            throw new IllegalArgumentException("address");
        }

        _bindIP = InetAddress.getByName(address);
    }
}
