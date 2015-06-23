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

import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.common.AppUtil;
import com.alachisoft.tayzgrid.common.util.MemoryUtil;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.util.Date;

/**
 * Idle Time to Live based derivative of ExpirationHint.
 *
 *
 */
public class IdleExpiration extends ExpirationHint implements ICompactSerializable, java.io.Serializable
{

    /**
     * the idle time to live value
     */
    private int _idleTimeToLive;
    /**
     * last timestamp when the expiration was checked
     */
    private int _lastTimeStamp;

    private final int IdleExpirationSize = 2 * MemoryUtil.NetIntSize;
    
    public IdleExpiration()
    {
        _hintType = ExpirationHintType.IdleExpiration;
    }

    /**
     * Constructor
     *
     * @param idleTTL the idle time to live value
     */
    public IdleExpiration(TimeSpan idleTTL)
    {
        this.SetBit(IS_VARIANT);
        _idleTimeToLive = (int) idleTTL.getTotalMiliSeconds() / 1000;
        
        try
        {
            _lastTimeStamp = AppUtil.DiffSeconds(new Date());
        }
        catch (ArgumentException argumentException)
        {
        }
        _hintType = ExpirationHintType.IdleExpiration;
    }

    public final TimeSpan getSlidingTime()
    {
        return new TimeSpan(0, 0, _idleTimeToLive);
    }

    @Override
    public String toString()
    {
        return "";
    }

    public final int getLastAccessTime()
    {
        return _lastTimeStamp;
    }

    /**
     * key to compare expiration hints.
     * @return 
     */
    @Override
    public int getSortKey()
    {
        return _lastTimeStamp + _idleTimeToLive;
    }

    /**
     * virtual method that returns true when the expiration has taken place, returns false otherwise.
     * @param context
     * @return 
     */
    @Override
    public boolean DetermineExpiration(CacheRuntimeContext context)
    {
        if (getHasExpired())
        {
            return true;
        }

        try
        {
            if ((new Integer(getSortKey())).compareTo(AppUtil.DiffSeconds(new Date())) < 0)
            {
                this.NotifyExpiration(this, null);
            }
        }
        catch (ArgumentException argumentException)
        {
        }
        return getHasExpired();
    }

    /**
     * Resets the time to live counter.
     * @param context
     * @return 
     * @throws com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException
     */
    @Override
    public boolean Reset(CacheRuntimeContext context) throws OperationFailedException
    {
        try
        {
            _lastTimeStamp = AppUtil.DiffSeconds(new Date());
        }
        catch (ArgumentException argumentException)
        {
        }
        return super.Reset(context);
    }

    @Override
    public final void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        super.deserialize(reader);
        _idleTimeToLive = reader.readInt();
        _lastTimeStamp = reader.readInt();
    }

    @Override
    public final void serialize(CacheObjectOutput writer) throws IOException
    {
        super.serialize(writer);
        writer.writeInt(_idleTimeToLive);
        writer.writeInt(_lastTimeStamp);
    }
    
    @Override
    public int getSize()
    {
        return super.getSize() + IdleExpirationSize;
    }
    
    @Override
    public int getInMemorySize()
    {
        int inMemorySize = this.getSize();
        
        inMemorySize += inMemorySize <= 24 ? 0 : MemoryUtil.NetOverHead;
        
        return inMemorySize;
    }
}
