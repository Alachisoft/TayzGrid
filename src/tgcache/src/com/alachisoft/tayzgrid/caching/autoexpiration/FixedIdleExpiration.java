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

import com.alachisoft.tayzgrid.runtime.util.TimeSpan;


/**
 * Fixed time expiry and Idle Time to live based derivative of ExpirationHint. Combines the effect of both.
 */
public class FixedIdleExpiration extends AggregateExpirationHint implements java.io.Serializable
{

    /**
     * Constructor
     *
     * @param idleTime sliding expiration hint
     * @param absoluteTime fixed expiration hint
     */
    public FixedIdleExpiration(TimeSpan idleTime, java.util.Date absoluteTime)
    {
        super(new FixedExpiration(absoluteTime), new IdleExpiration(idleTime));
        _hintType = ExpirationHintType.FixedIdleExpiration;
    }

    public FixedIdleExpiration()
    {
        _hintType = ExpirationHintType.FixedIdleExpiration;
    }

    @Override
    public String toString()
    {
        return "";
    }
}
