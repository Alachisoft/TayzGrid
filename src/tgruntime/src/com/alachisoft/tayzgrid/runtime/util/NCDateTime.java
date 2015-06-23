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
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


public class NCDateTime
{
    private int[] DaysToMonth365 = new int[]
    {
        0, 0x1f, 0x3b, 90, 120, 0x97, 0xb5, 0xd4, 0xf3, 0x111, 0x130, 0x14e, 0x16d
    };
    private int[] DaysToMonth366 = new int[]
    {
        0, 0x1f, 60, 0x5b, 0x79, 0x98, 0xb6, 0xd5, 0xf4, 0x112, 0x131, 0x14f, 0x16e
    };
    long dateData;
    int _year = -1, _month = -1, _day = -1, _hours = -1, _minutes = -1, _seconds = -1, _milliseconds = -1 ;


    /**
     * Creates .Net compatible Ticks. Use Calendar.get(field) for this constructor
     * @param year year
     * @param month month of year (Java is 0 based so add 1)
     * @param day day of Month
     * @param hours Hour of Day(24HRS format)
     * @param minutes minutes of an hour
     * @param seconds Seconds of a minute
     * @param milliseconds milliseconds
     * @throws ArgumentException
     */
    public NCDateTime(int year, int month, int day, int hours, int minutes, int seconds, int milliseconds) throws ArgumentException
    {
        if ((milliseconds < 0) || (milliseconds >= 0x3e8))
        {
            throw new ArgumentException("millisecond");
        }
        long num = DateToTicks(year, month, day) + TimeToTicks(hours, minutes, seconds);
        num += milliseconds * 0x2710L;
        if ((num < 0L) || (num > 0x2bca2875f4373fffL))
        {
            throw new ArgumentException("Arg_DateTimeRange");
        }
        this.dateData = (long) num;

    }

    public NCDateTime(Date date) throws ArgumentException
    {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.MILLISECOND, 0);
        c.setTime(date);
        if ((c.get(Calendar.MILLISECOND) < 0) || (c.get(Calendar.MILLISECOND) >= 0x3e8))
        {
            throw new ArgumentException("millisecond");
        }
        long num = DateToTicks(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH)) + TimeToTicks(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
        num += c.get(Calendar.MILLISECOND) * 0x2710L;
        if ((num < 0L) || (num > 0x2bca2875f4373fffL))
        {
            throw new ArgumentException("Arg_DateTimeRange");
        }
        this.dateData = (long) num;
    }


    /**
     * Provided Ticks to initialize and use getDate to get java.util.Date
     * @param ticks
     */
    public NCDateTime(long ticks)
    {
        this.dateData = ticks;
    }




    private long DateToTicks(int year, int month, int day) throws ArgumentException
    {
        if (((year >= 1) && (year <= 0x270f)) && ((month >= 1) && (month <= 12)))
        {
            int[] numArray = IsLeapYear(year) ? DaysToMonth366 : DaysToMonth365;
            if ((day >= 1) && (day <= (numArray[month] - numArray[month - 1])))
            {
                int num = year - 1;
                int num2 = ((((((num * 0x16d) + (num / 4)) - (num / 100)) + (num / 400)) + numArray[month - 1]) + day) - 1;
                return (num2 * 0xc92a69c000L);
            }
        }
        throw new ArgumentException("ArgumentOutOfRange_BadYearMonthDay");

    }

    private static boolean IsLeapYear(int year) throws ArgumentException
    {
        if ((year < 1) || (year > 0x270f))
        {
            throw new ArgumentException("ArgumentOutOfRange_Year");
        }
        if ((year % 4) != 0)
        {
            return false;
        }
        if ((year % 100) == 0)
        {
            return ((year % 400) == 0);
        }
        return true;
    }

    private long TimeToTicks(int hour, int minute, int second) throws ArgumentException
    {
        if ((((hour < 0) || (hour >= 0x18)) || ((minute < 0) || (minute >= 60))) || ((second < 0) || (second >= 60)))
        {
            throw new ArgumentException("ArgumentOutOfRange_BadHourMinuteSecond");
        }
        long num = ((hour * 0xe10L) + (minute * 60L)) + second;
        if ((num > 0xd6bf94d5e5L) || (num < -922337203685L))
        {
            throw new ArgumentException("Overflow_TimeSpanTooLong");
        }
        return (num * 0x989680L);
    }

    public int getDay()
    {
        if (_day == -1)
        {
            _day = GetDatePart(3);
        }
        return _day;
    }

    public int getHours()
    {
        if (_hours == -1)
        {
            _hours =  (int) ((this.getInternalTicks() / 0x861c46800L) % 0x18L);

        }
        return _hours;
    }

    public int getMilliseconds()
    {
        if (_milliseconds == -1)
        {
            _milliseconds =  (int) ((this.getInternalTicks() / 0x2710L) % 0x3e8L);
        }
        return _milliseconds;
    }

    public int getMinutes()
    {
        if (_minutes == -1)
        {
            _minutes =  (int) ((this.getInternalTicks() / 0x23c34600L) % 60L);
        }
        return _minutes;
    }

    public int getMonth()
    {
        if (_month == -1)
        {
            _month = GetDatePart(2);
        }
        return _month;
    }

    public int getSeconds()
    {
        if (_seconds == -1)
        {
            _seconds =  (int) ((this.getInternalTicks() / 0x989680L) % 60L);
        }
        return _seconds;
    }

    public int getYear()
    {
        if (_year == -1)
        {
            _year = GetDatePart(0);
        }
        return _year;
    }

    public long getTicks()
    {
        return getInternalTicks();
    }

    private long getInternalTicks()
    {
      return (((long) this.dateData) & 0x3fffffffffffffffL);
    }

    private int GetDatePart(int part)
    {
        int num2 = (int) (this.getInternalTicks() / 0xc92a69c000L);
        int num3 = num2 / 0x23ab1;
        num2 -= num3 * 0x23ab1;
        int num4 = num2 / 0x8eac;
        if (num4 == 4)
        {
            num4 = 3;
        }
        num2 -= num4 * 0x8eac;
        int num5 = num2 / 0x5b5;
        num2 -= num5 * 0x5b5;
        int num6 = num2 / 0x16d;
        if (num6 == 4)
        {
            num6 = 3;
        }
        if (part == 0)
        {
            return (((((num3 * 400) + (num4 * 100)) + (num5 * 4)) + num6) + 1);
        }
        num2 -= num6 * 0x16d;
        if (part == 1)
        {
            return (num2 + 1);
        }
        int[] numArray = ((num6 == 3) && ((num5 != 0x18) || (num4 == 3))) ? DaysToMonth366 : DaysToMonth365;
        int index = num2 >> 6;
        while (num2 >= numArray[index])
        {
            index++;
        }
        if (part == 2)
        {
            return index;
        }
        return ((num2 - numArray[index - 1]) + 1);
    }

    public Date getDate()
    {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.MILLISECOND, 0);

        cal.set(getYear(), getMonth()-1, getDay(), getHours(), getMinutes(), getSeconds());
        cal.set(Calendar.MILLISECOND, getMilliseconds());


        return cal.getTime();
    }


    /**
     * returns UTC based time
     * @return
     */
     public Date getLocalizedDate()
    {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        Calendar cal = Calendar.getInstance(utc);
        cal.clear();
        cal.set(Calendar.MILLISECOND, 0);

        cal.set(getYear(), getMonth()-1, getDay(), getHours(), getMinutes(), getSeconds());
        cal.set(Calendar.MILLISECOND, getMilliseconds());


        return cal.getTime();
    }


    public long getLocalizedTimeInTicks()
    {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        Calendar cal = Calendar.getInstance(utc);
        cal.clear();
        cal.set(Calendar.MILLISECOND, 0);

        cal.set(getYear(), getMonth(), getDay(), getHours(), getMinutes(), getSeconds());
        cal.set(Calendar.MILLISECOND, getMilliseconds());

        long tic = -1;
        try
        {
            tic = new NCDateTime(cal.getTime()).getTicks();
        }
        catch (ArgumentException argumentException)
        {
            //Not healthy code
        }

        return  tic;
    }

    public static Date getUTCNow()
    {
        try
        {
            NCDateTime ncd = new NCDateTime(Calendar.getInstance().getTime());
            return ncd.getLocalizedDate();
        }
        catch (ArgumentException argumentException)
        {
            return null;
        }
    }

}
