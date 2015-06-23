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

public class SystemFreeMemory extends SystemCounter
{

    public SystemFreeMemory(String name, String instance)
    {
        super(name, instance);
    }

    @Override
    protected void calculate(double value)
    {
        if (getSunOSMXBean() != null)
        {
            setValue(super.toMegaBytes(getSunOSMXBean().getFreePhysicalMemorySize()));
        }
    }
}
