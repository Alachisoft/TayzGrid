/*
* ===============================================================================
* Alachisoft (R) TayzGrid Integrations
* TayzGrid Provider for Hibernate
* ===============================================================================
* Copyright © Alachisoft.  All rights reserved.
* THIS CODE AND INFORMATION IS PROVIDED "AS IS" WITHOUT WARRANTY
* OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT
* LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
* FITNESS FOR A PARTICULAR PURPOSE.
* ===============================================================================
*/
package com.alachisoft.tayzgrid.integrations.hibernate.cache;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.Timestamper;

public class TayzGridProvider implements org.hibernate.cache.CacheProvider{

    private static Logger _log=LogManager.getLogger("TayzGridProvider");
    
    public TayzGridProvider()
    {
    }
    
    @Override
    public Cache buildCache(String regionName, Properties properties) throws CacheException {
        if (regionName == null)
        {
            regionName = "";
        }
        if (properties == null)
        {
            properties = new Properties();
        }
        if (_log.isDebugEnabled())
        {
            StringBuilder sb = new StringBuilder();

            Enumeration<Object> enumK = properties.keys();
            Collection<Object> enumV = properties.values();
            for (int i = 0; i < properties.size(); i++)
            {
                sb.append("name=");
                sb.append(enumK.toString());
                sb.append("&value=");
                sb.append(enumV.toString());
                sb.append(";");
            }
            _log.debug("building cache with region: " + regionName + ", properties: " + sb.toString());
        }
        
        return new TayzGrid(regionName, properties);
    }

    @Override
    public long nextTimestamp() {
        return Timestamper.next();
    }

    @Override
    public void start(Properties prprts) throws CacheException {
        //do nothing
    }

    @Override
    public void stop() {
        //do nothing
    }

    @Override
    public boolean isMinimalPutsEnabledByDefault() {
        return false;
    }
    
}
