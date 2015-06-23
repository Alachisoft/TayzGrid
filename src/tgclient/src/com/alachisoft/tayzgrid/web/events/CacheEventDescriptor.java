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
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.web.caching.CacheDataModificationListener;
import java.util.EnumSet;

/** 
 Instance of this class holds the link to the registered delegate
 Keep it safe and use it to unregister the registered delegate when required.
 The bool <see cref=" IsRegistered"/> returns false when the discriptor has been consumed to unregister the delegate.
 Then this instance can then be disposed of. Upon re-registering for the interested event, a new discriptor will be created.
*/
public final class CacheEventDescriptor
{
	private CacheEventDescriptor()
	{
	}

	 
        private EnumSet<EventType> _eventTypeEnumSet;
	private EventHandle _handle = null;
	private String _cacheName;
	private boolean _isRegistered;
	private CacheDataModificationListener _cacheDataNotificationListener;
	private EventDataFilter _datafilter;

	private Object _syncblk = new Object();

 
	public static CacheEventDescriptor CreateCacheDiscriptor(EnumSet<EventType> eventTypeEnumSet, String cacheName, CacheDataModificationListener listener, EventDataFilter datafilter)
	{
		CacheEventDescriptor descriptor = new CacheEventDescriptor();
		descriptor.setRegisteredAgainst(eventTypeEnumSet);
		descriptor.setCacheName(cacheName);
		descriptor.settCacheDataNotificationListener(listener);
		descriptor.setIsRegistered(true);
		descriptor.setDataFilter(datafilter);
		return descriptor;

	}

	public EventDataFilter getDataFilter()
	{
		return _datafilter;
	}
	public void setDataFilter(EventDataFilter value)
	{
		_datafilter = value;
	}

	/** 
	 Returns true if the linked event delegate is registered, returns false when the descriptor has been consumed
	 This property is ThreadSafe
	*/
	public boolean getIsRegistered()
	{
		synchronized (_syncblk)
		{
			return _isRegistered;
		}
	}
	public void setIsRegistered(boolean value)
	{
		synchronized (_syncblk)
		{
			_isRegistered = value;
		}
	}

	/** 
	 Name of the cache registered against
	*/
	public String getCacheName()
	{
		return _cacheName;
	}
	public void setCacheName(String value)
	{
		_cacheName = value;
	}

	/** 
	 Event Types registered against. Can be ORed to check registeration types
	*/
	public EnumSet<EventType> getRegisteredAgainst()
	{
		return _eventTypeEnumSet;
	}
	public void setRegisteredAgainst(EnumSet<EventType> value)
	{
		_eventTypeEnumSet = value;
	}
 
	public CacheDataModificationListener getCacheDataNotificationListener()
	{
		return _cacheDataNotificationListener;
	}
	public void settCacheDataNotificationListener(CacheDataModificationListener listener)
	{
		_cacheDataNotificationListener = listener;
	}
 
	public EventHandle getHandle()
	{
		return _handle;
	}
	public void setHandle(EventHandle value)
	{
		if (_handle == null)
		{
			setIsRegistered(true);
			_handle = value;
		}
	}

	//TODO: Need to write overloads for equality and stuff

}
