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

import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;

public class SpringCacheConfigurationManager {

    SpringCacheConfiguration[] _caches = null;

    public SpringCacheConfigurationManager(CacheList cacheList) throws ConfigurationException {
        if (cacheList == null || cacheList.getCaches() == null) {
            throw new ConfigurationException("No cache specified in configuration file.");
        }

        _caches = cacheList.getCaches();
        for (int i = 0; i < _caches.length; i++) {
            validateCacheConfig(_caches[i]);
        }
    }

    public SpringCacheConfiguration getCacheConfig(String cacheName) {
        for (int i = 0; i < _caches.length; i++) {
            if (_caches[i].getSpringCacheName().equals(cacheName)) {
                return _caches[i];
            }
        }
        return null;
    }

    public boolean contains(String cacheName) {
        return this.getCacheConfig(cacheName) != null;
    }

    private void validateCacheConfig(SpringCacheConfiguration config) throws ConfigurationException {

        if (config.getSpringCacheName() == null || config.getSpringCacheName().isEmpty()) {
            throw new ConfigurationException("cache name cannot be null or empty.");
        }
        if (config.getTayzgridInstanceName() == null || config.getTayzgridInstanceName().isEmpty()) {
            throw new ConfigurationException("tayzgrid-instance cannot be null for cache: " + config.getSpringCacheName());
        }
        if ((!config.getExpirationType().equalsIgnoreCase("absolute")) && (!config.getExpirationType().equalsIgnoreCase("sliding")) && (!config.getExpirationType().equalsIgnoreCase("none"))) {
            throw new ConfigurationException("Invalid value for expiraion-type for cache: " + config.getSpringCacheName());
        }
        if (!"none".equals(config.getExpirationType().toLowerCase())) {
            if (config.getExpirationPeriod() <= 0) {
                throw new ConfigurationException("Invalid value for expiraion-period in cache: " + config.getSpringCacheName() + ". Expiraion period must be greater than zero.");
            }
        }

        if (config.getPriority().equalsIgnoreCase("abovenormal")) {
            config.setCacheItemPriority(com.alachisoft.tayzgrid.runtime.CacheItemPriority.AboveNormal);
        } else if (config.getPriority().equalsIgnoreCase("belownormal")) {
            config.setCacheItemPriority(com.alachisoft.tayzgrid.runtime.CacheItemPriority.BelowNormal);
        } else if (config.getPriority().equalsIgnoreCase("default")) {
            config.setCacheItemPriority(com.alachisoft.tayzgrid.runtime.CacheItemPriority.Default);
        } else if (config.getPriority().equalsIgnoreCase("high")) {
            config.setCacheItemPriority(com.alachisoft.tayzgrid.runtime.CacheItemPriority.High);
        } else if (config.getPriority().equalsIgnoreCase("low")) {
            config.setCacheItemPriority(com.alachisoft.tayzgrid.runtime.CacheItemPriority.Low);
        } else if (config.getPriority().equalsIgnoreCase("normal")) {
            config.setCacheItemPriority(com.alachisoft.tayzgrid.runtime.CacheItemPriority.Normal);
        } else if (config.getPriority().equalsIgnoreCase("notremovable")) {
            config.setCacheItemPriority(com.alachisoft.tayzgrid.runtime.CacheItemPriority.NotRemovable);
        } else {
            throw new ConfigurationException("Invalid value for priority for cache: " + config.getSpringCacheName());
        }
    }
}
