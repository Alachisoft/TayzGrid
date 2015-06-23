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

import com.alachisoft.tayzgrid.common.ICloneable;
import com.alachisoft.tayzgrid.mapreduce.notification.TaskStatus;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

public class EventContext implements InternalCompactSerializable, Cloneable, java.io.Serializable
{
        private java.util.HashMap _fieldValueTable;

        public EventContext()
        {
        }

        public EventContext(EventContextFieldName fieldName, Object fieldValue)
        {
                Add(fieldName, fieldValue);
        }

	public final EventCacheEntry getItem()
	{
		return (EventCacheEntry)this.GetValueByField(EventContextFieldName.EventCacheEntry);
	}
	public final void setItem(EventCacheEntry value)
	{
		Add(EventContextFieldName.EventCacheEntry, value);
	}

	public final EventCacheEntry getOldItem()
	{
		return (EventCacheEntry)this.GetValueByField(EventContextFieldName.OldEventCacheEntry);
	}
	public final void setOldItem(EventCacheEntry value)
	{
		Add(EventContextFieldName.OldEventCacheEntry, value);
	}

        public final void Add(EventContextFieldName fieldName, Object fieldValue)
        {
                synchronized (this)
                {
                        if (_fieldValueTable == null)
                        {
                                _fieldValueTable = new java.util.HashMap();
                        }

                        _fieldValueTable.put(fieldName, fieldValue);
                }
        }

        public final Object GetValueByField(EventContextFieldName fieldName)
        {
                Object result = null;

                if (_fieldValueTable != null)
                {
                        result = _fieldValueTable.get(fieldName);
                }

                return result;
        }

        public final boolean Contains(EventContextFieldName fieldName)
        {
                boolean contains = false;

                if (_fieldValueTable != null)
                {
                        contains = _fieldValueTable.containsKey(fieldName);
                }

                return contains;
        }

        public final void RemoveValueByField(EventContextFieldName fieldName)
        {
               synchronized (this)
		{
			if (_fieldValueTable != null)
			{
				_fieldValueTable.remove(fieldName);
			}
		}

        }

        public final boolean HasEventID(EventContextOperationType operationType)
        {
                if (this.GetValueByField(EventContextFieldName.EventID) != null)
                {
                        return true;
                }
                return false;
        }

        public final EventId getEventID()
        {
                return (EventId)this.GetValueByField(EventContextFieldName.EventID);
        }

        public final void setEventID(EventId value)
        {
                Add(EventContextFieldName.EventID, value);
        }
        public final void setTaskStatus(TaskStatus status)
        {
            Add(EventContextFieldName.TaskStatus, status);
        }
        
        public final TaskStatus getTaskStatus()
        {
            return (TaskStatus)this.GetValueByField(EventContextFieldName.TaskStatus);
        }

        public final Object clone()
        {
              EventContext oc = new EventContext();
		synchronized (this)
		{
			if (oc._fieldValueTable == null)
			{
				oc._fieldValueTable = new java.util.HashMap();
			}
			else
			{
				oc._fieldValueTable.clear();
			}

			if (_fieldValueTable != null)
			{
				Iterator ide = _fieldValueTable.entrySet().iterator();
				while (ide.hasNext())
				{
                                    Entry entry = (Entry)ide.next();
                                    
					Object clone = entry.getValue() instanceof ICloneable ? ((ICloneable)entry.getValue()).clone()  : entry.getValue();
					oc._fieldValueTable.put(entry.getKey(), clone);
				}
			}
		}
		return oc;
        }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
         _fieldValueTable = (java.util.HashMap)reader.ReadObject();
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {
       synchronized (this)
		{
			writer.WriteObject(_fieldValueTable);
		}
    }
}
