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


package com.alachisoft.tayzgrid.web.caching;

import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.FixedExpiration;
import com.alachisoft.tayzgrid.caching.autoexpiration.IdleExpiration;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.util.Calendar;
import java.util.Date;


public final class WebCacheHelper {


    /**
     * Converts between TGCache item remove reason and web item remove reason.
     *
     * @param reason
     * @return
     */
    public static CacheItemRemovedReason GetWebItemRemovedReason(ItemRemoveReason reason) {
        switch (reason) {
          
            case Expired:
                return CacheItemRemovedReason.Expired;

            case Underused:
                return CacheItemRemovedReason.Underused;
        }
        return CacheItemRemovedReason.Removed;
    }


    /**
     * combines the absolute and sliding expiry params and returns a single
     * expiration hint value.
     *
     * @param absoluteExpiration the absolute expiration datatime
     * @param slidingExpiration the sliding expiration time
     * @return expiration hint If you set the <paramref
     * name="slidingExpiration"/> parameter to less than TimeSpan.Zero, or the
     * equivalent of more than one year, an <see
     * cref="ArgumentOutOfRangeException"/> is thrown. You cannot set both
     * sliding and absolute expirations on the same cached item. If you do so,
     * an <see cref="ArgumentException"/> is thrown.
     */
    public static ExpirationHint MakeFixedIdleExpirationHint(java.util.Date absoluteExpiration, TimeSpan slidingExpiration) {
        if (Cache.DefaultAbsoluteExpiration.equals(absoluteExpiration) && Cache.DefaultSlidingExpiration.equals(slidingExpiration)) {
            return null;
        }
        if (Cache.DefaultAbsoluteExpiration.equals(absoluteExpiration)) {
            if (slidingExpiration.compareTo(TimeSpan.ZERO) < 0) {
                throw new IllegalArgumentException("slidingExpiration");
            }

            Calendar calendar = Calendar.getInstance();
            Calendar currentCalendar = Calendar.getInstance();
            calendar.add(Calendar.YEAR, 1);

            if (slidingExpiration.getTotalMiliSeconds() - (calendar.getTimeInMillis() - currentCalendar.getTimeInMillis()) >= 0) {
                throw new IllegalArgumentException("slidingExpiration");
            }

            return new IdleExpiration(slidingExpiration);
        }
        if (Cache.DefaultSlidingExpiration.equals(slidingExpiration)) {
            return new FixedExpiration(absoluteExpiration);
        }
        throw new IllegalArgumentException("You cannot set both sliding and absolute expirations on the same cache item.");
    }

    public static void EvaluateTagsParameters(java.util.Hashtable queryInfo, String group) {
        if (queryInfo != null) {

            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(group) && queryInfo.get("tag-info") != null) //#else
            {
                if (!(group == null || group.length() == 0) && queryInfo.get("tag-info") != null) //#endif
                {
                    throw new IllegalArgumentException("You cannot set both groups and tags on the same cache item.");
                }
            }
        }
    }

    public static byte EvaluateExpirationParameters(java.util.Date absoluteExpiration, TimeSpan slidingExpiration) {
        if (Cache.DefaultAbsoluteExpiration.equals(absoluteExpiration) && Cache.DefaultSlidingExpiration.equals(slidingExpiration)) {
            return 2;
        }

        if (Cache.DefaultAbsoluteExpiration.equals(absoluteExpiration)) {
            if (slidingExpiration.compareTo(TimeSpan.ZERO) < 0) {
                throw new IllegalArgumentException("slidingExpiration");
            }

            Calendar calendar = Calendar.getInstance();
            Calendar currentCalendar = Calendar.getInstance();
            calendar.add(Calendar.YEAR, 1);

            if (slidingExpiration.getTotalMiliSeconds() - (calendar.getTimeInMillis() - currentCalendar.getTimeInMillis()) >= 0) {
                throw new IllegalArgumentException("slidingExpiration");
            }

            return 0;
        }

        if (Cache.DefaultSlidingExpiration.equals(slidingExpiration)) {
            return 1;
        }

        throw new IllegalArgumentException("You cannot set both sliding and absolute expirations on the same cache item.");
    }
}
