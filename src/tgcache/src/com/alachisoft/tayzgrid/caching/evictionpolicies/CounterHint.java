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

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

/**
 * Eviction Hint based on counter; Used in case of LFU based eviction.
 */
public class CounterHint extends EvictionHint implements ICompactSerializable, java.io.Serializable
{
    /**
     * Count for the hint
     */
    protected short _count = 1;

    /**
     * Constructor.
     */
    public CounterHint()
    {
        _hintType = EvictionHintType.CounterHint;
    }

    /**
     * Constructor, Just for debugging
     */
    public CounterHint(short count)
    {
        _hintType = EvictionHintType.CounterHint;
        _count = count;
    }

    /**
     * Get the count of the hint
     */
    public final short getCount()
    {
        return _count;
    }

    /**
     * Return if hint is to be changed on Update
     */
    @Override
    public boolean getIsVariant()
    {
        if (_count < 32767)
        {
            return true;
        }
        return false;
    }

    /**
     * Update the hint if required
     *
     * @return
     */
    @Override
    public boolean Update()
    {
        if (_count < 32767)
        {
            _count++;
            return true;
        }

        return false;
    }

    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        super.deserialize(reader);
        _count = reader.readShort();
    }

    public void serialize(CacheObjectOutput writer) throws IOException
    {
        super.serialize(writer);
        writer.writeShort(_count);
    }
    
    static int getInMemorySize()
    {
        return 24;
    }
}
