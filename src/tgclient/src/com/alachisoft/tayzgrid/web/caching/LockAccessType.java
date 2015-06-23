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

import com.alachisoft.tayzgrid.caching.topologies.CacheInsResult;


public final class LockAccessType
{

    /**
     * Indicates that lock is to be acquired
     */
    public final static byte ACQUIRE = 1;
    /**
     * Perform the operation only if item is not locked but dont acquire the lock
     */
    public final static byte DONT_ACQUIRE = 2;
    /**
     * Indicates that lock is to be released
     */
    public final static byte RELEASE = 3;
    /**
     * Indicates that lock is not to be released
     */
    public final static byte DONT_RELEASE = 4;
    /**
     * Perform the operation as if there is no lock
     */
    public final static byte IGNORE_LOCK = 5;
    /**
     * Optimistic locking; update the item in the cache only if the version is same
     */
    public final static byte COMPARE_VERSION = 6;
    /**
     * Optimistic locking; get the version while getting the object from the cache. This version is used to put the item back to the cache
     */
    public final static byte GET_VERSION = 7;
    public final static byte MATCH_VERSION = 8;
    public final static byte PRESERVE_VERSION = 9;
    public final static byte DEFAULT = 10;

    public static String getAccessType(byte accessType)
    {
        if (accessType == DEFAULT)
        {
            return "";
        }
        else
        {
            return Byte.toString(accessType);
        }
    }

}
