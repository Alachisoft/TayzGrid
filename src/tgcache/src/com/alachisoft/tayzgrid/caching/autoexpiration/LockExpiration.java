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

package com.alachisoft.tayzgrid.caching.autoexpiration;

import com.alachisoft.tayzgrid.common.AppUtil;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

public class LockExpiration implements ICompactSerializable
{

    
    private long _lastTimeStamp;
    private long _lockTTL;
    private TimeSpan _ttl = new TimeSpan();

    public LockExpiration()
    {
    }

    public LockExpiration(TimeSpan lockTimeout)
    {
        _ttl = lockTimeout;
    }

    public final void Set()
    {
        _lockTTL = _ttl.getTotalTicks();
        try
        {
            _lastTimeStamp = AppUtil.DiffTicks(new java.util.Date());
        }
        catch (ArgumentException argumentException)
        {
        }
    }

    private long getSortKey()
    {
        return _lastTimeStamp + _lockTTL;
    }

    public final boolean HasExpired()
    {
        try
        {
            if ((new Long(getSortKey())).compareTo(AppUtil.DiffTicks(new java.util.Date())) < 0)
            {
                return true;
            }
        }
        catch (ArgumentException argumentException)
        {
        }
        return false;
    }

    public final void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        _lockTTL = reader.readLong();
        _lastTimeStamp = reader.readLong();
        _ttl = (TimeSpan) reader.readObject();
    }

    public final void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeLong(_lockTTL);
        writer.writeLong(_lastTimeStamp);
        writer.writeObject(_ttl);
    }
}
