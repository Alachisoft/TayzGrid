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

package com.alachisoft.tayzgrid.common;

import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.util.Date;

/**
 * Provides options for locking.
 */
public final class LockOptions implements ICompactSerializable
{

    private Object _lockId;
    private java.util.Date _lockDate = new java.util.Date(0);
    private TimeSpan _lockAge = new TimeSpan();

    public LockOptions()
    {
        try
        {
            NCDateTime time = new NCDateTime(1970, 1, 1, 0, 0, 0, 0);
            _lockDate = time.getDate();
        }
        catch (Exception e)
        {
        }
    }

    public LockOptions(Object lockId, java.util.Date lockDate)
    {
        this._lockId = lockId;
        this._lockDate = lockDate;
    }

    /**
     * Gets or Sets a unique lock value for a cache key.
     */
    public Object getLockId()
    {
        return _lockId;
    }

    public void setLockId(Object value)
    {
        _lockId = value;
    }

    /**
     * The DateTime when this lock was acquired on the cachekey. This DateTime is set on the cache server not on the web server.
     */
    public java.util.Date getLockDate()
    {
        return _lockDate;
    }

    public void setLockDate(java.util.Date value)
    {
        _lockDate = value;
    }

    /**
     * The lock Age of the current lock. This is computed on the cache server are returned to the client.
     */
    public TimeSpan getLockAge()
    {
        return _lockAge;
    }

    public void setLockAge(TimeSpan value)
    {
        _lockAge = value;
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeObject(_lockId);
        writer.writeObject(_lockDate);
        writer.writeObject(_lockAge);
    }

    @Override
    public void deserialize(CacheObjectInput reader) throws IOException, ClassNotFoundException
    {
        _lockId = reader.readObject();
        _lockDate = (Date)reader.readObject();
        _lockAge = (TimeSpan)reader.readObject();
    }
}
