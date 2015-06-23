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

import com.alachisoft.tayzgrid.common.DeleteParams;
import com.alachisoft.tayzgrid.common.InsertParams;
import com.alachisoft.tayzgrid.common.InsertResult;
import com.alachisoft.tayzgrid.processor.JCacheEntryProcessor;
import com.alachisoft.tayzgrid.processor.TayzGridEntryProcessorResult;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.runtime.events.EventType;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.processor.JCacheEntryProcessorResult;
import com.alachisoft.tayzgrid.web.caching.CacheItem;
import com.alachisoft.tayzgrid.web.caching.DSReadOption;
import com.alachisoft.tayzgrid.web.caching.DSWriteOption;
import com.alachisoft.tayzgrid.web.events.CacheEventDescriptor;
import com.alachisoft.tayzgrid.web.events.CacheEventListener;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.Factory;
import javax.cache.event.CacheEntryListener;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;


public class TayzGridCache<K, V> implements Cache<K, V>{
    
    com.alachisoft.tayzgrid.web.caching.Cache innerCache;
    boolean cacheClosed = false;
    CacheManager cacheManager;
    Configuration<K, V> cacheConfiguration;
    private CacheEventDescriptor addEventDescriptor = null;
    private CacheEventDescriptor updateEventDescriptor = null;
    private CacheEventDescriptor deleteEventDescriptor = null;
    private boolean isExpiryEventConfigured = false;
    
    
    public <C extends Configuration<K, V>> TayzGridCache(CacheManager manager, com.alachisoft.tayzgrid.web.caching.Cache innerCache, C cacheConfiguration)
    {
        cacheManager = manager;        
        this.innerCache = innerCache;
        this.cacheConfiguration = cacheConfiguration;
    }
    
    @Override
    public V get(K key) {
        verifyCacheConnectivity();
        if(key == null)
            throw new NullPointerException("key cannot be null.");

        try {
            return (V) innerCache.get(key, DSReadOption.OptionalReadThru);
        } 
        catch (Exception ex) {
            throw new CacheException(ex);
        }
    }

    @Override
    public Map<K, V> getAll(Set<? extends K> keys) {
        verifyCacheConnectivity();
        if(keys == null || keys.contains(null))
            throw new NullPointerException("key cannot be null.");

        try {
            return innerCache.getBulk(keys.toArray(), DSReadOption.OptionalReadThru);
        } 
        catch (Exception ex) {
            throw new CacheException(ex);
        }
    }

    @Override
    public boolean containsKey(K key) {
        verifyCacheConnectivity();
        if(key == null)
            throw new NullPointerException("key cannot be null.");
        try {
            return innerCache.contains(key);
        } 
        catch (Exception ex) {
            throw new CacheException(ex);
        }
    }

    @Override
    public void loadAll(Set<? extends K> keys, boolean replaceExistingValues, CompletionListener completionListener) {
        verifyCacheConnectivity();
        if(keys == null)
            throw new NullPointerException("keys cannot be null.");
        try {
            innerCache.getBulkInternal(keys, replaceExistingValues, completionListener);
        }
        catch(Exception e) {
            throw new CacheException();
        }
        
    }

    @Override
    public void put(K key, V value) {
        verifyCacheConnectivity();
        if(key == null)
            throw new NullPointerException("key cannot be null.");
        if(value == null)
            throw new NullPointerException("value cannot be null.");
        try {
            innerCache.insertInternal(key, value, DSWriteOption.OptionalWriteThru, null);
        } 
        catch (Exception ex) {
            throw new CacheException(ex);
        }
    }

    @Override
    public V getAndPut(K key, V value) {
        
        verifyCacheConnectivity();
        if(key == null)
            throw new NullPointerException("key cannot be null.");
        if(value == null)
            throw new NullPointerException("value cannot be null.");
        try {
            //--TODO--
            InsertParams options = new InsertParams();
            options.ReturnExistingValue = true;
            InsertResult result = innerCache.insertInternal(key, value, DSWriteOption.OptionalWriteThru, options);
            return (V)result.ExistingValue;
        } catch (Exception ex) {
            throw new CacheException(ex);
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        verifyCacheConnectivity();
        Object[] keys = new Object[map.size()];
        CacheItem[] cItems = new CacheItem[map.size()];
        int i = 0;
        for(Map.Entry e : map.entrySet())
        {
            if(e.getKey() == null)
                throw  new NullPointerException("key cannot be null.");
            if(e.getValue() == null)
                throw new NullPointerException("value cannot be null.");
            keys[i] = e.getKey();
            cItems[i] = new CacheItem(e.getValue());
            i++;            
        }
        try {
            innerCache.insertBulk(keys, cItems, DSWriteOption.OptionalWriteThru, null);
        } 
        catch (Exception ex) {
            throw new CacheException(ex);
        }
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        verifyCacheConnectivity();
        if(key==null)
            throw new NullPointerException("key cannot be null.");
        if(value == null)
            throw new NullPointerException("value cannot be null.");
        try {
            //--TODO--
            InsertParams options = new InsertParams();
            options.IsReplaceOperation = true;
            options.CompareOldValue = true;
            options.OldValue = null;
            InsertResult result = innerCache.insertInternal(key, value, DSWriteOption.OptionalWriteThru, options);
            return result.Success;
        } catch (Exception ex) {
            throw new CacheException(ex);
        }
    }

    @Override
    public boolean remove(K key) {
        verifyCacheConnectivity();
        if(key==null)
            throw new NullPointerException("key cannot be null.");
        try {
            return innerCache.deleteInternal(key, DSWriteOption.OptionalWriteThru, null);
        } 
        catch (Exception ex) {
            throw new CacheException(ex);
        }   
    }

    @Override
    public boolean remove(K key, V oldValue) {
        verifyCacheConnectivity();
        if(key==null)
            throw new NullPointerException("key cannot be null.");
        if(oldValue == null)
            throw new NullPointerException("value cannot be null.");
        try {
            DeleteParams params = new DeleteParams();
            params.CompareOldValue = true;
            params.OldValue = oldValue;
            return innerCache.deleteInternal(key, DSWriteOption.OptionalWriteThru, params);
        } 
        catch (Exception ex) {
            throw new CacheException(ex);
        } 
    }

    @Override
    public V getAndRemove(K key) {
        verifyCacheConnectivity();
        if(key==null)
            throw new NullPointerException("key cannot be null.");
        try {
            return (V) innerCache.remove(key, DSWriteOption.OptionalWriteThru, null);
        } catch (Exception ex) {
            throw new CacheException(ex);
        }
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        verifyCacheConnectivity();
        if(key==null)
            throw new NullPointerException("key cannot be null.");
        if(newValue == null)
            throw new NullPointerException("value cannot be null.");
        try {
            //--TODO--
            InsertParams options = new InsertParams();
            options.IsReplaceOperation = true;
            options.CompareOldValue = true;
            options.OldValue = oldValue;
            InsertResult result = innerCache.insertInternal(key, newValue, DSWriteOption.OptionalWriteThru, options);
            return result.Success;
        } catch (Exception ex) {
            throw new CacheException(ex);
        }
    }

    @Override
    public boolean replace(K key, V value) {
        verifyCacheConnectivity();
        if(key==null)
            throw new NullPointerException("key cannot be null.");
        if(value == null)
            throw new NullPointerException("value cannot be null.");
        try {
            //--TODO--
            InsertParams options = new InsertParams();
            options.IsReplaceOperation = true;
            InsertResult result = innerCache.insertInternal(key, value, DSWriteOption.OptionalWriteThru, options);
            return result.Success;
        } catch (Exception ex) {
            throw new CacheException(ex);
        }    
    }

    @Override
    public V getAndReplace(K key, V value) {
        verifyCacheConnectivity();
        if(key==null)
            throw new NullPointerException("key cannot be null.");
        if(value == null)
            throw new NullPointerException("value cannot be null.");
        try {
            //--TODO--
            InsertParams options = new InsertParams();
            options.IsReplaceOperation = true;
            options.ReturnExistingValue = true;
            InsertResult result = innerCache.insertInternal(key, value, DSWriteOption.OptionalWriteThru, options);
            return (V)result.ExistingValue;
        } catch (Exception ex) {
            throw new CacheException(ex);
        }     
    }

    @Override
    public void removeAll(Set<? extends K> keys) {
        verifyCacheConnectivity();
        if(keys.contains(null))
            throw new NullPointerException("key cannot be null.");
        try {
            //--TODO-- cachewriter for all keys must be called
            innerCache.deleteBulk(keys.toArray(), DSWriteOption.OptionalWriteThru, null);
        } catch (Exception ex) {
            throw new CacheException(ex);
        }
    }

    @Override
    public void removeAll() {
        verifyCacheConnectivity();
        try {
            innerCache.clear();
        } 
        catch (Exception ex) {
            throw new CacheException(ex);
        }    
    }

    @Override
    public void clear() {
        verifyCacheConnectivity();
        try {
            innerCache.clear();
        } 
        catch (Exception ex) {
            throw new CacheException(ex);
        }
    }

    @Override
    public <C extends Configuration<K, V>> C getConfiguration(Class<C> type) {
        verifyCacheConnectivity();
        return (C)cacheConfiguration;
    }

    @Override
    public <T> T invoke(K key, EntryProcessor<K, V, T> entryProcessor, Object... arguments) throws EntryProcessorException {
        
        try{
            if(key==null || entryProcessor==null)
                throw new NullPointerException("key or entryProcessor can not be null");
            
            verifyCacheConnectivity();
            
           return (T)innerCache.invokeEntryProcessor(key, new JCacheEntryProcessor(entryProcessor), arguments);
        }
        catch(Exception ex)
        {
            throw new EntryProcessorException(ex);
        }        
    }

    @Override
    public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> keys, EntryProcessor<K, V, T> entryProcessor, Object... arguments) 
    {
        HashMap<K,EntryProcessorResult<T>> result=new HashMap<K,EntryProcessorResult<T>>();
        try {
            if (keys == null || entryProcessor == null) {
                throw new NullPointerException("keys or entryProcessor can not be null");
            }
            verifyCacheConnectivity();
            Map tgMap=innerCache.invokeEntryProcessorBulk(keys.toArray(),  new JCacheEntryProcessor(entryProcessor), arguments);
            
            if(tgMap!=null && tgMap.size()>0)
            {
                Iterator it=tgMap.entrySet().iterator();
                while(it.hasNext())
                {
                    Map.Entry<K,com.alachisoft.tayzgrid.processor.TayzGridEntryProcessorResult> entry=(Map.Entry) it.next();
                    
                    K key=entry.getKey();
                    TayzGridEntryProcessorResult tgEntryProcessorResult=entry.getValue();
                    
                    JCacheEntryProcessorResult<T> jcacheResult=null;
                    try
                    {
                        jcacheResult=new JCacheEntryProcessorResult<T>();
                        jcacheResult.setValue((T)tgEntryProcessorResult.get());
                    }catch(com.alachisoft.tayzgrid.runtime.exceptions.EntryProcessorException ex)
                    {
                        if(ex.getCause() instanceof javax.cache.processor.EntryProcessorException)
                            jcacheResult.setException((EntryProcessorException) ex.getCause());
                        else
                            jcacheResult.setException(new EntryProcessorException(ex));
                    }
                    catch(ClassCastException ex)
                    {
                        jcacheResult.setException(new EntryProcessorException(ex));
                    }
                    finally
                    {
                        result.put(key, jcacheResult);
                    }
                }                
            }
            
        } catch (Exception ex) {
            throw new EntryProcessorException(ex);
        }
        
        return result;

    }

    @Override
    public String getName() {
        verifyCacheConnectivity();
        return innerCache.toString();
    }

    @Override
    public CacheManager getCacheManager() {
        verifyCacheConnectivity();
        return cacheManager;
    }

    @Override
    public void close() {
        verifyCacheConnectivity();
        cacheManager.destroyCache(this.getName());
        cacheClosed = true;
    }
    
    void dispose()
    {
        verifyCacheConnectivity();
        try {
            this.cacheClosed = true;
            innerCache.dispose();
        } catch (GeneralFailureException ex) {
            Logger.getLogger(TayzGridCache.class.getName()).log(Level.SEVERE, null, ex);
        } catch (OperationFailedException ex) {
            Logger.getLogger(TayzGridCache.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ConfigurationException ex) {
            Logger.getLogger(TayzGridCache.class.getName()).log(Level.SEVERE, null, ex);
        }  
    }

    @Override
    public boolean isClosed() {
        return cacheClosed;
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return (T) innerCache;
    }

    @Override
    public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
        verifyCacheConnectivity();
        final Factory<CacheEntryListener<? super K, ? super V> > factory = cacheEntryListenerConfiguration.getCacheEntryListenerFactory();
        final CacheEntryListener cacheEntryListener = factory.create();
        final CacheEventListener<K,V> cacheEventListener = new CacheEventListener<K, V>(cacheEntryListener, this, cacheEntryListenerConfiguration);
        try {
            EnumSet<EventType> eventType = null;
            if(cacheEventListener.isOnCreatedListenerConfigured()) {
                eventType = EnumSet.of(EventType.ItemAdded);
                addEventDescriptor = innerCache.addCacheDataModificationListener(cacheEventListener, eventType, EventDataFilter.DataWithMetaData);
            }
            if(cacheEventListener.isOnUpdatedListenerConfigured()) {
                eventType = EnumSet.of(EventType.ItemUpdated);
                updateEventDescriptor = innerCache.addCacheDataModificationListener(cacheEventListener, eventType, EventDataFilter.DataWithMetaData);
            }
            if(cacheEventListener.isOnExpiredListenerConfigured() || cacheEventListener.isOnRemovedListenerConfigured()) {
                if(cacheEventListener.isOnExpiredListenerConfigured())
                    isExpiryEventConfigured = true;
                eventType = EnumSet.of(EventType.ItemRemoved);
                deleteEventDescriptor = innerCache.addCacheDataModificationListener(cacheEventListener, eventType, EventDataFilter.DataWithMetaData);
            }
            
        }
        catch (Exception e) {
            
        }
    }

    @Override
    public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
        verifyCacheConnectivity();
        final Factory<CacheEntryListener<? super K,? super V>> factory = cacheEntryListenerConfiguration.getCacheEntryListenerFactory();
        final CacheEntryListener cacheEntryListener = factory.create();
        final CacheEventListener<K,V> cacheEventListener = new CacheEventListener<K, V>(cacheEntryListener, this, cacheEntryListenerConfiguration);
        try {
        if(cacheEventListener.isOnCreatedListenerConfigured() && addEventDescriptor != null) 
            innerCache.removeCacheDataModificationListener(addEventDescriptor);
        if(cacheEventListener.isOnUpdatedListenerConfigured() && updateEventDescriptor != null)
            innerCache.removeCacheDataModificationListener(updateEventDescriptor);
        if(cacheEventListener.isOnRemovedListenerConfigured() && deleteEventDescriptor != null) {
            if(!isExpiryEventConfigured)
                innerCache.removeCacheDataModificationListener(deleteEventDescriptor);
        }
        if(cacheEventListener.isOnExpiredListenerConfigured() && deleteEventDescriptor != null) {
            isExpiryEventConfigured = false;
            innerCache.removeCacheDataModificationListener(deleteEventDescriptor);
        }
        }
        catch (Exception e) {
            
        }
    }
    
    private void verifyCacheConnectivity()
    {
        if(cacheClosed)
            throw new IllegalStateException("Cache is closed.");
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
        //--TODO-- iterator may also remove item from cache
        verifyCacheConnectivity();
        try{
        return new TGCacheIterator<Entry<K, V>>(innerCache);
        }
        catch(Exception ex){
            throw new CacheException(ex);
        }
    }
    
    class TGCacheIterator<T extends Entry<K,V>> implements Iterator<T> 
    {
        Enumeration enumerator;
        com.alachisoft.tayzgrid.web.caching.Cache cache;
        com.alachisoft.tayzgrid.web.caching.Cache.Entry current;
        
        public TGCacheIterator(com.alachisoft.tayzgrid.web.caching.Cache cache)
        {
            enumerator = cache.getEnumerator();
            this.cache = cache;
        }

        @Override
        public boolean hasNext() {
            return enumerator.hasMoreElements();
        }

        @Override
        public T next() {
            try{
            current = (com.alachisoft.tayzgrid.web.caching.Cache.Entry)enumerator.nextElement();
            return (T)new JCacheEntry<K, V>(current);}
            catch(Exception ex){
                throw new CacheException(ex);
            }
        }

        @Override
        public void remove() {
            try {
                cache.delete(current.getKey(),DSWriteOption.OptionalWriteThru, null);
            } catch (Exception ex) {
                throw new CacheException(ex);
            }
        }
    }

}
