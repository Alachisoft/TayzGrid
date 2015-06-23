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

public interface ServerMonitorMBean extends MonitorMBean
{


    public String getLoggedEvent();

    /**
     *
     * @return
     */
    public double getRequestsPerSec();

    /**
     *
     * @return
     */
    public double getResponsesPerSec();

    /**
     *
     * @return
     */
    public double getClientBytesSentPerSecStats();

    /**
     *
     * @return
     */
    public double getClientBytesRecievedPerSecStats();

    /**
     *
     * @return
     */
    public double getmSecPerCacheOperation();

    /**
     *
     * @return
     */
//    public double getmSecPerOperationBase();

    /**
     *
     * @return
     */
    public String getCachePorts();

    public double getSystemCpuUsage();

    public double getSystemFreeMemory();

    public double getSystemMemoryUsage();

    public double getVMCpuUsage();

    public double getVMCommittedMemory();

    public double getVMMaxMemory();

    public double getVMMemroyUsage();
    
    public double getVMNetworkUsage();
    
    public String getClientPort();
   
}
