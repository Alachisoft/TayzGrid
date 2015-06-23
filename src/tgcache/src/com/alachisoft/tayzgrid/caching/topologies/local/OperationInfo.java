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

package com.alachisoft.tayzgrid.caching.topologies.local;

import com.alachisoft.tayzgrid.caching.topologies.OperationType;
import com.alachisoft.tayzgrid.caching.CacheEntry;

public class OperationInfo {

    private Object _key;
    private CacheEntry _entry;
    private OperationType _opType = OperationType.values()[0];

    public OperationInfo(Object key, OperationType type) {
        _key = key;
        _opType = type;
    }

    public OperationInfo(Object key, CacheEntry entry, OperationType type) {
        this(key, type);
        _entry = entry;
    }

    public final Object getKey() {
        return _key;
    }

    public final CacheEntry getEntry() {
        return _entry;
    }

    public final Object getOpType() {
        return _opType;
    }
}
