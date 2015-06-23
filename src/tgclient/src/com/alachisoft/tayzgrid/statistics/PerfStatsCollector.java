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

package com.alachisoft.tayzgrid.statistics;

import com.alachisoft.tayzgrid.common.enums.Time;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.stats.UsageStats;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.AverageCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.FlipManager;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.NumberOfItemCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.PerformanceCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.RateOfCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.VMCpuUsage;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.VMMemoryUsage;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.VMNetworkUsage;
import com.alachisoft.tayzgrid.common.caching.statistics.monitoring.ClientMonitor;
import com.alachisoft.tayzgrid.common.caching.statistics.monitoring.Monitor;
import com.alachisoft.tayzgrid.common.caching.statistics.operations.ClientOperations;
import java.lang.management.ManagementFactory;

public class PerfStatsCollector implements IDisposable
{

    private PerformanceCounter _pcAvgItemSize;
    private PerformanceCounter _pcAddPerSec;
    private PerformanceCounter _pcGetPerSec;
    private PerformanceCounter _pcUpdPerSec;
    private PerformanceCounter _pcDelPerSec;
    private PerformanceCounter _pcReadOperationsPerSec;
    private PerformanceCounter _pcWriteOperationsPerSec;
    private PerformanceCounter _pcMsecPerGetAvg;
    private PerformanceCounter _pcMsecPerAddAvg;
    private PerformanceCounter _pcMsecPerUpdateAvg;
    private PerformanceCounter _pcMsecPerDelAvg;
    private PerformanceCounter _pcMsecPerDecryptionAvg;
    private PerformanceCounter _pcMsecPerEncryptionAvg;
    private PerformanceCounter _pcReqrQueueSize;
    private PerformanceCounter _pcCpuUsage;
    private PerformanceCounter _pcMemoryUsage;
    private PerformanceCounter _pcNetworkUsage;
    private PerformanceCounter _pcRequestsPerSec;
    
    private PerformanceCounter _pcAvgCompressedItemSize;
    private PerformanceCounter _pcMsecPerSerializationAvg;
    
    private PerformanceCounter _pcMsecPerEventAvg;
    private PerformanceCounter _pcEventProcesedPerSec;
    private PerformanceCounter _pcEventTriggeredPerSec;
    
    private UsageStats _usMsecPerGet;
    private UsageStats _usMsecPerDel;
    private UsageStats _usMsecPerUpdate;
    private UsageStats _usMsecPerAdd;
    private UsageStats _usMsecPerDecryption;
    private UsageStats _usMsecPerEncryption;
    private String _instanceName;
    private Thread flipManager = null;
    Monitor monitor;

    public PerfStatsCollector(String instance, boolean inproc)
    {
        _instanceName = GetInstanceName(instance, 0, inproc);
    }

    public PerfStatsCollector(String instance, int port, boolean inproc)
    {
        _instanceName = GetInstanceName(instance, port, inproc);
    }

    public final String GetInstanceName(String instanceName, int port, boolean inProc)
    {
        return !inProc ? instanceName : instanceName + " - " + ManagementFactory.getRuntimeMXBean().getName() + " - " + port;
    }

    public String getSnmpAddress()
    {
        return monitor.getStatsServerIp().getHostAddress().toString() + ":" + monitor.getSnmpPort();
    }

    /**
     *
     * @param inProc
     * @throws Exception
     */
    public void inializePerformanceCounters(boolean inProc) throws Exception
    {
        flipManager = new Thread(new FlipManager());
        flipManager.start();
        monitor = new ClientMonitor(_instanceName);

        monitor.startJMX();
        monitor.registerNode();
        monitor.startSNMP();

        
        
        _pcAddPerSec = new RateOfCounter("Additions/sec", _instanceName);
        monitor.registerCounter(ClientOperations.AddsPerSec, _pcAddPerSec);

        _pcGetPerSec = new RateOfCounter("Fetches/sec", _instanceName);
        monitor.registerCounter(ClientOperations.GetPerSec, _pcGetPerSec);

        _pcUpdPerSec = new RateOfCounter("Updates/sec", _instanceName);
        monitor.registerCounter(ClientOperations.UpdPerSec, _pcUpdPerSec);

        _pcDelPerSec = new RateOfCounter("Deletes/sec", _instanceName);
        monitor.registerCounter(ClientOperations.DelPerSec, _pcDelPerSec);

        _pcReadOperationsPerSec = new RateOfCounter("Read Operations/Sec", _instanceName);
        monitor.registerCounter(ClientOperations.ReadOperationsPerSec, _pcReadOperationsPerSec);

        _pcWriteOperationsPerSec = new RateOfCounter("Write Operations/Sec", _instanceName);
        monitor.registerCounter(ClientOperations.WriteOperationsPerSec, _pcWriteOperationsPerSec);
         
        
        _pcEventProcesedPerSec = new RateOfCounter("Event Processed/Sec", _instanceName);
        monitor.registerCounter(ClientOperations.EventProcessedPerSec, _pcEventProcesedPerSec);
        
        _pcEventTriggeredPerSec = new RateOfCounter("Event Triggered/Sec", _instanceName);
        monitor.registerCounter(ClientOperations.EventTriggeredPerSec, _pcEventTriggeredPerSec);
        
        _pcMsecPerEventAvg=new AverageCounter("Average 탎/Event", _instanceName);
        monitor.registerCounter(ClientOperations.AvgEventPerSec, _pcMsecPerEventAvg);
        
        
        
        _pcMsecPerGetAvg = new AverageCounter("Average 탎/fetch", _instanceName);
        monitor.registerCounter(ClientOperations.MSecPerGet, _pcMsecPerGetAvg);
        _usMsecPerGet = new UsageStats();


        _pcMsecPerAddAvg = new AverageCounter("Average 탎/add", _instanceName);
        monitor.registerCounter(ClientOperations.MSecPerAdd, _pcMsecPerAddAvg);
        _usMsecPerAdd = new UsageStats();


        _pcMsecPerDecryptionAvg = new AverageCounter("Average 탎/decryption", _instanceName);
        monitor.registerCounter(ClientOperations.MSecPerDecryption, _pcMsecPerDecryptionAvg);
        _usMsecPerDecryption = new UsageStats();

        _pcMsecPerEncryptionAvg = new AverageCounter("Average 탎/encryption", _instanceName);
        monitor.registerCounter(ClientOperations.MSecPerEncryption, _pcMsecPerEncryptionAvg);
        _usMsecPerEncryption = new UsageStats();

        _pcMsecPerUpdateAvg = new AverageCounter("Average 탎/update", _instanceName);
        monitor.registerCounter(ClientOperations.MSecPerUpdateAvg, _pcMsecPerUpdateAvg);
        _usMsecPerUpdate = new UsageStats();

        _pcMsecPerDelAvg = new AverageCounter("Average 탎/del", _instanceName);
        monitor.registerCounter(ClientOperations.MSecPerDelAvg, _pcMsecPerDelAvg);
        _usMsecPerDel = new UsageStats();

        _pcReqrQueueSize = new NumberOfItemCounter("Request Queue Size", _instanceName);
        monitor.registerCounter(ClientOperations.RequestQueueSize, _pcReqrQueueSize);

        _pcAvgItemSize = new AverageCounter("Average Item Size", _instanceName);
        monitor.registerCounter(ClientOperations.AvgItemSize, _pcAvgItemSize);

        _pcCpuUsage = new VMCpuUsage("Client Cpu Usage", _instanceName);
        monitor.registerCounter(ClientOperations.CpuUsage, _pcCpuUsage);

        _pcMemoryUsage = new VMMemoryUsage("Client Memory Usage", _instanceName);
        monitor.registerCounter(ClientOperations.MemoryUsage, _pcMemoryUsage);

        _pcNetworkUsage = new VMNetworkUsage("Client Network Usage", _instanceName);
         monitor.registerCounter(ClientOperations.NetworkUsage, _pcNetworkUsage);

         _pcRequestsPerSec = new RateOfCounter("Requests Per Sec", _instanceName);
         monitor.registerCounter(ClientOperations.RequestsPerSec, _pcRequestsPerSec);
         
         _pcAvgCompressedItemSize=new AverageCounter("Average Compressed Item Size", _instanceName);
         monitor.registerCounter(ClientOperations.AvgCompressedItemSize, _pcAvgCompressedItemSize);
         
         _pcMsecPerSerializationAvg=new AverageCounter("Average 탎/serialization", _instanceName);
         monitor.registerCounter(ClientOperations.MsecPerSerialization, _pcMsecPerSerializationAvg);
    }

    public void incrementClientRequestsPerSecStatsBy(long requests)
    {
        if (_pcRequestsPerSec != null)
        {
            synchronized (_pcRequestsPerSec)
            {
                _pcRequestsPerSec.incrementBy(requests);
            }
        }
    }

    /**
     *
     * Increment the performance counter for Cache update operations per second.
     */
    public void incrementAddPerSecStats()
    {
        if (_pcAddPerSec != null)
        {
            synchronized (_pcAddPerSec)
            {
                _pcAddPerSec.increment();
            }
        }

        if (_pcWriteOperationsPerSec != null)
        {
            synchronized (_pcWriteOperationsPerSec)
            {
                _pcWriteOperationsPerSec.increment();
            }
        }
    }

    /**
     *
     * Increment the performance counter for Cache get operations per second.
     */
    public void incrementGetPerSecStats()
    {
        if (_pcGetPerSec != null)
        {
            synchronized (_pcGetPerSec)
            {
                _pcGetPerSec.increment();
            }
        }

        if (_pcReadOperationsPerSec != null)
        {
            synchronized (_pcReadOperationsPerSec)
            {
                _pcReadOperationsPerSec.increment();
            }
        }
    }

    /**
     *
     * Increment the performance counter for Cache update operations per second.
     */
    public void incrementUpdPerSecStats()
    {
        if (_pcUpdPerSec != null)
        {
            synchronized (_pcUpdPerSec)
            {
                _pcUpdPerSec.increment();
            }
        }
        if (_pcWriteOperationsPerSec != null)
        {
            synchronized (_pcWriteOperationsPerSec)
            {
                _pcWriteOperationsPerSec.increment();
            }
        }
    }

    /**
     *
     * Increment the performance counter for Cache remove operations per second.
     */
    public void incrementDelPerSecStats()
    {
        if (_pcDelPerSec != null)
        {
            synchronized (_pcDelPerSec)
            {
                _pcDelPerSec.increment();
            }
        }
        if (_pcWriteOperationsPerSec != null)
        {
            synchronized (_pcWriteOperationsPerSec)
            {
                _pcWriteOperationsPerSec.increment();
            }
        }
    }


    //<editor-fold defaultstate="collapsed" desc="MSecPerGet">
    /**
     *
     * Timestamps the start of sampling interval for Cache avg. and max. per mill-second time of fetch operations.
     */
    public void mSecPerGetBeginSample()
    {
        if (_pcMsecPerGetAvg != null)
        {
            synchronized (_usMsecPerGet)
            {
                _usMsecPerGet.BeginSample();
            }
        }
    }

    /**
     * Timestamps and updates the counter for Cache avg. and max. per mill-second time of fetch operations.
     */
    public void mSecPerGetEndSample()
    {
        if (_pcMsecPerGetAvg != null)
        {
            synchronized (_pcMsecPerGetAvg)
            {
                _usMsecPerGet.EndSample();
                _pcMsecPerGetAvg.incrementBy(Time.toMicroSeconds(_usMsecPerGet.getCurrent(), Time.nSEC));
            }
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="MSecPerAdd">
    /**
     *
     * Timestamps the start of sampling interval for Cache avg. and max. per mill-second time of add operations.
     */
    public void mSecPerAddBeginSample()
    {
        if (_pcMsecPerAddAvg != null)
        {
            synchronized (_pcMsecPerAddAvg)
            {
                _usMsecPerAdd.BeginSample();
            }
        }
    }

    /**
     *
     * Timestamps and updates the counter for Cache avg. and max. per mill-second time of add operations.
     */
    public void mSecPerAddEndSample()
    {
        if (_pcMsecPerAddAvg != null)
        {
            synchronized (_pcMsecPerAddAvg)
            {
                _usMsecPerAdd.EndSample();
                _pcMsecPerAddAvg.incrementBy(Time.toMicroSeconds(_usMsecPerAdd.getCurrent(), Time.nSEC));
            }
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="MSecPerDecryption">
    /**
     *
     * Timestamps the start of sampling interval for Cache avg. and max. per mill-second time of decryption operations.
     */
    public void mSecPerDecryptionBeginSample()
    {
        if (_pcMsecPerDecryptionAvg != null)
        {
            synchronized (_pcMsecPerDecryptionAvg)
            {
                _usMsecPerDecryption.BeginSample();
            }
        }
    }

    /**
     *
     * Timestamps and updates the counter for Cache avg. and max. per mill-second time of decryption operations.
     */
    public void mSecPerDecryptionEndSample()
    {
        if (_pcMsecPerDecryptionAvg != null)
        {
            synchronized (_pcMsecPerDecryptionAvg)
            {
                _usMsecPerDecryption.EndSample();
                _pcMsecPerDecryptionAvg.incrementBy(Time.toMicroSeconds(_usMsecPerDecryption.getCurrent(),Time.nSEC));
            }
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="MSecPerEncryption">
    /**
     *
     * Timestamps the start of sampling interval for Cache avg. and max. per mill-second time of encryption operations.
     */
    public void mSecPerEncryptionBeginSample()
    {
        if (_pcMsecPerEncryptionAvg != null)
        {
            synchronized (_pcMsecPerEncryptionAvg)
            {
                _usMsecPerEncryption.BeginSample();
            }
        }
    }

    /**
     *
     * Timestamps and updates the counter for Cache avg. and max. per mill-second time of encryption operations.
     */
    public void mSecPerEncryptionEndSample()
    {
        if (_pcMsecPerEncryptionAvg != null)
        {
            synchronized (_pcMsecPerEncryptionAvg)
            {
                _usMsecPerEncryption.EndSample();
                _pcMsecPerEncryptionAvg.incrementBy(Time.toMicroSeconds(_usMsecPerEncryption.getCurrent(), Time.nSEC));
            }
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="MSecPerUpdate">
    /**
     *
     * Timestamps the start of sampling interval for Cache avg. and max. per mill-second time of Update operations.
     */
    public void mSecPerUpdateBeginSample()
    {
        if (_pcMsecPerUpdateAvg != null)
        {
            synchronized (_pcMsecPerUpdateAvg)
            {
                _usMsecPerUpdate.BeginSample();
            }
        }
    }

    /**
     *
     * Timestamps and updates the counter for Cache avg. and max. per mill-second time of Update operations.
     */
    public void mSecPerUpdateEndSample()
    {
        if (_pcMsecPerUpdateAvg != null)
        {
            synchronized (_pcMsecPerUpdateAvg)
            {
                _usMsecPerUpdate.EndSample();
                _pcMsecPerUpdateAvg.incrementBy(Time.toMicroSeconds(_usMsecPerUpdate.getCurrent(), Time.nSEC));
            }
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="MSecPerDel">
    /**
     *
     * Timestamps the start of sampling interval for Cache avg. and max. per mill-second time of Update operations.
     */
    public void mSecPerDelBeginSample()
    {
        if (_pcMsecPerDelAvg != null)
        {
            synchronized (_pcMsecPerDelAvg)
            {
                _usMsecPerDel.BeginSample();
            }
        }
    }

    /**
     *
     * Timestamps and updates the counter for Cache avg. and max. per mill-second time of Update operations.
     */
    public void mSecPerDelEndSample()
    {
        if (_pcMsecPerDelAvg != null)
        {
            synchronized (_pcMsecPerDelAvg)
            {
                _usMsecPerDel.EndSample();
                _pcMsecPerDelAvg.incrementBy(Time.toMicroSeconds(_usMsecPerDel.getCurrent(), Time.nSEC));
            }
        }
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="MSecPerSerialization">
    public void incrementMSecPerSerializaion(long value)
    {
       if(_pcMsecPerSerializationAvg != null)
       {
           synchronized(_pcMsecPerSerializationAvg)
           {
               _pcMsecPerSerializationAvg.incrementBy(value);
           }
       }
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="AvgCompressedItemSize">
    public void incrementAvgCompressedItemSize(long value)
    {
        if (_pcAvgCompressedItemSize != null)
        {
            synchronized (_pcAvgCompressedItemSize)
            {
                _pcAvgCompressedItemSize.incrementBy(value);
            }
        }
    }
    //</editor-fold>
    
    
     //<editor-fold defaultstate="collapsed" desc="MSecPerEvent">
    public void incrementMSecPerEvent(long value)
    {
       if(_pcMsecPerEventAvg != null)
       {
           synchronized(_pcMsecPerEventAvg)
           {
               _pcMsecPerEventAvg.incrementBy(value);
           }
       }
    }
    
    public void incrementMSecPerEvent()
    {
       if(_pcMsecPerEventAvg != null)
       {
           synchronized(_pcMsecPerEventAvg)
           {
               _pcMsecPerEventAvg.increment();
           }
       }
    }
    
    
    //</editor-fold>
    
     //<editor-fold defaultstate="collapsed" desc="EventProcessedPerSec">
    public void incrementEventProcessedPerSec(long value)
    {
       if(_pcEventProcesedPerSec != null)
       {
           synchronized(_pcEventProcesedPerSec)
           {
               _pcEventProcesedPerSec.incrementBy(value);
           }
       }
    }
    
    public void incrementEventProcessedPerSec()
    {
       if(_pcEventProcesedPerSec != null)
       {
           synchronized(_pcEventProcesedPerSec)
           {
               _pcEventProcesedPerSec.increment();
           }
       }
    }
    
    
    //</editor-fold>
    
     //<editor-fold defaultstate="collapsed" desc="EventTriggeredPerSec">
    public void incrementEventTriggeredPerSec(long value)
    {
       if(_pcEventTriggeredPerSec != null)
       {
           synchronized(_pcEventTriggeredPerSec)
           {
               _pcEventTriggeredPerSec.incrementBy(value);
           }
       }
    }
    
    public void incrementEventTriggeredPerSec()
    {
       if(_pcEventTriggeredPerSec != null)
       {
           synchronized(_pcEventTriggeredPerSec)
           {
               _pcEventTriggeredPerSec.increment();
           }
       }
    }
    
    
    //</editor-fold>
    
    
    
    /**
     *
     * Increments the performance counter for Average Item Size.
     *
     * @param itemSize
     */
    public void incrementAvgItemSize(long itemSize)
    {
        if (_pcAvgItemSize != null)
        {
            synchronized (_pcAvgItemSize)
            {
                _pcAvgItemSize.incrementBy(itemSize);
            }
        }
    }

    /**
     *
     * Increment the performance counter for Mirror Queue size by one.
     */
    public void incrementRequestQueueSize()
    {
        if (_pcReqrQueueSize != null)
        {
            synchronized (_pcReqrQueueSize)
            {
                _pcReqrQueueSize.increment();
            }
        }
    }

    /**
     *
     * Decrement the performance counter for Mirror Queue size by one.
     */
    public void decrementRequestQueueSize()
    {
        if (_pcReqrQueueSize != null)
        {
            synchronized (_pcReqrQueueSize)
            {
                if (_pcReqrQueueSize.getValue() > 0)
                {
                    _pcReqrQueueSize.decrement();
                }
            }
        }
    }

    public void dispose()
    {
        synchronized (this)
        {
            if (monitor != null)
            {
                if (_pcAddPerSec != null)
                {
                    monitor.unRegisterCounter(ClientOperations.AddsPerSec);
                    _pcAddPerSec = null;
                }
                if (_pcAvgItemSize != null)
                {
                    monitor.unRegisterCounter(ClientOperations.AvgItemSize);
                    _pcAvgItemSize = null;
                }
                
                if(_pcAvgCompressedItemSize!=null)
                {
                    monitor.unRegisterCounter(ClientOperations.AvgCompressedItemSize);
                }
                
                if(_pcMsecPerSerializationAvg!=null)
                {
                    monitor.unRegisterCounter(ClientOperations.MsecPerSerialization);
                }
                
                if(_pcMsecPerEventAvg!=null)
                {
                    monitor.unRegisterCounter(ClientOperations.AvgEventPerSec);
                }
                
                if(_pcEventProcesedPerSec!=null)
                {
                    monitor.unRegisterCounter(ClientOperations.EventProcessedPerSec);
                }
                if(_pcEventTriggeredPerSec!=null)
                {
                    monitor.unRegisterCounter(ClientOperations.EventTriggeredPerSec);
                }
                

                monitor.unRegisterNode();
                monitor.stopJMX();
                try
                {
                    monitor.stopSNMP();
                }
                catch (Exception e)
                {
                }
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
}