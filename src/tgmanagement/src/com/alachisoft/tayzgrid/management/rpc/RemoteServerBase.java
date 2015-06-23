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

package com.alachisoft.tayzgrid.management.rpc;

import com.alachisoft.tayzgrid.common.communication.IChannelFormatter;
import com.alachisoft.tayzgrid.common.communication.ITraceProvider;
import com.alachisoft.tayzgrid.common.communication.RequestManager;
import com.alachisoft.tayzgrid.common.communication.TcpChannel;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.net.DnsCache;



import java.net.InetAddress;
import java.net.UnknownHostException;

public abstract class RemoteServerBase implements IDisposable
{

    protected RequestManager _requestManager;
    protected String _server;
    protected int _port;
    protected String _bindIP;

    public RemoteServerBase(String server, int port) throws IllegalAccessException, Exception
    {
        this(server, port, null);

    }

    public RemoteServerBase(String server, int port, String bindIp) throws IllegalAccessException, Exception
    {
        if (server == null)
        {
            throw new IllegalAccessException("server");
        }
        try
        {
            InetAddress address = DnsCache.ResolveName(server);

        if (address == null)
        {
            throw new Exception("Failed to resolve server address to IP. Server =" + server);
        }
        _server = address.getHostAddress();
        }
        catch (UnknownHostException uhex)
        {
            throw new Exception(uhex);
            //throw new Exception("Cannot start cache on '" + server + "'. This operation might require other privileges.");
        }
        _port = port;
        _bindIP = bindIp;
    }
    
     public final int getRequestTimedout()
     {
	 return _requestManager.getRequestTimedout();
     }
     public final void setRequestTimedout(int value)
     {
             _requestManager.setRequestTimedout(value);
     }

    protected abstract IChannelFormatter GetChannelFormatter();

    protected boolean InitializeInternal()
    {
        return true;
    }

    public final void Initialize(ITraceProvider traceProvider) throws Exception
    {
        TcpChannel channel = new TcpChannel(_server, _port, _bindIP, traceProvider);
        channel.setFormatter(GetChannelFormatter());
        RequestManager requestManager = new RequestManager(channel);
        channel.Connect();
        _requestManager = requestManager;

        boolean initialized = false;
        try
        {
            initialized = InitializeInternal();
        }
        catch (RuntimeException e)
        {
            channel.Disconnect();
            _requestManager = null;
            throw e;
        }

        if (!initialized)
        {
            channel.Disconnect();
            _requestManager = null;
        }
    }

    public final void dispose()
    {
        if (_requestManager != null)
        {
            _requestManager.dispose();
        }
    }
}
