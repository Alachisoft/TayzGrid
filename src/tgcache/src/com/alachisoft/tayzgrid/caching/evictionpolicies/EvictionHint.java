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
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

/**
 * Abstract base class that serves as a placeholder for eviction specific data on object basis. For example a priority based eviction policy needs to set a priority with every
 * object. Similarly other eviction may need such kind of associations.
 */
public abstract class EvictionHint implements ICompactSerializable, java.io.Serializable
{ //: IComparable

    /**
     * Get the slot in which this hint should be placed
     */
    public EvictionHintType _hintType = EvictionHintType.values()[0];

    /**
     * Return if hint is to be changed on Update
     */
    public abstract boolean getIsVariant();

    /**
     * update the eviction value
     */
    public abstract boolean Update();

    public static EvictionHint ReadEvcHint(CacheObjectInput reader) throws IOException, ClassNotFoundException
    {
        EvictionHintType expHint = EvictionHintType.Parent;
        expHint = EvictionHintType.forValue(reader.readShort());
        EvictionHint tmpObj = null;
        switch (expHint)
        {
            case NULL:
                return null;

            case Parent:
                tmpObj = (EvictionHint) reader.readObject();
                return (EvictionHint) tmpObj;

            case CounterHint:
                CounterHint ch = new CounterHint();
                ((ICompactSerializable) ch).deserialize(reader);
                return (EvictionHint) ch;

            case PriorityEvictionHint:
                PriorityEvictionHint peh = new PriorityEvictionHint();
                ((ICompactSerializable) peh).deserialize(reader);
                return (EvictionHint) peh;

            case TimestampHint:
                TimestampHint tsh = new TimestampHint();
                ((ICompactSerializable) tsh).deserialize(reader);
                return (EvictionHint) tsh;

            default:
                break;
        }
        return null;
    }

    public static void WriteEvcHint(CacheObjectOutput writer, EvictionHint evcHint) throws IOException
    {
        if (evcHint == null)
        {
            writer.writeShort((short) EvictionHintType.NULL.getValue());
            return;
        }

        writer.writeShort((short) evcHint._hintType.getValue());
        ((ICompactSerializable) evcHint).serialize(writer);
    }

    @Override
    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        _hintType = EvictionHintType.forValue(reader.readShort());
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeShort((short) _hintType.getValue());
    }
    
    /**
     * Contain in memory size of eviction hint
     * @return 
     */
    static int getInMemorySize()
    {
        return MemoryUtil.NetEnumSize; // for _hintType
    }
}
