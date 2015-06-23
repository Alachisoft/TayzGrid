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

package com.alachisoft.tayzgrid.caching.topologies;

import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

public class CacheInsResultWithEntry implements ICompactSerializable, java.io.Serializable {

    private CacheInsResult _result = CacheInsResult.Success;
    private CacheEntry _entry = null;

    /**
     * The result of the Insert Operation.
     */
    public final CacheInsResult getResult() {
        return _result;
    }

    public final void setResult(CacheInsResult value) {
        _result = value;
    }

    /**
     * Old CacheEntry in case result is SuccessOverwrite.
     */
    public final CacheEntry getEntry() {
        return _entry;
    }

    public final void setEntry(CacheEntry value) {
        _entry = value;
    }

    public CacheInsResultWithEntry() {
    }

    /**
     * constructor
     *
     * @param entry
     * @param result
     */
    public CacheInsResultWithEntry(CacheEntry entry, CacheInsResult result) {
        _entry = entry;
        _result = result;
    }

    @Override
    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException {
        _entry = (CacheEntry) reader.readObject();
        _result = (CacheInsResult) reader.readObject();
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException {
        writer.writeObject(_entry);
        writer.writeObject(_result);
    }
}