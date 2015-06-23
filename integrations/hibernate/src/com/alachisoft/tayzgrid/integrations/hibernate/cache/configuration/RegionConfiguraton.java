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
