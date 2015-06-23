
package com.alachisoft.tayzgrid.web.events;

import com.alachisoft.tayzgrid.jsr107.TayzGridCache;
import com.alachisoft.tayzgrid.web.caching.TayzGrid;
import javax.cache.Cache;
import javax.cache.event.*;

/**
 * The class that extends the CacheEntryEvent class (Of JCache) 
 * @param <K>
 * @param <V> 
 */


public class JCacheEntryEvent<K,V> extends CacheEntryEvent<K,V> {

    
    CacheEventArg _cacheEventArg;
    Object _key;
    
    public JCacheEntryEvent(TayzGridCache<K,V> source, final EventType eventType, CacheEventArg cacheEventArg, Object key) {
        super(source, eventType);
        _cacheEventArg = cacheEventArg;
        _key = key;
    }
    
    @Override
    public V getOldValue() {
        return isOldValueAvailable() ? (V)_cacheEventArg.getOldItem().getValue() : null;
    }

    @Override
    public boolean isOldValueAvailable() {
        return _cacheEventArg.getOldItem().getValue() != null;
    }

    @Override
    public K getKey() {
        return (K)_key;
    }

    @Override
    public V getValue() {
        return (V)_cacheEventArg.getItem().getValue();
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        if (type.isAssignableFrom(this.getClass())) 
            return type.cast(this);
        return null;
    }
    
}
