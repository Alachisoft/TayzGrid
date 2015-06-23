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

import com.alachisoft.tayzgrid.caching.EventId;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.io.Serializable;

/**
 *
 * @author 
 */
public class Event implements ICompactSerializable, Serializable
{
    private EventId _eventId;
    private EventInfo _eventInfo;

    public Event()
    {
        _eventId = new EventId();
        _eventInfo = new EventInfo();
    }

    public Event(EventId eId, EventInfo eInfo)
    {
        _eventId = eId;
        _eventInfo = eInfo;
    }


    public final EventId getPersistedEventId()
    {
        return _eventId;
    }
    public final void setPersistedEventId(EventId value)
    {
        _eventId = value;
    }

    public final EventInfo getPersistedEventInfo()
    {
        return _eventInfo;
    }
    public final void setPersistedEventInfo(EventInfo value)
    {
        _eventInfo = value;
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException {
        writer.writeObject(_eventId);
        writer.writeObject(_eventInfo);
    }

    @Override
    public void deserialize(CacheObjectInput reader) throws IOException, ClassNotFoundException {
        _eventId = (EventId)reader.readObject();
        _eventInfo = (EventInfo)reader.readObject();
    }
}
