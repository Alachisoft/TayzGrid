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
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.util.Calendar;

/**
 * Fixed time expiration based derivative of ExpirationHint.
 */
public class FixedExpiration extends ExpirationHint implements ICompactSerializable, java.io.Serializable {

    /**
     * The absolute time when this hint expires.
     */
    private int _absoluteTime;
    private int _milliseconds;
    private long _ticks;

    /**
     * FixedExpiration instance size include _absoluteTime plus _ milliseconds
     */
    private final int FixedExpirationSize = 2 * MemoryUtil.NetIntSize;
    
    /**
     * Constructor
     *
     * @param absoluteTime absolute time when this hint expires
     */
    public FixedExpiration(java.util.Date absoluteTime) {
        try {
            _hintType = ExpirationHintType.FixedExpiration;

            //Absolute is already in UTC
            _absoluteTime = AppUtil.DiffSeconds(absoluteTime);
            _milliseconds = AppUtil.DiffMilliseconds(absoluteTime);
        } catch (ArgumentException argumentException) {
        }
    }

    public FixedExpiration(java.util.Date absoluteTime, long ticks) {
        this(absoluteTime);
        _ticks = ticks;
    }

    public FixedExpiration() {
        _hintType = ExpirationHintType.FixedExpiration;
    }

    public void setTime(java.util.Date absoluteTime) {
        try {
            //Absolute is already in UTC
            _absoluteTime = AppUtil.DiffSeconds(absoluteTime);
            _milliseconds = AppUtil.DiffMilliseconds(absoluteTime);
        } catch (ArgumentException argumentException) {
        }
    }

    /**
     * key to compare expiration hints.
     * @return 
     */
    @Override
    public int getSortKey() {
        return _absoluteTime;
    }

    /**
     * Gets the ticks
     * @return 
     */
    public long getTicks() {
        return _ticks;
    }

    /**
     * virtual method that returns true when the expiration has taken place,
     * returns false otherwise.
     * @param context
     * @return 
     */
    @Override
    public boolean DetermineExpiration(CacheRuntimeContext context) {
        if (getHasExpired()) {
            return true;
        }

        try {

            int currentTime = AppUtil.DiffSeconds(Calendar.getInstance().getTime());
            if (_absoluteTime < currentTime) {
                this.NotifyExpiration(this, null);
            }
        } catch (ArgumentException argumentException) {
        }

        return getHasExpired();
    }

    public final java.util.Date getAbsoluteTime() {
        java.util.Date dt = AppUtil.GetDateTime(_absoluteTime);

        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        cal.set(Calendar.MILLISECOND, _milliseconds);
        return cal.getTime();
    }

    @Override
    public String toString() {
        return "";
    }

    @Override
    public final void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException {
        super.deserialize(reader);
        _absoluteTime = reader.readInt();
        _milliseconds = reader.readInt();
        _ticks = reader.readLong();
    }

    @Override
    public final void serialize(CacheObjectOutput writer) throws IOException {
        super.serialize(writer);
        writer.writeInt(_absoluteTime);
        writer.writeInt(_milliseconds);
        writer.writeLong(_ticks);
    }
    
    @Override
    public int getSize()
    {
        return super.getSize() + FixedExpirationSize;
    }
    
    @Override
    public int getInMemorySize()
    {
        int inMemorySize = this.getSize();
        
        inMemorySize += inMemorySize <= 24 ? 0 : MemoryUtil.NetOverHead;
        
        return inMemorySize;
    }
}
