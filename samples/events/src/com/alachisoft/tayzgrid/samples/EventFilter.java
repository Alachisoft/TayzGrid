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
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListenerException;

public class EventFilter<K, V> implements CacheEntryEventFilter<K, V>, Serializable
{
    private EventType _eventType = EventType.All;
    
    public EventFilter(EventType eventType)
    {
        _eventType = eventType;
    }
   
    @Override
    public boolean evaluate(CacheEntryEvent<? extends K, ? extends V> event) throws CacheEntryListenerException
    {
        //System.out.println("EVENT: " + event.getEventType() + " -> " + event);

        if(_eventType == EventType.All)
            return true;

        switch(event.getEventType())
        {
            case CREATED:
                if(_eventType == EventType.ItemAdded
                || _eventType == EventType.ItemAddedRemoved
                || _eventType == EventType.ItemAddedUpdated)
                    return true;
            break;
            case UPDATED:
                if(_eventType == EventType.ItemUpdated
                || _eventType == EventType.ItemAddedUpdated
                || _eventType == EventType.ItemUpdatedRemoved)
                    return true;
            break;
            case EXPIRED:
                if(_eventType == EventType.ItemExpired)
                    return true;
            break;
            case REMOVED:
                if(_eventType == EventType.ItemRemoved
                || _eventType == EventType.ItemAddedRemoved
                || _eventType == EventType.ItemUpdatedRemoved)
                    return true;
            break;
        }
        return false;
    }   
}
