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

package com.alachisoft.tayzgrid.integrations.spring;

import com.alachisoft.tayzgrid.integrations.spring.configuration.TayzGridConfigurationManager;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public class TayzGridCacheManager implements CacheManager {

    private HashMap<String, Cache> caches = new HashMap<String, Cache>();
    private TayzGridConfigurationManager configurationManager = null;
    private Logger logger = null;
    private String logFilePath = null;

    public TayzGridCacheManager() {
    }

    public void setLogFilePath(String path) {
        logFilePath = path;
    }

    private void configureLogger() {
        try {
            logger = Logger.getLogger("com.alachisoft.tayzgrid.integrations.spring");
            logger.setLevel(Level.INFO);
            if (logFilePath == null) {
                logFilePath = System.getenv("TG_HOME");
                if (logFilePath == null) {
                    logFilePath = ".";
                } else {
                    logFilePath = logFilePath + File.separator + "log";
                }
            }
            if (!(new File(logFilePath)).exists()) {
                (new File(logFilePath)).mkdirs();
            }
            FileHandler fileTxt = new FileHandler(logFilePath + File.separator + "tayzgrid-spring-" + (new Date()).toString().replaceAll(":", "-") + ".txt");
            SimpleFormatter formatterTxt = new SimpleFormatter();
            fileTxt.setFormatter(formatterTxt);
            logger.addHandler(fileTxt);
        } catch (SecurityException ex) {
            Logger.getLogger(TayzGridCacheManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TayzGridCacheManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setTayzGridConfigurationManager(TayzGridConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    @Override
    public Cache getCache(String cacheName) {
        Cache cache = caches.get(cacheName);
        if (cache == null) {
            if (logger == null) {
                configureLogger();
            }
            try {
                cache = new TayzGridCache(cacheName, configurationManager.getCacheConfiguration(cacheName));
                caches.put(cacheName, cache);
            } catch (Exception ex) {
                Logger.getLogger(TayzGridCacheManager.class.getName()).log(Level.SEVERE, ex.getMessage());
            }
        }
        return cache;
    }

    @Override
    public Collection<String> getCacheNames() {
        return caches.keySet();
    }

    @Override
    protected void finalize() throws Throwable {
        for (Cache cache : caches.values()) {
            ((TayzGridCache) cache).dispose();
        }
        super.finalize();
    }
}
