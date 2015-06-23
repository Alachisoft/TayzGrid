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

package com.alachisoft.tayzgrid.common.threading;

import java.util.Calendar;

public class QueuedEvent
{

    public TimeScheduler.Task Task;
    
    /**
     * stopwatch to calculate the elapsed time for an event.
     */
    public long stopwatch;

    public QueuedEvent(TimeScheduler.Task task)
    {
        this.Task = task;
        stopwatch = System.currentTimeMillis();
    }

    public final java.util.Date getSchedTime()
    {
        Calendar cal = Calendar.getInstance();
        long milli = getInterval() - getElapsedTime();
        cal.add(Calendar.MILLISECOND, (int)milli);
        return cal.getTime();
    }

    public final long getInterval()
    {
        return Task.GetNextInterval();
    }

    public final long getElapsedTime()
    {
        return System.currentTimeMillis() - stopwatch;
    }

    public boolean ReQueue()
    {
        stopwatch = System.currentTimeMillis();
        return !Task.IsCancelled();
    }
}
