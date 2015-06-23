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

package com.alachisoft.tayzgrid.runtime;

/**
 * Specifies the relative priority of items stored in the Cache.
 * <P>
 * When the application's cache is full or runs low on memory, the Cache selectively purges
 * items to free system memory. When an item is added to the Cache, you can assign it a relative
 * priority compared to the other items stored in the Cache. Items you assign higher priority
 * values to are less likely to be deleted from the Cache when the server is processing a large
 * number of requests, while items you assign lower priority values are more likely to be deleted.
 * The default is Normal.
 *
 * @version 1.0
 * 
 */
public enum CacheItemPriority {

    /**
     * Cache items with this priority level are the most likely to be deleted
     * from the cache as the server frees system memory.
     */
    Low(1),

    /**
     * Cache items with this priority level are more likely to be deleted from
     * the cache as the server frees system memory than items assigned a Normal priority.
     */
    BelowNormal(2),

    /**
     * Cache items with this priority level are likely to be deleted from the cache as the server
     * frees system memory only after those items with Low or BelowNormal priority. This is the
     * default.
     */
    Normal(0),

    /**
     * Cache items with this priority level are less likely to be deleted as
     * the server frees system memory than those assigned a Normal priority.
     */
    AboveNormal(3),

    /**
     * Cache items with this priority level are the least likely to be deleted
     * from the cache as the server frees system memory.
     */
    High(4),

    /**
     * The cache items with this priority level will not be deleted from the
     * cache as the server frees system memory.
     */
    NotRemovable(5),

    /**
     * The default value for a cached item's priority is Normal
     */
    Default(6);

    private final int value;

    CacheItemPriority(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static CacheItemPriority forValue(int value)
    {
        switch (value)
        {
            
            case 0:
                return Normal;
            case 1:
                return Low;
            case 2:
                return BelowNormal;
            case 3:
                return AboveNormal;
            case 4:
                return High;
            case 5:
                return NotRemovable;
            case 6:
                return Default;
            default:
                return Default;

        }
    }

}


//~ Formatted by Jindent --- http://www.jindent.com
