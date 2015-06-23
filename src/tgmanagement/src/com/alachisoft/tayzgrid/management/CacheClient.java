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
import com.alachisoft.tayzgrid.caching.CacheFactory;
import com.alachisoft.tayzgrid.common.exceptions.ManagementException;
import com.alachisoft.tayzgrid.servicecontrol.CacheService;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.net.UnknownHostException;


/**
 * Manages client side connection to caches.
 */
public final class CacheClient
{

    private CacheClient()
    {
    }

    /**
     * Initialize a registered cache given by the ID.
     *
     * @param cacheId
     * @param timeout
     * @exception ArgumentNullException cacheId is a null reference (Nothing in Visual Basic).
     * @return A reference to <see cref="Cache"/> object.
     */
    public static Cache GetCacheInstance(String cacheId) throws ConfigurationException, ManagementException, UnknownHostException
    {
        return GetCacheInstance(cacheId, new TimeSpan(30000));
    }

    /**
     * Initialize a registered cache given by the ID.
     *
     * @param cacheId
     * @param timeout
     * @exception ArgumentNullException cacheId is a null reference (Nothing in Visual Basic).
     * @return A reference to <see cref="Cache"/> object.
     */
    public static Cache GetCacheInstance(String cacheId, TimeSpan timeout) throws ConfigurationException, ManagementException, UnknownHostException
    {
        if (cacheId == null)
        {
            throw new IllegalArgumentException("cacheId");
        }
        try
        {
            CacheConfig data = CacheConfigManager.GetCacheConfig(cacheId);
            return GetCacheInstance(data, timeout, false);
        }
        catch (RuntimeException e)
        {
            throw e;
        }
    }

    /**
     * Initialize a registered cache given by the ID.
     *
     * @param data
     * @param timeout
     * @exception ArgumentNullException data is a null reference (Nothing in Visual Basic).
     * @return A reference to <see cref="Cache"/> object.
     */
    public static Cache GetCacheInstance(CacheConfig data, TimeSpan timeout, boolean autoStart) throws ConfigurationException, UnknownHostException
    {
        if (data == null)
        {
            throw new IllegalArgumentException("data");
        }
        try
        {
            if (data == null)
            {
                return null;
            }
            if (data.getUseInProc())
            {
                return CacheFactory.CreateFromPropertyString(data.getPropertyString());
            }

            Cache cache = ConnectCacheInstance(data, timeout, autoStart);
            return cache;
        }
        catch (RuntimeException e)
        {
            throw e;
        }
    }

    /**
     * Creates and returns an instance of NCache.
     *
     * @param data
     * @param timeout
     * @return A reference to <see cref="Cache"/> object.
     */
    private static Cache ConnectCacheInstance(CacheConfig data, TimeSpan timeout, boolean autoStart) throws UnknownHostException
    {
        CacheService ncache = new CacheService();
        try
        {
            ncache.setUseTcp(data.getUseTcp());
            ncache.setServerName(data.getServerName());
            ncache.setPort(data.getPort());
            if (ncache.getServerName() == null || ncache.getServerName().length() < 1 || ncache.getServerName().compareTo(".") == 0 || ncache.getServerName().compareTo("localhost")== 0)
            {
                ncache.setServerName(java.net.InetAddress.getLocalHost().getHostName());
            }

            return null;
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        finally
        {
            ncache.dispose();
        }
    }
}
