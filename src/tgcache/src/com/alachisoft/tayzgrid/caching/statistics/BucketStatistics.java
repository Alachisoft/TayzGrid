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

package com.alachisoft.tayzgrid.caching.statistics;

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

public class BucketStatistics implements ICompactSerializable, java.io.Serializable {

    private long _count;
    private long _dataSize;

    public BucketStatistics() {
    }

    public final long getCount() {
        return _count;
    }

    public final void setCount(long value) {
        _count = value;
    }

    public final long getDataSize() {
        return _dataSize;
    }

    public final void setDataSize(long value) {
        _dataSize = value;
    }

    public final void Increment(long dataSize) {
        synchronized (this) {
            _count++;
            _dataSize += dataSize;
        }
    }

    public final void Decrement(long dataSize) {
        synchronized (this) {
            _count--;
            _dataSize -= dataSize;
        }
    }

    public final void Clear() {
        synchronized (this) {
            _count = 0;
            _dataSize = 0;
        }
    }

    public final void SerializeLocal(CacheObjectOutput writer) throws IOException {
        writer.writeLong(_count);
        writer.writeLong(_dataSize);
    }

    public final void DeserializeLocal(CacheObjectInput reader) throws IOException {
        _count = reader.readLong();
        _dataSize = reader.readLong();
    }

    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException {
        _count = reader.readLong();
        _dataSize = reader.readLong();
    }

    public void serialize(CacheObjectOutput writer) throws IOException {
        writer.writeLong(_count);
        writer.writeLong(_dataSize);
    }
}
