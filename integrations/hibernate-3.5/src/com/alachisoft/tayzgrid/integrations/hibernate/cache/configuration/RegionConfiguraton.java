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
package com.alachisoft.tayzgrid.integrations.hibernate.cache.configuration;

import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;

public class RegionConfiguraton {
    private String _regionName;
    private String _cacheName;
    private String _priority="Default";
    private String _expirationType="none";
    private int _expirationPeriod=0;
    private CacheItemPriority _cItemPriority;
    
    public RegionConfiguraton()
    {
    }
    
    @ConfigurationAttributeAnnotation(value = "name", appendText = "")
    public String getRegionName()
    {
        return _regionName;
    }
    
    @ConfigurationAttributeAnnotation(value = "name", appendText = "")
    public void setRegionName(String value)
    {
        _regionName=value;
    }

    @ConfigurationAttributeAnnotation(value = "cache-name", appendText = "")
    public String getCacheName()
    {
        return _cacheName;
    }
    
    @ConfigurationAttributeAnnotation(value = "cache-name", appendText = "")
    public void setCacheName(String value )
    {
        _cacheName=value;
    }
    
    @ConfigurationAttributeAnnotation(value = "priority", appendText = "")
    public String getPriority()
    {
        return _priority;
    }
    
    @ConfigurationAttributeAnnotation(value = "priority", appendText = "")
    public void setPriority(String value)
    {
        _priority=value;
    }
    
    @ConfigurationAttributeAnnotation(value = "expiration-type", appendText = "")
    public String getExpirationType()
    {
        return _expirationType;            
    }
    
    @ConfigurationAttributeAnnotation(value = "expiration-type", appendText = "")
    public void setExpirationType(String value)
    {
        _expirationType=value;
    }
    
    @ConfigurationAttributeAnnotation(value = "expiration-period", appendText = "")
    public int getExpirationPeriod()
    {
        return _expirationPeriod;
    }
    
    @ConfigurationAttributeAnnotation(value = "expiration-period", appendText = "")
    public void setExpirationPeriod(int value)
    {
        _expirationPeriod=value;
    }
    
    public CacheItemPriority getCacheItemPriority()
    {
        return _cItemPriority;
    }
    
    public void setCacheItemPriority(CacheItemPriority value)
    {
        _cItemPriority=value;
    }
    
}
