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
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;

public class SpringCacheConfiguration {

    private String springCacheName;
    private String tayzgridInstance;
    private String priority = "Default";
    private String expirationType = "none";
    private int expirationPeriod = 0;
    private CacheItemPriority cItemPriority;

    public SpringCacheConfiguration() {
    }

    @ConfigurationAttributeAnnotation(value = "name", appendText = "")
    public String getSpringCacheName() {
        return springCacheName;
    }

    @ConfigurationAttributeAnnotation(value = "name", appendText = "")
    public void setSpringCacheName(String value) {
        springCacheName = value;
    }

    @ConfigurationAttributeAnnotation(value = "tayzgrid-instance", appendText = "")
    public String getTayzgridInstanceName() {
        return tayzgridInstance;
    }

    @ConfigurationAttributeAnnotation(value = "tayzgrid-instance", appendText = "")
    public void setTayzgridInstanceName(String value) {
        tayzgridInstance = value;
    }

    @ConfigurationAttributeAnnotation(value = "priority", appendText = "")
    public String getPriority() {
        return priority;
    }

    @ConfigurationAttributeAnnotation(value = "priority", appendText = "")
    public void setPriority(String value) {
        priority = value;
    }

    @ConfigurationAttributeAnnotation(value = "expiration-type", appendText = "")
    public String getExpirationType() {
        return expirationType;
    }

    @ConfigurationAttributeAnnotation(value = "expiration-type", appendText = "")
    public void setExpirationType(String value) {
        expirationType = value;
    }

    @ConfigurationAttributeAnnotation(value = "expiration-period", appendText = "")
    public int getExpirationPeriod() {
        return expirationPeriod;
    }

    @ConfigurationAttributeAnnotation(value = "expiration-period", appendText = "")
    public void setExpirationPeriod(int value) {
        expirationPeriod = value;
    }

    public CacheItemPriority getCacheItemPriority() {
        return cItemPriority;
    }

    public void setCacheItemPriority(CacheItemPriority value) {
        cItemPriority = value;
    }
}
