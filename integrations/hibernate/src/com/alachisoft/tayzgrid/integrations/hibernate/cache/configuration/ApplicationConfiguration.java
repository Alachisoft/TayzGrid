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
