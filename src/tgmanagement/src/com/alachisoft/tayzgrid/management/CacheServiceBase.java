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

package com.alachisoft.tayzgrid.management;

import com.alachisoft.tayzgrid.caching.Cache;
import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import com.alachisoft.tayzgrid.management.CacheServer;
import com.alachisoft.tayzgrid.servicecontrol.CacheService;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.util.Iterator;
import java.util.Map;

public class CacheServiceBase
{

    /**
     * CacheServer object running on other nodes.      *
     */
    protected ICacheServer _server = null;

    /**
     * Address of the machine.
     */
    protected String _address;
    /**
     * Port
     */
    protected int _port;
    /**
     * IP address
     */
    private String _ipAddress;
    
    private boolean _useRemoting = false;

    /**
     * Constructor
     *
     * @param address
     */
    public CacheServiceBase(String address) throws Exception
    {
        this(address, CacheConfigManager.getTcpPort());
    }

    /**
     * Constructor
     *
     * @param address
     * @param port
     */
    public CacheServiceBase(String address, int port) throws Exception
    {
        _address = address;
        _port = port;

        Initialize();
        _ipAddress = _server.GetClusterIP();

    }

    /**
     * initilaise
     */
    protected void Initialize() throws Exception
    {
        CacheService ncache = null;

        if (_useRemoting)
        {
            ncache = new CacheService(_address, (long) _port, true);
        }
        else
        {
            ncache = new CacheRPCService(_address, _port);
        }

        try
        {
            _server = ncache.GetCacheServer(new TimeSpan());
        }
        catch (Exception e)
        {
            throw e;
        }
        finally
        {
            ncache.dispose();
        }
    }

    protected final ICacheServer getCacheServer()
    {
        return _server;
    }

    public final String getClusterIP()
    {
        return _ipAddress;
    }
}
