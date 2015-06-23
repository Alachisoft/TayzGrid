
package com.alachisoft.tayzgrid.caching.datasourceproviders.JCache;
import com.alachisoft.tayzgrid.runtime.caching.ProviderCacheItem;

import java.util.HashMap;
import java.util.Map;
import javax.cache.integration.*;

import com.alachisoft.tayzgrid.runtime.datasourceprovider.*;
/**
 *Needs to be altered for generic keys
 *
 */


public class JCacheReadThruProvider<K,V> implements ReadThruProvider{
    private CacheLoader _jcacheLoader;
    public JCacheReadThruProvider(CacheLoader<K,V> jCacheLoader){       
        _jcacheLoader = jCacheLoader;
    }

    @Override
    public void init(HashMap parameters, String cacheId) throws Exception {
    }

    @Override
    public void loadFromSource(Object key, ProviderCacheItem cacheItem) throws Exception {
        V value =(V)_jcacheLoader.load((K)key);
        cacheItem.setValue(value);    
    }
     
    @Override
    public HashMap<Object, ProviderCacheItem> loadFromSource(Object[] keys) throws Exception {
       JCacheIterable<K> jKeys = new JCacheIterable<K>((K[])keys);
       HashMap<Object, ProviderCacheItem> values = new HashMap<Object, ProviderCacheItem>();
       K key = null;
       V value = null;
       ProviderCacheItem cacheItem = null;
       Map<K, V> jValues = null;
       jValues = _jcacheLoader.loadAll(jKeys);
       for(Map.Entry<K,V> entry: jValues.entrySet()){
               key = entry.getKey();
               value = entry.getValue();
               cacheItem = new ProviderCacheItem(value);
              // cacheItem.setValue(value);
               values.put(key, cacheItem);
               key = null;
               value = null;
           }
       return values;    
    }
    @Override
    public void dispose() throws Exception {
        if(_jcacheLoader instanceof java.io.Closeable)
            ((java.io.Closeable)_jcacheLoader).close();
    }
}
