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

package com.alachisoft.tayzgrid.common.stats;

import java.io.Serializable;

/**
 * Class that is useful in capturing statistics.
 */
public class UsageStats implements Serializable
{

    /**
     * Timestamp for the begining of a sample.
     */
    private long _lastStart;
    /**
     * Timestamp for the end of a sample.
     */
    private long _lastStop;

    /**
     * Constructor
     */
    public UsageStats()
    {
        Reset();
    }

    /**
     * Returns the time interval for the last sample
     */
    public final long getCurrent()
    {
        synchronized (this)
        {
            return _lastStop - _lastStart;
        }
    }

    /**
     * Resets the statistics collected so far.
     */
    public final void Reset()
    {
        _lastStart = _lastStop = 0;
    }

    /**
     * Timestamps the start of a sampling interval.
     */
    public final void BeginSample()
    {
        _lastStart = System.nanoTime();
    }

    /**
     * Timestamps the end of interval and calculates the sample time
     */
    public final void EndSample()
    {
        _lastStop = System.nanoTime();
    }
}
