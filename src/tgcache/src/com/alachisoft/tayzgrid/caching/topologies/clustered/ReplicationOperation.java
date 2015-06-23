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

package com.alachisoft.tayzgrid.caching.topologies.clustered;

import com.alachisoft.tayzgrid.common.datastructures.IOptimizedQueueOperation;

public class ReplicationOperation implements IOptimizedQueueOperation {

    private int _size;
    private Object _data;
    private Object[] _userPayLoad;
    private long _payLoadSize;

    public ReplicationOperation(Object data) {
        this(data, 20);
    }

    public ReplicationOperation(Object data, int size) {
        _data = data;
        _size = size;
    }

    public ReplicationOperation(Object data, int size, Object[] userPayLoad, long payLoadSize) {
        this(data, size);
        _userPayLoad = userPayLoad;
        _payLoadSize = payLoadSize;
    }

    public final Object getData() {
        return _data;
    }

    public final void setData(Object value) {
        _data = value;
    }

    public final int getSize() {
        return _size;
    }

    public final void setSize(int value) {
        if (value > 0) {
            _size = value;
        }
    }

    public final Object[] getUserPayLoad() {
        return _userPayLoad;
    }

    public final void setUserPayLoad(Object[] value) {
        _userPayLoad = value;
    }

    public final long getPayLoadSize() {
        return _payLoadSize;
    }

    public final void setPayLoadSize(long value) {
        _payLoadSize = value;
    }
}
