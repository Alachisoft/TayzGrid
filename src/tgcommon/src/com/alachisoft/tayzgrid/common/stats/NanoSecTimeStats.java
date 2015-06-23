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

public class NanoSecTimeStats //implements Serializable
{

    private long start;
    private long stop;
    private long frequency;

    public NanoSecTimeStats()
    {
    }

    public final void Start()
    {
        start = System.nanoTime();
    }

    public final void Stop()
    {
        stop = System.nanoTime();
    }

    /**
     * returns the nanoseconds per iteration.
     *
     * @param iterations total iterations.
     * @return Nanoseconds per Iteration.
     */
    public final double Duration(int iterations)
    {
        return (stop - start) / (double) iterations;
    }
}
