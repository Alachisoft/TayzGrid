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

import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.web.caching.CacheItemVersion;

/** 
 This is a stripped down version of <see cref=" CacheItem"/>
 Contains basic information of an item present in the cache
 Will be provided in <see cref="QueryDataNotificationCallback"/> or <see cref="CacheItemRemovedCallback"/>
 but only when the event is registered against <see cref="EventDataFilter.Metadata"/> or <see cref="EventDataFilter.DataWithMetadata"/>
*/
public class EventCacheItem implements Cloneable
{
	/**  The actual object provided by the client application 
	*/
	private Object _value;

	private CacheItemPriority _cacheItemPriority;

	//Callbacks

	private boolean _resyncExpiredItems;
	private String _resyncProviderName;
	private String _group;
	private String _subGroup;

	 
	private CacheItemVersion _version;
	 

	/** 
	 Will contain the value present in the cache but only if the event was registered against
	 <see cref="EventDataFilter.Metadata"/> or <see cref="EventDataFilter.DataWithMetadata"/>
	 otherwise it will be null
	*/
	public final Object getValue()
	{
		return _value;
	}
	public final void setValue(Object value)
	{
		_value = value;
	}

	/** 
	 CacheItemPriority of the item present in the cache
	*/
	public final CacheItemPriority getCacheItemPriority()
	{
		return _cacheItemPriority;
	}
	public final void setCacheItemPriority(CacheItemPriority value)
	{
		_cacheItemPriority = value;
	}

	/** 
	 If items are to be ReSynced at expiry
	*/
	public final boolean getResyncExpiredItems()
	{
		return _resyncExpiredItems;
	}
	public final void setReSyncExpiredItems(boolean value)
	{
		_resyncExpiredItems = value;
	}

	/** 
	 Readthrough provider name when item will be resynced at expiry
	*/
	public final String getResyncProviderName()
	{
		return _resyncProviderName;
	}
	public final void setReSyncProviderName(String value)
	{
		_resyncProviderName = value;
	}

	/** 
	 Group of the item
	*/
	public final String getGroup()
	{
		return _group;
	}
	public final void setGroup(String value)
	{
		_group = value;
	}

	/** 
	 SubGroup of the item
	*/
	public final String getSubGroup()
	{
		return _subGroup;
	}
	public final void setSubGroup(String value)
	{
		_subGroup = value;
	}

	 

	/** 
	 Item version of the item
	*/
	public final CacheItemVersion getCacheItemVersion()
	{
		return _version;
	}
	public final void setCacheItemVersion(CacheItemVersion value)
	{
		_version = value;
	}

	 

	public EventCacheItem()
	{
	}


	public final Object clone()
	{
		EventCacheItem clone = new EventCacheItem();
		clone._group = _group;
		clone._subGroup = _subGroup;
		clone._version = _version;
		clone._resyncExpiredItems = _resyncExpiredItems;
		clone._resyncProviderName = _resyncProviderName;
		clone._cacheItemPriority = _cacheItemPriority;
		clone._value = _value;

		return clone;
	}
}
