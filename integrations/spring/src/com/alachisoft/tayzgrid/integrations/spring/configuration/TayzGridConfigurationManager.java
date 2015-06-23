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

import com.alachisoft.tayzgrid.common.configuration.ConfigurationBuilder;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TayzGridConfigurationManager {

    private String configFile = null;
    private ApplicationConfiguration appConfig;
    private SpringCacheConfigurationManager cacheConfigManager = null;
    private boolean initialized = false;

    public TayzGridConfigurationManager() {
    }

    public void initialize() throws ConfigurationException, FileNotFoundException, Exception {
        if (configFile == null) {
            throw new ConfigurationException("Configuration file not specified. Please make sure correct configuration file path is specified.");
        }
        ConfigurationBuilder configBuilder = new ConfigurationBuilder(configFile);
        configBuilder.RegisterRootConfigurationObject(ApplicationConfiguration.class);
        configBuilder.ReadConfiguration();

        Object[] configuraion = configBuilder.getConfiguration();
        boolean appConfigFound = false;
        if (configuraion != null && configuraion.length > 0) {
            appConfig = (ApplicationConfiguration) configuraion[0];
            appConfigFound = true;
        }

        if (!appConfigFound) {
            throw new ConfigurationException("TayzGrid configuration not found. Please make sure correct configuration file is used.");
        }

        if (appConfig.getDefaultCacheName()
                == null || appConfig.getDefaultCacheName().isEmpty()) {
            throw new ConfigurationException("default-cache-name cannot be null.");
        }

        cacheConfigManager = new SpringCacheConfigurationManager(appConfig.getCacheList());

        if (!cacheConfigManager.contains(appConfig.getDefaultCacheName())) {
            throw new ConfigurationException("Cache's configuration not specified for default-cache : " + appConfig.getDefaultCacheName());
        }
        initialized = true;
    }

    public void setConfigFile(String path) {
        configFile = path;
    }

    public SpringCacheConfiguration getCacheConfiguration(String cacheName) throws ConfigurationException, FileNotFoundException, Exception {
        if (!initialized) {
            this.initialize();
        }
        SpringCacheConfiguration config = cacheConfigManager.getCacheConfig(cacheName);
        if (config == null) {
            config = cacheConfigManager.getCacheConfig(appConfig.getDefaultCacheName());
            Logger.getLogger(TayzGridConfigurationManager.class.getName()).log(Level.WARNING, "Configuration not found for cache : {0}, using default cache configurations.", cacheName);
        }
        return config;
    }
}
