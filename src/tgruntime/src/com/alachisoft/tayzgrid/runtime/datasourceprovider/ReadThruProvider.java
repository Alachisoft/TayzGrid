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
import java.util.HashMap;

public interface ReadThruProvider {

    /**
     * Perform tasks like allocating resources or acquiring connections etc.
     * @param parameters - Startup parameters defined in the configuration
     */
    void init(HashMap parameters,String cacheId) throws Exception;

    /**
     *
     * @param key
     * @param cacheItem
     */
    void loadFromSource(Object key, ProviderCacheItem cacheItem) throws Exception;

    /**
     *
     * @param keys - array of keys
     * @return key value HashMap
     */
    HashMap<Object, ProviderCacheItem> loadFromSource(Object[] keys) throws Exception;

    /**
     * Perform tasks associated with freeing, releasing, or resetting resources.
     */
    void dispose() throws Exception;
}
