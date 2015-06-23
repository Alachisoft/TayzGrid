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

package com.alachisoft.tayzgrid.integrations.spring.configuration;

import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationRootAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;

@ConfigurationRootAnnotation(value = "application-config")
public class ApplicationConfiguration {

    private String applicationID;
    private boolean cacheExceptionEnabled = true;
    private String defaultRegion;
    private CacheList cacheRegions;

    @ConfigurationAttributeAnnotation(value = "application-id", appendText = "")
    public String getApplicationID() {
        return applicationID;
    }

    @ConfigurationAttributeAnnotation(value = "application-id", appendText = "")
    public void setApplicationID(String value) {
        applicationID = value;
    }

    @ConfigurationAttributeAnnotation(value = "enable-cache-exception", appendText = "")
    public boolean getCacheExceptionEnabled() {
        return cacheExceptionEnabled;
    }

    @ConfigurationAttributeAnnotation(value = "enable-cache-exception", appendText = "")
    public void setCacheExceptionEnabled(boolean value) {
        cacheExceptionEnabled = value;
    }

    @ConfigurationAttributeAnnotation(value = "default-cache-name", appendText = "")
    public String getDefaultCacheName() {
        return defaultRegion;
    }

    @ConfigurationAttributeAnnotation(value = "default-cache-name", appendText = "")
    public void setDefaultCacheName(String value) {
        defaultRegion = value;
    }

    @ConfigurationSectionAnnotation(value = "caches")
    public CacheList getCacheList() {
        return cacheRegions;
    }

    @ConfigurationSectionAnnotation(value = "caches")
    public void setCacheList(CacheList value) {
        cacheRegions = value;
    }
}
