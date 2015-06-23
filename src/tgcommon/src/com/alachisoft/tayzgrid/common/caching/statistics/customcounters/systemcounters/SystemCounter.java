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

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.enums.EventType;
import com.alachisoft.tayzgrid.common.logger.EventLogger;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.InstantaneousCounter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import org.hyperic.sigar.Sigar;

public abstract class SystemCounter extends InstantaneousCounter
{

    private static Sigar sigar = null;
    private static java.lang.management.OperatingSystemMXBean oSMXBean;
    private static java.lang.management.MemoryMXBean memoryMXBean;
    private static java.lang.management.MemoryUsage memoryUsage;
    public static java.util.List<MemoryPoolMXBean> m;
    static
    {
        try
        {
            sigar = new Sigar();
        }
        catch (Throwable e)
        {
            EventLogger.LogEvent("System counters will not be published on JMX or snmp.", EventType.ERROR);
        }

        try
        {
            oSMXBean = ManagementFactory.getOperatingSystemMXBean();
            memoryMXBean = ManagementFactory.getMemoryMXBean();
            m = ManagementFactory.getMemoryPoolMXBeans();
        }
        catch (Throwable e)
        {
            EventLogger.LogEvent("System counters will not be published on JMX.", EventType.ERROR);
        }
    }

    public static Sigar getSigar()
    {
        return sigar;
    }

    public static OperatingSystemMXBean getJavaOSMXBean()
    {
        return oSMXBean;
    }

    public static com.sun.management.OperatingSystemMXBean getSunOSMXBean()
    {
        try
        {
            if (oSMXBean != null)
            {
                return (com.sun.management.OperatingSystemMXBean) oSMXBean;
            }
        }
        catch (Throwable e)
        {
            e.getMessage();
        }
        return null;
    }


    public static MemoryMXBean getMemoryMXBean()
    {
        return memoryMXBean;
    }

    public SystemCounter(String name, String instance)
    {
        super(name, instance);
    }

    @Override
    protected abstract void calculate(double value);

    @Override
    protected void flipChanged()
    {
        calculate(_value);
    }

    @Override
    public void decrement()
    {
    }

    @Override
    public void decrementBy(double value)
    {
    }

    @Override
    public double getValue()
    {
        updateIfFlipChanged();
        return _value;
    }

    public double toMegaBytes(double bytes)
    {
        return bytes / 1048576; // 1048576 = (1024 (KB) * 1024) MB
    }

    @Override
    public void setValue(double _value)
    {
        super._value = Common.roundOff(_value, 1);
    }
}
