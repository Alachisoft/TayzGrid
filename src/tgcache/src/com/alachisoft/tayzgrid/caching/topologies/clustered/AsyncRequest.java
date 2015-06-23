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

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

public class AsyncRequest {

    private Object _operation;
    private Object _synKey;
    private Address _src;
    private long _reqId;

    public AsyncRequest(Object operation, Object syncKey) {
        _operation = operation;
        _synKey = syncKey;
    }

    public final Object getOperation() {
        return _operation;
    }

    public final void setOperation(Object value) {
        _operation = value;
    }

    public final Object getSyncKey() {
        return _synKey;
    }

    public final void setSyncKey(Object value) {
        _synKey = value;
    }

    public final Address getSrc() {
        return _src;
    }

    public final void setSrc(Address value) {
        _src = value;
    }

    public final long getRequsetId() {
        return _reqId;
    }

    public final void setRequsetId(long value) {
        _reqId = value;
    }

    public final void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException {
        _operation = reader.readObject();
        _synKey = reader.readObject();
    }

    public final void serialize(CacheObjectOutput writer) throws IOException {
        writer.writeObject(_operation);
        writer.writeObject(_synKey);
    }
}