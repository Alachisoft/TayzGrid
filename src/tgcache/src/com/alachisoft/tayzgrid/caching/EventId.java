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

package com.alachisoft.tayzgrid.caching;

import com.alachisoft.tayzgrid.caching.queries.QueryChangeType;
import com.alachisoft.tayzgrid.common.ICloneable;
import com.alachisoft.tayzgrid.persistence.EventType;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class EventId implements Cloneable,ICloneable, InternalCompactSerializable, java.io.Serializable {

    private String _eventUniqueId;
    private long _operationCounter;
    private int _eventCounter;
    private QueryChangeType _queryChangeType = QueryChangeType.None;
    private EventType _eventType;
    private String _queryId;
    private int _hashCode = -1;

    public EventId() {
    }

    public EventId(String eventUniqueId, long operationCounter, int eventCounter) 
    {
        _eventUniqueId = eventUniqueId;
        _operationCounter = operationCounter;
        _eventCounter = eventCounter;
    }

    public final String getEventUniqueID() {
        return _eventUniqueId;
    }

    public final void setEventUniqueID(String value) {
        _eventUniqueId = value;
    }

    public final long getOperationCounter() {
        return _operationCounter;
    }

    public final void setOperationCounter(long value) {
        _operationCounter = value;
    }

    public final int getEventCounter() {
        return _eventCounter;
    }

    public final void setEventCounter(int value) {
        _eventCounter = value;
    }

    public final QueryChangeType getQueryChangeType() {
        return _queryChangeType;
    }

    public final void setQueryChangeType(QueryChangeType value) {
        _queryChangeType = value;
    }

    public final EventType getEventType() {
        return _eventType;
    }

    public final void setEventType(EventType value) {
        _eventType = value;
    }

    public final String getQueryId() {
        return _queryId;
    }

    public final void setQueryId(String value) {
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(value)) {
            _queryId = value;
        }
    }

    public static EventId CreateEventId(OperationID opId) {
        EventId eventId = new EventId();
        eventId._eventUniqueId = opId.getOperationId();
        eventId._operationCounter = opId.getOpCounter();
        return eventId;
    }

    @Override
    public int hashCode() {
        if (_hashCode == -1 && _eventUniqueId == null) {
            return super.hashCode();
        } else if (_hashCode == -1) {
            _hashCode = (_eventUniqueId + (new Integer(_eventCounter)).toString() + ":" + (new Long(getOperationCounter())).toString() + ":" + _eventType.toString() + ":" + _queryChangeType.toString() + ":" + _queryId).hashCode();
        }

        return _hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        EventId eventId = (EventId) obj;
        if (this.getEventUniqueID().equals(eventId.getEventUniqueID()) && this.getEventCounter() == eventId.getEventCounter() && this.getOperationCounter() == eventId.getOperationCounter() && this.getQueryChangeType() == eventId.getQueryChangeType() && this.getEventType() == eventId.getEventType() && this._queryId.equals(eventId._queryId)) {
            return true;
        }
        return false;
    }

    public final Object clone() {
        EventId ei = new EventId();
        synchronized (this) {
            ei._eventUniqueId = _eventUniqueId;
            ei._operationCounter = _operationCounter;
            ei._eventCounter = _eventCounter;
            ei._queryChangeType = _queryChangeType;
            ei._eventType = _eventType;
            ei._queryId = _queryId;
        }
        return ei;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException 
    {         
        _eventCounter = reader.ReadInt32();
        _eventUniqueId = (String) reader.ReadObject();
        _operationCounter = reader.ReadInt64();
        _eventType = (EventType) reader.ReadObject();
        _queryChangeType = (QueryChangeType) reader.ReadObject();
        _queryId = (String) reader.ReadObject();
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {
        writer.Write(_eventCounter);
        writer.WriteObject(_eventUniqueId);
        writer.Write(_operationCounter);
        writer.Write(_eventType.getValue());
        writer.Write(_queryChangeType.getValue());
        writer.WriteObject(_queryId);
    }
}
