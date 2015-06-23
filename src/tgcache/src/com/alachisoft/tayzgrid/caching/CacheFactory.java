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

package com.alachisoft.tayzgrid.caching;

import com.alachisoft.tayzgrid.config.PropsConfigReader;
import com.alachisoft.tayzgrid.config.ConfigReader;
import com.alachisoft.tayzgrid.config.XmlConfigReader;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.common.event.NEventStart;
import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import java.util.HashMap;
import java.util.Map;

public class CacheFactory 
{
/**
     * A cache factory is an object that creates named cache objects. It provides abstraction by shielding various cache creation and initialization tasks.
     *
     * Creates a cache object by reading in cofiguration parameters from a .NET XML file.
     *
     * @param configFileName Name and/or path of the configuration file.
     * @param configSection Name and/or ID of the section in the configuration file.
     * @return return the Cache object
     * @throws com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException
     */
    public final Cache CreateFromXmlConfig(String configFileName, String configSection) throws ConfigurationException
    {
        ConfigReader xmlReader = new XmlConfigReader(configFileName, configSection);
        return CreateFromProperties(xmlReader.getProperties(), null, null, null, null, null, null);
    }

    /**
     * Creates a cache object by reading in cofiguration parameters from a .NET XML file.
     *
     * @param configFileName Name and/or path of the configuration file.
     * @param configSection Name and/or ID of the section in the configuration file.
     * @param itemAdded item added handler
     * @param itemRemoved item removed handler
     * @param itemUpdated item updated handler
     * @param cacheCleared cache cleared handler
     * @return return the Cache object
     */
    public static Cache CreateFromXmlConfig(String configFileName, String configSection, NEventStart itemAdded, NEventStart itemRemoved, NEventStart itemUpdated, NEventStart cacheCleared, NEventStart customRemove, NEventStart customUpdate) throws ConfigurationException
    {
        ConfigReader xmlReader = new XmlConfigReader(configFileName, configSection);
        return CreateFromProperties(xmlReader.getProperties(), itemAdded, itemRemoved, itemUpdated, cacheCleared, customRemove, customUpdate);
    }

    /**
     * This overload is used to pass on the security credentials of the user to the clustering layer to avoid the possibility of joining a cluster to non-authorized nodes.
     *
     * @param propertyString
     * @param userId
     * @param password
     * @return
     * @throws com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException
     */
    public static Cache CreateFromPropertyString(String propertyString, String userId, String password) throws ConfigurationException
    {
        return CreateFromPropertyString(propertyString, userId, password, false);
    }

    /**
     * This overload is used to pass on the security credentials of the user to the clustering layer to avoid the possibility of joining a cluster to non-authorized nodes.
     *
     * @param propertyString
     * @param userId
     * @param password
     * @param isStartedAsMirror
     * @return
     * @throws com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException
     */
    public static Cache CreateFromPropertyString(String propertyString, String userId, String password, boolean isStartedAsMirror) throws ConfigurationException
    {
        ConfigReader propReader = new PropsConfigReader(propertyString);
        return CreateFromProperties(propReader.getProperties(), null, null, null, null, null, null, null, userId, password, isStartedAsMirror, false);
    }

    public static Cache CreateFromPropertyString(String propertyString, CacheServerConfig config, String userId, String password, boolean isStartedAsMirror, boolean twoPhaseInitialization) throws Exception
    {
        return CreateFromPropertyString(propertyString,config,userId,password,isStartedAsMirror,twoPhaseInitialization,false);
    }
    /**
     * This overload is used to pass on the security credentials of the user to the clustering layer to avoid the possibility of joining a cluster to non-authorized nodes.
     *
     * @param propertyString
     * @return
     */
    public static Cache CreateFromPropertyString(String propertyString, CacheServerConfig config, String userId, String password, boolean isStartedAsMirror, boolean twoPhaseInitialization,boolean inProc) throws Exception
    {
        ConfigReader propReader = new PropsConfigReader(propertyString);
        HashMap map = propReader.getProperties();
        java.util.Map cacheConfig = (java.util.Map) map.get("cache");
        if(cacheConfig != null) {
            cacheConfig.put("inproc", inProc);
        }
        return CreateFromProperties(map, config, null, null, null, null, null, null, userId, password, isStartedAsMirror, twoPhaseInitialization);
    }

    public static Cache CreateFromPropertyString(Map propertyString, CacheServerConfig config, String userId, String password, boolean isStartedAsMirror, boolean twoPhaseInitialization) throws ConfigurationException
    {
        return CreateFromProperties(propertyString, config, null, null, null, null, null, null, userId, password, isStartedAsMirror, twoPhaseInitialization);
    }

    /**
     * Creates a cache object by parsing configuration string passed as parameter.
     * @param propertyString
     * @return 
     * @throws com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException
     */
    public static Cache CreateFromPropertyString(String propertyString) throws ConfigurationException
    {
        ConfigReader propReader = new PropsConfigReader(propertyString);
        return CreateFromProperties(propReader.getProperties(), null, null, null, null, null, null);
    }

    /**
     * Creates a cache object by parsing configuration string passed as parameter.
     *
     * @param propertyString property string provided by the user
     * @param itemAdded item added handler
     * @param itemRemoved item removed handler
     * @param itemUpdated item updated handler
     * @param cacheCleared cache cleared handler
     * @return return the Cache object
     */
    public static Cache CreateFromPropertyString(String propertyString, NEventStart itemAdded, NEventStart itemRemoved, NEventStart itemUpdated, NEventStart cacheCleared, NEventStart customRemove, NEventStart customUpdate) throws ConfigurationException
    {
        ConfigReader propReader = new PropsConfigReader(propertyString);
        return CreateFromProperties(propReader.getProperties(), itemAdded, itemRemoved, itemUpdated, cacheCleared, customRemove, customUpdate);
    }

    /**
     * Internal method that actually creates the cache. A HashMap containing the config parameters is passed to this method.
     *
     * @param propertyTable contains the properties provided by the user in the for of Hashtable
     * @param itemAdded item added handler
     * @param itemRemoved item removed handler
     * @param itemUpdated item updated handler
     * @param cacheMiss cache miss handler
     * @param cacheCleared cache cleared handler
     * @return return the Cache object
     */
    private static Cache CreateFromProperties(java.util.Map properties, NEventStart itemAdded, NEventStart itemRemoved, NEventStart itemUpdated, NEventStart cacheCleared, NEventStart customRemove, NEventStart customUpdate) throws ConfigurationException
    {
        Cache cache = new Cache();

        if (itemAdded != null)
        {
            cache.addItemAddedListner(itemAdded, null);
        }
        if (itemRemoved != null)
        {
            cache.addItemRemovedListner(itemRemoved, null);
        }
        if (itemUpdated != null)
        {
            cache.addItemUpdatedListner(itemUpdated, null);
        }
        if (cacheCleared != null)
        {
            cache.addCacheClearedListner(cacheCleared, null);
        }
        if (customRemove != null)
        {
            cache.addCustomRemoveNotifListner(customRemove, null);
        }
        if (customUpdate != null)
        {
            cache.addCustomUpdateNotifListner(customUpdate, null);
        }

        cache.Initialize(properties, true, null, null);
        return cache;
    }

    /**
     * Internal method that actually creates the cache. A HashMap containing the config parameters is passed to this method.
     *
     * @param propertyTable contains the properties provided by the user in the for of Hashtable
     * @param itemAdded item added handler
     * @param itemRemoved item removed handler
     * @param itemUpdated item updated handler
     * @param cacheMiss cache miss handler
     * @param cacheCleared cache cleared handler
     * @return return the Cache object
     */
    private static Cache CreateFromProperties(java.util.Map properties, NEventStart itemAdded, NEventStart itemRemoved, NEventStart itemUpdated, NEventStart cacheCleared, NEventStart customRemove, NEventStart customUpdate, String userId, String password) throws ConfigurationException
    {
        return CreateFromProperties(properties, null, itemAdded, itemRemoved, itemUpdated, cacheCleared, customRemove, customUpdate, userId, password, false, false);
    }

    /**
     * Internal method that actually creates the cache. A HashMap containing the config parameters is passed to this method.
     *
     * @param propertyTable contains the properties provided by the user in the for of Hashtable
     * @param itemAdded item added handler
     * @param itemRemoved item removed handler
     * @param itemUpdated item updated handler
     * @param cacheMiss cache miss handler
     * @param cacheCleared cache cleared handler
     * @return return the Cache object
     */
    private static Cache CreateFromProperties(java.util.Map properties, CacheServerConfig config, NEventStart itemAdded, NEventStart itemRemoved, NEventStart itemUpdated, NEventStart cacheCleared, NEventStart customRemove, NEventStart customUpdate, String userId, String password, boolean isStartingAsMirror, boolean twoPhaseInitialization) throws ConfigurationException
    {
        Cache cache = new Cache();
        cache.setConfiguration(config);
        if (itemAdded != null)
        {
            cache.addItemAddedListner(itemAdded, null);
        }
        if (itemRemoved != null)
        {
            cache.addItemRemovedListner(itemRemoved, null);
        }
        if (itemUpdated != null)
        {
            cache.addItemUpdatedListner(itemUpdated, null);
        }
        if (cacheCleared != null)
        {
            cache.addCacheClearedListner(cacheCleared, null);
        }
        if (customRemove != null)
        {
            cache.addCustomRemoveNotifListner(customRemove, null);
        }
        if (customUpdate != null)
        {
            cache.addCustomUpdateNotifListner(customUpdate, null);
        }
        boolean inproc = false;
        java.util.Map cacheConfig = (java.util.Map) properties.get("cache");
        if(cacheConfig != null && cacheConfig.containsKey("inproc")) {
            inproc = (Boolean)cacheConfig.get("inproc");
        }
        
        cache.Initialize(properties, inproc, userId, password, isStartingAsMirror, twoPhaseInitialization);
        return cache;
    }
}
