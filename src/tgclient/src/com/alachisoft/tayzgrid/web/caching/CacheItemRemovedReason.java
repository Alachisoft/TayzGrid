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

package com.alachisoft.tayzgrid.web.caching;

/**
 * Specifies the reason an item was removed from the Cache.
 * <PRE>
 * This enumeration works in concert with the CacheItemRemovedCallback delegate to notify your applications
 * when and why an object was removed from the Cache.
 * </PRE>
 * @version 1.0
 */
public enum CacheItemRemovedReason {

    

    /** The item is removed from the cache because it expired. */
    Expired,

    /**
     * The item is removed from the cache by a Cache.Remove method call or by an
     * Cache.Insert method call that specified the same key.
     */
    Removed,

    /** The item is removed from the cache because the system removed it to free memory. */
    Underused;
    
    	public int getValue() {
		return this.ordinal();
	}

	public static CacheItemRemovedReason forValue(int value) {
		return values()[value];
	}
}


//~ Formatted by Jindent --- http://www.jindent.com
