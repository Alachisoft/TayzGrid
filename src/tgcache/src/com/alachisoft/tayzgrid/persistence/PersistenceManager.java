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

package com.alachisoft.tayzgrid.persistence;

import com.alachisoft.tayzgrid.caching.CallbackInfo;
import com.alachisoft.tayzgrid.caching.EventStatus;
import com.alachisoft.tayzgrid.common.datastructures.SlidingIndex;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.util.Iterator;

public class PersistenceManager
{
    private SlidingIndex<Event> _index = new SlidingIndex<Event>(30);
    private java.util.Date _startTime = new java.util.Date(0);
    private java.util.Date _finalTime = new java.util.Date(0);
    
    public PersistenceManager(int perInterval)
    {
        _index = new SlidingIndex<Event>(perInterval);
        _startTime = new java.util.Date();
    }

    public final boolean HasCompleteData() throws ArgumentException
    {
        _finalTime = new java.util.Date();
        TimeSpan timeElapsed = TimeSpan.Subtract(_startTime, _finalTime);
        return timeElapsed.getTotalSeconds() >= _index.GetInterval();
    }

    public final void AddToPersistedEvent(Event evt)
    {
        if (evt != null)
        {
            _index.AddToIndex(evt);
        }
    }

    public final java.util.ArrayList<com.alachisoft.tayzgrid.persistence.Event> GetFilteredEventsList(String clientID, java.util.HashMap events, EventStatus registeredEventStatus)
    {
        java.util.Iterator en = _index.GetCurrentData();
        Event evt;
        java.util.ArrayList<Event> filteredEvents = new java.util.ArrayList<Event>();

        while (en.hasNext())
        {
            evt = (Event)en.next();
            if (!events.containsKey((evt.getPersistedEventId())))
            {
                switch (evt.getPersistedEventId().getEventType())
                {
                    case CACHE_CLEARED_EVENT:
                        if (registeredEventStatus.getIsCacheClearedEvent())
                        {
                                filteredEvents.add(evt);
                        }
                        break;
                    case ITEM_REMOVED_EVENT:
                        if (registeredEventStatus.getIsItemRemovedEvent())
                        {
                                filteredEvents.add(evt);
                        }
                        break;
                    case ITEM_ADDED_EVENT:
                        if (registeredEventStatus.getIsItemAddedEvent())
                        {
                                filteredEvents.add(evt);
                        }
                        break;
                    case ITEM_UPDATED_EVENT:
                        if (registeredEventStatus.getIsItemUpdatedEvent())
                        {
                               filteredEvents.add(evt);
                        }
                        break;                       
                    case ITEM_REMOVED_CALLBACK:
                    case ITEM_UPDATED_CALLBACK:
                    for (Iterator it = evt.getPersistedEventInfo().getCallBackInfoList().iterator(); it.hasNext();)
                    {
                        CallbackInfo cbInfo = (CallbackInfo) it.next();
                        if (cbInfo != null && cbInfo.getClient() != null && cbInfo.getClient().equals(clientID))
                        {
                            if (!filteredEvents.contains(evt))
                            {
                                    filteredEvents.add(evt);
                            }
                        }
                    }
                    break;
                }
            }
        }
        return filteredEvents;
    }
    
    public final void dispose()
    {
            _index = null;
    }

}
