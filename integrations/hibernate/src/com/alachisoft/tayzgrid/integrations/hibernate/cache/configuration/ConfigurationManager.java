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

import com.alachisoft.tayzgrid.common.configuration.ConfigurationBuilder;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;

public class ConfigurationManager {

    private static ConfigurationManager _singleton;
    private static Object _singletonLock = new Object();
    private ApplicationConfiguration _appConfig = null;
    private RegionConfigurationManager _regionConfigManager = null;

    private ConfigurationManager() throws ConfigurationException, FileNotFoundException, Exception {
        String appID = System.getProperty("tayzgrid.application_id");
        if (appID == null || appID.isEmpty()) {
            throw new ConfigurationException("tayzgrid.application-id not specified in System proprties.");
        }

        String configFilePath = this.GetFilePah("TayzGridHibernate.xml");
        ConfigurationBuilder configBuilder = new ConfigurationBuilder(configFilePath);
        configBuilder.RegisterRootConfigurationObject(ApplicationConfiguration.class);
        configBuilder.ReadConfiguration();

        Object[] configuraion = configBuilder.getConfiguration();
        boolean appConfigFound = false;
        if (configuraion != null && configuraion.length > 0) {
            for (int i = 0; i < configuraion.length; i++) {
                _appConfig = (ApplicationConfiguration) configuraion[i];
                if (_appConfig != null) {
                    if (_appConfig.getApplicationID() != null && _appConfig.getApplicationID().equalsIgnoreCase(appID)) {
                        appConfigFound = true;
                        break;
                    }
                }
            }
        }

        if (!appConfigFound) {
            throw new ConfigurationException("Invalid value of tayzgrid.application_id. Applicaion configuration not found for application-id = " + appID);
        }
        if (_appConfig.getDefaultRegion() == null || _appConfig.getDefaultRegion().isEmpty()) {
            throw new ConfigurationException("default-region cannot be null for application-id = " + _appConfig.getApplicationID());
        }

        _regionConfigManager = new RegionConfigurationManager(_appConfig.getCacheRegions());
        if (!_regionConfigManager.contains(_appConfig.getDefaultRegion())) {
            throw new ConfigurationException("Region's configuration not specified for default-region : " + _appConfig.getDefaultRegion());
        }

    }

    private String GetFilePah(String fileName) throws FileNotFoundException {
        String filePath = "";
        if (new File(fileName).exists()) {
            filePath = fileName;
        } else if (new File(".\\bin\\" + fileName).exists()) {
            filePath = ".\\bin\\" + fileName;
        } else if (new File(System.getenv("TG_HOME") + "\\config\\" + fileName).exists()) {
            filePath = System.getenv("TG_HOME") + "\\config\\" + fileName;
        } else {
            String envVatiableError = "";
            if (System.getenv("TG_HOME") == null || System.getenv("TG_HOME").isEmpty()) {
                envVatiableError = " Envronment variable TG_HOME not set to a valid directory path.";
            }
            throw new FileNotFoundException(fileName + " file not found.");
        }
        return filePath;
    }

    public static ConfigurationManager getInstance() throws ConfigurationException, FileNotFoundException, Exception {
        synchronized (_singletonLock) {
            if (_singleton == null) {
                _singleton = new ConfigurationManager();
            }
            return _singleton;
        }
    }

    public RegionConfiguraton getRegionConfiguration(String regionName) {
        RegionConfiguraton rConfig = _regionConfigManager.getRegionConfig(regionName);
        if (rConfig == null) {
            rConfig = _regionConfigManager.getRegionConfig(_appConfig.getDefaultRegion());
        }
        return rConfig;
    }

    public String getCacheKey(Object key) {
        String cacheKey = null;
        
            cacheKey = "HibernateTayzGrid:" + key.toString();
            if(!_appConfig.getKeyCaseSensitivity())
                cacheKey=cacheKey.toLowerCase();
        
        return cacheKey;
    }

    public boolean isExceptionEnabled() {
        return _appConfig.getCacheExceptionEnabled();
    }

    private String pad(String str, int size, String padChar) {
        StringBuffer padded = new StringBuffer(str);
        while (padded.length() < (size + str.length())) {
            padded.append(padChar);
        }
        return padded.toString();
    }
}
