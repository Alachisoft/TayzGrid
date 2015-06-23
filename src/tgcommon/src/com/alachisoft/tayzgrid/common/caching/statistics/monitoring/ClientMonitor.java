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

import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.net.Helper;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.PerformanceCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.operations.ClientOperations;
import java.util.EnumMap;
import org.weakref.jmx.Managed;

public class ClientMonitor extends Monitor implements ClientMonitorMBean
{

    private int clientNo;
    private EnumMap clientCounterStore;
    /**
     *
     * jmxInstanceCounter keeps count of how many caches, client has initialized. Since we only want to dispose JMX if it was the last cache that called disposed, this protective
     * counter helps us in doing that.
     */
    private static int jmxInstanceCounter;



    static
    {
        jmxInstanceCounter = 0;
    }

    /**
     *
     * @param nodeName
     */
    public ClientMonitor(String nodeName)
    {
        super(nodeName);
        clientCounterStore = new EnumMap<ClientOperations, PerformanceCounter>(ClientOperations.class);
        clientNo = jmxInstanceCounter;
        setPort();
    }

    public ClientMonitor(String nodeName, ILogger logger)
    {
        super(nodeName, logger);
        clientCounterStore = new EnumMap<ClientOperations, PerformanceCounter>(ClientOperations.class);
        setPort();
    }


    @Override
    public void setPort()
    {
       super.setSnmpPort(Helper.getFreePort(ServicePropValues.getStatsServerIp()));
    }

    @Override
    public void startJMX()
    {
        jmxInstanceCounter++;
        super.startJMX();
    }

    @Override
    public void stopJMX()
    {
        if (--jmxInstanceCounter == 0)
        {
            super.stopJMX();
        }
    }

    @Override
    public EnumMap getCounterStore()
    {
        return clientCounterStore;
    }

    @Override
    public String getExportString()
    {
        return getExportStringPrefix() + "client." + getNodeName() + clientNo;
    }

    @Managed(description="Average size of the item added to/fetched from the cache by the client.", name="Average Item Size")
    @Override
    public double getAverageItemSize()
    {
        return getCounter(ClientOperations.AvgItemSize);
    }

    @Managed(description="Percentage of CPU utilized by the client for performing operations.  ",name="Client CPU Usage")
    @Override
    public double getCpuUsage()
    {
        return getCounter(ClientOperations.CpuUsage);
    }

    @Managed(description="Number of Add operations per second.", name="Additions/sec")
    @Override
    public double getAddsPerSec()
    {

        return getCounter(ClientOperations.AddsPerSec);

    }

    @Managed(description="Number of Get operations per second.",name="Fetches/sec")
    @Override
    public double getGetsPerSec()
    {
        return getCounter(ClientOperations.GetPerSec);
    }

    @Managed(description="Number of Insert operations per second.",name="Updates/sec")
    @Override
    public double getInsertsPerSec()
    {
        return getCounter(ClientOperations.UpdPerSec);
    }

    @Managed(description="Number of Remove operations per second.",name="Deletes/sec")
    @Override
    public double getDelsPerSec()
    {
        return getCounter(ClientOperations.DelPerSec);
    }

    @Managed(description="Total number of rquests from all clients on a single machine waiting for response from cache server", name="Request queue size")
    @Override
    public double getRequestQueueSize()
    {
        return getCounter(ClientOperations.RequestQueueSize);
    }

    @Managed(description="Number of Read operations per second", name= "Read Operations/sec")
    @Override
    public double getReadOpsPerSec()
    {
        return getCounter(ClientOperations.ReadOperationsPerSec);
    }

    @Managed(description="Number of Write operations per second", name="Write Operations/sec")
    @Override
    public double getWriteOpsPerSec()
    {
        return getCounter(ClientOperations.WriteOperationsPerSec);
    }

    @Managed(description="Amount of network used by client to send/recived operations in percentage",name="Network Usage")
    @Override
    public double getNetworkUsage()
    {
        return getCounter(ClientOperations.NetworkUsage);
    }

    @Managed(description="The memory (in Magabyte) being used by the runing client. ",name="Memory Usage")
    @Override
    public double getMemoryUsage()
    {
        return getCounter(ClientOperations.MemoryUsage);
    }

    @Managed(description="Number of requests received (meaning cache commands like add, get, insert, remove etc.) by this client", name="Requests/sec")
    @Override
    public double getRequestPerSec()
    {
        return getCounter(ClientOperations.RequestsPerSec);
    }
    
    @Managed(description="Average time, in microseconds, taken to serialize/deserialize one object.", name="Average µs/serialization")
    @Override
    public double getMsecPerSerialization()
    {
        return getCounter(ClientOperations.MsecPerSerialization);
    }
    
    @Managed(description="Average size of the item added to/fetched from the cache.", name="Average Compressed Item Size")    
    @Override
    public double getAverageCompressedItemSize()
    {
        return getCounter(ClientOperations.AvgCompressedItemSize);
    }
    
    @Managed(description="Average per milli-second time of Event operations.", name="Average µs/Event")    
    @Override
    public double getMsecPerEvent()
    {
        return getCounter(ClientOperations.AvgEventPerSec);
    }
    
    @Managed(description="Counter for events processed per second.", name="Event Processed/Sec")    
    @Override
    public double getEventProcessedPerSec()
    {
        return getCounter(ClientOperations.EventProcessedPerSec);
    }
    
    @Managed(description="Counter for events Triggered/Received per second.", name="Event Triggered/Sec")    
    @Override
    public double getEventTriggeredPerSec()
    {
        return getCounter(ClientOperations.EventTriggeredPerSec);
    }
}
