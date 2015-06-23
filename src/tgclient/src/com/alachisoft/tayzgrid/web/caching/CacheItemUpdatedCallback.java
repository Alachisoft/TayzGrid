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

import com.alachisoft.tayzgrid.event.Notifications;

/**
 *Defines a callback method for notifying applications when an item is updated in the Cache.
 *<PRE>
 *Since this handler is invoked every time an item is removed from the Cache, doing a lot of processing inside the handler might have an impact on the performance of the cache and cluster.
 * It is therefore advisable to do minimal processing inside the handler.
 *</PRE>
 *
 */
interface CacheItemUpdatedCallback  extends Notifications
{

    /**
     * The method which is invoked.
     * @param key The cacheKey which is updated.
     */
    public void itemUpdated(Object key);
}


//~ Formatted by Jindent --- http://www.jindent.com
