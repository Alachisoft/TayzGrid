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

import com.alachisoft.tayzgrid.common.net.IRentableObject;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

/**
 * An info object that wraps multiple function codes and objects. Function codes
 * are to be defined by the clients/derivations of clustered cache.
 */
public class AggregateFunction implements ICompactSerializable, IRentableObject, java.io.Serializable {

    private Object[] _funcs;
    private int _rentId;

    public AggregateFunction(Function... funcs) {
        _funcs = new Function[funcs.length];
        System.arraycopy(funcs, 0, _funcs, 0, funcs.length);
    }

    public final Object[] getFunctions() {
        return _funcs;
    }

    public final void setFunctions(Object[] value) {
        _funcs = value;
    }

    @Override
    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException {
        _funcs = (Object[]) reader.readObject();
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException {
        writer.writeObject(_funcs);
    }

    /**
     * Gets or sets the rent id.
     */
    @Override
    public final int getRentId() {
        return _rentId;
    }

    @Override
    public final void setRentId(int value) {
        _rentId = value;
    }

}
