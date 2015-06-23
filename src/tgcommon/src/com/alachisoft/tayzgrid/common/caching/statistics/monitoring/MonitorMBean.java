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

package com.alachisoft.tayzgrid.common.caching.statistics.monitoring;

import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.PerformanceCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.operations.Operations;
import java.util.EnumMap;

public interface MonitorMBean
{

    public void startJMX();

    public void stopJMX();

    public void startSNMP();

    public void stopSNMP() throws Exception;

    public Boolean registerNode();

    public Boolean unRegisterNode();

    public void registerCounter(Operations operation, PerformanceCounter perfCounter) throws Exception;

    public void unRegisterCounter(Operations type);


    /**
     * temper with export string with caution as snmp paths rely on it.
     *
     * @return
     */
    public String getExportString();

    public String getNodeName();

    public EnumMap getCounterStore();

    public double getCounter(Operations operation);
     
}
