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

package com.alachisoft.tayzgrid.event;

//~--- JDK imports ------------------------------------------------------------

import java.util.EventListener;

//~--- interfaces -------------------------------------------------------------

/**
 * The listener interface for receiving "interesting" cache events
 * (item added, item upadted, item removed and cleared) in the cache.
 * <P>
 * The class that is interested in processing a cache event
 * either implements this interface (and all the methods it
 * contains) or extends the abstract <code>CacheAdapter</code> class
 * (overriding only the methods of interest).
 * <P>
 * The listener object created from that class is then registered with a
 * component using the component's <code>addCacheListener</code>
 * method. A cache event is generated when an item is added, updated,
 * removed from the cache.. A cache event is also generated when
 * the cache is cleared. When a cache event occurs, the relevant method
 * in the listener object is invoked, and the <code>CacheEvent</code>
 * is passed to it.
 *
 * @version 1.0
 *
 * @see CacheAdapter
 * @see CacheEvent
 *
 */
public interface CacheListener extends EventListener {

    /**
     * Invoked when the cache contents are cleared.
     */
    public void cacheCleared();

    /**
     * Invoked when an item is added in the cache.
     * @param cacheEvent Contains the CacheEvent details.
     */
    public void cacheItemAdded(CacheEvent cacheEvent);

    /**
     * Invoked when an item in the cache is removed.
     * @param cacheEvent Contains the event details.
     */
    public void cacheItemRemoved(CacheEvent cacheEvent);

    /**
     * Invoked when an item is updated in the cache.
     * @param cacheEvent Contains the event details.
     */
    public void cacheItemUpdated(CacheEvent cacheEvent);

}


//~ Formatted by Jindent --- http://www.jindent.com
