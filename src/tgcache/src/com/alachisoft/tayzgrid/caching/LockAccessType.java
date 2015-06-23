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

public enum LockAccessType {
	/** Indicates that lock is to be acquired.
	*/
	ACQUIRE(1),
	/** Perform the operation only if item is not locked but dont acquire the lock
	*/
	DONT_ACQUIRE(2),
	/** Indicates that lock is to be released.
	*/
	RELEASE(3),
	/** Indicates that lock is not to be released.
	*/
	DONT_RELEASE(4),
	/** Perform the operation as if there is no lock.
	*/
	IGNORE_LOCK(5),
	/** Optimistic locking; update the item in the cache only if the version is same.
	*/
	COMPARE_VERSION(6),
	/** Optimistic locking; get the version while getting the object from the cache. 
	 this version is used to put the item back to the cache.
	*/
	GET_VERSION(7),
	MATCH_VERSION(8),
	//muds:
	//this helps to preserve the version of the cache item when in case of client cache we
	//remove the local copy of the item before updating the remote copy of the cache item.
	//this is for internal use only.
	PRESERVE_VERSION(9),
	DEFAULT(10);

	private int intValue;
	private static java.util.HashMap<Integer, LockAccessType> mappings;
	private static java.util.HashMap<Integer, LockAccessType> getMappings() {
		if (mappings == null) {
			synchronized (LockAccessType.class) {
				if (mappings == null) {
					mappings = new java.util.HashMap<Integer, LockAccessType>();
				}
			}
		}
		return mappings;
	}

	private LockAccessType(int value) {
		intValue = value;
		LockAccessType.getMappings().put(value, this);
	}

	public int getValue() {
		return intValue;
	}

	public static LockAccessType forValue(int value) {
		return getMappings().get(value);
	}
}
