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
package com.alachisoft.tayzgrid.samples;

import java.io.Serializable;
import javax.cache.Cache;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;

public class EventListener<K, V> implements CacheEntryCreatedListener<K, V>, CacheEntryUpdatedListener<K, V>, CacheEntryExpiredListener<K, V>, CacheEntryRemovedListener<K, V>, Serializable {

    private Cache _cache;
    private EventType _eventType = EventType.All;
    private MutableCacheEntryListenerConfiguration _listenerConfig;

    public final boolean ShowMessage = true;
    boolean _isOldValueRequired = false;
    boolean _isSynchronous = false;

    //--- These objects are used for thread synchronization ...
    private final Object _syncPoint = new Object();

    public EventListener(Cache cache, int expectedEventCount) throws Exception {
        this(cache, EventType.All);
    }

    public EventListener(Cache cache, EventType eventType) throws Exception {
        this(cache, eventType, false, false);
    }

    public EventListener(Cache cache, EventType eventType, boolean isOldValueRequired, boolean isSynchronous) throws Exception {
        _cache = cache;
        _eventType = eventType;
        _isOldValueRequired = isOldValueRequired;
        _isSynchronous = isSynchronous;

        Register();
    }


    public final void Register() throws Exception {

        _listenerConfig = new MutableCacheEntryListenerConfiguration(
                FactoryBuilder.factoryOf(this),
                FactoryBuilder.factoryOf(new EventFilter(_eventType)),
                _isOldValueRequired,
                _isSynchronous
        );

        _cache.registerCacheEntryListener(_listenerConfig);
    }


    public void UnRegister() {
        if (_listenerConfig != null) {
            _cache.deregisterCacheEntryListener(_listenerConfig);
        }
    }

    @Override
    public void onCreated(Iterable<CacheEntryEvent<? extends K, ? extends V>> events) throws CacheEntryListenerException {
        synchronized (_syncPoint) {
            for (CacheEntryEvent event : events) {
                PrintEventInfo(event);
            }
        }
    }

    @Override
    public void onUpdated(Iterable<CacheEntryEvent<? extends K, ? extends V>> events) throws CacheEntryListenerException {
        synchronized (_syncPoint) {
            for (CacheEntryEvent event : events) {
                PrintEventInfo(event);
            }
        }
    }

    @Override
    public void onExpired(Iterable<CacheEntryEvent<? extends K, ? extends V>> events) throws CacheEntryListenerException {
        synchronized (_syncPoint) {
            for (CacheEntryEvent event : events) {
                PrintEventInfo(event);
            }
        }
    }

    @Override
    public void onRemoved(Iterable<CacheEntryEvent<? extends K, ? extends V>> events) throws CacheEntryListenerException {
        synchronized (_syncPoint) {
            for (CacheEntryEvent event : events) {
                PrintEventInfo(event);
            }
        }
    }


    protected void PrintEventInfo(CacheEntryEvent event) {
        if (ShowMessage) {
            System.out.println("Event on Key: " + event.getKey() + ", EventType: " + event.getEventType());
        }
    }

    protected void EventPrompt(String msg) {
        if (ShowMessage) {
            System.out.println("──► " + msg);
        }
    }  
}
