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

package com.alachisoft.tayzgrid.caching;

import com.alachisoft.tayzgrid.common.BitSet;


import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

public class CompressedValueEntry implements ICompactSerializable
{

    public Object Value;
    public BitSet Flag;

    public CompressedValueEntry()
    {
    }

    public CompressedValueEntry(Object value, BitSet flag)
    {
        this.Value = value;
        this.Flag = flag;
    }

    public void setValue(Object value)
    {
        this.Value = value;
    }

    public final void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        Value = reader.readObject();
        Object tempVar = reader.readObject();
        Flag = (BitSet) ((tempVar instanceof BitSet) ? tempVar : null);
    }

    public final void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeObject(Value);
        writer.writeObject(Flag);
    }
}
