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

 
package com.alachisoft.tayzgrid.web.management;

import com.alachisoft.tayzgrid.management.CacheServerModerator;
import com.alachisoft.tayzgrid.common.exceptions.ManagementException;

public class CacheManager
{

    /**
     * Starts an out-proc cache. The end result is the same as that of starting the cache using Cache Manager. It starts the cache on the specified cache server. If the Cache
     * Server could not be contacted then it throws a <see cref="Alachisoft.NCache.Runtime.Exceptions.ManagementException"/> exception.
     *
     * @param cacheId
     * @param serverName
     * @param primaryUserCredentials
     * @param secondaryUserCredentials
     * @throws ManagementException
     * @throws SecurityException
     */
    public static void startCache(String cacheId, String serverName) throws ManagementException
    {
        int maxTries = 2;
        do
        {
            try
            {
                  CacheServerModerator.StartCache(cacheId, serverName, null, null);
                return;
            }
            
            catch (ManagementException e)
            {
                throw e;
            }
        }
        while (maxTries > 0);
    }

    /**
     * Stops an out-proc cache. The end result is the same as that of stoping the cache using Cache Manager. It stops the cache only on the same server where the client
     * application is running. If the Cache Server could not be contacted then it throws a <see cref="Alachisoft.NCache.Runtime.Exceptions.ManagementException"/> exception.
     *
     * @param cacheId
     * @param primaryUserCredentials
     * @param secondaryUserCredentials
     * @throws ManagementException
     * @throws SecurityException
     */
    public static void stopCache(String cacheId, boolean isGracefulShutDown) throws  ManagementException
    {
        int maxTries = 2;
        do
        {
            try
            {
                CacheServerModerator.StopCache(cacheId, null, null, isGracefulShutDown);
                return;
            }
            catch (ManagementException e)
            {
                throw e;
            }
        }
        while (maxTries > 0);
    }

    /**
     * Stops an out-proc cache. The end result is the same as that of stoping the cache using Cache Manager. It stops the cache on the specified cache server. If the Cache Server
     * could not be contacted then it throws a <see cref="Alachisoft.NCache.Runtime.Exceptions.ManagementException"/> exception.
     *
     * @param cacheId
     * @param serverName
     * @param primaryUserCredentials
     * @param secondaryUserCredentials
     * @throws ManagementException
     * @throws SecurityException
     */
    public static void stopCache(String cacheId, String serverName) throws ManagementException
    {
        int maxTries = 2;
        do
        {
            try
            {
                CacheServerModerator.StopCache(cacheId, serverName, null, null, false);
                return;
            }
            catch (ManagementException e)
            {
                throw e;
            }
        }
        while (maxTries > 0);
    }

    
    

    
    
    //======================================Get Cache Health APIs
    /**
     * Get health Information of the specified cache.
     * @param cacheName name of the cache
     * @throws ManagementException
     * @throws SecurityException
     */
    
    public static com.alachisoft.tayzgrid.runtime.cachemanagement.CacheHealth getCacheHealth(String cacheName) throws ManagementException {
        return getCacheHealth(cacheName, "", com.alachisoft.tayzgrid.runtime.cachemanagement.CacheContext.JvCache, 0);
    }
 /**
     * Get health Information of the specified cache.
     * @param cacheName name of the cache
     * @param initialNodeName Name of any server node the specified cache, from which the cache information will be collected
     * @throws ManagementException
     * @throws SecurityException
     */
    public static com.alachisoft.tayzgrid.runtime.cachemanagement.CacheHealth getCacheHealth(String cacheName, String initialNodeName) throws ManagementException {
        return getCacheHealth(cacheName, initialNodeName, com.alachisoft.tayzgrid.runtime.cachemanagement.CacheContext.JvCache, 0);
    }
 /**
     * Get health Information of the specified cache.
     * @param cacheName name of the cache
     * @param context context of the cache.
     * @throws ManagementException
     * @throws SecurityException
     */
    public static com.alachisoft.tayzgrid.runtime.cachemanagement.CacheHealth getCacheHealth(String cacheName, com.alachisoft.tayzgrid.runtime.cachemanagement.CacheContext context) throws ManagementException {
        return getCacheHealth(cacheName, "", context, 0);
    }
 /**
     * Get health Information of the specified cache.
     * @param cacheName name of the cache
     * @param initialNodeName Name of any server node the specified cache, from which the cache information will be collected
     * @param context context of the cache.
     * @throws ManagementException
     * @throws SecurityException
     */
    public static com.alachisoft.tayzgrid.runtime.cachemanagement.CacheHealth getCacheHealth(String cacheName, String initialNodeName, com.alachisoft.tayzgrid.runtime.cachemanagement.CacheContext context) throws ManagementException {
        return getCacheHealth(cacheName, initialNodeName, context, 0);
    }
 /**
     * Get health Information of the specified cache.
     * @param cacheName name of the cache
     * @param initialNodeName Name of any server node the specified cache, from which the cache information will be collected
     * @param context context of the cache.
     * @param port port where cache service will listen
     * @throws ManagementException
     * @throws SecurityException
     */
    public static com.alachisoft.tayzgrid.runtime.cachemanagement.CacheHealth getCacheHealth(String cacheName, String initialNodeName, com.alachisoft.tayzgrid.runtime.cachemanagement.CacheContext context, int port) throws ManagementException {

        if (cacheName.equals("")) {
            throw new IllegalArgumentException("cache name not provided");
        }
        try {
            return CacheServerModerator.getCacheHealth(cacheName.toLowerCase(), initialNodeName, context, port);
        } catch (ManagementException ex) {
            throw ex;
        }
    }
    //=======================================Get Cache Clients APIs
 /**
     * Get information about all clients connected to the specified cache.
     * @param cacheName name of the cache.
     * @throws ManagementException
     * @throws SecurityException
     */
    public static java.util.HashMap<com.alachisoft.tayzgrid.runtime.cachemanagement.ServerNode, java.util.ArrayList<com.alachisoft.tayzgrid.runtime.cachemanagement.CacheClient>> getCacheClients(String cacheName) throws ManagementException {
        return getCacheClients(cacheName, "", com.alachisoft.tayzgrid.runtime.cachemanagement.CacheContext.JvCache, 0);
    }
 /**
     * Get information about all clients connected to the specified cache.
     * @param cacheName name of the cache.
     * @param initialNodeName Name of any server node the specified cache, from which the cache information will be collected
     * @throws ManagementException
     * @throws SecurityException
     */
    public static java.util.HashMap<com.alachisoft.tayzgrid.runtime.cachemanagement.ServerNode, java.util.ArrayList<com.alachisoft.tayzgrid.runtime.cachemanagement.CacheClient>> getCacheClients(String cacheName, String initialNodeName) throws ManagementException {
        return getCacheClients(cacheName, initialNodeName, com.alachisoft.tayzgrid.runtime.cachemanagement.CacheContext.JvCache, 0);
    }
 /**
     * Get information about all clients connected to the specified cache.
     * @param cacheName name of the cache.
     * @param context context of the cache.
     * @throws ManagementException
     * @throws SecurityException
     */
    public static java.util.HashMap<com.alachisoft.tayzgrid.runtime.cachemanagement.ServerNode, java.util.ArrayList<com.alachisoft.tayzgrid.runtime.cachemanagement.CacheClient>> getCacheClients(String cacheName, com.alachisoft.tayzgrid.runtime.cachemanagement.CacheContext context) throws ManagementException {
        return getCacheClients(cacheName, "", context, 0);
    }
 /**
     * Get information about all clients connected to the specified cache.
     * @param cacheName name of the cache.
     * @param initialNodeName Name of any server node the specified cache, from which the cache information will be collected
     * @param context context of the cache.
     * @throws ManagementException
     * @throws SecurityException
     */
    public static java.util.HashMap<com.alachisoft.tayzgrid.runtime.cachemanagement.ServerNode, java.util.ArrayList<com.alachisoft.tayzgrid.runtime.cachemanagement.CacheClient>> getCacheClients(String cacheName, String initialNodeName, com.alachisoft.tayzgrid.runtime.cachemanagement.CacheContext context) throws ManagementException {
        return getCacheClients(cacheName, initialNodeName, context, 0);
    }

     /**
     * Get information about all clients connected to the specified cache.
     * @param cacheName name of the cache.
     * @param initialNodeName Name of any server node the specified cache, from which the cache information will be collected
     * @param context context of the cache.
     * @param port port where cache service will listen.
     * @throws ManagementException
     * @throws SecurityException
     */
    public static java.util.HashMap<com.alachisoft.tayzgrid.runtime.cachemanagement.ServerNode, java.util.ArrayList<com.alachisoft.tayzgrid.runtime.cachemanagement.CacheClient>> getCacheClients(String cacheName, String initialNodeName, com.alachisoft.tayzgrid.runtime.cachemanagement.CacheContext context, int port) throws ManagementException {
        if (cacheName.equals("")) {
            throw new IllegalArgumentException("cache name not provided");
        }
        try {
            return CacheServerModerator.getCacheClients(cacheName.toLowerCase(), initialNodeName, context, port);
        } catch (ManagementException ex) {
            throw ex;
        }
    }
}
