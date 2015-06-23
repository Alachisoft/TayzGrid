
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

import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

public class PayloadInfo implements ICompactSerializable
{

    private CacheEntry _entry;
    private int _payLoadIndex;

    public PayloadInfo(CacheEntry entry, int index)
    {
        _entry = entry;
        _payLoadIndex = index;
    }

    public final CacheEntry getEntry()
    {
        return _entry;
    }

    /**
     *
     * @deprecated Only used for ICompactSerialization
     */
    @Deprecated
    public PayloadInfo()
    {
    }

    public final int getPayloadIndex()
    {
        return _payLoadIndex;
    }

    @Override
    public final void deserialize(CacheObjectInput reader) throws IOException, ClassNotFoundException
    {
        Object tempVar = reader.readObject();
        _entry = (CacheEntry) ((tempVar instanceof CacheEntry) ? tempVar : null);
        _payLoadIndex = reader.readInt();
    }

    @Override
    public final void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeObject(_entry);
        writer.writeInt(_payLoadIndex);
    }
}
