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
import com.alachisoft.tayzgrid.web.caching.CacheItemRemovedReason;
import java.util.EnumSet;

/** 
 This object is received when an even is raised and delegate <see cref=" CacheDataNotificationCallback"/> is executed
 CacheEventArg contains necessary information to identify the event and perform necessary actions accordingly.
 It inherits <see cref=" EventArg"/> which is also a parent of <see cref=" CQEventArg"/>
 This class is consistent for both selective and general events
*/
public class CacheEventArg extends EventArg
{

	private CacheItemRemovedReason _reason; //For remove only

	private CacheEventDescriptor _descriptor;

	/** 
	 Only applicable for general events otherwise it will be null
	*/
	public final CacheEventDescriptor getDescriptor()
	{
		return _descriptor;
	}
	public final void setDescriptor(CacheEventDescriptor value)
	{
		_descriptor = value;
	}



	/** 
	 Only applicable for <see cref="EventType.ItemRemove"/>
	 Otherwise default value is DependencyChanged
	*/
	public final CacheItemRemovedReason getCacheItemRemovedReason()
	{
		return _reason;
	}
	public final void setCacheItemRemovedReason(CacheItemRemovedReason value)
	{
		_reason = value;
	}

	public CacheEventArg(Object key, String cachename, EventType eventType, EventCacheItem item, CacheEventDescriptor discriptor)
	{
		super(cachename, eventType, item);
		_descriptor = discriptor;
	}

	public CacheEventArg(Object key, String cachename, EventType eventType, EventCacheItem item, CacheEventDescriptor discriptor, EventCacheItem olditem)
	{
		
                super(cachename, eventType, item, olditem);

		_descriptor = discriptor;
	}

	public CacheEventArg(Object key, String cachename, EventType eventType, EventCacheItem item, CacheEventDescriptor discriptor, CacheItemRemovedReason reason)
	{
		super(cachename, eventType, item);
		_reason = reason;
		_descriptor = discriptor;
	}
}
