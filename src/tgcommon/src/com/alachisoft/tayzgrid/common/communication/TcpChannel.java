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

import com.alachisoft.tayzgrid.common.communication.exceptions.ChannelException;
import com.alachisoft.tayzgrid.common.communication.exceptions.ConnectionException;
import com.alachisoft.tayzgrid.common.GenericCopier;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

public class TcpChannel implements IChannel
{

    private static final int DATA_SIZE_BUFFER_LENGTH = 10; //(in bytes)
    private IConnection _connection;
    private String _serverIP;
    private String _bindIP;
    private int _port;
    private byte[] _sizeBuffer = new byte[DATA_SIZE_BUFFER_LENGTH];
    private IChannelFormatter _formatter;
    private IChannelEventListener _eventListener;
    private Thread _receiverThread;
    private ITraceProvider _traceProvider;
    private boolean stopThread;

    public TcpChannel(String serverIP, int port, String bindingIP, ITraceProvider traceProvider)
    {
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(serverIP))
        {
            throw new IllegalArgumentException("serverIP");
        }

        _serverIP = serverIP;
        _port = port;
        _bindIP = bindingIP;
        _traceProvider = traceProvider;
    }
    private String privateName;

    public final String getName()
    {
        return privateName;
    }

    public final void setName(String value)
    {
        privateName = value;
    }

    public final boolean Connect() throws Exception
    {

        if (_formatter == null)
        {
            throw new Exception("Channel formatter is not specified");
        }

        if (_eventListener == null)
        {
            throw new Exception("There is no channel event listener specified");
        }

        try
        {
            if (_connection == null)
            {
                _connection = new TcpConnection();

                if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(_bindIP))
                {
                    _connection.Bind(_bindIP);
                }

                _connection.Connect(_serverIP, _port);

                _receiverThread = new Thread(new Runnable() {

                    @Override
                    public void run()
                    {
                        try
                        {
                            Run();
                        }
                        catch (UnsupportedEncodingException unsupportedEncodingException)
                        {
                            Logger.getLogger(GenericCopier.class.getName()).warning("UnsupportedEncodingException + " + unsupportedEncodingException.getMessage()) ;
                        }
                    }
                });
                _receiverThread.setDaemon(true);
                _receiverThread.start();
                return true;
            }
        }
        catch (ConnectionException ce)
        {
            if (_traceProvider != null)
            {
                _traceProvider.TraceError(getName() + ".Connect", ce.toString());
            }
            throw new ChannelException(ce.getMessage(), ce);
        }

        return false;
    }

    public final void Disconnect()
    {
        if (_connection != null)
        {
            _connection.Disconnect();
            if (_receiverThread != null && _receiverThread.isAlive())
            {
                this.stopThread = true;
            }
            _connection = null;
        }
    }

    @Override
    public final boolean SendMessage(Object message) throws ChannelException,java.io.UnsupportedEncodingException
    {
        if (message == null)
        {
            throw new IllegalArgumentException("message");
        }

        //first serialize the message using channel formatter
        byte[] serailizedMessage = null;
        try
        {
            serailizedMessage = _formatter.Serialize(message);
        }
        catch (Exception exception)
        {
            throw new ChannelException(exception.getMessage());
        }

        byte[] msgLength = (serailizedMessage.length + "").getBytes("UTF-8");

        //message is written in a specific order as expected by Socket server

        byte[] finalBuffer = null;
        try
        {
            finalBuffer = new byte[30 + serailizedMessage.length];
            System.arraycopy(msgLength, 0, finalBuffer, 20, msgLength.length);
            System.arraycopy(serailizedMessage, 0, finalBuffer, 30, serailizedMessage.length);
        }
        catch(Exception e)
        {
            Logger.getLogger(GenericCopier.class.getName()).warning("Exception + " + e.getMessage()) ;
        }


        try
        {
            if (EnsureConnected())
            {
                try
                {
                    _connection.Send(finalBuffer, 0, finalBuffer.length);
                    return true;
                }
                catch (ConnectionException e)
                {
                    if (EnsureConnected())
                    {
                        _connection.Send(finalBuffer, 0, finalBuffer.length);
                        return true;
                    }
                }
            }
        }
        catch (Exception e)
        {
            throw new ChannelException(e.getMessage());
        }

        return false;
    }

    private boolean EnsureConnected() throws Exception
    {
        if (_connection != null && !_connection.getIsConnected())
        {
            Disconnect();
            Connect();
        }

        return _connection.getIsConnected();
    }

    public final void RegisterEventListener(IChannelEventListener listener)
    {
        if (listener == null)
        {
            throw new IllegalArgumentException("listener");
        }

        _eventListener = listener;
    }

    public final void UnRegisterEventListener(IChannelEventListener listener)
    {
        if (listener != null && listener.equals(_eventListener))
        {
            _eventListener = null;
        }
    }

    public final IChannelFormatter getFormatter()
    {
        return _formatter;
    }

    public final void setFormatter(IChannelFormatter value)
    {
        _formatter = value;
    }

    private void Run() throws UnsupportedEncodingException
    {
        while (!stopThread)
        {
            try
            {

                //receive data size for the response
                if (_connection != null)
                {
                    _connection.Receive(_sizeBuffer, DATA_SIZE_BUFFER_LENGTH);
                    String resp = new String(_sizeBuffer);
                    int rspLength =  Integer.parseInt(resp.trim());
                    if (rspLength > 0)
                    {
                        byte[] dataBuffer = new byte[rspLength];
                        _connection.Receive(dataBuffer, rspLength);

                        //deserialize the message
                        IResponse response = null;
                        if (_formatter != null)
                        {
                            Object tempVar = _formatter.Deserialize(dataBuffer);
                            response = (IResponse) ((tempVar instanceof IResponse) ? tempVar : null);
                        }

                        if (_eventListener != null)
                        {
                            _eventListener.ReceiveResponse(response);
                        }
                    }

                }

            }
            catch (ConnectionException ce)
            {
                if (_traceProvider != null)
                {
                    _traceProvider.TraceError(getName() + ".Run", ce.toString());
                }
                if (_eventListener != null)
                {
                    _eventListener.ChannelDisconnected(ce.getMessage());
                }
                break;
            }
            catch (Exception e)
            {
                if (_traceProvider != null)
                {
                    _traceProvider.TraceError(getName() + ".Run", e.toString());
                }
            }
        }
    }
}
