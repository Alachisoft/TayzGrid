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

package com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters;

import static com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.SystemCounter.getJavaOSMXBean;
import static com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.SystemCounter.getSunOSMXBean;
import java.util.Random;

public class VMCpuUsage extends SystemCounter
{
    long nanoBefore;
    long cpuBefore;
    long cpuAfter;
    long nanoAfter;

    public VMCpuUsage(String name, String instance)
    {
        super(name, instance);
        if (getSunOSMXBean() != null && getJavaOSMXBean() != null)
        {
            nanoBefore = System.nanoTime();
            cpuBefore = getSunOSMXBean().getProcessCpuTime();
        }
    }

    @Override
    protected void calculate(double percent)
    {
        if (getSunOSMXBean() != null && getJavaOSMXBean() != null)
        {

            // Using JMX
            // Since we are trying to use sigar as less as possible hence aleranate algo using jmx
            cpuAfter = getSunOSMXBean().getProcessCpuTime();
            nanoAfter = System.nanoTime();
            if (nanoAfter > nanoBefore)
            {
                percent = ((cpuAfter - cpuBefore) * 100D) / (nanoAfter - nanoBefore);
            }
            else
            {
                percent = 0;
            }
            setValue(percent / getJavaOSMXBean().getAvailableProcessors());

            cpuBefore = getSunOSMXBean().getProcessCpuTime();
            nanoBefore = System.nanoTime();
        }
        return;
    }
}
