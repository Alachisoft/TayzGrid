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

import com.alachisoft.tayzgrid.management.ICacheServer;
import com.alachisoft.tayzgrid.management.CacheServer;
import com.alachisoft.tayzgrid.management.CacheConfigManager;
import com.alachisoft.tayzgrid.common.configuration.Activator;
import com.alachisoft.tayzgrid.common.exceptions.ManagementException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Represents the NCache remoting objects and allows you to start the service and get and instance of the CacheServer on a node.
 */
public class CacheService extends ServiceBase
{

    /**
     * Constructor
     */
    public CacheService() throws UnknownHostException
    {
    }

    /**
     * Overloaded Constructor
     *
     * @param server name of machine where the service is running.
     * @param useTcp use tcp channel for remoting.
     */

    public CacheService(String server, boolean useTcp) throws UnknownHostException
    {
        this(server, useTcp ? CacheConfigManager.getTcpPort() : CacheConfigManager.getHttpPort(), useTcp);
    }


    /**
     * Overloaded Constructor
     *
     * @param server name of machine where the service is running.
     * @param port port used by the remote server.
     * @param useTcp use tcp channel for remoting.
     */
    public CacheService(String server, long port, boolean useTcp) throws UnknownHostException
    {
        super(server, port, useTcp);
    }

    /**
     * Returns the instance of Cache manager running on the node, starts the service if not running.
     *
     * @return
     */
    public ICacheServer GetCacheServer(TimeSpan timeout) throws ManagementException,java.lang.Exception
    {
        ICacheServer cm = null;
        try
        {
            // Try to connect to cache manager first, saves time if the
            // service is already running.
            cm = ConnectCacheServer();
        }
        catch (ManagementException socketException)
        {
            try
            {
                Start(timeout);
                cm = ConnectCacheServer();

            }
            catch (ManagementException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new ManagementException(e.getMessage(), e);
            }
        }
        catch (Exception exception)
        {
            try
            {
                Start(timeout);
                cm = ConnectCacheServer();
            }
            catch (Exception e)
            {
                throw new ManagementException(e.getMessage(), e);
            }
        }
        return cm;
    }


    /**
     * Returns a running instance of CacheServer; does not start the service. If user specifies in the client application an ip address to server channels to bind to then this
     * method creates the server channel bound to that ip. also the object uri then uses the ip address instead of server name.
     *
     * @return
     */
    public ICacheServer ConnectCacheServer() throws ManagementException, IOException
    {

        return new CacheServer();
    }

    /**
     * Starts the NCache service on target machine.
     */
    protected final void Start(TimeSpan timeout) throws ManagementException
    {

    }
}
