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

package com.alachisoft.tayzgrid.caching.evictionpolicies;

import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.common.ISizableIndex;

/**
 * Allows end users to implement their own scavenging algorithm.
 */
public interface IEvictionPolicy extends ISizableIndex
{

    /**
     * Updates the data associated with indices.
     *
     * @param key
     * @param newHint
     */
    void Notify(Object key, EvictionHint oldhint, EvictionHint newHint);

    /**
     * Flush the data associated with eviction policy including indices.
     */
    void Clear();

    /**
     * Check if the provided eviction hint is compatible with the policy and return the compatible eviction hint
     *
     * @param eh eviction hint.
     * @return a hint compatible to the eviction policy.
     */
    EvictionHint CompatibleHint(EvictionHint eh);

    /**
     * Get the list of items that are selected for eviction.
     *
     * @param size size of data in store, in bytes
     */
    void Execute(CacheBase cache, CacheRuntimeContext context, long size);

    /**
     * Remove the specified key from the index.
     *
     * @param key
     */
    void Remove(Object key, EvictionHint hint);

    float getEvictRatio();

    void setEvictRatio(float value);
}
