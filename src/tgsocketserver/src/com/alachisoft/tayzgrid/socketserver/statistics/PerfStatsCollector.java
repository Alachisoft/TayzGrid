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
package com.alachisoft.tayzgrid.socketserver.statistics;

import com.alachisoft.tayzgrid.common.enums.EventType;
import com.alachisoft.tayzgrid.common.enums.Time;
import com.alachisoft.tayzgrid.common.EventCategories;
import com.alachisoft.tayzgrid.common.EventID;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.logger.EventLogger;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.PortPool;
import com.alachisoft.tayzgrid.common.stats.NanoSecTimeStats;
import com.alachisoft.tayzgrid.common.stats.UsageStats;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.AverageCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.FlipManager;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.NumberOfItemCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.PerformanceCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.PerformanceCounterBase;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.RateOfCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.StringCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.monitoring.Monitor;
import com.alachisoft.tayzgrid.common.caching.statistics.operations.ServerOperations;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.VMCommittedlMemory;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.SystemFreeMemory;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.VMCpuUsage;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.SystemCpuUsage;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.SystemMemoryUsage;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.SystemNetworkUsage;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.VMMaxMemory;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.VMMemoryUsage;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.VMNetworkUsage;
import com.alachisoft.tayzgrid.common.caching.statistics.monitoring.CacheServerMonitor;

import java.net.InetAddress;

/**
 * Summary description for PerfStatsCollector.
 */
public class PerfStatsCollector implements IDisposable {

    /**
     * Instance name.
     */
    private String _instanceName;
    /**
     * Port number.
     */
    private String _port;

    private StringCounter _clientPort;
    /**
     * performance counter for bytes sent per second.
     */
    private PerformanceCounterBase _pcClientBytesSentPerSec = null;
    /**
     * performance counter for bytes received per second.
     */
    private PerformanceCounterBase _pcClientBytesRecievedPerSec = null;
    /**
     * performance counter for cache requests per second.
     */
    private PerformanceCounterBase _pcRequestsPerSec = null;
    /**
     * performance counter for cache responses per second.
     */
    private PerformanceCounterBase _pcResponsesPerSec = null;
    /**
     * performance counter for avgerage per milli-second time of all cache
     * operations.
     */
    private PerformanceCounterBase _pcMsecPerCacheOperation = null;
    /**
     * performance counter for Cache max. per milli-second time of all cache
     * operations.
     */
//    private PerformanceCounterBase _pcMsecPerCacheOperationBase = null;
    /**
     * usage statistics for per milli-second time of all cache operations.
     */
    private UsageStats _usMsecPerCacheOperation = null;
    /**
     * usage statistics for per milli-second time of all cache operations.
     */
//    private NanoSecTimeStats _nsMsecPerCacheOperation = null;

    /**
     * performance counter for Number of bytes sent per second to other nodes of
     * the cluster..
     */
    private PerformanceCounterBase byteSentPerSec = null;

    /**
     * performance counter for Number of bytes receive per second to other nodes
     * of the cluster..
     */
    private PerformanceCounterBase byteReceivePerSec = null;

    /**
     * performance counter for Number of items in event queue.
     */
    private PerformanceCounterBase eventQueueCount = null;

//    /**
//     * performance counter for Number of General Notification Events in Queue.
//     */
//    private PerformanceCounterBase generalNotificationQueueSize = null;
//    
//    /**
//     * performance counter for Number of items in response queue.
//     */
//    private PerformanceCounterBase responseQueueCount = null;
//    
//    /**
//     * performance counter for Size of response queue specified in bytes.
//     */
//    private PerformanceCounterBase responseQueueSize = null;
    /**
     * usage statistics for number of events currently in queue for cache
     * general event notifications.
     */
    private PerformanceCounterBase _generalNotificationQueueSize = null;
    private PerformanceCounterBase _pcSystemFreeMemory = null;
    private PerformanceCounterBase _pcSystemCpuUsage = null;
    private PerformanceCounterBase _pcSystemMemoryUsage = null;
    private PerformanceCounterBase _pcSystemNetworkUsage = null;
    private PerformanceCounterBase _pcVMCommittedMemory = null;
    private PerformanceCounterBase _pcVMCpuUsage = null;
    private PerformanceCounterBase _pcVMMemoryUsage = null;
    private PerformanceCounterBase _pcVMMaxMemory = null;
    private PerformanceCounterBase _pcVMNetworkUsage = null;
    /**
     * Category name of counter performance data.
     */
    private static final String PC_CATEGORY = "TayzGrid";
    private static ILogger logger;
    private Thread flipManager;
    private InetAddress iP;
    Monitor monitor;

    /**
     * Constructor
     *
     * @param instanceName
     * @param port
     * @param iP
     */
    public PerfStatsCollector(String instanceName, int port, InetAddress iP, ILogger logger) {
        _port = ":" + (new Integer(port)).toString();
        _instanceName = instanceName;
        this.iP = iP;
        this.logger = logger;
    }

    /**
     * Returns true if the current user has the rights to read/write to
     * performance counters under the category of object cache.
     *
     * @return
     */
    public final String getInstanceName() {
        return _instanceName;
    }

    public final void setInstanceName(String value) {
        _instanceName = value;
    }

    /**
     * Returns true if the current user has the rights to read/write to
     * performance counters under the category of object cache.
     *
     * @return
     */
    public final boolean getUserHasAccessRights() {
        return true;
    }

    public final void setClientPort(String cacheID, String ip, String port) {
        if (!port.isEmpty() && !cacheID.isEmpty()) {
            _clientPort.appendValue(cacheID, ip, port);
        }
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    @Override
    public final void dispose() {
        synchronized (this) {
            if (monitor != null) {
                if (byteSentPerSec != null) {
                    if (monitor != null) {
                        monitor.unRegisterCounter(ServerOperations.byteSentPerSec);
                    }
                }

                if (byteReceivePerSec != null) {
                    if (monitor != null) {
                        monitor.unRegisterCounter(ServerOperations.bytereceivePerSec);
                    }
                }

                if (eventQueueCount != null) {
                    if (monitor != null) {
                        monitor.unRegisterCounter(ServerOperations.eventQueueCount);
                    }
                }

                if (_pcClientBytesRecievedPerSec != null) {
                    monitor.unRegisterCounter(ServerOperations.ClientBytesRecievedPerSec);
                    _pcClientBytesRecievedPerSec = null;
                }
                if (_pcClientBytesSentPerSec != null) {
                    monitor.unRegisterCounter(ServerOperations.ClientBytesSentPerSec);
                    _pcClientBytesSentPerSec = null;
                }
                if (_pcRequestsPerSec != null) {
                    monitor.unRegisterCounter(ServerOperations.RequestsPerSec);
                    _pcRequestsPerSec = null;
                }
                if (_pcResponsesPerSec != null) {
                    monitor.unRegisterCounter(ServerOperations.ResponsesPerSec);
                    _pcResponsesPerSec = null;
                }
                if (_pcMsecPerCacheOperation != null) {
                    monitor.unRegisterCounter(ServerOperations.MSecPerCacheOperation);
                    _pcMsecPerCacheOperation = null;
                }
                if (_pcVMCommittedMemory != null) {
                    monitor.unRegisterCounter(ServerOperations.SystemFreeMemory);
                }
                //SP2
                if (_generalNotificationQueueSize != null) {
                    monitor.unRegisterCounter(ServerOperations.GeneralNotificationQueueSize);
                    _generalNotificationQueueSize = null;
                }
                PortPool.getInstance().disposeSNMPPort(_instanceName);
                monitor.unRegisterNode();
                monitor.stopJMX();
                monitor = null;
                
                if(flipManager!=null)
                {
                    if(flipManager.isAlive())
                    {
                        flipManager.interrupt();
                    }
                }

            }
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Intialization">
    /**
     * Initializes the counter instances and category.
     */
    public final void InitializePerfCounters() {
        try {
            if (!getUserHasAccessRights()) {
                return;
            }
            synchronized (this) {
                monitor = new CacheServerMonitor(_instanceName, logger);
                flipManager = new Thread(new FlipManager());
                flipManager.start();

                monitor.startJMX();

                monitor.registerNode();

                monitor.startSNMP();

                _clientPort = new StringCounter("Client Port", _instanceName);
                monitor.registerCounter(ServerOperations.ClientPort, _clientPort);

                byteSentPerSec = new RateOfCounter("byte sent per sec", _instanceName);
                monitor.registerCounter(ServerOperations.byteSentPerSec, byteSentPerSec);

                byteReceivePerSec = new RateOfCounter("byte receive per sec", _instanceName);
                monitor.registerCounter(ServerOperations.bytereceivePerSec, byteReceivePerSec);

                eventQueueCount = new NumberOfItemCounter("event queue count", _instanceName);
                monitor.registerCounter(ServerOperations.eventQueueCount, eventQueueCount);

                _pcRequestsPerSec = new RateOfCounter("Requests/sec", _instanceName);
                monitor.registerCounter(ServerOperations.RequestsPerSec, _pcRequestsPerSec);

                _pcResponsesPerSec = new RateOfCounter("Response/sec", _instanceName);
                monitor.registerCounter(ServerOperations.ResponsesPerSec, _pcResponsesPerSec);

                _pcClientBytesSentPerSec = new RateOfCounter("Client bytes sent/sec", _instanceName);
                monitor.registerCounter(ServerOperations.ClientBytesSentPerSec, _pcClientBytesSentPerSec);

                _pcClientBytesRecievedPerSec = new RateOfCounter("Client bytes received/sec", _instanceName);
                monitor.registerCounter(ServerOperations.ClientBytesRecievedPerSec, _pcClientBytesRecievedPerSec);

                _pcMsecPerCacheOperation = new AverageCounter("Average sec/cache operation", _instanceName);
                monitor.registerCounter(ServerOperations.MSecPerCacheOperation, _pcMsecPerCacheOperation);

                _generalNotificationQueueSize = new NumberOfItemCounter(PC_CATEGORY, "General Notifications Queue Size", _instanceName);
                monitor.registerCounter(ServerOperations.GeneralNotificationQueueSize, _generalNotificationQueueSize);

                _pcSystemCpuUsage = new SystemCpuUsage("Total Cpu Consumption", _instanceName);
                monitor.registerCounter(ServerOperations.SystemCpuUsage, _pcSystemCpuUsage);

                _pcSystemFreeMemory = new SystemFreeMemory("Total Free Memory", _instanceName);
                monitor.registerCounter(ServerOperations.SystemFreeMemory, _pcSystemFreeMemory);

                _pcSystemMemoryUsage = new SystemMemoryUsage("Total Memory Usage", _instanceName);
                monitor.registerCounter(ServerOperations.SystemMemoryUsage, _pcSystemMemoryUsage);

                _pcSystemNetworkUsage = new SystemNetworkUsage("Totao NetworkUsage", _instanceName);
                monitor.registerCounter(ServerOperations.SystemNetworkUsage, _pcSystemNetworkUsage);

                _pcVMCpuUsage = new VMCpuUsage("TayzGrid Cpu Consumpiton", _instanceName);
                monitor.registerCounter(ServerOperations.VMCpuUsage, _pcVMCpuUsage);

                _pcVMCommittedMemory = new VMCommittedlMemory("Committed Virtual Memory", _instanceName);
                monitor.registerCounter(ServerOperations.VMCommittedMemory, _pcVMCommittedMemory);

                _pcVMMemoryUsage = new VMMemoryUsage("TayzGrid Memory Usage", _instanceName);
                monitor.registerCounter(ServerOperations.VMMemoryUsage, _pcVMMemoryUsage);

                _pcVMMaxMemory = new VMMaxMemory("Maximum TayzGrid Memory Usage", _instanceName);
                monitor.registerCounter(ServerOperations.VMMaxMemory, _pcVMMaxMemory);

                _pcVMNetworkUsage = new VMNetworkUsage("TayzGrid Network Usage", _instanceName);
                monitor.registerCounter(ServerOperations.VMNetworkUsage, _pcVMNetworkUsage);

                _usMsecPerCacheOperation = new UsageStats();
            }
        } catch (Exception e) {
            EventLogger.LogEvent("TayzGrid", "An error occured while initializing counters for Cache Server. " + e.toString(), EventType.ERROR, EventCategories.Error, EventID.GeneralError);
        }

    }
    //</editor-fold>

    public final void incrementByteSentPerSecStats() {
        if (byteSentPerSec != null) {
            synchronized (byteSentPerSec) {
                byteSentPerSec.increment();
            }
        }
    }

    public final void incrementByteSentPerSecStatsBy(long value) {
        if (byteSentPerSec != null) {
            synchronized (byteSentPerSec) {
                byteSentPerSec.incrementBy(value);
            }
        }
    }

    public final void incrementByteReceivePerSecStats() {
        if (byteReceivePerSec != null) {
            synchronized (byteReceivePerSec) {
                byteReceivePerSec.increment();
            }
        }
    }

    public final void incrementByteReceivePerSecStatsBy(long value) {
        if (byteReceivePerSec != null) {
            synchronized (byteReceivePerSec) {
                byteReceivePerSec.incrementBy(value);
            }
        }
    }

    public final void incrementEventQueueCount() {
        if (eventQueueCount != null) {
            synchronized (eventQueueCount) {
                eventQueueCount.increment();
            }
        }
    }

    public final void setEventQueueCount(long value) {
        if (eventQueueCount != null) {
            synchronized (eventQueueCount) {
                eventQueueCount.setValue(value);
            }
        }
    }

    public final void incrementEventQueueCountBy(long value) {
        if (eventQueueCount != null) {
            synchronized (eventQueueCount) {
                eventQueueCount.incrementBy(value);
            }
        }
    }

    /**
     * Increment the performance counter for Client bytes sent.
     *
     * @param bytesSent
     */
    public final void incrementBytesSentPerSecStats(long bytesSent) {
        if (_pcClientBytesSentPerSec != null) {
            synchronized (_pcClientBytesSentPerSec) {
                _pcClientBytesSentPerSec.incrementBy(bytesSent);
            }
        }
    }

    /**
     * Increment the performance counter for Client bytes received.
     *
     * @param bytesReceived
     */
    public final void incrementBytesReceivedPerSecStats(long bytesReceived) {
        if (_pcClientBytesRecievedPerSec != null) {
            synchronized (_pcClientBytesRecievedPerSec) {
                _pcClientBytesRecievedPerSec.incrementBy(bytesReceived);
            }
        }
    }

    /**
     * Increment the performance counter for Requests Per second.
     *
     * @param requests
     */
    public final void incrementRequestsPerSecStats(long requests) {
        if (_pcRequestsPerSec != null) {
            synchronized (_pcRequestsPerSec) {
                _pcRequestsPerSec.incrementBy(requests);
            }
        }
    }

    /**
     * Increment the performance counter for Responses Per second.
     *
     * @param responses
     */
    public final void incrementResponsesPerSecStats(long responses) {
        if (_pcResponsesPerSec != null) {
            synchronized (_pcResponsesPerSec) {
                _pcResponsesPerSec.incrementBy(responses);
            }
        }
    }

    /**
     * Timestamps the startJMX of sampling interval for avg. and max. per
     * mill-second time of all cache operations.
     */
    public final void mSecPerCacheOperationBeginSample() {
        if (_pcMsecPerCacheOperation != null) {
            synchronized (_usMsecPerCacheOperation)//_nsMsecPerCacheOperation)
            {
                _usMsecPerCacheOperation.BeginSample();
            }
        }
    }

    /**
     * Timestample and updates the counter for Cache avg. and max. per
     * mill-second time of any operation operation.
     */
    public final void mSecPerCacheOperationEndSample() {
        if (_pcMsecPerCacheOperation != null) {
            synchronized (_pcMsecPerCacheOperation) {
                _usMsecPerCacheOperation.EndSample();
                _pcMsecPerCacheOperation.incrementBy(Time.toMicroSeconds(_usMsecPerCacheOperation.getCurrent(), Time.nSEC)); //_usMsecPerCacheOperation.Current);
            }
        }
    }

    /**
     * Increment the performance counter for Cache General Notification Queue.
     *
     * @param count
     */
    public void SettNotificationQueueSizeStats(long count) {
        if (_generalNotificationQueueSize != null) {
            synchronized (_generalNotificationQueueSize) {
                _generalNotificationQueueSize.setValue(count);
            }
        }
    }
}
