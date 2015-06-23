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

package com.alachisoft.tayzgrid.servicecontrol;

import com.alachisoft.tayzgrid.common.remoting.RemotingChannels;
import com.alachisoft.tayzgrid.common.exceptions.ManagementException;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.net.UnknownHostException;

/**
 * Base class for services.
 */
public abstract class ServiceBase implements IDisposable
{

    /**
     * Server name.
     */
    protected String _serverName;
    /**
     * Use TCP channel for remoting.
     */
    protected boolean _useTcp = true;
    protected String _ip = "";
    /**
     * Remoting port name of IPC channel.
     */
    private String _portName;
    /**
     * Remoting port.
     */
    protected long _port;
    public static final int DEF_TCP_PORT = 8260;
    public static final int DEF_HTTP_PORT = 8261;
    /**
     */
    protected RemotingChannels _channel;

    /**
     * Constructor
     */
    public ServiceBase() throws UnknownHostException
    {
        this._serverName = java.net.InetAddress.getLocalHost().getHostName();
    }

    /**
     * Overloaded Constructor
     *
     * @param server name of machine where the service is running.
     * @param port port used by the remote server.
     * @param useTcp use tcp channel for remoting.
     */
    public ServiceBase(String server, long port, boolean useTcp) throws UnknownHostException
    {
        setServerName(server);
        setPort(port);
        setUseTcp(useTcp);
        if (this._serverName == null || this._serverName.trim().equalsIgnoreCase(""))
        {
            this._serverName = java.net.InetAddress.getLocalHost().getHostName();
        }
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or resetting unmanaged resources.
     *
     * @param disposing
     *
     *
     */
    private void dispose(boolean disposing)
    {
        if (disposing)
        {
            System.gc();
        }
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or resetting unmanaged resources.
     */
    public final void dispose()
    {
        dispose(true);
    }


    public String getIp()
    {
        return _ip;
    }



    /**
     * Server name.
     */
    public final String getServerName()
    {
        return _serverName;
    }

    public final void setServerName(String value)
    {
        _serverName = value;
    }

    /**
     * Use TCP channel for remoting.
     */
    public final boolean getUseTcp()
    {
        return _useTcp;
    }

    public final void setUseTcp(boolean value)
    {
        _useTcp = value;
    }

    /**
     * Remoting port.
     */
    public final long getPort()
    {
        return _port;
    }

    public final void setPort(long value)
    {
        _port = value;
    }

    /**
     * Starts the NCache service on target machine.
     */
    protected final void Start(TimeSpan timeout, String service) throws ManagementException
    {
        try
        {
        }
        catch (Exception e)
        {
            throw new ManagementException(e.getMessage(), e);
        }
    }
}
