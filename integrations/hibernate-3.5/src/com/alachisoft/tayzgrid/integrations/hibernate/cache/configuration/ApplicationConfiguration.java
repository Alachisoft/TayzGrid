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
import com.alachisoft.tayzgrid.common.configuration.ConfigurationRootAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;

@ConfigurationRootAnnotation(value = "application-config")
public class ApplicationConfiguration {
    private String _applicationID;
    private boolean _cacheExceptionEnabled=true;
    private String _defaultRegion;
    private boolean _keyCaseSensitivity=false;
    private CacheRegions _cacheRegions;
    
    @ConfigurationAttributeAnnotation(value = "application-id", appendText = "")
    public String getApplicationID()
    {
        return _applicationID;
    }
    
    @ConfigurationAttributeAnnotation(value = "application-id", appendText = "")
    public void setApplicationID(String value)
    {
        _applicationID=value;
    }
    
    @ConfigurationAttributeAnnotation(value = "enable-cache-exception", appendText = "")
    public boolean getCacheExceptionEnabled()
    {
        return _cacheExceptionEnabled;
    }
    
    @ConfigurationAttributeAnnotation(value = "enable-cache-exception", appendText = "")
    public void setCacheExceptionEnabled(boolean value)
    {
        _cacheExceptionEnabled=value;
    }
    
    @ConfigurationAttributeAnnotation(value = "default-region-name", appendText = "")
    public String getDefaultRegion()
    {
        return _defaultRegion;
    }
    
    @ConfigurationAttributeAnnotation(value = "default-region-name", appendText = "")
    public void setDefaultRegion(String value)
    {
        _defaultRegion=value;
    }
    
        @ConfigurationAttributeAnnotation(value = "key-case-sensitivity", appendText = "")
    public boolean getKeyCaseSensitivity()
    {
        return _keyCaseSensitivity;
    }
    
    @ConfigurationAttributeAnnotation(value = "key-case-sensitivity", appendText = "")
    public void setKeyCaseSensitivity(boolean value)
    {
        _keyCaseSensitivity=value;
    }
    
    @ConfigurationSectionAnnotation(value = "cache-regions")
    public CacheRegions getCacheRegions()
    {
        return _cacheRegions;
    }
    
    @ConfigurationSectionAnnotation(value = "cache-regions")
    public void setCacheRegions(CacheRegions value)
    {
        _cacheRegions=value;
    }

}
