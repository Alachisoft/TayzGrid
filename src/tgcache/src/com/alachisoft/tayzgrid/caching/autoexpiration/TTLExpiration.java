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

import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.util.Calendar;

/**
 * Time to Live based derivative of ExpirationHint.
 *
 */
public class TTLExpiration extends FixedExpiration implements java.io.Serializable
{

    public TTLExpiration(TimeSpan ttl)
    {
        super();

        NCDateTime ncDate = new NCDateTime(ttl.getTotalTicks());
        Calendar cal  = Calendar.getInstance();
        cal.add(Calendar.MILLISECOND, ncDate.getMilliseconds());
        cal.add(Calendar.SECOND, ncDate.getSeconds());
        cal.add(Calendar.MINUTE, ncDate.getMinutes());
        cal.add(Calendar.HOUR_OF_DAY, ncDate.getHours());
        cal.add(Calendar.DAY_OF_MONTH, ncDate.getDay());
        cal.add(Calendar.MONTH, ncDate.getYear()-1);
        cal.add(Calendar.YEAR, ncDate.getYear());
        super.setTime(cal.getTime());
        _hintType = ExpirationHintType.TTLExpiration;
    }

    public TTLExpiration()
    {
        _hintType = ExpirationHintType.TTLExpiration;
    }
}
