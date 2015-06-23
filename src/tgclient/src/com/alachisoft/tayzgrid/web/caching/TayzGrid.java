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

package com.alachisoft.tayzgrid.web.caching;

//~--- non-JDK imports --------------------------------------------------------
import com.alachisoft.tayzgrid.caching.CacheFactory;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import com.alachisoft.tayzgrid.management.CacheConfig;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;


//~--- JDK imports ------------------------------------------------------------
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheArgumentException;
import com.alachisoft.tayzgrid.statistics.PerfStatsCollector;
import com.alachisoft.tayzgrid.util.ConfigReader;
import com.alachisoft.tayzgrid.common.caching.expiration.ExpirationContract;
import com.alachisoft.tayzgrid.jsr107.configuration.JSR107InitParams;

public final class TayzGrid
{
    private static boolean _isExceptionEnabled = true;
    private static CacheCollection Caches = new CacheCollection();
    private static String _configPath = null;
    private int _refCounter = 0;

    //~--- methods ------------------------------------------------------------
    /**
     * Initializes the cache based on the cacheid. behaves in the same way as if you have called initializeCache(cacheid, null, 0);
     *
     * @return The cache instance.
     * @param cacheId The id of the cache to be initialized.
     * @throws CacheException Thrown incase of any error during intializaing the cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration
     * strings.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     */
    public static Cache initializeCache(String cacheId)
            throws ConfigurationException, GeneralFailureException, CacheException, Exception
    {
        if (cacheId == null)
        {
            throw new IllegalArgumentException("cacheId cannot be null.");
        }
        if (cacheId.trim().equals(""))
        {
            throw new IllegalArgumentException("cacheId cannot be an empty string.");
        }
        return initializeCache(cacheId, new CacheInitParams());

    }

    /**
     *
     * @param cacheId The cache id to initialize.
     * @param initParams initialization parameters, Security Credentials and other properties are provided here, if null is provided, default instance of CacheInitParams is used.
     * @return The cache instance.
     * @throws ConfigurationException
     * @throws GeneralFailureException
     * @throws CacheException
     * @see CacheInitParams
     */
    
    private static Cache initializeCache(String cacheId, String server, int port,
            CacheInitParams initParams)
            throws ConfigurationException, GeneralFailureException, CacheException, Exception
    {

        if (cacheId == null)
        {
            throw new IllegalArgumentException("cacheId cannot be null.");
        }
        if (cacheId.trim().equals(""))
        {
            throw new IllegalArgumentException("cacheId cannot be an empty string.");
        }
        CacheMode mode = initParams.getMode();

       
        //muds:
        //initialize cache call is made at max 2 times each time with different security credentials only
        //in case the first call throws a security exception.
        int maxTries = 2;
        try
        {
            CacheServerConfig config = null;

            if (mode != CacheMode.OutProc)
            {
                do
                {
                    try
                    {
                        if(initParams instanceof JSR107InitParams)
                            config = com.alachisoft.tayzgrid.config.newdom.DomHelper.convertToOldDom(((JSR107InitParams)initParams).getCacheServerConfig());
                        if(config==null)
                            config = com.alachisoft.tayzgrid.util.DirectoryUtil.GetCacheDom(cacheId, mode == CacheMode.InProc);

                    }
                    catch (Exception ex)
                    {
                        if (mode == CacheMode.Default)
                        {
                            mode = CacheMode.OutProc;
                        }
                        else
                        {
                            throw ex;
                        }
                    }
                    if (config != null)
                    {
                        if (config.getInProc())
                        {
                            ServicePropValues.loadServiceProp();
                        }
                        switch (mode)
                        {
                            case InProc:
                                config.setInProc(true);
                                break;
                            case OutProc:
                                config.setInProc(false);
                                break;
                        }
                    }
                    break; //muds: break the while loop...
                }
                while (maxTries > 0);
            }
            Cache primaryCache = null;
            synchronized (getCaches())
            {
                if (!getCaches().contains(cacheId))
                {
                    CacheImplBase cacheImpl = null;
//
                    if (config != null && config.getInProc())
                    {
                        com.alachisoft.tayzgrid.caching.Cache ncache = null;
                        Cache myCache = null;
                        maxTries = 2;
                        do
                        {
                            try
                            {

//                                if (config.getCacheType().equals("clustered-cache"))
//                                {
//                                    throw new Exception("Cannot start cache of clustered-type as in-proc cache.");
//                                }

                                CacheConfig cacheConfig = CacheConfig.FromDom(config);
                                
                                if (com.alachisoft.tayzgrid.web.caching.apilogging.DebugAPIConfigurations.isLoggingEnabled())
                                    myCache = new WrapperCache(new Cache(null, cacheId));
                                else
                                     
                                    myCache = new Cache(null, cacheId);
                                if(config.getExpirationPolicy() != null)
                                    myCache.setDefaultExpiration(new ExpirationContract(config.getExpirationPolicy().getPolicyType(), config.getExpirationPolicy().getDuration(), config.getExpirationPolicy().getUnit()));
      
                                    ncache = CacheFactory.CreateFromPropertyString(cacheConfig.getPropertyString(), config,
                                            null, null, false, false, true);
                                
                                cacheImpl = new InprocCache(ncache, cacheConfig, myCache);
                                myCache.setCacheImpl(cacheImpl);

                                if (primaryCache == null)
                                {
                                    primaryCache = myCache;
                                }
                                else
                                {
                                    primaryCache.addSecondaryInprocInstance(myCache);
                                }
                                break;
                            }
                            catch (Exception ex)
                            {
                                throw new CacheException(ex.getMessage());
                            }
                            
                        }
                        while (maxTries > 0);

                    }

                    else
                    {
                        maxTries = 2;
                        do
                        {
                                
                                PerfStatsCollector perfStatsCollector = new PerfStatsCollector(cacheId, false);
                            

                                if (com.alachisoft.tayzgrid.web.caching.apilogging.DebugAPIConfigurations.isLoggingEnabled())
                                    primaryCache = new WrapperCache(new Cache(null, cacheId, perfStatsCollector));
                                else
                                
                                    primaryCache = new Cache(null, cacheId, perfStatsCollector);
                                
                                perfStatsCollector.inializePerformanceCounters(false);
                                try
                                {
                                    cacheImpl = new RemoteCache(cacheId, primaryCache,server, port, initParams, perfStatsCollector);
                                }
                                catch (GeneralFailureException e)
                                {
                                    perfStatsCollector.dispose();
                                    throw e;
                                }
                                primaryCache.setCacheImpl(cacheImpl);
                                ((RemoteCache) cacheImpl).setParentCache(primaryCache);
                                break;
//                                   
                        }
                        while (maxTries > 0);
                    }
                    if (primaryCache != null)
                    {
                        // There are ultimately two different methods which are called when Cache is initialized,
                        // At both Methods Dynamic Compact Framework is initialized and at runtime Dynamic code is generated
                        // to create a User class's equivalnt Reflection code
                        try
                        {
                            getCaches().addCache(cacheId, primaryCache);

                        }
                        catch (Exception ex)
                        {
                            
                        }
                    }
                }
                else
                {
                    Cache c = (Cache) getCaches().getCache(cacheId);
                    c.addRef();
                    c.setExceptionsEnabled(getExceptionEnabled());

                    return c;
                }
            }

            getCaches().getCache(cacheId).setExceptionsEnabled(getExceptionEnabled());

            return (Cache) getCaches().getCache(cacheId);
        }
        catch (Exception e)
        {
            throw e;
        }
    }
    //internally used by sync cache

    public static Cache initializeCache(String cacheId, CacheInitParams initParams) throws Exception
    {
        if (cacheId == null)
        {
            throw new IllegalArgumentException("cacheId can not be null");
        }
        if (cacheId.trim().equals(""))
        {
            throw new IllegalArgumentException("cacheId cannot be an empty string");
        }

        if (initParams == null)
        {
            initParams = new CacheInitParams();
        }
        initParams.Initialize(cacheId);

        return initializeCache(cacheId, null, 0, initParams);
    }

    /**
     * @return returns Configuration Path
     */
    public static String getConfigPath()
    {

        return _configPath;
    }

    /**
     * Sets the directory path where the TGCache configuration files are placed.
     *
     * @param path The directory path of configuration files.
     */
    public static void setConfigPath(String path)
    {
        _configPath = path;
       
    }

    /**
     * @param value Flag that indicates whether exceptions are enabled or not. If this property is set the Cache object throws exceptions from public operations. If not set no
     * exception is thrown and the operation fails silently. Setting this flag is especially helpful during development phase of application since exceptions provide more
     * information about the specific causes of failure.
     * @see Cache
     */
    public static void setExceptionsEnabled(boolean value)
    {
        _isExceptionEnabled = value;
    }
    //~--- get methods --------------------------------------------------------

    /**
     * Maintains the list of running caches.
     *
     * @return the CacheCollection of the currently running caches.
     */
    public static CacheCollection getCaches()
    {
        return Caches;
    }

    /**
     * If this property is set the Cache object throws exceptions from public operations. If not set no exception is thrown and the operation fails silently. Setting this flag is
     * especially helpful during development phase of application since exceptions provide more information about the specific causes of failure.
     *
     * @return value Flag that indicates whether exceptions are enabled or not.
     */
    static boolean getExceptionEnabled()
    {
        return _isExceptionEnabled;
    }
}
//~ Formatted by Jindent --- http://www.jindent.com

