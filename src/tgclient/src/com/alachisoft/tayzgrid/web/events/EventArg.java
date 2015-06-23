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

package com.alachisoft.tayzgrid.web.events;

import com.alachisoft.tayzgrid.runtime.events.EventType;
import java.util.EnumSet;

/** 
 Parent of <see cref="EventArg"/> and <see cref="CacheEventArg"/>
 Contains the necessary information related to the event being raised
*/
public abstract class EventArg
{
 
	private String _cacheName;
	private EventType _eventType;
	private EventCacheItem _item; //Internal value will be null if fetch data was off, will be completely null if no data upon return was specified
        private EventCacheItem _oldItem; //For insert only
	 

        public final EventCacheItem getOldItem()
	{
		return _oldItem;
	}
	public final void setOldItem(EventCacheItem value)
	{
		_oldItem = value;
	}
	/** 
	 Name of the cache the event is raised against
	*/
	public final String getCacheName()
	{
		return _cacheName;
	}

	/** 
	 Event Type the event is raised against
	*/
	public final EventType getEventType()
	{
		return _eventType;
	}

	/** 
	 Contains the item if the event was registered against <see cref="EventDataFilter.Metadata"/> or <see cref="EventDataFilter.DataWithMetadata"/>
	*/
	public final EventCacheItem getItem()
	{
		return _item;
	}

	public EventArg(String cacheName, EventType eventType, EventCacheItem item)
	{
		_cacheName = cacheName;
		 
                _eventType = eventType;
		_item = item;
	}
        
        public EventArg(String cacheName, EventType eventType, EventCacheItem item, EventCacheItem oldItem)
	{
		_cacheName = cacheName;
		 
                _eventType = eventType;
		_item = item;
                _oldItem = oldItem;
	}

}
