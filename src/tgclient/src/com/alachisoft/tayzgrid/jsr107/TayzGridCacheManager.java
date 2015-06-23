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

package com.alachisoft.tayzgrid.jsr107;

import com.alachisoft.tayzgrid.common.CacheConfigParams;
import com.alachisoft.tayzgrid.config.newdom.BackingSource;
import com.alachisoft.tayzgrid.config.newdom.ExpirationPolicy;
import com.alachisoft.tayzgrid.config.newdom.Provider;
import com.alachisoft.tayzgrid.config.newdom.Readthru;
import com.alachisoft.tayzgrid.config.newdom.Writethru;
import com.alachisoft.tayzgrid.jsr107.configuration.CacheConfiguration;
import com.alachisoft.tayzgrid.jsr107.configuration.JSR107InitParams;
import com.alachisoft.tayzgrid.jsr107.spi.TayzGridCachingProvider;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.web.caching.TayzGrid;
import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.spi.CachingProvider;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class TayzGridCacheManager implements CacheManager{
    
    private TayzGridCachingProvider cacheProvider;
    //private ClassLoader classLoader;
    private URI uri;
    private Properties cacheProperties;
    private ConcurrentHashMap<String, TayzGridCache> managedCaches = new ConcurrentHashMap<String, TayzGridCache>();
    private HashSet<String> allCaches;
    public TayzGridCacheManager(TayzGridCachingProvider cacheProvider, ClassLoader classLoader, URI uri, Properties cacheProperties) throws ConfigurationException
    {
        this.cacheProvider = cacheProvider;        
        //this.classLoader = classLoader;
        this.uri = uri;
        this.cacheProperties = cacheProperties;
        File file = new File(uri);
        allCaches = GetCacheNames(file.getAbsolutePath());
        
    }

    @Override
    public CachingProvider getCachingProvider() {
        return cacheProvider;
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }

    @Override
    public Properties getProperties() {
        return cacheProperties;
    }

    @Override
    public <K, V, C extends Configuration<K, V>> javax.cache.Cache<K, V> createCache(String cacheName, C c) throws IllegalArgumentException {
        if(cacheName == null)
            throw new NullPointerException("cacheName cannot be null.");
        if(c == null)
            throw new NullPointerException("Configurations cannot be null");
        if(allCaches.contains(cacheName))
            throw new CacheException("Cache already exists.");
        
        JSR107InitParams initParams = null;
        if(c instanceof CacheConfiguration)
            initParams = ((CacheConfiguration)c).getInitParamsInternal();
        else
            initParams = new JSR107InitParams();
        //Assign reader and write factories
        Iterable<CacheEntryListenerConfiguration<K, V>> cacheEntryListeners = null;
        if(c instanceof CompleteConfiguration)
        {
            CompleteConfiguration config = (CompleteConfiguration<K, V>)c;
            BackingSource backingSource = new BackingSource();
            
            boolean isBSEnabled = false;
            
            if(config.isReadThrough() && config.getCacheLoaderFactory() == null) {
                throw new IllegalArgumentException("CacheLoader Factory cannot be null.");
            }
            else if(config.getCacheLoaderFactory() != null) {
                initParams.getCacheServerConfig().setCacheLoaderFactory(config.getCacheLoaderFactory());
                Readthru readthru = new Readthru();
                if(!config.isReadThrough()) 
                    initParams.getCacheServerConfig().setIsLoaderOnly(true);
                readthru.setEnabled(true);
                backingSource.setReadthru(readthru);
                isBSEnabled = true;
                
            }
            if(config.isWriteThrough())
            {
                if(config.getCacheWriterFactory() == null)
                    throw new IllegalArgumentException("CacheWriter Factory cannot be null.");
                initParams.getCacheServerConfig().setCacheWriterFactory(config.getCacheWriterFactory());
                
                Writethru writethru = new Writethru();
                writethru.setEnabled(true);
                backingSource.setWritethru(writethru);
                isBSEnabled = true;
           //     initParams.getCacheServerConfig().getCacheSettings().setBackingSource(backingSource);
            }
            if(isBSEnabled)
                initParams.getCacheServerConfig().getCacheSettings().setBackingSource(backingSource);
            
            if(config.getExpiryPolicyFactory()!= null)
            {
                ExpiryPolicy expiryPolicy = (ExpiryPolicy)config.getExpiryPolicyFactory().create();
                if(expiryPolicy != null)
                {
                    boolean isSliding = false;
                    Duration expiry = expiryPolicy.getExpiryForCreation();
                    if(expiry== null)
                        expiry = expiryPolicy.getExpiryForUpdate();
                    if(expiry==null)
                    {
                        expiry = expiryPolicy.getExpiryForAccess();
                        isSliding = expiry!=null;
                    }
                    
                    if(expiry!= null && !expiry.isEternal())
                    {
                        //Set absolute expiration as default cache expiration
                        com.alachisoft.tayzgrid.config.newdom.ExpirationPolicy cacheExpiration = new ExpirationPolicy();
                        if(isSliding)
                            cacheExpiration.setPolicyType("sliding");
                        else
                            cacheExpiration.setPolicyType("absolute");
                        cacheExpiration.setDuration(expiry.getTimeUnit().toMillis(expiry.getDurationAmount()));
                        cacheExpiration.setUnit("milliseconds");
                        initParams.getCacheServerConfig().getCacheSettings().setExpirationPolicy(cacheExpiration);
                    }
                }
            }
            cacheEntryListeners = config.getCacheEntryListenerConfigurations(); 
        }    
        
        if(c.isStoreByValue())
            initParams.getCacheServerConfig().getCacheSettings().setDataFormat("Object");
        try {
            com.alachisoft.tayzgrid.web.caching.Cache tgCache = TayzGrid.initializeCache(cacheName, initParams);
            TayzGridCache<K, V> cache = new TayzGridCache<K, V>(this, tgCache, c);
            if(cacheEntryListeners != null)
            {
                for(CacheEntryListenerConfiguration<K, V> listener : cacheEntryListeners)
                {
                    cache.registerCacheEntryListener(listener);
                }
            }
            managedCaches.put(cacheName, cache);
            allCaches.add(cacheName);
            return cache;
        } catch (Exception ex) {
            throw new javax.cache.CacheException(ex.getMessage(),ex);
        }
    }

    @Override
    public <K, V> javax.cache.Cache<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType) {
        if(cacheName == null)
            throw new NullPointerException("cacheName cannot be null.");
        
        try {
            TayzGridCache<K, V>  cache = managedCaches.get(cacheName);
            if(cache!= null)
            {
                Configuration config = cache.getConfiguration(Configuration.class);
                if(config.getKeyType() != keyType)
                    throw new IllegalArgumentException("Specified key type is incompatible with configured cache.");
                if(config.getValueType()!= valueType)
                    throw new IllegalArgumentException("Specified value type is incompatible with configured cache.");
                return cache;
            }
            com.alachisoft.tayzgrid.web.caching.Cache tgCache = TayzGrid.initializeCache(cacheName);
            cache = new TayzGridCache<K, V>(this, tgCache, new MutableConfiguration<K, V>().setTypes(keyType, valueType));
            managedCaches.put(cacheName, cache);
            return cache;
        } catch (Exception ex) {
            throw new javax.cache.CacheException(ex.getMessage(),ex);
        }  
    }

    @Override
    public <K, V> javax.cache.Cache<K, V> getCache(String cacheName) {
        if(cacheName == null)
            throw new NullPointerException("cacheName cannot be null.");
        
        try {
            TayzGridCache<K, V>  cache = managedCaches.get(cacheName);
            if(cache!= null)
                return cache;
            com.alachisoft.tayzgrid.web.caching.Cache tgCache = TayzGrid.initializeCache(cacheName);
            MutableConfiguration<K,V> configuration = new MutableConfiguration<K, V>();
            CacheConfigParams cacheConfigParams = tgCache.getCacheConfigurationInternal();
            configuration.setReadThrough(cacheConfigParams.getIsReadThru());
            configuration.setWriteThrough(cacheConfigParams.getIsWriteThru());
            configuration.setStatisticsEnabled(cacheConfigParams.getIsStatisticsEnabled());
            cache = new TayzGridCache<K, V>(this, tgCache, configuration);
            managedCaches.put(cacheName, cache);
            return cache;
        }
        catch (Exception ex) {
            throw new javax.cache.CacheException(ex.getMessage(),ex);
        }  
    }

    @Override
    public Iterable<String> getCacheNames() {
        return (HashSet<String>)(allCaches.clone());
    }

    @Override
    public void destroyCache(String cacheName) {
        allCaches.remove(cacheName);
        TayzGridCache cache = managedCaches.remove(cacheName);
        if(cache!=null)
            cache.dispose();
    }

    @Override
    public void enableManagement(String cacheName, boolean bln) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void enableStatistics(String cacheName, boolean bln) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isClosed() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public static HashSet<String> GetCacheNames(String path) throws ConfigurationException
    {
        File file = null;
        HashSet<String> cacheNames = new HashSet<String>();
        try
        {
            //String path = DirectoryUtil.getConfigPath(fileName);
            file = new File(path);
            if (!file.exists())
            {
                return cacheNames;
            }
            Document configuration = null;

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = null;

            try
            {
                builder = builderFactory.newDocumentBuilder();
            }
            catch (ParserConfigurationException ex)
            {
            }

            configuration = builder.parse(file);
            NodeList cacheList = configuration.getElementsByTagName("cache");

            for (int i = 0; i < cacheList.getLength(); i++)
            {
                Node cache = cacheList.item(i);
                if (cache.hasAttributes())
                {
                    String cacheName = cache.getAttributes().getNamedItem("id").getNodeValue();
                    if(cacheName!=null || !cacheName.equals(""))
                        cacheNames.add(cacheName.toLowerCase());
                }
            }
        }
        catch (Exception e)
        {
            throw new ConfigurationException("An error occured while reading config file. " + e.getMessage());
        }
        finally
        {
        }
        return cacheNames;
    }

}
