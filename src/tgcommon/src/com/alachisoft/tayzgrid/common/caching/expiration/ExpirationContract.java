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

package com.alachisoft.tayzgrid.common.caching.expiration;

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.runtime.exceptions.*;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class ExpirationContract implements InternalCompactSerializable{
    private ExpirationType _defaultType;
    private long _duration;
    private TimeUnit _unit;
    
    public ExpirationContract() throws ArgumentException
    {
        this("none",0,"seconds");
    }
    
    public ExpirationContract(String type, long duration, String unit) throws ArgumentException
    {
        if(type==null)
        {
            _defaultType = ExpirationType.NoExpiration;
        }
        else if(type.equals("none"))
        {
            _defaultType = ExpirationType.NoExpiration;
        }
        else if(type.equals("absolute")||type.equals("fixed"))
        {
            _defaultType = ExpirationType.FixedExpiration;
        }
        else if(type.equals("sliding"))
        {
            _defaultType = ExpirationType.SlidingExpiration;
        }
        else
            throw new ArgumentException("The expiration policy type provided in the cache configuration is not valid. ");
        
        if(unit == null)
        {
            _unit = TimeUnit.SECONDS;
        }
        else if(unit.equals("milliseconds"))
        {
            _unit = TimeUnit.MILLISECONDS;
        }
        else if(unit.equals("seconds"))
        {
            _unit = TimeUnit.SECONDS;
        }
        else if(unit.equals("minutes"))
        {
            _unit = TimeUnit.MINUTES;
        }
        else if(unit.equals("hours"))
        {
            _unit = TimeUnit.HOURS;
        }
        else if(unit.equals("days"))
        {
            _unit = TimeUnit.DAYS;            
        }
        else
            throw new ArgumentException("The provided unit for expiration in cache config is not valid. The valid units are milliseconds, seconds, minutes, hours, and days. ");
        _duration = duration;    
    }

    /**
     * @return the _defaultType
     */
    public ExpirationType getDefaultType() {
        return _defaultType;
    }

    /**
     * @param _defaultType the _defaultType to set
     */
    public void setDefaultType(ExpirationType _defaultType) {
        this._defaultType = _defaultType;
    }

    /**
     * @return the _unit
     */
    public TimeUnit getUnit() {
        return _unit;
    }

    /**
     * @param _unit the _unit to set
     */
    public void setUnit(TimeUnit _unit) {
        this._unit = _unit;
    }
    
    public Date getExpirationDate()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MILLISECOND, (int) _unit.toMillis(_duration));
        return calendar.getTime();
    }
    
    public TimeSpan getExpirationSpan()
    {
        return new TimeSpan(((int)_unit.toMillis(_duration)));        
    }
    
    public int getTypeOrdinal()
    {
        return _defaultType.ordinal();
    }
    
    public void setTypeOrdinal(int ordinal)
    {
        _defaultType = ExpirationType.values()[ordinal];
    }
    
    public int getTimeUnitOrdinal()
    {
        return _unit.ordinal();
    }
    
    public void setTimeUnitOrdinal(int ordinal)
    {
        _unit = TimeUnit.values()[ordinal];
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        _defaultType = (ExpirationType) Common.readAs(reader.ReadObject(), ExpirationType.class);
        _duration = reader.ReadInt64();
        _unit = (TimeUnit) Common.readAs(reader.ReadObject(), TimeUnit.class);
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {
        writer.WriteObject(_defaultType);
        writer.Write(_duration);
        writer.WriteObject(_unit);
    }

    /**
     * @return the _duration
     */
    public long getDuration() {
        return _duration;
    }

    /**
     * @param _duration the _duration to set
     */
    public void setDuration(long _duration) {
        this._duration = _duration;
    } 
    
    public HashMap<ExpirationType,Object> resolveClientExpirations(Date absoluteExpiration,TimeSpan slidingExpiration)
        {
        HashMap<ExpirationType,Object> resultMap = new HashMap<ExpirationType, Object>();
        if((absoluteExpiration==null)&&(slidingExpiration==null)){
            if(_defaultType==ExpirationType.FixedExpiration)
            {
                resultMap.put(ExpirationType.FixedExpiration, getExpirationDate());
                resultMap.put(ExpirationType.SlidingExpiration,slidingExpiration);
            }
            else if(_defaultType==ExpirationType.SlidingExpiration)
            {
                resultMap.put(ExpirationType.SlidingExpiration, getExpirationSpan());
                resultMap.put(ExpirationType.FixedExpiration, absoluteExpiration);
            }
            else
            {
                resultMap.put(ExpirationType.FixedExpiration, absoluteExpiration);
                resultMap.put(ExpirationType.SlidingExpiration, slidingExpiration);
            }
        }
        else{
            resultMap.put(ExpirationType.FixedExpiration, absoluteExpiration);
            resultMap.put(ExpirationType.SlidingExpiration, slidingExpiration);
        }
        
        return resultMap;
    }    
}
