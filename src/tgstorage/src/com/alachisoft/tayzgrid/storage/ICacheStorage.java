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
package com.alachisoft.tayzgrid.storage;

import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.ISizableIndex;
import java.util.Iterator;

/**
 * Interface that defines the standard operations to be implemented by all the
 * cache stores.
 */
public interface ICacheStorage extends IDisposable {

	/**
	 returns the number of objects contained in the cache.
	*/
	long getCount();

	/**
	 returns the size of data, in bytes, stored in cache
	*/
	long getSize();

	/**
	 returns the number of objects contained in the cache.
     * @return 
	*/
	long getMaxSize();
	void setMaxSize(long value);

	/**
	 returns the number if objects contained in the cache.
	*/
	long getMaxCount();
	void setMaxCount(long value);

	/**
	 retrun all the keys currently in the cache
	*/
	Object[] getKeys();

	/**
	 Removes all entries from the store.
	*/
	void Clear();

	/**
	 Determines whether the store contains a specific key.

	 @param key The key to locate in the store.
	 @return true if the store contains an element
	 with the specified key; otherwise, false.
	*/
	boolean Contains(Object key);

	/**
	 Get an object from the store, specified by the passed in key. Must be implemented
	 by cache stores.

	 @param key The key whose value to get.
	 @return cache entry.
	*/
	Object Get(Object key);

	/**
	 Get the size of item stored in store

	 @param key The key whose items size to get
	 @return Item size
	*/
	int GetItemSize(Object key);

	/**
	 Add the key value pair to the store. Must be implemented by cache stores.

	 @param key key
	 @param item object
         @param allowExtentedSize boolean
	 @return returns the result of operation.
	*/
	StoreAddResult Add(Object key, Object item, boolean allowExtentedSize);

	/**
	 Insert the key value pair to the store. Must be implemented by cache stores.

	 @param key key
	 @param item object
         * @param allowExtentedSize boolean
	 @return returns the result of operation.
	*/
	StoreInsResult Insert(Object key, Object item, boolean allowExtentedSize);

	/**
	 Removes an object from the store, specified by the passed in key. Must be
	 implemented by cache stores.

	 @param key The key whose value to remove.
	 @return cache entry.
	*/
	Object Remove(Object key);

	/**
	 Returns a .NET IEnumerator interface so that a client should be able
	 to iterate over the elements of the cache store.

	 @return IDictionaryEnumerator enumerator.
	*/
    Iterator GetEnumerator();
	   
	/**
	* Returns true if storage has to check for space,false otherwise.
	* 
	*/
	boolean getVirtualUnlimitedSpace();
	void setVirtualUnlimitedSpace(boolean isVirtualUnlimitedSpace);

    /**
     * Contains references to Different Indexes for actual Size Calculation
     * to iterate over the elements of the cache store
     * @return 
     */
    ISizableIndex getISizableQueryIndexManager();
    void setISizableQueryIndexManager(ISizableIndex i);
    ISizableIndex getISizableExpirationIndexManager();
    void setISizableExpirationIndexManager(ISizableIndex i);
    ISizableIndex getISizableEvictionIndexManager();
    void setISizableEvictionIndexManager(ISizableIndex i);

}
