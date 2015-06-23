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

package com.alachisoft.tayzgrid.runtime.util;

import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentNullException;
import java.util.Date;

/**
 * TimeSpan class is similiar to .Net System.TimeSpan. Though this class delivers minimal funcitonality that is required to specify "TimeSpan" value. It is used in giving
 * SlidingExpiration value while adding data in NCache.
 */
public class TimeSpan implements java.lang.Comparable<TimeSpan>, java.io.Serializable
{
    //: NR-1019 :       Implemented compareTo in TimeSpan
    public int compareTo(TimeSpan obj)
    {
        if(obj != null && obj instanceof TimeSpan)
        {
            if(this.getTotalTicks()==obj.getTotalTicks())
                return 0;
            else  if(this.getTotalTicks()>obj.getTotalTicks())
                return 1;
            else
                return -1;
        }
        throw new NullPointerException("argument can not be null");
    }

    private int _hours = 0;
    private int _minutes = 0;
    private int _seconds = 0;
    private int _miliseconds = 0;
    private long _ticks = 0L;

    public static TimeSpan ZERO = new TimeSpan(0);

    public static TimeSpan Subtract(Date from, Date to) throws ArgumentException
    {
        NCDateTime nc1 = new NCDateTime(from);
        NCDateTime nc2 = new NCDateTime(to);
        long timespan = nc1.getTicks() - nc2.getTicks();
        if(timespan < 0)
            timespan = timespan*-1;
        return new TimeSpan(timespan);
    }

    /**
     * Default Constructor: Initializes a new TimeSpan with zero time span.
     */
    public TimeSpan()
    {
    }

    /**
     * Summary: Initializes a new TimeSpan to the specified number of miliseconds.
     *
     * @param miliseconds A time period expressed in miliseconds units.
     */
    public TimeSpan(int miliseconds)
    {
        _miliseconds = miliseconds;
    }

    /**
     * Summary: Initializes a new System.TimeSpan to a specified number of minutes and seconds.
     *
     * @param minutes Number of minutes.
     * @param seconds Number of seconds.
     */
    public TimeSpan(int minutes, int seconds)
    {
        _minutes = minutes;
        _seconds = seconds;
    }

    /**
     * Summary: Initializes a new System.TimeSpan to a specified number of hours, minutes and seconds.
     *
     * @param hours Number of hours.
     * @param minutes Number of minutes.
     * @param seconds Number of seconds.
     */
    public TimeSpan(int hours, int minutes, int seconds)
    {
        _hours = hours;
        _minutes = minutes;
        _seconds = seconds;
    }

    /**
     * Summary: Initializes a new System.TimeSpan to a specified number of ticks..
     *
     * @param ticks A time period expressed in 100-nanosecond units. If ticks value is passed, it overrides other values specified through hours,minutes,seconds,miliseconds
     * @throws java.lang.IllegalArgumentException The parameters specify a System.TimeSpan value is not positive or out of range.
     */
    public TimeSpan(long ticks)
    {
        if (ticks < 0)
        {
            throw new java.lang.IllegalArgumentException("ticks value must be positive");
        }
        _ticks = ticks;
    }

    /**
     * Summary: Sets value of hours for this instance of TimeSpan.
     *
     * @param hours A time period expressed in hours.
     * @throws java.lang.IllegalArgumentException The parameters specify a System.TimeSpan value is not positive or out of range.
     */
    public void setHours(int hours)
    {
        if (hours < 0)
        {
            throw new java.lang.IllegalArgumentException("hours value must be positive");
        }
        _hours = hours;
    }

    /**
     * Summary: Sets value of minutes for this instance of TimeSpan.
     *
     * @param minutes A time period expressed in minutes.
     * @throws java.lang.IllegalArgumentException The parameters specify a System.TimeSpan value is not positive or out of range.
     */
    public void setMinutes(int minutes)
    {
        if (minutes < 0)
        {
            throw new java.lang.IllegalArgumentException("minutes value must be positive");
        }
        _minutes = minutes;
    }

    /**
     * Summary: Sets value of seconds for this instance of TimeSpan.
     *
     * @param seconds A time period expressed in seconds.
     * @throws java.lang.IllegalArgumentException The parameters specify a System.TimeSpan value is not positive or out of range.
     */
    public void setSeconds(int seconds)
    {
        if (seconds < 0)
        {
            throw new java.lang.IllegalArgumentException("seconds value must be positive");
        }
        _seconds = seconds;
    }

    /**
     * Summary: Sets value of miliseconds for this instance of TimeSpan.
     *
     * @param muiliseconds A time period expressed in miliseconds.
     * @throws java.lang.IllegalArgumentException The parameters specify a System.TimeSpan value is not positive or out of range.
     */
    public void setMiliSeconds(int miliseconds)
    {
        if (miliseconds < 0)
        {
            throw new java.lang.IllegalArgumentException("miliseconds value must be positive");
        }
        _miliseconds = miliseconds;
    }

    /**
     * Summary: This method calculated total miliseconds for this instance of TimeSpan.This includes hours + minutes + seconds + miliseconds computed in miliseconds. Ticks are not
     * used in computing this value.
     *
     * @return Total number of miliseconds represented by this instance of TimeSpan. If negative value is calculated, zero is returned.
     *
     */
    public long getTotalMiliSeconds()
    {
        if(_ticks>0)
            return _ticks/10000L;

        long hoursInMsec = _hours * 3600000L;
        long minutesInMsec = _minutes * 60000L;
        long secondsInMsec = _seconds * 1000L;
        long returnVal = hoursInMsec + minutesInMsec + secondsInMsec + _miliseconds;
        if (returnVal > 0)
        {
            return returnVal;
        }
        else
        {
            return 0;
        }
    }

    public long getTotalMinutes()
    {
        return this.getTotalMiliSeconds()/60000L;
    }

    public long getTotalSeconds()
    {
        return this.getTotalMiliSeconds()/1000L;
    }

    /**
     * Summary: This method calculates total ticks for this instance of TimeSpan.This includes hours + minutes + seconds + miliseconds computed in ticks. In case ticks value is
     * explicitly set, value is not computed instead original ticks value is returned.
     *
     * @return Total number of ticks represented by this instance of TimeSpan. If negative value is calculated, zero is returned.
     *
     */
    public long getTotalTicks()
    {
        if (_ticks > 0)
        {
            return _ticks;
        }

        long returnVal = getTotalMiliSeconds() * 10000; //10000 ticks in onemilisecond.

        if (returnVal > 0)
        {
            return returnVal;
        }
        else
        {
            return 0;
        }
    }

    @Override
    public boolean equals(Object object)
    {
        TimeSpan timespan = (TimeSpan) object;
        if (timespan.getTotalTicks() == this.getTotalTicks())
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 83 * hash + (int) (this._ticks ^ (this._ticks >>> 32));
        return hash;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getTotalMiliSeconds()).append(" milliseconds");
        return sb.toString();
    }





}
