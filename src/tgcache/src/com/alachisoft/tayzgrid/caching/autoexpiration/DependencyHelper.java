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




import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.util.HelperFxn;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

public class DependencyHelper {

    public static void GetCacheDependency(ExpirationHint hint, tangible.RefObject<java.util.Date> absoluteExpiration, tangible.RefObject<TimeSpan> slidingExpiration) {
    

        if (hint != null) {
            if (hint instanceof FixedExpiration) {
                absoluteExpiration.argvalue = HelperFxn.getDateFromUTCTicks(((FixedExpiration) hint).getTicks()); //cal.getTime();
            } else if (hint instanceof IdleExpiration) {
                slidingExpiration.argvalue = ((IdleExpiration) hint).getSlidingTime();
            } 
           
        }
    }



    public static ExpirationHint GetExpirationHint(java.util.Date absoluteExpiration, TimeSpan slidingExpiration) throws ArgumentException {
        ExpirationHint hint = null;

        if (absoluteExpiration == null && slidingExpiration == null) {
            return null;
        }

        if (absoluteExpiration == null) {
            hint = new IdleExpiration(slidingExpiration);
        } else {
            TimeZone utc = TimeZone.getTimeZone("UTC");
            Calendar cal = Calendar.getInstance(utc);
            cal.set(Calendar.MILLISECOND, 0);
            cal.setTime(absoluteExpiration);
            NCDateTime ncDate = new NCDateTime(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DATE), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND));

            absoluteExpiration = ncDate.getLocalizedDate();
            hint = new FixedExpiration(absoluteExpiration, ncDate.getTicks());
        }

        return hint;
    }

  
}
