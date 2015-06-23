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

import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Eviction Hint based on the timestamp; Used in case of LRU based Eviction.
 */
public class TimestampHint extends EvictionHint implements ICompactSerializable, java.io.Serializable
{
    /**
     * Time stamp for the hint
     */
    protected java.util.Date _dt = new java.util.Date(0);

    /**
     * Constructor.
     */
    public TimestampHint()
    {
        _hintType = EvictionHintType.TimestampHint;

        TimeZone utc = TimeZone.getTimeZone("UTC");
        _dt = Calendar.getInstance(utc).getTime();
    }

    /**
     * Return time stamp for the hint
     */
    public final java.util.Date getTimeStamp()
    {
        return _dt;
    }

    /**
     * Return if the hint is to be changed on Update
     */
    @Override
    public boolean getIsVariant()
    {
        return true;
    }

    /**
     * Update the hint if required
     *
     * @return
     */
    @Override
    public boolean Update()
    {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        _dt = Calendar.getInstance(utc).getTime();
        return true;
    }

    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        super.deserialize(reader);
        _dt = (new NCDateTime(reader.readLong())).getDate();
    }

    public void serialize(CacheObjectOutput writer) throws IOException
    {
        super.serialize(writer);
        Calendar c = Calendar.getInstance();
            c.clear();
            c.set(Calendar.MILLISECOND, 0);
            c.setTime((Date) _dt);
            NCDateTime ncdt = null;
        try
        {
            ncdt = new NCDateTime(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND), c.get(Calendar.MILLISECOND));
        }
        catch (ArgumentException argumentException)
        {
        }
        writer.writeLong(ncdt.getTicks());
    }
    
    static int getInMemorySize()
    {
        return 32;
    }
}
