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

import com.alachisoft.tayzgrid.common.util.MemoryUtil;
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

/**
 * Implements the eviction hint for Priority based eviction strategy.
 */
public class PriorityEvictionHint extends EvictionHint implements ICompactSerializable, java.io.Serializable
{
    /**
     * The actual priority value contained within this object.
     */
    private CacheItemPriority _priority = CacheItemPriority.values()[6];

    static int InMemorySize = 24;
    static {
        InMemorySize = MemoryUtil.GetInMemoryInstanceSize(
                                        EvictionHint.getInMemorySize() 
                                        + MemoryUtil.NetEnumSize);
    }
    /**
     * Get and set the priority
     */
    public final CacheItemPriority getPriority()
    {
        return _priority;
    }

    public final void setPriority(CacheItemPriority value)
    {
        _priority = value;
    }

    /**
     * Constructor.
     */
    public PriorityEvictionHint()
    {
        _hintType = EvictionHintType.PriorityEvictionHint;
        _priority = CacheItemPriority.Normal;
    }

    /**
     * Overloaded Constructor Based on Priority Value
     *
     * @param priority
     */
    public PriorityEvictionHint(CacheItemPriority priority)
    {
        _hintType = EvictionHintType.PriorityEvictionHint;
        _priority = priority;
    }

    /**
     * Return if hint is to be changed on Update
     */
    @Override
    public boolean getIsVariant()
    {
        return false;
    }

    /**
     * Update hint if required
     *
     * @return
     */
    @Override
    public boolean Update()
    {
        return false;
    }

    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        super.deserialize(reader);
        int priority = reader.readInt();
        switch(priority)
        {
            case 1:
                _priority = CacheItemPriority.Low;
                break;
            case 2:
                _priority = CacheItemPriority.BelowNormal;
                break;
            case 0:
                _priority = CacheItemPriority.Normal;
                break;
            case 3:
                _priority = CacheItemPriority.AboveNormal;
                break;
            case 4:
                _priority = CacheItemPriority.High;
                break;
            case 5:
                _priority = CacheItemPriority.NotRemovable;
                break;
            case 6:
                _priority = CacheItemPriority.Default;
                break;
            default:
                _priority = CacheItemPriority.Default;
                break;
        }
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException
    {
        super.serialize(writer);
        writer.writeInt(_priority.value());
    }
    
}
