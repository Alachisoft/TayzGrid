/*
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

package com.alachisoft.tayzgrid.cluster.stack;

 
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.PerformanceCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.RateOfCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.monitoring.ClusteredCacheMonitor;
import com.alachisoft.tayzgrid.common.caching.statistics.monitoring.Monitor;
import com.alachisoft.tayzgrid.common.caching.statistics.operations.CacheOperations;
import java.lang.management.ManagementFactory;

 
public class PerfStatsCollector implements IDisposable
{
   
   
    Monitor monitor;
    /**
     * Instance name.
     */
    private String _instanceName;
    /**
     * Port number.
     */
    private String _port;

    /**
     * performance counter for Clustered Operations.
     */
    private PerformanceCounter _pcClusteredOperationsPerSec = null;

    /**
     * Constructor
     *
     * @param instanceName
     */
    public PerfStatsCollector(String instanceName)
    {
        _instanceName = createInstanceName(instanceName, 0);
    }

    /**
     * Constructor
     *
     * @param instanceName
     * @param port
     */
    public PerfStatsCollector(String instanceName, int port)
    {

        _port = ":" + (new Integer(port)).toString();
        _instanceName = createInstanceName(instanceName, port);
    }

    private String createInstanceName(String instanceName, int port)
    {
        return instanceName + " - " + ManagementFactory.getRuntimeMXBean().getName() + "-" + port;
    }

    /**
     * Returns true if the current user has the rights to read/write to performance counters under the category of object cache.
     */
    public final String getInstanceName()
    {
        return _instanceName;
    }

    public final void setInstanceName(String value)
    {
        _instanceName = value;
    }

    /**
     * Returns true if the current user has the rights to read/write to performance counters under the category of object cache.
     */
    public final boolean getUserHasAccessRights()
    {

        return true;
    }


    /**
     * Performs application-defined tasks associated with freeing, releasing, or resetting unmanaged resources.
     */
    public final void dispose()
    {

        synchronized (this)
        {

            if (_pcClusteredOperationsPerSec != null)
            {

                if (this.monitor != null)
                {
                    monitor.unRegisterCounter(CacheOperations.ClusterOperationsPerSec);
                }
                _pcClusteredOperationsPerSec = null;
            }

            if (this.monitor != null)
            {
                monitor.unRegisterNode();
                monitor = null;
            }
        }
    }


    //<editor-fold defaultstate="collapsed" desc="Initailization">
    /**
     * Initializes the counter instances and category.
     *
     * @param enableDebuggingCounters
     * @param monitor
     */
    public final void InitializePerfCounters(boolean enableDebuggingCounters, Monitor monitor)
    {
        try
        {
            if (monitor == null || !Common.is(monitor, ClusteredCacheMonitor.class))
            {
               
                return;
            }
            if (!getUserHasAccessRights())
            {
                return;
            }

            synchronized (this)
            {
                String instname = _instanceName;
               
                _instanceName = instname;
           
                _pcClusteredOperationsPerSec = new RateOfCounter("Cluster ops/sec", _instanceName);
                monitor.registerCounter(CacheOperations.ClusterOperationsPerSec, _pcClusteredOperationsPerSec);
               
            }
        }
        catch (Exception e)
        {
             
        }
        
    }
     

    public final void incrementClusteredOperationsPerSecStats()
    {
 
        if (_pcClusteredOperationsPerSec != null)
        {
            synchronized (_pcClusteredOperationsPerSec)
            {
                _pcClusteredOperationsPerSec.increment();
            }
        }
    }
 
}
