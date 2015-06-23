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

package com.alachisoft.tayzgrid.caching.util;

import com.alachisoft.tayzgrid.caching.autoexpiration.FixedExpiration;
import com.alachisoft.tayzgrid.caching.autoexpiration.IdleExpiration;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Summary description for Util.
 */
public final class ConvHelper
{

    public static final java.util.Date NoAbsoluteExpiration = null;
    public static final TimeSpan NoSlidingExpiration = null;

    public static ExpirationHint MakeFixedIdleExpirationHint(java.util.Date dt, TimeSpan ts)
    {
        return null;
    }

    /**
     *
     *
     * @param ticks
     * @param isSliding
     * @return
     */
    public static ExpirationHint MakeExpirationHint(long ticks, boolean isAbsolute)
    {
        if (ticks == 0)
        {
            return null;
        }

        if (!isAbsolute)
        {
            TimeSpan slidingExpiration = new TimeSpan(ticks);
            if (slidingExpiration.equals(new TimeSpan(0L)))
            {
                throw new IllegalArgumentException("slidingExpiration");
            }
            try
            {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.YEAR, 1);
                NCDateTime ncd = new NCDateTime(cal.getTime());



                if (slidingExpiration.getTotalTicks() > (ncd.getTicks() - ((new NCDateTime(Calendar.getInstance().getTime())).getTicks())))
                {
                    throw new IllegalArgumentException("slidingExpiration");
                }
            }
            catch (ArgumentException argumentException)
            {
                throw new IllegalArgumentException("sliding Expiration " + argumentException.getMessage());
            }
            catch (IllegalArgumentException illegalArgumentException)
            {
                throw illegalArgumentException;
            }


            return new IdleExpiration(slidingExpiration);
        }
        else
        {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.clear();
            cal.set(Calendar.MILLISECOND, 0);
            NCDateTime ncd = new NCDateTime(ticks);
            cal.setTime(ncd.getLocalizedDate());
            return new FixedExpiration(cal.getTime(),ncd.getTicks());

        }
    }
}
