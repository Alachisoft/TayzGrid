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

package com.alachisoft.tayzgrid.jsr107;

import javax.cache.Cache.Entry;


public class JCacheEntry<K, V> implements Entry<K, V> {
    com.alachisoft.tayzgrid.web.caching.Cache.Entry entry;
    
    public JCacheEntry(com.alachisoft.tayzgrid.web.caching.Cache.Entry entry)
    {
        this.entry = entry;
    }

    @Override
    public K getKey() {
        return (K) entry.getKey();
    }

    @Override
    public V getValue() {
        return (V) entry.getValue();
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return (T) entry;
    }
}
