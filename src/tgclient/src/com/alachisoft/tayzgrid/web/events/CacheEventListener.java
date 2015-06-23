
package com.alachisoft.tayzgrid.web.events;

import com.alachisoft.tayzgrid.jsr107.TayzGridCache;
import com.alachisoft.tayzgrid.runtime.events.EventType;
import static com.alachisoft.tayzgrid.runtime.events.EventType.ItemAdded;
import static com.alachisoft.tayzgrid.runtime.events.EventType.ItemRemoved;
import static com.alachisoft.tayzgrid.runtime.events.EventType.ItemUpdated;
import com.alachisoft.tayzgrid.web.caching.CacheDataModificationListener;
import com.alachisoft.tayzgrid.web.caching.CacheItemRemovedReason;
import java.util.ArrayList;
import javax.cache.Cache;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;

/**
 *
 * The class that implements the CacheDataModificationListener for wrapping JCache Events.
 */


public class CacheEventListener<K,V> implements CacheDataModificationListener {

    private final CacheEntryListener<K,V> _cacheEntryListener;
    private final TayzGridCache<K,V> _jCache;
    private final CacheEntryEventFilter<? super K,? super V> _cacheEntryEventFilter;
    private boolean _isOnCreatedListener = false;
    private boolean _isOnExpiredListener = false;
    private boolean _isOnRemovedListener = false;
    private boolean _isOnUpdatedListener = false;
    
    
    
    public CacheEventListener(CacheEntryListener<K,V> cacheEntryListener, final TayzGridCache<K,V> jCache, final CacheEntryListenerConfiguration<K,V> cacheEntryListenerConfiguration) {
        _cacheEntryListener = cacheEntryListener;
        _jCache = jCache;
        
        if(cacheEntryListenerConfiguration.getCacheEntryEventFilterFactory() != null) 
            _cacheEntryEventFilter = cacheEntryListenerConfiguration.getCacheEntryEventFilterFactory().create();
        else
            _cacheEntryEventFilter = null;
        
        _isOnCreatedListener = implementsMethod(CacheEntryCreatedListener.class);
        _isOnExpiredListener = implementsMethod(CacheEntryExpiredListener.class);
        _isOnRemovedListener = implementsMethod(CacheEntryRemovedListener.class);
        _isOnUpdatedListener = implementsMethod(CacheEntryUpdatedListener.class);
    }
    
    @Override
    public void cacheDataModified(Object key, CacheEventArg eventArgs) {
        javax.cache.event.EventType eventType = determineEventType(eventArgs);
        
        JCacheEntryEvent<K,V> jCacheEntryEvent = new JCacheEntryEvent<K, V>(_jCache, eventType, eventArgs, key);
        
        if(_cacheEntryEventFilter != null && !_cacheEntryEventFilter.evaluate(jCacheEntryEvent))
            return;
        
        ArrayList arrayList = new ArrayList();
        arrayList.add(jCacheEntryEvent);
        determineEventListener(arrayList, eventType);
    }

    @Override
    public void cacheCleared() {
        
    }
    
    private javax.cache.event.EventType determineEventType(CacheEventArg cacheEventArg) {
        javax.cache.event.EventType eventType = null;
        
        if(cacheEventArg.getEventType() == EventType.ItemAdded)
            eventType = javax.cache.event.EventType.CREATED;
        else if(cacheEventArg.getEventType() == EventType.ItemUpdated)
            eventType = javax.cache.event.EventType.UPDATED;
        else if(cacheEventArg.getEventType() == EventType.ItemRemoved) {
            if(cacheEventArg.getCacheItemRemovedReason() == CacheItemRemovedReason.Expired) 
                    eventType = javax.cache.event.EventType.EXPIRED;
                else
                    eventType = javax.cache.event.EventType.REMOVED;
        }
        return eventType;
    }
    
    private void determineEventListener(ArrayList arrayList, javax.cache.event.EventType eventType) {
        
        if(eventType == javax.cache.event.EventType.CREATED) {
            if(_isOnCreatedListener)
                ((CacheEntryCreatedListener<K, V>)_cacheEntryListener).onCreated(arrayList);
        }
        else if(eventType == javax.cache.event.EventType.EXPIRED) {
            if(_isOnExpiredListener)
                    ((CacheEntryExpiredListener<K,V>) _cacheEntryListener).onExpired(arrayList);
        }
        else if(eventType == javax.cache.event.EventType.REMOVED) {
            if(_isOnRemovedListener)
                ((CacheEntryRemovedListener<? super K, ? super V>) _cacheEntryListener).onRemoved(arrayList);
        }
         else if(eventType == javax.cache.event.EventType.UPDATED) {
             if(_isOnUpdatedListener) 
                    ((CacheEntryUpdatedListener<K, V>)_cacheEntryListener).onUpdated(arrayList);
         }
    }
    
    /**
     * Determines if the specific sub-interface has been implemented by the user or not.
     **/
    private boolean implementsMethod(Class cl) {
        return cl.isAssignableFrom(_cacheEntryListener.getClass());
    }
    
    public boolean isOnCreatedListenerConfigured() {
        return _isOnCreatedListener;
    }
    
    public boolean isOnExpiredListenerConfigured() {
        return _isOnExpiredListener;
    }
    
    public boolean isOnRemovedListenerConfigured() {
        return _isOnRemovedListener;
    }
    
    public boolean isOnUpdatedListenerConfigured() {
        return _isOnUpdatedListener;
    }
}
