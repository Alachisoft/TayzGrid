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

package com.alachisoft.tayzgrid.jsr107.spi;

import com.alachisoft.tayzgrid.jsr107.TayzGridCacheManager;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.util.DirectoryUtil;
import java.io.File;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.OptionalFeature;
import javax.cache.spi.CachingProvider;


public class TayzGridCachingProvider implements CachingProvider{
    
    private static URI defaultURI;
    private static ConcurrentHashMap<URI, TayzGridCacheManager> cacheManagers = new ConcurrentHashMap<URI, TayzGridCacheManager>();

    public TayzGridCachingProvider() {
                try {
                    File file = new File(DirectoryUtil.getConfigPath("client.conf"));
            defaultURI = file.toURI();
        } catch (Exception ex) {
            throw new CacheException(ex);
        }
    }
    
    

    @Override
    public javax.cache.CacheManager getCacheManager(URI uri, ClassLoader classLoader, Properties properties) {
        try {
            TayzGridCacheManager manager = cacheManagers.get(uri);
            if(manager != null)
                return manager;
            manager =  new TayzGridCacheManager(this, classLoader, uri, properties);
            cacheManagers.put(uri, manager);
            return manager;
        } catch (ConfigurationException ex) {
            throw new CacheException(ex);
        }
    }

    @Override
    public ClassLoader getDefaultClassLoader() {
        return getClass().getClassLoader();
    }

    @Override
    public URI getDefaultURI() {
        return defaultURI;
    }

    @Override
    public Properties getDefaultProperties() {
        return null;
    }

    @Override
    public javax.cache.CacheManager getCacheManager(URI uri, ClassLoader classLoader) {
        return this.getCacheManager(uri, classLoader, null);
    }

    @Override
    public javax.cache.CacheManager getCacheManager() {
        return this.getCacheManager(getDefaultURI(), getDefaultClassLoader(), getDefaultProperties());
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void close(ClassLoader classLoader) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void close(URI uri, ClassLoader classLoader) {
        CacheManager manager = cacheManagers.remove(uri);
        if(manager != null)
            manager.close();
    }

    @Override
    public boolean isSupported(OptionalFeature optionalFeature) {
        if(optionalFeature == optionalFeature.STORE_BY_REFERENCE)
            return true;
        return false;
    }

}
