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

package com.alachisoft.tayzgrid.communication;


import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.net.NetworkData;
import com.alachisoft.tayzgrid.command.CommandOptions;
import com.alachisoft.tayzgrid.command.CommandResponse;
import com.alachisoft.tayzgrid.event.CommandEvent;
import com.alachisoft.tayzgrid.event.ResponseReceived;
import com.alachisoft.tayzgrid.common.threading.ThreadPool;
import com.alachisoft.tayzgrid.runtime.exceptions.ConnectionException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.util.ConfigReader;
import com.alachisoft.tayzgrid.util.Logs;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ConnectionManager implements ResponseReceived
{

    private static String _bindIP = null;
    private static InetAddress _bindingIP = null;
    public int PORT_NUMBER = 9800;
    private int socketBufferSize = 16384;
    private CommandEvent ce = null;
    private Latch _connectionStatusLatch = new Latch(ConnectionStatus.DISCONNECTED);
    private boolean keepRunning = true;
    private int port;
    private ReceiveThread receiveThread;
    private String ip;
    private Socket socket;
    public boolean notifRegistered;
    private InputStream in;
    private OutputStream out;
    private String _intendedRecipientIPAddress = "";
    private Address _serverAddress;
    private static final int editionId = EditionId.ENT.value(); // ENTERPRISE   EDITION
    public void SetBindIP(String value)
    {
      try
        {

            _bindIP=value;
            if (!_bindIP.trim().isEmpty())
            {
                String[] str = _bindIP.split("\\.");
                byte[] bite = new byte[4];
                bite[0] = new Integer(str[0]).byteValue();
                bite[1] = new Integer(str[1]).byteValue();
                bite[2] = new Integer(str[2]).byteValue();
                bite[3] = new Integer(str[3]).byteValue();
                _bindingIP = InetAddress.getByAddress(bite);
            }
        }
        catch (Exception exception)
        {
            int x = 0;
        }
    }
    private boolean _forcedDisconnect = false;
    private final Logs _logger;
    private boolean _isReconnecting;

    public String getIp()
    {
        return ip;
    }

    public String getIntendedRecipientIPAddress()
    {
        return _intendedRecipientIPAddress;
    }

    public void setIntendedRecipientIPAddress(String _intendedRecipientIPAddress)
    {
        this._intendedRecipientIPAddress = _intendedRecipientIPAddress;
    }

    //~--- constructors -------------------------------------------------------
    /**
     *
     * @param event
     * @throws java.io.IOException
     */
    public ConnectionManager(CommandEvent event, Logs logger, String bindIP  )
    {
        ce = event;
        _logger = logger;
         SetBindIP(bindIP);
    }

    //~--- methods ------------------------------------------------------------
    //<editor-fold defaultstate="collapsed" desc="~~~~ Assure Send/Receive ~~~~">
    public CommandResponse AssureRecieve() throws ConnectionException
    {
        byte[] buffer = new byte[CommandOptions.COMMAND_SIZE];
        AssureRecieve(buffer);

        ce.Write("AssureRecived: ");

        String s = new String(buffer, 0, CommandOptions.COMMAND_SIZE);

        
        CommandResponse resultItem = null;

        int commandSize = 0;
        try
        {
            commandSize = Integer.parseInt(s.trim());
        }
        catch (Exception ex)
        {
            
        }

        if (commandSize == 0)
        {
            ce.Write("AssureRecieved:\tCommand size is 0");
            throw new ConnectionException("Disconnected: can not receive.");
        }


        buffer = new byte[commandSize];
        AssureRecieve(buffer);
        resultItem = new CommandResponse(false);
        resultItem.setRawResult(buffer);


        return resultItem;
    }

    private void AssureRecieve(byte[] buffer) throws ConnectionException
    {
        int bytesRecieved = 0;

        try
        {
            synchronized (getInputStream())
            {
                do
                {
                    bytesRecieved += getInputStream().read(buffer, bytesRecieved, (buffer.length - bytesRecieved));
                    int x = 0;
                }
                while (bytesRecieved < buffer.length);
            }
        }
        catch (Exception e)
        {
            _connectionStatusLatch.setStatusBit(ConnectionStatus.DISCONNECTED, ConnectionStatus.CONNECTED);
            throw new ConnectionException("Disconnected: can not receive.");
        }
    }

    private synchronized void AssureSend(byte[] buffer, boolean checkConnected) throws ConnectionException
    {

        if (checkConnected && _connectionStatusLatch.isAnyBitsSet((byte) (ConnectionStatus.DISCONNECTED | ConnectionStatus.BUSY)))
        {
            throw new ConnectionException();
        }
        synchronized (getOutputStream())
        {
            try
            {
                getOutputStream().write(buffer);
                getOutputStream().flush();
            }
            catch (Exception ex)
            {
                _connectionStatusLatch.setStatusBit(ConnectionStatus.DISCONNECTED, ConnectionStatus.CONNECTED);
                throw new ConnectionException(ex.getMessage());
            }
        }
    }
    //</editor-fold>

    public boolean isThisMyIpAddress(InetAddress addr)
    {
        // Check if the address is a valid special local or loop back
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress())
        {
            return true;
        }

        // Check if the address is defined on any interface
        try
        {
            return NetworkInterface.getByInetAddress(addr) != null;
        }
        catch (Exception e)
        {
            return false;
        }
    }
    Socket soc = null;

    public synchronized boolean connect(String host, int port)
            throws ConnectionException, UnknownHostException
    {
        this.ip = host;
        this.port = port;
        this._serverAddress=new Address(host,port);
        try
        {
            _forcedDisconnect = false;

            try
            {
                if (soc != null)
                {
                    soc.close();
                }
            }
            catch (IOException iOException)
            {
            }

            if (_bindingIP != null)
            {
                soc = new Socket(host, port, _bindingIP, 0);
            }
            else
            {
                soc = new Socket(host, port);
            }

            if (soc != null)
            {
                soc.setTcpNoDelay(false);
                soc.setSendBufferSize(socketBufferSize);
                soc.setReceiveBufferSize(socketBufferSize);
                in = soc.getInputStream();
                out = soc.getOutputStream();
            }
        }
        catch (Exception e)
        {
            if (_logger!=null && _logger.getIsErrorLogsEnabled())
            {
                _logger.getCacheLog().Error("Connection.Connect", " can not connect to " + host + ":" + port + ". error: " + e.toString());
            }
            return false;
        }

        if (soc != null)
        {
            this.setSocket(soc);
            return true;
        }
        else
        {
            return false;
        }
    }

    public void connectionLost(Address serverAddress)
    {
        ce.serverLost(serverAddress, _forcedDisconnect);
    }

    public void disconnect()
    {

        try
        {
            _forcedDisconnect = true;
            keepRunning = false;
            try
            {
                if (this.in != null)
                {
                    this.in.close();
                }
                if (this.out != null)
                {
                    this.out.close();
                }
                if (this.socket != null && this.socket.isConnected())
                {
                    this.socket.close();
                }
            }
            catch (IOException ex)
            {
                ce.Write("disconnect:\t" + ex.getMessage());
            }

            this.socket = null;
            this.in = null;
            this.out = null;

            _connectionStatusLatch.setStatusBit(ConnectionStatus.DISCONNECTED, ConnectionStatus.CONNECTED);

            if (receiveThread != null && !receiveThread.isInterrupted() && receiveThread.isAlive())
            {
                try
                {
                    receiveThread.interrupt();
                    ce.Write("disconnect:\t thread destroyed");
                }
                catch (Exception ex)
                {
                }
            }
        }
        catch (Exception e)
        {
        }
    }

    /**
     *
     * @param result
     * @throws java.io.IOException
     */
    public void receiveEvent(CommandResponse result, Address serverAddress) throws IOException
    {
        final CommandResponse res = result;
        final Address server = serverAddress;
        try {
                ThreadPool.executeTask(new Runnable()
                {
                    public void run()
                    {
                        ce.commandReceived(res, server);
                    }
                });
            } 
            catch (Exception ex) 
            {
                if(_logger!=null && _logger.getIsErrorLogsEnabled())
                {
                    _logger.getCacheLog().Error("error on receive event.", ex.toString());
                } 
            }
    }

    /**
     *
     * @param com
     * @throws java.io.IOException
     */
    public boolean sendCommand(byte[] commandBytes, boolean checkConnected) throws ConnectionException
    {
        AssureSend(commandBytes, checkConnected);
        return true;
    }

    public void startReceiver()
    {
        receiveThread = new ReceiveThread(this, this.ip, this.port);
        receiveThread.setPriority(7);
        receiveThread.start();
    }

    //~--- get methods --------------------------------------------------------
    public int getPort()
    {
        return port;
    }

    private InputStream getInputStream()
    {
        return in;
    }

    public String getServer()
    {
        return ip;
    }
    public Address getServerAddress()
    {
        return this._serverAddress; 
    }  
    
    private Socket getSocket()
    {
        return socket;
    }

    public String getCurrentIP()
    {
        return this.socket != null? this.socket.getLocalAddress().getHostAddress(): null;
    }

    private OutputStream getOutputStream()
    {
        return out;
    }

    public Latch getStatusLatch()
    {
        return _connectionStatusLatch;
    }

    public static int getEditionId()
    {
        return editionId;
    }

    public static String getVersion()
    {
        return "";
    }

    public boolean isReconnecting()
    {
        return _isReconnecting;
    }

    public void setReconnecting(boolean value)
    {
        _isReconnecting = value;
    }

    public boolean isConnected()
    {
        return _connectionStatusLatch.isAnyBitsSet(ConnectionStatus.CONNECTED);
    }
    //~--- set methods --------------------------------------------------------

    private void setSocket(Socket aServer)
    {
        socket = aServer;
        NetworkData.registerIPToMonitor(getCurrentIP());
    }

    //~--- inner classes ------------------------------------------------------
    /**
     * inner class to generate async receive events.
     */
    private class ReceiveThread extends Thread
    {

        private ResponseReceived re;
        private String ip;
        private int port;

        //~--- constructors ---------------------------------------------------
        /**
         *
         * @param event
         */
        public ReceiveThread(ResponseReceived event, String ip, int port)
        {
            this.setName("Receive Thread");
            re = event;

            this.ip = ip;
            this.port = port;
        }

        //~--- methods --------------------------------------------------------
        @Override
        public void run()
        {
            int n = 0;

            while (true)
            {
                try
                {
                    CommandResponse result = null;

                    result = AssureRecieve();
                   
                    re.receiveEvent(result, new Address(ip,this.port));
                }
                catch (ConnectionException ex)
                {
                    if (!_forcedDisconnect)
                    {
                        _connectionStatusLatch.setStatusBit(ConnectionStatus.DISCONNECTED, ConnectionStatus.CONNECTED);
                    }
                    ce.Write("RecieveThread.run:\tConnection excpetion occured. calling connection lost");
                    try {
                        re.connectionLost(new Address(this.ip, this.port));
                    } catch (UnknownHostException ex1) {
                       if(_logger!=null && _logger.getIsErrorLogsEnabled())
                       {
                            _logger.getCacheLog().Error("error on receive parsing address", ex1.toString());
                       }
                    }
                    break;
                }
                catch (Exception e)
                {

                   if(_logger!=null && _logger.getIsErrorLogsEnabled())
                    {
                        _logger.getCacheLog().Error("error on receive.", e.toString());
                    }

                    break;
                }
            }
        }
    }
}
//~ Formatted by Jindent --- http://www.jindent.com

