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
 
package com.alachisoft.tayzgrid.web.persistence;

import com.alachisoft.tayzgrid.common.datastructures.SlidingIndex;
import com.alachisoft.tayzgrid.caching.EventId;

public class PersistenceManager
{
         
    private SlidingIndex<EventId> _index = new SlidingIndex<EventId>(30);

  
    private boolean isStoreEventsEnabled = false;

    private java.util.HashMap _duplicateEventLog;
    private boolean _checkEventDuplication = false;
    private java.util.Date _eventDuplicationStartTime = new java.util.Date(0);
    public final void StartEventDuplicationCheck()
    {
          
    }

    public final boolean StopEventDuplicationCheck()
    {
        return false;
    }

    private boolean CheckEventDuplication(EventId eventId)
    {
        synchronized (this)
        {
            if (_checkEventDuplication && !StopEventDuplicationCheck())
            {
                if (_duplicateEventLog.containsKey(eventId))
                {
                        return true;
                }
                else
                {
                        _duplicateEventLog.put(eventId, null);
                }
            }
            return false;
        }
    }

        

    public PersistenceManager(int interval)
    {
        _index = new SlidingIndex<EventId>(interval,true);
    }

    public final boolean PersistEvent(EventId evtId)
    {
         
        if (evtId != null)
        {
            return _index.AddToIndex(evtId);
        }

        return true;

    }

    public final java.util.ArrayList<com.alachisoft.tayzgrid.caching.EventId> GetPersistedEventsList()
    {
        java.util.Iterator en = _index.GetCurrentData();
        com.alachisoft.tayzgrid.caching.EventId evtId;
        java.util.ArrayList<com.alachisoft.tayzgrid.caching.EventId> events = new java.util.ArrayList<com.alachisoft.tayzgrid.caching.EventId>();

        while (en.hasNext())
        {
            evtId = (com.alachisoft.tayzgrid.caching.EventId)en.next();
            events.add(evtId);
        }
        return events;
    }

    public final void dispose()
    {
        _index = null;
    }
}
