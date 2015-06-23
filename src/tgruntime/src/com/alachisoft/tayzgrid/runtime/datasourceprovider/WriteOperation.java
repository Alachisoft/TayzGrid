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

package com.alachisoft.tayzgrid.runtime.datasourceprovider;
import com.alachisoft.tayzgrid.runtime.caching.ProviderCacheItem;

public class WriteOperation
{
	private Object _key;
	private ProviderCacheItem _cacheItem;
	private WriteOperationType _opType = WriteOperationType.values()[0];
	private int _retryCount;

	public WriteOperation(Object key, ProviderCacheItem cacheItem, WriteOperationType opType, int retryCount)
	{
		this._key = key;
		this._cacheItem = cacheItem;
		this._opType = opType;
		this._retryCount = retryCount;
	}

	/** 
	 Gets the key of cache item.
	*/
	public final Object getKey()
	{
		return _key;
	}
	/** 
	 Gets the cache item.
	*/
	public final ProviderCacheItem getProviderCacheItem()
	{
		return _cacheItem;
	}
        /** 
	 Sets the cache item.
	*/
	public final void setProviderCacheItem(ProviderCacheItem value)
	{
		_cacheItem = value;
	}
	/** 
	 Gets the type of Write operation.
	*/
	public final WriteOperationType getOperationType()
	{
		return _opType;
	}

	/** 
	 Specify number of retries in case of data source operation failure.
	*/
	public final int getRetryCount()
	{
		return _retryCount;
	}
}